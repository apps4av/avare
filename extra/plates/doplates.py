# Copyright (c) 2012-2017, Apps4av Inc. (apps4av@gmail.com) 
# Author: Zubair Khan
#All rights reserved.
#
#Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
#
#    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
#    * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
#
#THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
#


from osgeo import gdal
from pyproj import Proj, transform
import sys
import os
from subprocess import call

filename,ext = sys.argv[1].split(".pdf")

#warp
if 0 == os.system("gdalwarp -q -r lanczos -t_srs epsg:3857 " + filename + ".pdf " + filename + ".tif 2> /dev/null"):

    # find coordinates for geo tag in avare
    src = gdal.Open(filename + ".tif")
    ulx, xres, xskew, uly, yskew, yres  = src.GetGeoTransform()

    inProj = Proj(init='epsg:3857')
    outProj = Proj(init='epsg:4326') # this is lon/lat

    w = src.RasterXSize
    h = src.RasterYSize

    x, y = transform(inProj, outProj, ulx, uly)
    x0, y0 = transform(inProj, outProj, ulx + w * xres, uly + h * yres)

    comment = str(w / (x0 - x)) + '|' + str(h / (y0 - y)) + '|' + str(x) + '|' + str(y)

    # convert to png and add geo tag to it under Comment
    if 0 != os.system("mogrify -quiet -dither none -antialias -depth 8 -quality 00 -background white -alpha remove -colors 15 -format png -set Comment '" + comment + "' " + filename + ".tif") :
        print "error in " + filename


else:
    # no geotag info, do convert
    if 0 != os.system("mogrify -dither none -antialias -depth 8 -quality 00 -background white -alpha remove -colors 15 -density 150 -format png " + filename + ".pdf") :
        print "error in " + filename

# optimize png
os.system("optipng -quiet " + filename + ".png")

