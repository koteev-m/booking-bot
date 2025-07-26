#!/usr/bin/env bash
# Removes every zero-byte regular file recursively (except .git folder)
find . -type f -empty ! -path "./.git/*" -print -delete

# Usage:
#   ./clean_empty_files.sh
#   git add -u
#   git commit -m "chore(repo): remove empty placeholder files and update .gitignore"
