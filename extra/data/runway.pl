#!/usr/bin/perl
#Copyright (c) 2015 Apps4Av Inc.,
# Author Zubair Khan (governer@gmail.com) 
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

open FILE, "<APT.txt" or die $!;
while (<FILE>) {
	if(m/^APT/) {
		$id = ltrim(rtrim(substr($_, 27, 4)));
	}
	if(m/^RWY/) {
		$len = ltrim(rtrim(substr($_, 23, 5)));
		$wid = ltrim(rtrim(substr($_, 28, 4)));
		$type = ltrim(rtrim(substr($_, 32, 12)));
		$run0 = ltrim(rtrim(substr($_, 65, 3)));

		$lat = ltrim(rtrim(substr($_, 88, 14)));
		if(length($lat) == 14) {
			$latd = ltrim(rtrim(substr($lat, 0, 2))) / 1;
			$latm = ltrim(rtrim(substr($lat, 3, 2))) / 60;
			$lats = ltrim(rtrim(substr($lat, 6, 7))) / 3600;
			$latl = substr($lat, 13, 1);
			if($latl eq "N") { 
				$lt = ($latd + $latm + $lats);
			}
			else {
				$lt = -($latd + $latm + $lats);
			}
		}
		else {
			$lt = "";
		}
		$run0lat = $lt;

		$lon = ltrim(rtrim(substr($_, 115, 15)));
		if(length($lon) == 15) {
			$lond = ltrim(rtrim(substr($lon, 0, 3))) / 1;
			$lonm = ltrim(rtrim(substr($lon, 4, 2))) / 60;
			$lons = ltrim(rtrim(substr($lon, 7, 7))) / 3600;
			$lonl = substr($lon, 14, 1);
			if($lonl eq "W") { 
				$ln = -($lond + $lonm + $lons);
			}
			else {
				$ln = ($lond + $lonm + $lons);
			}
		}
		else {
			$ln = "";
		}
		$run0lon = $ln;

		$run0elev = ltrim(rtrim(substr($_, 142, 7)));
		$run0true = ltrim(rtrim(substr($_, 68, 3)));
		$run0dt = ltrim(rtrim(substr($_, 217, 4)));
		$run0light = ltrim(rtrim(substr($_, 237, 8)));
		$run0ils = ltrim(rtrim(substr($_, 71, 10)));
		$run0vgsi = ltrim(rtrim(substr($_, 228, 5)));
		$run0pattern = ltrim(rtrim(substr($_, 81, 1)));
		$run1 = ltrim(rtrim(substr($_, 287, 3)));
		
		$lat = ltrim(rtrim(substr($_, 310, 14)));
		if(length($lat) == 14) {
			$latd = ltrim(rtrim(substr($lat, 0, 2))) / 1;
			$latm = ltrim(rtrim(substr($lat, 3, 2))) / 60;
			$lats = ltrim(rtrim(substr($lat, 6, 7))) / 3600;
			$latl = substr($lat, 13, 1);
			if($latl eq "N") { 
				$lt = ($latd + $latm + $lats);
			}
			else {
				$lt = -($latd + $latm + $lats);
			}
		}
		else {
			$lt = "";
		}
		$run1lat = $lt;

		$lon = ltrim(rtrim(substr($_, 337, 15)));
		if(length($lon) == 15) {
			$lond = ltrim(rtrim(substr($lon, 0, 3))) / 1;
			$lonm = ltrim(rtrim(substr($lon, 4, 2))) / 60;
			$lons = ltrim(rtrim(substr($lon, 7, 7))) / 3600;
			$lonl = substr($lon, 14, 1);
			if($lonl eq "W") { 
				$ln = -($lond + $lonm + $lons);
			}
			else {
				$ln = ($lond + $lonm + $lons);
			}
		}
		else {
			$ln = "";
		}
		$run1lon = $ln;
		
		$run1elev = ltrim(rtrim(substr($_, 364, 7)));
		$run1true = ltrim(rtrim(substr($_, 290, 3)));
		$run1dt =  ltrim(rtrim(substr($_, 439, 4)));
		$run1light = ltrim(rtrim(substr($_, 459, 8)));
		$run1ils = ltrim(rtrim(substr($_, 293, 10)));
		$run1vgsi = ltrim(rtrim(substr($_, 450, 5)));
		$run1pattern = ltrim(rtrim(substr($_, 303, 1)));
		print "$id,$len,$wid,$type,$run0,$run1,$run0lat,$run1lat,$run0lon,$run1lon,$run0elev,$run1elev,$run0true,$run1true,$run0dt,$run1dt,$run0light,$run1light,$run0ils,$run1ils,$run0vgsi,$run1vgsi,$run0pattern,$run1pattern\n";
	}
}
close(FILE);


