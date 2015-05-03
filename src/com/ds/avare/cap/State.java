package com.ds.avare.cap;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;

import com.ds.avare.StorageService;
import com.ds.avare.position.Movement;
import com.ds.avare.position.Origin;
import com.ds.avare.position.Scale;
import com.ds.avare.shapes.CAPGridShape;
import com.ds.avare.shapes.Shape;
import com.ds.avare.storage.Preferences;

public class State {
	private String name;
	private String shortName;
	private float minLat;
	private float maxLat;
	private float minLon;
	private float maxLon;
	
	public State(String name, String shortName, float maxLat, float minLat,
			float minLon, float maxLon) {
		
		this.name = name;
		this.shortName = shortName;
		this.minLat = minLat;
		this.maxLat = maxLat;
		this.minLon = minLon;
		this.maxLon = maxLon;
	}

	public void drawGrids(Canvas canvas, Origin origin, Paint paint, StorageService service, Scale scale, 
			Movement movement, Typeface face, Preferences pref) {
		
		float currentLon = (minLon + 1);
		float currentLat = (minLat + 1);
		int gridNumber = 1;
		while (currentLat <= maxLat) {
			while (currentLon <= maxLon) {
//				if ((currentLat > (origin.getLatitudeUpper() + 1)) ||
//						(currentLat < (origin.getLatitudeLower() - 1)) ||
//						(currentLon < (origin.getLongitudeLeft() - 1)) ||
//						(currentLon > (origin.getLongitudeRight() + 1))) {
//					currentLat += 1;
//					gridNumber++;
//					continue;
//				}
				
				drawGrid(canvas, origin, paint, service, scale, movement, face, pref, currentLon, currentLat, gridNumber);
				currentLon += 0.25;
				gridNumber++;
			}
			currentLat += 0.25;
			currentLon = minLon;
		}
		
    	
	}

	private void drawGrid(Canvas canvas, Origin origin, Paint paint,
			StorageService service, Scale scale, Movement movement,
			Typeface face, Preferences pref, float currentLon, float currentLat, int gridNumber) {
		
		service.getShadowedText().draw(canvas, paint, String.valueOf(gridNumber), Color.GRAY,
				(float)origin.getOffsetX(((currentLon + (currentLon + 0.25)) / 2)),
				(float)origin.getOffsetY(((currentLat + (currentLat + 0.25)) / 2)));
		
		
		Shape capgrid = new CAPGridShape("GRID 1");
		capgrid.add(currentLon, currentLat, false);
		capgrid.add(currentLon, (currentLat + 0.25), false);
		capgrid.add((currentLon + 0.25), (currentLat + 0.25), false);
		capgrid.add((currentLon + 0.25), currentLat, false);
		capgrid.add(currentLon, currentLat, false);
		capgrid.drawShape(canvas, origin, scale, movement, paint, pref.isNightMode(), false);
	}
}
