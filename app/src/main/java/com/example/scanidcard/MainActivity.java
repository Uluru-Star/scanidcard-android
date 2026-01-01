package com.example.scanidcard;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.util.Log;
import com.example.scanidcard.BuildConfig;



/**
 * 主页：输入密钥 -> 选择身份证图片 -> 调用腾讯云 OCR -> 跳转结果页
 */
public class MainActivity extends AppCompatActivity {

    private static final String SP_NAME = "tencent_ocr_cfg";
    private static final String KEY_SECRET_ID = "secret_id";
    private static final String KEY_SECRET_KEY = "secret_key";
    private static final String KEY_REGION = "region";

    private EditText etSecretId;
    private EditText etSecretKey;
    private EditText etRegion;

    private Button btnPickImage;
    private Button btnRecognize;
    private ProgressBar progress;
    private TextView tvLog;
    private ImageView ivPreview;

    private RadioGroup rgCardSide;
    private RadioButton rbAuto;
    private RadioButton rbFront;
    private RadioButton rbBack;

    private Uri selectedImageUri;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final ActivityResultLauncher<String> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    ivPreview.setImageURI(uri);
                    appendLog("已选择图片: " + uri);
                } else {
                    appendLog("未选择图片");
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 安全日志（避免空指针）
        Log.d("ENV", "sidLen=" + (BuildConfig.TENCENT_SECRET_ID == null ? -1 : BuildConfig.TENCENT_SECRET_ID.length()));

        etSecretId = findViewById(R.id.etSecretId);
        etSecretKey = findViewById(R.id.etSecretKey);
        etRegion = findViewById(R.id.etRegion);

        btnPickImage = findViewById(R.id.btnPickImage);
        btnRecognize = findViewById(R.id.btnRecognize);
        progress = findViewById(R.id.progress);
        tvLog = findViewById(R.id.tvLog);
        ivPreview = findViewById(R.id.ivPreview);

        rgCardSide = findViewById(R.id.rgCardSide);
        rbAuto = findViewById(R.id.rbAuto);
        rbFront = findViewById(R.id.rbFront);
        rbBack = findViewById(R.id.rbBack);

        // 读取上次输入：只保留 region（密钥不再本地保存/回填）
        SharedPreferences sp = getSharedPreferences(SP_NAME, MODE_PRIVATE);
        etRegion.setText(sp.getString(KEY_REGION, "ap-guangzhou"));

        // 密钥从 BuildConfig 读取，不需要用户输入（禁用输入框避免误操作）
        etSecretId.setText("已从.env加载");
        etSecretKey.setText("已从.env加载");
        etSecretId.setEnabled(false);
        etSecretKey.setEnabled(false);

        // ✅关键：按钮点击事件（你之前缺了这个）
        btnPickImage.setOnClickListener(v -> {
            appendLog("点击：选择身份证图片");
            pickImageLauncher.launch("image/*");
        });

        btnRecognize.setOnClickListener(v -> {
            appendLog("点击：开始识别");
            doRecognize();
        });

        // ✅放最后：确保按钮可点
        setLoading(false);
    }


    private void doRecognize() {
        String secretId = BuildConfig.TENCENT_SECRET_ID;
        String secretKey = BuildConfig.TENCENT_SECRET_KEY;

        String region = etRegion.getText() != null ? etRegion.getText().toString().trim() : "";

            if (TextUtils.isEmpty(secretId) || TextUtils.isEmpty(secretKey)) {
                appendLog("未读取到 .env 中的 SecretId/SecretKey，请检查根目录 .env 是否存在且已填写");
                return;
            }

            if (selectedImageUri == null) {
            appendLog("请先选择身份证图片");
            return;
        }

        // 保存到 SharedPreferences（仅为便捷，真实项目不建议明文保存）
            getSharedPreferences(SP_NAME, MODE_PRIVATE)
                    .edit()
                    .putString(KEY_REGION, region)
                    .apply();


            String cardSide = null;
        if (rbFront.isChecked()) cardSide = "FRONT";
        else if (rbBack.isChecked()) cardSide = "BACK";

        setLoading(true);
        appendLog("开始识别，正在压缩图片并构造请求...");

        final String finalCardSide = cardSide;
        executor.execute(() -> {
            IdentifyResult result;
            try {
                // 1) 图片压缩 + Base64
                String base64 = ImageUtils.readAsBase64(this, selectedImageUri);

                appendLogOnUi("图片 Base64 已生成（长度=" + base64.length() + "），开始请求腾讯云 OCR...");

                // 2) 调用腾讯云 OCR
                result = TencentOcrClient.idCardOcr(secretId, secretKey, region, base64, finalCardSide);

            } catch (Exception e) {
                result = new IdentifyResult();
                result.setErrorcode(1);
                result.setErrormsg("识别失败：" + e.getMessage());
            }

            IdentifyResult finalResult = result;
            runOnUiThread(() -> {
                setLoading(false);

                if (finalResult.getErrorcode() == 0) {
                    appendLog("识别成功，跳转结果页");
                } else {
                    appendLog("识别失败：" + finalResult.getErrormsg());
                }

                Intent intent = new Intent(MainActivity.this, ResultActivity.class);
                intent.putExtra(ResultActivity.EXTRA_RESULT, finalResult);
                startActivity(intent);
            });
        });
    }

    private void setLoading(boolean loading) {
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnRecognize.setEnabled(!loading);
        btnPickImage.setEnabled(!loading);
    }


    private void appendLog(String msg) {
        String old = tvLog.getText() != null ? tvLog.getText().toString() : "";
        tvLog.setText(old + "\n" + msg);
    }

    private void appendLogOnUi(String msg) {
        runOnUiThread(() -> appendLog(msg));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 防止线程泄漏
        executor.shutdownNow();
    }
}
