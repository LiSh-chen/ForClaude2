import { Badge } from "@/components/ui/badge";
import { riskReminders } from "@/lib/ai/risk";
import { stars } from "@/lib/format";
import { STATUS_LABEL, type ReviewStatus } from "@/lib/queries";

export function Stars({ n }: { n: number }) {
  return (
    <span className="text-lg leading-none tracking-wide text-amber-500" role="img" aria-label={`${n} 星`}>
      {stars(n)}
    </span>
  );
}

const STATUS_VARIANT: Record<ReviewStatus, "default" | "success" | "warning" | "danger"> = {
  pending: "warning",
  replied: "success",
  skipped: "default",
  publish_failed: "danger",
};

export function StatusBadge({ status }: { status: ReviewStatus }) {
  return <Badge variant={STATUS_VARIANT[status]}>{STATUS_LABEL[status]}</Badge>;
}

export function AiStatusBadge({ aiStatus, hasDraft }: { aiStatus: string; hasDraft: boolean }) {
  if (hasDraft) return <Badge variant="info">草稿已備妥</Badge>;
  if (aiStatus === "failed") return <Badge variant="danger">AI 沒寫出來，請手動回覆</Badge>;
  if (aiStatus === "quota_exceeded") return <Badge variant="warning">本月額度已用完</Badge>;
  return null;
}

export function RiskBanner({ flags }: { flags: string[] }) {
  const reminders = riskReminders(flags);
  if (!reminders.length) return null;
  return (
    <div role="alert" className="rounded-md border border-red-200 bg-red-50 p-3 text-sm text-red-900">
      <p className="font-semibold">⚠ 請留意：{flags.join("、")}</p>
      <ul className="mt-1 list-disc pl-5">{reminders.map((r) => <li key={r}>{r}</li>)}</ul>
      <p className="mt-1">這則評論不會自動發布，發布前需要您確認。</p>
    </div>
  );
}
