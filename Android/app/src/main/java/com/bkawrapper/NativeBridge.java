package com.bkawrapper;

import android.content.res.AssetManager;

public class NativeBridge {

    static {
        System.loadLibrary("bkawrapper");
    }

    // Still used for context initialization in the C++ layer
    public static native void nativeInit(OtrService service);

    // Used for booting the engine on a background C++ pthread once extraction is confirmed
    public static native void nativeGameBoot(String otrPath, AssetManager assetManager);

    // Signals C++ that the EGL Surface is bound and ready to draw
    public static native void surfaceReady(int width, int height);

    // Called from GLRenderer's onDrawFrame() to flush the texture payload to the screen
    public static native void updateTexture(int unused);

    // Passes joystick and button states to the background N64 emulator loop
    public static native void nativeUpdateInput(int buttonMask, float stickX, float stickY);
}