package me.mss1r.axiomata.loading;

import net.minecraft.world.entity.Entity;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.UUID;

public class LoadingController {
    public interface Host {
        Entity entity();

        int loadStage();
        int cooldown();
        boolean ammoLoaded();
        int windingTime();
        int windingTotal();

        Component message(String key, Object... arguments);

        int stageDuration(String configKey);

        void loadingStarted(ServerLevel level);

        void loadingTick(ServerLevel level, Player player, InteractionHand hand,
                         int elapsedTicks, int totalTicks);

        void loadingCompleted(ServerLevel level, Player player, InteractionHand hand, int stageIndex);

        void loadingCancelled();

        boolean playSoundOnStart();

        boolean loopVisualReloadSound();

        int visualReloadSoundInterval();

        void playReloadSound(ServerLevel level);

        double serviceRange();

        int cooldownTotalTicks();

        String cooldownStatusKey();
    }

    private final Host host;
    private Player actor;
    private UUID actorUuid;
    private InteractionHand hand = InteractionHand.MAIN_HAND;
    private int stageIndex = -1;
    private int remainingTicks;
    private int totalTicks;
    private Item item = Items.AIR;
    private String status = "";
    private Item completingItem = Items.AIR;
    private int visualReloadSoundTicks;

    public LoadingController(Host host) {
        this.host = java.util.Objects.requireNonNull(host);
    }

    public boolean active() {
        return stageIndex >= 0;
    }

    public Item activeItem() {
        return item == Items.AIR ? completingItem : item;
    }

    public void clear() {
        stageIndex = -1;
        remainingTicks = 0;
        totalTicks = 0;
        item = Items.AIR;
        status = "";
        actor = null;
        actorUuid = null;
        hand = InteractionHand.MAIN_HAND;
    }

    public void cancelIfStageChanged(int newStage) {
        if (active() && stageIndex != newStage) {
            cancelSession();
        }
    }

    public boolean continueIfHeldItemMatches(Player player) {
        if (!active()) {
            return false;
        }
        if (actorUuid != null && actorUuid.equals(player.getUUID()) && !hasActiveItem(player)) {
            cancel(player, host.message("cancelled"));
            return false;
        }
        return true;
    }

    public InteractionResult begin(Player player, InteractionHand usedHand, ServerLevel level,
                                   int newStageIndex, LoadingRequirement stage) {
        return begin(player, usedHand, level, newStageIndex, stage, host.stageDuration(stage.timingKey()));
    }

    public InteractionResult begin(Player player, InteractionHand usedHand, ServerLevel level,
                                   int newStageIndex, LoadingRequirement stage, int durationTicks) {
        if (active()) {
            return showProgress(player);
        }

        ItemStack stack = player.getItemInHand(usedHand);
        stageIndex = newStageIndex;
        remainingTicks = Math.max(1, durationTicks);
        totalTicks = remainingTicks;
        item = stack.getItem();
        status = stage.feedback();
        actor = player;
        actorUuid = player.getUUID();
        hand = usedHand;
        host.loadingStarted(level);
        if (host.playSoundOnStart()) {
            host.playReloadSound(level);
        }
        showProgress(player);
        return InteractionResult.SUCCESS;
    }

    public InteractionResult beginMountedStage(Player player, InteractionHand usedHand,
                                               ServerLevel level, LoadingRequirement[] stages) {
        Entity entity = host.entity();
        if (entity.getFirstPassenger() != player) {
            return InteractionResult.PASS;
        }
        if (active()) {
            return showProgress(player);
        }
        if (host.cooldown() > 0) {
            return showCooldown(player);
        }

        int currentStage = host.loadStage();
        if (currentStage < 0 || currentStage >= stages.length) {
            return InteractionResult.SUCCESS;
        }

        LoadingRequirement stage = stages[currentStage];
        if (!canBegin(player, usedHand, stage)) {
            return InteractionResult.SUCCESS;
        }
        return begin(player, usedHand, level, currentStage, stage);
    }

    public boolean acceptsMountedItem(Player player, InteractionHand usedHand, LoadingRequirement[] stages) {
        Entity entity = host.entity();
        if (entity.getFirstPassenger() != player) {
            return false;
        }

        ItemStack stack = player.getItemInHand(usedHand);
        if (active()) {
            return stack.is(activeItem());
        }

        int currentStage = host.loadStage();
        return currentStage >= 0 && currentStage < stages.length
                && stages[currentStage].matches(stack.getItem());
    }

    public void tick(ServerLevel level) {
        if (!active()) {
            return;
        }

        Player activeActor = actor;
        if (activeActor == null || !activeActor.isAlive() || activeActor.isRemoved()
                || activeActor.level() != level) {
            cancelSession();
            return;
        }

        double serviceRange = host.serviceRange();
        if (activeActor.distanceToSqr(host.entity()) > serviceRange * serviceRange
                || host.loadStage() != stageIndex) {
            cancel(activeActor, host.message("cancelled"));
            return;
        }
        if (!hasActiveItem(activeActor)) {
            cancel(activeActor, host.message("cancelled"));
            return;
        }

        remainingTicks--;
        int elapsedTicks = totalTicks - remainingTicks;
        host.loadingTick(level, activeActor, hand, elapsedTicks, totalTicks);

        if (remainingTicks > 0) {
            if (remainingTicks % 10 == 0) {
                showProgress(activeActor);
            }
            return;
        }

        int completedStage = stageIndex;
        InteractionHand completedHand = hand;
        Item completedItem = item;
        clear();
        completingItem = completedItem;
        try {
            host.loadingCompleted(level, activeActor, completedHand, completedStage);
        } finally {
            completingItem = Items.AIR;
        }
    }

    public void tickVisualReloadSound(ServerLevel level) {
        Entity entity = host.entity();
        if (!host.loopVisualReloadSound() || active() || !host.ammoLoaded()
                || host.windingTime() <= 0 || !entity.isAlive()) {
            visualReloadSoundTicks = 0;
            return;
        }

        if (visualReloadSoundTicks <= 0) {
            host.playReloadSound(level);
            visualReloadSoundTicks = Math.max(1, host.visualReloadSoundInterval());
            return;
        }

        visualReloadSoundTicks--;
    }

    public InteractionResult showProgress(Player player) {
        if (!active()) {
            return InteractionResult.SUCCESS;
        }

        if (actorUuid != null && !player.getUUID().equals(actorUuid)) {
            player.displayClientMessage(host.message("busy"), true);
            return InteractionResult.SUCCESS;
        }

        player.displayClientMessage(status.isEmpty() ? host.message("state.ammunition",
                progressBar(totalTicks - remainingTicks, totalTicks)) : Component.translatable(status,
                progressBar(totalTicks - remainingTicks, totalTicks)), true);
        return InteractionResult.SUCCESS;
    }

    public InteractionResult showCooldown(Player player) {
        int remaining = host.cooldown();
        if (remaining <= 0) {
            return InteractionResult.SUCCESS;
        }

        int total = Math.max(host.cooldownTotalTicks(), remaining);
        player.displayClientMessage(Component.translatable(host.cooldownStatusKey(),
                progressBar(total - remaining, total)), true);
        return InteractionResult.SUCCESS;
    }

    public InteractionResult showWinding(Player player) {
        int remaining = host.windingTime();
        if (remaining <= 0) {
            return InteractionResult.SUCCESS;
        }

        int total = Math.max(host.windingTotal(), remaining);
        player.displayClientMessage(host.message("state.winding",
                progressBar(total - remaining, total)), true);
        return InteractionResult.SUCCESS;
    }

    public boolean canBegin(Player player, InteractionHand usedHand, LoadingRequirement required) {
        ItemStack stack = player.getItemInHand(usedHand);

        if (!required.matches(stack.getItem())) {
            showRequiredItem(player, required);
            return false;
        }

        if (!player.isCreative() && stack.getCount() < required.requiredStackCount()) {
            showRequiredAmount(player, required);
            return false;
        }
        return true;
    }

    public boolean consume(Player player, InteractionHand usedHand, LoadingRequirement required) {
        ItemStack stack = player.getItemInHand(usedHand);
        if (!required.matches(stack.getItem())
                || (activeItem() != Items.AIR && !stack.is(activeItem()))) {
            showRequiredItem(player, required);
            return false;
        }

        if (!player.isCreative()) {
            if (stack.getCount() < required.requiredStackCount()) {
                showRequiredAmount(player, required);
                return false;
            }
            if (required.itemPolicy() == LoadingRequirement.ItemPolicy.CONSUME) {
                stack.shrink(required.amount());
            }
            if (required.itemPolicy() == LoadingRequirement.ItemPolicy.DAMAGE_TOOL) {
                LoadingTools.damageHeldItem(stack, required.durabilityCost(), player, usedHand);
            }
        }
        return true;
    }

    private void cancel(Player actor, Component reason) {
        cancelSession();
        actor.displayClientMessage(reason, true);
    }

    private boolean hasActiveItem(Player actor) {
        return item != Items.AIR && actor.getItemInHand(hand).is(item);
    }

    private void cancelSession() {
        host.loadingCancelled();
        clear();
    }

    private void showRequiredItem(Player player, LoadingRequirement required) {
        player.displayClientMessage(
                host.message("next", required.item().getDefaultInstance().getHoverName()),
                true);
    }

    private void showRequiredAmount(Player player, LoadingRequirement required) {
        player.displayClientMessage(
                host.message("need_amount", required.amount(),
                        required.item().getDefaultInstance().getHoverName()), true);
    }

    private static String progressBar(int doneTicks, int totalTicks) {
        int width = 10;
        int done = Math.max(0, doneTicks);
        int total = Math.max(1, totalTicks);
        int filled = Math.max(0, Math.min(width, Math.round(done * width / (float) total)));
        return "[" + "#".repeat(filled) + "-".repeat(width - filled) + "]";
    }

}
