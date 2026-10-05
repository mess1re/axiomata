package me.mss1r.axiomata.structure;

import me.mss1r.axiomata.geometry.LocalBox;
import me.mss1r.axiomata.collision.OrientedBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.function.Function;

public record SectionBounds(List<Section> sections) {
    public SectionBounds {
        sections = List.copyOf(sections);
    }

    public Optional<Section> find(String name) {
        return sections.stream().filter(section -> section.name().equals(name)).findFirst();
    }

    /**
     * The visible section a segment hits first, with how far along: by its cubes, or, when it passes between every
     * cube, by the box around a section's cubes, so that the gaps of an open frame such as a ladder still count.
     */
    public Optional<Hit> firstHit(Vec3 start, Vec3 end, Set<String> visible, double marginPixels) {
        Optional<Hit> cube = closest(visible, section -> section.clip(start, end, marginPixels));
        return cube.isPresent() ? cube : closest(visible, section -> section.clipEnvelope(start, end, marginPixels));
    }

    private Optional<Hit> closest(Set<String> visible, Function<Section, OptionalDouble> clip) {
        Hit closest = null;
        for (Section section : sections) {
            if (!visible.contains(section.name())) {
                continue;
            }
            OptionalDouble hit = clip.apply(section);
            if (hit.isPresent() && (closest == null || hit.getAsDouble() < closest.distance())) {
                closest = new Hit(section, hit.getAsDouble());
            }
        }
        return Optional.ofNullable(closest);
    }

    /** {@code distance} is the squared fraction of the segment before the hit. */
    public record Hit(Section section, double distance) {
        public String name() {
            return section.name();
        }
    }

    public record Section(String name, LocalBox bounds, List<OrientedBox> parts) {
        public Section(String name, LocalBox bounds) {
            this(name, bounds, List.of());
        }

        public Section {
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("Section name must not be blank");
            }
            parts = List.copyOf(parts);
        }

        /** Segment endpoints and cube parts are in entity-local blocks; legacy bounds are model pixels. */
        public OptionalDouble clip(Vec3 start, Vec3 end, double marginPixels) {
            if (parts.isEmpty()) {
                Vec3 from = new Vec3(-start.x * 16, start.y * 16, start.z * 16);
                Vec3 to = new Vec3(-end.x * 16, end.y * 16, end.z * 16);
                return clipBox(new AABB(bounds.minX(), bounds.minY(), bounds.minZ(),
                        bounds.maxX(), bounds.maxY(), bounds.maxZ()).inflate(Math.max(0, marginPixels)), from, to);
            }
            double closest = Double.POSITIVE_INFINITY;
            double margin = Math.max(0, marginPixels) / 16;
            for (OrientedBox part : parts) {
                Vec3 from = part.rotation().transformInverse(start.subtract(part.center()));
                Vec3 to = part.rotation().transformInverse(end.subtract(part.center()));
                Vec3 half = part.halfExtent().add(margin, margin, margin);
                OptionalDouble hit = clipBox(new AABB(-half.x, -half.y, -half.z, half.x, half.y, half.z), from, to);
                if (hit.isPresent()) {
                    closest = Math.min(closest, hit.getAsDouble());
                }
            }
            return Double.isFinite(closest) ? OptionalDouble.of(closest) : OptionalDouble.empty();
        }

        /** Like {@link #clip}, against one box around all the section's cubes. */
        public OptionalDouble clipEnvelope(Vec3 start, Vec3 end, double marginPixels) {
            if (parts.isEmpty()) {
                return clip(start, end, marginPixels);
            }
            AABB envelope = parts.get(0).enclosingBounds();
            for (OrientedBox part : parts) {
                envelope = envelope.minmax(part.enclosingBounds());
            }
            return clipBox(envelope.inflate(Math.max(0, marginPixels) / 16), start, end);
        }

        private static OptionalDouble clipBox(AABB box, Vec3 start, Vec3 end) {
            if (box.contains(start)) {
                return OptionalDouble.of(0);
            }
            return box.clip(start, end).map(hit -> OptionalDouble.of(start.distanceToSqr(hit)
                    / start.distanceToSqr(end))).orElseGet(OptionalDouble::empty);
        }
    }
}

