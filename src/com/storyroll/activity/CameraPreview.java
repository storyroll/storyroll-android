/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.storyroll.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Size;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import java.io.IOException;
import java.util.List;

import com.storyroll.util.CameraUtility;

// Need the following import to get access to the app resources, since this
// class is in a sub-package.
//import com.example.android.apis.R;

// ----------------------------------------------------------------------

public class CameraPreview extends Activity {
    private static final String LOGTAG = null;
	private Preview mPreview;
    Camera mCamera;
    int numberOfCameras;
    int cameraCurrentlyLocked;

    // The first rear facing camera
    int defaultCameraId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Hide the window title.
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // Create a RelativeLayout container that will hold a SurfaceView,
        // and set it as the content of our activity.
        mPreview = new Preview(this);
        setContentView(mPreview);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // Find the total number of cameras available
        numberOfCameras = Camera.getNumberOfCameras();

        // Find the ID of the default camera
        CameraInfo cameraInfo = new CameraInfo();
            for (int i = 0; i < numberOfCameras; i++) {
                Camera.getCameraInfo(i, cameraInfo);
                if (cameraInfo.facing == CameraInfo.CAMERA_FACING_BACK) {
                    defaultCameraId = i;
                }
            }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Open the default i.e. the first rear facing camera.
        mCamera = Camera.open();
        int cameraOrientation = CameraUtility.getCameraDisplayOrientation(CameraPreview.this, 0, true);
        mCamera.setDisplayOrientation(cameraOrientation);
        cameraCurrentlyLocked = defaultCameraId;
        mPreview.setCamera(mCamera);
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Because the Camera object is a shared resource, it's very
        // important to release it when the activity is paused.
        if (mCamera != null) {
            mPreview.setCamera(null);
            mCamera.release();
            mCamera = null;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate our menu which can gather user input for switching camera
        MenuInflater inflater = getMenuInflater();
//        inflater.inflate(R.menu.camera_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
//        switch (item.getItemId()) {
//        case R.id.switch_cam:
//            // check for availability of multiple cameras
//            if (numberOfCameras == 1) {
//                AlertDialog.Builder builder = new AlertDialog.Builder(this);
//                builder.setMessage(this.getString(R.string.camera_alert))
//                       .setNeutralButton("Close", null);
//                AlertDialog alert = builder.create();
//                alert.show();
//                return true;
//            }
//
//            // OK, we have multiple cameras.
//            // Release this camera -> cameraCurrentlyLocked
//            if (mCamera != null) {
//                mCamera.stopPreview();
//                mPreview.setCamera(null);
//                mCamera.release();
//                mCamera = null;
//            }
//
//            // Acquire the next camera and request Preview to reconfigure
//            // parameters.
//            mCamera = Camera
//                    .open((cameraCurrentlyLocked + 1) % numberOfCameras);
//            cameraCurrentlyLocked = (cameraCurrentlyLocked + 1)
//                    % numberOfCameras;
//            mPreview.switchCamera(mCamera);
//
//            // Start the preview
//            mCamera.startPreview();
//            return true;
//        default:
//            return super.onOptionsItemSelected(item);
//        }
    	return super.onOptionsItemSelected(item);
    }


private Camera getCameraInstance() {
	Log.i(LOGTAG, "getCameraInstance");
	Camera c = null;
	try {

		
		c = Camera.open(0); // attempt to get a Camera instance
		
		// not much difference when is this being set
		int cameraOrientation = CameraUtility.getCameraDisplayOrientation(CameraPreview.this, 0, true);
		c.setDisplayOrientation(cameraOrientation);
		
		Log.d(LOGTAG, "cameraOrientation "+cameraOrientation);
		
		Camera.Parameters cp = c.getParameters();

		cp.setRotation(cameraOrientation);
		cp.set("orientation", "portrait");
//		cp.set("rotation", "90");
		cp.set("rotation", cameraOrientation+"");


		Size bestSize = CameraUtility.getOptimalPreviewSize(640, 640, c);
		Log.i(LOGTAG, "Best preview sizes: " + bestSize.width + ", "
				+ bestSize.height);
		
		cp.setPreviewSize(bestSize.width, bestSize.height);

		cp.setColorEffect(Camera.Parameters.EFFECT_MONO);
		// TOOD:
//		cp.setRecordingHint(true);
		
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
}
// ----------------------------------------------------------------------

/**
 * A simple wrapper around a Camera and a SurfaceView that renders a centered preview of the Camera
 * to the surface. We need to center the SurfaceView because not all devices have cameras that
 * support preview sizes at the same aspect ratio as the device's display.
 */
class Preview extends ViewGroup implements SurfaceHolder.Callback {
    private static final String LOGTAG = null;

	private final String TAG = "Preview";

    SurfaceView mSurfaceView;
    SurfaceHolder mHolder;
    Size mPreviewSize;
    List<Size> mSupportedPreviewSizes;
    Camera mCamera;

    Preview(Context context) {
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
        setMeasuredDimension(width, height);

        if (mSupportedPreviewSizes != null) {
//            mPreviewSize = CameraUtility.getOptimalPreviewSize(mSupportedPreviewSizes, width, height);
          mPreviewSize = CameraUtility.getOptimalPreviewSize(mSupportedPreviewSizes, 480, 480);

        	if (mPreviewSize!=null) {
//	            mPreviewSize.width = 640;
//	            mPreviewSize.width = 480;
	            Log.v(LOGTAG, "optimal preview size: "+mPreviewSize.width+" x "+mPreviewSize.height);

            }

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
            
//            if (mPreviewSize != null) {
//                previewWidth = mPreviewSize.width;
//                previewHeight = mPreviewSize.height;
//            }
            // in portrait, switch h & w?
//	          if (mPreviewSize != null) {
//	          previewWidth = mPreviewSize.height;
//	          previewHeight = mPreviewSize.width;
//	          }
	          
            // Center the child SurfaceView within the parent.
            if (width * previewHeight > height * previewWidth) {
                final int scaledChildWidth = previewWidth * height / previewHeight;
                child.layout((width - scaledChildWidth) / 2, 0,
                        (width + scaledChildWidth) / 2, height);
            } else {
                final int scaledChildHeight = previewHeight * width / previewWidth;
                child.layout(0, (height - scaledChildHeight) / 2,
                        width, (height + scaledChildHeight) / 2);
            }
        }
    }

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

    public void surfaceDestroyed(SurfaceHolder holder) {
        // Surface will be destroyed when we return, so stop the preview.
        if (mCamera != null) {
            mCamera.stopPreview();
        }
    }


    private Size getOptimalPreviewSize(List<Size> sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) w / h;
        if (sizes == null) return null;

        Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        // Try to find an size match aspect ratio and size
        for (Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        // Cannot find the one match the aspect ratio, ignore the requirement
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        // Now that the size is known, set up the camera parameters and begin
        // the preview.
        Camera.Parameters parameters = mCamera.getParameters();
        parameters.setPreviewSize(mPreviewSize.width, mPreviewSize.height);
        requestLayout();

        mCamera.setParameters(parameters);
        mCamera.startPreview();
    }

	
}
