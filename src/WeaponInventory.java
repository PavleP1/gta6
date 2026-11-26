
/**
 * RallyCity weapon inventory.
 *
 * Manages weapon selection, loaded ammunition, reserve
 * ammunition, reloads and firing delays.
 *
 * Compatible with CLDC 1.1 and MIDP 2.1.
 *
 * Projectile creation is handled separately by WeaponSystem.
 */
public class WeaponInventory {

    public static final int PISTOL = 0;
    public static final int SHOTGUN = 1;
    public static final int SMG = 2;

    public static final int WEAPON_COUNT = 3;

    private static final int[] MAGAZINE_SIZE = {
        12, 6, 30
    };

    private static final int[] START_RESERVE = {
        48, 24, 120
    };

    private static final float[] FIRE_DELAY = {
        0.30f, 0.75f, 0.10f
    };

    private static final int[] AMMO_LIMIT = {
        240, 120, 600
    };

    private int[] magazine = new int[WEAPON_COUNT];
    private int[] reserve = new int[WEAPON_COUNT];

    private boolean[] unlocked = new boolean[WEAPON_COUNT];

    private int selectedWeapon = PISTOL;

    private float fireTimer;

    public WeaponInventory() {
        for (int i = 0; i < WEAPON_COUNT; i++) {
            magazine[i] = 0;
            reserve[i] = 0;
            unlocked[i] = false;
        }

        unlocked[PISTOL] = true;

        magazine[PISTOL] = MAGAZINE_SIZE[PISTOL];
        reserve[PISTOL] = START_RESERVE[PISTOL];

        fireTimer = 0f;
    }

    /**
     * Select a weapon.
     * Returns false if the weapon is locked or invalid.
     */
    public boolean selectWeapon(int weapon) {
        if (!isValidWeapon(weapon)) return false;
        if (!unlocked[weapon]) return false;

        selectedWeapon = weapon;
        fireTimer = 0f;

        return true;
    }

    /**
     * Unlock a weapon.
     */
    public boolean unlockWeapon(int weapon) {
        if (!isValidWeapon(weapon)) return false;

        if (!unlocked[weapon]) {
            unlocked[weapon] = true;

            magazine[weapon] = MAGAZINE_SIZE[weapon];
            reserve[weapon] = START_RESERVE[weapon];
        }

        return true;
    }

    /**
     * Advance the firing cooldown.
     * Call this from the game update loop.
     */
    public void update(float dt) {
        if (dt <= 0f) return;

        fireTimer -= dt;

        if (fireTimer < 0f) {
            fireTimer = 0f;
        }
    }

    /**
     * Check whether the selected weapon can fire.
     */
    public boolean canFire() {
        if (!unlocked[selectedWeapon]) return false;
        if (fireTimer > 0f) return false;
        if (magazine[selectedWeapon] <= 0) return false;

        return true;
    }

    /**
     * Consume one round and start the firing cooldown.
     * Returns false if firing is not currently possible.
     */
    public boolean consumeShot() {
        if (!canFire()) return false;

        magazine[selectedWeapon]--;

        fireTimer = FIRE_DELAY[selectedWeapon];

        return true;
    }

    /**
     * Reload the selected weapon.
     * Returns false if it cannot be reloaded.
     */
    public boolean reload() {
        int weapon = selectedWeapon;

        if (!unlocked[weapon]) return false;

        int capacity = MAGAZINE_SIZE[weapon];

        if (magazine[weapon] >= capacity) {
            return false;
        }

        if (reserve[weapon] <= 0) {
            return false;
        }

        int needed = capacity - magazine[weapon];

        int amount = needed;

        if (amount > reserve[weapon]) {
            amount = reserve[weapon];
        }

        magazine[weapon] += amount;
        reserve[weapon] -= amount;

        fireTimer = 0f;

        return true;
    }

    /**
     * Add reserve ammunition to a weapon.
     */
    public void addAmmo(int weapon, int amount) {
        if (!isValidWeapon(weapon)) return;
        if (amount <= 0) return;
        if (!unlocked[weapon]) return;

        reserve[weapon] += amount;

        if (reserve[weapon] > AMMO_LIMIT[weapon]) {
            reserve[weapon] = AMMO_LIMIT[weapon];
        }
    }

    /**
     * Add ammunition to the currently selected weapon.
     */
    public void addAmmo(int amount) {
        addAmmo(selectedWeapon, amount);
    }

    /**
     * Set ammunition values.
     * Useful for loading saved game data.
     */
    public void setAmmo(
        int weapon,
        int loaded,
        int spare
    ) {
        if (!isValidWeapon(weapon)) return;

        if (loaded < 0) loaded = 0;
        if (spare < 0) spare = 0;

        if (loaded > MAGAZINE_SIZE[weapon]) {
            loaded = MAGAZINE_SIZE[weapon];
        }

        if (spare > AMMO_LIMIT[weapon]) {
            spare = AMMO_LIMIT[weapon];
        }

        magazine[weapon] = loaded;
        reserve[weapon] = spare;
    }

    /**
     * Set a weapon's unlocked state.
     */
    public void setUnlocked(int weapon, boolean value) {
        if (!isValidWeapon(weapon)) return;

        if (weapon == PISTOL && !value) {
            return;
        }

        unlocked[weapon] = value;

        if (!value && selectedWeapon == weapon) {
            selectedWeapon = PISTOL;
            fireTimer = 0f;
        }
    }

    public int getSelectedWeapon() {
        return selectedWeapon;
    }

    public int getLoadedAmmo() {
        return magazine[selectedWeapon];
    }

    public int getReserveAmmo() {
        return reserve[selectedWeapon];
    }

    public int getLoadedAmmo(int weapon) {
        if (!isValidWeapon(weapon)) return 0;

        return magazine[weapon];
    }

    public int getReserveAmmo(int weapon) {
        if (!isValidWeapon(weapon)) return 0;

        return reserve[weapon];
    }

    public int getMagazineSize(int weapon) {
        if (!isValidWeapon(weapon)) return 0;

        return MAGAZINE_SIZE[weapon];
    }

    public float getFireDelay(int weapon) {
        if (!isValidWeapon(weapon)) return 0f;

        return FIRE_DELAY[weapon];
    }

    public float getFireTimer() {
        return fireTimer;
    }

    public boolean isUnlocked(int weapon) {
        if (!isValidWeapon(weapon)) return false;

        return unlocked[weapon];
    }

    public boolean hasAmmo() {
        return magazine[selectedWeapon] > 0;
    }

    public boolean hasReserveAmmo() {
        return reserve[selectedWeapon] > 0;
    }

    public boolean isValidWeapon(int weapon) {
        return weapon >= 0 && weapon < WEAPON_COUNT;
    }

    public String getWeaponName(int weapon) {
        switch (weapon) {
            case PISTOL:
                return "Pistol";

            case SHOTGUN:
                return "Shotgun";

            case SMG:
                return "SMG";
        }

        return "Unknown";
    }

    /**
     * Restore the default inventory.
     */
    public void reset() {
        for (int i = 0; i < WEAPON_COUNT; i++) {
            magazine[i] = 0;
            reserve[i] = 0;
            unlocked[i] = false;
        }

        unlocked[PISTOL] = true;

        magazine[PISTOL] = MAGAZINE_SIZE[PISTOL];
        reserve[PISTOL] = START_RESERVE[PISTOL];

        selectedWeapon = PISTOL;
        fireTimer = 0f;
    }
}
