package com.orbbec.NativeNI;

import java.nio.ByteBuffer;

/**
 * Created by xlj on 17-3-2.
 */

public class DepthUtils {

    static {
        System.loadLibrary("DepthUtils");
    }

    public  native static int ConvertTORGBA(ByteBuffer src, ByteBuffer dst, int w, int h, int strideInBytes);
}
