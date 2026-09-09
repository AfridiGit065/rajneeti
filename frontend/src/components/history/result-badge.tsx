import { Badge, type BadgeTone } from "@/components/ui/badge";

const CONFIG: Record<
  "win" | "loss" | "draw",
  { label: string; tone: BadgeTone }
> = {
  win: { label: "জয়", tone: "emerald" },
  loss: { label: "পরাজয়", tone: "crimson" },
  draw: { label: "ড্র", tone: "neutral" },
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