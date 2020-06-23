package de.georgsieber.customerdb.tools;

import android.content.Context;

import java.text.NumberFormat;
import java.util.Locale;

public class NumTools {
    public static long tryParseLong(String value, long defaultVal) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }
    public static Double tryParseDouble(String str) {
        try {
            NumberFormat format = NumberFormat.getInstance(Locale.getDefault());
            Number number = format.parse(str);
            return number.doubleValue();
            //return Double.parseDouble(str);
        }
        catch(Exception e) {
            return null;
        }
    }
    public static Integer tryParseInt(String str) {
        try {
            return Integer.parseInt(str);
        }
        catch(Exception e) {
            return null;
        }
    }

    @SuppressWarnings("SameParameterValue")
    public static int dpToPx(int dp, Context c) {
        return (int)(dp * c.getResources().getDisplayMetrics().density);
    }
}
