/*
 * Copied from Avare code
 * To make exe, use gcj --main=Cycle Cycle.java -o Cycle
 */

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;


public class Cycle {

    /*
     * 
     */
    private static final int getFirstDate(int year) {
        // Date for first cycle every year in January starting 2014
        switch(year) {
            case 2020:
                return 2;
            case 2021:
                return 28;
            case 2022:
                return 27;
            case 2023:
                return 26;
            case 2024:
                return 25;
            case 2025:
                return 23;
            case 2026:
                return 22;
            case 2027:
                return 21;
            case 2028:
                return 20;
            case 2029:
                return 18;
            default:
                return 0;
        }
    }
    
    /**
     * Find cycle + or - offset
     * @return
     */
    public static String findCycleOffset(String cycleName, int offset) {        
        
        int cycle;
        try {
            cycle = Integer.parseInt(cycleName);
        }
        catch (Exception e) {
            return cycleName;
        }
        
        // like 1510 = 15, 10 (15 means 2015, 10 means #28 days)
        int cycleupper = (int)(cycle / 100);
        int cyclelower = cycle - (cycleupper * 100);
        int firstdate = getFirstDate(2000 + cycleupper);
        if(firstdate < 1) {
            return cycleName;
        }
        
        // find cycle time with offset
        GregorianCalendar then = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
        then.set(2000 + cycleupper, Calendar.JANUARY, firstdate, 9, 0, 0);
        then.add(Calendar.DAY_OF_MONTH, 28 * (cyclelower - 1 + offset));
        
        // find upper two digits of cycle.
        cycleupper = (then.get(Calendar.YEAR) - 2000);
        
        // find cyclelower
        firstdate = getFirstDate(2000 + cycleupper);
        GregorianCalendar first = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
        first.set(2000 + cycleupper, Calendar.JANUARY, firstdate, 9, 0, 0);

        cycle = cycleupper * 100 + 1;
        while(first.before(then)) {
            first.add(Calendar.DAY_OF_MONTH, 28);
            cycle++;
        }

        return "" + cycle;
    }
    
    /**
     * Find the date in January when first cycle begins 
     */
    private static String getCycle() {
        /*
         * US locale as this is a folder name not language translation
         */
        GregorianCalendar now = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
        int year = now.get(Calendar.YEAR);
        int firstdate = getFirstDate(year);
        GregorianCalendar now2 = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
        now2.set(year, Calendar.JANUARY, firstdate, 9, 0, 0);
        if (now2.after(now)) {
            /*
             * Lets handle the case when year has just turned
             */
            year--;
            firstdate = getFirstDate(year);
        }
        
        // cycle's upper two digit are year
        GregorianCalendar epoch = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
        int cycle = (year - 2000) * 100;
        
        if(firstdate < 1) {
            return "";
        }
        
        // now find cycle on todays date
        epoch.set(year, Calendar.JANUARY, firstdate, 9, 0, 0);
        cycle++;
        epoch.add(Calendar.DAY_OF_MONTH, 28);
        if(!epoch.after(now)) {
            while(true) {
                epoch.add(Calendar.DAY_OF_MONTH, 28);
                cycle++;
                if(epoch.after(now)) {
                    break;
                }
            }
        }

        return "" + cycle;
    }
    


    public static void main(String args[]) {
    
        String c = getCycle();
        String n = findCycleOffset(c, 1);
        if(args[0].equals("c")) {
            System.out.println(c);
        }
        if(args[0].equals("n")) {
            System.out.println(n);
        }

    }
    
}

