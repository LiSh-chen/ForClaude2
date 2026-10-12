import type { Metadata, Viewport } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "好評管家",
  description: "用 AI 幫你回覆 Google 評論，LINE 一鍵發布",
};

export const viewport: Viewport = { width: "device-width", initialScale: 1 };

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="zh-TW">
      <body className="min-h-dvh antialiased">{children}</body>
    </html>
  );
}
