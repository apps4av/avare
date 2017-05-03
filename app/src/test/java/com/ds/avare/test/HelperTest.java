package com.ds.avare.test;

import com.ds.avare.utils.CalendarHelper;
import com.ds.avare.utils.Helper;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;

/**
 * Created by pasniak on 2/16/2017.
 */
@RunWith(MockitoJUnitRunner.class)
public class HelperTest {
    @Test
    public void testCalculateEte() throws Exception {
        assertEquals(Helper.calculateEte(0, 0, 10, true), "--:--");
        assertEquals(Helper.calculateEte(0, 0, 10, false), "00.10");
        assertEquals(Helper.calculateEte(100, 100, 0, true), "01:00");
        assertEquals(Helper.calculateEte(10,  100, 0, true), "06.00");
    }
    @Test
    public void testPerfOfCalculateEte() throws Exception {
        // 750ms on my PC
        for (int dist = 0; dist < 100000; dist++) {
            for (int speed = 0; speed < 100; speed++) {
                String res1 = Helper.calculateEte(dist, speed, 0, true);
            }
        }
    }
    @Mock
    CalendarHelper mockCalendar;

    @Test
    public void testCalculateEta() throws Exception {

        Mockito.when(mockCalendar.getHour()).thenReturn(12);
        Mockito.when(mockCalendar.getMinute()).thenReturn(0);

        assertEquals(Helper.calculateEta(mockCalendar, 0, 0), "--:--");
        assertEquals(Helper.calculateEta(mockCalendar, 1000, 100), "22:00");
    }

    @Test
    public void testPerfOfCalculateEta() throws Exception {
        // 644ms on my PC
        // was 900ms when Calendar.getInstance() is called twice
        CalendarHelper ch = CalendarHelper.getInstance(0);
        for (int dist = 0; dist < 13000; dist++) {
            for (int speed = 0; speed < 100; speed++) {
                String res1 = Helper.calculateEta(ch, dist, speed);
            }
        }
    }
}