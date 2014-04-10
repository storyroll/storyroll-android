package com.storyroll.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager.LayoutParams;
import android.widget.ImageView;

import com.storyroll.R;
import com.storyroll.ui.AspectRatioImageView;
import com.storyroll.util.ImageUtility;
import com.viewpagerindicator.CirclePageIndicator;

public class HelpActivity extends FragmentActivity
{
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);

        getActionBar().hide();
        
        ImageView sliderOverlay = (ImageView)findViewById(R.id.sliderOverlay);
        ImageUtility.sliderAnimateRightToLeft(sliderOverlay);
        
        ImageAdapter ia = new ImageAdapter(this);

        ViewPager vp = (ViewPager) findViewById(R.id.pager);
        vp.setAdapter(ia);
        
        //Bind the title indicator to the adapter
        CirclePageIndicator circleIndicator = (CirclePageIndicator)findViewById(R.id.circles);
        final float density = getResources().getDisplayMetrics().density;
        circleIndicator.setRadius(7 * density);
        circleIndicator.setViewPager(vp);
        

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {

    }
    
	public class ImageAdapter extends PagerAdapter {
		Context context;
		private int[] HepSlideImages = new int[] { R.drawable.help_overlay_1, R.drawable.help_overlay_2,
				R.drawable.help_overlay_3, R.drawable.help_overlay_4 };

		ImageAdapter(Context context) {
			this.context = context;
		}

		@Override
		public int getCount() {
			return HepSlideImages.length;
		}

		@Override
		public boolean isViewFromObject(View view, Object object) {
			return view == ((ImageView) object);
		}

		@Override
		public Object instantiateItem(ViewGroup container, int position) {
			AspectRatioImageView imageView = new AspectRatioImageView(context);
			int padding = 0;
			imageView.setPadding(padding, padding, padding, padding);
			imageView.setScaleType(ImageView.ScaleType.FIT_XY);
			imageView.setImageResource(HepSlideImages[position]);
			
			ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			imageView.setLayoutParams(params);
			imageView.setAdjustViewBounds(true);
			if (position==HepSlideImages.length-1) {
				imageView.setOnTouchListener(new View.OnTouchListener() {
					
					@Override
					public boolean onTouch(View v, MotionEvent event) {
						// next step
						Intent intent = new Intent(getApplicationContext(), TabbedPlaylistActivity.class);

						// TODO
						intent.putExtra("TRIAL", false);
				        startActivity(intent);
						return false;
					}
				});
			}
			((ViewPager) container).addView(imageView, 0);
			return imageView;
		}

		@Override
		public void destroyItem(ViewGroup container, int position, Object object) {
			((ViewPager) container).removeView((ImageView) object);
		}
	}
}