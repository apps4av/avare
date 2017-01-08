#!/bin/bash

BASEURL="http://aeronav.faa.gov/afd"

if [ "$1" = "" ] ; then
 echo "$0 <update_name>"
 echo "	<update_name> like '15sep2016'"
 exit
fi

URL="${BASEURL}/${1}/"

echo "Getting ${URL}..."

rm -rf afd
mkdir afd
cd afd/
wget -I afd/${1} -m -X "*" $URL
