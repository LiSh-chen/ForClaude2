import { asc, eq } from "drizzle-orm";
import { SubmitButton } from "@/components/submit-button";
import { Badge } from "@/components/ui/badge";
import { Card } from "@/components/ui/card";
import { Select } from "@/components/ui/field";
import { getPlan } from "@/config/plans";
import { requireUser } from "@/lib/auth";
import { locations } from "@/lib/db/schema";
import { getQuota } from "@/lib/usage";
import { disableLocationAction, enableLocationAction, refreshLocationsAction, setIndustryAction } from "../actions";

const INDUSTRY_OPTIONS = [
  ["food", "餐飲"],
  ["beauty", "美業"],
  ["medical", "診所／醫療"],
  ["education", "補習班／教育"],
  ["other", "其他"],
] as const;

export default async function LocationsPage() {
  const { db, org } = await requireUser();
  const [locs, quota] = await Promise.all([
    db.select().from(locations).where(eq(locations.organizationId, org.id)).orderBy(asc(locations.name)),
    getQuota(db, org.id),
  ]);
  const plan = getPlan(quota.plan);
  const enabled = locs.filter((l) => l.enabled).length;

  return (
    <>
      <div>
        <h1 className="text-xl font-bold">您的店點</h1>
        <p className="text-sm text-muted-foreground">目前方案（{plan.name}）最多可啟用 {plan.maxLocations} 間，已啟用 {enabled} 間。</p>
      </div>

      <form action={refreshLocationsAction}>
        <SubmitButton variant="outline" pendingText="讀取中…">從 Google 重新讀取店點</SubmitButton>
      </form>

      {locs.length === 0 && <Card className="text-center text-muted-foreground">還沒有店點。請按上方「從 Google 重新讀取店點」。</Card>}

      <ul className="space-y-3">
        {locs.map((l) => (
          <li key={l.id}>
            <Card className="space-y-3">
              <div className="flex items-center justify-between gap-2">
                <p className="font-semibold">{l.name}</p>
                {l.enabled ? <Badge variant="success">已啟用</Badge> : <Badge>未啟用</Badge>}
              </div>
              <form action={setIndustryAction} className="flex gap-2">
                <input type="hidden" name="locationId" value={l.id} />
                <Select name="industry" defaultValue={l.industry} aria-label="產業別">
                  {INDUSTRY_OPTIONS.map(([v, label]) => <option key={v} value={v}>{label}</option>)}
                </Select>
                <SubmitButton variant="secondary" pendingText="…">儲存</SubmitButton>
              </form>
              <form action={l.enabled ? disableLocationAction : enableLocationAction}>
                <input type="hidden" name="locationId" value={l.id} />
                <SubmitButton variant={l.enabled ? "outline" : "default"} className="w-full" pendingText={l.enabled ? "停用中…" : "啟用並匯入評論中…"}>
                  {l.enabled ? "停用這間店" : "啟用這間店"}
                </SubmitButton>
              </form>
            </Card>
          </li>
        ))}
      </ul>
      <p className="text-xs text-muted-foreground">首次啟用會匯入最近 50 則評論，這些「歷史評論」不會通知您，也不會自動產生草稿；您可以到評論頁挑想回覆的處理。</p>
    </>
  );
}
