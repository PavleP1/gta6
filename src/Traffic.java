import java.util.Random;
import javax.microedition.m3g.*;

/** Lane-following traffic on the road grid + free-chase police with a 3-level wanted system. */
public class Traffic {
    public static final int N = 6, MAXC = 3;
    private static final float LANE = 3.5f;
    private static final int S = World3D.S;
    private static final int[] COLORS = {0xC03030, 0xE0C030, 0x30A050, 0xD0D0D0, 0x6060C0, 0xE07020};

    private Random rnd = new Random(42);
    // NPC state. axis 0 = driving along Z on road line x=line*S ; axis 1 = along X on z=line*S
    private int[] axis = new int[N], line = new int[N], dir = new int[N];
    private float[] t = new float[N], spd = new float[N], top = new float[N], wait = new float[N];
    private float[] px = new float[N], pz = new float[N], ph = new float[N];
    private boolean[] act = new boolean[N];
    private Mesh[] nm = new Mesh[N];
    // police
    public float[] cx = new float[MAXC], cz = new float[MAXC], ch = new float[MAXC], cs = new float[MAXC];
    public boolean[] cact = new boolean[MAXC];
    private Mesh[] cm = new Mesh[MAXC];
    public int wanted;
    public boolean busted, crashed;
    private float cool, evade, bust;
    private float[] pp = new float[2];

    public Traffic(Group root) {
        for (int k = 0; k < N; k++) {
            nm[k] = Car.buildMesh(COLORS[k % COLORS.length], false);
            nm[k].setRenderingEnable(false); root.addChild(nm[k]);
        }
        for (int k = 0; k < MAXC; k++) {
            cm[k] = Car.buildMesh(0xF0F0F0, true);
            cm[k].setRenderingEnable(false); root.addChild(cm[k]);
        }
    }

    public void reset() {
        wanted = 0; busted = false; cool = 0; evade = 0; bust = 0;
        for (int k = 0; k < N; k++) { act[k] = false; nm[k].setRenderingEnable(false); }
        for (int k = 0; k < MAXC; k++) { cact[k] = false; cm[k].setRenderingEnable(false); }
    }

    public void setWanted(int w) { wanted = w; evade = 0; bust = 0; }

    private boolean exists(int ax, int ln, int cell) {
        return ax == 0 ? World3D.roadX(ln, cell) : World3D.roadZ(cell, ln);
    }

    private void pos(int k) {
        if (axis[k] == 0) { px[k] = line[k] * S - dir[k] * LANE; pz[k] = t[k]; ph[k] = dir[k] > 0 ? 0f : Car.PI; }
        else { pz[k] = line[k] * S + dir[k] * LANE; px[k] = t[k]; ph[k] = dir[k] > 0 ? Car.PI / 2f : -Car.PI / 2f; }
    }

    private void move(int k, float dt) {
        float old = t[k], nt = old + dir[k] * spd[k] * dt;
        int c0 = World3D.fl(old), c1 = World3D.fl(nt);
        if (c0 != c1) {                       // crossing an intersection coordinate
            int jb = dir[k] > 0 ? c1 : c0;
            boolean fwd = exists(axis[k], line[k], c1);
            boolean pl = exists(1 - axis[k], jb, line[k]);
            boolean mi = exists(1 - axis[k], jb, line[k] - 1);
            float rr = rnd.nextFloat() * 100f;
            int turn = 0;
            if (rr < 22f && pl) turn = 1;
            else if (rr < 44f && mi) turn = -1;
            else if (!fwd) { if (pl) turn = 1; else if (mi) turn = -1; else turn = 2; }
            if (turn == 2) { dir[k] = -dir[k]; return; }
            if (turn != 0) {
                int ol = line[k];
                axis[k] = 1 - axis[k]; line[k] = jb; dir[k] = turn; t[k] = ol * S;
                return;
            }
        }
        t[k] = nt;
    }

    private void trySpawn(int k, Car car) {
        int a = rnd.nextFloat() < 0.5f ? 0 : 1;
        float side = rnd.nextFloat() < 0.5f ? -1f : 1f;
        float along = (a == 0 ? car.z : car.x) + side * (70f + rnd.nextFloat() * 40f);
        float perp = (a == 0 ? car.x : car.z) + (rnd.nextFloat() - 0.5f) * 160f;
        int ln = (int) Math.floor(perp / S + 0.5f);
        int cell = World3D.fl(along);
        if (!exists(a, ln, cell)) return;
        axis[k] = a; line[k] = ln; t[k] = along;
        dir[k] = rnd.nextFloat() < 0.5f ? 1 : -1;
        boolean city = Math.abs(car.x) < 400f && Math.abs(car.z) < 400f;
        top[k] = (city ? 9f : 17f) + rnd.nextFloat() * 5f;
        spd[k] = top[k] * 0.8f; wait[k] = 0; act[k] = true;
        nm[k].setRenderingEnable(true);
    }

    public void update(float dt, Car car, World3D map) {
        cool -= dt;
        for (int k = 0; k < N; k++) {
            if (!act[k]) { trySpawn(k, car); continue; }
            pos(k);
            float dx = px[k] - car.x, dz = pz[k] - car.z;
            if (dx * dx + dz * dz > 160f * 160f) { act[k] = false; nm[k].setRenderingEnable(false); continue; }
            float target = top[k];
            float sn = (float) Math.sin(ph[k]), cs2 = (float) Math.cos(ph[k]);
            float f = -dx * sn - dz * cs2, l = dx * cs2 - dz * sn;   // player relative to this NPC (forward / right-ish)
            if (f > 0f && f < 16f && Math.abs(l) < 2.6f) target = f < 8f ? 0f : target * 0.4f;
            for (int m = 0; m < N; m++) {
                if (m == k || !act[m] || axis[m] != axis[k] || line[m] != line[k] || dir[m] != dir[k]) continue;
                float gap = (t[m] - t[k]) * dir[k];
                if (gap > 0f && gap < 14f) target = gap < 7f ? 0f : Math.min(target, spd[m]);
            }
            if (wait[k] > 0f) { wait[k] -= dt; target = 0f; }
            spd[k] += (target - spd[k]) * Math.min(1f, dt * (target < spd[k] ? 3.5f : 1.2f));
            if (spd[k] > 0.05f) move(k, dt);
            pos(k);
            Car.put(nm[k], px[k], pz[k], ph[k]);

            dx = car.x - px[k]; dz = car.z - pz[k];
            float d2 = dx * dx + dz * dz;
            if (d2 < 9f) {
                float d = (float) Math.sqrt(d2);
                if (d < 0.05f) { dx = 1f; dz = 0f; d = 1f; }
                float ux = dx / d, uz = dz / d;
                car.x = px[k] + ux * 3f; car.z = pz[k] + uz * 3f;
                float vn = car.vx * ux + car.vz * uz;
                if (vn < 0f) {
                    car.vx -= 1.4f * vn * ux; car.vz -= 1.4f * vn * uz;
                    car.vx *= 0.75f; car.vz *= 0.75f;
                    if (-vn > 3f) crashed = true;
                    if (-vn > 5f && cool <= 0f) { wanted = Math.min(3, wanted + 1); cool = 4f; evade = 0f; }
                }
                spd[k] = 0f; wait[k] = 2.5f;
            }
        }
        updateCops(dt, car, map);
    }

    private void updateCops(float dt, Car car, World3D map) {
        float minD = 9999f;
        for (int c = 0; c < MAXC; c++) {
            if (c >= wanted) {
                if (cact[c]) { cact[c] = false; cm[c].setRenderingEnable(false); }
                continue;
            }
            if (!cact[c]) {
                float ang = rnd.nextFloat() * 2f * Car.PI;
                cx[c] = car.x + (float) Math.sin(ang) * 110f; cz[c] = car.z + (float) Math.cos(ang) * 110f;
                ch[c] = ang + Car.PI; cs[c] = 0f; cact[c] = true; cm[c].setRenderingEnable(true);
            }
            float dx = car.x - cx[c], dz = car.z - cz[c];
            float dist = (float) Math.sqrt(dx * dx + dz * dz);
            if (dist < minD) minD = dist;
            float da = Car.norm(Car.atan2(dx, dz) - ch[c]);
            float mt = 2.4f * dt * Math.max(0.3f, Math.min(1f, cs[c] / 6f));
            if (da > mt) da = mt; else if (da < -mt) da = -mt;
            ch[c] += da;
            float tgt = Math.min(31f, 8f + car.speed() + dist * 0.05f);
            if (dist < 6f) tgt = car.speed() * 0.8f;
            cs[c] += (tgt - cs[c]) * Math.min(1f, dt * 1.2f);
            cx[c] += (float) Math.sin(ch[c]) * cs[c] * dt;
            cz[c] += (float) Math.cos(ch[c]) * cs[c] * dt;
            pp[0] = cx[c]; pp[1] = cz[c];
            if (map.resolve(pp, 1.6f)) { cx[c] = pp[0]; cz[c] = pp[1]; cs[c] *= 0.6f; }
            Car.put(cm[c], cx[c], cz[c], ch[c]);
            if (dist < 3.2f) {
                float d = Math.max(dist, 0.1f), ux = dx / d, uz = dz / d;
                car.x = cx[c] + ux * 3.2f; car.z = cz[c] + uz * 3.2f;
                car.vx *= 0.85f; car.vz *= 0.85f; cs[c] *= 0.6f;
            }
        }
        if (wanted > 0) {
            if (minD > 90f) evade += dt; else evade = 0f;
            if (evade > 8f) { wanted = 0; evade = 0f; }
            if (minD < 9f && car.speed() < 4f) bust += dt; else bust = Math.max(0f, bust - dt);
            if (bust > 2.5f) { busted = true; bust = 0f; wanted = 0; }
        }
    }
}
