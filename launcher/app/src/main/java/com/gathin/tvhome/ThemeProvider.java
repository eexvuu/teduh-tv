package com.gathin.tvhome;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * Lets tools/theme.sh write the theme files into {@link Config#dir} on firmware where the adb shell is
 * locked out of Android/data (TCL G07, Android 11). Only the files {@link ThemeFiles} allows, and only for
 * callers holding WRITE_SECURE_SETTINGS, which the adb shell has and ordinary apps cannot get.
 *
 *   content write --uri content://com.gathin.tvhome.theme/config.json < /data/local/tmp/config.json
 *   content write --uri content://com.gathin.tvhome.theme/wallpapers/dusk.jpg < /data/local/tmp/dusk.jpg
 *   content delete --uri content://com.gathin.tvhome.theme/config.json
 *   content delete --uri content://com.gathin.tvhome.theme/wallpapers      (every image in the collection)
 *
 * A photos app on the TV (Teduh's Foto Latar), granted WRITE_SECURE_SETTINGS over adb at install, manages the collection
 * from the TV with it: query lists the pictures (one column, "name"), and update sets the on-TV choice that
 * Launcher settings' Wallpaper row sets (ContentValues "choice": a picture's name, "none", or null for the theme's
 * rotation). Every change moves Launcher settings' change count, so Home rebuilds with it when it comes back.
 */
public class ThemeProvider extends ContentProvider {

    private File file(Uri uri) throws FileNotFoundException {
        String path = ThemeFiles.path(uri.getPathSegments());
        File dir = Config.dir(getContext());
        if (dir == null || path == null) throw new FileNotFoundException(String.valueOf(uri));
        File f = new File(dir, path);
        File parent = f.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) throw new FileNotFoundException(String.valueOf(uri));
        return f;
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        File f = file(uri);
        if (mode.contains("w")) {
            changed(false, null);
            return ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_WRITE_ONLY
                    | ParcelFileDescriptor.MODE_CREATE | ParcelFileDescriptor.MODE_TRUNCATE);
        }
        return ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_READ_ONLY);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        changed(false, null);
        File dir = Config.dir(getContext());
        if (dir != null && ThemeFiles.isCollection(uri.getPathSegments())) {
            File[] images = new File(dir, ThemeFiles.WALLPAPERS).listFiles((d, name) -> ThemeFiles.collectionImage(name));
            int n = 0;
            if (images != null) for (File f : images) if (f.delete()) n++;
            return n;
        }
        try {
            return file(uri).delete() ? 1 : 0;
        } catch (FileNotFoundException e) {
            return 0;
        }
    }

    @Override
    public Cursor query(Uri uri, String[] p, String s, String[] a, String o) {
        MatrixCursor names = new MatrixCursor(new String[] {"name"});
        File dir = Config.dir(getContext());
        for (File f : WallpaperRotation.images(dir == null ? null : new File(dir, ThemeFiles.WALLPAPERS))) {
            names.addRow(new Object[] {f.getName()});
        }
        return names;
    }

    @Override
    public int update(Uri uri, ContentValues v, String s, String[] a) {
        changed(true, v == null ? null : v.getAsString("choice"));
        return 1;
    }

    /** Moves Launcher settings' change count (Home rebuilds on its next resume), and sets or clears the choice. */
    private void changed(boolean choose, String choice) {
        SharedPreferences settings = getContext().getSharedPreferences(Config.SETTINGS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor edit = settings.edit()
                .putLong(Config.SETTINGS_REVISION_KEY, settings.getLong(Config.SETTINGS_REVISION_KEY, 0) + 1);
        if (choose && choice == null) edit.remove(Config.WALLPAPER_CHOICE_KEY);
        else if (choose) edit.putString(Config.WALLPAPER_CHOICE_KEY, choice);
        edit.commit();
    }

    @Override public boolean onCreate() { return true; }
    @Override public String getType(Uri uri) { return null; }
    @Override public Uri insert(Uri uri, ContentValues values) { return null; }
}
