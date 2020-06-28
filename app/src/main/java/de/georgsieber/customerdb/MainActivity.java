package de.georgsieber.customerdb;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.print.PrintAttributes;
import android.print.PrintManager;
import androidx.annotation.NonNull;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.Toolbar;

import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.SearchView;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import de.georgsieber.customerdb.importexport.CalendarCsvBuilder;
import de.georgsieber.customerdb.importexport.CalendarIcsBuilder;
import de.georgsieber.customerdb.importexport.CustomerCsvBuilder;
import de.georgsieber.customerdb.importexport.CustomerVcfBuilder;
import de.georgsieber.customerdb.importexport.VoucherCsvBuilder;
import de.georgsieber.customerdb.model.CustomField;
import de.georgsieber.customerdb.model.Customer;
import de.georgsieber.customerdb.model.CustomerAppointment;
import de.georgsieber.customerdb.model.CustomerCalendar;
import de.georgsieber.customerdb.model.Voucher;
import de.georgsieber.customerdb.print.CustomerPrintDocumentAdapter;
import de.georgsieber.customerdb.tools.ColorControl;
import de.georgsieber.customerdb.tools.CommonDialog;
import de.georgsieber.customerdb.tools.DateControl;
import de.georgsieber.customerdb.tools.StorageControl;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    MainActivity me = this;

    ListView mListViewCustomers;
    ListView mListViewVouchers;
    Menu mOptionMenu;
    Menu mDrawerMenu;
    NavigationView mNavigationView;
    BottomNavigationView mBottomNavigationView;
    Button mButtonCalendarViewDay;
    CalendarFragment mCalendarFragment;
    Calendar mCalendarViewCalendar = Calendar.getInstance();

    CustomerDatabase mDb;
    List<Customer> mCustomers = new ArrayList<>();
    List<Voucher> mVouchers = new ArrayList<>();
    private CustomerAdapter mCurrentCustomerAdapter;
    private VoucherAdapter mCurrentVoucherAdapter;
    private CustomerAdapter.checkedChangedListener mCustomerCheckedChangedListener;
    private VoucherAdapter.checkedChangedListener mVoucherCheckedChangedListener;

    private FeatureCheck mFc;
    private boolean isInputOnlyModeActive = false;
    private boolean isLockActive = false;

    int mRemoteDatabaseConnType = 0;
    String mRemoteDatabaseConnURL = "";
    String mRemoteDatabaseConnUsername = "";
    String mRemoteDatabaseConnPassword = "";
    String mCurrency = "?";
    String mIomPassword = "";
    long mCurrentCalendarImportSelectedId = -1;

    public static final String PREFS_NAME = "CustomerDBprefs";
    private SharedPreferences mSettings;

    private final static int NEW_CUSTOMER_REQUEST = 0;
    private final static int VIEW_CUSTOMER_REQUEST = 1;
    private final static int SETTINGS_REQUEST = 2;
    private final static int PICK_CUSTOMER_VCF_REQUEST = 4;
    private final static int PICK_CUSTOMER_CSV_REQUEST = 5;
    private final static int BIRTHDAY_REQUEST = 6;
    private final static int ABOUT_REQUEST = 7;
    private final static int NEW_VOUCHER_REQUEST = 8;
    private final static int VIEW_VOUCHER_REQUEST = 9;
    private final static int PICK_VOUCHER_CSV_REQUEST = 10;
    private final static int NEW_APPOINTMENT_REQUEST = 11;
    private final static int PICK_CALENDAR_ICS_REQUEST = 12;
    private final static int PICK_CALENDAR_CSV_REQUEST = 13;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // init settings
        mSettings = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // init activity view
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // init toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // init drawer menu
        DrawerLayout drawer = findViewById(R.id.drawerLayoutMain);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        mNavigationView = findViewById(R.id.navigationViewMain);
        mDrawerMenu = mNavigationView.getMenu();
        mNavigationView.setNavigationItemSelectedListener(this);

        // init local database
        mDb = new CustomerDatabase(this);

        // init bottom navigation
        mBottomNavigationView = findViewById(R.id.bottomNavigation);
        mBottomNavigationView.setOnNavigationItemSelectedListener(
                new BottomNavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                        refreshView(item.getItemId());
                        return true;
                    }
                }
        );
        refreshView(null);

        // load sync settings
        loadSettings();

        // init views
        mListViewCustomers = findViewById(R.id.mainCustomerList);
        mListViewCustomers.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Customer customer = (Customer) mListViewCustomers.getItemAtPosition(position);
                Intent myIntent = new Intent(me, CustomerDetailsActivity.class);
                myIntent.putExtra("customer-id", customer.mId);
                me.startActivityForResult(myIntent, VIEW_CUSTOMER_REQUEST);
            }
        });
        mListViewCustomers.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                mCurrentCustomerAdapter.setShowCheckbox(!mCurrentCustomerAdapter.getShowCheckbox());
                if(!mCurrentCustomerAdapter.getShowCheckbox()) {
                    mCurrentCustomerAdapter.setAllChecked(false);
                }
                return true;
            }
        });
        mCalendarFragment = (CalendarFragment) getSupportFragmentManager().findFragmentById(R.id.fragmentCalendar);
        mButtonCalendarViewDay = findViewById(R.id.buttonCalendarChangeDay);
        mListViewVouchers = findViewById(R.id.mainVoucherList);
        mListViewVouchers.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Voucher voucher = (Voucher) mListViewVouchers.getItemAtPosition(position);
                Intent myIntent = new Intent(me, VoucherDetailsActivity.class);
                myIntent.putExtra("voucher-id", voucher.mId);
                me.startActivityForResult(myIntent, VIEW_VOUCHER_REQUEST);
            }
        });
        mListViewVouchers.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                mCurrentVoucherAdapter.setShowCheckbox(!mCurrentVoucherAdapter.getShowCheckbox());
                if(!mCurrentVoucherAdapter.getShowCheckbox()) {
                    mCurrentVoucherAdapter.setAllChecked(false);
                }
                return true;
            }
        });
        mCustomerCheckedChangedListener = new CustomerAdapter.checkedChangedListener() {
            @Override
            public void checkedChanged(ArrayList<Customer> checked) {
                refreshSelectedCountInfo(null);
            }
        };
        mVoucherCheckedChangedListener = new VoucherAdapter.checkedChangedListener() {
            @Override
            public void checkedChanged(ArrayList<Voucher> checked) {
                refreshSelectedCountInfo(null);
            }
        };
        refreshCustomersFromLocalDatabase();
        refreshAppointmentsFromLocalDatabase();
        refreshVouchersFromLocalDatabase();

        // init add buttons (FloatingActionButtons)
        findViewById(R.id.fabAdd).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switch(mBottomNavigationView.getSelectedItemId()) {
                    case R.id.bottomnav_customers:
                        me.startActivityForResult(new Intent(me, CustomerEditActivity.class), NEW_CUSTOMER_REQUEST);
                        break;
                    case R.id.bottomnav_calendar:
                        Intent intent = new Intent(me, CalendarAppointmentEditActivity.class);
                        intent.putExtra("appointment-day", mCalendarViewCalendar);
                        me.startActivityForResult(intent, NEW_APPOINTMENT_REQUEST);
                        break;
                    case R.id.bottomnav_vouchers:
                        me.startActivityForResult(new Intent(me, VoucherEditActivity.class), NEW_VOUCHER_REQUEST);
                        break;
                }
            }
        });
        findViewById(R.id.buttonAddCustomerInputOnlyMode).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                me.startActivityForResult(new Intent(me, CustomerEditActivity.class), NEW_CUSTOMER_REQUEST);
            }
        });
        findViewById(R.id.buttonInputOnlyModeUnlock).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onToggleInputOnlyModeButtonClick();
            }
        });
        findViewById(R.id.buttonLockUnlock).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onToggleLockButtonClick();
            }
        });

        // show dialogs
        //dialogNews();
        dialogEula();
    }

    private void refreshView(Integer itemId) {
        if(itemId == null) itemId = mBottomNavigationView.getSelectedItemId();

        findViewById(R.id.mainCustomerList).setVisibility(View.INVISIBLE);
        findViewById(R.id.mainCalendarView).setVisibility(View.INVISIBLE);
        findViewById(R.id.mainVoucherList).setVisibility(View.INVISIBLE);
        if(isLockActive || isInputOnlyModeActive) {
            return;
        }
        boolean state = true;
        switch(itemId) {
            case R.id.bottomnav_customers:
                findViewById(R.id.mainCustomerList).setVisibility(View.VISIBLE);
                state = true;
                break;
            case R.id.bottomnav_calendar:
                findViewById(R.id.mainCalendarView).setVisibility(View.VISIBLE);
                state = false;
                break;
            case R.id.bottomnav_vouchers:
                findViewById(R.id.mainVoucherList).setVisibility(View.VISIBLE);
                state = false;
                break;
        }
        if(mDrawerMenu != null) {
            mDrawerMenu.findItem(R.id.nav_input_only_mode).setEnabled(state);
            mDrawerMenu.findItem(R.id.nav_lock).setEnabled(state);
            mDrawerMenu.findItem(R.id.nav_filter).setEnabled(state);
            mDrawerMenu.findItem(R.id.nav_sort).setEnabled(state);
            mDrawerMenu.findItem(R.id.nav_newsletter).setEnabled(state);
            mDrawerMenu.findItem(R.id.nav_birthdays).setEnabled(state);
        }
        refreshSelectedCountInfo(itemId);
    }

    void refreshCustomersFromLocalDatabase() {
        refreshCustomersFromLocalDatabase(null);
    }
    private void refreshCustomersFromLocalDatabase(String search) {
        if(mCurrentGroup == null && mCurrentCity == null && mCurrentCountry == null) {
            mCustomers = mDb.getCustomers(search, false, false);
            resetActionBarTitle();
        } else {
            List<Customer> newCustomerList = new ArrayList<>();
            for(Customer c : mDb.getCustomers(search, false, false)) {
                if((mCurrentGroup == null || c.mCustomerGroup.equals(mCurrentGroup)) &&
                        (mCurrentCity == null || c.mCity.equals(mCurrentCity)) &&
                        (mCurrentCountry == null || c.mCountry.equals(mCurrentCountry))
                ) {
                    newCustomerList.add(c);
                }
            }
            mCustomers = newCustomerList;
            if(getSupportActionBar() != null)
                getSupportActionBar().setTitle(
                        ((mCurrentGroup != null ? mCurrentGroup : "") + " " +
                                (mCurrentCity != null ? mCurrentCity : "") + " " +
                                (mCurrentCountry != null ? mCurrentCountry : "")).trim()
                );
        }

        mCurrentCustomerAdapter = new CustomerAdapter(this, mCustomers, mCustomerCheckedChangedListener);
        mListViewCustomers.setAdapter(mCurrentCustomerAdapter);
        refreshCount();
        refreshSelectedCountInfo(null);
    }
    void refreshAppointmentsFromLocalDatabase() {
        mCalendarFragment.show(mDb.getCalendars(false), mCalendarViewCalendar.getTime());
        mButtonCalendarViewDay.setText(DateControl.birthdayDateFormat.format(mCalendarViewCalendar.getTime()));
    }
    void refreshVouchersFromLocalDatabase() {
        refreshVouchersFromLocalDatabase(null);
    }
    private void refreshVouchersFromLocalDatabase(String search) {
        mVouchers = mDb.getVouchers(search, false);
        resetActionBarTitle();

        mCurrentVoucherAdapter = new VoucherAdapter(this, mVouchers, mCurrency, mVoucherCheckedChangedListener);
        mListViewVouchers.setAdapter(mCurrentVoucherAdapter);
        refreshCount();
        refreshSelectedCountInfo(null);
    }

    public void onClickChangeCalendarViewDay(View v) {
        final DatePickerDialog.OnDateSetListener date = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                mCalendarViewCalendar.set(Calendar.YEAR, year);
                mCalendarViewCalendar.set(Calendar.MONTH, monthOfYear);
                mCalendarViewCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                refreshAppointmentsFromLocalDatabase();
            }
        };
        new DatePickerDialog(
                this, date,
                mCalendarViewCalendar.get(Calendar.YEAR),
                mCalendarViewCalendar.get(Calendar.MONTH),
                mCalendarViewCalendar.get(Calendar.DAY_OF_MONTH)
        ).show();
    }
    public void onClickPrevCalendarViewDay(View v) {
        mCalendarViewCalendar.add(Calendar.DAY_OF_MONTH, -1);
        refreshAppointmentsFromLocalDatabase();
    }
    public void onClickNextCalendarViewDay(View v) {
        mCalendarViewCalendar.add(Calendar.DAY_OF_MONTH, 1);
        refreshAppointmentsFromLocalDatabase();
    }

    private void loadSettings() {
        // restore remote database connection preferences
        mRemoteDatabaseConnType = mSettings.getInt("webapi-type", 0);
        mRemoteDatabaseConnURL = mSettings.getString("webapi-url", "");
        mRemoteDatabaseConnUsername = mSettings.getString("webapi-username", "");
        mRemoteDatabaseConnPassword = mSettings.getString("webapi-password", "");
        mCurrency = mSettings.getString("currency", "€");
        mIomPassword = mSettings.getString("iom-password", "");
        isInputOnlyModeActive = mSettings.getBoolean("iom", false);
        isLockActive = mSettings.getBoolean("locked", false);

        // migrate legacy API settings
        if(mRemoteDatabaseConnType == 0 && !mRemoteDatabaseConnURL.equals("") && !mRemoteDatabaseConnPassword.equals("") && mRemoteDatabaseConnUsername.equals("")) {
            SharedPreferences.Editor editor = mSettings.edit();
            editor.putInt("webapi-type", 3);
            editor.apply();
            mRemoteDatabaseConnType = 3;
        }

        // init colors
        ColorControl.updateActionBarColor(this, mSettings);
        ColorControl.updateAccentColor(findViewById(R.id.mainInputOnlyOverlay), mSettings);
        ColorControl.updateAccentColor(findViewById(R.id.mainLockOverlay), mSettings);
        ColorControl.updateAccentColor(findViewById(R.id.mainStartupOverlay), mSettings);
        ColorControl.updateAccentColor(findViewById(R.id.fabAdd), mSettings);
        ColorStateList colorStates = new ColorStateList(
                new int[][] {
                        new int[] {-android.R.attr.state_checked},
                        new int[] { android.R.attr.state_checked}
                },
                new int[] {
                        getResources().getColor(R.color.bottomNavigationViewInactiveColor),
                        ColorControl.getColorFromSettings(mSettings)
                });
        mBottomNavigationView.setItemTextColor(colorStates);
        mBottomNavigationView.setItemIconTintList(colorStates);
        View headerView = mNavigationView.getHeaderView(0);
        if(headerView != null)
            ColorControl.updateAccentColor(headerView.findViewById(R.id.linearLayoutDrawerNavHeader), mSettings);

        // refresh backup note
        if(headerView != null) {
            if(mRemoteDatabaseConnType == 0) {
                headerView.findViewById(R.id.linearLayoutDrawerBackupNote).setVisibility(View.VISIBLE);
            } else {
                headerView.findViewById(R.id.linearLayoutDrawerBackupNote).setVisibility(View.GONE);
            }
        }

        // load in-app purchases
        mFc = new FeatureCheck(this);
        mFc.setFeatureCheckReadyListener(new FeatureCheck.featureCheckReadyListener() {
            @Override
            public void featureCheckReady(boolean fetchSuccess) {
                if(mFc.unlockedCommercialUsage) {
                    (findViewById(R.id.textViewInputOnlyModeNotLicensed)).setVisibility(View.GONE);
                }
            }
        });
        mFc.init();

        // init logo image
        File logo = StorageControl.getStorageLogo(this);
        if(logo.exists()) {
            try {
                Bitmap myBitmap = BitmapFactory.decodeFile(logo.getAbsolutePath());
                ((ImageView) findViewById(R.id.imageViewInputOnlyModeLogo)).setImageBitmap(myBitmap);
                ((ImageView) findViewById(R.id.imageViewLockLogo)).setImageBitmap(myBitmap);
                final ImageView startupOverlay = findViewById(R.id.mainStartupOverlay);
                startupOverlay.setImageBitmap(myBitmap);
                startupOverlay.setVisibility(View.VISIBLE);
                Timer timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                startupOverlay.animate().alpha(0.0f).setListener(new AnimatorListenerAdapter() {
                                    @Override
                                    public void onAnimationEnd(Animator animation) {
                                        super.onAnimationEnd(animation);
                                        startupOverlay.setVisibility(View.GONE);
                                    }
                                });
                            }
                        });
                    }
                }, 2000);
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    static void setUnsyncedChanges(Context c) {
        SharedPreferences settings = c.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean("unsynced-changes", true);
        editor.apply();
    }
    static void setChangesSynced(Context c) {
        SharedPreferences settings = c.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean("unsynced-changes", false);
        editor.apply();
    }
    void refreshSyncIcon() {
        if(mOptionMenu == null) return;
        if(mSettings.getBoolean("unsynced-changes", false)) {
            if(mRemoteDatabaseConnType == 1 || mRemoteDatabaseConnType == 2) {
                mOptionMenu.findItem(R.id.action_sync).setIcon(getResources().getDrawable(R.drawable.ic_sync_problem_white_24dp));
            }
        } else {
            mOptionMenu.findItem(R.id.action_sync).setIcon(getResources().getDrawable(R.drawable.ic_sync_white_24dp));
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        DrawerLayout drawer = findViewById(R.id.drawerLayoutMain);
        switch(item.getItemId()) {
            case R.id.nav_information:
                Intent aboutIntent = new Intent(me, AboutActivity.class);
                startActivityForResult(aboutIntent, ABOUT_REQUEST);
                break;
            case R.id.nav_settings:
                Intent settingsIntent = new Intent(me, SettingsActivity.class);
                startActivityForResult(settingsIntent, SETTINGS_REQUEST);
                break;
            case R.id.nav_input_only_mode:
                drawer.closeDrawer(GravityCompat.START);
                onToggleInputOnlyModeButtonClick();
                break;
            case R.id.nav_lock:
                drawer.closeDrawer(GravityCompat.START);
                onToggleLockButtonClick();
                break;
            case R.id.nav_filter:
                drawer.closeDrawer(GravityCompat.START);
                filterDialog();
                break;
            case R.id.nav_sort:
                drawer.closeDrawer(GravityCompat.START);
                sortDialog();
                break;
            case R.id.nav_newsletter:
                drawer.closeDrawer(GravityCompat.START);
                doNewsletter();
                break;
            case R.id.nav_birthdays:
                drawer.closeDrawer(GravityCompat.START);
                openBirthdaysIntent();
                break;
            case R.id.nav_import_export:
                drawer.closeDrawer(GravityCompat.START);
                if(mBottomNavigationView.getSelectedItemId() == R.id.bottomnav_customers)
                    menuImportExportCustomer();
                else if(mBottomNavigationView.getSelectedItemId() == R.id.bottomnav_vouchers)
                    menuImportExportVoucher();
                else if(mBottomNavigationView.getSelectedItemId() == R.id.bottomnav_calendar)
                    menuImportExportCalendar();
                break;
            case R.id.nav_remove_selected:
                drawer.closeDrawer(GravityCompat.START);
                removeSelectedDialog();
                break;
            case R.id.nav_exit:
                mDb.close();
                finish();
                break;
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawerLayoutMain);
        if(drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        mOptionMenu = menu;
        enableDisableInputOnlyMode(isInputOnlyModeActive);
        enableDisableLock(isLockActive);
        refreshSyncIcon();

        SearchManager manager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        if(manager == null) return false;
        SearchView search = (SearchView) menu.findItem(R.id.action_search).getActionView();
        search.setSearchableInfo(manager.getSearchableInfo(getComponentName()));
        search.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                refreshCustomersFromLocalDatabase(query);
                refreshVouchersFromLocalDatabase(query);
                resetActionBarTitle();
                return false;
            }
            @Override
            public boolean onQueryTextChange(String query) {
                if(query == null || query.equals("")) {
                    refreshCustomersFromLocalDatabase(query);
                    refreshVouchersFromLocalDatabase(query);
                    resetActionBarTitle();
                }
                return true;
            }
        });

        return true;
    }

    @SuppressWarnings("SwitchStatementWithTooFewBranches")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.action_sync:
                doSync();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();
        incrementStartedCounter();
        refreshSyncIcon();
        checkSnackbarMessage();
        showAdOtherApps();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        StorageControl.deleteTempFiles(this);
    }

    private void doSync() {
        if(mRemoteDatabaseConnType == 1 || mRemoteDatabaseConnType == 2 || mRemoteDatabaseConnType == 3) {
            if(isNetworkConnected()) {
                dialogSyncProgress();
                List<Customer> allCustomers = mDb.getCustomers(null, true, true);
                List<Voucher> allVouchers = mDb.getVouchers(null,true);
                List<CustomerCalendar> allCalendars = mDb.getCalendars(true);
                List<CustomerAppointment> allAppointments = mDb.getAppointments(null,null, true);

                if(mRemoteDatabaseConnType == 1) {
                    new CustomerDatabaseApi(this, mSettings.getString("sync-purchase-token", ""), mRemoteDatabaseConnUsername, mRemoteDatabaseConnPassword, allCustomers, allVouchers, allCalendars, allAppointments).execute();
                }
                else if(mRemoteDatabaseConnType == 2) {
                    new CustomerDatabaseApi(this, mSettings.getString("sync-purchase-token", ""), mRemoteDatabaseConnURL, mRemoteDatabaseConnUsername, mRemoteDatabaseConnPassword, allCustomers, allVouchers, allCalendars, allAppointments).execute();
                }
                else if(mRemoteDatabaseConnType == 3) {
                    CommonDialog.show(this, getString(R.string.legacy_sync_warning), getString(R.string.legacy_sync_warning_text), CommonDialog.TYPE.WARN, false);
                    new CustomerDatabaseApiLegacy(this, mRemoteDatabaseConnURL, mRemoteDatabaseConnPassword, "sync", allCustomers, allVouchers).execute();
                }

            } else {
                CommonDialog.show(this,
                        getResources().getString(R.string.no_network_conn_title),
                        getResources().getString(R.string.no_network_conn_text),
                        CommonDialog.TYPE.WARN, false
                );
            }
        } else {
            dialogSyncNotConfigured();
        }
    }

    private void removeSelectedDialog() {
        switch(mBottomNavigationView.getSelectedItemId()) {
            case R.id.bottomnav_customers:
                final ArrayList<Customer> selectedCustomers = mCurrentCustomerAdapter.getCheckedItems();
                if(selectedCustomers.size() == 0) {
                    CommonDialog.show(this,
                            getResources().getString(R.string.nothing_selected),
                            getResources().getString(R.string.select_at_least_one), CommonDialog.TYPE.WARN, false);
                } else {
                    AlertDialog.Builder ad = new AlertDialog.Builder(me);
                    ad.setPositiveButton(getResources().getString(R.string.delete), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            for(Customer c : selectedCustomers) {
                                mDb.removeCustomer(c);
                            }
                            refreshCustomersFromLocalDatabase();
                        }});
                    ad.setNegativeButton(getResources().getString(R.string.abort), null);
                    ad.setTitle(getResources().getString(R.string.reallydelete_title));
                    ad.setMessage(
                            getResources().getQuantityString(R.plurals.delete_records, selectedCustomers.size(), selectedCustomers.size())
                    );
                    ad.setIcon(getResources().getDrawable(R.drawable.remove));
                    ad.show();
                }
                break;

            case R.id.bottomnav_vouchers:
                final ArrayList<Voucher> selectedVouchers = mCurrentVoucherAdapter.getCheckedItems();
                if(selectedVouchers.size() == 0) {
                    CommonDialog.show(this,
                            getResources().getString(R.string.nothing_selected),
                            getResources().getString(R.string.select_at_least_one), CommonDialog.TYPE.WARN, false);
                } else {
                    AlertDialog.Builder ad = new AlertDialog.Builder(me);
                    ad.setPositiveButton(getResources().getString(R.string.delete), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            for(Voucher v : selectedVouchers) {
                                mDb.removeVoucher(v);
                            }
                            refreshVouchersFromLocalDatabase();
                        }});
                    ad.setNegativeButton(getResources().getString(R.string.abort), null);
                    ad.setTitle(getResources().getString(R.string.reallydelete_title));
                    ad.setMessage(
                            getResources().getQuantityString(R.plurals.delete_records, selectedVouchers.size(), selectedVouchers.size())
                    );
                    ad.setIcon(getResources().getDrawable(R.drawable.remove));
                    ad.show();
                }
                break;
        }
    }

    private void sortDialog() {
        List<CustomField> customFields = mDb.getCustomFields();
        final Dialog ad = new Dialog(this);
        ad.requestWindowFeature(Window.FEATURE_NO_TITLE);
        ad.setContentView(R.layout.dialog_sort);
        ArrayAdapter<CustomField> a = new ArrayAdapter<>(this, R.layout.item_list_simple, customFields);
        Spinner s = ad.findViewById(R.id.spinnerSort);
        s.setAdapter(a);
        ad.show();
        ad.findViewById(R.id.buttonSort).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RadioButton radioButtonSortLastName = ad.findViewById(R.id.radioButtonSortLastName);
                RadioButton radioButtonSortFirstName = ad.findViewById(R.id.radioButtonSortFirstName);
                RadioButton radioButtonSortCustomField = ad.findViewById(R.id.radioButtonSortCustomField);
                boolean sortAsc = ((RadioButton) ad.findViewById(R.id.radioButtonSortAsc)).isChecked();
                if(radioButtonSortLastName.isChecked()) {
                    sort(CustomerComparator.FIELD.LAST_NAME, sortAsc);
                } else if(radioButtonSortFirstName.isChecked()) {
                    sort(CustomerComparator.FIELD.FIRST_NAME, sortAsc);
                } else if(radioButtonSortCustomField.isChecked()) {
                    CustomField customField = (CustomField) ((Spinner) ad.findViewById(R.id.spinnerSort)).getSelectedItem();
                    if(customField != null) {
                        String customFieldName = customField.toString();
                        sort(customFieldName, sortAsc);
                    }
                }
                ad.dismiss();
            }
        });
        ad.findViewById(R.id.buttonSortCancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                refreshCustomersFromLocalDatabase();
                ad.dismiss();
            }
        });
    }
    private void sort(String fieldTitle, boolean ascending) {
        mCustomers = mDb.getCustomers(null, false, false);
        Collections.sort(mCustomers, new CustomerComparator(fieldTitle, ascending));
        mCurrentCustomerAdapter = new CustomerAdapter(this, mCustomers, mCustomerCheckedChangedListener);
        mListViewCustomers.setAdapter(mCurrentCustomerAdapter);
    }
    private void sort(CustomerComparator.FIELD field, boolean ascending) {
        mCustomers = mDb.getCustomers(null, false, false);
        Collections.sort(mCustomers, new CustomerComparator(field, ascending));
        mCurrentCustomerAdapter = new CustomerAdapter(this, mCustomers, mCustomerCheckedChangedListener);
        mListViewCustomers.setAdapter(mCurrentCustomerAdapter);
    }

    private void onToggleLockButtonClick() {
        if(isLockActive) {
            if(mIomPassword.equals("")) {
                enableDisableLock(false);
            } else {
                // password check
                final Dialog ad = new Dialog(this);
                ad.requestWindowFeature(Window.FEATURE_NO_TITLE);
                ad.setContentView(R.layout.dialog_iom_unlock);
                Button button = ad.findViewById(R.id.buttonInputOnlyModeUnlock);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String passwordInput = ((EditText) ad.findViewById(R.id.editTextInputOnlyModeUnlockPassword)).getText().toString();
                        if(passwordInput.equals(mIomPassword)) {
                            ad.dismiss();
                            enableDisableLock(false);
                        } else {
                            ad.dismiss();
                            CommonDialog.show(me, getResources().getString(R.string.password_incorrect), "", CommonDialog.TYPE.FAIL, false);
                        }
                    }
                });
                if(ad.getWindow() != null) ad.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                ad.show();
            }
        } else {
            if(mFc.unlockedInputOnlyMode) {
                enableDisableLock(true);
            } else {
                dialogInApp(getResources().getString(R.string.feature_locked), getResources().getString(R.string.feature_locked_text));
            }
        }
    }
    private void enableDisableLock(boolean state) {
        if(isInputOnlyModeActive) return;
        isLockActive = state;
        SharedPreferences.Editor editor = mSettings.edit();
        editor.putBoolean("locked", state);
        editor.apply();
        if(mOptionMenu != null) {
            mOptionMenu.findItem(R.id.action_search).setVisible(!state);
        }
        if(mDrawerMenu != null) {
            mDrawerMenu.findItem(R.id.nav_input_only_mode).setEnabled(!state);
            mDrawerMenu.findItem(R.id.nav_lock).setEnabled(!state);
            mDrawerMenu.findItem(R.id.nav_filter).setEnabled(!state);
            mDrawerMenu.findItem(R.id.nav_sort).setEnabled(!state);
            mDrawerMenu.findItem(R.id.nav_newsletter).setEnabled(!state);
            mDrawerMenu.findItem(R.id.nav_birthdays).setEnabled(!state);
            mDrawerMenu.findItem(R.id.nav_import_export).setEnabled(!state);
            mDrawerMenu.findItem(R.id.nav_settings).setEnabled(!state);
            mDrawerMenu.findItem(R.id.nav_remove_selected).setEnabled(!state);
            mDrawerMenu.findItem(R.id.nav_exit).setEnabled(!state);
        }
        final View overlay = findViewById(R.id.mainLockOverlay);
        if(state) {
            findViewById(R.id.fabAdd).setVisibility(View.GONE);
            mBottomNavigationView.setVisibility(View.GONE);
            overlay.setAlpha(0.0f);
            overlay.setVisibility(View.VISIBLE);
            overlay.animate().alpha(1f).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    overlay.setAlpha(1f);
                }
            });
        } else {
            findViewById(R.id.fabAdd).setVisibility(View.VISIBLE);
            mBottomNavigationView.setVisibility(View.VISIBLE);
            overlay.animate().alpha(0.0f).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    overlay.setVisibility(View.GONE);
                }
            });
        }
        refreshView(null);
    }

    private void onToggleInputOnlyModeButtonClick() {
        if(isInputOnlyModeActive) {
            if(mIomPassword.equals("")) {
                enableDisableInputOnlyMode(false);
            } else {
                // password check
                final Dialog ad = new Dialog(this);
                ad.requestWindowFeature(Window.FEATURE_NO_TITLE);
                ad.setContentView(R.layout.dialog_iom_unlock);
                Button button = ad.findViewById(R.id.buttonInputOnlyModeUnlock);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String passwordInput = ((EditText) ad.findViewById(R.id.editTextInputOnlyModeUnlockPassword)).getText().toString();
                        if(passwordInput.equals(mIomPassword)) {
                            ad.dismiss();
                            enableDisableInputOnlyMode(false);
                        } else {
                            ad.dismiss();
                            CommonDialog.show(me, getResources().getString(R.string.password_incorrect), "", CommonDialog.TYPE.FAIL, false);
                        }
                    }
                });
                if(ad.getWindow() != null) ad.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                ad.show();
            }
        } else {
            if(mFc.unlockedInputOnlyMode) {
                enableDisableInputOnlyMode(true);
            } else {
                dialogInApp(getResources().getString(R.string.feature_locked), getResources().getString(R.string.feature_locked_text));
            }
        }
    }
    private void enableDisableInputOnlyMode(boolean state) {
        if(isLockActive) return;
        isInputOnlyModeActive = state;
        SharedPreferences.Editor editor = mSettings.edit();
        editor.putBoolean("iom", state);
        editor.apply();
        if(mOptionMenu != null) {
            mOptionMenu.findItem(R.id.action_search).setVisible(!state);
        }
        if(mDrawerMenu != null) {
            mDrawerMenu.findItem(R.id.nav_input_only_mode).setEnabled(!state);
            mDrawerMenu.findItem(R.id.nav_lock).setEnabled(!state);
            mDrawerMenu.findItem(R.id.nav_filter).setEnabled(!state);
            mDrawerMenu.findItem(R.id.nav_sort).setEnabled(!state);
            mDrawerMenu.findItem(R.id.nav_newsletter).setEnabled(!state);
            mDrawerMenu.findItem(R.id.nav_birthdays).setEnabled(!state);
            mDrawerMenu.findItem(R.id.nav_import_export).setEnabled(!state);
            mDrawerMenu.findItem(R.id.nav_settings).setEnabled(!state);
            mDrawerMenu.findItem(R.id.nav_remove_selected).setEnabled(!state);
            mDrawerMenu.findItem(R.id.nav_exit).setEnabled(!state);
        }
        final View overlay = findViewById(R.id.mainInputOnlyOverlay);
        if(state) {
            findViewById(R.id.fabAdd).setVisibility(View.GONE);
            mBottomNavigationView.setVisibility(View.GONE);
            overlay.setAlpha(0.0f);
            overlay.setVisibility(View.VISIBLE);
            overlay.animate().alpha(1f).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    overlay.setAlpha(1f);
                }
            });
        } else {
            findViewById(R.id.fabAdd).setVisibility(View.VISIBLE);
            mBottomNavigationView.setVisibility(View.VISIBLE);
            overlay.animate().alpha(0.0f).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    overlay.setVisibility(View.GONE);
                }
            });
        }
        refreshView(null);
    }

    private void openBirthdaysIntent() {
        ArrayList<Customer> birthdays = getSoonBirthdayCustomers(mCustomers);
        if(birthdays.size() == 0) {
            CommonDialog.show(this, getResources().getString(R.string.nobirthdays), "", CommonDialog.TYPE.WARN, false);
            return;
        }
        me.startActivityForResult(new Intent(me, BirthdayActivity.class), BIRTHDAY_REQUEST);
    }

    public void resetActionBarTitle() {
        if(getSupportActionBar() != null) getSupportActionBar().setTitle(getResources().getString(R.string.app_name));
    }

    private boolean isNetworkConnected() {
        ConnectivityManager connectivityManager = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        if(connectivityManager == null) return true;
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    static ArrayList<Customer> getSoonBirthdayCustomers(List<Customer> customers) {
        ArrayList<Customer> birthdayCustomers = new ArrayList<>();
        Date start = new Date();
        Date end = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(end);
        cal.add(Calendar.DATE, 14); // number of days to add
        end = cal.getTime();
        cal.setTime(start);
        cal.add(Calendar.DATE, -1); // number of days to add
        start = cal.getTime();
        for(Customer c: customers) {
            Date birthday = c.getNextBirthday();
            if(birthday != null && isWithinRange(birthday, start, end))
                birthdayCustomers.add(c);
        }
        Collections.sort(birthdayCustomers, new Comparator<Customer>() {
            public int compare(Customer o1, Customer o2) {
                return o1.getNextBirthday().compareTo(o2.getNextBirthday());
            }
        });
        return birthdayCustomers;
    }

    private void checkSnackbarMessage() {
        //Log.i("FEATURE", Integer.toString(mSettings.getInt("started", 0)));
        if((mSettings.getInt("started", 0) % 15) == 0) {
            if(mFc != null && mFc.isReady) {
                if(!mFc.unlockedCommercialUsage) {
                    if(mRemoteDatabaseConnURL.equals("")) {
                        showAdLicense();
                    } else {
                        Log.i("FEATURE", "Licensing information hidden because connected with API.");
                        checkBirthdays();
                    }
                } else {
                    Log.i("FEATURE", "Licensing information hidden because commercial usage allowed.");
                    checkBirthdays();
                }
            } else {
                Log.i("FEATURE", "Licensing information hidden because feature check not ready yet.");
                checkBirthdays();
            }
        } else {
            checkBirthdays();
        }
    }

    private void checkBirthdays() {
        ArrayList<Customer> birthdays = getSoonBirthdayCustomers(mCustomers);
        if(birthdays.size() > 0 && !isInputOnlyModeActive && !isLockActive) {
            Snackbar.make(
                    findViewById(R.id.coordinatorLayoutInner),
                    getResources().getQuantityString(R.plurals.birthdayssoon, birthdays.size(), birthdays.size()),
                    Snackbar.LENGTH_LONG)
                    .setAction(getResources().getString(R.string.view), new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            openBirthdaysIntent();
                        }
                    })
                    .show();
        }
    }

    private static boolean isWithinRange(Date testDate, Date startDate, Date endDate) {
        return ((!testDate.before(startDate)) && (!testDate.after(endDate)));
    }

    private Snackbar mCheckedCountInfoSnackbar;
    private void refreshSelectedCountInfo(Integer itemId) {
        if(mBottomNavigationView == null) return;
        if(itemId == null) itemId = mBottomNavigationView.getSelectedItemId();

        BaseAdapter currentAdapter = null;
        switch(itemId) {
            case R.id.bottomnav_customers:
                currentAdapter = mCurrentCustomerAdapter;
                break;
            case R.id.bottomnav_vouchers:
                currentAdapter = mCurrentVoucherAdapter;
                break;
        }
        final BaseAdapter finalCurrentAdapter = currentAdapter;
        final Integer finalItemId = itemId;

        int count = 0;
        if(finalCurrentAdapter instanceof CustomerAdapter) {
            count = ((CustomerAdapter) finalCurrentAdapter).getCheckedItems().size();
        } else if(finalCurrentAdapter instanceof VoucherAdapter) {
            count = ((VoucherAdapter) finalCurrentAdapter).getCheckedItems().size();
        }
        final int finalCount = count;

        if(count == 0) {
            if(mCheckedCountInfoSnackbar != null)
                mCheckedCountInfoSnackbar.dismiss();
        } else {
            if(mCheckedCountInfoSnackbar == null)
                mCheckedCountInfoSnackbar = Snackbar.make(
                        findViewById(R.id.coordinatorLayoutInner), "", Snackbar.LENGTH_INDEFINITE
                );
            mCheckedCountInfoSnackbar.setText(getResources().getQuantityString(R.plurals.selected_records, count, count));
            mCheckedCountInfoSnackbar.setAction(
                    (count == finalCurrentAdapter.getCount()) ? getResources().getString(R.string.deselect) : getResources().getString(R.string.select_all),
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            boolean state = true;
                            if(finalCount == finalCurrentAdapter.getCount()) {
                                state = false;
                            }
                            if(finalCurrentAdapter instanceof CustomerAdapter) {
                                ((CustomerAdapter) finalCurrentAdapter).setAllChecked(state);
                            } else if(finalCurrentAdapter instanceof VoucherAdapter) {
                                ((VoucherAdapter) finalCurrentAdapter).setAllChecked(state);
                            }
                            finalCurrentAdapter.notifyDataSetChanged();
                            // immediately show snackbar again
                            new java.util.Timer().schedule(
                                    new java.util.TimerTask() {
                                        @Override
                                        public void run() {
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    refreshSelectedCountInfo(finalItemId);
                                                }
                                            });
                                        }
                                    },
                                    400
                            );
                        }
                    });
            mCheckedCountInfoSnackbar.show();
        }
    }

    @SuppressLint("SetTextI18n")
    private void refreshCount() {
        if(mNavigationView != null) {
            int customersCount = mDb.getCustomers(null, false, false).size();
            int vouchersCount = mDb.getVouchers(null,false).size();
            String customerAmountString = getResources().getQuantityString(R.plurals.customersamount, customersCount, customersCount);
            String voucherAmountString = getResources().getQuantityString(R.plurals.vouchersamount, vouchersCount, vouchersCount);
            View headerView = mNavigationView.getHeaderView(0);
            ((TextView) headerView.findViewById(R.id.textViewDrawerCustomerCount)).setText(
                    customerAmountString
            );
            ((TextView) headerView.findViewById(R.id.textViewDrawerVoucherCount)).setText(
                    voucherAmountString
            );
        }
    }

    public Dialog dialogObjSyncProgress = null;
    public void dialogSyncProgress() {
        final Dialog ad = new Dialog(this);
        ad.setCancelable(false);
        ad.requestWindowFeature(Window.FEATURE_NO_TITLE);
        ad.setContentView(R.layout.dialog_syncprogress);
        ad.show();
        dialogObjSyncProgress = ad;
    }

    public void dialogSyncSuccess() {
        if(dialogObjSyncProgress != null) dialogObjSyncProgress.dismiss();
        CommonDialog.show(this, getResources().getString(R.string.syncsuccess), null, CommonDialog.TYPE.OK, false);
    }

    public void dialogSyncFail(String errorMessage) {
        if(dialogObjSyncProgress != null) dialogObjSyncProgress.dismiss();
        CommonDialog.show(this, getResources().getString(R.string.syncfail), errorMessage, CommonDialog.TYPE.FAIL, false);
    }

    public static boolean isValidEmail(CharSequence target) {
        return !TextUtils.isEmpty(target) && android.util.Patterns.EMAIL_ADDRESS.matcher(target).matches();
    }
    private void doNewsletter() {
        final Dialog ad = new Dialog(this);
        ad.requestWindowFeature(Window.FEATURE_NO_TITLE);
        ad.setContentView(R.layout.dialog_send_newsletter);
        final Button buttonComposeNewsletter = ad.findViewById(R.id.buttonComposeNewsletter);
        buttonComposeNewsletter.setEnabled(false);
        final Spinner dropdown = ad.findViewById(R.id.spinnerGroup);
        final RadioButton radioButtonSelected = ad.findViewById(R.id.radioButtonNewsletterReceiverSelected);
        final RadioButton radioButtonGroup = ad.findViewById(R.id.radioButtonNewsletterReceiverGroup);
        if(mCurrentCustomerAdapter.getCheckedItems().size() == 0) radioButtonSelected.setEnabled(false);
        CompoundButton.OnCheckedChangeListener occl = new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                buttonComposeNewsletter.setEnabled(true);
            }
        };
        radioButtonSelected.setOnCheckedChangeListener(occl);
        radioButtonGroup.setOnCheckedChangeListener(occl);
        ad.findViewById(R.id.buttonComposeNewsletter).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ad.dismiss();
                List<String> receiversList = new ArrayList<>();
                if(radioButtonGroup.isChecked()) {
                    String group = dropdown.getSelectedItem().toString();
                    for(Customer c : mCustomers) {
                        if(c.mNewsletter && c.mEmail != null && !c.mEmail.equals("") && isValidEmail(c.mEmail))
                            if(group.equals(getResources().getString(R.string.all)) || group.equals(c.mCustomerGroup))
                                receiversList.add(c.mEmail);
                    }
                } else if(radioButtonSelected.isChecked()) {
                    for(Customer c : mCurrentCustomerAdapter.getCheckedItems()) {
                        if(c.mEmail != null && !c.mEmail.equals("") && isValidEmail(c.mEmail))
                            receiversList.add(c.mEmail);
                    }
                }
                if(receiversList.size() == 0) {
                    CommonDialog.show(me,
                            getResources().getString(R.string.newsletter_no_customers_title),
                            getResources().getString(R.string.newsletter_no_customers_text), CommonDialog.TYPE.WARN,false);
                    return;
                }
                composeNewsletter(receiversList);
            }
        });
        ad.findViewById(R.id.buttonComposeNewsletterCancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ad.dismiss();
            }
        });
        ad.show();
        String[] items = getGroups(true);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.item_list_simple, items);
        dropdown.setAdapter(adapter);
    }
    private void composeNewsletter(List<String> receiversList) {
        String[] receivers = new String[receiversList.size()];
        receivers = receiversList.toArray(receivers);
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("message/rfc822");
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{});
        intent.putExtra(Intent.EXTRA_BCC, receivers);
        intent.putExtra(Intent.EXTRA_SUBJECT, getResources().getString(R.string.newsletter));
        intent.putExtra(Intent.EXTRA_TEXT,
                mSettings.getString("email-newsletter-template", getResources().getString(R.string.newsletter_text_template))
        );
        startActivity(Intent.createChooser(intent, getResources().getString(R.string.emailtocustomer)));
    }

    private void menuImportExportCustomer() {
        final Dialog ad = new Dialog(this);
        ad.requestWindowFeature(Window.FEATURE_NO_TITLE);
        ad.setContentView(R.layout.dialog_import_export_customer);
        if(mCurrentCustomerAdapter.getCheckedItems().size() == 0)
            ad.findViewById(R.id.checkBoxExportOnlySelected).setEnabled(false);
        ad.findViewById(R.id.buttonImportVCF).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ad.dismiss();
                Intent intent = new Intent();
                intent.setType("text/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent, "Select VCF file"), PICK_CUSTOMER_VCF_REQUEST);
            }
        });
        ad.findViewById(R.id.buttonImportCSV).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ad.dismiss();
                AlertDialog.Builder ad2 = new AlertDialog.Builder(me);
                ad2.setMessage(getResources().getString(R.string.import_csv_note));
                ad2.setNegativeButton(getResources().getString(R.string.abort), null);
                ad2.setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        Intent intent = new Intent();
                        intent.setType("text/*");
                        intent.setAction(Intent.ACTION_GET_CONTENT);
                        startActivityForResult(Intent.createChooser(intent, "Select CSV file"), PICK_CUSTOMER_CSV_REQUEST);
                    }
                });
                ad2.show();
            }
        });
        ad.findViewById(R.id.buttonExportVCF).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ad.dismiss();
                boolean onlySelected = ((CheckBox) ad.findViewById(R.id.checkBoxExportOnlySelected)).isChecked();
                boolean sendEmail = ((CheckBox) ad.findViewById(R.id.checkBoxExportSendEmail)).isChecked();
                CustomerVcfBuilder content;
                if(onlySelected)
                    content = new CustomerVcfBuilder(mCurrentCustomerAdapter.getCheckedItems());
                else
                    content = new CustomerVcfBuilder(mCustomers);
                File f = StorageControl.getStorageExportVcf(me);
                if(content.saveVcfFile(f)) {
                    if(sendEmail) {
                        emailFile(f);
                    } else {
                        CommonDialog.show(me, getResources().getString(R.string.export_ok), f.getPath(), CommonDialog.TYPE.OK, false);
                    }
                } else {
                    CommonDialog.show(me, getResources().getString(R.string.export_fail), f.getPath(), CommonDialog.TYPE.FAIL, false);
                }
                StorageControl.scanFile(f, me);
            }
        });
        ad.findViewById(R.id.buttonExportCSV).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ad.dismiss();
                boolean onlySelected = ((CheckBox) ad.findViewById(R.id.checkBoxExportOnlySelected)).isChecked();
                boolean sendEmail = ((CheckBox) ad.findViewById(R.id.checkBoxExportSendEmail)).isChecked();
                CustomerCsvBuilder content;
                if(onlySelected)
                    content = new CustomerCsvBuilder(mCurrentCustomerAdapter.getCheckedItems(), mDb.getCustomFields());
                else
                    content = new CustomerCsvBuilder(mCustomers, mDb.getCustomFields());
                File f = StorageControl.getStorageExportCsv(me);
                if(content.saveCsvFile(f)) {
                    if(sendEmail) {
                        emailFile(f);
                    } else {
                        CommonDialog.show(me, getResources().getString(R.string.export_ok), f.getPath(), CommonDialog.TYPE.OK, false);
                    }
                } else {
                    CommonDialog.show(me, getResources().getString(R.string.export_fail), f.getPath(), CommonDialog.TYPE.FAIL, false);
                }
                StorageControl.scanFile(f, me);
            }
        });
        ad.findViewById(R.id.buttonExportPDF).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                    boolean onlySelected = ((CheckBox) ad.findViewById(R.id.checkBoxExportOnlySelected)).isChecked();
                    List<Customer> printList = mCustomers;
                    if(onlySelected)
                        printList = mCurrentCustomerAdapter.getCheckedItems();
                    for(Customer c : printList) {
                        print(c);
                    }
                } else {
                    CommonDialog.show(me, getResources().getString(R.string.not_supported), getResources().getString(R.string.not_supported_printing), CommonDialog.TYPE.FAIL, false);
                }
            }
        });
        ad.findViewById(R.id.buttonExportCancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ad.dismiss();
            }
        });
        ad.show();
    }
    private void menuImportExportVoucher() {
        final Dialog ad = new Dialog(this);
        ad.requestWindowFeature(Window.FEATURE_NO_TITLE);
        ad.setContentView(R.layout.dialog_import_export_voucher);
        if(mCurrentVoucherAdapter.getCheckedItems().size() == 0)
            ad.findViewById(R.id.checkBoxExportOnlySelected).setEnabled(false);
        ad.findViewById(R.id.buttonImportCSV).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ad.dismiss();
                AlertDialog.Builder ad2 = new AlertDialog.Builder(me);
                ad2.setMessage(getResources().getString(R.string.import_csv_note_voucher));
                ad2.setNegativeButton(getResources().getString(R.string.abort), null);
                ad2.setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        Intent intent = new Intent();
                        intent.setType("text/*");
                        intent.setAction(Intent.ACTION_GET_CONTENT);
                        startActivityForResult(Intent.createChooser(intent, "Select CSV file"), PICK_VOUCHER_CSV_REQUEST);
                    }
                });
                ad2.show();
            }
        });
        ad.findViewById(R.id.buttonExportCSV).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ad.dismiss();
                boolean onlySelected = ((CheckBox) ad.findViewById(R.id.checkBoxExportOnlySelected)).isChecked();
                boolean sendEmail = ((CheckBox) ad.findViewById(R.id.checkBoxExportSendEmail)).isChecked();
                VoucherCsvBuilder content;
                if(onlySelected)
                    content = new VoucherCsvBuilder(mCurrentVoucherAdapter.getCheckedItems());
                else
                    content = new VoucherCsvBuilder(mVouchers);
                File f = StorageControl.getStorageExportCsv(me);
                if(content.saveCsvFile(f)) {
                    if(sendEmail) {
                        emailFile(f);
                    } else {
                        CommonDialog.show(me, getResources().getString(R.string.export_ok), f.getPath(), CommonDialog.TYPE.OK, false);
                    }
                } else {
                    CommonDialog.show(me, getResources().getString(R.string.export_fail), f.getPath(), CommonDialog.TYPE.FAIL, false);
                }
                StorageControl.scanFile(f, me);
            }
        });
        ad.findViewById(R.id.buttonExportCancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ad.dismiss();
            }
        });
        ad.show();
    }
    private void menuImportExportCalendar() {
        final Dialog ad = new Dialog(this);
        ad.requestWindowFeature(Window.FEATURE_NO_TITLE);
        ad.setContentView(R.layout.dialog_import_export_calendar);

        // load calendars
        final Spinner spinnerCalendar = ad.findViewById(R.id.spinnerCalendar);
        ArrayAdapter<CustomerCalendar> a = new ArrayAdapter<>(this, R.layout.item_list_simple, mDb.getCalendars(false));
        spinnerCalendar.setAdapter(a);

        ad.findViewById(R.id.buttonImportICS).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ad.dismiss();
                CustomerCalendar calendar = (CustomerCalendar) spinnerCalendar.getSelectedItem();
                if(calendar == null) {
                    CommonDialog.show(me, getString(R.string.error), getString(R.string.no_calendar_selected), CommonDialog.TYPE.WARN, false);
                    return;
                }
                mCurrentCalendarImportSelectedId = calendar.mId;
                Intent intent = new Intent();
                intent.setType("text/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent, "Select ICS file"), PICK_CALENDAR_ICS_REQUEST);
            }
        });
        ad.findViewById(R.id.buttonImportCSV).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ad.dismiss();
                CustomerCalendar calendar = (CustomerCalendar) spinnerCalendar.getSelectedItem();
                if(calendar == null) {
                    CommonDialog.show(me, getString(R.string.error), getString(R.string.no_calendar_selected), CommonDialog.TYPE.WARN, false);
                    return;
                }
                mCurrentCalendarImportSelectedId = calendar.mId;
                AlertDialog.Builder ad2 = new AlertDialog.Builder(me);
                ad2.setMessage(getResources().getString(R.string.import_csv_note));
                ad2.setNegativeButton(getResources().getString(R.string.abort), null);
                ad2.setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        Intent intent = new Intent();
                        intent.setType("text/*");
                        intent.setAction(Intent.ACTION_GET_CONTENT);
                        startActivityForResult(Intent.createChooser(intent, "Select CSV file"), PICK_CALENDAR_CSV_REQUEST);
                    }
                });
                ad2.show();
            }
        });
        ad.findViewById(R.id.buttonExportICS).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ad.dismiss();
                boolean sendEmail = ((CheckBox) ad.findViewById(R.id.checkBoxExportSendEmail)).isChecked();
                CustomerCalendar calendar = (CustomerCalendar) spinnerCalendar.getSelectedItem();
                if(calendar == null) {
                    CommonDialog.show(me, getString(R.string.error), getString(R.string.no_calendar_selected), CommonDialog.TYPE.WARN, false);
                    return;
                }
                CalendarIcsBuilder content = new CalendarIcsBuilder(mDb.getAppointments(calendar.mId, null, false));
                File f = StorageControl.getStorageExportIcs(me);
                if(content.saveIcsFile(f)) {
                    if(sendEmail) {
                        emailFile(f);
                    } else {
                        CommonDialog.show(me, getResources().getString(R.string.export_ok), f.getPath(), CommonDialog.TYPE.OK, false);
                    }
                } else {
                    CommonDialog.show(me, getResources().getString(R.string.export_fail), f.getPath(), CommonDialog.TYPE.FAIL, false);
                }
                StorageControl.scanFile(f, me);
            }
        });
        ad.findViewById(R.id.buttonExportCSV).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ad.dismiss();
                boolean sendEmail = ((CheckBox) ad.findViewById(R.id.checkBoxExportSendEmail)).isChecked();
                CustomerCalendar calendar = (CustomerCalendar) spinnerCalendar.getSelectedItem();
                if(calendar == null) {
                    CommonDialog.show(me, getString(R.string.error), getString(R.string.no_calendar_selected), CommonDialog.TYPE.WARN, false);
                    return;
                }
                CalendarCsvBuilder content = new CalendarCsvBuilder(mDb.getAppointments(calendar.mId, null, false));
                File f = StorageControl.getStorageExportCsv(me);
                if(content.saveCsvFile(f)) {
                    if(sendEmail) {
                        emailFile(f);
                    } else {
                        CommonDialog.show(me, getResources().getString(R.string.export_ok), f.getPath(), CommonDialog.TYPE.OK, false);
                    }
                } else {
                    CommonDialog.show(me, getResources().getString(R.string.export_fail), f.getPath(), CommonDialog.TYPE.FAIL, false);
                }
                StorageControl.scanFile(f, me);
            }
        });
        ad.findViewById(R.id.buttonExportCancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ad.dismiss();
            }
        });
        ad.show();
    }
    private void emailFile(File f) {
        Uri attachmentUri = FileProvider.getUriForFile(
                this,
                "de.georgsieber.customerdb.provider",
                f
        );
        // this opens app chooser instead of system email app
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("message/rfc822");
        intent.putExtra(Intent.EXTRA_STREAM, attachmentUri);
        startActivity(Intent.createChooser(intent, getResources().getString(R.string.emailtocustomer)));
    }
    private void print(Customer currentCustomer) {
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
                printManager.print(jobName, new CustomerPrintDocumentAdapter(this,currentCustomer), pa);
            }
        }
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

    private void dialogSyncNotConfigured() {
        AlertDialog.Builder ad = new AlertDialog.Builder(this);
        ad.setTitle(getResources().getString(R.string.sync_not_configured_title));
        ad.setMessage(getResources().getString(R.string.sync_not_configured_text));
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
                startActivity(new Intent(me, InfoActivity.class));
            }
        });
        ad.show();
    }

    public void dialogEula() {
        final SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        if(!settings.getBoolean("eulaok", false)) {
            final AlertDialog.Builder ad = new AlertDialog.Builder(me);
            ad.setPositiveButton(getResources().getString(R.string.eulaaccept), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putBoolean("eulaok", true);
                    editor.apply();
                }});
            ad.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    me.finish();
                }
            });
            ad.setTitle(getResources().getString(R.string.eula_title));
            ad.setMessage(getResources().getString(R.string.eula));
            ad.show();
        }
    }

    public void dialogNews() {
        final String newsKey = "news-shown-3.2";
        final SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        if(!settings.getBoolean(newsKey, false)) {
            final Dialog ad = new Dialog(this);
            ad.requestWindowFeature(Window.FEATURE_NO_TITLE);
            ad.setContentView(R.layout.dialog_news);
            Button button = ad.findViewById(R.id.buttonClose);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ad.dismiss();
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putBoolean(newsKey, true);
                    editor.apply();
                }
            });
            ad.show();
        }
    }

    private void showAdLicense() {
        Snackbar.make(
                findViewById(R.id.coordinatorLayoutInner),
                getResources().getString(R.string.not_licensed),
                Snackbar.LENGTH_LONG)
                .setAction(getResources().getString(R.string.fix), new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent aboutIntent = new Intent(me, AboutActivity.class);
                        startActivity(aboutIntent);
                    }
                })
                .show();
    }

    private void showAdOtherApps() {
        if(mSettings.getInt("started", 0) % 12 == 0
                && mSettings.getInt("ad-other-apps-shown", 0) < 2) {
            // increment counter
            SharedPreferences.Editor editor = mSettings.edit();
            editor.putInt("ad-other-apps-shown", mSettings.getInt("ad-other-apps-shown", 0)+1);
            editor.apply();

            // show ad "other apps"
            final Dialog ad = new Dialog(this);
            ad.requestWindowFeature(Window.FEATURE_NO_TITLE);
            ad.setContentView(R.layout.dialog_otherapps);
            ad.setCancelable(true);
            ad.findViewById(R.id.buttonRateCustomerDB).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    openPlayStore(getPackageName());
                    ad.hide();
                }
            });
            ad.findViewById(R.id.linearLayoutRateStars).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    openPlayStore(getPackageName());
                    ad.hide();
                }
            });
            ad.findViewById(R.id.buttonAdOtherAppsMasterplan).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/schorschii/masterplan"));
                    startActivity(browserIntent);
                }
            });
            ad.findViewById(R.id.buttonAdOtherAppsRemotePointer).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    openPlayStore("systems.sieber.remotespotlight");
                }
            });
            ad.show();
        }
    }
    private void openPlayStore(String appId) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appId)));
        } catch (android.content.ActivityNotFoundException ignored) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appId)));
        }
    }

    private void incrementStartedCounter() {
        final SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        int oldStartedValue = settings.getInt("started", 0);
        Log.i("STARTED",Integer.toString(oldStartedValue));
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt("started", oldStartedValue+1);
        editor.apply();
    }

    private String mCurrentGroup = null;
    private String mCurrentCity = null;
    private String mCurrentCountry = null;
    private void filterDialog() {
        final Dialog ad = new Dialog(this);
        ad.requestWindowFeature(Window.FEATURE_NO_TITLE);
        ad.setContentView(R.layout.dialog_filter);
        final Spinner dropdownCity = ad.findViewById(R.id.spinnerCity);
        final Spinner dropdownCountry = ad.findViewById(R.id.spinnerCountry);
        final Spinner dropdownGroup = ad.findViewById(R.id.spinnerGroup);
        ad.findViewById(R.id.buttonGroupSelect).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ad.dismiss();
                mCurrentGroup = dropdownGroup.getSelectedItem().toString();
                mCurrentCity = dropdownCity.getSelectedItem().toString();
                mCurrentCountry = dropdownCountry.getSelectedItem().toString();
                if(mCurrentGroup.equals(getString(R.string.all))) mCurrentGroup = null;
                if(mCurrentCity.equals(getString(R.string.all))) mCurrentCity = null;
                if(mCurrentCountry.equals(getString(R.string.all))) mCurrentCountry = null;
                refreshCustomersFromLocalDatabase();
            }
        });
        ad.findViewById(R.id.buttonAllSelect).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ad.dismiss();
                mCurrentGroup = null;
                mCurrentCity = null;
                mCurrentCountry = null;
                refreshCustomersFromLocalDatabase();
            }
        });
        ad.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                ad.dismiss();
            }
        });
        ad.show();

        ArrayAdapter<String> adapterGroup = new ArrayAdapter<>(this, R.layout.item_list_simple, getGroups(true));
        ArrayAdapter<String> adapterCity = new ArrayAdapter<>(this, R.layout.item_list_simple, getCities(true));
        ArrayAdapter<String> adapterCountry = new ArrayAdapter<>(this, R.layout.item_list_simple, getCountries(true));
        dropdownGroup.setAdapter(adapterGroup);
        dropdownCity.setAdapter(adapterCity);
        dropdownCountry.setAdapter(adapterCountry);
    }
    private String[] getGroups(boolean withAllEntry) {
        ArrayList<String> groups = new ArrayList<>();
        if(withAllEntry) groups.add(getResources().getString(R.string.all));
        for(Customer c : mDb.getCustomers(null, false, false)) {
            if(!groups.contains(c.mCustomerGroup) && !c.mCustomerGroup.equals(""))
                groups.add(c.mCustomerGroup);
        }
        String[] finalArray = new String[groups.size()];
        groups.toArray(finalArray);
        return finalArray;
    }
    private String[] getCities(boolean withAllEntry) {
        ArrayList<String> groups = new ArrayList<>();
        if(withAllEntry) groups.add(getResources().getString(R.string.all));
        for(Customer c : mDb.getCustomers(null, false, false)) {
            if(!groups.contains(c.mCity) && !c.mCity.equals(""))
                groups.add(c.mCity);
        }
        String[] finalArray = new String[groups.size()];
        groups.toArray(finalArray);
        return finalArray;
    }
    private String[] getCountries(boolean withAllEntry) {
        ArrayList<String> groups = new ArrayList<>();
        if(withAllEntry) groups.add(getResources().getString(R.string.all));
        for(Customer c : mDb.getCustomers(null, false, false)) {
            if(!groups.contains(c.mCountry) && !c.mCountry.equals(""))
                groups.add(c.mCountry);
        }
        String[] finalArray = new String[groups.size()];
        groups.toArray(finalArray);
        return finalArray;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode) {
            case(NEW_APPOINTMENT_REQUEST): {
                if(resultCode == Activity.RESULT_OK) {
                    refreshAppointmentsFromLocalDatabase();
                }
                break;
            }
            case(NEW_CUSTOMER_REQUEST): {
                if(resultCode == Activity.RESULT_OK) {
                    refreshCustomersFromLocalDatabase();
                }
                break;
            }
            case(NEW_VOUCHER_REQUEST): {
                if(resultCode == Activity.RESULT_OK) {
                    refreshVouchersFromLocalDatabase();
                }
                break;
            }
            case(BIRTHDAY_REQUEST):
            case(VIEW_CUSTOMER_REQUEST): {
                if(resultCode == Activity.RESULT_OK) {
                    if(data.getStringExtra("action").equals("update")) {
                        MainActivity.setUnsyncedChanges(this);
                        refreshCustomersFromLocalDatabase();
                    }
                }
                break;
            }
            case(VIEW_VOUCHER_REQUEST): {
                if(resultCode == Activity.RESULT_OK) {
                    if(data.getStringExtra("action").equals("update")) {
                        MainActivity.setUnsyncedChanges(this);
                        refreshVouchersFromLocalDatabase();
                    }
                }
                break;
            }
            case(PICK_CUSTOMER_VCF_REQUEST): {
                if(resultCode == Activity.RESULT_OK && data.getData() != null) {
                    try {
                        List<Customer> newCustomers = CustomerVcfBuilder.readVcfFile(new InputStreamReader(getContentResolver().openInputStream(data.getData())));
                        if(newCustomers.size() > 0) {
                            int counter = 0;
                            for(Customer c : newCustomers) {
                                c.mId = Customer.generateID(counter);
                                mDb.addCustomer(c);
                                counter ++;
                            }
                            refreshCustomersFromLocalDatabase();
                            CommonDialog.show(this, getResources().getString(R.string.import_ok),getResources().getQuantityString(R.plurals.imported, newCustomers.size(), newCustomers.size()), CommonDialog.TYPE.OK, false);
                            MainActivity.setUnsyncedChanges(this);
                        } else
                            CommonDialog.show(this, getResources().getString(R.string.import_fail),getResources().getString(R.string.import_fail_no_entries), CommonDialog.TYPE.FAIL, false);
                    } catch(Exception e) {
                        CommonDialog.show(this, getResources().getString(R.string.import_fail),e.getMessage(), CommonDialog.TYPE.FAIL, false);
                        e.printStackTrace();
                    }
                }
                break;
            }
            case(PICK_CUSTOMER_CSV_REQUEST): {
                if(resultCode == Activity.RESULT_OK && data.getData() != null) {
                    try {
                        List<Customer> newCustomers = CustomerCsvBuilder.readCsvFile(new InputStreamReader(getContentResolver().openInputStream(data.getData())));
                        if(newCustomers.size() > 0) {
                            int counter = 0;
                            for(Customer c : newCustomers) {
                                if(c.mId < 1 || mDb.getCustomerById(c.mId, true, false) != null) {
                                    //Log.e("CSV", "generated new ID");
                                    c.mId = Customer.generateID(counter);
                                }
                                mDb.addCustomer(c);
                                counter ++;
                            }
                            refreshCustomersFromLocalDatabase();
                            CommonDialog.show(this, getResources().getString(R.string.import_ok),getResources().getQuantityString(R.plurals.imported, newCustomers.size(), newCustomers.size()), CommonDialog.TYPE.OK, false);
                            MainActivity.setUnsyncedChanges(this);
                        } else
                            CommonDialog.show(this, getResources().getString(R.string.import_fail),getResources().getString(R.string.import_fail_no_entries), CommonDialog.TYPE.FAIL, false);
                    } catch(Exception e) {
                        CommonDialog.show(this, getResources().getString(R.string.import_fail), e.getMessage(), CommonDialog.TYPE.FAIL, false);
                        e.printStackTrace();
                    }
                }
                break;
            }
            case(PICK_VOUCHER_CSV_REQUEST): {
                if(resultCode == Activity.RESULT_OK && data.getData() != null) {
                    try {
                        List<Voucher> newVouchers = VoucherCsvBuilder.readCsvFile(new InputStreamReader(getContentResolver().openInputStream(data.getData())));
                        if(newVouchers.size() > 0) {
                            int counter = 0;
                            for(Voucher v : newVouchers) {
                                if(v.mId < 1 || mDb.getVoucherById(v.mId, true) != null) {
                                    v.mId = Voucher.generateID(counter);
                                }
                                mDb.addVoucher(v);
                                counter ++;
                            }
                            refreshVouchersFromLocalDatabase();
                            CommonDialog.show(this, getResources().getString(R.string.import_ok),getResources().getQuantityString(R.plurals.imported, newVouchers.size(), newVouchers.size()), CommonDialog.TYPE.OK, false);
                            MainActivity.setUnsyncedChanges(this);
                        } else
                            CommonDialog.show(this, getResources().getString(R.string.import_fail),getResources().getString(R.string.import_fail_no_entries), CommonDialog.TYPE.FAIL, false);
                    } catch(Exception e) {
                        CommonDialog.show(this, getResources().getString(R.string.import_fail), e.getMessage(), CommonDialog.TYPE.FAIL, false);
                        e.printStackTrace();
                    }
                }
                break;
            }
            case(PICK_CALENDAR_ICS_REQUEST): {
                if(resultCode == Activity.RESULT_OK && data.getData() != null) {
                    try {
                        List<CustomerAppointment> newAppointments = CalendarIcsBuilder.readIcsFile(new InputStreamReader(getContentResolver().openInputStream(data.getData())));
                        if(newAppointments.size() > 0) {
                            int counter = 0;
                            for(CustomerAppointment ca : newAppointments) {
                                ca.mId = CustomerAppointment.generateID(counter);
                                ca.mCalendarId = mCurrentCalendarImportSelectedId;
                                mDb.addAppointment(ca);
                                counter ++;
                            }
                            refreshAppointmentsFromLocalDatabase();
                            CommonDialog.show(this, getResources().getString(R.string.import_ok),getResources().getQuantityString(R.plurals.imported, newAppointments.size(), newAppointments.size()), CommonDialog.TYPE.OK, false);
                            MainActivity.setUnsyncedChanges(this);
                        } else
                            CommonDialog.show(this, getResources().getString(R.string.import_fail),getResources().getString(R.string.import_fail_no_entries), CommonDialog.TYPE.FAIL, false);
                    } catch(Exception e) {
                        CommonDialog.show(this, getResources().getString(R.string.import_fail),e.getMessage(), CommonDialog.TYPE.FAIL, false);
                        e.printStackTrace();
                    }
                }
                break;
            }
            case(PICK_CALENDAR_CSV_REQUEST): {
                if(resultCode == Activity.RESULT_OK && data.getData() != null) {
                    try {
                        List<CustomerAppointment> newAppointments = CalendarCsvBuilder.readCsvFile(new InputStreamReader(getContentResolver().openInputStream(data.getData())));
                        if(newAppointments.size() > 0) {
                            int counter = 0;
                            for(CustomerAppointment ca : newAppointments) {
                                if(ca.mId < 1 || mDb.getAppointmentById(ca.mId) != null) {
                                    ca.mId = Customer.generateID(counter);
                                }
                                ca.mCalendarId = mCurrentCalendarImportSelectedId;
                                mDb.addAppointment(ca);
                                counter ++;
                            }
                            refreshAppointmentsFromLocalDatabase();
                            CommonDialog.show(this, getResources().getString(R.string.import_ok),getResources().getQuantityString(R.plurals.imported, newAppointments.size(), newAppointments.size()), CommonDialog.TYPE.OK, false);
                            MainActivity.setUnsyncedChanges(this);
                        } else
                            CommonDialog.show(this, getResources().getString(R.string.import_fail),getResources().getString(R.string.import_fail_no_entries), CommonDialog.TYPE.FAIL, false);
                    } catch(Exception e) {
                        CommonDialog.show(this, getResources().getString(R.string.import_fail), e.getMessage(), CommonDialog.TYPE.FAIL, false);
                        e.printStackTrace();
                    }
                }
                break;
            }
            case(ABOUT_REQUEST):
            case(SETTINGS_REQUEST): {
                //loadSettings();
                recreate();
            }
        }
    }

}
