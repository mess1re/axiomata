package me.mss1r.axiomata.blueprint.api.definition;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class BlueprintDefinition {
    public Map<String, IngredientSpec> key = new LinkedHashMap<>();
    public ResultSpec result;
    public List<StageSpec> construction;
    /**
     * How many stages must stand before the build may be ended where it is, leaving the later stages unbuilt; zero
     * when every stage must be built.
     */
    public int min_stages;
    /** An item that stands in for a drawn blueprint, being itself the first stages already built. */
    public StarterSpec starter;

    public boolean buildsInWorld() {
        return result != null && result.deployment != null;
    }

    public int stageCount() {
        return construction == null ? 0 : construction.size();
    }

    /** Whether the build may end before its last stage, at any stage from {@link #min_stages} on. */
    public boolean isExtendable() {
        return min_stages > 0 && min_stages < stageCount();
    }

    public static final class StarterSpec {
        public String item;
        // The leading stages the starter item itself is. Stages beyond them count as built too when the item's
        // data already holds what they add, so a finished build taken up again carries on where it stopped.
        public int built_stages = 1;
    }

    public static final class StagePartSpec {
        public String key;
        // Zero means use the full count declared by the matching ingredient.
        public int count;
    }

    public static final class StageSpec {
        public String section;
        public String key;
        public List<String> keys;
        public List<StagePartSpec> materials;
        // Zero lets the construction plan derive work from the stage's material count.
        public int hits;
        // What building this stage adds to the result's data, for a build that may end before its last stage.
        public Map<String, Integer> adds;

        public List<StagePartSpec> parts() {
            if (materials != null && !materials.isEmpty()) {
                return materials;
            }
            List<StagePartSpec> whole = new ArrayList<>();
            for (String symbol : keys != null && !keys.isEmpty()
                    ? keys
                    : (key == null || key.isBlank() ? List.<String>of() : List.of(key))) {
                StagePartSpec part = new StagePartSpec();
                part.key = symbol;
                whole.add(part);
            }
            return whole;
        }
    }

    public static final class IngredientSpec {
        public String item;
        public int count = 1;

        @Override
        public boolean equals(Object object) {
            return object instanceof IngredientSpec other
                    && Objects.equals(item, other.item)
                    && count == other.count;
        }

        @Override
        public int hashCode() {
            return Objects.hash(item, count);
        }
    }

    public static final class ResultSpec {
        public String item;
        public int count = 1;
        public Map<String, Integer> custom_data = new LinkedHashMap<>();
        public DeploymentSpec deployment;

        @Override
        public boolean equals(Object object) {
            return object instanceof ResultSpec other
                    && Objects.equals(item, other.item)
                    && count == other.count
                    && Objects.equals(custom_data, other.custom_data)
                    && Objects.equals(deployment, other.deployment);
        }

        @Override
        public int hashCode() {
            return Objects.hash(item, count, custom_data, deployment);
        }
    }

    public static final class DeploymentSpec {
        public String mode = "ground";
        public String preview_entity;

        public boolean isWater() {
            return "water".equalsIgnoreCase(mode);
        }

        public boolean isValid() {
            if (!("ground".equalsIgnoreCase(mode) || "water".equalsIgnoreCase(mode))) {
                return false;
            }
            if (isWater() && (preview_entity == null || preview_entity.isBlank())) {
                return false;
            }
            return preview_entity == null || ResourceLocation.tryParse(preview_entity) != null;
        }

        @Override
        public boolean equals(Object object) {
            return object instanceof DeploymentSpec other
                    && Objects.equals(mode, other.mode)
                    && Objects.equals(preview_entity, other.preview_entity);
        }

        @Override
        public int hashCode() {
            return Objects.hash(mode, preview_entity);
        }
    }
}
