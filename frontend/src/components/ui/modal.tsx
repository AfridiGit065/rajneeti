"use client";

import { useEffect, useId, useRef, type ReactNode } from "react";
import { createPortal } from "react-dom";
import { cn } from "@/lib/cn";
import { X } from "./icons";

interface ModalProps {
  open: boolean;
  onClose: () => void;
  title?: string;
  subtitle?: string;
  children: ReactNode;
  size?: "sm" | "md" | "lg" | "xl";
  className?: string;
  showCloseButton?: boolean;
  labelledBy?: string;
}

const SIZES = {
  sm: "max-w-sm",
  md: "max-w-lg",
  lg: "max-w-2xl",
  xl: "max-w-4xl",
};

export function Modal({
  open,
  onClose,
  title,
  subtitle,
  children,
  size = "md",
  className,
  showCloseButton = true,
}: ModalProps) {
  const panelRef = useRef<HTMLDivElement>(null);
  const titleId = `modal-title-${useId()}`;

  useEffect(() => {
    if (!open) return;
    const prevOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    panelRef.current?.focus();
    const handleKey = (event: KeyboardEvent) => {
      if (event.key === "Escape") onClose();
    };
    window.addEventListener("keydown", handleKey);
    return () => {
      document.body.style.overflow = prevOverflow;
      window.removeEventListener("keydown", handleKey);
    };
  }, [open, onClose]);

  if (!open) return null;

  return createPortal(
    <div
      className="fixed inset-0 z-50 flex items-end justify-center p-4 sm:items-center"
      role="dialog"
      aria-modal="true"
      aria-labelledby={title ? titleId : undefined}
    >
      <button
        type="button"
        aria-label="ব্যাকড্রপ বন্ধ করুন"
        className="absolute inset-0 bg-deep-950/80 backdrop-blur-sm animate-fade-in cursor-default"
        onClick={onClose}
        tabIndex={-1}
      />
      <div
        ref={panelRef}
        tabIndex={-1}
        className={cn(
          "relative w-full outline-none",
          "animate-zoom-in rounded-2xl border border-gold-500/25 bg-deep-900/95 panel-emboss panel-texture",
          "max-h-[90vh] overflow-y-auto",
          SIZES[size],
          className,
        )}
      >
        {title ? (
          <div className="sticky top-0 z-10 border-b border-forest-500/20 bg-deep-900/95 px-6 py-4 backdrop-blur">
            <div className="flex items-start justify-between gap-4">
              <div>
                <h2 id={titleId} className="font-bengali text-xl font-semibold text-ivory">
                  {title}
                </h2>
                {subtitle ? <p className="mt-0.5 text-sm text-muted">{subtitle}</p> : null}
              </div>
              {showCloseButton ? (
                <button
                  type="button"
                  aria-label="বন্ধ করুন"
                  onClick={onClose}
                  className="rounded-lg p-1 text-muted transition-colors hover:bg-deep-700/60 hover:text-ivory focus-visible:outline-2 focus-visible:outline-gold-400"
                >
                  <X className="size-5" aria-hidden />
                </button>
              ) : null}
            </div>
          </div>
        ) : null}
        <div className="px-6 py-5">{children}</div>
      </div>
    </div>,
    document.body,
  );
}