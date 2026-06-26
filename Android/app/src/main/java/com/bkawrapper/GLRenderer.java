package com.bkawrapper;

import android.content.Context;
import android.content.res.AssetManager;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Log;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * GLRenderer
 *
 * Drives the N64 framebuffer → Android display pipeline.
 * Ensures the native engine boot is deferred until the GL context is fully ready.
 */
public class GLRenderer implements GLSurfaceView.Renderer {

    private static final String TAG = "BKA-GLRenderer";

    private final Context context;
    private final String assetDir;
    private final AssetManager mgr;

    // Use a volatile flag to ensure thread-safe access from the GLThread
    private static volatile boolean engineBooted = false;
    private boolean isSurfaceReady = false;

    public GLRenderer(Context context, String assetDir, AssetManager mgr) {
        this.context = context;
        this.assetDir = assetDir;
        this.mgr = mgr;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        Log.i(TAG, "onSurfaceCreated: GL context ready");
        GLES20.glClearColor(0f, 0f, 0f, 1f);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        Log.i(TAG, "onSurfaceChanged: " + width + "×" + height);
        GLES20.glViewport(0, 0, width, height);

        // Tell the native side the GL context is alive and provide ACTUAL dimensions
        NativeBridge.surfaceReady(width, height);
        isSurfaceReady = true;
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        // Defer boot to the first frame render. 
        // This ensures the GL context is fully bound, preventing early initialization crashes.
        if (!engineBooted && isSurfaceReady) {
            engineBooted = true;
            Log.i(TAG, "Booting native engine on first frame — assetDir=" + assetDir);
            
            // NativeBridge.nativeGameBoot spawns the detached pthread and returns immediately.
            NativeBridge.nativeGameBoot(assetDir, mgr);
        }

        if (isSurfaceReady) {
            // Standard frame render cycle
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
            NativeBridge.updateTexture(0);
        }
    }
}