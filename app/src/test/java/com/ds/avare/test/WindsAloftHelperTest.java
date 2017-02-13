package com.ds.avare.test;

import com.ds.avare.utils.WindsAloftHelper;
import com.ds.avare.weather.WindsAloft;

import org.junit.Before;
import org.junit.Test;
import org.xmlunit.matchers.EvaluateXPathMatcher;

import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;


/**
 * Created by pasniak on 2/12/2017.
 */
public class WindsAloftHelperTest {

    @Before
    public void setUp() throws Exception {
        wa = new WindsAloft();
    }

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
        String result = WindsAloftHelper.formatWindsHTML(wa, 3);

        assertCells(result, 1, new String[] {"3000", "0°", "0kt"});
    }

    @Test
    public void testAbove100kt() throws Exception {
        wa.w3k = "780061";
        String result = WindsAloftHelper.formatWindsHTML(wa, 3);

        assertCells(result, 1, new String[] {"3000", "280°", "100kt", "-61C"});
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
        String result = WindsAloftHelper.formatWindsHTML(wa, 18);

        assertRowCount(result, 5);
        assertCellCount(result, 5*4);
    }

    @Test
    public void TestFullTable() throws Exception {
        SetupFullTable();
        String result = WindsAloftHelper.formatWindsHTML(wa, 39);

        assertRowCount(result, 9);
        assertCellCount(result, 9*4);
    }

    @Test
    public void testGarbage() throws Exception {
        wa.w3k = "absdkfj";
        wa.w6k = "1212--"; // garbage temperature, parsing fails
        wa.w9k = "12345678";
        String result = WindsAloftHelper.formatWindsHTML(wa, 9);

        assertRowCount(result, 3);
    }

    ///this is needed to make html fragments parsable
    private static String html(String c) { return "<html>"+c+"</html>"; }
    private static String xml(String x) { return "<?xml version=\"1.0\"?>\n" +
            "<!DOCTYPE html [\n" +
            "    <!ENTITY nbsp \"&#160;\">\n" +
            "]>\n" + x;}

    ///see https://github.com/xmlunit/xmlunit
    private static void assertTdEndsWith(String result, int tr, int td, String end) {
        assertThat(result, EvaluateXPathMatcher.hasXPath("//html/table/tr["+tr+"]/td["+td+"]/text()",
                endsWith(end)));
    }
    private static void assertCells (String result, int row, String c[]) {
        String resultXml = xml(html(result));
        assertTdEndsWith(resultXml,row,1,c[0]);
        assertTdEndsWith(resultXml,row,2,c[1]);
        assertTdEndsWith(resultXml,row,3,c[2]);
        if (c.length==4) assertTdEndsWith(resultXml,row,4,c[3]);
    }
    private static void assertRowCount (String result, int count) {
        String resultXml = xml(html(result));
        assertThat(resultXml, EvaluateXPathMatcher.hasXPath("count(//html/table/tr)",
                is(Integer.toString(count))));
    }
    private static void assertCellCount (String result, int count) {
        String resultXml = xml(html(result));
        assertThat(resultXml, EvaluateXPathMatcher.hasXPath("count(//html/table/tr/td)",
                is(Integer.toString(count))));
    }
    private WindsAloft wa;
}