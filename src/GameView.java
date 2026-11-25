import javax.microedition.lcdui.*;
import javax.microedition.lcdui.game.GameCanvas;
import javax.microedition.m3g.*;

/**
 * RallyCity game loop, driving, missions, navigation,
 * service stations, fuel, damage and HUD.
 */
public class GameView extends GameCanvas implements Runnable {
    static final int SKY = 0xA8C4DC;

    static final float[] MX = {0f, -240f, 480f, -480f, 0f};
    static final float[] MZ = {240f, -120f, -480f, 480f, 0f};
    static final int[] MT = {0, 70, 100, 140, 100};
    static final int[] MW = {0, 0, 2, 0, 0};
    static final int[] MR = {300, 600, 1000, 1500, 800};
    static final String[] MS = {
        "Pick up the package", "Deliver it fast!",
        "COPS! Reach the farm road", "Rally stage: cross country",
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
    private Navigation navigation;
    private ServiceStations stations;
    private Mesh marker;
    private boolean kUp, kDown, kLeft, kRight, kHb, mapOpen;
    private int camMode, mi, money, fps, nearbyStation = -1;
    private float camH, mtime, msgT;
    private float[] pp = new float[2];
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
        int height = getHeight();
        if (height < 1) height = 1;
        cam.setPerspective(65f, (float)getWidth() / height, 2.5f, 135f);
        scene.addChild(cam);
        scene.setActiveCamera(cam);

        Mats.init(SKY);
        map = new World3D(scene);
        car = new Car(0x2E5FB0, false);
        scene.addChild(car.mesh);
        traffic = new Traffic(scene);
        sound = new Sound();
        navigation = new Navigation();
        stations = new ServiceStations();

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
        car.sync();
    }

    private void startMission() {
        mtime = MT[mi];
        marker.setTranslation(MX[mi], 0f, MZ[mi]);
        if (MW[mi] > 0) traffic.setWanted(MW[mi]);
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

    protected void keyPressed(int k) { key(k, true); }
    protected void keyReleased(int k) { key(k, false); }

    private void key(int k, boolean d) {
        int ga = 0;
        try { ga = getGameAction(k); } catch (Exception e) {}
        if (ga == UP) kUp = d;
        else if (ga == DOWN) kDown = d;
        else if (ga == LEFT) kLeft = d;
        else if (ga == RIGHT) kRight = d;
        else if (ga == FIRE) kHb = d;

        if (k >= 'A' && k <= 'Z') k += 32;
        switch (k) {
            case 'w': kUp = d; break;
            case 's': kDown = d; break;
            case 'a': kLeft = d; break;
            case 'd': kRight = d; break;
            case ' ': kHb = d; break;
            case 'c': if (d) camMode = (camMode + 1) % 3; break;
            case 'r': if (d) respawn(); break;
            case 'p': if (d) paused = !paused; break;
            case 'm': if (d) sound.toggle(); break;
            case 'n': if (d) mapOpen = !mapOpen; break;
            case 'v':
                if (d) {
                    navigation.setWaypoint(MX[mi], MZ[mi]);
                    say("Waypoint set");
                }
                break;
            case 'b':
                if (d) {
                    navigation.clearWaypoint();
                    say("Waypoint cleared");
                }
                break;
            case 'f': if (d) useServiceStation(); break;
            case 'q': if (d) midlet.quit(); break;
            default: if (k == -7 && d) midlet.quit();
        }
    }

    private void useServiceStation() {
        int station = stations.nearest(car.x, car.z);
        if (station < 0) { say("No service station nearby"); return; }
        if (car.speed() > ServiceStations.MAX_SPEED) {
            say("Stop the car first!");
            return;
        }
        int spent = 0;
        int remaining = money;
        if (stations.canRefuel(station)) {
            int cost = stations.refuel(car, remaining);
            spent += cost;
            remaining -= cost;
        }
        if (stations.canRepair(station)) {
            int cost = stations.repair(car, remaining);
            spent += cost;
            remaining -= cost;
        }
        money -= spent;
        if (spent > 0) say(stations.getName(station) + "  -$" + spent);
        else if (car.fuel >= 99.9f && car.damage <= 0.1f)
            say("Vehicle already in good condition");
        else if (money < ServiceStations.FUEL_PRICE &&
                 money < ServiceStations.REPAIR_PRICE)
            say("Not enough money for service");
        else say("No service available here");
    }

    public void run() {
        long last = System.currentTimeMillis();
        long fpsT = last;
        int frames = 0;
        while (run) {
            long t0 = System.currentTimeMillis();
            float dt = (t0 - last) / 1000f;
            last = t0;
            if (dt > 0.06f) dt = 0.06f;
            if (!paused) update(dt);
            draw();
            frames++;
            if (t0 - fpsT >= 1000) {
                fps = frames;
                frames = 0;
                fpsT = t0;
            }
            long sl = 33 - (System.currentTimeMillis() - t0);
            try { Thread.sleep(sl < 2 ? 2 : sl); }
            catch (InterruptedException e) {}
        }
    }

    private void update(float dt) {
        car.update(dt, kUp, kDown, kLeft, kRight, kHb,
                   World3D.surface(car.x, car.z));
        map.update(car.x, car.z);
        pp[0] = car.x;
        pp[1] = car.z;
        if (map.resolve(pp, Car.R)) {
            car.x = pp[0];
            car.z = pp[1];
            float impact = car.vx * map.nx + car.vz * map.nz;
            if (impact < -6f) {
                sound.crash();
                car.addDamage(Math.min(12f, -impact * 0.7f));
            }
            car.bounce(map.nx, map.nz);
        }

        traffic.update(dt, car, map);
        car.sync();
        if (traffic.crashed) {
            traffic.crashed = false;
            sound.crash();
            car.addDamage(5f);
        }
        sound.engine(car.speed(), kUp);
        sound.effects(traffic.wanted > 0, car.slip, car.speed());
        nearbyStation = stations.nearest(car.x, car.z);

        if (traffic.busted) {
            traffic.busted = false;
            money = Math.max(0, money - 200);
            respawn();
            startMission();
            say("BUSTED  -$200");
        }

        float dx = MX[mi] - car.x;
        float dz = MZ[mi] - car.z;
        if (dx * dx + dz * dz < 196f) {
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
        if (msgT > 0f) msgT -= dt;
        updateCamera(dt);
    }

    private void updateCamera(float dt) {
        float dh = Car.norm(car.h - camH);
        camH += dh * Math.min(1f, dt * (camMode == 2 ? 20f : 3.5f));
        float cx, cy, cz, pitch;
        if (camMode == 2) {
            camH = car.h;
            cx = car.x + (float)Math.sin(car.h) * 0.4f;
            cz = car.z + (float)Math.cos(car.h) * 0.4f;
            cy = 1.3f;
            pitch = 2f;
        } else {
            float dist = camMode == 0 ? 7f : 11f;
            cx = car.x - (float)Math.sin(camH) * dist;
            cz = car.z - (float)Math.cos(camH) * dist;
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
            if (bound) g3d.releaseTarget();
        }
        if (mapOpen) drawMap(g);
        else hud(g);
        flushGraphics();
    }

    private void hud(Graphics g) {
        int w = getWidth(), h = getHeight();
        g.setFont(Font.getFont(Font.FACE_PROPORTIONAL,
                               Font.STYLE_BOLD, Font.SIZE_SMALL));
        g.setColor(0xFFFFFF);
        g.drawString((int)(car.speed() * 3.6f) + " km/h",
                     w - 4, h - 4, Graphics.BOTTOM | Graphics.RIGHT);
        g.drawString("$" + money, 4, 4, Graphics.TOP | Graphics.LEFT);
        g.drawString(MS[mi], 4, 20, Graphics.TOP | Graphics.LEFT);
        if (MT[mi] > 0)
            g.drawString("Time " + (int)mtime, 4, 36,
                         Graphics.TOP | Graphics.LEFT);

        g.setColor(car.fuel < 15f ? 0xFF4040 : 0x40E080);
        g.drawString("FUEL " + (int)car.fuel + "%",
                     4, h - 38, Graphics.BOTTOM | Graphics.LEFT);
        g.setColor(car.damage > 60f ? 0xFF4040 : 0xFFFFFF);
        g.drawString("DAMAGE " + (int)car.damage + "%",
                     4, h - 22, Graphics.BOTTOM | Graphics.LEFT);

        if (traffic.wanted > 0) {
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < traffic.wanted; i++) sb.append('*');
            g.setColor(0xFF3030);
            g.drawString(sb.toString(), w / 2, 4,
                         Graphics.TOP | Graphics.HCENTER);
        }
        g.setColor(0xAAAAAA);
        g.drawString(fps + " fps", 4, h - 4,
                     Graphics.BOTTOM | Graphics.LEFT);

        int rad = 26, rx = w - rad - 4, ry = rad + 4;
        g.setColor(0x202020);
        g.fillArc(rx - rad, ry - rad, rad * 2, rad * 2, 0, 360);
        g.setColor(0xFFFFFF);
        g.fillRect(rx - 1, ry - 1, 3, 3);
        blip(g, rx, ry, rad, MX[mi], MZ[mi], 0xFFD21F);
        for (int c = 0; c < Traffic.MAXC; c++)
            if (traffic.cact[c])
                blip(g, rx, ry, rad, traffic.cx[c], traffic.cz[c], 0xFF3030);

        navigation.draw(g, w, car.x, car.z, car.h);

        if (nearbyStation >= 0) {
            g.setColor(0xFFD21F);
            g.drawString(car.speed() <= ServiceStations.MAX_SPEED ?
                         "F - SERVICE" : "STOP FOR SERVICE",
                         w / 2, h / 2 + 25,
                         Graphics.BASELINE | Graphics.HCENTER);
        }
        if (msgT > 0f) {
            g.setColor(0xFFFFFF);
            g.drawString(msg, w / 2, h / 3,
                         Graphics.BASELINE | Graphics.HCENTER);
        }
        if (paused)
            g.drawString("PAUSED (P)", w / 2, h / 2,
                         Graphics.BASELINE | Graphics.HCENTER);
    }

    private void drawMap(Graphics g) {
        int w = getWidth(), h = getHeight();
        int cx = w / 2, cy = h / 2;
        int available = Math.min(w - 24, h - 64);
        int radius = available / 2;
        if (radius < 20) radius = 20;
        float scale = (radius * 2f) / (World3D.S * 5f);

        g.setColor(0x101820);
        g.fillRect(0, 0, w, h);
        g.setColor(0xFFFFFF);
        g.drawString("CITY MAP", cx, 5, Graphics.TOP | Graphics.HCENTER);

        int ci = World3D.fl(car.x), cj = World3D.fl(car.z);
        for (int i = ci - 2; i <= ci + 2; i++) {
            for (int j = cj - 2; j <= cj + 2; j++) {
                int left = cx + (int)((i * World3D.S - car.x) * scale);
                int top = cy - (int)(((j + 1) * World3D.S - car.z) * scale);
                int size = (int)(World3D.S * scale) + 1;
                g.setColor(World3D.isCity(i, j) ? 0x263B49 : 0x304B35);
                g.fillRect(left, top, size, size);
                g.setColor(0x43535B);
                g.drawRect(left, top, size, size);
            }
        }

        g.setColor(0xB8B5AA);
        for (int i = ci - 2; i <= ci + 2; i++) {
            for (int j = cj - 2; j <= cj + 2; j++) {
                float wx = i * World3D.S, wz = j * World3D.S;
                if (World3D.roadX(i, j)) {
                    int sx = cx + (int)((wx - car.x) * scale);
                    int sy1 = cy - (int)(((j + 1) * World3D.S - car.z) * scale);
                    int sy2 = cy - (int)((j * World3D.S - car.z) * scale);
                    int rw = Math.max(3, (int)(World3D.RW * scale));
                    g.fillRect(sx - rw / 2, sy1, rw, sy2 - sy1 + 1);
                }
                if (World3D.roadZ(i, j)) {
                    int sy = cy - (int)((wz - car.z) * scale);
                    int sx1 = cx + (int)((i * World3D.S - car.x) * scale);
                    int sx2 = cx + (int)(((i + 1) * World3D.S - car.x) * scale);
                    int rw = Math.max(3, (int)(World3D.RW * scale));
                    g.fillRect(sx1, sy - rw / 2, sx2 - sx1 + 1, rw);
                }
            }
        }

        drawMapMarker(g, cx, cy, scale, MX[mi], MZ[mi], 0xFFD21F);
        if (navigation.hasWaypoint())
            drawMapMarker(g, cx, cy, scale, navigation.getTargetX(),
                          navigation.getTargetZ(), 0x35D9FF);
        for (int s = 0; s < ServiceStations.COUNT; s++)
            drawMapMarker(g, cx, cy, scale, stations.getX(s),
                          stations.getZ(s), 0x40E080);
        for (int c = 0; c < Traffic.MAXC; c++)
            if (traffic.cact[c])
                drawMapMarker(g, cx, cy, scale, traffic.cx[c],
                              traffic.cz[c], 0xFF3030);

        float sn = (float)Math.sin(car.h), cs = (float)Math.cos(car.h);
        g.setColor(0xFFFFFF);
        g.fillTriangle(cx + (int)(sn * 10f), cy - (int)(cs * 10f),
                       cx + (int)(-cs * 6f), cy - (int)(-sn * 6f),
                       cx + (int)(cs * 6f), cy - (int)(sn * 6f));
        g.setColor(0xFFFFFF);
        g.drawRect(cx - radius, cy - radius, radius * 2, radius * 2);

        // Map legend.
        int ly = h - 48;
        g.setColor(0xFFD21F);
        g.fillRect(8, ly, 7, 7);
        g.setColor(0xFFFFFF);
        g.drawString("Mission", 19, ly - 2, Graphics.TOP | Graphics.LEFT);
        g.setColor(0x35D9FF);
        g.fillRect(75, ly, 7, 7);
        g.setColor(0xFFFFFF);
        g.drawString("Waypoint", 86, ly - 2, Graphics.TOP | Graphics.LEFT);
        g.setColor(0x40E080);
        g.fillRect(8, ly + 18, 7, 7);
        g.setColor(0xFFFFFF);
        g.drawString("Service", 19, ly + 16, Graphics.TOP | Graphics.LEFT);
        g.setColor(0xFF3030);
        g.fillRect(75, ly + 18, 7, 7);
        g.setColor(0xFFFFFF);
        g.drawString("Police", 86, ly + 16, Graphics.TOP | Graphics.LEFT);
        g.drawString("N - CLOSE MAP", cx, h - 4,
                     Graphics.BOTTOM | Graphics.HCENTER);
    }

    private void drawMapMarker(Graphics g, int cx, int cy, float scale,
                               float x, float z, int color) {
        int sx = cx + (int)((x - car.x) * scale);
        int sy = cy - (int)((z - car.z) * scale);
        g.setColor(color);
        g.fillArc(sx - 3, sy - 3, 7, 7, 0, 360);
    }

    private void blip(Graphics g, int rx, int ry, int rad,
                      float x, float z, int color) {
        float dx = x - car.x, dz = z - car.z;
        float scale = 0.055f;
        int bx = rx + (int)(dx * scale);
        int by = ry - (int)(dz * scale);
        int ox = bx - rx, oy = by - ry;
        if (ox * ox + oy * oy > rad * rad) {
            float len = (float)Math.sqrt(ox * ox + oy * oy);
            if (len > 0f) {
                bx = rx + (int)(ox * rad / len);
                by = ry + (int)(oy * rad / len);
            }
        }
        g.setColor(color);
        g.fillRect(bx - 2, by - 2, 5, 5);
    }
}
