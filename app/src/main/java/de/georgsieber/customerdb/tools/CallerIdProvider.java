package de.georgsieber.customerdb.tools;

import android.content.ContentProvider;
import android.content.ContentValues;
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
import java.util.ArrayList;

import de.georgsieber.customerdb.CustomerDatabase;
import de.georgsieber.customerdb.R;
import de.georgsieber.customerdb.model.Customer;

public class CallerIdProvider extends ContentProvider {

    private final static int DIRECTORIES = 1;
    private final static int PHONE_LOOKUP = 2;
    private final static int PRIMARY_PHOTO = 3;

    private final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    private Uri authorityUri;

    private CustomerDatabase mDb;

    @Override
    public boolean onCreate() {
        String authority = getContext().getString(R.string.callerid_authority);
        authorityUri = Uri.parse("content://"+authority);
        uriMatcher.addURI(authority, "directories", DIRECTORIES);
        uriMatcher.addURI(authority, "phone_lookup/*", PHONE_LOOKUP);
        uriMatcher.addURI(authority, "photo/primary_photo/*", PRIMARY_PHOTO);
        mDb = new CustomerDatabase(getContext());
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
                String incomingNumber = uri.getPathSegments().get(1);
                Customer customer = mDb.getCustomerByNumber(incomingNumber);
                if(customer != null) {
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
                }
                cursor.addRow(values.toArray());
                return cursor;
        }
        return null;
    }

    @Override
    public AssetFileDescriptor openAssetFile(@NonNull Uri uri, @NonNull String mode) {
        if(uriMatcher.match(uri) == PRIMARY_PHOTO) {
            String incomingNumber = uri.getPathSegments().get(2);
            Customer customer = mDb.getCustomerByNumber(incomingNumber);
            if(customer != null && customer.getImage().length > 0) {
                return bytesToAssetFileDescriptor(customer.getImage());
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
