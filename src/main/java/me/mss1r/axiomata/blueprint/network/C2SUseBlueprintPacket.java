package me.mss1r.axiomata.blueprint.network;

import dev.architectury.networking.NetworkManager;
import me.mss1r.axiomata.ResourceIds;
import me.mss1r.axiomata.blueprint.api.BlueprintPermissions;
import me.mss1r.axiomata.blueprint.api.BlueprintTags;
import me.mss1r.axiomata.blueprint.api.ConstructionStarters;
import me.mss1r.axiomata.blueprint.internal.construction.ConstructionPlacementHelper;
import me.mss1r.axiomata.blueprint.api.definition.BlueprintDefinition;
import me.mss1r.axiomata.blueprint.api.definition.BlueprintDefinition.Material;
import me.mss1r.axiomata.blueprint.api.definition.BlueprintDefinitions;
import me.mss1r.axiomata.blueprint.api.construction.ConstructionDeployer;
import me.mss1r.axiomata.blueprint.api.event.BlueprintUsedEvent;
import me.mss1r.axiomata.blueprint.api.event.BlueprintEvents;
import me.mss1r.axiomata.blueprint.item.BlueprintItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;
//? if forge {
/*import net.minecraftforge.common.ForgeMod;
*///?}
//? if neoforge {
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import me.mss1r.axiomata.blueprint.BlueprintModule;
//?}

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

//? if forge {
/*public record C2SUseBlueprintPacket(String recipeId) {
*///?} else {
public record C2SUseBlueprintPacket(String recipeId) implements CustomPacketPayload {
    public static final Type<C2SUseBlueprintPacket> TYPE = new Type<>(
            ResourceIds.id(BlueprintModule.MOD_ID, "use_blueprint"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SUseBlueprintPacket> STREAM_CODEC =
            StreamCodec.ofMember(C2SUseBlueprintPacket::write, C2SUseBlueprintPacket::decode);

    private void write(RegistryFriendlyByteBuf buffer) {
        encode(this, buffer);
    }

    @Override
    public Type<C2SUseBlueprintPacket> type() {
        return TYPE;
    }
    //?}
    public static C2SUseBlueprintPacket decode(FriendlyByteBuf buffer) {
        return new C2SUseBlueprintPacket(buffer.readUtf());
    }

    public static void encode(C2SUseBlueprintPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.recipeId);
    }

    public static void handle(C2SUseBlueprintPacket packet, NetworkManager.PacketContext context) {
        context.queue(() -> {
            if (!(context.getPlayer() instanceof ServerPlayer player)) {
                return;
            }

            BlueprintDefinition recipe = BlueprintDefinitions.get(packet.recipeId);
            if (recipe == null) {
                player.displayClientMessage(Component.translatable("gui.axiomata.invalid_recipe"), false);
                return;
            }

            if (!BlueprintPermissions.canUse(player, recipe, packet.recipeId)) {
                player.displayClientMessage(Component.translatable("gui.axiomata.no_permission"), false);
                return;
            }

            if (!hasConstructionTools(player, packet.recipeId)) {
                player.displayClientMessage(Component.translatable("message.axiomata.need_construction_hammer"), false);
                return;
            }

            ItemStack blueprint = player.getOffhandItem();
            if (blueprint.isEmpty()) {
                player.displayClientMessage(Component.translatable("gui.axiomata.invalid_recipe"), false);
                return;
            }

            ItemStack result = BlueprintItem.createResultStack(recipe);
            if (result.isEmpty()) {
                player.displayClientMessage(Component.translatable("gui.axiomata.invalid_recipe"), false);
                return;
            }

            boolean buildsInWorld = recipe.buildsInWorld();
            List<Material> needed = buildsInWorld ? List.of() : recipe.totals();
            if (!buildsInWorld && !hasMaterials(player.getInventory(), needed)) {
                player.displayClientMessage(Component.translatable("gui.axiomata.not_enough"), false);
                for (Material material : needed) {
                    int missing = material.count() - count(player.getInventory(), material);
                    if (missing > 0) {
                        player.displayClientMessage(Component.literal("- ").append(material.displayName())
                                .append(": " + missing), false);
                    }
                }
                return;
            }

            net.minecraft.world.entity.Entity machine = null;
            if (buildsInWorld) {
                HitResult target = player.pick(
                        //? if forge {
                        /*player.getAttributeValue(ForgeMod.BLOCK_REACH.get()),
                        *///?} else {
                        player.blockInteractionRange(),
                        //?}
                        0.0F,
                        ConstructionPlacementHelper.requiresFluidTargeting(player.level(), result, recipe)
                );
                if (!(target instanceof BlockHitResult blockTarget) || target.getType() != HitResult.Type.BLOCK) {
                    player.displayClientMessage(Component.translatable("message.axiomata.construction_target_missing"), false);
                    return;
                }
                if (!player.serverLevel().mayInteract(player, blockTarget.getBlockPos())
                        || !player.mayUseItemAt(blockTarget.getBlockPos(), blockTarget.getDirection(), blueprint)) {
                    player.displayClientMessage(Component.translatable("gui.axiomata.no_permission"), false);
                    return;
                }

                machine = ConstructionDeployer.deploy(
                        player.serverLevel(),
                        packet.recipeId,
                        blueprint,
                        result,
                        blockTarget.getBlockPos(),
                        blockTarget.getDirection(),
                        blockTarget.getLocation(),
                        player.getYRot()
                );
                if (machine == null) {
                    player.displayClientMessage(Component.translatable("message.axiomata.construction_invalid_location"), false);
                    return;
                }
            }

            if (!buildsInWorld) {
                for (Material material : needed) {
                    take(player.getInventory(), material);
                }
            }

            if (!player.getAbilities().instabuild) {
                blueprint.shrink(1);
            }

            BlueprintEvents.USED.invoker().used(new BlueprintUsedEvent(
                    player, packet.recipeId, recipe, result, buildsInWorld, machine));

            if (machine != null) {
                player.displayClientMessage(
                        Component.translatable(
                                "message.axiomata.construction_started",
                                result.getHoverName()
                        ),
                        false
                );
            } else if (!player.getInventory().add(result.copy())) {
                player.drop(result.copy(), false);
            }
        });
    }

    private static boolean hasMaterials(Inventory inventory, List<Material> materials) {
        for (Material material : materials) {
            if (count(inventory, material) < material.count()) {
                return false;
            }
        }
        return true;
    }

    private static int count(Inventory inventory, Material material) {
        int found = 0;
        for (List<ItemStack> stacks : List.of(inventory.items, inventory.offhand)) {
            for (ItemStack stack : stacks) {
                if (material.matches(stack)) {
                    found += stack.getCount();
                }
            }
        }
        return found;
    }

    private static void take(Inventory inventory, Material material) {
        int remaining = material.count();
        for (List<ItemStack> stacks : List.of(inventory.items, inventory.offhand)) {
            for (ItemStack stack : stacks) {
                if (remaining <= 0) {
                    return;
                }
                if (material.matches(stack)) {
                    int taken = Math.min(stack.getCount(), remaining);
                    stack.shrink(taken);
                    remaining -= taken;
                }
            }
        }
    }

    private static boolean hasConstructionTools(ServerPlayer player, String recipeId) {
        return player.getMainHandItem().is(BlueprintTags.CONSTRUCTION_HAMMERS)
                && isBlueprintForRecipe(player.getOffhandItem(), recipeId);
    }

    private static boolean isBlueprintForRecipe(ItemStack stack, String recipeId) {
        return recipeId.equals(ConstructionStarters.definitionOf(stack));
    }
}
