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


output:
	#download
	${DOWNLOAD} http://localhost/nfdc/APT.txt APT.txt
	${DOWNLOAD} http://localhost/nfdc/AWOS.txt AWOS.txt
	${DOWNLOAD} http://localhost/nfdc/TWR.txt TWR.txt
	${DOWNLOAD} http://localhost/nfdc/FIX.txt FIX.txt
	${DOWNLOAD} http://localhost/nfdc/NAV.txt NAV.txt
	${DOWNLOAD} http://localhost/nfdc/AWY.txt AWY.txt
	${DOWNLOAD} http://localhost/nfdc/Additional_Data/AIXM/SAA-AIXM_5_Schema/SaaSubscriberFile.zip SaaSubscriberFile.zip
	#direct downloads
	${DOWNLOAD} https://nfdc.faa.gov/tod/DAILY_DOF.ZIP DAILY_DOF.ZIP
	${DOWNLOAD} http://ourairports.com/data/airports.csv airports.csv
	# Do it
	${CHDIR} ${BUILD_DIR} && \
		unzip DAILY_DOF.ZIP DOF.DAT; \
		unzip -d saadir SaaSubscriberFile.zip; \
		unzip -d saadir saadir/Saa_Sub_File.zip; \
		${MODULE_DIR}/saa.pl > saa.csv; \
		${MODULE_DIR}/airport.pl > airport.csv; \
		${MODULE_DIR}/runway.pl > runway.csv; \
		${MODULE_DIR}/freq.pl > freq.csv; \
		${MODULE_DIR}/fix.pl > fix.csv; \
		${MODULE_DIR}/nav.pl > nav.csv; \
		${MODULE_DIR}/dof.pl > dof.csv; \
		${MODULE_DIR}/awos.pl > awos.csv; \
		${MODULE_DIR}/aw.pl > aw.csv; \
		${MODULE_DIR}/ourairports.pl > ourairports.csv
	
clean:
	${CHDIR} ${BUILD_DIR} && \
		${REMOVE} APT.txt AWOS.txt TWR.txt FIX.txt NAV.txt AWY.txt saadir airports.csv saa.csv airport.csv runway.csv freq.csv fix.csv nav.csv dof.csv awos.csv aw.csv ourairports.csv DAILY_DOF.ZIP DOF.DAT SaaSubscriberFile.zip
