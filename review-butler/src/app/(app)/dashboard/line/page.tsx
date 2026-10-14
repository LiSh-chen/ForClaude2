import { eq } from "drizzle-orm";
import { SubmitButton } from "@/components/submit-button";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { requireUser } from "@/lib/auth";
import { locations } from "@/lib/db/schema";
import { formatTaipei } from "@/lib/format";
import { activeBindCode, listBindings } from "@/lib/line/bind";
import { createBindCodeAction, removeBindingAction, saveBindingLocationsAction, testBindingAction } from "../line-actions";

export default async function LinePage() {
  const { db, org } = await requireUser();
  const [bindings, locs, code] = await Promise.all([
    listBindings(db, org.id),
    db.select().from(locations).where(eq(locations.organizationId, org.id)),
    activeBindCode(db, org.id),
  ]);
  const enabled = locs.filter((l) => l.enabled);
  const addFriend = process.env.LINE_ADD_FRIEND_URL;

  return (
    <>
      <div>
        <h1 className="text-xl font-bold">LINE 通知</h1>
        <p className="text-sm text-muted-foreground">新評論和寫好的回覆草稿會傳到 LINE，您在 LINE 上按一下就能發布。老闆、店長都可以各自綁定。</p>
      </div>

      <Card className="space-y-4">
        <p className="font-semibold">綁定 LINE</p>
        <ol className="list-decimal space-y-1 pl-5 text-sm">
          <li>先加入好評管家的 LINE 官方帳號為好友{addFriend ? "" : "（上線後這裡會有加好友按鈕）"}。</li>
          <li>用下面任一種方式完成綁定。</li>
        </ol>
        {addFriend && <Button asChild variant="outline"><a href={addFriend} target="_blank" rel="noreferrer">加官方帳號好友</a></Button>}

        <div className="space-y-2 rounded-md border p-3">
          <p className="text-sm font-medium">方式一：用 LINE 帳號綁定（您自己）</p>
          <Button asChild size="lg" className="w-full"><a href="/api/auth/line/start">用 LINE 登入並綁定</a></Button>
        </div>

        <div className="space-y-2 rounded-md border p-3">
          <p className="text-sm font-medium">方式二：用綁定碼（適合請店長綁定，不用登入）</p>
          {code ? (
            <div className="space-y-1 text-center">
              <p className="text-4xl font-bold tracking-[0.3em]" aria-label={`綁定碼 ${code.code.split("").join(" ")}`}>{code.code}</p>
              <p className="text-xs text-muted-foreground">請對方把這 6 個數字傳到官方帳號的對話框。{formatTaipei(code.expiresAt)} 前有效，只能用一次。</p>
            </div>
          ) : (
            <p className="text-sm text-muted-foreground">按下按鈕產生 6 位數字，再請對方傳到官方帳號的對話框。</p>
          )}
          <form action={createBindCodeAction}><SubmitButton variant="outline" className="w-full" pendingText="產生中…">{code ? "重新產生綁定碼" : "產生綁定碼"}</SubmitButton></form>
        </div>
      </Card>

      <h2 className="font-semibold">已綁定的人（{bindings.length}）</h2>
      {bindings.length === 0 && <Card className="text-center text-sm text-muted-foreground">還沒有人綁定，新評論不會通知任何人。</Card>}
      <ul className="space-y-3">
        {bindings.map((b) => (
          <li key={b.id}>
            <Card className="space-y-3">
              <div className="flex items-center justify-between gap-2">
                <p className="font-semibold">{b.displayName || "（未命名）"}</p>
                {b.following ? <Badge variant="success">接收中</Badge> : <Badge variant="warning">已封鎖或取消好友</Badge>}
              </div>
              <form action={saveBindingLocationsAction} className="space-y-2">
                <input type="hidden" name="bindingId" value={b.id} />
                <p className="text-sm text-muted-foreground">要收到哪些店的通知：</p>
                {enabled.length === 0 && <p className="text-sm">還沒有啟用的店點。</p>}
                {enabled.map((l) => (
                  <label key={l.id} className="flex items-center gap-2 py-1">
                    <input type="checkbox" name="locationId" value={l.id} defaultChecked={b.locationIds.includes(l.id)} className="size-5" />
                    <span>{l.name}</span>
                  </label>
                ))}
                <SubmitButton variant="secondary" size="sm" pendingText="儲存中…">儲存</SubmitButton>
              </form>
              <div className="flex gap-2">
                <form action={testBindingAction} className="flex-1"><input type="hidden" name="bindingId" value={b.id} /><SubmitButton variant="outline" className="w-full" pendingText="傳送中…">傳測試訊息</SubmitButton></form>
                <form action={removeBindingAction} className="flex-1"><input type="hidden" name="bindingId" value={b.id} /><SubmitButton variant="ghost" className="w-full text-destructive" pendingText="處理中…">解除綁定</SubmitButton></form>
              </div>
            </Card>
          </li>
        ))}
      </ul>
    </>
  );
}
