package me.mss1r.axiomata.blueprint.client.renderer;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import me.mss1r.axiomata.ResourceIds;
import me.mss1r.axiomata.blueprint.BlueprintModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * White copies of the outline drawings. The drawings are black, and a tint only multiplies a texture's colour, so
 * ink is drawn from a white copy that keeps the drawing's line and its edges.
 */
public final class InkTextures {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<ResourceLocation, ResourceLocation> WHITE = new HashMap<>();

    private InkTextures() {
    }

    public static ResourceLocation white(ResourceLocation drawing) {
        return WHITE.computeIfAbsent(drawing, InkTextures::whiten);
    }

    /** Drops the copies, e.g. when the screen opens again after resource packs changed. */
    public static void clear() {
        WHITE.values().forEach(Minecraft.getInstance().getTextureManager()::release);
        WHITE.clear();
    }

    private static ResourceLocation whiten(ResourceLocation drawing) {
        Minecraft minecraft = Minecraft.getInstance();
        try (InputStream stream = minecraft.getResourceManager().open(drawing)) {
            NativeImage image = NativeImage.read(stream);
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    image.setPixelRGBA(x, y, image.getPixelRGBA(x, y) | 0x00FFFFFF);
                }
            }
            ResourceLocation white = ResourceIds.id(BlueprintModule.MOD_ID,
                    "ink/" + drawing.getNamespace() + "/" + drawing.getPath());
            minecraft.getTextureManager().register(white, new DynamicTexture(image));
            return white;
        } catch (IOException exception) {
            LOGGER.warn("Could not read outline drawing {}; ink is drawn from it as it is", drawing, exception);
            return drawing;
        }
    }
}
