package extension.game;

import gearth.extensions.ExtensionBase;
import gearth.extensions.IExtension;
import gearth.extensions.parsers.HFloorItem;
import gearth.extensions.parsers.HPoint;
import gearth.extensions.parsers.HStuff;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;


import java.util.*;

public class FloorState {

    private final Object lock = new Object();

    private long latestRequestTimestamp = -1;

    private volatile int[][] heightmap = null; // 256 * 256
    private volatile Map<Integer, HFloorItem> furniIdToItem = null;
    private volatile List<List<Map<Integer, HFloorItem>>> furnimap = null;
    private volatile char[][] floorplan = null;

    public FloorState(IExtension extension) {
        extension.intercept(HMessage.Direction.TOCLIENT, "Objects", this::parseFloorItems);

        extension.intercept(HMessage.Direction.TOCLIENT, "ObjectAdd", this::onObjectAdd);
        extension.intercept(HMessage.Direction.TOCLIENT, "ObjectRemove", this::onObjectRemove);
        extension.intercept(HMessage.Direction.TOCLIENT, "ObjectUpdate", this::onObjectUpdate);
        extension.intercept(HMessage.Direction.TOCLIENT, "SlideObjectBundle", this::onObjectMove);

        extension.intercept(HMessage.Direction.TOCLIENT, "ObjectDataUpdate", this::onDataUpdate);
        extension.intercept(HMessage.Direction.TOCLIENT, "ObjectsDataUpdate", this::onDataUpdates);

        extension.intercept(HMessage.Direction.TOCLIENT, "HeightMap", this::parseHeightmap);
        extension.intercept(HMessage.Direction.TOCLIENT, "HeightMapUpdate", this::heightmapUpdate);

        extension.intercept(HMessage.Direction.TOCLIENT, "FloorHeightMap", this::parseFloorPlan);

        extension.intercept(HMessage.Direction.TOCLIENT, "RoomEntryInfo", this::roomEntryInfo);


        extension.intercept(HMessage.Direction.TOCLIENT, "CloseConnection", m -> reset());
        extension.intercept(HMessage.Direction.TOSERVER, "Quit", m -> reset());
        extension.intercept(HMessage.Direction.TOCLIENT, "RoomReady", m -> reset());
    }

    private void parseFloorPlan(HMessage hMessage) {
        synchronized (lock) {
            HPacket packet = hMessage.getPacket();
            packet.readByte();
            packet.readInteger();
            String raw = packet.readString();
            String[] split = raw.split("\r");
            floorplan = new char[split[0].length()][];
            for (int x = 0; x < split[0].length(); x++) {
                floorplan[x] = new char[split.length];
                for (int y = 0; y < split.length; y++) {
                    floorplan[x][y] = split[y].charAt(x);
                }
            }
        }
    }

    private void roomEntryInfo(HMessage hMessage) {
        if (latestRequestTimestamp > System.currentTimeMillis() - 400) {
            hMessage.setBlocked(true); // request wasnt made by user
            latestRequestTimestamp = -1;
        }
    }

    public boolean inRoom() {
        return furnimap != null && floorplan != null && heightmap != null;
    }
    public void reset() {
        if (heightmap != null || furnimap != null || floorplan != null) {
            synchronized (lock) {
                heightmap = null;
                furniIdToItem = null;
                furnimap = null;
                floorplan = null;
            }
        }
    }

    private void parseHeightmap(HMessage hMessage) {
        HPacket packet = hMessage.getPacket();

        int columns = packet.readInteger();
        int tiles = packet.readInteger();
        int rows = tiles/columns;

        int[][] heightmap = new int[columns][];
        for (int col = 0; col < columns; col++) {
            heightmap[col] = new int[rows];
        }

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < columns; col++) {
                heightmap[col][row] = packet.readUshort();
            }
        }

        synchronized (lock) {
            this.heightmap = heightmap;
        }
    }
    private void heightmapUpdate(HMessage hMessage) {
        if (heightmap != null) {
            HPacket packet = hMessage.getPacket();
            int updates = packet.readByte();

            for (int i = 0; i < updates; i++) {
                int x = packet.readByte();
                int y = packet.readByte();
                int height = packet.readUshort();
                heightmap[x][y] = height;
            }
        }
    }

    private void parseFloorItems(HMessage hMessage) {
        HFloorItem[] floorItems = HFloorItem.parse(hMessage.getPacket());

        synchronized (lock) {
            if (furnimap == null) {
                furniIdToItem = new HashMap<>();
                furnimap = new ArrayList<>();

                for (int i = 0; i < 130; i++) {
                    furnimap.add(new ArrayList<>());
                    for (int j = 0; j < 130; j++) {
                        furnimap.get(i).add(new HashMap<>());
                    }
                }
            }

            for (HFloorItem item : floorItems) {
                furnimap.get(item.getTile().getX()).get(item.getTile().getY()).put(item.getId(), item);
                furniIdToItem.put(item.getId(), item);
            }
        }
    }

    private void onObjectRemove(HMessage hMessage) {
        if (inRoom()) {
            HPacket packet = hMessage.getPacket();
            int furniid = Integer.parseInt(packet.readString());
            removeObject(furniid);
        }
    }
    private void removeObject(int furniId) {
        synchronized (lock) {
            HFloorItem item = furniIdToItem.remove(furniId);
            if (item != null) {
                furnimap.get(item.getTile().getX()).get(item.getTile().getY()).remove(item.getId());
            }
        }
    }
    private void onObjectAdd(HMessage hMessage) {
        if (inRoom()) {
            addObject(hMessage.getPacket(), null);
        }

    }
    private void addObject(HPacket packet, String ownerName) {
        synchronized (lock) {
            HFloorItem item = new HFloorItem(packet);
            if (ownerName == null) {
                ownerName = packet.readString();
            }
            item.setOwnerName(ownerName);

            furnimap.get(item.getTile().getX()).get(item.getTile().getY()).put(item.getId(), item);
            furniIdToItem.put(item.getId(), item);
        }
    }
    private void onObjectUpdate(HMessage hMessage) {
        if (inRoom()) {
            HFloorItem newItem = new HFloorItem(hMessage.getPacket());

            HFloorItem old = furniIdToItem.get(newItem.getId());
            String owner = "";
            if (old != null) {
                owner = old.getOwnerName();
            }

            removeObject(newItem.getId());
            hMessage.getPacket().resetReadIndex();
            addObject(hMessage.getPacket(), owner);
        }
    }
    private void onObjectMove(HMessage hMessage) {
        if (inRoom()) {
            HPacket packet = hMessage.getPacket();
            int oldx = packet.readInteger();
            int oldy = packet.readInteger();
            int newx = packet.readInteger();
            int newy = packet.readInteger();

            int amount = packet.readInteger();

            synchronized (lock) {
                for (int i = 0; i < amount; i++) {
                    int furniId = packet.readInteger();
                    String oldz = packet.readString();
                    String newz = packet.readString();

                    HFloorItem item = furniIdToItem.get(furniId);
                    if (item != null) {
                        furnimap.get(item.getTile().getX()).get(item.getTile().getY()).remove(item.getId());
                        item.setTile(new HPoint(newx, newy, Double.parseDouble(newz)));
                        furnimap.get(newx).get(newy).put(item.getId(), item);
                    }
                }
            }

//            int roller = packet.readInteger();
        }
    }

    private void onDataUpdate(HPacket hPacket, int id) {
//        int id = Integer.parseInt(hPacket.readString());
        int category = hPacket.readInteger();
        Object[] stuff = HStuff.readData(hPacket, category);


        HFloorItem item = null;
        synchronized (lock) {
            if (inRoom() && furniIdToItem.containsKey(id)) {
                item = furniIdToItem.get(id);
                item.setCategory(category);
                item.setStuff(stuff);
            }
        }
    }

    private void onDataUpdates(HMessage hMessage) {
        HPacket packet = hMessage.getPacket();
        int updatesCount = packet.readInteger();
        for (int i = 0; i < updatesCount; i++) {
            int id = packet.readInteger();
            onDataUpdate(packet, id);
        }
    }

    private void onDataUpdate(HMessage hMessage) {
        HPacket packet = hMessage.getPacket();
        onDataUpdate(hMessage.getPacket(), Integer.parseInt(packet.readString()));
    }

    public void requestRoom(ExtensionBase ext) {
        latestRequestTimestamp = System.currentTimeMillis();
        ext.sendToServer(new HPacket("GetHeightMap", HMessage.Direction.TOSERVER));
    }

    public HFloorItem furniFromId(int id) {
        synchronized (lock) {
            return furniIdToItem.get(id);
        }
    }

    public List<HFloorItem> getFurniOnTile(int x, int y) {
        synchronized (lock) {
            if (inRoom()) {
                return new ArrayList<>(furnimap.get(x).get(y).values());
            }
        }
        return new ArrayList<>();
    }

    public double getTileHeight(int x, int y) {
        synchronized (lock) {
            return ((double)heightmap[x][y]) / 256;
        }
    }

    public char floorHeight(int x, int y) {
        char result;
        synchronized (lock) {
            result = (floorplan != null && x >= 0 && y >= 0 &&
                    x < floorplan.length && y < floorplan[x].length) ? floorplan[x][y] : 'x';
        }
        return result;
    }

    public ItemUsage itemUsability(int furniId, FurniDataTools furniDataTools) {
        HFloorItem item = furniIdToItem.get(furniId);
        if (item == null) return ItemUsage.NOT_USABLE;

        if (item.getUsagePolicy() == 0) return ItemUsage.NOT_USABLE;
        if (item.getUsagePolicy() == 1) return ItemUsage.USABLE_WITH_RIGHTS;
        if (item.getUsagePolicy() == 2) {
            if (furniDataTools.isOneWayGate(item.getTypeId())) {
                return ItemUsage.ONE_WAY_GATE;
            }
            return ItemUsage.USABLE_FOR_EVERYONE;
        }
        return ItemUsage.NOT_USABLE;
    }

}
