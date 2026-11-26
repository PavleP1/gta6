
/**
 * RallyCity combat controller.
 *
 * Connects WeaponInventory with WeaponSystem.
 * Player input and scene integration are handled later.
 */
public class CombatSystem {

    private WeaponInventory inventory;
    private WeaponSystem projectiles;

    private float reloadTimer;
    private boolean reloading;

    public CombatSystem(WeaponInventory inv,
                        WeaponSystem weapons) {
        if (inv == null || weapons == null) {
            throw new IllegalArgumentException(
                "Combat systems cannot be null");
        }

        inventory = inv;
        projectiles = weapons;

        reloadTimer = 0f;
        reloading = false;
    }

    /**
     * Update weapon cooldowns and reload progress.
     */
    public void update(float dt) {
        if (dt <= 0f) return;

        inventory.update(dt);

        if (reloading) {
            reloadTimer -= dt;

            if (reloadTimer <= 0f) {
                reloading = false;
                reloadTimer = 0f;
                inventory.reload();
            }
        }
    }

    /**
     * Fire the selected weapon.
     *
     * Coordinates are the player's firing position.
     * Heading is in radians.
     */
    public boolean fire(float x, float y, float z,
                        float heading) {
        if (reloading) return false;
        if (!inventory.canFire()) return false;

        if (!projectiles.fire(x, y, z, heading)) {
            return false;
        }

        inventory.consumeShot();
        return true;
    }

    /**
     * Begin reloading the selected weapon.
     */
    public boolean reload() {
        if (reloading) return false;
        if (!inventory.hasReserveAmmo()) return false;

        int weapon = inventory.getSelectedWeapon();

        if (inventory.getLoadedAmmo() >=
            inventory.getMagazineSize(weapon)) {
            return false;
        }

        reloading = true;
        reloadTimer = getReloadDuration(weapon);

        return true;
    }

    private float getReloadDuration(int weapon) {
        switch (weapon) {
            case WeaponInventory.SHOTGUN:
                return 1.8f;

            case WeaponInventory.SMG:
                return 1.5f;

            default:
                return 1.3f;
        }
    }

    public boolean selectWeapon(int weapon) {
        if (reloading) return false;
        return inventory.selectWeapon(weapon);
    }

    public boolean unlockWeapon(int weapon) {
        return inventory.unlockWeapon(weapon);
    }

    public void addAmmo(int weapon, int amount) {
        inventory.addAmmo(weapon, amount);
    }

    public void cancelReload() {
        reloading = false;
        reloadTimer = 0f;
    }

    public boolean isReloading() {
        return reloading;
    }

    public float getReloadTimer() {
        return reloadTimer;
    }

    public int getSelectedWeapon() {
        return inventory.getSelectedWeapon();
    }

    public int getLoadedAmmo() {
        return inventory.getLoadedAmmo();
    }

    public int getReserveAmmo() {
        return inventory.getReserveAmmo();
    }

    public String getWeaponName() {
        return inventory.getWeaponName(
            inventory.getSelectedWeapon());
    }

    public WeaponInventory getInventory() {
        return inventory;
    }

    public WeaponSystem getProjectiles() {
        return projectiles;
    }

    public void reset() {
        cancelReload();
        inventory.reset();
        projectiles.clear();
        projectiles.setAmmo(WeaponSystem.START_AMMO);
    }
}
