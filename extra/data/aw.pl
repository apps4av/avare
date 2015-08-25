#!/usr/bin/perl
#Copyright (c) 2012, Apps4av Inc. (apps4av@gmail.com) 
##All rights reserved.
##
##Redistribution and use in source and binary forms, with or without
#modification, are permitted provided that the following conditions are met:
##
##    * Redistributions of source code must retain the above copyright notice,
#this list of conditions and the following disclaimer.
##    * Redistributions in binary form must reproduce the above copyright
#notice, this list of conditions and the following disclaimer in the
#documentation and/or other materials provided with the distribution.
##
##THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
#AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
#IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
#DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
#FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
#DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
#SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
#CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
#OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
#OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
##
#

# parse federal airways

use strict;
use warnings;
    
sub  trim { my $s = shift; $s =~ s/^\s+|\s+$//g; return $s };

my $filename = 'AWY.txt';
open(my $fh, '<:encoding(UTF-8)', $filename) or die "Could not open file '$filename' $!";
while (my $row = <$fh>) {
    if ($row =~ /^AWY2/) {
        chomp $row;

        # get name, sequence number, lat, and lon of that
        my $name = trim(substr $row, 4, 5);
        my $seqn = trim(substr $row, 10, 5);
        my $lat = trim(substr $row, 83, 14);
        my $lon = trim(substr $row, 97, 14);

        # lat / lon are - separated degrees, minutes, seconds

        if(!($lon eq "" || $lat eq "")) {
            my $latg = chop($lat);
            (my $latd, my $latm, my $lats) = split('-', $lat);
            my $lata = $latd + $latm / 60.0 + $lats / 3600.0;
            if($latg eq "S") {
                $lata = -$lata;
            }

            my $long = chop($lon);
            (my $lond, my $lonm, my $lons) = split('-', $lon);
            my $lona = $lond + $lonm / 60.0 + $lons / 3600.0;
            if($long eq "W") {
                $lona = -$lona;
            }

            print "$name,$seqn,$lata,$lona\n";
        }
    }
}
