#!/bin/bash
# get the list of files

OUTDIR="./xml"
HOST="https://aviationweather.gov/adds/dataserver_current/current"
DESIGNATORS="aircraftreports metars airsigmets tafs"
WEATHERFILE="./output/weather"
WEATHERZIP="./weather.zip"
# Lets get the first 15 files on the list

mkdir $OUTDIR

for f in $DESIGNATORS; do
    HOSTDESIG="${HOST}/${f}.cache.xml"
    DESTFILENAME="$OUTDIR/${f}.xml"
    echo "Getting $f from $HOSTDESIG"
    echo "Writing output $DESTFILENAME"
    wget -q -O $DESTFILENAME $HOSTDESIG
#    cat $f >> $OUTFILE
done
# get upper level winds
wget -q -O winds.html https://aviationweather.gov/windtemp/data?level=l\&fcst=06\&region=all\&layout=off
awk '/raw data begins here/ {p=1}; p; /raw data ends here/ {p=0}' winds.html > $OUTDIR/winds.txt
./parseweather
FILEDATE=$(date -u +%M_%d_%Y_%H:%M_UTC)
echo $FILEDATE > $WEATHERFILE
echo "weather.db" >> $WEATHERFILE
echo "latest_fcat.png" >> $WEATHERFILE
echo "latest_fcat.txt" >> $WEATHERFILE
echo "Preparing zip file"
rm -f $WEATHERZIP
zip $WEATHERZIP output/*
echo "Complete"





