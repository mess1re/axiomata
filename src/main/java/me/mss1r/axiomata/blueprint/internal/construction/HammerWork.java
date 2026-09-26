package me.mss1r.axiomata.blueprint.internal.construction;

import dev.architectury.event.EventResult;
import me.mss1r.axiomata.blueprint.api.construction.BlueprintConstructionPlan;
import me.mss1r.axiomata.blueprint.api.construction.BuildProgress;
import me.mss1r.axiomata.blueprint.api.construction.ConstructionWork;
import me.mss1r.axiomata.blueprint.api.construction.UnderConstruction;
import me.mss1r.axiomata.blueprint.config.BlueprintServerConfig;
import me.mss1r.axiomata.blueprint.item.ConstructionHammerItem;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class HammerWork {
    private HammerWork() {
    }

    public static EventResult onEntityInteract(Player player, Entity target, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (!(held.getItem() instanceof ConstructionHammerItem)) {
            return EventResult.pass();
        }
        if (!(target instanceof UnderConstruction machine) || machine.isFullyBuilt()) {
            return EventResult.pass();
        }

        // Architectury reports both hands for one click. Only the main-hand server event may do
        // work; the other cases are still consumed so the target does not also handle the click.
        if (player.level().isClientSide() || hand != InteractionHand.MAIN_HAND) {
            return EventResult.interruptTrue();
        }
        if (player.isShiftKeyDown()) {
            dismantle(player, target, machine);
        } else {
            strike(player, target, machine);
        }
        return EventResult.interruptTrue();
    }

    private static void strike(Player player, Entity target, UnderConstruction machine) {
        if (player.getCooldowns().isOnCooldown(player.getMainHandItem().getItem())) {
            return;
        }
        BuildProgress progress = machine.buildProgress();
        BlueprintConstructionPlan.Stage stage = progress.currentStage();
        if (stage != null && !machine.acceptsBlowOnStage(player, stage.section())) {
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.displayClientMessage(Component.translatable(
                        "message.axiomata.construction.wrong_part",
                        StageNames.of(progress.blueprintId(), stage.section())), true);
            }
            return;
        }
        ConstructionWork.Result result = ConstructionWork.strike(machine, source(player));
        if (result.status() == ConstructionWork.Status.MISSING_MATERIAL) {
            if (player instanceof ServerPlayer serverPlayer && stage != null) {
                serverPlayer.displayClientMessage(Component.translatable(
                        "message.axiomata.construction.missing",
                        StageNames.of(progress.blueprintId(), stage.section()),
                        result.missing().getCount(), result.missing().getHoverName()), true);
            }
            return;
        }
        if (!result.worked()) {
            return;
        }

        player.getCooldowns().addCooldown(player.getMainHandItem().getItem(), BlueprintServerConfig.getHitCooldownTicks());
        player.level().playSound(null, target.blockPosition(), SoundEvents.WOOD_HIT,
                SoundSource.BLOCKS, 0.9F, 0.9F + player.level().random.nextFloat() * 0.2F);
        if (result.advanced()) {
            player.level().playSound(null, target.blockPosition(), SoundEvents.WOOD_PLACE,
                    SoundSource.BLOCKS, 1.0F, 1.1F);
            announceStage(player, progress);
        }
    }

    private static void dismantle(Player player, Entity target, UnderConstruction machine) {
        if (player.getCooldowns().isOnCooldown(player.getMainHandItem().getItem())) {
            return;
        }
        player.getCooldowns().addCooldown(player.getMainHandItem().getItem(), BlueprintServerConfig.getHitCooldownTicks());

        BuildProgress progress = machine.buildProgress();
        ConstructionWork.Result result = ConstructionWork.dismantle(machine, source(player));
        if (result.status() == ConstructionWork.Status.DONE) {
            target.discard();
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.displayClientMessage(
                        Component.translatable("message.axiomata.construction.cleared"), true);
            }
            return;
        }
        player.level().playSound(null, target.blockPosition(), SoundEvents.WOOD_BREAK,
                SoundSource.BLOCKS, 0.9F, 0.9F);
        announceStage(player, progress);
    }

    private static Container source(Player player) {
        return player.isCreative() ? null : player.getInventory();
    }

    private static void announceStage(Player player, BuildProgress progress) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        BlueprintConstructionPlan.Stage next = progress.currentStage();
        if (next == null) {
            serverPlayer.displayClientMessage(
                    Component.translatable("message.axiomata.construction.finished"), true);
            return;
        }
        MutableComponent needs = Component.empty();
        for (ItemStack material : next.materials()) {
            if (!needs.getSiblings().isEmpty()) {
                needs.append(", ");
            }
            needs.append(material.getHoverName()).append(" x" + material.getCount());
        }
        serverPlayer.displayClientMessage(Component.translatable(
                "message.axiomata.construction.next",
                StageNames.of(progress.blueprintId(), next.section()), needs), true);
    }
}
