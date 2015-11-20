#!/usr/bin/perl
# Copyright (c) Apps4Av Inc. (apps4av@gmail.com)
# All rights reserved.
#
# Redistribution and use in source and binary forms, with or without
# modification, are permitted provided that the following conditions
# are met:
#
# * Redistributions of source code must retain the above copyright
#   notice, this list of conditions and the following disclaimer.
# * Redistributions in binary form must reproduce the above copyright
#   notice, this list of conditions and the following disclaimer in
#   the documentation and/or other materials provided with the
#   distribution.
#
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
# "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
# LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
# A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
# HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
# INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
# BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
# OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED
# AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
# LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
# WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
# POSSIBILITY OF SUCH DAMAGE.
#
#zkhan


# create zip manifest for Avare
# use as ./zip.pl chartname.zip cycle
# e.g. ./zip.pl ONCNewZealand.zip 1512

use Archive::Zip qw( :ERROR_CODES :CONSTANTS );

# Add manifest to zip file
my $zip = Archive::Zip->new();

$zip->read($ARGV[0]) == AZ_OK or die "read error\n";

# remove path and extension
$ARGV[0] =~ s{\.[^.]+$}{};
$ARGV[0] =~ s{.*/}{};

# Remove existing manifest
$zip->removeMember($ARGV[0]);

# Add version
my $contents = "$ARGV[1]\n";

# Make file content
foreach my $member ($zip->memberNames()) {
    $contents .= $member . "\n";
}

# Put in manifest
my $f = $zip->addString($contents, $ARGV[0]);
$f->desiredCompressionMethod(COMPRESSION_DEFLATED);

# Overwrite
$zip->overwrite() == AZ_OK or die "write error\n";


