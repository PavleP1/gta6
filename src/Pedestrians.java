
import java.util.Random;
import javax.microedition.m3g.*;

/**
 * Lightweight pedestrian system for RallyCity.
 * Pedestrians wander near the player and stay off roads.
 *
 * Designed for CLDC 1.1, MIDP 2.1 and M3G.
 */
public class Pedestrians {

    public static final int COUNT = 5;

    private static final float SPAWN_MIN = 20f;
    private static final float SPAWN_MAX = 55f;
    private static final float DESPAWN = 95f;
    private static final float SPEED = 1.1f;

    private Random random = new Random(731);

    private Mesh[] meshes = new Mesh[COUNT];

    private float[] x = new float[COUNT];
    private float[] z = new float[COUNT];
    private float[] heading = new float[COUNT];

    private float[] targetX = new float[COUNT];
    private float[] targetZ = new float[COUNT];
    private float[] wait = new float[COUNT];

    private boolean[] active = new boolean[COUNT];

    public Pedestrians(Group root) {
        for (int i = 0; i < COUNT; i++) {
            meshes[i] = buildPedestrian(i);
            meshes[i].setRenderingEnable(false);
            root.addChild(meshes[i]);
        }
    }

    private Mesh buildPedestrian(int index) {
        int[] colors = {
            0x305F9F,
            0x9B4935,
            0x477C45,
            0x77718E,
            0xB18A36
        };

        int shirt = colors[index % colors.length];

        MeshBuilder b = new MeshBuilder(160, 0.05f);

        // Torso.
        b.box(
            -0.28f, 0.65f, -0.15f,
             0.28f, 1.25f, 0.15f,
            shirt
        );

        // Head.
        b.box(
            -0.17f, 1.25f, -0.16f,
             0.17f, 1.60f, 0.16f,
            0xD5A47C
        );

        // Left leg.
        b.box(
            -0.25f, 0.05f, -0.12f,
            -0.03f, 0.65f, 0.12f,
            0x30343D
        );

        // Right leg.
        b.box(
             0.03f, 0.05f, -0.12f,
             0.25f, 0.65f, 0.12f,
            0x30343D
        );

        // Left arm.
        b.box(
            -0.43f, 0.68f, -0.12f,
            -0.28f, 1.20f, 0.12f,
            shirt
        );

        // Right arm.
        b.box(
             0.28f, 0.68f, -0.12f,
             0.43f, 1.20f, 0.12f,
            shirt
        );

        return b.build();
    }

    public void update(float dt, float playerX, float playerZ) {
        for (int i = 0; i < COUNT; i++) {

            if (!active[i]) {
                trySpawn(i, playerX, playerZ);
                continue;
            }

            float dx = x[i] - playerX;
            float dz = z[i] - playerZ;

            if (dx * dx + dz * dz > DESPAWN * DESPAWN) {
                deactivate(i);
                continue;
            }

            if (wait[i] > 0f) {
                wait[i] -= dt;
                continue;
            }

            float tx = targetX[i] - x[i];
            float tz = targetZ[i] - z[i];

            float distance =
                (float)Math.sqrt(tx * tx + tz * tz);

            if (distance < 1.2f) {
                chooseTarget(i);
                continue;
            }

            heading[i] = Car.atan2(tx, tz);

            float step = SPEED * dt;

            if (step > distance) step = distance;

            float nx = x[i] +
                (float)Math.sin(heading[i]) * step;

            float nz = z[i] +
                (float)Math.cos(heading[i]) * step;

            int surface = World3D.surface(nx, nz);

            // Keep pedestrians away from roads.
            if (surface == 0 || surface == 1) {
                chooseTarget(i);
                continue;
            }

            x[i] = nx;
            z[i] = nz;

            meshes[i].setTranslation(x[i], 0f, z[i]);
            meshes[i].setOrientation(
                heading[i] * 57.29578f,
                0f, 1f, 0f
            );
        }
    }

    private void trySpawn(int i, float px, float pz) {
        for (int attempt = 0; attempt < 8; attempt++) {
            float angle = random.nextFloat() *
                          6.2831853f;

            float distance = SPAWN_MIN +
                random.nextFloat() *
                (SPAWN_MAX - SPAWN_MIN);

            float sx = px +
                (float)Math.sin(angle) * distance;

            float sz = pz +
                (float)Math.cos(angle) * distance;

            int surface = World3D.surface(sx, sz);

            if (surface == 0 || surface == 1) {
                continue;
            }

            x[i] = sx;
            z[i] = sz;
            heading[i] = angle;

            meshes[i].setTranslation(sx, 0f, sz);
            meshes[i].setOrientation(
                angle * 57.29578f,
                0f, 1f, 0f
            );

            active[i] = true;
            meshes[i].setRenderingEnable(true);

            chooseTarget(i);
            return;
        }
    }

    private void chooseTarget(int i) {
        for (int attempt = 0; attempt < 8; attempt++) {
            float angle = random.nextFloat() *
                          6.2831853f;

            float distance = 5f +
                random.nextFloat() * 12f;

            float tx = x[i] +
                (float)Math.sin(angle) * distance;

            float tz = z[i] +
                (float)Math.cos(angle) * distance;

            int surface = World3D.surface(tx, tz);

            if (surface == 0 || surface == 1) {
                continue;
            }

            targetX[i] = tx;
            targetZ[i] = tz;
            wait[i] = 0.3f + random.nextFloat() * 1.5f;
            return;
        }

        wait[i] = 1f;
    }

    private void deactivate(int i) {
        active[i] = false;
        meshes[i].setRenderingEnable(false);
    }

    /**
     * Removes the nearest pedestrian inside the given radius.
     * Returns true if a pedestrian was found.
     */
    public boolean hitNearest(float px, float pz,
                              float radius) {
        int found = -1;
        float best = radius * radius;

        for (int i = 0; i < COUNT; i++) {
            if (!active[i]) continue;

            float dx = x[i] - px;
            float dz = z[i] - pz;
            float d2 = dx * dx + dz * dz;

            if (d2 < best) {
                best = d2;
                found = i;
            }
        }

        if (found < 0) return false;

        deactivate(found);
        return true;
    }

    public boolean isActive(int i) {
        if (i < 0 || i >= COUNT) return false;
        return active[i];
    }

    public float getX(int i) {
        if (i < 0 || i >= COUNT) return 0f;
        return x[i];
    }

    public float getZ(int i) {
        if (i < 0 || i >= COUNT) return 0f;
        return z[i];
    }
}
