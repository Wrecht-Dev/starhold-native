#!/usr/bin/env bash
# Regenerate launcher icons, TV banner and desktop icon from tools/icon.svg.
# Requires ImageMagick 7 (`magick`).
set -euo pipefail
cd "$(dirname "$0")/.."

magick -background none tools/icon.svg -resize 1024x1024 tools/icon-1024.png

for pair in mdpi:48 hdpi:72 xhdpi:96 xxhdpi:144 xxxhdpi:192; do
  density=${pair%%:*}
  size=${pair##*:}
  magick tools/icon-1024.png -resize "${size}x${size}" \
    "composeApp/src/androidMain/res/mipmap-${density}/ic_launcher.png"
done

magick -size 320x180 xc:'#101010' \
  \( tools/icon-1024.png -resize 64x64 \) -geometry +24+58 -composite \
  -font composeApp/src/commonMain/composeResources/font/manrope_800.ttf \
  -pointsize 40 -kerning 7 -fill '#F5F5F5' -gravity West -annotate +104+0 'STARHOLD' \
  -fill '#6A6CF0' -draw "rectangle 104,132 300,137" \
  composeApp/src/androidMain/res/drawable/tv_banner.png

magick tools/icon-1024.png -resize 512x512 composeApp/src/desktopMain/resources/icon.png
magick tools/icon-1024.png -resize 1024x1024 iosApp/Starhold/Assets.xcassets/AppIcon.appiconset/icon-1024.png
echo "icons regenerated"
