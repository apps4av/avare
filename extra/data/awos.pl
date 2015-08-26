#!/usr/bin/perl
#Copyright (c) 2015 Apps4Av Inc.
# Authors Zubair Khan (governer@gmail.com), Jesse McGraw (jlmcgraw@gmail.com)
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
$debug = 0;

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

open FILE, "<AWOS.txt" or die $!;

#This variable is used to track if we're seeing the next AWOS after accumulating comments
$ready_to_print = "0";

$awos_count   = "0";
$awos_remarks = "0";
print
  "ident,type,commisionstatus,lt,ln,elevation,freq1,freq2,tel1,tel2,remark\n"
  if $debug;

while (<FILE>) {

    #replace all commas with spaces in each line
    $_ =~ s/,/ /g;
    if (m/^AWOS1/) {

        $ready_to_print++;

        if ( $ready_to_print eq "2"  && uc($commisionstatus) eq "Y"){
            print "$ident,$type,$commisionstatus,$lt,$ln,$elevation,$freq1,$freq2,$tel1,$tel2,$remark\n";
            $awos_count++;
            }
            $ready_to_print = "1";
            #Clear out the remarks string for each new AWOS listing
            $remark = "";         
        

        $ident           = ltrim( rtrim( substr( $_, 5,  4 ) ) );
        $type            = ltrim( rtrim( substr( $_, 9,  10 ) ) );
        $commisionstatus = ltrim( rtrim( substr( $_, 19, 1 ) ) );
        
        $commisiondate   = ltrim( rtrim( substr( $_, 20, 10 ) ) );

        $lat = ltrim( rtrim( substr( $_, 31, 14 ) ) );
	#If we have the right amount of latitude information parse out the separate components
        if ( length($lat) == 14 ) {
            $lat_d = ltrim( rtrim( substr( $lat, 0, 3 ) ) ) / 1;
            $lat_m = ltrim( rtrim( substr( $lat, 4, 2 ) ) ) / 60;
            $lat_s = ltrim( rtrim( substr( $lat, 7, 7 ) ) ) / 3600;
            $lat_l = substr( $lat, 0, 1 );
            if ( $lat_l eq "N" ) {
                $lt = ( $lat_d + $lat_m + $lat_s );
            }
            else {
                $lt = -( $lat_d + $lat_m + $lat_s );
            }
        }
        else {
            $lt = "";
        }

        $lon = ltrim( rtrim( substr( $_, 45, 14 ) ) );
	#If we have the right amount of longitude information parse out the separate components
        if ( length($lon) == 14 ) {
            $lon_d = ltrim( rtrim( substr( $lon, 0, 3 ) ) ) / 1;
            $lon_m = ltrim( rtrim( substr( $lon, 4, 2 ) ) ) / 60;
            $lon_s = ltrim( rtrim( substr( $lon, 7, 7 ) ) ) / 3600;
            $lon_l = substr( $lon, 14, 1 );
            if ( $lon_l eq "W" ) {
                $ln = -( $lon_d + $lon_m + $lon_s );
            }
            else {
                $ln = ( $lon_d + $lon_m + $lon_s );
            }
        }
        else {
            $ln = "";
        }
        $elevation     = ltrim( rtrim( substr( $_, 60,  7 ) ) );
        $surveymethod  = ltrim( rtrim( substr( $_, 67,  1 ) ) );
        $freq1         = ltrim( rtrim( substr( $_, 68,  7 ) ) );
        $freq2         = ltrim( rtrim( substr( $_, 75,  7 ) ) );
        $tel1          = ltrim( rtrim( substr( $_, 82,  14 ) ) );
        $tel2          = ltrim( rtrim( substr( $_, 96,  14 ) ) );
        $apt_id        = ltrim( rtrim( substr( $_, 110, 11 ) ) );
        $city          = ltrim( rtrim( substr( $_, 121, 40 ) ) );
        $state         = ltrim( rtrim( substr( $_, 161, 2 ) ) );
        $effectivedate = ltrim( rtrim( substr( $_, 163, 10 ) ) );

    }
    if (m/^AWOS2/) {
        $awos_remarks++;

	#There can be multiple comment lines for each station so accumulate them with . in between each
        $remark = ltrim( rtrim( substr( $_, 19, 236 ) ) ) . "..." . $remark;
    }

}

#This is a hack to print the last station
     if ( uc($commisionstatus) eq "Y"){
            print "$ident,$type,$commisionstatus,$lt,$ln,$elevation,$freq1,$freq2,$tel1,$tel2,$remark\n";
            $awos_count++;
        }

print "$awos_count active stations, $awos_remarks remarks\n" if $debug;
close(FILE);

