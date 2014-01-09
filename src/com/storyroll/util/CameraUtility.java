package com.storyroll.util;

import java.io.File;
import java.io.IOException;
import java.util.List;

import android.app.Activity;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.media.MediaRecorder;
import android.media.MediaRecorder.OnInfoListener;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.View;

public class CameraUtility {
	
	private static final String LOGTAG = "CAMERA";
	private static final String FRAGMENT_FILE_NAME = "new_fragment.mp4";
	public static final int VIDEO_LENGTH = 3000;

	public static int getCameraDisplayOrientation(Activity activity,
			int cameraId, boolean compensate) {
		android.hardware.Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();
		android.hardware.Camera.getCameraInfo(cameraId, info);
		int rotation = activity.getWindowManager().getDefaultDisplay()
				.getRotation();
		int degrees = 0;
		switch (rotation) {
		case Surface.ROTATION_0:
			degrees = 0;
			break;
		case Surface.ROTATION_90:
			degrees = 90;
			break;
		case Surface.ROTATION_180:
			degrees = 180;
			break;
		case Surface.ROTATION_270:
			degrees = 270;
			break;
		}

		int result;
		if (compensate && info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
			result = (info.orientation + degrees) % 360;
			result = (360 - result) % 360; // compensate the mirror
		} else { // back-facing
			result = (info.orientation - degrees + 360) % 360;
		}
		return result;
	}

	public static Camera.Size getOptimalPreviewSize(int width, int height, Camera c) {
		Camera.Size result = null;
//		// containing size?
//		Camera.Size result = getBestLargerPreviewSize(width, height, c);
//		if (result==null) {
//			// large enough resolution not supported, get the closest smaller resolution
			result = getBestSmallerPreviewSize(width, height, c);
//		}
		return result;
	}
	
	// get supported dimensions closest fitting within required dimensions
	public static Camera.Size getBestSmallerPreviewSize(int width, int height, Camera c) {
		return getBestSmallerPreviewSize(width, height, c.getParameters().getSupportedPreviewSizes());
	}
	
	// get supported dimensions closest fitting within required dimensions
	public static Camera.Size getBestSmallerPreviewSize(int width, int height, List<Size> sizes) {
		Camera.Size result = null;

		for (Camera.Size size : sizes) {
			if (size.width <= width && size.height <= height) {
				if (result == null) {
					result = size;
				} else {
					int resultArea = result.width * result.height;
					int newArea = size.width * size.height;

					if (newArea > resultArea) {
						result = size;
					}
				}
			}
		}
		return result;
	}
	
	// tget supported dimensions, containing required dimensions
	public static Camera.Size getBestLargerPreviewSize(int width, int height, List<Size> sizes) {
		Camera.Size result = null;
		for (Camera.Size size : sizes) {
			Log.v("CameraUtility", "camera supported preview size: "+size.width+" x "+size.height);
			if (size.width >= width && size.height >= height) {
				if (result == null) {
					result = size;
				} else {
					int resultArea = result.width * result.height;
					int newArea = size.width * size.height;

					if (newArea < resultArea) {
						result = size;
					}
				}
			}
		}
		return result;
	}
	
	public static Camera.Size getBestLargerPreviewSize(int width, int height, Camera c) {
		Camera.Parameters p = c.getParameters();
		return getBestLargerPreviewSize(width, height, p.getSupportedPreviewSizes());
		
	}
	
    public static Camera.Size getOptimalPreviewSize(List<Size> sizes, int w, int h) {
    	if (sizes==null) return null;

    	Camera.Size result = null;
		
        final double ASPECT_TOLERANCE = 0.2;
        double targetRatio = (double) w / h;

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
    
    public static Camera.Size getOptimalPreviewSize1(int w, int h, Camera c) {
    	Camera.Size result = null;
		Camera.Parameters p = c.getParameters();
		
        return getOptimalPreviewSize(p.getSupportedPreviewSizes(), w, h);
    }
    
//    public void centerSurfaceView(int l, int t, int r, int b) {
//        if (changed && getChildCount() > 0) {
//            final View child = getChildAt(0);
//
//            final int width = r - l;
//            final int height = b - t;
//
//            int previewWidth = width;
//            int previewHeight = height;
//            if (mPreviewSize != null) {
//                previewWidth = mPreviewSize.width;
//                previewHeight = mPreviewSize.height;
//            }
//
//            // Center the child SurfaceView within the parent.
//            if (width * previewHeight > height * previewWidth) {
//                final int scaledChildWidth = previewWidth * height / previewHeight;
//                child.layout((width - scaledChildWidth) / 2, 0,
//                        (width + scaledChildWidth) / 2, height);
//            } else {
//                final int scaledChildHeight = previewHeight * width / previewWidth;
//                child.layout(0, (height - scaledChildHeight) / 2,
//                        width, (height + scaledChildHeight) / 2);
//            }
//        }
//    }
    
	public static void viewToSquare(View view, int width) {
		android.view.ViewGroup.LayoutParams lp = view.getLayoutParams();
    	lp.width = width;
    	lp.height = width;
    	view.setLayoutParams(lp);
	}

	public static void viewToSquare(View view, Activity a) {
		viewToSquare(view, a.getWindowManager().getDefaultDisplay().getWidth());
	}

//	public static MediaRecorder prepareRecorder(Camera camera, int currentCameraId, SurfaceHolder previewHolder, Activity activity) 
//	throws IllegalStateException, IOException {
//		camera.unlock();
//
//		MediaRecorder recorder = new MediaRecorder();
//		recorder.setCamera(camera);
//
//		recorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
//		recorder.setVideoSource(MediaRecorder.VideoSource.DEFAULT);
//
//		// recorder.setProfile(camcorderProfile);
////		recorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));
//		
//		// best is to setvideosize after setoutput format but before
//		// setvideoencoder
//		recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
//		recorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
//
////		recorder.setVideoSize(bestSize.width, bestSize.height);
////		recorder.setVideoSize(1280, 720);
//		Size bestSize = CameraUtility.getOptimalPreviewSize(640, 640, camera);
//		recorder.setVideoSize(bestSize.width, bestSize.height);
//
////		recorder.setVideoEncoder(MediaRecorder.VideoEncoder.DEFAULT);
//		recorder.setVideoEncoder(MediaRecorder.VideoEncoder.MPEG_4_SP);
//
//		recorder.setVideoFrameRate(30);
//	
//		recorder.setOutputFile(getNewFragmentFilePath());
//		recorder.setMaxDuration((int) VIDEO_LENGTH);
//		recorder.setOnInfoListener((OnInfoListener) activity);
//
//        // Tags the video with an appropriate (90¡) angle in order to tell the phone how to display it
//		// the compensation parameter here is off, in order to work with both front and back facing cameras
//		int orientationHint = CameraUtility.getCameraDisplayOrientation(activity, currentCameraId, false);
//        recorder.setOrientationHint(orientationHint);
//        
//		recorder.setPreviewDisplay(previewHolder.getSurface());
//
//		recorder.prepare();
//		Log.v(LOGTAG, "Recorder prepared");
//		
//		return recorder;
//	}
	
	public static String getNewFragmentFilePath() {
		return AppUtility.getAppWorkingDir() + File.separator+FRAGMENT_FILE_NAME;
	}

}
