package com.ubicomp.ketdiary;

import java.io.File;
import java.util.Calendar;

import com.ubicomp.ketdiary.data.db.DatabaseControl;
import com.ubicomp.ketdiary.data.file.ColorRawFileHandler;
import com.ubicomp.ketdiary.data.file.MainStorage;
import com.ubicomp.ketdiary.data.file.PicFileHandler;
import com.ubicomp.ketdiary.data.file.TestDataParser2;
import com.ubicomp.ketdiary.data.structure.TestDetail;
import com.ubicomp.ketdiary.data.structure.TimeValue;
import com.ubicomp.ketdiary.dialog.AddNoteDialog2;
import com.ubicomp.ketdiary.dialog.NoteDialog4;
import com.ubicomp.ketdiary.main.fragment.TestFragment2;
import com.ubicomp.ketdiary.system.Config;
import com.ubicomp.ketdiary.system.PreferenceControl;
import com.ubicomp.ketdiary.test.bluetoothle.BluetoothLE;
import com.ubicomp.ketdiary.test.bluetoothle.BluetoothListener;
import com.ubicomp.ketdiary.test.camera.Tester;
import com.ubicomp.ketdiary.test.color.ColorDetectListener;
import com.ubicomp.ketdiary.test.color.ImageDetection;
import com.ubicomp.ketdiary.ui.CustomToastSmall;
import com.ubicomp.ketdiary2.R;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import android.widget.Toast;

public class ResultService3 extends Service implements BluetoothListener, ColorDetectListener{
	
	private ResultService3 myservice = this;
	private static Context context = App.getContext();
	private Activity activity;
	
	public  static  final  String TAG =  "MyService" ;  
	private Handler mhandler = new Handler();
	private Handler blehandler = new Handler();
	private Handler stophandler = new Handler();
	
	private long startTime;
	private static long timeout = MainActivity.getMainActivity().WAIT_RESULT_TIME; //1*60*1000;//10*60*1000;
	private Notification notification;
	private PendingIntent pendingIntent;
	
	private BluetoothLE ble = null;
	private boolean stateSuccess = false;
	public static boolean isConnect = false;
	public static boolean pictureSending = false;
	private static int state;
	
	private int result= -1;
	private DatabaseControl db;
	private int picNum = 0;
	private OpenSensorMsgTimer openSensorMsgTimer;
	private GetPictureTimer getPictureTimer;
	private final ConnectSensorTimer connectSensorTimer = new ConnectSensorTimer();
	
	private ProgressDialog dialog = null;
	private boolean first = true;
	private boolean second = true;
	public TestDataParser2 TDP;
	private ImageDetection imageDetection = null;
	private PicFileHandler picFileHandler = null;
	
	private int failedState = 0;
	private float connectionFailRate = 0;
	private String failedReason = "";
	private int colorReading = 0;
	private int colorReading1 = 0;
	private int colorReading2 = 0;
	private static final int REGULAR_CONNECT_FAIL = 8;
	private static final int CONNECT_FAIL  = 9;
	private static final int PIC_SEND_FAIL = 10;
	
	private static final int BEGIN_STATE = 9;
	private static final int FRAME_STATE = 10;
	private static final int REGULAR_STATE  = 11;
	private static final int DETECT_STATE = 12;
	private static final int RESULT_STATE = 13;
	private static final int END_STATE = 14;
	
	private static final int DETECT_THRESHOLD = 0; //1.88 modify
	
	private boolean testSuccess = false;
	private int regular_connect = 0;
	private boolean connect = false;
	private boolean debug = PreferenceControl.isDebugMode();
	private boolean demo = PreferenceControl.isDemo();
	
	public static long spentTime ;
	private ColorRawFileHandler colorRawFileHandler;
	private File mainDirectory = null;
	private long ts;
	private long minutes;
	private long seconds;
	private boolean active_disconnect = false;
	private boolean result2 = false;
	private boolean checkResultOnce = false;
	private boolean startFlag = false;
	
	private Calendar today = Calendar.getInstance(); 
	private int testResult = 0;
	private boolean svmTest = false;
	public static boolean savePic = false, firstPic = false;
	private int firstFailTimes;
	private boolean getFirstPic;
	private boolean failed;
	
    @Override  
    public  void onCreate() {  
        super.onCreate();  
        
        acquireWakeLock();
        writeToColorRawFile("Service OnCreate");
        Log.d(TAG,  "MyService onCreate" );
        if(debug)
        	CustomToastSmall.generateToast("Service Start!");
    }
    /**Use startService to call ResultService*/
	public int onStartCommand(Intent intent, int flags, int startId) {
		
	    notification =  new  Notification(R.drawable.k_logo, "有通知到來" , System.currentTimeMillis());  
	    Intent notificationIntent =  new  Intent( this , MainActivity.class );  
	    pendingIntent = PendingIntent.getActivity( this ,  0 ,  
	            notificationIntent,  0 );  
	    notification.setLatestEventInfo( this ,  "這是通知的標題" ,  "這是通知的內容" , pendingIntent);  
	    startForeground( 1 , notification);
	        	
	    state = BEGIN_STATE;   	
		initVariable();
		writeToColorRawFile("Service Start, State = " + state + " Device: " + PreferenceControl.getDeviceId());
		PreferenceControl.setResultServiceRun(true);
	    Log.d(TAG,  "onStartCommand() executed" );
	    if(!demo)
	    	mhandler.postDelayed(updateTimer, 1000);
	    else
	    	mhandler.postDelayed(demoTimer, 1000);


	    return  super.onStartCommand(intent, flags, startId);  
	}
	
	private void initVariable(){
		colorReading = 0;
		colorReading1 = 0;
		colorReading2 = 0;
		firstFailTimes = 0;
		getFirstPic = false;
		failed = false;
		
		timeout = PreferenceControl.getAfterCountDown()*1000;
		
		spentTime = timeout;
		isConnect = false;
		//isConnect = true;
		pictureSending = false;
		first = true;
		second = true;
		connect = false;
		checkResultOnce = false;
		regular_connect = 0;
		picNum = 0;
		active_disconnect = false;
		debug = PreferenceControl.isDebugMode();
        db = new DatabaseControl();
        openSensorMsgTimer = new OpenSensorMsgTimer();
		getPictureTimer = new GetPictureTimer();
        imageDetection = new ImageDetection(null);
		ts = PreferenceControl.getUpdateDetectionTimestamp();
		File dir = MainStorage.getMainStorageDirectory();
		mainDirectory = new File(dir, String.valueOf(ts));
        colorRawFileHandler = new ColorRawFileHandler(mainDirectory,String.valueOf(ts));
        if(ble == null ) {
        	//activity = (Activity) context;
        	Log.d("GG", "Service start connect");
	    	ble = new BluetoothLE( myservice , PreferenceControl.getDeviceId(), ts);	
        }
        //ble = Config.ble;
	            
	    PreferenceControl.setTestFail();
	    stateSuccess = false;
	    startTime = System.currentTimeMillis();	
	    
	    //
	    AddNoteDialog2.testFinished = false;
	}
    
	private Runnable updateTimer = new Runnable() {
		@SuppressWarnings("deprecation")
		public void run() {

			long passTime = System.currentTimeMillis() - PreferenceControl.getLatestTestCompleteTime();
			
			long startTime = System.currentTimeMillis();
			
			spentTime = timeout - passTime;
			
			minutes = (spentTime/1000)/60;
			seconds = (spentTime/1000) % 60;
			
			notification.setLatestEventInfo( myservice ,  "測試結果倒數" ,  minutes+":"+seconds , pendingIntent);  
	        startForeground( 1 , notification); 
	        
	        
	        if(seconds == 30){
	        	writeToColorRawFile("State: " + state);
	        }
	        
	        //mhandler.postDelayed(this, 1000);
	        if(state == BEGIN_STATE){
	        	if(spentTime > 3*60*1000){                  //最晚兩分鐘前要把第一張照拍好
		        	
		        	if(first){                                                   									
						if(openSensorMsgTimer!=null){
							openSensorMsgTimer.cancel();
				       		openSensorMsgTimer.start();
				       		first = false;
						}
					}
		        	if(isConnect && !pictureSending){
		        		openSensorMsgTimer.cancel();
						getFirstPic = false;
						pictureSending = true;
						savePic = false;

		        		Handler handler = new Handler();
		                handler.postDelayed(new Runnable() {
		                     @Override 
		                     public void run() { 
		                     	if(ble != null)
		 		        		{
									writeToColorRawFile("snap shot");

									ble.bleTakePicture();
									if(getPictureTimer!=null){
										getPictureTimer.cancel();
										getPictureTimer.start();
									}

									//ble.bleTakePicture();
									state = FRAME_STATE;
		                     		
		 		        		}
		                     } 
		                }, 5000);
		        	}
		        	
				    
	        	}
	        	if(spentTime <=0){
        			writeToColorRawFile("Time's up!");
        			goResultSuccess();
	        	/*if(svmTest)
        			goResultSuccess();
        		else
        			setTestFail("無法判斷檢測結果");*/
	        	
	        		state = END_STATE;
        		}
	        }
	        else if(state == FRAME_STATE){
	        	if(getFirstPic)
				{
	        		pictureSending = false;
	        		state = REGULAR_STATE;
	        	}
	        		
	        	else if(!getFirstPic && !pictureSending)
	        	{
	        		pictureSending = true;
	        		savePic = false;
	        		if(ble != null)
	        		{
	        			writeToColorRawFile("snap shot");
	        			ble.bleTakePicture();
						if(getPictureTimer!=null){
							getPictureTimer.cancel();
							getPictureTimer.start();
						}
	        		}
	        	}
	        	
	        	else if(spentTime < 10*60*1000 && !getFirstPic)
	        	{
	        		setTestFail("無法獲取試紙照片");
		        	active_disconnect = true;
		        	if(ble != null)	{
		        		//ble.bleHardTermination();
		        		clearDevice();
		        	}
		        		
		        	//!!
		        	ble = null;
	        	}
	        	if(spentTime <=0){;
        		writeToColorRawFile("Time's up!");
        		goResultSuccess();
	        	/*if(svmTest)
        			goResultSuccess();
        		else
        			setTestFail("無法判斷檢測結果");*/
	        	
	        	state = END_STATE;
        	}
	        }
	        else if(state == REGULAR_STATE){
	        	if(spentTime > 3*60*1000 ){ 
		        	if(seconds == 0){    //Regular connect after take  picture.
		        		if(connectSensorTimer != null)
		        			//connectSensorTimer.cancel();
		        			Log.i(TAG, "second = 0");
		        			if(!isConnect){
		        				Log.d("GG", "why!!!");
		        				connect = false;
		        				writeToColorRawFile("Connect Timer Start");
		        				connectSensorTimer.cancel();
		        				connectSensorTimer.start();
		        			}
		        	}
	        	}
	        	else if(spentTime < 2*60*1000 && picNum == 0 && !pictureSending){
	        		//state = DETECT_STATE;
	        		//state = RESULT_STATE;
	        		pictureSending = true;

	        		savePic = true;
	        		ble.bleTakePicture();
	                writeToColorRawFile("Write State : Snapshot");
	        	}
	        	else if(spentTime < 1*60*1000 && picNum == 1 && !pictureSending){
	        		//state = DETECT_STATE;
	        		pictureSending = true;
	        		state = RESULT_STATE;

	        		
	        		savePic = true;
	        		ble.bleTakePicture();
	                writeToColorRawFile("Write State : Snapshot");
	        	}
	        	else if(spentTime <= 10)
	        	{
					if(picNum == 0) {
						setTestFail("無法獲取試紙照片");
						active_disconnect = true;
						//ble.bleHardTermination();
						clearDevice();
						//!!
						ble = null;
					}
					else
						state = RESULT_STATE;
	        	}
	        }
	        else if(state == DETECT_STATE){
	        	
	        	if(seconds <= 10 && minutes == 0 && !checkResultOnce){
	        		if(picNum == 1){
	        			setTestFail("無法獲得照片");
	        		}
	        		//result2 = checkResult();
	        		writeToColorRawFile("checkResult");
	        		checkResultOnce = true;
				}
	        	else if(spentTime <=0){
	        		writeToColorRawFile("Time's up!");
	        		
		        	if(result2)
	        			goResultSuccess();
	        		else
	        			setTestFail("無法判斷檢測結果");
		        	
		        	state = END_STATE;
	        	}
	        	else if(spentTime < 2*60*1000 && picNum == 1){   //剩兩分鐘的時候才開始拍照傳照片
					
					if(second){									
						if(openSensorMsgTimer!=null){
							openSensorMsgTimer.cancel();
			        		openSensorMsgTimer.start();
			        		second = false;
						}
					}
					if(isConnect){
		        		openSensorMsgTimer.cancel();
		        	}										
					if(!stateSuccess && isConnect && !first){
						/*GG*/
						/*regular image*/
						/*writeToColorRawFile("Write State : 0x06");
			        	ble.bleWriteState((byte)0x06);*/
						ble.bleTakePicture();
		                writeToColorRawFile("Write State : Snapshot2");
			        	
			        	
			        	first = true;
					}
					
				}
	        	else if(picNum == 0){
	        		setTestFail("無過曝照片");
	        	}	  
	        	if(spentTime <=0){;
        		writeToColorRawFile("Time's up!");
        		goResultSuccess();
	        	/*if(svmTest)
        			goResultSuccess();
        		else
        			setTestFail("無法判斷檢測結果");*/
	        	
	        	state = END_STATE;
        	}
	        }
	        else if(state == RESULT_STATE){
	    
	        	/*if(seconds <= 10 && minutes == 0 && !checkResultOnce){
	        		result2 = checkResult();
	        		writeToColorRawFile("checkResult");
	        		checkResultOnce = true;
				}*/
	        	if(spentTime <=0){;
	        		writeToColorRawFile("Time's up!");
	        		goResultSuccess();
		        	/*if(svmTest)
	        			goResultSuccess();
	        		else
	        			setTestFail("無法判斷檢測結果");*/
					clearDevice();
		        	state = END_STATE;

	        	}
	        }
	        long endTime = System.currentTimeMillis();
	        long runTime = endTime - startTime;
	        
	        writeToColorRawFile("RunTime: " + runTime);
	        
	        mhandler.postDelayed(this, 1000);
	        
//	        if( spentTime < 0 ){
//	        	boolean isfail = PreferenceControl.isTestFail();
//	        	if( isfail ){
//	        		setTestFail("檢測照片接收失敗");
//	        	}
//	        }
		}
    };
    
	private Runnable demoTimer = new Runnable() {
		@SuppressWarnings("deprecation")
		public void run() {

			long passTime = System.currentTimeMillis() - PreferenceControl.getLatestTestCompleteTime();
			
			long startTime = System.currentTimeMillis();
			
			spentTime = timeout - passTime;
			
			minutes = (spentTime/1000)/60;
			seconds = (spentTime/1000) % 60;
			
			notification.setLatestEventInfo( myservice ,  "測試結果倒數" ,  minutes+":"+seconds , pendingIntent);  
	        startForeground( 1 , notification); 
	        
	        
	        if(seconds == 30){
	        	writeToColorRawFile("State: " + state);
	        }
	        
	        //mhandler.postDelayed(this, 1000);
	        if(state == BEGIN_STATE){
	        	if(spentTime > 2*60*1000){  //最晚兩分鐘前要把第一張照拍好
		        	
		        	if(first){                                                   									
						if(openSensorMsgTimer!=null){
							openSensorMsgTimer.cancel();
				       		openSensorMsgTimer.start();
				       		first = false;
						}
					}
		        	if(isConnect){
		        		openSensorMsgTimer.cancel();
		        	}
				    if(!stateSuccess && isConnect && picNum == 0){
				    	/*GG*/
				    	/*exposed image*/
				    	/*ble.bleWriteState((byte)0x03);
				    	writeToColorRawFile("Write State : 0x03");*/
				    	byte [] command = new byte[] {BluetoothLE.BLE_REQUEST_IMAGE_INFO};
		                ble.mAppStateTypeDef = BluetoothLE.AppStateTypeDef.APP_IMAGE_GET_HEADER;
		                ble.bleWriteCharacteristic1(command);
		                writeToColorRawFile("Write State : Snapshot");
						
				    }
	        	}
	        	else{
	        		setTestFail("過曝照片未傳完");
	        	}
	        }
	        else if(state == FRAME_STATE){
	        }
	        else if(state == REGULAR_STATE){
	        	if(spentTime > 3*60*1000 ){ 
		        	if(seconds == 0 && picNum == 1){    //Regular connect after take  picture.
		        		if(connectSensorTimer != null)
		        			//connectSensorTimer.cancel();
		        			Log.i(TAG, "second = 0");
		        			if(!isConnect){
		        				connect = false;
		        				writeToColorRawFile("Connect Timer Start");
		        				connectSensorTimer.cancel();
		        				connectSensorTimer.start();
		        			}
		        	}
		        	else if(picNum == 0){
		        		setTestFail("過曝照片未傳成功");
		        	}
	        	}
	        	else if(spentTime < 2*60*1000 ){
	        		state = RESULT_STATE;
	        		writeToColorRawFile("State = " + state);
	        	}
	        }
	        else if(state == DETECT_STATE){
	        	
	        	if(seconds <= 10 && minutes == 0 && !checkResultOnce){
	        		//result2 = checkResult();
	        		writeToColorRawFile("checkResult");
	        		checkResultOnce = true;
				}
	        	else if(spentTime <=0){
	        		writeToColorRawFile("Time's up!");
	        		
		        	if(result2)
	        			goResultSuccess();
	        		else
	        			setTestFail("無法判斷檢測結果");
		        	
		        	state = END_STATE;
	        	}
	        	else if(spentTime < 2*60*1000 && picNum == 1){   //剩兩分鐘的時候才開始拍照傳照片
					
					if(second){									
						if(openSensorMsgTimer!=null){
							openSensorMsgTimer.cancel();
			        		openSensorMsgTimer.start();
			        		second = false;
						}
					}
					if(isConnect){
		        		openSensorMsgTimer.cancel();
		        	}										
					if(!stateSuccess && isConnect && !first){
						/*GG*/
						/*regular image*/
						/*writeToColorRawFile("Write State : 0x06");
			        	ble.bleWriteState((byte)0x06);*/
						byte [] command = new byte[] {BluetoothLE.BLE_REQUEST_IMAGE_INFO};
		                ble.mAppStateTypeDef = BluetoothLE.AppStateTypeDef.APP_IMAGE_GET_HEADER;
		                ble.bleWriteCharacteristic1(command);
		                writeToColorRawFile("Write State : Snapshot2");
		                
			        	first = true;
					}
				}
	        	else if(picNum == 0){
	        		setTestFail("無過曝照片");
	        	}
	        	
	        }
	        else if(state == RESULT_STATE){
	        	
	        	/*if(seconds <= 10 && minutes == 0 && !checkResultOnce){
	        		result2 = checkResult();
	        		writeToColorRawFile("checkResult");
	        		checkResultOnce = true;
				}*/
	        	//Log.d("GG", "zzzzzz");
	        	if(spentTime <=0){
	        		writeToColorRawFile("Time's up!");
	        		
		        	if(testResult != 0)
	        			goResultSuccess();
	        		else
	        			setTestFail("無法判斷檢測結果");
		        	
		        	state = END_STATE;
		        	MainActivity.getMainActivity().enableTabAndClick(true);
	        	}
	        }
	        long endTime = System.currentTimeMillis();
	        long runTime = endTime - startTime;
	        
	        writeToColorRawFile("RunTime: " + runTime);
	        
	        mhandler.postDelayed(this, 1000);
	        
		}
    };
    
    @SuppressWarnings("deprecation")
	private void goResultSuccess(){   //檢測成功的話
    	writeToColorRawFile("goResult");
    	notification.defaults = Notification.DEFAULT_ALL;
		notification.flags |= Notification.FLAG_AUTO_CANCEL;
		notification.setLatestEventInfo( myservice , "檢測倒數結束", "前往測試結果", pendingIntent);
		NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		
		Log.i(TAG, "GoResult!");
		boolean inApp = PreferenceControl.getInApp();	
		Log.d("InApp",String.valueOf(inApp));
		if(!inApp)
			notificationManager.notify(0, notification);
    	
		if(!demo)
			mhandler.removeCallbacks(updateTimer);
		else
			mhandler.removeCallbacks(demoTimer);
		
		if(ble!=null){
			isConnect = false;
			/*GG*/
			/*disconnect and put device into sleep mode*/
			/*writeToColorRawFile("Write State : 0x05");
			ble.bleWriteState((byte)0x05);
			ble = null;*/
			byte[] command = new byte[]{BluetoothLE.BLE_SHUTDOWN};
            /* GG ble.setManualDisconnectFlag(true);*/
            ble.bleWriteCharacteristic1(command);
            writeToColorRawFile("Write State : Disconnet");

            ble = null;
		}
		
		if(colorRawFileHandler != null){
        	colorRawFileHandler.close();
        	colorRawFileHandler = null;
        }
		
		stopForeground(true);
		stopSelf();
    }
    
    private void setResultSuccess(){
    	failedReason = "檢測成功";
		setTestDetail();
		PreferenceControl.setCheckBars(true);
		PreferenceControl.setTestSuccess();
		testSuccess = true;	
    	
    }
    
    
    private void setTestDetail(){
    	if(TestFragment2.testDetail!=null){
	    	String cassetteId = TestFragment2.testDetail.cassetteId;
			long ts = PreferenceControl.getUpdateDetectionTimestamp();
			int firstVoltage = TestFragment2.testDetail.firstVoltage;
			int secondVoltage= TestFragment2.testDetail.secondVoltage;
			int devicePower = TestFragment2.testDetail.devicePower;
			String hardwardVersion = TestFragment2.testDetail.hardwareVersion;
			//Toast.makeText(myservice, "Check: "+ colorReading, Toast.LENGTH_SHORT).show();
			Log.i(TAG, "Check: "+ colorReading);
			
			failedState = state;
			TestDetail testDetail = new TestDetail(cassetteId, ts, failedState, firstVoltage,
					secondVoltage, devicePower, colorReading,
	                connectionFailRate, failedReason, hardwardVersion);
			
			/* Check appeal (fail) */
			if(failedState >= 8 && failedState <= 12)
			{
				long _ts = PreferenceControl.getAppealLastFail();
				TimeValue _tv = TimeValue.generate(_ts);
				TimeValue now_tv = TimeValue.generate(ts);
				int appealAble = PreferenceControl.getAppealAble();
				
				if(_ts == 0)
				{
					PreferenceControl.setAppealFail(true);
				}
				else
				{
					boolean sameday = _tv.isSameDay(now_tv);
					if(!sameday)
					{
						PreferenceControl.setAppealFail(true);
					}
					else
					{
						boolean pre = PreferenceControl.getAppealFail();
						if(pre)
						{
							PreferenceControl.setAppealFail(false);
							PreferenceControl.setAppealAble(1);
							PreferenceControl.setAppealStartTime(ts);
						}
						else
						{
							PreferenceControl.setAppealFail(true);
						}
					}
					
				}
				PreferenceControl.setAppealLastFail(ts);
			}
			/* check end */
			
			db.insertTestDetail(testDetail);
			db.insertCassette(cassetteId);
    	}
    }
    
	@Override
	public IBinder onBind(Intent intent) {
		
		return null;
	}
	private WakeLock wakeLock = null;
	//获取电源锁，保持该服务在屏幕熄灭时仍然获取CPU时，保持运行
	private void acquireWakeLock(){
		if (null == wakeLock){
			PowerManager pm = (PowerManager)context.getSystemService(Context.POWER_SERVICE);
			wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK|PowerManager.ON_AFTER_RELEASE, "ResultService3");
			if (null != wakeLock){
				wakeLock.acquire();
			}
		}
	}

	//释放设备电源锁
	private void releaseWakeLock(){
		if (null != wakeLock){
			wakeLock.release();
			wakeLock = null;
		}
	}
	
	
	@Override  
    public void onDestroy() {  
             
        spentTime = 0;
        releaseWakeLock();
        
        PreferenceControl.setInTest(false);
        writeToColorRawFile("ResultService Close");
        Log.d(TAG, "OnDestroy Call");
        if(debug)
        	CustomToastSmall.generateToast("Service Stop!");
        stophandler.postDelayed(stopThread, 2000);
        //stop();
        PreferenceControl.setResultServiceRun(false);
        super.onDestroy();
    }
	
	private Runnable stopThread = new Runnable() {
		public void run() {
			stop();
		}
	};
	
	private void stop(){
		
		if(!demo)
			mhandler.removeCallbacks(updateTimer);
		else
			mhandler.removeCallbacks(demoTimer);
		
		if(connectSensorTimer!=null){
        	connectSensorTimer.cancel();
        	//connectSensorTimer=null;
		}
        
        if(openSensorMsgTimer!=null){
			openSensorMsgTimer.cancel();
			openSensorMsgTimer=null;
		}
		if(getPictureTimer!=null){
			getPictureTimer.cancel();
			//getPictureTimer=null;
		}
        if(ble!= null){
        	active_disconnect = true;
        	/*GG*/
			/*disconnect and put device into sleep mode*/
			/*writeToColorRawFile("Write State : 0x05");
			ble.bleWriteState((byte)0x05);
			ble = null;*/
			//byte[] command = new byte[]{BluetoothLE.BLE_SHUTDOWN};
            /*GG ble.setManualDisconnectFlag(true);*/
            ble.bleSelfDisconnection();
            
        	ble = null;
        }
        if(colorRawFileHandler != null){
        	colorRawFileHandler.close();
        	colorRawFileHandler = null;
        }
        
        if(picFileHandler!=null){
        	//picFileHandler.removeMessages(0);
        	picFileHandler = null;
        }
              
        spentTime = 0;
	}

	private class GetPictureTimer extends CountDownTimer { //OpenSensor for take picture

		public GetPictureTimer() {
			super(60000, 2000);
		}

		@Override
		public void onFinish() {
			pictureSending = false;
			writeToColorRawFile("Picture Timeout!!");
		}

		@Override
		public void onTick(long millisUntilFinished) {

		}
	}
	
	private class OpenSensorMsgTimer extends CountDownTimer { //OpenSensor for take picture

		public OpenSensorMsgTimer() {
			//super(20000, 2000);
			super(60000, 2000);
		}

		@Override
		public void onFinish() {
			
			failedState = CONNECT_FAIL;
			Toast.makeText(myservice, "請開啟檢測器", Toast.LENGTH_SHORT).show();
			
			setTestFail("檢測器未開啟 - Take picture");
		}

		@Override
		public void onTick(long millisUntilFinished) {
			Log.d(TAG, "Timer: "+ millisUntilFinished);
			if(!isConnect){
        		ble.bleConnect();
        		writeToColorRawFile("Open Sensor Timer connect");
			}

		}
	}
	
	private class ConnectSensorTimer extends CountDownTimer { //Regular connect
		
		//private boolean connect = false;
		private int count = 0;
		
		public ConnectSensorTimer() {
			super(20000, 2000);
		}

		@Override
		public void onFinish() {		
			failedState = REGULAR_CONNECT_FAIL;
			Toast.makeText(myservice, "請開啟檢測器", Toast.LENGTH_SHORT).show();
			setTestFail("檢測器未開啟- Regular connect");
		}

		@Override
		public void onTick(long millisUntilFinished) {  //連接在斷掉
			Log.d(TAG, "Timer: "+ millisUntilFinished);
			if(!isConnect){
				if(ble!=null){
					ble.bleConnect();
					connect = true;
					Log.i(TAG, "Regular Connect: " + regular_connect);
					writeToColorRawFile("Regular Connect: " + regular_connect);
				}
			}
			/*if(isConnect && connect){
				count++;
				if(count > 3){
					regular_connect++;
					if(ble!=null){
						active_disconnect = true;
						ble.bleSelfDisconnection();
						writeToColorRawFile("Regular DisConnect: " + regular_connect);
					}
					count = 0;
				}
			}*/
			//Toast.makeText(myservice, "請開啟檢測器", Toast.LENGTH_SHORT).show();
		}
	}
	
	
	
	private void setTestFail(String reason){
		Log.d("GG", "GG reason : "+reason);
		if(failed)
			return;
		failed = true;
		PreferenceControl.setTestFail();
		PreferenceControl.setAfterTestState(NoteDialog4.STATE_TEST);
		
		failedReason = reason;
		writeToColorRawFile("Test Fail: "+ failedReason);
		
		
		spentTime = 0;

		setTestDetail();
		boolean inApp = PreferenceControl.getInApp();
		if(inApp){
			//MainActivity.getMainActivity().setResultFail();
			MainActivity.getMainActivity().resultFailHandler.sendEmptyMessage(0);
		}
		
		notification.defaults = Notification.DEFAULT_ALL;
		notification.flags |= Notification.FLAG_AUTO_CANCEL;
		notification.setLatestEventInfo( myservice , "檢測失敗", "回到測試頁重測", pendingIntent);
		NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
	
		Log.d("InApp",String.valueOf(inApp));
		if(!inApp)
			notificationManager.notify(0, notification);
		
		//stop();
		stopForeground(true);
		stopSelf();	
	}


	/*@Override
	  GG
	 
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		if(ble!=null)
			ble.onBleActivityResult(requestCode, resultCode, data);
			ble.onBLEActivityResult(requestCode, resultCode, data);
	}*/

    @Override
    public void bleNotSupported() {
    	  //Toast.makeText(this, "BLE not support", Toast.LENGTH_SHORT).show();
//        this.finish();
    	writeToColorRawFile("BLE not support");
    }

    @Override
    public void bleConnectionTimeout() {
    	Log.i(TAG, "connect timeout");
    	writeToColorRawFile("connect timeout");
    	if(debug && !isConnect){
    		CustomToastSmall.generateToast("請開啟檢測器");
    	}

    }

    @Override
    public void bleConnected() {
    	Log.d("GG", "Service ble connected");
    	
    	isConnect = true;
    	
    	if(state == BEGIN_STATE)
    		first = false;
    	else if(state == DETECT_STATE)
    		second = false;
    	
    	Log.i("GG", "ResultService BLE connected!!");
        Log.i(TAG, "BLE connected");
        writeToColorRawFile("BLE connected");
        if(debug)
        	CustomToastSmall.generateToast("BLE connected");
        	//Toast.makeText(this, "BLE connected", Toast.LENGTH_SHORT).show();
        if(openSensorMsgTimer!=null){
			openSensorMsgTimer.cancel();
		}
        //PreferenceControl.setTestSuccess();
        Handler handler = new Handler(); 
        handler.postDelayed(new Runnable() {
             @Override 
             public void run() { 
            	 //ble.bleRequestCassetteInfo();
            	 writeToColorRawFile("ble request voltage");
            	 byte[] command = new byte[]{BluetoothLE.BLE_REQUEST_SALIVA_VOLTAGE};
            	 if(ble != null)
            	 {
            		 ble.mAppStateTypeDef = BluetoothLE.AppStateTypeDef.APP_FETCH_INFO;
                 	ble.bleWriteCharacteristic1(command);
            	 }
            	 Handler handler = new Handler(); 
                 handler.postDelayed(new Runnable() {
                      @Override 
                      public void run() { 
                     	 //ble.bleRequestCassetteInfo();
                     	 writeToColorRawFile("ble request id");
                     	 if(ble != null)
                     	 {
                     		ble.bleRequestCassetteInfo();
                     	 }
                      } 
                 }, 1000); 
             } 
        }, 1000); 
        /*writeToColorRawFile("ble request voltage");
        byte[] command = new byte[]{BluetoothLE.BLE_REQUEST_SALIVA_VOLTAGE};
        ble.mAppStateTypeDef = BluetoothLE.AppStateTypeDef.APP_FETCH_INFO;
        ble.bleWriteCharacteristic1(command);*/
    }

    @Override
    public void bleDisconnected() { //如果被動斷線的話就主動重連
    	writeToColorRawFile("call back : ble dissconnect!");
    	isConnect = false;
    	stateSuccess = false;
    	pictureSending = false;
        Log.i(TAG, "BLE disconnected");
//        if(state == BEGIN_STATE)
//    		first = true;
        /*GG*/
        //if(ble!=null)
        //	writeToColorRawFile("BLE disconnected :" + ble.hardware_state);
        if(connectSensorTimer != null)
			connectSensorTimer.cancel();
        
        if(!active_disconnect){
        	writeToColorRawFile("passive disconnect");
        	if(state == BEGIN_STATE || state == FRAME_STATE || state == DETECT_STATE){
	        	if(openSensorMsgTimer!=null){
	        		openSensorMsgTimer.cancel();
	        		openSensorMsgTimer.start();
	        	}
        	}
        }
        else{
        	writeToColorRawFile("active disconnect");
        }
        
        if(picNum != 1){
        	//setTestFail();
        }
        
        active_disconnect = false;
        //Toast.makeText(this, "BLE disconnected", Toast.LENGTH_SHORT).show();
        //setState(new FailState("連接中斷"));
        
    }

    /*GG*/
    /*@Override
    public void bleWriteStateSuccess() {
        Log.i(TAG, "BLE ACTION_DATA_WRITE_SUCCESS");
        
        if(ble != null)
        	writeToColorRawFile("BLE ACTION_DATA_WRITE_SUCCESS :" +  ble.hardware_state);
        if(debug){
        	CustomToastSmall.generateToast("BLE write state success");
        }
        //Toast.makeText(this, "BLE write state success", Toast.LENGTH_SHORT).show();
        stateSuccess = true;
        
        if(state == BEGIN_STATE){ //write 0x03 success, enter FRAME STATE
        	state = FRAME_STATE;
        	writeToColorRawFile("State = " + state);
        }
    }*/

    @Override
    public void bleWriteStateFail() {
    	writeToColorRawFile("call back : write state fail");
        Log.i(TAG, "BLE ACTION_DATA_WRITE_FAIL");
        writeToColorRawFile("BLE ACTION_DATA_WRITE_FAIL");
        if(debug)
        	Toast.makeText(this, "BLE writefstate fail", Toast.LENGTH_SHORT).show();
        stateSuccess = false;
    }
    
    private boolean noplug = false;
    /*@Override
     * GG
    public void bleNoPlug() {
    	
        Log.i(TAG, "No test plug");
        writeToColorRawFile("No test plug");

        if(!noplug){
        	setTestFail("試紙匣被拔出");
        	noplug = true;
        }
        
       //Toast.makeText(this, "No test plug", Toast.LENGTH_SHORT).show();
    }*/

    /*GG*/
    /*
    @Override
    public void blePlugInserted(byte[] plugId) {
        //Log.i(TAG, "Test plug is inserted");
    }


    @Override
    public void bleColorReadings(byte[] colorReadings) {
    }
    


	@Override
	public void bleElectrodeAdcReading(byte header, byte[] adcReading) {
		Log.i(TAG, "State: "+String.valueOf(header));
		
	}*/

	
	private Runnable writeBle = new Runnable() {
			public void run() {
				blehandler.removeCallbacks(writeBle);
			if(ble!=null){
				/*GG*/
				/*regular image*/
				/*writeToColorRawFile("Write State : 0x06");
	        	ble.bleWriteState((byte)0x06);*/
				byte [] command = new byte[] {BluetoothLE.BLE_REQUEST_IMAGE_INFO};
                ble.mAppStateTypeDef = BluetoothLE.AppStateTypeDef.APP_IMAGE_GET_HEADER;
                ble.bleWriteCharacteristic1(command);
                writeToColorRawFile("Write State : Snapshot2");
			}
		}
    };
    
    private Runnable writeBle3 = new Runnable() {
		public void run() {
			blehandler.removeCallbacks(writeBle3);
		if(ble!=null){
			/*GG*/
	    	/*exposed image*/
	    	/*ble.bleWriteState((byte)0x03);
	    	writeToColorRawFile("Write State : 0x03");*/
	    	byte [] command = new byte[] {BluetoothLE.BLE_REQUEST_IMAGE_INFO};
            ble.mAppStateTypeDef = BluetoothLE.AppStateTypeDef.APP_IMAGE_GET_HEADER;
            ble.bleWriteCharacteristic1(command);
            writeToColorRawFile("Write State : Snapshot");
		}
	}
};

	/*GG*/
	/*
	@Override
	public void updateProcessRate(String rate) {
		//Toast.makeText(this, String.valueOf(rate).concat(" %"), Toast.LENGTH_SHORT).show();
		if(debug)
			CustomToastSmall.generateToast(String.valueOf(rate));
		Log.i(TAG, String.valueOf(rate));
		writeToColorRawFile(String.valueOf(rate));
	}

	@Override
	public void clearProcesssRate() {
		writeToColorRawFile("Retransmit, Write Data");	
	}


	@Override
	public void bleTakePictureSuccess(Bitmap bitmap) {
		picNum++;
		Log.i(TAG, "Picture: " + picNum + " Save");
		pictureSending = false;
		//Toast.makeText(this, "Picture: " + picNum + " Save", Toast.LENGTH_SHORT).show();
		
		writeToColorRawFile("Picture: " + picNum + " Save");
		if(picNum == 1){
			imageDetection.roiDetectionOnWhite(bitmap);
			state = REGULAR_STATE;
			writeToColorRawFile("State = " + state);
			
			if(ble!=null){
				active_disconnect = true;
				ble.bleDisconnect();
			}
			
			return;
		}
		else if(picNum == 2){
			state = RESULT_STATE;
			writeToColorRawFile("State = " + state);
			
			colorReading1 = imageDetection.testStripDetection(bitmap);
			writeToColorRawFile("ColorReading1: " + colorReading1);
			
//			if(colorReading1  == -1000){
//				Log.i(TAG, "Reading: " + colorReading);
//				setTestFail("無法判斷檢測結果");
//			}
			blehandler.postDelayed(writeBle, 10*1000);
//			else{
//				blehandler.postDelayed(writeBle, 10*1000);
//			}
		}
		else if(picNum == 3){
			
			colorReading2 = imageDetection.testStripDetection(bitmap);
			writeToColorRawFile("ColorReading2: " + colorReading2);
			
//			state = SUCCESS_STATE;
//			writeToColorRawFile("State = " + state);
//			setResultSuccess();
//			picFileHandler = new PicFileHandler(5, bitmap);
//			picFileHandler.save();		
		}	
	}
	*/
	
	/*private boolean checkSingleColor(int color){
		colorReading = color;
		
		if(colorReading == -1000){
			Log.i(TAG, "Reading: " + colorReading);
			writeToColorRawFile("Line Can't Detect");
			return false;
			//setTestFail("無法判斷檢測結果");
		}
		else{
			writeToColorRawFile("Line Good");
			int result2 = colorReading > DETECT_THRESHOLD ? 0:1;
        	PreferenceControl.setTestResult(testResult);
        	setResultSuccess();
        	return true;
		}			
	}*/
	
	/*private boolean checkResult(){
		boolean isSuccess = true;
		if(picNum == 2){
			writeToColorRawFile("check First Pic");
			isSuccess = checkSingleColor(colorReading1);		
		}
		else if(picNum == 3){
			if(colorReading1 == -1000){
				writeToColorRawFile("check Second Pic");
				isSuccess = checkSingleColor(colorReading2);
			}
			else{
				if(colorReading2 == -1000){
					writeToColorRawFile("check First Pic 2");
					isSuccess = checkSingleColor(colorReading1);
				}
				else{
					writeToColorRawFile("Average Score");
					int tmpColor = (colorReading1 + colorReading2)/2;
					isSuccess = checkSingleColor(tmpColor);
				}
			}
		}
		return isSuccess;
	}*/
	
	protected void writeToColorRawFile(String str) {
		Message msg = new Message();
		Bundle data = new Bundle();
		data.putString("COLOR", today.get(Calendar.MINUTE) + "分 " +minutes+":"+seconds+"\t" + str+ "\n");
		msg.setData(data);
		if(colorRawFileHandler!= null)
			colorRawFileHandler.sendMessage(msg);
	}

	/*GG*/
	/*
	@Override
	public void bleTakePictureFail(float dropRate) {
		
		if(picNum == 1 || picNum == 2)
			blehandler.postDelayed(writeBle, 10*1000);
		else if(picNum == 0){
			//setTestFail("過曝照片傳送失敗，重新傳送");
			writeToColorRawFile("Picture 1 sending fail: "+dropRate);
			
			blehandler.postDelayed(writeBle3, 10*1000);
//			failedState = PIC_SEND_FAIL;
//			connectionFailRate = dropRate;
//			Log.i(TAG, "DropRate: " + dropRate);
//			writeToColorRawFile("Picture sending fail: "+dropRate);
//			setTestFail("照片傳送失敗");
		}
	}

	@Override
	public void imgDetect(Bitmap bitmap) {		
	}
	
	@Override
	public void colorDetectSuccess(int check) {
		colorReading = check;
		setTestDetail();
	}

	@Override
	public void PictureRetransmit(int count) {
		writeToColorRawFile("Picture sending Retransmit: "+count);
		if(!isConnect){
    		ble.bleConnect();
    		writeToColorRawFile("Picture sending Reconect");
    	}	
	}

	@Override
	public void displayHardwareVersion(String version) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void writeDebug(String msg) {
		Log.i(TAG, "Msg: " + msg);
		writeToColorRawFile(msg);
	}
	
	
	//TODO: Prevent change cassette 
	@Override
	public void displayCurrentId(String id, int hardwareState, int power_notenough) {
		if(power_notenough == 1){
			Log.i(TAG, "Power: " + power_notenough);
			writeToColorRawFile("Power: " + power_notenough);
			PreferenceControl.setPowerNotEnough(power_notenough);
		}
	}
	@Override
	public void displayPower(int power) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public Context getContext() {
		// TODO Auto-generated method stub
		return null;
	}*/
	@Override
	public void bleWriteCharacteristic1Success() {
		// TODO Auto-generated method stub
		writeToColorRawFile("call back : write character success");
		Log.i(TAG, "BLE ACTION_DATA_WRITE_SUCCESS");
        
        //if(ble != null)
        //	writeToColorRawFile("BLE ACTION_DATA_WRITE_SUCCESS :" +  ble.hardware_state);
        if(debug){
        	CustomToastSmall.generateToast("BLE write state success");
        }
        //Toast.makeText(this, "BLE write state success", Toast.LENGTH_SHORT).show();
        stateSuccess = true;
        
        if(state == BEGIN_STATE){ //write 0x03 success, enter FRAME STATE
        	state = FRAME_STATE;
        	writeToColorRawFile("State = " + state);
        }
	}
	@Override
	public void blePlugInserted(int cassetteId) {
		Log.d("GG", "call back : cassetteId = "+ cassetteId);
		writeToColorRawFile("call back : cassetteId = "+ cassetteId);
			
		
	}
	@Override
	public void bleUpdateBattLevel(int level) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void bleUpdateSalivaVolt(int volt) {
		// TODO Auto-generated method stub
		Log.d("GG", "call back : volt = "+ volt);
		//writeToColorRawFile("call back : volt = "+ volt);
	}
	@Override
	public void bleGetImageSuccess(Bitmap bitmap) {
		if(getPictureTimer!=null){
			getPictureTimer.cancel();
		}
		writeToColorRawFile("call back : get image success");
		// TODO Auto-generated method stub
		if(savePic)
			picNum++;
		else
			getFirstPic = true;
		Log.i(TAG, "Picture: " + picNum + " Save");
		pictureSending = false;
		//Toast.makeText(this, "Picture: " + picNum + " Save", Toast.LENGTH_SHORT).show();
		
		writeToColorRawFile("Picture: " + picNum + " Save");
		
		
	}
	@Override
	public void bleGetImageFailure(float dropoutRate) {
		if(getPictureTimer!=null){
			getPictureTimer.cancel();
		}
		writeToColorRawFile("call back : get image fail");
		pictureSending = false;
		if(!savePic)
		{
			if(firstFailTimes < 3)
			{
				firstFailTimes++;
				ble.bleTakePicture();
			}
			else
			{
				setTestFail("無法獲取試紙照片");
	        	active_disconnect = true;
	        	//ble.bleHardTermination();
	        	clearDevice();
	        	//!!
	        	ble = null;
			}
			
		}
		
		// TODO Auto-generated method stub
		/*if(picNum == 1 || picNum == 2)
			ble.bleTakePicture();
		else if(picNum == 0){
			writeToColorRawFile("Picture 1 sending fail: ");
			ble.bleTakePicture();
			
		}*/
		
	}
	@Override
	public void colorDetectSuccess(int check) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public Context getContext() {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public void bleNoPlugDetected() {
		// TODO Auto-generated method stub
		//todo:noplug
        writeToColorRawFile("ble No test plug");

        if(!noplug){
        	setTestFail("試紙匣被拔出");
        	active_disconnect = true;
        	//ble.bleHardTermination();
        	clearDevice();
        	//!!
        	ble = null;
        	noplug = true;
        }
	}
	@Override
	public void notifyDeviceVersion(int version) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void bleNotifyDetectionResult(double score) {
		// TODO Auto-generated method stub
		
		if(score == 1)
			testResult = 1;
		else if(score == -1)
			testResult = 0;
		
		svmTest = true;
		checkResultOnce = true;
		PreferenceControl.setTestResult(testResult);
		setResultSuccess();
	}
	@Override
	public void bleReturnDeviceVersion(int version) {
		// TODO Auto-generated method stub
		
	}
	
	public void clearDevice()
	{
		ble.bleUnlockDevice();
		ble.bleCancelCassetteInfo();
		ble.bleSelfDisconnection();
	}

}
