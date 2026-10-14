import { Suspense } from "react";
import { Flash } from "@/components/flash";
import { NavBar } from "@/components/nav-bar";
import { Button } from "@/components/ui/button";
import { requireUser } from "@/lib/auth";
import { getCredentialStatus } from "@/lib/google-auth";
import { logoutAction } from "@/app/login/actions";

export default async function DashboardLayout({ children }: { children: React.ReactNode }) {
  const { db, org, user } = await requireUser();
  const cred = await getCredentialStatus(db, org.id);
  return (
    <div className="min-h-dvh pb-24 md:pb-8">
      <header className="border-b">
        <div className="mx-auto flex max-w-2xl items-center justify-between gap-3 px-4 py-3">
          <div className="min-w-0">
            <p className="truncate font-semibold">好評管家</p>
            <p className="truncate text-xs text-muted-foreground">{user.email}</p>
          </div>
          <form action={logoutAction}><Button variant="ghost" size="sm" type="submit">登出</Button></form>
        </div>
      </header>
      <NavBar />
      <main className="mx-auto max-w-2xl space-y-4 px-4 py-4">
        {cred !== "active" && (
          <p role="alert" className="rounded-md bg-amber-50 p-3 text-sm text-amber-900">
            Google 授權已失效，暫時無法抓取新評論或發布回覆。請重新登入 Google 授權。
          </p>
        )}
        <Suspense><Flash /></Suspense>
        {children}
      </main>
    </div>
  );
}
