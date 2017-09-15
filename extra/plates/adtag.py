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



import glob, os

# read AP diagram tag database
d = {}
with open("aps.csv") as f:
    for line in f:
        (key, val0, val1, val2, val3, val4, val5, val6, val7, val8, val9, val10, val11) = line.rstrip().split(",")
        v6 = float(val6)
        v7 = float(val7)
        v8 = float(val8)
        v9 = float(val9)
        v10 = float(val10)
        v11 = float(val11)
        d[str(key)] = str(v6) + "," + str(v7) + "," + str(v8) + "," + str(v9) + "," + str(v10) + "," + str(v11)

# now get AD pngs that need to be tagged
for f in glob.iglob("plates/**/*AIRPORT-DIAGRAM.png"):
    plates, airport, name = f.split("/")
    comment = d[airport]
    cmd = "mogrify -quiet -set Comment '" + comment + "' " + f
    # add comment tag
    if 0 != os.system(cmd) :
        print "unable to tag " + f
    
