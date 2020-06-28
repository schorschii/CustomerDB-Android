package de.georgsieber.customerdb.importexport;

import android.util.Log;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.georgsieber.customerdb.CustomerDatabase;
import de.georgsieber.customerdb.model.CustomerAppointment;

public class CalendarCsvBuilder {

    private List<CustomerAppointment> mAppointments = new ArrayList<>();

    public CalendarCsvBuilder(List<CustomerAppointment> _appointments) {
        mAppointments = _appointments;
    }
    public CalendarCsvBuilder(CustomerAppointment _appointment) {
        mAppointments.add(_appointment);
    }

    private String buildCsvContent() {
        StringWriter content = new StringWriter();

        CSVWriter csvWriter = new CSVWriter(content);

        List<String> headers = new ArrayList<>(Arrays.asList(
                "id", "title", "notes", "time_start", "time_end", "fullday", "customer", "location", "last_modified"
        ));
        csvWriter.writeNext(headers.toArray(new String[0]));

        for(CustomerAppointment ca : mAppointments) {
            List<String> values = new ArrayList<>(Arrays.asList(
                    Long.toString(ca.mId),
                    ca.mTitle,
                    ca.mNotes,
                    ca.mTimeStart==null ? "" : CustomerDatabase.storageFormatWithTime.format(ca.mTimeStart),
                    ca.mTimeEnd==null ? "" : CustomerDatabase.storageFormatWithTime.format(ca.mTimeEnd),
                    ca.mFullday ? "1" : "0",
                    ca.mCustomer,
                    ca.mLocation,
                    ca.mLastModified==null ? "" : CustomerDatabase.storageFormatWithTime.format(ca.mLastModified)
            ));
            csvWriter.writeNext(values.toArray(new String[0]));
        }

        return content.toString();
    }

    public boolean saveCsvFile(File f) {
        try {
            FileOutputStream stream = new FileOutputStream(f);
            stream.write(buildCsvContent().getBytes());
            stream.close();
            return true;
        } catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
        return false;
    }

    public static List<CustomerAppointment> readCsvFile(InputStreamReader isr) throws Exception {
        List<CustomerAppointment> newAppointments = new ArrayList<>();
        CSVReader reader = new CSVReader(isr);
        String[] headers = new String[]{};
        String[] line;
        int counter = 0;
        while((line = reader.readNext()) != null) {
            try {
                if(counter == 0) {
                    headers = line;
                } else {
                    CustomerAppointment newAppointment = new CustomerAppointment();
                    int counter2 = 0;
                    for(String field : line) {
                        //Log.e("CSV", headers[counter2] + " -> "+ field);
                        newAppointment.putAttribute(headers[counter2], field);
                        counter2 ++;
                    }
                    newAppointments.add(newAppointment);
                }
            } catch(Exception ignored) {}
            counter ++;
        }
        return newAppointments;
    }

}
