package id.teduh.photos;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;

/** The few pieces both screens share: colours, the font, text and the pill buttons. Sizes in dp, for 10 feet. */
final class Ui {
    static final int BACKGROUND = 0xFF0B1014;
    static final int SURFACE = 0xFF18232A;
    static final int ACCENT = 0xFF3D8BFF;
    static final int TEXT = 0xFFF2F7F4;
    static final int MUTED = 0xFFA9BDB4;

    private static Typeface font;

    private Ui() {}

    static Typeface font(Context ctx) {
        if (font == null) {
            try {
                font = ctx.getResources().getFont(R.font.outfit);
            } catch (Exception e) {
                font = Typeface.DEFAULT;
            }
        }
        return font;
    }

    static int dp(Context ctx, float v) {
        return Math.round(v * ctx.getResources().getDisplayMetrics().density);
    }

    static TextView text(Context ctx, String s, float sp, int colour, boolean bold) {
        TextView t = new TextView(ctx);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(colour);
        t.setTypeface(font(ctx));
        // Outfit is a variable font whose default instance is its thinnest.
        t.setFontVariationSettings(bold ? "'wght' 600" : "'wght' 400");
        t.setIncludeFontPadding(false);
        return t;
    }

    static GradientDrawable round(int colour, float radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(colour);
        d.setCornerRadius(radius);
        return d;
    }

    /** A focusable pill: dark until focused, then white with dark text, as the launcher's top buttons. */
    static TextView pill(Context ctx, String label, View.OnClickListener click) {
        TextView b = text(ctx, label, 17, TEXT, true);
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(ctx, 26), dp(ctx, 14), dp(ctx, 26), dp(ctx, 14));
        b.setFocusable(true);
        b.setClickable(true);
        b.setOnClickListener(click);
        float r = dp(ctx, 28);
        b.setBackground(round(SURFACE, r));
        b.setOnFocusChangeListener((v, focused) -> {
            v.setBackground(round(focused ? 0xFFFFFFFF : SURFACE, r));
            ((TextView) v).setTextColor(focused ? 0xFF0B1014 : TEXT);
            v.animate().scaleX(focused ? 1.05f : 1f).scaleY(focused ? 1.05f : 1f).setDuration(150).start();
        });
        return b;
    }
}
