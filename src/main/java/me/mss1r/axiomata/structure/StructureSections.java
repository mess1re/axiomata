package me.mss1r.axiomata.structure;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

// Section files include the exported cube counts. If a model changes without being re-exported,
// rendering no sections is safer than assigning stale cube indices to the wrong stage.
public record StructureSections(Map<String, Integer> expectedPartCounts, List<Section> sections) {
    public StructureSections {
        expectedPartCounts = Map.copyOf(new LinkedHashMap<>(expectedPartCounts));
        sections = List.copyOf(sections);
    }

    public boolean matches(Map<String, Integer> loadedPartCounts) {
        Objects.requireNonNull(loadedPartCounts, "loadedPartCounts");
        for (Map.Entry<String, Integer> entry : expectedPartCounts.entrySet()) {
            if (!entry.getValue().equals(loadedPartCounts.get(entry.getKey()))) {
                return false;
            }
        }
        return true;
    }

    public record Section(String name, Map<String, List<Integer>> parts) {
        public Section {
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("Section name must not be blank");
            }
            Map<String, List<Integer>> copied = new LinkedHashMap<>();
            parts.forEach((owner, indices) -> copied.put(owner, List.copyOf(indices)));
            parts = Map.copyOf(copied);
        }

        public boolean contains(String owner, int partIndex) {
            List<Integer> indices = parts.get(owner);
            return indices != null && indices.contains(partIndex);
        }
    }
}

