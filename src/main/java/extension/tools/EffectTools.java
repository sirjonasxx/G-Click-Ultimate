package extension.tools;

import extension.GClickUltimateController;
import extension.listeners.EnabledListener;
import gearth.extensions.parsers.HEntity;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import javafx.scene.control.RadioButton;
import misc.Functions;

public class EffectTools {

    private final GClickUltimateController extension;

    private int userId = -1;
    private int userIndex = -1;

    private int realEffect = 0;

    public EffectTools(GClickUltimateController e) {
        this.extension = e;

        extension.intercept(HMessage.Direction.TOCLIENT, "UserObject", this::onSelfUserObject);
        extension.sendToServer(new HPacket("InfoRetrieve", HMessage.Direction.TOSERVER));

        extension.intercept(HMessage.Direction.TOCLIENT, "Users", this::onUsersLoad);

        extension.intercept(HMessage.Direction.TOCLIENT, "Whisper", this::onWhisper);
        extension.sendToServer(new HPacket("Whisper", HMessage.Direction.TOSERVER, "x {", 0)); // initial index retrieve

        extension.intercept(HMessage.Direction.TOCLIENT, "CloseConnection", m -> reset());
        extension.intercept(HMessage.Direction.TOSERVER, "Quit", m -> reset());
        extension.intercept(HMessage.Direction.TOCLIENT, "RoomReady", m -> reset());
        extension.getDisconnectObservable().addListener(n -> reset());

        extension.intercept(HMessage.Direction.TOCLIENT, "AvatarEffect", this::onEffect);

        extension.getEnabledObservable().addListener(new EnabledListener() {
            @Override
            public void enabled() {
                if (!extension.ceNone_rdb.isSelected()) {
                    changeEffect();
                }
            }

            @Override
            public void disabled() {
                if (userIndex != -1) {
                    extension.sendToClient(
                            new HPacket("AvatarEffect", HMessage.Direction.TOCLIENT,
                                    userIndex, realEffect, 0)
                    );
                }
            }
        });

        RadioButton[] switches = {extension.ceDuck_rdb, extension.ceGhost_rdb, extension.ceNone_rdb, extension.ceBot_rdb, extension.ceMini_rdb};
        for (RadioButton switc : switches) switc.selectedProperty().addListener(o -> changeEffect());

    }

    private void changeEffect() {
        if (userIndex != -1) {
            extension.sendToClient(
                    new HPacket("AvatarEffect", HMessage.Direction.TOCLIENT,
                            userIndex, getOverrideEffect(), 0)
            );
        }
    }

    private void onEffect(HMessage hMessage) {
        HPacket hPacket = hMessage.getPacket();
        int index = hPacket.readInteger();
        int effect = hPacket.readInteger();

        if (index == userIndex && userIndex != -1) {
            realEffect = effect;

            if (extension.isEnabled()) {
                if (effect == 0 || (!extension.ceNone_rdb.isSelected() && effect >= 40 && effect < 44)) {
                    hPacket.replaceInt(10, getOverrideEffect());
                }
            }
        }
    }

    private void reset() {
        userIndex = -1;
        realEffect = 0;

        if (!extension.ceKeep_cbx.isSelected()) {
            extension.ceNone_rdb.setSelected(true);
        }
    }

    private void onWhisper(HMessage hMessage) {
        HPacket packet = hMessage.getPacket();
        int index = packet.readInteger();
        String text = packet.readString();

        if (userIndex == -1 && text.equals("{")) {
            userIndex = index;
            hMessage.setBlocked(true);
        }
    }

    private void onUsersLoad(HMessage hMessage) {
        HEntity[] users = HEntity.parse(hMessage.getPacket());
        for (HEntity hEntity : users) {
            if (userId != -1 && hEntity.getId() == userId) {
                userIndex = hEntity.getIndex();
                if (!extension.ceNone_rdb.isSelected() && extension.isEnabled()) {
                    new Thread(() -> {
                        Functions.sleep(500);
                        if (extension.isEnabled()) {
                            changeEffect();
                        }
                    }).start();
                }
            }
        }
    }

    private void onSelfUserObject(HMessage hMessage) {
        userId = hMessage.getPacket().readInteger();
    }

    private int getOverrideEffect() {
        if (extension.ceNone_rdb.isSelected()) return realEffect;
        if (extension.ceGhost_rdb.isSelected()) return 13;
        if (extension.ceDuck_rdb.isSelected()) return 170;
        if (extension.ceBot_rdb.isSelected()) return 188;
        if (extension.ceMini_rdb.isSelected()) return 189;
        return realEffect;
    }
}
