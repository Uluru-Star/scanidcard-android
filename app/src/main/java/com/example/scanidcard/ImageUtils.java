package com.example.scanidcard;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * 读取 Uri 图片并压缩成 JPEG，避免身份证图片过大导致请求失败或内存溢出。
 */
public class ImageUtils {

    /**
     * 将 Uri 图片压缩成 JPEG 的 byte[]（默认 max 1280x1280, quality=90）
     */
    public static byte[] readAndCompressJpeg(Context context, Uri uri) throws IOException {
        return readAndCompressJpeg(context, uri, 1280, 1280, 90);
    }

    public static byte[] readAndCompressJpeg(Context context, Uri uri, int maxWidth, int maxHeight, int quality)
            throws IOException {

        ContentResolver cr = context.getContentResolver();

        // 1) 先读尺寸
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        InputStream is1 = cr.openInputStream(uri);
        if (is1 == null) throw new IOException("无法打开图片输入流");
        try {
            BitmapFactory.decodeStream(is1, null, bounds);
        } finally {
            try { is1.close(); } catch (Exception ignored) {}
        }

        int inSampleSize = calculateInSampleSize(bounds, maxWidth, maxHeight);

        // 2) 重新按采样率解码
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inSampleSize = inSampleSize;
        opts.inPreferredConfig = Bitmap.Config.ARGB_8888;

        InputStream is2 = cr.openInputStream(uri);
        if (is2 == null) throw new IOException("无法打开图片输入流");
        Bitmap bitmap;
        try {
            bitmap = BitmapFactory.decodeStream(is2, null, opts);
        } finally {
            try { is2.close(); } catch (Exception ignored) {}
        }

        if (bitmap == null) throw new IOException("图片解码失败（可能不是有效的图片文件）");

        // 3) 压缩成 JPEG
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos);
        bitmap.recycle();

        return baos.toByteArray();
    }

    /**
     * 直接得到 Base64 字符串（不带换行）
     */
    public static String readAsBase64(Context context, Uri uri) throws IOException {
        byte[] jpg = readAndCompressJpeg(context, uri);
        return Base64Util.encode(jpg);
    }

    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        int height = options.outHeight;
        int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            int halfHeight = height / 2;
            int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return Math.max(1, inSampleSize);
    }
}
