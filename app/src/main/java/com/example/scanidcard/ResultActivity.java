package com.example.scanidcard;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

/**
 * 展示识别结果的 Activity
 */
public class ResultActivity extends AppCompatActivity {

    public static final String EXTRA_RESULT = "extra_result";

    private TextView tvStatus;
    private TextView tvError;
    private TextView tvName;
    private TextView tvSex;
    private TextView tvNation;
    private TextView tvBirth;
    private TextView tvAddress;
    private TextView tvIdNum;
    private TextView tvAuthority;
    private TextView tvValidDate;
    private TextView tvRequestId;
    private TextView tvRawJson;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        tvStatus = findViewById(R.id.tvStatus);
        tvError = findViewById(R.id.tvError);
        tvName = findViewById(R.id.tvName);
        tvSex = findViewById(R.id.tvSex);
        tvNation = findViewById(R.id.tvNation);
        tvBirth = findViewById(R.id.tvBirth);
        tvAddress = findViewById(R.id.tvAddress);
        tvIdNum = findViewById(R.id.tvIdNum);
        tvAuthority = findViewById(R.id.tvAuthority);
        tvValidDate = findViewById(R.id.tvValidDate);
        tvRequestId = findViewById(R.id.tvRequestId);
        tvRawJson = findViewById(R.id.tvRawJson);

        IdentifyResult result = (IdentifyResult) getIntent().getSerializableExtra(EXTRA_RESULT);
        if (result == null) {
            tvStatus.setText("未获取到识别结果");
            return;
        }

        if (result.getErrorcode() == 0) {
            tvStatus.setText("识别成功");
            tvError.setVisibility(View.GONE);
        } else {
            tvStatus.setText("识别失败");
            tvError.setVisibility(View.VISIBLE);
            tvError.setText(result.getErrormsg());
        }

        tvName.setText("姓名: " + safe(result.getName()));
        tvSex.setText("性别: " + safe(result.getSex()));
        tvNation.setText("民族: " + safe(result.getNation()));
        tvBirth.setText("出生日期: " + safe(result.getBirth()));
        tvAddress.setText("住址: " + safe(result.getAddress()));
        tvIdNum.setText("身份证号: " + safe(result.getIdNum()));

        tvAuthority.setText("签发机关: " + safe(result.getAuthority()));
        tvValidDate.setText("有效期限: " + safe(result.getValidDate()));

        if (!TextUtils.isEmpty(result.getRequestId())) {
            tvRequestId.setText("RequestId: " + result.getRequestId());
        } else {
            tvRequestId.setText("");
        }

        String raw = result.getRawJson();
        tvRawJson.setText(!TextUtils.isEmpty(raw) ? ("原始返回JSON:\n" + raw) : "");
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }
}
