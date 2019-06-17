package com.example.acc_gyo_mic;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.example.acc_gyo_mic.DeviceControlActivity.AudioRecordThread;

import android.annotation.SuppressLint;
import android.app.Service;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Vibrator;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Android Service ﾊｾﾀ�
 * 
 * @author dev
 * 
 */
public class Data_Service extends Service {
	 // 声明加速度传感器对象  
    private SensorManager sm = null; // 获取SensorManager对象，通过它可以获得距离，加速度等传感器对象  
    private Sensor accelerationSensor = null; // 加速度传感器  
    private Sensor gyosensor = null; // 磁力传感器  
    // ******************加速度传感器初始化变量*********************************************//  
    double gravity[] = new double[3];// 代表3个方向的重力加速度  
    public double xAcceleration = 0;// 代表3个方向的真正加速度  
    public double yAcceleration = 0;  
    public double zAcceleration = 0;  
    public double currentAcceleration = 0; // 当前的合加速度  
    public double maxAcceleration = 0; // 最大加速度  
    // 接下来定义的数组是为了对加速度传感器采集的加速度加以计算和转化而定义的，转化的目的是为了使数据看上去更符合我们平时的习惯  
    float[] magneticValues = new float[3];  
    float[] accelerationValues = new float[3];  
    float[] values = new float[3];  
    float[] rotate = new float[9];  
    // 初始化的三个方位角的值  
    public  float Yaw = 0;  
    public  float Pitch = 0; // values[1]  
    public float Roll = 0;  
      
    final static int CMD_STOP = 0;  
    final static int CMD_UPDATAE = 1;  
    
    private File input_file ;
	private File input_ACC;
	private File input_GYO;
	
	public String Data_Save_Path;
	
	FileOutputStream fos_ACC = null;
	FileOutputStream fos_GYO = null;
	FileOutputStream fos = null;
	
	MyAsyncTask write_task=new MyAsyncTask();
	
	private boolean isRecord=false;
      
      
    public void onCreate() {  
        super.onCreate();  
  
        /** 
         * 设置加速度传感器 
         */  
       
		
        sm = (SensorManager) this.getSystemService(SENSOR_SERVICE);  
        accelerationSensor = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER); 
        gyosensor=sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
       
        sm.registerListener(accelerationListener, accelerationSensor,  
                SensorManager.SENSOR_DELAY_GAME);  
        sm.registerListener(gyoneticListener, gyosensor,  
                SensorManager.SENSOR_DELAY_GAME);  

    }  
      
    public void onStart(Intent intent, int startId) {  
        super.onStart(intent, startId);  
        Log.i("-----------SensorService---------------","服务启动" );   
        Data_Save_Path=intent.getStringExtra("Data_Save_Path");
        int a=0;
        isRecord=true;
        try {
			File sdCardDir = Environment.getExternalStorageDirectory();
			input_ACC= new File(sdCardDir.getCanonicalPath() +Data_Save_Path+'/'+"ACC.txt");
			input_GYO= new File(sdCardDir.getCanonicalPath() +Data_Save_Path+'/'+"GYO.txt");

			try {

				fos_ACC = new FileOutputStream(input_ACC,true);// 建立一个可存取字节的文件
				fos_GYO = new FileOutputStream(input_GYO,true);// 建立一个可存取字节的文件
			} catch (Exception e) {
				e.printStackTrace();
			}

			//output_file = new File(sdCardDir.getCanonicalPath() +Data_Save_Path+'/'+"Voice.wav");
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
  
    }  
    //重写 onDestroy 方法  
    public void onDestroy() {  
        sm.unregisterListener(accelerationListener);  
        currentAcceleration = 0;  
        maxAcceleration = 0;  
        xAcceleration = yAcceleration = zAcceleration = 0;  
  
        // 注销监听器  
        sm.unregisterListener(gyoneticListener);  
        sm = null;  
        try {
			fos_ACC.close();
			fos_GYO.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        isRecord=false;
		
  
        super.onDestroy();  
    }  
      
      
    @Override  
    public IBinder onBind(Intent arg0) {  
        // TODO Auto-generated method stub  
        return null;  
    }  
  
      
    // **************************加速度测试传感器部分*************************  
    // 加速度传感器监听器，当获取的传感器数据发生精度要求范围内的变化时，监听器会调用onSensorChanged函数  
    SensorEventListener accelerationListener = new SensorEventListener() {  
          
        public void onSensorChanged(SensorEvent event) {  
        	if(event.sensor.getType()==Sensor.TYPE_ACCELEROMETER)    {
        	xAcceleration = event.values[SensorManager.DATA_X];          
        	yAcceleration = event.values[SensorManager.DATA_Y];          
        	zAcceleration = event.values[SensorManager.DATA_Z]; 
        	}
            // 计算三个方向上的和加速度  
            double G = Math.sqrt(Math.pow(xAcceleration, 2)  
                    + Math.pow(zAcceleration, 2) + Math.pow(yAcceleration, 2));  
            currentAcceleration = G;  
            if (currentAcceleration > maxAcceleration)  
                maxAcceleration = currentAcceleration;  
            String write_ACC= xAcceleration+","+yAcceleration+","+zAcceleration+","+currentAcceleration;
            if(isRecord){
            	 new MyAsyncTask().doInBackground(1,write_ACC);
                 // writeSensorData( event);
            	
            }
 
            Intent i = new Intent();  
            i.setAction("com.dm.accReceiver");  
            i.putExtra("xAcceleration", xAcceleration);  
            i.putExtra("yAcceleration", yAcceleration);  
            i.putExtra("zAcceleration", zAcceleration);  
            i.putExtra("currentAcceleration", currentAcceleration);  
            i.putExtra("maxAcceleration", maxAcceleration);  
            sendBroadcast(i);  
        }  
  
        public void onAccuracyChanged(Sensor sensor, int accuracy) {  
        }  
  
    };  
  
    // ************************陀螺仪传感器**************************  
    // 手机方位传感器监听器，当获取的加速度或者陀螺仪传感器数据发生精度要求范围内的变化时，监听器会调用onSensorChanged函数  
    SensorEventListener gyoneticListener = new SensorEventListener() {  
  
        public void onSensorChanged(SensorEvent event) {  
            // 如果是加速度传感器的值发生了变化  
        	if(event.sensor.getType()==Sensor.TYPE_GYROSCOPE)    
			{    
        		Yaw = event.values[SensorManager.DATA_X];          
        		Pitch = event.values[SensorManager.DATA_Y];          
        		Roll = event.values[SensorManager.DATA_Z];     
				
			} 
        	 double G1 = Math.sqrt(Math.pow(Yaw, 2)  
                     + Math.pow(Pitch, 2) + Math.pow(Roll, 2));  
            double currentAcceleration1 = G1; 
        	 String write_GYO= Yaw+","+Pitch+","+zAcceleration+","+Roll+","+currentAcceleration1;
        	 if(isRecord){
                 new MyAsyncTask().doInBackground(2,write_GYO);
             	//writeSensorData( event);
        	 }

              
            Intent i = new Intent();  
            i.setAction("com.dm.magReceiver");  
            i.putExtra("Yaw", Yaw);  
            i.putExtra("Pitch", Pitch);  
            i.putExtra("Roll", Roll);  
            sendBroadcast(i);  
        }  
  
        public void onAccuracyChanged(Sensor sensor, int accuracy) {  
        }  
    };  
    
    public class MyAsyncTask extends AsyncTask<String, Integer, String>//save and display the data here
	{
		@Override
		protected void onPreExecute()
		{
			super.onPreExecute();
			//    ﾔﾚonPreExecute()ﾖﾐﾎﾒﾃﾇﾈﾃProgressDialogﾏﾔﾊｾｳｴ
			// progressDialog.show();
		}
		protected void doInBackground(int flag,String result) {

			writeSensorData( flag,result);
		}

		protected void onPostExecute(String result)
		{
			super.onPostExecute(result);

		}
		@Override
		protected String doInBackground(String... arg0) {
			// TODO Auto-generated method stub
			//writeDateTOFile();//往文件中写入裸数据,20151121,可以先在手机中保存raw数据，然后再程序退出的情况下将数据封装成wav
			return null;
		}

	}
	
    private synchronized void writeSensorData(int flag,String result){
		//save_data write_file = new save_data();
		long time =System.currentTimeMillis();
		SimpleDateFormat format = new SimpleDateFormat(" HH:mm:ss");
		String str_time = format.format(new Date(time));


		if(flag==1)    
		{    
			
			if(fos_ACC!=null)
			{
				try {
					fos_ACC.write( (str_time+":"+result+'\n').getBytes());
					//fos_ACC.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}else if(flag==2)    
		{    
			
			if(fos_GYO!=null)
			{
				try {
					fos_GYO.write( (str_time+":"+result+'\n').getBytes());
					//fos_GYO.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}


			}
			
		}    
	}
}
