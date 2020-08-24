package de.georgsieber.customerdb.model;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.ThreadLocalRandom;

import de.georgsieber.customerdb.CustomerDatabase;
import de.georgsieber.customerdb.tools.NumTools;

public class CustomerAppointment {

    public long mId = -1;
    public long mCalendarId = -1;
    public String mTitle = "";
    public String mNotes = "";
    public Date mTimeStart = null;
    public Date mTimeEnd = null;
    public boolean mFullday = false;
    public String mCustomer = "";
    public Long mCustomerId = null;
    public String mLocation = "";
    public Date mLastModified = new Date();
    public int mRemoved = 0;

    public String mColor = "";

    public CustomerAppointment() {
        super();
    }
    public CustomerAppointment(long _id, long _calendarId, String _title, String _notes, Date _timeStart, Date _timeEnd, boolean _fullday, String _customer, Long _customerId, String _location, Date _lastModified, int _removed) {
        mId = _id;
        mCalendarId = _calendarId;
        mTitle = _title;
        mNotes = _notes;
        mTimeStart = _timeStart;
        mTimeEnd = _timeEnd;
        mFullday = _fullday;
        mCustomer = _customer;
        mCustomerId = _customerId;
        mLocation = _location;
        mLastModified = _lastModified;
        mRemoved = _removed;
    }

    @NonNull
    @Override
    public String toString() {
        return mTitle;
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) return true;
        if(o == null || getClass() != o.getClass()) return false;
        CustomerAppointment event = (CustomerAppointment) o;
        return mId == event.mId;
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
    public static long generateID(int suffix) {
        /* specific suffix for bulk import */
        @SuppressLint("SimpleDateFormat")
        DateFormat idFormat = new SimpleDateFormat("yyyyMMddkkmmss");
        return Long.parseLong(idFormat.format(new Date())+suffix);
    }

    public void putAttribute(String key, String value) {
        switch(key) {
            case "id":
                mId = NumTools.tryParseLong(value, mId);
                break;
            case "calendar_id":
                mCalendarId = NumTools.tryParseLong(value, mCalendarId);
                break;
            case "title":
                mTitle = value;
                break;
            case "notes":
                mNotes = value;
                break;
            case "time_start":
                try {
                    mTimeStart = CustomerDatabase.storageFormatWithTime.parse(value);
                } catch (ParseException ignored) {}
                break;
            case "time_end":
                try {
                    mTimeEnd = CustomerDatabase.storageFormatWithTime.parse(value);
                } catch (ParseException ignored) {}
                break;
            case "fullday":
                Integer parsed = NumTools.tryParseInt(value);
                mFullday = (parsed==null ? 0 : parsed) > 0;
                break;
            case "customer":
                mCustomer = value;
                break;
            case "customer_id":
                mCustomerId = NumTools.tryParseNullableLong(value, mCustomerId);
                break;
            case "location":
                mLocation = value;
                break;
            case "last_modified":
                try {
                    mLastModified = new Date();
                    mLastModified = CustomerDatabase.storageFormatWithTime.parse(value);
                } catch (ParseException ignored) {}
                break;
            case "removed":
                mRemoved = Integer.parseInt(value); break;
        }
    }

    Calendar mCalendar = Calendar.getInstance();
    public int getStartTimeInMinutes() {
        mCalendar.setTime(mTimeStart);
        int hour = mCalendar.get(Calendar.HOUR_OF_DAY);
        int min = mCalendar.get(Calendar.MINUTE);
        return (int)(hour * 60) + min;
    }
    public int getEndTimeInMinutes() {
        mCalendar.setTime(mTimeEnd);
        int hour = mCalendar.get(Calendar.HOUR_OF_DAY);
        int min = mCalendar.get(Calendar.MINUTE);
        return (int)(hour * 60) + min;
    }

}
