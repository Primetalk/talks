THEME_PATH="$(cd ../beamer/theme/"$(dirname -- "$1")" >/dev/null; pwd -P)/$(basename -- "$1")"
export TEXINPUTS=.:$THEME_PATH:

date
pandoc --to=beamer --output=proof-vs-test.pdf \
       --pdf-engine=lualatex \
       --slide-level=1 \
       --pdf-engine-opt=-output-directory=temp \
       --pdf-engine-opt=--shell-escape \
       -F mermaid-filter \
       --lua-filter $THEME_PATH/lua-filters/minted.lua \
       --lua-filter $THEME_PATH/lua-filters/list-table.lua \
       --no-highlight --verbose \
       proof-vs-test.md
date
