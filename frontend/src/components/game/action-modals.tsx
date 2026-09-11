"use client";

import Image from "next/image";
import { cn } from "@/lib/cn";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  Coins,
  Globe,
  ScrollText,
  Hand,
  Skull,
  Crown,
  AlertTriangle,
  Users,
  X,
} from "@/components/ui/icons";
import type { GameActionId, GamePlayer } from "@/types/game";

export type ActionModalStep =
  | { type: "none" }
  | { type: "income_success" }
  | { type: "foreign_aid_block_window" }
  | { type: "claim_minister" }
  | { type: "select_target_steal" }
  | { type: "claim_dalal"; targetPlayer: GamePlayer }
  | { type: "exchange_ui" }
  | { type: "select_target_assassinate" }
  | { type: "confirm_assassinate"; targetPlayer: GamePlayer }
  | { type: "select_target_coup" }
  | { type: "confirm_coup"; targetPlayer: GamePlayer };

interface ActionModalsProps {
  modalStep: ActionModalStep;
  aliveOpponents: GamePlayer[];
  onClose: () => void;
  setModalStep: (step: ActionModalStep) => void;
  onConfirm: (action: GameActionId, targetPlayerId?: string) => void;
}

export function ActionModals({
  modalStep,
  aliveOpponents,
  onClose,
  setModalStep,
  onConfirm,
}: ActionModalsProps) {
  if (modalStep.type === "none") return null;

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm animate-fade-in"
      role="dialog"
      aria-modal="true"
    >
      <div className="relative w-full max-w-lg rounded-3xl border-2 border-gold-500/40 bg-surface panel-emboss panel-texture p-6 sm:p-7 shadow-2xl space-y-5">
        <button
          type="button"
          onClick={onClose}
          className="absolute right-4 top-4 flex size-8 items-center justify-center rounded-full border border-white/10 bg-deep-950/80 text-muted hover:text-ivory transition-all cursor-pointer"
        >
          <X className="size-4" />
        </button>

        {/* 1. Income Success State */}
        {modalStep.type === "income_success" && (
          <div className="space-y-4 text-center">
            <div className="mx-auto flex size-14 items-center justify-center rounded-2xl border border-forest-500/40 bg-forest-500/15 text-forest-300 shadow-lg">
              <Coins className="size-8" />
            </div>
            <div>
              <h3 className="font-bengali text-2xl font-bold text-ivory">
                আয় (Income) সম্পন্ন হচ্ছে
              </h3>
              <p className="font-cinzel text-xs text-muted tracking-widest mt-0.5">
                Action: Income (+1 Coin)
              </p>
            </div>
            <div className="rounded-xl border border-forest-500/25 bg-deep-900/70 p-4 text-xs font-bengali text-parchment-200 leading-relaxed">
              &apos;আয়&apos; একটি মৌলিক অ্যাকশন। এটি কোনো চরিত্র দাবি করে না, তাই এটি চ্যালেঞ্জ বা ব্লক করা যায় না। আপনার কোষাগারে সরাসরি ১টি স্বর্ণমুদ্রা যোগ হবে।
            </div>
            <div className="flex gap-2">
              <Button variant="ghost" fullWidth onClick={onClose}>
                বাতিল
              </Button>
              <Button
                variant="premium"
                fullWidth
                onClick={() => {
                  onConfirm("income");
                  onClose();
                }}
              >
                নিশ্চিত করুন (+১ কয়েন)
              </Button>
            </div>
          </div>
        )}

        {/* 2. Foreign Aid Block State */}
        {modalStep.type === "foreign_aid_block_window" && (
          <div className="space-y-4">
            <div className="flex items-center gap-3">
              <span className="flex size-12 shrink-0 items-center justify-center rounded-2xl border border-gold-500/40 bg-gold-500/15 text-gold-300">
                <Globe className="size-6" />
              </span>
              <div>
                <h3 className="font-bengali text-xl font-bold text-ivory">
                  বিদেশি অনুদান (Foreign Aid)
                </h3>
                <p className="font-cinzel text-xs text-muted">
                  +2 Coins · Blockable by Minister
                </p>
              </div>
            </div>

            <div className="rounded-xl border border-gold-500/30 bg-deep-950/70 p-4 space-y-2">
              <div className="flex items-center justify-between">
                <span className="font-bengali text-xs font-bold text-gold-300">
                  সম্ভাব্য ব্লক পর্যায় (Block Window):
                </span>
                <Badge tone="crimson">মন্ত্রী দ্বারা ব্লকযোগ্য</Badge>
              </div>
              <p className="text-xs text-parchment-300 font-bengali leading-relaxed">
                আপনি ব্যাংক থেকে ২টি কয়েন দাবি করছেন। অন্য কোনো খেলোয়াড় যদি নিজের হাতে বা ব্লাফ করে <strong className="text-ivory">“মন্ত্রী”</strong> দাবি করে, তবে তারা আপনার এই অনুদানটি ব্লক করতে পারবে।
              </p>
            </div>

            <div className="flex gap-2">
              <Button variant="ghost" fullWidth onClick={onClose}>
                বাতিল
              </Button>
              <Button
                variant="premium"
                fullWidth
                onClick={() => {
                  onConfirm("foreign_aid");
                  onClose();
                }}
              >
                অনুদান ঘোষণা করুন (+২)
              </Button>
            </div>
          </div>
        )}

        {/* 3. Tax Claim Minister State */}
        {modalStep.type === "claim_minister" && (
          <div className="space-y-4">
            <div className="flex items-center gap-3">
              <span className="flex size-12 shrink-0 items-center justify-center rounded-2xl border border-gold-500/40 bg-gold-500/15 text-gold-300">
                <ScrollText className="size-6" />
              </span>
              <div>
                <h3 className="font-bengali text-xl font-bold text-ivory">
                  কর আদায় (Tax)
                </h3>
                <p className="font-cinzel text-xs text-muted">
                  +3 Coins · Requires Minister Claim
                </p>
              </div>
            </div>

            <div className="rounded-xl border border-gold-500/40 bg-gradient-to-b from-deep-900 to-deep-950 p-4 space-y-3">
              <div className="flex items-center gap-3">
                <div className="relative size-12 rounded-lg overflow-hidden border border-gold-400 shrink-0">
                  <Image
                    src="/assets/cards/minister.png"
                    alt="Minister"
                    fill
                    className="object-cover object-top"
                  />
                </div>
                <div>
                  <span className="font-bengali text-xs font-bold text-gold-300">
                    চরিত্র দাবি: মন্ত্রী (The Minister)
                  </span>
                  <p className="text-[11px] text-muted font-bengali">
                    কর আদায়ের জন্য আপনাকে প্রকাশ্যে &apos;মন্ত্রী&apos; দাবি করতে হবে।
                  </p>
                </div>
              </div>

              <div className="rounded-lg border border-white/5 bg-deep-950 p-2.5 text-xs text-parchment-300 font-bengali leading-relaxed">
                ⚠️ এটি <strong className="text-gold-300">চ্যালেঞ্জযোগ্য</strong>। আপনার হাতে মন্ত্রী না থাকলেও ব্লাফ করতে পারেন, তবে কেউ চ্যালেঞ্জ করে আপনার মিথ্যা প্রমাণ করলে ১টি ইনফ্লুয়েন্স হারাবেন।
              </div>
            </div>

            <div className="flex gap-2">
              <Button variant="ghost" fullWidth onClick={onClose}>
                বাতিল
              </Button>
              <Button
                variant="premium"
                fullWidth
                onClick={() => {
                  onConfirm("tax");
                  onClose();
                }}
              >
                মন্ত্রী দাবি করে কর নাও (+৩)
              </Button>
            </div>
          </div>
        )}

        {/* 4. Steal - Select Target */}
        {modalStep.type === "select_target_steal" && (
          <div className="space-y-4">
            <div className="flex items-center gap-3">
              <span className="flex size-12 shrink-0 items-center justify-center rounded-2xl border border-forest-500/40 bg-forest-500/15 text-forest-300">
                <Hand className="size-6" />
              </span>
              <div>
                <h3 className="font-bengali text-xl font-bold text-ivory">
                  চুরি (Steal) · লক্ষ্য নির্বাচন
                </h3>
                <p className="font-cinzel text-xs text-muted">
                  Select Target Player (+2 Coins)
                </p>
              </div>
            </div>

            <p className="text-xs text-parchment-300 font-bengali">
              কাকে টার্গেট করে ২ কয়েন চুরি করতে চান? নিচে থেকে একজন জীবিত প্রতিপক্ষ বেছে নিন:
            </p>

            <div className="space-y-2 max-h-56 overflow-y-auto pr-1">
              {aliveOpponents.map((opp) => (
                <button
                  key={opp.id}
                  type="button"
                  disabled={opp.coins === 0}
                  onClick={() => setModalStep({ type: "claim_dalal", targetPlayer: opp })}
                  className={cn(
                    "flex items-center justify-between w-full p-3 rounded-xl border text-left transition-all cursor-pointer",
                    opp.coins === 0
                      ? "border-white/5 bg-deep-950/40 opacity-50 cursor-not-allowed"
                      : "border-forest-500/25 bg-deep-900 hover:border-gold-400 hover:bg-deep-850",
                  )}
                >
                  <div className="flex items-center gap-2.5">
                    <Users className="size-4 text-gold-400" />
                    <div>
                      <p className="font-bengali text-sm font-bold text-ivory">
                        {opp.displayName ?? opp.username}
                      </p>
                      <p className="text-[10px] text-muted">
                        ইনফ্লুয়েন্স: {opp.influenceCards.filter((c) => !c.revealed).length}টি
                      </p>
                    </div>
                  </div>
                  <div className="text-right">
                    <span className="font-cinzel text-sm font-bold text-gold-300">
                      {opp.coins} Coins
                    </span>
                    {opp.coins === 0 && (
                      <span className="block text-[10px] text-crimson-400 font-bengali">
                        কয়েন নেই
                      </span>
                    )}
                  </div>
                </button>
              ))}
            </div>

            <Button variant="ghost" fullWidth onClick={onClose}>
              বাতিল
            </Button>
          </div>
        )}

        {/* 4b. Steal - Claim Dalal */}
        {modalStep.type === "claim_dalal" && (
          <div className="space-y-4">
            <div className="flex items-center gap-3">
              <div className="relative size-12 rounded-lg overflow-hidden border border-forest-400 shrink-0">
                <Image
                  src="/assets/cards/dalal.png"
                  alt="Dalal"
                  fill
                  className="object-cover object-top"
                />
              </div>
              <div>
                <h3 className="font-bengali text-xl font-bold text-ivory">
                  দালাল দাবি (Claim Dalal)
                </h3>
                <p className="font-cinzel text-xs text-muted">
                  Target: {modalStep.targetPlayer.displayName ?? modalStep.targetPlayer.username}
                </p>
              </div>
            </div>

            <div className="rounded-xl border border-forest-500/30 bg-deep-950/80 p-4 space-y-2 text-xs font-bengali text-parchment-200">
              <p>
                আপনি <strong className="text-ivory">{modalStep.targetPlayer.displayName ?? modalStep.targetPlayer.username}</strong>-এর থেকে ২ কয়েন চুরি করার জন্য নিজেকে <strong className="text-gold-300">“দালাল”</strong> হিসেবে দাবি করছেন।
              </p>
              <p className="text-muted text-[11px]">
                🛡️ টার্গেট খেলোয়াড় আমলা বা দালাল দাবি করে এই চুরি ব্লক করার চেষ্টা করতে পারে।
              </p>
            </div>

            <div className="flex gap-2">
              <Button
                variant="ghost"
                fullWidth
                onClick={() => setModalStep({ type: "select_target_steal" })}
              >
                টার্গেট পরিবর্তন
              </Button>
              <Button
                variant="premium"
                fullWidth
                onClick={() => {
                  onConfirm("steal", modalStep.targetPlayer.id);
                  onClose();
                }}
              >
                চুরি নিশ্চিত করুন (+২)
              </Button>
            </div>
          </div>
        )}

        {/* 5. Exchange UI - Intent Confirmation */}
        {modalStep.type === "exchange_ui" && (
          <div className="space-y-4">
            <div className="flex items-center gap-3">
              <div className="relative size-12 rounded-lg overflow-hidden border border-parchment-400 shrink-0">
                <Image
                  src="/assets/cards/amla.png"
                  alt="Amla"
                  fill
                  className="object-cover object-top"
                />
              </div>
              <div>
                <h3 className="font-bengali text-xl font-bold text-ivory">
                  কার্ড বদল (Exchange)
                </h3>
                <p className="font-cinzel text-xs text-muted">
                  Claim Amla · Draw 2 & Keep 2
                </p>
              </div>
            </div>

            <div className="rounded-xl border border-gold-500/40 bg-gradient-to-b from-deep-900 to-deep-950 p-4 space-y-3">
              <div className="flex items-center gap-3">
                <div className="relative size-12 rounded-lg overflow-hidden border border-gold-400 shrink-0">
                  <Image
                    src="/assets/cards/amla.png"
                    alt="Amla"
                    fill
                    className="object-cover object-top"
                  />
                </div>
                <div>
                  <span className="font-bengali text-xs font-bold text-gold-300">
                    চরিত্র দাবি: আমলা (The Amla)
                  </span>
                  <p className="text-[11px] text-muted font-bengali">
                    কার্ড বদলের জন্য আপনাকে প্রকাশ্যে &apos;আমলা&apos; দাবি করতে হবে।
                  </p>
                </div>
              </div>

              <div className="rounded-lg border border-white/5 bg-deep-950 p-2.5 text-xs text-parchment-300 font-bengali leading-relaxed">
                ⚠️ এটি <strong className="text-gold-300">চ্যালেঞ্জযোগ্য</strong>। ডেক থেকে ২টি নতুন কার্ড তুলে ২টি রেখে বাকিগুলো ফেরত দেবেন। চ্যালেঞ্জ সফল হলে ১টি ইনফ্লুয়েন্স হারাবেন।
              </div>
            </div>

            <div className="flex gap-2">
              <Button variant="ghost" fullWidth onClick={onClose}>
                বাতিল
              </Button>
              <Button
                variant="premium"
                fullWidth
                onClick={() => {
                  onConfirm("exchange");
                  onClose();
                }}
              >
                আমলা দাবি করে বদল শুরু করুন
              </Button>
            </div>
          </div>
        )}

        {/* 6. Assassinate - Select Target */}
        {modalStep.type === "select_target_assassinate" && (
          <div className="space-y-4">
            <div className="flex items-center gap-3">
              <span className="flex size-12 shrink-0 items-center justify-center rounded-2xl border border-crimson-500/40 bg-crimson-500/15 text-crimson-300">
                <Skull className="size-6" />
              </span>
              <div>
                <h3 className="font-bengali text-xl font-bold text-ivory">
                  সরিয়ে দেওয়া (Assassinate)
                </h3>
                <p className="font-cinzel text-xs text-muted">
                  Cost: 3 Coins · Target Elimination
                </p>
              </div>
            </div>

            <p className="text-xs text-parchment-300 font-bengali">
              ৩ কয়েন খরচ করে কোন খেলোয়াড়কে আঘাত করতে চান? লক্ষ্য নির্বাচন করুন:
            </p>

            <div className="space-y-2 max-h-56 overflow-y-auto pr-1">
              {aliveOpponents.map((opp) => (
                <button
                  key={opp.id}
                  type="button"
                  onClick={() =>
                    setModalStep({ type: "confirm_assassinate", targetPlayer: opp })
                  }
                  className="flex items-center justify-between w-full p-3 rounded-xl border border-crimson-500/30 bg-deep-900 hover:border-crimson-400 hover:bg-deep-850 text-left transition-all cursor-pointer"
                >
                  <div className="flex items-center gap-2.5">
                    <Skull className="size-4 text-crimson-400" />
                    <div>
                      <p className="font-bengali text-sm font-bold text-ivory">
                        {opp.displayName ?? opp.username}
                      </p>
                      <p className="text-[10px] text-muted">
                        অবশিষ্ট ইনফ্লুয়েন্স: {opp.influenceCards.filter((c) => !c.revealed).length}টি
                      </p>
                    </div>
                  </div>
                  <span className="text-xs font-bengali text-crimson-300 font-bold">
                    টার্গেট করুন ➔
                  </span>
                </button>
              ))}
            </div>

            <Button variant="ghost" fullWidth onClick={onClose}>
              বাতিল
            </Button>
          </div>
        )}

        {/* 6b. Assassinate - Cost Confirmation */}
        {modalStep.type === "confirm_assassinate" && (
          <div className="space-y-4">
            <div className="flex items-center gap-3">
              <div className="relative size-12 rounded-lg overflow-hidden border border-crimson-400 shrink-0">
                <Image
                  src="/assets/cards/ghatok.png"
                  alt="Ghatok"
                  fill
                  className="object-cover object-top"
                />
              </div>
              <div>
                <h3 className="font-bengali text-xl font-bold text-crimson-300">
                  হত্যার খরচ নিশ্চিতকরণ
                </h3>
                <p className="font-cinzel text-xs text-muted">
                  Claim Assassin · Cost: 3 Coins
                </p>
              </div>
            </div>

            <div className="rounded-xl border border-crimson-500/40 bg-crimson-950/40 p-4 space-y-2 text-xs font-bengali text-parchment-200">
              <div className="flex justify-between items-center text-sm font-bold pb-2 border-b border-crimson-500/20">
                <span>টার্গেট: {modalStep.targetPlayer.displayName ?? modalStep.targetPlayer.username}</span>
                <span className="text-crimson-300 font-cinzel">-৩ কয়েন</span>
              </div>
              <p>
                আপনি <strong className="text-ivory">ঘাতক (The Assassin)</strong> দাবি করছেন। সফল হলে টার্গেটের ১টি কার্ড নষ্ট হবে।
              </p>
              <p className="text-muted text-[11px]">
                🛡️ টার্গেট &apos;গোয়েন্দা&apos; দাবি করে এই হত্যাচেষ্টা ব্লক করতে পারে।
              </p>
            </div>

            <div className="flex gap-2">
              <Button
                variant="ghost"
                fullWidth
                onClick={() => setModalStep({ type: "select_target_assassinate" })}
              >
                টার্গেট পরিবর্তন
              </Button>
              <Button
                variant="danger"
                fullWidth
                onClick={() => {
                  onConfirm("assassinate", modalStep.targetPlayer.id);
                  onClose();
                }}
              >
                ৩ কয়েন দিয়ে হত্যা করো
              </Button>
            </div>
          </div>
        )}

        {/* 7. Coup - Select Target */}
        {modalStep.type === "select_target_coup" && (
          <div className="space-y-4">
            <div className="flex items-center gap-3">
              <span className="flex size-12 shrink-0 items-center justify-center rounded-2xl border border-gold-500/50 bg-gold-500/15 text-gold-300 shadow-gold">
                <Crown className="size-6" />
              </span>
              <div>
                <h3 className="font-bengali text-xl font-bold text-gold-gradient">
                  ক্ষমতা দখল (Coup) · লক্ষ্য নির্বাচন
                </h3>
                <p className="font-cinzel text-xs text-muted">
                  Cost: 7 Coins · Unblockable & Unchallengeable
                </p>
              </div>
            </div>

            <p className="text-xs text-parchment-300 font-bengali">
              ৭ কয়েন দিয়ে কার রাজনৈতিক ক্ষমতা ধ্বংস করতে চান? নিচে থেকে টার্গেট বেছে নিন:
            </p>

            <div className="space-y-2 max-h-56 overflow-y-auto pr-1">
              {aliveOpponents.map((opp) => (
                <button
                  key={opp.id}
                  type="button"
                  onClick={() => setModalStep({ type: "confirm_coup", targetPlayer: opp })}
                  className="flex items-center justify-between w-full p-3 rounded-xl border border-gold-500/30 bg-deep-900 hover:border-gold-400 hover:bg-deep-850 text-left transition-all cursor-pointer"
                >
                  <div className="flex items-center gap-2.5">
                    <Crown className="size-4 text-gold-400" />
                    <div>
                      <p className="font-bengali text-sm font-bold text-ivory">
                        {opp.displayName ?? opp.username}
                      </p>
                      <p className="text-[10px] text-muted">
                        ইনফ্লুয়েন্স: {opp.influenceCards.filter((c) => !c.revealed).length}টি
                      </p>
                    </div>
                  </div>
                  <span className="text-xs font-bengali text-gold-300 font-bold">
                    কুপ করুন ➔
                  </span>
                </button>
              ))}
            </div>

            <Button variant="ghost" fullWidth onClick={onClose}>
              বাতিল
            </Button>
          </div>
        )}

        {/* 7b. Coup - Destructive Confirmation */}
        {modalStep.type === "confirm_coup" && (
          <div className="space-y-4">
            <div className="flex items-center gap-3">
              <div className="flex size-12 items-center justify-center rounded-2xl border-2 border-crimson-500/70 bg-crimson-950 text-crimson-400 shrink-0">
                <AlertTriangle className="size-7" />
              </div>
              <div>
                <h3 className="font-bengali text-xl font-bold text-crimson-300">
                  চূড়ান্ত কুপ নিশ্চিতকরণ
                </h3>
                <p className="font-cinzel text-xs text-muted">
                  Destructive Action Confirmation
                </p>
              </div>
            </div>

            <div className="rounded-xl border border-crimson-500/50 bg-crimson-950/60 p-4 space-y-2 text-xs font-bengali text-parchment-200">
              <div className="flex justify-between items-center text-sm font-bold pb-2 border-b border-crimson-500/30">
                <span className="text-ivory">
                  টার্গেট: {modalStep.targetPlayer.displayName ?? modalStep.targetPlayer.username}
                </span>
                <span className="text-crimson-400 font-cinzel">-৭ কয়েন</span>
              </div>
              <p className="text-crimson-200">
                ⚠️ এটি একটি অপ্রতিরোধ্য ও অপরিবর্তনীয় সিদ্ধান্ত!
              </p>
              <p className="text-muted text-[11px] leading-relaxed">
                টার্গেট খেলোয়াড় তৎক্ষণাৎ তার ১টি ইনফ্লুয়েন্স কার্ড উন্মোচিত করে হারাতে বাধ্য হবে। কোনো খেলোয়াড় এই অ্যাকশন ব্লক করতে পারবে না এবং কোনো চ্যালেঞ্জ গ্রহণযোগ্য নয়।
              </p>
            </div>

            <div className="flex gap-2">
              <Button
                variant="ghost"
                fullWidth
                onClick={() => setModalStep({ type: "select_target_coup" })}
              >
                টার্গেট পরিবর্তন
              </Button>
              <Button
                variant="danger"
                fullWidth
                onClick={() => {
                  onConfirm("coup", modalStep.targetPlayer.id);
                  onClose();
                }}
              >
                ৭ কয়েন দিয়ে ক্ষমতা দখল সম্পন্ন করুন!
              </Button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
