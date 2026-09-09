import { Badge, type BadgeTone } from "@/components/ui/badge";

const CONFIG: Record<
  "win" | "loss" | "draw",
  { label: string; tone: BadgeTone }
> = {
  win: { label: "Win", tone: "emerald" },
  loss: { label: "Loss", tone: "crimson" },
  draw: { label: "Draw", tone: "neutral" },
};

export function ResultBadge({
  result,
  className,
}: {
  result: "win" | "loss" | "draw";
  className?: string;
}) {
  const config = CONFIG[result];
  return (
    <Badge tone={config.tone} className={className}>
      {config.label}
    </Badge>
  );
}