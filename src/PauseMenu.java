import javax.microedition.lcdui.Graphics;

/**
 * RallyCity pause menu.
 * Handles pause-menu drawing and selection.
 */
public class PauseMenu {
    public static final int RESUME = 0;
    public static final int SOUND = 1;
    public static final int MAP = 2;
    public static final int MAIN_MENU = 3;

    private static final String[] ITEMS = {
        "RESUME",
        "SOUND",
        "MAP",
        "MAIN MENU"
    };

    private int selected;
    private boolean soundOn = true;

    private static final int DARK = 0x101820;
    private static final int PANEL = 0x203545;
    private static final int GOLD = 0xE6A52E;
    private static final int WHITE = 0xFFFFFF;
    private static final int MUTED = 0xAAB8C2;

    public PauseMenu() {
        selected = RESUME;
    }

    public void setSound(boolean enabled) {
        soundOn = enabled;
    }

    public boolean isSoundOn() {
        return soundOn;
    }

    public int getSelected() {
        return selected;
    }

    public void up() {
        selected--;
        if (selected < 0) selected = ITEMS.length - 1;
    }

    public void down() {
        selected++;
        if (selected >= ITEMS.length) selected = 0;
    }

    /**
     * Returns the selected action.
     * SOUND toggles the sound setting and stays in the menu.
     */
    public int confirm() {
        if (selected == SOUND) {
            soundOn = !soundOn;
            return SOUND;
        }

        return selected;
    }

    public void draw(Graphics g, int width, int height) {
        g.setColor(DARK);
        g.fillRect(0, 0, width, height);

        int panelWidth = width - 24;
        if (panelWidth > 220) panelWidth = 220;

        int panelHeight = 190;
        if (panelHeight > height - 20)
            panelHeight = height - 20;

        int left = (width - panelWidth) / 2;
        int top = (height - panelHeight) / 2;

        g.setColor(PANEL);
        g.fillRect(left, top, panelWidth, panelHeight);

        g.setColor(GOLD);
        g.fillRect(left, top, panelWidth, 3);

        g.setColor(WHITE);
        g.drawString("PAUSED", width / 2, top + 17,
                Graphics.TOP | Graphics.HCENTER);

        int itemHeight = 30;
        int startY = top + 49;

        for (int i = 0; i < ITEMS.length; i++) {
            int y = startY + i * itemHeight;

            if (i == selected) {
                g.setColor(GOLD);
                g.fillRect(left + 9, y - 2,
                        panelWidth - 18, 25);
                g.setColor(DARK);
            } else {
                g.setColor(WHITE);
            }

            String label = ITEMS[i];

            if (i == SOUND) {
                label = soundOn ? "SOUND: ON" : "SOUND: OFF";
            }

            g.drawString(label, width / 2, y,
                    Graphics.TOP | Graphics.HCENTER);
        }

        g.setColor(MUTED);
        g.drawString("UP/DOWN - SELECT",
                width / 2, top + panelHeight - 28,
                Graphics.TOP | Graphics.HCENTER);

        g.drawString("FIRE - CONFIRM",
                width / 2, top + panelHeight - 14,
                Graphics.TOP | Graphics.HCENTER);
    }
}
