package de.georgsieber.customerdb.tools;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

public class DateControl {

    public static DateFormat birthdayDateFormat = DateFormat.getDateInstance(DateFormat.LONG, Locale.getDefault());
    public static DateFormat displayDateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.getDefault());
    public static DateFormat displayTimeFormat = DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault());

}
