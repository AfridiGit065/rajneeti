/**
 * Registers the `@/*` alias resolver for the bare `node --test` runner.
 * Loaded through `--import` from the npm `test` script.
 */
import { register } from "node:module";

register("./alias-hooks.mjs", import.meta.url);
