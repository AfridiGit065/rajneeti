import { test } from "node:test";
import assert from "node:assert/strict";
import { existsSync, readFileSync } from "node:fs";
import { join } from "node:path";
import { CARD_BACK_PATH, CHARACTERS } from "./characters.ts";

const PUBLIC_DIR = join(process.cwd(), "public");
const cardAsset = (path: string) => join(PUBLIC_DIR, path.replace(/^\//, ""));

test("card back path is the canonical public path", () => {
  assert.equal(CARD_BACK_PATH, "/assets/cards/back.png");
});

test("the card back asset exists where the browser will request it", () => {
  const file = cardAsset(CARD_BACK_PATH);
  assert.ok(existsSync(file), `missing asset: ${file}`);

  // Real PNG signature, so the request cannot 404 or be served as a stub.
  const head = readFileSync(file).subarray(0, 8);
  assert.deepEqual(
    [...head],
    [0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a],
  );
});

test("every character face referenced by the roster is present", () => {
  for (const character of CHARACTERS) {
    const file = cardAsset(character.imagePath);
    assert.ok(existsSync(file), `missing face for ${character.id}: ${file}`);
  }
});

test("no character face reuses the card back", () => {
  // A face and a back sharing a path would leak a hidden identity.
  for (const character of CHARACTERS) {
    assert.notEqual(character.imagePath, CARD_BACK_PATH);
  }
});
