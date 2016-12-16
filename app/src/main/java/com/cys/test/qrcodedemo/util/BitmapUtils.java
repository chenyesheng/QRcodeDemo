package com.cys.test.qrcodedemo.util;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by cys on 2016/12/16.
 */

public class BitmapUtils {

    /**
     * 把Bitmap保存成File(并压缩)
     * String dirs:存储目录完整地址
     * String saveFileName:保存文件名
     */
    public static File bitmapToFile(Bitmap bitmap, String dirs,
                                    String saveFileName, Context context) {
        File file = new File(dirs);
        if (!file.exists()) {
            file.mkdirs();
        }
        File saveFile = new File(file.getAbsolutePath(), saveFileName);
        try {
            FileOutputStream out = new FileOutputStream(saveFile);
            bitmap.compress(Bitmap.CompressFormat.PNG, 60, out);// (0-100)压缩文件
            out.close();
            Toast.makeText(context, "保存成功:" + dirs + saveFileName, Toast.LENGTH_SHORT).show();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        if (context != null) {// 通知系统更新图库
            Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            Uri uri = Uri.fromFile(saveFile);
            intent.setData(uri);
            context.sendBroadcast(intent);
        }
        return saveFile;
    }
}
