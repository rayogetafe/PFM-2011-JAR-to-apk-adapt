package pfm.android.compat.lcdui;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import java.io.IOException;
import java.io.InputStream;
import pfm.android.AndroidRuntime;

public final class Image {
    final Bitmap bitmap;
    private Image(Bitmap b) { bitmap = b; }

    public static Image createImage(int width, int height) {
        return new Image(Bitmap.createBitmap(Math.max(1,width), Math.max(1,height), Bitmap.Config.ARGB_8888));
    }

    public static Image createImage(String name) throws IOException {
        String p = name.startsWith("/") ? name.substring(1) : name;
        InputStream in = null;
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl != null) in = cl.getResourceAsStream(p);
        if (in == null) in = Image.class.getClassLoader().getResourceAsStream(p);
        if (in == null) {
            try { in = AndroidRuntime.activity().getAssets().open(p); } catch (IOException ignored) {}
        }
        if (in == null) throw new IOException("Resource not found: " + name);
        try {
            Bitmap b = BitmapFactory.decodeStream(in);
            if (b == null) throw new IOException("Unsupported image: " + name);
            return new Image(b.copy(Bitmap.Config.ARGB_8888, true));
        } finally { in.close(); }
    }

    public static Image fitted(String name, int width, int height) throws IOException {
        Image source = createImage(name);
        Image target = createImage(width, height);
        float scale = Math.min((float) width / source.getWidth(), (float) height / source.getHeight());
        int w = Math.max(1, Math.round(source.getWidth() * scale));
        int h = Math.max(1, Math.round(source.getHeight() * scale));
        int left = (width - w) / 2, top = (height - h) / 2;
        new Canvas(target.bitmap).drawBitmap(source.bitmap, null, new Rect(left, top, left + w, top + h),
                new Paint(Paint.FILTER_BITMAP_FLAG));
        return target;
    }

    public Graphics getGraphics() { return new Graphics(bitmap); }
    public int getWidth() { return bitmap.getWidth(); }
    public int getHeight() { return bitmap.getHeight(); }
}
