import type { LucideIcon } from "lucide-react";
import type { GameActionId } from "@/types/game";
import { cn } from "@/lib/cn";
import { getAction } from "@/lib/game/actions";
import { CHARACTERS, RULES } from "@/lib/game/rules";
import {
  Coins,
  Globe,
  ScrollText,
  Hand,
  RefreshCw,
  Skull,
  Crown,
  BookOpen,
  Dices,
  Layers,
  ListChecks,
  Ghost,
  Gavel,
  Shield,
  Trophy,
  Gamepad2,
} from "@/components/ui/icons";

export const ACTION_ICONS: Record<string, LucideIcon> = {
  income: Coins,
  foreign_aid: Globe,
  tax: ScrollText,
  steal: Hand,
  exchange: RefreshCw,
  assassinate: Skull,
  coup: Crown,
};

export interface TutorialStep {
  key: string;
  indexBn: string;
  labelBn: string;
  labelEn: string;
  icon: LucideIcon;
  summary: string;
  body: () => React.ReactNode;
}

function Bullet({
  icon,
  children,
}: {
  icon?: React.ReactNode;
  children: React.ReactNode;
}) {
  return (
    <li className="flex items-start gap-3">
      {icon ? (
        <span className="mt-0.5 flex size-6 shrink-0 items-center justify-center rounded-md border border-gold-500/25 bg-deep-800/60 text-gold-400">
          {icon}
        </span>
      ) : (
        <span className="mt-2.5 size-1.5 shrink-0 rounded-full bg-gold-500/70" />
      )}
      <span className="text-sm leading-relaxed text-parchment-300">{children}</span>
    </li>
  );
}

function StatChip({ label, value }: { label: string; value: number | string }) {
  return (
    <div className="flex flex-col items-center gap-1 rounded-xl border border-forest-500/25 bg-deep-800/60 px-4 py-3 text-center">
      <span className="font-bengali text-xl font-bold text-gold-300">{value}</span>
      <span className="text-[11px] text-muted">{label}</span>
    </div>
  );
}

function CardBack() {
  return (
    <div
      className={cn(
        "flex size-14 items-center justify-center rounded-xl border border-gold-500/40",
        "bg-gradient-to-b from-deep-700 to-deep-900 shadow-panel",
      )}
    >
      <span className="text-lg font-bold text-gold-400">?</span>
    </div>
  );
}

function CharacterChip({ id }: { id: string }) {
  const character = CHARACTERS.find((c) => c.id === id);
  if (!character) return null;
  return (
    <div className="flex flex-col items-center gap-1 rounded-xl border border-gold-500/25 bg-deep-800/60 px-3 py-2 text-center">
      <span className="text-xs font-bold text-ivory">{character.nameBn}</span>
      <span className="text-[10px] uppercase tracking-wider text-muted">
        {character.nameEn}
      </span>
    </div>
  );
}

/** Visual chip showing a gain (+N) or cost (−N) in coins. */
export function CoinDelta({ amount, label }: { amount: number; label: string }) {
  const gain = amount >= 0;
  return (
    <div
      className={cn(
        "flex items-center gap-1.5 rounded-lg border px-2.5 py-1 text-sm font-bold",
        gain ? "border-forest-500/40 text-forest-300" : "border-crimson-500/40 text-crimson-300",
      )}
    >
      {gain ? <Coins className="size-4" aria-hidden /> : null}
      <span>{gain ? `+${amount}` : `${amount}`}</span>
      <span className="text-[11px] font-normal text-muted">{label}</span>
    </div>
  );
}

interface ActionExampleProps {
  actionId: GameActionId;
}

/** Visual action example card with gain/cost chips from the sanctioned rule set. */
export function ActionExample({ actionId }: ActionExampleProps) {
  const action = getAction(actionId);
  const Icon = ACTION_ICONS[actionId];
  const character = action.requiresCharacter
    ? CHARACTERS.find((c) => c.id === action.requiresCharacter)
    : null;

  return (
    <div className="flex flex-col gap-2.5 rounded-xl border border-forest-500/25 bg-deep-800/50 p-3.5">
      <div className="flex items-center gap-2.5">
        <span
          className={cn(
            "flex size-9 shrink-0 items-center justify-center rounded-lg border",
            action.cost
              ? "border-crimson-500/30 bg-crimson-600/15 text-crimson-300"
              : "border-gold-500/30 bg-gold-500/10 text-gold-400",
          )}
        >
          <Icon className="size-4.5" aria-hidden />
        </span>
        <div className="min-w-0">
          <p className="truncate text-sm font-semibold text-ivory">{action.nameBn}</p>
          <p className="truncate text-[11px] uppercase tracking-wider text-muted">
            {action.nameEn}
          </p>
        </div>
      </div>
      <div className="flex flex-wrap items-center gap-1.5">
        {typeof action.gain === "number" && action.gain > 0 ? (
          <CoinDelta amount={action.gain} label={character ? character.nameBn : "কয়েন"} />
        ) : null}
        {action.cost ? <CoinDelta amount={-action.cost} label="খরচ" /> : null}
        {action.id === "exchange" ? (
          <span className="rounded-lg border border-parchment-500/30 bg-parchment-500/10 px-2.5 py-1 text-sm font-bold text-parchment-300">
            2 কার্ড
          </span>
        ) : null}
        {action.challengeable ? (
          <span className="rounded-lg bg-deep-700/70 px-2 py-1 text-[10px] font-semibold text-muted">
            চ্যালেঞ্জযোগ্য
          </span>
        ) : null}
        {action.blockable ? (
          <span className="rounded-lg bg-deep-700/70 px-2 py-1 text-[10px] font-semibold text-muted">
            ব্লকযোগ্য
          </span>
        ) : null}
      </div>
    </div>
  );
}

export const TUTORIAL_STEPS: readonly TutorialStep[] = [
  {
    key: "objective",
    indexBn: "০১",
    labelBn: "লক্ষ্য",
    labelEn: "Objective",
    icon: BookOpen,
    summary: "সবাই টিকবে — শেষ যে টিকে, সে-ই জয়ী।",
    body: () => (
      <ul className="space-y-3">
        <Bullet icon={<Gamepad2 className="size-3.5" aria-hidden />}>
          ২ থেকে {RULES.maxPlayers} জন খেলোয়াড় খেলে। প্রত্যেকে{" "}
          {RULES.maxInfluencePerPlayer}টি লুকানো ইনফ্লুয়েন্স কার্ড নিয়ে শুরু করে।
        </Bullet>
        <Bullet>
          প্রতিটি পালায় ইনফ্লুয়েন্স কমতে পারে — ছিটকে পড়া মানে খেলা শেষ।{" "}
          <span className="text-muted">(Lose influence, lose the game.)</span>
        </Bullet>
        <Bullet>
          শেষ কার্ড ধরে থাকা খেলোয়াড়ই বিজয়ী।{" "}
          <span className="text-muted">(Last player with influence wins.)</span>
        </Bullet>
      </ul>
    ),
  },
  {
    key: "setup",
    indexBn: "০২",
    labelBn: "প্রস্তুতি",
    labelEn: "Setup",
    icon: Dices,
    summary: "১৫ কার্ড · প্রত্যেকে ২টি গোপন কার্ড + ২ কয়েন।",
    body: () => (
      <div className="space-y-4">
        <ul className="space-y-3">
          <Bullet>
            ডেকে {RULES.deckSize}টি কার্ড — {CHARACTERS.length}টি চরিত্র, প্রতিটির{" "}
            {RULES.copiesPerCharacter}টি কপি।{" "}
            <span className="text-muted">(5 characters, 3 copies each.)</span>
          </Bullet>
          <Bullet>
            প্রত্যেককে {RULES.maxInfluencePerPlayer}টি কার্ড মুখ নিচে দেওয়া হয় এবং{" "}
            {RULES.startingCoins}টি কয়েন থাকে।
          </Bullet>
        </ul>
        <div className="flex flex-wrap items-end gap-4 rounded-xl border border-forest-500/20 bg-deep-800/40 p-4">
          <div className="flex items-start gap-2">
            <CardBack />
            <CardBack />
          </div>
          <span className="text-xs text-muted">+</span>
          <div className="flex items-center gap-1.5 text-gold-300">
            <Coins className="size-5" aria-hidden />
            <Coins className="size-5" aria-hidden />
            <span className="text-xs text-muted">শুরুর কয়েন</span>
          </div>
        </div>
        <div className="grid grid-cols-2 gap-2 sm:grid-cols-4">
          <StatChip label="সর্বোচ্চ খেলোয়াড়" value={RULES.maxPlayers} />
          <StatChip label="সর্বনিম্ন খেলোয়াড়" value={RULES.minPlayers} />
          <StatChip label="শুরুতে কয়েন" value={RULES.startingCoins} />
          <StatChip label="কার্ডের সংখ্যা" value={RULES.deckSize} />
        </div>
      </div>
    ),
  },
  {
    key: "influence",
    indexBn: "০৩",
    labelBn: "ইনফ্লুয়েন্স কার্ড",
    labelEn: "Influence Cards",
    icon: Layers,
    summary: "তোমার গোপন রাজনৈতিক প্রভাব।",
    body: () => (
      <div className="space-y-4">
        <ul className="space-y-3">
          <Bullet>
            কার্ড = প্রভাব। টিকে থাকার জন্য প্রভাব প্রয়োজন।{" "}
            <span className="text-muted">(Cards are your influence.)</span>
          </Bullet>
          <Bullet>
            কার্ড <strong className="text-ivory">সবাই থেকে লুকানো</strong> থাকে — শুধু তুমিই
            জানো তোমার হাতে কী আছে।
          </Bullet>
          <Bullet>
            ইনফ্লুয়েন্স হারালে একটি কার্ড প্রকাশ পায়; দুটিই গেলে ছিটকে পড়ো।
          </Bullet>
        </ul>
        <div className="grid grid-cols-2 gap-2 sm:grid-cols-3">
          {CHARACTERS.map((c) => (
            <CharacterChip key={c.id} id={c.id} />
          ))}
        </div>
      </div>
    ),
  },
  {
    key: "coins",
    indexBn: "০৪",
    labelBn: "কয়েন",
    labelEn: "Coins",
    icon: Coins,
    summary: "কয়েন দিয়ে কুপ ও সরিয়ে দেওয়া।",
    body: () => (
      <div className="space-y-4">
        <ul className="space-y-3">
          <Bullet>
            কয়েন = ক্ষমতার মুদ্রা। কাজে লাগে ক্ষমতা দখল (কুপ) ও সরিয়ে দিতে।{" "}
            <span className="text-muted">(Coins buy Coup and Assassinate.)</span>
          </Bullet>
          <Bullet>
            {RULES.forcedCoupThreshold}+ কয়েন হলে বাধ্যতামূলক কুপ — ঝুঁকি এড়াতে{" "}
            {RULES.coupCost} কয়েনের নিচে থাকাই বুদ্ধিমান।
          </Bullet>
          <Bullet>
            প্রতিটি পালায় আয়, অনুদান বা কর দিয়ে কয়েন বাড়াও।
          </Bullet>
        </ul>
        <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
          <div className="rounded-xl border border-gold-500/25 bg-deep-800/50 p-4 text-center">
            <p className="text-2xl font-bold text-gold-300">{RULES.incomeGain}+</p>
            <p className="mt-1 text-xs text-muted">আয়</p>
          </div>
          <div className="rounded-xl border border-gold-500/25 bg-deep-800/50 p-4 text-center">
            <p className="text-2xl font-bold text-gold-300">{RULES.foreignAidGain}+</p>
            <p className="mt-1 text-xs text-muted">বিদেশি অনুদান</p>
          </div>
          <div className="rounded-xl border border-gold-500/25 bg-deep-800/50 p-4 text-center">
            <p className="text-2xl font-bold text-gold-300">{RULES.taxGain}+</p>
            <p className="mt-1 text-xs text-muted">কর</p>
          </div>
          <div className="grid grid-cols-2 gap-3 sm:col-span-3 sm:grid-cols-2 lg:col-span-1">
            <div className="flex flex-col items-center justify-center rounded-xl border border-crimson-500/30 bg-crimson-600/10 p-4 text-center">
              <p className="text-2xl font-bold text-crimson-300">−{RULES.assassinateCost}</p>
              <p className="mt-1 text-xs text-muted">সরিয়ে দেওয়া</p>
            </div>
            <div className="flex flex-col items-center justify-center rounded-xl border border-crimson-500/30 bg-crimson-600/10 p-4 text-center">
              <p className="text-2xl font-bold text-crimson-300">−{RULES.coupCost}</p>
              <p className="mt-1 text-xs text-muted">ক্ষমতা দখল</p>
            </div>
          </div>
        </div>
      </div>
    ),
  },
  {
    key: "actions",
    indexBn: "০৫",
    labelBn: "অ্যাকশন",
    labelEn: "Actions",
    icon: ListChecks,
    summary: "সাতটি অ্যাকশন · সাথে মানচিত্র।",
    body: () => (
      <div className="space-y-3">
        <p className="text-sm leading-relaxed text-parchment-300">
          প্রতি পালায় একটি অ্যাকশন করো — আয়, অনুদান নাও, কর আদায় করো, চুরি করো, কার্ড
          বদলাও, সরিয়ে দাও বা কুপ করো।{" "}
          <span className="text-muted">(Take one action per turn.)</span>
        </p>
        <div className="grid grid-cols-1 gap-2.5 sm:grid-cols-2">
          {(
            ["income", "foreign_aid", "tax", "steal", "exchange", "assassinate", "coup"] as const
          ).map((id) => (
            <ActionExample key={id} actionId={id} />
          ))}
        </div>
      </div>
    ),
  },
  {
    key: "bluffing",
    indexBn: "০৬",
    labelBn: "ব্লাফিং",
    labelEn: "Bluffing",
    icon: Ghost,
    summary: "হাতে নেই, তবু দাবি করতে পারো।",
    body: () => (
      <div className="space-y-4">
        <ul className="space-y-3">
          <Bullet>
            চরিত্র <em className="text-gold-300">দাবি</em> করলেই অ্যাকশন চলতে পারে — হাতে কার্ড
            না থাকলেও।{" "}
            <span className="text-muted">(Claim characters you may not hold — that&apos;s the game.)</span>
          </Bullet>
          <Bullet>
            দাবি যত বেশি বিশ্বাসযোগ্য, চ্যালেঞ্জের ঝুঁকি তত কম।
          </Bullet>
          <Bullet>
            মিথ্যা ধরা পড়লে ইনফ্লুয়েন্স হারাও — ঝুঁকি হিসেব করেই ব্লাফ করো।
          </Bullet>
        </ul>
        <div className="flex items-center gap-4 rounded-xl border border-forest-500/20 bg-deep-800/40 p-4">
          <div className="flex flex-col items-center gap-2">
            <CardBack />
            <span className="text-[11px] text-muted">হাত (লুকানো)</span>
          </div>
          <span className="text-gold-500">→</span>
          <div className="flex flex-col gap-1">
            <p className="text-sm font-semibold text-ivory">“আমার মন্ত্রী আছে — কর আদায়!”</p>
            <p className="text-xs text-muted">“I claim the Minister — Tax!”</p>
          </div>
        </div>
      </div>
    ),
  },
  {
    key: "challenge",
    indexBn: "০৭",
    labelBn: "চ্যালেঞ্জ",
    labelEn: "Challenge",
    icon: Gavel,
    summary: "মিথ্যা দাবি ধরো — ঝুঁকিও নেও।",
    body: () => (
      <div className="space-y-4">
        <ul className="space-y-3">
          <Bullet>
            দাবি মিথ্যা মনে হলে <strong className="text-ivory">চ্যালেঞ্জ</strong> করো।{" "}
            <span className="text-muted">(Call out a bluff.)</span>
          </Bullet>
          <Bullet>
            দাবি সত্য হলে <strong className="text-crimson-300">তুমিই</strong> ১ ইনফ্লুয়েন্স
            হারাও; মিথ্যা হলে দাবিকারী হারায় এবং অ্যাকশন বাতিল হয়।
          </Bullet>
          <Bullet>
            শুধু চরিত্র-নির্ভর অ্যাকশন (কর, চুরি, বদল, সরিয়ে দেওয়া) চ্যালেঞ্জযোগ্য।
          </Bullet>
        </ul>
        <div className="grid grid-cols-1 gap-2 sm:grid-cols-2">
          <div className="rounded-xl border border-crimson-500/30 bg-crimson-600/10 p-4">
            <p className="text-sm font-semibold text-ivory">সত্য দাবি</p>
            <p className="mt-1 text-xs text-muted">চ্যালেঞ্জকারী ইনফ্লুয়েন্স হারায়</p>
          </div>
          <div className="rounded-xl border border-forest-500/30 bg-forest-600/10 p-4">
            <p className="text-sm font-semibold text-ivory">মিথ্যা দাবি</p>
            <p className="mt-1 text-xs text-muted">দাবিকারী ইনফ্লুয়েন্স হারায়</p>
          </div>
        </div>
      </div>
    ),
  },
  {
    key: "block",
    indexBn: "০৮",
    labelBn: "ব্লক",
    labelEn: "Block",
    icon: Shield,
    summary: "মনে রেখো কে কী আটকাতে পারে।",
    body: () => (
      <div className="space-y-4">
        <ul className="space-y-3">
          <Bullet>
            নির্দিষ্ট চরিত্র নির্দিষ্ট অ্যাকশন <strong className="text-ivory">ব্লক</strong> করতে
            পারে — কিন্তু ব্লকও মিথ্যা হতে পারে, চ্যালেঞ্জযোগ্য!
          </Bullet>
          <Bullet>ক্ষমতা দখল (কুপ) ব্লক করা যায় না।</Bullet>
        </ul>
        <div className="space-y-2">
          <BlockRow blocker="মন্ত্রী · Minister" action="বিদেশি অনুদান · Foreign Aid" />
          <BlockRow blocker="দালাল / আমলা · Broker / Bureaucrat" action="চুরি · Steal" />
          <BlockRow blocker="গোয়েন্দা · Detective" action="সরিয়ে দেওয়া · Assassinate" />
        </div>
      </div>
    ),
  },
  {
    key: "elimination",
    indexBn: "০৯",
    labelBn: "ছিটকে পড়া",
    labelEn: "Elimination",
    icon: Skull,
    summary: "০ ইনফ্লুয়েন্স = খেলা শেষ।",
    body: () => (
      <div className="space-y-4">
        <ul className="space-y-3">
          <Bullet>
            ইনফ্লুয়েন্স হারাও — চ্যালেঞ্জে ধরা পড়লে, সরিয়ে দিলে, বা কুপে।{" "}
            <span className="text-muted">(Lose cards via failed bluffs, Assassinate or Coup.)</span>
          </Bullet>
          <Bullet>দুটো কার্ডই হারালে ছিটকে পড়ো — খেলায় আর অংশ নিতে পারো না।</Bullet>
        </ul>
        <div className="flex items-center gap-3 rounded-xl border border-forest-500/20 bg-deep-800/40 p-4">
          <CardBack />
          <CardBack />
          <span className="text-crimson-300">→</span>
          <CardBack />
          <span className="text-crimson-300">→</span>
          <span className="text-2xl font-bold text-crimson-300">✕</span>
        </div>
      </div>
    ),
  },
  {
    key: "coup",
    indexBn: "১০",
    labelBn: "ক্ষমতা দখল",
    labelEn: "Coup",
    icon: Crown,
    summary: "৭ কয়েন · বন্ধুবিহীন আঘাত।",
    body: () => (
      <div className="space-y-4">
        <ul className="space-y-3">
          <Bullet>
            {RULES.coupCost} কয়েন খরচ করে টার্গেটের ১ ইনফ্লুয়েন্স কেড়ে নাও।{" "}
            <span className="text-muted">(Pay 7, remove one influence.)</span>
          </Bullet>
          <Bullet>
            চ্যালেঞ্জ <strong className="text-ivory">বা</strong> ব্লক করা যায় না — চরিত্র দাবির
            দরকারও নেই।
          </Bullet>
          <Bullet>
            {RULES.forcedCoupThreshold}+ কয়েন হলে পালায় কুপ করা বাধ্যতামূলক।
          </Bullet>
        </ul>
        <div className="flex items-center gap-4 rounded-xl border border-crimson-500/25 bg-deep-800/40 p-4">
          <CoinDelta amount={-RULES.coupCost} label="কুপের খরচ" />
          <span className="text-xs text-muted">শুধু টার্গেট কার্ড হারায় · ব্লকহীন</span>
        </div>
      </div>
    ),
  },
  {
    key: "winning",
    indexBn: "১১",
    labelBn: "জয়",
    labelEn: "Winning",
    icon: Trophy,
    summary: "শেষ দাঁড়িয়ে থাকা খেলোয়াড়ই জয়ী।",
    body: () => (
      <div className="space-y-4">
        <ul className="space-y-3">
          <Bullet>সবাই ছিটকে পড়লে — শেষ টিকে থাকা খেলোয়াড়ই <strong className="text-gold-300">জয়ী</strong>।</Bullet>
          <Bullet>কার্ড গোপন রাখো, কয়েন হিসেব করো, দাবি-ব্লক-চ্যালেঞ্জের খেলাটা বোঝো।</Bullet>
        </ul>
        <div className="flex items-center justify-center gap-4 rounded-xl border border-gold-500/30 bg-gradient-to-b from-gold-500/10 to-transparent p-6">
          <Trophy className="size-8 text-gold-400" aria-hidden />
          <p className="font-bengali text-xl font-bold text-gold-300">
            শেষ থাকে, যিনি জিতেন — সে-ই জয়ী
          </p>
        </div>
      </div>
    ),
  },
];

function BlockRow({ blocker, action }: { blocker: string; action: string }) {
  return (
    <div className="flex flex-wrap items-center justify-between gap-2 rounded-xl border border-forest-500/20 bg-deep-800/50 px-4 py-3">
      <div className="flex items-center gap-2">
        <Shield className="size-4 text-forest-300" aria-hidden />
        <span className="text-sm font-semibold text-ivory">{blocker}</span>
      </div>
      <span className="text-xs text-muted">ব্লক করে → {action}</span>
    </div>
  );
}