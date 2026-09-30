import { test } from "node:test";
import assert from "node:assert/strict";
import { ACTIONS, ACTION_MAP } from "@/lib/game/actions";
import {
  CHARACTERS,
  CHARACTER_MAP,
  CHARACTER_ENGLISH_NAMES,
  getCharacterDisplayName,
} from "@/lib/game/characters";

test("Character names - standard English character names", () => {
  assert.equal(CHARACTER_ENGLISH_NAMES.minister, "Minister");
  assert.equal(CHARACTER_ENGLISH_NAMES.ghatok, "Assassin");
  assert.equal(CHARACTER_ENGLISH_NAMES.dalal, "Broker");
  assert.equal(CHARACTER_ENGLISH_NAMES.amla, "Bureaucrat");
  assert.equal(CHARACTER_ENGLISH_NAMES.goyenda, "Detective");

  // Display name helper with/without Bengali
  assert.equal(getCharacterDisplayName("minister"), "Minister");
  assert.equal(getCharacterDisplayName("minister", true), "Minister (মন্ত্রী)");
  assert.equal(getCharacterDisplayName("ghatok"), "Assassin");
  assert.equal(getCharacterDisplayName("ghatok", true), "Assassin (ঘাতক)");
  assert.equal(getCharacterDisplayName("dalal"), "Broker");
  assert.equal(getCharacterDisplayName("dalal", true), "Broker (দালাল)");
  assert.equal(getCharacterDisplayName("amla"), "Bureaucrat");
  assert.equal(getCharacterDisplayName("amla", true), "Bureaucrat (আমলা)");
  assert.equal(getCharacterDisplayName("goyenda"), "Detective");
  assert.equal(getCharacterDisplayName("goyenda", true), "Detective (গোয়েন্দা)");
});

test("Action definitions - all 7 actions have valid English and Bengali names", () => {
  assert.equal(ACTIONS.length, 7);

  const expected: Record<string, { en: string; bn: string }> = {
    income: { en: "Income", bn: "আয়" },
    foreign_aid: { en: "Foreign Aid", bn: "বিদেশি অনুদান" },
    tax: { en: "Tax", bn: "কর আদায়" },
    steal: { en: "Steal", bn: "চুরি" },
    exchange: { en: "Exchange", bn: "কার্ড বদল" },
    assassinate: { en: "Assassinate", bn: "সরিয়ে দেওয়া" },
    coup: { en: "Coup", bn: "ক্ষমতা দখল" },
  };

  for (const [id, exp] of Object.entries(expected)) {
    const act = ACTION_MAP[id];
    assert.ok(act, `Action ${id} exists`);
    assert.equal(act.nameEn, exp.en);
    assert.equal(act.nameBn, exp.bn);
  }
});
