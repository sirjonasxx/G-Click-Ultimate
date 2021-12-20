package extension.tools;

import extension.GClickUltimateController;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Paint;
import misc.Functions;

import java.util.LinkedList;

/*
 * Utilities for
 * - click recharge (activity & latest ratelimit reset)
 * - all furni toggles go through here for correct scheduling of the recharge furni
 */
public class ToggleFurniUtils {

    private final Object rechargeLock = new Object();

    private final GClickUltimateController extension;

    private volatile long latestRechargeTimestamp = -1;

    private volatile boolean awaitFridge = false;
    private volatile int fridge = -1;

    public ToggleFurniUtils(GClickUltimateController e) {
        this.extension = e;

        abortRecharge();
        e.clickRechargeBtn.setOnAction(o -> {
            if (fridge == -1 && !awaitFridge) {
                enableRecharge();
            }
            else {
                abortRecharge();
            }
        });

        e.intercept(HMessage.Direction.TOSERVER, "UseFurniture", hMessage -> {
            synchronized (rechargeLock) {
                if (awaitFridge) {
                    hMessage.setBlocked(true);
                    fridge = hMessage.getPacket().readInteger();
                    awaitFridge = false;

                    Platform.runLater(() -> {
                        extension.rechargeLbl.setText("Enabled");
                        extension.rechargeStatePane.setBackground(new Background(new BackgroundFill(Paint.valueOf("#9AFD9F"), CornerRadii.EMPTY, Insets.EMPTY)));
                    });
                }
                else {
                    toggleInterceptHandler(hMessage);
                }
            }
        });
        extension.intercept(HMessage.Direction.TOCLIENT, "CloseConnection", m -> abortRecharge());
        extension.intercept(HMessage.Direction.TOSERVER, "Quit", m -> abortRecharge());
        extension.intercept(HMessage.Direction.TOCLIENT, "RoomReady", m -> abortRecharge());
        extension.getDisconnectObservable().addListener(n -> abortRecharge());

        new Thread(this::rechargeLoop).start();
        new Thread(this::toggleLoop).start();
    }

    private void rechargeLoop() {
        while(true) {
            Functions.sleep(20);
            synchronized (rechargeLock) {
                long now = System.currentTimeMillis();
                if (isActive() && now > latestRechargeTimestamp + 1400 && extension.getEnvironmentUtils().getTimeSinceTick() < 160) {
                    sendToggleMaybeDelayed(new HPacket("UseFurniture", HMessage.Direction.TOSERVER, fridge, 0));
                    updateLatestRecharge();
                }
            }
        }
    }

    private void abortRecharge() {
        synchronized (rechargeLock) {
            fridge = -1;
            awaitFridge = false;
        }
        Platform.runLater(() -> {
            extension.rechargeLbl.setText("No recharge");
            extension.clickRechargeBtn.setText("Select click recharger");
            extension.rechargeStatePane.setBackground(new Background(new BackgroundFill(Paint.valueOf("#F9B2B0"), CornerRadii.EMPTY, Insets.EMPTY)));
        });
    }

    private void enableRecharge() {
        synchronized (rechargeLock) {
            awaitFridge = true;
        }
        Platform.runLater(() -> {
            extension.rechargeLbl.setText("Awaiting furni");
            extension.clickRechargeBtn.setText("Abort");
            extension.rechargeStatePane.setBackground(new Background(new BackgroundFill(Paint.valueOf("#FFC485"), CornerRadii.EMPTY, Insets.EMPTY)));
        });
    }

    private boolean isActive() {
        return extension.isEnabled() && fridge != -1;
    }

    public void updateLatestRecharge() {
        latestRechargeTimestamp = System.currentTimeMillis();
    }



    private LinkedList<HPacket> togglePackets = new LinkedList<>();
    private volatile long latestToggleTimestamp = -1;
    private final Object toggleLoopLock = new Object();

    public void toggleInterceptHandler(HMessage hMessage) {
        boolean handleLater = false;

        synchronized (toggleLoopLock) {
            long now = System.currentTimeMillis();
            if (togglePackets.size() != 0 || latestToggleTimestamp > now - 110) {
                hMessage.setBlocked(true);
                handleLater = true;
            }
            else {
                latestToggleTimestamp = now;
            }
        }

        if (handleLater) {
            sendToggleMaybeDelayed(hMessage.getPacket());
        }
    }

    public void sendToggleMaybeDelayed(HPacket packet) {
        synchronized (toggleLoopLock) {
            if (togglePackets.size() > 2) return; // discard

            long now =  System.currentTimeMillis();

            if (togglePackets.size() == 0 && latestToggleTimestamp <= now - 110) {
                extension.sendToServer(packet);
                latestToggleTimestamp = now;
            }
            else {
                togglePackets.add(packet);
            }
        }
    }

    private void toggleLoop() {
        while (true) {
            Functions.sleep(5);
            synchronized (toggleLoopLock) {
                long now = System.currentTimeMillis();
                if (latestToggleTimestamp <= now - 110 && togglePackets.size() > 0) {
                    extension.sendToServer(togglePackets.pollFirst());
                    latestToggleTimestamp = now;
                }
            }
        }
    }

}
