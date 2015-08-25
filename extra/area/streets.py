#!/usr/bin/env python2

#Copyright (c) 2015, Apps4Av Inc.
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

# Need mapnik import of dev in Ubuntu, install from synaptic
from mapnik import *
import sqlite3 as lite
import math
import urllib
import sys
import shutil

def todb(values,proc):
	try:
	    con = lite.connect('areaplates.db')

	    cur = con.cursor()  

	    script = "CREATE TABLE IF NOT EXISTS geoplates(proc varchar(128), dx float, dy float, lon float, lat float);" 
	    script += "DELETE FROM geoplates where proc='" + proc + "';";
	    script += "INSERT INTO geoplates VALUES(" + values + ");";

	    cur.executescript(script)

	    con.commit()
	    
	except lite.Error, e:
	    
	    if con:
		con.rollback()
		
	    print "Error %s:" % e.args[0]
	    sys.exit(1)
	    
	finally:
	    
	    if con:
		con.close() 

def set_map(m,fname):

    # White BG
    m.background = Color('white')

    # A style that shows
    s = Style()

    # rules

    # rule for runway
    r1 = Rule()
    f1 = Filter("[aeroway] = 'runway'")
    r1.filter = f1

    ln1 = LineSymbolizer()
    stk1 = Stroke(Color('black'),1.0)
    stk1.width = 8;
    ln1.stroke = stk1
    
    tx1 = TextSymbolizer(Expression('[ref]'), 'DejaVu Sans Book', 20, Color('red'))
    tx1.halo_fill = Color('white')
    tx1.halo_radius = 2
    tx1.label_placement = label_placement.LINE_PLACEMENT
    dir(tx1)
    
    tx12 = TextSymbolizer(Expression('[name]'), 'DejaVu Sans Book', 20, Color('red'))
    tx12.halo_fill = Color('white')
    tx12.halo_radius = 2
    tx12.label_placement = label_placement.LINE_PLACEMENT
    dir(tx12)
    
    r1.symbols.append(ln1)
    r1.symbols.append(tx1)
    r1.symbols.append(tx12)
    s.rules.append(r1)

    # rule for taxiway
    r2 = Rule()
    f2 = Filter("[aeroway] = 'taxiway'")
    r2.filter = f2

    ln2 = LineSymbolizer()
    stk2 = Stroke(Color('green'),1.0)
    stk2.width = 2;
    stk2.line_join = line_join.ROUND_JOIN
    stk2.line_cap = line_cap.ROUND_CAP
    ln2.stroke = stk2
    
    tx2 = TextSymbolizer(Expression('[ref]'), 'DejaVu Sans Book', 16, Color('red'))
    tx2.halo_fill = Color('white')
    tx2.halo_radius = 2
    tx2.label_placement = label_placement.LINE_PLACEMENT
    dir(tx2)
    
    tx22 = TextSymbolizer(Expression('[name]'), 'DejaVu Sans Book', 16, Color('red'))
    tx22.halo_fill = Color('white')
    tx22.halo_radius = 2
    tx22.label_placement = label_placement.LINE_PLACEMENT
    dir(tx22)
    
    r2.symbols.append(ln2)
    r2.symbols.append(tx2)
    r2.symbols.append(tx22)
    s.rules.append(r2)
   
    # rule for others
    r3 = Rule()
    f3 = Filter("(not [aeroway] = 'runway') and (not [aeroway] = 'taxiway')")
    r3.filter = f3

    ln3 = LineSymbolizer()
    stk3 = Stroke(Color('blue'),1.0)
    stk3.width = 1;
    stk3.line_join = line_join.ROUND_JOIN
    stk3.line_cap = line_cap.ROUND_CAP
    ln3.stroke = stk3
    
    tx3 = TextSymbolizer(Expression('[name]'), 'DejaVu Sans Book', 14, Color('black'))
    tx3.halo_fill = Color('white')
    tx3.halo_radius = 1
    tx3.label_placement = label_placement.LINE_PLACEMENT
    dir(tx3)
    
    r3.symbols.append(ln3)
    r3.symbols.append(tx3)
    s.rules.append(r3)


    # Put style in
    m.append_style('avare style',s)

    # single layer
    layer = Layer('avare layer')

    # file to load data from, put in layer
    ds = Osm(file=fname)
    layer.datasource = ds
    layer.styles.append('avare style')
    m.layers.append(layer)

def do_one(airport,lonc,latc):

	# max size we can support
	x = 1664
	y = 1664
	mapfile = 'mapnik_style.xml'
	map_output = 'AREA.png'

	width = 0.015 / math.cos(latc / 57.3)
	height = 0.015

	# chart calculations
	latu=latc + height
	latd=latc - height
	lonl=lonc - width
	lonr=lonc + width
	latdiff = latd - latu
	londiff = lonr - lonl
	dx = x / londiff
	dy = y / latdiff

	#OSM projection
	inproj = Projection('+init=epsg:4326')
	#Map projection
	outproj = Projection('+proj=merc +a=6378137 +b=6378137 +lat_ts=0.0 +lon_0=0.0 +x_0=0.0 +y_0=0 +k=1.0 +units=m +nadgrids=@null +no_def  +over')

	# Download area of our airport
	if not os.path.exists(airport + ".osm"):
	    url = "http://overpass-api.de/api/map?bbox=" + str(lonl) + "," + str(latd) + "," + str(lonr) + "," + str(latu)
	    urllib.urlretrieve (url, airport + ".osm")
        
	m = Map(x,y)
	set_map(m, airport + ".osm")

	# projection
	m.srs = outproj.params()

	# bounded by
	bbox=(Envelope(lonl, latu, lonr, latd))

	# our bound must be transformed
	transform = ProjTransform(inproj,outproj)

	m.zoom_to_box(transform.forward(bbox))

	# Store in plates folder
	if not os.path.exists("area"):
	    os.makedirs("area")

	if not os.path.exists("area/" + airport):
	    os.makedirs("area/" + airport)

    	# render the map to an image
    	im = Image(x,y)
    	render(m, im)
    	im.save("area/" + airport + "/" + map_output,'png8:z=9:t=0')

	# This for database
	out = "'" + airport + "/" + map_output + "','" + str(dx) + "','" + str(dy) + "','" + str(lonl) + "','" + str(latu) + "'"
	todb(out,airport + "/" + map_output)

	# Show done
	print airport + " done" 

# Main looking for something like BED|-71.289|42.4699444444444 direct from database
inp = sys.argv[1].split("|")
do_one(str(inp[0]),float(inp[1]),float(inp[2]))

