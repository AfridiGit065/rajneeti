import type { LucideIcon } from "lucide-react";
import type { GameActionId } from "@/types/game";
import Image from "next/image";
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
  EyeOff,
  AlertTriangle,
  CheckCircle,
  XCircle,
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

function CardBack({ label }: { label?: string }) {
  return (
    <div className="flex flex-col items-center gap-1">
      <div
        className={cn(
          "relative flex h-20 w-14 items-center justify-center rounded-xl border-2 border-gold-500/40",
          "bg-gradient-to-b from-deep-750 via-deep-850 to-deep-950 shadow-gold transition-all duration-300",
        )}
      >
        <div className="absolute inset-1 rounded-lg border border-gold-500/20 bg-deep-900/80 flex flex-col items-center justify-center">
          <EyeOff className="size-4 text-gold-400/80 mb-1" />
          <span className="font-cinzel text-xs font-bold text-gold-400">?</span>
        </div>
      </div>
      {label && <span className="text-[10px] text-muted">{label}</span>}
    </div>
  );
}

function MiniCharacterCard({ id }: { id: string }) {
  const character = CHARACTERS.find((c) => c.id === id);
  if (!character) return null;
  return (
    <div className="group relative flex flex-col items-center gap-1 rounded-xl border border-gold-500/25 bg-deep-800/80 p-2 text-center transition-all hover:border-gold-400/60 hover:shadow-gold">
      <div className="relative size-12 overflow-hidden rounded-lg border border-gold-500/30 bg-deep-950">
        <Image
          src={character.imagePath}
          alt={character.nameBn}
          fill
          className="object-cover object-top"
          sizes="48px"
        />
      </div>
      <span className="font-bengali text-xs font-bold text-ivory">{character.nameBn}</span>
      <span className="font-cinzel text-[9px] uppercase tracking-wider text-muted">
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
        gain ? "border-forest-500/40 text-forest-300 bg-forest-900/20" : "border-crimson-500/40 text-crimson-300 bg-crimson-900/20",
      )}
    >
      <Coins className="size-4" aria-hidden />
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
    <div className="flex flex-col gap-2.5 rounded-xl border border-forest-500/25 bg-deep-800/50 p-3.5 transition-all hover:border-gold-500/40">
      <div className="flex items-center justify-between gap-2">
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
            <p className="truncate text-sm font-semibold text-ivory font-bengali">{action.nameBn}</p>
            <p className="truncate text-[11px] uppercase tracking-wider text-muted font-cinzel">
              {action.nameEn}
            </p>
          </div>
        </div>
        {character && (
          <span className="rounded-md border border-gold-500/30 bg-gold-500/10 px-2 py-0.5 text-[10px] font-bengali text-gold-300">
            {character.nameBn}
          </span>
        )}
      </div>
      <p className="text-xs text-parchment-300/90 font-bengali leading-relaxed">
        {action.description}
      </p>
      <div className="flex flex-wrap items-center gap-1.5 pt-1">
        {typeof action.gain === "number" && action.gain > 0 ? (
          <CoinDelta amount={action.gain} label="কয়েন আয়" />
        ) : null}
        {action.cost ? <CoinDelta amount={-action.cost} label="কয়েন খরচ" /> : null}
        {action.id === "exchange" ? (
          <span className="rounded-lg border border-parchment-500/30 bg-parchment-500/10 px-2.5 py-1 text-xs font-bold text-parchment-300 font-bengali">
            +২ কার্ড বদল
          </span>
        ) : null}
        {action.challengeable ? (
          <span className="rounded-lg bg-deep-700/70 border border-white/5 px-2 py-1 text-[10px] font-semibold text-muted font-bengali">
            চ্যালেঞ্জযোগ্য
          </span>
        ) : (
          <span className="rounded-lg bg-forest-900/40 border border-forest-500/20 px-2 py-1 text-[10px] font-semibold text-forest-300 font-bengali">
            চ্যালেঞ্জহীন
          </span>
        )}
        {action.blockable ? (
          <span className="rounded-lg bg-deep-700/70 border border-white/5 px-2 py-1 text-[10px] font-semibold text-muted font-bengali">
            ব্লকযোগ্য
          </span>
        ) : (
          <span className="rounded-lg bg-crimson-900/40 border border-crimson-500/20 px-2 py-1 text-[10px] font-semibold text-crimson-300 font-bengali">
            ব্লকহীন
          </span>
        )}
      </div>
    </div>
  );
}

export const TUTORIAL_STEPS: readonly TutorialStep[] = [
  {
    key: "objective",
    indexBn: "০১",
    labelBn: "খেলার মূল লক্ষ্য",
    labelEn: "Objective",
    icon: BookOpen,
    summary: "সবাইকে রাজনৈতিকভাবে পরাস্ত করে শেষ ব্যক্তি হিসেবে টিকে থাকাই একমাত্র লক্ষ্য।",
    body: () => (
      <ul className="space-y-3">
        <Bullet icon={<Gamepad2 className="size-3.5" aria-hidden />}>
          ২ থেকে {RULES.maxPlayers} জন খেলোয়াড় অংশ নেয়। প্রত্যেকে{" "}
          {RULES.maxInfluencePerPlayer}টি লুকানো ইনফ্লুয়েন্স কার্ড (প্রভাব কার্ড) ও{" "}
          {RULES.startingCoins}টি কয়েন নিয়ে শুরু করে।
        </Bullet>
        <Bullet icon={<Skull className="size-3.5" aria-hidden />}>
          প্রতিটি পালায় ভুল চালে বা আক্রমণে ইনফ্লুয়েন্স কমতে পারে — দুটি প্রভাব হারালে আপনি খেলা থেকে সম্পূর্ণ ছিটকে পড়বেন।{" "}
          <span className="text-muted">(Lose both influences, lose the game.)</span>
        </Bullet>
        <Bullet icon={<Trophy className="size-3.5" aria-hidden />}>
          টেবিলে শেষ কার্ড ধরে থাকা খেলোয়াড়ই চূড়ান্ত বিজয়ী নির্বাচিত হবেন।{" "}
          <span className="text-muted">(The last surviving player with influence wins.)</span>
        </Bullet>
      </ul>
    ),
  },
  {
    key: "setup",
    indexBn: "০২",
    labelBn: "খেলার প্রস্তুতি",
    labelEn: "Setup",
    icon: Dices,
    summary: "১৫টি মোট কার্ড · প্রত্যেকে ২টি গোপন কার্ড + ২টি প্রাথমিক কয়েন।",
    body: () => (
      <div className="space-y-4">
        <ul className="space-y-3">
        <Bullet>
            কোর্ট ডেকে মোট {RULES.deckSize}টি কার্ড থাকে — ৫টি স্বতন্ত্র চরিত্র, প্রতিটির{" "}
            ৩টি করে কপি।{" "}
            <span className="text-muted">(5 unique characters, 3 copies each = 15 cards deck.)</span>
          </Bullet>
          <Bullet>
            প্রতিটি খেলোয়াড়কে ২টি কার্ড উল্টো করে (মুখ নিচে) দেওয়া হয় যা অন্যরা দেখতে পায় না, এবং কোষাগারে{" "}
            {RULES.startingCoins}টি প্রাথমিক কয়েন বরাদ্দ থাকে।
          </Bullet>
        </ul>
        <div className="flex flex-wrap items-center justify-between gap-4 rounded-xl border border-forest-500/20 bg-deep-800/40 p-4">
          <div className="flex items-center gap-3">
            <div className="flex items-start gap-2">
              <CardBack label="কার্ড ১" />
              <CardBack label="কার্ড ২" />
            </div>
            <div className="text-xs text-parchment-300 font-bengali">
              <p className="font-bold text-ivory">২টি গোপন ইনফ্লুয়েন্স</p>
              <p className="text-[11px] text-muted">শুধু আপনিই দেখতে পাবেন</p>
            </div>
          </div>
          <div className="flex items-center gap-2 rounded-xl border border-gold-500/30 bg-gold-500/10 px-4 py-3">
            <Coins className="size-6 text-gold-400" aria-hidden />
            <div>
              <p className="text-lg font-bold text-gold-300 font-cinzel">+{RULES.startingCoins} Coins</p>
              <p className="text-[10px] text-muted font-bengali">শুরুর কোষাগার</p>
            </div>
          </div>
        </div>
        <div className="grid grid-cols-2 gap-2 sm:grid-cols-4">
          <StatChip label="খেলোয়াড় সংখ্যা" value={`${RULES.minPlayers}-${RULES.maxPlayers} জন`} />
          <StatChip label="শুরুতে ইনফ্লুয়েন্স" value={`${RULES.maxInfluencePerPlayer}টি`} />
          <StatChip label="শুরুতে কয়েন" value={`${RULES.startingCoins}টি`} />
          <StatChip label="মোট কার্ড ডেক" value={`${RULES.deckSize}টি`} />
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
    summary: "লুকানো কার্ডই আপনার রাজনৈতিক ক্ষমতা ও জীবন।",
    body: () => (
      <div className="space-y-4">
        <ul className="space-y-3">
          <Bullet icon={<EyeOff className="size-3.5" aria-hidden />}>
            কার্ড হলো আপনার প্রভাব (Influence)। কার্ডগুলো <strong className="text-ivory">অন্য সব খেলোয়াড় থেকে সম্পূর্ণ গোপন</strong> থাকে।
          </Bullet>
          <Bullet>
            আপনার হাতে যে চরিত্রগুলো আছে, তাদের বিশেষ ক্ষমতা আপনি সম্পূর্ণ নিরাপদে ব্যবহার করতে পারবেন।
          </Bullet>
          <Bullet>
            কোনো কারণে ইনফ্লুয়েন্স হারালে যেকোনো একটি কার্ড প্রকাশ্যে টেবিলে উন্মোচিত করতে হবে। উন্মোচিত কার্ডের ক্ষমতা নিষ্ক্রিয় হয়ে যায়।
          </Bullet>
        </ul>
        <div>
          <p className="text-xs font-semibold uppercase tracking-wider text-gold-400 font-cinzel mb-2">
            রাজনীতির ৫টি শক্তিশালী চরিত্র · 5 Characters
          </p>
          <div className="grid grid-cols-2 gap-2 sm:grid-cols-5">
            {CHARACTERS.map((c) => (
              <MiniCharacterCard key={c.id} id={c.id} />
            ))}
          </div>
        </div>
      </div>
    ),
  },
  {
    key: "coins",
    indexBn: "০৪",
    labelBn: "কোষাগার ও কয়েন",
    labelEn: "Coins",
    icon: Coins,
    summary: "কয়েন অর্জন করুন এবং ক্ষমতা দখল (Coup) ও হত্যায় খরচ করুন।",
    body: () => (
      <div className="space-y-4">
        <ul className="space-y-3">
          <Bullet>
            কয়েন হলো রাজনীতির অর্থনৈতিক চালিকাশক্তি। নির্দিষ্ট কয়েন জমিয়ে আপনি মারাত্মক আক্রমণ চালাতে পারেন।
          </Bullet>
          <Bullet icon={<AlertTriangle className="size-3.5 text-gold-400" aria-hidden />}>
            <strong className="text-gold-300">{RULES.forcedCoupThreshold}+ কয়েন নিয়ম:</strong> যদি কোনো পালায় আপনার কোষাগারে ১০ বা ততোধিক কয়েন জমা হয়, তবে আপনাকে অবশ্যই বাধ্যতামূলকভাবে ‘ক্ষমতা দখল’ (Coup) করতে হবে!
          </Bullet>
        </ul>
        <div className="grid grid-cols-1 gap-2.5 sm:grid-cols-3">
          <div className="rounded-xl border border-forest-500/30 bg-forest-900/20 p-4 text-center">
            <div className="flex items-center justify-center gap-1.5 text-forest-300">
              <Coins className="size-5" />
              <span className="text-2xl font-bold font-cinzel">+{RULES.incomeGain} / +{RULES.foreignAidGain} / +{RULES.taxGain}</span>
            </div>
            <p className="mt-1.5 text-xs font-bold text-ivory font-bengali">কয়েন উপার্জন</p>
            <p className="text-[11px] text-muted font-bengali">আয় (+১), অনুদান (+২), কর (+৩)</p>
          </div>
          <div className="rounded-xl border border-crimson-500/30 bg-crimson-900/20 p-4 text-center">
            <div className="flex items-center justify-center gap-1.5 text-crimson-300">
              <Skull className="size-5" />
              <span className="text-2xl font-bold font-cinzel">−{RULES.assassinateCost}</span>
            </div>
            <p className="mt-1.5 text-xs font-bold text-ivory font-bengali">সরিয়ে দেওয়া (Assassinate)</p>
            <p className="text-[11px] text-muted font-bengali">ঘাতকের ক্ষমতা প্রয়োগে খরচ</p>
          </div>
          <div className="rounded-xl border border-gold-500/40 bg-gold-900/20 p-4 text-center shadow-gold">
            <div className="flex items-center justify-center gap-1.5 text-gold-400">
              <Crown className="size-5" />
              <span className="text-2xl font-bold font-cinzel">−{RULES.coupCost}</span>
            </div>
            <p className="mt-1.5 text-xs font-bold text-gold-300 font-bengali">ক্ষমতা দখল (Coup)</p>
            <p className="text-[11px] text-muted font-bengali">অপ্রতিরোধ্য ও নিশ্চিত আঘাত</p>
          </div>
        </div>
      </div>
    ),
  },
  {
    key: "actions",
    indexBn: "০৫",
    labelBn: "অ্যাকশনসমূহ",
    labelEn: "Actions",
    icon: ListChecks,
    summary: "প্রতি পালায় একটি অ্যাকশন সম্পাদন করুন — ভিজ্যুয়াল নির্দেশিকা।",
    body: () => (
      <div className="space-y-4">
        <p className="text-sm leading-relaxed text-parchment-300 font-bengali">
          নিজের পালায় খেলোয়াড় যেকোনো একটি বৈধ অ্যাকশন বেছে নেন। নিচে সবগুলোর অর্জন ও খরচের তালিকা দেওয়া হলো:
        </p>
        <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
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
    labelBn: "ব্লাফিং ও মনস্তত্ত্ব",
    labelEn: "Bluffing",
    icon: Ghost,
    summary: "হাতে কার্ড না থাকলেও চরিত্রের ক্ষমতা দাবি করতে পারেন — এটাই রাজনীতির মূল খেলা!",
    body: () => (
      <div className="space-y-4">
        <ul className="space-y-3">
          <Bullet icon={<Ghost className="size-3.5 text-gold-400" aria-hidden />}>
            আপনার হাতে যে চরিত্র নেই, আপনি মুখে সে চরিত্রের নাম দাবি (Claim) করে তার অ্যাকশন বা প্রতিরোধ চালাতে পারেন।
          </Bullet>
          <Bullet>
            যতক্ষণ না অন্য কেউ আপনাকে মিথ্যাবাদী বলে সরাসরি <strong className="text-ivory">চ্যালেঞ্জ</strong> করছে, ততক্ষণ আপনার দাবি শতভাগ সত্য হিসেবে গৃহীত হবে!
          </Bullet>
          <Bullet icon={<AlertTriangle className="size-3.5 text-crimson-400" aria-hidden />}>
            সতর্কতা: মিথ্যা ব্লাফ ধরা পড়লে সাথে সাথে একটি কার্ড উন্মোচন করে ইনফ্লুয়েন্স হারাতে হবে।
          </Bullet>
        </ul>
        <div className="flex flex-col sm:flex-row items-center gap-4 rounded-xl border border-forest-500/20 bg-deep-800/40 p-4">
          <div className="flex items-center gap-2">
            <CardBack label="হাতের গোপন কার্ড" />
            <span className="text-xs text-muted font-bengali">(আসল কার্ড: আমলা)</span>
          </div>
          <span className="text-gold-500 text-lg hidden sm:inline">➔</span>
          <div className="rounded-xl border border-gold-500/30 bg-deep-950/80 p-3.5 flex-1">
            <p className="font-bengali text-sm font-bold text-gold-300">
              মুখের দাবি: “আমি মন্ত্রী! কোষাগার থেকে ৩ কয়েন কর আদায় করছি।”
            </p>
            <p className="text-[11px] text-muted mt-1 font-bengali">
              কেউ চ্যালেঞ্জ না করলে আপনি সফলভাবে ৩ কয়েন নিজের করে নেবেন!
            </p>
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
    summary: "কারো দাবিকে মিথ্যা মনে হলে চ্যালেঞ্জ করুন — ফলাফল কিন্তু মারাত্মক!",
    body: () => (
      <div className="space-y-4">
        <ul className="space-y-3">
          <Bullet icon={<Gavel className="size-3.5 text-gold-400" aria-hidden />}>
            যখন কোনো খেলোয়াড় কোনো চরিত্রের ক্ষমতা দাবি করে (যেমন: কর, চুরি, বদল, হত্যা বা ব্লক), অন্য যেকোনো খেলোয়াড় তাকে <strong className="text-ivory">“চ্যালেঞ্জ”</strong> জানাতে পারে।
          </Bullet>
          <Bullet>
            চ্যালেঞ্জ হলে দাবিকারীকে তার দাবি করা কার্ডটি দেখাতে হবে।
          </Bullet>
        </ul>
        <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
          <div className="rounded-xl border border-crimson-500/30 bg-crimson-900/15 p-4 space-y-2">
            <div className="flex items-center gap-2 text-crimson-400 font-bold font-bengali text-sm">
              <XCircle className="size-4" />
              যদি দাবিকারী সত্য প্রমাণ করে:
            </div>
            <p className="text-xs text-parchment-300 font-bengali leading-relaxed">
              দাবিকারী কার্ডটি ডেকে ফেরত দিয়ে নতুন একটি কার্ড টেনে নেবে। আর <strong className="text-ivory">চ্যালেঞ্জকারী ব্যর্থ হয়ে ১টি ইনফ্লুয়েন্স হারাবে!</strong> অ্যাকশন যথারীতি কার্যকর হবে।
            </p>
          </div>
          <div className="rounded-xl border border-forest-500/30 bg-forest-900/15 p-4 space-y-2">
            <div className="flex items-center gap-2 text-forest-300 font-bold font-bengali text-sm">
              <CheckCircle className="size-4" />
              যদি দাবিকারী ব্লাফ করে থাকে:
            </div>
            <p className="text-xs text-parchment-300 font-bengali leading-relaxed">
              দাবিকারীর মিথ্যা ফাঁস হয়ে যাবে এবং <strong className="text-ivory">দাবিকারী তৎক্ষণাৎ ১টি ইনফ্লুয়েন্স হারাবে!</strong> তার ঘোষণা করা অ্যাকশনটি বাতিল হয়ে যাবে।
            </p>
          </div>
        </div>
      </div>
    ),
  },
  {
    key: "block",
    indexBn: "০৮",
    labelBn: "ব্লক ও প্রতিরোধ",
    labelEn: "Block",
    icon: Shield,
    summary: "নির্দিষ্ট চরিত্রের ক্ষমতায় প্রতিপক্ষের আক্রমণ আটকে দিন।",
    body: () => (
      <div className="space-y-4">
        <ul className="space-y-3">
          <Bullet icon={<Shield className="size-3.5 text-forest-300" aria-hidden />}>
            নির্দিষ্ট কিছু অ্যাকশনকে নির্দিষ্ট চরিত্র দাবি করে <strong className="text-ivory">ব্লক (প্রতিরোধ)</strong> করা সম্ভব।
          </Bullet>
          <Bullet>
            ব্লক করাও একটি চরিত্র দাবি — তাই ব্লকের বিরুদ্ধেও অন্য খেলোয়াড়রা চ্যালেঞ্জ জানাতে পারে!
          </Bullet>
          <Bullet>
            ‘আয়’ (Income) এবং ‘ক্ষমতা দখল’ (Coup) কোনো চরিত্র দিয়েই ব্লক করা যায় না।
          </Bullet>
        </ul>
        <div className="space-y-2 pt-1">
          <BlockRow blocker="মন্ত্রী · The Minister" action="বিদেশি অনুদান (+২) ব্লক করে" />
          <BlockRow blocker="আমলা / দালাল · Bureaucrat / Broker" action="দালালের চুরি (+২) ব্লক করে" />
          <BlockRow blocker="গোয়েন্দা · The Detective" action="ঘাতকের হত্যা (-৩) ব্লক করে" />
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
    summary: "উভয় প্রভাব কার্ড হারালে খেলোয়াড় সম্পূর্ণভাবে অপসারিত হবেন।",
    body: () => (
      <div className="space-y-4">
        <ul className="space-y-3">
          <Bullet icon={<Skull className="size-3.5 text-crimson-400" aria-hidden />}>
            নিম্নলিখিত কারণে একজন খেলোয়াড় ইনফ্লুয়েন্স হারান:
            <ul className="list-disc pl-5 mt-1 space-y-1 text-xs text-muted">
              <li>ভুল চ্যালেঞ্জ জানালে অথবা নিজের ব্লাফ ধরা পড়লে।</li>
              <li>ঘাতকের সফল হত্যাকাণ্ডের শিকারে পরিণত হলে।</li>
              <li>কারো ক্ষমতা দখল (Coup) আক্রমণের শিকার হলে।</li>
            </ul>
          </Bullet>
          <Bullet>
            ইনফ্লুয়েন্স হারালে খেলোয়াড় নিজের পছন্দমতো একটি কার্ড সবার সামনে টেবিলে উন্মোচিত রাখেন।
          </Bullet>
          <Bullet>
            যার ২টি কার্ডই উন্মোচিত হয়ে যায়, তিনি খেলা থেকে সম্পূর্ণ এলিমিনেট হন এবং তার অবশিষ্ট সব কয়েন কোষাগারে বাজেয়াপ্ত হয়।
          </Bullet>
        </ul>
        <div className="flex flex-wrap items-center justify-center gap-3 rounded-xl border border-crimson-500/30 bg-crimson-950/20 p-4">
          <div className="flex items-center gap-2">
            <CardBack label="১ম ইনফ্লুয়েন্স" />
            <CardBack label="২য় ইনফ্লুয়েন্স" />
          </div>
          <span className="text-crimson-400 text-lg">➔</span>
          <div className="flex items-center gap-2">
            <CardBack label="১ম হারাল" />
            <div className="flex size-16 items-center justify-center rounded-xl border border-crimson-500/50 bg-crimson-900/30 text-crimson-400 font-bold text-sm">
              উন্মোচিত
            </div>
          </div>
          <span className="text-crimson-400 text-lg">➔</span>
          <div className="flex items-center gap-2">
            <div className="flex h-20 w-24 flex-col items-center justify-center rounded-xl border-2 border-crimson-500 bg-crimson-950 text-crimson-300">
              <Skull className="size-6 mb-1" />
              <span className="font-bengali text-xs font-bold">খেলা শেষ</span>
            </div>
          </div>
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
    summary: "৭ কয়েন খরচে লক্ষ্য খেলোয়াড়ের বিরুদ্ধে অপ্রতিরোধ্য অভ্যুত্থান।",
    body: () => (
      <div className="space-y-4">
        <ul className="space-y-3">
          <Bullet icon={<Crown className="size-3.5 text-gold-400" aria-hidden />}>
            কোষাগারে {RULES.coupCost} কয়েন জমা হলে যেকোনো খেলোয়াড় ‘ক্ষমতা দখল’ ঘোষণা করতে পারেন।
          </Bullet>
          <Bullet>
            টার্গেট খেলোয়াড় তৎক্ষণাৎ তার যেকোনো একটি কার্ড বেছে উন্মোচিত করতে বাধ্য হন।
          </Bullet>
          <Bullet icon={<CheckCircle className="size-3.5 text-forest-300" aria-hidden />}>
            <strong className="text-ivory">সম্পূর্ণ অপ্রতিরোধ্য:</strong> কুপের জন্য কোনো চরিত্র দাবির প্রয়োজন নেই। ফলে কুপ কোনো অবস্থাতেই <strong className="text-gold-300">চ্যালেঞ্জ করা যায় না এবং কোনো চরিত্র দিয়েই ব্লক করা যায় না</strong>।
          </Bullet>
          <Bullet icon={<AlertTriangle className="size-3.5 text-crimson-400" aria-hidden />}>
            যদি আপনার কাছে ১০ বা তার বেশি কয়েন থাকে, তবে আপনার পালায় কুপ করা আইনত বাধ্যতামূলক।
          </Bullet>
        </ul>
        <div className="flex flex-wrap items-center justify-between gap-4 rounded-xl border border-gold-500/30 bg-deep-900/60 p-4 shadow-gold">
          <div className="flex items-center gap-3">
            <span className="flex size-12 items-center justify-center rounded-xl border border-gold-500/40 bg-gold-500/15 text-gold-300">
              <Crown className="size-7" />
            </span>
            <div>
              <p className="font-bengali text-base font-bold text-ivory">ক্ষমতা দখল (Coup)</p>
              <p className="text-xs text-muted font-bengali">সরাসরি ১টি ইনফ্লুয়েন্স ধ্বংস</p>
            </div>
          </div>
          <div className="flex items-center gap-2">
            <CoinDelta amount={-RULES.coupCost} label="কুপ খরচ" />
            <span className="rounded-lg border border-forest-500/30 bg-forest-900/30 px-2.5 py-1 text-xs font-bold text-forest-300 font-bengali">
              ১০০% নিশ্চিত আঘাত
            </span>
          </div>
        </div>
      </div>
    ),
  },
  {
    key: "winning",
    indexBn: "১১",
    labelBn: "বিজয় অর্জন",
    labelEn: "Winning",
    icon: Trophy,
    summary: "সবাইকে পরাস্ত করে শেষ দাঁড়িয়ে থাকা খেলোয়াড়ই হবেন রাজনীতির সম্রাট।",
    body: () => (
      <div className="space-y-4">
        <ul className="space-y-3">
          <Bullet icon={<Trophy className="size-3.5 text-gold-400" aria-hidden />}>
            টেবিলের অন্য সব খেলোয়াড়দের উভয় ইনফ্লুয়েন্স কার্ড উন্মোচিত হয়ে গেলে, যিনি একমাত্র কার্ড ধরে বেঁচে থাকবেন তিনিই <strong className="text-gold-300 font-bengali">চূড়ান্ত বিজয়ী</strong> ঘোষিত হবেন!
          </Bullet>
          <Bullet>
            বিজয়ের চাবিকাঠি: নিজের কার্ড আড়ালে রাখুন, প্রতিপক্ষের ব্লাফ অনুধাবন করুন, হিসেব করে চ্যালেঞ্জ নিন এবং ৭ কয়েন জমিয়ে আঘাত হানুন।
          </Bullet>
        </ul>
        <div className="flex flex-col items-center justify-center gap-3 rounded-2xl border-2 border-gold-500/50 bg-gradient-to-b from-gold-500/20 via-deep-900 to-deep-950 p-6 text-center shadow-gold">
          <div className="flex size-16 items-center justify-center rounded-2xl border border-gold-400/50 bg-gold-400/20 text-gold-300 shadow-gold">
            <Trophy className="size-9" aria-hidden />
          </div>
          <div>
            <h3 className="font-bengali text-2xl font-bold text-gold-gradient">
              ক্ষমতার খেলায় সত্য নয়, বুদ্ধিই শেষ কথা
            </h3>
            <p className="font-cinzel text-xs uppercase tracking-widest text-muted mt-1">
              Rajneeti — The Game of Power
            </p>
          </div>
        </div>
      </div>
    ),
  },
];

function BlockRow({ blocker, action }: { blocker: string; action: string }) {
  return (
    <div className="flex flex-wrap items-center justify-between gap-2 rounded-xl border border-forest-500/20 bg-deep-800/50 px-4 py-3">
      <div className="flex items-center gap-2">
        <Shield className="size-4 text-forest-300 shrink-0" aria-hidden />
        <span className="text-sm font-semibold text-ivory font-bengali">{blocker}</span>
      </div>
      <span className="text-xs text-muted font-bengali">প্রতিরোধ: <span className="text-parchment-200 font-medium">{action}</span></span>
    </div>
  );
}
