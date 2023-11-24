package de.georgsieber.customerdb;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TimePicker;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.File;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import de.georgsieber.customerdb.importexport.CalendarCsvBuilder;
import de.georgsieber.customerdb.importexport.CalendarIcsBuilder;
import de.georgsieber.customerdb.model.Customer;
import de.georgsieber.customerdb.model.CustomerAppointment;
import de.georgsieber.customerdb.model.CustomerCalendar;
import de.georgsieber.customerdb.tools.ColorControl;
import de.georgsieber.customerdb.tools.CommonDialog;
import de.georgsieber.customerdb.tools.DateControl;
import de.georgsieber.customerdb.tools.StorageControl;

public class CalendarAppointmentEditActivity extends AppCompatActivity {

    private CalendarAppointmentEditActivity me;

    private long mCurrentAppointmentId = -1;
    private CustomerAppointment mCurrentAppointment;
    private SharedPreferences mSettings;
    private CustomerDatabase mDb;
    private Calendar mCalendar = Calendar.getInstance();
    private List<CustomerCalendar> mCustomerCalendars;

    ImageButton mButtonShowCustomer;
    Spinner mSpinnerCalendar;
    EditText mEditTextTitle;
    EditText mEditTextCustomer;
    EditText mEditTextNotes;
    EditText mEditTextLocation;
    Button mButtonDay;
    TimePicker mTimePickerStart;
    TimePicker mTimePickerEnd;
    ImageView mImageViewQrCode;

    private ActivityResultLauncher<Intent> mResultHandlerExportMoveFile;
    private File mCurrentExportFile;

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
        mButtonShowCustomer = findViewById(R.id.buttonShowCustomer);
        mButtonShowCustomer.setEnabled(false);
        mImageViewQrCode = findViewById(R.id.imageViewQrCode);
        mSpinnerCalendar = findViewById(R.id.spinnerCalendar);
        mEditTextTitle = findViewById(R.id.editTextTitle);
        mEditTextNotes = findViewById(R.id.editTextNotes);
        mEditTextCustomer = findViewById(R.id.editTextCustomer);
        mEditTextLocation = findViewById(R.id.editTextLocation);
        mButtonDay = findViewById(R.id.buttonDay);
        mTimePickerStart = findViewById(R.id.timePickerStart);
        mTimePickerStart.setIs24HourView(true);
        mTimePickerEnd = findViewById(R.id.timePickerEnd);
        mTimePickerEnd.setIs24HourView(true);

        // init activity result handler
        mResultHandlerExportMoveFile = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if(result.getResultCode() == RESULT_OK) {
                            Uri uri = result.getData().getData();
                            if(uri != null) {
                                try {
                                    StorageControl.moveFile(mCurrentExportFile, uri, me);
                                } catch(Exception e) {
                                    CommonDialog.show(me, getString(R.string.error), e.getMessage(), CommonDialog.TYPE.FAIL, false);
                                }
                            }
                        }
                    }
                }
        );

        // register events
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                refreshQrCode();
            }
        }, 1000, 1000);

        // load calendars
        mCustomerCalendars = mDb.getCalendars(false);
        ArrayAdapter<CustomerCalendar> a = new ArrayAdapter<>(this, R.layout.item_list_simple, mCustomerCalendars);
        mSpinnerCalendar.setAdapter(a);

        // load default values
        mEditTextTitle.setText(mSettings.getString("default-appointment-title", ""));
        mEditTextLocation.setText(mSettings.getString("default-appointment-location", ""));

        // get extra from parent intent
        Intent intent = getIntent();
        mCurrentAppointmentId = intent.getLongExtra("appointment-id", -1);
        mCurrentAppointment = mDb.getAppointmentById(mCurrentAppointmentId, false);
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
        if(mCurrentAppointmentId == -1) { // hide id and remove button if we are about to create a new appointment
            menu.findItem(R.id.action_id).setVisible(false);
            menu.findItem(R.id.action_remove).setVisible(false);
        } else {
            menu.findItem(R.id.action_id).setVisible(true);
            menu.findItem(R.id.action_remove).setVisible(true);
        }
        if(mCurrentAppointment != null && mCurrentAppointment.mId != -1) // show id in menu
            menu.findItem(R.id.action_id).setTitle( "ID: " + mCurrentAppointment.mId );
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
            case R.id.action_export:
                export();
                return true;
            case R.id.action_edit_done:
                saveAndExit();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private String mLastQrContent = "";
    @SuppressWarnings("SuspiciousNameCombination")
    private void refreshQrCode() {
        int WIDTH = 1200;
        if(!updateAppointmentObjectByInputs()) return;
        String content = "BEGIN:VEVENT" + "\n"
                + "SUMMARY:" + mEditTextTitle.getText().toString() + "\n"
                + "DESCRIPTION:" + mEditTextNotes.getText().toString().replace("\n", "\\n") + "\n"
                + "LOCATION:" + mEditTextLocation.getText().toString() + "\n"
                + "DTSTART:" + CalendarIcsBuilder.dateFormatIcs.format(mCurrentAppointment.mTimeStart) + "\n"
                + "DTEND:" + CalendarIcsBuilder.dateFormatIcs.format(mCurrentAppointment.mTimeEnd) + "\n"
                + "END:VEVENT" + "\n";
        if(mLastQrContent.equals(content)) return;
        mLastQrContent = content;
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, WIDTH, WIDTH);
            int w = bitMatrix.getWidth();
            int h = bitMatrix.getHeight();
            int[] pixels = new int[w * h];
            for (int y = 0; y < h; y++) {
                int offset = y * w;
                for (int x = 0; x < w; x++) {
                    pixels[offset + x] = bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE;
                }
            }
            final Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            bitmap.setPixels(pixels, 0, WIDTH, 0, 0, w, h);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mImageViewQrCode.setImageDrawable(new BitmapDrawable(bitmap));
                }
            });

        } catch (WriterException ignored) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mImageViewQrCode.setImageDrawable(new ColorDrawable(Color.WHITE));
                }
            });
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
        mEditTextLocation.setText(a.mLocation);

        if(a.mCustomerId != null) {
            Customer relatedCustomer = mDb.getCustomerById(a.mCustomerId, false, false);
            if(relatedCustomer != null) {
                mEditTextCustomer.setText(relatedCustomer.getFullName(false));
                mButtonShowCustomer.setEnabled(true);
            } else {
                mEditTextCustomer.setText(getString(R.string.removed_placeholder));
            }
        } else {
            mEditTextCustomer.setText(a.mCustomer);
        }

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

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean updateAppointmentObjectByInputs() {
        CustomerCalendar calendar = (CustomerCalendar) (mSpinnerCalendar).getSelectedItem();
        if(calendar == null) return false;

        mCurrentAppointment.mCalendarId = calendar.mId;
        mCurrentAppointment.mTitle = mEditTextTitle.getText().toString();
        mCurrentAppointment.mNotes = mEditTextNotes.getText().toString();
        mCurrentAppointment.mLocation = mEditTextLocation.getText().toString();

        String dateString = CustomerDatabase.storageFormatWithoutTime.format(mCalendar.getTime());
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
            mCurrentAppointment.mTimeStart = CustomerDatabase.parseDateRaw(dateStringStart);
            mCurrentAppointment.mTimeEnd = CustomerDatabase.parseDateRaw(dateStringEnd);
        } catch (ParseException e) {
            return false;
        }

        // save default length
        SharedPreferences.Editor editor = mSettings.edit();
        editor.putInt("appointment-length", (int)((mCurrentAppointment.mTimeEnd.getTime() - mCurrentAppointment.mTimeStart.getTime()) / 1000 / 60));
        editor.apply();

        return true;
    }
    private boolean updateAndCheckAppointment() {
        CustomerCalendar calendar = (CustomerCalendar) (mSpinnerCalendar).getSelectedItem();
        if(calendar == null) {
            CommonDialog.show(this, getString(R.string.error), getString(R.string.no_calendar_selected), CommonDialog.TYPE.WARN, false);
            return false;
        }

        if(!updateAppointmentObjectByInputs()) return false;

        if(mCurrentAppointment.mTimeStart.getTime() > mCurrentAppointment.mTimeEnd.getTime()) {
            CommonDialog.show(this, getString(R.string.error), getString(R.string.end_date_before_start_date), CommonDialog.TYPE.WARN, false);
            return false;
        }
        if(mCurrentAppointment.mTimeEnd.getTime() - mCurrentAppointment.mTimeStart.getTime() < 1000*60*5) {
            CommonDialog.show(this, getString(R.string.error), getString(R.string.appointment_too_short), CommonDialog.TYPE.WARN, false);
            return false;
        }

        return true;
    }

    private void saveAndExit() {
        if(updateAndCheckAppointment()) {
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

    private void export() {
        if(!updateAppointmentObjectByInputs()) return;
        final Dialog ad = new Dialog(this);
        ad.requestWindowFeature(Window.FEATURE_NO_TITLE);
        ad.setContentView(R.layout.dialog_export_single_appointment);
        ad.findViewById(R.id.buttonExportSingleCSV).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ad.dismiss();
                if(new CalendarCsvBuilder(mCurrentAppointment).saveCsvFile(getStorageExportCSV(), mDb)) {
                    mCurrentExportFile = getStorageExportCSV();
                    CommonDialog.exportFinishedDialog(me, getStorageExportCSV(), "text/csv",
                            new String[]{}, "", "", mResultHandlerExportMoveFile
                    );
                } else {
                    CommonDialog.show(me, getResources().getString(R.string.export_fail), getStorageExportCSV().getPath(), CommonDialog.TYPE.FAIL, false);
                }
                StorageControl.scanFile(getStorageExportCSV(), me);
            }
        });
        ad.findViewById(R.id.buttonExportSingleICS).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ad.dismiss();
                if(new CalendarIcsBuilder(mCurrentAppointment).saveIcsFile(getStorageExportICS())) {
                    mCurrentExportFile = getStorageExportICS();
                    CommonDialog.exportFinishedDialog(me, getStorageExportICS(), "text/calendar",
                            new String[]{}, "", "", mResultHandlerExportMoveFile
                    );
                } else {
                    CommonDialog.show(me, getResources().getString(R.string.export_fail), getStorageExportICS().getPath(), CommonDialog.TYPE.FAIL, false);
                }
                StorageControl.scanFile(getStorageExportICS(), me);
            }
        });
        ad.findViewById(R.id.buttonExportSingleCancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ad.dismiss();
            }
        });
        ad.show();
    }

    private File getStorageExportCSV() {
        File exportDir = new File(getExternalFilesDir(null), "export");
        exportDir.mkdirs();
        return new File(exportDir, "export."+ mCurrentAppointment.mId +".csv");
    }
    private File getStorageExportICS() {
        File exportDir = new File(getExternalFilesDir(null), "export");
        exportDir.mkdirs();
        return new File(exportDir, "export."+ mCurrentAppointment.mId +".ics");
    }

    public void onClickShowCustomer(View v) {
        if(mCurrentAppointment.mCustomerId != null) {
            showCustomerDetails(mCurrentAppointment.mCustomerId);
        }
    }
    public void onClickAddCustomer(View v) {
        chooseCustomerDialog();
    }
    public void onClickRemoveCustomer(View v) {
        mCurrentAppointment.mCustomer = "";
        mCurrentAppointment.mCustomerId = null;
        mEditTextCustomer.setText("");
        mButtonShowCustomer.setEnabled(false);
    }

    private void showCustomerDetails(long customerId) {
        Intent myIntent = new Intent(me, CustomerDetailsActivity.class);
        myIntent.putExtra("customer-id", customerId);
        me.startActivity(myIntent);
    }
    private void chooseCustomerDialog() {
        final Dialog ad = new Dialog(me);
        ad.requestWindowFeature(Window.FEATURE_NO_TITLE);
        ad.setContentView(R.layout.dialog_list);

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        if(ad.getWindow() != null) lp.copyFrom(ad.getWindow().getAttributes());
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.MATCH_PARENT;

        final List<Customer> customers = mDb.getCustomers(null, false, false, null);
        final Button buttonOK = ad.findViewById(R.id.buttonOK);
        final ListView listView = ad.findViewById(R.id.listViewDialogList);
        listView.setAdapter(new CustomerAdapter(me, customers, null));
        final EditText textBoxSearch = ad.findViewById(R.id.editTextDialogListSearch);

        textBoxSearch.addTextChangedListener(new TextWatcher() { // future search implementation
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }
            @Override
            public void afterTextChanged(Editable s) { }
        });
        buttonOK.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ad.dismiss();
                if(listView.getCheckedItemPosition() < 0) return;
                Customer newCustomer = (Customer) listView.getAdapter().getItem(listView.getCheckedItemPosition());
                mButtonShowCustomer.setEnabled(true);
                mCurrentAppointment.mCustomerId = newCustomer.mId;
                mCurrentAppointment.mCustomer = "";
                mEditTextCustomer.setText(newCustomer.getFullName(false));
            }
        });

        ad.show();
        ad.getWindow().setAttributes(lp);
    }

}
