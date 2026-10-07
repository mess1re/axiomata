package me.mss1r.axiomata.ballistics;

import me.mss1r.axiomata.ballistics.profile.ProjectilePhysicsProfile;
import me.mss1r.axiomata.data.profile.ProfileCatalog;
import me.mss1r.axiomata.collision.CollidableStructure;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public abstract class BallisticProjectile extends ThrowableItemProjectile {
    private static final double RENDER_DISTANCE = 160.0D;

    private static final String TAG_BASE_DAMAGE = "BaseDamage";
    private static final String TAG_IN_GROUND = "InGround";
    private static final String TAG_SHAKE_TIME = "ShakeTime";
    private static final String TAG_PHYSICS_PROFILE = "PhysicsProfile";
    private static final String TAG_BLOCK_AUTHORITY = "BlockAuthority";
    private static final String TAG_RESPONSIBLE_PLAYER = "ResponsiblePlayer";
    private static final double TICKS_PER_SECOND = 20.0D;
    /** Per-tick velocity multiplier vanilla applies to thrown projectiles in air. */
    private static final double VANILLA_AIR_RETENTION = 0.99D;
    /** Ticks a client extrapolates past the latest server position before waiting for the next one. */
    private static final int MAX_TICKS_AHEAD = 3;
    /**
     * Exact launch velocity. The spawn packet clamps velocity to 3.9 blocks/tick, and a cannon ball is about five times
     * faster.
     */
    private static final EntityDataAccessor<Vector3f> LAUNCH_VELOCITY =
            SynchedEntityData.defineId(BallisticProjectile.class, EntityDataSerializers.VECTOR3);

    private boolean applyingPredictedPhysics;
    private boolean hitEntityThisTick;
    private final Set<Integer> hitTargets = new HashSet<>();
    protected boolean inGround;
    protected int shakeTime;
    private double baseDamage = 2.0D;
    private SoundEvent hitSound = SoundEvents.ARROW_HIT;
    @Nullable
    private ResourceLocation physicsProfileId;
    // Set from the shooter while the superclass constructs, so these must not have initializers.
    private boolean blockAuthorityEnabled;
    @Nullable
    private UUID responsiblePlayerId;
    /** Game time of the last flight tick, used to detect shots the level stopped ticking (see DistantFlight). */
    private long lastFlownTick = Long.MIN_VALUE;
    /** Entry point into the body it is stuck in; the remaining energy opens the crater there. */
    @Nullable
    private Vec3 craterMouth;
    @Nullable
    private Vec3 craterFace;
    /** Client only: last server position, and how far the shot moved in the server tick before it. */
    @Nullable
    private Vec3 serverPosition;
    private Vec3 serverStep = Vec3.ZERO;
    private int serverPositionsThisTick;
    private int ticksAheadOfServer;

    public BallisticProjectile(EntityType<? extends BallisticProjectile> entityEntityType, Level level) {
        super(entityEntityType, level);
        moveItself();
    }

    @Override
    //? if forge {
    /*protected void defineSynchedData() {
        super.defineSynchedData();
        SynchedEntityData data = this.entityData;
    *///?} else {
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        var data = builder;
    //?}
        data.define(LAUNCH_VELOCITY, new Vector3f());
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distanceSquared) {
        double range = RENDER_DISTANCE * getViewScale();
        return distanceSquared < range * range;
    }

    public BallisticProjectile(EntityType<? extends BallisticProjectile> type, LivingEntity shooter, Level level) {
        super(type, shooter, level);
        moveItself();
    }

    /**
     * Shots move and collide on their own. A moving engine's arm or frame must not push or carry them, e.g. a slow
     * stone just released from a low-power throw.
     */
    private void moveItself() {
        noPhysics = true;
    }

    @Override
    protected Item getDefaultItem() {
        return Items.STONE;
    }

    // Vanilla gravity is skipped: {@link #flyThroughAir} applies drag and gravity after the move.
    //? if forge {
    /*@Override
    protected float getGravity() {
        return 0.0F;
    }
    *///?} else {
    @Override
    protected double getDefaultGravity() {
        return 0.0D;
    }
    //?}

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putDouble(TAG_BASE_DAMAGE, baseDamage);
        tag.putBoolean(TAG_IN_GROUND, inGround);
        tag.putInt(TAG_SHAKE_TIME, shakeTime);
        tag.putBoolean(blockAuthorityKey(), blockAuthorityEnabled);
        if (responsiblePlayerId != null) {
            tag.putUUID(TAG_RESPONSIBLE_PLAYER, responsiblePlayerId);
        }
        if (physicsProfileId != null) {
            tag.putString(TAG_PHYSICS_PROFILE, physicsProfileId.toString());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains(TAG_BASE_DAMAGE)) {
            baseDamage = tag.getDouble(TAG_BASE_DAMAGE);
        }
        if (tag.contains(TAG_IN_GROUND)) {
            inGround = tag.getBoolean(TAG_IN_GROUND);
        }
        if (tag.contains(TAG_SHAKE_TIME)) {
            shakeTime = tag.getInt(TAG_SHAKE_TIME);
        }
        blockAuthorityEnabled = tag.getBoolean(blockAuthorityKey());
        responsiblePlayerId = tag.hasUUID(TAG_RESPONSIBLE_PLAYER) ? tag.getUUID(TAG_RESPONSIBLE_PLAYER) : null;
        physicsProfileId = tag.contains(TAG_PHYSICS_PROFILE)
                ? ResourceLocation.tryParse(tag.getString(TAG_PHYSICS_PROFILE))
                : null;
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        // Ignore debris thrown from the hole this shot just made.
        if (target instanceof FallingBlockEntity || hasHitTarget(target) || !mayHitTarget(target)) {
            return false;
        }
        if (target instanceof CollidableStructure structure && !structure.collisionGroups().isEmpty()) {
            return true;
        }
        return super.canHitEntity(target);
    }

    @Override
    protected void onHit(HitResult hitResult) {
        if (hitResult.getType() == HitResult.Type.ENTITY) {
            hitEntityThisTick = true;
        }
        super.onHit(hitResult);
    }

    @Override
    protected void onHitEntity(EntityHitResult entityHitResult) {
        super.onHitEntity(entityHitResult);
        if (entityHitResult.getEntity().level().isClientSide()) return;
        Entity hitTarget = entityHitResult.getEntity();
        rememberHitTarget(hitTarget);
        if (livingTarget(hitTarget) != null) {
            damageTarget(hitTarget, (float) this.getBaseDamage());
        }
        setDeltaMovement(getDeltaMovement().scale(-0.9));
        setBaseDamage(getBaseDamage() * 0.9);
        if (getBaseDamage() <= 1) this.discard();
    }

    @Override
    public void tick() {
        Vec3 previousPos = position();
        Vec3 previousMovement = getDeltaMovement();
        hitEntityThisTick = false;

        if (this.level().isClientSide && !shouldPredictMotionOnClient()) {
            this.xo = getX();
            this.yo = getY();
            this.zo = getZ();
            super.baseTick();
            followServer();
            return;
        }

        applyingPredictedPhysics = true;
        try {
            super.tick();
            if (!this.inGround && !this.isRemoved()) {
                flyThroughAir();
            }
        } finally {
            applyingPredictedPhysics = false;
        }

        if (!this.inGround && !this.isRemoved() && this.level() instanceof ServerLevel serverLevel) {
            sweepMissedEntityHit(serverLevel, previousPos, position(), previousMovement);
        }
    }

    protected boolean shouldPredictMotionOnClient() {
        return false;
    }

    @Override
    //? if forge {
    /*public void lerpTo(double x, double y, double z, float yaw, float pitch, int interpolationSteps, boolean teleport) {
    *///?} else {
    public void lerpTo(double x, double y, double z, float yaw, float pitch, int interpolationSteps) {
    //?}
        Vec3 target = new Vec3(x, y, z);
        if (shouldPredictMotionOnClient()) {
            setPos(x, y, z);
        } else if (!target.equals(lerpTarget()) && !target.equals(position())) {
            // A rotation-only packet resends the current position or target.
            startFollowingServer();
            serverStep = target.subtract(serverPosition);
            serverPosition = target;
            serverPositionsThisTick++;
        }
        setRot(yaw, pitch);
    }

    private Vec3 lerpTarget() {
        return serverPosition != null ? serverPosition : position();
    }

    /** Starts at the spawn position with the exact launch velocity. */
    private void startFollowingServer() {
        if (serverPosition == null) {
            serverPosition = position();
            serverStep = new Vec3(entityData.get(LAUNCH_VELOCITY));
        }
    }

    //? if neoforge {
    @Override
    public double lerpTargetX() {
        return lerpTarget().x;
    }

    @Override
    public double lerpTargetY() {
        return lerpTarget().y;
    }

    @Override
    public double lerpTargetZ() {
        return lerpTarget().z;
    }
    //?}

    /**
     * Client: shows the shot where the server has it now, extrapolating from the latest server position by the last
     * server step for each elapsed tick. Server updates arrive unevenly (none one tick, two the next), so the shot
     * keeps moving through gaps and later positions correct it. A shot the server stopped has no velocity and stays
     * put.
     */
    private void followServer() {
        startFollowingServer();
        ticksAheadOfServer = Mth.clamp(ticksAheadOfServer + 1 - serverPositionsThisTick, 0, MAX_TICKS_AHEAD);
        serverPositionsThisTick = 0;
        Vec3 step = getDeltaMovement().lengthSqr() > 1.0E-8D ? serverStep : Vec3.ZERO;
        Vec3 shown = serverPosition.add(step.scale(ticksAheadOfServer));
        setPos(shown.x, shown.y, shown.z);
    }

    /**
     * Applies air drag from mass, diameter and drag coefficient, plus real gravity, instead of vanilla's flat 1% per
     * tick, which slows heavy balls far too much. In water, vanilla's drag still applies.
     */
    private void flyThroughAir() {
        if (level() instanceof ServerLevel serverLevel) {
            lastFlownTick = serverLevel.getGameTime();
            DistantFlight.track(this);
        }
        Vec3 movement = getDeltaMovement();
        if (isInWater()) {
            if (!isNoGravity()) {
                super.setDeltaMovement(movement.add(0.0D, -Ballistics.GRAVITY, 0.0D));
            }
            return;
        }
        ProjectilePhysicsProfile physics = getPhysicsProfile();
        double gravity = isNoGravity() ? 0.0D : Ballistics.GRAVITY;
        Ballistics.Flight air = new Ballistics.Flight(gravity, physics.airDrag(physics.diameterOf(this)),
                0.0D, 0);
        super.setDeltaMovement(air.afterMove(movement.scale(1.0D / VANILLA_AIR_RETENTION)));
    }

    /** Whether it flew on the given game tick. */
    public boolean flewOn(long gameTime) {
        return lastFlownTick == gameTime;
    }

    /** True while in the air, not stuck in or resting on anything. */
    public boolean isInFlight() {
        return !inGround && !isRemoved();
    }

    @Override
    public void setDeltaMovement(Vec3 deltaMovement) {
        super.setDeltaMovement(deltaMovement);
        if (!level().isClientSide && tickCount == 0) {
            entityData.set(LAUNCH_VELOCITY, deltaMovement.toVector3f());
        }
        if (!level().isClientSide && tickCount > 0 && !applyingPredictedPhysics) {
            markMotionDirty();
        }
    }

    protected void markMotionDirty() {
        this.hasImpulse = true;
    }

    protected boolean sweepMissedEntityHit(ServerLevel serverLevel, Vec3 previousPos, Vec3 currentPos, Vec3 previousMovement) {
        if (hitEntityThisTick || !shouldSupplementalEntitySweep()) {
            return false;
        }

        EntityHitResult hit = ProjectileSweep.findFirstEntityHit(serverLevel, this, previousPos, currentPos,
                previousMovement, this::canHitEntity, getSupplementalEntitySweepPadding());
        if (hit == null) {
            return false;
        }

        hitEntityThisTick = true;
        onHitEntity(hit);
        return true;
    }

    protected boolean shouldSupplementalEntitySweep() {
        return true;
    }

    protected double getSupplementalEntitySweepPadding() {
        return Math.max(0.35D, getBbWidth() * 0.5D);
    }

    @Override
    protected void onHitBlock(BlockHitResult blockHitResult) {
        super.onHitBlock(blockHitResult);
    }

    public double getBaseDamage() {
        return baseDamage;
    }

    public void setBaseDamage(double baseDamage) {
        this.baseDamage = baseDamage;
    }

    protected void setImpactSound(SoundEvent soundEvent) {
        this.hitSound = soundEvent;
    }

    protected SoundEvent getImpactSound() {
        return hitSound != null ? hitSound : SoundEvents.ANVIL_LAND;
    }

    @Override
    public void setOwner(@Nullable Entity owner) {
        super.setOwner(owner);
        if (hasBlockAuthority(owner)) {
            blockAuthorityEnabled = true;
            responsiblePlayerId = responsibleUuid(owner);
        }
    }

    /**
     * Lets a burst not fired by an engine (e.g. a placed pot) break blocks, with {@code responsible} as the player to
     * blame.
     */
    public void reachBlocksAs(@Nullable UUID responsible) {
        blockAuthorityEnabled = true;
        responsiblePlayerId = responsible;
    }

    /** Retains block-damage permission if the shooter is unloaded or removed. */
    public boolean canDamageBlocks() {
        return blockAuthorityEnabled;
    }

    /** Uses a different profile than the entity type's, for variants such as explosive rounds. */
    public void setPhysicsProfile(ResourceLocation profileId) {
        this.physicsProfileId = profileId;
    }

    public ProjectilePhysicsProfile getPhysicsProfile() {
        return physicsProfileId != null
                ? profileCatalog().get(physicsProfileId)
                : profileCatalog().forEntity(this.getType());
    }

    /**
     * Penetrates the hit block as far as it can. If it exits the far side it is moved there and keeps flying at the
     * remaining speed; otherwise the result says where it stopped.
     */
    protected ImpactResults.Drive driveInto(ServerLevel level, BlockHitResult hit) {
        Vec3 velocity = getDeltaMovement();
        double speed = velocity.length();
        BlockState struck = level.getBlockState(hit.getBlockPos());
        struck.onProjectileHit(level, struck, hit, this);
        if (!blockAuthorityEnabled || speed < 1.0E-6D) {
            return new ImpactResults.Drive(false, hit.getLocation(), speed, hit.getBlockPos(), hit.getLocation(),
                    Vec3.atLowerCornerOf(hit.getDirection().getNormal()));
        }
        ProjectilePhysicsProfile physics = getPhysicsProfile();
        double metresPerSecond = TICKS_PER_SECOND;
        ImpactResults.Drive drive = impacts().drive(level, physics, physics.diameterOf(this),
                hit.getLocation(), hit.getBlockPos(), velocity.scale(metresPerSecond), breaker());
        double kept = drive.speed() / metresPerSecond;
        if (drive.passedThrough()) {
            setPos(drive.position().x, drive.position().y, drive.position().z);
            setDeltaMovement(velocity.scale(kept / speed));
            setBaseDamage(Math.max(1.0D, getBaseDamage() * kept / speed));
            hasImpulse = true;
        }
        craterMouth = drive.passedThrough() ? null : drive.mouth();
        craterFace = drive.passedThrough() ? null : drive.face();
        return new ImpactResults.Drive(drive.passedThrough(), drive.position(), kept, drive.block(),
                drive.mouth(), drive.face());
    }

    /**
     * Spends the remaining energy where the shot stopped: breaks the surrounding material, then sets off any charge and
     * fire. {@code speed} is in blocks/tick.
     */
    protected void arrive(ServerLevel level, Vec3 at, double speed) {
        if (!blockAuthorityEnabled) {
            return;
        }
        ProjectilePhysicsProfile physics = getPhysicsProfile();
        Vec3 outward = getDeltaMovement().lengthSqr() > 1.0E-8D
                ? getDeltaMovement().normalize().reverse()
                : new Vec3(0.0D, 1.0D, 0.0D);
        impacts().stop(level, craterMouth != null ? craterMouth : at, craterFace != null ? craterFace : outward,
                outward.reverse(), physics, realSpeed(speed), physics.diameterOf(this), breaker());
        craterMouth = null;
        craterFace = null;
        double blastEnergy = payloadBlastEnergy(physics);
        ProjectilePhysicsProfile.Fire fire = payloadFire(physics);
        Player breaker = breaker();
        impacts().blast(level, at, outward, blastEnergy, breaker);
        impacts().ignite(level, at, fire, breaker);
        if (blastEnergy > 0.0D || fire.radius() > 0.0D) {
            onPayloadBurst(level, at, physics, responsiblePlayerId);
        }
    }

    /** Blast energy of the payload, in joules. */
    protected double payloadBlastEnergy(ProjectilePhysicsProfile physics) {
        return physics.blast().energy();
    }

    /** Fire spread by the payload. */
    protected ProjectilePhysicsProfile.Fire payloadFire(ProjectilePhysicsProfile physics) {
        return physics.fire();
    }

    /** Impact speed in m/s for a speed of {@code speed} blocks/tick. */
    public double realSpeed(double speed) {
        return speed * TICKS_PER_SECOND;
    }

    @Nullable
    private Player breaker() {
        Entity owner = getOwner();
        if (owner != null) {
            UUID current = responsibleUuid(owner);
            if (current != null) {
                responsiblePlayerId = current;
            }
            return responsiblePlayer(owner);
        }
        return level() instanceof ServerLevel serverLevel
                ? resolvePlayer(serverLevel, responsiblePlayerId)
                : null;
    }

    protected boolean damageLivingTarget(LivingEntity target, float damage) {
        return damageLivingTarget(target, damage, true);
    }

    protected boolean damageLivingTarget(LivingEntity target, float damage, boolean breakShield) {
        return damageTarget(target, damage, breakShield);
    }

    protected boolean damageTarget(Entity target, float damage) {
        return damageTarget(target, damage, true);
    }

    protected boolean damageTarget(Entity target, float damage, boolean breakShield) {
        Entity logical = ProjectileCombat.logicalTarget(target);
        if (logical instanceof LivingEntity living && breakShield) {
            ProjectileCombat.breakBlockingShield(living);
        }
        return target.hurt(damageSources().thrown(this, getOwner()), damage);
    }

    protected LivingEntity livingTarget(Entity target) {
        return ProjectileCombat.livingTarget(target);
    }

    protected void rememberHitTarget(Entity target) {
        hitTargets.add(ProjectileCombat.targetId(target));
    }

    protected boolean hasHitTarget(Entity target) {
        return hitTargets.contains(ProjectileCombat.targetId(target));
    }

    protected int hitTargetCount() {
        return hitTargets.size();
    }
    protected abstract ProfileCatalog<ProjectilePhysicsProfile> profileCatalog();

    protected abstract ImpactResolver impacts();

    protected String blockAuthorityKey() { return TAG_BLOCK_AUTHORITY; }

    protected boolean hasBlockAuthority(@Nullable Entity owner) { return owner instanceof Player; }

    @Nullable
    protected UUID responsibleUuid(@Nullable Entity owner) {
        return owner instanceof Player player ? player.getUUID() : null;
    }

    @Nullable
    protected Player responsiblePlayer(@Nullable Entity owner) {
        return owner instanceof Player player ? player : null;
    }

    @Nullable
    protected Player resolvePlayer(ServerLevel level, @Nullable UUID id) {
        return ProtectedBlockAccess.playerFor(level, id);
    }

    protected boolean mayHitTarget(Entity target) {
        Entity logical = ProjectileCombat.logicalTarget(target);
        Entity owner = ProjectileCombat.logicalTarget(getOwner());
        return logical != owner && (owner == null || !owner.isAlliedTo(logical)
                || owner.getTeam() == null || owner.getTeam().isAllowFriendlyFire());
    }

    protected void onPayloadBurst(ServerLevel level, Vec3 position,
                                  ProjectilePhysicsProfile physics, @Nullable UUID responsible) {
    }

}
