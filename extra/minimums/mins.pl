#!/usr/bin/perl
#Copyright (c) 2015, Apps4Av Inc.
# Author Zubair Khan (governer@gmail.com) 
#All rights reserved.
#
#Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
#
#    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
#    * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
#
#THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

# Runs on tmp.txt and extracts airport codes from it, run with mins.sh
open my $fh, "<", "../airport_min.txt" or die "could not open filename: $!";
my(%apts);
while (<$fh>) {
	chomp;
	push @{ $apts{$_} }, $_;
}


open (MYFILE, 'tmp.txt') or die;
while (<MYFILE>) {
	my($line) = $_;
	chomp($line);
	$line =~ s/\(FAA\)//g;
	$line =~ s/\(RNAV\)//g;
	$line =~ s/\(GPS\)//g;

	my @matches = ($line =~ m/\([0-9A-Z]{3}[0-9A-Z]?\)/g);

	foreach (@matches) {
		if(exists $apts{$_}) {
			$_ =~ s/[(]//;
			$_ =~ s/[)]//;
			print "$_,$ARGV[0]-$ARGV[1]\n";
		}
	}
}
close (MYFILE); 
