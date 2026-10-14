import { NextResponse } from "next/server";
import { z } from "zod";
import { getLine } from "@/lib/adapters";
import { getDb } from "@/lib/db/client";
import { bindingsForLineUser } from "@/lib/line/bind";
import { authorize } from "@/lib/line/handlers";
import { publishReply } from "@/lib/publish";
import { getReviewDetail } from "@/lib/queries";

/** LIFF 頁面呼叫：以 LINE 的 ID token 驗證身分，再檢查綁定與店點權限。 */
async function identify(req: Request, reviewId: string) {
  const token = req.headers.get("authorization")?.replace(/^Bearer\s+/i, "");
  if (!token) return { error: NextResponse.json({ error: "unauthenticated" }, { status: 401 }) };
  const profile = await getLine().verifyIdToken(token);
  if (!profile) return { error: NextResponse.json({ error: "invalid_token" }, { status: 401 }) };
  const db = await getDb();
  const bindings = await bindingsForLineUser(db, profile.userId);
  const auth = bindings.length ? await authorize({ db }, bindings, reviewId) : null;
  if (!auth) return { error: NextResponse.json({ error: "forbidden" }, { status: 403 }) };
  return { db, auth, userId: profile.userId };
}

export async function GET(req: Request, { params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const who = await identify(req, id);
  if ("error" in who) return who.error;
  const d = await getReviewDetail(who.db, who.auth.location.organizationId, id);
  if (!d) return NextResponse.json({ error: "not_found" }, { status: 404 });
  return NextResponse.json({
    locationName: d.location.name,
    stars: d.review.starRating,
    comment: d.review.comment,
    status: d.review.status,
    riskFlags: d.analysis?.riskFlags ?? [],
    draft: d.reply?.content ?? d.drafts[0]?.content ?? "",
    published: d.review.status === "replied" ? (d.reply?.content ?? "") : null,
    lastError: d.review.status === "publish_failed" ? d.reply?.errorMessage ?? null : null,
  });
}

const body = z.object({ content: z.string().max(4096), confirmRisk: z.boolean().optional() });

export async function POST(req: Request, { params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const who = await identify(req, id);
  if ("error" in who) return who.error;
  const parsed = body.safeParse(await req.json().catch(() => null));
  if (!parsed.success) return NextResponse.json({ error: "bad_request" }, { status: 400 });
  const result = await publishReply(who.db, {
    reviewId: id,
    organizationId: who.auth.location.organizationId,
    content: parsed.data.content,
    publishedBy: "line_user",
    lineUserId: who.userId,
    confirmRisk: parsed.data.confirmRisk,
  });
  return NextResponse.json(result);
}
