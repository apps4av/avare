#!/usr/bin/perl
#Copyright (c) 2015, Apps4Av Inc. (apps4av@gmail.com) 
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
# Author: zkhan


use strict;
use warnings;
use LWP::Simple;
use HTML::LinkExtor;
use XML::Parser;
use File::stat;
use Time::localtime;


my $printit = 0;
my $printitarea = 0;
my $printupper = 0;
my $printlower = 0;
my $printtimeeff = 0;
my $printtimeexp = 0;
my $printareagroup = 0;
my $printuomup = 0;
my $printuomlo = 0;
my $tfr = "";
my @linksXml;
sub cb {
        my($tag, %links) = @_;
        my $lk = "@{[%links]}\n";
    if ($lk and (-1 != index($lk, "save_pages"))) {
#replace html to xml to get XML
        $lk =~ s/\.html/.xml/g;
        $lk =~ s/href\s*//g;
#put in an array
        push(@linksXml, $lk);
    }
}
   
# The Handlers
sub hdl_start{
    my ($p, $elt, %atts) = @_;
    if($elt eq 'TFRAreaGroup') {
        $printareagroup = 1;
        $tfr=$tfr."TFR:: ";
    }
    if($elt eq 'dateEffective') {
        $printtimeeff = 1;
    }
    if($elt eq 'dateExpire') {
        $printtimeexp = 1;
    }
    if( $elt eq 'valDistVerUpper') {
        $printupper = 1;
    }
    if( $elt eq 'valDistVerLower') {
        $printlower = 1;
    }
    if( $elt eq 'uomDistVerUpper') {
        $printuomup = 1;
    }
    if( $elt eq 'uomDistVerLower') {
        $printuomlo = 1;
    }
    if($elt eq 'abdMergedArea') {
        $tfr=$tfr.",";
        $printitarea = 1;
    }
    if($elt eq 'geoLat') {
        $printit = 1;
    }
    if($elt eq 'geoLong') {
        $printit = 1;
    }
}
   
sub hdl_end {
    my ($p, $elt) = @_;
    if($elt eq 'TFRAreaGroup') {
        $tfr=$tfr.",";
        $printareagroup = 0;
    }
    if($elt eq 'abdMergedArea') {
        # remove , put by lat/lon
        chop($tfr);
        $printitarea = 0;
    }
    if($elt eq 'dateEffective') {
        $printtimeeff = 0;
    }
    if($elt eq 'dateExpire') {
        $printtimeexp = 0;
    }
    if( $elt eq 'valDistVerUpper') {
        $printupper = 0;
    }
    if( $elt eq 'valDistVerLower') {
        $printlower = 0;
    }
    if( $elt eq 'uomDistVerUpper') {
        $printuomup = 0;
    }
    if( $elt eq 'uomDistVerLower') {
        $printuomlo = 0;
    }
    if($elt eq 'geoLat') {
        $printit = 0;
    }
    if($elt eq 'geoLong') {
        $printit = 0;
    }
}
  
sub hdl_char {
    my ($p, $str) = @_;
    if($printareagroup == 0) {
        return;
    }
    if($printtimeeff) {
        $tfr = $tfr."Eff $str ";
    }
    if($printtimeexp) {
        $tfr = $tfr."Exp $str ";
    }
    if($printupper) {
        $tfr = $tfr."Top $str ";
    }
    if($printlower) {
        $tfr = $tfr."Low $str ";
    }
    if($printuomup) {
        $tfr = $tfr."$str ";
    }
    if($printuomlo) {
        $tfr = $tfr."$str ";
    }

    if($printit and $printitarea) {
        # now convert coords WGS to numbers and spit
        if (index($str, "N") != -1) {
            $str =~ s/N//g;
        }
        if (index($str, "S") != -1) {
            $str =~ s/S//g;
            $str = "-".$str;
        }
        if (index($str, "E") != -1) {
            $str =~ s/E//g;
        }
        if (index($str, "W") != -1) {
            $str =~ s/W//g;
            $str = "-".$str;
        }
        my $sum = $str + 0.0;
        $tfr = $tfr."$sum,";
    }
}
  
my $filename = "/home/apps4av/tfr.zip";

sub hdl_def { }

my $parser = new XML::Parser (Handlers => {
                              Start   => \&hdl_start,
                              End     => \&hdl_end,
                              Char    => \&hdl_char,
                              Default => \&hdl_def,
                            });
# get TFR list

my $response = get('http://tfr.faa.gov/tfr2/list.html') or die;
# extract links from it
my $LX = new HTML::LinkExtor(\&cb, 'http://tfr.faa.gov/');
$LX->parse($response);
#throw away duplicate links
my %hash = map { $_ => 1 } @linksXml;
my @unique = keys %hash;
# Now process each TFR XML
for my $url( @{unique} ) {
    $url =~ s/\/\.\.\//\//g;
    if(my $data_xml = get($url)) {
        $parser->parse($data_xml);
    }
}

# remove ,
chop($tfr);
open (MYFILE, '>tfr.txt');
print MYFILE $tfr
