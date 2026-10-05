package me.mss1r.axiomata.blueprint.network;

import dev.architectury.networking.NetworkManager;
import me.mss1r.axiomata.ResourceIds;
import me.mss1r.axiomata.blueprint.BlueprintModule;
import me.mss1r.axiomata.blueprint.internal.construction.SectionBoundsCatalog;
import me.mss1r.axiomata.collision.OrientedBox;
import me.mss1r.axiomata.collision.Rotation3;
import me.mss1r.axiomata.geometry.LocalBox;
import me.mss1r.axiomata.structure.SectionBounds;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
//? if neoforge {
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//?}

import java.util.Map;

//? if forge {
/*public record S2CConstructionBoundsPacket(Map<ResourceLocation, SectionBounds> bounds) {
*///?} else {
public record S2CConstructionBoundsPacket(Map<ResourceLocation, SectionBounds> bounds) implements CustomPacketPayload {
    public static final Type<S2CConstructionBoundsPacket> TYPE = new Type<>(
            ResourceIds.id(BlueprintModule.MOD_ID, "construction_bounds"));
    public static final StreamCodec<RegistryFriendlyByteBuf, S2CConstructionBoundsPacket> STREAM_CODEC =
            StreamCodec.ofMember(S2CConstructionBoundsPacket::write, S2CConstructionBoundsPacket::decode);

    private void write(RegistryFriendlyByteBuf buffer) {
        encode(this, buffer);
    }

    @Override
    public Type<S2CConstructionBoundsPacket> type() {
        return TYPE;
    }
    //?}

    public S2CConstructionBoundsPacket {
        bounds = Map.copyOf(bounds);
    }

    public static void encode(S2CConstructionBoundsPacket packet, FriendlyByteBuf buffer) {
        buffer.writeMap(packet.bounds, FriendlyByteBuf::writeResourceLocation, (out, sections) ->
                out.writeCollection(sections.sections(), (data, section) -> {
                    data.writeUtf(section.name());
                    LocalBox box = section.bounds();
                    writeVector(data, new Vec3(box.minX(), box.minY(), box.minZ()));
                    writeVector(data, new Vec3(box.maxX(), box.maxY(), box.maxZ()));
                    data.writeCollection(section.parts(), (partData, part) -> {
                        writeVector(partData, part.center());
                        writeVector(partData, part.halfExtent());
                        for (int row = 0; row < 3; row++) {
                            for (int column = 0; column < 3; column++) {
                                partData.writeDouble(part.rotation().element(row, column));
                            }
                        }
                    });
                }));
    }

    public static S2CConstructionBoundsPacket decode(FriendlyByteBuf buffer) {
        return new S2CConstructionBoundsPacket(buffer.readMap(FriendlyByteBuf::readResourceLocation, input ->
                new SectionBounds(input.readList(data -> {
                    String name = data.readUtf();
                    Vec3 min = readVector(data);
                    Vec3 max = readVector(data);
                    return new SectionBounds.Section(name, new LocalBox(min.x, min.y, min.z, max.x, max.y, max.z),
                            data.readList(partData -> new OrientedBox(readVector(partData), readVector(partData),
                                    new Rotation3(partData.readDouble(), partData.readDouble(), partData.readDouble(),
                                            partData.readDouble(), partData.readDouble(), partData.readDouble(),
                                            partData.readDouble(), partData.readDouble(), partData.readDouble()))));
                }))));
    }

    public static void handle(S2CConstructionBoundsPacket packet, NetworkManager.PacketContext context) {
        context.queue(() -> SectionBoundsCatalog.applySynced(packet.bounds));
    }

    private static void writeVector(FriendlyByteBuf buffer, Vec3 value) {
        buffer.writeDouble(value.x);
        buffer.writeDouble(value.y);
        buffer.writeDouble(value.z);
    }

    private static Vec3 readVector(FriendlyByteBuf buffer) {
        return new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
    }
}
