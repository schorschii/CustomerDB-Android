package de.georgsieber.customerdb.tools;

import android.annotation.SuppressLint;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.UriMatcher;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.ContactsContract;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import de.georgsieber.customerdb.CustomerDatabase;
import de.georgsieber.customerdb.MainActivity;
import de.georgsieber.customerdb.R;
import de.georgsieber.customerdb.model.Customer;

public class CallerIdProvider extends ContentProvider {

    private final static int DIRECTORIES = 1;
    private final static int PHONE_LOOKUP = 2;
    private final static int PRIMARY_PHOTO = 3;

    private final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    private Uri authorityUri;

    private CustomerDatabase mDb;
    private SharedPreferences mSettings;

    @Override
    public boolean onCreate() {
        String authority = getContext().getString(R.string.callerid_authority);
        authorityUri = Uri.parse("content://"+authority);
        uriMatcher.addURI(authority, "directories", DIRECTORIES);
        uriMatcher.addURI(authority, "phone_lookup/*", PHONE_LOOKUP);
        uriMatcher.addURI(authority, "photo/primary_photo/*", PRIMARY_PHOTO);
        mSettings = getContext().getSharedPreferences(MainActivity.PREFS_NAME, 0);
        try {
            mDb = new CustomerDatabase(getContext());
        } catch(Exception ex) {
            return false;
        }
        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] strings, @Nullable String s, @Nullable String[] strings1, @Nullable String s1) {
        MatrixCursor cursor = new MatrixCursor(strings);
        ArrayList<Object> values = new ArrayList<>();
        switch(uriMatcher.match(uri)) {
            case(DIRECTORIES):
                for(String c : strings) {
                    switch(c) {
                        //case(ContactsContract.Directory.PHOTO_SUPPORT):
                        //    values.add(ContactsContract.Directory.PHOTO_SUPPORT_FULL); break;
                        case(ContactsContract.Directory.ACCOUNT_NAME):
                        case(ContactsContract.Directory.ACCOUNT_TYPE):
                        case(ContactsContract.Directory.DISPLAY_NAME):
                            values.add(getContext().getString(R.string.app_name)); break;
                        case(ContactsContract.Directory.TYPE_RESOURCE_ID):
                            values.add(R.string.app_name); break;
                        case(ContactsContract.Directory.EXPORT_SUPPORT):
                            values.add(ContactsContract.Directory.EXPORT_SUPPORT_SAME_ACCOUNT_ONLY); break;
                        case(ContactsContract.Directory.SHORTCUT_SUPPORT):
                            values.add(ContactsContract.Directory.SHORTCUT_SUPPORT_NONE); break;
                        default: values.add(null);
                    }
                }
                cursor.addRow(values.toArray());
                return cursor;
            case(PHONE_LOOKUP):
                // check caller package - we only allow native android phone app
                String callerPackage = uri.getQueryParameter("callerPackage");
                if(callerPackage == null) return cursor;
                // as of 2022, I only know the Google dialer app which supports this phone lookup feature
                // so, we deny requests from other apps - feel free to open a pull request to add another compatible dialer app
                if(!callerPackage.equals("com.android.dialer") && !callerPackage.equals("com.google.android.dialer")) return cursor;

                // prevent bulk queries
                // this seems not to work anymore with recent Google dialer versions
                // they now do multiple lookups for one call for whatever reason
                // since we restricted the packages which can query the information, I can switch this check off with a clear conscience
                int currentTime = (int)(System.currentTimeMillis() / 1000L);
                //int lastLookup = mSettings.getInt("last-caller-id-lookup", 0);
                //if(currentTime - lastLookup < 1) return cursor;
                mSettings.edit().putInt("last-caller-id-lookup", currentTime).apply();

                // do the lookup
                if(mDb == null) return cursor;
                String incomingNumber = uri.getPathSegments().get(1);
                Customer customer = mDb.getCustomerByNumber(incomingNumber);
                if(customer != null) {
                    saveLastCallInfo(getContext(), incomingNumber, customer.getFullName(false));
                    for(String c : strings) {
                        switch(c) {
                            case(ContactsContract.PhoneLookup._ID):
                                values.add(customer.mId); break;
                            case(ContactsContract.PhoneLookup.DISPLAY_NAME):
                                values.add(customer.getFullName(false)); break;
                            case(ContactsContract.PhoneLookup.LABEL):
                                values.add(customer.mCustomerGroup); break;
                            case(ContactsContract.PhoneLookup.PHOTO_THUMBNAIL_URI):
                            case(ContactsContract.PhoneLookup.PHOTO_URI):
                                values.add(
                                        Uri.withAppendedPath(
                                                Uri.withAppendedPath(
                                                        Uri.withAppendedPath(
                                                                authorityUri,
                                                                "photo"
                                                        ),
                                                        "primary_photo"
                                                ),
                                                incomingNumber
                                        )
                                );
                                break;
                            default: values.add(null);
                        }
                    }
                    cursor.addRow(values.toArray());
                } else {
                    saveLastCallInfo(getContext(), incomingNumber, null);
                }
                return cursor;
        }
        return null;
    }

    @Override
    public AssetFileDescriptor openAssetFile(@NonNull Uri uri, @NonNull String mode) {
        if(uriMatcher.match(uri) == PRIMARY_PHOTO && mDb != null) {
            String incomingNumber = uri.getPathSegments().get(2);
            Customer customer = mDb.getCustomerByNumber(incomingNumber);
            if(customer != null && customer.getImage().length > 0) {
                // image from database
                return bytesToAssetFileDescriptor(customer.getImage());
            } else {
                // fallback image from resources
                return getContext().getResources().openRawResourceFd(R.drawable.logo_customerdb_raw);
            }
        }
        return null;
    }

    private AssetFileDescriptor bytesToAssetFileDescriptor(byte[] data) {
        try {
            ParcelFileDescriptor[] pipe = ParcelFileDescriptor.createPipe();
            InputStream inputStream = new ByteArrayInputStream(data);
            ParcelFileDescriptor.AutoCloseOutputStream outputStream = new ParcelFileDescriptor.AutoCloseOutputStream(pipe[1]);
            int len;
            while((len = inputStream.read()) >= 0) {
                outputStream.write(len);
            }
            inputStream.close();
            outputStream.flush();
            outputStream.close();
            return new AssetFileDescriptor(pipe[0], 0, AssetFileDescriptor.UNKNOWN_LENGTH);
        } catch(IOException ignored) {
            return null;
        }
    }

    private void saveLastCallInfo(Context c, String number, String customer) {
        @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");

        String callInfo = sdf.format(new Date()) +" via ContentProvider: "+
                number +" ("+ (customer == null ? c.getResources().getString(R.string.no_customer_found) : customer) +")";

        SharedPreferences.Editor editor = mSettings.edit();
        editor.putString("last-call-received", callInfo);
        editor.apply();
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        return null;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues contentValues) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String s, @Nullable String[] strings) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues contentValues, @Nullable  String s, @Nullable String[] strings) {
        throw new UnsupportedOperationException();
    }
}
