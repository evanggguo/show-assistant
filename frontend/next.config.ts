import type { NextConfig } from 'next'

const nextConfig: NextConfig = {
  // Docker 部署时使用 standalone 输出，减少镜像体积
  output: 'standalone',

  // 允许外部图片域（Owner 头像可能来自外部 CDN）
  images: {
    remotePatterns: [
      {
        protocol: 'https',
        hostname: '**',
      },
      {
        protocol: 'http',
        hostname: 'localhost',
      },
    ],
  },

}

export default nextConfig
