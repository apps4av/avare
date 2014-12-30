Live Taxi coordinates determination.

//+++2013-01-10
//    Copyright (C) 2013, Mike Rieker, Beverly, MA USA
//    Avare, open source moving map aviation GPS (apps4av.com)
//
//    This program is free software; you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation; version 2 of the License.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//
//    EXPECT it to FAIL when someone's HeALTh or PROpeRTy is at RISk.
//
//    You should have received a copy of the GNU General Public License
//    along with this program; if not, write to the Free Software
//    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA


gmcs /reference:System.Drawing.dll ReadArptDgmPng.cs
mono ReadArptDgmPng.exe

     1) Read png file into bitmap so we have an array of pixels.
     2) Make a monochromatic (strict black-and-white) version of the bitmap.
        Any pixel with a>200, r<100, g<100 and b<100 is considered black.
        This makes the bitmap easy to work with by eliminating all the
gray and colored areas.
     3) Remove margin stuff up to and including border lines.
        For each of the 4 edges:
            Scan inward until a full minimum sized row or column of black
pixels is found,
            continuing to scan inward until some white pixels are found.
Then clear up to
            and including all the black rows or columns found.
     4) Clear out any large black blobs (ie, anything 4x4 or bigger).
        This gets rid of most buildings and runways etc.
        It leaves the tick-marked lat/lon lines intact.
        Do not clear 3x3 blobs because some lat/lon lines are that thick.
     5) Clear out any isolated figures 50x50 pixels or smaller.
        This also gets rid of a lot of edge junk left over by the large
black blob filtering.
     6) Repeat the following using slopesz of 24,26,28,30,32,34,36,38:
         a) Scan the image for a slopesz x slopesz sized rectangle of
pixels that has these characteristics:
            - a black pixel in the center (so we get our potential line
segment nearly centerd in the box for
              best measurement)
            - exactly two contiguous black pixel groups around its perimeter
              (so we have something to measure the slope of)
            - those two perimeter points must be on exact opposite sides
of the rectangle +/- 1 pixel
                which means we presume it is a line segment going through
the center of the rectangle
            - near the center there is a barb of pixels perpendicular to
the presumed line
              between the two perimeter points (so we know we have a
lat/lon line and not just some stray mark)
            - either every row of the box must be occupied by at least one
black pixel or
              at least one column must be occupied by at least one black
pixel (so we know we have a continuous
              line and not a group of stray marks)
         b) Measure the slope by using the two points found on the
perimeter.  Use average if the line is thicker
            than one pixel.
         c) Save the line segment in a list, grouping them by slope.
        Repeat using different sizes as the barbs cannot penetrate the
perimeter of the box (as it would make a
        third group of black pixels).  By having that restriction, we
limit the size of the barbs we accept and so
        we don't get fooled by lines that are part of stray boxes.
     7) Look for the most frequently appearing slope and consider it to be
the major slope.
        Find the most popular minor slope by looking at those with a near
negative reciprocal.
        We don't yet know which are lats and which are lons yet though.
     8) Rotate the original colored image by the major angle so as to make
the lat/lon lines strictly vertical and
        horizontal.  Then monochromaticise it to make scanning for
character strings easier.
     9) Build a list of where the rotated lat/lon lines end up in the
rotated image.  Each will need just an
        X or a Y to describe it.
    10) Scan rotated image for groups of pixels that fit in a 30 x 30 or
smaller rectangle.
    11) For each box found, try to classify it as one of the characters we
care about, ie, 0-9, ^, ', ., N, S, E, W.
         a) normalize char box size to 13x17 grayscale pixels
         b) compare normalized char to idealized grayscale 13x17
characters, pixel by pixel,
            creating a sum of squared differences
         c) choose the char that has the smallest difference sum as the
proper decoding
        Crude but effective.
    12) Sort where those chars were found by ascending X-axis value (with
Y-axis value as a minor key).
    13) Build strings of proper lat/lon form from that list, by scanning
it left to right, considering only characters
        that have the same approximate Y value for any given scan.
        Repeat this step in all 4 orientations of the image as various
diagrams have the strings drawn in all ways.
    14) For each string found, associate it with nearest lat/lon tick
marked lines.
    15) Determine if the rotated image (from step 8) is considered
portrait or landscape by looking at the association
        of lat/lon strings with vertical/horizontal lat/lon tick marked
lines.  Note that some lat/lon strings may be
        near both an horizontal and vertical tick marked line.  We simply
use those that have a unique association and
        then impose that association on the ambiguous ones based on
whether they are N/S or E/W.
        Portrait means lat lines are horizontal, Landscape means lat lines
are vertical in the rotated image.
    16) Validate the result by checking that the resultant pixels are
square (lat/lon-wise) and all of the found lat
        and all of the found lon markers are evenly spaced.  This checks
to see that we didn't misread anything.
    17) Generate resultant latlon<->pixel conversion matrices by using the
spacing and tagging of the lat/lon tick
        marked lines and the angle used to rotate the image.
