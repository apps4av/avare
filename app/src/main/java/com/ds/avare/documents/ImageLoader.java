/*
Copyright (c) 2026, Apps4Av Inc. (apps4av.com)
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.ds.avare.documents;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import android.widget.ImageView;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Small image loader for the Documents screen: internet pictures (with a disk
 * cache), bundled thumbnails / covers from assets, and local picture files.
 * Decoding is done off the UI thread and the result is cached in memory.
 */
public final class ImageLoader {

    /** Internet pictures newer than this are not fetched again. */
    private static final long FRESH_MS = 5 * 60 * 1000L;
    private static final int CONNECT_TIMEOUT_MS = 15000;

    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(3);
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final LruCache<String, Bitmap> CACHE =
            new LruCache<String, Bitmap>((int) (Runtime.getRuntime().maxMemory() / 8 / 1024)) {
                @Override
                protected int sizeOf(String key, Bitmap value) {
                    return value.getByteCount() / 1024;
                }
            };

    private ImageLoader() { }

    /** Called on the main thread when a picture is ready (null if it failed). */
    public interface Callback {
        void onLoaded(Bitmap bitmap);
    }

    /** Where the pixels come from. */
    private interface Source {
        Bitmap load() throws Exception;
    }

    /** Show a picture bundled in assets (cover or PDF thumbnail) in the view. */
    public static void showAsset(final Context context, final String path, ImageView view,
                                 final int maxDim, int fallbackRes) {
        final Context app = context.getApplicationContext();
        show("asset:" + path, view, fallbackRes, new Source() {
            @Override
            public Bitmap load() throws Exception {
                InputStream in = app.getAssets().open(path);
                try {
                    return decode(readAll(in), maxDim);
                } finally {
                    in.close();
                }
            }
        });
    }

    /** Show a local picture file in the view. */
    public static void showFile(final File file, ImageView view, final int maxDim, int fallbackRes) {
        show("file:" + file.getAbsolutePath() + ":" + file.lastModified(), view, fallbackRes, new Source() {
            @Override
            public Bitmap load() throws Exception {
                return decodeFile(file, maxDim);
            }
        });
    }

    /** Show an internet picture in the view. */
    public static void showUrl(final Context context, final String url, ImageView view,
                               final int maxDim, int fallbackRes) {
        final File dir = new File(context.getApplicationContext().getCacheDir(), "documents");
        show("url:" + url, view, fallbackRes, new Source() {
            @Override
            public Bitmap load() throws Exception {
                return fetch(dir, url, maxDim);
            }
        });
    }

    /** Load an internet picture and hand it to the callback on the main thread. */
    public static void loadUrl(final Context context, final String url, final int maxDim,
                               final Callback callback) {
        final File dir = new File(context.getApplicationContext().getCacheDir(), "documents");
        load("url:" + url, callback, new Source() {
            @Override
            public Bitmap load() throws Exception {
                return fetch(dir, url, maxDim);
            }
        });
    }

    /** Load a local picture file and hand it to the callback on the main thread. */
    public static void loadFile(final File file, final int maxDim, final Callback callback) {
        load("file:" + file.getAbsolutePath() + ":" + file.lastModified(), callback, new Source() {
            @Override
            public Bitmap load() throws Exception {
                return decodeFile(file, maxDim);
            }
        });
    }

    private static void show(final String key, final ImageView view, final int fallbackRes,
                             final Source source) {
        view.setTag(key);
        Bitmap cached = CACHE.get(key);
        if (cached != null) {
            view.setImageBitmap(cached);
            return;
        }
        if (fallbackRes != 0) {
            view.setImageResource(fallbackRes);
        } else {
            view.setImageDrawable(null);
        }
        load(key, new Callback() {
            @Override
            public void onLoaded(Bitmap bitmap) {
                // the view may have been recycled for another row meanwhile
                if (bitmap != null && key.equals(view.getTag())) {
                    view.setImageBitmap(bitmap);
                }
            }
        }, source);
    }

    private static void load(final String key, final Callback callback, final Source source) {
        Bitmap cached = CACHE.get(key);
        if (cached != null) {
            callback.onLoaded(cached);
            return;
        }
        EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                Bitmap bitmap = null;
                try {
                    bitmap = source.load();
                } catch (Throwable ignored) {
                    // leave null, the caller keeps its fallback
                }
                if (bitmap != null) {
                    CACHE.put(key, bitmap);
                }
                final Bitmap result = bitmap;
                MAIN.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onLoaded(result);
                    }
                });
            }
        });
    }

    private static Bitmap fetch(File dir, String url, int maxDim) throws Exception {
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IllegalStateException("no cache dir");
        }
        File cached = new File(dir, md5(url));
        boolean fresh = cached.isFile()
                && System.currentTimeMillis() - cached.lastModified() < FRESH_MS;
        if (!fresh) {
            try {
                download(url, cached);
            } catch (Exception e) {
                // offline: an older copy is better than nothing
                if (!cached.isFile()) {
                    throw e;
                }
            }
        }
        return decodeFile(cached, maxDim);
    }

    private static void download(String url, File target) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
        conn.setReadTimeout(CONNECT_TIMEOUT_MS);
        File part = new File(target.getPath() + ".part");
        try {
            if (conn.getResponseCode() != 200) {
                throw new IllegalStateException("HTTP " + conn.getResponseCode());
            }
            InputStream in = new BufferedInputStream(conn.getInputStream());
            FileOutputStream out = new FileOutputStream(part);
            try {
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) > 0) {
                    out.write(buf, 0, n);
                }
            } finally {
                out.close();
                in.close();
            }
            if (target.exists() && !target.delete()) {
                throw new IllegalStateException("cannot replace cache file");
            }
            if (!part.renameTo(target)) {
                throw new IllegalStateException("cannot store cache file");
            }
        } finally {
            conn.disconnect();
            if (part.exists()) {
                part.delete();
            }
        }
    }

    private static byte[] readAll(InputStream in) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) {
            out.write(buf, 0, n);
        }
        return out.toByteArray();
    }

    private static Bitmap decodeFile(File file, int maxDim) throws Exception {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file.getPath(), bounds);
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, maxDim);
        Bitmap bitmap = BitmapFactory.decodeFile(file.getPath(), opts);
        if (bitmap == null) {
            throw new IllegalStateException("not a picture");
        }
        return bitmap;
    }

    private static Bitmap decode(byte[] data, int maxDim) {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(data, 0, data.length, bounds);
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, maxDim);
        return BitmapFactory.decodeByteArray(data, 0, data.length, opts);
    }

    private static int sampleSize(int width, int height, int maxDim) {
        int sample = 1;
        if (maxDim <= 0) {
            return sample;
        }
        while (width / (sample * 2) >= maxDim || height / (sample * 2) >= maxDim) {
            sample *= 2;
        }
        return sample;
    }

    private static String md5(String text) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] digest = md.digest(text.getBytes("UTF-8"));
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
