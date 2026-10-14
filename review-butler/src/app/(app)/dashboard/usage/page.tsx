import { eq } from "drizzle-orm";
import { Card } from "@/components/ui/card";
import { getPlan } from "@/config/plans";
import { requireUser } from "@/lib/auth";
import { subscriptions } from "@/lib/db/schema";
import { formatTaipei } from "@/lib/format";
import { getQuota } from "@/lib/usage";

export default async function UsagePage() {
  const { db, org } = await requireUser();
  const [quota, [sub]] = await Promise.all([getQuota(db, org.id), db.select().from(subscriptions).where(eq(subscriptions.organizationId, org.id))]);
  const plan = getPlan(quota.plan);
  const pct = quota.limit ? Math.min(100, Math.round((quota.used / quota.limit) * 100)) : 100;
  const daysLeft = sub?.trialEndsAt ? Math.max(0, Math.ceil((sub.trialEndsAt.getTime() - Date.now()) / 86_400_000)) : null;
  return (
    <>
      <h1 className="text-xl font-bold">本月用量</h1>
      <Card className="space-y-3">
        <p className="text-sm text-muted-foreground">AI 回覆草稿（含「換一個版本」）</p>
        <p className="text-3xl font-bold">{quota.used} <span className="text-lg font-normal text-muted-foreground">/ {quota.limit} 則</span></p>
        <div className="h-3 overflow-hidden rounded-full bg-muted" role="progressbar" aria-valuenow={quota.used} aria-valuemin={0} aria-valuemax={quota.limit}>
          <div className={pct >= 90 ? "h-full bg-red-500" : "h-full bg-primary"} style={{ width: `${pct}%` }} />
        </div>
        <p className="text-sm">還剩 <b>{quota.remaining}</b> 則。額度每月 1 日（台灣時間）重置。</p>
        {quota.remaining === 0 && <p className="rounded-md bg-amber-50 p-3 text-sm text-amber-900">額度已用完：新評論仍會照常通知您，但不會再自動產生草稿，您可以自己寫回覆，或升級方案。</p>}
      </Card>
      <Card className="space-y-1 text-sm">
        <p>目前方案：<b>{plan.name}</b>{plan.priceTwd ? `（NT$${plan.priceTwd} / 月）` : ""}</p>
        {sub?.status === "trialing" && daysLeft !== null && <p>試用還有 <b>{daysLeft}</b> 天（到 {formatTaipei(sub.trialEndsAt!, false)}），試用期間可使用進階方案的全部功能。</p>}
        <p className="text-muted-foreground">升級、付款與發票功能將在後續階段開放。</p>
      </Card>
    </>
  );
}
