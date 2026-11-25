
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Font;

/**
 * Lightweight waypoint navigation for RallyCity.
 * Uses world X/Z coordinates and works with MIDP 2.1.
 */
public class Navigation {
    private boolean active;
    private float targetX;
    private float targetZ;

    public Navigation() {
        active = false;
    }

    public void setWaypoint(float x, float z) {
        targetX = x;
        targetZ = z;
        active = true;
    }

    public void clearWaypoint() {
        active = false;
    }

    public boolean hasWaypoint() {
        return active;
    }

    public float distance(float playerX, float playerZ) {
        if (!active) return 0f;

        float dx = targetX - playerX;
        float dz = targetZ - playerZ;

        return (float)Math.sqrt(dx * dx + dz * dz);
    }

    /**
     * Returns the direction to the waypoint relative to the
     * player's current heading, in degrees.
     */
    public float direction(float playerX, float playerZ,
                           float heading) {
        if (!active) return 0f;

        float dx = targetX - playerX;
        float dz = targetZ - playerZ;

        float targetAngle = (float)Math.atan2(dx, dz);
        float relative = targetAngle - heading;

        while (relative > Math.PI) {
            relative -= (float)(Math.PI * 2.0);
        }

        while (relative < -Math.PI) {
            relative += (float)(Math.PI * 2.0);
        }

        return relative * 57.29578f;
    }

    public float getTargetX() {
        return targetX;
    }

    public float getTargetZ() {
        return targetZ;
    }

    /**
     * Draws a compact navigation display.
     */
    public void draw(Graphics g, int screenWidth,
                     float playerX, float playerZ,
                     float heading) {
        if (!active) return;

        int d = (int)distance(playerX, playerZ);
        float angle = direction(playerX, playerZ, heading);

        int cx = screenWidth / 2;
        int cy = 43;
        int radius = 13;

        g.setFont(Font.getFont(
            Font.FACE_PROPORTIONAL,
            Font.STYLE_BOLD,
            Font.SIZE_SMALL
        ));

        g.setColor(0x101820);
        g.fillRect(cx - 43, 17, 86, 57);

        g.setColor(0xFFD21F);
        g.drawRect(cx - 43, 17, 86, 57);

        g.setColor(0xFFFFFF);
        g.drawString(
            "WAYPOINT",
            cx, 19,
            Graphics.TOP | Graphics.HCENTER
        );

        // Compass circle.
        g.setColor(0x667784);
        g.drawArc(
            cx - radius, cy - radius,
            radius * 2, radius * 2,
            0, 360
        );

        // Arrow points toward the destination.
        float a = angle * 0.017453292f;

        int tipX = cx + (int)(Math.sin(a) * radius);
        int tipY = cy - (int)(Math.cos(a) * radius);

        int backX = cx - (int)(Math.sin(a) * 5f);
        int backY = cy + (int)(Math.cos(a) * 5f);

        int sideX = (int)(Math.cos(a) * 4f);
        int sideY = (int)(Math.sin(a) * 4f);

        g.setColor(0xFFD21F);
        g.drawLine(tipX, tipY,
                   backX + sideX, backY + sideY);
        g.drawLine(tipX, tipY,
                   backX - sideX, backY - sideY);

        g.setColor(0xFFFFFF);
        String distanceText;

        if (d >= 1000) {
            distanceText = ((int)(d / 100f) / 10f) + " km";
        } else {
            distanceText = d + " m";
        }

        g.drawString(
            distanceText,
            cx, 59,
            Graphics.TOP | Graphics.HCENTER
        );
    }
}
