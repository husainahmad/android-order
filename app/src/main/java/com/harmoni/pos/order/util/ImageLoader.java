package com.harmoni.pos.order.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Small async loader for hosted product images: downloads off the main thread
 * with downsampling and keeps a bounded memory cache so grid scrolling never
 * decodes the same link repeatedly. Images are cached by URL, which the menu
 * service returns after uploading to ImgBB.
 */
public final class ImageLoader {

    private static final int CACHE_BYTES = 8 * 1024 * 1024;
    private static final int MAX_EDGE = 256;
    private static final int TIMEOUT_MS = 10000;

    private static final LruCache<String, Bitmap> CACHE = new LruCache<String, Bitmap>(CACHE_BYTES) {
        @Override
        protected int sizeOf(String key, Bitmap value) {
            return value.getByteCount();
        }
    };
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(3);
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    public interface Callback {
        void onLoaded(Bitmap bitmap);
    }

    private ImageLoader() {
    }

    /** Downloads the image at {@code url}, invoking {@code callback} on the main thread. */
    public static void load(String url, Callback callback) {
        if (url == null || url.isEmpty()) {
            if (callback != null) callback.onLoaded(null);
            return;
        }
        Bitmap cached = CACHE.get(url);
        if (cached != null) {
            if (callback != null) callback.onLoaded(cached);
            return;
        }
        EXECUTOR.execute(() -> {
            Bitmap bitmap = downloadSampled(url);
            if (bitmap != null) {
                CACHE.put(url, bitmap);
            }
            final Bitmap result = bitmap;
            MAIN.post(() -> {
                if (callback != null) callback.onLoaded(result);
            });
        });
    }

    public static void evict(String url) {
        CACHE.remove(url);
    }

    private static Bitmap downloadSampled(String url) {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);
            conn.setDoInput(true);
            conn.connect();
            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) return null;

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (InputStream in = conn.getInputStream()) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
            }
            byte[] data = out.toByteArray();
            if (data.length == 0) return null;

            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(data, 0, data.length, bounds);
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null;

            int sample = 1;
            while (bounds.outWidth / (sample * 2) >= MAX_EDGE
                    || bounds.outHeight / (sample * 2) >= MAX_EDGE) {
                sample *= 2;
            }
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = sample;
            return BitmapFactory.decodeByteArray(data, 0, data.length, opts);
        } catch (Exception e) {
            return null;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }
}
