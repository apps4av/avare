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

rm -f *.csv *.xml *.db *.gz all weather latest_fcat.png latest_fcat.tif latest_fcat.txt flight_category.tif

date -u +"%m_%d_%Y_%H:%M_UTC" > weather 
echo weather.db >> weather
echo latest_fcat.png >> weather
echo latest_fcat.txt >> weather

CMD="http://aviationweather.gov/adds/dataserver_current/current/"

FL=aircraftreports.cache.xml
wget ${CMD}/${FL}.gz
gzip -d ${FL}.gz 
perl apirep.pl > apirep.csv

FL=airsigmets.cache.xml
wget ${CMD}/${FL}.gz
gzip -d ${FL}.gz
perl airsig.pl > airsig.csv

FL=tafs.cache.xml
wget ${CMD}/${FL}.gz
gzip -d ${FL}.gz
perl tafs.pl > tafs.csv

FL=metars.cache.xml
wget ${CMD}/${FL}.gz
gzip -d ${FL}.gz
perl metars.pl > metars.csv

curl -X GET  "https://aviationweather.gov/windtemp/data?level=low&region=all&layout=off" -o all 
perl wa.pl > wa.csv

sqlite3 weather.db < import.sql


./fcat.py

gdalwarp -s_srs EPSG:4326 -t_srs "+proj=merc +a=6378137 +b=6378137 +lat_ts=0.0 +lon_0=0.0 +x_0=0.0 +y_0=0 +k=1.0 +units=m +nadgrids=@null +wktext +no_defs" flight_category.tif latest_fcat.tif

convert latest_fcat.tif -transparent black latest_fcat.png
gdalinfo latest_fcat.tif -noct | tail -n 8 | head -n 4 | sed 's/.*(//' | sed 's/)//' > latest_fcat.txt
date -u +"%Y%m%d_%H%M" >> latest_fcat.txt


rm -f weather.zip
zip weather.zip weather.db weather latest_fcat.png latest_fcat.txt

rm -f *.csv *.xml *.db *.gz all weather latest_fcat.png latest_fcat.tif latest_fcat.txt flight_category.tif
