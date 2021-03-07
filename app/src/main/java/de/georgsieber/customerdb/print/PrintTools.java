package de.georgsieber.customerdb.print;

public class PrintTools {
    static String wordWrap(final String input, final int length) {
        if(input == null || length < 1) {
            throw new IllegalArgumentException("Invalid input args");
        }
        final String text = input.trim();
        if(text.length() > length && text.contains(" ")) {
            final String line = text.substring(0, length);
            final int lineBreakIndex = line.indexOf("\n");
            final int lineLastSpaceIndex = line.lastIndexOf(" ");
            final int inputFirstSpaceIndex = text.indexOf(" ");
            final int breakIndex = lineBreakIndex > -1 ? lineBreakIndex :
                    (lineLastSpaceIndex > -1 ? lineLastSpaceIndex : inputFirstSpaceIndex);
            return text.substring(0, breakIndex) + "\n" + wordWrap(text.substring(breakIndex + 1), length);
        } else {
            return text;
        }
    }
}
