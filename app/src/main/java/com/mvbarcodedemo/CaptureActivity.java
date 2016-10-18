package com.mvbarcodedemo;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;
import com.mvbarcodedemo.camera.CustomStatusLayout;
import com.mvbarcodedemo.camera.GraphicOverlay;
import com.mvbarcodedemo.detecthelper.BarcodeGraphic;
import com.mvbarcodedemo.detecthelper.BarcodeTrackerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;

public class CaptureActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = CaptureActivity.class.getSimpleName();
    private Context context;
    private RelativeLayout parentFrm;
    private SurfaceView camera_preview;
    private GraphicOverlay graphic_overlay;
    private BarcodeDetector barcodeDetector;
    private CameraSource cameraSource;

    private GestureDetector gestureDetector;
    private boolean isSurfaceCreated = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_capture);

        context = CaptureActivity.this;
        initAllItems();
        initCameraSource();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraSource.release(); //release the resources
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isSurfaceCreated)
            startCameraSource();
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        boolean c = gestureDetector.onTouchEvent(e);
        return c || super.onTouchEvent(e);
    }

    @Override
    public void onClick(View view) {

    }

    private void initAllItems() {
        linearLayout = (LinearLayout) findViewById(R.id.linear_status);
        camera_preview = (SurfaceView) findViewById(R.id.camera_preview);
        graphic_overlay = (GraphicOverlay) findViewById(R.id.graphic_overlay);

        linearLayout.setOnClickListener(this);
        parentFrm = (RelativeLayout) findViewById(R.id.activity_capture);
        gestureDetector = new GestureDetector(this, new CaptureGestureListener());

        barcodeDetector = new BarcodeDetector.Builder(context).build();
        BarcodeTrackerFactory trackerFactory = new BarcodeTrackerFactory(graphic_overlay);
        barcodeDetector.setProcessor(new MultiProcessor.Builder<>(trackerFactory).build());
    }

    private void initCameraSource() {

        DisplayMetrics metrics = getResources().getDisplayMetrics();
        boolean isPortrait = isPortraitMode();

        cameraSource = new CameraSource.Builder(context, barcodeDetector)
                .setAutoFocusEnabled(true)
                .setFacing(CameraSource.CAMERA_FACING_BACK)
                .setRequestedFps(15)
                .setRequestedPreviewSize(isPortrait ? metrics.heightPixels : metrics.widthPixels, isPortrait ? metrics.widthPixels : metrics.heightPixels)
                .build();

        camera_preview.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder surfaceHolder) {
                isSurfaceCreated = true;
                startCameraSource();
            }

            @Override
            public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

            }
        });

    }

    private void startCameraSource() {
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            cameraSource.start(camera_preview.getHolder());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class CaptureGestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            return onTap(e.getRawX(), e.getRawY()) || super.onSingleTapConfirmed(e);
        }
    }

    private boolean onTap(float rawX, float rawY) {
        // Find tap point in preview frame coordinates.
        int[] location = new int[2];
        graphic_overlay.getLocationOnScreen(location);
        float x = (rawX - location[0]) / graphic_overlay.getWidthScaleFactor();
        float y = (rawY - location[1]) / graphic_overlay.getHeightScaleFactor();

        // Find the barcode whose center is closest to the tapped point.
        Barcode best = null;
        float bestDistance = Float.MAX_VALUE;
        List<BarcodeGraphic> graphicsList = graphic_overlay.getGraphics();
        for (int i = 0; i < graphicsList.size(); i++) {
            Barcode barcode = graphicsList.get(i).getBarcode();
            if (barcode.getBoundingBox().contains((int) x, (int) y)) {
                // Exact hit, no need to keep looking.
                best = barcode;
                break;
            }
            float dx = x - barcode.getBoundingBox().centerX();
            float dy = y - barcode.getBoundingBox().centerY();
            float distance = (dx * dx) + (dy * dy);  // actually squared distance
            if (distance < bestDistance) {
                best = barcode;
                bestDistance = distance;
            }
        }

        if (best != null) {
//            Snackbar.make(parentFrm, "Detect Code: " + best.rawValue,
//                    Snackbar.LENGTH_SHORT)
//                    .show();
            addView(best.rawValue);
            return true;
        }
        return false;
    }

    public boolean isPortraitMode() {
        int orientation = context.getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            return false;
        }
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            return true;
        }

        Log.d(TAG, "isPortraitMode returning false by default");
        return false;
    }


    private LinearLayout linearLayout;
    private LinkedHashMap<String, String> stackData = new LinkedHashMap<String, String>();
    private String[] codeFormate = {"D", "0", "S"};

    private enum CodeType {DOC, FOLDER, SHELF}

    private void addView(String code) {

        boolean currentCodeStatus = false;
        if (stackData.size() >= 3) {
            stackData.clear();
            linearLayout.removeAllViews();
        }

        if (stackData.size() == 0) {
            if (code.contains(codeFormate[CodeType.DOC.ordinal()])) {
                currentCodeStatus = true;
            } else if (code.contains(codeFormate[CodeType.FOLDER.ordinal()])) {
                currentCodeStatus = true;
            } else {
                currentCodeStatus = false;
            }
        }

        if (stackData.size() == 1) {
            String prevCode = stackData.entrySet().toArray()[0].toString();
            if (prevCode.contains(codeFormate[CodeType.DOC.ordinal()]) && code.contains(codeFormate[CodeType.FOLDER.ordinal()])) {
                currentCodeStatus = true;
            } else if (prevCode.contains(codeFormate[CodeType.FOLDER.ordinal()]) && code.contains(codeFormate[CodeType.SHELF.ordinal()])) {
                currentCodeStatus = true;
            } else {
                currentCodeStatus = false;
            }
        }

        stackData.put(code, code);

        RelativeLayout layout = (RelativeLayout) LayoutInflater.from(context).inflate(R.layout.include_status, null);
        layout.setLayoutParams(new RelativeLayout.LayoutParams((getResources().getDisplayMetrics().widthPixels) / 3, RelativeLayout.LayoutParams.WRAP_CONTENT));
        CustomStatusLayout statusLayout = (CustomStatusLayout) layout.findViewById(R.id.view_status);
        ImageView imgCross = (ImageView) layout.findViewById(R.id.img_cross);
        (imgCross).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                linearLayout.removeView((RelativeLayout) view.getParent());
                stackData.remove(view.getTag().toString());
            }
        });
        statusLayout.setStatusOK(currentCodeStatus);
        statusLayout.setCode(code);
        imgCross.setTag(code);
        linearLayout.addView(layout);
    }

}
