package id.teduh.photos;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Turns a photo from a phone into a Home wallpaper: upright (the phone's EXIF rotation applied), scaled down until it
 * just covers a 1920x1080 screen, saved as JPEG into the launcher's collection. A 12 MP phone photo of 5 MB becomes
 * about 400 KB, which a 1 GB TV decodes quickly.
 */
final class PhotoImport {
    private static final int SCREEN_W = 1920, SCREEN_H = 1080;
    private static int counter;

    private PhotoImport() {}

    /** Saves the picture in file and returns its name in the collection; IOException when it is not a picture. */
    static String save(ContentResolver resolver, File file) throws IOException {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file.getPath(), bounds);
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw new IOException("not a picture");

        int rotation = rotation(file);
        boolean sideways = rotation == 90 || rotation == 270;
        // Width and height as the picture will stand, after the rotation.
        int w = sideways ? bounds.outHeight : bounds.outWidth;
        int h = sideways ? bounds.outWidth : bounds.outHeight;

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 1;
        while (w / (options.inSampleSize * 2) >= SCREEN_W && h / (options.inSampleSize * 2) >= SCREEN_H) {
            options.inSampleSize *= 2;
        }
        Bitmap decoded;
        try {
            decoded = BitmapFactory.decodeFile(file.getPath(), options);
        } catch (OutOfMemoryError e) {
            throw new IOException("too large");
        }
        if (decoded == null) throw new IOException("not a picture");

        int dw = sideways ? decoded.getHeight() : decoded.getWidth();
        int dh = sideways ? decoded.getWidth() : decoded.getHeight();
        float scale = Math.min(1f, Math.max(SCREEN_W / (float) dw, SCREEN_H / (float) dh));
        Matrix matrix = new Matrix();
        matrix.postRotate(rotation);
        matrix.postScale(scale, scale);
        Bitmap out = Bitmap.createBitmap(decoded, 0, 0, decoded.getWidth(), decoded.getHeight(), matrix, true);
        if (out != decoded) decoded.recycle();

        String name = newName();
        try (OutputStream os = resolver.openOutputStream(Launcher.photo(name), "w")) {
            if (os == null || !out.compress(Bitmap.CompressFormat.JPEG, 88, os)) throw new IOException("write failed");
        } finally {
            out.recycle();
        }
        return name;
    }

    /** foto-20261103-161502-1.jpg: sorts by the time it arrived, and fits the launcher's file name rules. */
    private static synchronized String newName() {
        String time = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.ROOT).format(new Date());
        return "foto-" + time + "-" + (++counter) + ".jpg";
    }

    private static int rotation(File file) {
        try {
            switch (new ExifInterface(file.getPath()).getAttributeInt(ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL)) {
                case ExifInterface.ORIENTATION_ROTATE_90: return 90;
                case ExifInterface.ORIENTATION_ROTATE_180: return 180;
                case ExifInterface.ORIENTATION_ROTATE_270: return 270;
                default: return 0;
            }
        } catch (IOException e) {
            return 0;
        }
    }
}
