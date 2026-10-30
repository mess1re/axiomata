package me.mss1r.axiomata.blueprint.api.definition;

import me.mss1r.axiomata.blueprint.internal.definition.BlueprintDefinitionRepository;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public final class BlueprintDefinitions {
    private BlueprintDefinitions() {
    }

    @Nullable
    public static BlueprintDefinition get(String id) {
        return BlueprintDefinitionRepository.get(id);
    }

    /** The id a blueprint has now, for an id saved before the blueprint was renamed (see its {@code aliases}). */
    public static String canonicalId(String id) {
        return BlueprintDefinitionRepository.canonicalId(id);
    }

    public static Map<String, BlueprintDefinition> allById() {
        return BlueprintDefinitionRepository.allById();
    }

    public static List<BlueprintDefinition> all() {
        return BlueprintDefinitionRepository.all();
    }
}
