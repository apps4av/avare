package com.ds.avare.test;

import org.xmlunit.matchers.EvaluateXPathMatcher;

import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.startsWith;

/**
 * Created by pasniak on 2/15/2017.
 */
public class HtmlAsserts {
    public static void assertCells(String result, int row, String cells[]) {
        String resultXml = xml(html(result));

        for (int i = 0; i < cells.length; i++) {
            assertTdEndsWith(resultXml, row, 1 + i, cells[i]);
        }
    }

    ///this is needed to make html fragments parsable
    public static String html(String c) { return "<html>"+c+"</html>"; }
    public static String xml(String x) { return "<?xml version=\"1.0\"?>\n" +
            "<!DOCTYPE html [\n" +
            "    <!ENTITY nbsp \"&#160;\">\n" +
            "]>\n" + x;}

    ///see https://github.com/xmlunit/xmlunit
    private static void assertTdEndsWith(String result, int tr, int td, String end) {
        assertThat(result, EvaluateXPathMatcher.hasXPath("//html/table/tr["+tr+"]/td["+td+"]/text()",
                endsWith(end)));
    }
    private static void assertTdStartsWith(String result, int tr, int td, String end) {
        assertThat(result, EvaluateXPathMatcher.hasXPath("//html/table/tr["+tr+"]/td["+td+"]/text()",
                startsWith(end)));
    }

    public static void assertRowCount (String result, int count) {
        String resultXml = xml(html(result));
        assertThat(resultXml, EvaluateXPathMatcher.hasXPath("count(//html/table/tr)",
                is(Integer.toString(count))));
    }
    public static void assertCellCount (String result, int count) {
        String resultXml = xml(html(result));
        assertThat(resultXml, EvaluateXPathMatcher.hasXPath("count(//html/table/tr/td)",
                is(Integer.toString(count))));
    }
}
