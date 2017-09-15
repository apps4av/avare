#!/bin/make
#Copyright Apps4Av Inc.
#Author Zubair Khan (governer@gmail.com) 
#All rights reserved.
#
#Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
#
#    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
#    * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
#
#THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
#

STATES=VI XX PR DC AL AK AZ AR CA CO CT DE FL GA HI ID IL IN IA KS KY LA ME MD MA MI MN MS MO MT NE NV NH NJ NM NY NC ND OH OK OR PA RI SC SD TN TX UT VT VA WA WV WI WY

output:
	#download
	${DOWNLOAD} http://localhost/plates/d-TPP_Metafile.xml d-TPP_Metafile.xml
	${DOWNLOAD} https://www.outerworldapps.com/WairToNowWork/avare_aptdiags.php aps.csv
	# Do it
	${CHDIR} ${BUILD_DIR} && \
		${REMOVE} list.txt; \
		python ${MODULE_DIR}/dlplates.py; \
		for state in ${STATES}; \
		do \
			${MODULE_DIR}/doplates.sh $${state}; \
		done; \
		${MODULE_DIR}/plate_list.pl > list_plate.txt
	
clean:
	${CHDIR} ${BUILD_DIR} && \
		${REMOVE} plates_* d-TPP_Metafile.xml list.txt list_plate.txt; \
		for state in ${STATES}; \
		do \
			${REMOVE} $${state}.zip; \
		done; \
		
