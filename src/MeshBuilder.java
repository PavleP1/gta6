import javax.microedition.m3g.*;

/**
 * Merges boxes/quads into ONE M3G mesh: shared vertex buffer (position + colour + UV),
 * one sub-mesh per material (see Mats). Vertex colour tints the texture and fakes lighting per face.
 */
public class MeshBuilder {
    private short[] pos, uv;
    private byte[] col, qmat;
    private int n, max;
    private float unit, inv;

    public MeshBuilder(int maxVerts, float unit) {
        max = maxVerts; this.unit = unit; inv = 1f / unit;
        pos = new short[maxVerts * 3]; col = new byte[maxVerts * 3];
        uv = new short[maxVerts * 2]; qmat = new byte[maxVerts / 4];
    }

    private void v(float x, float y, float z, int r, int g, int b, float u, float w) {
        int k = n * 3;
        pos[k] = (short) (int) Math.floor(x * inv + 0.5f);
        pos[k + 1] = (short) (int) Math.floor(y * inv + 0.5f);
        pos[k + 2] = (short) (int) Math.floor(z * inv + 0.5f);
        col[k] = (byte) r; col[k + 1] = (byte) g; col[k + 2] = (byte) b;
        uv[n * 2] = (short) (int) Math.floor(u * 256f + 0.5f);
        uv[n * 2 + 1] = (short) (int) Math.floor(w * 256f + 0.5f);
        n++;
    }

    /** corners: bottom-left, bottom-right, top-left, top-right seen from outside. uw/vh = texture repeats. */
    private void quad(float ax, float ay, float az, float bx, float by, float bz,
                      float cx, float cy, float cz, float dx, float dy, float dz,
                      int rgb, float s, int mat, float uw, float vh, boolean rot) {
        if (n + 4 > max) return;
        int r = (int) (((rgb >> 16) & 255) * s), g = (int) (((rgb >> 8) & 255) * s), b = (int) ((rgb & 255) * s);
        qmat[n / 4] = (byte) mat;
        if (!rot) {
            v(ax, ay, az, r, g, b, 0, vh); v(bx, by, bz, r, g, b, uw, vh);
            v(cx, cy, cz, r, g, b, 0, 0);  v(dx, dy, dz, r, g, b, uw, 0);
        } else {
            v(ax, ay, az, r, g, b, vh, 0); v(bx, by, bz, r, g, b, vh, uw);
            v(cx, cy, cz, r, g, b, 0, 0);  v(dx, dy, dz, r, g, b, 0, uw);
        }
    }

    /** Flat colour quad facing up. */
    public void flat(float x0, float z0, float x1, float z1, float y, int rgb) {
        quad(x0, y, z1, x1, y, z1, x0, y, z0, x1, y, z0, rgb, 1f, 0, 0, 0, false);
    }

    /** Textured quad facing up. a = repeats across X, b = repeats across Z. rot: texture U runs across Z (E-W roads). */
    public void flatT(float x0, float z0, float x1, float z1, float y, int rgb, int mat, float a, float b, boolean rot) {
        quad(x0, y, z1, x1, y, z1, x0, y, z0, x1, y, z0, rgb, 1f, mat, a, b, rot);
    }

    public void box(float x0, float y0, float z0, float x1, float y1, float z1, int rgb) {
        boxT(x0, y0, z0, x1, y1, z1, rgb, 0, 0, 1f);
    }

    /** Box (no bottom). tile = metres per texture repeat. */
    public void boxT(float x0, float y0, float z0, float x1, float y1, float z1, int rgb, int side, int top, float tile) {
        float w = (x1 - x0) / tile, d = (z1 - z0) / tile, h = (y1 - y0) / tile;
        quad(x0, y0, z1, x1, y0, z1, x0, y1, z1, x1, y1, z1, rgb, 0.85f, side, w, h, false); // +z
        quad(x1, y0, z0, x0, y0, z0, x1, y1, z0, x0, y1, z0, rgb, 0.60f, side, w, h, false); // -z
        quad(x1, y0, z1, x1, y0, z0, x1, y1, z1, x1, y1, z0, rgb, 0.75f, side, d, h, false); // +x
        quad(x0, y0, z0, x0, y0, z1, x0, y1, z0, x0, y1, z1, rgb, 0.55f, side, d, h, false); // -x
        quad(x0, y1, z1, x1, y1, z1, x0, y1, z0, x1, y1, z0, rgb, 1.00f, top, w, d, false);   // top
    }

    public Mesh build() {
        short[] p = new short[n * 3]; System.arraycopy(pos, 0, p, 0, n * 3);
        byte[] c = new byte[n * 3]; System.arraycopy(col, 0, c, 0, n * 3);
        short[] t = new short[n * 2]; System.arraycopy(uv, 0, t, 0, n * 2);
        VertexArray pa = new VertexArray(n, 3, 2); pa.set(0, n, p);
        VertexArray ca = new VertexArray(n, 3, 1); ca.set(0, n, c);
        VertexArray ta = new VertexArray(n, 2, 2); ta.set(0, n, t);
        VertexBuffer vb = new VertexBuffer();
        vb.setPositions(pa, unit, null);
        vb.setColors(ca);
        vb.setTexCoords(0, ta, 1f / 256f, null);

        int quads = n / 4, subs = 0;
        int[] cnt = new int[Mats.COUNT];
        for (int q = 0; q < quads; q++) cnt[qmat[q]]++;
        for (int m = 0; m < Mats.COUNT; m++) if (cnt[m] > 0) subs++;
        IndexBuffer[] ib = new IndexBuffer[subs];
        Appearance[] aps = new Appearance[subs];
        int s = 0;
        for (int m = 0; m < Mats.COUNT; m++) {
            if (cnt[m] == 0) continue;
            int[] idx = new int[cnt[m] * 4], len = new int[cnt[m]];
            int o = 0, l = 0;
            for (int q = 0; q < quads; q++) {
                if (qmat[q] != m) continue;
                for (int k = 0; k < 4; k++) idx[o++] = q * 4 + k;
                len[l++] = 4;
            }
            ib[s] = new TriangleStripArray(idx, len);
            aps[s] = Mats.ap[m];
            s++;
        }
        return new Mesh(vb, ib, aps);
    }
}
