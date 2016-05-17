package com.dxmtb.westonapp;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.util.Log;
import android.view.SurfaceHolder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

class DrawThread extends Thread {
    final private String TAG = "DrawThread";

    public boolean isRun;

    private SurfaceHolder holder;
    private int width, height;
    private String imagePath;


    public DrawThread(SurfaceHolder holder, String imagePath, int width, int height) {
        this.holder = holder;
        this.width = width;
        this.height = height;
        this.imagePath = imagePath;
        isRun = true;
    }

    public MappedByteBuffer getMappedBuffer() {
        FileChannel channel;
        try {
            channel = (new FileInputStream(new File(imagePath))).getChannel();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }

        MappedByteBuffer byteBuffer;
        try {
            byteBuffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, (int) channel.size());
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        return byteBuffer;
    }

    @Override
    public void run() {
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        MappedByteBuffer byteBuffer;

        do {
            byteBuffer = getMappedBuffer();
        } while (isRun && byteBuffer == null);

        while (isRun) {
            bitmap.copyPixelsFromBuffer(byteBuffer);
            byteBuffer.rewind();
            if (bitmap == null)
                continue;
            try {
                Canvas c = holder.lockCanvas();
                if (c != null) {
                    c.drawBitmap(bitmap, 0, 0, null);
                    holder.unlockCanvasAndPost(c);
                    Log.d(TAG, "refresh screen");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                sleep(1000/60);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        Log.d("weston", "thread out");
    }
}