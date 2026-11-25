
import javax.microedition.lcdui.*;
import javax.microedition.lcdui.game.GameCanvas;
import javax.microedition.m3g.*;

/** Game loop, input, camera, missions, HUD. Controls: W/S gas/brake, A/D steer, Space handbrake, C camera, R reset, P pause, Q quit. */
public class GameView extends GameCanvas implements Runnable {
    static final int SKY = 0xA8C4DC;

    // Missions: target x,z (road intersections), time limit (0 = none), wanted level on start, reward
    static final float[] MX = {0f, -240f, 480f, -480f, 0f};
    static final float[] MZ = {240f, -120f, -480f, 480f, 0f};
    static final int[] MT = {0, 70, 100, 140, 100};
    static final int[] MW = {0, 0, 2, 0, 0};
    static final int[] MR = {300, 600, 1000, 1500, 800};
    static final String[] MS = {
        "Pick up the package",
        "Deliver it fast!",
        "COPS! Reach the farm road",
        "Rally stage: cross country",
        "Back to the city"
    };

    private RallyMIDlet midlet;
    private Thread th;
    private volatile boolean run, paused;
    private Graphics3D g3d;
    private World scene;
    private Camera cam;
    private Transform camT = new Transform();
    private Car car;
    private World3D map;
    private Traffic traffic;
    private Sound sound;
    private Mesh marker;

    private boolean kUp, kDown, kLeft, kRight, kHb;
    private int camMode;
    private float camH;
    private float[] pp = new float[2];
    private int mi, money, fps;
    private float mtime, msgT;
    private String msg = "";

    public GameView(RallyMIDlet m) {
        super(false);
        midlet = m;
        setFullScreenMode(true);
        init();
    }

    private void init() {
        g3d = Graphics3D.getInstance();
        scene = new World();

        Background bg = new Background();
        bg.setColor(SKY);
        scene.setBackground(bg);

        cam = new Camera();
        cam.setPerspective(65f, (float) getWidth() / getHeight(), 2.5f, 135f);
        scene.addChild(cam);
        scene.setActiveCamera(cam);

        Mats.init(SKY);
        map = new World3D(scene);

        car = new Car(0x2E5FB0, false);
        scene.addChild(car.mesh);

        traffic = new Traffic(scene);
        sound = new Sound();

        MeshBuilder mb = new MeshBuilder(40, 0.1f);
        mb.box(-1f, 0f, -1f, 1f, 40f, 1f, 0xFFD21F);
        marker = mb.build();
        scene.addChild(marker);

        respawn();
        startMission();
    }

    private void respawn() {
        car.place(-3.5f, 20f, 0f);
        camH = 0f;
        traffic.reset();
    }

    private void startMission() {
        mtime = MT[mi];
        marker.setTranslation(MX[mi], 0f, MZ[mi]);

        if (MW[mi] > 0) {
            traffic.setWanted(MW[mi]);
        }

        say(MS[mi]);
    }

    private void say(String s) {
        msg = s;
        msgT = 3f;
    }

    public void start() {
        paused = false;

        if (th == null) {
            run = true;
            th = new Thread(this);
            th.start();
        }
    }

    public void pause() {
        paused = true;
        sound.closeAll();
    }

    public void stop() {
        run = false;
        sound.closeAll();
    }

    protected void hideNotify() {
        paused = true;
        sound.closeAll();
    }

    protected void keyPressed(int k) {
        key(k, true);
    }

    protected void keyReleased(int k) {
        key(k, false);
    }

    private void key(int k, boolean d) {
        int ga = 0;

        try {
            ga = getGameAction(k);
        } catch (Exception e) {
        }

        if (ga == UP) kUp = d;
        else if (ga == DOWN) kDown = d;
        else if (ga == LEFT) kLeft = d;
        else if (ga == RIGHT) kRight = d;
        else if (ga == FIRE) kHb = d;

        if (k >= 'A' && k <= 'Z') {
            k += 32;
        }

        switch (k) {
            case 'w':
                kUp = d;
                break;

            case 's':
                kDown = d;
                break;

            case 'a':
                kLeft = d;
                break;

            case 'd':
                kRight = d;
                break;

            case ' ':
                kHb = d;
                break;

            case 'c':
                if (d) camMode = (camMode + 1) % 3;
                break;

            case 'r':
                if (d) respawn();
                break;

            case 'p':
                if (d) paused = !paused;
                break;

            case 'm':
                if (d) sound.toggle();
                break;

            case 'q':
                if (d) midlet.quit();
                break;

            default:
                if (k == -7 && d) midlet.quit();
        }
    }

    public void run() {
        long last = System.currentTimeMillis();
        long fpsT = last;
        int frames = 0;

        while (run) {
            long t0 = System.currentTimeMillis();

            float dt = (t0 - last) / 1000f;
            last = t0;

            if (dt > 0.06f) {
                dt = 0.06f;
            }

            if (!paused) {
                update(dt);
            }

            draw();

            frames++;

            if (t0 - fpsT >= 1000) {
                fps = frames;
                frames = 0;
                fpsT = t0;
            }

            long sl = 33 - (System.currentTimeMillis() - t0);

            try {
                Thread.sleep(sl < 2 ? 2 : sl);
            } catch (InterruptedException e) {
            }
        }
    }

    private void update(float dt) {
        car.update(
            dt,
            kUp,
            kDown,
            kLeft,
            kRight,
            kHb,
            World3D.surface(car.x, car.z)
        );

        map.update(car.x, car.z);

        pp[0] = car.x;
        pp[1] = car.z;

        if (map.resolve(pp, Car.R)) {
            car.x = pp[0];
            car.z = pp[1];

            if (car.vx * map.nx + car.vz * map.nz < -6f) {
                sound.crash();
            }

            car.bounce(map.nx, map.nz);
        }

        traffic.update(dt, car, map);
        car.sync();

        if (traffic.crashed) {
            traffic.crashed = false;
            sound.crash();
        }

        // Background music has been removed.
        // Engine and gameplay sound effects remain active.
        sound.engine(car.speed(), kUp);
        sound.effects(traffic.wanted > 0, car.slip, car.speed());

        if (traffic.busted) {
            traffic.busted = false;
            money = Math.max(0, money - 200);
            respawn();
            startMission();
            say("BUSTED  -$200");
        }

        float dx = MX[mi] - car.x;
        float dz = MZ[mi] - car.z;

        if (dx * dx + dz * dz < 14f * 14f) {
            money += MR[mi];

            int got = MR[mi];
            mi = (mi + 1) % MS.length;

            startMission();
            say("Done! +$" + got);
        } else if (MT[mi] > 0) {
            mtime -= dt;

            if (mtime <= 0f) {
                money = Math.max(0, money - 100);
                startMission();
                say("Time up! -$100");
            }
        }

        if (msgT > 0f) {
            msgT -= dt;
        }

        updateCamera(dt);
    }

    private void updateCamera(float dt) {
        float dh = Car.norm(car.h - camH);
        camH += dh * Math.min(1f, dt * (camMode == 2 ? 20f : 3.5f));

        float cx, cy, cz, pitch;

        if (camMode == 2) {
            camH = car.h;

            cx = car.x + (float) Math.sin(car.h) * 0.4f;
            cz = car.z + (float) Math.cos(car.h) * 0.4f;

            cy = 1.3f;
            pitch = 2f;
        } else {
            float dist = camMode == 0 ? 7f : 11f;

            cx = car.x - (float) Math.sin(camH) * dist;
            cz = car.z - (float) Math.cos(camH) * dist;

            cy = camMode == 0 ? 3.0f : 4.8f;
            pitch = camMode == 0 ? 10f : 15f;
        }

        car.mesh.setRenderingEnable(camMode != 2);

        camT.setIdentity();
        camT.postTranslate(cx, cy, cz);
        camT.postRotate(camH * 57.29578f + 180f, 0f, 1f, 0f);
        camT.postRotate(-pitch, 1f, 0f, 0f);

        cam.setTransform(camT);
    }

    private void draw() {
        Graphics g = getGraphics();
        boolean bound = false;

        try {
            g3d.bindTarget(g, true, 0);
            bound = true;
            g3d.render(scene);
        } catch (Exception e) {
        } finally {
            if (bound) {
                g3d.releaseTarget();
            }
        }

        hud(g);
        flushGraphics();
    }

    private void hud(Graphics g) {
        int W = getWidth();
        int H = getHeight();

        g.setFont(Font.getFont(
            Font.FACE_PROPORTIONAL,
            Font.STYLE_BOLD,
            Font.SIZE_SMALL
        ));

        g.setColor(0xFFFFFF);

        g.drawString(
            (int) (car.speed() * 3.6f) + " km/h",
            W - 4, H - 4,
            Graphics.BOTTOM | Graphics.RIGHT
        );

        g.drawString("$" + money, 4, 4, Graphics.TOP | Graphics.LEFT);
        g.drawString(MS[mi], 4, 20, Graphics.TOP | Graphics.LEFT);

        if (MT[mi] > 0) {
            g.drawString("Time " + (int) mtime, 4, 36,
                         Graphics.TOP | Graphics.LEFT);
        }

        if (traffic.wanted > 0) {
            StringBuffer sb = new StringBuffer();

            for (int i = 0; i < traffic.wanted; i++) {
                sb.append('*');
            }

            g.setColor(0xFF3030);
            g.drawString(sb.toString(), W / 2, 4,
                         Graphics.TOP | Graphics.HCENTER);
        }

        g.setColor(0xAAAAAA);
        g.drawString(fps + " fps", 4, H - 4,
                     Graphics.BOTTOM | Graphics.LEFT);

        // Radar
        int rad = 26;
        int rx = W - rad - 4;
        int ry = rad + 4;

        g.setColor(0x202020);
        g.fillArc(rx - rad, ry - rad, rad * 2, rad * 2, 0, 360);

        g.setColor(0xFFFFFF);
        g.fillRect(rx - 1, ry - 1, 3, 3);

        blip(g, rx, ry, rad, MX[mi], MZ[mi], 0xFFD21F);

        for (int c = 0; c < Traffic.MAXC; c++) {
            if (traffic.cact[c]) {
                blip(g, rx, ry, rad,
                     traffic.cx[c], traffic.cz[c], 0xFF3030);
            }
        }

        if (msgT > 0f) {
            g.setColor(0xFFFFFF);
            g.drawString(msg, W / 2, H / 3,
                         Graphics.BASELINE | Graphics.HCENTER);
        }

        if (paused) {
            g.drawString("PAUSED (P)", W / 2, H / 2,
                         Graphics.BASELINE | Graphics.HCENTER);
        }
    }

    private void blip(
        Graphics g,
        int cx,
        int cy,
        int rad,
        float wx,
        float wz,
        int color
    ) {
        float dx = wx - car.x;
        float dz = wz - car.z;

        float sn = (float) Math.sin(car.h);
        float cs = (float) Math.cos(car.h);

        float sx = (-dx * cs + dz * sn) * 0.1f;
        float sy = (dx * sn + dz * cs) * 0.1f;

        float d = (float) Math.sqrt(sx * sx + sy * sy);

        if (d > rad - 2) {
            float k = (rad - 2) / d;
            sx *= k;
            sy *= k;
        }

        g.setColor(color);
        g.fillRect(
            cx + (int) sx - 1,
            cy - (int) sy - 1,
            3,
            3
        );
    }
}
