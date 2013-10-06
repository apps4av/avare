/*
Copyright (c) 2012, Zubair Khan (governer@gmail.com) 
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.ds.avare.weather;

import java.util.List;

import com.ds.avare.position.Coordinate;
import com.googlecode.jcsv.annotations.MapToColumn;

/**
 * Auto generated from CSV header, cleared to only needed fields
 * 
 * @author zkhan
 *
 */
public class AirSigMet {

    @MapToColumn(column=0)
    public String rawText;
    @MapToColumn(column=1)
    public String timeFrom;
    @MapToColumn(column=2)
    public String timeTo;
    @MapToColumn(column=3)
    public String points;
    @MapToColumn(column=4)
    public String minFt;
    @MapToColumn(column=5)
    public String maxFt;
    @MapToColumn(column=6)
    public String movementDeg;
    @MapToColumn(column=7)
    public String movementKt;
    @MapToColumn(column=8)
    public String hazard;
    @MapToColumn(column=9)
    public String severity;
    @MapToColumn(column=10)
    public String reportType;
    
    public List<Coordinate> coords;
}
