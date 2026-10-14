import { redirect } from "next/navigation";
import { Card } from "@/components/ui/card";
import { Input, Label } from "@/components/ui/field";
import { SubmitButton } from "@/components/submit-button";
import { isMockGoogle } from "@/lib/adapters";
import { getCurrentUser } from "@/lib/auth";
import { loginAction } from "./actions";

const ERRORS: Record<string, string> = {
  email: "請輸入正確的 Email。",
  real: "正式的 Google 登入尚未開通，請先在 .env.local 設定 USE_MOCK_ADAPTERS=true。",
};

export default async function LoginPage({ searchParams }: { searchParams: Promise<{ error?: string }> }) {
  if (await getCurrentUser()) redirect("/dashboard");
  const { error } = await searchParams;
  const mock = isMockGoogle();
  return (
    <main className="mx-auto flex min-h-dvh max-w-md flex-col justify-center gap-6 px-4 py-10">
      <div className="text-center">
        <h1 className="text-2xl font-bold">好評管家</h1>
        <p className="mt-2 text-muted-foreground">用 AI 幫您回覆 Google 評論</p>
      </div>
      <Card className="space-y-4 p-5">
        {error && <p className="rounded-md bg-red-50 p-3 text-sm text-red-800">{ERRORS[error] ?? "登入失敗"}</p>}
        {mock ? (
          <form action={loginAction} className="space-y-4">
            <div>
              <Label htmlFor="email">Email（開發模式：模擬 Google 登入）</Label>
              <Input id="email" name="email" type="email" required defaultValue="demo@example.com" autoComplete="email" />
              <p className="mt-1.5 text-xs text-muted-foreground">換一個 Email 就是另一家店，資料互不相通。</p>
            </div>
            <SubmitButton className="w-full" size="lg" pendingText="登入中…">以 Google 帳號登入</SubmitButton>
          </form>
        ) : (
          <p className="text-sm text-muted-foreground">正式的 Google 登入將在階段 3 開通。</p>
        )}
      </Card>
    </main>
  );
}
