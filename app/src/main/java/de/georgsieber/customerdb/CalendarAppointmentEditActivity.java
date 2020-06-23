package de.georgsieber.customerdb;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.Spinner;
import android.widget.TimePicker;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import de.georgsieber.customerdb.model.CustomerAppointment;
import de.georgsieber.customerdb.model.CustomerCalendar;
import de.georgsieber.customerdb.tools.ColorControl;
import de.georgsieber.customerdb.tools.CommonDialog;
import de.georgsieber.customerdb.tools.DateControl;

public class CalendarAppointmentEditActivity extends AppCompatActivity {

    private CalendarAppointmentEditActivity me;

    private long mCurrentAppointmentId = -1;
    private CustomerAppointment mCurrentAppointment;
    private SharedPreferences mSettings;
    private CustomerDatabase mDb;
    private Calendar mCalendar = Calendar.getInstance();
    private List<CustomerCalendar> mCustomerCalendars;

    Spinner mSpinnerCalendar;
    EditText mEditTextTitle;
    EditText mEditTextCustomer;
    EditText mEditTextNotes;
    Button mButtonDay;
    TimePicker mTimePickerStart;
    TimePicker mTimePickerEnd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // init settings
        mSettings = getSharedPreferences(MainActivity.PREFS_NAME, 0);

        // init database
        mDb = new CustomerDatabase(this);

        // init activity view
        super.onCreate(savedInstanceState);
        me = this;
        setContentView(R.layout.activity_calendar_appointment_edit);

        // init toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if(getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // init colors
        ColorControl.updateActionBarColor(this, mSettings);

        // find views
        mSpinnerCalendar = findViewById(R.id.spinnerCalendar);
        mEditTextTitle = findViewById(R.id.editTextTitle);
        mEditTextNotes = findViewById(R.id.editTextNotes);
        mEditTextCustomer = findViewById(R.id.editTextCustomer);
        mButtonDay = findViewById(R.id.buttonDay);
        mTimePickerStart = findViewById(R.id.timePickerStart);
        mTimePickerStart.setIs24HourView(true);
        mTimePickerEnd = findViewById(R.id.timePickerEnd);
        mTimePickerEnd.setIs24HourView(true);

        // load calendars
        mCustomerCalendars = mDb.getCalendars(false);
        ArrayAdapter<CustomerCalendar> a = new ArrayAdapter<>(this, R.layout.item_list_simple, mCustomerCalendars);
        mSpinnerCalendar.setAdapter(a);

        // get extra from parent intent
        Intent intent = getIntent();
        mCurrentAppointmentId = intent.getLongExtra("appointment-id", -1);
        mCurrentAppointment = mDb.getAppointmentById(mCurrentAppointmentId);
        if(mCurrentAppointment != null) {
            fillFields(mCurrentAppointment);
            getSupportActionBar().setTitle(getResources().getString(R.string.edit_appointment));
        } else {
            mCurrentAppointment = new CustomerAppointment();
            Object preselectDate = intent.getSerializableExtra("appointment-day");
            if(preselectDate instanceof Calendar) mCalendar = (Calendar) preselectDate;

            // apply default length
            int defaultLength = mSettings.getInt("appointment-length", 30);
            Calendar tempCalendar = Calendar.getInstance();
            tempCalendar.add(Calendar.MINUTE, defaultLength);
            if(Build.VERSION.SDK_INT < 23) {
                mTimePickerEnd.setCurrentHour(tempCalendar.get(Calendar.HOUR_OF_DAY));
                mTimePickerEnd.setCurrentMinute(tempCalendar.get(Calendar.MINUTE));
            } else {
                mTimePickerEnd.setHour(tempCalendar.get(Calendar.HOUR_OF_DAY));
                mTimePickerEnd.setMinute(tempCalendar.get(Calendar.MINUTE));
            }

            getSupportActionBar().setTitle(getResources().getString(R.string.new_appointment));
        }
        refreshDisplayDate();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_calendar_appointment_edit, menu);
        menu.findItem(R.id.action_remove).setVisible(mCurrentAppointmentId!=-1);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.action_remove:
                removeAndExit();
                return true;
            case R.id.action_edit_done:
                saveAndExit();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void fillFields(CustomerAppointment a) {
        for(CustomerCalendar c : mCustomerCalendars) {
            if(c.mId == a.mCalendarId) {
                mSpinnerCalendar.setSelection(mCustomerCalendars.indexOf(c));
            }
        }
        mEditTextTitle.setText(a.mTitle);
        mEditTextNotes.setText(a.mNotes);
        mEditTextCustomer.setText(a.mCustomer);

        mCalendar.setTime(a.mTimeEnd);
        if(Build.VERSION.SDK_INT < 23) {
            mTimePickerEnd.setCurrentHour(mCalendar.get(Calendar.HOUR_OF_DAY));
            mTimePickerEnd.setCurrentMinute(mCalendar.get(Calendar.MINUTE));
        } else {
            mTimePickerEnd.setHour(mCalendar.get(Calendar.HOUR_OF_DAY));
            mTimePickerEnd.setMinute(mCalendar.get(Calendar.MINUTE));
        }

        mCalendar.setTime(a.mTimeStart);
        if(Build.VERSION.SDK_INT < 23) {
            mTimePickerStart.setCurrentHour(mCalendar.get(Calendar.HOUR_OF_DAY));
            mTimePickerStart.setCurrentMinute(mCalendar.get(Calendar.MINUTE));
        } else {
            mTimePickerStart.setHour(mCalendar.get(Calendar.HOUR_OF_DAY));
            mTimePickerStart.setMinute(mCalendar.get(Calendar.MINUTE));
        }
    }

    private boolean updateAppointmentObjectByInputs() {
        CustomerCalendar calendar = ((CustomerCalendar) ((Spinner) mSpinnerCalendar).getSelectedItem());
        if(calendar == null) {
            CommonDialog.show(this, getString(R.string.error), getString(R.string.no_calendar_selected), CommonDialog.TYPE.WARN, false);
            return false;
        }
        mCurrentAppointment.mCalendarId = calendar.mId;
        mCurrentAppointment.mTitle = mEditTextTitle.getText().toString();
        mCurrentAppointment.mNotes = mEditTextNotes.getText().toString();
        mCurrentAppointment.mCustomer = mEditTextCustomer.getText().toString();

        String dateString = CustomerDatabase.storageFormat.format(mCalendar.getTime());
        String dateStringStart;
        String dateStringEnd;
        if(Build.VERSION.SDK_INT < 23) {
            dateStringStart = dateString + " " + mTimePickerStart.getCurrentHour() + ":" + mTimePickerStart.getCurrentMinute() + ":00";
            dateStringEnd = dateString + " " + mTimePickerEnd.getCurrentHour() + ":" + mTimePickerEnd.getCurrentMinute() + ":00";
        } else {
            dateStringStart = dateString + " " + mTimePickerStart.getHour() + ":" + mTimePickerStart.getMinute() + ":00";
            dateStringEnd = dateString + " " + mTimePickerEnd.getHour() + ":" + mTimePickerEnd.getMinute() + ":00";
        }
        try {
            mCurrentAppointment.mTimeStart = CustomerDatabase.storageFormatWithTime.parse(dateStringStart);
            mCurrentAppointment.mTimeEnd = CustomerDatabase.storageFormatWithTime.parse(dateStringEnd);
        } catch(ParseException ignored) {
            return false;
        }

        if(mCurrentAppointment.mTimeStart.getTime() > mCurrentAppointment.mTimeEnd.getTime()) {
            CommonDialog.show(this, getString(R.string.error), getString(R.string.end_date_before_start_date), CommonDialog.TYPE.WARN, false);
            return false;
        }
        if(mCurrentAppointment.mTimeEnd.getTime() - mCurrentAppointment.mTimeStart.getTime() < 1000*60*5) {
            CommonDialog.show(this, getString(R.string.error), getString(R.string.appointment_too_short), CommonDialog.TYPE.WARN, false);
            return false;
        }

        // save default length
        SharedPreferences.Editor editor = mSettings.edit();
        editor.putInt("appointment-length", (int)((mCurrentAppointment.mTimeEnd.getTime() - mCurrentAppointment.mTimeStart.getTime()) / 1000 / 60));
        editor.apply();

        return true;
    }

    private void saveAndExit() {
        if(updateAppointmentObjectByInputs()) {
            if(mCurrentAppointmentId == -1) {
                // insert new
                mDb.addAppointment(mCurrentAppointment);
            } else {
                // update in database
                mCurrentAppointment.mLastModified = new Date();
                mDb.updateAppointment(mCurrentAppointment);
            }
            MainActivity.setUnsyncedChanges(this);
            setResult(RESULT_OK);
            finish();
        }
    }

    private void removeAndExit() {
        mDb.removeAppointment(mCurrentAppointment);
        MainActivity.setUnsyncedChanges(this);
        setResult(RESULT_OK);
        finish();
    }

    public void onClickChangeDay(View v) {
        final DatePickerDialog.OnDateSetListener date = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                mCalendar.set(Calendar.YEAR, year);
                mCalendar.set(Calendar.MONTH, monthOfYear);
                mCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                refreshDisplayDate();
            }
        };
        new DatePickerDialog(
                this, date,
                mCalendar.get(Calendar.YEAR),
                mCalendar.get(Calendar.MONTH),
                mCalendar.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    private void refreshDisplayDate() {
        mButtonDay.setText(DateControl.birthdayDateFormat.format(mCalendar.getTime()));
    }

}
