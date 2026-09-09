"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useRef, useState } from "react";
import type { FormEvent } from "react";

import { AuthService } from "@/services/auth-service";
import {
  EMAIL_MAX,
  PASSWORD_MAX,
  PASSWORD_MIN,
  USERNAME_MAX,
  USERNAME_MIN,
  validateConfirm,
  validateEmail,
  validatePassword,
  validateRegister,
  validateUsername,
} from "@/lib/validation/auth";
import { AuthShell } from "@/components/auth/auth-shell";
import { Button } from "@/components/ui/button";
import { IconButton } from "@/components/ui/icon-button";
import { Input } from "@/components/ui/input";
import {
  AlertTriangle,
  ArrowRight,
  AtSign,
  CheckCheck,
  Eye,
  EyeOff,
  Lock,
  Mail,
  UserPlus,
} from "@/components/ui/icons";

type FormValues = {
  username: string;
  email: string;
  password: string;
  confirm: string;
};

type FieldName = keyof FormValues;
type FieldErrors = Partial<Record<FieldName, string>>;
type Status = "idle" | "submitting" | "success";

const INITIAL_VALUES: FormValues = {
  username: "",
  email: "",
  password: "",
  confirm: "",
};

export default function RegisterPage() {
  const router = useRouter();
  const redirectTimer = useRef<ReturnType<typeof setTimeout> | null>(null);
  const usernameCheckSeq = useRef(0);

  const [values, setValues] = useState<FormValues>(INITIAL_VALUES);
  const [errors, setErrors] = useState<FieldErrors>({});
  const [touched, setTouched] = useState<Partial<Record<FieldName, boolean>>>({});
  const [status, setStatus] = useState<Status>("idle");
  const [serverError, setServerError] = useState<string | null>(null);
  const [checkingUsername, setCheckingUsername] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirm, setShowConfirm] = useState(false);

  useEffect(
    () => () => {
      if (redirectTimer.current) clearTimeout(redirectTimer.current);
    },
    [],
  );

  const submitting = status === "submitting";

  async function checkUsernameAvailability(username: string) {
    const seq = ++usernameCheckSeq.current;
    setCheckingUsername(true);
    const result = await AuthService.checkUsername(username);
    if (seq !== usernameCheckSeq.current) return;
    setCheckingUsername(false);
    if (result.ok && !result.data.available) {
      setErrors((prev) => ({ ...prev, username: "এই ইউজারনেমটি ইতিমধ্যে ব্যবহৃত।" }));
    }
  }

  function handleChange(field: FieldName, value: string) {
    setValues((prev) => ({ ...prev, [field]: value }));
    setErrors((prev) => ({ ...prev, [field]: undefined }));
    setServerError(null);
  }

  function handleBlur(field: FieldName) {
    setTouched((prev) => ({ ...prev, [field]: true }));
    const nextError =
      field === "username"
        ? validateUsername(values.username)
        : field === "email"
          ? validateEmail(values.email)
          : field === "password"
            ? validatePassword(values.password)
            : validateConfirm(values.confirm, values.password);
    setErrors((prev) => ({ ...prev, [field]: nextError ?? undefined }));

    if (field === "username" && !nextError && values.username.trim().length >= USERNAME_MIN) {
      void checkUsernameAvailability(values.username.trim());
    }
  }

  async function handleSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setServerError(null);

    const nextErrors = validateRegister(values);
    setErrors(nextErrors);
    setTouched({ username: true, email: true, password: true, confirm: true });
    if (Object.values(nextErrors).some(Boolean)) return;

    setStatus("submitting");
    const username = values.username.trim();
    const result = await AuthService.register({
      username,
      displayName: username,
      email: values.email.trim(),
      password: values.password,
    });

    if (!result.ok) {
      setStatus("idle");
      setServerError(AuthService.normalizeError(result.error));
      if (result.error.error === "USERNAME_ALREADY_EXISTS" && result.error.message) {
        setErrors((prev) => ({ ...prev, username: result.error.message }));
      }
      return;
    }

    setStatus("success");
    redirectTimer.current = setTimeout(() => router.replace("/login"), 950);
  }

  const usernameError = touched.username ? errors.username : undefined;
  const emailError = touched.email ? errors.email : undefined;
  const passwordError = touched.password ? errors.password : undefined;
  const confirmError = touched.confirm ? errors.confirm : undefined;

  return (
    <AuthShell
      eyebrow="যোগ দিন · Sign Up"
      title="নিবন্ধন"
      subtitle="ক্ষমতার খেলায় নতুন একজন। মিথ্যা বলো না — অন্তত রেজিস্ট্রেশনে নয়।"
      footer={
        <>
          ইতিমধ্যে অ্যাকাউন্ট আছে?{" "}
          <Link
            href="/login"
            className="inline-flex items-center gap-1 font-semibold text-gold-400 transition-colors hover:text-gold-300"
          >
            লগইন করো
            <ArrowRight className="size-3.5" aria-hidden />
          </Link>
        </>
      }
    >
      {status === "success" ? (
        <div className="py-4 text-center animate-fade-up">
          <span className="mx-auto mb-4 flex size-14 items-center justify-center rounded-full border border-forest-400/40 bg-forest-500/15">
            <CheckCheck className="size-7 text-forest-300" aria-hidden />
          </span>
          <h2 className="font-bengali text-xl font-semibold text-ivory">নিবন্ধন সফল হয়েছে!</h2>
          <p className="mt-2 text-sm text-muted">লগইন পেজে নিয়ে যাওয়া হচ্ছে…</p>
          <Link
            href="/login"
            className="mt-5 inline-flex items-center gap-1.5 text-sm font-semibold text-gold-400 transition-colors hover:text-gold-300"
          >
            এখনই লগইন করো
            <ArrowRight className="size-4" aria-hidden />
          </Link>
        </div>
      ) : (
        <form onSubmit={handleSubmit} noValidate className="space-y-4">
          {serverError ? (
            <div
              role="alert"
              className="flex items-start gap-2.5 rounded-lg border border-crimson-500/35 bg-crimson-600/15 px-3.5 py-2.5 text-sm text-crimson-200"
            >
              <AlertTriangle className="mt-0.5 size-4 shrink-0" aria-hidden />
              <span>{serverError}</span>
            </div>
          ) : null}

          <Input
            label="ইউজারনেম"
            leadingIcon={<AtSign className="size-4" aria-hidden />}
            type="text"
            autoComplete="username"
            placeholder="e.g. shapla"
            minLength={USERNAME_MIN}
            maxLength={USERNAME_MAX}
            spellCheck={false}
            value={values.username}
            error={usernameError}
            hint={
              !usernameError && checkingUsername
                ? "ইউজারনেম পাওয়া যাচ্ছে কিনা দেখা হচ্ছে…"
                : undefined
            }
            disabled={submitting}
            onChange={(e) => handleChange("username", e.target.value)}
            onBlur={() => handleBlur("username")}
          />

          <Input
            label="ইমেইল"
            leadingIcon={<Mail className="size-4" aria-hidden />}
            type="email"
            autoComplete="email"
            placeholder="e.g. shapla@example.com"
            maxLength={EMAIL_MAX}
            value={values.email}
            error={emailError}
            disabled={submitting}
            onChange={(e) => handleChange("email", e.target.value)}
            onBlur={() => handleBlur("email")}
          />

          <Input
            label="পাসওয়ার্ড"
            leadingIcon={<Lock className="size-4" aria-hidden />}
            type={showPassword ? "text" : "password"}
            autoComplete="new-password"
            placeholder="কমপক্ষে ৬ অক্ষর"
            minLength={PASSWORD_MIN}
            maxLength={PASSWORD_MAX}
            value={values.password}
            error={passwordError}
            hint="অন্তত একটি অক্ষর এবং একটি সংখ্যা থাকতে হবে।"
            disabled={submitting}
            onChange={(e) => handleChange("password", e.target.value)}
            onBlur={() => handleBlur("password")}
            trailingAction={
              <IconButton
                type="button"
                variant="default"
                size="sm"
                label={showPassword ? "পাসওয়ার্ড লুকাও" : "পাসওয়ার্ড দেখাও"}
                className="border-transparent"
                onClick={() => setShowPassword((v) => !v)}
              >
                {showPassword ? (
                  <EyeOff className="size-4" aria-hidden />
                ) : (
                  <Eye className="size-4" aria-hidden />
                )}
              </IconButton>
            }
          />

          <Input
            label="পাসওয়ার্ড নিশ্চিত করো"
            leadingIcon={<Lock className="size-4" aria-hidden />}
            type={showConfirm ? "text" : "password"}
            autoComplete="new-password"
            placeholder="আবার পাসওয়ার্ড লিখুন"
            minLength={PASSWORD_MIN}
            maxLength={PASSWORD_MAX}
            value={values.confirm}
            error={confirmError}
            disabled={submitting}
            onChange={(e) => handleChange("confirm", e.target.value)}
            onBlur={() => handleBlur("confirm")}
            trailingAction={
              <IconButton
                type="button"
                variant="default"
                size="sm"
                label={showConfirm ? "পাসওয়ার্ড লুকাও" : "পাসওয়ার্ড দেখাও"}
                className="border-transparent"
                onClick={() => setShowConfirm((v) => !v)}
              >
                {showConfirm ? (
                  <EyeOff className="size-4" aria-hidden />
                ) : (
                  <Eye className="size-4" aria-hidden />
                )}
              </IconButton>
            }
          />

          <Button
            type="submit"
            variant="premium"
            size="lg"
            fullWidth
            loading={submitting}
            disabled={checkingUsername}
          >
            <UserPlus className="size-4" aria-hidden />
            নিবন্ধন করো
          </Button>

          <p className="text-center text-xs text-muted">
            রেজিস্ট্রেশন করলে তুমি আমাদের নিয়ম মেনে খেলতে রাজি আছো।
          </p>
        </form>
      )}
    </AuthShell>
  );
}