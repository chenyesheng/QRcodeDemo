package com.cys.test.qrcodedemo;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import com.cys.test.qrcodedemo.qrcode.CaptureActivity;
import com.cys.test.qrcodedemo.qrcode.common.EncoderTask;
import com.cys.test.qrcodedemo.util.BitmapUtils;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private EditText et_content;
    private Button btn_generate, btn_scan, btn_save;
    private ImageView iv_qrcode;
    private Bitmap codeBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        et_content = (EditText) findViewById(R.id.et_content);
        iv_qrcode = (ImageView) findViewById(R.id.iv_qrcode);
        btn_generate = (Button) findViewById(R.id.btn_generate);
        btn_scan = (Button) findViewById(R.id.btn_scan);
        btn_save = (Button) findViewById(R.id.btn_save);
        btn_generate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new EncoderTask(iv_qrcode).execute(et_content.getText().toString().trim());
            }
        });
        btn_scan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, CaptureActivity.class);
                startActivity(intent);
            }
        });
        btn_save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                codeBitmap = ((BitmapDrawable) iv_qrcode.getDrawable()).getBitmap();
                BitmapUtils.bitmapToFile(codeBitmap, getExternalCacheDir().getPath(), "QRcode-" + System.currentTimeMillis() + ".png", MainActivity.this);
            }
        });
    }

}
