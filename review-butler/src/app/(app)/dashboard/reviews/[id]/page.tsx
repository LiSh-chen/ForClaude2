import Link from "next/link";
import { notFound } from "next/navigation";
import { ChevronLeft } from "lucide-react";
import { AiStatusBadge, RiskBanner, Stars, StatusBadge } from "@/components/review-parts";
import { SubmitButton } from "@/components/submit-button";
import { Badge } from "@/components/ui/badge";
import { Card } from "@/components/ui/card";
import { Textarea } from "@/components/ui/field";
import { requireUser } from "@/lib/auth";
import { formatTaipei } from "@/lib/format";
import { getReviewDetail } from "@/lib/queries";
import { publishAction, regenerateAction, skipAction } from "../../actions";

const SENTIMENT = { positive: "正面", neutral: "中性", negative: "負面" } as const;
const PUBLISHER = { user: "您", line_user: "您（LINE）", auto: "自動回覆", external: "在 Google 上回覆" } as const;
const isUuid = (s: string) => /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(s);

export default async function ReviewDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const { db, org } = await requireUser();
  const d = isUuid(id) ? await getReviewDetail(db, org.id, id) : null;
  if (!d) notFound();
  const { review: r, location, analysis, drafts, reply } = d;
  const draft = drafts[0];
  const risky = (analysis?.riskFlags.length ?? 0) > 0;
  const done = r.status === "replied";

  return (
    <>
      <Link href="/dashboard" className="inline-flex items-center gap-1 text-sm text-muted-foreground"><ChevronLeft className="size-4" aria-hidden />回評論列表</Link>

      <Card className="space-y-3">
        <div className="flex items-center justify-between gap-2">
          <Stars n={r.starRating} />
          <StatusBadge status={r.status} />
        </div>
        <p className="whitespace-pre-wrap text-base leading-relaxed">{r.comment ?? <span className="text-muted-foreground">（顧客只留下星等，沒有文字）</span>}</p>
        <p className="text-xs text-muted-foreground">
          {location.name}・{r.reviewerDisplayName ?? "匿名"}・{formatTaipei(r.createTime)}
        </p>
        <div className="flex flex-wrap gap-1.5">
          {r.isHistorical && <Badge>歷史評論</Badge>}
          {r.isEdited && <Badge variant="info">顧客已編輯過這則評論</Badge>}
          {analysis && <Badge>{SENTIMENT[analysis.sentiment]}</Badge>}
          {analysis?.topics.map((t) => <Badge key={t} variant="info">{t}</Badge>)}
        </div>
      </Card>

      {analysis && <RiskBanner flags={analysis.riskFlags} />}

      {done ? (
        <Card className="space-y-2">
          <p className="font-semibold">已回覆</p>
          <p className="whitespace-pre-wrap leading-relaxed">{reply?.content}</p>
          <p className="text-xs text-muted-foreground">
            {reply?.publishedAt ? formatTaipei(reply.publishedAt) : ""}・由{reply ? PUBLISHER[reply.publishedBy] : "—"}發布
          </p>
        </Card>
      ) : (
        <>
          {r.status === "publish_failed" && reply?.errorMessage && (
            <p role="alert" className="rounded-md bg-red-50 p-3 text-sm text-red-800">上次發布失敗：{reply.errorMessage}。內容還在，可以再按一次發布。</p>
          )}
          <Card className="space-y-3">
            <div className="flex items-center justify-between gap-2">
              <p className="font-semibold">回覆草稿{draft ? `（第 ${draft.version} 版）` : ""}</p>
              <AiStatusBadge aiStatus={r.aiStatus} hasDraft={Boolean(draft)} />
            </div>
            <form action={publishAction} className="space-y-3">
              <input type="hidden" name="reviewId" value={r.id} />
              <Textarea
                name="content"
                required
                rows={8}
                defaultValue={reply?.content ?? draft?.content ?? ""}
                placeholder="還沒有草稿。您可以直接在這裡寫回覆，或按下方「讓 AI 寫一版」。"
                className="min-h-48 text-base"
                aria-label="回覆內容"
              />
              <p className="text-xs text-muted-foreground">可以直接修改文字。建議 40–120 字；請避免放網址、優惠或要求顧客改評論，Google 可能不接受。</p>
              {risky && (
                <label className="flex items-start gap-2 text-sm">
                  <input type="checkbox" name="confirmRisk" required className="mt-1 size-5" />
                  <span>我已了解這則評論有風險提醒，並確認這份回覆內容後再發布</span>
                </label>
              )}
              <SubmitButton size="lg" className="w-full" pendingText="發布中…">{risky ? "確認並發布到 Google" : "發布到 Google"}</SubmitButton>
            </form>
          </Card>

          <div className="grid grid-cols-2 gap-2">
            <form action={regenerateAction}>
              <input type="hidden" name="reviewId" value={r.id} />
              <SubmitButton variant="outline" className="w-full" pendingText="AI 寫作中…">{draft ? "🔄 換一個版本" : "✨ 讓 AI 寫一版"}</SubmitButton>
            </form>
            <form action={skipAction}>
              <input type="hidden" name="reviewId" value={r.id} />
              <input type="hidden" name="skip" value={r.status === "skipped" ? "0" : "1"} />
              <SubmitButton variant="outline" className="w-full">{r.status === "skipped" ? "改回待處理" : "⏭ 略過不回覆"}</SubmitButton>
            </form>
          </div>
          <p className="text-center text-xs text-muted-foreground">每次讓 AI 寫（含換版本）會用掉 1 則本月額度。</p>

          {drafts.length > 1 && (
            <details className="rounded-md border p-3 text-sm">
              <summary className="cursor-pointer">之前的版本（{drafts.length - 1}）</summary>
              <ul className="mt-2 space-y-3">
                {drafts.slice(1).map((v) => (
                  <li key={v.id} className="whitespace-pre-wrap border-t pt-2"><span className="text-xs text-muted-foreground">第 {v.version} 版</span>{"\n"}{v.content}</li>
                ))}
              </ul>
            </details>
          )}
        </>
      )}
    </>
  );
}
