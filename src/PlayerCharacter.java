
import javax.microedition.m3g.*;

/**
 * Lightweight on-foot player character for RallyCity.
 * Movement and rendering are managed independently of GameView.
 */
public class PlayerCharacter {
    private Mesh body;
    private Mesh head;
    private Group root;

    private float x;
    private float z;
    private float heading;
    private float speed;

    private boolean visible = true;

    // Reused collision data to avoid per-frame allocations.
    private final float[] collisionPosition = new float[2];

    public PlayerCharacter(Group parent) {
        root = new Group();

        // Body
        MeshBuilder mb = new MeshBuilder(80, 0.1f);
        mb.box(-0.22f, 0.45f, -0.13f,
               0.22f, 1.15f, 0.13f, 0x2457A6);

        body = mb.build();
        root.addChild(body);

        // Head
        MeshBuilder hb = new MeshBuilder(40, 0.1f);
        hb.box(-0.14f, 1.17f, -0.14f,
               0.14f, 1.45f, 0.14f, 0xD9A477);

        head = hb.build();
        root.addChild(head);

        parent.addChild(root);

        setVisible(true);
        sync();
    }

    public void place(float px, float pz, float angle) {
        x = px;
        z = pz;
        heading = angle;
        speed = 0f;
        sync();
    }

    public void update(float dt, boolean forward, boolean backward,
                       boolean left, boolean right, World3D world) {
        if (dt <= 0f) return;

        // Prevent large movement jumps after a slow frame.
        if (dt > 0.06f) dt = 0.06f;

        float turn = 0f;

        if (left) turn = 1f;
        if (right) turn = -1f;

        heading += turn * 2.4f * dt;

        float input = 0f;

        if (forward) input += 1f;
        if (backward) input -= 1f;

        float targetSpeed = input * 3.2f;

        speed += (targetSpeed - speed) *
                 Math.min(1f, dt * 8f);

        float nx = x + (float)Math.sin(heading) * speed * dt;
        float nz = z + (float)Math.cos(heading) * speed * dt;

        collisionPosition[0] = nx;
        collisionPosition[1] = nz;

        if (world != null &&
            world.resolve(collisionPosition, 0.28f)) {
            speed = 0f;
        } else {
            x = nx;
            z = nz;
        }

        sync();
    }

    public void sync() {
        root.setTranslation(x, 0f, z);
        root.setOrientation(heading * 57.29578f,
                            0f, 1f, 0f);
    }

    public void setVisible(boolean enabled) {
        visible = enabled;
        root.setRenderingEnable(enabled);
    }

    public boolean isVisible() {
        return visible;
    }

    public float getX() {
        return x;
    }

    public float getZ() {
        return z;
    }

    public float getHeading() {
        return heading;
    }

    public float getSpeed() {
        return speed;
    }

    public void stop() {
        speed = 0f;
    }
}
