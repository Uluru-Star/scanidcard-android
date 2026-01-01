package com.example.scanidcard;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * 腾讯云 API 3.0（TC3-HMAC-SHA256）签名实现（V3 签名）
 *
 * 注意：请求发送时的 Header 与 Payload 必须与签名计算过程中的内容完全一致，否则会返回签名不一致错误。
 */
public class Tc3Signer {

    public static final String ALGORITHM = "TC3-HMAC-SHA256";

    public static class SignResult {
        public String authorization;
        public String signature;
        public String canonicalRequest;
        public String stringToSign;
        public String date; // yyyy-MM-dd in UTC
        public long timestamp; // seconds
        public String signedHeaders;
        public String canonicalHeaders;
    }

    /**
     * 生成 Authorization 头。
     *
     * @param secretId    SecretId
     * @param secretKey   SecretKey
     * @param service     产品名称，例如 OCR 为 "ocr"
     * @param host        请求域名，例如 "ocr.tencentcloudapi.com"
     * @param action      接口 Action，例如 "IDCardOCR"
     * @param version     接口 Version，例如 "2018-11-19"
     * @param timestamp   秒级时间戳
     * @param payload     请求体 JSON 字符串（必须与实际发送完全一致）
     * @param contentType Content-Type（建议使用 "application/json" 并与实际发送一致）
     */
    public static SignResult sign(
            String secretId,
            String secretKey,
            String service,
            String host,
            String action,
            String version,
            long timestamp,
            String payload,
            String contentType
    ) throws Exception {

        // 注意时区必须是 UTC，否则 date 会不一致导致签名错误
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        String date = sdf.format(new Date(timestamp * 1000L));

        // ************* Step 1: 拼接规范请求串 canonicalRequest *************
        String httpRequestMethod = "POST";
        String canonicalUri = "/";
        String canonicalQueryString = "";

        // canonical headers 必须全部小写
        String canonicalHeaders = ""
                + "content-type:" + contentType + "\n"
                + "host:" + host + "\n"
                + "x-tc-action:" + action.toLowerCase(Locale.ROOT) + "\n";

        String signedHeaders = "content-type;host;x-tc-action";

        String hashedRequestPayload = sha256Hex(payload);

        String canonicalRequest = httpRequestMethod + "\n"
                + canonicalUri + "\n"
                + canonicalQueryString + "\n"
                + canonicalHeaders + "\n"
                + signedHeaders + "\n"
                + hashedRequestPayload;

        // ************* Step 2: 拼接待签名字符串 stringToSign *************
        String credentialScope = date + "/" + service + "/tc3_request";
        String hashedCanonicalRequest = sha256Hex(canonicalRequest);

        String stringToSign = ALGORITHM + "\n"
                + timestamp + "\n"
                + credentialScope + "\n"
                + hashedCanonicalRequest;

        // ************* Step 3: 计算签名 signature *************
        byte[] secretDate = hmacSha256(("TC3" + secretKey).getBytes(StandardCharsets.UTF_8), date);
        byte[] secretService = hmacSha256(secretDate, service);
        byte[] secretSigning = hmacSha256(secretService, "tc3_request");

        String signature = bytesToHex(hmacSha256(secretSigning, stringToSign));

        // ************* Step 4: 拼接 Authorization *************
        String authorization = ALGORITHM + " "
                + "Credential=" + secretId + "/" + credentialScope + ", "
                + "SignedHeaders=" + signedHeaders + ", "
                + "Signature=" + signature;

        SignResult result = new SignResult();
        result.authorization = authorization;
        result.signature = signature;
        result.canonicalRequest = canonicalRequest;
        result.stringToSign = stringToSign;
        result.date = date;
        result.timestamp = timestamp;
        result.signedHeaders = signedHeaders;
        result.canonicalHeaders = canonicalHeaders;
        return result;
    }

    /**
     * SHA-256 哈希，返回 16 进制小写字符串。
     *
     * 这里参考你提供的 HashEncryption 方法（替换 DatatypeConverter.printHexBinary）。
     */
    private static String sha256Hex(String s) throws Exception {
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        sha.update(s.getBytes(StandardCharsets.UTF_8));

        StringBuilder builder = new StringBuilder();
        for (byte b : sha.digest()) {
            String hex = Integer.toHexString(b & 0xFF);
            if (hex.length() == 1) hex = "0" + hex;
            builder.append(hex);
        }
        return builder.toString().toLowerCase(Locale.ROOT);
    }

    /**
     * HmacSHA256
     *
     * 这里参考你提供的 HashHmacSha256Encryption 方法。
     */
    private static byte[] hmacSha256(byte[] key, String msg) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(key, mac.getAlgorithm());
        mac.init(secretKeySpec);
        return mac.doFinal(msg.getBytes(StandardCharsets.UTF_8));
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            String hex = Integer.toHexString(b & 0xFF);
            if (hex.length() == 1) sb.append('0');
            sb.append(hex);
        }
        return sb.toString().toLowerCase(Locale.ROOT);
    }
}
