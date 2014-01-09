package com.storyroll.ui;

import java.util.List;

import com.storyroll.util.CameraUtility;

import android.content.Context;
import android.hardware.Camera.Size;
import android.util.AttributeSet;
import android.view.SurfaceView;
import android.view.View;
import android.widget.RelativeLayout;

public class VideoLayout extends RelativeLayout {

	public VideoLayout(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
	}

	public VideoLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
	}

	public VideoLayout(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		// TODO Auto-generated constructor stub
	}

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
            final View child = mSurfaceView;

            final int width = r - l;
            final int height = b - t;

            int previewWidth = width;
            int previewHeight = height;
            if (mPreviewSize != null) {
                previewWidth = mPreviewSize.width;
                previewHeight = mPreviewSize.height;
            }

            // Center the child SurfaceView within the parent.
            if (width * previewHeight < height * previewWidth) {
                final int scaledChildWidth = previewWidth * height / previewHeight;
                child.layout((width - scaledChildWidth) / 2, 0,
                        (width + scaledChildWidth) / 2, height);
            } else {
                final int scaledChildHeight = previewHeight * width / previewWidth;
                child.layout(0, (height - scaledChildHeight) / 2,
                        width, (height + scaledChildHeight) / 2);
            }
    }
    
    Size mPreviewSize;
    List<Size> mSupportedPreviewSizes;
    SurfaceView mSurfaceView;
    
    public void setSurfaceView(SurfaceView mSurfaceView) {
		this.mSurfaceView = mSurfaceView;
	}

	public void setSupportedPreviewSizes(List<Size> mSupportedPreviewSizes) {
		this.mSupportedPreviewSizes = mSupportedPreviewSizes;
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
            mPreviewSize = CameraUtility.getBestSmallerPreviewSize(width, height, mSupportedPreviewSizes);
        }
    }
}
