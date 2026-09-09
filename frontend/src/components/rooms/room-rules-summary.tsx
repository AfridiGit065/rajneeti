import { RULES } from "@/lib/game/rules";
import { Card } from "@/components/ui/card";
import { Coins, Crown, Shield, Users } from "@/components/ui/icons";

const ITEMS = [
  {
    icon: <Users className="size-4" aria-hidden />,
    title: "Players",
    text: `${RULES.minPlayers}–${RULES.maxPlayers} players — the host chooses room capacity.`,
  },
  {
    icon: <Shield className="size-4" aria-hidden />,
    title: "Influence Cards",
    text: `Each player starts with ${RULES.maxInfluencePerPlayer} face-down character cards.`,
  },
  {
    icon: <Coins className="size-4" aria-hidden />,
    title: "Starting Treasury",
    text: `Each player starts the game with ${RULES.startingCoins} coins.`,
  },
  {
    icon: <Crown className="size-4" aria-hidden />,
    title: "Victory Condition",
    text: "The last surviving player with influence remaining wins the game.",
  },
] as const;

export function RoomRulesSummary() {
  return (
    <Card className="h-full">
      <p className="mb-4 text-xs font-semibold uppercase tracking-[0.25em] text-gold-400">
        Rules Summary
      </p>
      <ul className="space-y-4">
        {ITEMS.map((item) => (
          <li key={item.title} className="flex items-start gap-3">
            <span className="mt-0.5 flex size-8 shrink-0 items-center justify-center rounded-lg border border-gold-500/25 bg-gold-500/10 text-gold-300">
              {item.icon}
            </span>
            <div>
              <p className="text-sm font-semibold text-ivory">{item.title}</p>
              <p className="mt-0.5 text-xs leading-relaxed text-muted">{item.text}</p>
            </div>
          </li>
        ))}
      </ul>
    </Card>
  );
}