package com.de.search.view;

import android.os.Bundle;
import android.widget.TextView;

import com.de.search.R;
import com.de.search.base.BaseActivity;

//Share a downloadable QR code with your friends
public class DownloadActivity extends BaseActivity {

    private TextView tvBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setView(R.layout.activity_download);
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void initView() {
        tvBack = findViewById(R.id.tv_back);
    }

    @Override
    protected void initData() {
        tvBack.setOnClickListener(view -> {
            startToActivity(HomeActivity.class);
            finish();
        });
    }

    @Override
    protected void initListener() {
    }

    @Override
    public void onBackPressed() {
        startToActivity(HomeActivity.class);
        finish();
    }
}