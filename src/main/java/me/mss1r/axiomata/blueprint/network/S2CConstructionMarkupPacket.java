package me.mss1r.axiomata.blueprint.network;

import dev.architectury.networking.NetworkManager;
import me.mss1r.axiomata.ResourceIds;
import me.mss1r.axiomata.blueprint.BlueprintModule;
import me.mss1r.axiomata.blueprint.internal.construction.ConstructionMarkupCatalog;
import me.mss1r.axiomata.collision.OrientedBox;
import me.mss1r.axiomata.collision.Rotation3;
import me.mss1r.axiomata.geometry.LocalBox;
import me.mss1r.axiomata.structure.ConstructionMarkup;
import me.mss1r.axiomata.structure.SectionBounds;
import me.mss1r.axiomata.structure.StructureSections;
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
/*public record S2CConstructionMarkupPacket(Map<ResourceLocation, ConstructionMarkup> markup) {
*///?} else {
public record S2CConstructionMarkupPacket(Map<ResourceLocation, ConstructionMarkup> markup) implements CustomPacketPayload {
    public static final Type<S2CConstructionMarkupPacket> TYPE = new Type<>(
            ResourceIds.id(BlueprintModule.MOD_ID, "construction_markup"));
    public static final StreamCodec<RegistryFriendlyByteBuf, S2CConstructionMarkupPacket> STREAM_CODEC =
            StreamCodec.ofMember(S2CConstructionMarkupPacket::write, S2CConstructionMarkupPacket::decode);

    private void write(RegistryFriendlyByteBuf buffer) {
        encode(this, buffer);
    }

    @Override
    public Type<S2CConstructionMarkupPacket> type() {
        return TYPE;
    }
    //?}

    public S2CConstructionMarkupPacket {
        markup = Map.copyOf(markup);
    }

    public static void encode(S2CConstructionMarkupPacket packet, FriendlyByteBuf buffer) {
        buffer.writeMap(packet.markup, FriendlyByteBuf::writeResourceLocation, (out, markup) -> {
            out.writeCollection(markup.bounds().sections(), (data, section) -> {
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
            });
            out.writeBoolean(markup.cubes() != null);
            if (markup.cubes() != null) {
                out.writeMap(markup.cubes().expectedPartCounts(), FriendlyByteBuf::writeUtf, FriendlyByteBuf::writeVarInt);
                out.writeCollection(markup.cubes().sections(), (data, section) -> {
                    data.writeUtf(section.name());
                    data.writeMap(section.parts(), FriendlyByteBuf::writeUtf,
                            (indices, cubes) -> indices.writeCollection(cubes, FriendlyByteBuf::writeVarInt));
                });
            }
        });
    }

    public static S2CConstructionMarkupPacket decode(FriendlyByteBuf buffer) {
        return new S2CConstructionMarkupPacket(buffer.readMap(FriendlyByteBuf::readResourceLocation, input -> {
            SectionBounds bounds = new SectionBounds(input.readList(data -> {
                String name = data.readUtf();
                Vec3 min = readVector(data);
                Vec3 max = readVector(data);
                return new SectionBounds.Section(name, new LocalBox(min.x, min.y, min.z, max.x, max.y, max.z),
                        data.readList(partData -> new OrientedBox(readVector(partData), readVector(partData),
                                new Rotation3(partData.readDouble(), partData.readDouble(), partData.readDouble(),
                                        partData.readDouble(), partData.readDouble(), partData.readDouble(),
                                        partData.readDouble(), partData.readDouble(), partData.readDouble()))));
            }));
            StructureSections cubes = input.readBoolean()
                    ? new StructureSections(input.readMap(FriendlyByteBuf::readUtf, FriendlyByteBuf::readVarInt),
                            input.readList(data -> new StructureSections.Section(data.readUtf(),
                                    data.readMap(FriendlyByteBuf::readUtf, indices -> indices.readList(FriendlyByteBuf::readVarInt)))))
                    : null;
            return new ConstructionMarkup(bounds, cubes);
        }));
    }

    public static void handle(S2CConstructionMarkupPacket packet, NetworkManager.PacketContext context) {
        context.queue(() -> ConstructionMarkupCatalog.applySynced(packet.markup));
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
