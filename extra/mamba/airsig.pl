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
my $valid_time_from = "";
my $valid_time_to = "";
my $point = "";
my $min_ft_msl = "";
my $max_ft_msl = "";
my $movement_dir_degrees = "";
my $movement_speed_kt = "";
my $hazard = "";
my $severity = "";
my $airsigmet_type = "";
my $longitude = "";
my $latitude = "";

my $p_raw_text = 0;
my $p_valid_time_from = 0;
my $p_valid_time_to = 0;
my $p_point = 0;
my $p_movement_dir_degrees = 0;
my $p_movement_speed_kt = 0;
my $p_airsigmet_type = 0;
my $p_longitude = 0;
my $p_latitude = 0;

my $start = 0;

sub initnew {
    $p_raw_text = 0;
    $p_valid_time_from = 0;
    $p_valid_time_to = 0;
    $p_point = 0;
    $p_movement_dir_degrees = 0;
    $p_movement_speed_kt = 0;
    $p_airsigmet_type = 0;
    $p_longitude = 0;
    $p_latitude = 0;

    $start = 0;
    $raw_text = "";
    $valid_time_from = "";
    $valid_time_to = "";
    $point = "";
    $min_ft_msl = "";
    $max_ft_msl = "";
    $movement_dir_degrees = "";
    $movement_speed_kt = "";
    $hazard = "";
    $severity = "";
    $airsigmet_type = "";
    $longitude = "";
    $latitude = "";
}


# The Handlers
sub hdl_start{
    my ($p, $elt, %atts) = @_;
    if($elt eq 'AIRSIGMET') {
        initnew();
        $start = 1;
    }

    if($start == 0) {
        return;
    }

    if($elt eq 'raw_text') {
        $p_raw_text = 1;
    }
    if($elt eq 'valid_time_from') {
        $p_valid_time_from = 1;
    }
    if($elt eq 'valid_time_to') {
        $p_valid_time_to = 1;
    }
    if($elt eq 'point') {
        $p_point = 1;
    }
    if($elt eq 'altitude') {
        $max_ft_msl = $atts{'max_ft_msl'};
        $min_ft_msl = $atts{'min_ft_msl'};
    }
    if($elt eq 'movement_dir_degrees') {
        $p_movement_dir_degrees = 1;
    }
    if($elt eq 'movement_speed_kt') {
        $p_movement_speed_kt = 1;
    }
    if($elt eq 'hazard') {
        $hazard=$atts{'type'};
        $severity=$atts{'severity'};
    }
    if($elt eq 'airsigmet_type') {
        $p_airsigmet_type = 1;
    }    
    
    if($p_point == 1) {
        if($elt eq 'longitude') {
            $p_longitude = 1;
        }    
        if($elt eq 'latitude') {
            $p_latitude = 1;
        }    
    }
}
   
sub hdl_end {
    my ($p, $elt) = @_;
    if($elt eq 'AIRSIGMET') {
        $raw_text //= "";
        $raw_text =~ s/\n//g;
        $raw_text =~ s/,/;/g;
        $valid_time_from //= "";
        $valid_time_to //= "";
        $point //= "";
        chop($point);
        $min_ft_msl //= "";
        $max_ft_msl //= "";
        $movement_dir_degrees //= "";
        $movement_speed_kt //= "";
        $hazard //= "";
        $severity //= "";
        $severity =~ s/,/;/g;
        $airsigmet_type //= "";
        print "$raw_text,$valid_time_from,$valid_time_to,$point,$min_ft_msl,$max_ft_msl,$movement_dir_degrees,$movement_speed_kt,$hazard,$severity,$airsigmet_type\n";
    }
    
    if($start == 0) {
        return;
    }
    
    if($elt eq 'raw_text') {
        $p_raw_text = 0;
    }
    if($elt eq 'valid_time_from') {
        $p_valid_time_from = 0;
    }
    if($elt eq 'valid_time_to') {
        $p_valid_time_to = 0;
    }
    if($elt eq 'point') {
        $p_point = 0;
    }
    if($elt eq 'movement_dir_degrees') {
        $p_movement_dir_degrees = 0;
    }
    if($elt eq 'movement_speed_kt') {
        $p_movement_speed_kt = 0;
    }
    if($elt eq 'airsigmet_type') {
        $p_airsigmet_type = 0;
    }    

    if($p_point == 1) {
        if($elt eq 'longitude') {
            $p_longitude = 0;
        }    
        if($elt eq 'latitude') {
            $p_latitude = 0;
        }    
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
    if($p_valid_time_from != 0) {
        $valid_time_from = $str;
    }
    if($p_valid_time_to != 0) {
        $valid_time_to = $str;
    }
    if($p_movement_dir_degrees != 0) {
        $movement_dir_degrees = $str;
    }
    if($p_movement_speed_kt != 0) {
        $movement_speed_kt = $str;
    }
    if($p_airsigmet_type != 0) {
        $airsigmet_type = $str;
    }    
    
    if($p_point == 1) {
        if($p_longitude != 0) {
            $point .= $str.":";
        }    
        if($p_latitude != 0) {
            $point .= $str.";";
        }    
    }
}
  
sub hdl_def { }

my $parser = new XML::Parser (Handlers => {
                              Start   => \&hdl_start,
                              End     => \&hdl_end,
                              Char    => \&hdl_char,
                              Default => \&hdl_def,
                            });
# get TFR list


initnew();
$parser->parsefile('airsigmets.cache.xml');

