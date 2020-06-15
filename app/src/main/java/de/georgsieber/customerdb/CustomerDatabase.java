package de.georgsieber.customerdb;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.telephony.PhoneNumberUtils;
import android.util.Log;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteStatement;

import java.io.File;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import de.georgsieber.customerdb.model.CustomField;
import de.georgsieber.customerdb.model.Customer;
import de.georgsieber.customerdb.model.Voucher;

import static android.content.Context.MODE_PRIVATE;

public class CustomerDatabase {

    @SuppressLint("SimpleDateFormat")
    static DateFormat storageFormat = new SimpleDateFormat("yyyy-MM-dd");
    @SuppressLint("SimpleDateFormat")
    public static DateFormat storageFormatWithTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private SQLiteDatabase db;
    Context context;

    CustomerDatabase(Context context) {
        this.context = context;
        db = context.openOrCreateDatabase(getStorage().getPath(), MODE_PRIVATE, null);
        db.execSQL("CREATE TABLE IF NOT EXISTS customer (id INTEGER PRIMARY KEY AUTOINCREMENT, title VARCHAR, first_name VARCHAR, last_name VARCHAR, phone_home VARCHAR, phone_mobile VARCHAR, phone_work VARCHAR, email VARCHAR, street VARCHAR, zipcode VARCHAR, city VARCHAR, country VARCHAR, birthday DATETIME, last_modified DATETIME, notes VARCHAR, removed INTEGER DEFAULT 0);");
        db.execSQL("CREATE TABLE IF NOT EXISTS voucher (id INTEGER PRIMARY KEY AUTOINCREMENT, current_value REAL, original_value REAL, from_customer VARCHAR, for_customer VARCHAR, issued DATETIME DEFAULT CURRENT_TIMESTAMP, valid_until DATETIME DEFAULT NULL, redeemed DATETIME DEFAULT NULL, last_modified DATETIME DEFAULT NULL, notes VARCHAR, removed INTEGER DEFAULT 0);");
        upgradeDatabase();
        scanFile(getStorage());
    }

    public void close() {
        db.close();
    }

    void beginTransaction() {
        db.beginTransaction();
    }
    void endTransaction() {
        db.endTransaction();
    }
    void commitTransaction() {
        db.setTransactionSuccessful();
    }

    private File getStorage() {
        return new File(context.getExternalFilesDir(null), "customers.sqlite");
    }

    private void scanFile(File f) {
        Uri uri = Uri.fromFile(f);
        Intent scanFileIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri);
        context.sendBroadcast(scanFileIntent);
    }

    private boolean columnNotExists(String table, String column) {
        Cursor cursor = db.rawQuery("PRAGMA table_info("+ table +")", null);
        if(cursor != null) {
            while(cursor.moveToNext()) {
                String name = cursor.getString(cursor.getColumnIndex("name"));
                if(column.equalsIgnoreCase(name)) {
                    return false;
                }
            }
        }
        if(cursor != null) cursor.close();
        return true;
    }

    private void upgradeDatabase() {
        if(columnNotExists("customer", "customer_group")) {
            Log.i("DBSchemaUpgrade","Now upgrading to 1.13...");
            db.execSQL("ALTER TABLE customer ADD COLUMN consent BLOB;");
            db.execSQL("ALTER TABLE customer ADD COLUMN newsletter INTEGER default 0;");
            db.execSQL("ALTER TABLE customer ADD COLUMN customer_group VARCHAR default '';");
        }
        if(columnNotExists("customer", "custom_fields")) {
            Log.i("DBSchemaUpgrade","Now upgrading to 1.33...");
            db.execSQL("ALTER TABLE customer ADD COLUMN custom_fields VARCHAR default '';");
            db.execSQL("CREATE TABLE IF NOT EXISTS customer_extra_fields (id INTEGER PRIMARY KEY AUTOINCREMENT, title VARCHAR, type INTEGER);");
            db.execSQL("CREATE TABLE IF NOT EXISTS customer_extra_presets (id INTEGER PRIMARY KEY AUTOINCREMENT, title VARCHAR, extra_field_id INTEGER);");
        }
        if(columnNotExists("customer", "image")) {
            Log.i("DBSchemaUpgrade","Now upgrading to 2.1...");
            db.execSQL("ALTER TABLE customer ADD COLUMN image BLOB;");
        }
        if(columnNotExists("voucher", "voucher_no")) {
            Log.i("DBSchemaUpgrade","Now upgrading to 3.0...");
            db.execSQL("ALTER TABLE voucher ADD COLUMN voucher_no VARCHAR NOT NULL DEFAULT '';");
            db.execSQL("UPDATE customer SET birthday = null WHERE birthday LIKE '%1800%';");
        }
    }


    Customer getCustomerByNumber(String number) {
        List<Customer> allCustomers = getCustomers(null);
        List<Customer> results = new ArrayList<>();
        for(Customer c : allCustomers) {
            if(PhoneNumberUtils.compare(c.mPhoneHome, number)
            || PhoneNumberUtils.compare(c.mPhoneMobile, number)
            || PhoneNumberUtils.compare(c.mPhoneWork, number)) {
                results.add(c);
                break;
            }
            for(CustomField cf : c.getCustomFields()) {
                if((!cf.mValue.trim().equals(""))
                        && PhoneNumberUtils.compare(cf.mValue.trim(), number)) {
                    results.add(c);
                }
            }
        }
        if(results.size() > 0)
            return results.get(0);
        return null;
    }

    List<CustomField> getCustomFields() {
        Cursor cursor = db.rawQuery("SELECT id, title, type FROM customer_extra_fields", null);
        ArrayList<CustomField> cf = new ArrayList<>();
        try {
            if (cursor.moveToFirst()) {
                do {
                    CustomField f = new CustomField(
                            cursor.getInt(0),
                            cursor.getString(1),
                            cursor.getInt(2)
                    );
                    cf.add(f);
                } while (cursor.moveToNext());
            }
        } catch (SQLiteException e) {
            Log.e("SQLite Error", e.getMessage());
            return null;
        } finally {
            cursor.close();
        }
        return cf;
    }
    List<CustomField> getCustomFieldPresets(int customFieldId) {
        Cursor cursor = db.rawQuery("SELECT id, title FROM customer_extra_presets WHERE extra_field_id = ?", new String[]{Integer.toString(customFieldId)});
        ArrayList<CustomField> presets = new ArrayList<>();
        try {
            if (cursor.moveToFirst()) {
                do {
                    presets.add(new CustomField(cursor.getInt(0), cursor.getString(1), -1));
                } while (cursor.moveToNext());
            }
        } catch (SQLiteException e) {
            Log.e("SQLite Error", e.getMessage());
            return null;
        } finally {
            cursor.close();
        }
        return presets;
    }
    void addCustomField(CustomField cf) {
        SQLiteStatement stmt = db.compileStatement("INSERT INTO customer_extra_fields (title, type) VALUES (?, ?)");
        stmt.bindString(1, cf.mTitle);
        stmt.bindLong(2, cf.mType);
        stmt.execute();
    }
    void addCustomFieldPreset(int fieldId, String preset) {
        SQLiteStatement stmt = db.compileStatement("INSERT INTO customer_extra_presets (title, extra_field_id) VALUES (?,?)");
        stmt.bindString(1, preset);
        stmt.bindLong(2, fieldId);
        stmt.execute();
    }
    void updateCustomField(CustomField cf) {
        SQLiteStatement stmt = db.compileStatement("UPDATE customer_extra_fields SET title = ?, type = ? WHERE id = ?");
        stmt.bindString(1, cf.mTitle);
        stmt.bindLong(2, cf.mType);
        stmt.bindLong(3, cf.mId);
        stmt.execute();
    }
    void removeCustomField(int id) {
        SQLiteStatement stmt = db.compileStatement("DELETE FROM customer_extra_fields WHERE id = ?");
        stmt.bindLong(1, id);
        stmt.execute();
    }
    void removeCustomFieldPreset(int id) {
        SQLiteStatement stmt = db.compileStatement("DELETE FROM customer_extra_presets WHERE id = ?");
        stmt.bindLong(1, id);
        stmt.execute();
    }

    List<Voucher> getVouchers(String search, boolean showRemoved) {
        Cursor cursor;
        String selectQuery = "SELECT id, current_value, original_value, voucher_no, from_customer, for_customer, issued, valid_until, redeemed, last_modified, notes, removed FROM voucher ORDER BY issued DESC";
        cursor = db.rawQuery(selectQuery, null);

        ArrayList<Voucher> vouchers = new ArrayList<>();
        try {
            if(cursor.moveToFirst()) {
                do {
                    Date issued = new Date();
                    try {
                        if(cursor.getString(6) != null && (!cursor.getString(6).equals("")))
                            issued = storageFormatWithTime.parse(cursor.getString(6));
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                    Date validUntil = null;
                    try {
                        if(cursor.getString(7) != null && (!cursor.getString(7).equals("")))
                            validUntil = storageFormatWithTime.parse(cursor.getString(7));
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                    Date redeemed = null;
                    try {
                        if(cursor.getString(8) != null && (!cursor.getString(8).equals("")))
                            redeemed = storageFormatWithTime.parse(cursor.getString(8));
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                    Date lastModified = null;
                    try {
                        if(cursor.getString(9) != null && (!cursor.getString(9).equals("")))
                            lastModified = storageFormatWithTime.parse(cursor.getString(9));
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                    Voucher v = new Voucher(
                            cursor.getLong(0),
                            cursor.getDouble(1),
                            cursor.getDouble(2),
                            cursor.getString(3),
                            cursor.getString(4),
                            cursor.getString(5),
                            issued,
                            validUntil,
                            redeemed,
                            lastModified,
                            cursor.getString(10),
                            cursor.getInt(11)
                    );

                    if(cursor.getInt(11) == 0 || showRemoved) {
                        if(search == null || search.equals("")) {
                            vouchers.add(v);
                        } else {
                            // filter
                            if(
                                    v.mNotes.toUpperCase().contains(search.toUpperCase()) ||
                                            v.mVoucherNo.toUpperCase().contains(search.toUpperCase()) ||
                                            v.mFromCustomer.toUpperCase().contains(search.toUpperCase()) ||
                                            v.mForCustomer.toUpperCase().contains(search.toUpperCase())
                                    ) vouchers.add(v);
                        }
                    }
                } while(cursor.moveToNext());
            }
        } catch(SQLiteException e) {
            Log.e("SQLite Error", e.getMessage());
            return null;
        } finally {
            cursor.close();
        }
        return vouchers;
    }
    void addVoucher(Voucher v) {
        SQLiteStatement stmt = db.compileStatement(
                "INSERT INTO voucher (id, current_value, original_value, voucher_no, from_customer, for_customer, issued, valid_until, redeemed, last_modified, notes, removed) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
        );

        long id = v.mId;
        if(id == -1) id = Voucher.generateID();

        String issuedString = "";
        if(v.mIssued != null) {
            issuedString = storageFormatWithTime.format(v.mIssued);
        }
        String validUntilString = "";
        if(v.mValidUntil != null) {
            validUntilString = storageFormatWithTime.format(v.mValidUntil);
        }
        String redeemedString = "";
        if(v.mRedeemed != null) {
            redeemedString = storageFormatWithTime.format(v.mRedeemed);
        }
        String lastModifiedString;
        if(v.mLastModified != null) {
            lastModifiedString = storageFormatWithTime.format(v.mLastModified);
        } else {
            lastModifiedString = storageFormatWithTime.format(new Date());
        }

        stmt.bindLong(1, id);
        stmt.bindDouble(2, v.mCurrentValue);
        stmt.bindDouble(3, v.mOriginalValue);
        stmt.bindString(4, v.mVoucherNo);
        stmt.bindString(5, v.mFromCustomer);
        stmt.bindString(6, v.mForCustomer);
        stmt.bindString(7, issuedString);
        stmt.bindString(8, validUntilString);
        stmt.bindString(9, redeemedString);
        stmt.bindString(10, lastModifiedString);
        stmt.bindString(11, v.mNotes);
        stmt.bindLong(12, v.mRemoved);
        stmt.execute();
    }
    void updateVoucher(Voucher v) {
        SQLiteStatement stmt = db.compileStatement(
                "UPDATE voucher SET current_value = ?, original_value = ?, voucher_no = ?, from_customer = ?, for_customer = ?, issued = ?, valid_until = ?, redeemed = ?, last_modified = ?, notes = ?, removed = ? WHERE id = ?"
        );

        String issuedString = "";
        if(v.mIssued != null) {
            issuedString = storageFormatWithTime.format(v.mIssued);
        }
        String validUntilString = "";
        if(v.mValidUntil != null) {
            validUntilString = storageFormatWithTime.format(v.mValidUntil);
        }
        String redeemedString = "";
        if(v.mRedeemed != null) {
            redeemedString = storageFormatWithTime.format(v.mRedeemed);
        }
        String lastModifiedString;
        if(v.mLastModified != null) {
            lastModifiedString = storageFormatWithTime.format(v.mLastModified);
        } else {
            lastModifiedString = storageFormatWithTime.format(new Date());
        }

        stmt.bindDouble(1, v.mCurrentValue);
        stmt.bindDouble(2, v.mOriginalValue);
        stmt.bindString(3, v.mVoucherNo);
        stmt.bindString(4, v.mFromCustomer);
        stmt.bindString(5, v.mForCustomer);
        stmt.bindString(6, issuedString);
        stmt.bindString(7, validUntilString);
        stmt.bindString(8, redeemedString);
        stmt.bindString(9, lastModifiedString);
        stmt.bindString(10, v.mNotes);
        stmt.bindLong(11, v.mRemoved);
        stmt.bindLong(12, v.mId);
        stmt.execute();
    }

    List<Customer> getCustomers(String search) {
        Cursor cursor;
        String selectQuery = "SELECT id, title, first_name, last_name, phone_home, phone_mobile, phone_work, email, street, zipcode, city, country, birthday, customer_group, newsletter, notes, custom_fields, last_modified, removed FROM customer WHERE removed = 0 ORDER BY last_name, first_name ASC";
        cursor = db.rawQuery(selectQuery, null);
        ArrayList<Customer> customers = new ArrayList<>();
        try {
            if(cursor.moveToFirst()) {
                do {
                    Date birthday = null;
                    try {
                        if(cursor.getString(12) != null && (!cursor.getString(12).equals("")))
                            birthday = storageFormat.parse(cursor.getString(12));
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                    Date lastModified = new Date();
                    try {
                        if(cursor.getString(17) != null && (!cursor.getString(17).equals("")))
                            lastModified = storageFormatWithTime.parse(cursor.getString(17));
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                    Customer c = new Customer(
                            cursor.getLong(0),
                            cursor.getString(1),
                            cursor.getString(2),
                            cursor.getString(3),
                            cursor.getString(4),
                            cursor.getString(5),
                            cursor.getString(6),
                            cursor.getString(7),
                            cursor.getString(8),
                            cursor.getString(9),
                            cursor.getString(10),
                            cursor.getString(11),
                            birthday,
                            cursor.getString(13),
                            cursor.getInt(14) > 0,
                            cursor.getString(15),
                            cursor.getString(16),
                            lastModified,
                            cursor.getInt(18)
                    );
                    if(search == null || search.equals("")) {
                        customers.add(c);
                    } else {
                        // search in default fields
                        String searchUpperCase = search.toUpperCase();
                        if(c.mTitle.toUpperCase().contains(searchUpperCase) ||
                                c.mFirstName.toUpperCase().contains(searchUpperCase) ||
                                c.mLastName.toUpperCase().contains(searchUpperCase) ||
                                c.mPhoneHome.toUpperCase().contains(searchUpperCase) ||
                                c.mPhoneMobile.toUpperCase().contains(searchUpperCase) ||
                                c.mPhoneWork.toUpperCase().contains(searchUpperCase) ||
                                c.mEmail.toUpperCase().contains(searchUpperCase) ||
                                c.mStreet.toUpperCase().contains(searchUpperCase) ||
                                c.mZipcode.toUpperCase().contains(searchUpperCase) ||
                                c.mCity.toUpperCase().contains(searchUpperCase) ||
                                c.mCustomerGroup.toUpperCase().contains(searchUpperCase) ||
                                c.mNotes.toUpperCase().contains(searchUpperCase)
                        ) {
                            customers.add(c);
                        } else {
                            // search in custom fields
                            for(CustomField cf : c.getCustomFields()) {
                                if(cf.mValue.toUpperCase().contains(searchUpperCase)) {
                                    customers.add(c);
                                    break;
                                }
                            }
                        }
                    }
                } while(cursor.moveToNext());
            }
        } catch(SQLiteException e) {
            Log.e("SQLite Error", e.getMessage());
            return null;
        } finally {
            cursor.close();
        }
        return customers;
    }

    Customer getCustomerById(long id) {
        return getCustomerById(id, false);
    }
    Customer getCustomerById(long id, boolean showDeleted) {
        List<Customer> customers;
        if(showDeleted) customers = getAllCustomers();
        else customers = getCustomers(null);
        for(Customer c : customers) {
            if(c.mId == id) {
                return c;
            }
        }
        return null;
    }

    List<Customer> getAllCustomers() {
        Cursor cursor = db.rawQuery("SELECT id, title, first_name, last_name, phone_home, phone_mobile, phone_work, email, street, zipcode, city, country, birthday, customer_group, newsletter, notes, custom_fields, last_modified, removed, image, consent FROM customer", null);

        ArrayList<Customer> customers = new ArrayList<>();
        try {
            if(cursor.moveToFirst()) {
                do {
                    Date birthday = null;
                    try {
                        if(cursor.getString(12) != null && (!cursor.getString(12).equals("")))
                            birthday = storageFormat.parse(cursor.getString(12));
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                    Date lastModified = new Date();
                    try {
                        if(cursor.getString(17) != null && (!cursor.getString(17).equals("")))
                            lastModified = storageFormatWithTime.parse(cursor.getString(17));
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                    Customer c = new Customer(
                            cursor.getLong(0),
                            cursor.getString(1),
                            cursor.getString(2),
                            cursor.getString(3),
                            cursor.getString(4),
                            cursor.getString(5),
                            cursor.getString(6),
                            cursor.getString(7),
                            cursor.getString(8),
                            cursor.getString(9),
                            cursor.getString(10),
                            cursor.getString(11),
                            birthday,
                            cursor.getString(13),
                            cursor.getInt(14) > 0,
                            cursor.getString(15),
                            cursor.getString(16),
                            lastModified,
                            cursor.getInt(18)
                    );
                    c.mImage = cursor.getBlob(19);
                    c.mConsentImage = cursor.getBlob(20);
                    customers.add(c);
                } while(cursor.moveToNext());
            }
        } catch(SQLiteException e) {
            Log.e("SQLite Error", e.getMessage());
            return null;
        } finally {
            cursor.close();
        }
        return customers;
    }

    Customer readCustomerImages(Customer c) {
        Cursor cursor;
        cursor = db.rawQuery(
                "SELECT image, consent FROM customer WHERE id = ?",
                new String[]{Long.toString(c.mId)}
        );
        try {
            if(cursor.moveToFirst()) {
                do {
                    c.mImage = cursor.getBlob(0);
                    c.mConsentImage = cursor.getBlob(1);
                } while(cursor.moveToNext());
            }
        } catch(SQLiteException e) {
            Log.e("SQLite Error", e.getMessage());
            return null;
        } finally {
            cursor.close();
        }
        return c;
    }

    void addCustomer(Customer c) {
        // do not add if name is empty
        if(c.mTitle.equals("")
                && c.mFirstName.equals("")
                && c.mLastName.equals(""))
            return;

        SQLiteStatement stmt = db.compileStatement(
                "INSERT INTO customer (id, title, first_name, last_name, phone_home, phone_mobile, phone_work, email, street, zipcode, city, country, birthday, customer_group, newsletter, notes, custom_fields, image, consent, last_modified, removed) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
        );

        long id = c.mId;
        if(id == -1) id = Customer.generateID();

        String birthdayString = "";
        if(c.mBirthday != null) {
            birthdayString = storageFormat.format(c.mBirthday);
        }
        String lastModifiedString;
        if(c.mLastModified != null) {
            lastModifiedString = storageFormatWithTime.format(c.mLastModified);
        } else {
            lastModifiedString = storageFormatWithTime.format(new Date());
        }

        stmt.bindLong(1, id);
        stmt.bindString(2, c.mTitle);
        stmt.bindString(3, c.mFirstName);
        stmt.bindString(4, c.mLastName);
        stmt.bindString(5, c.mPhoneHome);
        stmt.bindString(6, c.mPhoneMobile);
        stmt.bindString(7, c.mPhoneWork);
        stmt.bindString(8, c.mEmail);
        stmt.bindString(9, c.mStreet);
        stmt.bindString(10, c.mZipcode);
        stmt.bindString(11, c.mCity);
        stmt.bindString(12, c.mCountry);
        stmt.bindString(13, birthdayString);
        stmt.bindString(14, c.mCustomerGroup);
        stmt.bindLong(15, c.mNewsletter ? 1 : 0);
        stmt.bindString(16, c.mNotes);
        stmt.bindString(17, c.mCustomFields);
        stmt.bindBlob(18, c.getImage());
        stmt.bindBlob(19, c.getConsent());
        stmt.bindString(20, lastModifiedString);
        stmt.bindLong(21, c.mRemoved);
        stmt.execute();
    }

    void updateCustomer(Customer c) {
        SQLiteStatement stmt = db.compileStatement(
                "UPDATE customer SET title = ?, first_name = ?, last_name = ?, phone_home = ?, phone_mobile = ?, phone_work = ?, email = ?, street = ?, zipcode = ?, city = ?, country = ?, birthday = ?, customer_group = ?, newsletter = ?, notes = ?, image = ?, consent = ?, custom_fields = ?, last_modified = ?, removed = ? WHERE id = ?"
        );

        String birthdayString = "";
        if(c.mBirthday != null) {
            birthdayString = storageFormat.format(c.mBirthday);
        }
        String lastModifiedString;
        if(c.mLastModified != null) {
            lastModifiedString = storageFormatWithTime.format(c.mLastModified);
        } else {
            lastModifiedString = storageFormatWithTime.format(new Date());
        }

        stmt.bindString(1, c.mTitle);
        stmt.bindString(2, c.mFirstName);
        stmt.bindString(3, c.mLastName);
        stmt.bindString(4, c.mPhoneHome);
        stmt.bindString(5, c.mPhoneMobile);
        stmt.bindString(6, c.mPhoneWork);
        stmt.bindString(7, c.mEmail);
        stmt.bindString(8, c.mStreet);
        stmt.bindString(9, c.mZipcode);
        stmt.bindString(10, c.mCity);
        stmt.bindString(11, c.mCountry);
        stmt.bindString(12, birthdayString);
        stmt.bindString(13, c.mCustomerGroup);
        stmt.bindLong(14, c.mNewsletter ? 1 : 0);
        stmt.bindString(15, c.mNotes);
        stmt.bindBlob(16, c.getImage());
        stmt.bindBlob(17, c.getConsent());
        stmt.bindString(18, c.mCustomFields);
        stmt.bindString(19, lastModifiedString);
        stmt.bindLong(20, c.mRemoved);
        stmt.bindLong(21, c.mId);
        stmt.execute();
    }

    void removeCustomer(Customer c) {
        String currentDateString = storageFormatWithTime.format(new Date());
        SQLiteStatement stmt = db.compileStatement("UPDATE customer SET removed = 1, title = '', first_name = '', last_name = '', phone_home = '', phone_mobile = '', phone_work = '', email = '', street = '', city = '', country = '', notes = '', customer_group = '', custom_fields = '', image = '', consent = '', birthday = '', newsletter = 0, last_modified = ? WHERE id = ?");
        stmt.bindString(1, currentDateString);
        stmt.bindLong(2, c.mId);
        stmt.execute();
    }

    void removeVoucher(Voucher v) {
        String currentDateString = storageFormatWithTime.format(new Date());
        SQLiteStatement stmt = db.compileStatement("UPDATE voucher SET removed = 1, current_value = 0, original_value = 0, from_customer = '', for_customer = '', notes = '', last_modified = ? WHERE id = ?");
        stmt.bindString(1, currentDateString);
        stmt.bindLong(2, v.mId);
        stmt.execute();
    }

    void cleanCustomers() {
        db.execSQL("DELETE FROM customer WHERE removed = 1;");
    }
    void cleanVouchers() {
        db.execSQL("DELETE FROM voucher WHERE removed = 1;");
    }

    void truncateCustomer() {
        SQLiteStatement stmt = db.compileStatement("DELETE FROM customer WHERE 1=1");
        stmt.execute();
    }
    void truncateVoucher() {
        SQLiteStatement stmt = db.compileStatement("DELETE FROM voucher WHERE 1=1");
        stmt.execute();
    }
}
