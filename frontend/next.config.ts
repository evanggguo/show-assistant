import type { NextConfig } from 'next'

const nextConfig: NextConfig = {
  // Use standalone output for Docker deployment to reduce image size
  output: 'standalone',

  // Allow external image domains (owner avatars may be hosted on an external CDN)
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
