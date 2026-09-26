package me.mss1r.axiomata.blueprint.tracing;

import net.minecraft.resources.ResourceLocation;

public final class BlueprintOutline {
    private final ResourceLocation texture;
    private final OutlineMask mask;
    private OutlineField field;

    public BlueprintOutline(ResourceLocation texture, OutlineMask mask) {
        this.texture = texture;
        this.mask = mask;
    }

    public ResourceLocation texture() {
        return texture;
    }

    public OutlineMask mask() {
        return mask;
    }

    public synchronized OutlineField field() {
        if (field == null) {
            field = new OutlineField(mask);
        }
        return field;
    }

    public TracingSession openSession(int tolerancePixels) {
        return new TracingSession(mask, field(), tolerancePixels);
    }
}
