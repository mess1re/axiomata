package me.mss1r.axiomata.blueprint.menu;

public final class DrawingTableLayout {
    public static final int WIDTH = 640;
    public static final int HEIGHT = 320;

    public static final int PANEL_BORDER = 3;

    public static final int LIST_PANEL_X = 49;
    public static final int LIST_PANEL_Y = 49;
    public static final int LIST_PANEL_WIDTH = 111;
    public static final int LIST_PANEL_HEIGHT = 221;
    public static final int LIST_ROW_HEIGHT = 19;

    public static final int MATERIALS_PANEL_X = 479;
    public static final int MATERIALS_PANEL_Y = 49;
    public static final int MATERIALS_PANEL_WIDTH = 111;
    public static final int MATERIALS_PANEL_HEIGHT = 110;
    public static final int MATERIALS_COLUMNS = 5;
    public static final int MATERIALS_SPACING = 20;

    public static final int SHEET_SIZE = 256;
    public static final int SHEET_X = (WIDTH - SHEET_SIZE) / 2;
    public static final int SHEET_Y = (HEIGHT - SHEET_SIZE) / 2;

    public static final int FRAME_SIZE = 22;
    public static final int FRAME_INSET = 3;

    public static final int SLOT_FRAME_Y = 199;
    public static final int PAPER_FRAME_X = 479;
    public static final int INK_FRAME_X = 505;
    public static final int RESULT_FRAME_X = 568;

    public static final int ARROW_X = 536;
    public static final int ARROW_WIDTH = 22;
    public static final int ARROW_HEIGHT = 15;
    public static final int ARROW_Y = SLOT_FRAME_Y + (FRAME_SIZE - ARROW_HEIGHT) / 2;

    public static final int GROOVE_X = 479;
    public static final int GROOVE_WIDTH = 111;
    public static final int GROOVE_HEIGHT = 10;
    public static final int PROGRESS_GROOVE_Y = 240;
    public static final int WANDER_GROOVE_Y = 260;

    public static final int FILL_INSET = 1;
    public static final int FILL_WIDTH = GROOVE_WIDTH - FILL_INSET * 2;
    public static final int FILL_HEIGHT = GROOVE_HEIGHT - FILL_INSET * 2;

    public static int slotX(int frameX) {
        return frameX + FRAME_INSET;
    }

    public static int slotY() {
        return SLOT_FRAME_Y + FRAME_INSET;
    }

    public static int listContentX() {
        return LIST_PANEL_X + PANEL_BORDER;
    }

    public static int listContentY() {
        return LIST_PANEL_Y + PANEL_BORDER;
    }

    public static int listContentWidth() {
        return LIST_PANEL_WIDTH - PANEL_BORDER * 2;
    }

    public static int listRows() {
        return (LIST_PANEL_HEIGHT - PANEL_BORDER * 2) / LIST_ROW_HEIGHT;
    }

    private DrawingTableLayout() {
    }
}
