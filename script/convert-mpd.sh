#!/bin/bash

mp4_dir="$1"
bento4="$2"

if [ ! -d "$mp4_dir" ]; then
    echo "mp4 direction does not exist: $mp4_dir"
    exit 1;
fi

if [ ! -d "$bento4" ]; then
    echo "Bento4 directory does not exist: $bento4"
    exit 1;
fi

for mp4 in $mp4_dir/*.mp4; do
    name=${mp4%.mp4}
    "$bento4/mp4fragment" "$mp4" "$name-fragment.mp4"
    "$bento4/mp4dash" "$name-fragment.mp4" -f -o "$name"
    rm "$name-fragment.mp4"
    rm "$mp4"
done