package me.mss1r.axiomata.blueprint.api.event;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class BlueprintCreationCheckEvent {
    private final ServerPlayer player;
    private final ItemStack blueprint;
    private boolean allowed = true;

    public BlueprintCreationCheckEvent(ServerPlayer player, ItemStack blueprint) {
        this.player = player;
        this.blueprint = blueprint.copy();
    }

    public ServerPlayer player() {
        return player;
    }

    public ItemStack blueprint() {
        return blueprint.copy();
    }

    public boolean allowed() {
        return allowed;
    }

    public void deny() {
        allowed = false;
    }
}
