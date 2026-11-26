
import javax.microedition.m3g.*;

/**
 * Lightweight projectile system for RallyCity.
 * Handles firing, projectile movement and world collision.
 *
 * Designed for CLDC 1.1, MIDP 2.1 and M3G.
 */
public class WeaponSystem {

    public static final int MAX_BULLETS = 8;

    public static final int START_AMMO = 60;

    private static final float BULLET_SPEED = 55f;
    private static final float BULLET_LIFE = 1.8f;

    private Mesh[] bullets = new Mesh[MAX_BULLETS];

    private float[] x = new float[MAX_BULLETS];
    private float[] y = new float[MAX_BULLETS];
    private float[] z = new float[MAX_BULLETS];

    private float[] vx = new float[MAX_BULLETS];
    private float[] vz = new float[MAX_BULLETS];

    private float[] life = new float[MAX_BULLETS];

    private boolean[] active =
        new boolean[MAX_BULLETS];

    private int ammo = START_AMMO;

    public WeaponSystem(Group root) {
        for (int i = 0; i < MAX_BULLETS; i++) {
            bullets[i] = buildBullet();
            bullets[i].setRenderingEnable(false);
            root.addChild(bullets[i]);
        }
    }

    private Mesh buildBullet() {
        MeshBuilder b = new MeshBuilder(24, 0.02f);

        b.box(
            -0.035f, -0.035f, -0.12f,
             0.035f,  0.035f,  0.12f,
            0xFFD94A
        );

        return b.build();
    }

    /**
     * Fires one projectile in the supplied heading.
     * Returns false if there is no ammunition or free slot.
     */
    public boolean fire(float px, float py, float pz,
                        float heading) {
        if (ammo <= 0) return false;

        int slot = -1;

        for (int i = 0; i < MAX_BULLETS; i++) {
            if (!active[i]) {
                slot = i;
                break;
            }
        }

        if (slot < 0) return false;

        float sn = (float)Math.sin(heading);
        float cs = (float)Math.cos(heading);

        x[slot] = px + sn * 1.5f;
        y[slot] = py;
        z[slot] = pz + cs * 1.5f;

        vx[slot] = sn * BULLET_SPEED;
        vz[slot] = cs * BULLET_SPEED;

        life[slot] = BULLET_LIFE;
        active[slot] = true;

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
     * Projectiles disappear after hitting world geometry
     * or reaching their lifetime limit.
     */
    public void update(float dt, World3D world) {
        for (int i = 0; i < MAX_BULLETS; i++) {
            if (!active[i]) continue;

            life[i] -= dt;

            if (life[i] <= 0f) {
                deactivate(i);
                continue;
            }

            float oldX = x[i];
            float oldZ = z[i];

            x[i] += vx[i] * dt;
            z[i] += vz[i] * dt;

            float[] p = new float[2];
            p[0] = x[i];
            p[1] = z[i];

            if (world != null && world.resolve(p, 0.15f)) {
                deactivate(i);
                continue;
            }

            x[i] = p[0];
            z[i] = p[1];

            bullets[i].setTranslation(
                x[i], y[i], z[i]
            );

            // Prevent a projectile from moving backwards
            // if a collision adjusted its position.
            if (oldX == x[i] && oldZ == z[i]) {
                deactivate(i);
            }
        }
    }

    private void deactivate(int i) {
        active[i] = false;
        bullets[i].setRenderingEnable(false);
    }

    public void addAmmo(int amount) {
        if (amount <= 0) return;

        ammo += amount;

        if (ammo > 999) {
            ammo = 999;
        }
    }

    public int getAmmo() {
        return ammo;
    }

    public boolean hasAmmo() {
        return ammo > 0;
    }

    public boolean isActive(int i) {
        if (i < 0 || i >= MAX_BULLETS) return false;
        return active[i];
    }

    public float getX(int i) {
        if (i < 0 || i >= MAX_BULLETS) return 0f;
        return x[i];
    }

    public float getY(int i) {
        if (i < 0 || i >= MAX_BULLETS) return 0f;
        return y[i];
    }

    public float getZ(int i) {
        if (i < 0 || i >= MAX_BULLETS) return 0f;
        return z[i];
    }
}
