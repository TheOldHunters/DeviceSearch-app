package com.de.search.base;


import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;


/**
 * base activity
 */

public abstract class BaseActivity extends AppCompatActivity implements View.OnClickListener {

    //Whether the current activity is in the foreground
    protected boolean isForeground = false;
    //Whether to shut down the activity when receiving the message
    protected boolean isClose = true;

    //Be sure to set the layout first, otherwise you won't find the control
    protected int layoutRes;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //Must first onCreate, then create the page, otherwise the adaptation will be useless
        //because you need to load the adaptation, then load the page automatic adaptation;
        //Now if you load the page in the first load adaptation, will not fit, because the page has been loaded;
        super.onCreate(savedInstanceState);
        setContentView(layoutRes);

        //Top bar transparent full screen
        if (Build.VERSION.SDK_INT >= 21) {
            View decorView = getWindow().getDecorView();
            int option = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
            decorView.setSystemUiVisibility(option);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }

        initView();
        initData();
        initListener();

    }

    protected void setView(int layoutRes){
        this.layoutRes = layoutRes;
    }

    /**
     * Look for a view based on its Id
     *
     * @param resId
     * @param <T>
     * @return
     */
    protected <T extends View> T findView(int resId) {
        return (T) super.findViewById(resId);
    }

    //Initialize view
    protected abstract void initView();

    //Initialize data
    protected abstract void initData();

    //Initialize the listener
    protected abstract void initListener();

    //Displays toast. The default is a short display
    protected void showToast(String message) {
        showToast(message, Toast.LENGTH_LONG);
    }

    //Show toast
    protected void showToast(String message, int time) {
        Toast.makeText(this, message, time).show();
    }


    /**
     * Uniform jump Activity
     *
     * @param intent
     */
    protected void startToActivity(Intent intent) {
        startActivity(intent);
        // The first parameter is the animation of the target Activity when it enters
        // and the second parameter is the animation of the current Activity when it exits
    }


    protected void startToActivity(Class cls) {
        startToActivity(new Intent(this, cls));
    }

    @Override
    protected void onResume() {
        super.onResume();
        isForeground = true;

    }

    @Override
    protected void onPause() {
        super.onPause();

    }

    @Override
    protected void onStop() {
        super.onStop();
        isForeground = false;

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onClick(View v) {

    }

    @Override
    public void finish() {
        super.finish();
    }

}
