package id.teduh.photos;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The Home wallpapers, managed with the remote: every picture in the launcher's collection as a tile; OK on one
 * shows it on Home now or deletes it, "Tambah dari HP" adds more from a phone (UploadActivity), and "Ganti otomatis"
 * goes back to Home rotating through all of them.
 */
public class MainActivity extends Activity {
    private static final int COLUMNS = 4;

    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Map<String, Bitmap> thumbs = new HashMap<>();
    private LinearLayout grid;
    private TextView subtitle;
    private View addButton;
    private List<String> names = new ArrayList<>();
    /** The tile to focus after a reload, so a delete leaves focus next to where it was. */
    private int focusIndex = -1;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        int pad = Ui.dp(this, 56);

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(pad, Ui.dp(this, 44), pad, Ui.dp(this, 44));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        titles.addView(Ui.text(this, "Foto Latar", 36, Ui.TEXT, true));
        subtitle = Ui.text(this, "Memuat…", 16, Ui.MUTED, false);
        LinearLayout.LayoutParams sub = new LinearLayout.LayoutParams(-2, -2);
        sub.topMargin = Ui.dp(this, 10);
        titles.addView(subtitle, sub);
        header.addView(titles, new LinearLayout.LayoutParams(0, -2, 1));
        addButton = Ui.pill(this, "+  Tambah dari HP", v -> startActivity(new Intent(this, UploadActivity.class)));
        header.addView(addButton);
        View auto = Ui.pill(this, "Ganti otomatis", v -> choose(null));
        LinearLayout.LayoutParams autoParams = new LinearLayout.LayoutParams(-2, -2);
        autoParams.setMarginStart(Ui.dp(this, 16));
        header.addView(auto, autoParams);
        page.addView(header);

        grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL);
        grid.setClipChildren(false);
        page.setClipChildren(false);
        page.setClipToPadding(false);
        LinearLayout.LayoutParams gridParams = new LinearLayout.LayoutParams(-1, -2);
        gridParams.topMargin = Ui.dp(this, 36);
        page.addView(grid, gridParams);

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Ui.BACKGROUND);
        scroll.setFillViewport(true);
        // Room for a focused tile's zoom at the edges.
        scroll.setClipToPadding(false);
        scroll.addView(page);
        setContentView(scroll);
    }

    @Override
    protected void onResume() {
        super.onResume();
        reload();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        io.shutdownNow();
    }

    private void reload() {
        io.execute(() -> {
            List<String> list;
            String problem = null;
            try {
                list = Launcher.list(getContentResolver());
                if (list == null) problem = "Launcher Teduh TV belum terpasang atau versinya terlalu lama. Perbarui launcher-nya dulu.";
            } catch (SecurityException e) {
                list = null;
                problem = "Foto Latar belum diizinkan mengatur latar Home. Di komputer, minta Claude menjalankan:\n"
                        + "pm grant id.teduh.photos android.permission.WRITE_SECURE_SETTINGS";
            }
            List<String> found = list;
            String message = problem;
            main.post(() -> {
                if (isDestroyed()) return;
                if (message != null) showProblem(message);
                else show(found);
            });
        });
    }

    private void showProblem(String message) {
        grid.removeAllViews();
        subtitle.setText("Belum bisa dipakai");
        TextView box = Ui.text(this, message, 18, Ui.TEXT, false);
        box.setLineSpacing(Ui.dp(this, 6), 1f);
        box.setPadding(Ui.dp(this, 28), Ui.dp(this, 24), Ui.dp(this, 28), Ui.dp(this, 24));
        box.setBackground(Ui.round(Ui.SURFACE, Ui.dp(this, 18)));
        grid.addView(box);
        addButton.requestFocus();
    }

    private void show(List<String> list) {
        names = list;
        thumbs.keySet().retainAll(list);
        subtitle.setText(list.isEmpty() ? "Belum ada foto"
                : list.size() + " foto, bergantian sebagai latar Home");
        grid.removeAllViews();
        if (list.isEmpty()) {
            TextView empty = Ui.text(this, "Pilih \"Tambah dari HP\", lalu scan kode QR dengan HP untuk mengirim foto ke TV.",
                    20, Ui.MUTED, false);
            empty.setLineSpacing(Ui.dp(this, 6), 1f);
            grid.addView(empty);
            addButton.requestFocus();
            return;
        }
        int gap = Ui.dp(this, 24);
        int width = (getResources().getDisplayMetrics().widthPixels - 2 * Ui.dp(this, 56) - (COLUMNS - 1) * gap) / COLUMNS;
        int height = width * 9 / 16;
        LinearLayout row = null;
        List<View> tiles = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            if (i % COLUMNS == 0) {
                row = new LinearLayout(this);
                row.setClipChildren(false);   // a focused tile zooms past its row
                LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(-1, -2);
                if (i > 0) rp.topMargin = gap;
                grid.addView(row, rp);
            }
            View tile = tile(list.get(i), width, height);
            LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(width, height);
            if (i % COLUMNS > 0) tp.setMarginStart(gap);
            row.addView(tile, tp);
            tiles.add(tile);
        }
        TextView hint = Ui.text(this, "Pilih foto lalu tekan OK untuk menampilkannya sekarang atau menghapusnya.",
                15, Ui.MUTED, false);
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(-2, -2);
        hp.topMargin = Ui.dp(this, 32);
        grid.addView(hint, hp);
        int focus = focusIndex >= 0 ? Math.min(focusIndex, tiles.size() - 1) : 0;
        focusIndex = -1;
        tiles.get(focus).requestFocus();
        loadThumbs(list, width);
    }

    private View tile(String name, int width, int height) {
        FrameLayout tile = new FrameLayout(this);
        tile.setFocusable(true);
        tile.setClickable(true);
        tile.setTag(name);
        float radius = Ui.dp(this, 14);
        tile.setBackground(Ui.round(Ui.SURFACE, radius));
        tile.setClipToOutline(true);
        ImageView image = new ImageView(this);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        image.setImageBitmap(thumbs.get(name));
        tile.addView(image, new FrameLayout.LayoutParams(-1, -1));
        GradientDrawable ring = new GradientDrawable();
        ring.setCornerRadius(radius);
        ring.setStroke(Ui.dp(this, 4), 0xFFFFFFFF);
        tile.setOnFocusChangeListener((v, focused) -> {
            v.setForeground(focused ? ring : null);
            v.animate().scaleX(focused ? 1.06f : 1f).scaleY(focused ? 1.06f : 1f).setDuration(150).start();
            v.setElevation(focused ? Ui.dp(this, 8) : 0);
        });
        tile.setOnClickListener(v -> actions(name));
        return tile;
    }

    /** Thumbnails one by one off the main thread, each set on its tile as it is ready. */
    private void loadThumbs(List<String> list, int width) {
        for (String name : list) {
            if (thumbs.containsKey(name)) continue;
            io.execute(() -> {
                Bitmap b = Launcher.thumbnail(getContentResolver(), name, width);
                main.post(() -> {
                    if (b == null || isDestroyed()) return;
                    thumbs.put(name, b);
                    View tile = grid.findViewWithTag(name);
                    if (tile instanceof ViewGroup) ((ImageView) ((ViewGroup) tile).getChildAt(0)).setImageBitmap(b);
                });
            });
        }
    }

    private void actions(String name) {
        new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
                .setTitle("Foto ini")
                .setItems(new String[] {"Tampilkan di Home sekarang", "Hapus dari TV"}, (d, which) -> {
                    if (which == 0) choose(name);
                    else confirmDelete(name);
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    private void confirmDelete(String name) {
        new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
                .setTitle("Hapus foto ini?")
                .setMessage("Foto dihapus dari TV. Foto aslinya di HP tetap ada.")
                .setPositiveButton("Hapus", (d, w) -> {
                    focusIndex = names.indexOf(name);
                    io.execute(() -> {
                        try {
                            Launcher.delete(getContentResolver(), name);
                        } catch (RuntimeException ignored) {
                            // gone already, or the launcher went away: the reload shows what is there
                        }
                        main.post(this::reload);
                    });
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    /** One picture on Home (rotation stopped), or null for the rotation through all of them. */
    private void choose(String name) {
        try {
            Launcher.choose(getContentResolver(), name);
            Toast.makeText(this, name == null ? "Foto akan bergantian otomatis di Home."
                    : "Foto ini dipasang. Tekan Home untuk melihatnya.", Toast.LENGTH_LONG).show();
        } catch (RuntimeException e) {
            Toast.makeText(this, "Gagal mengubah latar Home.", Toast.LENGTH_LONG).show();
        }
    }
}
