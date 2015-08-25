#!/usr/bin/perl
#Copyright (c) 2012, Zubair Khan (governer@gmail.com) 
#All rights reserved.
#
#Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
#
#    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
#    * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
#
#THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
#


use strict;
use warnings;
use LWP::Simple;
use XML::Parser;
use CGI;
use File::Slurp;  
use File::Path;  

my $printurl = 0;
my $printaptid = 0;
my $airport = "";
my @list = ("");

rmtree('afd');
mkdir('afd');

open (MYFILE, '>afd.csv') or die;
 
# The Handlers
sub hdl_start{
	my ($p, $elt, %atts) = @_;
	if($elt eq 'aptid') {
		$printaptid = 1;
	}
	if($elt eq 'pdf') {
		$printurl = 1;
	}
}
   
sub hdl_end {
	my ($p, $elt) = @_;
	if($elt eq 'aptid') {
		$printaptid = 0;
	}
	if($elt eq 'pdf') {
		$printurl = 0;
	}
}
  
sub hdl_char {
	my ($p, $str) = @_;

	if($printaptid) {
		$airport = $str;
	}
	if($printurl) {
		if(!($airport eq ''))  {
			push (@list,$str);
			$str =~ s/\..*//g;
            $str =~ s/([a-z]*_[0-9]*).*/$1/;
			print MYFILE "$airport,$str\n";
            $airport="";
		}
	}
}

sub hdl_def {
} 
 
my $parser = new XML::Parser (Handlers => {
                              Start   => \&hdl_start,
                              End     => \&hdl_end,
                              Char    => \&hdl_char,
                              Default => \&hdl_def,
                            });

# Process each TAF 

my $response;

my $name = "afd.xml";

$response = read_file($name);

$parser->parse($response);

my $count = 0;
foreach(@list) {
    $count = $count + 1;
    my $nm = $_;
    my $dl = $_;
    $dl =~ s/([a-z]*)/\U$1/;

    $nm =~ s/([a-z]*_[0-9]*).*/$1.pdf/;
	if (!(-e "afd/$nm")) {
		print "$count of ";
		print scalar @list;
		print " $_ $nm \n"; 
		print "http://localhost/afd/$dl ";
		print "afd/$nm \n"; 
		getstore("http://localhost/afd/$dl", "afd/$nm");
	}
}
