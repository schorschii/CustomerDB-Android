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

import de.georgsieber.customerdb.model.CustomerAppointment;
import de.georgsieber.customerdb.model.CustomerCalendar;
import de.georgsieber.customerdb.model.CustomField;
import de.georgsieber.customerdb.model.Customer;
import de.georgsieber.customerdb.model.CustomerFile;
import de.georgsieber.customerdb.model.Voucher;

import static android.content.Context.MODE_PRIVATE;

@SuppressWarnings("TryFinallyCanBeTryWithResources")
public class CustomerDatabase {

    @SuppressLint("SimpleDateFormat")
    static DateFormat storageFormat = new SimpleDateFormat("yyyy-MM-dd");
    @SuppressLint("SimpleDateFormat")
    public static DateFormat storageFormatWithTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private SQLiteDatabase db;
    private Context context;

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
            db.execSQL("ALTER TABLE customer ADD COLUMN consent BLOB;");
            db.execSQL("ALTER TABLE customer ADD COLUMN newsletter INTEGER default 0;");
            db.execSQL("ALTER TABLE customer ADD COLUMN customer_group VARCHAR default '';");
        }

        if(columnNotExists("customer", "custom_fields")) {
            db.execSQL("ALTER TABLE customer ADD COLUMN custom_fields VARCHAR default '';");
            db.execSQL("CREATE TABLE IF NOT EXISTS customer_extra_fields (id INTEGER PRIMARY KEY AUTOINCREMENT, title VARCHAR, type INTEGER);");
            db.execSQL("CREATE TABLE IF NOT EXISTS customer_extra_presets (id INTEGER PRIMARY KEY AUTOINCREMENT, title VARCHAR, extra_field_id INTEGER);");
        }

        if(columnNotExists("customer", "image")) {
            db.execSQL("ALTER TABLE customer ADD COLUMN image BLOB;");
        }

        if(columnNotExists("voucher", "voucher_no")) {
            db.execSQL("ALTER TABLE voucher ADD COLUMN voucher_no VARCHAR NOT NULL DEFAULT '';");
            db.execSQL("UPDATE customer SET birthday = null WHERE birthday LIKE '%1800%';");
        }

        if(columnNotExists("customer_file", "content")) {
            String currentDateString = storageFormatWithTime.format(new Date());
            beginTransaction();
            db.execSQL("CREATE TABLE IF NOT EXISTS calendar (id INTEGER PRIMARY KEY AUTOINCREMENT, title VARCHAR NOT NULL, color VARCHAR NOT NULL, notes VARCHAR NOT NULL, last_modified DATETIME DEFAULT CURRENT_TIMESTAMP, removed INTEGER DEFAULT 0);");
            db.execSQL("CREATE TABLE IF NOT EXISTS appointment (id INTEGER PRIMARY KEY AUTOINCREMENT, calendar_id INTEGER NOT NULL, title VARCHAR NOT NULL, notes VARCHAR NOT NULL, time_start DATETIME, time_end DATETIME, fullday INTEGER DEFAULT 0, customer VARCHAR NOT NULL, location VARCHAR NOT NULL, last_modified DATETIME DEFAULT CURRENT_TIMESTAMP, removed INTEGER DEFAULT 0);");
            db.execSQL("CREATE TABLE IF NOT EXISTS customer_file (id INTEGER PRIMARY KEY AUTOINCREMENT, customer_id INTEGER NOT NULL, name VARCHAR NOT NULL, content BLOB NOT NULL);");
            Cursor cursor = db.rawQuery("SELECT id, consent FROM customer", null);
            try {
                if(cursor.moveToFirst()) {
                    do {
                        if(!cursor.isNull(1) && cursor.getBlob(1).length > 0) {
                            SQLiteStatement stmt = db.compileStatement("INSERT INTO customer_file (customer_id, name, content) VALUES (?, ?, ?)");
                            stmt.bindLong(1, cursor.getLong(0));
                            stmt.bindString(2, context.getString(R.string.consent)+".jpg");
                            stmt.bindBlob(3, cursor.getBlob(1));
                            stmt.execute();
                        }
                        SQLiteStatement stmt = db.compileStatement("UPDATE customer SET consent = ?, last_modified = ? WHERE id = ?");
                        stmt.bindNull(1);
                        stmt.bindString(2, currentDateString);
                        stmt.bindLong(3, cursor.getLong(0));
                        stmt.execute();
                    } while (cursor.moveToNext());
                }
            } catch (SQLiteException e) {
                Log.e("SQLite Error", e.getMessage());
                System.exit(1);
            } finally {
                cursor.close();
            }
            commitTransaction();
            endTransaction();
        }

    }


    List<CustomerCalendar> getCalendars(boolean showRemoved) {
        String sql = "SELECT id, title, color, notes, last_modified, removed FROM calendar WHERE removed = 0";
        if(showRemoved) sql = "SELECT id, title, color, notes, last_modified, removed FROM calendar";
        Cursor cursor = db.rawQuery(sql, null);
        ArrayList<CustomerCalendar> cf = new ArrayList<>();
        try {
            if(cursor.moveToFirst()) {
                do {
                    Date lastModified = null;
                    try {
                        if(cursor.getString(4) != null && (!cursor.getString(4).equals("")))
                            lastModified = storageFormatWithTime.parse(cursor.getString(4));
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                    CustomerCalendar f = new CustomerCalendar(
                            cursor.getLong(0),
                            cursor.getString(1),
                            cursor.getString(2),
                            cursor.getString(3),
                            lastModified,
                            cursor.getInt(5)
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
    void addCalendar(CustomerCalendar c) {
        SQLiteStatement stmt = db.compileStatement("INSERT INTO calendar (id, title, color, notes, last_modified, removed) VALUES (?,?,?,?,?,?)");

        if(c.mId == -1) c.mId = CustomerCalendar.generateID();
        String lastModifiedString;
        if(c.mLastModified != null) {
            lastModifiedString = storageFormatWithTime.format(c.mLastModified);
        } else {
            lastModifiedString = storageFormatWithTime.format(new Date());
        }
        stmt.bindLong(1, c.mId);
        stmt.bindString(2, c.mTitle);
        stmt.bindString(3, c.mColor);
        stmt.bindString(4, c.mNotes);
        stmt.bindString(5, lastModifiedString);
        stmt.bindLong(6, c.mRemoved);
        stmt.execute();
    }
    void updateCalendar(CustomerCalendar c) {
        SQLiteStatement stmt = db.compileStatement(
                "UPDATE calendar SET title = ?, color = ?, notes = ?, last_modified = ?, removed = ? WHERE id = ?"
        );

        String lastModifiedString;
        if(c.mLastModified != null) {
            lastModifiedString = storageFormatWithTime.format(c.mLastModified);
        } else {
            lastModifiedString = storageFormatWithTime.format(new Date());
        }
        stmt.bindString(1, c.mTitle);
        stmt.bindString(2, c.mColor);
        stmt.bindString(3, c.mNotes);
        stmt.bindString(4, lastModifiedString);
        stmt.bindLong(5, c.mRemoved);
        stmt.bindLong(6, c.mId);
        stmt.execute();
    }
    void removeCalendar(CustomerCalendar c) {
        String currentDateString = storageFormatWithTime.format(new Date());
        SQLiteStatement stmt = db.compileStatement("UPDATE calendar SET removed = 1, title = '', color = '', notes = '', last_modified = ? WHERE id = ?");
        stmt.bindString(1, currentDateString);
        stmt.bindLong(2, c.mId);
        stmt.execute();

        SQLiteStatement stmt2 = db.compileStatement("UPDATE appointment SET removed = 1, calendar_id = -1, title = '', notes = '', time_start = NULL, time_end = NULL, fullday = 0, customer = '', location = '', last_modified = ? WHERE calendar_id = ?");
        stmt2.bindString(1, currentDateString);
        stmt2.bindLong(2, c.mId);
        stmt2.execute();
    }

    CustomerAppointment getAppointmentById(long id) {
        Cursor cursor = db.rawQuery(
                "SELECT id, calendar_id, title, notes, time_start, time_end, fullday, customer, location, last_modified, removed FROM appointment WHERE removed = 0 AND id = ?",
                new String[]{ Long.toString(id) }
        );
        try {
            if(cursor.moveToFirst()) {
                do {
                    Date startTime = null;
                    try {
                        if(cursor.getString(4) != null && (!cursor.getString(4).equals("")))
                            startTime = storageFormatWithTime.parse(cursor.getString(4));
                    } catch (ParseException e) {
                        Log.e("CAL", e.getLocalizedMessage());
                    }
                    Date endTime = null;
                    try {
                        if(cursor.getString(5) != null && (!cursor.getString(5).equals("")))
                            endTime = storageFormatWithTime.parse(cursor.getString(5));
                    } catch (ParseException e) {
                        Log.e("CAL", e.getLocalizedMessage());
                    }
                    Date lastModified = null;
                    try {
                        if(cursor.getString(9) != null && (!cursor.getString(9).equals("")))
                            lastModified = storageFormatWithTime.parse(cursor.getString(9));
                    } catch (ParseException ignored) {}
                    return new CustomerAppointment(
                            cursor.getLong(0),
                            cursor.getLong(1),
                            cursor.getString(2),
                            cursor.getString(3),
                            startTime,
                            endTime,
                            cursor.getInt(6) > 0,
                            cursor.getString(7),
                            cursor.getString(8),
                            lastModified,
                            cursor.getInt(10)
                    );
                } while (cursor.moveToNext());
            }
        } catch (SQLiteException e) {
            Log.e("SQLite Error", e.getMessage());
            return null;
        } finally {
            cursor.close();
        }
        return null;
    }
    List<CustomerAppointment> getAppointments(Long calendarId, Date day, boolean showRemoved) {
        Cursor cursor;
        if(calendarId != null && day != null && !showRemoved) {
            @SuppressLint("SimpleDateFormat") SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
            cursor = db.rawQuery(
                    "SELECT id, calendar_id, title, notes, time_start, time_end, fullday, customer, location, last_modified, removed FROM appointment WHERE calendar_id = ? AND strftime('%Y-%m-%d',time_start) = ? AND removed = 0",
                    new String[]{Long.toString(calendarId), format.format(day)}
            );
        } else if(calendarId != null && day == null && !showRemoved) {
            cursor = db.rawQuery(
                    "SELECT id, calendar_id, title, notes, time_start, time_end, fullday, customer, location, last_modified, removed FROM appointment WHERE calendar_id = ? AND removed = 0",
                    new String[]{Long.toString(calendarId)}
            );
        } else {
            String sql;
            if(showRemoved) {
                sql = "SELECT id, calendar_id, title, notes, time_start, time_end, fullday, customer, location, last_modified, removed FROM appointment";
            } else {
                sql = "SELECT id, calendar_id, title, notes, time_start, time_end, fullday, customer, location, last_modified, removed FROM appointment WHERE removed = 0";
            }
            cursor = db.rawQuery(sql, null);
        }

        ArrayList<CustomerAppointment> al = new ArrayList<>();
        try {
            if(cursor.moveToFirst()) {
                do {
                    Date startTime = null;
                    try {
                        if(cursor.getString(4) != null && (!cursor.getString(4).equals("")))
                            startTime = storageFormatWithTime.parse(cursor.getString(4));
                    } catch (ParseException e) {
                        Log.e("CAL", e.getLocalizedMessage());
                    }
                    Date endTime = null;
                    try {
                        if(cursor.getString(5) != null && (!cursor.getString(5).equals("")))
                            endTime = storageFormatWithTime.parse(cursor.getString(5));
                    } catch (ParseException e) {
                        Log.e("CAL", e.getLocalizedMessage());
                    }
                    Date lastModified = null;
                    try {
                        if(cursor.getString(9) != null && (!cursor.getString(9).equals("")))
                            lastModified = storageFormatWithTime.parse(cursor.getString(9));
                    } catch (ParseException ignored) {}
                    CustomerAppointment a = new CustomerAppointment(
                            cursor.getLong(0),
                            cursor.getLong(1),
                            cursor.getString(2),
                            cursor.getString(3),
                            startTime,
                            endTime,
                            cursor.getInt(6) > 0,
                            cursor.getString(7),
                            cursor.getString(8),
                            lastModified,
                            cursor.getInt(10)
                    );
                    al.add(a);
                } while (cursor.moveToNext());
            }
        } catch (SQLiteException e) {
            Log.e("SQLite Error", e.getMessage());
            return null;
        } finally {
            cursor.close();
        }
        return al;
    }
    void addAppointment(CustomerAppointment a) {
        SQLiteStatement stmt = db.compileStatement("INSERT INTO appointment (id, calendar_id, title, notes, time_start, time_end, fullday, customer, location, last_modified, removed) VALUES (?,?,?,?,?,?,?,?,?,?,?)");

        if(a.mId == -1) a.mId = CustomerAppointment.generateID();
        String timeStartString;
        if(a.mTimeStart != null) {
            timeStartString = storageFormatWithTime.format(a.mTimeStart);
        } else {
            timeStartString = storageFormatWithTime.format(new Date());
        }
        String timeEndString;
        if(a.mTimeEnd != null) {
            timeEndString = storageFormatWithTime.format(a.mTimeEnd);
        } else {
            timeEndString = storageFormatWithTime.format(new Date());
        }
        String lastModifiedString;
        if(a.mLastModified != null) {
            lastModifiedString = storageFormatWithTime.format(a.mLastModified);
        } else {
            lastModifiedString = storageFormatWithTime.format(new Date());
        }
        stmt.bindLong(1, a.mId);
        stmt.bindLong(2, a.mCalendarId);
        stmt.bindString(3, a.mTitle);
        stmt.bindString(4, a.mNotes);
        stmt.bindString(5, timeStartString);
        stmt.bindString(6, timeEndString);
        stmt.bindLong(7, a.mFullday ? 1 : 0);
        stmt.bindString(8, a.mCustomer);
        stmt.bindString(9, a.mLocation);
        stmt.bindString(10, lastModifiedString);
        stmt.bindLong(11, a.mRemoved);
        stmt.execute();
    }
    void updateAppointment(CustomerAppointment a) {
        SQLiteStatement stmt = db.compileStatement(
                "UPDATE appointment SET calendar_id = ?, title = ?, notes = ?, time_start = ?, time_end = ?, fullday = ?, customer = ?, location = ?, last_modified = ?, removed = ? WHERE id = ?"
        );

        String timeStartString;
        if(a.mTimeStart != null) {
            timeStartString = storageFormatWithTime.format(a.mTimeStart);
        } else {
            timeStartString = storageFormatWithTime.format(new Date());
        }
        String timeEndString;
        if(a.mTimeEnd != null) {
            timeEndString = storageFormatWithTime.format(a.mTimeEnd);
        } else {
            timeEndString = storageFormatWithTime.format(new Date());
        }
        String lastModifiedString;
        if(a.mLastModified != null) {
            lastModifiedString = storageFormatWithTime.format(a.mLastModified);
        } else {
            lastModifiedString = storageFormatWithTime.format(new Date());
        }
        stmt.bindLong(1, a.mCalendarId);
        stmt.bindString(2, a.mTitle);
        stmt.bindString(3, a.mNotes);
        stmt.bindString(4, timeStartString);
        stmt.bindString(5, timeEndString);
        stmt.bindLong(6, a.mFullday ? 1 : 0);
        stmt.bindString(7, a.mCustomer);
        stmt.bindString(8, a.mLocation);
        stmt.bindString(9, lastModifiedString);
        stmt.bindLong(10, a.mRemoved);
        stmt.bindLong(11, a.mId);
        stmt.execute();
    }
    void removeAppointment(CustomerAppointment a) {
        String currentDateString = storageFormatWithTime.format(new Date());
        SQLiteStatement stmt = db.compileStatement("UPDATE appointment SET removed = 1, calendar_id = -1, title = '', notes = '', time_start = NULL, time_end = NULL, fullday = 0, customer = '', location = '', last_modified = ? WHERE id = ?");
        stmt.bindString(1, currentDateString);
        stmt.bindLong(2, a.mId);
        stmt.execute();
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

    List<Customer> getCustomers(String search, boolean showRemoved, boolean withFiles) {
        Cursor cursor;
        String selectQuery;
        if(showRemoved) {
            selectQuery = "SELECT id, title, first_name, last_name, phone_home, phone_mobile, phone_work, email, street, zipcode, city, country, birthday, customer_group, newsletter, notes, custom_fields, last_modified, removed FROM customer ORDER BY last_name, first_name ASC";
        } else {
            selectQuery = "SELECT id, title, first_name, last_name, phone_home, phone_mobile, phone_work, email, street, zipcode, city, country, birthday, customer_group, newsletter, notes, custom_fields, last_modified, removed FROM customer WHERE removed = 0 ORDER BY last_name, first_name ASC";
        }
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

        if(withFiles) {
            ArrayList<Customer> customersWithFiles = new ArrayList<>();
            for(Customer c : customers) {
                customersWithFiles.add(getCustomerFiles(c));
            }
            return customersWithFiles;
        }

        return customers;
    }

    Customer getCustomerFiles(Customer c) {
        Cursor cursor = db.rawQuery("SELECT image FROM customer WHERE id = ?", new String[]{Long.toString(c.mId)});
        try {
            if(cursor.moveToFirst()) {
                do {
                    c.mImage = cursor.getBlob(0);
                } while(cursor.moveToNext());
            }
        } catch(SQLiteException e) {
            Log.e("SQLite Error", e.getMessage());
        } finally {
            cursor.close();
        }

        c.mFiles = new ArrayList<>();
        Cursor cursor2 = db.rawQuery("SELECT name, content FROM customer_file WHERE customer_id = ?", new String[]{Long.toString(c.mId)});
        try {
            if(cursor2.moveToFirst()) {
                do {
                    c.mFiles.add(new CustomerFile( cursor2.getString(0), cursor2.getBlob(1) ));
                } while(cursor2.moveToNext());
            }
        } catch(SQLiteException e) {
            Log.e("SQLite Error", e.getMessage());
        } finally {
            cursor2.close();
        }

        return c;
    }

    Customer getCustomerByNumber(String number) {
        List<Customer> allCustomers = getCustomers(null, false, false);
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

    Customer getCustomerById(long id, boolean showDeleted, boolean withFiles) {
        // Do not fetch files for all customers! We'll fetch files only for the one ID match!
        List<Customer> customers = getCustomers(null, showDeleted, false);
        for(Customer c : customers) {
            if(c.mId == id) {
                if(withFiles) {
                    return getCustomerFiles(c);
                }
                return c;
            }
        }
        return null;
    }

    Voucher getVoucherById(long id) {
        return getVoucherById(id, false);
    }
    Voucher getVoucherById(long id, boolean showDeleted) {
        List<Voucher> vouchers = getVouchers(null, showDeleted);
        for(Voucher v : vouchers) {
            if(v.mId == id) {
                return v;
            }
        }
        return null;
    }

    void addCustomer(Customer c) {
        // do not add if name is empty
        if(c.mTitle.equals("")
                && c.mFirstName.equals("")
                && c.mLastName.equals(""))
            return;

        SQLiteStatement stmt = db.compileStatement(
                "INSERT INTO customer (id, title, first_name, last_name, phone_home, phone_mobile, phone_work, email, street, zipcode, city, country, birthday, customer_group, newsletter, notes, custom_fields, image, last_modified, removed) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
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
        stmt.bindString(19, lastModifiedString);
        stmt.bindLong(20, c.mRemoved);
        stmt.execute();

        if(c.mFiles != null) {
            for(CustomerFile file : c.mFiles) {
                SQLiteStatement stmt3 = db.compileStatement("INSERT INTO customer_file (customer_id, name, content) VALUES (?,?,?)");
                stmt3.bindLong(1, id);
                stmt3.bindString(2, file.mName);
                stmt3.bindBlob(3, file.mContent);
                stmt3.execute();
            }
        }
    }

    void updateCustomer(Customer c) {
        SQLiteStatement stmt = db.compileStatement(
                "UPDATE customer SET title = ?, first_name = ?, last_name = ?, phone_home = ?, phone_mobile = ?, phone_work = ?, email = ?, street = ?, zipcode = ?, city = ?, country = ?, birthday = ?, customer_group = ?, newsletter = ?, notes = ?, image = ?, custom_fields = ?, last_modified = ?, removed = ? WHERE id = ?"
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
        stmt.bindString(17, c.mCustomFields);
        stmt.bindString(18, lastModifiedString);
        stmt.bindLong(19, c.mRemoved);
        stmt.bindLong(20, c.mId);
        stmt.execute();

        if(c.mFiles != null) {
            SQLiteStatement stmt2 = db.compileStatement("DELETE FROM customer_file WHERE customer_id = ?");
            stmt2.bindLong(1, c.mId);
            stmt2.execute();
            for(CustomerFile file : c.mFiles) {
                SQLiteStatement stmt3 = db.compileStatement("INSERT INTO customer_file (customer_id, name, content) VALUES (?,?,?)");
                stmt3.bindLong(1, c.mId);
                stmt3.bindString(2, file.mName);
                stmt3.bindBlob(3, file.mContent);
                stmt3.execute();
            }
        }
    }

    void removeCustomer(Customer c) {
        String currentDateString = storageFormatWithTime.format(new Date());
        SQLiteStatement stmt = db.compileStatement("UPDATE customer SET removed = 1, title = '', first_name = '', last_name = '', phone_home = '', phone_mobile = '', phone_work = '', email = '', street = '', city = '', country = '', notes = '', customer_group = '', custom_fields = '', image = '', consent = '', birthday = '', newsletter = 0, last_modified = ? WHERE id = ?");
        stmt.bindString(1, currentDateString);
        stmt.bindLong(2, c.mId);
        stmt.execute();

        SQLiteStatement stmt2 = db.compileStatement("DELETE FROM customer_file WHERE customer_id = ?");
        stmt2.bindLong(1, c.mId);
        stmt2.execute();
    }

    void removeVoucher(Voucher v) {
        String currentDateString = storageFormatWithTime.format(new Date());
        SQLiteStatement stmt = db.compileStatement("UPDATE voucher SET removed = 1, current_value = 0, original_value = 0, from_customer = '', for_customer = '', notes = '', last_modified = ? WHERE id = ?");
        stmt.bindString(1, currentDateString);
        stmt.bindLong(2, v.mId);
        stmt.execute();
    }

    void truncateCustomers() {
        SQLiteStatement stmt = db.compileStatement("DELETE FROM customer WHERE 1=1");
        stmt.execute();
        SQLiteStatement stmt2 = db.compileStatement("DELETE FROM customer_file WHERE 1=1");
        stmt2.execute();
    }
    void truncateVouchers() {
        SQLiteStatement stmt = db.compileStatement("DELETE FROM voucher WHERE 1=1");
        stmt.execute();
    }
    void truncateCalendars() {
        SQLiteStatement stmt = db.compileStatement("DELETE FROM calendar WHERE 1=1");
        stmt.execute();
    }
    void truncateAppointments() {
        SQLiteStatement stmt = db.compileStatement("DELETE FROM appointment WHERE 1=1");
        stmt.execute();
    }
}
