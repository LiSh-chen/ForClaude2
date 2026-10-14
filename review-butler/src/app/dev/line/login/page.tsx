import { notFound } from "next/navigation";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Input } from "@/components/ui/field";
import { mockLine } from "@/lib/line/simulator";

export const dynamic = "force-dynamic";

/** 模擬 LINE Login 授權頁：選一個 LINE 帳號來授權。 */
export default async function MockLineLogin({ searchParams }: { searchParams: Promise<{ state?: string; redirect_uri?: string }> }) {
  const line = mockLine();
  if (!line || process.env.NODE_ENV === "production") notFound();
  const { state, redirect_uri } = await searchParams;
  if (!state || !redirect_uri) notFound();
  return (
    <main className="mx-auto flex min-h-dvh max-w-md flex-col justify-center gap-4 p-4">
      <Card className="space-y-3">
        <p className="text-lg font-semibold">LINE Login（模擬）</p>
        <p className="text-sm text-muted-foreground">「好評管家」想取得您的 LINE 名稱與帳號識別碼。真實環境這裡是 LINE 的授權畫面。</p>
        {[...line.users.entries()].map(([id, name]) => (
          <form key={id} method="GET" action="/dev/line/login/authorize">
            <input type="hidden" name="userId" value={id} /><input type="hidden" name="state" value={state} /><input type="hidden" name="redirect_uri" value={redirect_uri} />
            <Button type="submit" variant="outline" className="w-full">以「{name}」授權</Button>
          </form>
        ))}
        <form method="GET" action="/dev/line/login/authorize" className="space-y-2 border-t pt-3">
          <input type="hidden" name="state" value={state} /><input type="hidden" name="redirect_uri" value={redirect_uri} />
          <Input name="name" placeholder="或建立一個新的 LINE 帳號（暱稱）" />
          <Button type="submit" className="w-full">建立並授權</Button>
        </form>
      </Card>
    </main>
  );
}
