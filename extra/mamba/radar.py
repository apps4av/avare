#!/usr/bin/python
#Copyright (c) 2021, Apps4Av Inc. (apps4av@gmail.com) 
#All rights reserved.
#
#Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
#
#    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
#    * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
#
#THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
#
# Author zkhan, pgustafson
#

import requests
import bs4
import re
import os 
import datetime

os.system('rm -f CONUS_L2_CREF* conus *latest_radaronly* conus.zip latest.txt')


# there is an html table here
url = 'https://mrms.ncep.noaa.gov/data/RIDGEII/L2/CONUS/CREF_QCD/'

#parse html
soup = bs4.BeautifulSoup(requests.get(url).text, 'lxml')
table = soup.find('table')
listOfFiles = []

# go row by row and find radar images
rows = table.find_all('tr')
for row in rows:
    td = row.find_all('td')
    if len(td) > 0:
        floc = td[0].find('a').get('href')
        if None != floc:
            # parse time out of file names
            fm = re.search("^CONUS_L2_CREF_QCD_(\d*)_(\d*).tif.gz$", floc)
            if None != fm:
                datetimeStr = fm.group(1) + fm.group(2)
                d = {}
                d['date'] = datetime.datetime.strptime(datetimeStr, '%Y%m%d%H%M%S')
                # save the latest
                d['file'] = fm.group(0)
                listOfFiles.append(d)
                

#manifest prep                
os.system('date -u +"%m_%d_%Y_%H:%M_UTC" > conus')
os.system('echo latest.txt >> conus')

# sort
slist = sorted(listOfFiles, key=lambda i: i['date'], reverse=True)
#download latest number of images 
images = ['latest_radaronly', 'latest_radaronly1', 'latest_radaronly2']
for i in range(0, len(images)):
    ftodl = slist[i * 5]['file']
    timeobj = slist[i * 5]['date']
    os.system('wget ' + url + ftodl)
    os.system('gzip -d ' + ftodl)

    # warp from EPSG 4326 to avare recongnized format
    os.system('gdalwarp -r near -s_srs EPSG:4326 -t_srs EPSG:3857 -of gtiff ' + ftodl.replace('.tif.gz', '.tif') + ' ' + images[i] + '.tif')
    #resize to below 2kx2k
    os.system('convert ' + images[i] + '.tif -transparent black -resize 25% ' + images[i] + '.png')
    #save date and coordinates in file
    os.system('gdalinfo ' + images[i] + '.tif -noct | grep -E "Upper Left|Upper Right|Lower Left|Lower Right" | sed \'s/.*(//\' | sed \'s/)//\' >> latest.txt')
    os.system('echo ' + timeobj.strftime('%Y%m%d_%H%M') + '>> latest.txt') #yyyyMMdd_HHmm
    os.system('echo ' + images[i] + '.png >> conus')
    #zip
    os.system('zip conus.zip ' + images[i] + '.png')

#finalize
os.system('zip conus.zip conus latest.txt')
os.system('rm -f CONUS_L2_CREF* conus *latest_radaronly* latest.txt')
