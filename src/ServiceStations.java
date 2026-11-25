
/**
 * RallyCity service stations.
 *
 * Three locations:
 * 1. City garage: fuel and repairs
 * 2. Eastern fuel station: fuel
 * 3. Western repair shop: repairs
 *
 * Compatible with CLDC 1.1 / MIDP 2.1.
 */
public class ServiceStations {

    public static final int FUEL = 0;
    public static final int REPAIR = 1;
    public static final int BOTH = 2;

    public static final int COUNT = 3;

    public static final float USE_DISTANCE = 18f;
    public static final float MAX_SPEED = 2f;

    public static final int FUEL_PRICE = 2;
    public static final int REPAIR_PRICE = 3;

    private static final float[] X = {
        0f,
        240f,
        -240f
    };

    private static final float[] Z = {
        0f,
        0f,
        0f
    };

    private static final int[] TYPE = {
        BOTH,
        FUEL,
        REPAIR
    };

    private static final String[] NAME = {
        "CITY GARAGE",
        "FUEL STATION",
        "REPAIR SHOP"
    };

    public ServiceStations() {
    }

    public float getX(int index) {
        if (index < 0 || index >= COUNT) return 0f;
        return X[index];
    }

    public float getZ(int index) {
        if (index < 0 || index >= COUNT) return 0f;
        return Z[index];
    }

    public int getType(int index) {
        if (index < 0 || index >= COUNT) return -1;
        return TYPE[index];
    }

    public String getName(int index) {
        if (index < 0 || index >= COUNT) return "";
        return NAME[index];
    }

    /**
     * Returns the nearest station within interaction range.
     * Returns -1 when no station is close enough.
     */
    public int nearest(float x, float z) {
        int found = -1;
        float best = USE_DISTANCE * USE_DISTANCE;

        for (int i = 0; i < COUNT; i++) {
            float dx = X[i] - x;
            float dz = Z[i] - z;
            float d2 = dx * dx + dz * dz;

            if (d2 <= best) {
                best = d2;
                found = i;
            }
        }

        return found;
    }

    public boolean canRefuel(int index) {
        if (index < 0 || index >= COUNT) return false;

        return TYPE[index] == FUEL ||
               TYPE[index] == BOTH;
    }

    public boolean canRepair(int index) {
        if (index < 0 || index >= COUNT) return false;

        return TYPE[index] == REPAIR ||
               TYPE[index] == BOTH;
    }

    /**
     * Refuels the car as much as the player's money allows.
     * Returns the amount of money spent.
     */
    public int refuel(Car car, int money) {
        if (car == null || money <= 0) return 0;

        float missing = 100f - car.fuel;

        if (missing <= 0f) return 0;

        int affordable = money / FUEL_PRICE;

        if (affordable <= 0) return 0;

        float amount = missing;

        if (amount > affordable) {
            amount = affordable;
        }

        int cost = (int)Math.ceil(amount * FUEL_PRICE);

        if (cost > money) {
            cost = money;
            amount = cost / (float)FUEL_PRICE;
        }

        car.refuel(amount);

        return cost;
    }

    /**
     * Repairs the car as much as the player's money allows.
     * Returns the amount of money spent.
     */
    public int repair(Car car, int money) {
        if (car == null || money <= 0) return 0;

        float missing = car.damage;

        if (missing <= 0f) return 0;

        int affordable = money / REPAIR_PRICE;

        if (affordable <= 0) return 0;

        float amount = missing;

        if (amount > affordable) {
            amount = affordable;
        }

        int cost = (int)Math.ceil(amount * REPAIR_PRICE);

        if (cost > money) {
            cost = money;
            amount = cost / (float)REPAIR_PRICE;
        }

        car.repair(amount);

        return cost;
    }

    /**
     * Returns a short description of the station type.
     */
    public String getServiceDescription(int index) {
        if (index < 0 || index >= COUNT) return "";

        if (TYPE[index] == BOTH) {
            return "FUEL + REPAIR";
        }

        if (TYPE[index] == FUEL) {
            return "FUEL";
        }

        return "REPAIR";
    }
}
