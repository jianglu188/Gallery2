package com.android.gallery3d.ui;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Movie;
import android.graphics.RectF;
import android.net.Uri;
import android.os.SystemClock;

import com.android.gallery3d.common.Utils;
import com.android.gallery3d.glrenderer.BitmapTexture;
import com.android.gallery3d.glrenderer.GLCanvas;
import com.android.photos.data.GalleryBitmapPool;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class GifScreenNail implements ScreenNail {
    private static final String TAG = "GifScreenNail";
    private Bitmap mBitmap;
    private GifScreenView mGifScreen;
    private int mHeight;
    private int mWidth;

    public GifScreenNail(Bitmap bitmap, Activity activity, Uri uri) {
        mWidth = bitmap.getWidth();
        mHeight = bitmap.getHeight();
        mBitmap = bitmap;
        mGifScreen = new GifScreenView(activity.getApplicationContext(), uri, bitmap);
    }

    private static void recycleBitmap(Bitmap bitmap) {
        if (bitmap == null) return;
        GalleryBitmapPool.getInstance().put(bitmap);
    }

    public void draw(GLCanvas canvas, int x, int y, int width, int height) {
        mGifScreen.draw(canvas, x, y, width, height);
    }

    public void draw(GLCanvas canvas, RectF source, RectF dest) {
        mGifScreen.draw(canvas, source, dest);
    }

    public int getHeight() {
        return mHeight;
    }

    public int getWidth() {
        return mWidth;
    }

    public boolean isAnimating() {
        return false;
    }

    public boolean isGifPic() {
        return mGifScreen.isGifPic();
    }

    public void noDraw() {
    }

    public void recycle() {
        if (mGifScreen != null) {
            mGifScreen.recycle();
            mGifScreen = null;
        }
        recycleBitmap(mBitmap);
        mBitmap = null;
    }

    public void startTime() {
        mGifScreen.startTime();
    }

    private class GifScreenView {
        private Canvas mCanvas;
        private Context mContext;
        private int mDegree = 0;
        private Movie mMovie;
        private long mMovieStart;
        private BitmapTexture mTexture;
        private Bitmap mUploadBitmap;

        public GifScreenView(Context context, Uri uri, Bitmap bitmap) {
            mContext = context;
            if (uri == null)
                throw new IllegalArgumentException("uri cannot be null");

            InputStream inputStream = null;
            try {
                inputStream = mContext.getContentResolver().openInputStream(uri);
                byte[] bytes = streamToBytes(inputStream);
                mMovie = Movie.decodeByteArray(bytes, 0, bytes.length);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } finally {
                Utils.closeSilently(inputStream);
            }

            if (mMovie == null) return;
            if (mMovie.duration() == 0) {
                Log.w("TiledScreenNail", "mMovie.duration() = 0: " + uri.toString());
            }
            mUploadBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), getConfig(bitmap));
            mCanvas = new Canvas(mUploadBitmap);
            mCanvas.drawBitmap(bitmap, 0.0F, 0.0F, null);
            mTexture = new BitmapTexture(mUploadBitmap);
        }

        private Bitmap.Config getConfig(Bitmap bitmap) {
            Bitmap.Config config = bitmap.getConfig();
            if (config == null) {
                config = Bitmap.Config.ARGB_8888;
            }
            return config;
        }

        private byte[] streamToBytes(InputStream inputStream) {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            byte[] buff = new byte[100];
            int length = 0;
            try {
                while ((length = inputStream.read(buff, 0, 100)) > 0) {
                    outputStream.write(buff, 0, length);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return outputStream.toByteArray();
        }

        protected void draw(Canvas canvas) {
            if (mMovie == null) return;
            canvas.drawColor(-1);
            long current = SystemClock.uptimeMillis();
            if (mMovieStart == 0L) mMovieStart = current;

            int duration = mMovie.duration();
            if (duration == 0) duration = 2000;
            int time = (int) ((current - mMovieStart) % duration);
            mMovie.setTime(time);
            int width = mMovie.width();
            int height = mMovie.height();
            int w = canvas.getWidth();
            int h = canvas.getHeight();
            canvas.save();
            canvas.rotate(mDegree, w / 2, h / 2);
            mMovie.draw(canvas, (w - width) / 2, (h - height) / 2);
            canvas.restore();
        }

        public void draw(GLCanvas canvas, int x, int y, int w, int h) {
            if (mTexture != null)
                mTexture.yield();
            draw(mCanvas);
            if (mTexture != null)
                mTexture.draw(canvas, x, y, w, h);
        }

        public void draw(GLCanvas canvas, RectF source, RectF dest) {
            if (mTexture != null)
                mTexture.yield();
            draw(mCanvas);
            if (mTexture != null)
                mTexture.draw(canvas, source, dest);
        }

        public boolean isGifPic() {
            return mMovie != null;
        }

        public void recycle() {
            if (mTexture != null) {
                mTexture.recycle();
                mTexture = null;
            }
            if (mUploadBitmap != null) {
                mUploadBitmap.recycle();
                mUploadBitmap = null;
            }
            mCanvas = null;
        }

        public void startTime() {
            mMovieStart = 0L;
        }
    }
}