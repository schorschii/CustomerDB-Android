package de.georgsieber.customerdb;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;

import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.text.InputType;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Space;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import de.georgsieber.customerdb.model.CustomField;
import de.georgsieber.customerdb.model.Customer;
import de.georgsieber.customerdb.model.CustomerFile;
import de.georgsieber.customerdb.tools.ColorControl;
import de.georgsieber.customerdb.tools.CommonDialog;
import de.georgsieber.customerdb.tools.DateControl;
import de.georgsieber.customerdb.tools.NumTools;
import de.georgsieber.customerdb.tools.StorageControl;


@SuppressWarnings("TryFinallyCanBeTryWithResources")
public class CustomerEditActivity extends AppCompatActivity {

    private CustomerEditActivity me;

    private CustomerDatabase mDb;
    private FeatureCheck mFc;

    private long mCurrentCustomerId = -1;
    private Customer mCurrentCustomer;
    private List<CustomField> mCustomFields;
    private boolean mIsInoutOnlyModeActive = false;

    private final static int CUSTOMER_IMAGE_PICK_REQUEST = 1;
    private final static int CUSTOMER_IMAGE_CAMERA_REQUEST = 2;
    private final static int ABOUT_REQUEST = 3;
    private final static int FILE_CAMERA_REQUEST = 4;
    private final static int FILE_PICK_REQUEST = 5;
    private final static int FILE_DRAW_REQUEST = 6;

    Calendar mBirthdayCalendar = null;

    EditText mEditTextTitle;
    EditText mEditTextFirstName;
    EditText mEditTextLastName;
    EditText mEditTextPhoneHome;
    EditText mEditTextPhoneMobile;
    EditText mEditTextPhoneWork;
    EditText mEditTextEmail;
    EditText mEditTextStreet;
    EditText mEditTextZipcode;
    EditText mEditTextCity;
    EditText mEditTextCountry;
    EditText mEditTextGroup;
    EditText mEditTextNotes;
    CheckBox mCheckBoxNewsletter;
    CheckBox mCheckBoxConsent;
    Button mButtonBirthday;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // init settings
        SharedPreferences settings = getSharedPreferences(MainActivity.PREFS_NAME, 0);

        // init database
        mDb = new CustomerDatabase(this);

        // load in-app purchases
        mFc = new FeatureCheck(this);
        mFc.init();

        // init activity view
        super.onCreate(savedInstanceState);
        me = this;
        setContentView(R.layout.activity_customer_edit);

        // init toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if(getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // init colors
        ColorControl.updateActionBarColor(this, settings);

        // find views
        mEditTextTitle = findViewById(R.id.editTextTitle);
        mEditTextFirstName = findViewById(R.id.editTextFirstName);
        mEditTextLastName = findViewById(R.id.editTextLastName);
        mEditTextPhoneHome = findViewById(R.id.editTextPhoneHome);
        mEditTextPhoneMobile = findViewById(R.id.editTextPhoneMobile);
        mEditTextPhoneWork = findViewById(R.id.editTextPhoneWork);
        mEditTextEmail = findViewById(R.id.editTextEmail);
        mEditTextStreet = findViewById(R.id.editTextStreet);
        mEditTextZipcode = findViewById(R.id.editTextZipcode);
        mEditTextCity = findViewById(R.id.editTextCity);
        mEditTextCountry = findViewById(R.id.editTextCountry);
        mEditTextGroup = findViewById(R.id.editTextGroup);
        mEditTextNotes = findViewById(R.id.editTextNotes);
        mCheckBoxNewsletter = findViewById(R.id.checkBoxEditNewsletter);
        mCheckBoxConsent = findViewById(R.id.checkBoxEditConsent);
        mButtonBirthday = findViewById(R.id.buttonBirthday);

        // apply settings
        settings = getSharedPreferences(MainActivity.PREFS_NAME, 0);
        if(settings.getBoolean("phone-allow-text", false)) {
            mEditTextPhoneHome.setInputType(InputType.TYPE_CLASS_TEXT);
            mEditTextPhoneMobile.setInputType(InputType.TYPE_CLASS_TEXT);
            mEditTextPhoneWork.setInputType(InputType.TYPE_CLASS_TEXT);
        }
        if(!settings.getBoolean("show-phone-field", true)) {
            mEditTextPhoneHome.setVisibility(View.GONE);
            mEditTextPhoneMobile.setVisibility(View.GONE);
            mEditTextPhoneWork.setVisibility(View.GONE);
        }
        if(!settings.getBoolean("show-email-field", true)) {
            mEditTextEmail.setVisibility(View.GONE);
        }
        if(!settings.getBoolean("show-phone-field", true)
                && !settings.getBoolean("show-email-field", true)) {
            findViewById(R.id.linearLayoutContact).setVisibility(View.GONE);
        }
        if(!settings.getBoolean("show-address-field", true)) {
            findViewById(R.id.linearLayoutAddress).setVisibility(View.GONE);
        }
        if(!settings.getBoolean("show-group-field", true)) {
            findViewById(R.id.linearLayoutGroup).setVisibility(View.GONE);
        }
        if(!settings.getBoolean("show-notes-field", true)) {
            findViewById(R.id.linearLayoutNotes).setVisibility(View.GONE);
        }
        if(!settings.getBoolean("show-newsletter-field", true)) {
            findViewById(R.id.linearLayoutNewsletter).setVisibility(View.GONE);
        }
        if(!settings.getBoolean("show-birthday-field", true)) {
            findViewById(R.id.linearLayoutBirthday).setVisibility(View.GONE);
        }
        if(!settings.getBoolean("show-files", true)) {
            findViewById(R.id.linearLayoutFilesContainer).setVisibility(View.GONE);
        }

        // load default values
        mEditTextTitle.setText(settings.getString("default-customer-title", getResources().getString(R.string.titledefault)));
        mEditTextCity.setText(settings.getString("default-customer-city", ""));
        mEditTextCountry.setText(settings.getString("default-customer-country", ""));
        mEditTextGroup.setText(settings.getString("default-customer-group", ""));

        // show consent checkbox
        if(settings.getBoolean("iom", false) && settings.getBoolean("show-consent-field", false)) {
            mIsInoutOnlyModeActive = true;
            findViewById(R.id.linearLayoutConsent).setVisibility(View.VISIBLE);
        }

        // get extra from parent intent
        Intent intent = getIntent();
        mCurrentCustomerId = intent.getLongExtra("customer-id", -1);
        mCurrentCustomer = mDb.getCustomerById(mCurrentCustomerId, false, true);
        if(mCurrentCustomer != null) {
            fillFields(mCurrentCustomer);
            if(getSupportActionBar() != null)
                getSupportActionBar().setTitle(getResources().getString(R.string.edit_customer));
        } else {
            mCurrentCustomer = new Customer();
            if(getSupportActionBar() != null)
                getSupportActionBar().setTitle(getResources().getString(R.string.new_customer));
        }

        // init custom fields
        LinearLayout linearLayout = findViewById(R.id.linearLayoutCustomFieldsEdit);
        linearLayout.removeAllViews();

        mCustomFields = mDb.getCustomFields();
        if(mCustomFields.size() > 0) linearLayout.setVisibility(View.VISIBLE);
        for(CustomField cf : mCustomFields) {
            TextView descriptionView = new TextView(this);
            descriptionView.setText(cf.mTitle);
            descriptionView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,LinearLayout.LayoutParams.WRAP_CONTENT));

            String value = "";
            if(mCurrentCustomer != null) {
                value = mCurrentCustomer.getCustomField(cf.mTitle);
                if(value == null) value = "";
            }

            View valueView;

            if(cf.mType == 2) {

                valueView = new Spinner(this);
                cf.mEditViewReference = valueView;
                valueView.setBackgroundResource(R.drawable.background_spinner);
                reloadCustomFieldPresets((Spinner)valueView, cf.mId, value);

            } else {

                valueView = new EditText(this);
                cf.mEditViewReference = valueView;

                if(cf.mType == 1) {

                    ((EditText)valueView).setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);

                } else if(cf.mType == 3) {

                    // open DatePickerDialog on click
                    final EditText editTextValue = ((EditText)valueView);
                    editTextValue.setInputType(InputType.TYPE_NULL);
                    editTextValue.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                        @Override
                        public void onFocusChange(View view, boolean hasFocus) {
                            if(hasFocus) {
                                onCustomDateFieldClick(editTextValue);
                            }
                        }
                    });
                    editTextValue.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            onCustomDateFieldClick(editTextValue);
                        }
                    });

                    // try parse old value and show it in local format
                    try {
                        Date selectedDate = CustomerDatabase.storageFormatWithTime.parse(value);
                        value = DateControl.birthdayDateFormat.format(selectedDate);
                    } catch(Exception ignored) {}

                } else if(cf.mType == 4) {

                    ((EditText)valueView).setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);

                } else {

                    ((EditText)valueView).setInputType(InputType.TYPE_CLASS_TEXT);

                }

                ((EditText)valueView).setText(value);

            }

            View spaceView = new Space(this);
            spaceView.setLayoutParams(new LinearLayout.LayoutParams(0, NumTools.dpToPx(20, this)));
            linearLayout.addView(spaceView);

            valueView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            linearLayout.addView(descriptionView);
            linearLayout.addView(valueView);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_customer_edit, menu);
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode) {
            case(CUSTOMER_IMAGE_CAMERA_REQUEST) : {
                if(resultCode == RESULT_OK && data != null && data.getExtras() != null) {
                    Bitmap photo = (Bitmap) data.getExtras().get("data");
                    mCurrentCustomer.mImage = compressToJpeg(photo, 50);
                    refreshImage();
                }
                break;
            }
            case(CUSTOMER_IMAGE_PICK_REQUEST) : {
                if(resultCode == RESULT_OK && data != null && data.getData() != null) {
                    try {
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), data.getData());
                        mCurrentCustomer.mImage = compressToJpeg(resizeCompressToJpeg(bitmap, 400, 400), 50);
                    } catch (IOException e) {
                        CommonDialog.show(me, getString(R.string.error), e.getLocalizedMessage(), CommonDialog.TYPE.FAIL, false);
                    }
                    refreshImage();
                }
                break;
            }

            case(FILE_CAMERA_REQUEST) : {
                if(resultCode == RESULT_OK) {
                    try {
                        //Bitmap photo = (Bitmap) data.getExtras().get("data"); // thumbnail
                        Bitmap bitmap = BitmapFactory.decodeFile(StorageControl.getStorageImageTemp(this).getAbsolutePath());
                        byte[] jpegBytes = compressToJpeg(resizeCompressToJpeg(bitmap, 800, 600), 70);
                        mCurrentCustomer.addFile(new CustomerFile(StorageControl.getNewPictureFilename(this), jpegBytes), this);
                    } catch(Exception e) {
                        CommonDialog.show(me, getString(R.string.error), e.getLocalizedMessage(), CommonDialog.TYPE.FAIL, false);
                    }
                }
                refreshFiles();
                break;
            }
            case(FILE_PICK_REQUEST) : {
                if(resultCode == RESULT_OK && data != null && data.getData() != null) {
                    String filename = getFileName(data.getData());
                    ContentResolver cr = getContentResolver();
                    try {
                        if(cr.getType(data.getData()).startsWith("image/")) {
                            // try to compress the image
                            Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), data.getData());
                            byte[] jpegBytes = compressToJpeg(resizeCompressToJpeg(bitmap, 800, 600), 70);
                            mCurrentCustomer.addFile(new CustomerFile(filename, jpegBytes), this);
                        } else {
                            // save raw
                            InputStream is = getContentResolver().openInputStream(data.getData());
                            byte[] dataBytes = getByteArrayFromInputStream(is);
                            mCurrentCustomer.addFile(new CustomerFile(filename, dataBytes), this);
                        }
                    } catch(Exception e) {
                        CommonDialog.show(me, getString(R.string.error), e.getLocalizedMessage(), CommonDialog.TYPE.FAIL, false);
                    }
                }
                refreshFiles();
                break;
            }
            case(FILE_DRAW_REQUEST) : {
                if(resultCode == RESULT_OK && data != null && data.getExtras() != null) {
                    try {
                        mCurrentCustomer.addFile(new CustomerFile(StorageControl.getNewDrawingFilename(this), Base64.decode(data.getExtras().getString("image"), Base64.DEFAULT)), this);
                    } catch(Exception e) {
                        CommonDialog.show(me, getString(R.string.error), e.getLocalizedMessage(), CommonDialog.TYPE.FAIL, false);
                    }
                }
                refreshFiles();
                break;
            }

            case(ABOUT_REQUEST) : {
                mFc.init();
                break;
            }
        }
    }

    @SuppressWarnings("SameParameterValue")
    private static Bitmap resizeCompressToJpeg(Bitmap image, int maxWidth, int maxHeight) {
        if((image.getWidth() > maxWidth || image.getHeight() > maxHeight) && maxHeight > 0 && maxWidth > 0) {
            int width = image.getWidth();
            int height = image.getHeight();
            float ratioBitmap = (float) width / (float) height;
            float ratioMax = (float) maxWidth / (float) maxHeight;
            int finalWidth = maxWidth;
            int finalHeight = maxHeight;
            if(ratioMax > ratioBitmap) {
                finalWidth = (int) ((float)maxHeight * ratioBitmap);
            } else {
                finalHeight = (int) ((float)maxWidth / ratioBitmap);
            }
            image = Bitmap.createScaledBitmap(image, finalWidth, finalHeight, true);
        }
        return image;
    }
    private static byte[] compressToJpeg(Bitmap image, int compressRatio) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.JPEG, compressRatio, stream);
        byte[] byteArray = stream.toByteArray();
        image.recycle();
        return byteArray;
    }

    public String getFileName(Uri uri) {
        String result = null;
        if(uri.getScheme().equals("content")) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            try {
                if(cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } finally {
                cursor.close();
            }
        }
        if(result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if(cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    private byte[] getByteArrayFromInputStream(InputStream is) {
        try {
            ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int len = 0;
            while((len = is.read(buffer)) != -1) {
                byteBuffer.write(buffer, 0, len);
            }
            return byteBuffer.toByteArray();
        } catch (Exception e) {
            Log.e("FILE", "Error: " + e.toString());
        }
        return null;
    }

    void onCustomDateFieldClick(final EditText editTextValue) {
        hideKeyboardFrom(me, editTextValue);
        Calendar selected = Calendar.getInstance();
        try {
            selected.setTime( DateControl.birthdayDateFormat.parse(editTextValue.getText().toString()) );
        } catch(Exception ignored) {}
        final DatePickerDialog.OnDateSetListener listener = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                Calendar cal = Calendar.getInstance();
                cal.set(Calendar.YEAR, year);
                cal.set(Calendar.MONTH, monthOfYear);
                cal.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                editTextValue.setText(DateControl.birthdayDateFormat.format(cal.getTime()));
            }
        };
        new DatePickerDialog(
                CustomerEditActivity.this, listener,
                selected.get(Calendar.YEAR),
                selected.get(Calendar.MONTH),
                selected.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    static void hideKeyboardFrom(Context context, View view) {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Activity.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    private void dialogInApp(String title, String text) {
        AlertDialog.Builder ad = new AlertDialog.Builder(this);
        ad.setTitle(title);
        ad.setMessage(text);
        ad.setIcon(getResources().getDrawable(R.drawable.ic_warning_orange_24dp));
        ad.setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        ad.setNeutralButton(getResources().getString(R.string.more), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                startActivityForResult(new Intent(me, AboutActivity.class), ABOUT_REQUEST);
            }
        });
        ad.show();
    }

    private void saveAndExit() {
        if(mEditTextTitle.getText().toString().equals("") && mEditTextFirstName.getText().toString().equals("") && mEditTextLastName.getText().toString().equals("")) {
            CommonDialog.show(this, getString(R.string.name_empty), getString(R.string.please_fill_name), CommonDialog.TYPE.WARN, false);
            return;
        }
        if(mCheckBoxNewsletter.isChecked() && mEditTextEmail.getText().toString().equals("")) {
            CommonDialog.show(this, getString(R.string.email_empty), getString(R.string.please_fill_email), CommonDialog.TYPE.WARN, false);
            return;
        }
        if(mIsInoutOnlyModeActive && !mCheckBoxConsent.isChecked()) {
            CommonDialog.show(this, getString(R.string.data_processing_consent), getString(R.string.please_accept_data_processing), CommonDialog.TYPE.WARN, false);
            return;
        }

        if(mDb.getCustomers(null, false, false).size() >= 500 && !mFc.unlockedLargeCompany) {
            dialogInApp(getResources().getString(R.string.feature_locked), getResources().getString(R.string.feature_locked_large_company_text));
            return;
        }

        updateCustomerObjectByInputs();
        if(mCurrentCustomer.mId == -1) {
            // insert new customer
            mDb.addCustomer(mCurrentCustomer);
        } else {
            // update in database
            mCurrentCustomer.mLastModified = new Date();
            mDb.updateCustomer(mCurrentCustomer);
        }

        MainActivity.setUnsyncedChanges(this);

        setResult(RESULT_OK);
        finish();
    }

    private void reloadCustomFieldPresets(Spinner spinner, int fieldId, String defaultValue) {
        List<CustomField> options = mDb.getCustomFieldPresets(fieldId);
        ArrayAdapter<CustomField> a = new ArrayAdapter<CustomField>(this, R.layout.item_list_simple, options);
        spinner.setAdapter(a);
        for(CustomField f : options) {
            if(f.mTitle.equals(defaultValue)) {
                Log.e("EQUALS", defaultValue);
                spinner.setSelection(options.indexOf(f));
            }
        }
    }

    private void fillFields(Customer c) {
        mEditTextTitle.setText(c.mTitle);
        mEditTextFirstName.setText(c.mFirstName);
        mEditTextLastName.setText(c.mLastName);
        mEditTextPhoneHome.setText(c.mPhoneHome);
        mEditTextPhoneMobile.setText(c.mPhoneMobile);
        mEditTextPhoneWork.setText(c.mPhoneWork);
        mEditTextEmail.setText(c.mEmail);
        mEditTextStreet.setText(c.mStreet);
        mEditTextZipcode.setText(c.mZipcode);
        mEditTextCity.setText(c.mCity);
        mEditTextCountry.setText(c.mCountry);
        mEditTextNotes.setText(c.mNotes);
        mEditTextGroup.setText(c.mCustomerGroup);
        mCheckBoxNewsletter.setChecked(c.mNewsletter);
        if(c.mBirthday == null) {
            mBirthdayCalendar = null;
        } else {
            mBirthdayCalendar = Calendar.getInstance();
        }
        if(mBirthdayCalendar == null) {
            mButtonBirthday.setText(getString(R.string.no_date_set));
        } else {
            mBirthdayCalendar.setTime(c.mBirthday);
            mButtonBirthday.setText(DateControl.birthdayDateFormat.format(mBirthdayCalendar.getTime()));
        }
        refreshImage();
        refreshFiles();
    }

    private void refreshImage() {
        if(mCurrentCustomer.getImage().length != 0) {
            Bitmap bitmap = BitmapFactory.decodeByteArray(mCurrentCustomer.getImage(), 0, mCurrentCustomer.getImage().length);
            ((ImageView) findViewById(R.id.imageViewEditCustomerImage)).setImageBitmap(bitmap);
        } else {
            ((ImageView) findViewById(R.id.imageViewEditCustomerImage)).setImageDrawable(getResources().getDrawable(R.drawable.ic_person_black_96dp));
        }
    }

    private void refreshFiles() {
        LinearLayout linearLayoutFilesView = findViewById(R.id.linearLayoutFilesView);
        linearLayoutFilesView.removeAllViews();
        int counter = 0;
        for(final CustomerFile file : mCurrentCustomer.getFiles()) {
            final int count = counter;

            @SuppressLint("InflateParams") LinearLayout linearLayoutFile = (LinearLayout) LayoutInflater.from(this).inflate(R.layout.item_file_edit, null);
            Button buttonFilename = linearLayoutFile.findViewById(R.id.buttonFile);
            buttonFilename.setText(file.mName);
            buttonFilename.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final Dialog ad = new Dialog(me);
                    ad.requestWindowFeature(Window.FEATURE_NO_TITLE);
                    ad.setContentView(R.layout.dialog_input_box);
                    ((TextView) ad.findViewById(R.id.textViewInputBox)).setText(getResources().getString(R.string.enter_new_name));
                    ((TextView) ad.findViewById(R.id.editTextInputBox)).setText(file.mName);
                    if(ad.getWindow() != null) ad.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                    ad.findViewById(R.id.buttonInputBoxOK).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            ad.dismiss();
                            String newName = ((TextView) ad.findViewById(R.id.editTextInputBox)).getText().toString();
                            mCurrentCustomer.renameFile(count, newName);
                            refreshFiles();
                        }
                    });
                    ad.show();
                }
            });

            Button buttonFileRemove = linearLayoutFile.findViewById(R.id.buttonFileRemove);
            buttonFileRemove.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mCurrentCustomer.removeFile(count);
                    refreshFiles();
                }
            });

            linearLayoutFilesView.addView(linearLayoutFile);
            counter ++;
        }
    }

    private void updateCustomerObjectByInputs() {
        mCurrentCustomer.mTitle = mEditTextTitle.getText().toString();
        mCurrentCustomer.mFirstName = mEditTextFirstName.getText().toString();
        mCurrentCustomer.mLastName = mEditTextLastName.getText().toString();
        mCurrentCustomer.mPhoneHome = mEditTextPhoneHome.getText().toString();
        mCurrentCustomer.mPhoneMobile = mEditTextPhoneMobile.getText().toString();
        mCurrentCustomer.mPhoneWork = mEditTextPhoneWork.getText().toString();
        mCurrentCustomer.mEmail = mEditTextEmail.getText().toString().trim();
        mCurrentCustomer.mStreet = mEditTextStreet.getText().toString();
        mCurrentCustomer.mZipcode = mEditTextZipcode.getText().toString();
        mCurrentCustomer.mCity = mEditTextCity.getText().toString();
        mCurrentCustomer.mCountry = mEditTextCountry.getText().toString();
        mCurrentCustomer.mBirthday = mBirthdayCalendar == null ? null : mBirthdayCalendar.getTime();
        mCurrentCustomer.mLastModified = new Date();
        mCurrentCustomer.mNotes = mEditTextNotes.getText().toString();
        mCurrentCustomer.mCustomerGroup = mEditTextGroup.getText().toString();
        mCurrentCustomer.mNewsletter = mCheckBoxNewsletter.isChecked();

        for(CustomField cf : mCustomFields) {
            String newValue = "";
            if(cf.mEditViewReference instanceof EditText) {
                newValue = ((EditText)cf.mEditViewReference).getText().toString();
                if(cf.mType == 3) {
                    try {
                        // try parse date and save it in normalized format
                        Date selectedDate = DateControl.birthdayDateFormat.parse(newValue);
                        newValue = CustomerDatabase.storageFormatWithTime.format(selectedDate);
                    } catch(Exception ignored) {}
                }
            } else if(cf.mEditViewReference instanceof Spinner) {
                CustomField selectedItem = ((CustomField) ((Spinner)cf.mEditViewReference).getSelectedItem());
                if(selectedItem != null) newValue = selectedItem.mTitle;
            }
            mCurrentCustomer.updateOrCreateCustomField(cf.mTitle, newValue);
        }
    }

    public void setCustomerBirthday(View v) {
        if(mBirthdayCalendar == null) {
            mBirthdayCalendar = Calendar.getInstance();
        }
        final DatePickerDialog.OnDateSetListener date = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                mBirthdayCalendar.set(Calendar.YEAR, year);
                mBirthdayCalendar.set(Calendar.MONTH, monthOfYear);
                mBirthdayCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                mButtonBirthday.setText(DateControl.birthdayDateFormat.format(mBirthdayCalendar.getTime()));
            }
        };
        new DatePickerDialog(
                CustomerEditActivity.this, date,
                mBirthdayCalendar.get(Calendar.YEAR),
                mBirthdayCalendar.get(Calendar.MONTH),
                mBirthdayCalendar.get(Calendar.DAY_OF_MONTH)
        ).show();
    }
    public void removeCustomerBirthday(View v) {
        mBirthdayCalendar = null;
        mButtonBirthday.setText(getString(R.string.no_date_set));
    }

    public void setCustomerImage(View v) {
        final Dialog ad = new Dialog(this);
        ad.requestWindowFeature(Window.FEATURE_NO_TITLE);
        ad.setContentView(R.layout.dialog_customer_image);
        ad.findViewById(R.id.buttonConsentFromCamera).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ad.dismiss();
                Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                me.startActivityForResult(cameraIntent, CUSTOMER_IMAGE_CAMERA_REQUEST);
            }
        });
        ad.findViewById(R.id.buttonConsentFromGallery).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ad.dismiss();
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent, getString(R.string.choose_from_gallery)), CUSTOMER_IMAGE_PICK_REQUEST);
            }
        });
        ad.findViewById(R.id.buttonConsentCancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ad.dismiss();
            }
        });
        ad.show();
    }
    public void removeCustomerImage(View v) {
        mCurrentCustomer.mImage = new byte[0];
        refreshImage();
    }

    public void onClickAddFile(View v) {
        if(mFc == null || !mFc.unlockedFiles) {
            dialogInApp(getResources().getString(R.string.feature_locked), getResources().getString(R.string.feature_locked_text));
            return;
        }

        final Dialog ad = new Dialog(this);
        ad.requestWindowFeature(Window.FEATURE_NO_TITLE);
        ad.setContentView(R.layout.dialog_file_add);
        ad.findViewById(R.id.buttonConsentFromCamera).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ad.dismiss();
                Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                Uri photoURI = FileProvider.getUriForFile(me, "de.georgsieber.customerdb.provider", StorageControl.getStorageImageTemp(me));
                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                me.startActivityForResult(cameraIntent, FILE_CAMERA_REQUEST);
            }
        });
        ad.findViewById(R.id.buttonConsentFromGallery).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ad.dismiss();
                Intent intent = new Intent();
                intent.setType("*/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent, getString(R.string.choose_from_gallery)), FILE_PICK_REQUEST);
            }
        });
        ad.findViewById(R.id.buttonConsentFromTouchscreen).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ad.dismiss();
                Intent intent = new Intent(me, DrawActivity.class);
                startActivityForResult(intent, FILE_DRAW_REQUEST);
            }
        });
        ad.findViewById(R.id.buttonConsentCancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ad.dismiss();
            }
        });
        ad.show();
    }

}
