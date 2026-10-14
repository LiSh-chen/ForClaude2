"use client";

import { useEffect, useState } from "react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Textarea } from "@/components/ui/field";
import { stars } from "@/lib/format";

interface Detail {
  locationName: string;
  stars: number;
  comment: string | null;
  status: string;
  riskFlags: string[];
  draft: string;
  published: string | null;
  lastError: string | null;
}

type View = { phase: "loading" } | { phase: "error"; message: string } | { phase: "ready"; detail: Detail } | { phase: "done"; text: string };

const RESULT_TEXT: Record<string, string> = {
  published: "✅ 已發布到 Google",
  already_published: "這則已經回覆過了",
  in_progress: "正在發布中，請稍候再看一次",
  risk_confirmation_required: "這則評論有風險提醒，請先勾選確認",
};

/**
 * 在 LINE 內開啟的編輯頁（LIFF）。身分來自 LINE 的 ID token，由伺服器驗證；
 * 本機模擬（mock）時改用 ?mockUser= 帶入使用者，方便在沒有 LINE 的環境測試。
 */
export function LiffReview({ reviewId, liffId, mock }: { reviewId: string; liffId: string; mock: boolean }) {
  const [view, setView] = useState<View>({ phase: "loading" });
  const [token, setToken] = useState("");
  const [content, setContent] = useState("");
  const [confirmRisk, setConfirmRisk] = useState(false);
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        let idToken: string;
        if (mock) {
          const user = new URLSearchParams(location.search).get("mockUser");
          if (!user) throw new Error("開發模式：請從 /dev/line 模擬器的按鈕開啟這個頁面");
          idToken = `mock-id:${user}`;
        } else {
          const liff = (await import("@line/liff")).default;
          await liff.init({ liffId });
          if (!liff.isLoggedIn()) { liff.login(); return; }
          idToken = liff.getIDToken() ?? "";
        }
        const res = await fetch(`/api/liff/review/${reviewId}`, { headers: { Authorization: `Bearer ${idToken}` } });
        if (res.status === 403) throw new Error("您沒有這間店的操作權限");
        if (!res.ok) throw new Error("讀取失敗，請回到 LINE 重新開啟");
        const detail = (await res.json()) as Detail;
        if (cancelled) return;
        setToken(idToken);
        setContent(detail.draft);
        setView({ phase: "ready", detail });
      } catch (e) {
        if (!cancelled) setView({ phase: "error", message: e instanceof Error ? e.message : "發生錯誤" });
      }
    })();
    return () => { cancelled = true; };
  }, [reviewId, liffId, mock]);

  async function publish() {
    setBusy(true);
    setMessage(null);
    try {
      const res = await fetch(`/api/liff/review/${reviewId}`, {
        method: "POST",
        headers: { Authorization: `Bearer ${token}`, "Content-Type": "application/json" },
        body: JSON.stringify({ content, confirmRisk }),
      });
      const out = (await res.json()) as { status?: string; reason?: string; error?: string };
      if (out.status === "published" || out.status === "already_published") setView({ phase: "done", text: RESULT_TEXT[out.status]! });
      else if (out.status === "failed") setMessage(`❌ 發布失敗：${out.error}。內容還在，可以再按一次。`);
      else setMessage(out.status === "invalid" ? (out.reason ?? "內容不符合規定") : (RESULT_TEXT[out.status ?? ""] ?? "發布失敗，請稍後再試"));
    } catch {
      setMessage("連線失敗，請稍後再試");
    } finally {
      setBusy(false);
    }
  }

  const shell = (children: React.ReactNode) => <main className="mx-auto max-w-md space-y-4 px-4 py-6">{children}</main>;

  if (view.phase === "loading") return shell(<p className="text-center text-muted-foreground">載入中…</p>);
  if (view.phase === "error") return shell(<p role="alert" className="rounded-md bg-red-50 p-3 text-red-800">{view.message}</p>);
  if (view.phase === "done") {
    return shell(
      <Card className="space-y-3 text-center">
        <p className="text-lg font-semibold">{view.text}</p>
        {!mock && <Button onClick={async () => (await import("@line/liff")).default.closeWindow()}>回到 LINE</Button>}
      </Card>,
    );
  }

  const d = view.detail;
  const risky = d.riskFlags.length > 0;
  if (d.published !== null) {
    return shell(
      <Card className="space-y-2">
        <p className="font-semibold">這則評論已經回覆過了</p>
        <p className="whitespace-pre-wrap">{d.published}</p>
      </Card>,
    );
  }
  return shell(
    <>
      <Card className="space-y-2">
        <div className="flex items-center justify-between">
          <span className="text-lg text-amber-500" role="img" aria-label={`${d.stars} 星`}>{stars(d.stars)}</span>
          <span className="text-xs text-muted-foreground">{d.locationName}</span>
        </div>
        <p className="whitespace-pre-wrap">{d.comment ?? "（顧客只留下星等，沒有文字）"}</p>
        {risky && <div className="flex flex-wrap gap-1.5">{d.riskFlags.map((f) => <Badge key={f} variant="danger">⚠ {f}</Badge>)}</div>}
      </Card>
      {d.lastError && <p role="alert" className="rounded-md bg-red-50 p-3 text-sm text-red-800">上次發布沒有成功：{d.lastError}。修改後可以再發布一次。</p>}
      <Card className="space-y-3">
        <p className="font-semibold">您的回覆</p>
        <Textarea value={content} onChange={(e) => setContent(e.target.value)} rows={8} className="min-h-48 text-base" aria-label="回覆內容" placeholder="在這裡寫回覆…" />
        <p className="text-xs text-muted-foreground">建議 40–120 字；請避免放網址、優惠或要求顧客改評論，Google 可能不接受。</p>
        {risky && (
          <label className="flex items-start gap-2 text-sm">
            <input type="checkbox" className="mt-1 size-5" checked={confirmRisk} onChange={(e) => setConfirmRisk(e.target.checked)} />
            <span>我已了解這則評論有風險提醒，並確認這份回覆內容後再發布</span>
          </label>
        )}
        {message && <p role="alert" className="rounded-md bg-amber-50 p-3 text-sm text-amber-900">{message}</p>}
        <Button size="lg" className="w-full" disabled={busy || !content.trim() || (risky && !confirmRisk)} onClick={publish}>
          {busy ? "發布中…" : "發布到 Google"}
        </Button>
      </Card>
    </>,
  );
}
