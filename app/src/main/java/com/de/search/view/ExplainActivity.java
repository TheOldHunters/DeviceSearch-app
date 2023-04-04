package com.de.search.view;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.de.search.R;
import com.de.search.base.BaseActivity;

public class ExplainActivity extends BaseActivity {
    private TextView tvBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setView(R.layout.activity_explain);
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
        tvBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startToActivity(HomeActivity.class);
                finish();
            }
        });
    }

    @Override
    public void onBackPressed() {
        startToActivity(HomeActivity.class);
        finish();
    }
}