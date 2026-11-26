
/**
 * RallyCity player health and armour system.
 *
 * Lightweight Java ME compatible class.
 * Does not depend on M3G or other game classes.
 */
public class PlayerStats {

    public static final int MAX_HEALTH = 100;
    public static final int MAX_ARMOUR = 100;

    private int health;
    private int armour;
    private boolean dead;

    public PlayerStats() {
        reset();
    }

    /**
     * Applies damage to the player.
     *
     * Armour absorbs damage first.
     * Returns the amount of health actually lost.
     */
    public int takeDamage(int damage) {
        if (damage <= 0 || dead) {
            return 0;
        }

        int remaining = damage;

        if (armour > 0) {
            int absorbed = remaining;

            if (absorbed > armour) {
                absorbed = armour;
            }

            armour -= absorbed;
            remaining -= absorbed;
        }

        int healthLost = remaining;

        if (healthLost > health) {
            healthLost = health;
        }

        health -= healthLost;

        if (health <= 0) {
            health = 0;
            dead = true;
        }

        return healthLost;
    }

    /**
     * Restores player health.
     * Returns the amount restored.
     */
    public int heal(int amount) {
        if (amount <= 0 || dead) {
            return 0;
        }

        int oldHealth = health;

        health += amount;

        if (health > MAX_HEALTH) {
            health = MAX_HEALTH;
        }

        return health - oldHealth;
    }

    /**
     * Adds armour.
     * Returns the amount added.
     */
    public int addArmour(int amount) {
        if (amount <= 0 || dead) {
            return 0;
        }

        int oldArmour = armour;

        armour += amount;

        if (armour > MAX_ARMOUR) {
            armour = MAX_ARMOUR;
        }

        return armour - oldArmour;
    }

    /**
     * Sets health directly.
     */
    public void setHealth(int value) {
        if (value < 0) {
            value = 0;
        }

        if (value > MAX_HEALTH) {
            value = MAX_HEALTH;
        }

        health = value;
        dead = (health == 0);
    }

    /**
     * Sets armour directly.
     */
    public void setArmour(int value) {
        if (value < 0) {
            value = 0;
        }

        if (value > MAX_ARMOUR) {
            value = MAX_ARMOUR;
        }

        armour = value;
    }

    /**
     * Restores health and armour after respawning.
     */
    public void respawn() {
        health = MAX_HEALTH;
        armour = 0;
        dead = false;
    }

    /**
     * Resets all player stats.
     */
    public void reset() {
        health = MAX_HEALTH;
        armour = 0;
        dead = false;
    }

    public int getHealth() {
        return health;
    }

    public int getArmour() {
        return armour;
    }

    public boolean isDead() {
        return dead;
    }

    public boolean isAlive() {
        return !dead;
    }

    public int getHealthPercent() {
        return health;
    }

    public int getArmourPercent() {
        return armour;
    }
}
