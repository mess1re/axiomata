package me.mss1r.axiomata.blueprint.integration.jei;

import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import me.mss1r.axiomata.blueprint.client.screen.DrawingTableScreen;
import net.minecraft.client.renderer.Rect2i;

import java.util.List;

public class TableGuiHandler implements IGuiContainerHandler<DrawingTableScreen> {

    @Override
    public List<Rect2i> getGuiExtraAreas(DrawingTableScreen screen) {
        return List.of(new Rect2i(0, 0, screen.width, screen.height));
    }
}
