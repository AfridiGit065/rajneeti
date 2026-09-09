import { cn } from "@/lib/cn";
import { Trophy } from "@/components/ui/icons";
import type { UserPublic } from "@/types/user";

const SIZES = {
  sm: {
    avatar: "size-8 text-xs",
    name: "text-sm",
    rating: "text-[0.68rem]",
  },
  md: {
    avatar: "size-11 text-base",
    name: "text-base",
    rating: "text-xs",
  },
} as const;

export function PlayerMiniProfile({
  user,
  size = "md",
  showRating = true,
  className,
}: {
  user: UserPublic;
  size?: "sm" | "md";
  showRating?: boolean;
  className?: string;
}) {
  const s = SIZES[size];
  const rating = user.rating ?? user.level * 100;
  const title = user.rating ? `রেটিং ${user.rating}` : `লেভেল ${user.level}`;

  return (
    <div className={cn("flex items-center gap-3", className)}>
      <span
        className={cn(
          "flex shrink-0 items-center justify-center rounded-full font-bold text-deep-950",
          "bg-gradient-to-b from-forest-500 to-forest-600",
          size === "md" ? "border-2 border-gold-500/45" : "border border-forest-400/40",
          user.titled && "ring-1 ring-gold-400/70",
          s.avatar,
        )}
        aria-hidden
      >
        {user.avatarInitial}
      </span>
      <span className="flex min-w-0 flex-col leading-tight">
        <span className={cn("truncate font-semibold text-ivory", s.name)}>
          {user.displayName}
        </span>
        {showRating ? (
          <span
            className={cn("flex items-center gap-1 font-medium text-gold-300/90", s.rating)}
            title={title}
          >
            <Trophy
              className={cn(size === "md" ? "size-3.5" : "size-3", "text-gold-400")}
              aria-hidden
            />
            {rating}
          </span>
        ) : null}
      </span>
    </div>
  );
}