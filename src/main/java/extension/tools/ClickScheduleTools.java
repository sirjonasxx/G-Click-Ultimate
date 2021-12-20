package extension.tools;

import extension.GClickUltimateController;
import gearth.protocol.HMessage;
import misc.Functions;

public class ClickScheduleTools {

    private final GClickUltimateController extension;

    private volatile long latestWalkTimestamp = -1;
    private volatile boolean isClicking = false;

    public ClickScheduleTools(GClickUltimateController e) {
        this.extension = e;

        e.intercept(HMessage.Direction.TOSERVER, "MoveAvatar", this::onUserWalk);

    }

    private void onUserWalk(HMessage hMessage) {
        if (!extension.isEnabled() || !extension.csScheduleClicks_cbx.isSelected()) {
            latestWalkTimestamp = System.currentTimeMillis();
            return;
        }

        if (extension.getShiftUtils().isShiftClicked() && extension.ctShiftNoWalk_cbx.isSelected()) {
            // blocked by clickthrough
            return;
        }

        if (isClicking) {
            hMessage.setBlocked(true);
            return;
        }

        long now = System.currentTimeMillis();

        // guarantee success click
        if (now >= latestWalkTimestamp + 500 + Math.max(burstAmount() * burstTimeout(), Math.max(extension.getEnvironmentUtils().getPingVariation(), 13))) {
//            System.out.println(String.format("now: %d", now));
//            System.out.println(String.format("reference: %d + %d", latestWalkTimestamp, Math.max(burstAmount() * burstTimeout(), Math.max(extension.getEnvironmentUtils().getPingVariation(), 13))));
            latestWalkTimestamp = now;
        }
        else {
            int awaitTime = (int) (latestWalkTimestamp + 503 - now);
            hMessage.setBlocked(true);
            isClicking = true;
            new Thread(() -> {
                if (awaitTime > 0) Functions.sleep(awaitTime);
                latestWalkTimestamp = System.currentTimeMillis();

                boolean inCurrentTick = extension.getEnvironmentUtils().getTimeSinceTick() < 500 - extension.getEnvironmentUtils().getPing() - 25;
                int clicks = burstAmount();
                for (int i = 0; i < clicks; i++) {
                    // will be clicked in next tick, avoid
                    if (inCurrentTick && extension.getEnvironmentUtils().getTimeSinceTick() >= 500 - extension.getEnvironmentUtils().getPing() - 5) {
                        break;
                    }
                    extension.sendToServer(hMessage.getPacket());
                    if (i < clicks - 1) {
                        Functions.sleep(burstTimeout());
                    }
                }
                isClicking = false;
            }).start();
        }
    }

    private int burstAmount() {
        return extension.csBurstClicks_cbx.isSelected() ? extension.burst_click_spinner.getValue() : 1;
    }

    private int burstTimeout() {
        return 5;
    }
}
