# Copyright (c) 2012-2015, Apps4av Inc. (apps4av@gmail.com) 
# Author: Zubair Khan (governer@gmail.com)
#All rights reserved.
#
#Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
#
#    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
#    * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
#
#THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
#

import xml.etree.ElementTree as ET
import shutil
import os 
import urllib

tree = ET.parse('d-TPP_Metafile.xml')
root = tree.getroot()

for state in root.findall('state_code'):

    # state
    stname  = state.get('ID')
    folder = "./" + 'plates_' + stname
    if os.path.exists(folder):
        shutil.rmtree(folder)
    os.makedirs(folder)

    # airport in state/city
    for city in state.findall('city_name'):
        for airport in city.findall('airport_name'):
            apid = airport.get('apt_ident')
            apfolder = folder + "/" + apid
            if os.path.exists(apfolder):
                shutil.rmtree(apfolder)
            os.makedirs(apfolder)

            # record
            for record in airport.findall('record'):
                # remove () comma, then replace space and slahes by dash
                name = record.find('chart_name').text
		name = name.replace("," ,"")
		name = name.replace("'", "")
		name = name.replace("(", "")
		name = name.replace(")", "")
		name = name.replace(" ", "-")
		name = name.replace("/", "-")
		name = name.replace("\\", "-")
                code = record.find('chart_code').text
                pdf = record.find('pdf_name').text
                if code == "MIN":
                    continue
               
                pdfname = apfolder + "/" + name + ".pdf"
                print "downloading " + pdfname
                urllib.urlretrieve("http://localhost/plates/" + pdf, apfolder + "/" + name + ".pdf")
