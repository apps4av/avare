#!/usr/bin/python
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
#zkhan

# Install python, python pyproj, gdal, gdal-bin, python gdal, imagemagik
# The charts to be tagged need to be in UTM projection with central meridian specified (read from the chart)
# 
# Open a file named input.txt and put names of all input files
# An input file represents one chart and looks like:
#  filename=d_1.jpg
#  lon0=15
#  xy0=9191 5666
#  lonlat0=0 56
#  xy1=4899 3232
#  lonlat1=-11 60
#  xy2=914 60
#  lonlat2=-24 64
#  xy3=3155 3169
#  lonlat3=-16 60
#  dims=-22 64 0 56
#
# where, 
# lat0 is central meridian (specified with projection)
# xy0 is the x and y coordinates of a point you choose on the chart (in pixels)
# lonlat0 is the longitude and latitude of the xy0 point (in decimal N is +, S is -, E is +, W is -)
# similarly xy1, lonlat1, xy2, lonlat2, xy3, lonlat3 are chosen well apart from each other and on a diagonal. These are the GCP.
# find dims. This is lon, lat, lon, lat of top and bottom corners and used to strip out white boundaries of the chart

# once all charts to process are specified with their input files, and the input files put in the input.txt, the script is run, and a tiles folder is created where Avare readable tiles are placed.
# Open the file tiles/10/openlayers.html in an Internet browser to see the result on google maps overlay. 

import os
import filecmp 
import zipfile 
from pyproj import Proj, transform

# WGS84 lon, lat to UTM coordinates
def getCoords(lonlat, lon0):
	inProj = Proj(init='epsg:4326') # this is lon/lat
	outProj = Proj(proj='tmerc', lon_0 = float(lon0))
	coords = lonlat.split(" ")
	x, y = transform(inProj, outProj, float(coords[0]), float(coords[1]))
	return str(x) + " " + str(y) # this is UTM

# get projection of top left, bottom right corners
def getDims(dims):
	inProj = Proj(init='epsg:4326') # this is lon/lat
	outProj = Proj(init='epsg:3857') # 900913
	coords = dims.split(" ")
	x0, y0 = transform(inProj, outProj, float(coords[0]), float(coords[1]))
	x1, y1 = transform(inProj, outProj, float(coords[2]), float(coords[3]))
	return str(x0) + " " + str(y0) + " " + str(x1) + " " + str(y1) # this is UTM

# input file read
def readOneInput(fname):
	with open(fname) as f:
	    content = f.readlines()

	output = {} 
		
	for line in content:
		tokens = line.rstrip().split('=')
		if len(tokens) == 2:
			output[tokens[0]] = tokens[1]
	return output

def delFile(filename):
	if os.path.exists(filename):
		os.remove(filename)

# project from UTM to google 900913 (epsg:3857)
def projectIt(inputf):

	delFile(inputf["filename"] + ".tif")
	delFile("tpc.tif")
	
	lon0 = inputf["lon0"]
	# gcp x y lon lat
	gcp0 = inputf["xy0"] + " " + getCoords(inputf["lonlat0"], lon0) 
	gcp1 = inputf["xy1"] + " " + getCoords(inputf["lonlat1"], lon0) 
	gcp2 = inputf["xy2"] + " " + getCoords(inputf["lonlat2"], lon0)
	gcp3 = inputf["xy3"] + " " + getCoords(inputf["lonlat3"], lon0)

	# give ground control points
	string = "gdal_translate -a_srs '+proj=tmerc +lon_0=" + lon0 + "' -gcp " + gcp0 + " -gcp " + gcp1 + " -gcp " + gcp2 + " -gcp " + gcp3 + " " + inputf["filename"] + " " + " temp"
	print string
	os.system(string)

	# warp to google
	string = "gdalwarp -order 1 -r cubic -dstnodata '51'  -t_srs 'epsg:3857' temp temp2"
	print string
	os.system(string)
	
	# crop, compress
	dims = inputf["dims"]
	string = ""
	if dims == 'full':
		string = "gdal_translate -co tiled=yes -co blockxsize=512 -co blockysize=512 -co compress=deflate " +  " temp2 " + inputf["filename"] + ".tif"
	else:
		string = "gdal_translate -co tiled=yes -co blockxsize=512 -co blockysize=512 -co compress=deflate " +  "-projwin " + getDims(dims) + " temp2 " + inputf["filename"] + ".tif"
	print string
	os.system(string)

	# clean up 
	delFile("temp")
	delFile("temp2")

# tile and etc.
def final(all):
	#final image
	string = "gdalwarp -order 1 -r cubic " + all + " tpc.tif"
	print string
	os.system(string)
	#final tiles
	string = "./gdal2tiles.py -r cubic -w openlayers -c MUAVLLC --no-kml --resume -t 'TPC' tpc.tif tiles/11" # put in tpc folder
	print string
	os.system(string)


# read list of files to process in input
def readInput():
	allfiles = ""
	with open('input.txt') as f:
	    content = f.readlines()
	for fl in content:
		if fl.startswith("#") :
			continue # skip comment
		inputf = readOneInput(fl.rstrip())
		projectIt(inputf)
		allfiles += inputf["filename"] + ".tif "
	final(allfiles)

# zip, convert to png into file called input.zip
def zipit():
	# compress, zip
	zipf = zipfile.ZipFile('input.zip', 'w')
	for (dir, _, files) in os.walk("tiles"):
		for f in files:
			path = os.path.join(dir, f)
			if path.endswith(".png"):
				print path
				# skip black files
				if filecmp.cmp(path, 'black.png'):
					continue
				# JPEG others
				os.system("mogrify -format jpg -quality 90 " + path)
				zipf.write(path.replace(".png", ".jpg"))


# main
readInput()
zipit()

