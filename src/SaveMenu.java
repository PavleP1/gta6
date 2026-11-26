
import javax.microedition.rms.RecordStore;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;

/**
 * RallyCity save system.
 * Stores progress in the phone's Java ME RMS memory.
 */
public class SaveGame {
    private static final String STORE = "RallyCitySave";
    private static final int VERSION = 1;

    public float x;
    public float z;
    public float heading;
    public int money;
    public int mission;
    public float fuel;
    public float damage;
    public int cameraMode;
    public boolean waypointActive;
    public float waypointX;
    public float waypointZ;

    public SaveGame() {
    }

    public static boolean save(SaveGame data) {
        if (data == null) return false;

        RecordStore rs = null;

        try {
            ByteArrayOutputStream bytes =
                new ByteArrayOutputStream();
            DataOutputStream out =
                new DataOutputStream(bytes);

            out.writeInt(VERSION);
            out.writeFloat(data.x);
            out.writeFloat(data.z);
            out.writeFloat(data.heading);
            out.writeInt(data.money);
            out.writeInt(data.mission);
            out.writeFloat(data.fuel);
            out.writeFloat(data.damage);
            out.writeInt(data.cameraMode);
            out.writeBoolean(data.waypointActive);
            out.writeFloat(data.waypointX);
            out.writeFloat(data.waypointZ);
            out.flush();

            byte[] record = bytes.toByteArray();

            rs = RecordStore.openRecordStore(STORE, true);

            if (rs.getNumRecords() == 0) {
                rs.addRecord(record, 0, record.length);
            } else {
                int id = rs.getNextRecordID() - 1;
                if (id < 1) id = 1;
                rs.setRecord(id, record, 0, record.length);
            }

            return true;
        } catch (Exception e) {
            return false;
        } finally {
            if (rs != null) {
                try {
                    rs.closeRecordStore();
                } catch (Exception e) {
                }
            }
        }
    }

    public static SaveGame load() {
        RecordStore rs = null;

        try {
            rs = RecordStore.openRecordStore(STORE, false);

            if (rs.getNumRecords() == 0) return null;

            byte[] record = rs.getRecord(1);
            DataInputStream in = new DataInputStream(
                new ByteArrayInputStream(record)
            );

            int version = in.readInt();
            if (version != VERSION) return null;

            SaveGame data = new SaveGame();

            data.x = in.readFloat();
            data.z = in.readFloat();
            data.heading = in.readFloat();
            data.money = in.readInt();
            data.mission = in.readInt();
            data.fuel = in.readFloat();
            data.damage = in.readFloat();
            data.cameraMode = in.readInt();
            data.waypointActive = in.readBoolean();
            data.waypointX = in.readFloat();
            data.waypointZ = in.readFloat();

            return data;
        } catch (Exception e) {
            return null;
        } finally {
            if (rs != null) {
                try {
                    rs.closeRecordStore();
                } catch (Exception e) {
                }
            }
        }
    }

    public static void delete() {
        try {
            RecordStore.deleteRecordStore(STORE);
        } catch (Exception e) {
        }
    }
}
