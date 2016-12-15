package com.cys.test.qrcodedemo.qrcode.common;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.net.Uri;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class BitmapUtils {

	public static Bitmap decodeSampledBitmapFromResource(Resources res,
			int resId, int reqWidth, int reqHeight) {

		// First decode with inJustDecodeBounds=true to check dimensions
		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeResource(res, resId, options);

		// Calculate inSampleSize
		options.inSampleSize = calculateInSampleSize(options, reqWidth,
				reqHeight);

		// Decode bitmap with inSampleSize set
		options.inJustDecodeBounds = false;
		return BitmapFactory.decodeResource(res, resId, options);
	}

	public static int calculateInSampleSize(BitmapFactory.Options options,
			int reqWidth, int reqHeight) {
		// Raw height and width of image
		final int height = options.outHeight;
		final int width = options.outWidth;
		int inSampleSize = 1;

		if (height > reqHeight || width > reqWidth) {

			// Calculate ratios of height and width to requested height and
			// width
			final int heightRatio = Math.round((float) height
					/ (float) reqHeight);
			final int widthRatio = Math.round((float) width / (float) reqWidth);

			// Choose the smallest ratio as inSampleSize value, this will
			// guarantee
			// a final image with both dimensions larger than or equal to the
			// requested height and width.
			inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
		}

		return inSampleSize;
	}

	public static Bitmap getCompressedBitmap(String path) {
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(path, options);
		options.inSampleSize = calculateInSampleSize(options, 480, 800);
		options.inJustDecodeBounds = false;
		return BitmapFactory.decodeFile(path, options);
	}

	/**
	 * 通过uri获取图片并进行压缩
	 *
	 * @param uri
	 */
	public static Bitmap getBitmapFromUri2(Context ac, Uri uri) {
		InputStream input;
		try {
			input = ac.getContentResolver().openInputStream(uri);

			BitmapFactory.Options onlyBoundsOptions = new BitmapFactory.Options();
			onlyBoundsOptions.inJustDecodeBounds = true;
			onlyBoundsOptions.inDither = true;//optional
			onlyBoundsOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;//optional
			BitmapFactory.decodeStream(input, null, onlyBoundsOptions);
			input.close();
			int originalWidth = onlyBoundsOptions.outWidth;
			int originalHeight = onlyBoundsOptions.outHeight;
			if ((originalWidth == -1) || (originalHeight == -1))
				return null;
			//缩放比。由于是固定比例缩放，只用高或者宽其中一个数据进行计算即可
			int be = 1;//be=1表示不缩放
			if (originalHeight * originalWidth / 1024 / 1024 > 1) {// 大于6M

				//图片分辨率以1080*1920 高分辨率为标准
				float hh = 1280f;
				float ww = 768f;
				if (originalWidth > originalHeight && originalWidth > ww) {//如果宽度大的话根据宽度固定大小缩放
					be = (int) (originalWidth / ww);
				} else if (originalWidth < originalHeight && originalHeight > hh) {//如果高度高的话根据宽度固定大小缩放
					be = (int) (originalHeight / hh);
				}
				if (be <= 0)
					be = 1;
			}
			//比例压缩
			BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
			bitmapOptions.inSampleSize = be;//设置缩放比例
			bitmapOptions.inDither = true;//optional
			bitmapOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;//optional
			input = ac.getContentResolver().openInputStream(uri);
			Bitmap bitmap = BitmapFactory.decodeStream(input, null, bitmapOptions);
			input.close();
			return bitmap;//再进行质量压缩
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
		}
		return null;
	}

	public static Bitmap getVideoThumbnail(String videoPath, int width, int height, int kind) {
		Bitmap bitmap = null;
		// 获取视频的缩略图
		bitmap = ThumbnailUtils.createVideoThumbnail(videoPath, kind);
//		System.out.println("w"+bitmap.getWidth());
//		System.out.println("h"+bitmap.getHeight());
//		bitmap = ThumbnailUtils.extractThumbnail(bitmap, width, height,
//				ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
		return bitmap;
	}
}
