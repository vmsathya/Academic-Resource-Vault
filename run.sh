#!/bin/bash
set -e

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$DIR"

echo "Building Academic Resource Vault..."
mkdir -p bin
javac -encoding UTF-8 -cp "lib/*" -d bin $(find src -name "*.java")

echo "Starting Academic Resource Vault server..."
exec java -cp "bin:lib/*" com.vault.Main "$@"
