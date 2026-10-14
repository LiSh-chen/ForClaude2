import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  reactStrictMode: true,
  // 開發模式的 N 指示器會蓋住手機版底部導覽
  devIndicators: false,
  // PGlite 含 WASM，不要被打包
  serverExternalPackages: ["@electric-sql/pglite"],
};

export default nextConfig;
