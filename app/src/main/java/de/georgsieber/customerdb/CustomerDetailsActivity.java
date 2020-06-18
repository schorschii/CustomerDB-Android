package de.georgsieber.customerdb;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.print.PrintAttributes;
import android.print.PrintManager;
import android.provider.MediaStore;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.URLSpan;
import android.util.Base64;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Space;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;

import de.georgsieber.customerdb.importexport.CustomerCsvBuilder;
import de.georgsieber.customerdb.importexport.CustomerVcfBuilder;
import de.georgsieber.customerdb.model.CustomField;
import de.georgsieber.customerdb.model.Customer;
import de.georgsieber.customerdb.print.CustomerPrintDocumentAdapter;
import de.georgsieber.customerdb.tools.BitmapCompressor;
import de.georgsieber.customerdb.tools.ColorControl;
import de.georgsieber.customerdb.tools.CommonDialog;
import de.georgsieber.customerdb.tools.StorageControl;
import de.georgsieber.customerdb.tools.DateControl;


public class CustomerDetailsActivity extends AppCompatActivity {
    private CustomerDatabase mDb;
    Customer mCurrentCustomer = null;
    CustomerDetailsActivity me;
    SharedPreferences mSettings;

    private final static int PICK_CONSENT_IMAGE_REQUEST = 1;
    private final static int TAKE_CONSENT_IMAGE_REQUEST = 3;
    private final static int DRAW_CONSENT_IMAGE_REQUEST = 4;
    private final static int EDIT_CUSTOMER_REQUEST = 2;

    TextView mTextViewName;
    TextView mTextViewPhoneHome;
    TextView mTextViewPhoneMobile;
    TextView mTextViewPhoneWork;
    TextView mTextViewEmail;
    TextView mTextViewAddress;
    TextView mTextViewGroup;
    TextView mTextViewNotes;
    TextView mTextViewNewsletter;
    TextView mTextViewBirthday;
    TextView mTextViewLastChanged;

    ImageButton mButtonPhoneHomeMore;
    ImageButton mButtonPhoneMobileMore;
    ImageButton mButtonPhoneWorkMore;
    ImageButton mButtonEmailMore;
    ImageButton mButtonAddressMore;
    ImageButton mButtonGroupMore;
    ImageButton mButtonNotesMore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // init settings
        mSettings = getSharedPreferences(MainActivity.PREFS_NAME, 0);

        // init activity view
        super.onCreate(savedInstanceState);
        me = this;
        setContentView(R.layout.activity_customer_details);
        setSupportActionBar((Toolbar)findViewById(R.id.toolbarView));
        if(getSupportActionBar() != null) {
            getSupportActionBar().setTitle(getResources().getString(R.string.detailview));
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // local database init
        mDb = new CustomerDatabase(this);

        // init colors
        ColorControl.updateActionBarColor(this, mSettings);
        ColorControl.updateAccentColor(findViewById(R.id.fabEdit), mSettings);

        // find views
        mTextViewName = findViewById(R.id.textViewName);
        mTextViewPhoneHome = findViewById(R.id.textViewPhoneHome);
        mTextViewPhoneMobile = findViewById(R.id.textViewPhoneMobile);
        mTextViewPhoneWork = findViewById(R.id.textViewPhoneWork);
        mTextViewEmail = findViewById(R.id.textViewEmail);
        mTextViewAddress = findViewById(R.id.textViewAddress);
        mTextViewGroup = findViewById(R.id.textViewGroup);
        mTextViewNotes = findViewById(R.id.textViewNotes);
        mTextViewNewsletter = findViewById(R.id.textViewAdditional);
        mTextViewBirthday = findViewById(R.id.textViewBirthday);
        mTextViewLastChanged = findViewById(R.id.textViewLastModified);
        mButtonPhoneHomeMore = findViewById(R.id.buttonPhoneHomeMore);
        mButtonPhoneMobileMore = findViewById(R.id.buttonPhoneMobileMore);
        mButtonPhoneWorkMore = findViewById(R.id.buttonPhoneWorkMore);
        mButtonEmailMore = findViewById(R.id.buttonEmailMore);
        mButtonAddressMore = findViewById(R.id.buttonAddressMore);
        mButtonGroupMore = findViewById(R.id.buttonGroupMore);
        mButtonNotesMore = findViewById(R.id.buttonNotesMore);

        // init context menus
        View.OnClickListener contextMenuClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.showContextMenu();
            }
        };
        mButtonPhoneHomeMore.setOnClickListener(contextMenuClickListener);
        mButtonPhoneMobileMore.setOnClickListener(contextMenuClickListener);
        mButtonPhoneWorkMore.setOnClickListener(contextMenuClickListener);
        mButtonEmailMore.setOnClickListener(contextMenuClickListener);
        mButtonAddressMore.setOnClickListener(contextMenuClickListener);
        mButtonGroupMore.setOnClickListener(contextMenuClickListener);
        mButtonNotesMore.setOnClickListener(contextMenuClickListener);
        registerForContextMenu(mButtonPhoneHomeMore);
        registerForContextMenu(mButtonPhoneMobileMore);
        registerForContextMenu(mButtonPhoneWorkMore);
        registerForContextMenu(mButtonEmailMore);
        registerForContextMenu(mButtonAddressMore);
        registerForContextMenu(mButtonGroupMore);
        registerForContextMenu(mButtonNotesMore);

        // hide fields
        if(!mSettings.getBoolean("show-phone-field", true)) {
            findViewById(R.id.linearLayoutPhone).setVisibility(View.GONE);
        }
        if(!mSettings.getBoolean("show-email-field", true)) {
            findViewById(R.id.linearLayoutEmail).setVisibility(View.GONE);
        }
        if(!mSettings.getBoolean("show-address-field", true)) {
            findViewById(R.id.linearLayoutAddress).setVisibility(View.GONE);
        }
        if(!mSettings.getBoolean("show-group-field", true)) {
            findViewById(R.id.linearLayoutGroup).setVisibility(View.GONE);
        }
        if(!mSettings.getBoolean("show-notes-field", true)) {
            findViewById(R.id.linearLayoutNotes).setVisibility(View.GONE);
        }
        if(!mSettings.getBoolean("show-newsletter-field", true)) {
            findViewById(R.id.linearLayoutNewsletter).setVisibility(View.GONE);
        }
        if(!mSettings.getBoolean("show-birthday-field", true)) {
            findViewById(R.id.linearLayoutBirthday).setVisibility(View.GONE);
        }

        // init fab
        FloatingActionButton fab = findViewById(R.id.fabEdit);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent myIntent = new Intent(me, CustomerEditActivity.class);
                myIntent.putExtra("customer", mCurrentCustomer);
                me.startActivityForResult(myIntent, EDIT_CUSTOMER_REQUEST);
            }
        });

        // fill fields
        Intent intent = getIntent();
        mCurrentCustomer = mDb.getCustomerById( // load current values from database with images
                ((Customer)intent.getParcelableExtra("customer")).mId
        );
        if(mCurrentCustomer == null) {
            finish();
        } else {
            mCurrentCustomer = mDb.readCustomerImages(mCurrentCustomer);
            createListEntries(mCurrentCustomer);
        }
    }

    @Override
    public void onDestroy() {
        mDb.close();
        super.onDestroy();
    }

    @Override
    public void finish() {
        // report MainActivity to update customer list
        if(changed) {
            Intent output = new Intent();
            output.putExtra("action", "update");
            setResult(RESULT_OK, output);
        }
        super.finish();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode) {
            case(TAKE_CONSENT_IMAGE_REQUEST) : {
                if(resultCode == RESULT_OK && data != null && data.getExtras() != null) {
                    try {
                        Log.e("extra", "START");
                        Bitmap photo = (Bitmap) data.getExtras().get("data");
                        ByteArrayOutputStream stream = new ByteArrayOutputStream();
                        photo.compress(Bitmap.CompressFormat.JPEG, 90, stream);
                        byte[] byteArray = stream.toByteArray();
                        photo.recycle();
                        mCurrentCustomer.mConsentImage = byteArray;
                        Log.e("extra", byteArray.length+"");
                        setUpdateAfterFinish();
                    } catch(Exception ignored) {}
                }
                break;
            }
            case(PICK_CONSENT_IMAGE_REQUEST) : {
                if(resultCode == RESULT_OK && data != null && data.getData() != null) {
                    mCurrentCustomer.mConsentImage = getDataBytes(data);
                    setUpdateAfterFinish();
                }
                break;
            }
            case(DRAW_CONSENT_IMAGE_REQUEST) : {
                if(resultCode == RESULT_OK && data != null && data.getExtras() != null) {
                    mCurrentCustomer.mConsentImage = android.util.Base64.decode(
                            data.getExtras().getString("image"), Base64.DEFAULT
                    );
                    setUpdateAfterFinish();
                }
                break;
            }
            case(EDIT_CUSTOMER_REQUEST) : {
                if(resultCode == Activity.RESULT_OK) {
                    mCurrentCustomer = data.getParcelableExtra("customer");
                    setUpdateAfterFinish();
                }
                break;
            }
        }
    }

    private byte[] getDataBytes(Intent data) {
        try {
            InputStream inputStream = this.getContentResolver().openInputStream(data.getData());
            byte[] targetArray = new byte[inputStream.available()];
            inputStream.read(targetArray);

            // write temp image file and scan it
            File fl = StorageControl.getStorageImageTemp(this);
            FileOutputStream stream = new FileOutputStream(fl);
            stream.write(targetArray);
            stream.flush(); stream.close();
            StorageControl.scanFile(fl, this);

            // compress image
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            BitmapCompressor.getSmallBitmap(fl).compress(Bitmap.CompressFormat.JPEG, 25, out);

            // is compressed image smaller than original?
            if(out.toByteArray().length > targetArray.length) {
                return targetArray;
            } else {
                return out.toByteArray();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new byte[0];
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_customer_details, menu);
        if(mCurrentCustomer != null && mCurrentCustomer.mId != -1) // show mId in menu
            menu.findItem(R.id.action_id).setTitle( "ID: " + mCurrentCustomer.mId );
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.action_consent:
                consentMenu();
                return true;
            case R.id.action_remove:
                confirmRemove();
                return true;
            case R.id.action_export:
                this.export();
                return true;
            case R.id.action_print:
                print();
                return true;
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("changed", changed);
    }
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        changed = savedInstanceState.getBoolean("changed");
    }

    private final static int CMI_PHOME_CALL = 1;
    private final static int CMI_PHOME_MSG = 2;
    private final static int CMI_PHOME_CPY = 3;
    private final static int CMI_PMOBILE_CALL = 4;
    private final static int CMI_PMOBILE_MSG = 5;
    private final static int CMI_PMOBILE_CPY = 6;
    private final static int CMI_PWORK_CALL = 7;
    private final static int CMI_PWORK_MSG = 8;
    private final static int CMI_PWORK_CPY = 9;
    private final static int CMI_EMAIL_MSG = 10;
    private final static int CMI_EMAIL_CPY = 11;
    private final static int CMI_ADDRESS_MAP = 12;
    private final static int CMI_ADDRESS_CPY = 13;
    private final static int CMI_GROUP_CPY = 14;
    private final static int CMI_NOTES_CPY = 15;

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        if(v == mButtonPhoneHomeMore) {
            menu.setHeaderTitle(mCurrentCustomer.mPhoneHome);
            menu.add(0, CMI_PHOME_CALL, 0, getString(R.string.do_call));
            menu.add(0, CMI_PHOME_MSG, 0, getString(R.string.send_message));
            menu.add(0, CMI_PHOME_CPY, 0, getString(R.string.copy_to_clipboard));
        }
        else if(v == mButtonPhoneMobileMore) {
            menu.setHeaderTitle(mCurrentCustomer.mPhoneMobile);
            menu.add(0, CMI_PMOBILE_CALL, 0, getString(R.string.do_call));
            menu.add(0, CMI_PMOBILE_MSG, 0, getString(R.string.send_message));
            menu.add(0, CMI_PMOBILE_CPY, 0, getString(R.string.copy_to_clipboard));
        }
        else if(v == mButtonPhoneWorkMore) {
            menu.setHeaderTitle(mCurrentCustomer.mPhoneWork);
            menu.add(0, CMI_PWORK_CALL, 0, getString(R.string.do_call));
            menu.add(0, CMI_PWORK_MSG, 0, getString(R.string.send_message));
            menu.add(0, CMI_PWORK_CPY, 0, getString(R.string.copy_to_clipboard));
        }
        else if(v == mButtonEmailMore) {
            menu.setHeaderTitle(mCurrentCustomer.mEmail);
            menu.add(0, CMI_EMAIL_MSG, 0, getString(R.string.send_message));
            menu.add(0, CMI_EMAIL_CPY, 0, getString(R.string.copy_to_clipboard));
        }
        else if(v == mButtonAddressMore) {
            menu.setHeaderTitle(mCurrentCustomer.getAddress());
            menu.add(0, CMI_ADDRESS_MAP, 0, getString(R.string.show_on_map));
            menu.add(0, CMI_ADDRESS_CPY, 0, getString(R.string.copy_to_clipboard));
        }
        else if(v == mButtonGroupMore) {
            menu.setHeaderTitle(mCurrentCustomer.mCustomerGroup);
            menu.add(0, CMI_GROUP_CPY, 0, getString(R.string.copy_to_clipboard));
        }
        else if(v == mButtonNotesMore) {
            menu.setHeaderTitle(mCurrentCustomer.mNotes);
            menu.add(0, CMI_NOTES_CPY, 0, getString(R.string.copy_to_clipboard));
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case CMI_PHOME_CALL:
                startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + mCurrentCustomer.mPhoneHome.replaceAll("[^\\d]", ""))));
                return true;
            case CMI_PHOME_MSG:
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.fromParts("sms", mCurrentCustomer.mPhoneHome, null)));
                return true;
            case CMI_PHOME_CPY:
                toClipboard(mCurrentCustomer.mPhoneHome);
                return true;

            case CMI_PMOBILE_CALL:
                startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + mCurrentCustomer.mPhoneMobile.replaceAll("[^\\d]", ""))));
                return true;
            case CMI_PMOBILE_MSG:
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.fromParts("sms", mCurrentCustomer.mPhoneMobile, null)));
                return true;
            case CMI_PMOBILE_CPY:
                toClipboard(mCurrentCustomer.mPhoneMobile);
                return true;

            case CMI_PWORK_CALL:
                startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + mCurrentCustomer.mPhoneWork.replaceAll("[^\\d]", ""))));
                return true;
            case CMI_PWORK_MSG:
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.fromParts("sms", mCurrentCustomer.mPhoneWork, null)));
                return true;
            case CMI_PWORK_CPY:
                toClipboard(mCurrentCustomer.mPhoneWork);
                return true;

            case CMI_EMAIL_MSG:
                onClickEmailLink(null);
                return true;
            case CMI_EMAIL_CPY:
                toClipboard(mCurrentCustomer.mEmail);
                return true;

            case CMI_ADDRESS_MAP:
                try {
                    Uri gmmIntentUri = Uri.parse("geo:0,0?q="+ mCurrentCustomer.getAddress());
                    Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                    mapIntent.setPackage("com.google.android.apps.maps");
                    startActivity(mapIntent);
                } catch(Exception ex) {
                    ex.printStackTrace();
                }
                return true;
            case CMI_ADDRESS_CPY:
                toClipboard(mCurrentCustomer.getAddress());
                return true;

            case CMI_GROUP_CPY:
                toClipboard(mCurrentCustomer.mCustomerGroup);
                return true;

            case CMI_NOTES_CPY:
                toClipboard(mCurrentCustomer.mNotes);
                return true;
        }
        return false;
    }

    private boolean changed = false;
    private void setUpdateAfterFinish() {
        // update last modified
        changed = true;
        mCurrentCustomer.mLastModified = new Date();

        // update in database
        mDb.updateCustomer(mCurrentCustomer);

        // update view
        createListEntries(mCurrentCustomer);
    }

    private void print() {
        if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            PrintManager printManager = (PrintManager) this.getSystemService(Context.PRINT_SERVICE);
            if(printManager != null) {
                String jobName = "Customer Database";
                PrintAttributes pa = new PrintAttributes.Builder()
                        .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                        .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                        .setResolution(new PrintAttributes.Resolution("customerdb", PRINT_SERVICE, 300, 300))
                        .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                        .build();
                printManager.print(jobName, new CustomerPrintDocumentAdapter(this, mCurrentCustomer), pa);
            }
        } else {
            CommonDialog.show(this,getResources().getString(R.string.not_supported), getResources().getString(R.string.not_supported_printing), CommonDialog.TYPE.FAIL, false);
        }
    }

    private void export() {
        final Dialog ad = new Dialog(this);
        ad.requestWindowFeature(Window.FEATURE_NO_TITLE);
        ad.setContentView(R.layout.dialog_export_single);
        ad.findViewById(R.id.buttonExportSingleCSV).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ad.dismiss();
                boolean sendMail = ((CheckBox) ad.findViewById(R.id.checkBoxExportSendEmailSingle)).isChecked();
                if(new CustomerCsvBuilder(mCurrentCustomer, mDb.getCustomFields()).saveCsvFile(getStorageExportCSV())) {
                    if(sendMail) {
                        emailFile(getStorageExportCSV());
                    } else {
                        CommonDialog.show(me, getResources().getString(R.string.export_ok), getStorageExportCSV().getPath(), CommonDialog.TYPE.OK, false);
                    }
                } else {
                    CommonDialog.show(me, getResources().getString(R.string.export_fail), getStorageExportCSV().getPath(), CommonDialog.TYPE.FAIL, false);
                }
                StorageControl.scanFile(getStorageExportCSV(), me);
            }
        });
        ad.findViewById(R.id.buttonExportSingleVCF).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ad.dismiss();
                boolean sendMail = ((CheckBox) ad.findViewById(R.id.checkBoxExportSendEmailSingle)).isChecked();
                if(new CustomerVcfBuilder(mCurrentCustomer).saveVcfFile(getStorageExportVCF())) {
                    if(sendMail) {
                        emailFile(getStorageExportVCF());
                    } else {
                        CommonDialog.show(me, getResources().getString(R.string.export_ok), getStorageExportVCF().getPath(), CommonDialog.TYPE.OK, false);
                    }
                } else {
                    CommonDialog.show(me, getResources().getString(R.string.export_fail), getStorageExportVCF().getPath(), CommonDialog.TYPE.FAIL, false);
                }
                StorageControl.scanFile(getStorageExportVCF(), me);
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
        return new File(exportDir, "export."+ mCurrentCustomer.mId+".csv");
    }
    private File getStorageExportVCF() {
        File exportDir = new File(getExternalFilesDir(null), "export");
        exportDir.mkdirs();
        return new File(exportDir, "export."+ mCurrentCustomer.mId+".vcf");
    }

    private void emailFile(File f) {
        Uri attachmentUri = FileProvider.getUriForFile(
                this, "de.georgsieber.customerdb.provider", f
        );
        // this opens app chooser instead of system mEmail app
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("message/rfc822");
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{mCurrentCustomer.mEmail});
        intent.putExtra(Intent.EXTRA_SUBJECT,
                mSettings.getString("email-export-subject", getResources().getString(R.string.email_export_subject_template))
        );
        intent.putExtra(Intent.EXTRA_TEXT,
                mSettings.getString("email-export-template", getResources().getString(R.string.email_export_text_template))
                        .replace("CUSTOMER", mCurrentCustomer.getFullName(false)) + "\n\n"
        );
        intent.putExtra(Intent.EXTRA_STREAM, attachmentUri);
        startActivity(Intent.createChooser(intent, getResources().getString(R.string.emailtocustomer)));
    }

    private void confirmRemove() {
        AlertDialog.Builder ad = new AlertDialog.Builder(me);
        ad.setPositiveButton(getResources().getString(R.string.delete), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                mDb.removeCustomer(mCurrentCustomer);
                changed = false;
                Intent output = new Intent();
                output.putExtra("action", "update");
                setResult(RESULT_OK, output);
                finish();
            }});
        ad.setNegativeButton(getResources().getString(R.string.abort), null);
        ad.setTitle(getResources().getString(R.string.reallydelete_title));
        ad.setMessage(getResources().getString(R.string.reallydelete));
        ad.setIcon(getResources().getDrawable(R.drawable.remove));
        ad.show();
    }

    private void confirmRemoveConsent() {
        new AlertDialog.Builder(this)
                .setMessage(getResources().getString(R.string.consent_remove_text))
                .setPositiveButton(getResources().getString(R.string.delete), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        mCurrentCustomer.mConsentImage = new byte[0];
                        setUpdateAfterFinish();
                    }})
                .setNegativeButton(getResources().getString(R.string.abort), null).show();
    }

    private void consentMenu() {
        if(mCurrentCustomer.getConsent().length == 0) {

            final Dialog ad = new Dialog(this);
            ad.requestWindowFeature(Window.FEATURE_NO_TITLE);
            ad.setContentView(R.layout.dialog_consentmenu_create);
            ad.findViewById(R.id.buttonConsentFromCamera).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ad.dismiss();
                    Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    /*
                    Uri photoURI = FileProvider.getUriForFile(me,
                            "systems.sieber.itinventory.tmpimgprovider",
                            StorageManager.getTempImageStorage(me)
                    );
                    cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                    */
                    me.startActivityForResult(cameraIntent, CustomerDetailsActivity.TAKE_CONSENT_IMAGE_REQUEST);
                }
            });
            ad.findViewById(R.id.buttonConsentFromGallery).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ad.dismiss();
                    Intent intent = new Intent();
                    intent.setType("image/*");
                    intent.setAction(Intent.ACTION_GET_CONTENT);
                    startActivityForResult(Intent.createChooser(intent, getString(R.string.choose_from_gallery)), PICK_CONSENT_IMAGE_REQUEST);
                }
            });
            ad.findViewById(R.id.buttonConsentFromTouchscreen).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ad.dismiss();
                    Intent intent = new Intent(me, DrawActivity.class);
                    startActivityForResult(intent, CustomerDetailsActivity.DRAW_CONSENT_IMAGE_REQUEST);
                }
            });
            ad.findViewById(R.id.buttonConsentCancel).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ad.dismiss();
                }
            });
            ad.show();

        } else {

            final Dialog ad = new Dialog(this);
            ad.requestWindowFeature(Window.FEATURE_NO_TITLE);
            ad.setContentView(R.layout.dialog_consentmenu_exists);
            ad.findViewById(R.id.buttonConsentExistsView).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ad.dismiss();
                    if(mCurrentCustomer.getConsent().length != 0) {
                        try {
                            FileOutputStream stream = new FileOutputStream(StorageControl.getStorageImageTemp(me));
                            stream.write(mCurrentCustomer.getConsent());
                            stream.flush(); stream.close();

                            Uri openUri = FileProvider.getUriForFile(
                                    me,
                                    "de.georgsieber.customerdb.provider",
                                    StorageControl.getStorageImageTemp(me)
                            );

                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            intent.setDataAndType(openUri, "image/*");
                            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            startActivity(intent);
                            //Bitmap bitmap = BitmapFactory.decodeByteArray(mCurrentCustomer.mConsentImage, 0, mCurrentCustomer.mConsentImage.length);
                            //mImage.setImageBitmap(bitmap);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        StorageControl.scanFile(StorageControl.getStorageImageTemp(me), me);
                    } else {
                        CommonDialog.show(me, "", getResources().getString(R.string.zero_data), CommonDialog.TYPE.FAIL, false);
                    }
                }
            });
            ad.findViewById(R.id.buttonConsentExistsRemove).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ad.dismiss();
                    confirmRemoveConsent();
                }
            });
            ad.findViewById(R.id.buttonConsentExistsCancel).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ad.dismiss();
                }
            });
            ad.show();

        }
    }

    private void createListEntries(Customer c) {
        String additionalInfo = "";
        if(c.mNewsletter)
            additionalInfo += getResources().getString(R.string.yes);
        else
            additionalInfo += getResources().getString(R.string.no);
        additionalInfo += " / ";
        if(c.getConsent().length == 0)
            additionalInfo += getResources().getString(R.string.no);
        else
            additionalInfo += getResources().getString(R.string.yes);

        ((TextView) findViewById(R.id.textViewName)).setText( c.getFullName(false) );
        ((TextView) findViewById(R.id.textViewPhoneHome)).setText( c.mPhoneHome );
        ((TextView) findViewById(R.id.textViewPhoneMobile)).setText( c.mPhoneMobile );
        ((TextView) findViewById(R.id.textViewPhoneWork)).setText( c.mPhoneWork );
        ((TextView) findViewById(R.id.textViewEmail)).setText( c.mEmail );
        ((TextView) findViewById(R.id.textViewAddress)).setText( c.getAddress() );
        ((TextView) findViewById(R.id.textViewBirthday)).setText( c.getBirthdayString() );
        ((TextView) findViewById(R.id.textViewLastModified)).setText( DateControl.displayDateFormat.format(c.mLastModified) );
        ((TextView) findViewById(R.id.textViewNotes)).setText( c.mNotes );
        ((TextView) findViewById(R.id.textViewGroup)).setText( c.mCustomerGroup );
        ((TextView) findViewById(R.id.textViewAdditional)).setText( additionalInfo );

        if(c.mPhoneHome.equals("")) mButtonPhoneHomeMore.setEnabled(false);
        else mButtonPhoneHomeMore.setEnabled(true);

        if(c.mPhoneMobile.equals("")) mButtonPhoneMobileMore.setEnabled(false);
        else mButtonPhoneMobileMore.setEnabled(true);

        if(c.mPhoneWork.equals("")) mButtonPhoneWorkMore.setEnabled(false);
        else mButtonPhoneWorkMore.setEnabled(true);

        if(c.mEmail.equals("")) mButtonEmailMore.setEnabled(false);
        else mButtonEmailMore.setEnabled(true);

        if(c.getAddress().equals("")) mButtonAddressMore.setEnabled(false);
        else mButtonAddressMore.setEnabled(true);

        if(c.mCustomerGroup.equals("")) mButtonGroupMore.setEnabled(false);
        else mButtonGroupMore.setEnabled(true);

        if(c.mNotes.equals("")) mButtonNotesMore.setEnabled(false);
        else mButtonNotesMore.setEnabled(true);

        // fake link... we handle the email click event not with autolink, because this launches always the system email app on huawei devices :-(
        final CharSequence text = ((TextView) findViewById(R.id.textViewEmail)).getText();
        final SpannableString spannableString = new SpannableString( text );
        spannableString.setSpan(new URLSpan(""), 0, spannableString.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        ((TextView) findViewById(R.id.textViewEmail)).setText(spannableString, TextView.BufferType.SPANNABLE);

        // custom fields
        final float scale = getResources().getDisplayMetrics().density;
        LinearLayout linearLayout = findViewById(R.id.linearLayoutCustomFieldsView);
        linearLayout.removeAllViews();
        List<CustomField> customFields = mDb.getCustomFields();
        if(customFields.size() > 0) linearLayout.setVisibility(View.VISIBLE);
        for(CustomField cf : customFields) {
            TextView descriptionView = new TextView(this);
            descriptionView.setText(cf.mTitle);
            descriptionView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,LinearLayout.LayoutParams.WRAP_CONTENT));
            String value = c.getCustomField(cf.mTitle);

            TextView valueView = new TextView(this);
            valueView.setTextIsSelectable(true);
            if(value != null) {
                String displayValue = value;
                if(cf.mType == 3) {
                    // try parse value from storage (normalized) format and show it in local format
                    try {
                        Date selectedDate = CustomerDatabase.storageFormatWithTime.parse(value);
                        displayValue = DateControl.birthdayDateFormat.format(selectedDate);
                    } catch(Exception ignored) {}
                }
                valueView.setText(displayValue);
            }
            valueView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,LinearLayout.LayoutParams.WRAP_CONTENT));
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                valueView.setTextAppearance(R.style.TextAppearance_AppCompat);
            }
            valueView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);

            View spaceView = new Space(this);
            spaceView.setLayoutParams(new LinearLayout.LayoutParams(0, (int)(20/*dp*/ * scale + 0.5f)));
            linearLayout.addView(spaceView);

            linearLayout.addView(descriptionView);
            linearLayout.addView(valueView);
        }

        // customer image
        if(mCurrentCustomer.getImage().length != 0) {
            Bitmap bitmap = BitmapFactory.decodeByteArray(mCurrentCustomer.getImage(), 0, mCurrentCustomer.getImage().length);
            ((ImageView) findViewById(R.id.imageViewCustomerImage)).setImageBitmap(bitmap);
        } else {
            ((ImageView) findViewById(R.id.imageViewCustomerImage)).setImageDrawable(getResources().getDrawable(R.drawable.ic_person_black_96dp));
        }
    }

    public void onClickEmailLink(View v) {
        // this opens app chooser instead of system mEmail app
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("message/rfc822");
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{mCurrentCustomer.mEmail});
        intent.putExtra(Intent.EXTRA_SUBJECT, mSettings.getString("email-subject", getResources().getString(R.string.email_subject_template)));
        intent.putExtra(Intent.EXTRA_TEXT,
                mSettings.getString("email-template", getResources().getString(R.string.email_text_template))
                        .replace("CUSTOMER", mCurrentCustomer.getFullName(false)) + "\n\n"
        );
        startActivity(Intent.createChooser(intent, getResources().getString(R.string.emailtocustomer)));
    }

    private void toClipboard(String text) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("phone", text);
        if (clipboard != null) {
            clipboard.setPrimaryClip(clip);
        }
        Snackbar.make(findViewById(R.id.fabEdit), getResources().getString(R.string.copied_to_clipboard), Snackbar.LENGTH_SHORT)
                .setAction("Action", null)
                .show();
    }

}
