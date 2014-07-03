# Copyright (c) 2012, Zubair Khan (governer@gmail.com) 
#All rights reserved.
#
#Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
#
#    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
#    * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
#
#THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
#


CYCLE=1405

#Never use _ and . in file names

clean:
	rm -rf tiles *.db vfr-eu.zip vfr-eu-warped.tif databases-eu.zip files.csv
	rm -rf tiles
	mkdir -p tiles/${CYCLE}/vfr
#	mkdir -p tiles/${CYCLE}/ifr

prep:
	mkdir -p tiles/${CYCLE}/vfr/eu
	gdalwarp -q -r bilinear -t_srs WGS84 vfr-eu.tif vfr-eu-warped.tif

#	mkdir -p tiles/${CYCLE}/ifr/eu
#	gdalwarp -q -r bilinear -t_srs WGS84 ifr-eu.tif ifr-eu-warped.tif


retile:
	./gdal_retile.py -r cubicspline -co COMPRESS=DEFLATE -co ZLEVEL=6 -levels 4 -targetDir tiles/${CYCLE}/vfr/eu -ps 512 512 -useDirForEachRow vfr-eu-warped.tif

#	./gdal_retile.py -r cubicspline -co COMPRESS=DEFLATE -co ZLEVEL=6 -levels 4 -targetDir tiles/${CYCLE}/ifr/eu -ps 512 512 -useDirForEachRow ifr-eu-warped.tif


finalize:
	rm -f files.csv
	for file in `find tiles/${CYCLE}/vfr -name "*.tif" -print | sed 's/.tif'// | sort`; do \
		echo $$file ; \
		gdal_translate -of jpeg -co QUALITY=50 $$file.tif $$file.jpg; \
		rm -f $$file.jpg.aux.xml ; \
		echo $$file.jpg,`python coords.py $$file.tif``echo $$file|cut -d/ -f3`,`echo $$file |cut -f5 -d/` >> files.csv ; \
	done
	sqlite3 maps.vfr.db < importfiles.sql
	rm -f files.csv
	#for file in `find tiles/${CYCLE}/ifr -name "*.tif" -print | sed 's/.tif'// | sort`; do \
		echo $$file ; \
		gdal_translate -of jpeg -co QUALITY=50 $$file.tif $$file.jpg; \
		rm -f $$file.jpg.aux.xml ; \
		echo $$file.jpg,`python coords.py $$file.tif``echo $$file|cut -d/ -f3`,`echo $$file |cut -f5 -d/` >> files.csv ; \
	done
	#sqlite3 maps.ifr.db < import.sql
	#rm -f files.csv

database:
	sqlite3 main.db < importother.sql

zipit:
	zip databases-eu.zip -r -1 -T maps.vfr.db main.db
	zip vfr-eu.zip -r -1 -T `sqlite3 maps.vfr.db "select name from files"`
	#zip ifr-eu.zip -r -1 -T `sqlite3 maps.ifr.db "select name from files"`
