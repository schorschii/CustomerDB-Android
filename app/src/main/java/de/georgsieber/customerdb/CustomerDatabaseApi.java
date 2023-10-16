package de.georgsieber.customerdb;

import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;

import de.georgsieber.customerdb.model.Customer;
import de.georgsieber.customerdb.model.CustomerAppointment;
import de.georgsieber.customerdb.model.CustomerCalendar;
import de.georgsieber.customerdb.model.CustomerFile;
import de.georgsieber.customerdb.model.Voucher;

public class CustomerDatabaseApi {

    static String MANAGED_API = "https://customerdb.sieber.systems/api.php";

    private WeakReference<MainActivity> mMainActivityReference;

    private String mPurchaseToken;
    private String mApiUrl;
    private String mUsername;
    private String mPassword;

    private CustomerDatabase mDb;

    CustomerDatabaseApi(MainActivity context, String purchaseToken, String username, String password, CustomerDatabase db) {
        mMainActivityReference = new WeakReference<>(context);
        mPurchaseToken = purchaseToken;
        mApiUrl = MANAGED_API;
        mUsername = username;
        mPassword = password;
        mDb = db;
    }
    CustomerDatabaseApi(MainActivity context, String purchaseToken, String url, String username, String password, CustomerDatabase db) {
        mMainActivityReference = new WeakReference<>(context);
        mPurchaseToken = purchaseToken;
        mApiUrl = url;
        mUsername = username;
        mPassword = password;
        mDb = db;
    }

    private readyListener mReadyListener = null;
    public interface readyListener {
        void ready(Exception e);
    }
    void setReadyListener(readyListener listener) {
        this.mReadyListener = listener;
    }

    protected void sync(final Date diffSince) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    putCustomers(diffSince);
                    readCustomers(diffSince);
                    if(mReadyListener != null) mReadyListener.ready(null);
                } catch(Exception e) {
                    if(mReadyListener != null) mReadyListener.ready(e);
                }
            }
        }).start();
    }

    private void putCustomers(Date diffSince) throws Exception {
        try {
            JSONArray jarrayCustomers = new JSONArray();
            for(Customer c : mDb.getCustomers(null, true, true, diffSince)) {
                JSONArray jsonFiles = new JSONArray();
                for(CustomerFile file : c.getFiles()) {
                    JSONObject jsonFile = new JSONObject();
                    jsonFile.put("name", file.mName);
                    jsonFile.put("content", Base64.encodeToString(file.mContent, Base64.NO_WRAP));
                    jsonFiles.put(jsonFile);
                }

                JSONObject jc = new JSONObject();
                jc.put("id", c.mId);
                jc.put("title", c.mTitle);
                jc.put("first_name", c.mFirstName);
                jc.put("last_name", c.mLastName);
                jc.put("phone_home", c.mPhoneHome);
                jc.put("phone_mobile", c.mPhoneMobile);
                jc.put("phone_work", c.mPhoneWork);
                jc.put("email", c.mEmail);
                jc.put("street", c.mStreet);
                jc.put("zipcode", c.mZipcode);
                jc.put("city", c.mCity);
                jc.put("country", c.mCountry);
                jc.put("birthday", c.mBirthday == null ? JSONObject.NULL : CustomerDatabase.dateToStringRaw(c.mBirthday));
                jc.put("customer_group", c.mCustomerGroup);
                jc.put("newsletter", c.mNewsletter ? 1 : 0);
                jc.put("notes", c.mNotes);
                jc.put("image", (c.mImage==null ? JSONObject.NULL : Base64.encodeToString(c.mImage, Base64.NO_WRAP)));
                jc.put("consent", JSONObject.NULL);
                jc.put("files", (jsonFiles.length()==0 ? JSONObject.NULL : jsonFiles.toString()));
                jc.put("custom_fields", c.mCustomFields);
                jc.put("last_modified", CustomerDatabase.dateToString(c.mLastModified));
                jc.put("removed", c.mRemoved);
                jarrayCustomers.put(jc);
            }

            JSONArray jarrayCalendars = new JSONArray();
            for(CustomerCalendar c : mDb.getCalendars(true)) {
                JSONObject jc = new JSONObject();
                jc.put("id", c.mId);
                jc.put("title", c.mTitle);
                jc.put("color", c.mColor);
                jc.put("notes", c.mNotes);
                jc.put("last_modified", CustomerDatabase.dateToString(c.mLastModified));
                jc.put("removed", c.mRemoved);
                jarrayCalendars.put(jc);
            }

            JSONArray jarrayAppointments = new JSONArray();
            for(CustomerAppointment a : mDb.getAppointments(null,null, true, diffSince)) {
                JSONObject jc = new JSONObject();
                jc.put("id", a.mId);
                jc.put("calendar_id", a.mCalendarId);
                jc.put("title", a.mTitle);
                jc.put("notes", a.mNotes);
                jc.put("time_start", a.mTimeStart == null ? JSONObject.NULL : CustomerDatabase.dateToStringRaw(a.mTimeStart));
                jc.put("time_end", a.mTimeEnd == null ? JSONObject.NULL : CustomerDatabase.dateToStringRaw(a.mTimeEnd));
                jc.put("fullday", a.mFullday);
                jc.put("customer", a.mCustomer);
                jc.put("customer_id", a.mCustomerId == null ? JSONObject.NULL : a.mCustomerId);
                jc.put("location", a.mLocation);
                jc.put("last_modified", CustomerDatabase.dateToString(a.mLastModified));
                jc.put("removed", a.mRemoved);
                jarrayAppointments.put(jc);
            }

            JSONArray jarrayVouchers = new JSONArray();
            for(Voucher v : mDb.getVouchers(null,true, diffSince)) {
                JSONObject jc = new JSONObject();
                jc.put("id", v.mId);
                jc.put("original_value", v.mOriginalValue);
                jc.put("current_value", v.mCurrentValue);
                jc.put("voucher_no", v.mVoucherNo);
                jc.put("from_customer", v.mFromCustomer);
                jc.put("from_customer_id", v.mFromCustomerId == null ? JSONObject.NULL : v.mFromCustomerId);
                jc.put("for_customer", v.mForCustomer);
                jc.put("for_customer_id", v.mForCustomerId == null ? JSONObject.NULL : v.mForCustomerId);
                jc.put("issued", CustomerDatabase.dateToString(v.mIssued));
                jc.put("valid_until", v.mValidUntil == null ? JSONObject.NULL : CustomerDatabase.dateToString(v.mValidUntil));
                jc.put("redeemed", v.mRedeemed == null ? JSONObject.NULL : CustomerDatabase.dateToString(v.mRedeemed));
                jc.put("notes", v.mNotes);
                jc.put("last_modified", CustomerDatabase.dateToString(v.mLastModified));
                jc.put("removed", v.mRemoved);
                jarrayVouchers.put(jc);
            }

            JSONObject jparams = new JSONObject();
            jparams.put("playstore_token", mPurchaseToken);
            jparams.put("username", mUsername);
            jparams.put("password", mPassword);
            jparams.put("customers", jarrayCustomers);
            jparams.put("vouchers", jarrayVouchers);
            jparams.put("calendars", jarrayCalendars);
            jparams.put("appointments", jarrayAppointments);

            JSONObject jroot = new JSONObject();
            jroot.put("jsonrpc", "2.0");
            jroot.put("id", 1);
            jroot.put("method", "customerdb.put");
            jroot.put("params", jparams);

            String result = openConnection(jroot.toString());
            //Log.e("API", result);

            try {
                JSONObject jresult = new JSONObject(result);
                if(jresult.isNull("result") || !jresult.getBoolean("result")) {
                    throw new Exception(jresult.getString("error"));
                }
            } catch(JSONException e) {
                throw new Exception(result);
            }
        } catch(JSONException ex) {
            throw new Exception(ex.getMessage());
        }
    }

    private void readCustomers(Date diffSince) throws Exception {
        MainActivity activity = mMainActivityReference.get();
        try {
            JSONObject jparams = new JSONObject();
            jparams.put("diff_since", CustomerDatabase.dateToString(diffSince));
            jparams.put("files", false);
            jparams.put("playstore_token", mPurchaseToken);
            jparams.put("username", mUsername);
            jparams.put("password", mPassword);

            JSONObject jroot = new JSONObject();
            jroot.put("jsonrpc", "2.0");
            jroot.put("id", 1);
            jroot.put("method", "customerdb.read");
            jroot.put("params", jparams);

            String result = openConnection(jroot.toString());
            //Log.e("API", jroot.toString());
            //Log.e("API", result);

            try {
                JSONObject jresult = new JSONObject(result);
                if(jresult.isNull("result") || !jresult.isNull("error")) {
                    throw new Exception(jresult.getString("error"));
                }
            } catch(JSONException e) {
                throw new Exception(result);
            }

            try {
                JSONObject jresult = new JSONObject(result);
                JSONObject jresults = jresult.getJSONObject("result");
                JSONArray jcustomers = jresults.getJSONArray("customers");
                JSONArray jvouchers = jresults.getJSONArray("vouchers");
                JSONArray jcalendars = jresults.getJSONArray("calendars");
                JSONArray jappointments = jresults.getJSONArray("appointments");

                activity.mDb.beginTransaction();

                for(int i = 0; i < jcustomers.length(); i++) {
                    JSONObject jo = jcustomers.getJSONObject(i);
                    Customer c = new Customer();
                    c.mFiles = new ArrayList<>();
                    Iterator<String> iter = jo.keys();
                    while(iter.hasNext()) {
                        String key = iter.next();
                        if(jo.isNull(key)) continue;
                        String value = jo.getString(key);
                        c.putCustomerAttribute(key, value);
                    }
                    if(c.mId <= 0) continue;
                    if(activity.mDb.getCustomerById(c.mId, true, false) == null) {
                        activity.mDb.addCustomer(c);
                    } else {
                        activity.mDb.updateCustomer(c);
                    }
                    if(!jo.isNull("files")) {
                        readCustomer(c.mId);
                    }
                }
                for(int i = 0; i < jcalendars.length(); i++) {
                    JSONObject jo = jcalendars.getJSONObject(i);
                    CustomerCalendar c = new CustomerCalendar();
                    Iterator<String> iter = jo.keys();
                    while(iter.hasNext()) {
                        String key = iter.next();
                        if(jo.isNull(key)) continue;
                        String value = jo.getString(key);
                        c.putAttribute(key, value);
                    }
                    if(c.mId <= 0) continue;
                    if(activity.mDb.getCalendarById(c.mId, true) == null) {
                        activity.mDb.addCalendar(c);
                    } else {
                        activity.mDb.updateCalendar(c);
                    }
                }
                for(int i = 0; i < jappointments.length(); i++) {
                    JSONObject jo = jappointments.getJSONObject(i);
                    CustomerAppointment a = new CustomerAppointment();
                    Iterator<String> iter = jo.keys();
                    while(iter.hasNext()) {
                        String key = iter.next();
                        if(jo.isNull(key)) continue;
                        String value = jo.getString(key);
                        a.putAttribute(key, value);
                    }
                    if(a.mId <= 0) continue;
                    if(activity.mDb.getAppointmentById(a.mId, true) == null) {
                        activity.mDb.addAppointment(a);
                    } else {
                        activity.mDb.updateAppointment(a);
                    }
                }
                for(int i = 0; i < jvouchers.length(); i++) {
                    JSONObject jo = jvouchers.getJSONObject(i);
                    Voucher v = new Voucher();
                    Iterator<String> iter = jo.keys();
                    while(iter.hasNext()) {
                        String key = iter.next();
                        if(jo.isNull(key)) continue;
                        String value = jo.getString(key);
                        v.putVoucherAttribute(key, value);
                    }
                    if(v.mId <= 0) continue;
                    if(activity.mDb.getVoucherById(v.mId, true) == null) {
                        activity.mDb.addVoucher(v);
                    } else {
                        activity.mDb.updateVoucher(v);
                    }
                }

                activity.mDb.commitTransaction();
                activity.mDb.endTransaction();
            } catch(Exception e) {
                activity.mDb.endTransaction();
                try {
                    JSONObject jresult = new JSONObject(result);
                    throw new Exception("Get Values Failed ("+e.getMessage()+") - "+jresult.getString("error"));
                } catch(JSONException e2) {
                    throw new Exception("Get Values Failed ("+e.getMessage()+") - "+result);
                }
            }
        } catch(JSONException ex) {
            throw new Exception(ex.getMessage());
        }
    }

    private void readCustomer(long customerId) throws Exception {
        MainActivity activity = mMainActivityReference.get();

        JSONObject jparams = new JSONObject();
        jparams.put("customer_id", customerId);
        jparams.put("playstore_token", mPurchaseToken);
        jparams.put("username", mUsername);
        jparams.put("password", mPassword);

        JSONObject jroot = new JSONObject();
        jroot.put("jsonrpc", "2.0");
        jroot.put("id", 1);
        jroot.put("method", "customerdb.read.customer");
        jroot.put("params", jparams);

        String result = openConnection(jroot.toString());
        //Log.e("API", jroot.toString());
        //Log.e("API", result);

        try {
            JSONObject jresult = new JSONObject(result);
            if(jresult.isNull("result") || !jresult.isNull("error")) {
                throw new Exception(jresult.getString("error"));
            }
        } catch(JSONException e) {
            throw new Exception(result);
        }

        JSONObject jresult = new JSONObject(result);
        JSONObject jo = jresult.getJSONObject("result");
        Customer c = new Customer();
        Iterator<String> iter = jo.keys();
        while(iter.hasNext()) {
            String key = iter.next();
            if(jo.isNull(key)) continue;
            String value = jo.getString(key);
            c.putCustomerAttribute(key, value);
        }
        activity.mDb.updateCustomer(c);
    }

    private String openConnection(String send) throws Exception {
        String text = "";
        BufferedReader reader = null;

        try {

            URL url = new URL(mApiUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept-Language", Locale.getDefault().getLanguage());
            OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
            wr.write(send);
            wr.flush();

            int statusCode = conn.getResponseCode();
            if(statusCode == 200)
                reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            else
                reader = new BufferedReader(new InputStreamReader((conn).getErrorStream()));
            StringBuilder sb2 = new StringBuilder();
            String line;
            while((line = reader.readLine()) != null) {
                sb2.append(line).append("\n");
            }
            text = sb2.toString();

            if(text.equals("")) text = statusCode + " " + conn.getResponseMessage();

        } catch(Exception ex) {

            throw new Exception(ex.getMessage());

        } finally {

            if(reader != null) try {
                reader.close();
            } catch(IOException e) {
                e.printStackTrace();
            }

        }

        return text;
    }

}
