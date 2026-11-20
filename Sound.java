import java.io.InputStream;
import javax.microedition.media.*;
import javax.microedition.media.control.VolumeControl;

/** Music (2 looping tracks), engine loop (3 speed bands) and one effect slot (crash / siren / skid). All failures are swallowed. */
public class Sound implements PlayerListener {
    private static final int FX_CRASH = 0, FX_SIREN = 1, FX_SKID = 2;
    private static final String[] ENG = {"/eng0.wav", "/eng1.wav", "/eng2.wav"};
    private static final String[] MUS = {"/music0.wav", "/music1.wav"};
    private static final String[] FX = {"/crash.wav", "/siren.wav", "/skid.wav"};
    private Player music, engine, fx;
    private int track = -1, band = -1, fxId = -1;
    public boolean on = true;

    private Player make(String res, boolean loop, int vol) {
        try {
            InputStream is = getClass().getResourceAsStream(res);
            Player p = Manager.createPlayer(is, "audio/x-wav");
            p.realize();
            if (loop) p.setLoopCount(-1);
            VolumeControl vc = (VolumeControl) p.getControl("VolumeControl");
            if (vc != null) vc.setLevel(vol);
            p.prefetch();
            return p;
        } catch (Throwable e) { return null; }
    }

    private void kill(Player p) {
        if (p == null) return;
        try { p.stop(); } catch (Throwable e) { }
        try { p.close(); } catch (Throwable e) { }
    }

    public void setMusic(int t) {
        if (t == track) return;
        track = t;
        kill(music); music = null;
        if (!on) return;
        music = make(MUS[t], true, 45);
        try { if (music != null) music.start(); } catch (Throwable e) { }
    }

    public void engine(float speed, boolean gas) {
        if (!on) return;
        int b = speed < 3f ? 0 : (speed < 15f ? 1 : 2);
        if (b == band) return;
        band = b;
        kill(engine);
        engine = make(ENG[b], true, 60);
        try { if (engine != null) engine.start(); } catch (Throwable e) { }
    }

    private void playFx(int id, boolean loop) {
        kill(fx); fx = null; fxId = id;
        if (!on) return;
        fx = make(FX[id], loop, id == FX_CRASH ? 90 : 55);
        if (fx != null) {
            fx.addPlayerListener(this);
            try { fx.start(); } catch (Throwable e) { }
        }
    }

    public void crash() { playFx(FX_CRASH, false); }

    /** call every frame: siren while wanted, tyre squeal while sliding sideways. */
    public void effects(boolean siren, float slip, float speed) {
        if (fxId == FX_CRASH) return;
        int want = siren ? FX_SIREN : ((Math.abs(slip) > 4f && speed > 6f) ? FX_SKID : -1);
        if (want == fxId) return;
        if (want < 0) { kill(fx); fx = null; fxId = -1; }
        else playFx(want, true);
    }

    public void toggle() {
        on = !on;
        if (on) { int t = track; track = -1; band = -1; setMusic(t < 0 ? 0 : t); }
        else { kill(music); music = null; kill(engine); engine = null; kill(fx); fx = null; fxId = -1; }
    }

    public void closeAll() {
        kill(music); kill(engine); kill(fx);
        music = null; engine = null; fx = null; track = -1; band = -1; fxId = -1;
    }

    public void playerUpdate(Player p, String event, Object data) {
        if (PlayerListener.END_OF_MEDIA.equals(event) && p == fx && fxId == FX_CRASH) {
            kill(fx); fx = null; fxId = -1;
        }
    }
}
