import { eq } from "drizzle-orm";
import { SubmitButton } from "@/components/submit-button";
import { Card } from "@/components/ui/card";
import { Input, Label, Textarea } from "@/components/ui/field";
import { requireUser } from "@/lib/auth";
import { brandSettings } from "@/lib/db/schema";
import { saveSettingsAction } from "../actions";

const TONES = [
  ["親切", "溫暖、像鄰家店家，大多數店家適合"],
  ["專業", "穩重有禮，適合診所、補習班"],
  ["活潑", "輕快有親和力，適合年輕客群"],
] as const;
const SALUTATIONS = [["我們", "「我們很開心…」"], ["本店", "「本店會改進…」"], ["店名", "用您的店名自稱"]] as const;

function Choice({ name, value, hint, checked }: { name: string; value: string; hint: string; checked: boolean }) {
  return (
    <label className="block cursor-pointer">
      <input type="radio" name={name} value={value} defaultChecked={checked} className="peer sr-only" />
      <span className="block rounded-md border p-3 peer-checked:border-primary peer-checked:ring-2 peer-checked:ring-primary peer-focus-visible:ring-2">
        <span className="block font-medium">{value}</span>
        <span className="block text-sm text-muted-foreground">{hint}</span>
      </span>
    </label>
  );
}

export default async function SettingsPage() {
  const { db, org } = await requireUser();
  const [b] = await db.select().from(brandSettings).where(eq(brandSettings.organizationId, org.id));
  return (
    <>
      <div>
        <h1 className="text-xl font-bold">回覆風格</h1>
        <p className="text-sm text-muted-foreground">設定一次，之後 AI 寫的草稿都會照這個風格。不確定的話，用預設就好。</p>
      </div>
      <form action={saveSettingsAction} className="space-y-5">
        <Card className="space-y-3">
          <p className="font-semibold">語氣</p>
          <div className="space-y-2">{TONES.map(([v, h]) => <Choice key={v} name="tone" value={v} hint={h} checked={(b?.tone ?? "親切") === v} />)}</div>
        </Card>
        <Card className="space-y-3">
          <p className="font-semibold">怎麼稱呼自己</p>
          <div className="space-y-2">{SALUTATIONS.map(([v, h]) => <Choice key={v} name="salutation" value={v} hint={h} checked={(b?.salutation ?? "我們") === v} />)}</div>
        </Card>
        <Card className="space-y-4">
          <div>
            <Label htmlFor="signature">結尾署名（選填）</Label>
            <Input id="signature" name="signature" maxLength={60} defaultValue={b?.signature ?? ""} placeholder="例如：阿財牛肉麵 全體同仁 敬上" />
          </div>
          <div>
            <Label htmlFor="storeInfo">店家資訊（選填）</Label>
            <Textarea id="storeInfo" name="storeInfo" maxLength={1500} defaultValue={b?.storeInfo ?? ""} placeholder={"例如：每週一公休；平日 11:00–21:00；招牌是紅燒牛肉麵；可以預約包廂。"} />
            <p className="mt-1.5 text-xs text-muted-foreground">AI 只會引用您寫在這裡的內容，不會自己編造營業時間或活動。</p>
          </div>
        </Card>
        <SubmitButton size="lg" className="w-full" pendingText="儲存中…">儲存設定</SubmitButton>
      </form>
    </>
  );
}
