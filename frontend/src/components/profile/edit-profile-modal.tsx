"use client";

import { useState } from "react";
import Image from "next/image";
import { Modal } from "@/components/ui/modal";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Lock, UserIcon, Check, X, Sparkles } from "@/components/ui/icons";

interface EditProfileModalProps {
  open: boolean;
  onClose: () => void;
  onSave: (data: { username: string; avatarUrl: string }) => void;
  profile: {
    username: string;
    avatarUrl?: string;
    avatarInitial: string;
    rating: number;
    wins: number;
    losses: number;
    totalMatches: number;
  };
}

export function EditProfileModal({
  open,
  onClose,
  onSave,
  profile,
}: EditProfileModalProps) {
  const [username, setUsername] = useState(profile.username);
  const [avatarUrl, setAvatarUrl] = useState(profile.avatarUrl ?? "");
  const [error, setError] = useState<string | null>(null);
  const [previewError, setPreviewError] = useState(false);

  const [prevSnapshot, setPrevSnapshot] = useState({ open, profile });

  // Adjust state during render when the modal opens or the profile changes (React-sanctioned pattern).
  if (prevSnapshot.open !== open || prevSnapshot.profile !== profile) {
    setPrevSnapshot({ open, profile });
    setUsername(profile.username);
    setAvatarUrl(profile.avatarUrl ?? "");
    setError(null);
    setPreviewError(false);
  }

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const trimmed = username.trim();
    if (!trimmed) {
      setError("ইউজারনেম খালি হতে পারে না");
      return;
    }
    if (trimmed.length < 3 || trimmed.length > 30) {
      setError("ইউজারনেম ৩ থেকে ৩০ অক্ষরের মধ্যে হতে হবে");
      return;
    }
    onSave({
      username: trimmed,
      avatarUrl: avatarUrl.trim(),
    });
    onClose();
  };

  return (
    <Modal
      open={open}
      onClose={onClose}
      title="প্রোফাইল সম্পাদনা · Edit Profile"
      subtitle="আপনার ব্যবহারকারী নাম ও অবতার কাস্টমাইজ করুন"
      size="md"
    >
      <form onSubmit={handleSubmit} className="space-y-6">
        {/* Avatar Live Preview */}
        <div className="flex items-center gap-4 rounded-xl border border-forest-500/20 bg-deep-900/60 p-4">
          <div className="relative flex size-16 shrink-0 items-center justify-center overflow-hidden rounded-full border-2 border-gold-500/50 bg-deep-800 text-gold-300 shadow-gold">
            {avatarUrl && !previewError ? (
              <Image
                src={avatarUrl}
                alt="Avatar preview"
                fill
                className="object-cover"
                onError={() => setPreviewError(true)}
              />
            ) : (
              <span className="font-bengali text-2xl font-bold">
                {username ? username.slice(0, 2).toUpperCase() : profile.avatarInitial}
              </span>
            )}
          </div>
          <div className="space-y-1">
            <h4 className="text-sm font-semibold text-ivory flex items-center gap-1.5">
              <Sparkles className="size-3.5 text-gold-400" />
              অবতার প্রিভিউ (Avatar Preview)
            </h4>
            <p className="text-xs text-muted">
              {avatarUrl
                ? previewError
                  ? "ছবির লিংক কাজ করছে না, ডিফল্ট ইনিশিয়াল প্রদর্শিত হবে"
                  : "কাস্টম অবতার সক্রিয় আছে"
                : "কোনো লিংক না দিলে অক্ষরের ইনিশিয়াল প্রদর্শিত হবে"}
            </p>
          </div>
        </div>

        {/* Editable Fields */}
        <div className="space-y-4">
          <Input
            label="ব্যবহারকারী নাম · Username (Editable)"
            value={username}
            onChange={(e) => {
              setUsername(e.target.value);
              setError(null);
            }}
            placeholder="আপনার নতুন ইউজারনেম লিখুন"
            leadingIcon={<UserIcon className="size-4" />}
            error={error ?? undefined}
            hint="অন্যান্য খেলোয়াড়রা আপনাকে এই নামে চিনবে"
            required
          />

          <Input
            label="অবতার ছবির লিংক · Avatar URL Placeholder (Editable)"
            value={avatarUrl}
            onChange={(e) => {
              setAvatarUrl(e.target.value);
              setPreviewError(false);
            }}
            placeholder="https://example.com/avatar.png"
            hint="যেকোনো বৈধ ছবির সরাসরি URL পেস্ট করুন অথবা খালি রাখুন"
          />
        </div>

        {/* Locked / Read-Only Fields Section (Strict Requirement: rating, wins, losses, matches cannot be edited) */}
        <div className="rounded-xl border border-forest-500/25 bg-deep-950/70 p-4 space-y-3">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2 text-xs font-semibold uppercase tracking-wider text-gold-400">
              <Lock className="size-3.5 text-gold-400" />
              <span>গেম রেকর্ড (অপরিবর্তনযোগ্য · Read-Only)</span>
            </div>
            <span className="text-[10px] text-muted font-mono">VERIFIED DATA</span>
          </div>

          <p className="text-xs text-muted">
            রেটিং ও ম্যাচের পরিসংখ্যান সার্ভার দ্বারা স্বয়ংক্রিয়ভাবে নির্ধারিত হয় এবং ম্যানুয়ালি পরিবর্তনযোগ্য নয়।
          </p>

          <div className="grid grid-cols-2 sm:grid-cols-4 gap-2 pt-1">
            {/* Rating - Locked */}
            <div className="rounded-lg border border-forest-500/20 bg-deep-900/50 p-2.5 text-center">
              <span className="text-[10px] uppercase text-muted block">Rating</span>
              <span className="font-bold text-ivory text-sm">{profile.rating}</span>
            </div>

            {/* Total Matches - Locked */}
            <div className="rounded-lg border border-forest-500/20 bg-deep-900/50 p-2.5 text-center">
              <span className="text-[10px] uppercase text-muted block">Matches</span>
              <span className="font-bold text-ivory text-sm">{profile.totalMatches}</span>
            </div>

            {/* Wins - Locked */}
            <div className="rounded-lg border border-forest-500/20 bg-deep-900/50 p-2.5 text-center">
              <span className="text-[10px] uppercase text-forest-300 block">Wins</span>
              <span className="font-bold text-forest-300 text-sm">{profile.wins}</span>
            </div>

            {/* Losses - Locked */}
            <div className="rounded-lg border border-forest-500/20 bg-deep-900/50 p-2.5 text-center">
              <span className="text-[10px] uppercase text-crimson-300 block">Losses</span>
              <span className="font-bold text-crimson-300 text-sm">{profile.losses}</span>
            </div>
          </div>
        </div>

        {/* Action Buttons */}
        <div className="flex items-center justify-end gap-3 pt-2">
          <Button type="button" variant="outline" size="md" onClick={onClose}>
            <X className="size-4" />
            বাতিল (Cancel)
          </Button>
          <Button type="submit" variant="premium" size="md">
            <Check className="size-4" />
            সংরক্ষণ করুন (Save)
          </Button>
        </div>
      </form>
    </Modal>
  );
}
