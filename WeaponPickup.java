
import javax.microedition.m3g.*;

/**
 * Lightweight weapon and ammunition pickups.
 *
 * Place pickups in the world and call update()
 * to collect them when the player gets close.
 */
public class WeaponPickup {

    public static final int AMMO_PISTOL = 0;
    public static final int AMMO_SHOTGUN = 1;
    public static final int AMMO_SMG = 2;

    public static final int WEAPON_SHOTGUN = 3;
    public static final int WEAPON_SMG = 4;

    public static final int MAX_PICKUPS = 8;

    private static final float PICKUP_DISTANCE = 1.8f;

    private Group root;
    private Mesh[] meshes;

    private float[] x;
    private float[] z;

    private int[] type;
    private int[] amount;

    private boolean[] active;

    private int count;

    public WeaponPickup(Group parent) {
        root = new Group();
        parent.addChild(root);

        meshes = new Mesh[MAX_PICKUPS];
        x = new float[MAX_PICKUPS];
        z = new float[MAX_PICKUPS];
        type = new int[MAX_PICKUPS];
        amount = new int[MAX_PICKUPS];
        active = new boolean[MAX_PICKUPS];

        count = 0;

        createMeshes();
    }

    private void createMeshes() {
        for (int i = 0; i < MAX_PICKUPS; i++) {
            MeshBuilder mb = new MeshBuilder(40, 0.1f);

            int color = 0xFFD21F;

            mb.box(-0.22f, 0f, -0.22f,
                    0.22f, 0.35f, 0.22f, color);

            meshes[i] = mb.build();

            meshes[i].setRenderingEnable(false);
            root.addChild(meshes[i]);
        }
    }

    /**
     * Add a pickup.
     *
     * Types:
     * AMMO_PISTOL, AMMO_SHOTGUN, AMMO_SMG,
     * WEAPON_SHOTGUN, WEAPON_SMG.
     */
    public boolean add(float px, float pz,
                       int pickupType, int pickupAmount) {

        if (pickupType < AMMO_PISTOL ||
            pickupType > WEAPON_SMG) {
            return false;
        }

        for (int i = 0; i < MAX_PICKUPS; i++) {
            if (!active[i]) {
                x[i] = px;
                z[i] = pz;
                type[i] = pickupType;
                amount[i] = pickupAmount;

                active[i] = true;

                meshes[i].setTranslation(px, 0.1f, pz);
                meshes[i].setRenderingEnable(true);

                if (count < MAX_PICKUPS) {
                    count++;
                }

                return true;
            }
        }

        return false;
    }

    /**
     * Check whether the player collected a pickup.
     * Returns the number of pickups collected.
     */
    public int update(float playerX, float playerZ,
                      WeaponInventory inventory) {

        if (inventory == null) return 0;

        int collected = 0;
        float limit = PICKUP_DISTANCE * PICKUP_DISTANCE;

        for (int i = 0; i < MAX_PICKUPS; i++) {
            if (!active[i]) continue;

            float dx = playerX - x[i];
            float dz = playerZ - z[i];

            if (dx * dx + dz * dz > limit) {
                continue;
            }

            boolean accepted = false;

            switch (type[i]) {
                case AMMO_PISTOL:
                    inventory.addAmmo(
                        WeaponInventory.PISTOL, amount[i]);
                    accepted = true;
                    break;

                case AMMO_SHOTGUN:
                    if (inventory.isUnlocked(
                        WeaponInventory.SHOTGUN)) {
                        inventory.addAmmo(
                            WeaponInventory.SHOTGUN, amount[i]);
                        accepted = true;
                    }
                    break;

                case AMMO_SMG:
                    if (inventory.isUnlocked(
                        WeaponInventory.SMG)) {
                        inventory.addAmmo(
                            WeaponInventory.SMG, amount[i]);
                        accepted = true;
                    }
                    break;

                case WEAPON_SHOTGUN:
                    accepted = inventory.unlockWeapon(
                        WeaponInventory.SHOTGUN);
                    break;

                case WEAPON_SMG:
                    accepted = inventory.unlockWeapon(
                        WeaponInventory.SMG);
                    break;
            }

            if (accepted) {
                active[i] = false;
                meshes[i].setRenderingEnable(false);
                count--;
                collected++;
            }
        }

        return collected;
    }

    public void clear() {
        for (int i = 0; i < MAX_PICKUPS; i++) {
            active[i] = false;
            meshes[i].setRenderingEnable(false);
        }

        count = 0;
    }

    public int getActiveCount() {
        return count;
    }

    public boolean isActive(int index) {
        if (index < 0 || index >= MAX_PICKUPS) {
            return false;
        }

        return active[index];
    }

    public float getX(int index) {
        if (index < 0 || index >= MAX_PICKUPS) {
            return 0f;
        }

        return x[index];
    }

    public float getZ(int index) {
        if (index < 0 || index >= MAX_PICKUPS) {
            return 0f;
        }

        return z[index];
    }
}
