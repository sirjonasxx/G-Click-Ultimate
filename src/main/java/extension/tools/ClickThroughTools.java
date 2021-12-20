package extension.tools;

import extension.GClickUltimateController;
import extension.listeners.EnabledListener;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import javafx.beans.Observable;

public class ClickThroughTools {

    private final GClickUltimateController extension;

    private volatile boolean isClickThrough = false;
    private volatile boolean clickThroughOnFirstWalk = false;

    private volatile boolean legitHelmet = false; // would the user have a helmet if gclickthrough was disabled

    public ClickThroughTools(GClickUltimateController e) {
        this.extension = e;
        e.intercept(HMessage.Direction.TOCLIENT, "RoomReady", hMessage -> onNewRoomEnter());

        e.intercept(HMessage.Direction.TOSERVER, "MoveAvatar", this::onMoveAvatar);

        e.intercept(HMessage.Direction.TOSERVER, "LookTo", this::maybeBlockLoad);
        e.intercept(HMessage.Direction.TOSERVER, "GetSelectedBadges", this::maybeBlockLoad);
        e.intercept(HMessage.Direction.TOSERVER, "GetRelationshipStatusInfo", this::maybeBlockLoad);

        e.intercept(HMessage.Direction.TOCLIENT, "YouArePlayingGame", this::onPlayingGame);

        e.ctAlways_rdb.selectedProperty().addListener(this::onSelectionChange);
        e.ctEnabled_rdb.selectedProperty().addListener(this::onSelectionChange);
        e.ctDisabled_rdb.selectedProperty().addListener(this::onSelectionChange);

        e.getEnabledObservable().addListener(new EnabledListener() {
            @Override
            public void enabled() {
                onSelectionChange(null);
            }

            @Override
            public void disabled() {
                clickThroughOnFirstWalk = false;
                isClickThrough = legitHelmet;
                extension.sendToClient(new HPacket("YouArePlayingGame", HMessage.Direction.TOCLIENT, isClickThrough));
            }
        });
    }

    private void onSelectionChange(Observable o) {
        if (extension.isEnabled()) {
            clickThroughOnFirstWalk = false;
            isClickThrough = legitHelmet || extension.ctAlways_rdb.isSelected() || extension.ctEnabled_rdb.isSelected();

            extension.sendToClient(new HPacket("YouArePlayingGame", HMessage.Direction.TOCLIENT, isClickThrough));
        }
    }

    private void onPlayingGame(HMessage hMessage) {
        boolean status = hMessage.getPacket().readBoolean();
        legitHelmet = status;
        if (extension.isEnabled()) {
            if (!status && (extension.ctEnabled_rdb.isSelected() || extension.ctAlways_rdb.isSelected())) {
                hMessage.setBlocked(true);
            }
            else {
                isClickThrough = status;
            }
        }
        else {
            isClickThrough = status;
        }
    }

    private void maybeBlockLoad(HMessage hMessage) {
        if (extension.isEnabled() && extension.ctBlockSideloads_cbx.isSelected() && isClickThrough) {
            hMessage.setBlocked(true);
        }
    }

    private void onMoveAvatar(HMessage hMessage) {
        if (extension.isEnabled()) {
            if (extension.getShiftUtils().isShiftClicked() /*&& isClickThrough*/ && extension.ctShiftNoWalk_cbx.isSelected()) {
                hMessage.setBlocked(true);
            }
            if (clickThroughOnFirstWalk) {
                clickThroughOnFirstWalk = false;
                isClickThrough = true;
                extension.sendToClient(new HPacket("YouArePlayingGame", HMessage.Direction.TOCLIENT, isClickThrough));
            }
        }
    }

    private void onNewRoomEnter() {
        legitHelmet = false;
        if (extension.isEnabled()) {
            isClickThrough = false;
            clickThroughOnFirstWalk = false;
            if (extension.ctEnabled_rdb.isSelected()) {
                extension.ctDisabled_rdb.setSelected(true);
            }
            else if (extension.ctAlways_rdb.isSelected()) {
                clickThroughOnFirstWalk = true;
            }
        }
    }
}
