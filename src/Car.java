
import javax.microedition.m3g.*;

/**
 * RallyCity vehicle physics with fuel and damage.
 * Compatible with CLDC 1.1 / MIDP 2.1.
 */
public class Car {
    public static final float PI = 3.1415927f;
    public static final float R = 1.7f;
    public static final float MAXV = 36f;

    public float x, z, h, vx, vz, steer, slip;
    public Mesh mesh;

    public float fuel = 100f;
    public float damage = 0f;

    public Car(int color, boolean police) {
        mesh = buildMesh(color, police);
    }

    public void place(float px, float pz, float ph) {
        x = px;
        z = pz;
        h = ph;
        vx = 0;
        vz = 0;
        steer = 0;
    }

    public float speed() {
        return (float)Math.sqrt(vx * vx + vz * vz);
    }

    public void refuel(float amount) {
        fuel += amount;
        if (fuel > 100f) fuel = 100f;
        if (fuel < 0f) fuel = 0f;
    }

    public void repair(float amount) {
        damage -= amount;
        if (damage < 0f) damage = 0f;
    }

    public void refuelAll() {
        fuel = 100f;
    }

    public void repairAll() {
        damage = 0f;
    }

    public void addDamage(float amount) {
        damage += amount;
        if (damage > 100f) damage = 100f;
    }

    public boolean isOutOfFuel() {
        return fuel <= 0.1f;
    }

    public boolean isHeavilyDamaged() {
        return damage >= 75f;
    }

    /**
     * surf: 0 asphalt, 1 dirt road, 2 pavement, 3 grass/offroad.
     */
    public void update(float dt, boolean gas, boolean brake,
                       boolean left, boolean right,
                       boolean hb, int surf) {
        float grip, drag;

        switch (surf) {
            case 0:
                grip = 9f;
                drag = 0.25f;
                break;
            case 1:
                grip = 4.2f;
                drag = 0.45f;
                break;
            case 2:
                grip = 8f;
                drag = 0.45f;
                break;
            default:
                grip = 3f;
                drag = 1.0f;
                break;
        }

        float tgt = (right ? 1f : 0f) -
                    (left ? 1f : 0f);

        steer += (tgt - steer) * Math.min(1f, dt * 7f);

        float sn = (float)Math.sin(h);
        float cs = (float)Math.cos(h);

        float vf = vx * sn + vz * cs;
        float vl = -vx * cs + vz * sn;

        slip = vl;

        /*
         * Fuel is consumed only while accelerating.
         * Consumption rises slightly with speed.
         */
        if (gas && fuel > 0.1f) {
            float consumption = 0.018f +
                                Math.abs(vf) * 0.0008f;

            fuel -= consumption * dt;

            if (fuel < 0f) fuel = 0f;
        }

        /*
         * Low fuel and damage reduce engine power.
         */
        float fuelPower = fuel <= 0.1f ? 0f :
                          (fuel < 15f ? 0.55f : 1f);

        float damagePower = 1f - damage * 0.004f;

        if (damagePower < 0.45f) {
            damagePower = 0.45f;
        }

        float enginePower = fuelPower * damagePower;

        if (gas && enginePower > 0f) {
            if (vf < -0.5f) {
                vf += 30f * enginePower * dt;
            } else {
                vf += 18f * enginePower *
                      (1f - vf / MAXV) * dt;
            }
        }

        if (brake) {
            if (vf > 0.5f) {
                vf -= 30f * dt;
            } else if (vf > -9f) {
                vf -= 9f * dt;
            }
        }

        vf -= vf * drag * dt;

        float g = grip;

        if (hb) {
            g = 1.1f;
            vf -= vf * 0.9f * dt;
        }

        vl -= vl * Math.min(1f, g * dt);

        float sa = steer * 0.55f /
                   (1f + Math.abs(vf) * 0.1f);

        float yaw = vf * sa / 2.6f;

        if (hb) yaw *= 1.7f;

        /*
         * Heavy damage makes steering less responsive.
         */
        yaw *= 1f - damage * 0.003f;

        vx = sn * vf - cs * vl;
        vz = cs * vf + sn * vl;

        x += vx * dt;
        z += vz * dt;

        h -= yaw * dt;
    }

    public void bounce(float nx, float nz) {
        float vn = vx * nx + vz * nz;

        if (vn < 0f) {
            vx -= 1.3f * vn * nx;
            vz -= 1.3f * vn * nz;

            vx *= 0.85f;
            vz *= 0.85f;
        }
    }

    public void sync() {
        put(mesh, x, z, h);
    }

    public static void put(Mesh m, float px,
                           float pz, float hr) {
        m.setTranslation(px, 0.3f, pz);
        m.setOrientation(
            hr * 57.29578f,
            0f, 1f, 0f
        );
    }

    public static float atan2(float y, float x) {
        if (x == 0f && y == 0f) return 0f;

        float ax = Math.abs(x);
        float ay = Math.abs(y);
        float a;

        if (ax >= ay) {
            float q = ay / ax;
            a = q * (0.9724f - 0.1919f * q * q);
        } else {
            float q = ax / ay;
            a = 1.5707963f -
                q * (0.9724f - 0.1919f * q * q);
        }

        if (x < 0f) a = PI - a;
        if (y < 0f) a = -a;

        return a;
    }

    public static float norm(float a) {
        while (a > PI) a -= 2f * PI;
        while (a < -PI) a += 2f * PI;
        return a;
    }

    /** Textured vehicle mesh. Faces +Z. */
    public static Mesh buildMesh(int color,
                                 boolean police) {
        MeshBuilder b = new MeshBuilder(300, 0.05f);

        b.flat(
            -1.05f, -2.2f,
            1.05f, 2.2f,
            0.02f, 0x1A1A1A
        );

        b.boxT(
            -0.9f, 0.35f, -2.1f,
            0.9f, 0.95f, 2.1f,
            color, Mats.PAINT, Mats.PAINT, 2f
        );

        b.boxT(
            -0.75f, 0.95f, -1.1f,
            0.75f, 1.45f, 0.9f,
            0xFFFFFF, Mats.GLASS, Mats.GLASS, 1.5f
        );

        b.boxT(
            0.78f, 0f, 1.2f,
            1.0f, 0.7f, 1.9f,
            0xFFFFFF, Mats.WHEEL, Mats.WHEEL, 0.7f
        );

        b.boxT(
            -1.0f, 0f, 1.2f,
            -0.78f, 0.7f, 1.9f,
            0xFFFFFF, Mats.WHEEL, Mats.WHEEL, 0.7f
        );

        b.boxT(
            0.78f, 0f, -1.9f,
            1.0f, 0.7f, -1.2f,
            0xFFFFFF, Mats.WHEEL, Mats.WHEEL, 0.7f
        );

        b.boxT(
            -1.0f, 0f, -1.9f,
            -0.78f, 0.7f, -1.2f,
            0xFFFFFF, Mats.WHEEL, Mats.WHEEL, 0.7f
        );

        b.box(
            -0.8f, 0.55f, 2.1f,
            0.8f, 0.75f, 2.14f,
            0xFFF2B0
        );

        b.box(
            -0.8f, 0.55f, -2.14f,
            0.8f, 0.75f, -2.1f,
            0xC01818
        );

        if (police) {
            b.box(
                -0.45f, 1.45f, -0.2f,
                0f, 1.6f, 0.2f,
                0xE02020
            );

            b.box(
                0f, 1.45f, -0.2f,
                0.45f, 1.6f, 0.2f,
                0x2040E0
            );
        }

        return b.build();
    }
}
