import Image from "next/image";
import { cn } from "@/lib/cn";
import { CARD_BACK_PATH } from "@/lib/game/characters";

/**
 * The official Rajneeti card back, used for every face-down card.
 *
 * Kept as a single component so the asset is referenced in exactly one place
 * and cannot drift between the hand, the opponent seats and the deck. Sizing is
 * always driven by the caller through the surrounding box, so this renders at
 * whatever dimensions its parent already uses.
 *
 * The `alt` is intentionally empty: the art is decorative, and a hidden card
 * must not announce anything about the character it conceals. The accessible
 * name for a card comes from its container, which already reads
 * "গোপন প্রভাব কার্ড" for a face-down card.
 */
export function CardBackImage({
  className,
  sizes = "180px",
  priority = false,
}: {
  className?: string;
  sizes?: string;
  priority?: boolean;
}) {
  return (
    <Image
      src={CARD_BACK_PATH}
      alt=""
      fill
      sizes={sizes}
      priority={priority}
      draggable={false}
      className={cn("select-none object-cover", className)}
    />
  );
}
