#!/usr/bin/env bash
set -u
F=/tmp/rajn_stat_fa.pdf
echo "===PDF21_STRUCTURE==="
echo "HDR=$(head -c 8 "$F" | tr -d '\0' | cat -v)"
echo "TAIL=$(tail -c 16 "$F" | tr -d '\0' | cat -v)"
echo "EOF_CNT=$(grep -c '%%EOF' "$F")"
echo "PRODUCER=$(strings "$F" 2>/dev/null | grep -aoE 'OpenPDF [0-9.]+' | head -1)"
echo "===PDF21_TEXT_QUERY==="
echo "S_REJNEETI=$(strings "$F" 2>/dev/null | grep -ci 'rajneeti')"
echo "S_STATISTICS=$(strings "$F" 2>/dev/null | grep -ci 'statistics')"
echo "S_LEADERBOARD=$(strings "$F" 2>/dev/null | grep -ci 'leaderboard')"
echo "S_MATCHHISTORY=$(strings "$F" 2>/dev/null | grep -ci 'match history')"
echo "S_MATCHES=$(strings "$F" 2>/dev/null | grep -ci -E 'total.*match|Total Matches')"
echo "S_WINRATE=$(strings "$F" 2>/dev/null | grep -ci 'win rate')"
echo "===PDF21_DIFF(object names)==="
strings "$F" 2>/dev/null | grep -aoE '/\(Rajneeti Statistics PDF\)|/Subtype /Type1' | sort -u
echo "===DONE21==="
