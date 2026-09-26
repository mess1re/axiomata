package me.mss1r.axiomata.construction;

import java.util.List;

public record ConstructionPlan<M>(List<Stage<M>> stages) {
    public ConstructionPlan {
        stages = List.copyOf(stages);
    }

    public int stageCount() {
        return stages.size();
    }

    public Stage<M> stage(int index) {
        return index >= 0 && index < stages.size() ? stages.get(index) : null;
    }

    public int totalWorkUnits() {
        return stages.stream().mapToInt(Stage::workUnits).sum();
    }

    public record Stage<M>(String section, List<M> materials, int workUnits) {
        public Stage {
            section = section == null ? "" : section;
            materials = List.copyOf(materials);
            if (workUnits < 1) {
                throw new IllegalArgumentException("A construction stage must require at least one work unit");
            }
        }
    }
}

