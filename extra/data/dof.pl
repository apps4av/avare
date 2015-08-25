#!/usr/bin/perl
#Copyright (c) 2015 Apps4Av Inc.
# Author Zubair Khan (governer@gmail.com) 
#All rights reserved.
#
#Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
#
#    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
#    * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
#
#THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

# Right trim function to remove trailing whitespace
sub rtrim($)
{
	my $string = shift;
	$string =~ s/\s+$//;
	return $string;
}

# Left trim function to remove leading whitespace
sub ltrim($)
{
	my $string = shift;
	$string =~ s/^\s+//;
	return $string;
}

open FILE, "<DOF.DAT" or die $!;
while (<FILE>) {
	if(m/^[0-9A-Z][0-9A-Z]-/) {
		$latd = ltrim(rtrim(substr($_, 35, 2))) / 1;
		$latm = ltrim(rtrim(substr($_, 38, 2))) / 60;
		$lats = ltrim(rtrim(substr($_, 41, 5))) / 3600;
		$latl = ltrim(rtrim(substr($_, 46, 1)));
		if($latl eq "N") { 
			$lt = ($latd + $latm + $lats);
		}
		else {
			$lt = -($latd + $latm + $lats);
		}
		$lond = ltrim(rtrim(substr($_, 48, 3))) / 1;
		$lonm = ltrim(rtrim(substr($_, 52, 2))) / 60;
		$lons = ltrim(rtrim(substr($_, 55, 5))) / 3600;
		$lonl = ltrim(rtrim(substr($_, 60, 1)));
		if($lonl eq "W") { 
			$ln = -($lond + $lonm + $lons);
		}
		else {
			$ln = ($lond + $lonm + $lons);
		}

		$ht = ltrim(rtrim(substr($_, 83, 5))) / 1;
		$htagl = ltrim(rtrim(substr($_, 77, 5))) / 1;
        if($htagl >= 400)  {
    		print "$lt,$ln,$ht\n";
        }
	}
}
close(FILE);


