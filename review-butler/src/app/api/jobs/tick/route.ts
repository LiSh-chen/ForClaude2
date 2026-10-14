import { getDb } from "@/lib/db/client";
import { runTick } from "@/lib/jobs/tick";

export const maxDuration = 300;

function authorized(req: Request): boolean {
  const secret = process.env.CRON_SECRET;
  if (!secret) return process.env.NODE_ENV !== "production"; // 正式環境一定要設定 CRON_SECRET
  return req.headers.get("authorization") === `Bearer ${secret}`;
}

async function handle(req: Request) {
  if (!authorized(req)) return new Response("Unauthorized", { status: 401 });
  return Response.json(await runTick(await getDb()));
}

// Vercel Cron 以 GET 呼叫並帶 `Authorization: Bearer $CRON_SECRET`
export const GET = handle;
export const POST = handle;
