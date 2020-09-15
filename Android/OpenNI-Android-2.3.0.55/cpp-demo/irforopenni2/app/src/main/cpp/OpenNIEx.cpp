//
// Created by xlj on 17-3-6.
//



#include "OpenNIEx.h"


typedef struct RGBA{
    uint8_t r;
    uint8_t g;
    uint8_t b;
    uint8_t a;
}RGBA;

OpenNIEx::OpenNIEx(){

    m_histogram = new uint32_t[HISTSIZE];
    mWidth = 1280;
    mHeight = 800;
}

OpenNIEx::~OpenNIEx(){

    delete m_histogram;
}

int OpenNIEx::init(){
    OpenNI::setLogAndroidOutput(true);
    OpenNI::setLogMinSeverity(0);

    Status rc = OpenNI::initialize();
    if (rc != STATUS_OK)
    {
        LOGE("Initialize failed\n%s\n", OpenNI::getExtendedError());
        return -1;
    }


    return 0;
}

int OpenNIEx::enumerateDevices(int vid, int pid){
    Array<DeviceInfo>  array;
    OpenNI::enumerateDevices(&array);
    if(array.getSize() <= 0){
        LOGE("findn't  OpenNI  device");
        return -1;
    }
    int size = array.getSize();
    const char* uri = NULL;
    for (int i = 0; i < size; ++i) {
        if(array[i].getUsbProductId() == pid && array[i].getUsbVendorId() ==  vid){
            uri = array[i].getUri();
            break;
        }
    }

    if(uri == NULL){
        LOGE("%d : %d  is Not OpenNI Device", vid, pid);
        return -1;
    }


    if(mDevice.open(uri) != STATUS_OK){
        LOGE("open %s failed", uri);
        return -1;
    }


    Status rc;
    if (mDevice.getSensorInfo(SENSOR_IR) != NULL)
    {
        rc = mIRStream.create(mDevice, SENSOR_IR);
        if (rc != STATUS_OK)
        {
            LOGE("Couldn't create IR stream\n%s\n", OpenNI::getExtendedError());
            return -1;
        }
    } else{
        LOGE("Get IR Sensor failed");
        return -1;
    }

    return 0;
}

int OpenNIEx::waitAndUpdate(){
    VideoStream* pStream = &mIRStream;
    int changedStreamDummy;
    Status rc = OpenNI::waitForAnyStream(&pStream, 1, &changedStreamDummy, READ_WAIT_TIMEOUT);
    if (rc != STATUS_OK)
    {
        LOGE("Wait failed! (timeout is %d ms)\n%s\n", READ_WAIT_TIMEOUT, OpenNI::getExtendedError());
        return -1;
    }

    return 0;
}

int OpenNIEx::open(int vid, int pid){

    if(init() != STATUS_OK){
        return -1;
    }


    if(enumerateDevices(vid, pid) < 0){
        return -1;
    }

    
    VideoMode mode = mIRStream.getVideoMode();
    mode.setResolution(mWidth, mHeight);
    mode.setPixelFormat(PIXEL_FORMAT_RGB888);
    mIRStream.setVideoMode(mode);

    Status rc = mIRStream.start();
    if (rc != STATUS_OK)
    {
        LOGE("Couldn't start the IR stream\n%s\n", OpenNI::getExtendedError());
        return -1;
    }


    return 0;
}

void OpenNIEx::close(){

    mIRStream.stop();
    mIRStream.destroy();
    mDevice.close();
    OpenNI::shutdown();

    return ;
}

int OpenNIEx::ConventToRGBA(uint8_t * texture, int textureWidth, int textureHeight){

   Status rc = mIRStream.readFrame(&mFrame);
    if (rc != STATUS_OK)
    {
        LOGE("Read failed!\n%s\n", OpenNI::getExtendedError());
        return -1;
    }

//    memcpy(texture, mFrame.getData(), mFrame.getDataSize());
    const uint8_t* pFrameData = (const uint8_t*)mFrame.getData();
    for (int y = 0; y < mFrame.getHeight(); ++y)
    {
        uint8_t * pTexture = texture + ((mFrame.getCropOriginY() + y) * textureWidth + mFrame.getCropOriginX()) * 4;
        const RGB888Pixel* pData = (const RGB888Pixel*)(pFrameData + y * mFrame.getStrideInBytes());
        for (int x = 0; x < mFrame.getWidth(); ++x, ++pData, pTexture += 4)
        {
            pTexture[0] = pData->r;
            pTexture[1] = pData->g;
            pTexture[2] = pData->b;
            pTexture[3] = 255;
        }
    }


    return 0;
}
