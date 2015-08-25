#!/bin/bash
#Copyright (c) 2015, Apps4av Inc. (apps4av@gmail.com) 
#All rights reserved.
#
#Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
#
#    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
#    * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
#
#THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
#

# author zkhan

NP=8

# The navcan website has two versions, current and next, which update every 56
# days. Get the next one when preparing charts.

function dopage {
    # Find the name of the airport, which is prefixed on airport diagram page
    # with -AD, many cases may exist, in which case take the last line
    AD=`pdf2txt -p $1 canads.pdf | grep '\-AD' | tail -n 1`
    FR=`pdf2txt -p $1 canads.pdf | grep 'CARTE D’AÉRODROME' | tail -n 1`
    # If not an AD page, continue to next page
    echo "$1 $AD"
    # This logic exists to also use English version of the chart as French
    # follows English
    if [[ -n $FR ]]
    then
        continue
    fi
    if [[ $AD != *"-AD" ]]
    then
        if [[ $AD != *"-AD-1" ]]
        # first page only
        then
            continue
        fi
    fi
    # Find airport name as in CYKD (all 4 digits/letters)
    IMG=`echo $AD | cut -b 1-4`
    # If already processed, continue to next
    if [ -d plates/$IMG ]; then
        continue
    fi
    # Avare plates format
    mkdir -p plates/$IMG
    # Page - 1 for convert
    PAGE=`expr $1 - 1`
    # Convert to ~1400x800 pixel image
    convert -density 150x150 canads.pdf[$PAGE] plates/$IMG/AIRPORT-DIAGRAM.png
}

# Find number of pages in this doc, for our FOR loop
PAGES=`pdfinfo canads.pdf | grep Pages | sed 's/Pages:\s*//'`

# Process all pages
num=0
for i in `seq 8 $PAGES`;
do 
    dopage $i &
    num=$((num + 1));
    if [ $num -eq ${NP} ] ; then wait ; num=0 ; fi;
done

# Zip up the result
rm -f CAN_ADS.zip
zip CAN_ADS.zip -r -i*.png plates 
