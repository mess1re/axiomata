package me.mss1r.axiomata.tests.fabric;

import com.mojang.authlib.GameProfile;
import eu.pb4.common.protection.api.CommonProtection;
import eu.pb4.common.protection.api.ProtectionProvider;
import me.mss1r.axiomata.ResourceIds;
import me.mss1r.axiomata.ballistics.ProjectilePassThroughControl;
import me.mss1r.axiomata.ballistics.ProtectedBlockAccess;
import me.mss1r.axiomata.blueprint.internal.definition.BlueprintDefinitionRepository;
import me.mss1r.axiomata.blueprint.tracing.OutlineRepository;
import me.mss1r.axiomata.collision.*;
import me.mss1r.axiomata.collision.system.StructureMotionSystem;
import me.mss1r.axiomata.platform.AxiomataPlatform;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.*;

public final class FabricRuntimeTests implements ModInitializer, FabricGameTest {
    private static EntityType<Frame> frameType;
    private static final Set<BlockPos> PROTECTED = new HashSet<>();
    private static final Set<BlockPos> CANCEL_BREAK = new HashSet<>();
    private static final Map<BlockPos, UUID> PLACEMENT_OWNERS = new HashMap<>();
    private static final ProtectedBlockAccess POLICY = new ProtectedBlockAccess(() -> ProtectedBlockAccess.Policy.RESPECT_PROTECTION);

    @Override public void onInitialize() {
        frameType = Registry.register(BuiltInRegistries.ENTITY_TYPE,
                ResourceIds.id("axiomata_fabric_tests", "frame"),
                EntityType.Builder.<Frame>of(Frame::new, MobCategory.MISC).sized(.5F, 1F)
                    .build("axiomata_fabric_tests:frame"));
        CommonProtection.register(ResourceIds.id("axiomata_fabric_tests", "protection"), new ProtectionProvider() {
            @Override public boolean isProtected(Level level, BlockPos pos) { return PROTECTED.contains(pos); }
            @Override public boolean isAreaProtected(Level level, AABB area) {
                return PROTECTED.stream().anyMatch(pos -> area.contains(Vec3.atCenterOf(pos)));
            }
            @Override public boolean canPlaceBlock(Level level, BlockPos pos, GameProfile profile, Player player) {
                PLACEMENT_OWNERS.put(pos.immutable(), profile.getId());
                return !PROTECTED.contains(pos);
            }
        });
        PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, blockEntity) -> !CANCEL_BREAK.contains(pos));
    }

    private static ServerPlayer player(GameTestHelper helper) {
        return (ServerPlayer) AxiomataPlatform.fakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "AxiomataTest"));
    }
    private static Frame frame(GameTestHelper helper, int x, double y, int z) {
        Frame frame = frameType.create(helper.getLevel());
        BlockPos pos = helper.absolutePos(new BlockPos(x, (int)y, z));
        frame.setPos(pos.getX() + .5D, pos.getY() + y - (int)y, pos.getZ() + .5D);
        frame.previous = frame.collisionTransform();
        helper.getLevel().addFreshEntity(frame);
        return frame;
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void resourceReloadLoadsDrawableBlueprints(GameTestHelper helper) {
        String id = "axiomata_fabric_tests:rope";
        BlueprintDefinitionRepository.validate(helper.getLevel().getServer());
        var definition = BlueprintDefinitionRepository.get(id);
        helper.assertTrue(definition != null, "The Fabric reload listener must load the test mod's blueprint");
        helper.assertTrue(OutlineRepository.isDrawable(id) && OutlineRepository.get(id) != null,
                "The outline listener must make the blueprint drawable");
        helper.assertTrue(BlueprintDefinitionRepository.toNetworkJson().contains(id),
                "The outgoing catalog must include the loaded blueprint");
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void persistentDataSurvivesEntityNbtAndDoesNotAlias(GameTestHelper helper) {
        var entity = new FallingBlockEntity(EntityType.FALLING_BLOCK, helper.getLevel());
        AxiomataPlatform.persistentData(entity).putString("artillery", "owner-and-context");
        CompoundTag saved = entity.saveWithoutId(new CompoundTag());
        AxiomataPlatform.persistentData(entity).putString("artillery", "changed-after-save");
        var restored = new FallingBlockEntity(EntityType.FALLING_BLOCK, helper.getLevel());
        restored.load(saved);
        helper.assertTrue("owner-and-context".equals(AxiomataPlatform.persistentData(restored).getString("artillery")), "Entity state must survive NBT without aliasing");
        restored.load(new CompoundTag());
        helper.assertTrue(AxiomataPlatform.persistentData(restored).isEmpty(), "Loading absent state must clear old markers");
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void deniedPlacementLeavesTheWorldUnchanged(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        helper.getLevel().setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        var actor = player(helper);
        PROTECTED.add(pos);
        try {
            helper.assertTrue(!POLICY.placeBlock(helper.getLevel(), pos, Blocks.STONE.defaultBlockState(), actor), "Protection must deny placement");
            helper.assertTrue(helper.getLevel().isEmptyBlock(pos), "Denied placement must not change the world");
        } finally { PROTECTED.remove(pos); }
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void deniedBreakHonorsFabricCallbacks(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        helper.getLevel().setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
        CANCEL_BREAK.add(pos);
        try {
            helper.assertTrue(!POLICY.breakBlock(helper.getLevel(), pos, player(helper)), "Fabric cancellation must be respected");
            helper.assertTrue(helper.getLevel().getBlockState(pos).is(Blocks.STONE), "Canceled block must remain");
        } finally { CANCEL_BREAK.remove(pos); }
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void protectedBreakAndAttributedPlacement(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        helper.getLevel().setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
        var actor = player(helper);
        PROTECTED.add(pos);
        try {
            helper.assertTrue(!POLICY.breakBlock(helper.getLevel(), pos, actor), "Provider must deny breaking");
        } finally { PROTECTED.remove(pos); }
        helper.assertTrue(POLICY.breakBlock(helper.getLevel(), pos, actor), "Allowed block must break");
        helper.assertTrue(POLICY.placeBlock(helper.getLevel(), pos, Blocks.STONE.defaultBlockState(), actor), "Allowed block must be placed");
        helper.assertTrue(actor.getUUID().equals(PLACEMENT_OWNERS.remove(pos)), "Placement must retain the player's UUID");
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void lifecycleIndexesAndRemovesStructures(GameTestHelper helper) {
        var frame = frame(helper, 3, 2, 3);
        helper.assertTrue(StructureCollisionRegistry.snapshot().contains(frame), "Successful entity addition must index geometry");
        frame.discard();
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(!StructureCollisionRegistry.snapshot().contains(frame), "Removed entity must leave the index");
            helper.succeed();
        });
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void supportedCargoFollowsTranslation(GameTestHelper helper) {
        var deck = frame(helper, 3, 2, 3);
        var cargo = frame(helper, 3, 2.5D, 3);
        cargo.groups = List.of(CollisionGroup.fixed("cargo", new CollisionShape(Vec3.ZERO,
                List.of(CollisionPart.axisAligned(new AABB(-.25, 0, -.25, .25, .5, .25))))));
        StructureMotionSystem.tickStructure(deck);
        StructureMotionSystem.tickStructure(cargo);
        double before = cargo.getX();
        helper.runAfterDelay(2, () -> {
            deck.previous = deck.collisionTransform();
            deck.setPos(deck.getX() + 1, deck.getY(), deck.getZ());
            StructureMotionSystem.tickStructure(deck);
            helper.assertTrue(Math.abs(cargo.getX() - before - 1) < .001, "Supported cargo must follow the deck once");
            helper.succeed();
        });
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void projectilesSkipAnOpeningAndHitTheNextEntity(GameTestHelper helper) {
        var opening = frame(helper, 2, 2, 3);
        var solid = frame(helper, 5, 2, 3);
        opening.open = true;
        var start = opening.position().add(-1, .5, 0);
        var end = solid.position().add(1, .5, 0);
        var arrow = new Arrow(EntityType.ARROW, helper.getLevel());
        var hit = ProjectileUtil.getEntityHitResult(helper.getLevel(), arrow, start, end, new AABB(start, end).inflate(1), e -> e == opening || e == solid, 0);
        helper.assertTrue(hit != null && hit.getEntity() == solid, "An opening must not consume the ray or hide the next hit");
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void openingsRemainPickableForPlayers(GameTestHelper helper) {
        var opening = frame(helper, 2, 2, 3);
        opening.open = true;
        var start = opening.position().add(-1, .5, 0);
        var end = opening.position().add(1, .5, 0);
        var hit = ProjectileUtil.getEntityHitResult(helper.getLevel(), player(helper), start, end, new AABB(start, end).inflate(1), e -> e == opening, 0);
        helper.assertTrue(hit != null && hit.getEntity() == opening, "Projectile openings must not disable ordinary interaction");
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void serverAcceptsInteractionAtTheModelSurface(GameTestHelper helper) {
        var target = frame(helper, 8, 2, 3);
        target.groups = List.of(CollisionGroup.fixed("near_surface", new CollisionShape(Vec3.ZERO,
                List.of(CollisionPart.axisAligned(new AABB(-7, 0, -1, -6, 2, 1))))));
        StructureMotionSystem.tickStructure(target);
        var actor = player(helper);
        Vec3 position = target.position().add(-8, 0, 0);
        actor.setPos(position.x, position.y, position.z);
        actor.setYRot(-90); actor.setXRot(0);
        actor.connection.handleInteract(ServerboundInteractPacket.createInteractionPacket(target, false, InteractionHand.MAIN_HAND));
        helper.assertTrue(target.interactions == 1, "Server must accept a nearby surface when the root box is too far away");
        helper.succeed();
    }

    public static final class Frame extends Entity implements CollidableStructure, ProjectilePassThroughControl {
        private StructureTransform previous;
        private boolean open;
        private int interactions;
        private List<CollisionGroup> groups = List.of(CollisionGroup.fixed("deck", new CollisionShape(Vec3.ZERO,
                List.of(CollisionPart.axisAligned(new AABB(-2, 0, -2, 2, .5, 2))))));
        Frame(EntityType<Frame> type, Level level) { super(type, level); setNoGravity(true); }
        @Override public List<CollisionGroup> collisionGroups() { return groups == null ? List.of() : groups; }
        @Override public StructureTransform collisionTransform() { return new StructureTransform(getX(), getY(), getZ(), getYRot()); }
        @Override public StructureTransform previousCollisionTransform() { return previous == null ? collisionTransform() : previous; }
        @Override public boolean allowsProjectilePassage(Vec3 start, Vec3 end) { return open; }
        @Override public boolean isPickable() { return true; }
        @Override public InteractionResult interact(Player player, InteractionHand hand) { interactions++; return InteractionResult.SUCCESS; }
        //? if >=1.21 {
        @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {}
        //?} else {
        /*@Override protected void defineSynchedData() {}
        *///?}
        @Override protected void readAdditionalSaveData(CompoundTag tag) {}
        @Override protected void addAdditionalSaveData(CompoundTag tag) {}
    }
}
