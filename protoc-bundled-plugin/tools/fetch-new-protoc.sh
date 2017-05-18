#!/bin/bash

# May be used to fetch a new version of the protoc binaries.

set -e

export PB_VERSION=3.3.0
export BUNDLE_BIN_DIR="$PWD"/protoc-bundled-plugin/src/main/binaries/"$PB_VERSION"

echo "Fetching $PB_VERSION into $BUNDLE_BIN_DIR"

if ! [ -d "$(dirname "$BUNDLE_BIN_DIR")" ]; then
  echo "$0 must be run from project root"
  exit 1
fi

mkdir -p "$BUNDLE_BIN_DIR"

curl -L -O https://github.com/google/protobuf/releases/download/v"$PB_VERSION"/protoc-"$PB_VERSION"-linux-x86_32.zip
curl -L -O https://github.com/google/protobuf/releases/download/v"$PB_VERSION"/protoc-"$PB_VERSION"-linux-x86_64.zip
curl -L -O https://github.com/google/protobuf/releases/download/v"$PB_VERSION"/protoc-"$PB_VERSION"-osx-x86_64.zip
curl -L -O https://github.com/google/protobuf/releases/download/v"$PB_VERSION"/protoc-"$PB_VERSION"-osx-x86_32.zip
curl -L -O https://github.com/google/protobuf/releases/download/v"$PB_VERSION"/protoc-"$PB_VERSION"-win32.zip

unzip -p protoc-"$PB_VERSION"-linux-x86_32.zip bin/protoc \
  > "$BUNDLE_BIN_DIR"/protoc-"$PB_VERSION"-linux-x86
unzip -p protoc-"$PB_VERSION"-linux-x86_64.zip bin/protoc \
  > "$BUNDLE_BIN_DIR"/protoc-"$PB_VERSION"-linux-x86_64
unzip -p protoc-"$PB_VERSION"-osx-x86_64.zip bin/protoc \
  > "$BUNDLE_BIN_DIR"/protoc-"$PB_VERSION"-mac_os_x-x86_64
unzip -p protoc-"$PB_VERSION"-osx-x86_32.zip bin/protoc \
  > "$BUNDLE_BIN_DIR"/protoc-"$PB_VERSION"-mac_os_x-x86
unzip -p protoc-"$PB_VERSION"-win32.zip bin/protoc.exe \
  > "$BUNDLE_BIN_DIR"/protoc-"$PB_VERSION"-win32-x86.exe

chmod +x "$BUNDLE_BIN_DIR"/protoc-*
chmod -x "$BUNDLE_BIN_DIR"/protoc-*.exe

rm *.zip

echo Do not forget to update the POM to add '<artifact>' entries for
echo the new binaries.
