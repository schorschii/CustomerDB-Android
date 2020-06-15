package de.georgsieber.customerdb.tools;

import java.text.NumberFormat;
import java.util.Locale;

public class NumTools {
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
}
