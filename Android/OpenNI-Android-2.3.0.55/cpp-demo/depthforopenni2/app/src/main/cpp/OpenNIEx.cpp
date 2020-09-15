//
// Created by xlj on 17-3-6.
//



#include "OpenNIEx.h"



OpenNIEx::OpenNIEx(){

    m_histogram = new uint32_t[HISTSIZE];
    mWidth = 640;
    mHeight = 480;
}

OpenNIEx::~OpenNIEx(){

    delete m_histogram;
}

int OpenNIEx::init(){

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
    if (mDevice.getSensorInfo(SENSOR_DEPTH) != NULL)
    {
        rc = mDepth.create(mDevice, SENSOR_DEPTH);
        if (rc != STATUS_OK)
        {
            LOGE("Couldn't create depth stream\n%s\n", OpenNI::getExtendedError());
            return -1;
        }
    } else{
        LOGE("Get Depth Sensor failed");
        return -1;
    }

    return 0;
}

int OpenNIEx::waitAndUpdate(){
    VideoStream* pStream = &mDepth;
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


    VideoMode mode = mDepth.getVideoMode();
    mode.setResolution(mWidth, mHeight);
    mode.setPixelFormat(PIXEL_FORMAT_DEPTH_1_MM);
    mDepth.setVideoMode(mode);

    Status rc = mDepth.start();
    if (rc != STATUS_OK)
    {
        LOGE("Couldn't start the depth stream\n%s\n", OpenNI::getExtendedError());
        return -1;
    }


    return 0;
}

void OpenNIEx::close(){

    mDepth.stop();
    mDepth.destroy();
    mDevice.close();
    OpenNI::shutdown();

    return ;
}

int OpenNIEx::ConventToRGBA(uint8_t * texture, int textureWidth, int textureHeight){

   Status rc = mDepth.readFrame(&mFrame);
    if (rc != STATUS_OK)
    {
        LOGE("Read failed!\n%s\n", OpenNI::getExtendedError());
        return -1;
    }


    uint8_t* pFrameData = (uint8_t*)mFrame.getData();

    calcDepthHist(mFrame);

    for (int y = 0; y < mFrame.getHeight(); ++y)
    {
        uint8_t* pTexture = texture + ((mFrame.getCropOriginY() + y) * textureWidth + mFrame.getCropOriginX()) * 4;
        const OniDepthPixel* pDepth = (const OniDepthPixel*)(pFrameData + y * mFrame.getStrideInBytes());
        for (int x = 0; x < mFrame.getWidth(); ++x, ++pDepth, pTexture += 4)
        {
            int val = m_histogram[*pDepth];
            pTexture[0] = val;
            pTexture[1] = val;
            pTexture[2] = 0x0;
            pTexture[3] = 255;
        }
    }


    return 0;
}

void OpenNIEx::calcDepthHist(VideoFrameRef& frame)
{
    unsigned int value = 0;
    unsigned int index = 0;
    unsigned int x = 0;
    unsigned int y = 0;
    unsigned int numberOfPoints = 0;
    const OniDepthPixel* pDepth = (const OniDepthPixel*)frame.getData();

    if (m_histogram == NULL)
    {
        m_histogram = (uint32_t *)malloc(HISTSIZE * sizeof(int));
    }

    // Calculate the accumulative histogram
    memset(m_histogram, 0, HISTSIZE*sizeof(int));

    for (int i = 0; i < frame.getDataSize() / sizeof(DepthPixel); ++i, ++pDepth)
    {
        value = *pDepth;

        if (value != 0)
        {
            m_histogram[value]++;
            numberOfPoints++;
        }
    }

    for (index = 1; index < HISTSIZE; index++)
    {
        m_histogram[index] += m_histogram[index - 1];
    }

    if (numberOfPoints != 0)
    {
        for (index = 1; index < HISTSIZE; index++)
        {
            m_histogram[index] = (unsigned int)(256 * (1.0f - ((float)m_histogram[index] / numberOfPoints)));
        }
    }
}