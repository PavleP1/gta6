import javax.microedition.lcdui.Display;
import javax.microedition.midlet.MIDlet;

public class RallyMIDlet extends MIDlet {
    private GameView gv;

    protected void startApp() {
        if (gv == null) {
            gv = new GameView(this);
            Display.getDisplay(this).setCurrent(gv);
        }
        gv.start();
    }
    protected void pauseApp() { if (gv != null) gv.pause(); }
    protected void destroyApp(boolean u) { if (gv != null) gv.stop(); }
    public void quit() { destroyApp(true); notifyDestroyed(); }
}
