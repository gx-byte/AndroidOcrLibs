package com.fanjun.orclibs;

import android.content.Intent;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.ImageView;
import kernal.bankcard.android.BankCardAPI;

import com.fanjun.orclibs.Utils.CameraUtils;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

/**
 * 识别银行卡demo
 */
public class ScanBankCardActivity extends AppCompatActivity implements Camera.PreviewCallback, SurfaceHolder.Callback {
    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    private BankCardAPI bankCardAPI;
    Camera mCamera;
    ImageView view2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);
        surfaceView = findViewById(R.id.mSurfaceView);
        view2 = findViewById(R.id.view2);
        CameraUtils.init(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);//禁止息屏
        this.surfaceHolder = this.surfaceView.getHolder();
        this.surfaceHolder.addCallback(this);
        this.surfaceHolder.setType(3);
        this.bankCardAPI = new BankCardAPI();
        this.bankCardAPI.WTInitCardKernal("", 0);
    }

    protected void onResume() {
        super.onResume();
    }

    private byte[] data;

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        this.data = data;
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

    /**
     * 初始化相机
     */
    void initCamera() {
        try {
            mCamera.setPreviewCallback(this);
            Camera.Parameters parameters = mCamera.getParameters();
            parameters.setPreviewFormat(ImageFormat.NV21);
            Camera.Size optionSize = CameraUtils.findBestPreviewResolution(mCamera);
            parameters.setPreviewSize(optionSize.width, optionSize.height);
            parameters.setPictureSize(optionSize.width, optionSize.height);
            mCamera.setParameters(parameters);
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
            this.bankCardAPI.WTUnInitCardKernal();
            //关闭sdk
        } catch (Exception e) {
        }
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
                                    final Camera.Size size = mCamera.getParameters().getPreviewSize();
                                    //SDK识别
                                    int[] arrayOfInt1 = new int[4];
                                    int[] arrayOfInt3 = new int[32000];
                                    char[] arrayOfChar1 = new char[30];
                                    char[] arrayOfChar2 = new char[30];
                                    int[] arrayOfInt2 = new int[1];
                                    bankCardAPI.SetFilterInvalidCard(1);
                                    bankCardAPI.SetExpiryDateFlag(1);
                                    int[] arrayOfInt = {view2.getLeft(), view2.getTop(), view2.getRight(), view2.getBottom()};
                                    bankCardAPI.WTSetROI(arrayOfInt, view2.getWidth(), view2.getHeight());
                                    int i1 = bankCardAPI.RecognizeNV21(data, size.width, size.height, arrayOfInt1, arrayOfChar1, 30, arrayOfInt2, arrayOfInt3, arrayOfChar2);
                                    if (i1 == 0) {
                                        success(arrayOfInt3, arrayOfChar1, 2, arrayOfChar2);
                                    }
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

    @Override
    public void finish() {
        super.finish();
        closeCamera();
    }

    boolean hasSuccess = false;

    /**
     * 识别结果
     *
     * @param PicR    生成的图片
     * @param StringR 卡号 中间会有空格
     * @param success 未知
     * @param StringD 有效期截止年月
     */
    void success(int[] PicR, char[] StringR, int success, char[] StringD) {
        if (hasSuccess) {
            return;
        }
        hasSuccess = true;
        //卡号(含空格)
        String cardNo = new String(StringR);
        //过期日期
        String expiryDate = new String(StringD);
        //卡号(不含空格)
        String cardNo2 = cardNo.replaceAll(" +", "");
        //银行卡详细明细 $招商银行$银联IC金卡$03080000$借记卡$
        String bankInfo = bankCardAPI.GetBankInfo(cardNo2);
        bankInfo =  bankInfo.substring(1, bankInfo.length() - 1);
        String[] bankInfos = bankInfo.split("\\$");
        //发卡银行
        String bankName = bankInfos[0];
        //卡名
        String cardName = bankInfos[1];
        //机构代码
        String organizationCode = bankInfos[2];
        //卡种
        String cardType = bankInfos[3];

        Intent i = new Intent();
        String ocrResult = "卡号："+cardNo+"\n"
                +"过期日期:"+expiryDate+"\n"
                +"发卡银行:"+bankName+"\n"
                +"卡名:"+cardName+"\n"
                +"机构代码:"+organizationCode+"\n"
                +"卡种:"+cardType+"\n";
        i.putExtra("OCRResult", ocrResult);
        setResult(RESULT_OK, i);
        finish();
    }
}
