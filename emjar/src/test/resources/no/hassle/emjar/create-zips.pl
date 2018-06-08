#!/usr/bin/perl

use strict;
use Archive::Zip::SimpleZip qw(:zip_method);

our $DEFAULT_MANIFEST = "Manifest-Version: 1.0\r\n\r\n";
our $TIMESTAMP = 1400000000;

$Archive::Zip::SimpleZip::PARAMS{"time"} ||= [IO::Compress::Base::Common::Parse_any, undef];

sub createZip {
    my ($name, $initargs, $entryargs, $oversize, $method) = @_;
    my $buf = "";
    my $inner = Archive::Zip::SimpleZip->new(\$buf, %$initargs)
	or die "Unable to create Zip";
    $inner->addString("X-EmJar-Test: inner\r\n".$DEFAULT_MANIFEST,
		      Name => "META-INF/MANIFEST.MF",
		      Time => $TIMESTAMP + 1);
    if ($oversize) {
	$inner->addString("\0" x (128 * 1024 * 1024 + 1),
			  Name => "oversize",
			  Time => $TIMESTAMP + 2);
    }
    $inner->addString($name,
		      Name => "entry-$name.txt",
		      Time => $TIMESTAMP + 3,
		      %$entryargs);
    $inner->add("dir-entry",
		Stream => 0);
    $inner->close();

    my $outer = Archive::Zip::SimpleZip->new("bundle-$name.jar", %$initargs)
	or die "Unable to create Zip";
    $outer->addString("X-EmJar-Test: bundle\r\n".$DEFAULT_MANIFEST,
		      Name => "META-INF/MANIFEST.MF",
		      Time => $TIMESTAMP + 4);
    $outer->addString($buf,
		      Name => "lib-$name.jar",
		      Method => ($method || ZIP_CM_STORE),
		      Time => $TIMESTAMP + 5);
    $outer->close();
}

mkdir("dir-entry");
utime($TIMESTAMP + 6, $TIMESTAMP + 6, "dir-entry");

for my $m (0, 1) {
    for my $s (0, 1) {
	for my $l (0, 1) {
	    for my $c (0, 1) {
		my $name = ($m ? "M" : "m")
		    .($s ? "S" : "s")
		    .($l ? "L" : "l")
		    .($c ? "C" : "c");
		createZip($name, {Minimal => $m, Stream => $s, Zip64 => $l}, {Comment => $c});
	    }
	}
    }
}

createZip("s-large", {Stream => 0}, {}, 1);
createZip("S-large", {Stream => 1}, {}, 1);
createZip("Z-large", {}, {}, 1, ZIP_CM_DEFLATE);
createZip("æøå😱 %&;*+`\"\\-weird", {}, {});
createZip("signed", {}, {});

rmdir("dir-entry");

### Creating the signed example is manual process at this point:
# unzip bundle-signed.jar lib-signed.jar
# rm bundle-signed.jar
# keytool -genkey -keyalg RSA -alias testkey -keystore teststore -validity 36524 -storepass password
# jarsigner -keystore teststore -storepass password -verbose lib-signed.jar testkey
# jar -c0f bundle-signed.jar lib-signed.jar
# rm teststore lib-signed.jar
