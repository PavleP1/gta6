
"""Writes compile-only stubs of the Java ME APIs the game uses."""
import os

OUT = os.path.join('build', 'stubsrc')
C = lambda n: 'Integer.parseInt("%d")' % n

files = {}


def add(pkg, name, body):
    files[(pkg, name)] = body


# ---------------- MIDP
add('javax.microedition.midlet', 'MIDletStateChangeException',
    'public class MIDletStateChangeException extends Exception {}')

add('javax.microedition.midlet', 'MIDlet', '''public abstract class MIDlet {
    protected MIDlet() {}
    protected abstract void startApp() throws MIDletStateChangeException;
    protected abstract void pauseApp();
    protected abstract void destroyApp(boolean unconditional) throws MIDletStateChangeException;
    public final void notifyDestroyed() {}
}''')

add('javax.microedition.lcdui', 'Displayable', '''public abstract class Displayable {
    public int getWidth() { return 0; }
    public int getHeight() { return 0; }
    public void repaint() {}
}''')

add('javax.microedition.lcdui', 'Display', '''public class Display {
    public static Display getDisplay(javax.microedition.midlet.MIDlet m) { return null; }
    public void setCurrent(Displayable d) {}
}''')

add('javax.microedition.lcdui', 'Font', '''public class Font {
    public static final int FACE_PROPORTIONAL = %s, STYLE_BOLD = %s, SIZE_SMALL = %s;
    public static Font getFont(int face, int style, int size) { return null; }
}''' % (C(64), C(1), C(8)))

add('javax.microedition.lcdui', 'Graphics', '''public class Graphics {
    public static final int HCENTER = %s, VCENTER = %s, LEFT = %s, RIGHT = %s, TOP = %s, BOTTOM = %s, BASELINE = %s;

    public void setColor(int rgb) {}
    public void setFont(Font f) {}
    public void drawString(String s, int x, int y, int anchor) {}

    public void fillRect(int x, int y, int w, int h) {}
    public void drawRect(int x, int y, int w, int h) {}
    public void drawLine(int x1, int y1, int x2, int y2) {}
    public void drawArc(int x, int y, int w, int h, int startAngle, int arcAngle) {}
    public void fillArc(int x, int y, int w, int h, int startAngle, int arcAngle) {}
    public void fillTriangle(int x1, int y1, int x2, int y2, int x3, int y3) {}
}''' % tuple(C(v) for v in (1, 2, 4, 8, 16, 32, 64)))

add('javax.microedition.lcdui', 'Image', '''public class Image {
    public static Image createImage(String name) throws java.io.IOException { return null; }
}''')

add('javax.microedition.lcdui', 'Canvas', '''public abstract class Canvas extends Displayable {
    public static final int UP = %s, DOWN = %s, LEFT = %s, RIGHT = %s, FIRE = %s;
    protected Canvas() {}
    public void setFullScreenMode(boolean b) {}
    public int getGameAction(int keyCode) { return 0; }
    protected void keyPressed(int k) {}
    protected void keyReleased(int k) {}
    protected void hideNotify() {}
    protected void showNotify() {}
    protected abstract void paint(Graphics g);
}''' % tuple(C(v) for v in (1, 6, 2, 5, 8)))

add('javax.microedition.lcdui.game', 'GameCanvas', '''public abstract class GameCanvas extends javax.microedition.lcdui.Canvas {
    protected GameCanvas(boolean suppress) {}
    protected javax.microedition.lcdui.Graphics getGraphics() { return null; }
    public void flushGraphics() {}
    public void paint(javax.microedition.lcdui.Graphics g) {}
}''')

# ---------------- RMS (Record Management System)
add('javax.microedition.rms', 'RecordStoreException',
    'public class RecordStoreException extends Exception {}')

add('javax.microedition.rms', 'RecordStoreNotFoundException',
    'public class RecordStoreNotFoundException extends RecordStoreException {}')

add('javax.microedition.rms', 'RecordStore', '''public class RecordStore {
    public static RecordStore openRecordStore(String name, boolean create)
        throws RecordStoreException {
        return null;
    }

    public static void deleteRecordStore(String name)
        throws RecordStoreException {}

    public int getNumRecords() throws RecordStoreException { return 0; }

    public int addRecord(byte[] data, int offset, int length)
        throws RecordStoreException { return 1; }

    public int getNextRecordID() throws RecordStoreException { return 1; }

    public void setRecord(int id, byte[] data, int offset, int length)
        throws RecordStoreException {}

    public byte[] getRecord(int id) throws RecordStoreException {
        return new byte[0];
    }

    public void closeRecordStore() throws RecordStoreException {}
}''')

# ---------------- MMAPI (audio)
add('javax.microedition.media', 'MediaException',
    'public class MediaException extends Exception {}')

add('javax.microedition.media', 'Control',
    'public interface Control {}')

add('javax.microedition.media.control', 'VolumeControl',
    'public interface VolumeControl extends javax.microedition.media.Control { int setLevel(int level); }')

add('javax.microedition.media', 'PlayerListener', '''public interface PlayerListener {
    String END_OF_MEDIA = new String("endOfMedia");
    void playerUpdate(Player player, String event, Object data);
}''')

add('javax.microedition.media', 'Player', '''public interface Player {
    void realize() throws MediaException;
    void prefetch() throws MediaException;
    void start() throws MediaException;
    void stop() throws MediaException;
    void close();
    void setLoopCount(int count);
    Control getControl(String name);
    void addPlayerListener(PlayerListener l);
}''')

add('javax.microedition.media', 'Manager', '''public final class Manager {
    public static Player createPlayer(java.io.InputStream is, String type)
        throws java.io.IOException, MediaException {
        return null;
    }
}''')

# ---------------- M3G (JSR-184)
M = 'javax.microedition.m3g'

add(M, 'Object3D', 'public abstract class Object3D {}')

add(M, 'Transform', '''public class Transform {
    public Transform() {}
    public void setIdentity() {}
    public void postTranslate(float x, float y, float z) {}
    public void postRotate(float angle, float ax, float ay, float az) {}
}''')

add(M, 'Transformable', '''public abstract class Transformable extends Object3D {
    public void setTransform(Transform t) {}
    public void setTranslation(float x, float y, float z) {}
    public void setOrientation(float angle, float ax, float ay, float az) {}
}''')

add(M, 'Node',
    'public abstract class Node extends Transformable { public void setRenderingEnable(boolean b) {} }')

add(M, 'Group',
    'public class Group extends Node { public void addChild(Node n) {} public void removeChild(Node n) {} }')

add(M, 'Camera',
    'public class Camera extends Node { public Camera() {} public void setPerspective(float fovy, float aspect, float near, float far) {} }')

add(M, 'Background',
    'public class Background extends Object3D { public Background() {} public void setColor(int argb) {} }')

add(M, 'World',
    'public class World extends Group { public World() {} public void setBackground(Background b) {} public void setActiveCamera(Camera c) {} }')

add(M, 'Fog', '''public class Fog extends Object3D {
    public static final int EXPONENTIAL = %s, LINEAR = %s;
    public Fog() {}
    public void setMode(int m) {}
    public void setLinear(float near, float far) {}
    public void setColor(int rgb) {}
}''' % (C(80), C(81)))

add(M, 'PolygonMode', '''public class PolygonMode extends Object3D {
    public static final int CULL_BACK = %s, CULL_FRONT = %s, CULL_NONE = %s, SHADE_FLAT = %s, SHADE_SMOOTH = %s;
    public PolygonMode() {}
    public void setCulling(int c) {}
    public void setShading(int s) {}
    public void setPerspectiveCorrectionEnable(boolean b) {}
}''' % tuple(C(v) for v in (160, 161, 162, 164, 165)))

add(M, 'Image2D', '''public class Image2D extends Object3D {
    public static final int RGB = %s, RGBA = %s;
    public Image2D(int format, Object image) {}
}''' % (C(99), C(100)))

add(M, 'Texture2D', '''public class Texture2D extends Transformable {
    public static final int FILTER_BASE_LEVEL = %s, FILTER_LINEAR = %s, FILTER_NEAREST = %s, FUNC_MODULATE = %s, WRAP_CLAMP = %s, WRAP_REPEAT = %s;
    public Texture2D(Image2D img) {}
    public void setFiltering(int levelFilter, int imageFilter) {}
    public void setWrapping(int s, int t) {}
    public void setBlending(int func) {}
}''' % tuple(C(v) for v in (208, 209, 210, 227, 240, 241)))

add(M, 'Appearance', '''public class Appearance extends Object3D {
    public Appearance() {}
    public void setPolygonMode(PolygonMode p) {}
    public void setFog(Fog f) {}
    public void setTexture(int unit, Texture2D t) {}
}''')

add(M, 'VertexArray', '''public class VertexArray extends Object3D {
    public VertexArray(int numVertices, int numComponents, int componentSize) {}
    public void set(int first, int num, byte[] values) {}
    public void set(int first, int num, short[] values) {}
}''')

add(M, 'VertexBuffer', '''public class VertexBuffer extends Object3D {
    public VertexBuffer() {}
    public void setPositions(VertexArray a, float scale, float[] bias) {}
    public void setColors(VertexArray a) {}
    public void setTexCoords(int unit, VertexArray a, float scale, float[] bias) {}
}''')

add(M, 'IndexBuffer', 'public abstract class IndexBuffer extends Object3D {}')

add(M, 'TriangleStripArray',
    'public class TriangleStripArray extends IndexBuffer { public TriangleStripArray(int[] indices, int[] stripLengths) {} }')

add(M, 'Mesh', '''public class Mesh extends Node {
    public Mesh(VertexBuffer v, IndexBuffer i, Appearance a) {}
    public Mesh(VertexBuffer v, IndexBuffer[] i, Appearance[] a) {}
}''')

add(M, 'Graphics3D', '''public final class Graphics3D {
    public static Graphics3D getInstance() { return null; }
    public void bindTarget(Object target, boolean depth, int hints) {}
    public void releaseTarget() {}
    public void render(World w) {}
}''')

# ---------------- Write all stub source files
for (pkg, name), body in files.items():
    directory = os.path.join(OUT, *pkg.split('.'))
    os.makedirs(directory, exist_ok=True)
    with open(os.path.join(directory, name + '.java'), 'w') as f:
        f.write('package %s;\n%s\n' % (pkg, body))

print('wrote %d stub classes' % len(files))
