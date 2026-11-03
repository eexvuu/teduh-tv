package id.teduh.photos;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * The launcher's wallpaper collection, through its ThemeProvider (launcher/.../ThemeProvider.java): the same
 * folder tools/theme.sh fills from a computer. Every call needs WRITE_SECURE_SETTINGS, granted over ADB at install;
 * without it they throw SecurityException.
 */
final class Launcher {
    private static final Uri BASE = Uri.parse("content://com.gathin.tvhome.theme");
    /** The launcher reads at most this many pictures from the collection. */
    static final int MAX_PHOTOS = 100;

    private Launcher() {}

    /** The pictures' names, sorted as Home rotates them; null when the launcher is missing or too old to list them. */
    static List<String> list(ContentResolver resolver) {
        try (Cursor c = resolver.query(BASE.buildUpon().appendPath("wallpapers").build(), null, null, null, null)) {
            if (c == null) return null;
            List<String> names = new ArrayList<>();
            while (c.moveToNext()) names.add(c.getString(0));
            return names;
        }
    }

    static Uri photo(String name) {
        return BASE.buildUpon().appendPath("wallpapers").appendPath(name).build();
    }

    static void delete(ContentResolver resolver, String name) {
        resolver.delete(photo(name), null, null);
    }

    /** Shows this picture on Home and stops the rotation; null goes back to the rotation through all of them. */
    static void choose(ContentResolver resolver, String name) {
        ContentValues values = new ContentValues();
        values.put("choice", name);
        resolver.update(BASE.buildUpon().appendPath("choice").build(), values, null, null);
    }

    /** A small copy for the gallery, about width pixels across; null when it cannot be read. */
    static Bitmap thumbnail(ContentResolver resolver, String name, int width) {
        try {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            try (InputStream in = resolver.openInputStream(photo(name))) {
                BitmapFactory.decodeStream(in, null, bounds);
            }
            if (bounds.outWidth <= 0) return null;
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 1;
            while (bounds.outWidth / (options.inSampleSize * 2) >= width) options.inSampleSize *= 2;
            options.inPreferredConfig = Bitmap.Config.RGB_565;
            try (InputStream in = resolver.openInputStream(photo(name))) {
                return BitmapFactory.decodeStream(in, null, options);
            }
        } catch (Exception | OutOfMemoryError e) {
            return null;
        }
    }
}
