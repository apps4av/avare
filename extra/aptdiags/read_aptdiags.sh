#!/bin/bash
#
#  Read through all .png files in aptdiags directory
#  and put their display parameters in aptdiags.csv
#

##+++2013-01-10
##    Copyright (C) 2013, Mike Rieker, Beverly, MA USA
##
##    This program is free software; you can redistribute it and/or modify
##    it under the terms of the GNU General Public License as published by
##    the Free Software Foundation; version 2 of the License.
##
##    This program is distributed in the hope that it will be useful,
##    but WITHOUT ANY WARRANTY; without even the implied warranty of
##    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
##    GNU General Public License for more details.
##
##    EXPECT it to FAIL when someone's HeALTh or PROpeRTy is at RISk.
##
##    You should have received a copy of the GNU General Public License
##    along with this program; if not, write to the Free Software
##    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

##    Avare, open source moving map aviation GPS (support@apps4av.net)

set -e

#
# Read a line from stdin that has plates/<aptid>.png.
# Start up a thread to process it, waiting first if
# thread of same index number is already running.
#
#  $1 = thread index number
#
function readandstartup
{
    if read pngfile
    then
        startup $1
    fi
}

#
# Start a thread to process a png file.
# Wait first though if a thread of same 
# index is already running.
#
#  $1 = thread index number
#  $pngfile = filename ending in .png
#
function startup
{
    waitfor $1
    $bn onefile $pngfile read_pngs.$1.pid &
}

#
# Wait for thread of the given index to terminate.
#
#  $1 = thread index number
#
function waitfor
{
    pid=`cat read_pngs.$1.pid`
    if [ "$pid" != "0" ]
    then
        wait $pid
        echo 0 > read_pngs.$1.pid
    fi
}

#
# Main script entry
#
#  $1 = "" : normal external start
#  $1 = "onefile" : recursive reference to process one .png file
#       $2 = filename ending in .png
#       $3 = pid filename
#  $1 = "allfiles" : recursive reference to read .png filenames 
#                    from find and manage threads to process them
#
cd `dirname $0`
bn=`pwd`/`basename $0`
if [ "$1" == "onefile" ]
then
    #
    # This runs in a thread to process a single .png file and
    # write the results to aptdiags.csv.
    #
    pngfile=$2
    pidfile=$3
    echo $BASHPID > $pidfile
    nopng=${pngfile%.png}
    aptid=${nopng##*/}
    echo `date` @@@@@@@@@@@@@@@@@@@@@ $nopng
    mono ReadArptDgmPng.exe $pngfile \
            -csvoutfile aptdiags.csv -csvoutid $aptid \
            -markedpng marked_$pngfile 2>&1 | tee $nopng.log
    if [ ! -s $nopng.log ]
    then
        rm -f $nopng.log
    fi
elif [ "$1" == "allfiles" ]
then
    #
    # This reads the .png filenames from stdin as produced by find
    # and manages threads to process them.
    #
    rm -f read_pngs.*.pid
    echo 0 > read_pngs.0.pid
    echo 0 > read_pngs.1.pid
    echo 0 > read_pngs.2.pid
    echo 0 > read_pngs.3.pid
    echo 0 > read_pngs.4.pid
    echo 0 > read_pngs.5.pid
    echo 0 > read_pngs.6.pid
    while read pngfile
    do
        startup 0
        readandstartup 1
        readandstartup 2
        readandstartup 3
        readandstartup 4
        readandstartup 5
        readandstartup 6
    done
    waitfor 0
    waitfor 1
    waitfor 2
    waitfor 3
    waitfor 4
    waitfor 5
    waitfor 6
    rm -f read_pngs.*.pid
else
    #
    # This is the main script.
    # It cleans up stuff that might be laying around
    # then runs a find command to locate all the .png
    # files and passes the list to our 'allfiles'
    # sub-function for processing.
    #
    rm -f aptdiags.csv
    rm -rf marked_aptdiags
    mkdir marked_aptdiags
    find aptdiags -name \*.log -delete
    find aptdiags -name \*.png | sort | $bn allfiles
fi
