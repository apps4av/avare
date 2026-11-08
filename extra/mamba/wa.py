#!/usr/bin/env python3
#Copyright (c) 2026, Apps4Av Inc. (apps4av@gmail.com)
#All rights reserved.
#
#Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
#
#    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
#    * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
#
#THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
#
# Winds aloft for the wa table of weather.db, printed as wa.csv on stdout.
#
# The legacy FD text product (/api/data/windtemp) was discontinued with the NAM
# model, except for the Pacific territories. This reads the GFA forecast the GFA
# map is drawn from (/api/data/wind: direction and speed on a 2 degree lattice,
# one level and one forecast hour per request) at each winds aloft station and
# writes FD wind groups, the same as AvareX does, so the app is unchanged.
#
# Columns: stationid,valid,longitude,latitude,w3k,w6k,w9k,w12k,w18k,w24k,w30k,w34k,w39k

import json
import math
import sys
import urllib.error
import urllib.parse
import urllib.request
from concurrent.futures import ThreadPoolExecutor
from datetime import datetime, timedelta, timezone

HOST = "https://aviationweather.gov"
HEADERS = {"User-Agent": "Avare"}

# Same 06 hour forecast the legacy product had.
FORECAST_HOURS = 6

# GFA wind levels: surface, then hundreds of feet.
LEVELS = ["sfc", "030", "060", "090", "120", "180", "240", "300", "360", "420"]

# Lattice spacing is 2^(9 - zoom) * 0.125 degrees, zoom 5 is 2 degrees, about the
# spacing of the stations. One box for CONUS, Alaska, Hawaii and Puerto Rico.
ZOOM = 5
BBOX = "17,-180,72,-60"

# Winds aloft stations: id, latitude, longitude.
STATIONS = [
    ("BHM", 33.55, -86.7333), ("HSV", 34.55, -86.7667), ("MGM", 32.2167, -86.3167),
    ("MOB", 30.6833, -88.2333), ("ADK", 51.9333, -176.4167), ("ADQ", 57.7667, -152.5833),
    ("AKN", 58.7333, -156.75), ("ANC", 61.2333, -149.55), ("ANN", 55.05, -131.6167),
    ("BET", 60.5833, -161.5833), ("BRW", 71.2833, -156.5167), ("BTI", 70.1667, -143.9167),
    ("BTT", 66.9, -151.5), ("CDB", 55.1833, -162.3667), ("CZF", 61.7833, -166.0333),
    ("EHM", 58.65, -162.0667), ("FAI", 64.7167, -148.1833), ("FYU", 66.5833, -145.0833),
    ("GAL", 64.7333, -156.9333), ("GKN", 62.15, -145.45), ("HOM", 59.65, -151.4833),
    ("JNU", 58.4333, -134.6833), ("LUR", 68.8833, -166.1167), ("MCG", 62.8167, -155.4),
    ("MDO", 59.5, -146.3), ("OME", 64.6167, -165.0833), ("ORT", 63.0667, -142.0667),
    ("OTZ", 66.65, -162.9), ("SNP", 57.15, -170.6167), ("TKA", 62.3167, -150.1),
    ("UNK", 63.8833, -160.8), ("YAK", 59.6167, -139.5), ("IKO", 52.95, -168.85),
    ("AFM", 67.1, -157.85), ("5AB", 52.4167, 176.0), ("5AC", 52.0, -135.0), ("5AD", 54.0, -145.0),
    ("5AE", 55.0, -155.0), ("5AF", 56.0, -137.0), ("5AG", 58.0, -142.0), ("FSM", 35.3833, -94.2667),
    ("LIT", 34.6667, -92.1667), ("PHX", 33.4167, -111.8833), ("PRC", 34.7, -112.4667),
    ("TUS", 32.1167, -110.8167), ("BIH", 37.3667, -118.35), ("BLH", 33.5833, -114.75),
    ("FAT", 36.8833, -119.8), ("FOT", 40.6667, -124.2333), ("ONT", 34.05, -117.6),
    ("RBL", 40.0833, -122.2333), ("SAC", 38.4333, -121.55), ("SAN", 32.7333, -117.1833),
    ("SBA", 34.5, -119.7667), ("SFO", 37.6167, -122.3667), ("SIY", 41.7833, -122.45),
    ("WJF", 34.7333, -118.2167), ("ALS", 37.3333, -105.8), ("DEN", 39.8, -104.8833),
    ("GJT", 39.05, -108.7833), ("PUB", 38.2833, -104.4167), ("BDL", 41.9333, -72.6833),
    ("EYW", 24.5833, -81.8), ("JAX", 30.4333, -81.55), ("MIA", 25.95, -80.45),
    ("MLB", 28.1, -80.6333), ("PFN", 30.2, -85.6667), ("PIE", 27.9, -82.6833),
    ("TLH", 30.55, -84.3667), ("ATL", 33.6167, -84.4333), ("CSG", 32.6, -85.0167),
    ("SAV", 32.15, -81.1), ("ITO", 19.7167, -155.05), ("HNL", 21.3167, -157.9167),
    ("LIH", 21.9833, -159.3333), ("OGG", 20.9, -156.4333), ("LNY", 20.7833, -156.95),
    ("KOA", 19.7333, -156.05), ("BOI", 43.5667, -116.2333), ("LWS", 46.3667, -116.8667),
    ("PIH", 42.8667, -112.65), ("JOT", 41.5333, -88.3167), ("SPI", 39.8333, -89.6667),
    ("EVV", 38.0333, -87.5167), ("FWA", 40.9667, -85.1833), ("IND", 39.8, -86.3667),
    ("BRL", 40.7167, -90.9167), ("DBQ", 42.4, -90.7), ("DSM", 41.4333, -93.6333),
    ("MCW", 43.0833, -93.3167), ("GCK", 37.9167, -100.7167), ("GLD", 39.3833, -101.6833),
    ("ICT", 37.7167, -97.45), ("SLN", 38.8667, -97.6167), ("LOU", 38.1, -85.5667),
    ("LCH", 30.1333, -93.1), ("MSY", 30.0167, -90.1667), ("SHV", 32.7667, -93.8),
    ("BGR", 44.8333, -68.8667), ("CAR", 46.8667, -68.0167), ("PWM", 43.6333, -70.3),
    ("EMI", 39.4833, -76.9667), ("ACK", 41.2667, -70.0167), ("BOS", 42.35, -70.9833),
    ("ECK", 43.25, -82.7167), ("MKG", 43.1667, -86.0333), ("MQT", 46.5167, -87.5833),
    ("SSM", 46.4, -84.3), ("TVC", 44.6667, -85.5333), ("AXN", 45.95, -95.2167),
    ("DLH", 46.8, -92.2), ("INL", 48.55, -93.4), ("MSP", 45.1333, -93.3667),
    ("CGI", 37.2167, -89.5667), ("COU", 38.8, -92.2167), ("MKC", 39.2667, -94.5833),
    ("SGF", 37.35, -93.3333), ("STL", 38.85, -90.4667), ("JAN", 32.5, -90.1667),
    ("BIL", 45.8, -108.6167), ("DLN", 45.2333, -112.5333), ("GPI", 48.2, -114.1667),
    ("GGW", 48.2, -106.6167), ("GTF", 47.45, -111.4), ("MLS", 46.3667, -105.95),
    ("HAT", 35.2667, -75.55), ("ILM", 34.35, -77.8667), ("RDU", 35.8667, -78.7833),
    ("DIK", 46.85, -102.7667), ("GFK", 47.95, -97.1833), ("MOT", 48.25, -101.2833),
    ("BFF", 41.8833, -103.4667), ("GRI", 40.9833, -98.3), ("OMA", 41.1667, -95.7333),
    ("ONL", 42.4667, -98.6833), ("BML", 44.6333, -71.1833), ("ACY", 39.45, -74.5667),
    ("ABQ", 35.0333, -106.8), ("FMN", 36.7333, -108.0833), ("ROW", 33.3333, -104.6167),
    ("TCC", 35.1667, -103.5833), ("ZUN", 34.95, -109.15), ("BAM", 40.5667, -116.9167),
    ("ELY", 39.2833, -114.8333), ("LAS", 36.0667, -115.15), ("RNO", 39.5167, -119.65),
    ("ALB", 42.7333, -73.8), ("BUF", 42.9167, -78.6333), ("JFK", 40.6167, -73.7667),
    ("PLB", 44.8, -73.4), ("SYR", 43.15, -76.2), ("CLE", 41.35, -82.15), ("CMH", 39.9833, -82.9167),
    ("CVG", 39.0, -84.7), ("GAG", 36.3333, -99.8667), ("OKC", 35.4, -97.6333),
    ("TUL", 36.1833, -95.7833), ("AST", 46.15, -123.8667), ("IMB", 44.6333, -119.7),
    ("LKV", 42.4833, -120.5), ("OTH", 43.4, -124.1667), ("PDX", 45.7333, -122.5833),
    ("RDM", 44.25, -121.3), ("AGC", 40.2667, -80.0333), ("AVP", 41.2667, -75.6833),
    ("PSB", 40.9, -77.9833), ("CAE", 33.85, -81.05), ("CHS", 32.8833, -80.0333),
    ("FLO", 34.2167, -79.65), ("GSP", 34.8833, -82.2167), ("ABR", 45.4167, -98.3667),
    ("FSD", 43.6333, -96.7667), ("PIR", 44.3833, -100.15), ("RAP", 43.9667, -103.0),
    ("BNA", 36.1167, -86.6667), ("MEM", 35.05, -89.9667), ("TRI", 36.4667, -82.4),
    ("TYS", 35.9, -83.8833), ("ABI", 32.4667, -99.85), ("AMA", 35.2833, -101.6333),
    ("BRO", 25.9167, -97.3667), ("CLL", 30.6, -96.4167), ("CRP", 27.9, -97.4333),
    ("DAL", 32.8333, -96.85), ("DRT", 29.3667, -100.9167), ("ELP", 31.8, -106.2667),
    ("HOU", 29.6333, -95.2667), ("INK", 31.8667, -103.2333), ("LBB", 33.7, -101.9),
    ("LRD", 27.4667, -99.4167), ("MRF", 30.2833, -103.6167), ("PSX", 28.75, -96.3),
    ("SAT", 28.6333, -98.45), ("SPS", 33.9833, -98.5833), ("BCE", 37.6833, -112.3),
    ("SLC", 40.85, -111.9667), ("ORF", 36.8833, -76.2), ("RIC", 37.5, -77.3167),
    ("ROA", 37.3333, -80.0667), ("GEG", 47.55, -117.6167), ("SEA", 47.4333, -122.3),
    ("YKM", 46.5667, -120.4333), ("GRB", 44.55, -88.1833), ("LSE", 43.8667, -91.25),
    ("CRW", 38.3333, -81.7667), ("EKN", 38.9, -80.0833), ("CZI", 43.9833, -106.4333),
    ("LND", 42.8, -108.7167), ("MBW", 41.8333, -106.0), ("RKS", 41.5833, -109.0),
    ("2XG", 30.3333, -78.5), ("T01", 28.5, -93.5), ("T06", 28.5, -91.0), ("T07", 28.5, -88.0),
    ("4J3", 28.5, -85.0), ("H51", 26.5, -95.0), ("H52", 26.0, -89.5), ("H61", 26.5, -84.0),
    ("JON", 16.7333, -169.5333), ("MAJ", 7.0667, 171.2667), ("KWA", 8.7167, 167.7333),
    ("MDY", 28.2, -177.3833), ("PPG", -14.3333, -170.7167), ("TTK", 5.35, 162.9667),
    ("AWK", 19.2833, 166.65), ("GRO", 14.1833, 145.2333), ("GSN", 15.1167, 145.7333),
    ("TNI", 15.0, 145.6167), ("GUM", 13.4833, 144.8), ("TKK", 7.4667, 151.85),
    ("PNI", 6.9833, 158.2167), ("ROR", 7.3667, 134.55), ("T11", 9.5, 138.0833),
]


def get(path, params, tries=3):
    """Reply body as text, or None if not published or the server keeps failing."""
    url = HOST + path + "?" + urllib.parse.urlencode(params)
    for _ in range(tries):
        try:
            req = urllib.request.Request(url, headers=HEADERS)
            with urllib.request.urlopen(req, timeout=60) as r:
                if r.status == 200:
                    return r.read().decode("utf-8")
                if r.status in (204, 400, 404):
                    return None
        except urllib.error.HTTPError as e:
            if e.code in (204, 400, 404):
                return None
        except Exception:
            pass
    return None


def latest_cycle():
    """Newest hourly GFA run that has been published, as a UTC datetime."""
    body = get("/api/info/gfa", {"date": int(datetime.now(timezone.utc).timestamp())})
    if body is None:
        return None
    try:
        datim = json.loads(body)["datim"]  # 20261105_23
        return datetime.strptime(datim[:11], "%Y%m%d_%H").replace(tzinfo=timezone.utc)
    except Exception:
        return None


def wind_points(body):
    """Points of one level as {(lat, lon): (direction, knots)}."""
    out = {}
    if body is None:
        return out
    try:
        for f in json.loads(body)["features"]:
            lon, lat = f["geometry"]["coordinates"][:2]
            d = f["properties"].get("wdir")
            s = f["properties"].get("wspd")
            if isinstance(d, (int, float)) and isinstance(s, (int, float)):
                out[(float(lat), float(lon))] = (min(max(round(d), 0), 360), min(max(round(s), 0), 999))
    except Exception:
        return {}
    return out


def spacing(values):
    """Spacing of a lattice axis: the smallest gap between neighbors."""
    v = sorted(set(values))
    gaps = [b - a for a, b in zip(v, v[1:]) if b > a]
    return min(gaps) if gaps else 2.0


class Lattice:
    """Wind of every level on the regular lattice, readable anywhere inside it."""

    def __init__(self, levels):
        lats = [k[0] for level in levels for k in level]
        lons = [k[1] for level in levels for k in level]
        self.lat0, self.lon0 = min(lats), min(lons)
        self.dlat, self.dlon = spacing(lats), spacing(lons)
        self.rows = round((max(lats) - self.lat0) / self.dlat) + 1
        self.cols = round((max(lons) - self.lon0) / self.dlon) + 1
        self.wind = []
        for level in levels:
            grid = {}
            for (lat, lon), w in level.items():
                grid[(round((lat - self.lat0) / self.dlat), round((lon - self.lon0) / self.dlon))] = w
            self.wind.append(grid)

    def uv(self, level, lat, lon):
        """East and north wind in knots (toward which the air moves), or None."""
        fi = (lat - self.lat0) / self.dlat
        fj = (lon - self.lon0) / self.dlon
        i0, j0 = math.floor(fi), math.floor(fj)
        a, b = fi - i0, fj - j0
        u = v = weight = 0.0
        for di in (0, 1):
            for dj in (0, 1):
                i, j = i0 + di, j0 + dj
                w = (1 - a if di == 0 else a) * (1 - b if dj == 0 else b)
                p = self.wind[level].get((i, j))
                if w <= 0 or p is None:
                    continue
                radians = math.radians(p[0])
                u += -p[1] * math.sin(radians) * w
                v += -p[1] * math.cos(radians) * w
                weight += w
        if weight < 0.5:
            return None
        return u / weight, v / weight


def blend(a, b, t):
    if a is None:
        return b
    if b is None:
        return a
    return a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t


def encode(w):
    """FD wind group from east and north knots: direction it comes from in tens of
    degrees then speed, 9900 under 5 knots, and 50 added to the direction at 100
    knots or more."""
    if w is None:
        return ""
    u, v = w
    speed = math.hypot(u, v)
    if speed < 4.5:
        return "9900"
    degrees = math.degrees(math.atan2(-u, -v)) % 360
    tens = int(math.floor(degrees / 10 + 0.5)) % 36 or 36
    knots = int(math.floor(speed + 0.5))
    if knots >= 100:
        tens += 50
        knots = min(knots - 100, 99)
    return "%02d%02d" % (tens, knots)


def gfa_rows():
    """CSV rows from the GFA forecast, and the ids of the stations in them."""
    cycle = latest_cycle()
    if cycle is None:
        return [], set()
    # The newest run may not be complete yet, so fall back to the ones before it.
    for back in range(3):
        run = cycle - timedelta(hours=back)

        def level(name, run=run):
            return get("/api/data/wind", {
                "model": "gfaak", "level": name, "bbox": BBOX, "zoom": ZOOM, "density": 0,
                "wrap": "true", "dref": run.strftime("%Y%m%d"), "tref": run.strftime("%H"),
                "fhr": "%02d" % FORECAST_HOURS})

        with ThreadPoolExecutor(max_workers=6) as pool:
            levels = [wind_points(b) for b in pool.map(level, LEVELS)]
        if not any(levels):
            continue
        lattice = Lattice(levels)
        valid = "VALID %s FOR USE GFA F%02d" % (
            (run + timedelta(hours=FORECAST_HOURS)).strftime("%d%H%MZ"), FORECAST_HOURS)
        rows, ids = [], set()
        for sid, lat, lon in STATIONS:
            uv = [lattice.uv(l, lat, lon) for l in range(len(LEVELS))]
            if all(w is None for w in uv[1:]):
                continue  # outside the forecast
            groups = [encode(uv[l]) for l in range(1, 8)]  # 3000 to 30000 ft
            groups.append(encode(blend(uv[7], uv[8], (34000 - 30000) / 6000.0)))  # 34000 ft
            groups.append(encode(blend(uv[8], uv[9], (39000 - 36000) / 6000.0)))  # 39000 ft
            rows.append([sid, valid, lon, lat] + groups)
            ids.add(sid)
        if rows:
            return rows, ids
    return [], set()


def pacific_rows(have):
    """Rows for the stations the GFA does not cover, from the legacy FD text
    that is still published for the Pacific territories."""
    body = get("/api/data/windtemp", {"region": "other_pac", "fcst": "06", "level": "low", "layout": "off"})
    if body is None or body.lstrip().startswith("{"):
        return []
    lines = body.splitlines()
    header = None
    valid = ""
    rows = []
    for line in lines:
        if line.startswith("VALID"):
            valid = line.replace(" TEMPS NEG ABV 24000", "").strip().replace(",", " ")
        elif line.startswith("FT"):
            header = line.split()[1:]  # 1000 1500 2000 3000 6000 ...
        elif header and len(line.split()) == len(header) + 1:
            tokens = line.split()
            sid = tokens[0]
            if sid in have:
                continue
            found = [s for s in STATIONS if s[0] == sid]
            if not found:
                continue
            wind = dict(zip(header, tokens[1:]))
            groups = [wind.get(k, "") for k in ("3000", "6000", "9000", "12000", "18000", "24000")]
            rows.append([sid, valid, found[0][2], found[0][1]] + groups + ["", "", ""])
    return rows


def main():
    rows, ids = gfa_rows()
    try:
        rows += pacific_rows(ids)
    except Exception as e:
        print("pacific winds failed: %s" % e, file=sys.stderr)
    if not rows:
        print("no winds aloft", file=sys.stderr)
        return 1
    for r in rows:
        print(",".join(str(x) for x in r))
    return 0


if __name__ == "__main__":
    sys.exit(main())
