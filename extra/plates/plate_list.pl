#!/usr/bin/perl
#!/bin/bash
# Copyright (c) 2012-2014, Apps4av Inc. (apps4av@gmail.com) 
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

# Run this script on the plates folder to find a list of plates that will 
# be exported to geo tagging list on the server. This takes in list.txt
# formatted as:
# AK/ENN/RNAV-GPS-RWY-04L.png
# ...

open (MYFILE, 'list.txt');
my @array;
my $index = 0;
while (<MYFILE>) {
 	chomp;
    @data = split(/\//, $_);
    if (
        ($data[2] =~ /^ILS-/) or
        ($data[2] =~ /^HI-ILS-/) or
        ($data[2] =~ /^VOR-/) or
        ($data[2] =~ /^LDA-/) or
        ($data[2] =~ /^RNAV-/) or
        ($data[2] =~ /^NDB-/) or
        ($data[2] =~ /^LOC-/) or
        ($data[2] =~ /^HI-LOC-/) or
        ($data[2] =~ /^SDA-/) or
        ($data[2] =~ /^GPS-/) or
        ($data[2] =~ /^TACAN-/) or
        ($data[2] =~ /^HI-VOR/) or
        ($data[2] =~ /^HI-TACAN/) or
        ($data[2] =~ /^COPTER-/) or
        0
        ) {
            if(
                ($data[2] !~ /-CONT/)
            ) {   
                $array[$index] = "$_\n";
                $index++;
            }
    }

}
close (MYFILE); 
print sort @array;
