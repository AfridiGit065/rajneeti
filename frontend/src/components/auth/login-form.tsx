"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";
import type { FormEvent } from "react";

import { AuthService } from "@/services/auth-service";
import { useAuthStore } from "@/store/auth-store";
import { useToast } from "@/hooks/use-toast";
import { validateEmail, validateLogin, validatePassword } from "@/lib/validation/auth";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import { IconButton } from "@/components/ui/icon-button";
import { Input } from "@/components/ui/input";
import {
  AlertTriangle,
  Eye,
  EyeOff,
  Lock,
  LogIn,
  Mail,
  UserPlus,
} from "@/components/ui/icons";

type FieldErrors = Partial<Record<"email" | "password", string>>;

export function LoginForm() {
  const router = useRouter();
  const login = useAuthStore((s) => s.login);
  const { info } = useToast();

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [remember, setRemember] = useState(false);
  const [errors, setErrors] = useState<FieldErrors>({});
  const [touched, setTouched] = useState<{ email: boolean; password: boolean }>({
    email: false,
    password: false,
  });
  const [submitting, setSubmitting] = useState(false);
  const [serverError, setServerError] = useState<string | null>(null);
  const [showPassword, setShowPassword] = useState(false);

  function handleBlur(field: "email" | "password") {
    setTouched((prev) => ({ ...prev, [field]: true }));
    const nextError =
      field === "email" ? validateEmail(email) : validatePassword(password);
    setErrors((prev) => ({ ...prev, [field]: nextError ?? undefined }));
  }

  async function handleSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setServerError(null);

    const nextErrors = validateLogin({ email, password });
    setErrors(nextErrors);
    setTouched({ email: true, password: true });
    if (nextErrors.email || nextErrors.password) return;

    setSubmitting(true);
    const result = await login({ email: email.trim(), password }, remember);

    if (!result.ok) {
      setSubmitting(false);
      setServerError(AuthService.normalizeError(result.error));
      return;
    }

    router.replace("/lobby");
  }

  const emailError = touched.email ? errors.email : undefined;
  const passwordError = touched.password ? errors.password : undefined;

  return (
    <form onSubmit={handleSubmit} noValidate className="space-y-5">
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
        label="Email"
        leadingIcon={<Mail className="size-4" aria-hidden />}
        type="email"
        autoComplete="email"
        placeholder="e.g. shapla@example.com"
        value={email}
        error={emailError}
        disabled={submitting}
        onChange={(e) => {
          setEmail(e.target.value);
          setErrors((prev) => ({ ...prev, email: undefined }));
          setServerError(null);
        }}
        onBlur={() => handleBlur("email")}
      />

      <Input
        label="Password"
        leadingIcon={<Lock className="size-4" aria-hidden />}
        type={showPassword ? "text" : "password"}
        autoComplete="current-password"
        placeholder="Enter your password"
        value={password}
        error={passwordError}
        disabled={submitting}
        onChange={(e) => {
          setPassword(e.target.value);
          setErrors((prev) => ({ ...prev, password: undefined }));
          setServerError(null);
        }}
        onBlur={() => handleBlur("password")}
        trailingAction={
          <IconButton
            type="button"
            variant="default"
            size="sm"
            label={showPassword ? "Hide password" : "Show password"}
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

      <div className="flex items-center justify-between gap-2">
        <Checkbox
          checked={remember}
          disabled={submitting}
          onChange={(e) => setRemember(e.target.checked)}
          label="Remember me"
        />
        <button
          type="button"
          disabled={submitting}
          onClick={() =>
            info("Password Reset", "Password reset is coming soon. Please use the demo credentials below.")
          }
          className="text-xs font-medium text-muted transition-colors hover:text-gold-300 disabled:opacity-45"
        >
          Forgot password?
        </button>
      </div>

      <Button type="submit" variant="premium" size="lg" fullWidth loading={submitting}>
        <LogIn className="size-4" aria-hidden />
        Log In
      </Button>

      <Link href="/register" className="block">
        <Button type="button" variant="secondary" size="lg" fullWidth disabled={submitting}>
          <UserPlus className="size-4" aria-hidden />
          Create an Account
        </Button>
      </Link>

      <div className="flex items-start gap-2 rounded-lg border border-forest-500/20 bg-deep-900/60 px-3.5 py-2.5 text-xs text-muted">
        <Lock className="mt-0.5 size-3.5 shrink-0 text-gold-400" aria-hidden />
        <span>
          Demo Credentials —{" "}
          <code className="rounded bg-deep-800 px-1 py-0.5 font-mono text-parchment-300">
            {AuthService.demoCredentials.email}
          </code>{" "}
          /{" "}
          <code className="rounded bg-deep-800 px-1 py-0.5 font-mono text-parchment-300">
            {AuthService.demoCredentials.password}
          </code>
        </span>
      </div>
    </form>
  );
}