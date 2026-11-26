
import java.util.Random;
import javax.microedition.m3g.*;

/**
 * Lightweight pedestrian manager for RallyCity.
 *
 * Creates a small number of pedestrians that walk around
 * the active world. Meshes are created once and reused.
 */
public class Pedestrians {

    public static final int MAX_PEDESTRIANS = 5;

    private static final float WALK_SPEED = 1.8f;
    private static final float ARRIVE_DISTANCE = 2.0f;
    private static final float DESPAWN_DISTANCE = 105f;
    private static final float COLLISION_RADIUS = 0.45f;

    private Group root;
    private World3D world;
    private Random random = new Random();

    private Mesh[] models = new Mesh[MAX_PEDESTRIANS];

    private float[] x = new float[MAX_PEDESTRIANS];
    private float[] z = new float[MAX_PEDESTRIANS];

    private float[] targetX = new float[MAX_PEDESTRIANS];
    private float[] targetZ = new float[MAX_PEDESTRIANS];

    private float[] heading = new float[MAX_PEDESTRIANS];
    private float[] wait = new float[MAX_PEDESTRIANS];

    private float[][] collision = new float[MAX_PEDESTRIANS][2];

    private boolean[] active = new boolean[MAX_PEDESTRIANS];

    private int activeCount;

    public Pedestrians(Group parent, World3D world) {
        if (parent == null || world == null) {
            throw new IllegalArgumentException(
                "Parent and world must not be null"
            );
        }

        this.root = parent;
        this.world = world;

        for (int i = 0; i < MAX_PEDESTRIANS; i++) {
            models[i] = buildPedestrian(i);
            models[i].setRenderingEnable(false);
            root.addChild(models[i]);
        }
    }

    private Mesh buildPedestrian(int index) {
        MeshBuilder mb = new MeshBuilder(48, 0.02f);

        int[] colors = {
            0x3569B7,
            0xB84C38,
            0x4D9857,
            0xB58A32,
            0x8056A8
        };

        int shirt = colors[index % colors.length];

        // Body.
        mb.box(
            -0.28f, 0.55f, -0.16f,
             0.28f, 1.35f,  0.16f,
            shirt
        );

        // Head.
        mb.box(
            -0.20f, 1.35f, -0.18f,
             0.20f, 1.75f,  0.18f,
            0xD6A477
        );

        return mb.build();
    }

    /**
     * Call once when the player enters the world.
     */
    public void spawn(float playerX, float playerZ) {
        activeCount = 0;

        for (int i = 0; i < MAX_PEDESTRIANS; i++) {
            active[i] = false;
            models[i].setRenderingEnable(false);

            if (findSpawnPosition(i, playerX, playerZ)) {
                active[i] = true;
                activeCount++;

                chooseDestination(i);

                models[i].setTranslation(
                    x[i], 0f, z[i]
                );

                models[i].setRenderingEnable(true);
            }
        }
    }

    /**
     * Finds a position away from the player and roads.
     */
    private boolean findSpawnPosition(
        int index,
        float playerX,
        float playerZ
    ) {
        for (int attempt = 0; attempt < 30; attempt++) {
            float angle = random.nextFloat() * 6.2831853f;
            float distance = 12f + random.nextFloat() * 38f;

            float px = playerX +
                (float)Math.sin(angle) * distance;

            float pz = playerZ +
                (float)Math.cos(angle) * distance;

            int surface = World3D.surface(px, pz);

            // Avoid roads.
            if (surface == 0 || surface == 1) {
                continue;
            }

            x[index] = px;
            z[index] = pz;
            heading[index] = angle;
            wait[index] = random.nextFloat() * 2f;

            return true;
        }

        return false;
    }

    private void chooseDestination(int index) {
        for (int attempt = 0; attempt < 12; attempt++) {
            float angle = random.nextFloat() * 6.2831853f;
            float distance = 5f + random.nextFloat() * 14f;

            float tx = x[index] +
                (float)Math.sin(angle) * distance;

            float tz = z[index] +
                (float)Math.cos(angle) * distance;

            int surface = World3D.surface(tx, tz);

            if (surface == 0 || surface == 1) {
                continue;
            }

            targetX[index] = tx;
            targetZ[index] = tz;
            heading[index] = angle;
            return;
        }

        wait[index] = 1.5f;
        targetX[index] = x[index];
        targetZ[index] = z[index];
    }

    /**
     * Update pedestrians.
     *
     * dt is elapsed time in seconds.
     */
    public void update(
        float dt,
        float playerX,
        float playerZ
    ) {
        if (dt <= 0f) return;

        // Prevent large movement after a paused frame.
        if (dt > 0.1f) dt = 0.1f;

        for (int i = 0; i < MAX_PEDESTRIANS; i++) {
            if (!active[i]) {
                if (findSpawnPosition(i, playerX, playerZ)) {
                    active[i] = true;
                    activeCount++;

                    chooseDestination(i);

                    models[i].setTranslation(
                        x[i], 0f, z[i]
                    );

                    models[i].setRenderingEnable(true);
                }
                continue;
            }

            float dx = x[i] - playerX;
            float dz = z[i] - playerZ;

            if (dx * dx + dz * dz >
                DESPAWN_DISTANCE * DESPAWN_DISTANCE) {

                active[i] = false;
                activeCount--;

                models[i].setRenderingEnable(false);
                continue;
            }

            if (wait[i] > 0f) {
                wait[i] -= dt;

                if (wait[i] <= 0f) {
                    chooseDestination(i);
                }

                continue;
            }

            float moveX = targetX[i] - x[i];
            float moveZ = targetZ[i] - z[i];

            float distance = (float)Math.sqrt(
                moveX * moveX + moveZ * moveZ
            );

            if (distance < ARRIVE_DISTANCE) {
                wait[i] = 0.5f + random.nextFloat() * 2f;
                chooseDestination(i);
                continue;
            }

            if (distance > 0.001f) {
                moveX /= distance;
                moveZ /= distance;
            }

            // Use Car's compatible atan2 implementation.
            heading[i] = Car.atan2(moveX, moveZ);

            float step = WALK_SPEED * dt;

            if (step > distance) {
                step = distance;
            }

            float nextX = x[i] + moveX * step;
            float nextZ = z[i] + moveZ * step;

            collision[i][0] = nextX;
            collision[i][1] = nextZ;

            boolean blocked = world.resolve(
                collision[i],
                COLLISION_RADIUS
            );

            if (blocked) {
                chooseDestination(i);
                wait[i] = 0.4f;
                continue;
            }

            // Do not let pedestrians walk onto roads.
            int surface = World3D.surface(nextX, nextZ);

            if (surface == 0 || surface == 1) {
                chooseDestination(i);
                continue;
            }

            x[i] = nextX;
            z[i] = nextZ;

            models[i].setTranslation(
                x[i], 0f, z[i]
            );

            models[i].setOrientation(
                heading[i] * 57.29578f,
                0f, 1f, 0f
            );
        }
    }

    public void clear() {
        for (int i = 0; i < MAX_PEDESTRIANS; i++) {
            if (active[i]) {
                active[i] = false;
                models[i].setRenderingEnable(false);
            }
        }

        activeCount = 0;
    }

    public int getActiveCount() {
        return activeCount;
    }

    public boolean isActive(int index) {
        if (index < 0 || index >= MAX_PEDESTRIANS) {
            return false;
        }

        return active[index];
    }

    public float getX(int index) {
        if (index < 0 || index >= MAX_PEDESTRIANS) {
            return 0f;
        }

        return x[index];
    }

    public float getZ(int index) {
        if (index < 0 || index >= MAX_PEDESTRIANS) {
            return 0f;
        }

        return z[index];
    }
}
