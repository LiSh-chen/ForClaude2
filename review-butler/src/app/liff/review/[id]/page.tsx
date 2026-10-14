import { isMockLine } from "@/lib/adapters";
import { LiffReview } from "./liff-review";

export const metadata = { title: "編輯回覆｜好評管家" };

export default async function LiffReviewPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  return <LiffReview reviewId={id} liffId={process.env.LIFF_ID ?? ""} mock={isMockLine()} />;
}
