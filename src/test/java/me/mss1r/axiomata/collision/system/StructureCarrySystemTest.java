package me.mss1r.axiomata.collision.system;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import me.mss1r.axiomata.collision.*;
import me.mss1r.axiomata.blueprint.api.construction.BuildProgress;
import me.mss1r.axiomata.blueprint.api.construction.UnderConstruction;
import net.minecraft.resources.ResourceLocation;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.profiling.InactiveProfiler;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;

import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

class StructureCarrySystemTest {
    private Level level;
    private final List<TestEntity> entities = new ArrayList<>();
    private static final CollisionShape DECK = shape(new AABB(-5, -.25, -5, 5, 0, 5));
    private static final CollisionShape CARGO = shape(new AABB(-.5, 0, -.5, .5, 1, .5));

    @BeforeEach
    void world() {
        level = mock(Level.class);
        when(level.getBlockCollisions(any(), any())).thenReturn(List.of());
        when(level.getEntityCollisions(any(), any())).thenReturn(List.of());
        when(level.getBlockState(any())).thenReturn(Blocks.AIR.defaultBlockState());
        when(level.getWorldBorder()).thenReturn(new WorldBorder());
        when(level.getProfiler()).thenReturn(InactiveProfiler.INSTANCE);
        when(level.getEntities(
                        any(Entity.class),
                        any(AABB.class),
                        org.mockito.ArgumentMatchers.<Predicate<Entity>>any()))
                .thenReturn(List.of());
    }

    @AfterEach
    void cleanup() {
        entities.forEach(StructureCollisionRegistry::unregister);
    }

    private static CollisionShape shape(AABB box) {
        return new CollisionShape(Vec3.ZERO, List.of(CollisionPart.axisAligned(box)));
    }

    private TestEntity entity(CollisionShape shape, double x, double y, double z) {
        TestEntity entity = new TestEntity(level, shape);
        entity.setPos(x, y, z);
        entity.previous = entity.collisionTransform();
        entities.add(entity);
        StructureMotionSystem.tickStructure(entity);
        return entity;
    }

    @Test
    void carriesCargoWhoseOriginIsOutsideTheParentsVanillaBox() {
        TestEntity parent = entity(DECK, 0, 0, 0);
        TestEntity child = entity(CARGO, 3, 0, 0);
        parent.setPos(1, 0, 0);
        StructureMotionSystem.tickStructure(parent);
        assertEquals(4, child.getX(), .001);
        assertEquals(0, child.getY(), .001);
        assertTrue(child.onGround());
        assertFalse(child.isPassenger());
    }

    @Test
    void carriesCargoVertically() {
        TestEntity parent = entity(DECK, 0, 0, 0);
        TestEntity child = entity(CARGO, 0, 0, 0);
        parent.setPos(0, 1, 0);
        StructureMotionSystem.tickStructure(parent);
        assertEquals(1, child.getY(), .001);
    }

    @Test
    void contactOnlyWheelsBelowAnElevatedMechanismAreFoundByAFlatPlatform() {
        TestEntity platform = entity(DECK, 0, 0, 0);
        TestEntity engine = new TestEntity(level, shape(new AABB(-.5, .53, -.5, .5, 2, .5))) {
            @Override public List<CollisionGroup> supportContactGroups() {
                return List.of(CollisionGroup.fixed("footing", shape(new AABB(-.5, 0, -.5, .5, .01, .5))));
            }
        };
        entities.add(engine);
        engine.setPos(2, 0, 0);
        StructureMotionSystem.tickStructure(engine);
        platform.setPos(1, 0, 0);
        StructureMotionSystem.tickStructure(platform);
        assertEquals(3, engine.getX(), .001);
        assertEquals(0, engine.getY(), .001);
    }

    private Site site(double x, double y, double z) {
        Site site = new Site(level);
        site.setPos(x, y, z);
        entities.add(site);
        StructureMotionSystem.tickStructure(site);
        return site;
    }

    @Test
    void aBareConstructionSiteFollowsTranslationAndRotationWithoutBecomingSolid() {
        TestEntity parent = entity(DECK, 0, 0, 0);
        Site site = site(2, 0, 0);
        assertTrue(StructureGeometry.solid(site).isEmpty());
        parent.setPos(1, 1, 0);
        parent.setYRot(90);
        StructureMotionSystem.tickStructure(parent);
        assertEquals(1, site.getX(), .001);
        assertEquals(1, site.getY(), .001);
        assertEquals(2, site.getZ(), .001);
        assertEquals(90, site.getYRot(), .001);
        assertTrue(StructureGeometry.solid(site).isEmpty(), "A carried site must not create invisible walls");
        StructureMotionSystem.tickStructure(site);
        StructureMotionSystem.tickStructure(parent);
        assertEquals(2, site.getZ(), .001, "No double carry in the site's own tick");
    }

    @Test
    void aMotionlessSiteIsIndexedOnLevelAdditionAndUnindexedOnUnload() {
        Site site = new Site(level);
        entities.add(site);
        site.onAddedToLevel();
        assertTrue(StructureCollisionRegistry.snapshot().stream().anyMatch(found -> found == site));
        site.onRemovedFromLevel();
        assertFalse(StructureCollisionRegistry.snapshot().stream().anyMatch(found -> found == site));
    }

    @Test
    void finishingAConstructionSiteDoesNotLoseItsSupport() {
        TestEntity parent = entity(DECK, 0, 0, 0);
        Site site = site(2, 0, 0);
        parent.setPos(1, 0, 0);
        StructureMotionSystem.tickStructure(parent);
        site.finished = true;
        StructureCollisionSystem.register(site);
        parent.setPos(2, 0, 0);
        StructureMotionSystem.tickStructure(parent);
        assertEquals(4, site.getX(), .001);
        assertFalse(StructureGeometry.solid(site).isEmpty());
    }

    @Test
    void aFreshlyLoadedSiteReacquiresSupportWithoutSavedEntityIds() {
        TestEntity parent = entity(DECK, 0, 0, 0);
        Site first = site(2, 0, 0);
        parent.setPos(1, 0, 0);
        parent.setYRot(30);
        StructureMotionSystem.tickStructure(parent);
        CompoundTag tag = first.saveWithoutId(new CompoundTag());
        first.discard();
        Site loaded = site(0, 0, 0);
        loaded.load(tag);
        StructureCollisionSystem.register(loaded);
        Vec3 expected = loaded.position().add(1, 0, 0);
        parent.setPos(2, 0, 0);
        StructureMotionSystem.tickStructure(parent);
        assertEquals(expected.x, loaded.getX(), .001);
        assertEquals(expected.z, loaded.getZ(), .001);
        assertEquals(30, loaded.getYRot(), .001);
    }

    @Test
    void aBareSiteOverADeckOpeningIsNotCarried() {
        var sides = new CollisionShape(Vec3.ZERO, List.of(
                CollisionPart.axisAligned(new AABB(-5, -.25, -5, -1, 0, 5)),
                CollisionPart.axisAligned(new AABB(1, -.25, -5, 5, 0, 5))));
        TestEntity parent = entity(sides, 0, 0, 0);
        Site site = site(0, 0, 0);
        parent.setPos(0, 0, 1);
        StructureMotionSystem.tickStructure(parent);
        assertEquals(0, site.getZ(), .001);
    }

    @Test
    void aBareSiteCanBeCarriedAsPartOfAStackWithoutMakingTheStackSolid() {
        TestEntity parent = entity(DECK, 0, 0, 0);
        TestEntity middle = entity(CARGO, 2, 0, 0);
        Site site = site(2, 1, 0);
        parent.setYRot(90);
        StructureMotionSystem.tickStructure(parent);
        assertEquals(0, site.getX(), .001);
        assertEquals(2, site.getZ(), .001);
        assertEquals(1, site.getY(), .001);
        StructureMotionSystem.tickStructure(middle);
        assertEquals(2, site.getZ(), .001);
        assertTrue(StructureGeometry.solid(site).isEmpty());
    }

    @Test
    void placementRespectsPlannedSitesAndRealBlockGeometry() {
        TestEntity candidate = entity(CARGO, 0, 0, 0);
        assertTrue(StructurePlacement.isClear(candidate, candidate.collisionTransform()));
        Site site = site(0, 0, 0);
        assertFalse(StructurePlacement.isClear(candidate, candidate.collisionTransform()), "Unbuilt sites reserve their space");
        site.discard();
        when(level.getBlockCollisions(any(), any())).thenReturn(
                List.of(Shapes.create(new AABB(.2, .2, .2, .4, .4, .4))));
        assertFalse(StructurePlacement.isClear(candidate, candidate.collisionTransform()));
        when(level.getBlockCollisions(any(), any())).thenReturn(
                List.of(Shapes.create(new AABB(2, .2, .2, 3, .4, .4))));
        assertTrue(StructurePlacement.isClear(candidate, candidate.collisionTransform()), "Nearby blocks outside the parts do not block placement");
    }

    @Test
    void sparseTallGeometryStillHitsAnObstacleAtMastHeight() {
        CollisionShape sparse =
                new CollisionShape(
                        Vec3.ZERO,
                        List.of(
                                CollisionPart.axisAligned(new AABB(-10, -.25, -10, 10, 0, 10)),
                                CollisionPart.axisAligned(new AABB(-.1, 0, -.1, .1, 30, .1))));
        TestEntity ship = entity(sparse, 0, 0, 0);
        AABB obstacle = new AABB(.6, 15, -.5, 1.6, 16, .5);
        double[] queriedVolume = {0};
        when(level.getBlockCollisions(any(), any()))
                .thenAnswer(
                        invocation -> {
                            AABB query = invocation.getArgument(1);
                            queriedVolume[0] +=
                                    query.getXsize() * query.getYsize() * query.getZsize();
                            return query.intersects(obstacle)
                                    ? List.of(Shapes.create(obstacle))
                                    : List.of();
                        });
        Vec3 allowed = StructureTerrainCollision.clampMotion(ship, new Vec3(2, 0, 0));
        assertEquals(.5, allowed.x, .001, "A narrow high mast must retain its terrain collision");
        assertTrue(queriedVolume[0] < 6000, "Do not traverse the mast-to-hull empty volume");
    }

    @Test
    void rotatesCargoAndDoesNotCarryTwiceWhenItsTickRunsLater() {
        TestEntity parent = entity(DECK, 0, 0, 0);
        TestEntity child = entity(CARGO, 2, 0, 0);
        parent.setYRot(90);
        StructureMotionSystem.tickStructure(parent);
        assertEquals(0, child.getX(), .001);
        assertEquals(2, child.getZ(), .001);
        assertEquals(90, child.getYRot(), .001);
        StructureMotionSystem.tickStructure(child);
        StructureMotionSystem.tickStructure(parent);
        assertEquals(2, child.getZ(), .001);
        assertEquals(90, child.getYRot(), .001);
    }

    @Test
    void propagatesAcrossThreeLevelsWithoutRepeatingTheMiddleDelta() {
        TestEntity parent = entity(DECK, 0, 0, 0);
        TestEntity middle = entity(CARGO, 0, 0, 0);
        TestEntity top = entity(CARGO, 0, 1, 0);
        parent.setPos(1, 0, 0);
        StructureMotionSystem.tickStructure(parent);
        assertEquals(1, middle.getX(), .001);
        assertEquals(1, top.getX(), .001);
        StructureMotionSystem.tickStructure(middle);
        assertEquals(1, top.getX(), .001);
    }

    @Test
    void turnsAWholeStackAboutTheSupportOnce() {
        TestEntity parent = entity(DECK, 0, 0, 0);
        TestEntity middle = entity(CARGO, 2, 0, 0);
        TestEntity top = entity(CARGO, 2, 1, 0);
        parent.setYRot(90);
        StructureMotionSystem.tickStructure(parent);
        assertEquals(0, top.getX(), .001);
        assertEquals(1, top.getY(), .001);
        assertEquals(2, top.getZ(), .001);
        assertEquals(90, top.getYRot(), .001);
        StructureMotionSystem.tickStructure(middle);
        StructureMotionSystem.tickStructure(top);
        assertEquals(2, top.getZ(), .001);
        assertEquals(90, top.getYRot(), .001);
    }

    @Test
    void upwardVelocityDetachesCargo() {
        TestEntity parent = entity(DECK, 0, 0, 0);
        TestEntity child = entity(CARGO, 0, .01, 0);
        child.setDeltaMovement(0, .2, 0);
        parent.setPos(1, 0, 0);
        StructureMotionSystem.tickStructure(parent);
        assertEquals(0, child.getX(), .001);
    }

    @Test
    void liftsAStackInsteadOfTreatingCargoAsACeiling() {
        TestEntity parent = entity(DECK, 0, 0, 0);
        TestEntity middle = entity(CARGO, 0, 0, 0);
        TestEntity top = entity(CARGO, 0, 1, 0);
        Vec3 allowed = StructureTerrainCollision.clampMotion(parent, new Vec3(0, 1, 0));
        assertEquals(1, allowed.y, .001);
        parent.setPos(0, allowed.y, 0);
        StructureMotionSystem.tickStructure(parent);
        assertEquals(1, middle.getY(), .001);
        assertEquals(2, top.getY(), .001);
    }

    @Test
    void aCeilingAboveTheTopCargoConstrainsTheWholeStack() {
        TestEntity parent = entity(DECK, 0, 0, 0);
        TestEntity middle = entity(CARGO, 0, 0, 0);
        TestEntity top = entity(CARGO, 0, 1, 0);
        when(level.getBlockCollisions(any(), any()))
                .thenReturn(List.of(Shapes.create(new AABB(-2, 2.5, -2, 2, 3, 2))));
        Vec3 allowed = StructureTerrainCollision.clampMotion(parent, new Vec3(0, 2, 0));
        assertEquals(.5, allowed.y, .001);
        parent.setPos(0, allowed.y, 0);
        StructureMotionSystem.tickStructure(parent);
        assertEquals(.5, middle.getY(), .001);
        assertEquals(1.5, top.getY(), .001);
    }

    @Test
    void aGapInTheDeckDoesNotCarryCargo() {
        CollisionShape sides =
                new CollisionShape(
                        Vec3.ZERO,
                        List.of(
                                CollisionPart.axisAligned(new AABB(-5, -.25, -5, -1, 0, 5)),
                                CollisionPart.axisAligned(new AABB(1, -.25, -5, 5, 0, 5))));
        TestEntity parent = entity(sides, 0, 0, 0);
        TestEntity child = entity(CARGO, 0, 0, 0);
        parent.setPos(0, 0, 1);
        StructureMotionSystem.tickStructure(parent);
        assertEquals(0, child.getZ(), .001);
    }

    @Test
    void formerCargoCanBecomeTheSupportWithoutKeepingAStaleAttachment() {
        TestEntity parent = entity(DECK, 0, 0, 0);
        TestEntity formerCargo = entity(CARGO, 0, 0, 0);
        parent.setPos(.1, 0, 0);
        StructureMotionSystem.tickStructure(parent);
        formerCargo.setPos(0, -1, 0);
        parent.setPos(0, .25, 0);
        StructureMotionSystem.tickStructure(formerCargo);
        StructureMotionSystem.tickStructure(parent);
        formerCargo.setPos(1, -1, 0);
        StructureMotionSystem.tickStructure(formerCargo);
        assertEquals(1, parent.getX(), .001);
    }

    @Test
    void aCarriedEntityStopsAtWorldObstacles() {
        TestEntity parent = entity(DECK, 0, 0, 0);
        TestEntity child = entity(CARGO, 0, 0, 0);
        when(level.getBlockCollisions(any(), any()))
                .thenReturn(List.of(Shapes.create(new AABB(1.5, 0, -2, 2, 3, 2))));
        parent.setPos(3, 0, 0);
        StructureMotionSystem.tickStructure(parent);
        assertEquals(1, child.getX(), .001);
    }

    @Test
    void fullMotionUsesGeometryAgainstOtherStructures() {
        TestEntity wall = entity(shape(new AABB(0, -2, -2, .1, 2, 2)), 0, 0, 0);
        TestEntity child = entity(CARGO, -3, 0, 0);
        Vec3 allowed = StructureTerrainCollision.clampMotion(child, new Vec3(10, 0, 0));
        assertEquals(2.5, allowed.x, .001);
        assertNotNull(wall);
    }

    @Test
    void geometryOutsideTheRootBoxAlsoCollidesWithBlocks() {
        TestEntity child = entity(shape(new AABB(3, 0, -.5, 4, 1, .5)), 0, 0, 0);
        when(level.getBlockCollisions(any(), any()))
                .thenReturn(List.of(Shapes.create(new AABB(5, 0, -2, 6, 3, 2))));
        assertEquals(1, StructureTerrainCollision.clampMotion(child, new Vec3(10, 0, 0)).x, .001);
    }

    @Test
    void entityMoveUsesTheGeometrySolverThroughTheMixin() {
        entity(shape(new AABB(0, -2, -2, .1, 2, 2)), 0, 0, 0);
        TestEntity child = entity(CARGO, -3, 0, 0);
        child.move(MoverType.SELF, new Vec3(10, 0, 0));
        assertEquals(-.5, child.getX(), .001);
    }

    @Test
    void geometryMovementRetainsTheConsumersStepHeight() {
        TestEntity child = entity(CARGO, -.75, 0, 0);
        child.stepHeight = .6F;
        child.setOnGround(true);
        when(level.getBlockCollisions(any(), any()))
                .thenReturn(List.of(Shapes.create(new AABB(.5, 0, -2, 1.5, .5, 2))));
        child.move(MoverType.SELF, new Vec3(2, 0, 0));
        assertEquals(1.25, child.getX(), .001);
        assertEquals(.5, child.getY(), .001);
    }

    @Test
    void aCeilingPreventsStepping() {
        TestEntity child = entity(CARGO, -.75, 0, 0);
        child.stepHeight = .6F;
        child.setOnGround(true);
        when(level.getBlockCollisions(any(), any()))
                .thenReturn(
                        List.of(
                                Shapes.create(new AABB(.5, 0, -2, 1.5, .5, 2)),
                                Shapes.create(new AABB(-2, 1.2, -2, 3, 2, 2))));
        child.move(MoverType.SELF, new Vec3(2, 0, 0));
        assertEquals(0, child.getX(), .001);
        assertEquals(0, child.getY(), .001);
    }

    @Test
    void geometryDoesNotBypassTheWorldBorder() {
        TestEntity child = entity(CARGO, 3, 0, 0);
        level.getWorldBorder().setSize(8);
        assertEquals(.5, StructureTerrainCollision.clampMotion(child, new Vec3(5, 0, 0)).x, .001);
    }

    @Test
    void rotationChecksTheArcNotOnlyTheFinalPose() {
        TestEntity beam = entity(shape(new AABB(-4, 0, -.05, 4, 1, .05)), 0, 0, 0);
        when(level.getBlockCollisions(any(), any()))
                .thenReturn(List.of(Shapes.create(new AABB(2.75, 0, 2.75, 2.85, 1, 2.85))));
        float yaw = StructureTerrainCollision.clampYaw(beam, 90);
        assertTrue(
                yaw > 1 && yaw < 80,
                "An intermediate obstacle blocks an otherwise clear final heading");
        assertEquals(0, beam.getYRot(), 1e-7, "Clamping is a query and must not rotate the entity");
    }

    @Test
    void anOrdinaryEntityCanStandOnCargoAndFollowTheStack() {
        TestEntity parent = entity(DECK, 0, 0, 0);
        TestEntity cargo = entity(CARGO, 0, 0, 0);
        Entity walker =
                new Entity(EntityType.ARMOR_STAND, level) {
                    @Override
                    protected void defineSynchedData(SynchedEntityData.Builder builder) {}

                    @Override
                    protected void readAdditionalSaveData(CompoundTag tag) {}

                    @Override
                    protected void addAdditionalSaveData(CompoundTag tag) {}

                    @Override
                    protected MovementEmission getMovementEmission() {
                        return MovementEmission.NONE;
                    }
                };
        walker.setPos(0, 1, 0);
        walker.move(MoverType.SELF, new Vec3(0, -.1, 0));
        assertEquals(1, walker.getY(), .001);
        assertTrue(walker.onGround());
        when(level.getEntities(
                        any(Entity.class),
                        any(AABB.class),
                        org.mockito.ArgumentMatchers.<Predicate<Entity>>any()))
                .thenAnswer(
                        call -> {
                            Predicate<Entity> filter = call.getArgument(2);
                            return filter.test(walker) ? List.of(walker) : List.of();
                        });
        parent.setPos(1, 0, 0);
        StructureMotionSystem.tickStructure(parent);
        StructureMotionSystem.tickStructure(cargo);
        assertEquals(1, walker.getX(), .001);
        assertEquals(1, walker.getY(), .001);
    }

    @Test
    void matchingNetworkIdsInDifferentWorldsDoNotShareMotionHistory() throws Exception {
        TestEntity server = entity(DECK, 0, 0, 0);
        Level clientLevel = mock(Level.class);
        var clientSide = Level.class.getField("isClientSide");
        clientSide.setAccessible(true);
        clientSide.setBoolean(clientLevel, true);
        when(clientLevel.getEntities(
                        any(Entity.class),
                        any(AABB.class),
                        org.mockito.ArgumentMatchers.<Predicate<Entity>>any()))
                .thenReturn(List.of());
        TestEntity client = new TestEntity(clientLevel, DECK);
        client.setId(server.getId());
        client.setPos(.05, 0, 0);
        client.previous = client.collisionTransform();
        entities.add(client);
        assertEquals(server, client, "Minecraft compares Entity network IDs, not object identity");

        Entity walker =
                new Entity(EntityType.ARMOR_STAND, level) {
                    @Override
                    protected void defineSynchedData(SynchedEntityData.Builder builder) {}

                    @Override
                    protected void readAdditionalSaveData(CompoundTag tag) {}

                    @Override
                    protected void addAdditionalSaveData(CompoundTag tag) {}
                };
        walker.setPos(0, .0002, 0);
        when(level.getEntities(
                        any(Entity.class),
                        any(AABB.class),
                        org.mockito.ArgumentMatchers.<Predicate<Entity>>any()))
                .thenAnswer(
                        call -> {
                            Predicate<Entity> filter = call.getArgument(2);
                            return filter.test(walker) ? List.of(walker) : List.of();
                        });
        for (int tick = 0; tick < 20; tick++) {
            StructureMotionSystem.tickStructure(client);
            StructureMotionSystem.tickStructure(server);
            assertEquals(
                    0,
                    walker.getX(),
                    1e-6,
                    "A stationary deck must not replay the other world's interpolation delta");
        }
    }

    private Entity walker() {
        Entity walker =
                new Entity(EntityType.ARMOR_STAND, level) {
                    @Override
                    protected void defineSynchedData(SynchedEntityData.Builder builder) {}

                    @Override
                    protected void readAdditionalSaveData(CompoundTag tag) {}

                    @Override
                    protected void addAdditionalSaveData(CompoundTag tag) {}

                    @Override
                    protected MovementEmission getMovementEmission() {
                        return MovementEmission.NONE;
                    }

                    @Override
                    public float maxUpStep() {
                        return .6F;
                    }
                };
        walker.setPos(-.7, 0, 0);
        walker.setBoundingBox(new AABB(-1, 0, -.3, -.4, 1.8, .3));
        walker.setOnGround(true);
        return walker;
    }

    @Test
    void ordinaryWalkerStepsOntoSmallGeometryLedges() {
        entity(DECK, 0, 0, 0);
        for (double height : new double[] {.0625, .25, .5}) {
            TestEntity ledge = entity(shape(new AABB(0, 0, -2, 2, height, 2)), 0, 0, 0);
            Entity walker = walker();
            walker.move(MoverType.SELF, new Vec3(.8, -.08, 0));
            assertEquals(.1, walker.getX(), .001, "step " + height);
            assertEquals(height, walker.getY(), .001, "step " + height);
            assertTrue(walker.onGround());
            StructureCollisionRegistry.unregister(ledge);
        }
    }

    @Test
    void ordinaryWalkerCannotStepOverTallWallsOrThroughCeilings() {
        entity(DECK, 0, 0, 0);
        TestEntity tall = entity(shape(new AABB(0, 0, -2, 2, .75, 2)), 0, 0, 0);
        Entity walker = walker();
        walker.move(MoverType.SELF, new Vec3(.8, -.08, 0));
        assertEquals(-.3, walker.getX(), .001);
        assertEquals(0, walker.getY(), .001);
        StructureCollisionRegistry.unregister(tall);
        entity(shape(new AABB(0, 0, -2, 2, .5, 2)), 0, 0, 0);
        when(level.getBlockCollisions(any(), any()))
                .thenReturn(List.of(Shapes.create(new AABB(-2, 2, -2, 3, 3, 2))));
        walker = walker();
        walker.move(MoverType.SELF, new Vec3(.8, -.08, 0));
        assertEquals(-.3, walker.getX(), .001);
        assertEquals(0, walker.getY(), .001);
    }

    @Test
    void anAirborneWalkerDoesNotGainAnExtraStep() {
        entity(shape(new AABB(0, -1, -2, 2, .5, 2)), 0, 0, 0);
        Entity walker = walker();
        walker.setOnGround(false);
        walker.move(MoverType.SELF, new Vec3(.8, -.08, 0));
        assertEquals(-.3, walker.getX(), .001);
        assertEquals(-.08, walker.getY(), .001);
    }

    @Test
    void ordinaryStepChecksRotatedGeometryAndStructureCeilings() {
        entity(DECK, 0, 0, 0);
        TestEntity ledge = entity(shape(new AABB(0, 0, -2, 2, .25, 2)), 0, 0, 0);
        ledge.setYRot(10);
        StructureCollisionSystem.register(ledge);
        Entity walker = walker();
        walker.move(MoverType.SELF, new Vec3(.8, -.08, 0));
        assertEquals(.1, walker.getX(), .001);
        assertEquals(.25, walker.getY(), .001);
        entity(shape(new AABB(-2, 1.9, -2, 3, 3, 2)), 0, 0, 0);
        walker = walker();
        walker.move(MoverType.SELF, new Vec3(.8, -.08, 0));
        assertTrue(walker.getX() < 0, "A geometry ceiling prevents stepping too");
        assertEquals(0, walker.getY(), .001);
    }

    @Test
    void vanillaMovementIsUntouchedAwayFromStructures() {
        Entity walker = walker();
        Vec3 vanilla = new Vec3(.2, .4, .1);
        assertEquals(
                vanilla,
                StructureCollisionSystem.collideMovement(walker, new Vec3(.8, 0, .1), vanilla)
                        .allowed());
    }

    private static class TestEntity extends Entity implements CollidableStructure {
        private final List<CollisionGroup> groups;
        private StructureTransform previous;
        private float stepHeight;

        TestEntity(Level level, CollisionShape shape) {
            super(EntityType.ARMOR_STAND, level);
            groups = List.of(CollisionGroup.fixed("solid", shape));
        }

        @Override
        public List<CollisionGroup> collisionGroups() {
            return groups;
        }

        @Override
        public StructureTransform collisionTransform() {
            return new StructureTransform(getX(), getY(), getZ(), getYRot());
        }

        @Override
        public StructureTransform previousCollisionTransform() {
            return previous == null ? collisionTransform() : previous;
        }

        @Override
        protected void defineSynchedData(SynchedEntityData.Builder builder) {}

        @Override
        protected void readAdditionalSaveData(CompoundTag tag) {}

        @Override
        protected void addAdditionalSaveData(CompoundTag tag) {}

        @Override
        protected MovementEmission getMovementEmission() {
            return MovementEmission.NONE;
        }

        @Override
        public float maxUpStep() {
            return stepHeight;
        }
    }

    private static final class Site extends TestEntity implements UnderConstruction {
        private final BuildProgress progress = BuildProgress.finished();
        private boolean finished;

        Site(Level level) {
            super(level, CARGO);
        }

        @Override public BuildProgress buildProgress() { return progress; }
        @Override public boolean isFullyBuilt() { return finished; }
        @Override public ResourceLocation constructionModel() { return null; }
    }
}
