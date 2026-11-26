
import javax.microedition.lcdui.Display;
import javax.microedition.midlet.MIDlet;

/** RallyCity MIDlet entry point and screen manager. */
public class RallyMIDlet extends MIDlet {
    private Display display;
    private GameView gv;
    private MenuCanvas menu;

    protected void startApp() {
        display = Display.getDisplay(this);
        if (menu == null) menu = new MenuCanvas(this);

        if (gv != null) {
            display.setCurrent(gv);
            gv.start();
        } else {
            display.setCurrent(menu);
        }
    }

    protected void pauseApp() {
        if (gv != null) gv.pause();
    }

    protected void destroyApp(boolean unconditional) {
        if (gv != null) gv.stop();
    }

    public void newGame() {
        if (gv != null) gv.stop();

        gv = new GameView(this);
        display.setCurrent(gv);
        gv.start();
    }

    public void showMenu() {
        if (gv != null) gv.pause();
        if (display != null && menu != null) {
            display.setCurrent(menu);
        }
    }

    public void continueGame() {
        if (gv == null) return;

        display.setCurrent(gv);
        gv.start();
    }

    public boolean hasGame() {
        return gv != null;
    }

    public void quit() {
        destroyApp(true);
        notifyDestroyed();
    }
}
