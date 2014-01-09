package com.storyroll.ui;

import com.storyroll.R;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.FrameLayout;

public class BottomPane extends FrameLayout {
	// compensate for unknown out of square element size(s)
	int mSubstractHeight = 0;

	public BottomPane(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
	}

    public BottomPane(Context context, AttributeSet attrs) {
		super(context, attrs);
		TypedArray a = context.getTheme().obtainStyledAttributes(
		        attrs,
		        R.styleable.BottomPane,
		        0, 0);	
	   try {
		   mSubstractHeight = a.getDimensionPixelOffset(R.styleable.BottomPane_substractHeight, 0);
	   } finally {
	       a.recycle();
	   }	
    }

	public BottomPane(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		TypedArray a = context.getTheme().obtainStyledAttributes(
		        attrs,
		        R.styleable.BottomPane,
		        0, 0);	
	   try {
		   mSubstractHeight = a.getDimensionPixelOffset(R.styleable.BottomPane_substractHeight, 0);
	   } finally {
	       a.recycle();
	   }
	}

	@Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    	int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        super.onMeasure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(height-width-mSubstractHeight, MeasureSpec.EXACTLY));
            // or you can use this if you want the square to use height as it basis
            // super.onMeasure(heightMeasureSpec, heightMeasureSpec); 
    }
}
