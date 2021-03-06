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
use XML::Parser;
use File::stat;
use Time::localtime;

my $raw_text = "";
my $issue_time = "";
my $station_id = "";

my $p_raw_text = 0;
my $p_issue_time = 0;
my $p_station_id = 0;

my $start = 0;

sub initnew {
    $raw_text = "";
    $issue_time = "";
    $station_id = "";

    $p_raw_text = 0;
    $p_issue_time = 0;
    $p_station_id = 0;
}


# The Handlers
sub hdl_start{
    my ($p, $elt, %atts) = @_;
    if($elt eq 'TAF') {
        initnew();
        $start = 1;
    }

    if($start == 0) {
        return;
    }

    if($elt eq 'raw_text') {
        $p_raw_text = 1;
    }
    if($elt eq 'issue_time') {
        $p_issue_time = 1;
    }
    if($elt eq 'station_id') {
        $p_station_id = 1;
    }    
}
   
sub hdl_end {
    my ($p, $elt) = @_;
    if($elt eq 'TAF') {
        $raw_text //= "";
        $raw_text =~ s/\n//g;
        $raw_text =~ s/,/;/g;
        $issue_time //= "";
        $station_id //= "";
        print "$raw_text,$issue_time,$station_id\n";
    }
    
    if($start == 0) {
        return;
    }
    
    if($elt eq 'raw_text') {
        $p_raw_text = 0;
    }
    if($elt eq 'issue_time') {
        $p_issue_time = 0;
    }
    if($elt eq 'station_id') {
        $p_station_id = 0;
    }    

}
  
sub hdl_char {
    my ($p, $str) = @_;

    if($start == 0) {
        return;
    }
    
    if($p_raw_text != 0) {
        $raw_text .= $str;
    }
    if($p_issue_time != 0) {
        $issue_time = $str;
    }
    if($p_station_id != 0) {
        $station_id = $str;
    }
}
  
sub hdl_def { }

my $parser = new XML::Parser (Handlers => {
                              Start   => \&hdl_start,
                              End     => \&hdl_end,
                              Char    => \&hdl_char,
                              Default => \&hdl_def,
                            });

initnew();
$parser->parsefile('tafs.cache.xml');

