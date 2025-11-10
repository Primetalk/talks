# THEME_PATH="$(cd ../beamer/theme/"$(dirname -- "$1")" >/dev/null; pwd -P)/$(basename -- "$1")"
#export TEXINPUTS=.:$THEME_PATH:
#       -F mermaid-filter \
#       --lua-filter $THEME_PATH/lua-filters/minted.lua \
#       --lua-filter $THEME_PATH/lua-filters/list-table.lua \
export TEXMFHOME=$HOME/texmf
#       --pdf-engine=xelatex -V mainfont="DejaVu Sans"

date
pandoc --to=beamer --output=../2025-11-22-proof-vs-test.pdf \
       --pdf-engine=xelatex \
       --slide-level=1 \
       --pdf-engine-opt=-output-directory=temp \
       --pdf-engine-opt=--shell-escape \
       --no-highlight --verbose \
       proof-vs-test.md
date
