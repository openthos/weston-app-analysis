package com.dxmtb.westonapp;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

import com.dxmtb.westonapp.WestonProto.InputEventProto;

public class MainActivity extends Activity {
    final public static String TMP_DIR_MESSAGE = "TMP_DIR_MESSAGE";
    final public static String WIDTH_MESSAGE = "WIDTH_MESSAGE";
    final public static String HEIGHT_MESSAGE = "HEIGHT_MESSAGE";

    private static final int BTN_LEFT = 0x110;
    private static final int BTN_RIGHT = 0x111;

    final private String TAG = "MainActivity";

    private WestonView westonView;
    private SocketThread socketThread;
    private BlockingQueue<InputEventProto> eventQueue;
    private int buttonState = 0;
    final private boolean HAS_INPUT = true;

    private String imagePath = "image.bin";
    private String socketPath = "/data/data/com.dxmtb.westonapp/files/weston_socket";

    private int width = 800, height = 600;

    private void CheckPermissions() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                this.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        } else {
            Log.d("weston", "WRITE_EXTERNAL_STORAGE granted");
        }

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                this.checkSelfPermission(Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED){
            requestPermissions(new String[]{Manifest.permission.INTERNET}, 2);
        } else {
            Log.d("weston", "INTERNET granted");
        }
    }

    void launchWestonIfNeeded(Intent intent) {
        String tmpDir = intent.getStringExtra(TMP_DIR_MESSAGE);
        String height = intent.getStringExtra(HEIGHT_MESSAGE);
        String width = intent.getStringExtra(WIDTH_MESSAGE);
        if (tmpDir == null || height == null || width == null) {
            Log.w(TAG, "Didn't find enough information to launch weston. Intent: " + intent);
            return;
        }

        this.width = Integer.parseInt(width);
        this.height = Integer.parseInt(height);

        String filesPath = getFilesDir().getAbsolutePath();
        this.imagePath = filesPath + tmpDir + "/image.bin";
        this.socketPath = filesPath + tmpDir + "/weston_socket";
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        CheckPermissions();

        Intent intent = getIntent();
        launchWestonIfNeeded(intent);

        eventQueue = new LinkedBlockingDeque();
        socketThread = new SocketThread(eventQueue, socketPath);
        if (HAS_INPUT)
            socketThread.start();

        westonView = new WestonView(this, imagePath, width, height);
        setContentView(westonView);

        Log.d(TAG, "onCreate done");
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        socketThread.stopRun();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        Log.d(TAG, "dispatchKeyEvent: " + event);
        InputEventProto eventProto = new InputEventProto();
        eventProto.type = InputEventProto.KeyEventType;
        eventProto.time = event.getEventTime();
        eventProto.keyEvent = new WestonProto.KeyEvent();
        eventProto.keyEvent.key = event.getScanCode();

        switch (event.getAction()) {
            case KeyEvent.ACTION_DOWN:
            case KeyEvent.ACTION_UP:
                eventProto.keyEvent.actionType = event.getAction();
                break;
            default:
                return super.dispatchKeyEvent(event);
        }

        try {
            eventQueue.put(eventProto);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return true;
    }

    @Override
    public boolean dispatchGenericMotionEvent(MotionEvent event) {
        Log.d(TAG, "dispatchGenericMotionEvent: " + event);

        int[] location = new int[2] ;
        westonView.getLocationOnScreen(location);

        InputEventProto eventProto = new InputEventProto();
        eventProto.type = InputEventProto.MotionEventType;
        eventProto.time = event.getEventTime();
        eventProto.motionEvent = new WestonProto.MotionEvent();
        eventProto.motionEvent.x = Math.round(event.getX() - location[0]);
        eventProto.motionEvent.y = Math.round(event.getY() - location[1]);

        switch (event.getAction()) {
            case MotionEvent.ACTION_HOVER_MOVE:
            //case MotionEvent.ACTION_BUTTON_PRESS:
            //case MotionEvent.ACTION_BUTTON_RELEASE:
                eventProto.motionEvent.actionType = event.getAction();
                break;
            case MotionEvent.ACTION_SCROLL:
                eventProto.motionEvent.actionType = event.getAction();
                eventProto.motionEvent.axis =
                        -event.getAxisValue(MotionEvent.AXIS_VSCROLL);
                break;
            default:
                return false;
        }

        try {
            eventQueue.put(eventProto);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return super.dispatchGenericMotionEvent(event);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        Log.d(TAG, "dispatchTouchEvent: " + event);

        int[] location = new int[2] ;
        westonView.getLocationOnScreen(location);

        InputEventProto eventProto = new InputEventProto();
        eventProto.type = InputEventProto.MotionEventType;
        eventProto.time = event.getEventTime();
        eventProto.motionEvent = new WestonProto.MotionEvent();
        eventProto.motionEvent.x = Math.round(event.getX() - location[0]);
        eventProto.motionEvent.y = Math.round(event.getY() - location[1]);

        switch (event.getAction()) {
            case MotionEvent.ACTION_HOVER_MOVE:
            case MotionEvent.ACTION_BUTTON_PRESS:
            case MotionEvent.ACTION_BUTTON_RELEASE:
                eventProto.motionEvent.actionType = event.getAction();
                break;
            case MotionEvent.ACTION_DOWN:
                eventProto.motionEvent.actionType = MotionEvent.ACTION_BUTTON_PRESS;
                if (event.getButtonState() == MotionEvent.BUTTON_SECONDARY) {
                    buttonState = BTN_RIGHT;
                } else {
                    if (event.getButtonState() != MotionEvent.BUTTON_PRIMARY)
                        // still regard as left button
                        Log.d(TAG, "unknown button state " + event.getButtonState());
                    buttonState = BTN_LEFT;
                }
                eventProto.motionEvent.button = buttonState;
                break;
            case MotionEvent.ACTION_UP:
                eventProto.motionEvent.actionType = MotionEvent.ACTION_BUTTON_RELEASE;
                eventProto.motionEvent.button = buttonState;
                break;
            case MotionEvent.ACTION_MOVE:
                eventProto.motionEvent.actionType = MotionEvent.ACTION_HOVER_MOVE;
                break;
            case MotionEvent.ACTION_SCROLL:
                eventProto.motionEvent.actionType = event.getAction();
                eventProto.motionEvent.axis =
                        -event.getAxisValue(MotionEvent.AXIS_VSCROLL);
                break;
            default:
                return false;
        }

        try {
            eventQueue.put(eventProto);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return super.dispatchTouchEvent(event);
    }

}