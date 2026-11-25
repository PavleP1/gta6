
import javax.microedition.lcdui.*;

public class MenuCanvas extends Canvas {
    private RallyMIDlet midlet;
    private int selected;
    private int screen;

    private static final int MENU = 0;
    private static final int CONTROLS = 1;
    private static final int MULTIPLAYER = 2;

    private static final String[] ITEMS = {
        "NEW GAME",
        "CONTINUE",
        "CONTROLS",
        "LOCAL MULTIPLAYER",
        "EXIT"
    };

    private static final int BG = 0x101820;
    private static final int PANEL = 0x172B3A;
    private static final int GOLD = 0xE6A52E;
    private static final int WHITE = 0xFFFFFF;
    private static final int MUTED = 0xAAB8C2;

    public MenuCanvas(RallyMIDlet m) {
        midlet = m;
        selected = 0;
        screen = MENU;
        setFullScreenMode(true);
    }

    protected void paint(Graphics g) {
        int w = getWidth();
        int h = getHeight();

        g.setColor(BG);
        g.fillRect(0, 0, w, h);

        g.setColor(PANEL);
        g.fillRect(0, h / 3, w, h - h / 3);

        g.setColor(GOLD);
        g.fillRect(0, h / 3, w, 2);

        drawHeader(g, w, h);

        if (screen == CONTROLS) {
            drawControls(g, w, h);
        } else if (screen == MULTIPLAYER) {
            drawMultiplayer(g, w, h);
        } else {
            drawMenu(g, w, h);
        }

        g.setColor(0x8999A5);
        g.drawString("RALLYCITY", w / 2, h - 5,
                     Graphics.BOTTOM | Graphics.HCENTER);
    }

    private void drawHeader(Graphics g, int w, int h) {
        int titleY = h / 18;

        g.setColor(WHITE);
        g.drawString("RALLY", w / 2, titleY,
                     Graphics.TOP | Graphics.HCENTER);

        g.setColor(GOLD);
        g.drawString("CITY", w / 2, titleY + 19,
                     Graphics.TOP | Graphics.HCENTER);

        g.setColor(MUTED);
        g.drawString("OPEN WORLD DRIVING", w / 2, titleY + 40,
                     Graphics.TOP | Graphics.HCENTER);
    }

    private void drawMenu(Graphics g, int w, int h) {
        int itemH = h / 10;
        if (itemH > 29) itemH = 29;
        if (itemH < 21) itemH = 21;

        int startY = h / 2 - (ITEMS.length * itemH) / 2 + 5;
        int highlightW = w - 30;

        if (highlightW > 200) highlightW = 200;

        for (int i = 0; i < ITEMS.length; i++) {
            int y = startY + i * itemH;

            if (i == selected) {
                g.setColor(GOLD);
                g.fillRect(w / 2 - highlightW / 2,
                           y - 2, highlightW, itemH - 2);
                g.setColor(0x17202A);
            } else {
                g.setColor(WHITE);
            }

            if (i == 1 && !midlet.hasGame()) {
                g.setColor(0x65727C);
            }

            g.drawString(ITEMS[i], w / 2, y,
                         Graphics.TOP | Graphics.HCENTER);
        }

        g.setColor(MUTED);
        g.drawString("UP / DOWN - SELECT", w / 2, h - 27,
                     Graphics.BOTTOM | Graphics.HCENTER);
        g.drawString("FIRE - CONFIRM", w / 2, h - 15,
                     Graphics.BOTTOM | Graphics.HCENTER);
    }

    private void drawControls(Graphics g, int w, int h) {
        g.setColor(GOLD);
        g.drawString("CONTROLS", w / 2, h / 2 - 61,
                     Graphics.TOP | Graphics.HCENTER);

        g.setColor(WHITE);

        int y = h / 2 - 36;
        int gap = 16;

        g.drawString("2 / W - ACCELERATE", w / 2, y,
                     Graphics.TOP | Graphics.HCENTER);
        g.drawString("8 / S - BRAKE / REVERSE", w / 2, y + gap,
                     Graphics.TOP | Graphics.HCENTER);
        g.drawString("4 / A - STEER LEFT", w / 2, y + gap * 2,
                     Graphics.TOP | Graphics.HCENTER);
        g.drawString("6 / D - STEER RIGHT", w / 2, y + gap * 3,
                     Graphics.TOP | Graphics.HCENTER);
        g.drawString("FIRE / SPACE - HANDBRAKE", w / 2, y + gap * 4,
                     Graphics.TOP | Graphics.HCENTER);
        g.drawString("C - CHANGE CAMERA", w / 2, y + gap * 5,
                     Graphics.TOP | Graphics.HCENTER);
        g.drawString("P - PAUSE   M - SOUND", w / 2, y + gap * 6,
                     Graphics.TOP | Graphics.HCENTER);

        g.setColor(GOLD);
        g.drawString("0 / LEFT - BACK", w / 2, h - 25,
                     Graphics.BOTTOM | Graphics.HCENTER);
    }

    private void drawMultiplayer(Graphics g, int w, int h) {
        int centerY = h / 2;

        g.setColor(GOLD);
        g.drawString("LOCAL MULTIPLAYER", w / 2, centerY - 43,
                     Graphics.TOP | Graphics.HCENTER);

        g.setColor(WHITE);
        g.drawString("COMING SOON", w / 2, centerY - 12,
                     Graphics.TOP | Graphics.HCENTER);

        g.setColor(MUTED);
        g.drawString("LOCAL CONNECTION ONLY", w / 2, centerY + 13,
                     Graphics.TOP | Graphics.HCENTER);
        g.drawString("DEVICE LINK TESTING", w / 2, centerY + 31,
                     Graphics.TOP | Graphics.HCENTER);
        g.drawString("NO ONLINE SERVERS", w / 2, centerY + 49,
                     Graphics.TOP | Graphics.HCENTER);

        g.setColor(GOLD);
        g.drawString("0 / LEFT - BACK", w / 2, h - 25,
                     Graphics.BOTTOM | Graphics.HCENTER);
    }

    protected void keyPressed(int keyCode) {
        int action = getGameAction(keyCode);

        if (screen == CONTROLS || screen == MULTIPLAYER) {
            if (keyCode == '0' || action == FIRE ||
                action == LEFT || action == GAME_A) {
                screen = MENU;
                repaint();
            }
            return;
        }

        if (action == UP || keyCode == '2') {
            selected--;
            if (selected < 0) selected = ITEMS.length - 1;
            repaint();
        } else if (action == DOWN || keyCode == '8') {
            selected++;
            if (selected >= ITEMS.length) selected = 0;
            repaint();
        } else if (action == FIRE || keyCode == '5') {
            choose();
        } else if (keyCode == '0' || action == LEFT) {
            screen = CONTROLS;
            repaint();
        }
    }

    private void choose() {
        if (selected == 0) {
            midlet.newGame();
        } else if (selected == 1) {
            if (midlet.hasGame()) {
                midlet.continueGame();
            }
        } else if (selected == 2) {
            screen = CONTROLS;
            repaint();
        } else if (selected == 3) {
            screen = MULTIPLAYER;
            repaint();
        } else if (selected == 4) {
            midlet.quit();
        }
    }
}
