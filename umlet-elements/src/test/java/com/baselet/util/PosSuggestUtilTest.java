package com.baselet.util;

import org.junit.Assert;
import org.junit.Test;

public class PosSuggestUtilTest {

    @Test
    public void shouldSuggestInCenterSuccess() {
        int[] vs = PosSuggestUtil.suggestValue(2, 3, 2, 3);
        Assert.assertArrayEquals(new int[]{3, 3}, vs);
    }


    @Test
    public void shouldSuggestInRangeSuccess() {
        int[] vs = PosSuggestUtil.suggestValue(2, 3, 4, 3);
        Assert.assertArrayEquals(new int[]{4, 4}, vs);
    }


    @Test
    public void shouldSuggestInBorderSuccess() {
        int[] vs = PosSuggestUtil.suggestValue(2, 3, 5, 3);
        Assert.assertArrayEquals(new int[]{5, 5}, vs);
    }


    @Test
    public void shouldSuggestOutofRangeSuccess() {
        int[] vs = PosSuggestUtil.suggestValue(2, 3, 6, 3);
        Assert.assertArrayEquals(new int[]{5, 6}, vs);
    }

    @Test
    public void shouldSuggestOutofRange2Success() {
        int[] vs = PosSuggestUtil.suggestValue(6, 3, 2, 3);
        Assert.assertArrayEquals(new int[]{6, 5}, vs);
    }

}
