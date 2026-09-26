package me.mss1r.axiomata.blueprint.api.definition;

import me.mss1r.axiomata.blueprint.internal.definition.BlueprintDefinitionCatalog;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public final class BlueprintDefinitions {
    private BlueprintDefinitions() {
    }

    @Nullable
    public static BlueprintDefinition get(String id) {
        return BlueprintDefinitionCatalog.get(id);
    }

    public static Map<String, BlueprintDefinition> allById() {
        return BlueprintDefinitionCatalog.allById();
    }

    public static List<BlueprintDefinition> all() {
        return BlueprintDefinitionCatalog.all();
    }
}
