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
		
		float currentLon = minLon;
		float currentLat = minLat;
		while (currentLon < maxLon) {
			while (currentLat < maxLat) {
				if ((currentLat > (origin.getLatitudeUpper() + 1)) ||
						(currentLat < (origin.getLatitudeLower() - 1)) ||
						(currentLon < (origin.getLongitudeLeft() - 1)) ||
						(currentLon > (origin.getLongitudeRight() + 1))) {
					currentLat += 1;
					continue;
				}
				
				drawGrid(canvas, origin, paint, service, scale, movement, face, pref, currentLon, currentLat);
				currentLat += 0.25;
			}
			currentLon += 0.25;
			currentLat = minLat;
		}
		
    	
	}

	private void drawGrid(Canvas canvas, Origin origin, Paint paint,
			StorageService service, Scale scale, Movement movement,
			Typeface face, Preferences pref, float currentLon, float currentLat) {
		
		service.getShadowedText().draw(canvas, paint, "254", Color.GRAY,
				(float)origin.getOffsetX(((currentLon + (currentLon + 0.25)) / 2)),
				(float)origin.getOffsetY(((currentLat + (currentLat + 0.25)) / 2)));
		
		
		Shape capgrid = new CAPGridShape("GRID 1");
		capgrid.add(currentLon, currentLat, false);
		capgrid.add(currentLon, (currentLat + 0.25), false);
		capgrid.add((currentLon + 0.25), (currentLat + 0.25), false);
		capgrid.add((currentLon + 0.25), currentLat, false);
		capgrid.add(currentLon, currentLat, false);
		capgrid.makePolygon();
		capgrid.drawShape(canvas, origin, scale, movement, paint, face, pref.isNightMode());
	}
}
