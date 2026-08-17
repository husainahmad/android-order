package com.harmoni.pos.order.util;

import android.content.Context;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.harmoni.pos.order.data.model.Category;
import com.harmoni.pos.order.data.model.Product;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.List;

public class JsonCache {

    private static final Gson GSON = new GsonBuilder()
            .addSerializationExclusionStrategy(new NoImageBlobStrategy())
            .addDeserializationExclusionStrategy(new NoImageBlobStrategy())
            .create();
    private static final String CATEGORY_FILE = "category_cache.json";
    private static final String PRODUCT_PREFIX = "product_cache_";

    private JsonCache() {}

    private static File cacheDir(Context context) {
        return context.getCacheDir();
    }

    public static List<Category> readCategories(Context context) {
        return read(cacheDir(context), CATEGORY_FILE, new TypeToken<List<Category>>() {}.getType());
    }

    public static void writeCategories(Context context, List<Category> categories) {
        write(cacheDir(context), CATEGORY_FILE, categories);
    }

    public static List<Product> readProducts(Context context, int categoryId) {
        return read(cacheDir(context), PRODUCT_PREFIX + categoryId + ".json", new TypeToken<List<Product>>() {}.getType());
    }

    public static void writeProducts(Context context, int categoryId, List<Product> products) {
        write(cacheDir(context), PRODUCT_PREFIX + categoryId + ".json", products);
    }

    public static void clear(Context context) {
        File dir = cacheDir(context);
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.getName().endsWith(".json")) {
                f.delete();
            }
        }
    }

    private static <T> T read(File dir, String name, Type type) {
        File file = new File(dir, name);
        if (!file.exists()) return null;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            return GSON.fromJson(reader, type);
        } catch (Exception e) {
            return null;
        }
    }

    private static void write(File dir, String name, Object data) {
        File file = new File(dir, name);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            GSON.toJson(data, writer);
        } catch (Exception ignored) {
        }
    }

    /** Keeps heavy base64 image blobs out of the on-disk cache so reads/writes stay fast. */
    private static final class NoImageBlobStrategy implements ExclusionStrategy {
        @Override
        public boolean shouldSkipField(FieldAttributes f) {
            return "imageBlob".equals(f.getName());
        }

        @Override
        public boolean shouldSkipClass(Class<?> clazz) {
            return false;
        }
    }
}
