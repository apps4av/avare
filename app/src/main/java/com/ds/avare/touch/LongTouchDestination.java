/*-
 * SPDX-License-Identifier: BSD-2-Clause
 *
 * Copyright (c) 2012, Apps4Av Inc. (apps4av.com)
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice unmodified, this list of conditions, and the following
 *    disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.ds.avare.touch;

import com.ds.avare.weather.Airep;
import com.ds.avare.weather.Metar;
import com.ds.avare.weather.Taf;
import com.ds.avare.weather.WindsAloft;

import java.util.LinkedList;

/**
 * Works with LongTouchGesture
 * @author zkhan
 *
 */
public class LongTouchDestination {

    private String airport;
    private String info;
    private String tfr;
    private String mets;
    private Taf taf;
    private WindsAloft wa;
    private Metar metar;
    private String sua;
    private String layer;
    private LinkedList<Airep> airep;
    private String performance;
    private String navaids;
    private boolean more;


    public String getAirport() {
        return airport;
    }

    public void setAirport(String airport) {
        this.airport = airport;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public String getTfr() {
        return tfr;
    }

    public void setTfr(String tfr) {
        this.tfr = tfr;
    }

    public String getMets() {
        return mets;
    }

    public void setMets(String mets) {
        this.mets = mets;
    }

    public Taf getTaf() {
        return taf;
    }

    public void setTaf(Taf taf) {
        this.taf = taf;
    }

    public WindsAloft getWa() {
        return wa;
    }

    public void setWa(WindsAloft wa) {
        this.wa = wa;
    }

    public Metar getMetar() {
        return metar;
    }

    public void setMetar(Metar metar) {
        this.metar = metar;
    }

    public String getSua() {
        return sua;
    }

    public void setSua(String sua) {
        this.sua = sua;
    }

    public String getLayer() {
        return layer;
    }

    public void setLayer(String layer) {
        this.layer = layer;
    }

    public LinkedList<Airep> getAirep() {
        return airep;
    }

    public void setAirep(LinkedList<Airep> airep) {
        this.airep = airep;
    }

    public String getPerformance() {
        return performance;
    }

    public void setPerformance(String performance) {
        this.performance = performance;
    }

    public String getNavaids() {
        return navaids;
    }

    public void setNavaids(String navaids) {
        this.navaids = navaids;
    }

    public boolean hasMoreButtons() {
        return more;
    }

    public void setMoreButtons(boolean more) {
        this.more = more;
    }

}
