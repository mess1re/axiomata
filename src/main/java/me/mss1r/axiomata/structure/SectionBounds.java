package me.mss1r.axiomata.structure;

import me.mss1r.axiomata.geometry.LocalBox;

import java.util.List;
import java.util.Optional;

public record SectionBounds(List<Section> sections) {
    public SectionBounds {
        sections = List.copyOf(sections);
    }

    public Optional<Section> find(String name) {
        return sections.stream().filter(section -> section.name().equals(name)).findFirst();
    }

    public record Section(String name, LocalBox bounds) {
        public Section {
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("Section name must not be blank");
            }
        }
    }
}

