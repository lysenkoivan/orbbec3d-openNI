package com.orbbec.obDepth2;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.orbbec.utils.GlobalDef;
import com.orbbec.view.OpenGLView;

import org.openni.*;
import org.openni.DeviceInfo;
import org.openni.OpenNI;
import org.openni.PixelFormat;
import org.openni.SensorType;
import org.openni.VideoFrameRef;
import org.openni.VideoMode;
import org.openni.VideoStream;
import org.openni.android.OpenNIHelper;

import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.TimeoutException;

public class MainActivity extends AppCompatActivity implements OpenNIHelper.DeviceOpenListener, ActivityCompat.OnRequestPermissionsResultCallback {

    String TAG = "obDepth";

    boolean mInit_Ok = false;
    private Device mDevice;
    private Thread m_thread;
    private OpenNIHelper mOpenNIHelper;
    private VideoStream mStream;

    private List<VideoMode> mVideoModes;
    private HomeKeyListener mHomeListener;

    private OpenGLView mGLView;
    private Context mContext;
    private boolean mExit = false;

    private int mWidth = GlobalDef.RESOLUTION_X;
    private int mHeight = GlobalDef.RESOLUTION_Y;
    private final int DEPTH_NEED_PERMISSION = 33;


    private Object m_sync = new Object();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);
        mContext = this;
        mGLView = (OpenGLView) findViewById(R.id.glView);

        mOpenNIHelper = new OpenNIHelper(this);
        mOpenNIHelper.requestDeviceOpen(this);
    }

    @Override
    public void onDeviceOpened(UsbDevice device) {
        init(device);

        mStream = VideoStream.create(mDevice, SensorType.DEPTH);
        mVideoModes = mStream.getSensorInfo().getSupportedVideoModes();

        for (VideoMode mode : mVideoModes) {
            int X = mode.getResolutionX();
            int Y = mode.getResolutionY();
            int fps = mode.getFps();

            Log.d(TAG, " support resolution: " + X + " x " + Y + " fps: " + fps + ", (" + mode.getPixelFormat() + ")");
            if (X == mWidth && Y == mHeight && mode.getPixelFormat() == PixelFormat.DEPTH_1_MM) {
                mStream.setVideoMode(mode);
                Log.v(TAG, " setmode");
            }

        }

        startThread();
    }

    private void init(UsbDevice device) {
        OpenNI.setLogAndroidOutput(true);
        OpenNI.setLogMinSeverity(0);
        OpenNI.initialize();

        List<DeviceInfo> opennilist = OpenNI.enumerateDevices();
        if (opennilist.size() <= 0) {
            Toast.makeText(this, " openni enumerateDevices 0 devices", Toast.LENGTH_LONG).show();
            return;
        }

        mDevice = null;
        //Find device ID
        for (int i = 0; i < opennilist.size(); i++) {
            if (opennilist.get(i).getUsbProductId() == device.getProductId()) {
                mDevice = Device.open();
                break;
            }
        }

        if (mDevice == null) {
            Toast.makeText(this, " openni open devices failed: " + device.getDeviceName(), Toast.LENGTH_LONG).show();
            return;
        }
    }

    @Override
    public void onDeviceOpenFailed(String msg) {
        showAlertAndExit("Open Device failed: " + msg);
    }

    void startThread() {
        mInit_Ok = true;
        m_thread = new Thread() {

            @Override
            public void run() {

                List<VideoStream> streams = new ArrayList<VideoStream>();

                streams.add(mStream);

                mStream.start();

                while (!mExit) {

                    try {
                        OpenNI.waitForAnyStream(streams, 2000);
                    } catch (TimeoutException e) {
                        e.printStackTrace();
                        continue;
                    }

                    synchronized (m_sync) {

                        mGLView.update(mStream);

                    }

                }
            }
        };

        m_thread.start();
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (mInit_Ok) {
            mExit = true;
            if (m_thread != null) {
                try {
                    m_thread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            long time1 = System.currentTimeMillis();
            if (mStream != null) {
                mStream.stop();
            }

            long time2 = System.currentTimeMillis();

            Log.e(TAG, "  stop delay time: " + (time2-time1));

            if (mDevice != null) {
                mDevice.close();
            }


            Log.i(TAG, "Activity exit!");
            System.exit(0);
        }
    }

    private void showAlertAndExit(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(message);
        builder.setNeutralButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        });
        builder.show();
    }


    /**
     * 注册Home键的监听
     */
    private void registerHomeListener() {
        mHomeListener = new HomeKeyListener(this);
        mHomeListener
                .setOnHomePressedListener(new HomeKeyListener.OnHomePressedListener() {

                    @Override
                    public void onHomePressed() {
                        // TODO 进行点击Home键的处理
                        finish();
                    }

                    @Override
                    public void onHomeLongPressed() {
                        // TODO 进行长按Home键的处理
                    }
                });
        mHomeListener.startWatch();
    }

    private void unRegisterHomeListener() {
        if (mHomeListener != null) {
            mHomeListener.stopWatch();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == DEPTH_NEED_PERMISSION) {


            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Permission Grant");
                Toast.makeText(mContext, "Permission Grant", Toast.LENGTH_SHORT).show();


            } else {
                // Permission Denied
                Log.d(TAG, "Permission Denied");
                Toast.makeText(mContext, "Permission Denied", Toast.LENGTH_SHORT).show();
            }

        }
    }
    
}
