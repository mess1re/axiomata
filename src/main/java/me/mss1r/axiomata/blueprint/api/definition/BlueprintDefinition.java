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

/**
 * A blueprint: what it builds, and the stages it is built in, each taking its own materials and blows of the hammer.
 * See {@code docs/BLUEPRINTS.md} for the file format.
 */
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

    /** How many stages must stand before the build may end where it is; zero when every stage must be built. */
    public int minStages() {
        return minStages;
    }

    @Nullable
    public Starter starter() {
        return starter;
    }

    /** The drawing this blueprint is traced from: its own, or another blueprint's it names. */
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

    /** Whether the build may end before its last stage, at any stage from {@link #minStages()} on. */
    public boolean isExtendable() {
        return minStages > 0 && minStages < stageCount();
    }

    /** Everything the build takes from stage {@code from} on, each material once with its whole count. */
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
     * What a blueprint makes: an item, which a build in the world places as {@code entity} or as whatever the item
     * itself places; {@code data} is written into the item as whole numbers.
     */
    public record Result(ResourceLocation item, int count, Map<String, Integer> data, @Nullable Placement placement,
                         @Nullable ResourceLocation entity) {
        public Result {
            data = Map.copyOf(data);
        }
    }

    /** One stage of a build: the part of the model it raises, the blows it takes, and its materials. */
    public record Stage(String section, int hits, List<Material> materials, Map<String, Integer> adds) {
        public Stage {
            materials = List.copyOf(materials);
            adds = Map.copyOf(adds);
        }
    }

    /** An item that stands in for a drawn blueprint, being itself the first {@code builtStages} stages. */
    public record Starter(ResourceLocation item, int builtStages) {
    }

    /** A material: one item, or any item of a tag, and how many of it. */
    public record Material(@Nullable ResourceLocation item, @Nullable ResourceLocation tag, int count) {
        public static Material ofItem(ResourceLocation item, int count) {
            return new Material(item, null, count);
        }

        public static Material ofTag(ResourceLocation tag, int count) {
            return new Material(null, tag, count);
        }

        /** How the material is written in a blueprint: an item id, or a tag id after {@code #}. */
        public String key() {
            return item != null ? item.toString() : "#" + tag;
        }

        public Material withCount(int count) {
            return new Material(item, tag, count);
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

        /** The item that shows this material: itself, or the first item of its tag. */
        public ItemStack displayStack() {
            Item shown = Items.BARRIER;
            if (item != null) {
                shown = BuiltInRegistries.ITEM.get(item);
            } else if (tag != null) {
                shown = BuiltInRegistries.ITEM.getTag(TagKey.create(Registries.ITEM, tag))
                        .flatMap(set -> set.stream().findFirst())
                        .map(holder -> holder.value())
                        .orElse(Items.BARRIER);
            }
            return new ItemStack(shown, Math.max(1, count));
        }

        /** Its name for a message: the item's, or for a tag "any" of the item that shows it. */
        public Component displayName() {
            Component shown = displayStack().getHoverName();
            return item != null ? shown : Component.translatable("gui.axiomata.any_of", shown);
        }
    }
}
