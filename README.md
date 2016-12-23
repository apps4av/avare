avare
=====

Avare Aviation GPS for Android.

Download from the Google Play Store: https://play.google.com/store/apps/details?id=com.ds.avare&hl=en

Avare provides offline moving map on all FAA charts (VFR Sectional, IFR lo/hi enroute, Approach Plate, WAC & TAC); plus A/FD info, Airport Diagrams w/GPS Taxi, and Terminal Procedures. 

Also included: GPS status, Terrain/Elevation Maps, 50 Nearest airports, Obstacles, and more. Some FAA Charts cover Puerto Rico, plus parts of Canada, Mexico and the Caribbean. 

Canadian topo moving maps and unofficial airport info are also now available and volunteers may add other countries.

Avare Advantages and Features:
* Made by & for pilots
* Easy to learn & use
* Quick & responsive
* All free FAA materials & more
* Runs on most Android devices
* No ads or invasive permissions

More Features:
* Type in any address or coordinates.
* METAR & TAF (Internet fetch).
* ADSB NEXRAD, METAR, TAF, PIREP, Winds (free AddOn apps).
* Graphic & text TFRs (Internet fetch).
* Terrain maps & Canada topo, MSL & AGL display.
* External GPS, autopilot and flite sim (free AddOn apps).
* Obstacles: Any over 500' AGL within 200' of GPS altitude.
* Frequencies, runways, fuel availability, & other A/FD info.
* 13000+ Geo. referenced approach plates and taxi diagrams
* Landscape or Portrait display Preference, North Up or Track Up (NU/TU).
* Draw notes directly on-screen, or Pan freely around charts (Draw/Pan).
* Press any two points on chart for approximate distance between them.
* Location icon can be centered or track from your placement on screen.
* GPS compass direction, Bearing, and Distance to and FROM any point on the map by long-pressing one finger on that point. Great for your ATC and CTAF radio calls.
* Built in CAP (Civil Air Patrol) conventional grid system.


BUILDING:
Checkout in Android Studio using File->New->Project From Version Control->GitHub. Give URL as https://github.com/apps4av/avare.git, then press Clone.

After Checkout is complete, press the Android Studio Toolbar button "Sync Project With Gradle Files".

RUNNING:
Run in Android Studio on Emulator, or an actual device.

TEST EVERY CYCLE:

Check 1: Server
---------------

1. Look at new data on server and chmod a+r all files, chmod g+w all files, and see nothing stands out in size of file

2. Verify that number of files are same as number of files in last cycle (use command ls -la | wc -l)

Check 2: Databases
------------------

1. Clear data of app, go through registration and before download for database, go to preferences and change cycle to next, then check "show all bases"

2. Download database

3. Go to simulation mode

4. Find BOS

5. See text A/FD is correct (runways appear, freqencies appear, all fields look filled properly)

6. Go to Map and verify runways extensions are drawn correctly

7. Go to Plan and create a plan KBOS BOS V16 CMK, verify V16 is converetd to fixes and all are identified

8. Verify that OPLA airport is found and its A/FD from OUR-AP is correct

9. Go to Map, see charts (Sectional, TAC, WAC, IFR low, IFR high, IFR area) show in fully zoomed out, note no cut-off edges, no missing areas, no lines

10. Pan to go to Alaska, then to Caribbean and see there is no odd missing areas

Check 3: Full data
------------------

1. Download following: Plates Georeference Info, NewYork sectional, Boston TAC, CF-19 WAC, IFR Low NE, IFR Hi NE, IFR area ANC, Canada ADs, Minimums T/A, Plates MA, VFR area Canada, VFR area MA, A/FD NE, Elevation NE, Boston Heli, Grand Canyon

2. Find airport BOS

3. Go to Map, see charts (Sectional, TAC, WAC, IFR low, IFR high) show fully zoomed in, out, and intermediate. Note no cut-off edges, no missing areas, no lines, aircraft on center of airport

4. Find airport ANC

5. Go to Map, see charts (IFR area) show fully zoomed in, out, and intermediate. Note no cut-off edges, no missing areas, no lines, aircraft on center of airport

6. Find airport GCN

7. Go to Map, see charts (Heli/Other) for grand canyon, show fully zoomed in, out, and intermediate. Note no cut-off edges, no missing areas, no lines, aircraft on center of airport

8. Find airport BOS

9. Go to Map, see charts (Heli/Other) for boston heli, show fully zoomed in, out, and intermediate. Note no cut-off edges, no missing areas, no lines, aircraft on center of airport

10. Find airport BVY

11. Go to A/FD, and see graphical A/FD has correct date and is correctly showing BVY on it

12. Go to Plate, and see Airport Diagram with georef and correct date, Area plate with georef, t/o and dep minimums with correct date and airport, and see some approach plates with correct date and georef info

13. In Plate see if AP button for some approach plates is green and loads correctly to Plan

14. Find airport CYVR

15. Go to plate and see that airport diagram shows in Plates, area plate with georef shows as well

16. Find airport BOS, go to 3D and see all obstacles are showing

17. Click on airport KBED, verify VOR radials BOS313015, make sure SUA R4102A, and 4102B show with altitude, TX/RX frequencies, and Note.

