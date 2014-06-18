package co.storyroll.base;


import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ProgressBar;

public class ProgressListActivity extends MenuListActivity
{
	private static final String LOGTAG = "ProgressListAct";
    protected static ProgressBar mLoadingProgressBar = null;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onStart() {
        super.onStart();

        // Custom indefinite ProgressBar
        final ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 20); //Use dp resources
        mLoadingProgressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        mLoadingProgressBar.setIndeterminate(true);
        mLoadingProgressBar.setLayoutParams(lp);
        mLoadingProgressBar.getIndeterminateDrawable().setColorFilter(Color.parseColor("#FF5E48"), PorterDuff.Mode.SRC_IN);
        mLoadingProgressBar.setVisibility(View.VISIBLE);
        final ViewGroup decor = (ViewGroup) getWindow().getDecorView();
        decor.addView(mLoadingProgressBar);
        final ViewTreeObserver vto = decor.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            final View content = findViewById(android.R.id.content);
            @Override
            public void onGlobalLayout() {
                int top = content.getTop();
                //Dont do anything until getTop has a value above 0.
                if (top == 0)
                    return;
//                ViewTreeObserver observer = mLoadingProgressBar.getViewTreeObserver();
//                observer.removeGlobalOnLayoutListener(this);
                mLoadingProgressBar.setY(top-12);
            }
        });
    }

    protected void showProgress(boolean show) {
        if (mLoadingProgressBar!=null) {
            mLoadingProgressBar.setVisibility(show?View.VISIBLE:View.INVISIBLE);
        }
    }
	
}
