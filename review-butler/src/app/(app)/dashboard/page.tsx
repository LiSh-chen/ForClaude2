import Link from "next/link";
import { and, eq } from "drizzle-orm";
import { RefreshCw } from "lucide-react";
import { AiStatusBadge, Stars, StatusBadge } from "@/components/review-parts";
import { SubmitButton } from "@/components/submit-button";
import { Badge } from "@/components/ui/badge";
import { Card } from "@/components/ui/card";
import { Select } from "@/components/ui/field";
import { Button } from "@/components/ui/button";
import { isMockGoogle } from "@/lib/adapters";
import { requireUser } from "@/lib/auth";
import { locations } from "@/lib/db/schema";
import { relativeTime } from "@/lib/format";
import { listReviews, reviewCounts, STATUS_LABEL, type ReviewStatus } from "@/lib/queries";
import { cn } from "@/lib/utils";
import { simulateReviewAction, syncNowAction } from "./actions";

type SP = { location?: string; stars?: string; status?: string; scope?: string; page?: string };
const STATUSES = Object.keys(STATUS_LABEL) as ReviewStatus[];

function href(sp: SP, patch: Partial<SP>) {
  const q = new URLSearchParams();
  for (const [k, v] of Object.entries({ ...sp, ...patch })) if (v) q.set(k, v);
  return `/dashboard${q.size ? `?${q}` : ""}`;
}

export default async function ReviewsPage({ searchParams }: { searchParams: Promise<SP> }) {
  const sp = await searchParams;
  const { db, org } = await requireUser();
  const locs = await db.select().from(locations).where(eq(locations.organizationId, org.id));
  const active = locs.filter((l) => l.enabled);

  if (!active.length) {
    return (
      <Card className="space-y-3 text-center">
        <p className="text-lg font-semibold">歡迎使用好評管家</p>
        <p className="text-muted-foreground">第一步：選擇要幫您管理評論的店點。</p>
        <Button asChild size="lg"><Link href="/dashboard/locations">選擇店點</Link></Button>
      </Card>
    );
  }

  const history = sp.scope === "history";
  const status = (STATUSES as string[]).includes(sp.status ?? "") ? (sp.status as ReviewStatus) : history ? undefined : "pending";
  const stars = Number(sp.stars) >= 1 && Number(sp.stars) <= 5 ? Number(sp.stars) : undefined;
  const locationId = locs.some((l) => l.id === sp.location) ? sp.location : undefined;
  const filters = { locationId, stars, status, historical: history, page: Number(sp.page) || 1 };

  const [{ items, hasMore, page }, { counts, historyPending }] = await Promise.all([
    listReviews(db, org.id, filters),
    reviewCounts(db, org.id, { locationId, stars }),
  ]);

  return (
    <>
      <div className="flex flex-wrap items-center gap-2">
        <form action={syncNowAction}>
          <SubmitButton pendingText="檢查中…（AI 寫草稿需要一點時間）"><RefreshCw className="size-4" aria-hidden />立即檢查新評論</SubmitButton>
        </form>
        <p className="text-xs text-muted-foreground">系統每 30 分鐘也會自動檢查。</p>
      </div>

      {isMockGoogle() && (
        <details className="rounded-md border border-dashed p-3 text-sm">
          <summary className="cursor-pointer font-medium">開發工具：模擬一則新評論</summary>
          <form action={simulateReviewAction} className="mt-3 flex flex-wrap gap-2">
            <Select name="locationId" className="flex-1 basis-40" defaultValue={active[0]!.id}>
              {active.map((l) => <option key={l.id} value={l.id}>{l.name}</option>)}
            </Select>
            <Select name="kind" className="flex-1 basis-40" defaultValue="5">
              <option value="5">5 星・稱讚</option>
              <option value="4">4 星・小抱怨</option>
              <option value="3">3 星・普通</option>
              <option value="2">2 星・等太久</option>
              <option value="1">1 星・湯冷、有頭髮</option>
              <option value="food">1 星・食物中毒（風險）</option>
              <option value="en">5 星・英文</option>
            </Select>
            <SubmitButton variant="outline" pendingText="新增中…">新增並產生草稿</SubmitButton>
          </form>
        </details>
      )}

      <div className="flex gap-2 overflow-x-auto pb-1" role="tablist" aria-label="狀態">
        {STATUSES.map((s) => (
          <Link
            key={s}
            href={href(sp, { status: s, scope: undefined, page: undefined })}
            className={cn("rounded-full border px-4 py-2 text-sm whitespace-nowrap", !history && status === s ? "border-primary bg-primary text-primary-foreground" : "bg-background")}
          >
            {STATUS_LABEL[s]} {counts[s]}
          </Link>
        ))}
      </div>

      <div className="grid grid-cols-2 gap-2">
        <form className="contents">
          {sp.status && <input type="hidden" name="status" value={sp.status} />}
          {sp.scope && <input type="hidden" name="scope" value={sp.scope} />}
          <Select name="location" defaultValue={locationId ?? ""} aria-label="店點">
            <option value="">全部店點</option>
            {locs.map((l) => <option key={l.id} value={l.id}>{l.name}</option>)}
          </Select>
          <Select name="stars" defaultValue={stars ? String(stars) : ""} aria-label="星等">
            <option value="">全部星等</option>
            {[5, 4, 3, 2, 1].map((n) => <option key={n} value={n}>{n} 星</option>)}
          </Select>
          <Button type="submit" variant="secondary" size="sm" className="col-span-2">套用篩選</Button>
        </form>
      </div>

      {!history && historyPending > 0 && (
        <Link href={href(sp, { scope: "history", status: undefined, page: undefined })} className="block rounded-md bg-muted p-3 text-sm">
          另有 <b>{historyPending}</b> 則尚未回覆的歷史評論，您可以挑想處理的來回覆 →
        </Link>
      )}
      {history && (
        <div className="flex items-center justify-between rounded-md bg-muted p-3 text-sm">
          <span>歷史評論（開通前就已存在，不會通知）</span>
          <Link href={href(sp, { scope: undefined, status: "pending", page: undefined })} className="underline">回到新評論</Link>
        </div>
      )}

      {items.length === 0 ? (
        <Card className="text-center text-muted-foreground">這裡目前沒有評論。</Card>
      ) : (
        <ul className="space-y-3">
          {items.map(({ review: r, locationName, riskFlags, hasDraft }) => (
            <li key={r.id}>
              <Link href={`/dashboard/reviews/${r.id}`} className="block">
                <Card className="space-y-2 transition-colors hover:bg-muted/50">
                  <div className="flex items-center justify-between gap-2">
                    <Stars n={r.starRating} />
                    <span className="text-xs text-muted-foreground">{relativeTime(r.createTime)}</span>
                  </div>
                  <p className="line-clamp-3 text-base">{r.comment ?? <span className="text-muted-foreground">（顧客只留下星等，沒有文字）</span>}</p>
                  <div className="flex flex-wrap items-center gap-1.5">
                    <span className="text-xs text-muted-foreground">{locationName}</span>
                    <StatusBadge status={r.status} />
                    {r.isHistorical && <Badge>歷史</Badge>}
                    {r.isEdited && <Badge variant="info">顧客已編輯</Badge>}
                    {riskFlags && riskFlags.length > 0 && <Badge variant="danger">⚠ {riskFlags[0]}</Badge>}
                    {r.status === "pending" && <AiStatusBadge aiStatus={r.aiStatus} hasDraft={hasDraft} />}
                  </div>
                </Card>
              </Link>
            </li>
          ))}
        </ul>
      )}

      <div className="flex justify-between">
        {page > 1 ? <Button asChild variant="outline"><Link href={href(sp, { page: String(page - 1) })}>上一頁</Link></Button> : <span />}
        {hasMore && <Button asChild variant="outline"><Link href={href(sp, { page: String(page + 1) })}>下一頁</Link></Button>}
      </div>
    </>
  );
}
