package com.example.acc_gyo_mic;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.KeyguardManager;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Debug;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;
import android.widget.Toast;

/**
 * @ClassName: CommunicationActivity
 * @Description: Bluetoothﾁｬｽﾓﾍｨﾑｶﾀ�
 * @author Davidﾐ｡ﾋｶ
 * @date 2014ﾄ�2ﾔﾂ14ﾈﾕ ﾏﾂﾎ�4:59:29
 * 
 */
public class DeviceControlActivity extends Activity  {

	private final static String TAG = DeviceControlActivity.class
			.getSimpleName();

	private Bundle data;
	private String[] bLEDevAddress=new String[20];
	private Handler mHandler1;
	private int handle_time=0;
	private int flag;
	public boolean connect;
	public boolean connect1;
	public boolean connect2;
	private String data_savepath;
	public ArrayList<BluetoothGattCharacteristic> characteristics;
	public ArrayList<BluetoothGattCharacteristic> characteristics1;
	public ArrayList<BluetoothGattCharacteristic> characteristics2;
	public BluetoothGattCharacteristic mNotifyCharacteristic;
	BluetoothGattService gattServices;
	public String Data_Save_Path;
	public String Information_SavePath;
	File targetFile_tremor= null ;
	File targetFile_heart= null;
	BufferedWriter out_tremor = null;
	BufferedWriter out_heart = null;
	BufferedWriter out_motive = null;
	BufferedWriter out_voice = null;

	public boolean con_lock_motive=false,con_lock_heart=false,con_lock_voice=false;
	private Vibrator vibrator=null;
	private File input_file ;
	private File input_ACC;
	private File input_GYO;
	TextView tv_devName, tv_receiveData;
	TextView tv_devName1, tv_receiveData1;
	TextView tv_devName2, data_view,tv_receiveData2;
	ScrollView data_Scroll,data_Scroll1,data_Scroll2;
	EditText et_writeContent;
	Button btn_sendMsg;
	//record audio, beginning
	private int audioSource = MediaRecorder.AudioSource.MIC;
	// 设置音频采样率，44100是目前的标准，但是某些设备仍然支持22050，16000，11025
	private static int sampleRateInHz = 8000;
	// 设置音频的录制的声道CHANNEL_IN_STEREO为双声道，CHANNEL_CONFIGURATION_MONO为单声道
	@SuppressWarnings("deprecation")
	//private static int channelConfig = AudioFormat.CHANNEL_IN_STEREO ;
	private int channelConfig =AudioFormat.CHANNEL_CONFIGURATION_STEREO;;
	// 音频数据格式:PCM 16位每个样本。保证设备支持。PCM 8位每个样本。不一定能得到设备支持。
	private static int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
	// 缓冲区字节大小
	private int bufferSizeInBytes = 0;
	private Button Start;
	private Button Stop;
	private AudioRecord audioRecord;
	private boolean isRecord = false;// 设置正在录制的状态
	//AudioName裸音频数据文件
	private static final String AudioName = "/sdcard/love.raw";
	//NewAudioName可播放的音频文件
	private static final String NewAudioName = "/sdcard/new.wav";
	private static boolean mark=false;
	Thread write_file=new Thread(new AudioRecordThread());
	private Thread threadRecord;
	private static final String AUDIO_RECORDER_FOLDER = "audioRecorder";
	private static final String AUDIO_RECORDER_TEMP_FILE = "record_temp.raw";
	private static final String AUDIO_RECORDER_FILE = "session.wav";
	private static final int RECORDER_BPP = 16;
	FileOutputStream fos_ACC = null;
	FileOutputStream fos_GYO = null;
	FileOutputStream fos = null;
	Intent service;
	//record audio, end
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.ble_communication);
		Intent intent=this.getIntent();
		Data_Save_Path=intent.getStringExtra("Data_Path");
		Information_SavePath=intent.getStringExtra("Information_Path");
		vibrator = (Vibrator) this.getSystemService(VIBRATOR_SERVICE);
		//mark=intent.getBooleanExtra("audio_mark", false);
		//creatAudioRecord();

		try {
			init();
			startRecord_vn2();
			service = new Intent(this, Data_Service.class); 
			service.putExtra("Data_Save_Path", Data_Save_Path);
	        startService(service);  
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		Log.e(TAG, "in onCreate");
	}
	class AudioRecordThread implements Runnable {
		@Override
		public void run() {
			//writeDateTOFile();//往文件中写入裸数据,20151121,可以先在手机中保存raw数据，然后再程序退出的情况下将数据封装成wav
			//copyWaveFile(AudioName, NewAudioName);//给裸数据加上头文件
			//new MyAsyncTask().doInBackground();
			writeAudioDataToFile_vn2();

		}
	}
	private void startRecord_vn2()
	{

		bufferSizeInBytes = AudioRecord.getMinBufferSize(sampleRateInHz,
				channelConfig, audioFormat);

		audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
				sampleRateInHz, channelConfig, audioFormat, bufferSizeInBytes);
		audioRecord.startRecording();

		isRecord = true;
		write_file.start();//show data
		//new MyAsyncTask().doInBackground();

	}
	

	private void writeAudioDataToFile_vn2()
	{
		byte[] bs = new byte[bufferSizeInBytes];

		try {
			if (input_file.exists()) {
				input_file.delete();
			}
			fos = new FileOutputStream(input_file);// 建立一个可存取字节的文件
		} catch (Exception e) {
			e.printStackTrace();
		}

		int line = 0;
		if(fos!=null)
		{
			while(isRecord)
			{
				line = audioRecord.read(bs, 0, bufferSizeInBytes);
				/////////////////////////////////////////////
				//int r = audioRecord.read(buffer, 0, bufferSizeInBytes);  
				long v = 0;  
				// 将 buffer 内容取出，进行平方和运算  
				for (int i = 0; i < bs.length-2; i=i+2) { 
					byte[] a=new byte[4];
					a[0]=bs[i];
					a[1]=bs[i+1];
					a[2]=bs[i+2];
					a[3]=bs[i+3];
					int value=bytesToShort(a, 0);
					v += value;  
				}  
				// 平方和除以数据总长度，得到音量大小。  
				double mean = (v*(-1)) / (double) (line/2);  
				final double volume = 10 * Math.log10(mean);  
				Log.d(TAG, "分贝值:" + volume);  
				////////////////////////////////////////////
				if(line!=AudioRecord.ERROR_INVALID_OPERATION)
				{
					try 
					{
						fos.write(bs);

						tv_receiveData2.post(new Runnable(){
							public void run(){
								tv_receiveData2.setText("分贝值:" + volume);
							}

						});
					} 
					catch (IOException e) 
					{
						e.printStackTrace();
					}
				}
			}
		}

	}

	public static int bytesToInt(byte[] ary, int offset) {  
		int value;    
		value = (int) ((ary[offset]&0xFF)   
				| ((ary[offset+1]<<8) & 0xFF00)  
				| ((ary[offset+2]<<16)& 0xFF0000)   
				| ((ary[offset+3]<<24) & 0xFF000000));  
		return value;
	}  

	public static short bytesToShort(byte[] ary, int offset) {  
		short value;    
		value = (short) ((ary[offset]&0xFF)   
				| ((ary[offset+1]<<8) & 0xFF00)  
				| ((ary[offset+2]<<16)& 0xFF0000)   
				| ((ary[offset+3]<<24) & 0xFF000000));  
		return value;
	}  


	private void alarm(){
		//根据指定的模式进行震动
		//第一个参数：该数组中第一个元素是等待多长的时间才启动震动，
		//之后将会是开启和关闭震动的持续时间，单位为毫秒
		//第二个参数：重复震动时在pattern中的索引，如果设置为-1则表示不重复震动
		vibrator.vibrate(new long[]{400,800,1200,1600}, 2);

	}

	private void alarm_vn2(){
		//根据指定的模式进行震动
		//第一个参数：该数组中第一个元素是等待多长的时间才启动震动，
		//之后将会是开启和关闭震动的持续时间，单位为毫秒
		//第二个参数：重复震动时在pattern中的索引，如果设置为-1则表示不重复震动

		long [] pattern = {40,1000,40,1000}; // 停止 开启 停止 开启
		vibrator.vibrate(pattern,2); //重复两次上面的pattern 如果只想震动一次，index设为-1

	}

	private void init() throws Exception {

		mHandler1=new Handler();
		tv_devName = (TextView) findViewById(R.id.tv_devName);
		//data_Scroll=(ScrollView) findViewById(R.id.scrollView1);
		tv_receiveData = (TextView) findViewById(R.id.textView1);
		tv_receiveData.setText("");

		tv_devName1 = (TextView) findViewById(R.id.tv_devName1);
		//data_Scroll1=(ScrollView) findViewById(R.id.scrollView2);
		tv_receiveData1 = (TextView) findViewById(R.id.textView2);
		tv_receiveData1.setText("");

		/////////////////////////////////////////////////////////////////////////
		//here we add the second scroll to show data from third BLE
		tv_devName2 = (TextView) findViewById(R.id.tv_devName2);
		//data_Scroll2=(ScrollView) findViewById(R.id.scrollView3);
		tv_receiveData2 = (TextView) findViewById(R.id.textView3);
		tv_receiveData2.setText("");
		
		 try {
				File sdCardDir = Environment.getExternalStorageDirectory();
				input_file = new File(sdCardDir.getCanonicalPath() +Data_Save_Path+'/'+"Voice.raw");
	
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
	     
	}

	protected void onStart(){
		super.onStart();
		Log.e(TAG, "in onStart");

	}

	@Override
	protected void onResume() {//remove the code into onStart method
		super.onResume();
		IntentFilter magFilter = new IntentFilter();  
		magFilter.addAction("com.dm.magReceiver"); 
	
		registerReceiver(magReceiver, magFilter);  
		IntentFilter accFilter = new IntentFilter();  
        accFilter.addAction("com.dm.accReceiver");  
        registerReceiver(accReceiver, accFilter);  

	};

	@Override
	protected void onPause() {
		super.onPause();
		Log.e(TAG, "in onPause");
		//sensorManager.unregisterListener(sensorEventListener);    
	}

	BroadcastReceiver magReceiver = new BroadcastReceiver() {  
		@Override  
		public void onReceive(Context context, Intent intent) {  
			Bundle bundle = intent.getExtras();// 获得 Bundle  
			float Yaw = bundle.getFloat("Yaw", 0);  
            float Pitch = bundle.getFloat("Pitch", 0);  
            float Roll = bundle.getFloat("Roll", 0);  
            tv_receiveData.setText("gyroscope: " + Yaw + ", " + Pitch + ", " + Roll);  
               
             Log.i("-----------magReceiver---------------",   
                     String.valueOf(Yaw) + "|" +  
                     String.valueOf(Pitch) + "|" +  
                     String.valueOf(Roll) );   
		}  
	};  

	BroadcastReceiver accReceiver = new BroadcastReceiver() {  
		@Override  
		public void onReceive(Context context, Intent intent) {  
			Bundle bundle = intent.getExtras();// 获得 Bundle  
			double xAcceleration = bundle.getDouble("xAcceleration", 0);  
			double  yAcceleration = bundle.getDouble("yAcceleration", 0);  
			double zAcceleration = bundle.getDouble("zAcceleration", 0);  
			double currentAcceleration = bundle.getDouble("currentAcceleration", 0);  
			double maxAcceleration = bundle.getDouble("maxAcceleration", 0);  
			 tv_receiveData1.setText("Orientation: " + xAcceleration + ", " + yAcceleration + ", " + zAcceleration);  
             Log.i("-----------accReceiver---------------", String.valueOf(xAcceleration) + "|" +  
                     String.valueOf(xAcceleration) + "|" +  
                     String.valueOf(yAcceleration) + "|" +  
                     String.valueOf(zAcceleration) + "|" +  
                     String.valueOf(currentAcceleration) + "|" +  
                     String.valueOf(maxAcceleration) );   

		}
	};  

	@Override
	protected void onDestroy() {
		super.onDestroy();
		Log.e(TAG, "in onDestory");
		audioRecord.stop();
		isRecord=false;
		/*try {
			 fos.flush();
			fos.close();
			
		} catch (IOException e) {
			
			e.printStackTrace();
		}*/
		unregisterReceiver(accReceiver);
		unregisterReceiver(magReceiver);
		stopService(service);


	}

}
