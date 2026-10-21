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
    @Nullable
    private final ResourceLocation outline;

    public BlueprintDefinition(Result result, List<Stage> stages, @Nullable ResourceLocation outline) {
        this.result = result;
        this.stages = List.copyOf(stages);
        this.outline = outline;
    }

    public Result result() {
        return result;
    }

    public List<Stage> stages() {
        return stages;
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

    /** Total materials of every stage, merged by key. */
    public List<Material> totals() {
        Map<String, Material> totals = new LinkedHashMap<>();
        for (Stage stage : stages) {
            for (Material material : stage.materials()) {
                totals.merge(material.key(), material, (have, more) -> have.withCount(have.count() + more.count()));
            }
        }
        return new ArrayList<>(totals.values());
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

    public record Stage(String section, int hits, List<Material> materials) {
        public Stage {
            materials = List.copyOf(materials);
        }
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

        /** Tag names use the tag.item.namespace.path translation convention; unknown tags keep their id. */
        public Component displayName() {
            if (item != null) return displayStack().getHoverName();
            String key = "tag.item." + tag.getNamespace() + "." + tag.getPath().replace('/', '.');
            return Component.translatable("gui.axiomata.any_of",
                    Component.translatableWithFallback(key, "#" + tag));
        }
    }
}
