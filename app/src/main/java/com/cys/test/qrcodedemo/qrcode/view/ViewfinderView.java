package com.cys.test.qrcodedemo.qrcode.view;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.View;

import com.cys.test.qrcodedemo.R;
import com.cys.test.qrcodedemo.qrcode.camera.CameraManager;
import com.cys.test.qrcodedemo.util.DensityUtils;
import com.google.zxing.ResultPoint;

import java.util.ArrayList;
import java.util.List;

/**
 * This view is overlaid on top of the camera preview. It adds the viewfinder
 * rectangle and partial transparency outside it, as well as the laser scanner
 * animation and result points.
 * 
 * <br/>
 * <br/>
 * 该视图是覆盖在相机的预览视图之上的一层视图。扫描区构成原理，其实是在预览视图上画四块遮罩层，
 * 中间留下的部分保持透明，并画上一条激光线，实际上该线条就是展示而已，与扫描功能没有任何关系。
 * 
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class ViewfinderView extends View {

	private static final String TAG = "log";
	
	private static final int OPAQUE = 0xFF;

	/**
	 * 四个绿色边角对应的长度
	 */
	private int ScreenRate=20;

	/**
	 * 四个绿色边角对应的宽度
	 */
	private int CORNER_WIDTH = 4;
	/**
	 * 四个白线的宽度
	 */
	private int WHITE_WIDTH = 2;
	/**
	 * 扫描框中的中间线的宽度
	 */
	private int MIDDLE_LINE_WIDTH = 3;

	/**
	 * 扫描框中的中间线的与扫描框左右的间隙
	 */
	private int MIDDLE_LINE_PADDING = 3;

	/**
	 * 中间那条线每次刷新移动的距离
	 */
	private int SPEEN_DISTANCE = 1;
	/**
	 * 中间扫码线扫过的距离
	 */
	private static int SPEEN_DISTANCE_TOTAL = 0;

	
	/**
	 * 字体大小
	 */
	private int TEXT_SIZE = 16;
	/**
	 * 字体距离扫描框下面的距离
	 */
	private int TEXT_PADDING_TOP = 30;

	/**
	 * 画笔对象的引用
	 */
	private Paint paint;

	/**
	 * 中间滑动线的最顶端位置
	 */
	private int slideTop;

	/**
	 * 中间滑动线的最底端位置
	 */
	private int slideBottom;

	/**
	 * 将扫描的二维码拍下来，这里没有这个功能，暂时不考虑
	 */
	private Bitmap resultBitmap;
	private final int maskColor;
	private final int resultColor;

	private final int resultPointColor;
	private List<ResultPoint> possibleResultPoints;
	private List<ResultPoint> lastPossibleResultPoints;

	boolean isFirst;

	private CameraManager cameraManager;
	
	private static final int MAX_RESULT_POINTS = 20;
	
	private Context context;

	// This constructor is used when the class is built from an XML resource.
	public ViewfinderView(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.context=context;
		ScreenRate = DensityUtils.dp2px(context, ScreenRate);
		CORNER_WIDTH= DensityUtils.dp2px(context, CORNER_WIDTH);
		WHITE_WIDTH=DensityUtils.dp2px(context, WHITE_WIDTH);
		MIDDLE_LINE_WIDTH=DensityUtils.dp2px(context, MIDDLE_LINE_WIDTH);
		MIDDLE_LINE_PADDING=DensityUtils.dp2px(context, MIDDLE_LINE_PADDING);
		SPEEN_DISTANCE=DensityUtils.dp2px(context, SPEEN_DISTANCE);
		TEXT_PADDING_TOP=DensityUtils.dp2px(context, TEXT_PADDING_TOP);
		TEXT_SIZE=DensityUtils.sp2px(context, TEXT_SIZE);
		

		paint = new Paint(Paint.ANTI_ALIAS_FLAG);
		Resources resources = getResources();
		maskColor = resources.getColor(R.color.viewfinder_mask);
		resultColor = resources.getColor(R.color.result_view);

		resultPointColor = resources.getColor(R.color.possible_result_points);
		possibleResultPoints = new ArrayList<ResultPoint>(5);
		lastPossibleResultPoints = null;
		
		SPEEN_DISTANCE_TOTAL=0;
		
		mbitmap=BitmapFactory.decodeResource(getResources(),
				R.drawable.barcode_laser_line);
	}

	public void setCameraManager(CameraManager cameraManager) {
		this.cameraManager = cameraManager;
	}

	private Rect frame;
	private Bitmap mbitmap=null;
	private Rect lineRect=null;
	
	@Override
	public void onDraw(Canvas canvas) {
		if (cameraManager == null) {
			return; // not ready yet, early draw before done configuring
		}
		frame = cameraManager.getFramingRect();
		if (frame == null) {
			return;
		}

		// 绘制遮掩层
		drawCover(canvas, frame);

		if (resultBitmap != null) { // 绘制扫描结果的图
			// Draw the opaque result bitmap over the scanning rectangle
			paint.setAlpha(0xA0);
			canvas.drawBitmap(resultBitmap, null, frame, paint);
		} else {

			// 画扫描框边上的角
			drawRectEdges(canvas, frame);

			// 绘制矩形四条线
			drawRectLine(canvas, frame);

			// 绘制扫描线
			drawScanningLine(canvas, frame);
			
			//画扫描框下面的字
			paint.setColor(Color.WHITE);
			paint.setTextSize(TEXT_SIZE);
			paint.setAlpha(OPAQUE);
			paint.setTextAlign(Paint.Align.CENTER);
			paint.setTypeface(Typeface.create("System", Typeface.NORMAL));
			canvas.drawText(getResources().getString(R.string.scan_text),canvas.getWidth()/2, frame.top - TEXT_PADDING_TOP, paint);
			
			invalidateView(frame);
		}
	}
	
	private void invalidateView(Rect frame){
		
		if(Looper.getMainLooper()==Looper.myLooper()){
			invalidate(frame.left, frame.top, frame.right, frame.bottom);
		}else{
			postInvalidate(frame.left, frame.top, frame.right, frame.bottom);
		}
		
	}

	
	/**
	 * 绘制扫描线
	 * 
	 * @param canvas
	 * @param frame
	 *            扫描框
	 */
	private void drawScanningLine(Canvas canvas, Rect frame) {
		
		// 初始化中间线滑动的最上边和最下边
		if (!isFirst) {
			isFirst = true;
			slideTop = frame.top;
			slideBottom = frame.bottom;
		}
		//绘制中间的扫描线,每次刷新界面，中间的线往下移动SPEEN_DISTANCE
		slideTop += SPEEN_DISTANCE;
		if(slideTop >= frame.bottom){
			slideTop = frame.top;
			SPEEN_DISTANCE_TOTAL=0;
		}
		SPEEN_DISTANCE_TOTAL+=SPEEN_DISTANCE;
		
		if(mbitmap.getHeight()> SPEEN_DISTANCE_TOTAL){
			lineRect = new Rect(frame.left, frame.top, frame.right, frame.top+SPEEN_DISTANCE_TOTAL);
			canvas.drawBitmap(mbitmap, null,lineRect, paint);
		}else{
			lineRect = new Rect(frame.left, slideTop + MIDDLE_LINE_WIDTH-mbitmap.getHeight(), frame.right, slideTop + MIDDLE_LINE_WIDTH);
			canvas.drawBitmap(mbitmap, null,lineRect, paint);
		}
		
	}

	/**
	 * 绘制遮掩层
	 * 
	 * @param canvas
	 * @param frame
	 */
	private void drawCover(Canvas canvas, Rect frame) {

		// 获取屏幕的宽和高
		int width = canvas.getWidth();
		int height = canvas.getHeight();

		// Draw the exterior (i.e. outside the framing rect) darkened
		paint.setColor(resultBitmap != null ? resultColor : maskColor);

		// 画出扫描框外面的阴影部分，共四个部分，扫描框的上面到屏幕上面，扫描框的下面到屏幕下面
		// 扫描框的左边面到屏幕左边，扫描框的右边到屏幕右边
		canvas.drawRect(0, 0, width, frame.top, paint);
		canvas.drawRect(0, frame.top, frame.left, frame.bottom + 1, paint);
		canvas.drawRect(frame.right + 1, frame.top, width, frame.bottom + 1,
				paint);
		canvas.drawRect(0, frame.bottom + 1, width, height, paint);

	}

	/**
	 * 描绘方形的四个角
	 * 
	 * @param canvas
	 * @param frame
	 */
	private void drawRectEdges(Canvas canvas, Rect frame) {

		// 画扫描框边上的角，总共8个部分
		paint.setColor(Color.WHITE);
		paint.setAlpha(OPAQUE);
		canvas.drawRect(frame.left - CORNER_WIDTH, frame.top - CORNER_WIDTH,
				frame.left + ScreenRate - CORNER_WIDTH, frame.top, paint);
		canvas.drawRect(frame.left - CORNER_WIDTH, frame.top - CORNER_WIDTH,
				frame.left, frame.top + ScreenRate - CORNER_WIDTH, paint);

		canvas.drawRect(frame.right + CORNER_WIDTH - ScreenRate, frame.top
				- CORNER_WIDTH, frame.right + CORNER_WIDTH, frame.top, paint);
		canvas.drawRect(frame.right, frame.top - CORNER_WIDTH, frame.right
				+ CORNER_WIDTH, frame.top + ScreenRate - CORNER_WIDTH, paint);

		canvas.drawRect(frame.left - CORNER_WIDTH, frame.bottom + CORNER_WIDTH
				- ScreenRate, frame.left, frame.bottom + CORNER_WIDTH, paint);
		canvas.drawRect(frame.left - CORNER_WIDTH, frame.bottom, frame.left
				- CORNER_WIDTH + ScreenRate, frame.bottom + CORNER_WIDTH, paint);

		canvas.drawRect(frame.right - ScreenRate + CORNER_WIDTH, frame.bottom,
				frame.right + CORNER_WIDTH, frame.bottom + CORNER_WIDTH, paint);
		canvas.drawRect(frame.right, frame.bottom - ScreenRate + CORNER_WIDTH,
				frame.right + CORNER_WIDTH, frame.bottom + CORNER_WIDTH, paint);

	}

	/**
	 * 绘制矩形的四条边
	 * 
	 * @param canvas
	 * @param frame
	 */
	private void drawRectLine(Canvas canvas, Rect frame) {

		// 绘制四根白线
		paint.setColor(Color.WHITE);
		paint.setAlpha(OPAQUE);
		// 左边白线
		canvas.drawRect(frame.left - WHITE_WIDTH, frame.top - WHITE_WIDTH,
				frame.left, frame.bottom + WHITE_WIDTH, paint);
		// 上边白线
		canvas.drawRect(frame.left - WHITE_WIDTH, frame.top - WHITE_WIDTH,
				frame.right + WHITE_WIDTH, frame.top, paint);
		// 右边白线
		canvas.drawRect(frame.right, frame.top - WHITE_WIDTH, frame.right
				+ WHITE_WIDTH, frame.bottom + WHITE_WIDTH, paint);
		canvas.drawRect(frame.left - WHITE_WIDTH, frame.bottom, frame.right
				+ WHITE_WIDTH, frame.bottom + WHITE_WIDTH, paint);

	}

	public void drawViewfinder() {
		Bitmap resultBitmap = this.resultBitmap;
		this.resultBitmap = null;
		if (resultBitmap != null) {
			resultBitmap.recycle();
		}
		invalidate();
	}
	

	/**
	 * Draw a bitmap with the result points highlighted instead of the live
	 * scanning display.
	 * 
	 * @param barcode
	 *            An image of the decoded barcode.
	 */
	public void drawResultBitmap(Bitmap barcode) {
		resultBitmap = barcode;
		invalidate();
	}

	public void addPossibleResultPoint(ResultPoint point) {
		
		List<ResultPoint> points = possibleResultPoints;
		synchronized (points) {
			points.add(point);
			int size = points.size();
			if (size > MAX_RESULT_POINTS) {
				// trim it
				points.subList(0, size - MAX_RESULT_POINTS / 2).clear();
			}
		}
	}


}
