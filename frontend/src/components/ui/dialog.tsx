"use client";

import type { ReactNode } from "react";
import { Modal } from "./modal";

interface DialogProps {
  open: boolean;
  onClose: () => void;
  title: string;
  subtitle?: string;
  icon?: ReactNode;
  children: ReactNode;
  footer?: ReactNode;
}

/** Dialog = Modal with a footer-aware layout for decision prompts and info panels. */
export function Dialog({
  open,
  onClose,
  title,
  subtitle,
  icon,
  children,
  footer,
}: DialogProps) {
  return (
    <Modal open={open} onClose={onClose} title={title} subtitle={subtitle}>
      {icon ? (
        <div className="mb-4 flex items-center gap-3">
          <div className="flex size-11 items-center justify-center rounded-xl border border-gold-500/30 bg-gold-500/10 text-gold-300">
            {icon}
          </div>
          {subtitle ? <p className="text-sm text-muted">{subtitle}</p> : null}
        </div>
      ) : null}
      <div className="text-sm leading-relaxed text-parchment-300">{children}</div>
      {footer ? (
        <div className="mt-6 flex flex-wrap items-center justify-end gap-3">{footer}</div>
      ) : null}
    </Modal>
  );
}