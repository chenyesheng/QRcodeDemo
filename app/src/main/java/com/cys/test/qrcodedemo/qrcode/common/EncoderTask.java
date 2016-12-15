package com.cys.test.qrcodedemo.qrcode.common;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.widget.ImageView;

import java.lang.ref.WeakReference;

/**
 * 生成二维码
 */
public class EncoderTask extends AsyncTask<String, Void, Bitmap> {
    private WeakReference<ImageView> imageViewWeakReference;
    private Encoder encoder;

    public EncoderTask(ImageView imageView) {
        imageViewWeakReference = new WeakReference<ImageView>(imageView);
        encoder = new Encoder.Builder()
                .setOutputBitmapPadding(0)
                .setOutputBitmapWidth(350)
                .setOutputBitmapHeight(350)
                .build();
    }

    @Override
    protected Bitmap doInBackground(String... params) {
        return encoder.encode(params[0]);
    }

    @Override
    protected void onPostExecute(Bitmap bitmap) {
        if (imageViewWeakReference.get() != null) {
            imageViewWeakReference.get().setImageBitmap(bitmap);
        }
    }
}
