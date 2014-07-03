CREATE TABLE airports(LocationID Text,ARPLatitude float,ARPLongitude float,Type Text,FacilityName Text,Use Text,FSSPhone Text,Manager Text,ManagerPhone Text,ARPElevation Text,MagneticVariation Text,TrafficPatternAltitude Text,FuelTypes Text,Customs Text,Beacon Text,LightSchedule Text,SegCircle Text,ATCT Text,UNICOMFrequencies Text,CTAFFrequency Text,NonCommercialLandingFee Text,State Text, City Text);
.separator ","
.import airport.csv airports 

CREATE TABLE airportfreq(LocationID Text,Type Text, Freq Text);
.import freq.csv airportfreq 

CREATE TABLE airportrunways(LocationID Text,Length Text,Width Text,Surface Text,LEIdent Text,HEIdent Text,LELatitude Text,HELatitude Text,LELongitude Text,HELongitude Text,LEElevation Text,HEElevation Text,LEHeadingT Text,HEHeading Text,LEDT Text,HEDT Text,LELights Text,HELights Text,LEILS Text,HEILS Text,LEVGSI Text,HEVGSI Text,LEPattern Text, HEPattern Text);

.import runway.csv airportrunways 

CREATE TABLE airportdiags(LocationID Text, tfwA float, tfwB float, tfwC float, tfwD float, tfwE float, tfwF float, wftA float, wftB float, wftC float, wftD float, wftE float, wftF float);

.import airdiags/aptdiags.csv airportdiags

CREATE TABLE nav(LocationID Text,ARPLatitude float,ARPLongitude float,Type Text,FacilityName Text);

.import nav.csv nav

CREATE TABLE fix(LocationID Text,ARPLatitude float,ARPLongitude float,Type Text,FacilityName Text);

.import fix.csv fix

CREATE TABLE obs(ARPLatitude float,ARPLongitude float,Height float);
.import dof.csv obs 

CREATE TABLE afd(LocationID Text,File Text);

.import afd.csv afd

CREATE TABLE takeoff(LocationID Text,File Text);

.import mins/to.csv takeoff

CREATE TABLE alternate(LocationID Text,File Text);

.import mins/alt.csv alternate

CREATE TABLE awos(LocationID Text, Type Text, Status Text, Latitude float,Longitude float, Elevation Text, Frequency1 Text, Frequency2 Text, Telephone1 Text, Telephone2 Text, Remark Text);

.import awos.csv awos

CREATE TABLE saa(designator TEXT,name TEXT,upperlimit TEXT,lowerlimit TEXT,begintime TEXT,endtime TEXT,timeref TEXT,beginday TEXT,endday TEXT,day TEXT,FreqTx TEXT,FreqRx TEXT,lat FLOAT,lon FLOAT);
.import saa.csv saa

.import canap.csv airports
.import canrun.csv airportrunways
.import canfreq.csv airportfreq
