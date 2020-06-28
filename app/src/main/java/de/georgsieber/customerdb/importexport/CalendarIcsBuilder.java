package de.georgsieber.customerdb.importexport;

import android.annotation.SuppressLint;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import de.georgsieber.customerdb.model.Customer;
import de.georgsieber.customerdb.model.CustomerAppointment;

public class CalendarIcsBuilder {

    static class IcsEntry {
        ArrayList<IcsField> mFields = new ArrayList<>();
        IcsEntry() {
            super();
        }
    }
    static class IcsField {
        String[] mOptions;
        String[] mValues;
        IcsField(String[] options, String[] values) {
            mOptions = options;
            mValues = values;
        }
    }

    private List<CustomerAppointment> mAppointments = new ArrayList<>();

    public CalendarIcsBuilder(List<CustomerAppointment> _appointments) {
        mAppointments = _appointments;
    }
    public CalendarIcsBuilder(CustomerAppointment _appointment) {
        mAppointments.add(_appointment);
    }

    @SuppressLint("SimpleDateFormat")
    public static DateFormat dateFormatIcs = new SimpleDateFormat("yyyyMMdd'T'HHmmss");

    private String buildIcsContent() {
        StringBuilder content = new StringBuilder();
        content.append("BEGIN:VCALENDAR\n");
        content.append("VERSION:2.0\n");
        for(CustomerAppointment ca : mAppointments) {
            content.append("BEGIN:VEVENT\n");
            content.append("SUMMARY:").append(ca.mTitle).append("\n");
            content.append("DESCRIPTION:").append(escapeIcsValue(ca.mNotes)).append("\n");
            content.append("LOCATION:").append(ca.mLocation).append("\n");
            content.append("DTSTART:").append(dateFormatIcs.format(ca.mTimeStart)).append("\n");
            content.append("DTEND:").append(dateFormatIcs.format(ca.mTimeEnd)).append("\n");
            content.append("END:VEVENT\n\n");
        }
        content.append("END:VCALENDAR\n");
        return content.toString();
    }

    private String escapeIcsValue(String value) {
        return value.replace("\n", "\\n");
    }

    public boolean saveIcsFile(File f) {
        try {
            FileOutputStream stream = new FileOutputStream(f);
            stream.write(buildIcsContent().getBytes());
            stream.close();
            return true;
        } catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
        return false;
    }

    private static String[] ICS_FIELDS = {
            "PRODID", "METHOD", "BEGIN", "TZID", "DTSTART", "DTEND", "DTSTAMP", "RRULE",
            "TZOFFSETFROM", "TZOFFSETTO", "END", "UID", "ORGANIZER", "LOCATION", "GEO",
            "SUMMARY", "DESCRIPTION", "CLASS", "VERSION",
    };
    private static boolean isIcsField(String text) {
        String upperText = text.toUpperCase();
        String[] keyValue = upperText.split(":");
        if(keyValue.length >= 1) {
            String[] subKeys = keyValue[0].split(";");
            if(subKeys[0].startsWith("X-")) {
                return true;
            }
            for(String field : ICS_FIELDS) {
                if(subKeys[0].startsWith(field)) {
                    return true;
                }
            }
        }
        return false;
    }

    @SuppressWarnings("StringConcatenationInLoop")
    public static List<CustomerAppointment> readIcsFile(InputStreamReader isr) throws IOException {
        // STEP 0 : a ics field can be broken up into multiple lines
        // we concatenate them here into a single line again
        BufferedReader br = new BufferedReader(isr);
        ArrayList<String> icsFields = new ArrayList<>();
        String currFieldValue = "";
        String line0;
        while((line0 = br.readLine()) != null) {
            if(isIcsField(line0)) {
                if(!currFieldValue.trim().equals("")) {
                    icsFields.add(currFieldValue);
                }
                currFieldValue = line0.trim();
            } else {
                String append = line0.trim();
                // avoid the double equal sign hell
                if(append.startsWith("=") && currFieldValue.endsWith("=")) {
                    append = append.substring(1);
                }
                currFieldValue += append;
            }
        }
        if(!currFieldValue.trim().equals("")) {
            icsFields.add(currFieldValue);
        }

        // STEP 1 : parse ICS string into structured data
        IcsEntry tempIcsEntry = null;
        ArrayList<IcsEntry> tempIcsEntries = new ArrayList<>();
        for(String line : icsFields) {
            String upperLine = line.toUpperCase();
            if(upperLine.startsWith("BEGIN:VEVENT")) {
                tempIcsEntry = new IcsEntry();
            }
            if(upperLine.startsWith("END:VEVENT")) {
                if(tempIcsEntry != null) {
                    tempIcsEntries.add(tempIcsEntry);
                }
                tempIcsEntry = null;
            }
            if(tempIcsEntry != null) {
                String[] keyValue = line.split(":", 2);
                if(keyValue.length != 2) continue;
                String[] options = keyValue[0].split(";");
                String[] values = keyValue[1].split(";");
                if(QuotedPrintable.isVcfFieldQuotedPrintableEncoded(options)) {
                    // decode quoted printable encoded fields
                    ArrayList<String> decodedValuesList = new ArrayList<>();
                    for(String v : values) {
                        decodedValuesList.add(QuotedPrintable.decode(v));
                    }
                    tempIcsEntry.mFields.add(new IcsField(options, decodedValuesList.toArray(new String[0])));
                } else {
                    tempIcsEntry.mFields.add(new IcsField(options, values));
                }
            }
        }

        // STEP 2 : create customers from ICS data
        List<CustomerAppointment> newAppointments = new ArrayList<>();
        for(IcsEntry e : tempIcsEntries) {
            CustomerAppointment newAppointment = new CustomerAppointment();

            // apply all ICS fields
            for(IcsField f : e.mFields) {
                switch(f.mOptions[0].toUpperCase()) {
                    case "SUMMARY":
                        if(f.mValues.length >= 1) newAppointment.mTitle = f.mValues[0];
                        break;

                    case "DESCRIPTION":
                        if(f.mValues.length >= 1) newAppointment.mNotes = f.mValues[0];
                        break;

                    case "LOCATION":
                        newAppointment.mLocation = f.mValues[0];
                        break;

                    case "DTSTART":
                        try {
                            newAppointment.mTimeStart = dateFormatIcs.parse(f.mValues[0]);
                        } catch (Exception ignored) {}
                        break;

                    case "DTEND":
                        try {
                            newAppointment.mTimeEnd = dateFormatIcs.parse(f.mValues[0]);
                        } catch (Exception ignored) {}
                        break;
                }
            }

            // only add if name is not empty
            if(!newAppointment.mTitle.equals("")) {
                newAppointments.add(newAppointment);
            }
        }

        return newAppointments;
    }
}
