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

open FILE, "<FIX.txt" or die $!;
while (<FILE>) {
	if(m/^FIX1/) {
		$id = ltrim(rtrim(substr($_, 4, 6)));
		$type = ltrim(rtrim(substr($_, 212, 15)));
		$name = ltrim(rtrim(substr($_, 4, 7))) . " " . ltrim(rtrim(substr($_, 141, 10)));
		$name =~ s/,/;/g;
		$lat = ltrim(rtrim(substr($_, 66, 13)));
		$latd = ltrim(rtrim(substr($lat, 0, 2))) / 1;
		$latm = ltrim(rtrim(substr($lat, 3, 2))) / 60;
		$lats = ltrim(rtrim(substr($lat, 6, 7))) / 3600;
		$latl = substr($lat, 12, 1);
		if($latl eq "N") { 
			$lt = ($latd + $latm + $lats);
		}
		else {
			$lt = -($latd + $latm + $lats);
		}
		$lon = ltrim(rtrim(substr($_, 80, 14)));
		$lond = ltrim(rtrim(substr($lon, 0, 3))) / 1;
		$lonm = ltrim(rtrim(substr($lon, 4, 2))) / 60;
		$lons = ltrim(rtrim(substr($lon, 7, 6))) / 3600;
		$lonl = substr($lon, 13, 1);
		if($lonl eq "W") { 
			$ln = -($lond + $lonm + $lons);
		}
		else {
			$ln = ($lond + $lonm + $lons);
		}
		print "$id,$lt,$ln,$type,$name\n";
	}
}
close(FILE);


