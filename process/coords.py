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

from osgeo import gdal,ogr,osr
from gdalconst import *
import sys

def GetExtent(gt,cols,rows):
    ''' Return list of corner coordinates from a geotransform

    '''

    px=0;
    py=0;
    lonul=gt[0]+(px*gt[1])+(py*gt[2])
    latul=gt[3]+(px*gt[4])+(py*gt[5])
    
    px=0;
    py=rows;
    lonll=gt[0]+(px*gt[1])+(py*gt[2])
    latll=gt[3]+(px*gt[4])+(py*gt[5])

    px=cols;
    py=0;
    lonur=gt[0]+(px*gt[1])+(py*gt[2])
    latur=gt[3]+(px*gt[4])+(py*gt[5])
    
    px=cols;
    py=rows;
    lonlr=gt[0]+(px*gt[1])+(py*gt[2])
    latlr=gt[3]+(px*gt[4])+(py*gt[5])

    px=cols / 2;
    py=rows / 2;
    lonc=gt[0]+(px*gt[1])+(py*gt[2])
    latc=gt[3]+(px*gt[4])+(py*gt[5])

    print "%f,%f,%f,%f,%f,%f,%f,%f,%f,%f," % (lonul, latul, lonll, latll, lonur, latur, lonlr, latlr, lonc, latc)

    return;

ds=gdal.Open(sys.argv[1], GA_ReadOnly)
gt=ds.GetGeoTransform()
cols = ds.RasterXSize
rows = ds.RasterYSize
GetExtent(gt,cols,rows)
