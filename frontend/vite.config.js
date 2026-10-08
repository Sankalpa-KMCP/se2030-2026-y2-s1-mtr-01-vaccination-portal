import process from 'node:process';
import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';

function createChunkName(id) {
  if (id.includes('/node_modules/')) {
    if (id.includes('/react/') || id.includes('/react-dom/') || id.includes('/react-router')) {
      return 'react-vendor';
    }
    if (id.includes('/bootstrap/')) {
      return 'bootstrap-vendor';
    }
    return 'vendor';
  }

  const featureMatch = id.match(/\/src\/features\/([^/]+)\//);
  if (featureMatch) {
    return `feature-${featureMatch[1]}`;
  }

  if (id.includes('/src/auth/')) {
    return 'auth';
  }

  return undefined;
}

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '');
  const backendPort = env.SERVER_PORT || env.BACKEND_PORT || '8080';
  const target = env.BACKEND_URL || `http://localhost:${backendPort}`;

  return {
    plugins: [react()],
    build: {
      rollupOptions: {
        output: {
          manualChunks: createChunkName,
        },
      },
    },
    server: {
      proxy: {
        '/api': {
          target,
          changeOrigin: true,
        },
      },
    },
  };
});
