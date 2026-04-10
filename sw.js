// Mokuro Reader — Service Worker
// Caches the app shell so it works fully offline after first load.

const CACHE = 'mokuro-v1';

// Files to cache on install (app shell only — manga data is in IndexedDB)
const SHELL = [
  './mokuro-reader.html',
  './manifest.json',
];

// Google Fonts are cached on first fetch so the app looks right offline too.
const FONT_CACHE = 'mokuro-fonts-v1';

self.addEventListener('install', (e) => {
  e.waitUntil(
    caches.open(CACHE).then((c) => c.addAll(SHELL)).then(() => self.skipWaiting()),
  );
});

self.addEventListener('activate', (e) => {
  e.waitUntil(
    caches.keys().then((keys) =>
      Promise.all(
        keys
          .filter((k) => k !== CACHE && k !== FONT_CACHE)
          .map((k) => caches.delete(k)),
      ),
    ).then(() => self.clients.claim()),
  );
});

self.addEventListener('fetch', (e) => {
  const url = new URL(e.request.url);

  // Google Fonts — cache-first so they work offline
  if (url.hostname === 'fonts.googleapis.com' || url.hostname === 'fonts.gstatic.com') {
    e.respondWith(
      caches.open(FONT_CACHE).then(async (c) => {
        const cached = await c.match(e.request);
        if (cached) return cached;
        const fresh = await fetch(e.request);
        c.put(e.request, fresh.clone());
        return fresh;
      }),
    );
    return;
  }

  // MyMemory translation API — network only (no caching, always fresh)
  if (url.hostname === 'api.mymemory.translated.net') {
    e.respondWith(fetch(e.request));
    return;
  }

  // App shell — cache-first
  e.respondWith(
    caches.match(e.request).then((cached) => cached || fetch(e.request)),
  );
});
