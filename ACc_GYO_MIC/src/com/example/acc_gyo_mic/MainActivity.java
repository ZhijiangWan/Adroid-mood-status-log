package com.example.acc_gyo_mic;

import java.io.File;
import java.io.RandomAccessFile;
import java.sql.Date;
import java.text.SimpleDateFormat;


import android.support.v7.app.ActionBarActivity;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;


public class MainActivity extends Activity {


	private EditText subject_name;
	private EditText subject_ID;
	private EditText experimenter_name;
	private Button information_save;
	private Button program_quit;
	private Spinner experiment_type;
	public String sub_name;
	public String sub_ID;
	public String experimenter;
	public String type;
	String Transfer_Path;
	String FILE_Data="/Data.txt";//save the data
	String FILE_Information="/Information.txt";//save the related experiment information

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.experiment_information);
		information_save=(Button)this.findViewById(R.id.save); 
		subject_name=(EditText)this.findViewById(R.id.sub_name);
		subject_ID=(EditText)this.findViewById(R.id.sub_ID);
		experimenter_name=(EditText)this.findViewById(R.id.experimenter_name);
		experiment_type=(Spinner)this.findViewById(R.id.spinner2);
		information_save.setOnClickListener(new SaveButtonListener()); 
	}

	class SaveButtonListener implements android.view.View.OnClickListener 
	{ 
		@Override 

		public  void onClick(View v) 

		{ 
			String Transfer_path = null;
			String path2;
			sub_name=subject_name.getText().toString();  
			if(sub_name==null||sub_name.equals(""))
			{
				subject_name.setError("please input something:");
				return;

			}
			//According to different Bluetooth address, caling dufferent function to write data
			sub_ID=subject_ID.getText().toString();
			experimenter=experimenter_name.getText().toString();
			if(experimenter==null||experimenter.equals(""))
			{
				experimenter_name.setError("please input something:");
				return;

			}
			type=experiment_type.getSelectedItem().toString().trim();
			if(type.contains("multimode")){
				Transfer_Path=Save_Multimode_Data();

			}

			// String Transfer_path=Save_HeartRate_Data();//transfer the path which recode the data
			Intent intent=new Intent(); 

			intent.setClass(MainActivity.this,DeviceControlActivity.class); 

			intent.putExtra("Data_Path", Transfer_Path); 
			intent.putExtra("Information_save", Transfer_Path +FILE_Information);

			MainActivity.this.startActivity(intent); 

		}        

	}

	private String Save_Multimode_Data() {
		int toast_control_data=0;
		int toast_control_SD=0;
		// Time time = new Time("GMT+8");  
		// time.setToNow();
		// String time_w=time.year+"\\"+time.month+"\\"+time.monthDay+"\\"+time.hour+":"+time.minute+":"+time.second;
		long time =System.currentTimeMillis();
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss");
		String str_time = format.format(new Date(time));

		String Directory_name=sub_name+str_time;//take it as the name of sub-folder
		String path1=null;
		//String path2 = null;

		String path_subdirectory=null;
		//if (data != null) 
		{
			//mDataField.setText(data);
			try {
				if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
					//ｻ｡SDｿｨｵﾄﾄｿﾂｼ
					File sdCardDir = Environment.getExternalStorageDirectory();
					path1="/Multimode wearable Data";


					path_subdirectory=path1+"/"+Directory_name;


					File targetDir = new File(sdCardDir.getCanonicalPath() + path_subdirectory);
					File targetFile = new File(sdCardDir.getCanonicalPath() + path_subdirectory+FILE_Information);

					if(!targetDir.exists()){
						targetDir.mkdirs();//create the folder

					}

					//ﾒﾔﾖｸｶｨﾎﾄｼ�ｴｴｽｨRandomAccessFileｶﾔﾏ�
					RandomAccessFile raf = new RandomAccessFile(targetFile, "rw");
					//ｽｫﾎﾄｼ�ｼﾇﾂｼﾖｸﾕ�ﾒﾆｶｯｵｽﾗ鋓�
					raf.seek(targetFile.length());
					String str1="data collected time: ";
					String str2=str_time;
					String str3="subject name: ";
					String str4=sub_name;
					String str5="subject ID: ";
					String str6=sub_ID;
					String str7="experimenter name: ";
					String str8=experimenter;
					String str9="experiment type: ";
					String str10=type;

					String t = str1+str2+"\r\n"
							+str3+str4+"\r\n"
							+str5+str6+"\r\n"
							+str7+str8+"\r\n"
							+str9+str10+"\r\n"
							;

					raf.write(t.getBytes());




					raf.close();
					/* OutputStream out=new FileOutputStream(targetFile,"rw");  
	                     out.write(data.getBytes());  
	                     out.close();  */

				}else{
					if(toast_control_SD<20)
					{
						Toast.makeText(getApplicationContext(), "not write to SD card, please restart the program or check your code.",Toast.LENGTH_SHORT).show();
						toast_control_SD++;

					}

				}
			} catch (Exception e) {
				e.printStackTrace();
			}



		}
		return path_subdirectory;

	}

	@Override
	protected void onResume() {
		super.onResume();

	}

	@Override
	protected void onPause() {
		super.onPause();

	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		//getMenuInflater().inflate(R.menu.gatt_services, menu);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		return super.onOptionsItemSelected(item);
	}
}
