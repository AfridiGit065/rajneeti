"use client";

import { useState } from "react";
import Image from "next/image";
import Link from "next/link";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import {
  Trophy,
  Swords,
  Coins,
  Shield,
  ShieldAlert,
  Flame,
  Clock3,
  Users,
  Pencil,
  History,
  Crown,
  Sparkles,
  ArrowRight,
  TrendingUp,
  CheckCircle,
  XCircle,
  MinusCircle,
} from "@/components/ui/icons";
import { useAuthStore } from "@/store/auth-store";
import { MOCK_CURRENT_USER } from "@/mocks/users";
import { MOCK_PROFILE_STATS } from "@/repositories/mock/mock-meta-repository";
import { MOCK_MATCH_HISTORY } from "@/mocks/meta";
import { EditProfileModal } from "@/components/profile/edit-profile-modal";
import { useToast } from "@/hooks/use-toast";

type TabKey = "overview" | "statistics" | "matches";

export default function ProfilePage() {
  const authUser = useAuthStore((s) => s.user);
  const setUser = useAuthStore((s) => s.setUser);
  const { success: toastSuccess } = useToast();

  // State initialized from mock data / auth store
  const [profile, setProfile] = useState({
    id: authUser?.id ?? MOCK_CURRENT_USER.id,
    username: authUser?.username ?? MOCK_CURRENT_USER.username,
    displayName: authUser?.displayName ?? MOCK_CURRENT_USER.displayName,
    avatarInitial: authUser?.avatarInitial ?? MOCK_CURRENT_USER.avatarInitial,
    avatarUrl: (authUser as { avatarUrl?: string })?.avatarUrl ?? "",
    level: authUser?.level ?? MOCK_CURRENT_USER.level,
    rating: 1845,
    totalMatches: MOCK_PROFILE_STATS.gamesPlayed,
    wins: MOCK_PROFILE_STATS.wins,
    losses: MOCK_PROFILE_STATS.losses,
    draws: MOCK_PROFILE_STATS.draws,
    // Calculate accurate win rate from wins and matches
    winRate: Number(
      ((MOCK_PROFILE_STATS.wins / MOCK_PROFILE_STATS.gamesPlayed) * 100).toFixed(1)
    ),
    totalCoinsEarned: MOCK_PROFILE_STATS.totalCoinsEarned,
    bluffsSucceeded: MOCK_PROFILE_STATS.bluffsSucceeded,
    challengesWon: MOCK_PROFILE_STATS.challengesWon,
    eliminations: MOCK_PROFILE_STATS.eliminations,
    winStreak: 5,
  });

  const [activeTab, setActiveTab] = useState<TabKey>("overview");
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [avatarError, setAvatarError] = useState(false);

  // Handle Profile Update (only username & avatarUrl editable)
  const handleProfileSave = (data: { username: string; avatarUrl: string }) => {
    setProfile((prev) => ({
      ...prev,
      username: data.username,
      avatarUrl: data.avatarUrl,
      avatarInitial: data.username.slice(0, 2).toUpperCase(),
    }));
    setAvatarError(false);

    // Also update authStore if logged in
    if (authUser) {
      setUser({
        ...authUser,
        username: data.username,
        avatarInitial: data.username.slice(0, 2).toUpperCase(),
      });
    }

    toastSuccess(
      "প্রোফাইল আপডেট হয়েছে",
      "আপনার ইউজারনেম এবং অবতার সফলভাবে সংরক্ষণ করা হয়েছে।"
    );
  };

  return (
    <div className="space-y-8 pb-12">
      {/* ── Premium Profile Header Banner ───────────────── */}
      <section className="relative overflow-hidden rounded-3xl border border-forest-500/25 bg-surface panel-emboss panel-texture p-6 sm:p-8">
        {/* Subtle decorative background gradient */}
        <div
          className="pointer-events-none absolute inset-0 opacity-40"
          style={{
            background:
              "radial-gradient(ellipse 60% 50% at 85% 15%, rgb(201 165 60 / 0.18), transparent 60%)," +
              "radial-gradient(ellipse 50% 40% at 15% 85%, rgb(44 110 82 / 0.25), transparent 60%)",
          }}
        />

        <div className="relative z-10 flex flex-col gap-6 md:flex-row md:items-center md:justify-between">
          {/* Avatar and Identity */}
          <div className="flex flex-col sm:flex-row items-center sm:items-start gap-5 text-center sm:text-left">
            {/* Avatar Circle with Gold Ring & Level Badge */}
            <div className="relative">
              <div className="relative flex size-24 sm:size-28 shrink-0 items-center justify-center overflow-hidden rounded-full border-2 border-gold-500/50 bg-gradient-to-b from-deep-700 to-deep-900 text-gold-300 shadow-gold">
                {profile.avatarUrl && !avatarError ? (
                  <Image
                    src={profile.avatarUrl}
                    alt={profile.username}
                    fill
                    className="object-cover"
                    onError={() => setAvatarError(true)}
                  />
                ) : (
                  <span className="font-bengali text-4xl sm:text-5xl font-bold">
                    {profile.avatarInitial}
                  </span>
                )}
              </div>

              {/* Level indicator pill on avatar bottom */}
              <div className="absolute -bottom-2 left-1/2 -translate-x-1/2 whitespace-nowrap rounded-full border border-gold-400/40 bg-deep-950 px-2.5 py-0.5 text-[11px] font-bold text-gold-300 shadow-md">
                Lv. {profile.level}
              </div>
            </div>

            {/* Player details */}
            <div className="space-y-1.5 pt-1">
              <div className="flex flex-wrap items-center justify-center sm:justify-start gap-2">
                <h1 className="font-bengali text-2xl sm:text-3xl font-bold text-ivory">
                  {profile.displayName}
                </h1>
                <Badge tone="gold" className="gap-1">
                  <Crown className="size-3" />
                  গ্র্যান্ডমাস্টার · Grandmaster
                </Badge>
              </div>

              <p className="text-sm font-mono text-muted">
                @{profile.username}
              </p>

              <div className="flex flex-wrap items-center justify-center sm:justify-start gap-3 pt-1 text-xs text-parchment-300">
                <span className="flex items-center gap-1.5 text-gold-400 font-semibold">
                  <Trophy className="size-3.5" />
                  রেটিং: {profile.rating}
                </span>
                <span className="text-forest-500">•</span>
                <span className="text-forest-300">
                  জাতীয় মেধা তালিকা: #২
                </span>
                <span className="text-forest-500">•</span>
                <span className="text-muted">
                  সদস্য: সেপ্টেম্বর ২০২৪
                </span>
              </div>
            </div>
          </div>

          {/* Action Buttons: Edit Profile & View Match History */}
          <div className="flex flex-wrap items-center justify-center gap-3">
            {/* Button: Edit Profile */}
            <Button
              variant="outline"
              size="md"
              onClick={() => setIsEditModalOpen(true)}
              className="gap-2 font-bengali"
            >
              <Pencil className="size-4 text-gold-400" />
              প্রোফাইল সম্পাদনা · Edit Profile
            </Button>

            {/* Button: View Match History */}
            <Link href="/history">
              <Button
                variant="premium"
                size="md"
                className="gap-2 font-bengali shadow-gold"
              >
                <History className="size-4" />
                ম্যাচ ইতিহাস · View Match History
              </Button>
            </Link>
          </div>
        </div>
      </section>

      {/* ── Key Performance Metrics (Required Displays) ───── */}
      <section className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-5 sm:gap-4">
        {/* Rating */}
        <div className="rounded-2xl border border-forest-500/20 bg-deep-900/60 p-4 transition-all hover:border-gold-500/35">
          <div className="flex items-center justify-between text-muted">
            <span className="text-xs uppercase tracking-wider font-semibold font-cinzel">
              Rating
            </span>
            <Trophy className="size-4 text-gold-400" />
          </div>
          <p className="mt-2 text-2xl sm:text-3xl font-bold text-ivory">
            {profile.rating}
          </p>
          <p className="mt-1 text-[11px] text-forest-300 flex items-center gap-1">
            <TrendingUp className="size-3" />
            +২৫ রেটিং পয়েন্ট (এই সপ্তাহে)
          </p>
        </div>

        {/* Total Matches */}
        <div className="rounded-2xl border border-forest-500/20 bg-deep-900/60 p-4 transition-all hover:border-gold-500/35">
          <div className="flex items-center justify-between text-muted">
            <span className="text-xs uppercase tracking-wider font-semibold font-cinzel">
              Total Matches
            </span>
            <Swords className="size-4 text-forest-400" />
          </div>
          <p className="mt-2 text-2xl sm:text-3xl font-bold text-ivory">
            {profile.totalMatches}
          </p>
          <p className="mt-1 text-[11px] text-muted">
            মোট সম্পন্ন খেলা
          </p>
        </div>

        {/* Wins */}
        <div className="rounded-2xl border border-forest-500/20 bg-deep-900/60 p-4 transition-all hover:border-forest-400/40">
          <div className="flex items-center justify-between text-forest-300">
            <span className="text-xs uppercase tracking-wider font-semibold font-cinzel">
              Wins
            </span>
            <CheckCircle className="size-4 text-forest-400" />
          </div>
          <p className="mt-2 text-2xl sm:text-3xl font-bold text-forest-300">
            {profile.wins}
          </p>
          <p className="mt-1 text-[11px] text-forest-300/80">
            বিজয়ী ম্যাচসমূহ
          </p>
        </div>

        {/* Losses */}
        <div className="rounded-2xl border border-forest-500/20 bg-deep-900/60 p-4 transition-all hover:border-crimson-400/40">
          <div className="flex items-center justify-between text-crimson-300">
            <span className="text-xs uppercase tracking-wider font-semibold font-cinzel">
              Losses
            </span>
            <XCircle className="size-4 text-crimson-400" />
          </div>
          <p className="mt-2 text-2xl sm:text-3xl font-bold text-crimson-300">
            {profile.losses}
          </p>
          <p className="mt-1 text-[11px] text-crimson-300/80">
            পরাজিত ম্যাচসমূহ
          </p>
        </div>

        {/* Win Rate */}
        <div className="col-span-2 sm:col-span-1 rounded-2xl border border-gold-500/30 bg-deep-900/60 p-4 shadow-gold">
          <div className="flex items-center justify-between text-gold-300">
            <span className="text-xs uppercase tracking-wider font-semibold font-cinzel">
              Win Rate
            </span>
            <Sparkles className="size-4 text-gold-400" />
          </div>
          <p className="mt-2 text-2xl sm:text-3xl font-bold text-gold-gradient">
            {profile.winRate}%
          </p>
          <div className="mt-2 h-1.5 w-full overflow-hidden rounded-full bg-deep-950">
            <div
              className="h-full rounded-full bg-gradient-to-r from-forest-500 via-gold-400 to-gold-300"
              style={{ width: `${profile.winRate}%` }}
            />
          </div>
        </div>
      </section>

      {/* ── Section Tabs: Overview | Statistics | Recent Matches ─ */}
      <div className="flex border-b border-forest-500/20 gap-2 sm:gap-4 overflow-x-auto pb-px">
        {[
          { key: "overview", label: "Overview (এক নজরে)" },
          { key: "statistics", label: "Statistics (পরিসংখ্যান)" },
          { key: "matches", label: "Recent Matches (সাম্প্রতিক ম্যাচ)" },
        ].map((tab) => {
          const active = activeTab === tab.key;
          return (
            <button
              key={tab.key}
              type="button"
              onClick={() => setActiveTab(tab.key as TabKey)}
              className={`pb-3 px-3 text-sm font-semibold transition-all relative whitespace-nowrap cursor-pointer ${
                active
                  ? "text-gold-300 border-b-2 border-gold-400"
                  : "text-muted hover:text-ivory"
              }`}
            >
              {tab.label}
            </button>
          );
        })}
      </div>

      {/* ── Tab Content: 1. Overview ─────────────────────── */}
      {activeTab === "overview" && (
        <section className="space-y-6 animate-fade-in">
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
            {/* Win/Loss Distribution Card */}
            <div className="rounded-2xl border border-forest-500/25 bg-deep-900/70 p-6 space-y-4">
              <h3 className="font-bengali font-bold text-lg text-ivory flex items-center gap-2">
                <Trophy className="size-4 text-gold-400" />
                ম্যাচ অনুপাত বিশ্লেষণ
              </h3>

              <div className="space-y-3">
                <div className="flex justify-between text-xs text-muted">
                  <span>জয় ({profile.wins})</span>
                  <span>পরাজয় ({profile.losses})</span>
                  <span>ড্র ({profile.draws})</span>
                </div>

                <div className="flex h-3 w-full overflow-hidden rounded-full bg-deep-950">
                  <div
                    className="bg-forest-400 transition-all"
                    style={{
                      width: `${(profile.wins / profile.totalMatches) * 100}%`,
                    }}
                    title={`Wins: ${profile.wins}`}
                  />
                  <div
                    className="bg-crimson-400 transition-all"
                    style={{
                      width: `${(profile.losses / profile.totalMatches) * 100}%`,
                    }}
                    title={`Losses: ${profile.losses}`}
                  />
                  <div
                    className="bg-gold-400 transition-all"
                    style={{
                      width: `${(profile.draws / profile.totalMatches) * 100}%`,
                    }}
                    title={`Draws: ${profile.draws}`}
                  />
                </div>

                <div className="grid grid-cols-3 gap-2 pt-2 text-center text-xs">
                  <div className="rounded-lg bg-deep-800/60 p-2 border border-forest-500/20">
                    <span className="text-forest-300 font-bold block text-sm">
                      {((profile.wins / profile.totalMatches) * 100).toFixed(0)}%
                    </span>
                    <span className="text-[10px] text-muted">Win Rate</span>
                  </div>
                  <div className="rounded-lg bg-deep-800/60 p-2 border border-crimson-500/20">
                    <span className="text-crimson-300 font-bold block text-sm">
                      {((profile.losses / profile.totalMatches) * 100).toFixed(0)}%
                    </span>
                    <span className="text-[10px] text-muted">Loss Rate</span>
                  </div>
                  <div className="rounded-lg bg-deep-800/60 p-2 border border-gold-500/20">
                    <span className="text-gold-300 font-bold block text-sm">
                      {profile.winStreak}
                    </span>
                    <span className="text-[10px] text-muted">Win Streak</span>
                  </div>
                </div>
              </div>
            </div>

            {/* Favorite Character Archetype */}
            <div className="rounded-2xl border border-forest-500/25 bg-deep-900/70 p-6 space-y-4">
              <h3 className="font-bengali font-bold text-lg text-ivory flex items-center gap-2">
                <Crown className="size-4 text-gold-400" />
                সর্বাধিক ব্যবহৃত চরিত্র
              </h3>

              <div className="flex items-center gap-4 rounded-xl border border-gold-500/30 bg-deep-950/60 p-4">
                <div className="flex size-14 shrink-0 items-center justify-center rounded-xl bg-gradient-to-b from-deep-700 to-deep-900 border border-gold-500/40 text-gold-300 font-bengali text-2xl font-bold">
                  ম
                </div>
                <div className="space-y-0.5">
                  <div className="font-bengali font-bold text-base text-ivory">
                    মন্ত্রী (THE MINISTER)
                  </div>
                  <div className="text-xs text-gold-400 font-medium">
                    সফলতা হার: ৮৪%
                  </div>
                  <div className="text-[11px] text-muted">
                    ট্যাক্স আদায়ে পারদর্শী
                  </div>
                </div>
              </div>

              <p className="text-xs text-muted leading-relaxed">
                অর্থনীতি নিয়ন্ত্রণ ও কর আদায় আপনার অন্যতম সফল কৌশল হিসেবে চিহ্নিত হয়েছে।
              </p>
            </div>

            {/* Quick Actions & Account Status */}
            <div className="rounded-2xl border border-forest-500/25 bg-deep-900/70 p-6 space-y-4 flex flex-col justify-between">
              <div>
                <h3 className="font-bengali font-bold text-lg text-ivory flex items-center gap-2">
                  <Shield className="size-4 text-forest-400" />
                  অ্যাকাউন্ট স্ট্যাটাস
                </h3>
                <div className="mt-3 space-y-2 text-xs">
                  <div className="flex justify-between py-1.5 border-b border-forest-500/15">
                    <span className="text-muted">প্লেয়ার আইডি</span>
                    <span className="font-mono text-ivory">{profile.id}</span>
                  </div>
                  <div className="flex justify-between py-1.5 border-b border-forest-500/15">
                    <span className="text-muted">নিরাপত্তা স্তর</span>
                    <span className="text-forest-300 font-medium">ভেরিফায়েড প্লেয়ার</span>
                  </div>
                  <div className="flex justify-between py-1.5">
                    <span className="text-muted">গেম সিজন</span>
                    <span className="text-gold-300 font-semibold">সিজন ১ (সক্রিয়)</span>
                  </div>
                </div>
              </div>

              <div className="pt-2 flex gap-2">
                <Button
                  variant="outline"
                  size="sm"
                  fullWidth
                  onClick={() => setIsEditModalOpen(true)}
                  className="gap-1.5 font-bengali"
                >
                  <Pencil className="size-3.5" />
                  Edit Profile
                </Button>
                <Link href="/history" className="flex-1">
                  <Button variant="outline" size="sm" fullWidth className="gap-1.5 font-bengali">
                    <History className="size-3.5" />
                    Match History
                  </Button>
                </Link>
              </div>
            </div>
          </div>

          {/* Quick preview of recent matches */}
          <div className="rounded-2xl border border-forest-500/25 bg-deep-900/50 p-6 space-y-4">
            <div className="flex items-center justify-between">
              <h3 className="font-bengali font-bold text-lg text-ivory flex items-center gap-2">
                <Clock3 className="size-4 text-gold-400" />
                সর্বশেষ ম্যাচের সারসংক্ষেপ
              </h3>
              <Link
                href="/history"
                className="text-xs text-gold-400 hover:text-gold-300 font-semibold flex items-center gap-1 transition-colors"
              >
                সবগুলো দেখুন
                <ArrowRight className="size-3.5" />
              </Link>
            </div>

            <div className="divide-y divide-forest-500/15">
              {MOCK_MATCH_HISTORY.slice(0, 3).map((match) => (
                <div
                  key={match.matchId}
                  className="flex items-center justify-between py-3 text-sm"
                >
                  <div className="flex items-center gap-3">
                    <span
                      className={`flex size-8 shrink-0 items-center justify-center rounded-lg font-bold text-xs ${
                        match.result === "win"
                          ? "bg-forest-500/20 text-forest-300 border border-forest-500/40"
                          : match.result === "loss"
                          ? "bg-crimson-500/20 text-crimson-300 border border-crimson-500/40"
                          : "bg-gold-500/20 text-gold-300 border border-gold-500/40"
                      }`}
                    >
                      {match.result === "win" ? "W" : match.result === "loss" ? "L" : "D"}
                    </span>
                    <div>
                      <div className="font-bengali font-semibold text-ivory">
                        প্রতিদ্বন্দ্বী: {match.opponentName}
                      </div>
                      <div className="text-xs text-muted flex items-center gap-2">
                        <span>{match.playerCount} জন খেলোয়াড়</span>
                        <span>•</span>
                        <span>{match.durationMinutes} মিনিট</span>
                      </div>
                    </div>
                  </div>

                  <div className="text-right">
                    <span
                      className={`font-semibold text-xs capitalize ${
                        match.result === "win"
                          ? "text-forest-300"
                          : match.result === "loss"
                          ? "text-crimson-300"
                          : "text-gold-300"
                      }`}
                    >
                      {match.result === "win" ? "বিজয়ী (১ম)" : match.result === "loss" ? "পরাজয়" : "ড্র"}
                    </span>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </section>
      )}

      {/* ── Tab Content: 2. Statistics ──────────────────── */}
      {activeTab === "statistics" && (
        <section className="space-y-6 animate-fade-in">
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
            {/* Coins Earned */}
            <div className="rounded-2xl border border-forest-500/25 bg-deep-900/60 p-5 space-y-2">
              <div className="flex items-center justify-between text-gold-300">
                <span className="text-xs font-semibold uppercase tracking-wider">
                  Total Coins Earned
                </span>
                <Coins className="size-5 text-gold-400" />
              </div>
              <p className="text-3xl font-bold text-ivory">
                {profile.totalCoinsEarned.toLocaleString()}
              </p>
              <p className="text-xs text-muted font-bengali">
                খেলায় অর্জিত মোট স্বর্ণমুদ্রা
              </p>
            </div>

            {/* Bluffs Succeeded */}
            <div className="rounded-2xl border border-forest-500/25 bg-deep-900/60 p-5 space-y-2">
              <div className="flex items-center justify-between text-forest-300">
                <span className="text-xs font-semibold uppercase tracking-wider">
                  Bluffs Succeeded
                </span>
                <ShieldAlert className="size-5 text-forest-400" />
              </div>
              <p className="text-3xl font-bold text-forest-300">
                {profile.bluffsSucceeded}
              </p>
              <p className="text-xs text-muted font-bengali">
                সফল মনস্তাত্ত্বিক ব্ল্যাফ চাল
              </p>
            </div>

            {/* Challenges Won */}
            <div className="rounded-2xl border border-forest-500/25 bg-deep-900/60 p-5 space-y-2">
              <div className="flex items-center justify-between text-gold-300">
                <span className="text-xs font-semibold uppercase tracking-wider">
                  Challenges Won
                </span>
                <Trophy className="size-5 text-gold-400" />
              </div>
              <p className="text-3xl font-bold text-gold-gradient">
                {profile.challengesWon}
              </p>
              <p className="text-xs text-muted font-bengali">
                প্রতিপক্ষের বিরুদ্ধে জয়ী চ্যালেঞ্জ
              </p>
            </div>

            {/* Eliminations */}
            <div className="rounded-2xl border border-forest-500/25 bg-deep-900/60 p-5 space-y-2">
              <div className="flex items-center justify-between text-crimson-300">
                <span className="text-xs font-semibold uppercase tracking-wider">
                  Eliminations
                </span>
                <Swords className="size-5 text-crimson-400" />
              </div>
              <p className="text-3xl font-bold text-crimson-300">
                {profile.eliminations}
              </p>
              <p className="text-xs text-muted font-bengali">
                প্রতিপক্ষের ইনফ্লুয়েন্স অপসারণ
              </p>
            </div>

            {/* Current Win Streak */}
            <div className="rounded-2xl border border-forest-500/25 bg-deep-900/60 p-5 space-y-2">
              <div className="flex items-center justify-between text-forest-300">
                <span className="text-xs font-semibold uppercase tracking-wider">
                  Win Streak
                </span>
                <Flame className="size-5 text-forest-400" />
              </div>
              <p className="text-3xl font-bold text-forest-300">
                {profile.winStreak} ম্যাচ
              </p>
              <p className="text-xs text-muted font-bengali">
                চলমান টানা অপরাজিত থাকার রেকর্ড
              </p>
            </div>

            {/* Average Match Duration */}
            <div className="rounded-2xl border border-forest-500/25 bg-deep-900/60 p-5 space-y-2">
              <div className="flex items-center justify-between text-muted">
                <span className="text-xs font-semibold uppercase tracking-wider">
                  Avg Duration
                </span>
                <Clock3 className="size-5 text-muted" />
              </div>
              <p className="text-3xl font-bold text-ivory">
                ১৪.২ মিনিট
              </p>
              <p className="text-xs text-muted font-bengali">
                প্রতি ম্যাচের গড় স্থায়িত্ব
              </p>
            </div>
          </div>
        </section>
      )}

      {/* ── Tab Content: 3. Recent Matches ──────────────── */}
      {activeTab === "matches" && (
        <section className="space-y-6 animate-fade-in">
          <div className="rounded-2xl border border-forest-500/25 bg-surface panel-emboss panel-texture p-6">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-6">
              <div>
                <h3 className="font-bengali font-bold text-xl text-ivory">
                  সাম্প্রতিক ম্যাচ তালিকা · Recent Matches
                </h3>
                <p className="text-xs text-muted">
                  আপনার শেষ খেলা ম্যাচগুলোর বিস্তারিত রেকর্ড
                </p>
              </div>

              {/* View Match History Button */}
              <Link href="/history">
                <Button variant="outline" size="sm" className="gap-2 font-bengali">
                  <History className="size-4" />
                  সম্পূর্ণ ইতিহাস · View Match History
                </Button>
              </Link>
            </div>

            <div className="space-y-3">
              {MOCK_MATCH_HISTORY.map((match) => (
                <div
                  key={match.matchId}
                  className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 p-4 rounded-xl border border-forest-500/20 bg-deep-900/70 hover:border-gold-500/30 transition-all"
                >
                  <div className="flex items-center gap-3">
                    {/* Result Icon Badge */}
                    <div
                      className={`flex size-10 shrink-0 items-center justify-center rounded-xl font-bold text-sm ${
                        match.result === "win"
                          ? "bg-forest-500/20 text-forest-300 border border-forest-500/40"
                          : match.result === "loss"
                          ? "bg-crimson-500/20 text-crimson-300 border border-crimson-500/40"
                          : "bg-gold-500/20 text-gold-300 border border-gold-500/40"
                      }`}
                    >
                      {match.result === "win" ? (
                        <CheckCircle className="size-5" />
                      ) : match.result === "loss" ? (
                        <XCircle className="size-5" />
                      ) : (
                        <MinusCircle className="size-5" />
                      )}
                    </div>

                    {/* Match Info */}
                    <div>
                      <div className="flex items-center gap-2">
                        <span className="font-bengali font-bold text-base text-ivory">
                          বনাম {match.opponentName}
                        </span>
                        <span className="font-mono text-[10px] text-muted bg-deep-950 px-2 py-0.5 rounded border border-white/5">
                          {match.matchId}
                        </span>
                      </div>

                      <div className="flex flex-wrap items-center gap-3 text-xs text-muted mt-0.5">
                        <span className="flex items-center gap-1">
                          <Users className="size-3" />
                          {match.playerCount} খেলোয়াড়
                        </span>
                        <span>•</span>
                        <span className="flex items-center gap-1">
                          <Clock3 className="size-3" />
                          {match.durationMinutes} মিনিট
                        </span>
                        <span>•</span>
                        <span>অবস্থান: {match.position}ম স্থান</span>
                      </div>
                    </div>
                  </div>

                  <div className="flex items-center justify-between sm:justify-end gap-3 pt-2 sm:pt-0 border-t sm:border-t-0 border-forest-500/10">
                    <span
                      className={`px-3 py-1 rounded-full text-xs font-bold tracking-wider uppercase border ${
                        match.result === "win"
                          ? "bg-forest-500/15 text-forest-300 border-forest-500/30"
                          : match.result === "loss"
                          ? "bg-crimson-500/15 text-crimson-300 border-crimson-500/30"
                          : "bg-gold-500/15 text-gold-300 border-gold-500/30"
                      }`}
                    >
                      {match.result === "win" ? "VICTORY" : match.result === "loss" ? "DEFEAT" : "DRAW"}
                    </span>
                  </div>
                </div>
              ))}
            </div>

            <div className="mt-6 text-center">
              <Link href="/history">
                <Button variant="premium" size="md" className="gap-2 font-bengali">
                  <History className="size-4" />
                  View Match History (সকল ম্যাচের ইতিহাস)
                </Button>
              </Link>
            </div>
          </div>
        </section>
      )}

      {/* ── Edit Profile Modal ──────────────────────────── */}
      <EditProfileModal
        open={isEditModalOpen}
        onClose={() => setIsEditModalOpen(false)}
        onSave={handleProfileSave}
        profile={{
          username: profile.username,
          avatarUrl: profile.avatarUrl,
          avatarInitial: profile.avatarInitial,
          rating: profile.rating,
          wins: profile.wins,
          losses: profile.losses,
          totalMatches: profile.totalMatches,
        }}
      />
    </div>
  );
}