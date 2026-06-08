#!/usr/bin/env bash
# Captures the build-output baseline used by migration gate G1 (docs/05 §2).
# Usage: ./capture-baseline.sh <buildOutputDir> <label>
# Run from core-leo-cdp/. Writes docs/migration-baseline/<label>-*.txt
set -euo pipefail
OUT="${1:?buildOutputDir required}"
LABEL="${2:?label required (e.g. gradle694 / gradle9)}"
DIR="$(cd "$(dirname "$0")" && pwd)"

# 1. output tree (paths only, sorted)
(cd "$OUT" && find . -type f | sort) > "$DIR/$LABEL-tree.txt"

# 2. deps jar set
ls "$OUT/deps" | sort > "$DIR/$LABEL-deps.txt"

# 3. manifests of the starter jars (strip the volatile timestamped version)
: > "$DIR/$LABEL-manifests.txt"
for j in "$OUT"/leo-*-starter-*.jar; do
  echo "== $(basename "$j") ==" >> "$DIR/$LABEL-manifests.txt"
  unzip -p "$j" META-INF/MANIFEST.MF | tr -d '\r' \
    | grep -vE '^Implementation-Version' >> "$DIR/$LABEL-manifests.txt"
done

# 4. minified JS checksums (committed + CDN-published output)
{ sha256sum public/js/leo-observer/*.js 2>/dev/null;
  find resources/app-templates/leocdp-admin/common-resources-min -name '*.js' -print0 2>/dev/null \
    | xargs -0 sha256sum 2>/dev/null; } | sed 's|\\|/|g' | sort -k2 > "$DIR/$LABEL-js-sha256.txt"

# 5. minified JS checksums EXCLUDING the volatile "// Version: ..." first line
{ for f in public/js/leo-observer/*.js $(find resources/app-templates/leocdp-admin/common-resources-min -name '*.js' 2>/dev/null); do
    printf '%s  %s\n' "$(tail -n +2 "$f" | sha256sum | cut -d' ' -f1)" "$f";
  done; } | sed 's|\\|/|g' | sort -k2 > "$DIR/$LABEL-js-sha256-noversion.txt"

# 6. bytecode major version of a starter class
unzip -p "$OUT"/leo-main-starter-*.jar leotech/starter/MainHttpStarter.class | od -A n -t u1 -N 8 \
  | awk '{print "MainHttpStarter class-file major version: " $7*256+$8}' > "$DIR/$LABEL-bytecode.txt"

echo "Baseline '$LABEL' captured in $DIR"
