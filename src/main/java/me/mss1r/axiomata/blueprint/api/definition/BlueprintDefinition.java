package me.mss1r.axiomata.blueprint.api.definition;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Immutable blueprint definition. File format: https://github.com/mess1re/axiomata/wiki/Blueprint-Data */
public final class BlueprintDefinition {
    public static final int FORMAT_VERSION = 3;

    private final Result result;
    private final List<Stage> stages;
    private final int minStages;
    @Nullable
    private final Starter starter;
    @Nullable
    private final ResourceLocation outline;

    public BlueprintDefinition(Result result, List<Stage> stages, int minStages, @Nullable Starter starter,
                               @Nullable ResourceLocation outline) {
        this.result = result;
        this.stages = List.copyOf(stages);
        this.minStages = minStages;
        this.starter = starter;
        this.outline = outline;
    }

    public Result result() {
        return result;
    }

    public List<Stage> stages() {
        return stages;
    }

    /** Stages required before the build can be ended early; 0 means every stage is required. */
    public int minStages() {
        return minStages;
    }

    @Nullable
    public Starter starter() {
        return starter;
    }

    /** Outline id for the drawing table: {@code outline} if set, otherwise the blueprint id. */
    public String outlineId(String blueprintId) {
        return outline != null ? outline.toString() : blueprintId;
    }

    @Nullable
    public ResourceLocation outline() {
        return outline;
    }

    public boolean buildsInWorld() {
        return result.placement() != null;
    }

    public int stageCount() {
        return stages.size();
    }

    /** True when the build can be ended early, from {@link #minStages()} on. */
    public boolean isExtendable() {
        return minStages > 0 && minStages < stageCount();
    }

    /** Total materials of the stages from {@code from} onward, merged by key. */
    public List<Material> materialsFrom(int from) {
        Map<String, Material> totals = new LinkedHashMap<>();
        for (int index = Math.max(0, from); index < stages.size(); index++) {
            for (Material material : stages.get(index).materials()) {
                totals.merge(material.key(), material, (have, more) -> have.withCount(have.count() + more.count()));
            }
        }
        return new ArrayList<>(totals.values());
    }

    public List<Material> totals() {
        return materialsFrom(0);
    }

    public enum Placement {
        GROUND, WATER
    }

    /**
     * {@code entity} and {@code placement} are set for builds placed in the world. {@code data} is written to the item
     * as integers.
     */
    public record Result(ResourceLocation item, int count, Map<String, Integer> data, @Nullable Placement placement,
                         @Nullable ResourceLocation entity) {
        public Result {
            data = Map.copyOf(data);
        }
    }

    public record Stage(String section, int hits, List<Material> materials, Map<String, Integer> adds) {
        public Stage {
            materials = List.copyOf(materials);
            adds = Map.copyOf(adds);
        }
    }

    /** Item that starts this build and counts as its first {@code builtStages} stages. */
    public record Starter(ResourceLocation item, int builtStages) {
    }

    /**
     * An item or an item tag, with a count. {@code returns} is the item a tag material is given back as, when nothing
     * records which items of the tag were paid.
     */
    public record Material(@Nullable ResourceLocation item, @Nullable ResourceLocation tag, int count,
                           @Nullable ResourceLocation returns) {
        public Material(@Nullable ResourceLocation item, @Nullable ResourceLocation tag, int count) {
            this(item, tag, count, null);
        }

        public static Material ofItem(ResourceLocation item, int count) {
            return new Material(item, null, count);
        }

        public static Material ofTag(ResourceLocation tag, int count) {
            return new Material(null, tag, count);
        }

        /** Key as written in the file: an item id, or a tag id prefixed with {@code #}. */
        public String key() {
            return item != null ? item.toString() : "#" + tag;
        }

        public Material withCount(int count) {
            return new Material(item, tag, count, returns);
        }

        public Material withReturns(@Nullable ResourceLocation returns) {
            return new Material(item, tag, count, returns);
        }

        public boolean matches(ItemStack stack) {
            if (stack.isEmpty()) {
                return false;
            }
            if (item != null) {
                return item.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()));
            }
            return tag != null && stack.is(TagKey.create(Registries.ITEM, tag));
        }

        /** Item to show: the item itself, or each item of the tag in turn, one per second. */
        public ItemStack displayStack() {
            Item shown = Items.BARRIER;
            if (item != null) {
                shown = BuiltInRegistries.ITEM.get(item);
            } else {
                List<Item> choices = tagItems();
                if (!choices.isEmpty()) {
                    shown = choices.get((int) (System.currentTimeMillis() / 1000L % choices.size()));
                }
            }
            return new ItemStack(shown, Math.max(1, count));
        }

        /** Item to give back: the item itself, {@code returns}, or the first item of the tag. */
        public ItemStack returnStack() {
            Item given = Items.BARRIER;
            if (item != null) {
                given = BuiltInRegistries.ITEM.get(item);
            } else if (returns != null) {
                given = BuiltInRegistries.ITEM.get(returns);
            } else {
                List<Item> choices = tagItems();
                if (!choices.isEmpty()) {
                    given = choices.get(0);
                }
            }
            return new ItemStack(given, Math.max(1, count));
        }

        private List<Item> tagItems() {
            if (tag == null) {
                return List.of();
            }
            return BuiltInRegistries.ITEM.getTag(TagKey.create(Registries.ITEM, tag))
                    .map(set -> set.stream().map(holder -> holder.value()).toList())
                    .orElse(List.of());
        }

        /** Display name: the item's name, or "any of" for a tag. */
        public Component displayName() {
            Component shown = displayStack().getHoverName();
            return item != null ? shown : Component.translatable("gui.axiomata.any_of", shown);
        }
    }
}
