import { NextResponse } from "next/server";
import { newSimUser, mockLine } from "@/lib/line/simulator";

/** 模擬 LINE Login 授權完成：與真實流程一致，是一次純粹的 GET 導向（不經過 server action）。 */
export async function GET(req: Request) {
  const line = mockLine();
  if (!line || process.env.NODE_ENV === "production") return new Response("Not found", { status: 404 });
  const q = new URL(req.url).searchParams;
  const state = q.get("state");
  const redirectUri = q.get("redirect_uri");
  if (!state || !redirectUri) return new Response("Bad request", { status: 400 });
  let userId = q.get("userId") ?? "";
  if (!userId || !line.users.has(userId)) userId = newSimUser(q.get("name") ?? "");
  // 只取路徑，避免 APP_BASE_URL 與本機實際網址不同時導向錯誤的主機
  const target = new URL(new URL(redirectUri, "http://localhost").pathname, req.url);
  target.searchParams.set("code", `mock-code:${userId}`);
  target.searchParams.set("state", state);
  return NextResponse.redirect(target);
}
