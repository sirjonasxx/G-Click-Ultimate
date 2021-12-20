package extension.tools;

import extension.GClickUltimateController;
import extension.game.ItemUsage;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import misc.Functions;

public class OtherClickTools {

    private final GClickUltimateController extension;

    public OtherClickTools(GClickUltimateController extension) {
        this.extension = extension;

        extension.intercept(HMessage.Direction.TOSERVER, "MoveObject", this::onMoveObject);

        new Thread(this::antiAfkLoop).start();
        extension.intercept(HMessage.Direction.TOCLIENT, "Whisper", this::onWhisper);
    }

    private void onWhisper(HMessage hMessage) {
        if (extension.isEnabled() && extension.antiAfkCbx.isSelected()) {
            HPacket packet = hMessage.getPacket();
            packet.readInteger();
            String text = packet.readString();
            if (text.equals("-")) {
                hMessage.setBlocked(true);
            }
        }
    }

    private void antiAfkLoop() {
        while (true) {
            Functions.sleep(280000);
            if (extension.isEnabled() && extension.antiAfkCbx.isSelected()) {
                extension.sendToServer(new HPacket("Whisper", HMessage.Direction.TOSERVER, "x -", 0));
            }
        }
    }

    private void onMoveObject(HMessage hMessage) {
        int furniId = hMessage.getPacket().readInteger();

        if (extension.isEnabled() && extension.getShiftUtils().isShiftClicked()) {
            if (extension.blockShiftRotations_cbx.isSelected()) {
                hMessage.setBlocked(true);
            }
            if (extension.autoShiftDoubleClick_cbx.isSelected() && extension.getFloorState().inRoom()) {
                ItemUsage itemUsage = extension.getFloorState().itemUsability(furniId, extension.getFurniDataTools());
                if (itemUsage == ItemUsage.USABLE_FOR_EVERYONE) {
                    extension.getToggleFurniUtils().sendToggleMaybeDelayed(
                            new HPacket("UseFurniture", HMessage.Direction.TOSERVER, furniId, 0)
                    );
                }
                else if (itemUsage == ItemUsage.ONE_WAY_GATE) {
                    extension.getToggleFurniUtils().updateLatestRecharge();
                    extension.sendToServer(new HPacket("EnterOneWayDoor", HMessage.Direction.TOSERVER, furniId));
                }
            }
        }
    }
}
