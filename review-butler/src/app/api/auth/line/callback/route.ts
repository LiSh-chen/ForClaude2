import { NextResponse } from "next/server";
import { getLine } from "@/lib/adapters";
import { getCurrentUser } from "@/lib/auth";
import { getDb } from "@/lib/db/client";
import { upsertBinding } from "@/lib/line/bind";
import { textMessage } from "@/lib/line/flex";
import { lineCallbackUrl } from "@/lib/line/links";
import { LINE_NONCE_COOKIE, verifyLoginState } from "@/lib/line/login-state";

function back(req: Request, params: Record<string, string>) {
  const url = new URL("/dashboard/line", req.url);
  for (const [k, v] of Object.entries(params)) url.searchParams.set(k, v);
  const res = NextResponse.redirect(url);
  res.cookies.delete({ name: LINE_NONCE_COOKIE, path: "/api/auth/line" });
  return res;
}

/** LINE Login 回呼：驗證 state 與 nonce、確認仍是同一家店登入中，才以授權碼換身分並建立綁定。 */
export async function GET(req: Request) {
  const url = new URL(req.url);
  if (url.searchParams.get("error")) return back(req, { error: "您取消了 LINE 授權，沒有完成綁定" });
  const me = await getCurrentUser();
  if (!me) return NextResponse.redirect(new URL("/login", req.url));
  const cookieNonce = req.headers.get("cookie")?.match(new RegExp(`${LINE_NONCE_COOKIE}=([^;]+)`))?.[1];
  const verified = verifyLoginState(url.searchParams.get("state"), cookieNonce);
  const code = url.searchParams.get("code");
  if (!verified || !code || verified.orgId !== me.org.id) return back(req, { error: "綁定連結已失效或不正確，請重新操作" });

  const line = getLine();
  try {
    const profile = await line.exchangeLoginCode({ code, redirectUri: lineCallbackUrl() });
    const { created } = await upsertBinding(await getDb(), me.org.id, profile);
    // 立刻推一則訊息：同時驗證「這位已經加好友、真的收得到通知」
    await line.pushMessage(profile.userId, [textMessage("✅ 綁定成功！之後有新的 Google 評論，我會把寫好的回覆草稿傳給您。")]).catch(() => undefined);
    return back(req, { notice: created ? `已綁定 ${profile.displayName || "LINE 帳號"}。如果沒有收到歡迎訊息，請先加官方帳號好友。` : "這個 LINE 帳號已經綁定過了" });
  } catch (e) {
    console.error("[line login] callback failed:", e);
    return back(req, { error: "LINE 綁定失敗，請再試一次" });
  }
}
