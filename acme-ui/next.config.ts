import type { NextConfig } from 'next'

const nextConfig: NextConfig = {
  // Enable standalone output for Docker/Kubernetes deployment
  // This creates a minimal build with only necessary files
  output: 'standalone',
}

export default nextConfig
