package com.dxmtb.westonapp;

import android.content.Context;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

class WestonView extends SurfaceView implements SurfaceHolder.Callback {
    private SurfaceHolder holder;
    private DrawThread myThread;
    final private String TAG = "WestonView";

    private String imagePath;
    private int width, height;

    public WestonView(Context context, String imagePath, int width, int height) {
        super(context);

        this.imagePath = imagePath;
        this.width = width;
        this.height = height;

        holder = this.getHolder();
        holder.addCallback(this);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {
        Log.d(TAG, "View Width and Height: " + width + "x" + height);
        if (!myThread.isAlive()) {
//            myThread.width = width;
//            myThread.height = height;
            myThread.isRun = true;
            myThread.start();
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        myThread = new DrawThread(holder, imagePath, width, height);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d("weston", "surface destroy");
        myThread.isRun = false;
        myThread = null;
    }

}
