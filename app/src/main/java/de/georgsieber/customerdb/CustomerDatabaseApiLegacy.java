package de.georgsieber.customerdb;

import android.os.AsyncTask;
import android.util.Base64;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.List;

import de.georgsieber.customerdb.model.Customer;
import de.georgsieber.customerdb.model.Voucher;

class CustomerDatabaseApiLegacy extends AsyncTask<Void, Void, String> {

    private WeakReference<MainActivity> activityReference;

    private String authkey;
    private String urlString;
    private String method;
    private List<Customer> customers;
    private List<Voucher> vouchers;
    private static final String POST_PARAM_KEYVALUE_SEPARATOR = "=";
    private static final String POST_PARAM_SEPARATOR = "&";
    private static final String POST_ENCODING = "UTF-8";

    CustomerDatabaseApiLegacy(MainActivity context, String url, String authkey, String method, List<Customer> customers, List<Voucher> vouchers) {
        activityReference = new WeakReference<>(context);
        this.urlString = url;
        this.authkey = authkey;
        this.method = method;
        this.customers = customers;
        this.vouchers = vouchers;
    }

    @Override
    protected String doInBackground(Void... params) {
        try {
            if(this.method.equals("sync")) {
                this.method = "putEntries";
                String statusPutEntries = openConnection();
                if(statusPutEntries.trim().equals("OK")) {
                    this.method = "getEntries";
                    onPostGetEntries(openConnection());
                    return "OK";
                } else {
                    Log.i("putEntriesFailed", statusPutEntries);
                    return statusPutEntries;
                }
            } else {
                return openConnection();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "doInBackground failed";
    }

    private String openConnection() {
        StringBuilder sb = new StringBuilder();
        try {
            sb.append(URLEncoder.encode("_AUTHKEY", POST_ENCODING));
            sb.append(POST_PARAM_KEYVALUE_SEPARATOR);
            sb.append(URLEncoder.encode(this.authkey, POST_ENCODING));
            sb.append(POST_PARAM_SEPARATOR);
            sb.append(URLEncoder.encode("_METHOD", POST_ENCODING));
            sb.append(POST_PARAM_KEYVALUE_SEPARATOR);
            sb.append(URLEncoder.encode(this.method, POST_ENCODING));
            sb.append(POST_PARAM_SEPARATOR);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        if(this.method.equals("putEntries")) {
            if(this.customers != null) {
                for(Customer c : customers) {
                    String birthdayString = c.mBirthday==null ? "1800-01-01" : CustomerDatabase.storageFormat.format(c.mBirthday) + " 00:00:00";
                    String lastModifiedString = CustomerDatabase.storageFormatWithTime.format(c.mLastModified);
                    sb.append(appendParameter("id", String.valueOf(c.mId)));
                    sb.append(appendParameter("title", c.mTitle));
                    sb.append(appendParameter("first_name", c.mFirstName));
                    sb.append(appendParameter("last_name", c.mLastName));
                    sb.append(appendParameter("phone_home", c.mPhoneHome));
                    sb.append(appendParameter("phone_mobile", c.mPhoneMobile));
                    sb.append(appendParameter("phone_work", c.mPhoneWork));
                    sb.append(appendParameter("email", c.mEmail));
                    sb.append(appendParameter("street", c.mStreet));
                    sb.append(appendParameter("zipcode", c.mZipcode));
                    sb.append(appendParameter("city", c.mCity));
                    sb.append(appendParameter("country", c.mCountry));
                    sb.append(appendParameter("birthday", birthdayString));
                    sb.append(appendParameter("last_modified", lastModifiedString));
                    sb.append(appendParameter("notes", c.mNotes));
                    sb.append(appendParameter("customer_group", c.mCustomerGroup));
                    sb.append(appendParameter("newsletter", c.mNewsletter ? "1" : "0"));
                    sb.append(appendParameter("image", Base64.encodeToString(c.getImage(), Base64.NO_WRAP)));
                    sb.append(appendParameter("consentImage", Base64.encodeToString(c.getConsent(), Base64.NO_WRAP)));
                    sb.append(appendParameter("custom_fields", c.mCustomFields));
                    sb.append(appendParameter("removed", String.valueOf(c.mRemoved)));
                }
            }
            if(this.vouchers != null) {
                for(Voucher v : vouchers) {
                    String issuedString = v.mIssued==null ? "1800-01-01" : CustomerDatabase.storageFormatWithTime.format(v.mIssued);
                    String redeemedString = v.mRedeemed==null ? "1800-01-01" : CustomerDatabase.storageFormatWithTime.format(v.mRedeemed);
                    String validUntilString = v.mValidUntil==null ? "1800-01-01" : CustomerDatabase.storageFormatWithTime.format(v.mValidUntil);
                    String lastModifiedString = CustomerDatabase.storageFormatWithTime.format(v.mLastModified);
                    sb.append(appendParameter("v_id", String.valueOf(v.mId)));
                    sb.append(appendParameter("v_current_value", String.valueOf(v.mCurrentValue)));
                    sb.append(appendParameter("v_original_value", String.valueOf(v.mOriginalValue)));
                    sb.append(appendParameter("v_from_customer", String.valueOf(v.mFromCustomer)));
                    sb.append(appendParameter("v_for_customer", String.valueOf(v.mForCustomer)));
                    sb.append(appendParameter("v_issued", issuedString));
                    sb.append(appendParameter("v_redeemed", redeemedString));
                    sb.append(appendParameter("v_valid_until", validUntilString));
                    sb.append(appendParameter("v_notes", String.valueOf(v.mNotes)));
                    sb.append(appendParameter("v_last_modified", lastModifiedString));
                    sb.append(appendParameter("v_removed", String.valueOf(v.mRemoved)));
                }
            }
        }

        String text = "";
        BufferedReader reader = null;

        try {
            URL url = new URL(urlString);
            URLConnection conn = url.openConnection();
            conn.setDoOutput(true);
            OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
            wr.write(sb.toString());
            wr.flush();

            reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder sb2 = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb2.append(line).append("\n");
            }
            text = sb2.toString();
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            if (reader != null) try {
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return text;
    }

    private String appendParameter(String key, String value) {
        String appendString = "";
        try {
            appendString += URLEncoder.encode(key+"[]", POST_ENCODING);
            appendString += POST_PARAM_KEYVALUE_SEPARATOR;
            appendString += URLEncoder.encode(value, POST_ENCODING);
            appendString += POST_PARAM_SEPARATOR;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return appendString;
    }

    @Override
    protected void onPostExecute(String result) {
        // get a reference to the activity if it is still there
        MainActivity activity = activityReference.get();
        if(activity == null) return;
        activity.refreshCustomersFromLocalDatabase();
        activity.refreshVouchersFromLocalDatabase();
        if(result.trim().equals("OK")) {
            activity.dialogSyncSuccess();
            MainActivity.setChangesSynced(activity);
        } else {
            activity.dialogSyncFail(result);
        }
        activity.refreshSyncIcon();
    }

    private void onPostGetEntries(String result) {
        MainActivity activity = activityReference.get();
        if(activity == null) return;
        activity.mDb.truncateCustomer();
        activity.mDb.truncateVoucher();
        String[] records = result.split("\\r?\\n");
        for (String record : records) {
            if(record.startsWith("v_")) {
                Voucher v = new Voucher();
                String[] keyvalues = record.split("&");
                for (String keyvalue : keyvalues) {
                    String key = "";
                    String value = "";
                    String[] splitResult = keyvalue.split("=");
                    try {
                        if (splitResult.length > 0)
                            key = URLDecoder.decode(splitResult[0], POST_ENCODING);
                        if (splitResult.length > 1)
                            value = URLDecoder.decode(splitResult[1], POST_ENCODING);
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    v.putVoucherAttribute(key, value);
                }
                activity.mDb.addVoucher(v);
            } else {
                Customer c = new Customer();
                String[] keyvalues = record.split("&");
                for (String keyvalue : keyvalues) {
                    String key = "";
                    String value = "";
                    String[] splitResult = keyvalue.split("=");
                    try {
                        if (splitResult.length > 0)
                            key = URLDecoder.decode(splitResult[0], POST_ENCODING);
                        if (splitResult.length > 1)
                            value = URLDecoder.decode(splitResult[1], POST_ENCODING);
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    c.putCustomerAttribute(key, value);
                }
                activity.mDb.addCustomer(c);
            }
        }
    }

}
