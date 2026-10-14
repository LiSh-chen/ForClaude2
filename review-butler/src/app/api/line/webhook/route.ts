import { getDb } from "@/lib/db/client";
import { handleWebhook } from "@/lib/line/handlers";

/** LINE Messaging API webhook。簽章必須以「未解析的原始 body」驗證。 */
export async function POST(req: Request) {
  const raw = await req.text();
  const { status } = await handleWebhook({ db: await getDb() }, raw, req.headers.get("x-line-signature"));
  return new Response(null, { status });
}
