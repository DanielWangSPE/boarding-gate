import { defineConfig } from '@vben/vite-config';

import ElementPlus from 'unplugin-element-plus/vite';

export default defineConfig(async () => {
  return {
    application: {},
    vite: {
      plugins: [
        ElementPlus({
          format: 'esm',
        }),
      ],
      server: {
        proxy: {
          '/api': {
            changeOrigin: true,
            //  后端 Spring Boot 的 context-path 也是 /api，所以不做 rewrite
            target: 'http://localhost:8080',
            ws: true,
          },
        },
      },
    },
  };
});
