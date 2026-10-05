package me.mss1r.axiomata.blueprint.api.visual;

import me.mss1r.axiomata.blueprint.internal.construction.ConstructionMarkupCatalog;
import me.mss1r.axiomata.blueprint.internal.construction.StageNames;
import me.mss1r.axiomata.blueprint.api.definition.BlueprintDefinition;
import me.mss1r.axiomata.blueprint.api.definition.BlueprintDefinitions;
import me.mss1r.axiomata.structure.ConstructionMarkup;
import me.mss1r.axiomata.structure.SectionBounds;
import me.mss1r.axiomata.structure.StructureSections;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public final class BlueprintConstructionVisuals {
    public record State(Set<String> builtSections, String activeSection,
                        int activeStage, int stageCount) {
    }

    private BlueprintConstructionVisuals() {
    }

    public static StructureSections sections(ResourceLocation model) {
        ConstructionMarkup markup = ConstructionMarkupCatalog.get(model);
        return markup == null ? null : markup.cubes();
    }

    public static SectionBounds bounds(ResourceLocation model) {
        ConstructionMarkup markup = ConstructionMarkupCatalog.get(model);
        return markup == null ? null : markup.bounds();
    }

    public static Component stageName(String blueprintId, String section) {
        return StageNames.of(blueprintId, section);
    }

    public static State state(String blueprintId, int completedStages) {
        BlueprintDefinition definition = BlueprintDefinitions.get(blueprintId);
        if (definition == null || definition.stages().isEmpty()) {
            return new State(Set.of(), "", 0, 0);
        }

        int completed = Math.max(0, Math.min(completedStages, definition.stageCount()));
        Set<String> built = new LinkedHashSet<>();
        for (int index = 0; index < completed; index++) {
            String section = definition.stages().get(index).section();
            if (!section.isEmpty()) {
                built.add(section);
            }
        }

        String active = completed < definition.stageCount()
                ? definition.stages().get(completed).section()
                : "";
        return new State(
                Collections.unmodifiableSet(built),
                active,
                completed,
                definition.stageCount());
    }

}
