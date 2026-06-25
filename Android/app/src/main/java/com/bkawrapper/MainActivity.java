package com.bkawrapper;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.opengl.GLSurfaceView;

import java.io.File;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class MainActivity extends AppCompatActivity {

    private static final String TAG              = "BKA-MainActivity";
    private static final int    PICK_ROM_REQUEST = 1001;
    private static final String SENTINEL_FILENAME = "extraction_complete";

    private View        menuOverlay;
    private View        otrContainer;
    private ProgressBar progressBar;
    private TextView    progressText;
    private TextView    currentArtifactText;
    private GLSurfaceView glSurfaceView;

    static {
        System.loadLibrary("bkawrapper");
    }

    private final BroadcastReceiver progressReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) return;

            switch (action) {
                case OtrService.ACTION_OTR_PROGRESS: {
                    int percent = intent.getIntExtra("percent", 0);
                    String status = intent.getStringExtra("status");
                    updateUI(percent, status);
                    break;
                }
                case OtrService.ACTION_OTR_COMPLETE:
                    handleExtractionComplete();
                    break;
                case OtrService.ACTION_OTR_ERROR:
                    handleExtractionError(intent.getStringExtra("message"));
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (hasExtractionCompleted()) {
            bootGameEngine();
        } else {
            neutralizeXmlGLSurfaceView((ViewGroup) findViewById(android.R.id.content));
            menuOverlay         = findViewById(R.id.menu_overlay);
            otrContainer        = findViewById(R.id.otr_ui_container);
            progressBar         = findViewById(R.id.otr_progress_bar);
            progressText        = findViewById(R.id.otr_progress_text);
            currentArtifactText = findViewById(R.id.otr_current_artifact);
            new MenuController(this);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter();
        filter.addAction(OtrService.ACTION_OTR_PROGRESS);
        filter.addAction(OtrService.ACTION_OTR_COMPLETE);
        filter.addAction(OtrService.ACTION_OTR_ERROR);
        LocalBroadcastManager.getInstance(this).registerReceiver(progressReceiver, filter);
        if (glSurfaceView != null) glSurfaceView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(progressReceiver);
        if (glSurfaceView != null) glSurfaceView.onPause();
    }

    private boolean hasExtractionCompleted() {
        File sentinel = new File(getFilesDir(), SENTINEL_FILENAME);
        File romBase  = new File(getFilesDir(), "rom_base.bin");
        return sentinel.exists() && romBase.exists();
    }

    private void neutralizeXmlGLSurfaceView(ViewGroup group) {
        if (group == null) return;
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            if (child instanceof GLSurfaceView) {
                GLSurfaceView dummy = (GLSurfaceView) child;
                dummy.setVisibility(View.GONE);
            } else if (child instanceof ViewGroup) {
                neutralizeXmlGLSurfaceView((ViewGroup) child);
            }
        }
    }

    private void handleExtractionComplete() {
        // Use a Handler to post to the main Looper to ensure the UI thread 
        // has finished the transition before we swap the surface.
        new Handler(Looper.getMainLooper()).postDelayed(this::bootGameEngine, 500);
    }

    private void bootGameEngine() {
        Log.i(TAG, "Booting Game Engine (Async Context Binding)");
        
        final String assetDir = getFilesDir().getAbsolutePath();
        final AssetManager mgr = getAssets();

        glSurfaceView = new GLSurfaceView(this);
        glSurfaceView.setEGLContextClientVersion(3);
        glSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 24, 8);
        glSurfaceView.setPreserveEGLContextOnPause(true);

        // GLRenderer now runs on its own dedicated GLThread managed by GLSurfaceView.
        // This stops the main UI deadlock.
        glSurfaceView.setRenderer(new GLRenderer(this, assetDir, mgr));
        glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        setContentView(glSurfaceView);
    }

    // [Keep original openFilePicker, onActivityResult, startExtraction, updateUI, handleExtractionError methods here]
    // (Truncated for brevity in response, ensure they are included in your actual file)
    public void openFilePicker() { /* ... */ }
    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) { /* ... */ }
    private void startExtraction(Uri romUri) { /* ... */ }
    private void updateUI(int percent, String fileName) { /* ... */ }
    private void handleExtractionError(String message) { /* ... */ }
}