package extension.tools;

import org.jnativehook.GlobalScreen;
import org.jnativehook.NativeHookException;
import org.jnativehook.keyboard.NativeKeyEvent;
import org.jnativehook.keyboard.NativeKeyListener;

import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class ShiftUtils implements NativeKeyListener {

    private volatile boolean shiftClicked = false;

    public ShiftUtils() {
        // https://stackoverflow.com/questions/26565236/jnativehook-how-do-you-keep-from-printing-everything-that-happens
        // Clear previous logging configurations.
        LogManager.getLogManager().reset();

        // Get the logger for "org.jnativehook" and set the level to off.
        Logger logger = Logger.getLogger(GlobalScreen.class.getPackage().getName());
        logger.setLevel(Level.OFF);

        try {
            GlobalScreen.registerNativeHook();
        } catch (NativeHookException e) {
            e.printStackTrace();
        }

        GlobalScreen.addNativeKeyListener(this);
    }

    public boolean isShiftClicked() {
        return shiftClicked;
    }

    @Override
    public void nativeKeyPressed(NativeKeyEvent e) {
        if (e.getKeyCode() == NativeKeyEvent.VC_SHIFT_L) {
            shiftClicked = true;
        }
    }

    @Override
    public void nativeKeyReleased(NativeKeyEvent e) {
        if (e.getKeyCode() == NativeKeyEvent.VC_SHIFT_L) {
            shiftClicked = false;
        }
    }

    @Override
    public void nativeKeyTyped(NativeKeyEvent e) { }
}
