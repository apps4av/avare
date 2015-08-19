# Copyright (c) 2015 Apps4Av Inc.
# Author Zubair Khan (governer@gmail.com), Peter A. Gustafson (peter.gustafson@wmich.edu)
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

export NP=8

# Canada
num=0
rm -rf area 
mkdir area
for input in `sqlite3 main.db "select LocationID,ARPLongitude,ARPLatitude from airports where Type=='OUR-AP' and State='CN'"`; do
    echo $input;
    ${MODULE_DIR}/streets.py $input &
    num=$((num + 1));
    if [ $num -eq ${NP} ] ; then wait ; num=0 ; fi;
done
files=`sqlite3 main.db "select LocationID from airports where Type=='OUR-AP' and State='CN'"`
rm -f AreaCN.zip
zip -r -9 AreaCN.zip `echo $files | sed 's/\([a-zA-Z0-9]*\)/area\/\1/g'`


#USA
STATES="PR AL AK AZ AR CA CO CT DE FL GA HI ID IL IN IA KS KY LA ME MD MA MI MN MS MO MT NE NV NH NJ NM NY NC ND OH OK OR PA RI SC SD TN TX UT VT VA WA WV WI WY"

num=0
rm -rf area 
mkdir area
for input in `sqlite3 main.db "select LocationID,ARPLongitude,ARPLatitude from airports where Type=='AIRPORT'"`; do
    echo $input;
    ${MODULE_DIR}/streets.py $input &
    num=$((num + 1));
    if [ $num -eq ${NP} ] ; then wait ; num=0 ; fi;
done

# zip up
for state in ${STATES}; do
    files=`sqlite3 main.db "select LocationID from airports where Type=='AIRPORT' and state='${state}'"`
    rm -f Area$state.zip
    zip -r -9 Area$state.zip `echo $files | sed 's/\([a-zA-Z0-9]*\)/area\/\1/g'`
done
