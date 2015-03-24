#!/bin/bash

# Notes:
# Static builds in fedora requires glibc-static, libstdc++-static and zlib-static.
# Cross compiling to 32 bit on fedora x86_64 requires gcc-c++.i686
# glibc-static.i686 libstdc++-static.i686 and zlib-static.i686
# libgcc.i686 glibc-devel.i686 libstdc++-devel.i686.
# Cross compiling to 32 bit on ubuntu x86_64 requires g++-multilib and libz-dev:i386.

ver=${1:-2.6.1}
host=$(uname -s -m | tr 'A-Z ' 'a-z-' | sed 's/darwin/mac_os_x/')

build() {
    local sys=$1; shift # Remaining args passed directly to build

    local build=target/build-$sys
    local inst=$PWD/target/inst-$sys/$ver
    local makeopts="LDFLAGS=-all-static" # Very static
    # local makeopts="LDFLAGS=-static-libgcc CXXFLAGS=-static-libstdc++" # Quite static

    echo Building protoc $ver for $sys
    wget -nc https://github.com/google/protobuf/releases/download/v$ver/protobuf-$ver.tar.bz2
    rm -rf "$build" "$inst"
    mkdir -p "$build"
    tar -C "$build" -jxf protobuf-$ver.tar.bz2
    (cd $build/protobuf-$ver && \
	env "$@" ./configure --disable-shared --prefix=$inst && \
	make $makeopts -j4 install) || exit

    echo
    echo '----------------------------------------------------------------------'

    local instdir=src/main/binaries/$ver
    mkdir -p $instdir
    local final=$instdir/protoc-$ver-$sys
    cp -a $inst/bin/protoc $final
    strip $final
    ls -l $final
    file $final
    $final --version
}

build $host
if [ "$host" = "linux-x86_64" ]; then
    build "linux-x86" CXXFLAGS=-m32 CFLAGS=-m32
fi
