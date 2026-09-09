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
      title="Edit Profile"
      subtitle="Customize your username and avatar image"
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
              Avatar Preview
            </h4>
            <p className="text-xs text-muted">
              {avatarUrl
                ? previewError
                  ? "Image URL not loading, default initial will be used"
                  : "Custom avatar is active"
                : "Leave empty to use your initial as default avatar"}
            </p>
          </div>
        </div>

        {/* Editable Fields */}
        <div className="space-y-4">
          <Input
            label="Username"
            value={username}
            onChange={(e) => {
              setUsername(e.target.value);
              setError(null);
            }}
            placeholder="Enter your username"
            leadingIcon={<UserIcon className="size-4" />}
            error={error ?? undefined}
            hint="Other players will identify you by this name"
            required
          />

          <Input
            label="Avatar Image URL (Optional)"
            value={avatarUrl}
            onChange={(e) => {
              setAvatarUrl(e.target.value);
              setPreviewError(false);
            }}
            placeholder="https://example.com/avatar.png"
            hint="Paste a valid direct image URL or leave blank"
          />
        </div>

        {/* Locked / Read-Only Fields Section (Strict Requirement: rating, wins, losses, matches cannot be edited) */}
        <div className="rounded-xl border border-forest-500/25 bg-deep-950/70 p-4 space-y-3">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2 text-xs font-semibold uppercase tracking-wider text-gold-400">
              <Lock className="size-3.5 text-gold-400" />
              <span>Game Records (Read-Only)</span>
            </div>
            <span className="text-[10px] text-muted font-mono">VERIFIED DATA</span>
          </div>

          <p className="text-xs text-muted">
            Rating and match statistics are tracked automatically and cannot be modified.
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
            Cancel
          </Button>
          <Button type="submit" variant="premium" size="md">
            <Check className="size-4" />
            Save Changes
          </Button>
        </div>
      </form>
    </Modal>
  );
}
