package com.cys.test.qrcodedemo.qrcode;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.provider.MediaStore.MediaColumns;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.cys.test.qrcodedemo.R;
import com.cys.test.qrcodedemo.qrcode.camera.CameraManager;
import com.cys.test.qrcodedemo.qrcode.common.BitmapUtils;
import com.cys.test.qrcodedemo.qrcode.decode.BitmapDecoder;
import com.cys.test.qrcodedemo.qrcode.decode.CaptureActivityHandler;
import com.cys.test.qrcodedemo.qrcode.view.ViewfinderView;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.DecodeHintType;
import com.google.zxing.Result;
import com.google.zxing.client.result.ResultParser;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Map;

/**
 * This activity opens the camera and does the actual scanning on a background
 * thread. It draws a viewfinder to help the user place the barcode correctly,
 * shows feedback as the image processing is happening, and then overlays the
 * results when a scan is successful.
 * <p/>
 * 此Activity所做的事： 1.开启camera，在后台独立线程中完成扫描任务；
 * 2.绘制了一个扫描区（viewfinder）来帮助用户将条码置于其中以准确扫描； 3.扫描成功后会将扫描结果展示在界面上。
 *
 * @author dswitkin@google.com (Daniel Switkin)
 * @author Sean Owen
 */
public final class CaptureActivity extends Activity implements
        SurfaceHolder.Callback {

    private static final String TAG = CaptureActivity.class.getSimpleName();

    private static final int REQUEST_CODE = 100;

    private static final int PARSE_BARCODE_FAIL = 300;
    private static final int PARSE_BARCODE_SUC = 200;

    /**
     * 是否有预览
     */
    private boolean hasSurface;

    /**
     * 活动监控器。如果手机没有连接电源线，那么当相机开启后如果一直处于不被使用状态则该服务会将当前activity关闭。
     * 活动监控器全程监控扫描活跃状态，与CaptureActivity生命周期相同.每一次扫描过后都会重置该监控，即重新倒计时。
     */
    private InactivityTimer inactivityTimer;

    /**
     * 声音震动管理器。如果扫描成功后可以播放一段音频，也可以震动提醒，可以通过配置来决定扫描成功后的行为。
     */
    private BeepManager beepManager;

    /**
     * 闪光灯调节器。自动检测环境光线强弱并决定是否开启闪光灯
     */
    private AmbientLightManager ambientLightManager;

    private CameraManager cameraManager;
    /**
     * 扫描区域
     */
    private ViewfinderView viewfinderView;

    private CaptureActivityHandler handler;

    private Result lastResult;

    private boolean isFlashlightOpen;

    /**
     * 【辅助解码的参数(用作MultiFormatReader的参数)】 编码类型，该参数告诉扫描器采用何种编码方式解码，即EAN-13，QR
     * Code等等 对应于DecodeHintType.POSSIBLE_FORMATS类型
     * 参考DecodeThread构造函数中如下代码：hints.put(DecodeHintType.POSSIBLE_FORMATS,
     * decodeFormats);
     */
    private Collection<BarcodeFormat> decodeFormats;

    /**
     * 【辅助解码的参数(用作MultiFormatReader的参数)】 该参数最终会传入MultiFormatReader，
     * 上面的decodeFormats和characterSet最终会先加入到decodeHints中 最终被设置到MultiFormatReader中
     * 参考DecodeHandler构造器中如下代码：multiFormatReader.setHints(hints);
     */
    private Map<DecodeHintType, ?> decodeHints;

    /**
     * 【辅助解码的参数(用作MultiFormatReader的参数)】 字符集，告诉扫描器该以何种字符集进行解码
     * 对应于DecodeHintType.CHARACTER_SET类型
     * 参考DecodeThread构造器如下代码：hints.put(DecodeHintType.CHARACTER_SET,
     * characterSet);
     */
    private String characterSet;

    private Result savedResultToShow;

    private IntentSource source;

    /**
     * 图片的路径
     */
    private String photoPath;

    private Handler mHandler = new MyHandler(this);

    static class MyHandler extends Handler {

        private WeakReference<CaptureActivity> activityReference;

        public MyHandler(CaptureActivity activity) {
            activityReference = new WeakReference<CaptureActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            if (activityReference.get() == null) {
                return;
            }


            switch (msg.what) {
                case PARSE_BARCODE_SUC: // 解析图片成功
                    activityReference.get().checkQrCode(msg.obj.toString());
                    break;

                case PARSE_BARCODE_FAIL:// 解析图片失败
                    Log.d(TAG, "未找到二维码，解析失败");
                    Toast.makeText(activityReference.get(), "未找到二维码，解析失败", Toast.LENGTH_SHORT).show();
                    break;

                default:
                    break;
            }

            super.handleMessage(msg);
        }

    }

    private Button capture_flashlight;
    private TextView light;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.scan_act_layout);
        initView();
    }

    public void initView() {
        hasSurface = false;
        inactivityTimer = new InactivityTimer(this);
        beepManager = new BeepManager(this);
        ambientLightManager = new AmbientLightManager(this);

        capture_flashlight = (Button) findViewById(R.id.capture_flashlight);
        capture_flashlight.setBackgroundResource(R.drawable.scan_light_selector);
        light = (TextView) findViewById(R.id.light);
        light.setText("开灯");

    }

    @Override
    protected void onResume() {
        super.onResume();

        // CameraManager must be initialized here, not in onCreate(). This is
        // necessary because we don't
        // want to open the camera driver and measure the screen size if we're
        // going to show the help on
        // first launch. That led to bugs where the scanning rectangle was the
        // wrong size and partially
        // off screen.

        // 相机初始化的动作需要开启相机并测量屏幕大小，这些操作
        // 不建议放到onCreate中，因为如果在onCreate中加上首次启动展示帮助信息的代码的 话，
        // 会导致扫描窗口的尺寸计算有误的bug
        cameraManager = new CameraManager(getApplication());

        viewfinderView = (ViewfinderView) findViewById(R.id.capture_viewfinder_view);
        viewfinderView.setCameraManager(cameraManager);

        handler = null;
        lastResult = null;

        // 摄像头预览功能必须借助SurfaceView，因此也需要在一开始对其进行初始化
        // 如果需要了解SurfaceView的原理
        // 参考:http://blog.csdn.net/luoshengyang/article/details/8661317
        SurfaceView surfaceView = (SurfaceView) findViewById(R.id.capture_preview_view); // 预览
        SurfaceHolder surfaceHolder = surfaceView.getHolder();
        if (hasSurface) {
            // The activity was paused but not stopped, so the surface still
            // exists. Therefore
            // surfaceCreated() won't be called, so init the camera here.
            initCamera(surfaceHolder);

        } else {
            // 防止sdk8的设备初始化预览异常
            surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

            // Install the callback and wait for surfaceCreated() to init the
            // camera.
            surfaceHolder.addCallback(this);
        }

        // 加载声音配置，其实在BeemManager的构造器中也会调用该方法，即在onCreate的时候会调用一次
        beepManager.updatePrefs();

        // 启动闪光灯调节器
        ambientLightManager.start(cameraManager);

        // 恢复活动监控器
        inactivityTimer.onResume();

        source = IntentSource.NONE;
        decodeFormats = null;
        characterSet = null;
    }

    @Override
    protected void onPause() {
        if (handler != null) {
            handler.quitSynchronously();
            handler = null;
        }
        inactivityTimer.onPause();
        ambientLightManager.stop();

        // 关闭摄像头
        cameraManager.closeDriver();
        if (!hasSurface) {
            SurfaceView surfaceView = (SurfaceView) findViewById(R.id.capture_preview_view);
            SurfaceHolder surfaceHolder = surfaceView.getHolder();
            surfaceHolder.removeCallback(this);
        }

        light.setText("开灯");
        capture_flashlight
                .setBackgroundResource(R.drawable.scan_light_selector);
        isFlashlightOpen = false;

        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        beepManager.close();
    }

    @Override
    protected void onDestroy() {
        inactivityTimer.shutdown();
        super.onDestroy();
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                if ((source == IntentSource.NONE) && lastResult != null) { // 重新进行扫描
                    restartPreviewAfterDelay(0L);
                    return true;
                }
                break;
            case KeyEvent.KEYCODE_FOCUS:
            case KeyEvent.KEYCODE_CAMERA:
                // Handle these events so they don't launch the Camera app
                return true;

            case KeyEvent.KEYCODE_VOLUME_UP:
                cameraManager.zoomIn();
                return true;

            case KeyEvent.KEYCODE_VOLUME_DOWN:
                cameraManager.zoomOut();
                return true;

        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case REQUEST_CODE:

                    Uri selectedImage = null;
                    try {
                        selectedImage = intent.getData();
                    } catch (Exception e) {
                        return;
                    }
                    try {
                        String[] filePathColumn = {MediaColumns.DATA};

                        Cursor cursor = this.getContentResolver().query(
                                selectedImage, filePathColumn, null, null, null);
                        cursor.moveToFirst();

                        int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                        photoPath = cursor.getString(columnIndex);
                        cursor.close();
                    } catch (Exception e) {
                        String s = selectedImage.toString();
                        if (s.length() > 7) {
                            photoPath = s.substring(7, s.length());
                        } else {
                            photoPath = "";
                        }
                    }

                    Log.d(TAG, "onActivityResult: photoPath=" + photoPath);

                    new Thread(new Runnable() {

                        @Override
                        public void run() {

                            Bitmap img = BitmapUtils.getCompressedBitmap(photoPath);

                            BitmapDecoder decoder = new BitmapDecoder(
                                    CaptureActivity.this);
                            Result result = decoder.getRawResult(img);

                            if (result != null) {
                                Message m = mHandler.obtainMessage();
                                m.what = PARSE_BARCODE_SUC;
                                m.obj = ResultParser.parseResult(result).toString();
                                mHandler.sendMessage(m);
                            } else {
                                Message m = mHandler.obtainMessage();
                                m.what = PARSE_BARCODE_FAIL;
                                mHandler.sendMessage(m);
                            }


                        }
                    }).start();

                    break;

            }
        }

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (holder == null) {
            Log.e(TAG,
                    "*** WARNING *** surfaceCreated() gave us a null surface!");
        }
        if (!hasSurface) {
            hasSurface = true;
            initCamera(holder);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {
        /* hasSurface = false; */
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        hasSurface = false;
    }

    /**
     * A valid barcode has been found, so give an indication of success and show
     * the results.
     *
     * @param rawResult   The contents of the barcode.
     * @param scaleFactor amount by which thumbnail was scaled
     * @param barcode     A greyscale bitmap of the camera data which was decoded.
     */
    public void handleDecode(Result rawResult, Bitmap barcode, float scaleFactor) {

        // 重新计时
        inactivityTimer.onActivity();
        lastResult = rawResult;
        // 把图片画到扫描框
        viewfinderView.drawResultBitmap(barcode);

        beepManager.playBeepSoundAndVibrate();

        checkQrCode(ResultParser.parseResult(rawResult).toString());
    }

    public void restartPreviewAfterDelay(long delayMS) {
        if (handler != null) {
            handler.sendEmptyMessageDelayed(R.id.restart_preview, delayMS);
        }
        resetStatusView();
    }

    public ViewfinderView getViewfinderView() {
        return viewfinderView;
    }

    public Handler getHandler() {
        return handler;
    }

    public CameraManager getCameraManager() {
        return cameraManager;
    }

    private void resetStatusView() {
        viewfinderView.setVisibility(View.VISIBLE);
        lastResult = null;
    }

    public void drawViewfinder() {
        viewfinderView.drawViewfinder();
    }

    private void initCamera(SurfaceHolder surfaceHolder) {
        if (surfaceHolder == null) {
            throw new IllegalStateException("No SurfaceHolder provided");
        }

        if (cameraManager.isOpen()) {
            Log.w(TAG,
                    "initCamera() while already open -- late SurfaceView callback?");
            return;
        }
        try {
            cameraManager.openDriver(surfaceHolder);
            // Creating the handler starts the preview, which can also throw a
            // RuntimeException.
            if (handler == null) {
                handler = new CaptureActivityHandler(this, decodeFormats,
                        decodeHints, characterSet, cameraManager);
            }
            decodeOrStoreSavedBitmap(null, null);
        } catch (IOException ioe) {
            Log.w(TAG, ioe);
            displayFrameworkBugMessageAndExit();
        } catch (RuntimeException e) {
            // Barcode Scanner has seen crashes in the wild of this variety:
            // java.?lang.?RuntimeException: Fail to connect to camera service
            Log.w(TAG, "Unexpected error initializing camera", e);
            displayFrameworkBugMessageAndExit();
        }
    }

    /**
     * 向CaptureActivityHandler中发送消息，并展示扫描到的图像
     *
     * @param bitmap
     * @param result
     */
    private void decodeOrStoreSavedBitmap(Bitmap bitmap, Result result) {
        // Bitmap isn't used yet -- will be used soon
        if (handler == null) {
            savedResultToShow = result;
        } else {
            if (result != null) {
                savedResultToShow = result;
            }
            if (savedResultToShow != null) {
                Message message = Message.obtain(handler,
                        R.id.decode_succeeded, savedResultToShow);
                handler.sendMessage(message);
            }
            savedResultToShow = null;
        }
    }

    private void displayFrameworkBugMessageAndExit() {
        // AlertDialog.Builder builder = new AlertDialog.Builder(this);
        // builder.setTitle(getString(R.string.app_name));
        // builder.setMessage(getString(R.string.msg_camera_framework_bug));
        // builder.setPositiveButton(R.string.button_ok, new
        // FinishListener(this));
        // builder.setOnCancelListener(new FinishListener(this));
        // builder.show();
        // <string
        // name="msg_camera_framework_bug">抱歉，Android相机出现问题。您可能需要重启设备。</string>
        // <string name="button_ok">确定</string>

        Log.d(TAG, "displayFrameworkBugMessageAndExit: 相机权限被禁用");
    }

    /**
     * 开关灯
     *
     * @param view
     */
    public void lightOnClick(View view) {

        if (isFlashlightOpen) {
            cameraManager.setTorch(false); // 关闭闪光灯
            isFlashlightOpen = false;
            light.setText("开灯");
            capture_flashlight
                    .setBackgroundResource(R.drawable.scan_light_selector);
        } else {
            cameraManager.setTorch(true); // 打开闪光灯
            isFlashlightOpen = true;
            light.setText("关灯");
            capture_flashlight.setBackgroundResource(R.drawable.scan_light_on);
        }
    }

    /**
     * 解码图片
     *
     * @param view
     */
    public void photoOnClick(View view) {
        // 打开手机中的相册
        Intent intent = new Intent();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            // intent.setAction(Intent.ACTION_OPEN_DOCUMENT);
            intent = new Intent(
                    Intent.ACTION_PICK,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        } else {
            intent.setAction(Intent.ACTION_GET_CONTENT);
        }
        intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                "image/*");
        Intent wrapperIntent = Intent.createChooser(intent, "选择二维码图片");
        this.startActivityForResult(wrapperIntent, REQUEST_CODE);
    }


    // /////////////////////////////////////////////////////////////////////////////////////////////////
    // //////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * 检查二维码的扫码结果
     */
    private void checkQrCode(String result) {
        // TODO 扫描后的逻辑处理
        Toast.makeText(this, "扫码结果=" + result, Toast.LENGTH_SHORT).show();
        finish();
    }


}
