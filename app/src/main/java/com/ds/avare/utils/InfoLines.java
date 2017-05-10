/*
Copyright (c) 2012, Apps4Av Inc. (apps4av.com) 
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

 * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
 *
 *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.ds.avare.utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;

import com.ds.avare.R;
import com.ds.avare.StorageService;
import com.ds.avare.instruments.CDI;
import com.ds.avare.instruments.Odometer;
import com.ds.avare.place.Destination;
import com.ds.avare.storage.Preferences;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/***
 * Object to handle all of the text on the top two lines of the screen along
 * with their configured content
 * 
 * @author Ron
 * 
 */
public class InfoLines {

    // Simple class to encapsulate a field location on the screen
    public class InfoLineFieldLoc {
    	private int 	 mRowIdx;
    	private int 	 mFieldIdx;
    	private String[] mOptions;
    	private int 	 mSelected;

        private InfoLineFieldLoc(int aRowIdx, int aFieldIdx, String[] aOptions,
                int aSelected) {
            mRowIdx = aRowIdx;
            mFieldIdx = aFieldIdx;
            mOptions = aOptions; // What valid options that are for this field
            mSelected = aSelected; // Currently selected option in the mOptions
                                   // list
        }

        public String[] getOptions() {
            return mOptions;
        }

        public int getSelected() {
            return mSelected;
        }
    }

    // Dynamic data fields related items
    private float mShadowY; // How high the status display lines are
    private int mDisplayWidth; // Horizontal display size
    private int mFieldWidth; // width of each field
    private int mCharWidth; // width of one character
    private int mDisplayOrientation; // portrait or landscape
    private int mFieldPosX[]; // X positions of the fields left edges
    private int mFieldLines[][]; // Configuration/content of the status lines
    private int mRowCount; // How many status rows are in use
    private float mAltitude;

    private Context mContext;
    private Preferences mPref;
    private StorageService mService;

    private String[] mOptionList;	// Cache the options as well for performance
    private String[] mFieldTitles;	// Cache field titles too

    // Constants to indicate the display orientation
    static final int ID_DO_LANDSCAPE = 0;
    static final int ID_DO_PORTRAIT  = 1;

    // To add new display fields, take the ID_FLD_MAX value, and adjust MAX up
    // by 1.
    // ID_FLD_MAX must always be the highest, and ID_FLD_NUL the lowest
    // Ensure that the string-array "TextFieldOptions" is update with the new
    // entry in the proper order
    static final int ID_FLD_NUL = 0;
    static final int ID_FLD_GMT = 1;
    static final int ID_FLD_LT  = 2;
    static final int ID_FLD_SPD = 3;
    static final int ID_FLD_HDG = 4;
    static final int ID_FLD_BRG = 5;
    static final int ID_FLD_DST = 6;
    static final int ID_FLD_DIS = 7;
    static final int ID_FLD_ETE = 8;
    static final int ID_FLD_ETA = 9;
    static final int ID_FLD_MSL = 10;
    static final int ID_FLD_HOB = 11;
    static final int ID_FLD_VSI = 12;
    static final int ID_FLD_VSR = 13;
    static final int ID_FLD_ODO = 14;
    static final int ID_FLD_CDI = 15;
    static final int ID_FLD_FPR = 16;
    static final int ID_FLD_FUL = 17;
    static final int ID_FLD_TMR = 18;
    static final int ID_FLD_SPP = 19;
    static final int ID_FLD_HDP = 20;
    static final int ID_FLD_MSP = 21;
    static final int ID_FLD_MAX = 22;
    static final String NOVALUE = "     ";

    static final double TITLE_TO_TEXT_RATIO = 2.5;

    static final int MAX_INFO_ROWS = 2;
    static final int MAX_FIELD_SIZE_IN_CHARS = 5;

    // Return how much in the Y direction we take up
    public float getHeight() {
        return mShadowY;
    }

    /***
     * Is there a field to display at the indicated location. To figure this out
     * we need the X/Y of the location along with the paint object (which
     * determines text size)
     * 
     * @param aPaint
     * @param posX
     * @param posY
     * @return an InfoLineFieldLoc object which identifies the field, its
     *         choices and current selection OR null
     */
    public InfoLineFieldLoc findField(Paint aPaint, float posX, float posY) {
        float dataY = aPaint.getTextSize();
        float titleY = dataY / (float) TITLE_TO_TEXT_RATIO;
        float lineY = dataY + titleY;

        if (posY > lineY * MAX_INFO_ROWS) {
            return null;
        }

        // Did we tap on Row0 or Row1 ?
        int nRowIdx = 0;
        if (posY > lineY) {
            nRowIdx = 1;
        }

        // Make the adjustment here in case we are in PORTRAIT display mode
        nRowIdx += (mDisplayOrientation == ID_DO_LANDSCAPE) ? 0 : MAX_INFO_ROWS;

        // Find out what field we tapped over
        int nFieldIdx = mFieldPosX.length - 1;
        for (int idx = 1; idx < mFieldPosX.length; idx++) {
            if (mFieldPosX[idx] > posX) {
                nFieldIdx = idx - 1;
                break;
            }
        }

        // Get our currently displayed value for this field
        int nSelected = mFieldLines[nRowIdx][nFieldIdx];

        // Build up the available options list
        List<String> optionAvail = new ArrayList<String>();

        // Loop through the master list and include the item only under certain
        // conditions
        for (int idx = 0, maxIdx = mOptionList.length; idx < maxIdx; idx++) {
            if (idx == 0) {
                optionAvail.add(mOptionList[0]); // always allow the NONE
            } else if (idx == nSelected) {
                optionAvail.add(mOptionList[idx]); // always add what it
                                                  // currently IS
                nSelected = optionAvail.size() - 1; // reflect the selected
                                                    // position in the new list
            } else if (!isShowing(idx)) {
                optionAvail.add(mOptionList[idx]); // add others
            }
        }

        // If we still have a selection that is outside our available range,
        // then default it to NONE
        if(nSelected >= optionAvail.size()) {
        	nSelected = 0;
        }
        
        // OK, the new option list is built and we have what should currently be
        // selected in there. Return this info to the caller
        return new InfoLineFieldLoc(nRowIdx, nFieldIdx,
                optionAvail.toArray(new String[optionAvail.size()]), nSelected);
    }

    /***
     * Is this type of field already on the display ?
     * 
     * @param nFieldType
     *            Type of field to search for
     * @return true/false to indicate it is already shown at another location
     */
    private boolean isShowing(int nFieldType) {
        int nRowIdx = (mDisplayOrientation == ID_DO_LANDSCAPE) 
        				? 0 : MAX_INFO_ROWS;

        // Loop through the 2 entire status lines that are configured for
        // this display mode. Return true if we find it in either
        for (int idx = 0; idx < MAX_INFO_ROWS; idx++) {
            for (int fldIdx = 0, maxIdx = mFieldLines[idx].length; fldIdx < maxIdx; fldIdx++) {
                if (mFieldLines[idx + nRowIdx][fldIdx] == nFieldType) {
                    return true;
                }
            }
        }
        return false;
    }

    /***
     * Set the desired field to the type specified. It's a few hoops to get it
     * from what it is, to what it needs to be.
     * 
     * @param infoLineFieldLoc
     *            what field to change
     * @param nSelected
     *            selection index of the new field content
     */
    public void setFieldType(InfoLineFieldLoc infoLineFieldLoc, int nSelected) {
        if (rangeCheck(infoLineFieldLoc, nSelected) == true) { // Make sure field in range

            // Fetch the string from the index passed in
            String option = infoLineFieldLoc.mOptions[nSelected];

            // Find out the index of the new selection within the master list
            for (int idx = 0, maxIdx = mOptionList.length; idx < maxIdx; idx++) {

                // If we find the exact match, then set the index, save and
                // re-calc a few things
                if (mOptionList[idx].contentEquals(option) == true) {
                    mFieldLines[infoLineFieldLoc.mRowIdx][infoLineFieldLoc.mFieldIdx] = idx;
                    mPref.setRowFormats(buildConfigString()); // Save to storage
                    setRowCount(); // A row may have been totally turned off
                    return;
                }
            }
        }
    }

    /***
     * Construct this object passing in the LocationView that did the creation.
     * The fields are defaulted to what is read from the shared preferences
     * 
     * @param service
     */
    public InfoLines(StorageService service) {
        mContext = service;
        mPref = new Preferences(mContext);
        mService = service;

        // separate lines for portrait vs landscape
        mFieldLines = new int[MAX_INFO_ROWS * 2][ID_FLD_MAX]; 

        // One config string for all lines
        String rowFormats = mPref.getRowFormats(); 

        // Split the string to get each row
        String strRows[] = rowFormats.split(" "); 

        for (int rowIdx = 0; rowIdx < strRows.length; rowIdx++) {
        	// Split the row string to get each field
            String arFields[] = strRows[rowIdx].split(","); 

            // Now parse the line for the values. Handle the case of invalid array
            // bounds just in case the config setting is corrupt due to version/feature change
            for (int idx = 0; idx < arFields.length; idx++) {
            	try { mFieldLines[rowIdx][idx] = Integer.parseInt(arFields[idx]);
            	} catch(Exception e) { }
            }
        }
        setRowCount(); // Determine how many rows to use

        // Read some string array values out of the resource list that we examine
        mOptionList  = mContext.getResources().getStringArray(R.array.TextFieldOptions);
        mFieldTitles = mContext.getResources().getStringArray(R.array.TextFieldOptionTitles);
    }


    public void touch(InfoLineFieldLoc infoLineFieldLoc) {
        if (infoLineFieldLoc == null) {
            return;
        }

        // Each field processes the gesture differently.
        switch (mFieldLines[infoLineFieldLoc.mRowIdx][infoLineFieldLoc.mFieldIdx]) {

            case ID_FLD_FUL: {
                if (mService != null) {
                    if(null != mService.getFuelTimer()) {
                        mService.getFuelTimer().toggleState();
                    }
                }
                break;
            }
            case ID_FLD_TMR: {
                if (mService != null) {
                    if(null != mService.getUpTimer()) {
                        mService.getUpTimer().toggleState();
                    }
                }
                break;
            }
        }
    }


     /***
     * A LONG_PRESS gesture over one of the display fields
     *
     * @param infoLineFieldLoc
     *            the field to receive the gesture
     */
    public void longPress(InfoLineFieldLoc infoLineFieldLoc) {
        if (infoLineFieldLoc == null) {
            return;
        }

        // Each field processes the gesture differently.
        switch (mFieldLines[infoLineFieldLoc.mRowIdx][infoLineFieldLoc.mFieldIdx]) {

            // Odometer - reset the value to zero
            case ID_FLD_ODO: {
                if (mService != null) {
                    Odometer odometer = mService.getOdometer();
                    if (odometer != null) {
                        odometer.reset();
                    }
                }
                break;
            }

            // Hobbs flight meter - reset it to zero
            case ID_FLD_HOB: {
                if (mService != null) {
                    mService.getFlightTimer().reset();
                }
                break;
            }

            // Current destination - clear it out.
            case ID_FLD_DST: {
                if (mService != null) {
                    if (mService.getDestination() != null) {
                        mService.setDestination(null);
                    }
                }
                break;
            }

            case ID_FLD_FUL: {
                if(null != mService) {
                    if(null != mService.getFuelTimer()) {
                        mService.getFuelTimer().reset();
                    }
                }
                break;
            }

            case ID_FLD_TMR: {
                if(null != mService) {
                    if(null != mService.getUpTimer()) {
                        mService.getUpTimer().reset();
                    }
                }
                break;
            }


            default:
                break;
        }
        return;
    }

    private void setRowCount() {
        // Determine how many status rows are being displayed
        mRowCount = 0;
        int baseRowIdx = (mDisplayOrientation == ID_DO_LANDSCAPE) ? 0
                : MAX_INFO_ROWS;
        for (int rowIdx = 0, rowMax = MAX_INFO_ROWS; rowIdx < rowMax; rowIdx++) {
            for (int fldIdx = 0, fldMax = mFieldLines[baseRowIdx + rowIdx].length; fldIdx < fldMax; fldIdx++) {
                if (mFieldLines[baseRowIdx + rowIdx][fldIdx] != ID_FLD_NUL) {
                    mRowCount = (rowIdx + 1);
                }
            }
        }
    }

    /***
     * Validate the field being selected. This does some range checks
     * to make sure the field and selected index values are sane
     * @param iLFL collection of field information locations
     * @param nSelected the index of the selected item
     * @return true/false
     */
    boolean rangeCheck(InfoLineFieldLoc iLFL, int nSelected) {
    	// Check that the row index is valid
        if ((iLFL.mRowIdx < 0) || (iLFL.mRowIdx >= mFieldLines.length))
            return false;

        // And now the column (field) index
        if ((iLFL.mFieldIdx < 0)
                || (iLFL.mFieldIdx >= mFieldLines[iLFL.mRowIdx].length))
            return false;

        // If the selected index is larger than our collection of
        // possibles, then fail
        if (nSelected >= iLFL.mOptions.length) {
        	return false;
        }
        return true;
    }

    /***
     * This method draws the top two lines of the display.
     *
     * @param canvas
     * @param aPaint
     *            for what text to use
     * @param aTextColor
     *            text color
     * @param aTextColorOpposite
     *            opposite
     * @param aShadow
     *            shadow radius
     */
    public void drawCornerTextsDynamic(Canvas canvas, Paint aPaint,
            int aTextColor, int aTextColorOpposite, int aShadow, int width, int height,
            String errorMessage, String priorityMessage) {
        // If the screen width has changed since the last time, we need to
        // recalc
        // the positions of the fields
        resizeFields(aPaint, width, height);

        float dataY = aPaint.getTextSize();
        float titleY = dataY / (float) TITLE_TO_TEXT_RATIO;
        float lineY = dataY + titleY;
        mShadowY = lineY * mRowCount + aShadow;

        if(mService != null && mService.getGpsParams() != null) {
            mAltitude = (float) mService.getGpsParams().getAltitude();
        }

        // Draw the shadowed background on the top 2 lines if we are configured
        // to do so
        if (mPref.isShowBackground()) {
            aPaint.setShadowLayer(0, 0, 0, 0);
            aPaint.setColor(aTextColorOpposite);
            aPaint.setAlpha(0x7f);
            canvas.drawRect(0, 0, mDisplayWidth, mShadowY, aPaint);
            aPaint.setAlpha(0xff);
        }
        aPaint.setShadowLayer(aShadow, aShadow, aShadow, Color.BLACK);

        // White text that is left aligned
        aPaint.setTextAlign(Align.LEFT);
        aPaint.setColor(aTextColor);

        // Lines 0/1 are for landscape, 2/3 for portrait
        int nStartLine = (mDisplayOrientation == ID_DO_LANDSCAPE) ? 0
                : MAX_INFO_ROWS;

        // The top row can be either the priority message or
        // the configured display values
        if (priorityMessage != null) {
            canvas.drawText(priorityMessage, 0, lineY - 1, aPaint);
        } else {
            for (int idx = 0, max = mFieldPosX.length; idx < max; idx++) {
                canvas.drawText(
                        getDisplayFieldValue(mFieldLines[nStartLine][idx],
                                false), mFieldPosX[idx], lineY - 1, aPaint);

                aPaint.setTextSize(titleY);
                String title = getDisplayFieldValue(
                        mFieldLines[nStartLine][idx], true);
                canvas.drawText(
                        title,
                        mFieldPosX[idx]
                                + (mFieldWidth - mCharWidth - (int) aPaint
                                        .measureText(title)) / 2, lineY - dataY
                                + 2, aPaint);
                aPaint.setTextSize(dataY);
            }
        }

        // The second line is either the red error message or
        // the additional line of configured values
        if (errorMessage != null) {
            aPaint.setTextAlign(Align.RIGHT);
            aPaint.setColor(Color.RED);
            canvas.drawText(errorMessage, mDisplayWidth, lineY * 2 - 1, aPaint);
        } else {
            for (int idx = 0, max = mFieldPosX.length; idx < max; idx++) {
                canvas.drawText(
                        getDisplayFieldValue(mFieldLines[nStartLine + 1][idx],
                                false), mFieldPosX[idx], lineY * 2 - 1, aPaint);

                aPaint.setTextSize(titleY);
                String title = getDisplayFieldValue(
                        mFieldLines[nStartLine + 1][idx], true);
                canvas.drawText(
                        title,
                        mFieldPosX[idx]
                                + (mFieldWidth - mCharWidth - (int) aPaint
                                        .measureText(title)) / 2, lineY * 2
                                - dataY + 2, aPaint);
                aPaint.setTextSize(dataY);
            }
        }
    }

    /***
     * Calculate the quantity and size of that we can display with the given
     * display width and paint.
     *
     * @param aPaint
     * @param aDisplayWidth
     */
    private void resizeFields(Paint aPaint, int aDisplayWidth, int aDisplayHeight) {
        // If the size did not change, then we don't need to do any work
        if (mDisplayWidth == aDisplayWidth)
            return;

        // Set our copy of what the display width is.
        mDisplayWidth = aDisplayWidth;

        // In what direction is the display used  ?
    	mDisplayOrientation = (aDisplayWidth > aDisplayHeight)
    						  ? ID_DO_LANDSCAPE : ID_DO_PORTRAIT;

        // Fetch the NULL field to figure out how large it is
        String strField = getDisplayFieldValue(ID_FLD_NUL, false) + " ";
        float charWidths[] = new float[strField.length()];
        aPaint.getTextWidths(strField, charWidths);
        mCharWidth = (int) charWidths[0];
        mFieldWidth = mCharWidth * strField.length();

        // Now we can determine the max fields per line we can display
        int maxFieldsPerLine = mDisplayWidth / mFieldWidth;

        // This bit of code ensures there is nothing configured to display
        // in fields that might appear off the right side of the screen. This
        // only is a problem at startup, if the default settings contain more
        // fields
        // that we can show. We do this calc now since we need to know the paint
        // specifics
        // to figure out how many fields can fit.
        int nStartLine = (mDisplayOrientation == ID_DO_LANDSCAPE) ? 0
                : MAX_INFO_ROWS;
        for (int rowIdx = 0; rowIdx < MAX_INFO_ROWS; rowIdx++) {
            for (int idx = maxFieldsPerLine; idx < mFieldLines[nStartLine
                    + rowIdx].length; idx++) {
                mFieldLines[nStartLine + rowIdx][idx] = ID_FLD_NUL;
            }
        }

        // There might be leftover space. Divide it so that it pads between
        // the fields
        int nLeftoverSpace = mDisplayWidth - maxFieldsPerLine * mFieldWidth;
        int nPadding = nLeftoverSpace / (maxFieldsPerLine - 1);

        // Now calculate the horizontal position of each field
        int nRightShift = nPadding;
        mFieldPosX = new int[maxFieldsPerLine];
        mFieldPosX[0] = 0;
        for (int idx = 1, max = mFieldPosX.length; idx < max; idx++) {
            mFieldPosX[idx] = mFieldPosX[idx - 1] + mFieldWidth + nRightShift;

            // If this is the last field then make it right justified
            if (idx == max - 1) {
                mFieldPosX[idx] = mDisplayWidth - mFieldWidth
                        + (int) charWidths[0];
            }

            // Adjust the padding between this and the next field
            if (nLeftoverSpace > nPadding) {
                nLeftoverSpace -= nPadding;
                nRightShift = nPadding;
            } else {
                nRightShift = nLeftoverSpace;
                nLeftoverSpace = 0;
            }
        }
    }

    /***
     * Return a string that represents the value of the desired field
     *
     * @param aField
     *            which type of field is being requested
     * @param aTitle
     *            true if we are only to return the title of the field
     * @return string display value for that field
     */
    private String getDisplayFieldValue(int aField, boolean aTitle) {

    	// If we are being requested to return the title, then look
    	// it up in the resource string array
        if (aTitle == true) {

        	// Local time is special, get the identifier of the local
        	// timezone
            if (aField == ID_FLD_LT) {
                return Calendar.getInstance().getTimeZone().getID();
            }

            // Ensure the index is valid in our array and return
            // with that item
            if (mFieldTitles.length > aField) {
                return mFieldTitles[aField];
            }

            // Invalid request
            return NOVALUE;
        }

        // Return the string value of a specific field
        switch (aField) {
            case ID_FLD_VSI: {
                if (mService != null) {
                    if (mService.getVSI() != null) {
                        double vsi = mService.getVSI().getValue();
                        return String.format(Locale.getDefault(), "%+05.0f", vsi);
                    }
                }
                break;
            }

            case ID_FLD_SPD: {
                if (mService != null && mService.getGpsParams() != null) {
                    return Helper.centerString(String.format(Locale.getDefault(),
                            "%.0f%s", mService.getGpsParams().getSpeed(),
                            Preferences.speedConversionUnit), MAX_FIELD_SIZE_IN_CHARS);
                }
                break;
            }

            case ID_FLD_HOB: {
                if (mService != null) {
                    return ""
                            + mService.getFlightTimer()
                                    .getValue();
                }
                break;
            }

            case ID_FLD_HDG: {
                if (mService != null && mService.getGpsParams() != null) {
                    return " "
                            + Helper.correctConvertHeading(Math.round((Helper
                                    .getMagneticHeading(mService.getGpsParams()
                                            .getBearing(), mService.getGpsParams()
                                            .getDeclinition())))) + '\u00B0';
                }
                break;
            }

            case ID_FLD_BRG: {
                if (mService != null && mService.getGpsParams() != null) {
                    if (mService.getDestination() != null) {
                        return " "
                                + Helper.correctConvertHeading(Math.round((Helper
                                        .getMagneticHeading(mService
                                                .getDestination().getBearing(),
                                                mService.getGpsParams()
                                                        .getDeclinition()))))
                                + '\u00B0';
                    }
                }
                break;
            }

            case ID_FLD_DST: {
                if (mService != null) {
                    if (mService.getDestination() != null) {
                        String name = mService.getDestination().getID();
                        if (Helper.isGPSCoordinate(name)) { // Change a direct coordinate to GPS
                            name = Destination.GPS;
                        } else if (name.length() > 5) { // Truncate name if larger than 5 chars
                            name = name.substring(0, 4);
                        }
                        return Helper.centerString(name, MAX_FIELD_SIZE_IN_CHARS);
                    }
                }
                break;
            }

            case ID_FLD_DIS: {
                if (mService != null) {
                    if (mService.getDestination() != null) {
                        double distance = mService.getDestination()
                                .getDistance();
                        String fmtString = distance >= 100 ? "%.0f%s" : "%4.1f%s";

                        return Helper.centerString(String.format(
                                Locale.getDefault(),
                                fmtString, // (distance) + "%s", // "%.0f%s",
                                distance,
                                Preferences.distanceConversionUnit),
                                MAX_FIELD_SIZE_IN_CHARS);
                    }
                }
                break;
            }

            case ID_FLD_ETE: {
                if (mService != null) {
                    if (mService.getDestination() != null) {
                        return ""
                                + mService.getDestination().getEte();
                    }
                }
                break;
            }

            case ID_FLD_ETA: {
                if (mService != null) {
                    if (mService.getDestination() != null) {
                        return ""
                                + mService.getDestination().getEta();
                    }
                }
                break;
            }

            case ID_FLD_LT: {
                Calendar localTime = Calendar.getInstance();
                return String.format(Locale.getDefault(), "%02d:%02d",
                        localTime.get(Calendar.HOUR_OF_DAY),
                        localTime.get(Calendar.MINUTE));
            }

            case ID_FLD_GMT: {
                Calendar localTime = Calendar.getInstance();
                localTime.setTimeZone(TimeZone.getTimeZone("UTC"));
                return String.format(Locale.getDefault(), "%02d:%02d",
                        localTime.get(Calendar.HOUR_OF_DAY),
                        localTime.get(Calendar.MINUTE));
            }

            case ID_FLD_MSL: {
                return Helper.centerString(Helper
                        .calculateAltitudeFromMSL(mAltitude), MAX_FIELD_SIZE_IN_CHARS);
            }

            // If we have a destination set that is a BASE,
            // calculate the vertical speed required to reach the destination
            case ID_FLD_VSR: {
                if (mService != null && mService.getGpsParams() != null) {
                    Destination destination = mService.getDestination();
                    if (destination != null) {
                        return destination.getVerticalSpeedTo(mService
                                .getGpsParams());
                    }
                }
                break;
            }

            // If we have a destination set that is a BASE,
            // calculate the flight path required to reach the destination
            case ID_FLD_FPR: {
                if (mService != null && mService.getGpsParams() != null) {
                    Destination destination = mService.getDestination();
                    if (destination != null) {
                        return destination.getFlightPathRequired(mService
                                .getGpsParams());
                    }
                }
                break;
            }

            case ID_FLD_ODO: {
                if (mService != null) {
                    Odometer odometer = mService.getOdometer();
                    if (odometer != null) {
                        double value = odometer.getValue();
                        return String.format(Locale.getDefault(),
                                getFmtString(value), value);
                    }
                }
                break;
            }

            case ID_FLD_CDI: {
                if (mService != null) {
                    if (mService.getDestination() != null) {
                        CDI cdi = mService.getCDI();
                        if (cdi != null) {
                            double value = cdi.getDeviation();
                            return String.format(Locale.getDefault(),
                                    getFmtString(value), value);
                        }
                    }
                }
                break;
            }

            case ID_FLD_FUL: {
                if (null != mService) {
                    if (null != mService.getFuelTimer()) {
                            return mService.getFuelTimer().getDisplay();
                    }
                }
                break;
            }

            case ID_FLD_TMR: {
                if (null != mService) {
                    if (null != mService.getUpTimer()) {
                        return mService.getUpTimer().getDisplay();
                    }
                }
                break;
            }
            case ID_FLD_SPP: {
                if (mService != null && mService.getExtendedGpsParams() != null) {
                    return Helper.centerString(String.format(Locale.getDefault(),
                            "%.0f%s", mService.getExtendedGpsParams().getSpeedTrend(),
                            Preferences.speedConversionUnit), MAX_FIELD_SIZE_IN_CHARS);
                }
                break;
            }
            case ID_FLD_HDP: {
                if (mService != null && mService.getExtendedGpsParams() != null && mService.getGpsParams() != null) {
                    return " "
                            + Helper.correctConvertHeading(Math.round((Helper
                            .getMagneticHeading(mService.getExtendedGpsParams()
                                    .getBearingTrend(), mService.getGpsParams()
                                    .getDeclinition())))) + '\u00B0';
                }
                break;
            }
            case ID_FLD_MSP: {
                if (mService != null && mService.getExtendedGpsParams() != null) {
                    return Helper.centerString(Helper
                            .calculateAltitudeFromMSL((float) mService.getExtendedGpsParams().getAltitudeTrend()), MAX_FIELD_SIZE_IN_CHARS);
                }
                break;
            }
        }
        return NOVALUE;
    }

    /***
     * Return a format string for the value passed in
     *
     * @param value
     * @return
     */
    private String getFmtString(double value) {
        String fmtString = "";
        if (value >= 9999)
            fmtString = "%05.0f";
        else if (value >= 1000)
            fmtString = " %04.0f";
        else if (value >= 100)
            fmtString = " %03.0f ";
        else if (value >= 10)
            fmtString = " %04.1f";
        else
            fmtString = " %03.2f";
        return fmtString;
    }

    /***
     * Create the configuration string for the current settings
     *
     * @return string that represents the current format of the fields
     */
    private String buildConfigString() {
        String rowFormats = "";
        for (int rowIdx = 0, rowMax = mFieldLines.length; rowIdx < rowMax; rowIdx++) {
            for (int fldIdx = 0, fldMax = mFieldLines[rowIdx].length; fldIdx < fldMax; fldIdx++) {
                rowFormats += mFieldLines[rowIdx][fldIdx];
                if (fldIdx < fldMax - 1) {
                    rowFormats += ',';
                }
            }
            if (rowIdx < rowMax - 1) {
                rowFormats += ' ';
            }
        }
        return rowFormats;
    }
}
