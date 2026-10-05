package me.mss1r.axiomata.structure;

import org.jetbrains.annotations.Nullable;

/**
 * Everything one {@code construction/<model>.json} file says about a model: where each section can be hit, and which
 * cubes the client draws for it. Files written before the cube lists existed have no {@code cubes}.
 */
public record ConstructionMarkup(SectionBounds bounds, @Nullable StructureSections cubes) {
}
