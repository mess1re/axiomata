package me.mss1r.axiomata;

import net.minecraft.resources.ResourceLocation;

/**
 * Creates resource locations in a way every supported version accepts. Forge only added the 1.21 factories in 47.3, and
 * OptiFine replaces the class without them, so 1.20.1 uses the constructor.
 */
public final class ResourceIds {
    private ResourceIds() {
    }

    @SuppressWarnings("removal")
    public static ResourceLocation id(String namespace, String path) {
        //? if forge {
        /*return new ResourceLocation(namespace, path);
        *///?} else {
        return ResourceLocation.fromNamespaceAndPath(namespace, path);
        //?}
    }

    @SuppressWarnings("removal")
    public static ResourceLocation parse(String location) {
        //? if forge {
        /*return new ResourceLocation(location);
        *///?} else {
        return ResourceLocation.parse(location);
        //?}
    }
}
