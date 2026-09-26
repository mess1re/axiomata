package me.mss1r.axiomata.blueprint.network;

import dev.architectury.networking.NetworkManager;
import me.mss1r.axiomata.blueprint.menu.DrawingTableMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
//? if neoforge {
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import me.mss1r.axiomata.blueprint.BlueprintModule;
//?}

//? if forge {
/*public record C2STakeResultFromTablePacket() {
*///?} else {
public record C2STakeResultFromTablePacket() implements CustomPacketPayload {
    public static final Type<C2STakeResultFromTablePacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(BlueprintModule.MOD_ID, "take_table_result"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2STakeResultFromTablePacket> STREAM_CODEC =
            StreamCodec.ofMember(C2STakeResultFromTablePacket::write, C2STakeResultFromTablePacket::decode);

    private void write(RegistryFriendlyByteBuf buffer) {
        encode(this, buffer);
    }

    @Override
    public Type<C2STakeResultFromTablePacket> type() {
        return TYPE;
    }
    //?}
    public static void encode(C2STakeResultFromTablePacket packet, FriendlyByteBuf buffer) {
    }

    public static C2STakeResultFromTablePacket decode(FriendlyByteBuf buffer) {
        return new C2STakeResultFromTablePacket();
    }

    public static void handle(C2STakeResultFromTablePacket packet, NetworkManager.PacketContext context) {
        context.queue(() -> {
            if (!(context.getPlayer() instanceof ServerPlayer player)
                    || !(player.containerMenu instanceof DrawingTableMenu menu)) return;

            ItemStack result = menu.takeResult(player);
            if (!result.isEmpty()) {
                if (!player.addItem(result) && !result.isEmpty()) {
                    player.drop(result, false);
                }
                player.level().playSound(null, player.blockPosition(),
                        SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, SoundSource.BLOCKS, 1.0F, 1.0F);
                menu.slotsChanged(menu.getContainer());
            }
        });
    }
}
