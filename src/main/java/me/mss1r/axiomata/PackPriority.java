package me.mss1r.axiomata;

import net.minecraft.server.packs.resources.Resource;

import java.util.ArrayList;
import java.util.List;

/**
 * Orders a resource stack so files from any datapack override files shipped inside mod jars. Some loaders put the
 * mod data pack above world datapacks, which would otherwise make a mod's own files impossible to override.
 */
public final class PackPriority {
    private PackPriority() {
    }

    /** The stack lowest first, as {@code listResourceStacks} returns it, with mod-jar resources moved to the bottom. */
    public static List<Resource> datapacksOverMods(List<Resource> stack) {
        List<Resource> ordered = new ArrayList<>(stack.size());
        stack.stream().filter(resource -> isModJar(resource.sourcePackId())).forEach(ordered::add);
        stack.stream().filter(resource -> !isModJar(resource.sourcePackId())).forEach(ordered::add);
        return ordered;
    }

    /** Forge names mod packs {@code mod:<id>}, NeoForge {@code mod/<id>} under its {@code mod_data} pack. */
    public static boolean isModJar(String packId) {
        return packId.startsWith("mod:") || packId.startsWith("mod/") || packId.equals("mod_data");
    }
}
