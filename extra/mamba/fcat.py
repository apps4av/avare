#!/usr/bin/python
#Copyright (c) 2015, Apps4Av Inc. (apps4av@gmail.com) 
#All rights reserved.
#
#Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
#
#    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
#    * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
#
#THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
#
#
# Author: zkhan

import sqlite3
from osgeo import gdal, ogr, osr
import os
import numpy as np

# find coordinate of the lon/lat pair in an array
def coord2pixelOffset(rasterfn,x,y):
    raster = gdal.Open(rasterfn)
    geotransform = raster.GetGeoTransform()
    originX = geotransform[0]
    originY = geotransform[3]
    pixelWidth = geotransform[1]
    pixelHeight = geotransform[5]
    xOffset = int((x - originX)/pixelWidth)
    yOffset = int((y - originY)/pixelHeight)
    return xOffset,yOffset


# convert a raster with 3 bands to 3 arrays
def raster2array(rasterfn):
    raster = gdal.Open(rasterfn)
    band1 = raster.GetRasterBand(1)
    band2 = raster.GetRasterBand(2)
    band3 = raster.GetRasterBand(3)
    return band1.ReadAsArray(), band1.ReadAsArray(), band2.ReadAsArray()

# convert 3 arrays to raster with 3 bands
def array2raster(rasterfn,newRasterfn,array1,array2,array3):
    raster = gdal.Open(rasterfn)
    geotransform = raster.GetGeoTransform()
    originX = geotransform[0]
    originY = geotransform[3]
    pixelWidth = geotransform[1]
    pixelHeight = geotransform[5]
    cols = raster.RasterXSize
    rows = raster.RasterYSize

    driver = gdal.GetDriverByName('GTiff')
    outRaster = driver.Create(newRasterfn, cols, rows, 3, gdal.GDT_Byte)
    outRaster.SetGeoTransform((originX, pixelWidth, 0, originY, 0, pixelHeight))
    outband = outRaster.GetRasterBand(1)
    outband.WriteArray(array1)
    outband = outRaster.GetRasterBand(2)
    outband.WriteArray(array2)
    outband = outRaster.GetRasterBand(3)
    outband.WriteArray(array3)
    outRasterSRS = osr.SpatialReference()
    outRasterSRS.ImportFromWkt(raster.GetProjectionRef())
    outRaster.SetProjection(outRasterSRS.ExportToWkt())
    outband.FlushCache()

# main
# this is a template
rasterfn = 'flight_category.tif'

#  make a new tiff in ESPG4326
driver = gdal.GetDriverByName('GTiff')  
# this size is big enough not to overlfow android bitmap
ds = driver.Create(rasterfn, 1680, 1000, 3, gdal.GDT_Byte)  
proj = osr.SpatialReference()  
proj.SetWellKnownGeogCS("EPSG:4326")  
ds.SetProjection(proj.ExportToWkt())
# only cover CON US for now
geotransform = (-127, 0.036, 0, 51, 0, -0.028)
ds.SetGeoTransform(geotransform) 
ds = None 

# Convert Raster to array
rasterArray1,rasterArray2,rasterArray3 = raster2array(rasterfn)

# get flight conditions from weather.db
conn = sqlite3.connect('weather.db')
c = conn.cursor()
# for all metars
for row in c.execute("SELECT flight_category,longitude,latitude FROM metars"):
    # try to get lon/lat
    try:
        x, y = coord2pixelOffset(rasterfn, float(row[1]), float(row[2]))
    except:
        continue;

    # bound by limits of image
    if x < 0 or y < 0 or x > 1679 or y > 999:
        continue
    # set colors based on flight category (RGB)
    if row[0] == 'IFR':
        rasterArray1[y][x] = 255 
        rasterArray2[y][x] = 0
        rasterArray3[y][x] = 0
    elif row[0] == 'VFR':
        rasterArray1[y][x] = 0
        rasterArray2[y][x] = 127 
        rasterArray3[y][x] = 0
    elif row[0] == 'MVFR':
        rasterArray1[y][x] = 0
        rasterArray2[y][x] = 0
        rasterArray3[y][x] = 127 
    elif row[0] == 'LIFR':
        rasterArray1[y][x] = 127
        rasterArray2[y][x] = 0
        rasterArray3[y][x] = 127

conn.close()


# Write updated array to new raster
array2raster(rasterfn,rasterfn,rasterArray1,rasterArray2,rasterArray3)

