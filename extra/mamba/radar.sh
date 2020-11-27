#!/bin/bash
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

rm -f latest.txt mosaic_times.txt latest_radaronly.gif latest_radaronly.gfw latest_radaronly.png latest_radaronly.tif conus

date -u +"%m_%d_%Y_%H:%M_UTC" > conus
echo latest.txt >> conus
echo latest_radaronly.png >> conus

wget http://radar.weather.gov/ridge/Conus/RadarImg/latest_radaronly.gif
wget http://radar.weather.gov/ridge/Conus/RadarImg/latest_radaronly.gfw
wget http://radar.weather.gov/ridge/Conus/RadarImg/mosaic_times.txt

gdalwarp -s_srs EPSG:4326 -t_srs "+proj=merc +a=6378137 +b=6378137 +lat_ts=0.0 +lon_0=0.0 +x_0=0.0 +y_0=0 +k=1.0 +units=m +nadgrids=@null +wktext +no_defs" latest_radaronly.gif latest_radaronly.tif

convert latest_radaronly.tif -transparent black -resize 40% latest_radaronly.png
gdalinfo latest_radaronly.tif -noct | grep -E "Upper Left|Upper Right|Lower Left|Lower Right" | sed 's/.*(//' | sed 's/)//' > latest.txt
tail -1 mosaic_times.txt >> latest.txt

rm -f conus.zip
zip conus.zip conus latest.txt latest_radaronly.png

rm -f latest.txt mosaic_times.txt latest_radaronly.gif latest_radaronly.gfw latest_radaronly.png latest_radaronly.tif conus
