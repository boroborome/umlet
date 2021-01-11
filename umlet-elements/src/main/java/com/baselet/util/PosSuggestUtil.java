package com.baselet.util;

public class PosSuggestUtil {
    public static int[] suggestValue(int v1, int size1, int v2, int size2) {
        int end1 = v1 + size1;
        int end2 = v2 + size2;

        int start = Math.max(v1, v2);
        int end = Math.min(end1, end2);

        if (start <= end) {
            int m = (start + end) / 2;
            return new int[]{m, m};
        }
        if (v1 > v2) {
            return new int[]{v1, end2};
        } else {
            return new int[]{end1, v2};
        }
    }
}
