package com.de.search.view;

import android.os.Bundle;
import android.widget.TextView;

import com.de.search.R;
import com.de.search.base.BaseActivity;

public class IntroductionActivity extends BaseActivity {
    private TextView tvBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setView(R.layout.activity_introduction);
        super.onCreate(savedInstanceState);

    }

    @Override
    protected void initView() {
        tvBack = findViewById(R.id.tv_back);

    }

    @Override
    protected void initData() {

    }

    @Override
    protected void initListener() {
        tvBack.setOnClickListener(view -> {
            startToActivity(HomeActivity.class);
            finish();
        });
    }

    @Override
    public void onBackPressed() {
        startToActivity(HomeActivity.class);
        finish();
    }
}