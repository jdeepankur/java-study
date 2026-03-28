#!/usr/bin/env bash
# check.sh — run .in/.out tests against your Java source files directly
# Requires Java 11+
#
# Usage:
#   ./check.sh          run all questions
#   ./check.sh q3       run one question only

PASS=0; FAIL=0

GRN="\033[32m"; RED="\033[31m"; YEL="\033[33m"; BLD="\033[1m"; RST="\033[0m"

run_q() {
    local q=$1
    local cls="Q${q:1}"   # q1 -> Q1
    local src="${cls}.java"

    if [ ! -f "$src" ]; then
        echo -e "  ${YEL}SKIP${RST}  ${q}: ${src} not found"
        return
    fi

    local any=0
    for infile in ${q}/test*.in; do
        [ -f "$infile" ] || continue
        local outfile="${infile%.in}.out"
        local testname=$(basename "${infile%.in}")
        any=1

        actual=$(java "$src" < "$infile" 2>/dev/null)
        expected=$(cat "$outfile")

        if [ "$q" = "q5" ]; then
            ok=true
            for tok in $expected; do
                echo "$actual" | grep -qF "$tok" || ok=false
            done
            if $ok; then
                echo -e "  ${GRN}PASS${RST}  ${q}/${testname}"
                PASS=$((PASS+1))
            else
                echo -e "  ${RED}FAIL${RST}  ${q}/${testname}"
                echo -e "       expected tokens: $expected"
                echo -e "       got:             $actual"
                FAIL=$((FAIL+1))
            fi
        elif [ "$q" = "q1" ]; then
            actual_sorted=$(echo "$actual" | tr ' ' '\n' | sort -n | tr '\n' ' ' | sed 's/ $//')
            expected_sorted=$(echo "$expected" | tr ' ' '\n' | sort -n | tr '\n' ' ' | sed 's/ $//')
            if [ "$actual_sorted" = "$expected_sorted" ]; then
                echo -e "  ${GRN}PASS${RST}  ${q}/${testname}"
                PASS=$((PASS+1))
            else
                echo -e "  ${RED}FAIL${RST}  ${q}/${testname}"
                echo -e "       expected: $expected"
                echo -e "       got:      $actual"
                FAIL=$((FAIL+1))
            fi
        else
            if [ "$actual" = "$expected" ]; then
                echo -e "  ${GRN}PASS${RST}  ${q}/${testname}"
                PASS=$((PASS+1))
            else
                echo -e "  ${RED}FAIL${RST}  ${q}/${testname}"
                diff <(echo "$expected") <(echo "$actual") | sed 's/^/       /'
                FAIL=$((FAIL+1))
            fi
        fi
    done

    [ $any -eq 0 ] && echo -e "  ${YEL}WARN${RST}  no .in files found in ${q}/"
}

echo ""
echo -e "${BLD}TravelFusion Practice — Test Runner${RST}"
echo "────────────────────────────────────"

if [ -n "$1" ]; then
    run_q "$1"
else
    for q in q1 q2 q3 q4 q5 q6 q7 q8; do
        run_q "$q"
    done
fi

echo "────────────────────────────────────"
echo -e "${BLD}${GRN}${PASS} passed${RST}  ${RED}${FAIL} failed${RST}"
echo ""
[ $FAIL -eq 0 ]
