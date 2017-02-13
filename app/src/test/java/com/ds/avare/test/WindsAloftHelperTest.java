package com.ds.avare.test;

import com.ds.avare.utils.WindsAloftHelper;
import com.ds.avare.weather.WindsAloft;

import org.junit.Test;
import org.xmlunit.matchers.EvaluateXPathMatcher;
import org.xmlunit.matchers.HasXPathMatcher;

import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.MatcherAssert.assertThat;


/**
 * Created by Michal on 2/12/2017.
 */
public class WindsAloftHelperTest {
    @Test
    public void testLevel3000() throws Exception {
        WindsAloft wa = new WindsAloft();
        wa.station = "STATION";
        wa.w3k = "1430";
        String result = WindsAloftHelper.formatWindsHTML(wa, 3);
        String resultXml = xml(html(result));

        assertThat(resultXml, HasXPathMatcher.hasXPath("//html/table/tr"));
        assertCells(resultXml, 1, new String[] {"3000" , "140°" ,"30kt"});
    }

    @Test
    public void testLevel6000() throws Exception {
        WindsAloft wa = new WindsAloft();
        wa.station = "STATION";
        wa.w3k = "1430";
        wa.w6k = "2837+02";
        String result = WindsAloftHelper.formatWindsHTML(wa, 6);
        String resultXml = xml(html(result));

        assertCells(resultXml, 2, new String[] {"6000", "280°", "37kt", "2C"});
    }

    ///this is needed to make html fragments parsable
    private static String html(String c) { return "<html>"+c+"</html>"; }
    private static String xml(String x) { return "<?xml version=\"1.0\"?>\n" +
            "<!DOCTYPE html [\n" +
            "    <!ENTITY nbsp \"&#160;\">\n" +
            "]>\n" + x;}
    private static void assertTdEndsWith(String resultXml, int tr, int td, String end) {
        assertThat(resultXml, EvaluateXPathMatcher.hasXPath("//html/table/tr["+tr+"]/td["+td+"]/text()",
                endsWith(end)));
    }
    private static void assertCells (String resultXml, int row, String c[]) {
        assertTdEndsWith(resultXml,row,1,c[0]);
        assertTdEndsWith(resultXml,row,2,c[1]);
        assertTdEndsWith(resultXml,row,3,c[2]);
        if (c.length==4) assertTdEndsWith(resultXml,row,4,c[3]);
    }
}