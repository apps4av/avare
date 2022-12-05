package com.ds.avare.test;

import com.ds.avare.utils.WindsAloftHelper;
import com.ds.avare.weather.WindsAloft;

import org.junit.Before;
import org.junit.Test;

import static com.ds.avare.test.HtmlAsserts.assertCellCount;
import static com.ds.avare.test.HtmlAsserts.assertCells;
import static com.ds.avare.test.HtmlAsserts.assertRowCount;
import static junit.framework.Assert.assertEquals;


/**
 * Created by pasniak on 2/12/2017.
 * tests all public interfaces of WindsAloftHelper:
 *  formatWindsHTML
 *  DirSpeed.parseFrom
 */
public class WindsAloftHelperTest {

    @Before
    public void setUp() throws Exception {
        wa = new WindsAloft();
    }

    // test winds/temps parsing
    @Test
    public void testDirSpeedInterface() throws Exception {
        WindsAloftHelper.DirSpeed wind = WindsAloftHelper.DirSpeed.parseFrom("1430");
        assertEquals(wind.Dir, 140);
        assertEquals(wind.Speed, 30);
    }

    @Test
    public void testDirSpeedInterfaceWithTemps() throws Exception {
        WindsAloftHelper.DirSpeed wind = WindsAloftHelper.DirSpeed.parseFrom("1212+00");
        assertEquals(wind.Dir, 120);
        assertEquals(wind.Speed, 12);
    }

    @Test (expected=StringIndexOutOfBoundsException.class)
    public void testDirSpeedInterfaceWithBadData1() throws Exception {
        WindsAloftHelper.DirSpeed wind = WindsAloftHelper.DirSpeed.parseFrom("+00");
    }

    @Test (expected=StringIndexOutOfBoundsException.class)
    public void testDirSpeedInterfaceWithBadData2() throws Exception {
        WindsAloftHelper.DirSpeed wind = WindsAloftHelper.DirSpeed.parseFrom("+");
    }

    @Test (expected=NumberFormatException.class)
    public void testDirSpeedInterfaceGarbage() throws Exception {
        WindsAloftHelper.DirSpeed.parseFrom("garbage");
    }

    // test output table formatting
    @Test
    public void testEmpty3000() throws Exception {
        wa.w3k = "";
        wa.w6k = "2837+02";
        String result = WindsAloftHelper.formatWindsHTML(wa, 6);

        assertRowCount(result, 2);
    }

    @Test
    public void testLevel3000() throws Exception {
        wa.w3k = "1430";
        String result = WindsAloftHelper.formatWindsHTML(wa, 3);

        assertCells(result, 1, new String[] {"3000" , "140°" ,"30kt"});
    }

    @Test
    public void testLevel6000() throws Exception {
        wa.w3k = "1430";
        wa.w6k = "2837+02";
        String result = WindsAloftHelper.formatWindsHTML(wa, 6);

        assertCells(result, 2, new String[] {"6000", "280°", "37kt", "2C"});
    }

    @Test
    public void testLightAndVariable() throws Exception {
        wa.w3k = "9900";
        wa.w6k = "9900-01";

        String result = WindsAloftHelper.formatWindsHTML(wa, 6);

        assertCells(result, 1, new String[] {"3000", "0°", "0kt"});
        assertCells(result, 2, new String[] {"6000", "0°", "0kt", "-1C"});
    }


    @Test
    public void testAbove100kt() throws Exception {
        wa.w3k = "780061";
        wa.w6k = "850459";
        String result = WindsAloftHelper.formatWindsHTML(wa, 6);

        assertCells(result, 1, new String[] {"3000", "280°", "100kt", "-61C"});
        assertCells(result, 2, new String[] {"6000", "350°", "104kt", "-59C"});
    }

    private void SetupFullTable() {
        String[] winds = "9900 0214+13 3609+09 3410+03 3120-09 3126-22 292739 293148 772457".split(" ");
        wa.w3k = winds[0];
        wa.w6k = winds[1];
        wa.w9k = winds[2];
        wa.w12k = winds[3];
        wa.w18k = winds[4]; // show up to this level so 5 rows
        wa.w24k = winds[5];
        wa.w30k = winds[6];
        wa.w34k = winds[7];
        wa.w39k = winds[8];
    }

    @Test
    public void TestFullTableUpTo18() throws Exception {
        SetupFullTable();
        String result = WindsAloftHelper.formatWindsHTML(wa, 18); // up to w18k

        assertRowCount(result, 5); // 5 rows (3,6,9,12,18) up to w18k
        assertCellCount(result, 5*4);
    }

    @Test
    public void TestFullTable() throws Exception {
        SetupFullTable();
        String result = WindsAloftHelper.formatWindsHTML(wa, 39);

        assertRowCount(result, 9); // all 9 rows
        assertCellCount(result, 9*4);
    }

    @Test
    public void testGarbage() throws Exception {
        wa.w3k = "garbage";
        wa.w6k = "1212--"; // garbage temperature, parsing fails
        wa.w9k = "12345678";
        wa.w12k = "+00"; // short string
        String result = WindsAloftHelper.formatWindsHTML(wa, 12);

        assertRowCount(result, 4);
    }

    private WindsAloft wa;
}