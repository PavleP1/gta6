
import javax.microedition.m3g.*;

/**
 * RallyCity weapon projectile manager.
 *
 * Features:
 * - Multiple simultaneous projectiles
 * - Ammunition tracking
 * - Projectile lifetime
 * - World collision detection
 * - Reusable collision data to reduce garbage collection
 *
 * Compatible with CLDC 1.1, MIDP 2.1 and M3G (JSR-184).
 *
 * This class manages projectiles only. Player controls, weapon
 * selection, damage, NPC reactions and firing input will be
 * connected later through GameView.
 */
public class WeaponSystem {

    public static final int MAX_BULLETS = 8;
    public static final int START_AMMO = 60;

    private static final float BULLET_SPEED = 55f;
    private static final float BULLET_LIFE = 1.8f;
    private static final float BULLET_RADIUS = 0.15f;
    private static final float MAX_STEP = 1.0f;

    private Mesh[] bullets = new Mesh[MAX_BULLETS];

    private float[] x = new float[MAX_BULLETS];
    private float[] y = new float[MAX_BULLETS];
    private float[] z = new float[MAX_BULLETS];

    private float[] vx = new float[MAX_BULLETS];
    private float[] vz = new float[MAX_BULLETS];

    private float[] life = new float[MAX_BULLETS];

    private boolean[] active = new boolean[MAX_BULLETS];

    private float[] collisionPoint = new float[2];

    private int ammo = START_AMMO;

    private int activeCount;

    /**
     * Creates the projectile meshes and attaches them to the
     * supplied scene group.
     */
    public WeaponSystem(Group root) {
        if (root == null) {
            throw new IllegalArgumentException("Root group is null");
        }

        for (int i = 0; i < MAX_BULLETS; i++) {
            bullets[i] = buildBullet();
            bullets[i].setRenderingEnable(false);
            root.addChild(bullets[i]);
        }
    }

    /**
     * Builds a small visible projectile.
     */
    private Mesh buildBullet() {
        MeshBuilder builder = new MeshBuilder(24, 0.02f);

        builder.box(
            -0.035f, -0.035f, -0.12f,
             0.035f,  0.035f,  0.12f,
            0xFFD94A
        );

        return builder.build();
    }

    /**
     * Fires a projectile.
     *
     * px, py, pz = firing position.
     * heading = direction in radians.
     *
     * Returns false if ammunition is empty or all projectile
     * slots are occupied.
     */
    public boolean fire(float px, float py, float pz,
                        float heading) {

        if (ammo <= 0) {
            return false;
        }

        int slot = -1;

        for (int i = 0; i < MAX_BULLETS; i++) {
            if (!active[i]) {
                slot = i;
                break;
            }
        }

        if (slot < 0) {
            return false;
        }

        float sn = (float)Math.sin(heading);
        float cs = (float)Math.cos(heading);

        x[slot] = px + sn * 1.5f;
        y[slot] = py;
        z[slot] = pz + cs * 1.5f;

        vx[slot] = sn * BULLET_SPEED;
        vz[slot] = cs * BULLET_SPEED;

        life[slot] = BULLET_LIFE;

        active[slot] = true;
        activeCount++;

        bullets[slot].setTranslation(
            x[slot], y[slot], z[slot]
        );

        bullets[slot].setOrientation(
            heading * 57.29578f,
            0f, 1f, 0f
        );

        bullets[slot].setRenderingEnable(true);

        ammo--;

        return true;
    }

    /**
     * Updates all active projectiles.
     *
     * Movement is divided into small steps to reduce the
     * chance of projectiles passing through obstacles.
     */
    public void update(float dt, World3D world) {

        if (dt <= 0f) {
            return;
        }

        for (int i = 0; i < MAX_BULLETS; i++) {

            if (!active[i]) {
                continue;
            }

            life[i] -= dt;

            if (life[i] <= 0f) {
                deactivate(i);
                continue;
            }

            float moveX = vx[i] * dt;
            float moveZ = vz[i] * dt;

            float distance = (float)Math.sqrt(
                moveX * moveX + moveZ * moveZ
            );

            int steps = (int)Math.ceil(distance / MAX_STEP);

            if (steps < 1) {
                steps = 1;
            }

            float stepX = moveX / steps;
            float stepZ = moveZ / steps;

            boolean collided = false;

            for (int s = 0; s < steps; s++) {

                collisionPoint[0] = x[i] + stepX;
                collisionPoint[1] = z[i] + stepZ;

                if (world != null &&
                    world.resolve(collisionPoint, BULLET_RADIUS)) {

                    collided = true;
                    break;
                }

                x[i] = collisionPoint[0];
                z[i] = collisionPoint[1];
            }

            if (collided) {
                deactivate(i);
                continue;
            }

            bullets[i].setTranslation(
                x[i], y[i], z[i]
            );
        }
    }

    /**
     * Deactivates one projectile and hides its mesh.
     */
    private void deactivate(int index) {

        if (index < 0 || index >= MAX_BULLETS) {
            return;
        }

        if (!active[index]) {
            return;
        }

        active[index] = false;

        if (activeCount > 0) {
            activeCount--;
        }

        bullets[index].setRenderingEnable(false);
    }

    /**
     * Deactivates every projectile.
     */
    public void clear() {

        for (int i = 0; i < MAX_BULLETS; i++) {
            deactivate(i);
        }
    }

    /**
     * Adds ammunition.
     */
    public void addAmmo(int amount) {

        if (amount <= 0) {
            return;
        }

        ammo += amount;

        if (ammo > 999) {
            ammo = 999;
        }
    }

    /**
     * Refills ammunition to the starting amount.
     */
    public void reload() {
        ammo = START_AMMO;
    }

    /**
     * Sets ammunition directly.
     */
    public void setAmmo(int amount) {

        if (amount < 0) {
            amount = 0;
        }

        if (amount > 999) {
            amount = 999;
        }

        ammo = amount;
    }

    /**
     * Returns remaining ammunition.
     */
    public int getAmmo() {
        return ammo;
    }

    /**
     * Returns true if ammunition is available.
     */
    public boolean hasAmmo() {
        return ammo > 0;
    }

    /**
     * Returns the number of active projectiles.
     */
    public int getActiveCount() {
        return activeCount;
    }

    /**
     * Checks whether a projectile slot is active.
     */
    public boolean isActive(int index) {

        if (index < 0 || index >= MAX_BULLETS) {
            return false;
        }

        return active[index];
    }

    public float getX(int index) {

        if (index < 0 || index >= MAX_BULLETS) {
            return 0f;
        }

        return x[index];
    }

    public float getY(int index) {

        if (index < 0 || index >= MAX_BULLETS) {
            return 0f;
        }

        return y[index];
    }

    public float getZ(int index) {

        if (index < 0 || index >= MAX_BULLETS) {
            return 0f;
        }

        return z[index];
    }
}
