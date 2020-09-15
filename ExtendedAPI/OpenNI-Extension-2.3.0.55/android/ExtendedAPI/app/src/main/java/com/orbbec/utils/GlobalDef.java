package com.orbbec.utils;

/**
 * Created by zlh on 2015/8/2.
 */
public class GlobalDef {

    public static final boolean FPS_ON = false;
    public static String PACKAGE_NAME;

    // Resolution
    public static final int RES_DISPLAY_WIDTH = 640;//彩色图显示宽度，在非UVC下设置为与RES_COLOR_WIDTH大小一致，否则与RES_UVC_WIDTH一致
    public static final int RES_DISPLAY_HEIGHT = 480;//彩色图显示高度，在非UVC下设置为与RES_COLOR_HEIGHT大小一致，否则与RES_UVC_HEIGHT一致
    public static final int RES_COLOR_WIDTH = 640;//普通摄像头分辨率
    public static final int RES_COLOR_HEIGHT = 400;
    public static final int RES_DEPTH_WIDTH = 640;//深度图分辨率
    public static final int RES_DEPTH_HEIGHT = 480;
    /*public static final int RES_DEPTH_WIDTH = 320;//深度图分辨率
    public static final int RES_DEPTH_HEIGHT = 240;*/
}
