package com.storyroll.shake;

import com.storyroll.activity.VideoCaptureActivity;

import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.os.Vibrator;
import android.util.Log;
import android.widget.Toast;

public class ShakeService extends Service implements SensorEventListener {
	private static final String LOGTAG = "ShakeService";

	SensorManager mSensorManager;

	  /** Minimum movement force to consider. */
	  private static final int MIN_FORCE = 10;

	  /**
	   * Minimum times in a shake gesture that the direction of movement needs to
	   * change.
	   */
	  private static final int MIN_DIRECTION_CHANGE = 3;

	  /** Maximum pause between movements. */
	  private static final int MAX_PAUSE_BETHWEEN_DIRECTION_CHANGE = 200;

	  /** Maximum allowed time for shake gesture. */
	  private static final int MAX_TOTAL_DURATION_OF_SHAKE = 400;

	  /** Time when the gesture started. */
	  private long mFirstDirectionChangeTime = 0;

	  /** Time when the last movement started. */
	  private long mLastDirectionChangeTime;

	  /** How many movements are considered so far. */
	  private int mDirectionChangeCount = 0;

	  /** The last x position. */
	  private float lastX = 0;

	  /** The last y position. */
	  private float lastY = 0;

	  /** The last z position. */
	  private float lastZ = 0;

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	public void onCreate() {
		Log.d(LOGTAG, "onCreate");
		super.onCreate();
		lastUpdate = System.currentTimeMillis();
		//register your sensor manager listener here
		mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		mSensorManager.registerListener(this,
				mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
				SensorManager.SENSOR_DELAY_UI);
	}

	public void onDestroy() {
		Log.d(LOGTAG, "onDestroy");
		super.onDestroy();
		//unregister your listener here
	    mSensorManager.unregisterListener(this);
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d(LOGTAG, "onStartCommand");
	    // TODO Auto-generated method stub
	    mSensorManager = (SensorManager) getApplicationContext()
	            .getSystemService(SENSOR_SERVICE);
	    lastUpdate = System.currentTimeMillis();
	    mSensorManager.registerListener(this,
				mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
				SensorManager.SENSOR_DELAY_UI);
	    return START_STICKY;
	}
	
	public void onStart(Intent intent, int startId) {
		Log.i(LOGTAG, "shake detection service started");
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {

	}

//	private void getAccelerometer(SensorEvent event) {
//		if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
//			float[] values = event.values;
//			// Movement
//			float x = values[0];
//			float y = values[1];
//			float z = values[2];
//
//			float accelationSquareRoot = (x * x + y * y + z * z)
//					/ (SensorManager.GRAVITY_EARTH * SensorManager.GRAVITY_EARTH);
//			long actualTime = System.currentTimeMillis();
//			if (accelationSquareRoot >= 2) //
//			{
//				if (actualTime - lastUpdate < 2000) {
//					count++;
//					return;
//				}
//
//				lastUpdate = actualTime;
//
//				{
//					// logics
//					Log.v(LOGTAG, "phone shaken");
//					onShaken();
//				}
//			}
//
//		}
//	}

//	public void onSensorChanged(SensorEvent event) {
//
//		getAccelerometer(event);
//
//	}
	
//	@Override
//	public void onSensorChanged(SensorEvent event) {
//	    if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
//	        //detect the shake and do your work here
//	    }
//
//	}

	  @Override
	  public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            getAccelerometer(event);
        }  
	  }	
	  private long lastUpdate;

	  private void getAccelerometer(SensorEvent event) {
		    float[] values = event.values;
		    // Movement
		    float x = values[0];
		    float y = values[1];
		    float z = values[2];

		    float accelationSquareRoot = (x * x + y * y + z * z)
		            / (SensorManager.GRAVITY_EARTH * SensorManager.GRAVITY_EARTH);
		    long actualTime = System.currentTimeMillis();
		    if (accelationSquareRoot >= 7) //
		    {
		        if (actualTime - lastUpdate < 2000) {
		            return;
		        }
		        lastUpdate = actualTime;
		        Log.v(LOGTAG, "Device was shuffed _ " + accelationSquareRoot);
		        Vibrator v = (Vibrator) getApplicationContext().getSystemService(VIBRATOR_SERVICE);
		        v.vibrate(600);
		        onShake();
		    }
		}
	  
	  /**
	   * Resets the shake parameters to their default values.
	   */
	  private void resetShakeParameters() {
	    mFirstDirectionChangeTime = 0;
	    mDirectionChangeCount = 0;
	    mLastDirectionChangeTime = 0;
	    lastX = 0;
	    lastY = 0;
	    lastZ = 0;
	  }
//	protected void onResume() {
//		// super.onResume();
//		// register this class as a listener for the orientation and
//		// accelerometer sensors
//		sensorManager.registerListener(this,
//				sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
//				SensorManager.SENSOR_DELAY_NORMAL);
//	}
//
//	protected void onPause() {
//		// unregister listener
//		sensorManager.unregisterListener(this);
//
//	}
 
	private void onShake(){
	 Intent intent = new Intent(getApplicationContext(), VideoCaptureActivity.class);
	 intent.putExtra("MODE_NEW", true).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	 startActivity(intent);
	}
}