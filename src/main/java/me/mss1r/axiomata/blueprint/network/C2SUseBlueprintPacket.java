package me.mss1r.axiomata.blueprint.network;

import me.mss1r.axiomata.platform.AxiomataPlatform;

import me.mss1r.axiomata.blueprint.api.construction.ConstructionTarget;
import dev.architectury.networking.NetworkManager;
import me.mss1r.axiomata.ResourceIds;
import me.mss1r.axiomata.blueprint.api.BlueprintPermissions;
import me.mss1r.axiomata.blueprint.menu.BlueprintUseMenu;
import me.mss1r.axiomata.blueprint.internal.construction.ConstructionPlacementHelper;
import me.mss1r.axiomata.blueprint.api.construction.MaterialAllocation;
import me.mss1r.axiomata.blueprint.api.definition.BlueprintDefinition;
import me.mss1r.axiomata.blueprint.api.definition.BlueprintDefinition.Material;
import me.mss1r.axiomata.blueprint.api.definition.BlueprintDefinitions;
import me.mss1r.axiomata.blueprint.api.construction.ConstructionDeployer;
import me.mss1r.axiomata.blueprint.api.event.BlueprintUsedEvent;
import me.mss1r.axiomata.blueprint.api.event.BlueprintEvents;
import me.mss1r.axiomata.blueprint.item.BlueprintItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
//? if <1.21 {
/*import me.mss1r.axiomata.platform.AxiomataPlatform;
*///?}
//? if >=1.21 {
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import me.mss1r.axiomata.blueprint.BlueprintCommon;
//?}

import java.util.List;
import java.util.Map;

//? if <1.21 {
/*public record C2SUseBlueprintPacket(String recipeId) {
*///?} else {
public record C2SUseBlueprintPacket(String recipeId) implements CustomPacketPayload {
    public static final Type<C2SUseBlueprintPacket> TYPE = new Type<>(
            ResourceIds.id(BlueprintCommon.MOD_ID, "use_blueprint"));
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
            if (!(player.containerMenu instanceof BlueprintUseMenu menu)
                    || !packet.recipeId.equals(menu.getRecipeId()) || !menu.stillValid(player)) {
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

            if (!BlueprintItem.canUse(player, recipe, packet.recipeId, menu.getHand())) {
                return;
            }

            ItemStack blueprint = player.getItemInHand(menu.getHand());
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
            List<ItemStack> materials = menu.materialStacks(player);
            Map<Material, Integer> missing = MaterialAllocation.shortfall(materials, needed);
            if (!missing.isEmpty()) {
                player.displayClientMessage(Component.translatable("gui.axiomata.not_enough"), false);
                missing.forEach((material, count) -> player.displayClientMessage(
                        Component.literal("- ").append(material.displayName()).append(": " + count), false));
                return;
            }

            net.minecraft.world.entity.Entity machine = null;
            if (buildsInWorld) {
                ConstructionTarget target = ConstructionPlacementHelper.target(
                        player,
                        recipe,
                        //? if <1.21 {
                        /*AxiomataPlatform.blockReach(player),
                        *///?} else {
                        player.blockInteractionRange(),
                        //?}
                        1.0F
                );
                if (target == null) {
                    player.displayClientMessage(Component.translatable("message.axiomata.construction_target_missing"), false);
                    return;
                }
                if (!player.serverLevel().mayInteract(player, target.pos())
                        || !player.mayUseItemAt(target.pos(), target.face(), blueprint)) {
                    player.displayClientMessage(Component.translatable("gui.axiomata.no_permission"), false);
                    return;
                }

                machine = ConstructionDeployer.deploy(
                        player.serverLevel(),
                        packet.recipeId,
                        blueprint,
                        result,
                        target,
                        player.getYRot(),
                        player
                );
                if (machine == null) {
                    player.displayClientMessage(Component.translatable("message.axiomata.construction_invalid_location"), false);
                    return;
                }
            }

            MaterialAllocation.take(materials, needed);

            if (!player.getAbilities().instabuild) {
                blueprint.shrink(1);
            }
            // Complete the menu transaction before callbacks or another queued use packet can run.
            player.closeContainer();

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
            } else {
                ItemStack remaining = result.copy();
                player.getInventory().add(remaining);
                if (!remaining.isEmpty()) {
                    player.drop(remaining, false);
                }
            }
        });
    }

}
