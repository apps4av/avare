#!/usr/bin/perl
#Copyright (c) 2015 Apps4Av Inc.
# Author: Zubair Khan (governer@gmail.com) 
#All rights reserved.
#
#Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
#
#    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
#    * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
#
#THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
#

#
#CREATE TABLE airports(LocationID Text,ARPLatitude float,ARPLongitude float,Type Text,FacilityName Text,Use Text,FSSPhone Text,Manager Text,ManagerPhone Text,ARPElevation Text,MagneticVariation Text,TrafficPatternAltitude Text,FuelTypes Text,Customs Text,Beacon Text,LightSchedule Text,SegCircle Text,ATCT Text,UNICOMFrequencies Text,CTAFFrequency Text,NonCommercialLandingFee Text,State Text, City Text);
open FILE, "<airports.csv" or die $!;
while (<FILE>) {
    @sp = split(',', $_);
    if (!($sp[8] eq "\"iso_country\"")) {
        if (!($sp[8] eq "\"US\"")) {
            split_string($_, $sp[8]);
        }
    }
}
close(FILE);

# Split CSV string with ""
sub split_string {
    my $text = shift;
    my $country = shift;
    my @new = ();
    push(@new, $+) while $text =~ m{ \s*(
        # groups the phrase inside double quotes
        "([^\"\\]*(?:\\.[^\"\\]*)*)"\s*,?
        # groups the phrase inside single quotes
        | '([^\'\\]*(?:\\.[^\'\\]*)*)'\s*,?
        # trims leading/trailing space from phrase
        | ([^,\s]+(?:\s+[^,\s]+)*)\s*,?
        # just to grab empty phrases
        | (),
        )\s*}gx;
    push(@new, undef) if $text =~ m/,\s*$/;

    $name = $new[3];
    $name =~ s/,/ /g;

    # Canada to be set to CN for area plates
    if($country eq "\"CA\"") {
        print "$new[1],$new[4],$new[5],OUR-AP,$name,,,,,,,,,,,,,,,,,CN,\n";
    }
    else {
        print "$new[1],$new[4],$new[5],OUR-AP,$name,,,,,,,,,,,,,,,,,,\n";
    }
}
