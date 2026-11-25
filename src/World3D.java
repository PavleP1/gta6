
import java.util.Random;
import javax.microedition.m3g.*;

/**
 * Endless procedural open world. Cells are S x S metres. Only 3x3 cells around the player exist.
 * City = cells -3..2 (grid roads, buildings). Outside = rural (dirt roads every 4 cells, trees, farms).
 */
public class World3D {
    public static final int S = 120;
    public static final float RW = 14f;
    private static final int[] PAL = {0xFFFFFF, 0xE0D8C8, 0xC8D0DC, 0xE8C8A8, 0xB8C4D8, 0xD8D8D8, 0xEED8C0};

    public static class Cell {
        int i, j;
        Mesh mesh;
        float[] bx = new float[4 * 48];
        int nb;
    }

    private Group root;
    public Cell[] cells = new Cell[9];
    private int ci = 99999, cj = 99999;
    public float nx, nz;

    public World3D(Group root) {
        this.root = root;
    }

    public static int fl(float v) {
        return (int) Math.floor(v / S);
    }

    private static boolean mod4(int i) {
        return ((i % 4) + 4) % 4 == 0;
    }

    public static boolean isCity(int i, int j) {
        return i >= -3 && i <= 2 && j >= -3 && j <= 2;
    }

    public static boolean roadX(int i, int j) {
        return (i >= -3 && i <= 3 && j >= -3 && j <= 2) || mod4(i);
    }

    public static boolean roadZ(int i, int j) {
        return (j >= -3 && j <= 3 && i >= -3 && i <= 2) || mod4(j);
    }

    public static int surface(float x, float z) {
        int li = (int) Math.floor(x / S + 0.5f);
        int lj = (int) Math.floor(z / S + 0.5f);
        int cx = fl(x);
        int cz = fl(z);
        boolean city = isCity(cx, cz);
        float hw = RW * 0.5f;

        if (Math.abs(x - li * S) < hw && roadX(li, cz)) return city ? 0 : 1;
        if (Math.abs(z - lj * S) < hw && roadZ(cx, lj)) return city ? 0 : 1;

        return city ? 2 : 3;
    }

    public void update(float px, float pz) {
        int i = fl(px);
        int j = fl(pz);

        if (i == ci && j == cj) return;

        ci = i;
        cj = j;

        Cell[] nc = new Cell[9];

        for (int a = -1; a <= 1; a++) {
            for (int b = -1; b <= 1; b++) {
                Cell f = null;

                for (int k = 0; k < 9; k++) {
                    if (cells[k] != null &&
                        cells[k].i == i + a &&
                        cells[k].j == j + b) {
                        f = cells[k];
                        break;
                    }
                }

                if (f == null) {
                    f = build(i + a, j + b);
                    root.addChild(f.mesh);
                }

                nc[(a + 1) * 3 + (b + 1)] = f;
            }
        }

        for (int k = 0; k < 9; k++) {
            Cell o = cells[k];

            if (o == null) continue;

            boolean keep = false;

            for (int m = 0; m < 9; m++) {
                if (nc[m] == o) {
                    keep = true;
                    break;
                }
            }

            if (!keep) root.removeChild(o.mesh);
        }

        cells = nc;
    }

    /**
     * Push circle out of buildings and trees.
     * Skips cells whose boundaries cannot possibly overlap the collision circle.
     */
    public boolean resolve(float[] p, float r) {
        boolean hit = false;

        float px = p[0];
        float pz = p[1];

        for (int k = 0; k < 9; k++) {
            Cell c = cells[k];

            if (c == null) continue;

            float minX = c.i * S;
            float minZ = c.j * S;
            float maxX = minX + S;
            float maxZ = minZ + S;

            // The collision circle cannot touch this cell.
            if (px + r < minX || px - r > maxX ||
                pz + r < minZ || pz - r > maxZ) {
                continue;
            }

            for (int q = 0; q < c.nb; q++) {
                int o = q * 4;

                float x0 = c.bx[o];
                float z0 = c.bx[o + 1];
                float x1 = c.bx[o + 2];
                float z1 = c.bx[o + 3];

                if (p[0] < x0 - r || p[0] > x1 + r ||
                    p[1] < z0 - r || p[1] > z1 + r) {
                    continue;
                }

                float qx = p[0] < x0 ? x0 : (p[0] > x1 ? x1 : p[0]);
                float qz = p[1] < z0 ? z0 : (p[1] > z1 ? z1 : p[1]);

                float dx = p[0] - qx;
                float dz = p[1] - qz;
                float d2 = dx * dx + dz * dz;

                if (d2 >= r * r) continue;

                if (d2 > 0.0001f) {
                    float d = (float) Math.sqrt(d2);
                    nx = dx / d;
                    nz = dz / d;

                    p[0] = qx + nx * r;
                    p[1] = qz + nz * r;
                } else {
                    float l = p[0] - x0;
                    float rr = x1 - p[0];
                    float u = p[1] - z0;
                    float dn = z1 - p[1];

                    float m = Math.min(Math.min(l, rr), Math.min(u, dn));

                    if (m == l) {
                        nx = -1;
                        nz = 0;
                        p[0] = x0 - r;
                    } else if (m == rr) {
                        nx = 1;
                        nz = 0;
                        p[0] = x1 + r;
                    } else if (m == u) {
                        nx = 0;
                        nz = -1;
                        p[1] = z0 - r;
                    } else {
                        nx = 0;
                        nz = 1;
                        p[1] = z1 + r;
                    }
                }

                hit = true;
            }
        }

        return hit;
    }

    private void addBox(Cell c, float x0, float z0, float x1, float z1) {
        if (c.nb >= 48) return;

        int o = c.nb * 4;

        c.bx[o] = x0;
        c.bx[o + 1] = z0;
        c.bx[o + 2] = x1;
        c.bx[o + 3] = z1;

        c.nb++;
    }

    private Cell build(int i, int j) {
        Cell c = new Cell();
        c.i = i;
        c.j = j;

        Random r = new Random(((long) i * 73856093L) ^
                              ((long) j * 19349663L) ^
                              0x2545F491L);

        boolean city = isCity(i, j);

        MeshBuilder mb = new MeshBuilder(1300, 0.02f);

        float ox = i * S;
        float oz = j * S;
        float hw = RW * 0.5f;

        int gm = city ? Mats.PAVE : Mats.GRASS;

        mb.flatT(0, 0, S, S, 0f, 0xFFFFFF, gm,
                 S / 8f, S / 8f, false);

        int rm = city ? Mats.ROAD : Mats.DIRT;
        float rl = S / RW;

        boolean rx = roadX(i, j);
        boolean rz = roadZ(i, j);

        if (rx) {
            mb.flatT(-hw, 0, hw, S, 0.06f, 0xFFFFFF,
                     rm, 1f, rl, false);
        }

        if (rz) {
            mb.flatT(0, -hw, S, hw, 0.12f, 0xFFFFFF,
                     rm, rl, 1f, true);
        }

        if (city) {
            float m = hw + 4f;
            float sl = (S - 2 * m) / 3f;

            for (int a = 0; a < 3; a++) {
                for (int b = 0; b < 3; b++) {
                    if (r.nextFloat() < 0.1f) continue;

                    float bw = sl * (0.6f + 0.35f * r.nextFloat());
                    float bd = sl * (0.6f + 0.35f * r.nextFloat());

                    float bxc = m + sl * (a + 0.5f);
                    float bzc = m + sl * (b + 0.5f);

                    float wx = ox + bxc;
                    float wz = oz + bzc;

                    float d = (float) Math.sqrt(wx * wx + wz * wz);

                    float hh = 7f + r.nextFloat() * 12f;

                    if (d < 250f) {
                        hh += r.nextFloat() * (250f - d) * 0.16f;
                    }

                    int col = PAL[(int) (r.nextFloat() * PAL.length) % PAL.length];
                    int fm = Mats.FAC0 + (int) (r.nextFloat() * 4f) % 4;

                    mb.boxT(bxc - bw / 2, 0, bzc - bd / 2,
                            bxc + bw / 2, hh, bzc + bd / 2,
                            col, fm, Mats.ROOF, 12f);

                    addBox(c, wx - bw / 2, wz - bd / 2,
                           wx + bw / 2, wz + bd / 2);
                }
            }
        } else {
            if (r.nextFloat() < 0.7f) {
                float a = 10 + r.nextFloat() * 40;
                float b = 10 + r.nextFloat() * 40;
                float fw = 20 + r.nextFloat() * 40;
                float fd = 20 + r.nextFloat() * 40;

                mb.flatT(a, b, a + fw, b + fd, 0.02f,
                         r.nextFloat() < 0.5f ? 0xFFE070 : 0xB8F0A0,
                         Mats.GRASS, fw / 8f, fd / 8f, false);
            }

            boolean r0x = roadX(i, j);
            boolean r1x = roadX(i + 1, j);
            boolean r0z = roadZ(i, j);
            boolean r1z = roadZ(i, j + 1);

            float mg = hw + 4f;
            int nt = 6 + (int) (r.nextFloat() * 6f);

            for (int k = 0; k < nt; k++) {
                float lx = 2 + r.nextFloat() * (S - 4);
                float lz = 2 + r.nextFloat() * (S - 4);

                if ((r0x && lx < mg) || (r1x && lx > S - mg) ||
                    (r0z && lz < mg) || (r1z && lz > S - mg)) {
                    continue;
                }

                if (k < nt - 2) {
                    float s = 0.8f + r.nextFloat() * 0.7f;

                    mb.boxT(lx - 0.4f, 0, lz - 0.4f,
                            lx + 0.4f, 2.5f * s, lz + 0.4f,
                            0xFFFFFF, Mats.BARK, Mats.BARK, 2f);

                    mb.boxT(lx - 1.8f * s, 2.5f * s, lz - 1.8f * s,
                            lx + 1.8f * s, 6f * s, lz + 1.8f * s,
                            r.nextFloat() < 0.5f ? 0xFFFFFF : 0xD8F0C8,
                            Mats.LEAVES, Mats.LEAVES, 3f);

                    addBox(c, ox + lx - 0.5f, oz + lz - 0.5f,
                           ox + lx + 0.5f, oz + lz + 0.5f);
                } else {
                    float s = 1f + r.nextFloat() * 1.5f;

                    mb.boxT(lx - s, 0, lz - s,
                            lx + s, s * 0.9f, lz + s,
                            0xFFFFFF, Mats.ROCK, Mats.ROCK, 3f);

                    addBox(c, ox + lx - s, oz + lz - s,
                           ox + lx + s, oz + lz + s);
                }
            }

            if (r.nextFloat() < 0.2f) {
                float hx = 25 + r.nextFloat() * (S - 60);
                float hz = 25 + r.nextFloat() * (S - 60);

                mb.boxT(hx, 0, hz, hx + 10, 5, hz + 8,
                        0xFFFFFF, Mats.PLASTER, Mats.PLASTER, 4f);

                mb.boxT(hx - 0.5f, 5, hz - 0.5f,
                        hx + 10.5f, 6.2f, hz + 8.5f,
                        0xFFFFFF, Mats.TILES, Mats.TILES, 4f);

                addBox(c, ox + hx, oz + hz,
                       ox + hx + 10, oz + hz + 8);
            }
        }

        c.mesh = mb.build();
        c.mesh.setTranslation(ox, 0, oz);

        return c;
    }
}
