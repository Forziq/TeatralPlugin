#!/bin/sh
# Moves the Claude Code setup (agents, commands, hooks, dashboard, skill, settings) into .claude/.
# Run once from the project folder: sh scripts/install-harness.sh
set -e
cd "$(dirname "$0")/.."
if [ ! -d claude-setup ]; then
  echo "Nothing to install: claude-setup/ isn't here (already installed?)."
  exit 0
fi
mkdir -p .claude
if [ -f .claude/settings.json ]; then
  cp .claude/settings.json ".claude/settings.json.bak-$(date +%Y%m%d%H%M%S)"
  echo "Backed up your existing .claude/settings.json"
fi
cp -R claude-setup/. .claude/
rm -rf claude-setup
chmod +x scripts/*.sh 2>/dev/null || true
echo "Installed. Files now in .claude/:"
find .claude -type f -not -name '*.bak-*' | sort
