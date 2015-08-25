#!/usr/bin/perl
#Copyright (c) 2015, Apps4Av Inc.
# Author zkhan
#All rights reserved.
#
#Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
#
#    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
#    * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
#
#THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
#

# Right trim function to remove trailing whitespace
sub rtrim($) {
    my $string = shift;
    $string =~ s/\s+$//;
    return $string;
}

# Left trim function to remove leading whitespace
sub ltrim($) {
    my $string = shift;
    $string =~ s/^\s+//;
    return $string;
}

open FILE, "<APT.txt" or die $!;
while (<FILE>) {
    if (m/^APT/) {
        #sanitize input text for output to csv
        $_ =~ s/,|"/ /g;
        $id   = ltrim( rtrim( substr( $_, 27,  4 ) ) );
        $type = ltrim( rtrim( substr( $_, 14,  12 ) ) );
        $name = ltrim( rtrim( substr( $_, 133, 50 ) ) );
        $state = ltrim( rtrim( substr( $_, 91, 2 ) ) );
        $city = ltrim( rtrim( substr( $_, 93, 40 ) ) );
        
        $manager    = ltrim( rtrim( substr( $_, 355, 35 ) ) );
        $managertel = ltrim( rtrim( substr( $_, 507, 16 ) ) );
        
        $lat  = ltrim( rtrim( substr( $_,   523, 14 ) ) );
        $latd = ltrim( rtrim( substr( $lat, 0,   2 ) ) ) / 1;
        $latm = ltrim( rtrim( substr( $lat, 3,   2 ) ) ) / 60;
        $lats = ltrim( rtrim( substr( $lat, 6,   7 ) ) ) / 3600;
        $latl = substr( $lat, 13, 1 );

        if ( $latl eq "N" ) {
            $lt = ( $latd + $latm + $lats );
        }
        else {
            $lt = -( $latd + $latm + $lats );
        }
        $lon  = ltrim( rtrim( substr( $_,   550, 15 ) ) );
        $lond = ltrim( rtrim( substr( $lon, 0,   3 ) ) ) / 1;
        $lonm = ltrim( rtrim( substr( $lon, 4,   2 ) ) ) / 60;
        $lons = ltrim( rtrim( substr( $lon, 7,   7 ) ) ) / 3600;
        $lonl = substr( $lon, 14, 1 );
        if ( $lonl eq "W" ) {
            $ln = -( $lond + $lonm + $lons );
        }
        else {
            $ln = ( $lond + $lonm + $lons );
        }
        $var  = ltrim( rtrim( substr( $_, 586, 3 ) ) );
        $fuel = ltrim( rtrim( substr( $_, 900, 40 ) ) );
        my $cut = substr( $fuel, 0, length($fuel) );
        $fuel = "";
        while ( length($cut) > 0 ) {
            $fueltype = ltrim( rtrim( substr( $cut, 0, 5 ) ) );
            $fueltype =~ s/^(A|B)/JET-$1/;
            $fueltype =~ s/^(80)$/$1\(RED\)/;
            $fueltype =~ s/^(100)$/$1\(GREEN\)/;
            $fueltype =~ s/^(100LL)$/$1\(BLUE\)/;
            $fuel = $fuel . " " . $fueltype;

            $cut = substr( $cut, 5, length($cut) );
        }
        $fuel       = ltrim( rtrim($fuel) );
        $use        = ltrim( rtrim( substr( $_, 185, 2 ) ) );
        $elevation  = ltrim( rtrim( substr( $_, 578, 7 ) ) );
        $patterna   = ltrim( rtrim( substr( $_, 593, 4 ) ) );
        $ctaff      = ltrim( rtrim( substr( $_, 988, 7 ) ) );
        $unicom     = ltrim( rtrim( substr( $_, 981, 7 ) ) );
        $atct       = ltrim( rtrim( substr( $_, 980, 1 ) ) );
        $fee        = ltrim( rtrim( substr( $_, 1002, 1 ) ) );
        $lightsched = ltrim( rtrim( substr( $_, 966, 7 ) ) );
        $segcircle  = ltrim( rtrim( substr( $_, 995, 4 ) ) );

        $c1 = ltrim( rtrim( substr( $_, 877, 1 ) ) );
        $c2 = ltrim( rtrim( substr( $_, 878, 1 ) ) );
        $custom = $c1 . $c2;
        $beacon = ltrim( rtrim( substr( $_, 999, 3 ) ) );
        $tel    = ltrim( rtrim( substr( $_, 762, 16 ) ) );

        print
"$id,$lt,$ln,$type,$name,$use,$tel,$manager,$managertel,$elevation,$var,$patterna,$fuel,$custom,$beacon,$lightsched,$segcircle,$atct,$unicom,$ctaff,$fee,$state,$city\n";
    }
}
close(FILE);

