# Copyright (c) Apps4Av Inc. (apps4av@gmail.com)
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

# This script will generate everything except charts
# 1. Set up an apache server at http://localhost 
# 2. Install Python (with sqlite3, mapnik, urllib, shutils, pdftools-for pdf2txt), PERL (with XML, XML XPath, File, Archive::Zip, File Slurp, LWP Simple), ImageMagick, sqlite3, pdftotext, make, wget, unzip
# 3. Put data in server http://localhost under:
# 	Unzip all plate products from FAA (http://www.faa.gov/air_traffic/flight_info/aeronav/digital_products/dtpp/ DDTPP*_XX.zip) under /plates/
# 	Unzip AFD product from FAA (http://www.faa.gov/air_traffic/flight_info/aeronav/digital_products/dafd/ DAFD_XX.zip) under /afd/
# 	Unzip NFDC product from FAA (https://nfdc.faa.gov/fadds/subscriptionDownload.do?productId=548752 56DySubscription_XYZ.zip) under /nfdc/
# 4. place http://www.navcanada.ca/EN/products-and-services/Documents/CanadianAirportCharts_Next.pdf in /can
# 5. make CYCLE=1508

	
