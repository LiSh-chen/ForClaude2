import { simPostbackAction } from "@/app/dev/line/actions";

type N = Record<string, unknown> & { type?: string; contents?: unknown };
const SIZE: Record<string, string> = { xs: "text-[11px]", sm: "text-xs", md: "text-sm", lg: "text-base", xl: "text-xl" };
const GAP: Record<string, string> = { xs: "gap-0.5", sm: "gap-1.5", md: "gap-3" };
const MARGIN: Record<string, string> = { sm: "mt-1.5", md: "mt-3", lg: "mt-4" };

/** 只支援本系統用到的 Flex 元件子集，用來在沒有 LINE 時預覽卡片長相。 */
export function FlexPreview({ node, userId }: { node: unknown; userId: string }) {
  return <Render n={node as N} userId={userId} />;
}

function Render({ n, userId }: { n: N; userId: string }) {
  const kids = (Array.isArray(n.contents) ? n.contents : []) as N[];
  switch (n.type) {
    case "carousel":
      return <div className="flex gap-2 overflow-x-auto pb-2">{kids.map((k, i) => <div key={i} className="w-72 shrink-0"><Render n={k} userId={userId} /></div>)}</div>;
    case "bubble":
      return (
        <div className="w-72 overflow-hidden rounded-xl border bg-white text-black shadow-sm">
          {(["header", "body", "footer"] as const).map((part) => n[part] ? <div key={part} className={part === "body" ? "px-3 py-2" : "px-3 py-2"}><Render n={n[part] as N} userId={userId} /></div> : null)}
        </div>
      );
    case "box": {
      const row = n.layout === "horizontal";
      return <div className={`flex ${row ? "flex-row" : "flex-col"} ${GAP[String(n.spacing)] ?? ""} ${MARGIN[String(n.margin)] ?? ""}`}>{kids.map((k, i) => <div key={i} className={row ? "flex-1" : ""}><Render n={k} userId={userId} /></div>)}</div>;
    }
    case "text":
      return <p className={`${SIZE[String(n.size)] ?? "text-sm"} ${n.weight === "bold" ? "font-bold" : ""} ${MARGIN[String(n.margin)] ?? ""} whitespace-pre-wrap break-words`} style={{ color: typeof n.color === "string" ? n.color : undefined }}>{String(n.text)}</p>;
    case "separator":
      return <hr className={`border-gray-200 ${MARGIN[String(n.margin)] ?? ""}`} />;
    case "button": {
      const a = n.action as { type: string; label: string; data?: string; uri?: string; displayText?: string };
      const cls = `block w-full rounded-md px-2 py-2 text-center text-xs ${n.style === "primary" ? "bg-[#06C755] font-semibold text-white" : "border border-gray-300 text-gray-800"}`;
      if (a.type === "uri") {
        const href = a.uri!.includes("/liff/review/") ? `${a.uri}${a.uri!.includes("?") ? "&" : "?"}mockUser=${encodeURIComponent(userId)}` : a.uri!;
        return <a className={cls} href={href} target="_blank" rel="noreferrer">{a.label}</a>;
      }
      return (
        <form action={simPostbackAction}>
          <input type="hidden" name="userId" value={userId} />
          <input type="hidden" name="data" value={a.data} />
          <input type="hidden" name="label" value={a.displayText ?? ""} />
          <button type="submit" className={cls}>{a.label}</button>
        </form>
      );
    }
    default:
      return null;
  }
}
