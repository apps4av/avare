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

open FILE, "<TWR.txt" or die $!;
while (<FILE>) {
	if(m/^TWR1/) {
		$id = ltrim(rtrim(substr($_, 4, 4)));
	}
	if(m/^TWR3/) {
		$_ = substr($_, 8, length($_));
		while(length($_) > 93) {
			$freq=ltrim(rtrim(substr($_, 0, 44)));
			$freq =~ s/,/;/g;
			$type=ltrim(rtrim(substr($_, 44, 50)));
			$type =~ s/,/;/g;
			$_ = substr($_, 94, length($_));
			if(
				($type =~ "ATIS") ||
				($type =~ "GND") ||
				($type =~ "LCL") ||
				($type =~ "EMERG") ||
				($type =~ "GATE") ||
				($type =~ "CD")) {
				print "$id,$type,$freq\n";
			}
			
		}
		
	}
	if(m/^TWR6/) {
		$attend = ltrim(rtrim(substr($_, 13, length($_))));
		$attend =~ s/,/ /g;
		print "$id,Remark,$attend\n";
	}
}
close(FILE);


