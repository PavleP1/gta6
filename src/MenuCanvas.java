
import javax.microedition.lcdui.*;

public class MenuCanvas extends Canvas {
    private RallyMIDlet midlet;
    private int selected;
    private boolean controls;

    private static final String[] ITEMS = {
        "NEW GAME", "CONTINUE", "CONTROLS", "EXIT"
    };

    public MenuCanvas(RallyMIDlet m) {
        midlet = m;
        setFullScreenMode(true);
    }

    protected void paint(Graphics g) {
        int w = getWidth();
        int h = getHeight();

        g.setColor(0x101820);
        g.fillRect(0, 0, w, h);

        g.setColor(0x172B3A);
        g.fillRect(0, h / 3, w, h - h / 3);

        g.setColor(0xE6A52E);
        g.fillRect(0, h / 3, w, 3);

        g.setColor(0xFFFFFF);
        g.drawString("RALLY", w / 2, h / 7,
                     Graphics.TOP | Graphics.HCENTER);

        g.setColor(0xE6A52E);
        g.drawString("CITY", w / 2, h / 7 + 20,
                     Graphics.TOP | Graphics.HCENTER);

        g.setColor(0xB8C7D2);
        g.drawString("OPEN WORLD DRIVING", w / 2, h / 7 + 43,
                     Graphics.TOP | Graphics.HCENTER);

        if (controls) {
            drawControls(g, w, h);
        } else {
            drawMenu(g, w, h);
        }

        g.setColor(0x8999A5);
        g.drawString("NOKIA E72 EDITION", w / 2, h - 18,
                     Graphics.BOTTOM | Graphics.HCENTER);
    }

    private void drawMenu(Graphics g, int w, int h) {
        int itemH = 27;
        int startY = h / 2 - 2 * itemH;

        for (int i = 0; i < ITEMS.length; i++) {
            int y = startY + i * itemH;

            if (i == selected) {
                g.setColor(0xE6A52E);
                g.fillRect(w / 2 - 76, y - 2, 152, 23);
                g.setColor(0x17202A);
            } else {
                g.setColor(0xE4EAF0);
            }

            if (i == 1 && !midlet.hasGame()) {
                g.setColor(0x65727C);
            }

            g.drawString(ITEMS[i], w / 2, y,
                         Graphics.TOP | Graphics.HCENTER);
        }

        g.setColor(0xAAB8C2);
        g.drawString("UP / DOWN - SELECT", w / 2, h - 43,
                     Graphics.BOTTOM | Graphics.HCENTER);
        g.drawString("FIRE - CONFIRM", w / 2, h - 29,
                     Graphics.BOTTOM | Graphics.HCENTER);
    }

    private void drawControls(Graphics g, int w, int h) {
        g.setColor(0xE6A52E);
        g.drawString("CONTROLS", w / 2, h / 2 - 51,
                     Graphics.TOP | Graphics.HCENTER);

        g.setColor(0xFFFFFF);

        int y = h / 2 - 24;
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

        g.setColor(0xE6A52E);
        g.drawString("0 / BACK - RETURN", w / 2, h - 43,
                     Graphics.BOTTOM | Graphics.HCENTER);
    }

    protected void keyPressed(int keyCode) {
        int action = getGameAction(keyCode);

        if (controls) {
            if (keyCode == '0' || action == FIRE || action == LEFT) {
                controls = false;
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
            controls = true;
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
            controls = true;
            repaint();
        } else if (selected == 3) {
            midlet.quit();
        }
    }
}
