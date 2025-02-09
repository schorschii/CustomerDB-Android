package de.georgsieber.customerdb;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.appcompat.app.AppCompatActivity;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Date;
import java.util.List;

import de.georgsieber.customerdb.model.CustomField;
import de.georgsieber.customerdb.model.Customer;
import de.georgsieber.customerdb.model.CustomerCalendar;
import de.georgsieber.customerdb.tools.ColorControl;
import de.georgsieber.customerdb.tools.CommonDialog;
import de.georgsieber.customerdb.tools.StorageControl;


public class SettingsActivity extends AppCompatActivity {

    private final static int PICK_IMAGE_REQUEST = 1;
    private final static int INAPP_REQUEST = 2;
    private final static int SYNCINFO_REQUEST = 3;

    private SettingsActivity me;
    private CustomerDatabase mDb;
    private FeatureCheck mFc;
    private SharedPreferences mSettings;

    Spinner mSpinnerCustomFields;
    Spinner mSpinnerCalendars;
    RadioButton mRadioButtonNoSync;
    RadioButton mRadioButtonCloudSync;
    RadioButton mRadioButtonOwnServerSync;
    EditText mEditTextUrl;
    EditText mEditTextUsername;
    EditText mEditTextPassword;
    EditText mEditTextBirthdayPreviewDays;
    EditText mEditTextCurrency;
    EditText mEditTextPrintFontSize;
    CheckBox mCheckBoxAllowTextInPhoneNumbers;
    RadioGroup mRadioGroupTheme;
    RadioButton mRadioButtonDarkModeSystem;
    RadioButton mRadioButtonDarkModeOn;
    RadioButton mRadioButtonDarkModeOff;
    View mViewColorChanger;
    View mViewColorPreview;
    CheckBox mCheckBoxShowPicture;
    CheckBox mCheckBoxShowPhoneField;
    CheckBox mCheckBoxShowEmailField;
    CheckBox mCheckBoxShowAddressField;
    CheckBox mCheckBoxShowNotesField;
    CheckBox mCheckBoxShowNewsletterField;
    CheckBox mCheckBoxShowBirthdayField;
    CheckBox mCheckBoxShowGroupField;
    CheckBox mCheckBoxShowFiles;
    CheckBox mCheckBoxShowConsentField;

    private int mRemoteDatabaseConnType = 0;
    private String mRemoteDatabaseConnURL = "";
    private String mRemoteDatabaseConnUsername = "";
    private String mRemoteDatabaseConnPassword = "";
    private int mBirthdayPreviewDays = 0;
    private String mCurrency = "";
    private int mPrintFontSize = 0;
    private Boolean mAllowTextInPhoneNumbers = false;
    private String mDefaultCustomerTitle = "";
    private String mDefaultCustomerCity = "";
    private String mDefaultCustomerCountry = "";
    private String mDefaultCustomerGroup = "";
    private String mEmailSubject = "";
    private String mEmailTemplate = "";
    private String mEmailNewsletterTemplate = "";
    private String mEmailExportSubject = "";
    private String mEmailExportTemplate = "";
    private String mDefaultAppointmentTitle = "";
    private String mDefaultAppointmentLocation = "";
    private String mIomPassword = "";
    private int mColorDarkMode = -1;
    private int mColor = 0;
    private boolean showCustomerPicture;
    private boolean showPhoneField;
    private boolean showEmailField;
    private boolean showAddressField;
    private boolean showNotesField;
    private boolean showNewsletterField;
    private boolean showBirthdayField;
    private boolean showGroupField;
    private boolean showFiles;
    private boolean showConsentField;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // init settings
        mSettings = getSharedPreferences(MainActivity.PREFS_NAME, 0);

        // init activity view
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        me = this;
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        // init toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if(getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // find views
        mSpinnerCustomFields = findViewById(R.id.spinnerCustomField);
        mSpinnerCalendars = findViewById(R.id.spinnerCalendar);
        mRadioButtonNoSync = findViewById(R.id.radioButtonNoSync);
        mRadioButtonCloudSync = findViewById(R.id.radioButtonCloudSync);
        mRadioButtonOwnServerSync = findViewById(R.id.radioButtonOwnServerSync);
        mEditTextUrl = findViewById(R.id.editTextURL);
        mEditTextUsername = findViewById(R.id.editTextUsername);
        mEditTextPassword = findViewById(R.id.editTextPassword);
        mEditTextBirthdayPreviewDays = findViewById(R.id.editTextBirthdayPreviewDays);
        mEditTextCurrency = findViewById(R.id.editTextCurrency);
        mEditTextPrintFontSize = findViewById(R.id.editTextPrintFontSize);
        mCheckBoxAllowTextInPhoneNumbers = findViewById(R.id.checkBoxAllowTextInPhoneNumbers);
        mRadioGroupTheme = findViewById(R.id.radioGroupTheme);
        mRadioButtonDarkModeSystem = findViewById(R.id.radioButtonDarkModeSystem);
        mRadioButtonDarkModeOn = findViewById(R.id.radioButtonDarkModeOn);
        mRadioButtonDarkModeOff = findViewById(R.id.radioButtonDarkModeOff);
        mViewColorChanger = findViewById(R.id.viewColorChanger);
        mViewColorPreview = findViewById(R.id.viewColorPreview);
        mCheckBoxShowPicture = findViewById(R.id.checkBoxShowPicture);
        mCheckBoxShowPhoneField = findViewById(R.id.checkBoxShowPhoneField);
        mCheckBoxShowEmailField = findViewById(R.id.checkBoxShowEmailField);
        mCheckBoxShowAddressField = findViewById(R.id.checkBoxShowAddressField);
        mCheckBoxShowNotesField = findViewById(R.id.checkBoxShowNotesField);
        mCheckBoxShowNewsletterField = findViewById(R.id.checkBoxShowNewsletterField);
        mCheckBoxShowBirthdayField = findViewById(R.id.checkBoxShowBirthdayField);
        mCheckBoxShowGroupField = findViewById(R.id.checkBoxShowGroupField);
        mCheckBoxShowFiles = findViewById(R.id.checkBoxShowFiles);
        mCheckBoxShowConsentField = findViewById(R.id.checkBoxShowConsentField);

        // init DB
        mDb = new CustomerDatabase(this);
        reloadCustomFields();
        reloadCalendars();

        // register events
        mViewColorChanger.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mFc == null || !mFc.unlockedDesignOptions) {
                    dialogInApp(getResources().getString(R.string.feature_locked), getResources().getString(R.string.feature_locked_text));
                    return;
                }
                int defaultColor = Color.argb(0xff, ColorControl.DEFAULT_COLOR_R, ColorControl.DEFAULT_COLOR_G, ColorControl.DEFAULT_COLOR_B);
                showColorDialog(mColor!=defaultColor, mColor, new ColorDialogCallback() {
                    @Override
                    public void ok(boolean customColor, int red, int green, int blue) {
                        if(customColor)
                            mColor = Color.argb(0xff, red, green, blue);
                        else
                            mColor = defaultColor;
                        updateColorPreviewButton(mColor, mViewColorPreview, null);
                    }
                });
            }
        });

        // load settings
        loadSettings();

        // update color
        ColorControl.updateActionBarColor(this, mSettings);

        // init logo buttons
        showHideLogoButtons();

        // load in-app purchases
        mFc = new FeatureCheck(this);
        mFc.setFeatureCheckReadyListener(new FeatureCheck.featureCheckReadyListener() {
            @Override
            public void featureCheckReady(boolean fetchSuccess) {
                if(mFc.unlockedInputOnlyMode) {
                    findViewById(R.id.linearLayoutPassword).setVisibility(View.VISIBLE);
                } else {
                    findViewById(R.id.linearLayoutPassword).setVisibility(View.GONE);
                }
                if(!mFc.unlockedDesignOptions) {
                    findViewById(R.id.radioButtonDarkModeSystem).setEnabled(false);
                    findViewById(R.id.radioButtonDarkModeOn).setEnabled(false);
                    findViewById(R.id.radioButtonDarkModeOff).setEnabled(false);
                }
            }
        });
        mFc.init();
    }

    void loadSettings() {
        // restore preferences
        mRemoteDatabaseConnType = mSettings.getInt("webapi-type", 0);
        mRemoteDatabaseConnURL = mSettings.getString("webapi-url","");
        mRemoteDatabaseConnUsername = mSettings.getString("webapi-username","");
        mRemoteDatabaseConnPassword = mSettings.getString("webapi-password","");
        mBirthdayPreviewDays = mSettings.getInt("birthday-preview-days", BirthdayActivity.DEFAULT_BIRTHDAY_PREVIEW_DAYS);
        mCurrency = mSettings.getString("currency", "€");
        mPrintFontSize = mSettings.getInt("print-font-size", 44);
        mAllowTextInPhoneNumbers = mSettings.getBoolean("phone-allow-text", false);
        mDefaultCustomerTitle = mSettings.getString("default-customer-title", "");
        mDefaultCustomerCity = mSettings.getString("default-customer-city", "");
        mDefaultCustomerCountry = mSettings.getString("default-customer-country", "");
        mDefaultCustomerGroup = mSettings.getString("default-customer-group", "");
        mEmailSubject = mSettings.getString("email-subject", getResources().getString(R.string.email_subject_template));
        mEmailTemplate = mSettings.getString("email-template", getResources().getString(R.string.email_text_template));
        mEmailNewsletterTemplate = mSettings.getString("email-newsletter-template", getResources().getString(R.string.newsletter_text_template));
        mEmailExportSubject = mSettings.getString("email-export-subject", getResources().getString(R.string.email_export_subject_template));
        mEmailExportTemplate = mSettings.getString("email-export-template", getResources().getString(R.string.email_export_text_template));
        mDefaultAppointmentTitle = mSettings.getString("default-appointment-title", "");
        mDefaultAppointmentLocation = mSettings.getString("default-appointment-location", "");
        mIomPassword = mSettings.getString("iom-password", "");
        mColorDarkMode = mSettings.getInt("dark-mode-native", -1);
        mColor = Color.argb(0xff,
                mSettings.getInt("design-red", ColorControl.DEFAULT_COLOR_R),
                mSettings.getInt("design-green", ColorControl.DEFAULT_COLOR_G),
                mSettings.getInt("design-blue", ColorControl.DEFAULT_COLOR_B));
        showCustomerPicture = mSettings.getBoolean("show-customer-picture", true);
        showPhoneField = mSettings.getBoolean("show-phone-field", true);
        showEmailField = mSettings.getBoolean("show-email-field", true);
        showAddressField = mSettings.getBoolean("show-address-field", true);
        showNotesField = mSettings.getBoolean("show-notes-field", true);
        showNewsletterField = mSettings.getBoolean("show-newsletter-field", true);
        showBirthdayField = mSettings.getBoolean("show-birthday-field", true);
        showGroupField = mSettings.getBoolean("show-group-field", true);
        showFiles = mSettings.getBoolean("show-files", true);
        showConsentField = mSettings.getBoolean("show-consent-field", false);
        String lastCallReceived = mSettings.getString("last-call-received", "");

        // fill text boxes
        switch(mRemoteDatabaseConnType) {
            case 0:
                mRadioButtonNoSync.setChecked(true);
                break;
            case 1:
                mRadioButtonCloudSync.setChecked(true);
                break;
            case 2:
                mRadioButtonOwnServerSync.setChecked(true);
                break;
        }
        showHideSyncOptions(null);

        if(mColorDarkMode == AppCompatDelegate.MODE_NIGHT_YES) {
            mRadioButtonDarkModeOn.setChecked(true);
        } else if(mColorDarkMode == AppCompatDelegate.MODE_NIGHT_NO) {
            mRadioButtonDarkModeOff.setChecked(true);
        } else {
            mRadioButtonDarkModeSystem.setChecked(true);
        }

        mEditTextUrl.setText(mRemoteDatabaseConnURL);
        mEditTextUsername.setText(mRemoteDatabaseConnUsername);
        mEditTextPassword.setText(mRemoteDatabaseConnPassword);
        mEditTextBirthdayPreviewDays.setText(Integer.toString(mBirthdayPreviewDays));
        mEditTextCurrency.setText(mCurrency);
        mEditTextPrintFontSize.setText(Integer.toString(mPrintFontSize));
        mCheckBoxAllowTextInPhoneNumbers.setChecked(mAllowTextInPhoneNumbers);
        updateColorPreviewButton(mColor, mViewColorPreview, null);
        mCheckBoxShowPicture.setChecked(showCustomerPicture);
        mCheckBoxShowPhoneField.setChecked(showPhoneField);
        mCheckBoxShowEmailField.setChecked(showEmailField);
        mCheckBoxShowAddressField.setChecked(showAddressField);
        mCheckBoxShowNotesField.setChecked(showNotesField);
        mCheckBoxShowNewsletterField.setChecked((showNewsletterField));
        mCheckBoxShowBirthdayField.setChecked(showBirthdayField);
        mCheckBoxShowGroupField.setChecked(showGroupField);
        mCheckBoxShowFiles.setChecked(showFiles);
        mCheckBoxShowConsentField.setChecked(showConsentField);

        if(lastCallReceived != null && lastCallReceived.equals("")) {
            findViewById(R.id.textViewLastCallReceived).setVisibility(View.GONE);
        } else {
            ((TextView) findViewById(R.id.textViewLastCallReceived)).setText(
                    getResources().getString(R.string.last_call) + " " + lastCallReceived
            );
        }
    }

    public void showHideSyncOptions(View v) {
        mEditTextUrl.setVisibility(View.GONE);
        mEditTextUsername.setVisibility(View.GONE);
        mEditTextPassword.setVisibility(View.GONE);
        if(((RadioButton) findViewById(R.id.radioButtonCloudSync)).isChecked()) {
            mEditTextUsername.setVisibility(View.VISIBLE);
            mEditTextPassword.setVisibility(View.VISIBLE);
        }
        else if(((RadioButton) findViewById(R.id.radioButtonOwnServerSync)).isChecked()) {
            mEditTextUrl.setVisibility(View.VISIBLE);
            mEditTextUsername.setVisibility(View.VISIBLE);
            mEditTextPassword.setVisibility(View.VISIBLE);
        }
    }

    private void showHideLogoButtons() {
        File logo = StorageControl.getStorageLogo(this);
        if(logo.exists()) {
            findViewById(R.id.buttonSettingsRemoveLogo).setVisibility(View.VISIBLE);
            findViewById(R.id.buttonSettingsSetLogo).setVisibility(View.GONE);
        } else {
            findViewById(R.id.buttonSettingsRemoveLogo).setVisibility(View.GONE);
            findViewById(R.id.buttonSettingsSetLogo).setVisibility(View.VISIBLE);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.action_settings_done:
                onSetMiscButtonClick();
                return true;
            case R.id.action_settings_help:
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(getResources().getString(R.string.help_website)));
                startActivity(browserIntent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_settings, menu);
        return true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode) {
            case(PICK_IMAGE_REQUEST) : {
                if(resultCode == RESULT_OK && data != null && data.getData() != null) {
                    try {
                        InputStream inputStream = this.getContentResolver().openInputStream(data.getData());
                        byte[] targetArray = new byte[inputStream.available()];
                        inputStream.read(targetArray);
                        File fl = StorageControl.getStorageLogo(this);
                        FileOutputStream stream = new FileOutputStream(fl);
                        stream.write(targetArray);
                        stream.flush();
                        stream.close();
                        scanFile(fl);
                        showHideLogoButtons();
                    } catch(Exception e) {
                        e.printStackTrace();
                    }
                }
                break;
            }
            case(INAPP_REQUEST) : {
                mFc.init();
                break;
            }
            case(SYNCINFO_REQUEST) : {
                loadSettings();
                break;
            }
        }
    }

    private void updateColorPreview(int red, int green, int blue, View v) {
        v.setBackgroundColor(Color.argb(0xff, red, green, blue));
    }
    private void updateColorPreviewButton(int color, View previewView, EditText hexTextBox) {
        previewView.setBackgroundColor(Color.argb(0xff, Color.red(color), Color.green(color), Color.blue(color)));
        if(hexTextBox != null) hexTextBox.setText(String.format("#%06X", (0xFFFFFF & color)));
    }
    interface ColorDialogCallback {
        void ok(boolean customColor, int red, int green, int blue);
    }
    private void showColorDialog(Boolean customColor, int initialColor, final ColorDialogCallback colorDialogFinished) {
        final Dialog ad = new Dialog(this);
        ad.requestWindowFeature(Window.FEATURE_NO_TITLE);
        ad.setContentView(R.layout.dialog_color);
        final CheckBox checkBoxCustomColor = ad.findViewById(R.id.checkBoxCustomColor);
        final EditText editTextColorHex = ad.findViewById(R.id.editTextColorHex);
        final SeekBar seekBarRed = ad.findViewById(R.id.seekBarRed);
        final SeekBar seekBarGreen = ad.findViewById(R.id.seekBarGreen);
        final SeekBar seekBarBlue = ad.findViewById(R.id.seekBarBlue);
        final View colorPreview = ad.findViewById(R.id.viewColorPreview);
        final Button buttonOK = ad.findViewById(R.id.buttonOK);
        if(customColor == null) {
            checkBoxCustomColor.setVisibility(View.GONE);
        } else {
            checkBoxCustomColor.setChecked(customColor);
            if(!customColor) {
                seekBarRed.setEnabled(false);
                seekBarGreen.setEnabled(false);
                seekBarBlue.setEnabled(false);
                editTextColorHex.setEnabled(false);
            }
        }
        checkBoxCustomColor.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                seekBarRed.setEnabled(b);
                seekBarGreen.setEnabled(b);
                seekBarBlue.setEnabled(b);
                editTextColorHex.setEnabled(b);
            }
        });
        seekBarRed.setProgress(Color.red(initialColor));
        seekBarGreen.setProgress(Color.green(initialColor));
        seekBarBlue.setProgress(Color.blue(initialColor));
        seekBarRed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateColorPreviewButton(Color.argb(0xff, seekBarRed.getProgress(), seekBarGreen.getProgress(), seekBarBlue.getProgress()), colorPreview, editTextColorHex);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }
        });
        seekBarGreen.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateColorPreviewButton(Color.argb(0xff, seekBarRed.getProgress(), seekBarGreen.getProgress(), seekBarBlue.getProgress()), colorPreview, editTextColorHex);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }
        });
        seekBarBlue.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateColorPreviewButton(Color.argb(0xff, seekBarRed.getProgress(), seekBarGreen.getProgress(), seekBarBlue.getProgress()), colorPreview, editTextColorHex);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }
        });
        editTextColorHex.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) { }
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                try {
                    int newColor = Color.parseColor(charSequence.toString());
                    seekBarRed.setProgress(Color.red(newColor));
                    seekBarGreen.setProgress(Color.green(newColor));
                    seekBarBlue.setProgress(Color.blue(newColor));
                    //updateColorPreview(Color.argb(0xff, seekBarRed.getProgress(), seekBarGreen.getProgress(), seekBarBlue.getProgress()), colorPreview, null);
                } catch(Exception ignored) { }
            }
            @Override
            public void afterTextChanged(Editable editable) { }
        });
        updateColorPreviewButton(Color.argb(0xff, seekBarRed.getProgress(), seekBarGreen.getProgress(), seekBarBlue.getProgress()), colorPreview, editTextColorHex);
        ad.show();
        ad.getWindow().setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        buttonOK.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(colorDialogFinished != null) {
                    colorDialogFinished.ok(checkBoxCustomColor.isChecked(), seekBarRed.getProgress(), seekBarGreen.getProgress(), seekBarBlue.getProgress());
                }
                ad.dismiss();
            }
        });
    }

    private void scanFile(File f) {
        Uri uri = Uri.fromFile(f);
        Intent scanFileIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri);
        sendBroadcast(scanFileIntent);
    }

    protected void saveSettings() {
        SharedPreferences.Editor editor = mSettings.edit();
        editor.putInt("webapi-type", mRemoteDatabaseConnType);
        editor.putString("webapi-url", mRemoteDatabaseConnURL);
        editor.putString("webapi-username", mRemoteDatabaseConnUsername);
        editor.putString("webapi-password", mRemoteDatabaseConnPassword);
        editor.putInt("birthday-preview-days", mBirthdayPreviewDays);
        editor.putString("currency", mCurrency);
        editor.putInt("print-font-size", mPrintFontSize);
        editor.putBoolean("phone-allow-text", mAllowTextInPhoneNumbers);
        editor.putString("default-customer-title", mDefaultCustomerTitle);
        editor.putString("default-customer-city", mDefaultCustomerCity);
        editor.putString("default-customer-country", mDefaultCustomerCountry);
        editor.putString("default-customer-group", mDefaultCustomerGroup);
        editor.putString("email-subject", mEmailSubject);
        editor.putString("email-template", mEmailTemplate);
        editor.putString("email-newsletter-template", mEmailNewsletterTemplate);
        editor.putString("email-export-subject", mEmailExportSubject);
        editor.putString("email-export-template", mEmailExportTemplate);
        editor.putString("default-appointment-title", mDefaultAppointmentTitle);
        editor.putString("default-appointment-location", mDefaultAppointmentLocation);
        editor.putString("iom-password", mIomPassword);
        editor.putInt("dark-mode-native", mColorDarkMode);
        editor.putInt("design-red", Color.red(mColor));
        editor.putInt("design-green", Color.green(mColor));
        editor.putInt("design-blue", Color.blue(mColor));
        editor.putBoolean("show-customer-picture", showCustomerPicture);
        editor.putBoolean("show-phone-field", showPhoneField);
        editor.putBoolean("show-email-field", showEmailField);
        editor.putBoolean("show-address-field", showAddressField);
        editor.putBoolean("show-notes-field", showNotesField);
        editor.putBoolean("show-newsletter-field", showNewsletterField);
        editor.putBoolean("show-birthday-field", showBirthdayField);
        editor.putBoolean("show-group-field", showGroupField);
        editor.putBoolean("show-files", showFiles);
        editor.putBoolean("show-consent-field", showConsentField);
        editor.apply();
    }

    public void dialog(String text, final boolean finishActivity) {
        AlertDialog ad = new AlertDialog.Builder(this).create();
        ad.setCancelable(false);
        ad.setMessage(text);
        ad.setButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                if(finishActivity) finish();
            }
        });
        ad.show();
    }

    public void onSetMiscButtonClick() {
        mRemoteDatabaseConnURL = mEditTextUrl.getText().toString();
        mRemoteDatabaseConnUsername = mEditTextUsername.getText().toString();
        mRemoteDatabaseConnPassword = mEditTextPassword.getText().toString();
        if(mRadioButtonCloudSync.isChecked())
            mRemoteDatabaseConnType = 1;
        else if(mRadioButtonOwnServerSync.isChecked())
            mRemoteDatabaseConnType = 2;
        else {
            mRemoteDatabaseConnType = 0;
            mRemoteDatabaseConnURL = "";
            mRemoteDatabaseConnUsername = "";
            mRemoteDatabaseConnPassword = "";
        }

        if(mRadioButtonDarkModeOn.isChecked()) {
            mColorDarkMode = AppCompatDelegate.MODE_NIGHT_YES;
        } else if(mRadioButtonDarkModeOff.isChecked()) {
            mColorDarkMode = AppCompatDelegate.MODE_NIGHT_NO;
        } else {
            mColorDarkMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
        }
        if(mColorDarkMode != CustomerDatabaseApp.getAppTheme(me)) {
            CustomerDatabaseApp.setAppTheme(mColorDarkMode);
        }

        try {
            mBirthdayPreviewDays = Integer.parseInt(mEditTextBirthdayPreviewDays.getText().toString());
        } catch(NumberFormatException ignored) {}
        mCurrency = mEditTextCurrency.getText().toString();
        try {
            mPrintFontSize = Integer.parseInt(mEditTextPrintFontSize.getText().toString());
            mPrintFontSize = Math.max(10, Math.min(100, mPrintFontSize));
        } catch(NumberFormatException ignored) {}
        mAllowTextInPhoneNumbers = mCheckBoxAllowTextInPhoneNumbers.isChecked();
        showCustomerPicture = mCheckBoxShowPicture.isChecked();
        showPhoneField = mCheckBoxShowPhoneField.isChecked();
        showEmailField = mCheckBoxShowEmailField.isChecked();
        showAddressField = mCheckBoxShowAddressField.isChecked();
        showNotesField = mCheckBoxShowNotesField.isChecked();
        showNewsletterField = mCheckBoxShowNewsletterField.isChecked();
        showBirthdayField = mCheckBoxShowBirthdayField.isChecked();
        showGroupField = mCheckBoxShowGroupField.isChecked();
        showFiles = mCheckBoxShowFiles.isChecked();
        showConsentField = mCheckBoxShowConsentField.isChecked();

        String lastUsername = mSettings.getString("webapi-username","");
        if(mRemoteDatabaseConnType != 0 && !lastUsername.equals("") && !lastUsername.equals(mRemoteDatabaseConnUsername)) {
            // exit after dialog
            DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch(which) {
                        case DialogInterface.BUTTON_POSITIVE:
                            mSettings.edit().putLong("last-successful-sync", 0).apply();
                            mDb.truncateCustomers();
                            mDb.truncateVouchers();
                            mDb.truncateCalendars();
                            mDb.truncateAppointments();
                            saveSettings();
                            finish();
                            break;

                        case DialogInterface.BUTTON_NEGATIVE:
                            saveSettings();
                            finish();
                            break;
                    }
                }
            };
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(getString(R.string.sync_account_changed))
                    .setMessage(getString(R.string.sync_account_changed_text))
                    .setPositiveButton(getString(R.string.yes), dialogClickListener)
                    .setNegativeButton(getString(R.string.no), dialogClickListener).show();
        } else {
            // normal exit
            saveSettings();
            finish();
        }
    }

    public void onApiHelpButtonClick(View v) {
        Intent infoIntent = new Intent(this, InfoActivity.class);
        startActivityForResult(infoIntent, SYNCINFO_REQUEST);
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
                startActivityForResult(new Intent(me, AboutActivity.class), INAPP_REQUEST);
            }
        });
        ad.show();
    }

    private void setTextFieldsFocusable(boolean focusable) {
        View[] editViews = {
                mEditTextUrl, mEditTextUsername, mEditTextPassword,
                mEditTextBirthdayPreviewDays, mEditTextCurrency, mEditTextPrintFontSize
        };
        for(View v : editViews) {
            v.setFocusable(focusable);
            v.setFocusableInTouchMode(focusable);
        }
    }

    public void onSetPasswordButtonClick(View v) {
        final Dialog ad = new Dialog(this);
        ad.requestWindowFeature(Window.FEATURE_NO_TITLE);
        ad.setContentView(R.layout.dialog_set_password);
        if(ad.getWindow() != null) ad.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        ad.show();
        ad.findViewById(R.id.buttonInputBoxOK).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String password1 = ((EditText) ad.findViewById(R.id.editTextPassword)).getText().toString();
                String password2 = ((EditText) ad.findViewById(R.id.editTextPasswordConfirm)).getText().toString();
                if(!password1.equals(password2)) {
                    dialog(getResources().getString(R.string.passwords_not_matching), false);
                    return;
                }
                mIomPassword = password1;
                ad.dismiss();
            }
        });
    }

    public void onAddCustomFieldButtonClick(View v) {
        if(mFc == null || !mFc.unlockedCustomFields) {
            dialogInApp(getResources().getString(R.string.feature_locked), getResources().getString(R.string.feature_locked_text));
            return;
        }
        setTextFieldsFocusable(false);
        final Dialog ad = new Dialog(this);
        ad.requestWindowFeature(Window.FEATURE_NO_TITLE);
        ad.setContentView(R.layout.dialog_new_custom_field);
        if(ad.getWindow() != null) ad.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        ad.findViewById(R.id.buttonNewCustomFieldOK).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ad.dismiss();
                String input = ((EditText) ad.findViewById(R.id.editTextCustomFieldTitle)).getText().toString();
                if(input.trim().equals("")) {
                    CommonDialog.show(me, getString(R.string.error), getString(R.string.name_cannot_be_empty), CommonDialog.TYPE.FAIL, false);
                    return;
                }
                int type = -1;
                if(((RadioButton) ad.findViewById(R.id.radioButtonNewFieldAlphanumeric)).isChecked())
                    type = 0;
                if(((RadioButton) ad.findViewById(R.id.radioButtonNewFieldNumeric)).isChecked())
                    type = 1;
                if(((RadioButton) ad.findViewById(R.id.radioButtonNewFieldDropDown)).isChecked())
                    type = 2;
                if(((RadioButton) ad.findViewById(R.id.radioButtonNewFieldDate)).isChecked())
                    type = 3;
                if(((RadioButton) ad.findViewById(R.id.radioButtonNewFieldAlphanumericMultiLine)).isChecked())
                    type = 4;
                mDb.addCustomField(new CustomField(input, type));
                reloadCustomFields();
            }
        });
        ad.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                setTextFieldsFocusable(true);
            }
        });
        ad.show();
    }
    public void onRemoveCustomFieldButtonClick(View v) {
        if(mSpinnerCustomFields.getSelectedItem() != null) {
            AlertDialog.Builder ad = new AlertDialog.Builder(me);
            ad.setPositiveButton(getResources().getString(R.string.delete), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    mDb.removeCustomField(((CustomField) mSpinnerCustomFields.getSelectedItem()).mId);
                    reloadCustomFields();
                }});
            ad.setNegativeButton(getResources().getString(R.string.abort), null);
            ad.setTitle(getResources().getString(R.string.reallydelete_title));
            ad.setMessage(((CustomField) mSpinnerCustomFields.getSelectedItem()).mTitle);
            ad.show();
        }
    }
    public void onEditCustomFieldButtonClick(View v) {
        final CustomField currentCustomField = (CustomField) mSpinnerCustomFields.getSelectedItem();
        final String oldFieldTitle = currentCustomField.mTitle;

        setTextFieldsFocusable(false);
        final Dialog ad = new Dialog(this);
        ad.requestWindowFeature(Window.FEATURE_NO_TITLE);
        ad.setContentView(R.layout.dialog_new_custom_field);
        if(ad.getWindow() != null) ad.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        ((EditText) ad.findViewById(R.id.editTextCustomFieldTitle)).setText(currentCustomField.mTitle);
        final RadioButton radioButtonAlphanumeric = ad.findViewById(R.id.radioButtonNewFieldAlphanumeric);
        final RadioButton radioButtonNumeric = ad.findViewById(R.id.radioButtonNewFieldNumeric);
        final RadioButton radioButtonDropDown = ad.findViewById(R.id.radioButtonNewFieldDropDown);
        final RadioButton radioButtonDate = ad.findViewById(R.id.radioButtonNewFieldDate);
        final RadioButton radioButtonAlphanumericMultiLine = ad.findViewById(R.id.radioButtonNewFieldAlphanumericMultiLine);
        if(currentCustomField.mType == 0) radioButtonAlphanumeric.setChecked(true);
        else if(currentCustomField.mType == 1) radioButtonNumeric.setChecked(true);
        else if(currentCustomField.mType == 2) radioButtonDropDown.setChecked(true);
        else if(currentCustomField.mType == 3) radioButtonDate.setChecked(true);
        else if(currentCustomField.mType == 4) radioButtonAlphanumericMultiLine.setChecked(true);
        ad.findViewById(R.id.buttonNewCustomFieldOK).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ad.dismiss();

                String input = ((EditText) ad.findViewById(R.id.editTextCustomFieldTitle)).getText().toString();
                int type = -1;
                if(radioButtonAlphanumeric.isChecked()) type = 0;
                if(radioButtonNumeric.isChecked()) type = 1;
                if(radioButtonDropDown.isChecked()) type = 2;
                if(radioButtonDate.isChecked()) type = 3;
                if(radioButtonAlphanumericMultiLine.isChecked()) type = 4;

                if(input.trim().equals("")) {
                    CommonDialog.show(me, getString(R.string.error), getString(R.string.name_cannot_be_empty), CommonDialog.TYPE.FAIL, false);
                    return;
                }
                currentCustomField.mTitle = input;
                currentCustomField.mType = type;
                mDb.updateCustomField(currentCustomField);

                // rebase custom fields in customer objects
                if(!oldFieldTitle.equals(input)) {
                    for(Customer c : mDb.getCustomers(null, false, false, null)) {
                        List<CustomField> fields = c.getCustomFields();
                        for(CustomField field : fields) {
                            if(field.mTitle.equals(oldFieldTitle)) {
                                fields.add(new CustomField(input, field.mValue));
                                fields.remove(field);
                                c.setCustomFields(fields);
                                c.mLastModified = new Date();
                                mDb.updateCustomer(c);
                                break;
                            }
                        }
                    }
                }

                reloadCustomFields();
            }
        });
        ad.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                setTextFieldsFocusable(true);
            }
        });
        ad.show();
        ((EditText) ad.findViewById(R.id.editTextCustomFieldTitle)).selectAll();
    }

    public void onAddCustomFieldValueButtonClick(View v) {
        Spinner s1 = (findViewById(R.id.spinnerCustomField));
        CustomField cf = (CustomField) s1.getSelectedItem();
        if(cf == null) return;
        final int fieldId = cf.mId;

        setTextFieldsFocusable(false);
        final Dialog ad = new Dialog(this);
        ad.requestWindowFeature(Window.FEATURE_NO_TITLE);
        ad.setContentView(R.layout.dialog_input_box);
        ((TextView) ad.findViewById(R.id.textViewInputBox)).setText(getResources().getString(R.string.new_drop_down_value));
        if(ad.getWindow() != null) ad.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        ad.findViewById(R.id.buttonInputBoxOK).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ad.dismiss();
                String input = ((EditText) ad.findViewById(R.id.editTextInputBox)).getText().toString();
                mDb.addCustomFieldPreset(fieldId, input);
                reloadCustomFieldPresets();
            }
        });
        ad.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                setTextFieldsFocusable(true);
            }
        });
        ad.show();
    }
    public void onRemoveCustomFieldValueButtonClick(View v) {
        final Spinner s = findViewById(R.id.spinnerCustomFieldDropDownValues);
        if(s.getSelectedItem() != null) {
            AlertDialog.Builder ad = new AlertDialog.Builder(me);
            ad.setPositiveButton(getResources().getString(R.string.delete), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    mDb.removeCustomFieldPreset(((CustomField) s.getSelectedItem()).mId);
                    reloadCustomFieldPresets();
                }});
            ad.setNegativeButton(getResources().getString(R.string.abort), null);
            ad.setTitle(getResources().getString(R.string.reallydelete_title));
            ad.setMessage(((CustomField) s.getSelectedItem()).mTitle);
            ad.show();
        }
    }

    private void reloadCustomFields() {
        ArrayAdapter<CustomField> a = new ArrayAdapter<>(this, R.layout.item_list_simple, mDb.getCustomFields());
        mSpinnerCustomFields.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                CustomField cf = (CustomField) parent.getSelectedItem();
                if(cf.mType == 2) {
                    findViewById(R.id.linearLayoutCustomFieldsSettingsDropDownValues).setVisibility(View.VISIBLE);
                    reloadCustomFieldPresets();
                } else {
                    findViewById(R.id.linearLayoutCustomFieldsSettingsDropDownValues).setVisibility(View.GONE);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                findViewById(R.id.linearLayoutCustomFieldsSettingsDropDownValues).setVisibility(View.GONE);
            }
        });
        mSpinnerCustomFields.setAdapter(a);
        if(a.getCount() == 0) {
            findViewById(R.id.linearLayoutCustomFieldsSettingsDropDownValues).setVisibility(View.GONE);
            findViewById(R.id.buttonEditCustomFieldSettings).setVisibility(View.GONE);
            findViewById(R.id.buttonRemoveCustomFieldSettings).setVisibility(View.GONE);
        } else {
            findViewById(R.id.buttonEditCustomFieldSettings).setVisibility(View.VISIBLE);
            findViewById(R.id.buttonRemoveCustomFieldSettings).setVisibility(View.VISIBLE);
        }
    }
    private void reloadCustomFieldPresets() {
        Spinner s1 = (findViewById(R.id.spinnerCustomField));
        CustomField cf = (CustomField) s1.getSelectedItem();
        if(cf == null) return;
        ArrayAdapter<CustomField> a = new ArrayAdapter<>(this, R.layout.item_list_simple, mDb.getCustomFieldPresets(cf.mId));
        Spinner s2 = (findViewById(R.id.spinnerCustomFieldDropDownValues));
        s2.setAdapter(a);
        s2.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        if(a.getCount() == 0) {
            findViewById(R.id.buttonRemoveCustomFieldValueSettings).setVisibility(View.GONE);
        } else {
            findViewById(R.id.buttonRemoveCustomFieldValueSettings).setVisibility(View.VISIBLE);
        }
    }

    private void reloadCalendars() {
        ArrayAdapter<CustomerCalendar> a = new ArrayAdapter<>(this, R.layout.item_list_simple, mDb.getCalendars(false));
        mSpinnerCalendars.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {}
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
        mSpinnerCalendars.setAdapter(a);
        if(a.getCount() == 0) {
            findViewById(R.id.buttonEditCalendarSettings).setVisibility(View.GONE);
            findViewById(R.id.buttonRemoveCalendarSettings).setVisibility(View.GONE);
        } else {
            findViewById(R.id.buttonEditCalendarSettings).setVisibility(View.VISIBLE);
            findViewById(R.id.buttonRemoveCalendarSettings).setVisibility(View.VISIBLE);
        }
    }
    public void onAddCalendarButtonClick(View v) {
        if(mFc == null || !mFc.unlockedCalendar) {
            dialogInApp(getResources().getString(R.string.feature_locked), getResources().getString(R.string.feature_locked_text));
            return;
        }

        setTextFieldsFocusable(false);
        final Dialog ad = new Dialog(this);
        ad.requestWindowFeature(Window.FEATURE_NO_TITLE);
        ad.setContentView(R.layout.dialog_new_calendar);
        if(ad.getWindow() != null) ad.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);

        final EditText editTextTitle = ad.findViewById(R.id.editTextTitle);
        final View colorPreviewCalendar = ad.findViewById(R.id.viewColorPreview);
        final SeekBar seekBarRed = ad.findViewById(R.id.seekBarRed);
        final SeekBar seekBarGreen = ad.findViewById(R.id.seekBarGreen);
        final SeekBar seekBarBlue = ad.findViewById(R.id.seekBarBlue);
        seekBarRed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateColorPreview(seekBarRed.getProgress(), seekBarGreen.getProgress(), seekBarBlue.getProgress(), colorPreviewCalendar);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }
        });
        seekBarGreen.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateColorPreview(seekBarRed.getProgress(), seekBarGreen.getProgress(), seekBarBlue.getProgress(), colorPreviewCalendar);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }
        });
        seekBarBlue.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateColorPreview(seekBarRed.getProgress(), seekBarGreen.getProgress(), seekBarBlue.getProgress(), colorPreviewCalendar);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }
        });
        updateColorPreview(seekBarRed.getProgress(), seekBarGreen.getProgress(), seekBarBlue.getProgress(), colorPreviewCalendar);
        ad.findViewById(R.id.buttonOK).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ad.dismiss();
                if(editTextTitle.getText().toString().trim().equals("")) {
                    CommonDialog.show(me, getString(R.string.error), getString(R.string.name_cannot_be_empty), CommonDialog.TYPE.FAIL, false);
                    return;
                }
                CustomerCalendar c = new CustomerCalendar();
                c.mId = CustomerCalendar.generateID();
                c.mTitle = editTextTitle.getText().toString();
                c.mColor = ColorControl.getHexColor(Color.rgb(seekBarRed.getProgress(), seekBarGreen.getProgress(), seekBarBlue.getProgress()));
                if(!c.mTitle.equals("")) mDb.addCalendar(c);
                reloadCalendars();
            }
        });
        ad.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                setTextFieldsFocusable(true);
            }
        });
        ad.show();
    }
    public void onRemoveCalendarButtonClick(View v) {
        if(mSpinnerCalendars.getSelectedItem() != null) {
            AlertDialog.Builder ad = new AlertDialog.Builder(me);
            ad.setPositiveButton(getResources().getString(R.string.delete), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    mDb.removeCalendar(((CustomerCalendar) mSpinnerCalendars.getSelectedItem()));
                    reloadCalendars();
                }});
            ad.setNegativeButton(getResources().getString(R.string.abort), null);
            ad.setTitle(getResources().getString(R.string.reallydelete_title));
            ad.setMessage(((CustomerCalendar) mSpinnerCalendars.getSelectedItem()).mTitle);
            ad.show();
        }
    }
    public void onEditCalendarButtonClick(View v) {
        final CustomerCalendar currentCalendar = (CustomerCalendar) mSpinnerCalendars.getSelectedItem();
        int color = Color.parseColor(currentCalendar.mColor);

        setTextFieldsFocusable(false);
        final Dialog ad = new Dialog(this);
        ad.requestWindowFeature(Window.FEATURE_NO_TITLE);
        ad.setContentView(R.layout.dialog_new_calendar);
        if(ad.getWindow() != null) ad.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        final View colorPreviewCalendar = ad.findViewById(R.id.viewColorPreview);
        final EditText editTextTitle = (EditText) ad.findViewById(R.id.editTextTitle);
        editTextTitle.setText(currentCalendar.mTitle);
        final SeekBar seekBarRed = ((SeekBar) ad.findViewById(R.id.seekBarRed));
        final SeekBar seekBarGreen = ((SeekBar) ad.findViewById(R.id.seekBarGreen));
        final SeekBar seekBarBlue = ((SeekBar) ad.findViewById(R.id.seekBarBlue));
        seekBarRed.setProgress(Color.red(color));
        seekBarRed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateColorPreview(seekBarRed.getProgress(), seekBarGreen.getProgress(), seekBarBlue.getProgress(), colorPreviewCalendar);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }
        });
        seekBarGreen.setProgress(Color.green(color));
        seekBarGreen.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateColorPreview(seekBarRed.getProgress(), seekBarGreen.getProgress(), seekBarBlue.getProgress(), colorPreviewCalendar);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }
        });
        seekBarBlue.setProgress(Color.blue(color));
        seekBarBlue.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateColorPreview(seekBarRed.getProgress(), seekBarGreen.getProgress(), seekBarBlue.getProgress(), colorPreviewCalendar);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }
        });
        updateColorPreview(seekBarRed.getProgress(), seekBarGreen.getProgress(), seekBarBlue.getProgress(), colorPreviewCalendar);
        ad.findViewById(R.id.buttonOK).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ad.dismiss();
                if(editTextTitle.getText().toString().trim().equals("")) {
                    CommonDialog.show(me, getString(R.string.error), getString(R.string.name_cannot_be_empty), CommonDialog.TYPE.FAIL, false);
                    return;
                }
                currentCalendar.mTitle = editTextTitle.getText().toString();
                currentCalendar.mColor = ColorControl.getHexColor(Color.rgb(seekBarRed.getProgress(), seekBarGreen.getProgress(), seekBarBlue.getProgress()));
                currentCalendar.mLastModified = new Date();
                mDb.updateCalendar(currentCalendar);
                reloadCalendars();
            }
        });
        ad.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                setTextFieldsFocusable(true);
            }
        });
        ad.show();
        editTextTitle.selectAll();
    }

    public void onChooseLogoButtonClick(View v) {
        if(mFc == null || !mFc.unlockedDesignOptions) {
            dialogInApp(getResources().getString(R.string.feature_locked), getResources().getString(R.string.feature_locked_text));
            return;
        }
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Image"), PICK_IMAGE_REQUEST);
    }

    public void onRemoveLogoButtonClick(View v) {
        File logo = StorageControl.getStorageLogo(this);
        if(logo.exists()) {
            if(logo.delete()) {
                scanFile(logo);
                dialog(getResources().getString(R.string.removed_logo), false);
            } else {
                dialog(getResources().getString(R.string.removed_logo_failed), false);
            }
        }
        showHideLogoButtons();
    }

    public void changeDefaultCustomerTitle(View v) {
        final Dialog ad = inputBox(getResources().getString(R.string.default_customer_title), this.mDefaultCustomerTitle);
        ad.findViewById(R.id.buttonInputBoxOK).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                me.mDefaultCustomerTitle = ((EditText) ad.findViewById(R.id.editTextInputBox)).getText().toString();
                ad.dismiss();
            }
        });
    }
    public void changeDefaultCustomerCity(View v) {
        final Dialog ad = inputBox(getResources().getString(R.string.default_customer_city), this.mDefaultCustomerCity);
        ad.findViewById(R.id.buttonInputBoxOK).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                me.mDefaultCustomerCity = ((EditText) ad.findViewById(R.id.editTextInputBox)).getText().toString();
                ad.dismiss();
            }
        });
    }
    public void changeDefaultCustomerCountry(View v) {
        final Dialog ad = inputBox(getResources().getString(R.string.default_customer_country), this.mDefaultCustomerCountry);
        ad.findViewById(R.id.buttonInputBoxOK).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                me.mDefaultCustomerCountry = ((EditText) ad.findViewById(R.id.editTextInputBox)).getText().toString();
                ad.dismiss();
            }
        });
    }
    public void changeDefaultCustomerGroup(View v) {
        final Dialog ad = inputBox(getResources().getString(R.string.default_customer_group), this.mDefaultCustomerGroup);
        ad.findViewById(R.id.buttonInputBoxOK).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                me.mDefaultCustomerGroup = ((EditText) ad.findViewById(R.id.editTextInputBox)).getText().toString();
                ad.dismiss();
            }
        });
    }
    public void changeEmailSubject(View v) {
        final Dialog ad = inputBox(getResources().getString(R.string.email_subject), this.mEmailSubject);
        ad.findViewById(R.id.buttonInputBoxOK).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                me.mEmailSubject = ((EditText) ad.findViewById(R.id.editTextInputBox)).getText().toString();
                ad.dismiss();
            }
        });
    }
    public void changeEmailTemplate(View v) {
        final Dialog ad = inputBox(getResources().getString(R.string.email_template_long), this.mEmailTemplate);
        ad.findViewById(R.id.buttonInputBoxOK).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                me.mEmailTemplate = ((EditText) ad.findViewById(R.id.editTextInputBox)).getText().toString();
                ad.dismiss();
            }
        });
    }
    public void changeNewsletterTemplate(View v) {
        final Dialog ad = inputBox(getResources().getString(R.string.newsletter_template), this.mEmailNewsletterTemplate);
        ad.findViewById(R.id.buttonInputBoxOK).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                me.mEmailNewsletterTemplate = ((EditText) ad.findViewById(R.id.editTextInputBox)).getText().toString();
                ad.dismiss();
            }
        });
    }
    public void changeExportSubject(View v) {
        final Dialog ad = inputBox(getResources().getString(R.string.email_export_subject), this.mEmailExportSubject);
        ad.findViewById(R.id.buttonInputBoxOK).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                me.mEmailExportSubject = ((EditText) ad.findViewById(R.id.editTextInputBox)).getText().toString();
                ad.dismiss();
            }
        });
    }
    public void changeExportTemplate(View v) {
        final Dialog ad = inputBox(getResources().getString(R.string.email_export_template), this.mEmailExportTemplate);
        ad.findViewById(R.id.buttonInputBoxOK).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                me.mEmailExportTemplate = ((EditText) ad.findViewById(R.id.editTextInputBox)).getText().toString();
                ad.dismiss();
            }
        });
    }
    public void changeDefaultAppointmentTitle(View v) {
        final Dialog ad = inputBox(getResources().getString(R.string.default_appointment_title), this.mDefaultAppointmentTitle);
        ad.findViewById(R.id.buttonInputBoxOK).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                me.mDefaultAppointmentTitle = ((EditText) ad.findViewById(R.id.editTextInputBox)).getText().toString();
                ad.dismiss();
            }
        });
    }
    public void changeDefaultAppointmentLocation(View v) {
        final Dialog ad = inputBox(getResources().getString(R.string.default_appointment_location), this.mDefaultAppointmentLocation);
        ad.findViewById(R.id.buttonInputBoxOK).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                me.mDefaultAppointmentLocation = ((EditText) ad.findViewById(R.id.editTextInputBox)).getText().toString();
                ad.dismiss();
            }
        });
    }

    private Dialog inputBox(String text, String defaultValue) {
        final Dialog ad = new Dialog(this);
        ad.requestWindowFeature(Window.FEATURE_NO_TITLE);
        ad.setContentView(R.layout.dialog_input_box);
        ((TextView) ad.findViewById(R.id.textViewInputBox)).setText(text);
        ((EditText) ad.findViewById(R.id.editTextInputBox)).setText(defaultValue);
        ((EditText) ad.findViewById(R.id.editTextInputBox)).selectAll();
        if(ad.getWindow() != null) ad.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        ad.show();
        return ad;
    }

    public void onClickInstallPluginApp(View v) {
        Intent intent = getPackageManager().getLaunchIntentForPackage(getString(R.string.plugin_app_package_name));
        if(intent == null) {
            // open download page
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(getResources().getString(R.string.plugin_app_url)));
            startActivity(browserIntent);
        } else {
            // start plugin app
            startActivity(intent);
        }
    }

}
