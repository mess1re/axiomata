package me.mss1r.axiomata.blueprint.internal.construction;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class StageNames {
    private StageNames() {
    }

    public static Component of(String blueprintId, String section) {
        if (section == null || section.isBlank()) {
            return Component.empty();
        }
        ResourceLocation blueprint = ResourceLocation.tryParse(blueprintId);
        String prefix = blueprint == null
                ? "minecraft.unknown"
                : blueprint.getNamespace() + "." + blueprint.getPath().replace('/', '.');
        return Component.translatableWithFallback("stage." + prefix + "." + section, section);
    }
}
