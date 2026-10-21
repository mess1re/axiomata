package me.mss1r.axiomata.collision;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import me.mss1r.axiomata.blueprint.api.construction.ConstructionTarget;
import me.mss1r.axiomata.blueprint.api.construction.UnderConstruction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import org.junit.jupiter.api.*;
import java.util.*;

class ConstructionTargetTest {
    private final List<CollidableStructure> registered = new ArrayList<>();
    private Level level;
    private Player player;
    private static final Vec3 EYE = new Vec3(0, 2, 0);

    @BeforeEach void world() {
        level = mock(Level.class);
        player = mock(Player.class);
        when(player.level()).thenReturn(level);
        when(player.getEyePosition(1)).thenReturn(EYE);
        when(player.getViewVector(1)).thenReturn(new Vec3(0, 0, 1));
        when(player.pick(anyDouble(), anyFloat(), anyBoolean())).thenReturn(
                BlockHitResult.miss(EYE.add(0, 0, 4), Direction.NORTH, new BlockPos(0, 2, 4)));
    }

    @AfterEach void cleanup() { registered.forEach(StructureCollisionRegistry::unregister); }

    private CollidableStructure structure(boolean unfinished) {
        Entity entity = mock(Entity.class, withSettings().extraInterfaces(CollidableStructure.class, UnderConstruction.class));
        CollidableStructure structure = (CollidableStructure) entity;
        var solid = box("deck", 3);
        when(entity.level()).thenReturn(level);
        when(((UnderConstruction) entity).isFullyBuilt()).thenReturn(!unfinished);
        when(structure.collisionTransform()).thenReturn(new StructureTransform(0, 0, 0, 0));
        when(structure.collisionGroups()).thenReturn(List.of(box("rigging", 1), solid));
        when(structure.solidCollisionGroups()).thenReturn(List.of(solid));
        StructureCollisionRegistry.register(structure);
        registered.add(structure);
        return structure;
    }

    private static CollisionGroup box(String name, double z) {
        return CollisionGroup.fixed(name, new CollisionShape(Vec3.ZERO,
                List.of(CollisionPart.axisAligned(new AABB(-.5, 1, z, .5, 3, z + .1)))));
    }

    @Test void picksSolidGeometryInsteadOfDecorativeRigging() {
        var structure = structure(false);
        var target = ConstructionTarget.pick(player, 4, 1, false);
        assertNotNull(target);
        assertSame(structure, target.structure());
        assertEquals(3, target.location().z, .001);
    }

    @Test void blocksOccludeTheDeckAndReachIsRespected() {
        structure(false);
        var block = new BlockHitResult(EYE.add(0, 0, 2), Direction.NORTH, new BlockPos(0, 2, 2), false);
        when(player.pick(anyDouble(), anyFloat(), anyBoolean())).thenReturn(block);
        assertSame(block, ConstructionTarget.pick(player, 4, 1, false).block());
        when(player.pick(anyDouble(), anyFloat(), anyBoolean())).thenReturn(
                BlockHitResult.miss(EYE.add(0, 0, 2), Direction.NORTH, new BlockPos(0, 2, 2)));
        assertNull(ConstructionTarget.pick(player, 2, 1, false));
    }

    @Test void unfinishedStructuresAreNotDecksAndFluidPlacementStaysBlockOnly() {
        structure(true);
        assertNull(ConstructionTarget.pick(player, 4, 1, false));
        structure(false);
        assertNull(ConstructionTarget.pick(player, 4, 1, true));
    }

    @Test void rejectsInvalidTargetsAndInvalidReach() {
        assertThrows(IllegalArgumentException.class, () -> new ConstructionTarget(null, null, Vec3.ZERO));
        assertThrows(IllegalArgumentException.class, () -> ConstructionTarget.on(structure(false), new Vec3(Double.NaN, 0, 0)));
        assertNull(ConstructionTarget.pick(player, Double.POSITIVE_INFINITY, 1, false));
        assertNull(ConstructionTarget.pick(player, -1, 1, false));
    }
}
