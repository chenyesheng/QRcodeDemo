package com.cys.test.qrcodedemo;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.cys.test.qrcodedemo.qrcode.CaptureActivity;

public class MainActivity extends AppCompatActivity {

    private Button btn_generate, btn_scan;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btn_generate = (Button) findViewById(R.id.btn_generate);
        btn_scan = (Button) findViewById(R.id.btn_scan);
        btn_generate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
        btn_scan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, CaptureActivity.class);
                startActivity(intent);
            }
        });
    }
}
