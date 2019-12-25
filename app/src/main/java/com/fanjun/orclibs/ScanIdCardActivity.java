package com.fanjun.orclibs;

import android.content.Intent;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.ImageView;

import com.fanjun.orclibs.Utils.CameraUtils;
import com.msd.ocr.idcard.LibraryInitOCR;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import kernal.bankcard.android.BankCardAPI;

/**
 * 识别身份证开源 demo （驾驶证一样）
 * 实际上识别身份证和驾驶证的API是一样的
 */
public class ScanIdCardActivity extends AppCompatActivity implements Camera.PreviewCallback, SurfaceHolder.Callback {
    private SurfaceView mSurfaceView;
    private SurfaceHolder surfaceHolder;
    private BankCardAPI bankCardAPI;
    Camera mCamera;
    ImageView view2;
    Handler ocrHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);
        mSurfaceView = findViewById(R.id.mSurfaceView);
        view2 = findViewById(R.id.view2);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);//禁止息屏
        initHandler();
        LibraryInitOCR.initOCR(this);
        LibraryInitOCR.initDecode(this, ocrHandler, true);//第三个参数是是否保存图片
        CameraUtils.init(this);
    }
    /**
     * 初始化handler
     */
    void initHandler() {
        ocrHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    //解码成功
                    case LibraryInitOCR.DECODE_SUCCESS: {
                        Intent intent = (Intent) msg.obj;
                        String result = intent.getStringExtra("OCRResult");
                        JSONObject jsonRes = null;
                        try {
                            jsonRes = new JSONObject(result);
                            /*身份证相关
                            type:正面
                            sex:性别
                            folk:民族
                            birt:日期
                            num:号码
                            addr:住址
                            issue:签发机关
                            valid:有效期限
                            imgPath:整体照片
                            headPath:头像路径

                            驾驶证相关
                            nation：国家
                            startTime：初始领证
                            drivingType：准驾车型
                            registerDate：有效期限*/
                            String idcard = jsonRes.optString("num");
                            String realname = jsonRes.optString("name");
                            Intent i = new Intent();
                            String ocrResult = "姓名："+realname+"\n"
                                    +"身份证号:"+idcard+"\n";
                            i.putExtra("OCRResult", ocrResult);
                            setResult(RESULT_OK, i);
                            finish();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        break;
                    }
                    //解码失败
                    case LibraryInitOCR.DECODE_FAIL: {
                        break;
                    }
                }
            }
        };
    }

    /**
     * 初始化相机
     */
    void initCamera() {
        try {
            mCamera.setPreviewCallback(this);
            Camera.Parameters parameters = mCamera.getParameters();
            parameters.setPreviewFormat(ImageFormat.NV21);
            //根据设置的宽高 和手机支持的分辨率对比计算出合适的宽高算法
            Camera.Size optionSize = CameraUtils.findBestPreviewResolution(mCamera);
            // parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);//部分手机无效
            parameters.setPreviewSize(optionSize.width, optionSize.height);
            //设置照片尺寸
            parameters.setPictureSize(optionSize.width, optionSize.height);
            mCamera.setParameters(parameters);
            //开启预览
            mCamera.startPreview();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 释放相机
     */
    void closeCamera() {
        try {
            if (autoFocusTimer != null) {
                autoFocusTimer.cancel();
            }
            if (mCamera != null) {
                mCamera.stopPreview();
                //mCamera.release();//加上要挂啊
                mCamera = null;
            }
            LibraryInitOCR.closeDecode();
        } catch (Exception e) {
        }
    }

    @Override
    public void finish() {
        super.finish();
        closeCamera();
    }
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        try {
            mCamera = Camera.open(0);//0:后置 1：前置
            initCamera();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        try {
            mCamera.setPreviewDisplay(holder);
            initAutoFocusTimer();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        closeCamera();
    }

    private byte[] data;

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        this.data = data;
    }

    Timer autoFocusTimer;

    void initAutoFocusTimer() {
        if (autoFocusTimer == null) {
            autoFocusTimer = new Timer();
            autoFocusTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    if (mCamera != null) {
                        mCamera.autoFocus(new Camera.AutoFocusCallback() {
                            @Override
                            public void onAutoFocus(boolean success, Camera camera) {
                                if (data != null) {
                                    Camera.Size size = mCamera.getParameters().getPreviewSize();
                                    Rect rect = new Rect();
                                    rect.top = view2.getTop();
                                    rect.right = view2.getRight();
                                    rect.left = view2.getLeft();
                                    rect.bottom = view2.getBottom();
                                    LibraryInitOCR.decode(rect, size.width, size.height, data);
                                    if (mCamera != null) {
                                        mCamera.cancelAutoFocus();
                                    }
                                }
                            }
                        });

                    }
                }
            }, 0, 600);
        }
    }
}
