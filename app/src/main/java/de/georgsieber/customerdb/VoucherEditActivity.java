package de.georgsieber.customerdb;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import de.georgsieber.customerdb.model.Customer;
import de.georgsieber.customerdb.model.Voucher;
import de.georgsieber.customerdb.tools.ColorControl;
import de.georgsieber.customerdb.tools.CommonDialog;
import de.georgsieber.customerdb.tools.NumTools;

public class VoucherEditActivity extends AppCompatActivity {

    private VoucherEditActivity me;

    private long mCurrentVoucherId = -1;
    private Voucher mCurrentVoucher;
    private SharedPreferences mSettings;
    private CustomerDatabase mDb;

    private DateFormat mDateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault());
    private Calendar mValidUntilCalendar = Calendar.getInstance();

    ImageButton mButtonShowFromCustomer;
    ImageButton mButtonShowForCustomer;
    EditText mEditTextValue;
    EditText mEditTextVoucherNo;
    Button mButtonValidUntil;
    EditText mEditTextFromCustomer;
    EditText mEditTextForCustomer;
    EditText mEditTextNotes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // init settings
        mSettings = getSharedPreferences(MainActivity.PREFS_NAME, 0);

        // init database
        mDb = new CustomerDatabase(this);

        // init activity view
        super.onCreate(savedInstanceState);
        me = this;
        setContentView(R.layout.activity_voucher_edit);

        // init toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if(getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // init colors
        ColorControl.updateActionBarColor(this, mSettings);

        // set currency label
        ((TextView) findViewById(R.id.textViewCurrency)).setText(mSettings.getString("currency", "€"));

        // find views
        mButtonShowFromCustomer = findViewById(R.id.buttonShowFromCustomer);
        mButtonShowFromCustomer.setEnabled(false);
        mButtonShowForCustomer = findViewById(R.id.buttonShowForCustomer);
        mButtonShowForCustomer.setEnabled(false);
        mEditTextValue = findViewById(R.id.editTextValue);
        mEditTextVoucherNo = findViewById(R.id.editTextVoucherNo);
        mButtonValidUntil = findViewById(R.id.buttonValidUntil);
        mEditTextFromCustomer = findViewById(R.id.editTextFromCustomer);
        mEditTextForCustomer = findViewById(R.id.editTextForCustomer);
        mEditTextNotes = findViewById(R.id.editTextNotes);

        // get extra from parent intent
        Intent intent = getIntent();
        mCurrentVoucherId = intent.getLongExtra("voucher-id", -1);
        mCurrentVoucher = mDb.getVoucherById(mCurrentVoucherId);
        if(mCurrentVoucher != null) {
            fillFields(mCurrentVoucher);
            getSupportActionBar().setTitle(getResources().getString(R.string.edit_voucher));
        } else {
            mCurrentVoucher = new Voucher();
            getSupportActionBar().setTitle(getResources().getString(R.string.new_voucher));

            // show sync hint
            if(mSettings.getInt("webapi-type", 0) > 0) {
                findViewById(R.id.linearLayoutSyncInfo).setVisibility(View.VISIBLE);
            }
        }
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_voucher_edit, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.action_edit_done:
                saveAndExit();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @SuppressLint("SetTextI18n")
    public void insertVoucherNumber(View v) {
        int newVoucherNo = mSettings.getInt("voucher-no", 0)+1;
        while(existsVoucherNo(Integer.toString(newVoucherNo))) {
            newVoucherNo ++;
        }
        mEditTextVoucherNo.setText(Integer.toString(newVoucherNo));
    }

    private boolean existsVoucherNo(String voucherNo) {
        CustomerDatabase db = new CustomerDatabase(this);
        for(Voucher v : db.getVouchers(null, true)) {
            if(v.mVoucherNo.equals(voucherNo)) {
                return true;
            }
        }
        return false;
    }

    private void saveAndExit() {
        if(updateVoucherObjectByInputs()) {
            Integer currentVoucherNo = NumTools.tryParseInt(mEditTextVoucherNo.getText().toString());
            if(currentVoucherNo != null) {
                SharedPreferences.Editor editor = mSettings.edit();
                editor.putInt("voucher-no", currentVoucherNo);
                editor.apply();
            }
            if(mCurrentVoucher.mId == -1) {
                // insert new voucher
                mDb.addVoucher(mCurrentVoucher);
            } else {
                // update in database
                mCurrentVoucher.mLastModified = new Date();
                mDb.updateVoucher(mCurrentVoucher);
            }

            MainActivity.setUnsyncedChanges(this);

            setResult(RESULT_OK);
            finish();
        } else {
            CommonDialog.show(this, getResources().getString(R.string.invalid_number), getResources().getString(R.string.invalid_number_text), CommonDialog.TYPE.FAIL, false);
        }
    }

    private void fillFields(Voucher v) {
        mEditTextValue.setText(v.getCurrentValueString());
        mEditTextVoucherNo.setText(v.mVoucherNo);
        mEditTextVoucherNo.setEnabled(false);
        findViewById(R.id.buttonInsertVoucherNumber).setVisibility(View.GONE);
        mEditTextNotes.setText(v.mNotes);
        if(v.mFromCustomerId != null) {
            Customer relatedCustomer = mDb.getCustomerById(v.mFromCustomerId, false, false);
            if(relatedCustomer != null) {
                mEditTextFromCustomer.setText(relatedCustomer.getFullName(false));
                mButtonShowFromCustomer.setEnabled(true);
            }
        } else {
            mEditTextFromCustomer.setText(v.mFromCustomer);
        }
        if(v.mForCustomerId != null) {
            Customer relatedCustomer = mDb.getCustomerById(v.mForCustomerId, false, false);
            if(relatedCustomer != null) {
                mEditTextForCustomer.setText(relatedCustomer.getFullName(false));
                mButtonShowForCustomer.setEnabled(true);
            }
        } else {
            mEditTextForCustomer.setText(v.mFromCustomer);
        }
        if(v.mValidUntil == null) {
            mValidUntilCalendar = null;
        } else {
            mValidUntilCalendar = Calendar.getInstance();
        }
        if(mValidUntilCalendar == null) {
            mButtonValidUntil.setText(getString(R.string.no_date_set));
        } else {
            mValidUntilCalendar.setTime(v.mValidUntil);
            mButtonValidUntil.setText(mDateFormat.format(mValidUntilCalendar.getTime()));
        }
    }

    private boolean updateVoucherObjectByInputs() {
        String newValueString = mEditTextValue.getText().toString();
        Double newValueDouble = NumTools.tryParseDouble(newValueString);
        if(newValueDouble == null) return false;

        mCurrentVoucher.mCurrentValue = newValueDouble;
        if(mCurrentVoucher.mId == -1) {
            mCurrentVoucher.mOriginalValue = mCurrentVoucher.mCurrentValue;
        }
        mCurrentVoucher.mVoucherNo = mEditTextVoucherNo.getText().toString();
        mCurrentVoucher.mNotes = mEditTextNotes.getText().toString();
        mCurrentVoucher.mValidUntil = mValidUntilCalendar == null ? null : mValidUntilCalendar.getTime();
        mCurrentVoucher.mLastModified = new Date();
        return true;
    }

    public void setValidUntil(View v) {
        if(mValidUntilCalendar == null) {
            mValidUntilCalendar = Calendar.getInstance();
        }
        final DatePickerDialog.OnDateSetListener date = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                mValidUntilCalendar.set(Calendar.YEAR, year);
                mValidUntilCalendar.set(Calendar.MONTH, monthOfYear);
                mValidUntilCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                mButtonValidUntil.setText(mDateFormat.format(mValidUntilCalendar.getTime()));
            }
        };
        new DatePickerDialog(
                VoucherEditActivity.this, date,
                mValidUntilCalendar.get(Calendar.YEAR),
                mValidUntilCalendar.get(Calendar.MONTH),
                mValidUntilCalendar.get(Calendar.DAY_OF_MONTH)
        ).show();
    }
    public void removeValidUntil(View v) {
        mValidUntilCalendar = null;
        mButtonValidUntil.setText(getString(R.string.no_date_set));
    }

    public void onClickShowFromCustomer(View v) {
        if(mCurrentVoucher.mFromCustomerId != null) {
            showCustomerDetails(mCurrentVoucher.mFromCustomerId);
        }
    }
    public void onClickAddFromCustomer(View v) {
        chooseCustomerDialog(true);
    }
    public void onClickRemoveFromCustomer(View v) {
        mCurrentVoucher.mFromCustomer = "";
        mCurrentVoucher.mFromCustomerId = null;
        mEditTextFromCustomer.setText("");
        mButtonShowFromCustomer.setEnabled(false);
    }

    public void onClickShowForCustomer(View v) {
        if(mCurrentVoucher.mForCustomerId != null) {
            showCustomerDetails(mCurrentVoucher.mForCustomerId);
        }
    }
    public void onClickAddForCustomer(View v) {
        chooseCustomerDialog(false);
    }
    public void onClickRemoveForCustomer(View v) {
        mCurrentVoucher.mForCustomer = "";
        mCurrentVoucher.mForCustomerId = null;
        mEditTextForCustomer.setText("");
        mButtonShowForCustomer.setEnabled(false);
    }

    private void showCustomerDetails(long customerId) {
        Intent myIntent = new Intent(me, CustomerDetailsActivity.class);
        myIntent.putExtra("customer-id", customerId);
        me.startActivity(myIntent);
    }
    private void chooseCustomerDialog(final boolean setFromCustomer) {
        final Dialog ad = new Dialog(me);
        ad.requestWindowFeature(Window.FEATURE_NO_TITLE);
        ad.setContentView(R.layout.dialog_list);

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        if(ad.getWindow() != null) lp.copyFrom(ad.getWindow().getAttributes());
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.MATCH_PARENT;

        final List<Customer> customers = mDb.getCustomers(null, false, false);
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
                Customer newCustomer = (Customer) listView.getAdapter().getItem(listView.getCheckedItemPosition());
                if(setFromCustomer) {
                    mButtonShowFromCustomer.setEnabled(true);
                    mCurrentVoucher.mFromCustomerId = newCustomer.mId;
                    mCurrentVoucher.mFromCustomer = "";
                    mEditTextFromCustomer.setText(newCustomer.getFullName(false));
                } else {
                    mButtonShowForCustomer.setEnabled(true);
                    mCurrentVoucher.mForCustomerId = newCustomer.mId;
                    mCurrentVoucher.mForCustomer = "";
                    mEditTextForCustomer.setText(newCustomer.getFullName(false));
                }
            }
        });

        ad.show();
        ad.getWindow().setAttributes(lp);
    }

}
