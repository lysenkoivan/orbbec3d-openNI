package com.orbbec.ExtendedAPI;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.orbbec.NativeNI.IRUtils;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeoutException;

public class IRActivity extends Activity implements OpenNIHelper.DeviceOpenListener, SeekBar.OnSeekBarChangeListener, CompoundButton.OnCheckedChangeListener {

	String TAG = "ExtendedAPI";

	boolean mInit_Ok = false;
	private Device mDevice;
	private Thread m_thread;
	private OpenNIHelper mOpenNIHelper;
	private VideoStream mStream;
	private VideoStream depthStream;

	private List<VideoMode> mVideoModes;
	private VideoFrameRef mLastFrame;
	private HomeKeyListener mHomeListener;
	private ByteBuffer mByteBuf;
	private OpenGLView mIRView;

	private boolean mExit = false;

	private int mWidth = GlobalDef.RES_COLOR_WIDTH;
	private int mHeight = GlobalDef.RES_COLOR_HEIGHT;

	boolean usbPermissonGrant = false;

	private SeekBar mExposureSeekBar, mGainSeekBar;
	private CheckBox mIRCheckBox, mLDPCheckBox, mIrFloodCheckBox;
	private TextView mExposText, mGainText;
	private TextView mSnText, mDevTypeText;

	private float m_gain = 8;
	private int m_exposure= 40;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		OpenNI.setLogAndroidOutput(true);
		OpenNI.setLogMinSeverity(0);
		OpenNI.initialize();

		setContentView(R.layout.main);
		mIRView = (OpenGLView)findViewById(R.id.irView);
		mIRCheckBox = (CheckBox) findViewById(R.id.checkIR);
		mLDPCheckBox = (CheckBox) findViewById(R.id.checkLDP);
		mIrFloodCheckBox = (CheckBox) findViewById(R.id.checkIRFLOOD);

		mOpenNIHelper = new OpenNIHelper(this);
		mOpenNIHelper.requestDeviceOpen(this);

	}


	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	public void onDeviceOpened(UsbDevice device) {

		List<DeviceInfo> devices = OpenNI.enumerateDevices();
		if (devices.isEmpty()) {
			showAlertAndExit("OpenNI enumerateDevices device 0");
		}else {

			mDevice = Device.open();

			depthStream = VideoStream.create(mDevice, SensorType.DEPTH);
			depthStream.start();

			mStream = VideoStream.create(mDevice, SensorType.IR);
			mVideoModes = mStream.getSensorInfo().getSupportedVideoModes();

			for (VideoMode mode : mVideoModes) {
				int X = mode.getResolutionX();
				int Y = mode.getResolutionY();
				int fps = mode.getFps();

				if (X == mWidth && Y == mHeight && mode.getPixelFormat() == PixelFormat.RGB888) {
					mStream.setVideoMode(mode);

				}
			}

			IRUtils.DeviceInit(mDevice.getHandle());
			mExposureSeekBar = (SeekBar) findViewById(R.id.expBar);
			mGainSeekBar = (SeekBar) findViewById(R.id.gainBar);
			mExposureSeekBar.setOnSeekBarChangeListener(this);
			mGainSeekBar.setOnSeekBarChangeListener(this);

			mSnText = (TextView) findViewById(R.id.snText);
			//mSnText.setText(IRUtils.getSerialNumber());
			mDevTypeText = (TextView) findViewById(R.id.devTypeText);
			mDevTypeText.setText(IRUtils.getDeviceType());
			mGainText = (TextView) findViewById(R.id.gainText);
			mExposText = (TextView) findViewById(R.id.ExpText);

			mIRCheckBox.setOnCheckedChangeListener(this);
			mLDPCheckBox.setOnCheckedChangeListener(this);
			mIrFloodCheckBox.setOnCheckedChangeListener(this);

			startThread();
		}
	}

    @Override
    public void onDeviceOpenFailed(String s) {
        //String pid = device.getProductId()+"";
        Toast.makeText(this, "usb device open failed", Toast.LENGTH_SHORT).show();
    }


	void startThread(){
		mInit_Ok = true;
		m_thread = new Thread(){

			@Override
			public void run() {

				List<VideoStream> streams = new ArrayList<VideoStream>();
				streams.add(mStream);
				mStream.start();

				while (!mExit) {
					VideoFrameRef frame = null;
					try {
						OpenNI.waitForAnyStream(streams, 2000);
					} catch (TimeoutException e) {
						e.printStackTrace();
						continue;
					}
					mIRView.update(mStream);
				}
			}
		};

		m_thread.start();
	}

	@Override
	protected void onStop() {
		super.onStop();

		if(mInit_Ok) {
			mExit = true;
			if (m_thread != null) {
				try {
					m_thread.join();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

			if (mStream != null) {
				mStream.stop();

			}

			if (mDevice != null) {
				mDevice.close();
			}

			mOpenNIHelper.shutdown();

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

	private HashMap<String, UsbDevice> getDevList()
	{
		UsbManager manager = (UsbManager)getSystemService(Context.USB_SERVICE);
		HashMap<String, UsbDevice> deviceList = manager.getDeviceList();
		Iterator<UsbDevice> iterator = deviceList.values().iterator();
		while (iterator.hasNext())
		{
			UsbDevice device = (UsbDevice)iterator.next();
			int vendorId = device.getVendorId();
			int productId = device.getProductId();

			if((vendorId == 0x1D27 && (productId == 0x05FC || productId != 0x0601)) ||
					(vendorId ==0x2BC5 && (productId >= 0x0401 && productId <= 0x04FF))
					){
				continue;
			}else {
				iterator.remove();
			}
		}

		return deviceList;
	}


	@Override
	public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
		switch (seekBar.getId()) {
			case R.id.expBar:
				mExposText.setText("Exposure: " + IRUtils.getExposure() );
				IRUtils.setExposure((short)i);
				break;
			case R.id.gainBar:
				mGainText.setText("Gain: " + IRUtils.getGain());
				IRUtils.setGain(i);
				break;
		}
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {

	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {

	}

	@Override
	public void onCheckedChanged(CompoundButton compoundButton, boolean b) {

		switch (compoundButton.getId()){
			case R.id.checkIR:
				enableLaser(b);
				break;
			case R.id.checkLDP:
				enableLDP(b);
				break;
			case R.id.checkIRFLOOD:
				enableIrFlood(b);
				break;
		}
	}

	private void enableLaser(boolean enable){
		if(enable){
			Toast.makeText(this, "enable IR", Toast.LENGTH_SHORT).show();
			IRUtils.enableLaser(1);
		}else{
			Toast.makeText(this, "disable IR", Toast.LENGTH_SHORT).show();
			IRUtils.enableLaser(0);
		}
	}

	private void enableLDP(boolean enable){
		if(enable){
			Toast.makeText(this, "enable LDP", Toast.LENGTH_SHORT).show();
			IRUtils.enableLDP(1);
		}else{
			Toast.makeText(this, "disable LDP", Toast.LENGTH_SHORT).show();
			IRUtils.enableLDP(0);
		}
	}

	private void enableIrFlood(boolean enable) {
		if (enable) {
			Toast.makeText(this, "enable ir flood", Toast.LENGTH_SHORT).show();
			IRUtils.enableIrFlood(1);
		} else {
			Toast.makeText(this, "enable ir flood", Toast.LENGTH_SHORT).show();
			IRUtils.enableIrFlood(0);
		}
	}
}
