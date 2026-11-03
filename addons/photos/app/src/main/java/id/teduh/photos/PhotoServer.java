package id.teduh.photos;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

/**
 * The page a phone opens from the QR code, and the uploads it sends: plain HTTP on the home network, only while
 * the "Tambah dari HP" screen is open (UploadActivity starts and closes it). Every path starts with a random code
 * shown in the QR, so another device on the network that guesses the port gets nothing. One connection at a time;
 * each photo is one POST whose body is the file.
 */
final class PhotoServer extends Thread {
    interface Listener {
        void received(String name);
    }

    private static final String TAG = "TeduhPhotos";
    static final int PORT = 8765;
    /** A phone photo is 2 to 10 MB; anything far larger is not one. */
    private static final long MAX_BYTES = 40L * 1024 * 1024;

    private final Context ctx;
    private final String code;
    private final Listener listener;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ServerSocket socket;
    private volatile boolean closed;

    PhotoServer(Context ctx, String code, Listener listener) throws IOException {
        super("photo-server");
        this.ctx = ctx.getApplicationContext();
        this.code = code;
        this.listener = listener;
        ServerSocket s = new ServerSocket();
        s.setReuseAddress(true);
        try {
            s.bind(new InetSocketAddress(PORT));
        } catch (IOException taken) {
            s.close();
            s = new ServerSocket();
            s.bind(new InetSocketAddress(0));
        }
        socket = s;
    }

    int port() {
        return socket.getLocalPort();
    }

    void close() {
        closed = true;
        try {
            socket.close();
        } catch (IOException ignored) {
            // closing anyway
        }
    }

    @Override
    public void run() {
        while (!closed) {
            try (Socket client = socket.accept()) {
                client.setSoTimeout(60_000);
                handle(client);
            } catch (IOException e) {
                if (!closed) Log.w(TAG, "request: " + e);
            }
        }
    }

    private void handle(Socket client) throws IOException {
        InputStream in = new BufferedInputStream(client.getInputStream());
        OutputStream out = client.getOutputStream();
        String[] request = line(in).split(" ");
        long length = 0;
        for (String header; !(header = line(in)).isEmpty(); ) {
            int colon = header.indexOf(':');
            if (colon > 0 && header.substring(0, colon).trim().equalsIgnoreCase("content-length")) {
                try {
                    length = Long.parseLong(header.substring(colon + 1).trim());
                } catch (NumberFormatException e) {
                    length = -1;
                }
            }
        }
        if (request.length < 2) return;
        String method = request[0], path = request[1];
        int query = path.indexOf('?');
        if (query >= 0) path = path.substring(0, query);
        String page = "/" + code;

        if (method.equals("GET") && (path.equals(page) || path.equals(page + "/"))) {
            reply(out, 200, "text/html; charset=utf-8", Page.HTML);
        } else if (method.equals("POST") && path.equals(page + "/upload")) {
            upload(in, out, length);
        } else {
            reply(out, 404, "text/plain; charset=utf-8", "Kode salah. Scan ulang kode QR di TV.");
        }
    }

    private void upload(InputStream in, OutputStream out, long length) throws IOException {
        if (length <= 0) {
            reply(out, 411, "text/plain; charset=utf-8", "Foto kosong.");
            return;
        }
        if (length > MAX_BYTES) {
            reply(out, 413, "text/plain; charset=utf-8", "Foto terlalu besar (maksimal 40 MB).");
            return;
        }
        List<String> now;
        try {
            now = Launcher.list(ctx.getContentResolver());
        } catch (RuntimeException e) {
            now = null;
        }
        if (now == null) {
            reply(out, 503, "text/plain; charset=utf-8", "Launcher Teduh TV tidak bisa dihubungi.");
            return;
        }
        if (now.size() >= Launcher.MAX_PHOTOS) {
            reply(out, 409, "text/plain; charset=utf-8", "TV sudah berisi 100 foto. Hapus beberapa dulu.");
            return;
        }
        File tmp = File.createTempFile("upload", ".img", ctx.getCacheDir());
        try {
            try (OutputStream file = new FileOutputStream(tmp)) {
                byte[] buf = new byte[64 * 1024];
                long left = length;
                while (left > 0) {
                    int n = in.read(buf, 0, (int) Math.min(buf.length, left));
                    if (n < 0) throw new IOException("upload cut short");
                    file.write(buf, 0, n);
                    left -= n;
                }
            }
            String name;
            try {
                name = PhotoImport.save(ctx.getContentResolver(), tmp);
            } catch (IOException e) {
                reply(out, 415, "text/plain; charset=utf-8", "Bukan foto yang bisa dibaca TV.");
                return;
            }
            reply(out, 200, "text/plain; charset=utf-8", "ok");
            main.post(() -> listener.received(name));
        } finally {
            //noinspection ResultOfMethodCallIgnored
            tmp.delete();
        }
    }

    private static void reply(OutputStream out, int status, String type, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        String head = String.format(Locale.ROOT, "HTTP/1.1 %d %s\r\nContent-Type: %s\r\nContent-Length: %d\r\n"
                + "Cache-Control: no-store\r\nConnection: close\r\n\r\n", status, status == 200 ? "OK" : "Error", type, bytes.length);
        out.write(head.getBytes(StandardCharsets.US_ASCII));
        out.write(bytes);
        out.flush();
    }

    /** One header line without its CRLF; at most 8 KB. */
    private static String line(InputStream in) throws IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        for (int c; (c = in.read()) >= 0 && c != '\n'; ) {
            if (c != '\r') b.write(c);
            if (b.size() > 8192) throw new IOException("header too long");
        }
        return b.toString("ISO-8859-1");
    }
}
