#!/bin/bash

# Ensure ImageMagick is installed
if ! command -v convert &> /dev/null
then
    echo "ImageMagick not found. Installing..."
    sudo apt-get update && sudo apt-get install -y imagemagick
fi

IMAGE_FILE=$1
if [ -z "$IMAGE_FILE" ]; then
    echo "Usage: ./generate_icons.sh <path_to_image>"
    return 1
fi

if [ ! -f "$IMAGE_FILE" ]; then
    echo "File $IMAGE_FILE not found."
    return 1
fi

MODULES=("kasir-android/app-store" "kasir-android/app-admin")

for MODULE in "${MODULES[@]}"; do
    echo "Generating icons for $MODULE..."

    RES_DIR="$MODULE/src/main/res"
    mkdir -p "$RES_DIR/mipmap-mdpi"
    mkdir -p "$RES_DIR/mipmap-hdpi"
    mkdir -p "$RES_DIR/mipmap-xhdpi"
    mkdir -p "$RES_DIR/mipmap-xxhdpi"
    mkdir -p "$RES_DIR/mipmap-xxxhdpi"

    # mdpi: 48x48
    convert "$IMAGE_FILE" -resize 48x48\! "$RES_DIR/mipmap-mdpi/ic_launcher.png"
    convert "$IMAGE_FILE" -resize 48x48\! "$RES_DIR/mipmap-mdpi/ic_launcher_round.png"

    # hdpi: 72x72
    convert "$IMAGE_FILE" -resize 72x72\! "$RES_DIR/mipmap-hdpi/ic_launcher.png"
    convert "$IMAGE_FILE" -resize 72x72\! "$RES_DIR/mipmap-hdpi/ic_launcher_round.png"

    # xhdpi: 96x96
    convert "$IMAGE_FILE" -resize 96x96\! "$RES_DIR/mipmap-xhdpi/ic_launcher.png"
    convert "$IMAGE_FILE" -resize 96x96\! "$RES_DIR/mipmap-xhdpi/ic_launcher_round.png"

    # xxhdpi: 144x144
    convert "$IMAGE_FILE" -resize 144x144\! "$RES_DIR/mipmap-xxhdpi/ic_launcher.png"
    convert "$IMAGE_FILE" -resize 144x144\! "$RES_DIR/mipmap-xxhdpi/ic_launcher_round.png"

    # xxxhdpi: 192x192
    convert "$IMAGE_FILE" -resize 192x192\! "$RES_DIR/mipmap-xxxhdpi/ic_launcher.png"
    convert "$IMAGE_FILE" -resize 192x192\! "$RES_DIR/mipmap-xxxhdpi/ic_launcher_round.png"

    echo "Done for $MODULE."
done

echo "All icons generated successfully."
