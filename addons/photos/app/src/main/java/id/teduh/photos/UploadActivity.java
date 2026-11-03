package id.teduh.photos;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import io.nayuki.qrcodegen.QrCode;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.Locale;

/**
 * "Tambah dari HP": a QR code with the address of the upload page, served from this TV while the screen is open.
 * The phone, on the same Wi-Fi, opens it with its camera and sends photos; each one shows up here as it arrives.
 * A new random code each time the screen opens; leaving the screen stops the server.
 */
public class UploadActivity extends Activity {
    private static final String TAG = "TeduhPhotos";

    private PhotoServer server;
    private ImageView qr;
    private TextView address, received;
    private int count;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        int pad = Ui.dp(this, 64);
        LinearLayout page = new LinearLayout(this);
        page.setBackgroundColor(Ui.BACKGROUND);
        page.setGravity(Gravity.CENTER_VERTICAL);
        page.setPadding(pad, pad, pad, pad);

        LinearLayout left = new LinearLayout(this);
        left.setOrientation(LinearLayout.VERTICAL);
        left.addView(Ui.text(this, "Kirim foto dari HP", 36, Ui.TEXT, true));
        String[] steps = {
                "Sambungkan HP ke Wi-Fi yang sama dengan TV.",
                "Buka kamera HP, arahkan ke kode QR, lalu buka tautannya.",
                "Ketuk Pilih foto, pilih satu atau beberapa foto.",
        };
        for (int i = 0; i < steps.length; i++) {
            LinearLayout step = new LinearLayout(this);
            step.setGravity(Gravity.CENTER_VERTICAL);
            TextView n = Ui.text(this, String.valueOf(i + 1), 18, Ui.BACKGROUND, true);
            n.setGravity(Gravity.CENTER);
            n.setBackground(Ui.round(Ui.ACCENT, Ui.dp(this, 18)));
            step.addView(n, new LinearLayout.LayoutParams(Ui.dp(this, 36), Ui.dp(this, 36)));
            TextView t = Ui.text(this, steps[i], 20, Ui.TEXT, false);
            LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(-2, -2);
            tp.setMarginStart(Ui.dp(this, 18));
            step.addView(t, tp);
            LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-2, -2);
            sp.topMargin = Ui.dp(this, i == 0 ? 36 : 20);
            left.addView(step, sp);
        }
        TextView or = Ui.text(this, "Tanpa kamera? Ketik alamat ini di browser HP:", 16, Ui.MUTED, false);
        LinearLayout.LayoutParams op = new LinearLayout.LayoutParams(-2, -2);
        op.topMargin = Ui.dp(this, 40);
        left.addView(or, op);
        address = Ui.text(this, "", 24, Ui.TEXT, true);
        LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(-2, -2);
        ap.topMargin = Ui.dp(this, 10);
        left.addView(address, ap);
        received = Ui.text(this, "Belum ada foto diterima", 18, Ui.MUTED, false);
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(-2, -2);
        rp.topMargin = Ui.dp(this, 36);
        left.addView(received, rp);
        page.addView(left, new LinearLayout.LayoutParams(0, -2, 1));

        LinearLayout right = new LinearLayout(this);
        right.setOrientation(LinearLayout.VERTICAL);
        right.setGravity(Gravity.CENTER_HORIZONTAL);
        qr = new ImageView(this);
        qr.setBackground(Ui.round(Color.WHITE, Ui.dp(this, 20)));
        int qrPad = Ui.dp(this, 18);
        qr.setPadding(qrPad, qrPad, qrPad, qrPad);
        int size = Ui.dp(this, 300);
        right.addView(qr, new LinearLayout.LayoutParams(size, size));
        TextView done = Ui.pill(this, "Selesai", v -> finish());
        LinearLayout.LayoutParams dp = new LinearLayout.LayoutParams(-2, -2);
        dp.topMargin = Ui.dp(this, 28);
        right.addView(done, dp);
        LinearLayout.LayoutParams rightParams = new LinearLayout.LayoutParams(-2, -2);
        rightParams.setMarginStart(Ui.dp(this, 48));
        page.addView(right, rightParams);
        setContentView(page);
        done.requestFocus();
    }

    @Override
    protected void onStart() {
        super.onStart();
        String ip = homeAddress();
        if (ip == null) {
            address.setText("TV belum tersambung ke jaringan");
            qr.setImageBitmap(null);
            return;
        }
        String code = String.format(Locale.ROOT, "%06d", new SecureRandom().nextInt(1_000_000));
        try {
            server = new PhotoServer(this, code, name -> {
                count++;
                received.setText(count + " foto diterima. Kirim lagi, atau pilih Selesai.");
                received.setTextColor(0xFF2BB673);
            });
            server.start();
        } catch (IOException e) {
            Log.w(TAG, "server: " + e);
            address.setText("Gagal menyiapkan penerimaan foto");
            return;
        }
        String url = "http://" + ip + ":" + server.port() + "/" + code;
        address.setText(url.substring("http://".length()));
        qr.setImageBitmap(qrBitmap(url));
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (server != null) server.close();
        server = null;
    }

    /** The TV's address on the home network: the first site-local IPv4 of an interface that is up. */
    private static String homeAddress() {
        try {
            for (NetworkInterface nif : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!nif.isUp() || nif.isLoopback()) continue;
                for (InetAddress a : Collections.list(nif.getInetAddresses())) {
                    if (a instanceof Inet4Address && a.isSiteLocalAddress()) return a.getHostAddress();
                }
            }
        } catch (SocketException e) {
            Log.w(TAG, "address: " + e);
        }
        return null;
    }

    private static Bitmap qrBitmap(String text) {
        QrCode code = QrCode.encodeText(text, QrCode.Ecc.MEDIUM);
        int n = code.size;
        int scale = Math.max(1, 640 / n);
        Bitmap b = Bitmap.createBitmap(n * scale, n * scale, Bitmap.Config.RGB_565);
        int[] row = new int[n * scale];
        for (int y = 0; y < n; y++) {
            for (int x = 0; x < n; x++) {
                int c = code.getModule(x, y) ? Color.BLACK : Color.WHITE;
                for (int k = 0; k < scale; k++) row[x * scale + k] = c;
            }
            for (int k = 0; k < scale; k++) b.setPixels(row, 0, n * scale, 0, y * scale + k, n * scale, 1);
        }
        return b;
    }
}
