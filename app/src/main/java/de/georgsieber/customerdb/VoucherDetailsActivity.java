package de.georgsieber.customerdb;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.print.PrintAttributes;
import android.print.PrintManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.Date;

import de.georgsieber.customerdb.model.Voucher;
import de.georgsieber.customerdb.print.VoucherPrintDocumentAdapter;
import de.georgsieber.customerdb.tools.ColorControl;
import de.georgsieber.customerdb.tools.CommonDialog;
import de.georgsieber.customerdb.tools.NumTools;

public class VoucherDetailsActivity extends AppCompatActivity {

    private VoucherDetailsActivity me;

    private CustomerDatabase mDb;
    private long mCurrentVoucherId = -1;
    private Voucher mCurrentVoucher = null;
    private SharedPreferences mSettings;
    private String currency = "?";
    private boolean mChanged = false;

    TextView mTextViewCurrentValue;
    TextView mTextViewOriginalValue;
    TextView mTextViewVoucherNo;
    TextView mTextViewFromCustomer;
    TextView mTextViewForCustomer;
    TextView mTextViewNotes;
    TextView mTextViewIssued;
    TextView mTextViewValidUntil;
    TextView mTextViewRedeemed;
    TextView mTextViewLastModified;

    private final static int EDIT_VOUCHER_REQUEST = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // init settings
        mSettings = getSharedPreferences(MainActivity.PREFS_NAME, 0);
        currency = mSettings.getString("currency", "€");

        // init activity view
        super.onCreate(savedInstanceState);
        me = this;
        setContentView(R.layout.activity_voucher_details);
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
        mTextViewCurrentValue = findViewById(R.id.textViewCurrentValue);
        mTextViewOriginalValue = findViewById(R.id.textViewOriginalValue);
        mTextViewVoucherNo = findViewById(R.id.textViewVoucherNo);
        mTextViewFromCustomer = findViewById(R.id.textViewFromCustomer);
        mTextViewForCustomer = findViewById(R.id.textViewForCustomer);
        mTextViewNotes = findViewById(R.id.textViewNotes);
        mTextViewIssued = findViewById(R.id.textViewIssued);
        mTextViewValidUntil = findViewById(R.id.textViewValidUntil);
        mTextViewRedeemed = findViewById(R.id.textViewRedeemed);
        mTextViewLastModified = findViewById(R.id.textViewLastModified);

        // init fab
        FloatingActionButton fab = findViewById(R.id.fabEdit);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent myIntent = new Intent(me, VoucherEditActivity.class);
                myIntent.putExtra("voucher-id", mCurrentVoucher.mId);
                me.startActivityForResult(myIntent, EDIT_VOUCHER_REQUEST);
            }
        });

        // get current voucher
        Intent intent = getIntent();
        mCurrentVoucherId = intent.getLongExtra("voucher-id", -1);
        loadVoucher();
    }

    private void loadVoucher() {
        // load current values from database
        mCurrentVoucher = mDb.getVoucherById(mCurrentVoucherId, false);
        if(mCurrentVoucher == null) {
            finish();
        } else {
            createListEntries(mCurrentVoucher);
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
        if(mChanged) {
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
            case(EDIT_VOUCHER_REQUEST) : {
                if(resultCode == Activity.RESULT_OK) {
                    mChanged = true;
                    loadVoucher();
                }
                break;
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_voucher_details, menu);
        if(mCurrentVoucher != null && mCurrentVoucher.mId != -1) // show mId in menu
            menu.findItem(R.id.action_id).setTitle( "ID: " + mCurrentVoucher.mId );
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.action_redeem:
                showRedeemVoucher(mCurrentVoucher);
                return true;
            case R.id.action_remove:
                confirmRemove();
                return true;
            //case R.mId.action_export: // not implemented yet
            //    this.export();
            //    return true;
            case R.id.action_print:
                print(mCurrentVoucher);
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
        outState.putBoolean("changed", mChanged);
    }
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mChanged = savedInstanceState.getBoolean("changed");
    }

    private void confirmRemove() {
        AlertDialog.Builder ad = new AlertDialog.Builder(me);
        ad.setPositiveButton(getResources().getString(R.string.delete), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                mDb.removeVoucher(mCurrentVoucher);
                mChanged = false;
                Intent output = new Intent();
                output.putExtra("action", "update");
                setResult(RESULT_OK, output);
                finish();
            }});
        ad.setNegativeButton(getResources().getString(R.string.abort), null);
        ad.setTitle(getResources().getString(R.string.reallydelete_title));
        ad.setMessage(getResources().getString(R.string.reallydelete_voucher));
        ad.setIcon(getResources().getDrawable(R.drawable.remove));
        ad.show();
    }

    @SuppressLint("SetTextI18n")
    private void createListEntries(Voucher v) {
        mTextViewCurrentValue.setText( v.getCurrentValueString()+" "+currency );
        mTextViewOriginalValue.setText( v.getOriginalValueString()+" "+currency );
        mTextViewVoucherNo.setText( v.mVoucherNo );
        mTextViewFromCustomer.setText( v.mFromCustomer );
        mTextViewForCustomer.setText( v.mForCustomer );
        mTextViewNotes.setText( v.mNotes );
        mTextViewIssued.setText( v.getIssuedString() );
        mTextViewValidUntil.setText( v.getValidUntilString() );
        mTextViewRedeemed.setText( v.getRedeemedString() );
        mTextViewLastModified.setText( v.getLastModifiedString() );
    }

    public void showRedeemVoucher(final Voucher vo) {
        final Dialog ad = new Dialog(this);
        ad.requestWindowFeature(Window.FEATURE_NO_TITLE);
        ad.setContentView(R.layout.dialog_voucher_redeem);
        ad.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);

        final EditText editTextVoucherNotes = ad.findViewById(R.id.editTextVoucherNewNotes);
        final EditText editTextValueRedeem = ad.findViewById(R.id.editTextVoucherValueRedeem);

        editTextVoucherNotes.setText(vo.mNotes);
        editTextValueRedeem.setText(vo.getCurrentValueString());
        editTextValueRedeem.selectAll();

        ad.findViewById(R.id.buttonVoucherRedeemOK).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ad.dismiss();

                String subtractValueString = editTextValueRedeem.getText().toString();
                Double subtractValue = NumTools.tryParseDouble(subtractValueString);
                if(subtractValue == null) {
                    CommonDialog.show(me, getResources().getString(R.string.invalid_number), getResources().getString(R.string.invalid_number_text), CommonDialog.TYPE.FAIL, false);
                } else {
                    vo.mCurrentValue -= subtractValue;
                    vo.mNotes = editTextVoucherNotes.getText().toString();
                    vo.mRedeemed = new Date();
                    vo.mLastModified = new Date();

                    mDb.updateVoucher(vo);
                    mChanged = true;
                    MainActivity.setUnsyncedChanges(me);
                    loadVoucher();
                }
            }
        });
        ad.findViewById(R.id.buttonVoucherRedeemClose).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ad.dismiss();
            }
        });
        ad.show();
    }

    private void print(Voucher v) {
        if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            PrintManager printManager = (PrintManager) this.getSystemService(Context.PRINT_SERVICE);
            if(printManager != null) {
                PrintAttributes.MediaSize ms = PrintAttributes.MediaSize.ISO_A5;
                String jobName = "Customer Database Voucher";
                PrintAttributes pa = new PrintAttributes.Builder()
                        .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                        .setMediaSize(ms.asLandscape())
                        .setResolution(new PrintAttributes.Resolution("customerdb", PRINT_SERVICE, 300, 300))
                        .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                        .build();
                printManager.print(jobName, new VoucherPrintDocumentAdapter(this, v, currency), pa);
            }
        } else {
            CommonDialog.show(this, getResources().getString(R.string.not_supported), getResources().getString(R.string.not_supported_printing), CommonDialog.TYPE.FAIL, false);
        }
    }

}
