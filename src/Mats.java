import java.io.IOException;
import javax.microedition.lcdui.Image;
import javax.microedition.m3g.*;

/** Material table: one shared Appearance per texture (fog + flat shading + optional texture). */
public class Mats {
    public static final int NONE = 0, FAC0 = 1, FAC1 = 2, FAC2 = 3, FAC3 = 4, ROOF = 5, ROAD = 6, DIRT = 7,
        PAVE = 8, GRASS = 9, BARK = 10, LEAVES = 11, ROCK = 12, PLASTER = 13, TILES = 14, GLASS = 15,
        WHEEL = 16, PAINT = 17, COUNT = 18;
    private static final String[] NAMES = {null, "fac0", "fac1", "fac2", "fac3", "roof", "road", "dirt",
        "pave", "grass", "bark", "leaves", "rock", "plaster", "tiles", "glass", "wheel", "paint"};
    public static Appearance[] ap = new Appearance[COUNT];

    /** Perspective-correct only where texture swim would be visible (big flat surfaces + facades). */
    private static boolean persp(int m) {
        return m == ROAD || m == DIRT || m == PAVE || m == GRASS || (m >= FAC0 && m <= FAC3);
    }

    public static void init(int sky) {
        Fog fog = new Fog();
        fog.setMode(Fog.LINEAR); fog.setLinearDistances(40f, 125f); fog.setColor(sky);
        for (int m = 0; m < COUNT; m++) {
            Appearance a = new Appearance();
            PolygonMode pm = new PolygonMode();
            pm.setCulling(PolygonMode.CULL_BACK);
            pm.setShading(PolygonMode.SHADE_FLAT);
            pm.setPerspectiveCorrectionEnable(persp(m));
            a.setPolygonMode(pm);
            a.setFog(fog);
            if (NAMES[m] != null) {
                try {
                    Image img = Image.createImage("/" + NAMES[m] + ".png");
                    Texture2D t = new Texture2D(new Image2D(Image2D.RGB, img));
                    t.setFiltering(Texture2D.FILTER_BASE_LEVEL, Texture2D.FILTER_NEAREST);
                    t.setWrapping(Texture2D.WRAP_REPEAT, Texture2D.WRAP_REPEAT);
                    t.setBlending(Texture2D.FUNC_MODULATE);
                    a.setTexture(0, t);
                } catch (IOException e) {
                } catch (RuntimeException e) { }
            }
            ap[m] = a;
        }
    }
}
