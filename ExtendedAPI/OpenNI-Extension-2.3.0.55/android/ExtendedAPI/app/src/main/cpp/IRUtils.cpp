//
// Created by xlj on 17-3-2.
//

#include <jni.h>
#include <android/log.h>
#include "./Include/OpenNI.h"
#include "./Include/PS1080.h"

#define REGISTER_CLASS "com/orbbec/NativeNI/IRUtils"

#define LOG_TAG "IRUtils-Jni"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG,LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR,LOG_TAG, __VA_ARGS__)

typedef unsigned char uint8_t;
typedef struct {
    uint8_t r;
    uint8_t g;
    uint8_t b;
}RGB888Pixel;

openni::Device* g_Device = NULL;

int ConventFromIRToRGBA(uint8_t* src, uint8_t* dst,  int w, int h, int strideInBytes){
    for (int y = 0; y < h; ++y)
    {
        uint8_t* pTexture = dst +  (y*w*4);
        const RGB888Pixel* pData = (const RGB888Pixel*)(src + y * strideInBytes);
        for (int x = 0; x < w; ++x, ++pData, pTexture += 4)
        {
            pTexture[0] = pData->r;
            pTexture[1] = pData->g;
            pTexture[2] = pData->b;
            pTexture[3] = 255;
        }

    }

    return 0;
}

jint RGB888TORGBA(JNIEnv* env, jobject obj, jobject src, jobject dst, jint w, jint h, jint strideInBytes){

    if(src == nullptr || dst == nullptr){
        return -1;
    }
    uint8_t* srcBuf = (uint8_t*)env->GetDirectBufferAddress(src);

    uint8_t* dstBuf = (uint8_t*)env->GetDirectBufferAddress(dst);

    ConventFromIRToRGBA(srcBuf, dstBuf, w, h, strideInBytes);

    return 0;
}

jint DeviceInit(JNIEnv* env, jobject obj, jlong deviceHandle){
    g_Device = new openni::Device((OniDeviceHandle)deviceHandle);
    char serNumber[12];
    int dataSize = sizeof(serNumber);
    memset(serNumber, 0, dataSize);
    g_Device->getProperty(openni::OBEXTENSION_ID_SERIALNUMBER, (uint8_t *) &serNumber, &dataSize);
    LOGE("serialnumber %s", serNumber);

    char devname[20];
    dataSize = sizeof(devname);
    memset(devname, 0, dataSize);
    g_Device->getProperty(openni::OBEXTENSION_ID_DEVICETYPE, (uint8_t *) &devname, &dataSize);
    LOGE("devname %s", devname);

    return 0;
}

jstring getSerialNumber(JNIEnv* env, jobject obj) {
    char serNumber[12];
    int dataSize = sizeof(serNumber);
    memset(serNumber, 0, dataSize);
    g_Device->getProperty(openni::OBEXTENSION_ID_SERIALNUMBER, (uint8_t *) &serNumber, &dataSize);
    LOGE("serialnumber %s", serNumber);
    return (env)->NewStringUTF(serNumber);
}

jstring getDeviceType(JNIEnv* env, jobject obj) {
    char devname[20];
    int dataSize = sizeof(devname);
    memset(devname, 0, dataSize);
    g_Device->getProperty(openni::OBEXTENSION_ID_DEVICETYPE, (uint8_t *) &devname, &dataSize);
    LOGE("devname %s", devname);
    return (env)->NewStringUTF(devname);
}

jint setGain(JNIEnv* env, jobject obj, int value){
    int dataSize = 4;
    int gain= value;
    g_Device->setProperty(openni::OBEXTENSION_ID_IR_GAIN, (uint8_t *) &gain, dataSize);
    return 0;
}

jint setExposure(JNIEnv* env, jobject obj,  jshort value){
    int dataSize = 4;
    int exposure = value;
    g_Device->setProperty(openni::OBEXTENSION_ID_IR_EXP, (uint8_t *) &exposure, dataSize);
    return 0;
}

jint getGain(JNIEnv* env, jobject obj){
    int dataSize = 4;
    int value = 0;
    g_Device->getProperty(openni::OBEXTENSION_ID_IR_GAIN, (uint8_t *) &value, &dataSize);
    return value;
}

jint getExposure(JNIEnv* env, jobject obj){
    int dataSize = 4;
    int value = 0;
    g_Device->getProperty(openni::OBEXTENSION_ID_IR_EXP, (uint8_t *) &value, &dataSize);
    return value;
}

jint enableLaser(JNIEnv* env, jobject obj, jint enable){
    int dataSize = 4;
    int value = enable;
    g_Device->setProperty(openni::OBEXTENSION_ID_LASER_EN, (uint8_t *) &value, dataSize);
    return value;
}

jint enableLDP(JNIEnv* env, jobject obj, jint enable){
    int dataSize = 4;
    int value = enable;
    g_Device->setProperty(openni::OBEXTENSION_ID_LDP_EN, (uint8_t *) &value, dataSize);
    return value;
}

jint enableIrFlood(JNIEnv* env, jobject obj, jint enable){
    int value = enable;
    g_Device->setProperty(XN_MODULE_PROPERTY_IRFLOOD_STATE, value);
    return value;
}

JNINativeMethod jniMethods[] = {
        { "RGB888TORGBA",                      "(Ljava/nio/ByteBuffer;Ljava/nio/ByteBuffer;III)I",                      (void*)&RGB888TORGBA},
        { "DeviceInit",                      "(J)I",                      (void*)&DeviceInit},
        { "setGain",                      "(I)I",                      (void*)&setGain},
        { "setExposure",                      "(S)I",                      (void*)&setExposure},

        {"getSerialNumber",               "()Ljava/lang/String;",      (void*)&getSerialNumber},
        {"getDeviceType",                 "()Ljava/lang/String;",      (void*)&getDeviceType},

        { "getGain",                      "()I",                      (void*)&getGain},
        { "getExposure",                      "()I",                      (void*)&getExposure},

        { "enableLaser",                      "(I)I",                      (void*)&enableLaser},
        { "enableLDP",                      "(I)I",                      (void*)&enableLDP},
        { "enableIrFlood",                      "(I)I",                      (void*)&enableIrFlood},

};

jint JNI_OnLoad(JavaVM* vm, void* reserved)
{
    JNIEnv* env;
    jclass gCallbackClass = nullptr;

    if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) {
        return -1;
    }

    jclass clz     = env ->FindClass(REGISTER_CLASS);
    gCallbackClass = (jclass)env ->NewGlobalRef(clz);
    env ->RegisterNatives(clz, jniMethods, sizeof(jniMethods) / sizeof(JNINativeMethod));
    env ->DeleteLocalRef(clz);
    LOGD("IRUtils JNI_OnLoad");
    return JNI_VERSION_1_6;
}

void JNI_OnUnLoad(JavaVM* vm, void* reserved){
    if(g_Device != NULL){
        delete g_Device;
        g_Device = NULL;
    }
}