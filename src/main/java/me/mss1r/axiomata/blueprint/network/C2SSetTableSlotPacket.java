package me.mss1r.axiomata.blueprint.network;

import dev.architectury.networking.NetworkManager;
import me.mss1r.axiomata.blueprint.blockentity.DrawingTableBlockEntity;
import me.mss1r.axiomata.blueprint.menu.DrawingTableMenu;
import me.mss1r.axiomata.blueprint.registry.BlueprintItems;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
//? if neoforge {
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import me.mss1r.axiomata.blueprint.BlueprintModule;
//?}

//? if forge {
/*public record C2SSetTableSlotPacket(int slot, boolean insert) {
*///?} else {
public record C2SSetTableSlotPacket(int slot, boolean insert) implements CustomPacketPayload {
    public static final Type<C2SSetTableSlotPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(BlueprintModule.MOD_ID, "set_table_slot"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SSetTableSlotPacket> STREAM_CODEC =
            StreamCodec.ofMember(C2SSetTableSlotPacket::write, C2SSetTableSlotPacket::decode);

    private void write(RegistryFriendlyByteBuf buffer) {
        encode(this, buffer);
    }

    @Override
    public Type<C2SSetTableSlotPacket> type() {
        return TYPE;
    }
    //?}
    public static void encode(C2SSetTableSlotPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.slot);
        buffer.writeBoolean(packet.insert);
    }

    public static C2SSetTableSlotPacket decode(FriendlyByteBuf buffer) {
        return new C2SSetTableSlotPacket(buffer.readVarInt(), buffer.readBoolean());
    }

    public static void handle(C2SSetTableSlotPacket packet, NetworkManager.PacketContext context) {
        context.queue(() -> {
            if (!(context.getPlayer() instanceof ServerPlayer player)
                    || !(player.containerMenu instanceof DrawingTableMenu menu)) {
                return;
            }
            if (!menu.stillValid(player)) {
                return;
            }
            Item expected = itemFor(packet.slot());
            if (expected == null) {
                return;
            }

            SimpleContainer container = menu.getContainer();
            if (packet.insert()) {
                if (!container.getItem(packet.slot()).isEmpty()) {
                    return;
                }
                if (!takeFromInventory(player, expected)) {
                    return;
                }
                container.setItem(packet.slot(), new ItemStack(expected, 1));
            } else {
                ItemStack removed = container.getItem(packet.slot());
                if (removed.isEmpty()) {
                    return;
                }
                container.setItem(packet.slot(), ItemStack.EMPTY);
                player.getInventory().placeItemBackInInventory(removed);
            }
            menu.slotsChanged(container);
            menu.broadcastChanges();
        });
    }

    private static Item itemFor(int slot) {
        if (slot == DrawingTableBlockEntity.SLOT_PAPER) {
            return Items.PAPER;
        }
        if (slot == DrawingTableBlockEntity.SLOT_INK) {
            return BlueprintItems.INK_BLOCK.get();
        }
        return null;
    }

    private static boolean takeFromInventory(ServerPlayer player, Item item) {
        for (int index = 0; index < player.getInventory().getContainerSize(); index++) {
            ItemStack stack = player.getInventory().getItem(index);
            if (!stack.isEmpty() && stack.getItem() == item) {
                stack.shrink(1);
                if (stack.isEmpty()) {
                    player.getInventory().setItem(index, ItemStack.EMPTY);
                }
                return true;
            }
        }
        return false;
    }
}
