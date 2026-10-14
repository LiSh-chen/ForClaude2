import Link from "next/link";
import { notFound } from "next/navigation";
import { FlexPreview } from "@/components/line-flex-preview";
import { SubmitButton } from "@/components/submit-button";
import { Input } from "@/components/ui/field";
import { mockLine, userLog } from "@/lib/line/simulator";
import { simFollowAction, simNewUserAction, simTextAction, simTickAction } from "./actions";

export const metadata = { title: "LINE 模擬器（開發用）" };
export const dynamic = "force-dynamic";

export default async function SimulatorPage({ searchParams }: { searchParams: Promise<{ u?: string; tick?: string }> }) {
  const line = mockLine();
  if (!line || process.env.NODE_ENV === "production") notFound();
  const { u, tick } = await searchParams;
  const users = [...line.users.entries()];
  const userId = u && line.users.has(u) ? u : users[0]?.[0];

  const items = userId
    ? [
        ...line.sent.filter((m) => m.userId === userId).map((m) => ({ from: "bot" as const, at: m.at, kind: m.kind, messages: m.messages })),
        ...userLog().filter((m) => m.userId === userId).map((m) => ({ from: "user" as const, at: m.at, text: m.text })),
      ].sort((a, b) => a.at.getTime() - b.at.getTime())
    : [];

  return (
    <main className="mx-auto max-w-md space-y-3 bg-[#8CABD9]/20 p-3">
      <header className="space-y-2 rounded-lg bg-white p-3 text-sm">
        <p className="font-semibold">LINE 模擬器（開發用）</p>
        <p className="text-xs text-muted-foreground">沒有真實 LINE channel 時，用這裡扮演「店主的 LINE」：訊息、按鈕、LIFF 編輯頁都走和正式環境一樣的簽章與處理流程。</p>
        <div className="flex flex-wrap gap-1.5">
          {users.map(([id, name]) => (
            <Link key={id} href={`/dev/line?u=${id}`} className={`rounded-full border px-3 py-1 text-xs ${id === userId ? "bg-black text-white" : ""}`}>{name}</Link>
          ))}
        </div>
        <form action={simNewUserAction} className="flex gap-2">
          <Input name="name" placeholder="新增一位 LINE 使用者（暱稱）" className="h-9 text-sm" />
          <SubmitButton size="sm" variant="secondary" pendingText="…">新增並加好友</SubmitButton>
        </form>
      </header>

      {!userId ? <p className="p-6 text-center text-sm text-muted-foreground">先新增一位 LINE 使用者。</p> : (
        <>
          <section aria-label="對話" className="space-y-3 rounded-lg bg-[#8CABD9]/40 p-3">
            {items.length === 0 && <p className="text-center text-xs text-muted-foreground">還沒有訊息</p>}
            {items.map((m, i) =>
              m.from === "user" ? (
                <div key={i} className="flex justify-end"><p className="max-w-[80%] rounded-2xl bg-[#06C755] px-3 py-2 text-sm text-white">{m.text}</p></div>
              ) : (
                <div key={i} className="space-y-1.5">
                  {m.messages.map((msg, j) =>
                    msg.type === "text" ? (
                      <p key={j} className="max-w-[85%] whitespace-pre-wrap rounded-2xl bg-white px-3 py-2 text-sm">{String(msg.text)}</p>
                    ) : msg.type === "flex" ? (
                      <div key={j} title={String(msg.altText)}><FlexPreview node={msg.contents} userId={userId} /></div>
                    ) : null,
                  )}
                  <p className="text-[10px] text-muted-foreground">{m.kind === "reply" ? "↩ 回覆（免費）" : "📣 推播"}</p>
                </div>
              ),
            )}
          </section>
          <form action={simTextAction} className="flex gap-2 rounded-lg bg-white p-2">
            <input type="hidden" name="userId" value={userId} />
            <Input name="text" placeholder="輸入訊息，例如：綁定碼、待處理、統計、短一點" className="h-10" autoComplete="off" />
            <SubmitButton pendingText="…">送出</SubmitButton>
          </form>
          <div className="flex flex-wrap gap-2 text-xs">
            <Link href={`/dev/line?u=${userId}`} className="rounded-md border bg-white px-3 py-2">🔄 重新整理</Link>
            <form action={simTickAction}><input type="hidden" name="userId" value={userId} /><SubmitButton variant="outline" size="sm" pendingText="執行中…">⏱ 執行排程一次</SubmitButton></form>
            <form action={simFollowAction}><input type="hidden" name="userId" value={userId} /><input type="hidden" name="unfollow" value="1" /><SubmitButton variant="outline" size="sm">🚫 封鎖官方帳號</SubmitButton></form>
            <form action={simFollowAction}><input type="hidden" name="userId" value={userId} /><SubmitButton variant="outline" size="sm">➕ 重新加好友</SubmitButton></form>
          </div>
          {tick && <p className="rounded-md bg-white p-2 text-xs">排程結果：{tick}</p>}
        </>
      )}
    </main>
  );
}
