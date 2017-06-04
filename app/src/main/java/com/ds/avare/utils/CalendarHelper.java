package com.ds.avare.utils;

import java.util.Calendar;

import static java.util.Calendar.HOUR_OF_DAY;
import static java.util.Calendar.MINUTE;

/**
 * Created by pasniak on 4/28/2017.
 */

public class CalendarHelper {
    private static CalendarHelper mInstance = null; // singleton


    private Calendar mCalendar;

    private CalendarHelper() {
        mCalendar = Calendar.getInstance();
    }

    public static CalendarHelper getInstance(long timeInMillis) {
        if(null == mInstance) {
            mInstance = new CalendarHelper();
        }
        mInstance.setTime(timeInMillis);
        return mInstance;
    }

    private void setTime(long timeInMillis) { mCalendar.setTimeInMillis(timeInMillis);}
    public int getHour()
    {
        return mCalendar.get(HOUR_OF_DAY);
    }
    public int getMinute()
    {
        return mCalendar.get(MINUTE);
    }
}
