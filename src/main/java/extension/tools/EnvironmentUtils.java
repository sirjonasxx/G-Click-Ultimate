package extension.tools;


import extension.GClickUltimateController;
import gearth.extensions.parsers.HAction;
import gearth.extensions.parsers.HEntityUpdate;
import gearth.protocol.HMessage;

import java.util.Arrays;

// keep track of room tick, ping, ping variation, safe ratelimit timeouts
public class EnvironmentUtils {

//    private final GClickUltimateController extension;

    private volatile long latestPingTimestamp = -1;

    private volatile int ping = 45;
    private volatile double pingVariation = 10;
    private volatile long latestRoomTick = -1;

    public EnvironmentUtils(GClickUltimateController e) {
//        this.extension = e;

        e.intercept(HMessage.Direction.TOSERVER, "LatencyPingRequest", hMessage -> {
            latestPingTimestamp = System.currentTimeMillis();
        });
        e.intercept(HMessage.Direction.TOCLIENT, "LatencyPingResponse", hMessage -> {
            if (latestPingTimestamp != -1) {
                int newPing = (int) (System.currentTimeMillis() - latestPingTimestamp) / 2;
                pingVariation = pingVariation * 0.66 + (Math.abs(ping - newPing)) * 0.34;
                if (pingVariation > 15) {
                    pingVariation = 15;
                }
                ping = newPing;
            }
        });

        e.intercept(HMessage.Direction.TOCLIENT, "UserUpdate", hMessage -> {
            if (Arrays.stream(HEntityUpdate.parse(hMessage.getPacket()))
                    .anyMatch(hEntityUpdate -> hEntityUpdate.getAction() == HAction.Move) &&
                    System.currentTimeMillis() > latestRoomTick + 400) {
                latestRoomTick = System.currentTimeMillis() - ping;
            }
        });
    }

    public int getTimeSinceTick() {
        if (latestRoomTick == -1) {
            return 0;
        }

        long now = System.currentTimeMillis();
        return (int) ((now - latestRoomTick) + 500) % 500;
    }

    public int getPingVariation() {
        return (int) Math.ceil(pingVariation);
    }

    public int getPing() {
        return ping;
    }

}
