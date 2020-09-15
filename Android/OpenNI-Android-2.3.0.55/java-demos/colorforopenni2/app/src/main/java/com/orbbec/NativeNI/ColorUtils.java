package com.orbbec.NativeNI;

import java.nio.ByteBuffer;

/**
 * Created by xlj on 17-3-2.
 */

public class ColorUtils {

    static {
        System.loadLibrary("ColorUtils");
    }

    public  native static int RGB888TORGBA(ByteBuffer src, ByteBuffer dst, int w, int h, int strideInBytes);
}
