package me.mss1r.axiomata.blueprint.api.visual;

import me.mss1r.axiomata.blueprint.internal.construction.BuildSectionCatalog;
import me.mss1r.axiomata.blueprint.internal.construction.SectionBoundsCatalog;
import me.mss1r.axiomata.blueprint.internal.construction.StageNames;
import me.mss1r.axiomata.blueprint.api.definition.BlueprintDefinition;
import me.mss1r.axiomata.blueprint.api.definition.BlueprintDefinitions;
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
        return BuildSectionCatalog.get(model);
    }

    public static SectionBounds bounds(ResourceLocation model) {
        return SectionBoundsCatalog.get(model);
    }

    public static Component stageName(String blueprintId, String section) {
        return StageNames.of(blueprintId, section);
    }

    public static State state(String blueprintId, int completedStages) {
        BlueprintDefinition definition = BlueprintDefinitions.get(blueprintId);
        if (definition == null || definition.construction == null || definition.construction.isEmpty()) {
            return new State(Set.of(), "", 0, 0);
        }

        int completed = Math.max(0, Math.min(completedStages, definition.construction.size()));
        Set<String> built = new LinkedHashSet<>();
        for (int index = 0; index < completed; index++) {
            String section = sectionName(definition.construction.get(index));
            if (!section.isEmpty()) {
                built.add(section);
            }
        }

        String active = completed < definition.construction.size()
                ? sectionName(definition.construction.get(completed))
                : "";
        return new State(
                Collections.unmodifiableSet(built),
                active,
                completed,
                definition.construction.size());
    }

    private static String sectionName(BlueprintDefinition.StageSpec stage) {
        return stage == null || stage.section == null ? "" : stage.section;
    }
}
