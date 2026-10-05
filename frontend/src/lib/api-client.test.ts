import { test } from "node:test";
import assert from "node:assert/strict";
import { normalizeApiBase } from "@/lib/api-client";

test("a bare origin is kept as-is", () => {
  assert.equal(normalizeApiBase("http://localhost:8080"), "http://localhost:8080");
  assert.equal(
    normalizeApiBase("http://10.106.103.227:8080"),
    "http://10.106.103.227:8080",
  );
});

test("a trailing slash does not change the base", () => {
  assert.equal(normalizeApiBase("http://localhost:8080/"), "http://localhost:8080");
  assert.equal(normalizeApiBase("http://localhost:8080///"), "http://localhost:8080");
});

test("a trailing /api is stripped so paths cannot double up", () => {
  assert.equal(normalizeApiBase("http://localhost:8080/api"), "http://localhost:8080");
  assert.equal(normalizeApiBase("http://localhost:8080/api/"), "http://localhost:8080");
  assert.equal(normalizeApiBase("http://10.0.0.5:8080/API"), "http://10.0.0.5:8080");
});

test("an unset value falls back to the local backend", () => {
  assert.equal(normalizeApiBase(undefined), "http://localhost:8080");
  assert.equal(normalizeApiBase(""), "http://localhost:8080");
});

test("only the trailing /api segment is removed, never a path prefix", () => {
  assert.equal(
    normalizeApiBase("https://api.example.com/api"),
    "https://api.example.com",
  );
  assert.equal(
    normalizeApiBase("https://example.com/api/v1"),
    "https://example.com/api/v1",
  );
});

test("registration resolves to a public path, never /api/api", () => {
  const base = normalizeApiBase("http://localhost:8080/api");
  assert.equal(`${base}/api/auth/register`, "http://localhost:8080/api/auth/register");
});