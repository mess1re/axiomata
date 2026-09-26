package me.mss1r.axiomata.blueprint.internal.construction;

import me.mss1r.axiomata.blueprint.api.construction.UnderConstruction;
import me.mss1r.axiomata.blueprint.api.definition.BlueprintDefinition;
import net.minecraft.core.BlockPos;
//? if neoforge {
import net.minecraft.core.component.DataComponents;
//?}
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashSet;

public final class ConstructionPlacementHelper {
    private static final double WATER_HIT_EPSILON = 1.0E-4D;
    private static final double GROUND_SUPPORT_RATIO = 0.4D;
    private static final double BOUNDS_EPSILON = 1.0E-4D;
    private static final double PLACEMENT_OVERLAP_MARGIN = 0.1D;
    private static final double CONSTRUCTION_SITE_SEARCH_MARGIN = 12.0D;
    private ConstructionPlacementHelper() {
    }

    public enum PlacementMode {
        GROUND,
        WATER
    }

    public record PlacementPlan(PlacementMode mode, BlockPos anchorPos, Vec3 worldPos, AABB bounds, boolean valid) {
    }

    public static boolean requiresFluidTargeting(Level level, ItemStack stack) {
        return requiresFluidTargeting(level, stack, null);
    }

    public static boolean requiresFluidTargeting(Level level, ItemStack stack, @Nullable BlueprintDefinition recipe) {
        if (recipe != null && recipe.result != null && recipe.result.deployment != null) {
            return recipe.result.deployment.isWater();
        }
        Entity previewEntity = createPreviewEntity(level, stack, Vec3.ZERO, 0.0F, 0, null);
        return previewEntity instanceof Boat;
    }

    @Nullable
    public static PlacementPlan findPlacement(Level level, ItemStack stack, BlockPos targetPos, Direction targetFace, float yaw) {
        return findPlacement(level, stack, targetPos, targetFace, Vec3.atCenterOf(targetPos), yaw, null);
    }

    @Nullable
    public static PlacementPlan findPlacement(Level level, ItemStack stack, BlockPos targetPos, Direction targetFace, @Nullable Vec3 hitLocation, float yaw) {
        return findPlacement(level, stack, targetPos, targetFace, hitLocation, yaw, null);
    }

    @Nullable
    public static PlacementPlan findPlacement(Level level, ItemStack stack, BlockPos targetPos, Direction targetFace,
                                                @Nullable Vec3 hitLocation, float yaw,
                                                @Nullable BlueprintDefinition recipe) {
        Entity previewEntity = createPreviewEntity(level, stack, getProbePos(targetPos, hitLocation), yaw, 0, recipe);
        boolean groundBlockResult = isGroundBlockResult(stack);
        boolean explicitDeployment = recipe != null && recipe.result != null && recipe.result.deployment != null;
        if (previewEntity == null && !groundBlockResult && !explicitDeployment) {
            return null;
        }

        return requiresFluidTargeting(level, stack, recipe)
                ? findWaterPlacement(level, stack, targetPos, targetFace, hitLocation, yaw, recipe)
                : findGroundPlacement(level, stack, targetPos, targetFace, yaw, recipe);
    }

    @Nullable
    public static PlacementPlan findPlacementAtAnchor(Level level, ItemStack stack, BlockPos anchorPos, PlacementMode mode, float yaw) {
        return findPlacementAtAnchor(level, stack, anchorPos, mode, yaw, null);
    }

    @Nullable
    public static PlacementPlan findPlacementAtAnchor(Level level, ItemStack stack, BlockPos anchorPos, PlacementMode mode,
                                                       float yaw, @Nullable BlueprintDefinition recipe) {
        Entity previewEntity = createPreviewEntity(level, stack, getAnchorProbePos(level, anchorPos, mode), yaw, 0, recipe);
        boolean groundBlockResult = isGroundBlockResult(stack);
        boolean explicitDeployment = recipe != null && recipe.result != null && recipe.result.deployment != null;
        if (previewEntity == null && !(groundBlockResult && mode == PlacementMode.GROUND) && !explicitDeployment) {
            return null;
        }

        return mode == PlacementMode.WATER
                ? createWaterPlan(level, stack, anchorPos, getWaterSurfacePos(level, anchorPos), yaw, recipe)
                : createGroundPlan(level, stack, anchorPos, yaw, recipe);
    }

    @Nullable
    public static Entity createPreviewEntity(Level level, ItemStack stack, Vec3 worldPos, float yaw, int tickCount) {
        return createPreviewEntity(level, stack, worldPos, yaw, tickCount, null);
    }

    @Nullable
    public static Entity createPreviewEntity(Level level, ItemStack stack, Vec3 worldPos, float yaw, int tickCount,
                                              @Nullable BlueprintDefinition recipe) {
        EntityType<?> entityType = resolveEntityType(stack, recipe);
        if (entityType == null) {
            return null;
        }

        Entity entity = entityType.create(level);
        if (entity == null) {
            return null;
        }

        configureEntity(entity, stack, worldPos, yaw, tickCount);
        return entity;
    }

    public static AABB getPlannedBounds(Level level, ItemStack stack, Vec3 worldPos, float yaw,
                                        @Nullable BlueprintDefinition recipe) {
        Entity previewEntity = createPreviewEntity(level, stack, worldPos, yaw, 0, recipe);
        if (previewEntity != null) {
            return previewEntity.getBoundingBox();
        }

        BlockState previewBlockState = createPreviewBlockState(stack, yaw);
        if (previewBlockState != null) {
            BlockPos blockPos = BlockPos.containing(worldPos.x, worldPos.y - 0.05D, worldPos.z);
            return getBlockPreviewBounds(level, blockPos, previewBlockState);
        }

        return new AABB(
                worldPos.x - 0.5D, worldPos.y, worldPos.z - 0.5D,
                worldPos.x + 0.5D, worldPos.y + 1.0D, worldPos.z + 0.5D
        );
    }

    @Nullable
    public static EntityType<?> resolveEntityType(ItemStack stack) {
        return resolveEntityType(stack, null);
    }

    @Nullable
    public static EntityType<?> resolveEntityType(ItemStack stack, @Nullable BlueprintDefinition recipe) {
        if (recipe != null && recipe.result != null && recipe.result.deployment != null
                && recipe.result.deployment.preview_entity != null) {
            ResourceLocation configuredId = ResourceLocation.tryParse(recipe.result.deployment.preview_entity);
            return configuredId == null ? null : BuiltInRegistries.ENTITY_TYPE.getOptional(configuredId).orElse(null);
        }

        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (itemId == null) {
            return null;
        }

        EntityType<?> direct = BuiltInRegistries.ENTITY_TYPE.getOptional(itemId).orElse(null);
        if (direct != null) return direct;

        String itemPath = itemId.getPath();
        String normalizedItemPath = itemPath.endsWith("_spawner")
                ? itemPath.substring(0, itemPath.length() - "_spawner".length())
                : itemPath;
        EntityType<?> bestMatch = null;
        int bestMatchLength = -1;
        for (ResourceLocation entityId : BuiltInRegistries.ENTITY_TYPE.keySet()) {
            if (!entityId.getNamespace().equals(itemId.getNamespace())) {
                continue;
            }

            String entityPath = entityId.getPath();
            if ((itemPath.equals(entityPath)
                    || itemPath.endsWith("_" + entityPath)
                    || normalizedItemPath.equals(entityPath)
                    || normalizedItemPath.endsWith("_" + entityPath))
                    && entityPath.length() > bestMatchLength) {
                bestMatch = BuiltInRegistries.ENTITY_TYPE.get(entityId);
                bestMatchLength = entityPath.length();
            }
        }

        return bestMatch;
    }

    private static boolean isGroundBlockResult(ItemStack stack) {
        return stack.getItem() instanceof BlockItem;
    }

    @Nullable
    public static BlockState createPreviewBlockState(ItemStack stack, float yaw) {
        if (!(stack.getItem() instanceof BlockItem blockItem)) {
            return null;
        }

        BlockState state = blockItem.getBlock().defaultBlockState();
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            state = state.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.fromYRot(yaw));
        }
        return state;
    }

    public static void configureEntity(Entity entity, ItemStack stack, Vec3 worldPos, float yaw, int tickCount) {
        entity.moveTo(worldPos.x, worldPos.y, worldPos.z, yaw, 0.0F);
        entity.setYRot(yaw);
        entity.yRotO = yaw;
        entity.tickCount = tickCount;

        if (entity instanceof Boat boat) {
            Boat.Type boatType = resolveBoatType(stack);
            if (boatType != null) {
                boat.setVariant(boatType);
            }
        }

        if (entity instanceof LivingEntity living) {
            living.yBodyRot = yaw;
            living.yBodyRotO = yaw;
            living.yHeadRot = yaw;
            living.yHeadRotO = yaw;
        }

        if (entity instanceof UnderConstruction underConstruction) {
            underConstruction.orientForPlacement(yaw);
        }

        if (
                //? if forge {
                /*stack.hasCustomHoverName()
                *///?} else {
                stack.has(DataComponents.CUSTOM_NAME)
                //?}
        ) {
            entity.setCustomName(stack.getHoverName());
        }
    }

    @Nullable
    private static PlacementPlan findGroundPlacement(Level level, ItemStack stack, BlockPos targetPos, Direction targetFace,
                                                      float yaw, @Nullable BlueprintDefinition recipe) {
        return createGroundPlan(level, stack, targetPos, yaw, recipe);
    }

    private static PlacementPlan createGroundPlan(Level level, ItemStack stack, BlockPos supportPos, float yaw,
                                                   @Nullable BlueprintDefinition recipe) {
        BlockPos sitePos = supportPos.above();
        Entity previewEntity = createPreviewEntity(level, stack, Vec3.ZERO, yaw, 0, recipe);
        BlockState previewBlockState = createPreviewBlockState(stack, yaw);
        Vec3 worldPos = new Vec3(supportPos.getX() + 0.5D, supportPos.getY() + 1.05D, supportPos.getZ() + 0.5D);
        AABB bounds = new AABB(worldPos.x - 0.5D, worldPos.y, worldPos.z - 0.5D, worldPos.x + 0.5D, worldPos.y + 1.35D, worldPos.z + 0.5D);

        if (previewEntity != null) {
            previewEntity.setPos(sitePos.getX() + 0.5D, sitePos.getY() + 1.0D, sitePos.getZ() + 0.5D);
            double yOffset = getYOffset(level, sitePos, true, previewEntity.getBoundingBox());
            worldPos = new Vec3(sitePos.getX() + 0.5D, sitePos.getY() + yOffset, sitePos.getZ() + 0.5D);
            configureEntity(previewEntity, stack, worldPos, yaw, 0);
            bounds = previewEntity.getBoundingBox();
        } else if (previewBlockState != null) {
            worldPos = Vec3.atCenterOf(sitePos);
            bounds = getBlockPreviewBounds(level, sitePos, previewBlockState);
        }

        boolean valid = hasGroundSupport(level, supportPos, bounds)
                && !overlapsExistingConstructionSite(level, bounds)
                && (previewEntity == null || level.noCollision(previewEntity, bounds));

        return new PlacementPlan(PlacementMode.GROUND, supportPos.immutable(), worldPos, bounds, valid);
    }

    private static AABB getBlockPreviewBounds(Level level, BlockPos blockPos, BlockState state) {
        VoxelShape shape = state.getShape(level, blockPos);
        return shape.isEmpty() ? new AABB(blockPos) : shape.bounds().move(blockPos);
    }

    @Nullable
    private static PlacementPlan findWaterPlacement(Level level, ItemStack stack, BlockPos targetPos, Direction targetFace,
                                                     @Nullable Vec3 hitLocation, float yaw,
                                                     @Nullable BlueprintDefinition recipe) {
        LinkedHashSet<BlockPos> candidates = new LinkedHashSet<>();
        addWaterCandidates(candidates, targetPos, targetFace);

        PlacementPlan fallback = null;
        for (BlockPos waterPos : candidates) {
            Vec3 preferredPos = shouldUseHitLocationForWaterPos(waterPos, hitLocation)
                    ? new Vec3(hitLocation.x, getWaterSurfacePos(level, waterPos).y, hitLocation.z)
                    : getWaterSurfacePos(level, waterPos);
            PlacementPlan plan = createWaterPlan(level, stack, waterPos, preferredPos, yaw, recipe);
            if (fallback == null) {
                fallback = plan;
            }
            if (plan.valid()) {
                return plan;
            }
        }

        return fallback;
    }

    private static PlacementPlan createWaterPlan(Level level, ItemStack stack, BlockPos waterPos, Vec3 desiredWorldPos,
                                                  float yaw, @Nullable BlueprintDefinition recipe) {
        Vec3 worldPos = desiredWorldPos;
        Entity previewEntity = createPreviewEntity(level, stack, worldPos, yaw, 0, recipe);
        AABB bounds = new AABB(worldPos.x - 0.5D, worldPos.y, worldPos.z - 0.5D, worldPos.x + 0.5D, worldPos.y + 1.0D, worldPos.z + 0.5D);

        if (previewEntity != null) {
            configureEntity(previewEntity, stack, worldPos, yaw, 0);
            bounds = previewEntity.getBoundingBox();
        }

        boolean valid = level.getFluidState(waterPos).is(FluidTags.WATER)
                && hasWaterSupport(level, bounds, waterPos.getY())
                && !overlapsExistingConstructionSite(level, bounds)
                && (previewEntity == null || level.noCollision(previewEntity, bounds));

        return new PlacementPlan(PlacementMode.WATER, waterPos.immutable(), worldPos, bounds, valid);
    }

    @Nullable
    private static Boat.Type resolveBoatType(ItemStack stack) {
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (itemId == null) {
            return null;
        }

        String itemPath = itemId.getPath();
        Boat.Type bestMatch = null;
        int bestLength = -1;
        for (Boat.Type type : Boat.Type.values()) {
            String name = type.getName();
            if ((itemPath.equals(name) || itemPath.startsWith(name + "_")) && name.length() > bestLength) {
                bestMatch = type;
                bestLength = name.length();
            }
        }

        return bestMatch;
    }

    private static void addWaterCandidates(LinkedHashSet<BlockPos> candidates, BlockPos targetPos, Direction targetFace) {
        addCandidate(candidates, targetPos);
        addCandidate(candidates, targetPos.relative(targetFace));
        addCandidate(candidates, targetPos.above());
        addCandidate(candidates, targetPos.below());

        if (targetFace.getAxis().isHorizontal()) {
            addCandidate(candidates, targetPos.relative(targetFace).above());
            addCandidate(candidates, targetPos.relative(targetFace).below());
        }
    }

    private static void addCandidate(LinkedHashSet<BlockPos> candidates, BlockPos candidate) {
        candidates.add(candidate.immutable());
    }

    private static Vec3 getProbePos(BlockPos targetPos, @Nullable Vec3 hitLocation) {
        return hitLocation != null ? hitLocation : Vec3.atCenterOf(targetPos);
    }

    private static Vec3 getAnchorProbePos(Level level, BlockPos anchorPos, PlacementMode mode) {
        return mode == PlacementMode.WATER ? getWaterSurfacePos(level, anchorPos) : Vec3.atCenterOf(anchorPos.above());
    }

    private static Vec3 getWaterSurfacePos(Level level, BlockPos waterPos) {
        double fluidHeight = level.getFluidState(waterPos).getHeight(level, waterPos);
        if (fluidHeight <= 0.0D) {
            fluidHeight = 1.0D;
        }
        return new Vec3(waterPos.getX() + 0.5D, waterPos.getY() + fluidHeight, waterPos.getZ() + 0.5D);
    }

    private static boolean shouldUseHitLocationForWaterPos(BlockPos waterPos, @Nullable Vec3 hitLocation) {
        if (hitLocation == null) {
            return false;
        }
        return resolveWaterHitPos(hitLocation).equals(waterPos);
    }

    private static BlockPos resolveWaterHitPos(Vec3 hitLocation) {
        return BlockPos.containing(hitLocation.x, hitLocation.y - WATER_HIT_EPSILON, hitLocation.z);
    }

    public static boolean hasWaterSupport(Level level, AABB bounds, int waterY) {
        double sampleY = waterY + 0.5D;
        double minX = bounds.minX + 0.2D;
        double maxX = bounds.maxX - 0.2D;
        double minZ = bounds.minZ + 0.2D;
        double maxZ = bounds.maxZ - 0.2D;

        return isWaterAt(level, bounds.getCenter().x, sampleY, bounds.getCenter().z)
                && isWaterAt(level, minX, sampleY, minZ)
                && isWaterAt(level, minX, sampleY, maxZ)
                && isWaterAt(level, maxX, sampleY, minZ)
                && isWaterAt(level, maxX, sampleY, maxZ);
    }

    private static boolean isWaterAt(Level level, double x, double y, double z) {
        return level.getFluidState(BlockPos.containing(x, y, z)).is(FluidTags.WATER);
    }

    public static boolean hasGroundSupport(Level level, BlockPos centerSupportPos, AABB bounds) {
        BlockState centerState = level.getBlockState(centerSupportPos);
        if (!centerState.isFaceSturdy(level, centerSupportPos, Direction.UP)) {
            return false;
        }

        double footprintArea = Math.max(BOUNDS_EPSILON, bounds.getXsize() * bounds.getZsize());
        double supportedArea = 0.0D;
        int minX = Mth.floor(bounds.minX + BOUNDS_EPSILON);
        int maxX = Mth.floor(bounds.maxX - BOUNDS_EPSILON);
        int minZ = Mth.floor(bounds.minZ + BOUNDS_EPSILON);
        int maxZ = Mth.floor(bounds.maxZ - BOUNDS_EPSILON);

        for (int x = minX; x <= maxX; x++) {
            double overlapX = Math.max(0.0D, Math.min(bounds.maxX, x + 1.0D) - Math.max(bounds.minX, x));
            for (int z = minZ; z <= maxZ; z++) {
                double overlapZ = Math.max(0.0D, Math.min(bounds.maxZ, z + 1.0D) - Math.max(bounds.minZ, z));
                BlockPos supportPos = new BlockPos(x, centerSupportPos.getY(), z);
                BlockState supportState = level.getBlockState(supportPos);
                if (supportState.isFaceSturdy(level, supportPos, Direction.UP)) {
                    supportedArea += overlapX * overlapZ;
                }
            }
        }

        return supportedArea + BOUNDS_EPSILON >= footprintArea * GROUND_SUPPORT_RATIO;
    }

    private static boolean overlapsExistingConstructionSite(Level level, AABB candidateBounds) {
        // Unfinished machines have no solid collision, so their render bounds stand in here. This
        // prevents placing a second construction site through the first one.
        AABB collisionBounds = candidateBounds.inflate(PLACEMENT_OVERLAP_MARGIN);
        AABB searchBounds = collisionBounds.inflate(CONSTRUCTION_SITE_SEARCH_MARGIN);
        return level.getEntitiesOfClass(Entity.class, searchBounds,
                        entity -> entity instanceof UnderConstruction machine && !machine.isFullyBuilt())
                .stream()
                .filter(machine -> !machine.isRemoved())
                .map(machine -> machine.getBoundingBoxForCulling().inflate(PLACEMENT_OVERLAP_MARGIN))
                .anyMatch(existing -> existing.intersects(collisionBounds));
    }

    private static double getYOffset(LevelReader reader, BlockPos pos, boolean canMoveDown, AABB aabb) {
        AABB collisionBox = new AABB(pos);
        if (canMoveDown) {
            collisionBox = collisionBox.expandTowards(0.0D, -1.0D, 0.0D);
        }

        Iterable<VoxelShape> collisions = reader.getCollisions(null, collisionBox);
        return 1.0D + Shapes.collide(Direction.Axis.Y, aabb, collisions, canMoveDown ? -2.0D : -1.0D);
    }
}
