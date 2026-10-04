package me.mss1r.axiomata.blueprint.item;

import dev.architectury.registry.menu.ExtendedMenuProvider;
import dev.architectury.registry.menu.MenuRegistry;
import me.mss1r.axiomata.ResourceIds;
import me.mss1r.axiomata.blueprint.api.BlueprintTags;
import me.mss1r.axiomata.blueprint.api.definition.BlueprintDefinition;
import me.mss1r.axiomata.blueprint.api.definition.BlueprintDefinitions;
import me.mss1r.axiomata.blueprint.api.construction.BuildQuality;
import me.mss1r.axiomata.blueprint.registry.BlueprintItems;
import net.minecraft.core.registries.BuiltInRegistries;
//? if neoforge {
import net.minecraft.core.component.DataComponents;
//?}
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
//? if neoforge {
import net.minecraft.world.item.component.CustomData;
//?}
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

public class BlueprintItem extends Item {
    private static final String QUALITY_TAG = "Quality";
    private static final String AUTHOR_TAG = "Author";
    private static final String WANDERED_TAG = "Wandered";
    private static final String WANDER_BUDGET_TAG = "WanderBudget";

    public BlueprintItem() { super(new Properties().stacksTo(1)); }

    public static void stamp(ItemStack stack, String author, int wandered, int wanderBudget) {
        updateTag(stack, tag -> {
            tag.putString(AUTHOR_TAG, author == null ? "" : author);
            tag.putInt(WANDERED_TAG, wandered);
            tag.putInt(WANDER_BUDGET_TAG, wanderBudget);
            tag.putString(QUALITY_TAG, BuildQuality.of(wandered, wanderBudget).id());
        });
    }

    public static void stampQuality(ItemStack stack, String author, BuildQuality quality) {
        updateTag(stack, tag -> {
            tag.putString(AUTHOR_TAG, author == null ? "" : author);
            tag.putString(QUALITY_TAG, quality.id());
            tag.remove(WANDERED_TAG);
            tag.remove(WANDER_BUDGET_TAG);
        });
    }

    public static BuildQuality getQuality(ItemStack stack) {
        CompoundTag tag = readTag(stack);
        return BuildQuality.byId(tag.getString(QUALITY_TAG));
    }

    public static String getAuthor(ItemStack stack) {
        return readTag(stack).getString(AUTHOR_TAG);
    }

    //? if forge {
    /*@Override
    public void appendHoverText(ItemStack stack, @javax.annotation.Nullable net.minecraft.world.level.Level level,
                                java.util.List<net.minecraft.network.chat.Component> lines,
                                net.minecraft.world.item.TooltipFlag flag) {
        super.appendHoverText(stack, level, lines, flag);
        appendBlueprintTooltip(stack, lines, flag);
    }
    *///?} else {
    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context,
                                java.util.List<net.minecraft.network.chat.Component> lines,
                                net.minecraft.world.item.TooltipFlag flag) {
        super.appendHoverText(stack, context, lines, flag);
        appendBlueprintTooltip(stack, lines, flag);
    }
    //?}

    private static void appendBlueprintTooltip(ItemStack stack,
                                               java.util.List<net.minecraft.network.chat.Component> lines,
                                               net.minecraft.world.item.TooltipFlag flag) {
        CompoundTag tag = readTag(stack);
        if (!tag.contains(QUALITY_TAG)) {
            return;
        }
        BuildQuality quality = getQuality(stack);
        lines.add(net.minecraft.network.chat.Component
                .translatable("tooltip.axiomata.blueprint.quality." + quality.id())
                .withStyle(quality.colour()));
        String author = getAuthor(stack);
        if (!author.isEmpty()) {
            lines.add(net.minecraft.network.chat.Component
                    .translatable("tooltip.axiomata.blueprint.author", author)
                    .withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
        }
        if (flag.isAdvanced() && tag.contains(WANDERED_TAG)) {
            lines.add(net.minecraft.network.chat.Component
                    .literal("wander " + tag.getInt(WANDERED_TAG) + " / " + tag.getInt(WANDER_BUDGET_TAG))
                    .withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
        }
    }

    public static ItemStack create(BlueprintDefinition recipe, String id) {
        ItemStack stack = new ItemStack(BlueprintItems.BLUEPRINT.get());
        // Store the catalogue id, not a copy of the recipe. Existing blueprints should follow a
        // data-pack rebalance instead of carrying stale costs forever.
        updateTag(stack, tag -> tag.putString("RecipeId", id));
        //? if forge {
        /*stack.setHoverName(Component.translatable("item.axiomata.blueprint_name", getResultName(recipe)));
        *///?} else {
        stack.set(DataComponents.CUSTOM_NAME,
                Component.translatable("item.axiomata.blueprint_name", getResultName(recipe)));
        //?}
        return stack;
    }

    @Nullable
    public static String getRecipeId(ItemStack stack) {
        if (stack.isEmpty() || stack.getItem() != BlueprintItems.BLUEPRINT.get()) {
            return null;
        }
        String recipeId = readTag(stack).getString("RecipeId");
        return recipeId.isBlank() ? null : recipeId;
    }

    public static ItemStack createResultStack(BlueprintDefinition recipe) {
        if (recipe == null) {
            return ItemStack.EMPTY;
        }
        ResourceLocation itemId = recipe.result().item();
        if (!BuiltInRegistries.ITEM.containsKey(itemId)) {
            return ItemStack.EMPTY;
        }

        ItemStack result = new ItemStack(BuiltInRegistries.ITEM.get(itemId), recipe.result().count());
        if (!recipe.result().data().isEmpty()) {
            CompoundTag tag = new CompoundTag();
            recipe.result().data().forEach(tag::putInt);
            //? if forge {
            /*result.setTag(tag);
            *///?} else {
            CustomData.update(DataComponents.CUSTOM_DATA, result, customData -> customData.merge(tag));
            //?}
        }
        return result;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!canOpenBlueprint(player, hand)) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable("message.axiomata.need_construction_hammer"), true);
            }
            return InteractionResultHolder.fail(stack);
        }

        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            openUseMenu(serverPlayer, readTag(stack).getString("RecipeId"), stack);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    /**
     * Opens the build menu for a blueprint, the drawn one held or the item standing in for it, held in the off hand
     * with a construction hammer.
     */
    public static void openUseMenu(ServerPlayer serverPlayer, String recipeId, ItemStack stack) {
        {
            BlueprintDefinition recipe = BlueprintDefinitions.get(recipeId);
            if (recipe != null) {
                MenuRegistry.openExtendedMenu(serverPlayer, new ExtendedMenuProvider() {
                    @Override
                    public Component getDisplayName() {
                        return Component.translatable("item.axiomata.blueprint_name", getResultName(recipe));
                    }
                    @Override
                    public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int containerId, net.minecraft.world.entity.player.Inventory inv, Player player) {
                        return new me.mss1r.axiomata.blueprint.menu.BlueprintUseMenu(containerId, inv, recipe, stack, recipeId);
                    }
                    @Override
                    public void saveExtraData(net.minecraft.network.FriendlyByteBuf buf) {
                        buf.writeUtf(recipeId);
                    }
                });
            }
        }
    }

    private boolean canOpenBlueprint(Player player, InteractionHand hand) {
        return hand == InteractionHand.OFF_HAND
                && player.getOffhandItem().getItem() == this
                && player.getMainHandItem().is(BlueprintTags.CONSTRUCTION_HAMMERS);
    }

    private static CompoundTag readTag(ItemStack stack) {
        //? if forge {
        /*CompoundTag tag = stack.getTag();
        return tag == null ? new CompoundTag() : tag;
        *///?} else {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        //?}
    }

    private static void updateTag(ItemStack stack, Consumer<CompoundTag> updater) {
        //? if forge {
        /*updater.accept(stack.getOrCreateTag());
        *///?} else {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, updater);
        //?}
    }

    private static Component getResultName(BlueprintDefinition recipe) {
        Item resultItem = BuiltInRegistries.ITEM.get(recipe.result().item());
        Component resultName = resultItem != null
                ? new ItemStack(resultItem).getHoverName()
                : Component.literal(recipe.result().item().toString());

        Integer sections = recipe.result().data().get("Sections");
        return sections != null
                ? Component.translatable("item.axiomata.result_with_sections", resultName, sections)
                : resultName;
    }
}
