import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// Local-dev-only: injects HTTP Basic Auth into proxied requests to a
// local AEM author instance, which requires authentication on /bin/*
// and .model.json by default - confirmed live (unauthenticated proxied
// requests get redirected to /libs/granite/core/content/login.html,
// which then fails as a CORS-blocked cross-origin redirect target).
// Never used for the production build (vite build doesn't run the dev
// server/proxy at all). Override AEM_DEV_USER/AEM_DEV_PASSWORD env vars
// for a real instance instead of the local admin:admin default.
function withDevAuth(target) {
  const user = process.env.AEM_DEV_USER || 'admin';
  const password = process.env.AEM_DEV_PASSWORD || 'admin';
  const auth = Buffer.from(`${user}:${password}`).toString('base64');
  return {
    target,
    changeOrigin: true,
    configure: (proxy) => {
      proxy.on('proxyReq', (proxyReq) => {
        proxyReq.setHeader('Authorization', `Basic ${auth}`);
      });
    },
  };
}

export default defineConfig({
  plugins: [react()],
  build: {
    outDir: 'build',
    sourcemap: false,
  },
  server: {
    port: 3000,
    open: true,
    // Without this, every /bin/bmad/*, /content/forms/af/*.model.json,
    // and /adobe/forms/af/submit/* fetch from App.jsx hits Vite's own
    // dev server instead of AEM - confirmed live: it returns Vite's SPA
    // fallback HTML, which fails to parse as JSON.
    proxy: {
      '/bin': withDevAuth('http://localhost:4502'),
      '/content': withDevAuth('http://localhost:4502'),
      '/adobe': withDevAuth('http://localhost:4502'),
      '/libs': withDevAuth('http://localhost:4502'),
    },
  },
  test: {
    globals: true,
    environment: 'jsdom',
    setupFiles: './src/setupTests.js',
  },
});
