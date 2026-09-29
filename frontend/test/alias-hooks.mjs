/**
 * Module resolution hook for `node --test`.
 *
 * The app is bundled by Next/Turbopack, which understands the `@/*` path alias
 * declared in tsconfig. The bare `node --test` runner does not, so a test that
 * imports a module which itself uses `@/...` fails with ERR_MODULE_NOT_FOUND.
 *
 * This hook teaches the runner the same mapping, which keeps frontend logic
 * unit-testable without pulling in a bundler or a test framework. It only
 * rewrites the `@/` prefix and leaves every other specifier untouched.
 */
import { statSync } from "node:fs";
import { dirname, resolve as resolvePath } from "node:path";
import { fileURLToPath, pathToFileURL } from "node:url";

const projectRoot = resolvePath(dirname(fileURLToPath(import.meta.url)), "..");
const srcRoot = resolvePath(projectRoot, "src");

function firstExistingFile(base) {
  for (const candidate of [base, `${base}.ts`, `${base}.tsx`]) {
    try {
      if (statSync(candidate).isFile()) return candidate;
    } catch {
      // not a file; try the next extension
    }
  }
  return null;
}

export async function resolve(specifier, context, nextResolve) {
  if (!specifier.startsWith("@/")) {
    return nextResolve(specifier, context);
  }

  const target = firstExistingFile(resolvePath(srcRoot, specifier.slice(2)));
  if (!target) {
    return nextResolve(specifier, context);
  }

  return nextResolve(pathToFileURL(target).href, context);
}
