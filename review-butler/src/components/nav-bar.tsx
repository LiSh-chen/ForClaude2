"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { BarChart3, Bell, MessageSquare, Settings, Store } from "lucide-react";
import { cn } from "@/lib/utils";

const ITEMS = [
  { href: "/dashboard", label: "評論", icon: MessageSquare, match: (p: string) => p === "/dashboard" || p.startsWith("/dashboard/reviews") },
  { href: "/dashboard/locations", label: "店點", icon: Store, match: (p: string) => p.startsWith("/dashboard/locations") },
  { href: "/dashboard/line", label: "LINE", icon: Bell, match: (p: string) => p.startsWith("/dashboard/line") },
  { href: "/dashboard/settings", label: "設定", icon: Settings, match: (p: string) => p.startsWith("/dashboard/settings") },
  { href: "/dashboard/usage", label: "用量", icon: BarChart3, match: (p: string) => p.startsWith("/dashboard/usage") },
];

/** 手機：固定在底部的大按鈕導覽；桌面：置於頁首下方。 */
export function NavBar() {
  const pathname = usePathname();
  return (
    <nav className="fixed inset-x-0 bottom-0 z-10 border-t bg-background md:static md:border-t-0 md:border-b" aria-label="主選單">
      <ul className="mx-auto flex max-w-2xl">
        {ITEMS.map(({ href, label, icon: Icon, match }) => {
          const active = match(pathname);
          return (
            <li key={href} className="flex-1">
              <Link
                href={href}
                aria-current={active ? "page" : undefined}
                className={cn("flex flex-col items-center gap-0.5 py-2.5 text-xs md:flex-row md:justify-center md:gap-2 md:py-3 md:text-sm", active ? "font-semibold text-foreground" : "text-muted-foreground")}
              >
                <Icon className="size-6 md:size-4" aria-hidden />
                {label}
              </Link>
            </li>
          );
        })}
      </ul>
    </nav>
  );
}
