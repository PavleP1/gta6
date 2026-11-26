
import javax.microedition.lcdui.*;
import javax.microedition.lcdui.game.GameCanvas;
import javax.microedition.m3g.*;

/**
 * RallyCity main game.
 * Driving, on-foot movement, pedestrians, weapons,
 * missions, navigation, player stats and HUD.
 *
 * MIDP 2.1 / CLDC 1.1 / M3G.
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
    private PlayerCharacter player;
    private World3D map;
    private Traffic traffic;
    private Sound sound;
    private Navigation navigation;
    private ServiceStations stations;

    private Pedestrians pedestrians;
    private WeaponInventory inventory;
    private WeaponSystem weapons;
    private CombatSystem combat;
    private WeaponPickup pickups;
    private PlayerStats stats;

    private Mesh marker;

    private boolean kUp, kDown, kLeft, kRight, kHb;
    private boolean mapOpen;
    private boolean inVehicle = true;

    private int pauseSelection;
    private int camMode, mi, money, fps;
    private int nearbyStation = -1;

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

        cam.setPerspective(
            65f,
            (float)getWidth() / height,
            2.5f,
            135f
        );

        scene.addChild(cam);
        scene.setActiveCamera(cam);

        Mats.init(SKY);

        map = new World3D(scene);

        car = new Car(0x2E5FB0, false);
        scene.addChild(car.mesh);

        player = new PlayerCharacter(scene);

        traffic = new Traffic(scene);
        sound = new Sound();
        navigation = new Navigation();
        stations = new ServiceStations();

        pedestrians = new Pedestrians(scene, map);
        inventory = new WeaponInventory();
        weapons = new WeaponSystem(scene);
        combat = new CombatSystem(inventory, weapons);
        pickups = new WeaponPickup(scene);
        stats = new PlayerStats();

        MeshBuilder mb = new MeshBuilder(40, 0.1f);
        mb.box(-1f, 0f, -1f,
               1f, 40f, 1f, 0xFFD21F);

        marker = mb.build();
        scene.addChild(marker);

        respawn();
        createPickups();
        startMission();
    }

    private void createPickups() {
        // Weapon and ammunition pickups near the starting area.
        pickups.add(8f, 22f,
                    WeaponPickup.WEAPON_SHOTGUN, 0);

        pickups.add(-12f, 32f,
                    WeaponPickup.AMMO_PISTOL, 24);

        pickups.add(15f, 38f,
                    WeaponPickup.WEAPON_SMG, 0);

        pickups.add(-18f, 18f,
                    WeaponPickup.AMMO_SHOTGUN, 12);

        pickups.add(22f, 12f,
                    WeaponPickup.AMMO_SMG, 60);
    }

    private void respawn() {
        car.place(-3.5f, 20f, 0f);
        camH = 0f;

        player.place(-3.5f, 20f, 0f);
        player.setVisible(false);

        inVehicle = true;

        traffic.reset();
        car.sync();

        stats.respawn();
    }

    private void startMission() {
        mtime = MT[mi];

        marker.setTranslation(
            MX[mi], 0f, MZ[mi]
        );

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

        if (paused) {
            if (!d) return;

            if (k >= 'A' && k <= 'Z') {
                k += 32;
            }

            if (k == 'p' || k == 27) {
                paused = false;
                return;
            }

            if (ga == UP || k == 'w' || k == '2') {
                pauseSelection =
                    (pauseSelection + 3) % 4;

            } else if (ga == DOWN || k == 's' || k == '8') {
                pauseSelection =
                    (pauseSelection + 1) % 4;

            } else if (ga == FIRE || k == '5' || k == ' ') {
                if (pauseSelection == 0) {
                    paused = false;

                } else if (pauseSelection == 1) {
                    sound.toggle();

                } else if (pauseSelection == 2) {
                    mapOpen = !mapOpen;
                    paused = false;

                } else if (pauseSelection == 3) {
                    midlet.showMenu();
                }
            }

            return;
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
                if (d) openPauseMenu();
                break;

            case 'm':
                if (d) sound.toggle();
                break;

            case 'n':
                if (d) mapOpen = !mapOpen;
                break;

            case 'v':
                if (d) {
                    navigation.setWaypoint(
                        MX[mi], MZ[mi]
                    );
                    say("Waypoint set");
                }
                break;

            case 'b':
                if (d) {
                    navigation.clearWaypoint();
                    say("Waypoint cleared");
                }
                break;

            case 'f':
                if (d && inVehicle) {
                    useServiceStation();
                }
                break;

            case 'e':
                if (d) toggleVehicle();
                break;

            case 'j':
                if (d && !inVehicle) fireWeapon();
                break;

            case 'k':
                if (d && !inVehicle) reloadWeapon();
                break;

            case '1':
                if (d && !inVehicle) selectWeapon(
                    WeaponInventory.PISTOL
                );
                break;

            case '2':
                if (d && !inVehicle) selectWeapon(
                    WeaponInventory.SHOTGUN
                );
                break;

            case '3':
                if (d && !inVehicle) selectWeapon(
                    WeaponInventory.SMG
                );
                break;

            case 'q':
                if (d) midlet.quit();
                break;

            default:
                if (k == -7 && d) midlet.quit();
        }
    }

    private void toggleVehicle() {
        if (inVehicle) {
            if (car.speed() > 2.0f) {
                say("Stop the car first");
                return;
            }

            float sideX =
                (float)Math.cos(car.h) * 2.5f;

            float sideZ =
                -(float)Math.sin(car.h) * 2.5f;

            player.place(
                car.x + sideX,
                car.z + sideZ,
                car.h
            );

            player.setVisible(true);
            car.mesh.setRenderingEnable(true);

            inVehicle = false;
            kUp = kDown = kLeft = kRight = kHb = false;

            say("On foot - E to enter vehicle");

        } else {
            float dx = player.getX() - car.x;
            float dz = player.getZ() - car.z;

            if (dx * dx + dz * dz > 16f) {
                say("Move closer to your vehicle");
                return;
            }

            player.stop();
            player.setVisible(false);

            inVehicle = true;

            kUp = kDown = kLeft = kRight = kHb = false;

            say("Vehicle entered");
        }
    }

    private void fireWeapon() {
        if (combat.isReloading()) {
            say("Reloading");
            return;
        }

        boolean fired = combat.fire(
            player.getX(),
            1.05f,
            player.getZ(),
            player.getHeading()
        );

        if (!fired) {
            if (combat.getLoadedAmmo() <= 0) {
                say("Out of ammo - press K");
            }
        }
    }

    private void reloadWeapon() {
        if (combat.reload()) {
            say("Reloading " + combat.getWeaponName());
        } else if (combat.getLoadedAmmo() > 0) {
            say("Cannot reload");
        } else {
            say("No reserve ammunition");
        }
    }

    private void selectWeapon(int weapon) {
        if (combat.selectWeapon(weapon)) {
            say(combat.getWeaponName() + " selected");
        } else {
            say("Weapon not available");
        }
    }

    private void openPauseMenu() {
        paused = true;
        pauseSelection = 0;

        sound.closeAll();

        kUp = kDown = kLeft = kRight = kHb = false;
    }

    private void useServiceStation() {
        int station = stations.nearest(car.x, car.z);

        if (station < 0) {
            say("No service station nearby");
            return;
        }

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

        if (spent > 0) {
            say(stations.getName(station) + "  -$" + spent);

        } else if (car.fuel >= 99.9f &&
                   car.damage <= 0.1f) {
            say("Vehicle already in good condition");

        } else if (money < ServiceStations.FUEL_PRICE &&
                   money < ServiceStations.REPAIR_PRICE) {
            say("Not enough money for service");

        } else {
            say("No service available here");
        }
    }

    private float activeX() {
        return inVehicle ? car.x : player.getX();
    }

    private float activeZ() {
        return inVehicle ? car.z : player.getZ();
    }

    private float activeHeading() {
        return inVehicle ? car.h : player.getHeading();
    }

    private float activeSpeed() {
        return inVehicle ? car.speed() : player.getSpeed();
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
        if (inVehicle) {
            updateVehicle(dt);
        } else {
            updateOnFoot(dt);
        }

        float px = activeX();
        float pz = activeZ();

        map.update(px, pz);

        pedestrians.update(dt, px, pz);

        weapons.update(dt, map);
        combat.update(dt);

        int collected = pickups.update(
            px, pz, inventory
        );

        if (collected > 0) {
            say("Pickup collected");
        }

        nearbyStation = inVehicle ?
            stations.nearest(car.x, car.z) : -1;

        updateMission(dt, px, pz);

        if (msgT > 0f) {
            msgT -= dt;
        }

        updateCamera(dt);
    }

    private void updateVehicle(float dt) {
        car.update(
            dt, kUp, kDown, kLeft, kRight, kHb,
            World3D.surface(car.x, car.z)
        );

        pp[0] = car.x;
        pp[1] = car.z;

        if (map.resolve(pp, Car.R)) {
            car.x = pp[0];
            car.z = pp[1];

            float impact =
                car.vx * map.nx + car.vz * map.nz;

            if (impact < -6f) {
                sound.crash();

                car.addDamage(
                    Math.min(12f, -impact * 0.7f)
                );
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
        sound.effects(
            traffic.wanted > 0,
            car.slip,
            car.speed()
        );

        if (traffic.busted) {
            traffic.busted = false;

            money = Math.max(0, money - 200);

            respawn();
            startMission();

            say("BUSTED  -$200");
        }
    }

    private void updateOnFoot(float dt) {
        player.update(
            dt,
            kUp, kDown, kLeft, kRight,
            map
        );

        sound.engine(0f, false);
        sound.effects(false, 0f, 0f);

        if (stats.isDead()) {
            return;
        }
    }

    private void updateMission(float dt, float px, float pz) {
        float dx = MX[mi] - px;
        float dz = MZ[mi] - pz;

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
    }

    private void updateCamera(float dt) {
        float targetHeading = activeHeading();

        float dh = Car.norm(targetHeading - camH);

        camH += dh *
            Math.min(1f, dt * (camMode == 2 ? 20f : 3.5f));

        float cx, cy, cz, pitch;

        if (!inVehicle) {
            float dist = camMode == 0 ? 5f : 8f;

            if (camMode == 2) {
                cx = player.getX() +
                    (float)Math.sin(targetHeading) * 0.25f;

                cz = player.getZ() +
                    (float)Math.cos(targetHeading) * 0.25f;

                cy = 1.4f;
                pitch = 3f;

            } else {
                cx = player.getX() -
                    (float)Math.sin(camH) * dist;

                cz = player.getZ() -
                    (float)Math.cos(camH) * dist;

                cy = camMode == 0 ? 2.8f : 4.2f;
                pitch = camMode == 0 ? 9f : 14f;
            }

            car.mesh.setRenderingEnable(false);

        } else {
            if (camMode == 2) {
                camH = car.h;

                cx = car.x +
                    (float)Math.sin(car.h) * 0.4f;

                cz = car.z +
                    (float)Math.cos(car.h) * 0.4f;

                cy = 1.3f;
                pitch = 2f;

            } else {
                float dist = camMode == 0 ? 7f : 11f;

                cx = car.x -
                    (float)Math.sin(camH) * dist;

                cz = car.z -
                    (float)Math.cos(camH) * dist;

                cy = camMode == 0 ? 3.0f : 4.8f;
                pitch = camMode == 0 ? 10f : 15f;
            }

            car.mesh.setRenderingEnable(camMode != 2);
        }

        camT.setIdentity();

        camT.postTranslate(cx, cy, cz);

        camT.postRotate(
            camH * 57.29578f + 180f,
            0f, 1f, 0f
        );

        camT.postRotate(
            -pitch, 1f, 0f, 0f
        );

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

        if (paused) {
            drawPauseMenu(g);
        } else if (mapOpen) {
            drawMap(g);
        } else {
            hud(g);
        }

        flushGraphics();
    }

    private void hud(Graphics g) {
        int w = getWidth();
        int h = getHeight();

        // Default font avoids unsupported Font constants.
        g.setFont(Font.getDefaultFont());

        g.setColor(0xFFFFFF);

        if (inVehicle) {
            g.drawString(
                (int)(car.speed() * 3.6f) + " km/h",
                w - 4, h - 4,
                Graphics.BOTTOM | Graphics.RIGHT
            );
        } else {
            g.drawString(
                "ON FOOT",
                w - 4, h - 4,
                Graphics.BOTTOM | Graphics.RIGHT
            );
        }

        g.drawString(
            "$" + money,
            4, 4,
            Graphics.TOP | Graphics.LEFT
        );

        g.drawString(
            MS[mi],
            4, 20,
            Graphics.TOP | Graphics.LEFT
        );

        if (MT[mi] > 0) {
            g.drawString(
                "Time " + (int)mtime,
                4, 36,
                Graphics.TOP | Graphics.LEFT
            );
        }

        if (inVehicle) {
            g.setColor(
                car.fuel < 15f ? 0xFF4040 : 0x40E080
            );

            g.drawString(
                "FUEL " + (int)car.fuel + "%",
                4, h - 38,
                Graphics.BOTTOM | Graphics.LEFT
            );

            g.setColor(
                car.damage > 60f ? 0xFF4040 : 0xFFFFFF
            );

            g.drawString(
                "DAMAGE " + (int)car.damage + "%",
                4, h - 22,
                Graphics.BOTTOM | Graphics.LEFT
            );

        } else {
            g.setColor(0x40E080);

            g.drawString(
                "HEALTH " + stats.getHealth(),
                4, h - 38,
                Graphics.BOTTOM | Graphics.LEFT
            );

            g.setColor(0x55BFFF);

            g.drawString(
                "ARMOUR " + stats.getArmour(),
                4, h - 22,
                Graphics.BOTTOM | Graphics.LEFT
            );

            g.setColor(0xFFFFFF);

            String ammoText =
                combat.getWeaponName() + " " +
                combat.getLoadedAmmo() + "/" +
                combat.getReserveAmmo();

            g.drawString(
                ammoText,
                w - 4, h - 22,
                Graphics.BOTTOM | Graphics.RIGHT
            );

            if (combat.isReloading()) {
                g.drawString(
                    "RELOADING",
                    w / 2, h - 48,
                    Graphics.BOTTOM | Graphics.HCENTER
                );
            }
        }

        if (traffic.wanted > 0) {
            StringBuffer sb = new StringBuffer();

            for (int i = 0; i < traffic.wanted; i++) {
                sb.append('*');
            }

            g.setColor(0xFF3030);

            g.drawString(
                sb.toString(),
                w / 2, 4,
                Graphics.TOP | Graphics.HCENTER
            );
        }

        g.setColor(0xAAAAAA);

        g.drawString(
            fps + " fps",
            4, h - 4,
            Graphics.BOTTOM | Graphics.LEFT
        );

        int rad = 26;
        int rx = w - rad - 4;
        int ry = rad + 4;

        g.setColor(0x202020);

        g.fillArc(
            rx - rad, ry - rad,
            rad * 2, rad * 2, 0, 360
        );

        g.setColor(0xFFFFFF);
        g.fillRect(rx - 1, ry - 1, 3, 3);

        blip(
            g, rx, ry, rad,
            MX[mi], MZ[mi], 0xFFD21F
        );

        for (int c = 0; c < Traffic.MAXC; c++) {
            if (traffic.cact[c]) {
                blip(
                    g, rx, ry, rad,
                    traffic.cx[c], traffic.cz[c],
                    0xFF3030
                );
            }
        }

        navigation.draw(
            g, w,
            activeX(), activeZ(),
            activeHeading()
        );

        if (nearbyStation >= 0) {
            g.setColor(0xFFD21F);

            g.drawString(
                car.speed() <= ServiceStations.MAX_SPEED ?
                    "F - SERVICE" : "STOP FOR SERVICE",
                w / 2, h / 2 + 25,
                Graphics.BASELINE | Graphics.HCENTER
            );
        }

        if (!inVehicle) {
            g.setColor(0xFFFFFF);

            g.drawString(
                "E - VEHICLE",
                w / 2, h - 4,
                Graphics.BOTTOM | Graphics.HCENTER
            );
        }

        if (msgT > 0f) {
            g.setColor(0xFFFFFF);

            g.drawString(
                msg,
                w / 2, h / 3,
                Graphics.BASELINE | Graphics.HCENTER
            );
        }
    }

    private void drawPauseMenu(Graphics g) {
        int w = getWidth();
        int h = getHeight();

        g.setColor(0x080E14);
        g.fillRect(0, 0, w, h);

        int panelW = w - 28;

        if (panelW > 220) panelW = 220;

        int panelH = 154;
        int left = (w - panelW) / 2;
        int top = (h - panelH) / 2;

        g.setColor(0x172B3A);
        g.fillRect(left, top, panelW, panelH);

        g.setColor(0xE6A52E);
        g.drawRect(left, top, panelW, panelH);

        g.setFont(Font.getDefaultFont());

        g.drawString(
            "PAUSED",
            w / 2, top + 12,
            Graphics.TOP | Graphics.HCENTER
        );

        String[] items = {
            "RESUME",
            sound.on ? "SOUND: ON" : "SOUND: OFF",
            mapOpen ? "CLOSE MAP" : "OPEN MAP",
            "MAIN MENU"
        };

        for (int i = 0; i < items.length; i++) {
            int y = top + 42 + i * 23;

            if (i == pauseSelection) {
                g.setColor(0xE6A52E);

                g.fillRect(
                    left + 10, y - 2,
                    panelW - 20, 19
                );

                g.setColor(0x17202A);

            } else {
                g.setColor(0xFFFFFF);
            }

            g.drawString(
                items[i],
                w / 2, y,
                Graphics.TOP | Graphics.HCENTER
            );
        }

        g.setColor(0xAAB8C2);

        g.drawString(
            "UP/DOWN SELECT  FIRE OK",
            w / 2, top + panelH - 17,
            Graphics.TOP | Graphics.HCENTER
        );
    }

    private void drawMap(Graphics g) {
        int w = getWidth();
        int h = getHeight();

        int cx = w / 2;
        int cy = h / 2;

        int available = Math.min(w - 24, h - 64);
        int radius = available / 2;

        if (radius < 20) radius = 20;

        float px = activeX();
        float pz = activeZ();

        float scale = (radius * 2f) /
                      (World3D.S * 5f);

        g.setColor(0x101820);
        g.fillRect(0, 0, w, h);

        g.setColor(0xFFFFFF);

        g.drawString(
            "CITY MAP",
            cx, 5,
            Graphics.TOP | Graphics.HCENTER
        );

        int ci = World3D.fl(px);
        int cj = World3D.fl(pz);

        for (int i = ci - 2; i <= ci + 2; i++) {
            for (int j = cj - 2; j <= cj + 2; j++) {
                int left = cx +
                    (int)((i * World3D.S - px) * scale);

                int top = cy -
                    (int)(((j + 1) * World3D.S - pz) * scale);

                int size = (int)(World3D.S * scale) + 1;

                g.setColor(
                    World3D.isCity(i, j) ?
                    0x263B49 : 0x304B35
                );

                g.fillRect(left, top, size, size);

                g.setColor(0x43535B);
                g.drawRect(left, top, size, size);
            }
        }

        g.setColor(0xB8B5AA);

        for (int i = ci - 2; i <= ci + 2; i++) {
            for (int j = cj - 2; j <= cj + 2; j++) {
                float wx = i * World3D.S;
                float wz = j * World3D.S;

                if (World3D.roadX(i, j)) {
                    int sx = cx +
                        (int)((wx - px) * scale);

                    int sy1 = cy -
                        (int)(((j + 1) * World3D.S - pz) * scale);

                    int sy2 = cy -
                        (int)((j * World3D.S - pz) * scale);

                    int rw = Math.max(
                        3, (int)(World3D.RW * scale)
                    );

                    g.fillRect(
                        sx - rw / 2, sy1,
                        rw, sy2 - sy1 + 1
                    );
                }

                if (World3D.roadZ(i, j)) {
                    int sy = cy -
                        (int)((wz - pz) * scale);

                    int sx1 = cx +
                        (int)((i * World3D.S - px) * scale);

                    int sx2 = cx +
                        (int)(((i + 1) * World3D.S - px) * scale);

                    int rw = Math.max(
                        3, (int)(World3D.RW * scale)
                    );

                    g.fillRect(
                        sx1, sy - rw / 2,
                        sx2 - sx1 + 1, rw
                    );
                }
            }
        }

        drawMapMarker(
            g, cx, cy, scale,
            MX[mi], MZ[mi], 0xFFD21F
        );

        if (navigation.hasWaypoint()) {
            drawMapMarker(
                g, cx, cy, scale,
                navigation.getTargetX(),
                navigation.getTargetZ(),
                0x35D9FF
            );
        }

        for (int s = 0; s < ServiceStations.COUNT; s++) {
            drawMapMarker(
                g, cx, cy, scale,
                stations.getX(s),
                stations.getZ(s),
                0x40E080
            );
        }

        for (int c = 0; c < Traffic.MAXC; c++) {
            if (traffic.cact[c]) {
                drawMapMarker(
                    g, cx, cy, scale,
                    traffic.cx[c], traffic.cz[c],
                    0xFF3030
                );
            }
        }

        float heading = activeHeading();

        float sn = (float)Math.sin(heading);
        float cs = (float)Math.cos(heading);

        g.setColor(0xFFFFFF);

        g.fillTriangle(
            cx + (int)(sn * 10f),
            cy - (int)(cs * 10f),

            cx + (int)(-cs * 6f),
            cy - (int)(-sn * 6f),

            cx + (int)(cs * 6f),
            cy - (int)(sn * 6f)
        );

        g.setColor(0xFFFFFF);

        g.drawRect(
            cx - radius, cy - radius,
            radius * 2, radius * 2
        );

        int ly = h - 48;

        g.setColor(0xFFD21F);
        g.fillRect(8, ly, 7, 7);

        g.setColor(0xFFFFFF);

        g.drawString(
            "Mission", 19, ly - 2,
            Graphics.TOP | Graphics.LEFT
        );

        g.setColor(0x35D9FF);
        g.fillRect(75, ly, 7, 7);

        g.setColor(0xFFFFFF);

        g.drawString(
            "Waypoint", 86, ly - 2,
            Graphics.TOP | Graphics.LEFT
        );

        g.setColor(0x40E080);
        g.fillRect(8, ly + 18, 7, 7);

        g.setColor(0xFFFFFF);

        g.drawString(
            "Service", 19, ly + 16,
            Graphics.TOP | Graphics.LEFT
        );

        g.setColor(0xFF3030);
        g.fillRect(75, ly + 18, 7, 7);

        g.setColor(0xFFFFFF);

        g.drawString(
            "Police", 86, ly + 16,
            Graphics.TOP | Graphics.LEFT
        );

        g.drawString(
            "N - CLOSE MAP",
            cx, h - 4,
            Graphics.BOTTOM | Graphics.HCENTER
        );
    }

    private void drawMapMarker(
        Graphics g, int cx, int cy, float scale,
        float x, float z, int color
    ) {
        int sx = cx +
            (int)((x - activeX()) * scale);

        int sy = cy -
            (int)((z - activeZ()) * scale);

        g.setColor(color);

        g.fillArc(
            sx - 3, sy - 3,
            7, 7, 0, 360
        );
    }

    private void blip(
        Graphics g, int rx, int ry, int rad,
        float x, float z, int color
    ) {
        float dx = x - activeX();
        float dz = z - activeZ();

        float scale = 0.055f;

        int bx = rx + (int)(dx * scale);
        int by = ry - (int)(dz * scale);

        int ox = bx - rx;
        int oy = by - ry;

        if (ox * ox + oy * oy > rad * rad) {
            float len = (float)Math.sqrt(
                ox * ox + oy * oy
            );

            if (len > 0f) {
                bx = rx + (int)(ox * rad / len);
                by = ry + (int)(oy * rad / len);
            }
        }

        g.setColor(color);
        g.fillRect(bx - 2, by - 2, 5, 5);
    }
}
