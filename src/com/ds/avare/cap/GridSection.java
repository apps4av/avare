package com.ds.avare.cap;

import java.util.ArrayList;
import java.util.List;

public class GridSection {
	private List<State> states;
	
	public GridSection() {
		states = new ArrayList<State>();
//		states.add(new State("Alaska", "AK", (float)72, (float)52, (float)-175, (float)-126));
//		states.add(new State("Alabama", "AL", (float)35.25, (float)30.25, (float)-88.75, (float)-84.75));
//		states.add(new State("Arkansas", "AR", (float)36.75, (float)32.75, (float)-94.75, (float)-89.75));
//		states.add(new State("Arizona", "AZ", (float)37.25, (float)31.25, (float)-115, (float)-108.75));
//		states.add(new State("California/S", "CA/S", (float)37, (float)32.5, (float)-122.25, (float)-114));
//		states.add(new State("California/N", "CA/N", (float)42.25, (float)36, (float)-124.5, (float)-117));
//		states.add(new State("Colorado", "CO", (float)41.25, (float)36.75, (float)-109.25, (float)-101.75));
//		states.add(new State("Connecticut", "CT", (float)42.25, (float)40.75, (float)-74, (float)-71));
//		states.add(new State("Delaware", "DE", (float)40, (float)38.25, (float)-76, (float)-75));
//		states.add(new State("Florida", "FL", (float)31.25, (float)24.5, (float)-87.75, (float)-79.75));
//		states.add(new State("Georgia", "GA", (float)35.25, (float)30.25, (float)-85.75, (float)-80.75));
//		states.add(new State("Iowa", "IA", (float)43.75, (float)40.25, (float)-96.75, (float)-90));
//		states.add(new State("Idaho", "ID", (float)49, (float)41.75, (float)-117.5, (float)-110.75));
//		states.add(new State("Illinois", "IL", (float)43, (float)37, (float)-92, (float)-87));
//		states.add(new State("Indiana", "IN", (float)42, (float)37.5, (float)-88.25, (float)-84.75));
//		states.add(new State("Kansas", "KS", (float)40.25, (float)36.75, (float)-102.25, (float)-94.5));
//		states.add(new State("Kentucky", "KY", (float)39.25, (float)36.5, (float)-89.75, (float)-81.75));	
//		states.add(new State("Louisiana", "LA", (float)33.25, (float)29, (float)-94.25, (float)-89.5));
//		states.add(new State("Massachusetts", "MA", (float)43, (float)41.25, (float)-73.75, (float)-69.75));
//		states.add(new State("Maryland", "MD", (float)39.75, (float)37.75, (float)-79.5, (float)-75));
//		states.add(new State("Maine", "ME", (float)47.75, (float)43, (float)-71.25, (float)-66.75));
//		states.add(new State("Michigan", "MI", (float)47.75, (float)41.5, (float)-90.5, (float)-82.25));
//		states.add(new State("Minnesota", "MN", (float)49, (float)43.25, (float)-97.5, (float)-89.5));
//		states.add(new State("Missouri", "MO", (float)40.75, (float)35.75, (float)-95.75, (float)-89));
//		states.add(new State("Mississippi", "MS", (float)35.25, (float)30, (float)-91.75, (float)-88));
//		states.add(new State("Montana", "MT", (float)49, (float)44.25, (float)-116.25, (float)-103.75));
//		states.add(new State("North Carolina", "NC", (float)36.75, (float)33.75, (float)-84.5, (float)-75.5));
//		states.add(new State("North Dakota", "ND", (float)49, (float)45.75, (float)-104.25, (float)-96.5));
//		states.add(new State("Nebraska", "NE", (float)43.25, (float)39.75, (float)-104.25, (float)-95.25));
//		states.add(new State("New Hampshire", "NH", (float)45.25, (float)42.5, (float)-72.75, (float)-70.75));
//		states.add(new State("New Jersey", "NJ", (float)41.5, (float)38.75, (float)-75.75, (float)-73.75));
//		states.add(new State("New Mexico", "NM", (float)37.25, (float)31.25, (float)-109.25, (float)-102.75));
//		states.add(new State("Nevada", "NV", (float)42.25, (float)35, (float)-120.25, (float)-114));
//		states.add(new State("New York", "NY", (float)45.25, (float)40.25, (float)-80, (float)-73));
//		states.add(new State("Ohio", "OH", (float)42, (float)38.25, (float)-85, (float)-80.25));
//		states.add(new State("Oklahoma", "OK", (float)37.25, (float)34, (float)-103.25, (float)-94.25));
		states.add(new State("Oregon", "OR", (float)46.25, (float)41.75, (float)-124.75, (float)-116.75));
//		states.add(new State("Pennsylvania", "PA", (float)42.25, (float)39.5, (float)-80.75, (float)-74.5));
//		states.add(new State("Rhode Island", "RI", (float)42.25, (float)41.25, (float)-72, (float)-71));
//		states.add(new State("South Carolina", "SC", (float)35.25, (float)31.75, (float)-83.5, (float)-78.5));
//		states.add(new State("South Dakota", "SD", (float)46, (float)42.5, (float)-104.25, (float)-96.25));
//		states.add(new State("Tennessee", "TN", (float)36.75, (float)34.75, (float)-90.25, (float)-81.5));
//		states.add(new State("Texas/E", "TX/E", (float)34.75, (float)25.75, (float)-100.25, (float)-93.25));
//		states.add(new State("Texas/W", "TX/W", (float)36.75, (float)28, (float)-106.75, (float)-99.75));
//		states.add(new State("Utah", "UT", (float)42.25, (float)36.75, (float)-114.25, (float)-108.75));
//		states.add(new State("Virginia", "VA", (float)39.75, (float)36.5, (float)-83.75, (float)-75));
//		states.add(new State("Vermont", "VT", (float)45.25, (float)42.5, (float)-73.75, (float)-71.25));
//		states.add(new State("Washington", "WA", (float)49, (float)45.5, (float)-124.75, (float)-116.75));
//		states.add(new State("Wisconsin", "WI", (float)47, (float)42.25, (float)-93, (float)-86.75));
//		states.add(new State("West Virginia", "WV", (float)40.75, (float)37, (float)-82.75, (float)-77.5));
//		states.add(new State("Wyoming", "WY", (float)45.25, (float)40.75, (float)-111.25, (float)-104));
//		states.add(new State("CONUS", "CONUS", (float)49, (float)25, (float)-125.25, (float)-66.5));
	}

	public List<State> getStates() {
		return states;
	}
}
