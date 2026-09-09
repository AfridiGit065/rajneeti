export const USERNAME_MIN = 3;
export const USERNAME_MAX = 24;
export const PASSWORD_MIN = 6;
export const PASSWORD_MAX = 128;
export const EMAIL_MAX = 254;

export const USERNAME_PATTERN = /^[a-zA-Z0-9._]+$/;
export const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/;

export interface LoginValues {
  email: string;
  password: string;
}

export interface RegisterValues {
  username: string;
  email: string;
  password: string;
  confirm: string;
}

function isBlank(value: string): boolean {
  return !value.trim();
}

export function validateUsername(value: string): string | null {
  if (isBlank(value)) return "ইউজারনেম দিন।";
  const trimmed = value.trim();
  if (trimmed.length < USERNAME_MIN)
    return `ইউজারনেম কমপক্ষে ${USERNAME_MIN} অক্ষরের হতে হবে।`;
  if (trimmed.length > USERNAME_MAX)
    return `ইউজারনেম সর্বোচ্চ ${USERNAME_MAX} অক্ষরের হতে পারে।`;
  if (!USERNAME_PATTERN.test(trimmed))
    return "শুধু ইংরেজি অক্ষর, সংখ্যা, '.' এবং '_' ব্যবহার করা যাবে।";
  if (/^\d+$/.test(trimmed)) return "ইউজারনেম শুধু সংখ্যা হতে পারবে না।";
  return null;
}

export function validateEmail(value: string): string | null {
  if (isBlank(value)) return "ইমেইল ঠিকানা দিন।";
  const trimmed = value.trim();
  if (trimmed.length > EMAIL_MAX) return "ইমেইল ঠিকানা খুব দীর্ঘ।";
  if (!EMAIL_PATTERN.test(trimmed)) return "সঠিক ইমেইল ঠিকানা দিন।";
  return null;
}

export function validatePassword(value: string): string | null {
  if (isBlank(value)) return "পাসওয়ার্ড দিন।";
  if (value.length < PASSWORD_MIN)
    return `পাসওয়ার্ড কমপক্ষে ${PASSWORD_MIN} অক্ষরের হতে হবে।`;
  if (value.length > PASSWORD_MAX)
    return `পাসওয়ার্ড সর্বোচ্চ ${PASSWORD_MAX} অক্ষরের হতে পারে।`;
  if (!/[A-Za-z]/.test(value) || !/\d/.test(value))
    return "পাসওয়ার্ডে অন্তত একটি অক্ষর এবং একটি সংখ্যা থাকতে হবে।";
  return null;
}

export function validateConfirm(value: string, password: string): string | null {
  if (isBlank(value)) return "পাসওয়ার্ড আবার লিখুন।";
  if (value !== password) return "দুই পাসওয়ার্ড মিলছে না।";
  return null;
}

export type LoginFieldErrors = Partial<Record<keyof LoginValues, string>>;

export function validateLogin(values: LoginValues): LoginFieldErrors {
  const errors: LoginFieldErrors = {};
  const emailError = validateEmail(values.email);
  if (emailError) errors.email = emailError;
  const passwordError = validatePassword(values.password);
  if (passwordError) errors.password = passwordError;
  return errors;
}

export type RegisterFieldErrors = Partial<Record<keyof RegisterValues, string>>;

export function validateRegister(values: RegisterValues): RegisterFieldErrors {
  const errors: RegisterFieldErrors = {};
  const usernameError = validateUsername(values.username);
  if (usernameError) errors.username = usernameError;
  const emailError = validateEmail(values.email);
  if (emailError) errors.email = emailError;
  const passwordError = validatePassword(values.password);
  if (passwordError) errors.password = passwordError;
  const confirmError = validateConfirm(values.confirm, values.password);
  if (confirmError) errors.confirm = confirmError;
  return errors;
}