package me.mss1r.axiomata.blueprint.client.renderer;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.Util;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Function;

public final class ConstructionHighlightRenderType extends RenderStateShard {
    private static ShaderInstance shader;
    private static final ShaderStateShard SHADER = new ShaderStateShard(() -> shader);

    private static final Function<ResourceLocation, RenderType> TYPES = Util.memoize(texture ->
            RenderType.create(
                    "axiomata:construction_highlight",
                    DefaultVertexFormat.NEW_ENTITY,
                    VertexFormat.Mode.QUADS,
                    256,
                    true,
                    false,
                    RenderType.CompositeState.builder()
                            .setShaderState(SHADER)
                            .setTextureState(new TextureStateShard(texture, false, false))
                            .setTransparencyState(NO_TRANSPARENCY)
                            .setCullState(NO_CULL)
                            .setLightmapState(LIGHTMAP)
                            .setOverlayState(OVERLAY)
                            .setWriteMaskState(COLOR_DEPTH_WRITE)
                            .createCompositeState(true)
            ));

    private ConstructionHighlightRenderType() {
        super(null, null, null);
    }

    public static RenderType of(ResourceLocation texture) {
        return TYPES.apply(texture);
    }

    public static void setShader(ShaderInstance shader) {
        ConstructionHighlightRenderType.shader = shader;
    }
}
