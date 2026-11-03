package com.majnioui.tv.prayer;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.app.LocaleManager;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.LocaleList;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Today's prayer times on the launcher's Home, under the clock, without opening the app (the launcher's
 * game_day contract, docs/modules.md). One row: a header with the place and the Hijri date, the whole day as a
 * table, and the next prayer, which Home colours and counts down to. Read-only and offline: it answers from the times the app saved; when they are not today's it asks
 * Aladhan once in the background and tells Home when the new times are in.
 */
public class TurnsProvider extends ContentProvider {
    static final String AUTHORITY = "com.majnioui.tv.prayer.turns";
    static final Uri TURNS = Uri.parse("content://" + AUTHORITY + "/turns");
    private static final String[] COLUMNS = {"top_line", "word", "start_ms", "end_ms", "colour", "calendar", "turn_seconds", "table"};
    /** The next prayer in warm amber: it reads on any photo, where the app's green can sink into leaves or sea. */
    private static final int NEXT_COLOUR = 0xFFFFB300;
    /** Skip Sunrise: it is shown on the top line but is not a prayer to count down to. */
    private static final int[] PRAYERS = {0, 2, 3, 4, 5};
    private static final long MINUTE_MS = 60_000L;

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] args, String sort) {
        Context ctx = getContext();
        MatrixCursor out = new MatrixCursor(COLUMNS);
        if (ctx == null) return out;
        ctx = inAppLanguage(ctx);
        SharedPreferences prefs = ctx.getSharedPreferences(PrayerScheduler.PREFS_NAME, Context.MODE_PRIVATE);
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(new Date());
        if (!today.equals(prefs.getString(PrayerScheduler.KEY_DATE, ""))) PrayerScheduler.fetchToday(ctx);

        String[] names = PrayerScheduler.NAMES;
        int[] minutes = new int[names.length];
        // The day as a table, one cell per time, Sunrise included: Home draws each as its name over its time.
        StringBuilder table = new StringBuilder();
        for (int i = 0; i < names.length; i++) {
            minutes[i] = prefs.getInt("min_" + names[i], -1);
            if (minutes[i] < 0) return out;   // nothing saved yet: no panel until the first fetch is in
            if (i > 0) table.append('|');
            table.append(ctx.getString(PrayerScheduler.NAME_RES[i])).append(' ').append(prefs.getString("time_" + names[i], ""));
        }
        // "Waktu Sholat Jakarta  •  22 Jumadil Awal 1448 H"; the date is left out until a fetch has saved it.
        String top = ctx.getString(R.string.home_header, ctx.getString(R.string.place));
        int month = prefs.getInt("hijri_month", 0);
        String[] months = ctx.getResources().getStringArray(R.array.hijri_months);
        if (month >= 1 && month <= months.length) {
            top += "  •  " + prefs.getString("hijri_day", "") + " " + months[month - 1] + " "
                    + prefs.getString("hijri_year", "") + " H";
        }

        Calendar now = Calendar.getInstance();
        int nowMin = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
        int next = -1;
        for (int i : PRAYERS) {
            if (minutes[i] > nowMin) { next = i; break; }
        }
        Calendar at = (Calendar) now.clone();
        if (next < 0) {
            // After Isya: tomorrow's Subuh, a minute or so off until tomorrow's times are fetched.
            next = 0;
            at.add(Calendar.DAY_OF_YEAR, 1);
        }
        at.set(Calendar.HOUR_OF_DAY, minutes[next] / 60);
        at.set(Calendar.MINUTE, minutes[next] % 60);
        at.set(Calendar.SECOND, 0);
        at.set(Calendar.MILLISECOND, 0);
        long start = at.getTimeInMillis();
        String word = ctx.getString(PrayerScheduler.NAME_RES[next]) + " " + prefs.getString("time_" + names[next], "");
        // The row ends a minute after the adhan, so Home asks again and gets the prayer after it.
        out.addRow(new Object[] {top, word, start, start + MINUTE_MS, NEXT_COLOUR, "prayer", 60, table.toString()});
        return out;
    }

    /**
     * The app's own language (Settings, Apps, Language on Android 13+). A process started only for this provider
     * answers in the TV's language otherwise, so the names on Home could differ from the app's.
     */
    private static Context inAppLanguage(Context ctx) {
        if (Build.VERSION.SDK_INT < 33) return ctx;
        LocaleList app = ctx.getSystemService(LocaleManager.class).getApplicationLocales();
        if (app.isEmpty()) return ctx;
        Configuration c = new Configuration(ctx.getResources().getConfiguration());
        c.setLocales(app);
        return ctx.createConfigurationContext(c);
    }

    static void changed(Context ctx) {
        ctx.getContentResolver().notifyChange(TURNS, null);
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException("read-only");
    }

    @Override
    public int delete(Uri uri, String selection, String[] args) {
        throw new UnsupportedOperationException("read-only");
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] args) {
        throw new UnsupportedOperationException("read-only");
    }
}
