package de.georgsieber.customerdb.importexport;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

class QuotedPrintable {

    @SuppressWarnings("CharsetObjectCanBeUsed")
    static String decode(String str) {
        ArrayList<Byte> bytes = new ArrayList<>();
        for(int i = 0; i < str.length(); i++) {
            char currentChar = str.charAt(i);
            try {
                if(currentChar == '=' && i+2 <= str.length()-1) {
                    char nextChar1 = str.charAt(i+1);
                    char nextChar2 = str.charAt(i+2);
                    int val = Integer.parseInt(String.valueOf(nextChar1)+String.valueOf(nextChar2), 16);
                    bytes.add((byte) val);
                    i += 2;
                    continue;
                }
            } catch(Exception ignored) {}
            bytes.add((byte) currentChar);
        }
        try {
            return new String(toByteArray(bytes), "UTF-8");
        } catch(UnsupportedEncodingException ignored) {
            return str;
        }
    }

    @SuppressWarnings("CharsetObjectCanBeUsed")
    static String encode(String str) {
        StringBuilder encoded = new StringBuilder();
        try {
            for(byte b : str.getBytes("UTF-8")) {
                encoded.append("=").append(String.format("%02X", b));
            }
        } catch(UnsupportedEncodingException ignored) {}
        return encoded.toString();
    }

    private static byte[] toByteArray(ArrayList<Byte> in) {
        final int n = in.size();
        byte[] ret = new byte[n];
        for (int i = 0; i < n; i++) {
            ret[i] = in.get(i);
        }
        return ret;
    }

    static boolean isVcfFieldQuotedPrintableEncoded(String[] fieldOptions) {
        for(String option : fieldOptions) {
            if(option.toUpperCase().equals("ENCODING=QUOTED-PRINTABLE")) {
                return true;
            }
        }
        return false;
    }

}
