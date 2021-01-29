#Copyright (c) 2015, Apps4Av Inc. (apps4av@gmail.com)
#All rights reserved.
#
#Redistribution and use in source and binary forms, with or without
#modification, are permitted provided that the following conditions are met:
#
#    * Redistributions of source code must retain the above copyright
#notice, this list of conditions and the following disclaimer.
#    * Redistributions in binary form must reproduce the above copyright
#notice, this list of conditions and the following disclaimer in the
#documentation and/or other materials provided with the distribution.
#
#THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
#IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
#THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
#PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
#CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
#EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
#PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
#OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
#WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
#OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
#ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
#
#
# Author: zkhan
# mods: mrieker

import unittest
from HTMLParser import HTMLParser
import datetime
import string
import urllib
import sys
import pytz
import re

# Loads game and race schedule from Internet.
# To start the script: python main.py mlb|nfl|nascar
# All times are Eastern Time, they are converted to Zulu only when printing

def makeEpoch():
    local = pytz.utc
    epoch = datetime.datetime.utcfromtimestamp(0)
    local_dt = local.localize(epoch, is_dst=None)
    return local_dt.astimezone(pytz.utc)


def normalizeUrl(url):
    if (url.startswith('http:')):
        return url
    else:
        return 'http:' + url

def getCurrentYear():
    return datetime.date.today().year

epoch = makeEpoch()

translateMonth = {
    'Jan' : 'Jan',
    'Feb' : 'Feb',
    'Mar' : 'Mar',
    'Apr' : 'Apr',
    'May' : 'May',
    'Jun' : 'Jun',
    'Jul' : 'Jul',
    'Aug' : 'Aug',
    'Sept' : 'Sep',
    'Sep' : 'Sep',
    'Oct' : 'Oct',
    'Nov' : 'Nov',
    'Dec' : 'Dec'
}

mlbTeams = [
    # (short_name, game_palce, home_stadium)
    ('ari', 'Arizona', 'Chase Field'),  # Arizona Diamondbacks
    ('atl', 'Atlanta', 'SunTrust Park'),  # Atlanta Braves
    ('bal', 'Baltimore', 'Oriole Park at Camden Yards'),  # Baltimore Orioles
    ('bos', 'Boston', 'Fenway Park'),  # Boston Red Sox
    ('chc', 'Chicago Cubs', 'Wrigley Field'),  # Chicago Cubs
    ('chw', 'Chicago Sox', 'Guaranteed Rate Field'),  # Chicago White Sox
    ('cin', 'Cincinnati', 'Great American Ball Park'),  # Cincinnati Reds
    ('cle', 'Cleveland', 'Progressive Field'),  # Cleveland Indians
    ('col', 'Colorado', 'Coors Field'),  # Colorado Rockies
    ('det', 'Detroit', 'Comerica Park'),  # Detroit Tigers
    ('hou', 'Houston', 'Minute Maid Park'),  # Houston Astros
    ('kan', 'Kansas City', 'Kauffman Stadium'),  # Kansas City Royals
    ('laa', 'LA Angels', 'Angel Stadium'),  # Los Angeles Angels
    ('lad', 'LA Dodgers', 'Dodger Stadium'),  # Los Angeles Dodgers
    ('mia', 'Miami', 'Marlins Park'),  # Miami Marlins
    ('mil', 'Milwaukee', 'Miller Park'),  # Milwaukee Brewers
    ('min', 'Minnesota', 'Target Field'),  # Minnesota Twins
    ('nym', 'NY Mets', 'Citi Field'),  # New York Mets
    ('nyy', 'NY Yankees', 'Yankee Stadium'),  # New York Yankees
    ('oak', 'Oakland', 'Oakland Alameda Coliseum'),  # Oakland Athletics
    ('phi', 'Philadelphia', 'Citizens Bank Park'),  # Philadelphia Phillies
    ('pit', 'Pittsburgh', 'PNC Park'),  # Pittsburgh Pirates
    ('sd', 'San Diego', 'Petco Park'),  # San Diego Padres
    ('sf', 'San Francisco', 'AT&T Park'),  # San Francisco Giants
    ('sea', 'Seattle', 'Safeco Field'),  # Seattle Mariners
    ('stl', 'St. Louis', 'Busch Stadium'),  # St. Louis Cardinals
    ('tam', 'Tampa Bay', 'Tropicana Field'),  # Tampa Bay Rays
    ('tex', 'Texas', 'Globe Life Park in Arlington'),  # Texas Rangers
    ('tor', 'Toronto', 'Rogers Centre'),  # Toronto Blue Jays
    ('wsh', 'Washington', 'Nationals Park')  # Washington Nationals
]

nflTeams = [
    # (short_name, game_palce, home_stadium)
    ('ari', 'Arizona', 'University of Phoenix Stadium'), # Arizona Cardinals
    ('atl', 'Atlanta', 'Mercedes-Benz Stadium'), # Atlanta Falcons
    ('bal', 'Baltimore', 'M&T Bank Stadium'), # Baltimore Ravens
    ('buf', 'Buffalo', 'M&T Bank Stadium'), # Buffalo Bills
    ('car', 'Carolina', 'Bank of America Stadium'), # Carolina Panthers
    ('chi', 'Chicago', 'Soldier Field'), # Chicago Bears
    ('cin', 'Cincinnati', 'Paul Brown Stadium'), # Cincinnati Bengals
    ('cle', 'Cleveland', 'FirstEnergy Stadium'), # Cleveland Browns
    ('dal', 'Dallas', 'AT&T Stadium'), # Dallas Cowboys
    ('den', 'Denver', 'Sports Authority Field'), # Denver Broncos
    ('det', 'Detroit', 'Ford Field'), # Detroit Lions
    ('gb', 'Green Bay', 'Lambeau Field'), # Green Bay Packers
    ('hou', 'Houston', 'NRG Stadium'), # Houston Texans
    ('ind', 'Indianapolis', 'Lucas Oil Stadium'), # Indianapolis Colts
    ('jac', 'Jacksonville', 'EverBank Field'), # Jacksonville Jaguars
    ('kan', 'Kansas City', 'Arrowhead Stadium'), # Kansas City Chiefs
    ('lac', 'Los Angeles', 'Los Angeles Memorial Coliseum'), # Los Angeles Chargers
    ('lar', 'Los Angeles', 'Los Angeles Memorial Coliseum'), # Los Angeles Rams
    ('mia', 'Miami', 'Hard Rock Stadium'), # Miami Dolphins
    ('min', 'Minnesota', 'U.S. Bank Stadium'), # Minnesota Vikings
    ('nep', 'New England', 'Gillette Stadium'), # New England Patriots
    ('nos', 'New Orleans', 'Mercedes-Benz Superdome'), # New Orleans Saints
    ('nyg', 'NY Giants', 'MetLife Stadium'), # New York Giants
    ('nyj', 'NY Jets', 'MetLife Stadium'), # New York Jets
    ('oak', 'Oakland', 'Oakland Alameda Coliseum'), # Oakland Raiders
    ('phi', 'Philadelphia', 'Lincoln Financial Field'), # Philadelphia Eagles
    ('pit', 'Pittsburgh', 'Heinz Field'), # Pittsburgh Steelers
    ('sf', 'San Francisco', 'Levi\'s Stadium'), # San Francisco 49ers
    ('sea', 'Seattle', 'CenturyLink Field'), # Seattle Seahawks
    ('tb', 'Tampa Bay', 'Raymond James Stadium'), # Tampa Bay Buccaneers
    ('ten', 'Tennessee', 'Nissan Stadium'), # Tennessee Titans
    ('wsh', 'Washington', 'FedEx Field') # Washington Redskins
]

stadiumCoords = {
    # MLB
    'Nationals Park': '38.8730,-77.0074',
    'Citizens Bank Park': '39.9061,-75.1665',
    'Angel Stadium': '33.8003,-117.8827',
    'Fort Bragg Ballpark': '35.162535,-79.001944',
    'Citi Field': '40.7571,-73.8458',
    'Safeco Field': '47.5918,-122.3319',
    'Tropicana Field': '27.7682,-82.6534',
    'Petco Park': '32.7073,-117.1566',
    'AT&T Park': '37.7786,-122.3893',
    'Great American Ball Park': '39.0979,-84.5082',
    'U.S. Cellular Field': '41.8297,-87.6335',
    'Comerica Park': '42.3390,-83.0485',
    'Chase Field': '33.4455,-112.0667',
    'Fenway Park': '42.3467,-71.0972',
    'Oriole Park at Camden Yards': '39.2841,-76.6215',
    'Busch Stadium': '38.6226,-90.1928',
    'Minute Maid Park': '29.7573,-95.3555',
    'Globe Life Park in Arlington': '32.7513,-97.0825',
    'Kauffman Stadium': '39.0517,-94.4803',
    'Progressive Field': '41.4962,-81.6852',
    'Rogers Centre': '43.6414,-79.3894',
    'PNC Park': '40.4469,-80.0057',
    'Oakland Alameda Coliseum': '37.7516,-122.2005',
    'Target Field': '44.9817,-93.2778',
    'Turner Field': '33.7348,-84.3900',
    'Wrigley Field': '41.9484,-87.6553',
    'Yankee Stadium': '40.8296,-73.9262',
    'Miller Park': '43.0280,-87.9712',
    'Coors Field': '39.7559,-104.9942',
    'Marlins Park': '25.7782,-80.2205',
    'Dodger Stadium': '34.0739,-118.2400',
    'SunTrust Park': '33.8907,-84.4678',
    'Guaranteed Rate Field': '41.8299,-87.6338',
    'BB Ballpark': '35.2284,-80.8486',
     # NFL
    'Lambeau Field': '44.5013,-88.0622',
    'Gillette Stadium': '42.0909,-71.2643',
    'New Miami Stadium': '25.9580,-80.2389',
    'Qualcomm Stadium': '32.7831,-117.1196',
    'Oakland Coliseum': '37.7516,-122.2005',
    'Arrowhead Stadium': '39.0489,-94.4839',
    'CenturyLink Field': '47.5952,-122.3316',
    'Paul Brown Stadium': '39.0955,-84.5161',
    'Lucas Oil Stadium': '39.7601,-86.1639',
    'University of Phoenix Stadium': '33.5276,-112.2626',
    'AT&T Stadium': '32.7473,-97.0945',
    'Raymond James Stadium': '27.9759,-82.5033',
    'Sports Authority Field': '39.744145,-105.020050',
    'Twickenham Stadium': '51.4559,-0.3415',
    'Bank of America Stadium': '35.2258,-80.8528',
    'Nissan Stadium': '36.1665,-86.7713',
    'Ford Field': '42.3400,-83.0456',
    'Estadio Azteca': '19.3029,-99.1505',
    'Heinz Field': '40.4468,-80.0158',
    'Wembley Stadium': '51.5560,-0.2795',
    'Levi\'s Stadium': '37.4032,-121.9698',
    'Georgia Dome': '33.7577,-84.4008',
    'Lincoln Financial Field': '39.9008,-75.1674',
    'NRG Stadium': '29.6847,-95.4107',
    'Mercedes-Benz Superdome': '29.9511,-90.0812',
    'U.S. Bank Stadium': '44.9737,-93.2577',
    'FedEx Field': '38.9076,-76.8645',
    'MetLife Stadium': '40.8128,-74.0742',
    'EverBank Field': '30.3239,-81.6373',
    'Ralph Wilson Stadium': '42.7738,-78.7870',
    'Soldier Field': '41.8623,-87.6167',
    'M&T Bank Stadium': '39.2780,-76.6227',
    'Los Angeles Memorial Coliseum': '34.0141,-118.2879',
    'FirstEnergy Stadium': '41.5061,-81.6995',
    'Mercedes-Benz Stadium': '33.7554,-84.4012',
    'New Era Field': '42.7738,-78.7870',
    'StubHub Center': '33.8644,-118.2611',
    'Hard Rock Stadium': '25.9580,-80.2389',
    #NASCAR
    'New Hampshire Motor Speedway': '43.3627,-71.4606',
    'Charlotte Motor Speedway': '35.3520,-80.6827',
    'Auto Club Speedway': '34.0889,-117.5002',
    'Texas Motor Speedway': '33.0372,-97.2822',
    'Michigan International Speedway': '42.0648,-84.2412',
    'Atlanta Motor Speedway': '33.3868,-84.3164',
    'Pocono Raceway': '41.0543,-75.5113',
    'Indianapolis Motor Speedway': '39.7954,-86.2353',
    'Ism Raceway': '33.3749,-112.3112', # alias for Phoenix International Raceway
    'Phoenix International Raceway': '33.3749,-112.3112',
    'Daytona International Speedway': '29.1852,-81.0705',
    'Chicagoland Speedway': '41.4748,-88.0576',
    'Kentucky Speedway': '38.7114,-84.9167',
    'Talladega Superspeedway': '33.5691,-86.0709',
    'Kansas Speedway': '39.1157,-94.8310',
    'Dover International Speedway': '39.1899,-75.5307',
    'Sonoma': '38.1599,-122.4549',
    'Watkins Glen International': '42.3383,-76.9264',
    'Bristol Motor Speedway': '36.5157,-82.2570',
    'Richmond Raceway': '37.5924,-77.4195', # alias for Richmond International Raceway
    'Richmond International Raceway': '37.5924,-77.4195',
    'Martinsville Speedway': '36.6341,-79.8517',
    'Las Vegas Motor Speedway': '36.2714,-115.0101',
    'Darlington Raceway': '34.2953,-79.9056',
    'Homestead-Miami Speedway': '25.4518,-80.4089'
}

class RawSchedule(object):
    def __init__(self, opponent, day, time):
        self.m_opponent = opponent
        self.m_day = day
        self.m_time = time

    def __repr__(self):
        return 'opponent [' + self.m_opponent + '] day [' + self.m_day + '] time [' + self.m_time + ']'

class Schedule (object):
    def __init__(self, location, day, time):
        self.m_location = location
        self.m_day = day
        self.m_time = time

    def __repr__(self):
        # Print in milliseconds from the epoch
        # millis = int((datetime.datetime.combine(self.m_day, self.m_time) - epoch).total_seconds()) * 1000
        # return str(millis) + ','+ self.m_location + ',' + stadiumCoords[self.m_location]
        # Print as date-time
        zulu = self.easternToZulu()
        millis = int((zulu - epoch).total_seconds()) * 1000
        #return str(self.m_day) +',' + str(self.m_time) + ','+ str(zulu.date()) + ',' + str(zulu.time()) + ',' + str(millis) + ',' + self.m_location + ',' + stadiumCoords[self.m_location]
        return str(millis) + ',' + self.m_location + ',' + stadiumCoords[self.m_location]

    def easternToZulu(self):
        local = pytz.timezone('America/New_York')
        dt = datetime.datetime(self.m_day.year, self.m_day.month, self.m_day.day, self.m_time.hour, self.m_time.minute, 0, 0)
        local_dt = local.localize(dt, is_dst = None)
        return local_dt.astimezone(pytz.utc)

class ScheduleHtmlParser(HTMLParser) :
    def __init__(self, placeToStadium):
        HTMLParser.__init__(self)
        self.m_rawSchedule = []
        self.m_placeToStadium = placeToStadium

    def createSchedule(self):
        ret = []
        for raw in self.m_rawSchedule:
            try:
                ret.append(self.parseRawSchedule(raw))
            except:
                # print '----> cannot parse', raw
                pass
        return ret

class MlbScheduleHtmlParser(ScheduleHtmlParser):
    DAY = 0
    OPPONENT = 1
    TIME = 2

    def __init__(self, placeToStadium):
        ScheduleHtmlParser.__init__(self, placeToStadium)
        self.m_column = 0
        self.m_tableCount = 0
        self.m_inTd = False
        self.m_date = ''
        self.m_opponent = ''
        self.m_time = ''

    def handle_starttag(self, tag, attrs):
        if tag == 'table':
            self.m_tableCount += 1

        if tag == 'tr':
            self.m_column = -1

        if tag == 'td':
            self.m_column += 1
            self.m_inTd = True

    def handle_endtag(self, tag):
        if tag == 'table':
            self.m_tableCount -= 1
        if tag == 'td':
            self.m_inTd = False

        if tag == 'tr':
            self.m_rawSchedule.append(RawSchedule(self.m_opponent, self.m_date, self.m_time))
            self.m_column = -1
            self.m_date = ''
            self.m_opponent = ''
            self.m_time = ''

    def handle_data(self, data):
        if self.m_tableCount != 2 or not self.m_inTd:
            return

        if self.m_column == MlbScheduleHtmlParser.DAY:
            self.m_date = data
        elif self.m_column == MlbScheduleHtmlParser.OPPONENT:
            self.m_opponent = data
        elif self.m_column == MlbScheduleHtmlParser.TIME:
            self.m_time = data

    def parseRawSchedule(self, raw):
        placeToStadium = self.m_placeToStadium
        opponent = raw.m_opponent
        place = parseOpponent(placeToStadium, opponent)
        day = parseDate(raw.m_day)
        time = parseTime(raw.m_time + ' pm')
        return Schedule(place, day, time)

class NflScheduleHtmlParser(ScheduleHtmlParser):
    DAY = 1
    OPPONENT = 2
    TIME = 3

    def __init__(self, placeToStadium):
        ScheduleHtmlParser.__init__(self, placeToStadium)
        self.m_column = 0
        self.m_tableCount = 0
        self.m_inTd = False
        self.m_inLi = False
        self.m_date = ''
        self.m_at = ''
        self.m_opponent = ''
        self.m_time = ''

    def handle_starttag(self, tag, attrs):
        if tag == 'table':
            self.m_tableCount += 1

        if tag == 'tr':
            self.m_column = -1

        if tag == 'td':
            self.m_column += 1
            self.m_inTd = True

        if tag == 'li':
            self.m_inLi = True

    def handle_endtag(self, tag):
        if tag == 'table':
            self.m_tableCount -= 1
        if tag == 'td':
            self.m_inTd = False
        if tag == 'li':
            self.m_inLi = False

        if tag == 'tr':
            self.m_rawSchedule.append(RawSchedule(self.m_at + self.m_opponent, self.m_date, self.m_time))
            self.m_column = -1
            self.m_date = ''
            self.m_at = ''
            self.m_opponent = ''
            self.m_time = ''

    def handle_data(self, data):
        if self.m_tableCount != 1 or not self.m_inTd:
            return

        if self.m_inLi and not self.m_at and data == '@':
            self.m_at = 'at '
            return

        if self.m_column == NflScheduleHtmlParser.DAY:
            self.m_date = data
        elif self.m_column == NflScheduleHtmlParser.OPPONENT:
            self.m_opponent = data
        elif self.m_column == NflScheduleHtmlParser.TIME:
            self.m_time = data

    def parseRawSchedule(self, raw):
        return Schedule(parseOpponent(self.m_placeToStadium, raw.m_opponent), parseDate(trimDayOfWeek(raw.m_day)), parseTime(raw.m_time))

class NascarScheduleHtmlParser(ScheduleHtmlParser):
    DAYTIME = 0
    LOCATION = 1

    def __init__(self):
        ScheduleHtmlParser.__init__(self, None)
        self.m_ready = False
        self.m_column = -1
        self.m_dateTime = ''
        self.m_location = ''
        self.m_inH1 = False
        self.m_year = 0

    def handle_starttag(self, tag, attrs):
        if tag == 'h1':
            self.m_inH1 = True
            return

        if self.m_ready:
            if tag == 'td':
                self.m_column += 1
            elif tag == 'tr':
                self.m_column = -1
                self.m_location = ''
                self.m_dateTime = ''

    def handle_endtag(self, tag):
        if tag == 'h1':
            self.m_inH1 = False
            return
        elif tag == 'tr':
            if len(self.m_location) > 20 and len(self.m_dateTime) > 10:
                dateTime = parseDateTime(self.m_dateTime)
                day = dateTime.date()
                day = day.replace(self.m_year, day.month, day.day)
                time = dateTime.time()
                self.m_rawSchedule.append(Schedule(parseLocation(self.m_location), day, time))

        if self.m_ready and tag == 'table':
            self.m_ready = False

    def handle_data(self, data):
        if self.m_inH1 and self.m_year == 0 and data.find('Schedule') != -1:
            self.m_year = int(data.split(' ')[0])

        # The values we are looking for go somewhere after word "RESULTS/TICKETS"
        if not self.m_ready and data.find('RESULTS/TICKETS') != -1:
            self.m_ready = True
            return

        if not self.m_ready:
            return

        if self.m_column == NascarScheduleHtmlParser.DAYTIME:
            self.m_dateTime = self.m_dateTime + ',' + data
        elif self.m_column == NascarScheduleHtmlParser.LOCATION:
            self.m_location = self.m_location + ',' + data


    def parseRawSchedule(self, raw):
        return raw

def loadHtml(url):
    return urllib.urlopen(url).read()

def getTeamSchedule(s, parser):
    parser.feed(s)
    return parser.createSchedule()

def parseDate(s):
    tokens = re.split('\W+', s)
    if len(tokens) != 2:
        raise ValueError('Cannot parse date [' + s + ']')
    year = getCurrentYear()
    month = tokens[0]

    if month in translateMonth:
        month = translateMonth[tokens[0]]
        day = tokens[1]
        return datetime.datetime.strptime(month + ' ' + day + ' ' + str(year), '%b %d %Y').date()
    else:
        raise ValueError('Cannot parse date [' + s + ']')

def trimDayOfWeek(s):
    n = s.find(' ')
    return s[n + 1:]

def parseTime(s):
    try:
        return datetime.datetime.strptime(s, '%I:%M %p').time()
    except ValueError:
        return datetime.datetime(datetime.MINYEAR, 1, 1)

def parseDateTime(s):
    return datetime.datetime.strptime(s, ',%a,,%b,%d,%I:%M %p ET')

def parseLocation(s):
    return s.split(',')[2]

def parseOpponent(placeToStadium, strOpponent):
    if strOpponent.startswith('at'):
        return placeToStadium[strOpponent[3:]]
    else:
        return placeToStadium[strOpponent]

def getUnique(items):
    ret = {}
    for item in items:
        ret[str(item)] = item
    return ret.values()

def loadMlb():
    parser = MlbScheduleHtmlParser(makePlaceToStadium(mlbTeams))
    for teamUrl in makeScheduleList('http://www.espn.com/mlb/teams/printSchedule/_/team/', mlbTeams, '/season/' + str(getCurrentYear())):
        ret = getTeamSchedule(loadHtml(teamUrl), parser)
    return getUnique(ret)

def loadNfl():
    parser = NflScheduleHtmlParser(makePlaceToStadium(nflTeams))
    for teamUrl in makeScheduleList('http://insider.espn.com/nfl/print/schedule?team=', nflTeams, ''):
        ret = getTeamSchedule(loadHtml(teamUrl), parser)
    return getUnique(ret)

def loadNascar():
    startUrl = 'http://espn.go.com/racing/schedule'
    parser = NascarScheduleHtmlParser()
    ret = getTeamSchedule(loadHtml(startUrl), parser)
    return ret

def makeScheduleList(header, teamList, trailer):
    return [header + team[0] + trailer for team in teamList]

def makePlaceToStadium(teams):
    ret = {}
    for team in teams:
        ret[team[1]] = team[2]
    return ret

def main():
    if sys.argv[-1] == 'mlb':
        schedule = loadMlb()
    elif sys.argv[-1] == 'nfl':
        schedule = loadNfl()
    elif sys.argv[-1] == 'nascar':
        schedule = loadNascar()
    else:
        print 'usage: python main.py mlb|nfl|nascar'
        return

    for s in schedule:
        print s

if __name__ == '__main__':
    main()


