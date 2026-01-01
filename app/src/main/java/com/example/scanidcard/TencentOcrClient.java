package com.example.scanidcard;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * 使用 HttpURLConnection 调用腾讯云 OCR：身份证识别（IDCardOCR）
 */
public class TencentOcrClient {

    // OCR 服务固定参数
    public static final String HOST = "ocr.tencentcloudapi.com";
    public static final String SERVICE = "ocr";
    public static final String ACTION = "IDCardOCR";
    public static final String VERSION = "2018-11-19";
    public static final String ENDPOINT = "https://" + HOST + "/";

    // 本项目统一使用 application/json（与签名计算保持一致）
    public static final String CONTENT_TYPE = "application/json";

    /**
     * 调用身份证识别接口
     *
     * @param secretId    腾讯云 SecretId
     * @param secretKey   腾讯云 SecretKey
     * @param region      可选：地域，如 ap-beijing（不填也可）
     * @param imageBase64 图片 Base64（不要包含 data:image/... 前缀）
     * @param cardSide    可选："FRONT" / "BACK" / null（不填代表自动）
     */
    public static IdentifyResult idCardOcr(
            String secretId,
            String secretKey,
            String region,
            String imageBase64,
            String cardSide
    ) throws Exception {

        long timestamp = System.currentTimeMillis() / 1000L;

        // 1) 组装请求体（payload）
        JSONObject payloadObj = new JSONObject();
        payloadObj.put("ImageBase64", imageBase64);
        if (cardSide != null && !cardSide.trim().isEmpty()) {
            payloadObj.put("CardSide", cardSide);
        }
        String payload = payloadObj.toString();

        // 2) 生成签名（Authorization）
        Tc3Signer.SignResult signResult = Tc3Signer.sign(
                secretId,
                secretKey,
                SERVICE,
                HOST,
                ACTION,
                VERSION,
                timestamp,
                payload,
                CONTENT_TYPE
        );

        // 3) 发送 HTTP 请求
        URL url = new URL(ENDPOINT);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        try {
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setUseCaches(false);

            conn.setConnectTimeout(15000);
            conn.setReadTimeout(20000);

            // ====== Header（必须与签名过程一致）======
            conn.setRequestProperty("Authorization", signResult.authorization);
            conn.setRequestProperty("Content-Type", CONTENT_TYPE);
            conn.setRequestProperty("Host", HOST);
            conn.setRequestProperty("X-TC-Action", ACTION);
            conn.setRequestProperty("X-TC-Timestamp", String.valueOf(timestamp));
            conn.setRequestProperty("X-TC-Version", VERSION);
            if (region != null && !region.trim().isEmpty()) {
                conn.setRequestProperty("X-TC-Region", region.trim());
            }

            // 可选：语言
            conn.setRequestProperty("X-TC-Language", "zh-CN");

            // 写入 body
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream(), StandardCharsets.UTF_8));
            writer.write(payload);
            writer.flush();
            writer.close();

            int code = conn.getResponseCode();
            InputStream is = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();

            String respText = readAll(is);

            // 4) 解析返回
            IdentifyResult result = parseIdentifyResult(respText);
            // 额外保存调试信息（便于你论文截图或排错）
            result.setRawJson(respText);
            return result;

        } finally {
            conn.disconnect();
        }
    }

    private static IdentifyResult parseIdentifyResult(String respText) {
        IdentifyResult result = new IdentifyResult();
        try {
            JSONObject root = new JSONObject(respText);
            JSONObject resp = root.optJSONObject("Response");
            if (resp == null) {
                result.setErrorcode(1);
                result.setErrormsg("返回数据中缺少 Response 字段");
                return result;
            }

            result.setRequestId(resp.optString("RequestId", ""));

            // 错误结构：{"Response":{"Error":{"Code":"","Message":""},"RequestId":""}}
            if (resp.has("Error")) {
                JSONObject err = resp.optJSONObject("Error");
                String code = err != null ? err.optString("Code", "") : "";
                String msg = err != null ? err.optString("Message", "") : "";
                result.setErrorcode(1);
                result.setErrormsg(code + (msg.isEmpty() ? "" : (": " + msg)));
                return result;
            }

            result.setErrorcode(0);
            result.setErrormsg("");

            // 正面字段
            result.setName(resp.optString("Name", ""));
            result.setSex(resp.optString("Sex", ""));
            result.setNation(resp.optString("Nation", ""));
            result.setBirth(resp.optString("Birth", ""));
            result.setAddress(resp.optString("Address", ""));
            result.setIdNum(resp.optString("IdNum", ""));

            // 反面字段
            result.setAuthority(resp.optString("Authority", ""));
            result.setValidDate(resp.optString("ValidDate", ""));

            // 其他
            result.setAdvancedInfo(resp.optString("AdvancedInfo", ""));

            return result;

        } catch (Exception e) {
            result.setErrorcode(1);
            result.setErrormsg("解析 JSON 失败：" + e.getMessage());
            return result;
        }
    }

    private static String readAll(InputStream is) throws Exception {
        if (is == null) return "";
        BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line).append('\n');
        }
        br.close();
        return sb.toString();
    }
}
