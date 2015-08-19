#!/bin/bash
# Copyright (c) 2012-2014, Apps4av Inc. (apps4av@gmail.com) 
# Author: Zubair Khan (governer@gmail.com)
# Author: Peter A. Gustafson (peter.gustafson@wmich.edu)
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

DPI=150

## Convert to png
find afd -name "*.pdf" | 
xargs -P ${NP} -n 1 mogrify -dither none -antialias -density ${DPI} -depth 8 -quality 00 -background white -alpha remove -colors 15 -format png 
wait

## Optimize the png for file size and rendering
find afd -name "*.png" | 
xargs -P ${NP} -n 1 optipng -quiet
wait

##

for a in NE NC NW SE SC SW EC AK PAC; do
    rm -f AFD_${a}.zip; 
done

zip -r -i \*ne\*\*.png -1 -T -q AFD_NE.zip afd
zip -r -i \*nc\*\*.png -1 -T -q AFD_NC.zip afd
zip -r -i \*nw\*\*.png -1 -T -q AFD_NW.zip afd
zip -r -i \*se\*\*.png -1 -T -q AFD_SE.zip afd
zip -r -i \*sc\*\*.png -1 -T -q AFD_SC.zip afd
zip -r -i \*sw\*\*.png -1 -T -q AFD_SW.zip afd
zip -r -i \*ec\*\*.png -1 -T -q AFD_EC.zip afd
zip -r -i \*ak\*\*.png -1 -T -q AFD_AK.zip afd
zip -r -i \*pac\*\*.png -1 -T -q AFD_PAC.zip afd
