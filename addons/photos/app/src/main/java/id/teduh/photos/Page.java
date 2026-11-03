package id.teduh.photos;

/** The upload page the phone opens from the QR code. Self-contained: no fonts, scripts or images from elsewhere. */
final class Page {
    private Page() {}

    static final String HTML = """
            <!doctype html>
            <html lang="id">
            <head>
            <meta charset="utf-8">
            <meta name="viewport" content="width=device-width, initial-scale=1">
            <title>Kirim foto ke TV</title>
            <style>
            body{margin:0;font-family:system-ui,-apple-system,"Segoe UI",Roboto,sans-serif;background:#0b1014;color:#eef5f1}
            main{max-width:520px;margin:0 auto;padding:32px 20px 48px}
            h1{font-size:26px;margin:0 0 8px}
            p{color:#a9bdb4;margin:0 0 24px;line-height:1.5}
            label{display:block;text-align:center;background:#3d8bff;color:#fff;font-weight:700;padding:18px;border-radius:14px;font-size:18px}
            input{display:none}
            ul{list-style:none;padding:0;margin:24px 0 0}
            li{display:flex;gap:12px;align-items:center;padding:10px 0;border-bottom:1px solid #1d2a30}
            li img{width:72px;height:44px;object-fit:cover;border-radius:8px;background:#1d2a30;flex:none}
            li span{flex:1;font-size:14px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
            li b{font-size:14px;color:#a9bdb4;flex:none}
            li b.ok{color:#2bb673} li b.err{color:#ff8a80;white-space:normal;max-width:45%}
            </style>
            </head>
            <body>
            <main>
            <h1>Kirim foto ke TV</h1>
            <p>Pilih satu atau beberapa foto. Foto langsung masuk ke TV dan ikut bergantian sebagai latar Home.
            Foto mendatar (landscape) paling pas di layar TV.</p>
            <label for="f">Pilih foto</label>
            <input id="f" type="file" accept="image/*" multiple>
            <ul id="l"></ul>
            </main>
            <script>
            const f = document.getElementById('f'), l = document.getElementById('l');
            const url = location.pathname.replace(/\\/$/, '') + '/upload';
            f.onchange = async () => {
              const files = [...f.files];
              f.value = '';
              const rows = files.map(file => {
                const li = document.createElement('li');
                const img = document.createElement('img');
                img.src = URL.createObjectURL(file);
                const name = document.createElement('span');
                name.textContent = file.name;
                const state = document.createElement('b');
                state.textContent = 'Menunggu';
                li.append(img, name, state);
                l.prepend(li);
                return state;
              });
              for (let i = 0; i < files.length; i++) {
                const state = rows[i];
                state.textContent = 'Mengirim...';
                try {
                  const r = await fetch(url, {method: 'POST', body: files[i]});
                  const text = await r.text();
                  state.textContent = r.ok ? 'Terkirim' : text;
                  state.className = r.ok ? 'ok' : 'err';
                } catch (e) {
                  state.textContent = 'Gagal. Layar Tambah dari HP di TV masih terbuka?';
                  state.className = 'err';
                }
              }
            };
            </script>
            </body>
            </html>
            """;
}
