#!/bin/bash
# Copyright (c) 2015 Apps4Av Inc.
# Author Zubair Khan (governer@gmail.com)
# Author Peter A. Gustafson (peter.gustafson@wmich.edu)
# All rights reserved.
#
# Redistribution and use in source and binary forms, with or without
# modification, are permitted provided that the following conditions
# are met:
#
# * Redistributions of source code must retain the above copyright
#   notice, this list of conditions and the following disclaimer.
# * Redistributions in binary form must reproduce the above copyright
#   notice, this list of conditions and the following disclaimer in
#   the documentation and/or other materials provided with the
#   distribution.
#
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
# "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
# LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
# A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
# HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
# INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
# BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
# OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED
# AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
# LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
# WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
# POSSIBILITY OF SUCH DAMAGE.
#
 
NP=8

REGNS="AK EC1 EC2 EC3 NC1 NC2 NC3 NE1 NE2 NE3 NE4 NW1 PAC SC1 SC2 SC3 SC4 SC5 SE1 SE2 SE3 SE4 SW1 SW2 SW3 SW4"

LINK=http://localhost/plates

DPI=150


${MODULE_DIR}/airport.pl > airport_min.txt;  

rm -rf mins
mkdir mins
cd mins

rm -f *.png
rm -f *.csv
rm -f *.txt
rm -f *.PDF
rm -f alternates.zip 
rm -rf minimums

for REG in ${REGNS}; do
    wget ${LINK}/${REG}TO.PDF -O ${REG}TO.PDF
    wget ${LINK}/${REG}ALT.PDF -O ${REG}ALT.PDF
done

## Parallel version
ls *TO.PDF |
xargs -P ${NP} -n 1 mogrify -dither none -antialias -density ${DPI} -depth 8 -quality 00 -background white -alpha remove -colors 15 -format png 
ls *ALT.PDF |
xargs -P ${NP} -n 1 mogrify -dither none -antialias -density ${DPI} -depth 8 -quality 00 -background white -alpha remove -colors 15 -format png 
wait

find . -name "*.png" | 
xargs -P ${NP} -n 1 optipng -quiet
wait

for REG in ${REGNS}
do
    ${MODULE_DIR}/mins.sh ${REG}TO >> to.csv
done

for REG in ${REGNS}
do
    ${MODULE_DIR}/mins.sh ${REG}ALT >> alt.csv
done

mkdir minimums
mkdir minimums/A
mkdir minimums/E
mkdir minimums/N
mkdir minimums/P
mkdir minimums/S
mv A*.png minimums/A
mv E*.png minimums/E
mv N*.png minimums/N
mv P*.png minimums/P
mv S*.png minimums/S

rm -f alternates.zip
zip -r -i "*.png" -1 -T -q alternates.zip minimums

cp alternates.zip ../

