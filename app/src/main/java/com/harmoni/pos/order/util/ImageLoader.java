package com.harmoni.pos.order.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.LruCache;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Small async loader for base64-embedded product images: decodes off the
 * main thread with downsampling and keeps a bounded memory cache so grid
 * scrolling never decodes full-resolution blobs repeatedly.
 */
public final class ImageLoader {

    private static final int CACHE_BYTES = 8 * 1024 * 1024;
    private static final int MAX_EDGE = 256;

    private static final LruCache<Integer, Bitmap> CACHE = new LruCache<Integer, Bitmap>(CACHE_BYTES) {
        @Override
        protected int sizeOf(Integer key, Bitmap value) {
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

    /** Loads {@code base64Blob} for {@code key}, invoking {@code callback} on the main thread. */
    public static void load(int key, String base64Blob, Callback callback) {
        Bitmap cached = CACHE.get(key);
        if (cached != null) {
            if (callback != null) callback.onLoaded(cached);
            return;
        }
        EXECUTOR.execute(() -> {
            Bitmap bitmap = decodeSampled(base64Blob);
            if (bitmap != null) {
                CACHE.put(key, bitmap);
            }
            final Bitmap result = bitmap;
            MAIN.post(() -> {
                if (callback != null) callback.onLoaded(result);
            });
        });
    }

    public static void evict(int key) {
        CACHE.remove(key);
    }

    private static Bitmap decodeSampled(String blob) {
        if (blob == null || blob.isEmpty()) return null;
        try {
            byte[] data = Base64.decode(blob, Base64.DEFAULT);
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
        }
    }
}
