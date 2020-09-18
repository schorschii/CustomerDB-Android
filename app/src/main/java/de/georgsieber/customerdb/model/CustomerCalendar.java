package de.georgsieber.customerdb.model;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ThreadLocalRandom;

import de.georgsieber.customerdb.CustomerDatabase;
import de.georgsieber.customerdb.tools.NumTools;

public class CustomerCalendar {

    public long mId = -1;
    public String mTitle = "";
    public String mColor = "";
    public String mNotes = "";
    public Date mLastModified = new Date();
    public int mRemoved = 0;

    public CustomerCalendar() {
        super();
    }
    public CustomerCalendar(long _id, String _title, String _color, String _notes, Date _lastModified, int _removed) {
        mId = _id;
        mTitle = _title;
        mColor = _color;
        mNotes = _notes;
        mLastModified = _lastModified;
        mRemoved = _removed;
    }

    @NonNull
    @Override
    public String toString() {
        return mTitle;
    }

    public static long generateID() {
        /* This function generates an unique mId for a new record.
         * Required in order to get unique ids over multiple devices,
         * which are not in sync with the central mysql server */
        @SuppressLint("SimpleDateFormat")
        DateFormat idFormat = new SimpleDateFormat("yyyyMMddkkmmss");
        String random;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            random = String.valueOf(ThreadLocalRandom.current().nextInt(1, 100));
        } else {
            random = "10";
        }
        return Long.parseLong(idFormat.format(new Date())+random);
    }

    public void putAttribute(String key, String value) {
        switch(key) {
            case "id":
                mId = NumTools.tryParseLong(value, mId);
                break;
            case "title":
                mTitle = value;
                break;
            case "color":
                mColor = value;
                break;
            case "notes":
                mNotes = value;
                break;
            case "last_modified":
                try {
                    mLastModified = CustomerDatabase.parseDate(value);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                break;
            case "removed":
                mRemoved = Integer.parseInt(value); break;
        }
    }

}
