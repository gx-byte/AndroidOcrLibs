package com.fanjun.orclibs;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.msd.ocr.idcard.LibraryInitOCR;
import com.yanzhenjie.permission.AndPermission;
import com.yanzhenjie.permission.Permission;

import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    Button mScanIdCard, mScanDriverCard, mScanBankCard;
    TextView mSuccessText;
    public static final int SCAN_IDCARD_REQUEST = 1;
    public static final int SCAN_DRIVERCARD_REQUEST = 2;
    public static final int SCAN_BANKCARD_REQUEST = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mScanIdCard = findViewById(R.id.mScanIdCard);
        mScanDriverCard = findViewById(R.id.mScanDriverCard);
        mScanBankCard = findViewById(R.id.mScanBankCard);
        mSuccessText = findViewById(R.id.mSuccessText);
        mScanIdCard.setOnClickListener(this);
        mScanDriverCard.setOnClickListener(this);
        mScanBankCard.setOnClickListener(this);
        AndPermission.with(this).runtime().permission(Permission.Group.CAMERA, Permission.Group.STORAGE).start();
    }

    @Override
    public void onClick(View v) {
        Bundle bundle = null;
        switch (v.getId()) {
            //识别身份证
            case R.id.mScanIdCard:
                LibraryInitOCR.initOCR(this);
                bundle = new Bundle();
                bundle.putBoolean("saveImage", true);
                bundle.putInt("requestCode", SCAN_IDCARD_REQUEST);
                bundle.putInt("type", 0); //0身份证, 1驾驶证
                LibraryInitOCR.startScan(MainActivity.this, bundle);
                break;
            //识别驾驶证
            case R.id.mScanDriverCard:
                LibraryInitOCR.initOCR(this);
                bundle = new Bundle();
                bundle.putBoolean("saveImage", true);
                bundle.putInt("requestCode", SCAN_DRIVERCARD_REQUEST);
                bundle.putInt("type", 1); //0身份证, 1驾驶证
                LibraryInitOCR.startScan(MainActivity.this, bundle);
                break;
            //识别银行卡
            case R.id.mScanBankCard:
                Intent intent = new Intent(MainActivity.this, ScanBankCardActivity.class);
                startActivityForResult(intent, SCAN_BANKCARD_REQUEST);
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case SCAN_IDCARD_REQUEST:
                    String result = data.getStringExtra("OCRResult");
                    try {
                        JSONObject jo = new JSONObject(result);
                        StringBuffer sb = new StringBuffer();
                        sb.append(String.format("正面 = %s\n", jo.opt("type")));
                        sb.append(String.format("姓名 = %s\n", jo.opt("name")));
                        sb.append(String.format("性别 = %s\n", jo.opt("sex")));
                        sb.append(String.format("民族 = %s\n", jo.opt("folk")));
                        sb.append(String.format("日期 = %s\n", jo.opt("birt")));
                        sb.append(String.format("号码 = %s\n", jo.opt("num")));
                        sb.append(String.format("住址 = %s\n", jo.opt("addr")));
                        sb.append(String.format("签发机关 = %s\n", jo.opt("issue")));
                        sb.append(String.format("有效期限 = %s\n", jo.opt("valid")));
                        sb.append(String.format("整体照片 = %s\n", jo.opt("imgPath")));
                        sb.append(String.format("头像路径 = %s\n", jo.opt("headPath")));
                        mSuccessText.setText(sb.toString());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    break;
                case SCAN_DRIVERCARD_REQUEST:
                    String result1 = data.getStringExtra("OCRResult");
                    try {
                        JSONObject jo = new JSONObject(result1);
                        StringBuffer sb = new StringBuffer();
                        sb.append(String.format("整体照片 = %s\n", jo.opt("imgPath")));
                        sb.append(String.format("国家 = %s\n", jo.opt("nation")));
                        sb.append(String.format("初始领证 = %s\n", jo.opt("startTime")));
                        sb.append(String.format("准驾车型 = %s\n", jo.opt("drivingType")));
                        sb.append(String.format("有效期限 = %s\n", jo.opt("registerDate")));
                        mSuccessText.setText(sb.toString());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    break;
                case SCAN_BANKCARD_REQUEST:
                    String result2 = data.getStringExtra("OCRResult");
                    mSuccessText.setText(result2);
            }
        }
    }
}
