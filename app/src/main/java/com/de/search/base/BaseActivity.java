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

    //当前activity是否位于前台
    protected boolean isForeground = false;
    //收到关闭activity消息时是否关闭
    protected boolean isClose = true;

    // 一定要先设置布局，不然找不到控件
    protected int layoutRes;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 一定要先onCreate，再创建页面，不然适配就会没用，因为要加载适配的东西，再加载页面自动适配；
        // 现在如果加载页面先在加载适配，就会适配不了，因为页面已经加载完了；
        super.onCreate(savedInstanceState);
        setContentView(layoutRes);

        // 顶头栏透明全屏
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
     * 根据Id查找view
     *
     * @param resId
     * @param <T>
     * @return
     */
    protected <T extends View> T findView(int resId) {
        return (T) super.findViewById(resId);
    }

    //初始化view
    protected abstract void initView();

    //初始化数据
    protected abstract void initData();

    //初始化监听器
    protected abstract void initListener();

    //显示toast，默认是短时间显示
    protected void showToast(String message) {
        showToast(message, Toast.LENGTH_LONG);
    }

    //显示toast
    protected void showToast(String message, int time) {
        Toast.makeText(this, message, time).show();
    }


    /**
     * 统一跳转Activity
     *
     * @param intent
     */
    protected void startToActivity(Intent intent) {
        startActivity(intent);
        // 第一个参数是目标Activity进入时的动画，第二个参数是当前Activity退出时的动画
//        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
    }

    /**
     * 统一跳转Activity
     *
     * @param cls
     */
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
        //  overridePendingTransition(android.R.anim.slide_in_left, R.anim.activity_out);
    }



}
