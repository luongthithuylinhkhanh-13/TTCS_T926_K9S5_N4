const CACHE_NAME = 'construction-platform-v1';
const APP_SHELL = [
  '/',
  '/manifest.webmanifest',
  '/icons/app-icon.svg',
  '/icons/app-icon-192.png',
  '/icons/app-icon-512.png',
];
const STATIC_DESTINATIONS = new Set(['script', 'style', 'image', 'font', 'worker']);

self.addEventListener('install', (event) => {
  event.waitUntil((async () => {
    const cache = await caches.open(CACHE_NAME);
    const pageResponse = await fetch('/');
    if (!pageResponse.ok) {
      throw new Error(`Không thể tải giao diện PWA để lưu ngoại tuyến: ${pageResponse.status}`);
    }

    await cache.put('/', pageResponse.clone());
    const html = await pageResponse.text();
    const assets = new Set(
      Array.from(html.matchAll(/(?:src|href)=["'](\/assets\/[^"']+)["']/g), (match) => match[1])
    );

    for (const assetPath of assets) {
      const response = await fetch(assetPath);
      if (!response.ok) {
        throw new Error(`Không thể lưu tài nguyên PWA ${assetPath}: ${response.status}`);
      }

      await cache.put(assetPath, response.clone());

      if (assetPath.endsWith('.css')) {
        const css = await response.text();
        const cssAssets = Array.from(
          css.matchAll(/url\(["']?(\/assets\/[^)"']+)["']?\)/g),
          (match) => match[1]
        );

        for (const cssAssetPath of cssAssets) {
          const cssAssetResponse = await fetch(cssAssetPath);
          if (!cssAssetResponse.ok) {
            throw new Error(`Không thể lưu tài nguyên PWA ${cssAssetPath}: ${cssAssetResponse.status}`);
          }
          await cache.put(cssAssetPath, cssAssetResponse);
        }
      }
    }

    await cache.addAll(APP_SHELL.slice(1));
    await self.skipWaiting();
  })());
});

self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys()
      .then((cacheNames) => Promise.all(
        cacheNames
          .filter((cacheName) => cacheName !== CACHE_NAME)
          .map((cacheName) => caches.delete(cacheName))
      ))
      .then(() => self.clients.claim())
  );
});

self.addEventListener('fetch', (event) => {
  const { request } = event;

  if (request.method !== 'GET' || new URL(request.url).origin !== self.location.origin) {
    return;
  }

  if (request.mode === 'navigate') {
    const networkUpdate = fetch(request).then((response) => {
      if (response.ok) {
        const responseToCache = response.clone();
        event.waitUntil(
          caches.open(CACHE_NAME)
            .then((cache) => cache.put('/', responseToCache))
        );
      }
      return response;
    });

    event.respondWith(
      caches.match('/')
        .then((cachedPage) => {
          if (cachedPage) {
            event.waitUntil(
              networkUpdate.catch((error) => {
                console.error('Không thể cập nhật giao diện đã lưu:', error);
              })
            );
            return cachedPage;
          }
          return networkUpdate;
        })
    );
    return;
  }

  if (!STATIC_DESTINATIONS.has(request.destination)) {
    return;
  }

  event.respondWith(
    caches.match(request).then((cachedResponse) => {
      if (cachedResponse) {
        return cachedResponse;
      }

      return fetch(request).then((response) => {
        if (response.ok && response.type === 'basic') {
          const responseToCache = response.clone();
          event.waitUntil(
            caches.open(CACHE_NAME)
              .then((cache) => cache.put(request, responseToCache))
          );
        }
        return response;
      });
    })
  );
});
