package com.ds.avare.utils;

import java.util.Calendar;

import static java.util.Calendar.HOUR_OF_DAY;
import static java.util.Calendar.MINUTE;

/**
 * Created by pasniak on 4/28/2017.
 */

public class CalendarHelper {
    final private Calendar calendar;

    public CalendarHelper() {
        calendar = Calendar.getInstance();
    }

    public int getHour()
    {
        return calendar.get(HOUR_OF_DAY);
    }
    public int getMinute()
    {
        return calendar.get(MINUTE);
    }
}

