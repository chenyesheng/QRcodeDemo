package com.cys.test.qrcodedemo.qrcode.common;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;

import com.cys.test.qrcodedemo.qrcode.decode.BitmapDecoder;
import com.google.zxing.Result;
import com.google.zxing.client.result.ResultParser;

/**
 * 二维码解码
 */
public class DecodeUtils {

    public static void decodeQCCode(Context context, String filePath, DecodeImageListener decodeImageListener) {
        DecodeFromFileTask decodeTask = new DecodeFromFileTask(context, decodeImageListener);
        decodeTask.execute(filePath);
    }

    public static void decodeQCCode(Context context, Bitmap bitmap, DecodeImageListener decodeImageListener) {
        DecodeFromBitmapTask decodeTask = new DecodeFromBitmapTask(context, decodeImageListener);
        decodeTask.execute(bitmap);
    }

    static class DecodeFromFileTask extends AsyncTask<String, Void, Result> {
        private Context context;
        private DecodeImageListener decodeImageListener;

        public DecodeFromFileTask(Context context, DecodeImageListener decodeImageListener) {
            this.context = context;
            this.decodeImageListener = decodeImageListener;
        }

        @Override
        protected Result doInBackground(String... params) {
            Bitmap img = BitmapUtils.getCompressedBitmap(params[0]);
            BitmapDecoder decoder = new BitmapDecoder(context);
            Result result = decoder.getRawResult(img);
            return result;
        }

        @Override
        protected void onPostExecute(Result result) {
            super.onPostExecute(result);
            if (decodeImageListener != null) {
                if (result == null) {
                    decodeImageListener.fail();
                } else {
                    decodeImageListener.success(ResultParser.parseResult(result).toString());
                }
            }
        }
    }

    static class DecodeFromBitmapTask extends AsyncTask<Bitmap, Void, Result> {
        private Context context;
        private DecodeImageListener decodeImageListener;

        public DecodeFromBitmapTask(Context context, DecodeImageListener decodeImageListener) {
            this.context = context;
            this.decodeImageListener = decodeImageListener;
        }

        @Override
        protected Result doInBackground(Bitmap... params) {
            BitmapDecoder decoder = new BitmapDecoder(context);
            Result result = decoder.getRawResult(params[0]);
            return result;
        }

        @Override
        protected void onPostExecute(Result result) {
            super.onPostExecute(result);
            if (decodeImageListener != null) {
                if (result == null) {
                    decodeImageListener.fail();
                } else {
                    decodeImageListener.success(ResultParser.parseResult(result).toString());
                }
            }
        }
    }

    public interface DecodeImageListener {
        void success(String str);
        void fail();
    }


}
