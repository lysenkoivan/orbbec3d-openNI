package com.orbbec.NativeNI;

import java.nio.ByteBuffer;

/**
 * Created by xlj on 17-3-2.
 */

public class IRUtils {

    static {
        System.loadLibrary("IRUtils");
    }

    public  native static int RGB888TORGBA(ByteBuffer src, ByteBuffer dst, int w, int h, int strideInBytes);
    public  native static int DeviceInit(long handle);

    public native static String getSerialNumber();
    public native static String getDeviceType();

    public  native static int setGain(int value);
    public  native static int setExposure(short value);

    public  native static int getGain();
    public  native static int getExposure();

    public  native static int enableLaser(int enable);
    public  native static int enableLDP(int enable);

    public native static int enableIrFlood(int enable);
}
