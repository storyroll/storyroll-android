package com.storyroll.activity;

import java.io.IOException;
import java.util.List;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.TextView;
import android.widget.VideoView;

import com.storyroll.R;
import com.storyroll.util.CameraUtility;

public class CameraTestActivity extends Activity {
	private static final String LOGTAG = "VIDEOTEST";

//	private SurfaceView surfaceView;

	private VideoView videoView;
	private Camera camera;
//	SurfaceHolder previewHolder = null;
	private Button rotateButton;
	TextView redButtonText, videocapReadyMessage, startStoryMessage;
	View redButton, redButtonCircle;
	
	Preview1 mPreview;
	ViewGroup videoHolder;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_videotest);
		getActionBar().hide();
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

//		surfaceView = (SurfaceView) findViewById(R.id.CameraView);

		videoView = (VideoView) findViewById(R.id.videoPlayerView);
		videoHolder = (ViewGroup) findViewById(R.id.videoHolder);
		
        mPreview = new Preview1(this);
        camera = getCameraInstance();
        mPreview.setCamera(camera);
        
        videoHolder.addView(mPreview);
        LayoutParams p = videoHolder.getLayoutParams();
        Display d = getWindowManager().getDefaultDisplay();
        Log.v(LOGTAG, "display: w="+d.getWidth()+", h="+d.getHeight());
//        p.width = d.getWidth();
//        p.height= d.getHeight();
//        videoHolder.setLayoutParams(p);
        
        p = mPreview.getLayoutParams();
        p.width = d.getWidth();
        p.height= d.getHeight();
        
        mPreview.setLayoutParams(p);
        
//        CameraUtility.viewToSquare(videoHolder, this);
//        CameraUtility.viewToSquare(mPreview, this);
        

        camera.startPreview();

		
//		if (previewHolder == null) {
//			previewHolder = surfaceView.getHolder();
//		}
//		previewHolder.addCallback(this);
		
		rotateButton = (Button)findViewById(R.id.rotateButton);

		if(Camera.getNumberOfCameras() > 1){
			rotateButton.setOnClickListener(new SwitchCameraClickListener());
		}
		
		//if phone has only one camera, don't show "switch camera" button
		if(Camera.getNumberOfCameras() > 1){
			rotateButton.setVisibility(View.VISIBLE);
		}
		
//		setRedButtonState(BUTTON_RED);
//		redButtonText.setText(R.string.record);
		

	}
	
	private int cameraOrientation;
	private Camera.Size bestSize;
	private Integer currentCameraId = null;
	MediaRecorder recorder;
	
	private static final boolean BUTTON_RED=false;
	private static final boolean BUTTON_YELLOW=true;
	
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	private void setRedButtonState(boolean isDefault) {
		if (isDefault) {
			redButtonCircle.setBackground(getResources().getDrawable(R.drawable.circle_flash));
    		redButtonText.setTextColor(getResources().getColor(R.color.sr_text_rec_button_flash));
		}
		else {
			redButtonCircle.setBackground(getResources().getDrawable(R.drawable.circle));
    		redButtonText.setTextColor(getResources().getColor(R.color.sr_vidcap_ctrl_text));
		}
	}
	
	// "SWITCH CAMERA" button control
	class SwitchCameraClickListener implements Button.OnClickListener {
		
		@Override
		public void onClick(View v) {

		    camera.stopPreview();
			//NB: if you don't release the current camera before switching, you app will crash
		    camera.release();
		    //swap the id of the camera to be used
		    currentCameraId++;
		    if (currentCameraId >= Camera.getNumberOfCameras())
		    	currentCameraId = 0;
//			    if(currentCameraId == Camera.CameraInfo.CAMERA_FACING_BACK){
//			        currentCameraId = Camera.CameraInfo.CAMERA_FACING_FRONT;
//			    }
//			    else {
//			        currentCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
//			    }
		    camera = getCameraInstance();
//		    try {
//		        //this step is critical or preview on new camera will no know where to render to
//		        camera.setPreviewDisplay(previewHolder);
//		    } catch (IOException e) {
//		        e.printStackTrace();
//		        camera.release();
//				camera = null;
//		    }
		    camera.startPreview();
		}
				
	}
	// Camera, Recorder

	private Camera getCameraInstance() {
		Log.i(LOGTAG, "getCameraInstance");
		Camera c = null;
		try {
			if (currentCameraId==null) {
				// get default camera id
				int cameras = Camera.getNumberOfCameras();
				Log.v(LOGTAG, "Cameras: " + cameras);
				currentCameraId = 0;
			}
			
			c = Camera.open(currentCameraId); // attempt to get a Camera instance
			
			// not much difference when is this being set
			cameraOrientation = CameraUtility.getCameraDisplayOrientation(this, currentCameraId, true);
			c.setDisplayOrientation(cameraOrientation);
			
			Log.d(LOGTAG, "cameraOrientation "+cameraOrientation);
			
			Camera.Parameters cp = c.getParameters();

			cp.setRotation(cameraOrientation);
			cp.set("orientation", "portrait");
//			cp.set("rotation", "90");
			cp.set("rotation", cameraOrientation+"");


			bestSize = CameraUtility.getOptimalPreviewSize(768, 768, c);
			Log.i(LOGTAG, "Best preview sizes: " + bestSize.width + ", "
					+ bestSize.height);
			
			cp.setPreviewSize(bestSize.width, bestSize.height);

			cp.setColorEffect(Camera.Parameters.EFFECT_MONO);
			// TOOD:
//			cp.setRecordingHint(true);
			
			List<String> focusModes = cp.getSupportedFocusModes();
			if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
			  // Autofocus mode is supported
				cp.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
			}

			c.setParameters(cp);
		} catch (Exception e) {
			// Camera is not available (in use or does not exist)
			Log.e(LOGTAG, "Camera is not available (in use or does not exist)");
		}
		return c; // returns null if camera is unavailable
	}
	
	
	// SurfaceHolder.Callback

//	@Override
//	public void surfaceDestroyed(SurfaceHolder holder) {
//		camera.stopPreview();
//		camera.release();
//		camera = null;
//	}
//
//	@Override
//	public void surfaceCreated(SurfaceHolder holder) {
//		camera = getCameraInstance();
//
//		try {
//			camera.setPreviewDisplay(holder);
//		} catch (IOException e) {
//			e.printStackTrace();
//			camera.release();
//			camera = null;
//		}
//	}
//
//	@Override
//	public void surfaceChanged(SurfaceHolder holder, int format, int width,
//			int height) {
//		camera.startPreview();
//	}
}

//----------------------------------------------------------------------

/**
 * A simple wrapper around a Camera and a SurfaceView that renders a centered preview of the Camera
 * to the surface. We need to center the SurfaceView because not all devices have cameras that
 * support preview sizes at the same aspect ratio as the device's display.
 */
class Preview1 extends ViewGroup implements SurfaceHolder.Callback {
    private final String TAG = "Preview";

    SurfaceView mSurfaceView;
    SurfaceHolder mHolder;
    Size mPreviewSize;
    List<Size> mSupportedPreviewSizes;
    Camera mCamera;

    Preview1(Context context) {
        super(context);

        mSurfaceView = new SurfaceView(context);
        addView(mSurfaceView);

        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = mSurfaceView.getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        

    }
    
    public void setCamera(Camera camera) {
        mCamera = camera;
        if (mCamera != null) {
        	// msa:
        	mPreviewSize = mCamera.getParameters().getPreviewSize();
            mSupportedPreviewSizes = mCamera.getParameters().getSupportedPreviewSizes();
            requestLayout();
        }
        
    }

    public void switchCamera(Camera camera) {
       setCamera(camera);
       try {
           camera.setPreviewDisplay(mHolder);
       } catch (IOException exception) {
           Log.e(TAG, "IOException caused by setPreviewDisplay()", exception);
       }
       Camera.Parameters parameters = camera.getParameters();
       parameters.setPreviewSize(mPreviewSize.width, mPreviewSize.height);
       requestLayout();

       camera.setParameters(parameters);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // We purposely disregard child measurements because act as a
        // wrapper to a SurfaceView that centers the camera preview instead
        // of stretching it.
        final int width = resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        final int height = resolveSize(getSuggestedMinimumHeight(), heightMeasureSpec);
        
    	Log.v("LOG", "onMeasure:setMeasuredDimension "+width + ", "+height);

        setMeasuredDimension(width, height);

        if (mSupportedPreviewSizes != null) {
        	// TODO: 
//            mPreviewSize = CameraUtility.getOptimalPreviewSize(mSupportedPreviewSizes, width, height);
//        	mPreviewSize = CameraUtility.getBestLargerPreviewSize(width, height, mSupportedPreviewSizes);
//        	mPreviewSize = mCamera.getnew Size(640, 480);
//        	Log.v("LOG", "best prev size: "+mPreviewSize.width + ", "+mPreviewSize.height);
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (changed && getChildCount() > 0) {
            final View child = getChildAt(0);

            final int width = r - l;
            final int height = b - t;

            int previewWidth = width;
            int previewHeight = height;
            if (mPreviewSize != null) {
                previewWidth = mPreviewSize.width;
                previewHeight = mPreviewSize.height;
            }
            
            Log.v("LOG", "w="+width+", h="+height+", pw="+previewWidth+", ph="+previewHeight);

            // Center the child SurfaceView within the parent.
            if (width * previewHeight > height * previewWidth) {
                final int scaledChildWidth = previewWidth * height / previewHeight;
                Log.v("onLayout"," first branch");
                child.layout((width - scaledChildWidth) / 2, 0,
                        (width + scaledChildWidth) / 2, height);

            } else {
                Log.v("onLayout"," second branch");

                final int scaledChildHeight = previewHeight * width / previewWidth;
                child.layout(0, (height - scaledChildHeight) / 2,
                        width, (height + scaledChildHeight) / 2);

                
            }
        }
    }

    @Override
	public void surfaceCreated(SurfaceHolder holder) {
        // The Surface has been created, acquire the camera and tell it where
        // to draw.
        try {
            if (mCamera != null) {
                mCamera.setPreviewDisplay(holder);
            }
        } catch (IOException exception) {
            Log.e(TAG, "IOException caused by setPreviewDisplay()", exception);
        }
    }

    @Override
	public void surfaceDestroyed(SurfaceHolder holder) {
        // Surface will be destroyed when we return, so stop the preview.
        if (mCamera != null) {
            mCamera.stopPreview();
        }
    }

//
//    private Size getOptimalPreviewSize(List<Size> sizes, int w, int h) {
//        final double ASPECT_TOLERANCE = 0.1;
//        double targetRatio = (double) w / h;
//        if (sizes == null) return null;
//
//        Size optimalSize = null;
//        double minDiff = Double.MAX_VALUE;
//
//        int targetHeight = h;
//
//        // Try to find an size match aspect ratio and size
//        for (Size size : sizes) {
//            double ratio = (double) size.width / size.height;
//            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
//            if (Math.abs(size.height - targetHeight) < minDiff) {
//                optimalSize = size;
//                minDiff = Math.abs(size.height - targetHeight);
//            }
//        }
//
//        // Cannot find the one match the aspect ratio, ignore the requirement
//        if (optimalSize == null) {
//            minDiff = Double.MAX_VALUE;
//            for (Size size : sizes) {
//                if (Math.abs(size.height - targetHeight) < minDiff) {
//                    optimalSize = size;
//                    minDiff = Math.abs(size.height - targetHeight);
//                }
//            }
//        }
//        return optimalSize;
//    }

    @Override
	public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
    	Log.v("Preview", "surfaceChanged, camera.startPreview");
        // Now that the size is known, set up the camera parameters and begin
        // the preview.
        Camera.Parameters parameters = mCamera.getParameters();
        parameters.setPreviewSize(mPreviewSize.width, mPreviewSize.height);
        requestLayout();

        mCamera.setParameters(parameters);
        mCamera.startPreview();
    }

}
