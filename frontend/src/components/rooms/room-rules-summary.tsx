import { RULES } from "@/lib/game/rules";
import { Card } from "@/components/ui/card";
import { Coins, Crown, Shield, Users } from "@/components/ui/icons";

const ITEMS = [
  {
    icon: <Users className="size-4" aria-hidden />,
    title: "খেলোয়াড়",
    text: `${RULES.minPlayers}–${RULES.maxPlayers} জন — হোস্ট রুমের আকার ঠিক করে।`,
  },
  {
    icon: <Shield className="size-4" aria-hidden />,
    title: "গোপন কার্ড",
    text: `প্রত্যেকে ${RULES.maxInfluencePerPlayer}টি লুকানো ইনফ্লুয়েন্স কার্ড নিয়ে শুরু করে।`,
  },
  {
    icon: <Coins className="size-4" aria-hidden />,
    title: "শুরুর কয়েন",
    text: `${RULES.startingCoins} কয়েন হাতে নিয়েই খেলা শুরু।`,
  },
  {
    icon: <Crown className="size-4" aria-hidden />,
    title: "জয়ের শর্ত",
    text: "শেষ পর্যন্ত টিকে থাকা শেষ খেলোয়াড়ই জয়ী।",
  },
] as const;

export function RoomRulesSummary() {
  return (
    <Card className="h-full">
      <p className="mb-4 text-xs font-semibold uppercase tracking-[0.25em] text-gold-400">
        খেলার নিয়ম
      </p>
      <ul className="space-y-4">
        {ITEMS.map((item) => (
          <li key={item.title} className="flex items-start gap-3">
            <span className="mt-0.5 flex size-8 shrink-0 items-center justify-center rounded-lg border border-gold-500/25 bg-gold-500/10 text-gold-300">
              {item.icon}
            </span>
            <div>
              <p className="font-bengali text-sm font-semibold text-ivory">{item.title}</p>
              <p className="mt-0.5 text-xs leading-relaxed text-muted">{item.text}</p>
            </div>
          </li>
        ))}
      </ul>
    </Card>
  );
}