
/**
 * RallyCity combat controller.
 *
 * WeaponInventory is the authoritative source for ammunition.
 * WeaponSystem handles projectile creation and movement.
 *
 * Compatible with CLDC 1.1 and MIDP 2.1.
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
                "Combat systems cannot be null"
            );
        }

        inventory = inv;
        projectiles = weapons;

        reloadTimer = 0f;
        reloading = false;

        syncAmmo();
    }

    /**
     * Synchronize projectile ammunition with the selected
     * weapon's loaded magazine.
     */
    private void syncAmmo() {
        projectiles.setAmmo(inventory.getLoadedAmmo());
    }

    /**
     * Update firing cooldowns and reload progress.
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
                syncAmmo();
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

        syncAmmo();

        if (!projectiles.fire(x, y, z, heading)) {
            return false;
        }

        if (!inventory.consumeShot()) {
            syncAmmo();
            return false;
        }

        syncAmmo();
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

    /**
     * Select a weapon and synchronize its magazine.
     */
    public boolean selectWeapon(int weapon) {
        if (reloading) return false;

        if (!inventory.selectWeapon(weapon)) {
            return false;
        }

        syncAmmo();
        return true;
    }

    public boolean unlockWeapon(int weapon) {
        boolean result = inventory.unlockWeapon(weapon);
        syncAmmo();
        return result;
    }

    public void addAmmo(int weapon, int amount) {
        inventory.addAmmo(weapon, amount);
        syncAmmo();
    }

    /**
     * Cancel an unfinished reload.
     */
    public void cancelReload() {
        reloading = false;
        reloadTimer = 0f;
        syncAmmo();
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
            inventory.getSelectedWeapon()
        );
    }

    public WeaponInventory getInventory() {
        return inventory;
    }

    public WeaponSystem getProjectiles() {
        return projectiles;
    }

    /**
     * Restore default weapons and ammunition.
     */
    public void reset() {
        cancelReload();
        inventory.reset();
        projectiles.clear();
        syncAmmo();
    }
}
