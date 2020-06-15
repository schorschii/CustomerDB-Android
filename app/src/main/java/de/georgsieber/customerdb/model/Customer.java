package de.georgsieber.customerdb.model;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Base64;

import androidx.annotation.NonNull;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import de.georgsieber.customerdb.CustomerDatabase;
import de.georgsieber.customerdb.tools.DateControl;


public class Customer implements Parcelable {
    public long mId = -1;
    public String mTitle = "";
    public String mFirstName = "";
    public String mLastName = "";
    public String mPhoneHome = "";
    public String mPhoneMobile = "";
    public String mPhoneWork = "";
    public String mEmail = "";
    public String mStreet = "";
    public String mZipcode = "";
    public String mCity = "";
    public String mCountry = "";
    public Date mBirthday;
    public String mCustomerGroup = "";
    public boolean mNewsletter = false;
    public String mNotes = "";
    public byte[] mImage = new byte[0];
    public byte[] mConsentImage = new byte[0];
    public String mCustomFields = "";
    public Date mLastModified = new Date();
    public int mRemoved = 0;

    public Customer() {
        super();
    }
    public Customer(long _id,
                    String _title,
                    String _firstName,
                    String _lastName,
                    String _phoneHome,
                    String _phoneMobile,
                    String _phoneWork,
                    String _email,
                    String _street,
                    String _zipcode,
                    String _city,
                    String _country,
                    Date _birthday,
                    String _group,
                    boolean _newsletter,
                    String _notes,
                    String _customFields,
                    Date _lastModified) {
        mId = _id;
        mTitle = _title;
        mFirstName = _firstName;
        mLastName = _lastName;
        mPhoneHome = _phoneHome;
        mPhoneMobile = _phoneMobile;
        mPhoneWork = _phoneWork;
        mEmail = _email;
        mStreet = _street;
        mZipcode = _zipcode;
        mCity = _city;
        mCountry = _country;
        mBirthday = _birthday;
        mCustomerGroup = _group;
        mNewsletter = _newsletter;
        mNotes = _notes;
        mCustomFields = _customFields;
        mLastModified = _lastModified;
    }
    public Customer(long _id,
                    String _title,
                    String _firstName,
                    String _lastName,
                    String _phoneHome,
                    String _phoneMobile,
                    String _phoneWork,
                    String _email,
                    String _street,
                    String _zipcode,
                    String _city,
                    String _country,
                    Date _birthday,
                    String _group,
                    boolean _newsletter,
                    String _notes,
                    String _customFields,
                    Date _lastModified,
                    int _removed) {
        mId = _id;
        mTitle = _title;
        mFirstName = _firstName;
        mLastName = _lastName;
        mPhoneHome = _phoneHome;
        mPhoneMobile = _phoneMobile;
        mPhoneWork = _phoneWork;
        mEmail = _email;
        mStreet = _street;
        mZipcode = _zipcode;
        mCity = _city;
        mCountry = _country;
        mBirthday = _birthday;
        mCustomerGroup = _group;
        mNewsletter = _newsletter;
        mNotes = _notes;
        mCustomFields = _customFields;
        mLastModified = _lastModified;
        mRemoved = _removed;
    }

    public static long generateID() {
        /* This function generates an unique mId for a new record.
         * Required in order to get unique ids over multiple devices,
         * which are not in sync with the central mysql server */
        @SuppressLint("SimpleDateFormat")
        DateFormat idFormat = new SimpleDateFormat("yyyyMMddkkmmss");
        String random;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            random = String.valueOf(ThreadLocalRandom.current().nextInt(1, 100));
        } else {
            random = "10";
        }
        return Long.parseLong(idFormat.format(new Date())+random);
    }
    public static long generateID(int suffix) {
        /* specific suffix for bulk import */
        @SuppressLint("SimpleDateFormat")
        DateFormat idFormat = new SimpleDateFormat("yyyyMMddkkmmss");
        return Long.parseLong(idFormat.format(new Date())+suffix);
    }

    private long tryParseLong(String value, long defaultVal) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }

    public void putCustomerAttribute(String key, String value) {
        switch(key) {
            case "id":
                mId = tryParseLong(value, mId); break;
            case "title":
                mTitle = value; break;
            case "first_name":
                mFirstName = value; break;
            case "last_name":
                mLastName = value; break;
            case "phone_home":
                mPhoneHome = value; break;
            case "phone_mobile":
                mPhoneMobile = value; break;
            case "phone_work":
                mPhoneWork = value; break;
            case "email":
                mEmail = value; break;
            case "street":
                mStreet = value; break;
            case "zipcode":
                mZipcode = value; break;
            case "city":
                mCity = value; break;
            case "country":
                mCountry = value; break;
            case "birthday":
                try {
                    if(value.contains("1800")) {
                        mBirthday = null;
                    } else {
                        mBirthday = CustomerDatabase.storageFormatWithTime.parse(value);
                    }
                } catch (ParseException ignored) {}
                break;
            case "last_modified":
                try {
                    mLastModified = new Date();
                    mLastModified = CustomerDatabase.storageFormatWithTime.parse(value);
                } catch (ParseException ignored) {}
                break;
            case "notes":
                mNotes = value; break;
            case "customer_group":
                mCustomerGroup = value; break;
            case "newsletter":
                mNewsletter = (value.equals("1")); break;
            case "consent":
                mConsentImage = Base64.decode(value, Base64.NO_WRAP); break;
            case "image":
                mImage = Base64.decode(value, Base64.NO_WRAP); break;
            case "custom_fields":
                mCustomFields = value; break;
            case "removed":
                mRemoved = Integer.parseInt(value); break;
            default:
                updateOrCreateCustomField(key, value);
        }
    }

    public byte[] getConsent() {
        if(this.mConsentImage == null)
            return new byte[0];
        else
            return this.mConsentImage;
    }
    public byte[] getImage() {
        if(this.mImage == null)
            return new byte[0];
        else
            return this.mImage;
    }

    @NonNull
    @Override
    public String toString() {
        return this.mLastName + ", " + this.mFirstName;
    }

    public String getFullName(boolean lastNameFirst) {
        String final_title = "";
        if(!this.mTitle.equals("")) final_title = this.mTitle +" ";

        String final_name;
        if(this.mLastName.equals("")) final_name = this.mFirstName;
        else if(this.mFirstName.equals("")) final_name = this.mLastName;
        else if(lastNameFirst) final_name = this.mLastName + ", " + this.mFirstName;
        else final_name = this.mFirstName + " " + this.mLastName;

        return final_title + final_name;
    }

    public String getAddress() {
        if((this.mStreet == null && this.mZipcode == null && this.mCity == null && this.mCountry == null)
                || (this.mStreet.equals("") && this.mZipcode.equals("") && this.mCity.equals("") && this.mCountry.equals("")))
            return "";
        else if(!this.mStreet.equals("") && !this.mZipcode.equals("") && this.mCity.equals("") && this.mCountry.equals(""))
            return this.mStreet + "\n" + this.mZipcode;
        else if(this.mStreet.equals("") && this.mZipcode.equals("") && !this.mCity.equals("") && !this.mCountry.equals(""))
            return this.mCity + " " + this.mCountry;
        else
            return this.mStreet + "\n" + this.mZipcode + ", " + this.mCity + " " + this.mCountry;
    }

    private static boolean isSameDay(Date date1, Date date2) {
        if(date1 == null || date2 == null) {
            throw new IllegalArgumentException("The dates must not be null");
        }
        Calendar cal1 = Calendar.getInstance();
        cal1.setTime(date1);
        Calendar cal2 = Calendar.getInstance();
        cal2.setTime(date2);
        return isSameDay(cal1, cal2);
    }
    private static boolean isSameDay(Calendar cal1, Calendar cal2) {
        if(cal1 == null || cal2 == null) {
            throw new IllegalArgumentException("The dates must not be null");
        }
        return (cal1.get(Calendar.ERA) == cal2.get(Calendar.ERA) &&
                cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR));
    }
    private static boolean isToday(Date date) {
        return isSameDay(date, Calendar.getInstance().getTime());
    }
    public String getBirthdayString() {
        return getBirthdayString("");
    }
    public String getBirthdayString(String todayNote) {
        // 1900 = birthday without year
        // 1800 = no birthday

        if(mBirthday == null) return "";

        String years = "";
        if(android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int diff = Period.between(mBirthday.toInstant().atZone(ZoneId.systemDefault()).toLocalDate(), LocalDate.now()).getYears();
            if(diff < 120) years = " (" + diff + ")";
        }

        String finalString = DateControl.birthdayDateFormat.format(mBirthday) + years;
        if(!todayNote.equals("") && isToday(getNextBirthday())) finalString += " " + todayNote;
        return finalString;
    }

    public Date getNextBirthday() {
        if(mBirthday == null) return null;
        try {
            Calendar current = Calendar.getInstance();

            Calendar cal = Calendar.getInstance();
            cal.setTime(mBirthday);
            cal.set(Calendar.YEAR, current.get(Calendar.YEAR));

            if(cal.get(Calendar.DAY_OF_MONTH) != current.get(Calendar.DAY_OF_MONTH)
            || cal.get(Calendar.MONTH) != current.get(Calendar.MONTH)) { // birthday is today - this is ok
                if(cal.getTimeInMillis() < current.getTimeInMillis()) { // birthday this year is already in the past - go to next year
                    cal.set(Calendar.YEAR, current.get(Calendar.YEAR) + 1);
                }
            }
            return cal.getTime();
        } catch(Exception ignored) {
            return null;
        }
    }

    public String getFirstLine() {
        return this.getFullName(true);
    }

    public String getSecondLine() {
        if(!mPhoneHome.equals("")) return mPhoneHome;
        else if(!mPhoneMobile.equals("")) return mPhoneMobile;
        else if(!mPhoneWork.equals("")) return mPhoneWork;
        else if(!mEmail.equals("")) return mEmail;
        else return getFirstNotEmptyCustomFieldString();
    }


    /* parsing custom fields string */

    public List<CustomField> getCustomFields() {
        List<CustomField> fields = new ArrayList<>();
        for(String kv : mCustomFields.split("&")) {
            String[] kv_split = kv.split("=");
            if(kv_split.length == 2) {
                try {
                    fields.add(new CustomField(
                            URLDecoder.decode(kv_split[0], "UTF-8"),
                            URLDecoder.decode(kv_split[1], "UTF-8")
                    ));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        }
        return fields;
    }
    private String getFirstNotEmptyCustomFieldString() {
        for(CustomField field : getCustomFields()) {
            if(!field.mValue.equals("")) {
                return field.mValue;
            }
        }
        return "";
    }
    public String getCustomField(String key) {
        for(CustomField cf : getCustomFields()) {
            if(cf.mTitle.equals(key)) {
                return cf.mValue;
            }
        }
        return null;
    }
    public void setCustomFields(List<CustomField> fields) {
        StringBuilder sb = new StringBuilder();
        for(CustomField cf : fields) {
            try {
                sb.append(URLEncoder.encode(cf.mTitle, "UTF-8"));
                sb.append("=");
                sb.append(URLEncoder.encode(cf.mValue, "UTF-8"));
                sb.append("&");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        mCustomFields = sb.toString();
    }
    public void updateOrCreateCustomField(String title, String value) {
        List<CustomField> fields = getCustomFields();
        for(CustomField cf : fields) {
            if(cf.mTitle.equals(title)) {
                cf.mValue = value;
                setCustomFields(fields);
                return;
            }
        }
        fields.add(new CustomField(title, value));
        setCustomFields(fields);
    }


    /* everything below here is for implementing Parcelable */

    @Override
    public int describeContents() {
        return 0;
    }

    // write your object's data to the passed-in Parcel
    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeLong(mId);
        out.writeString(mTitle);
        out.writeString(mFirstName);
        out.writeString(mLastName);
        out.writeString(mPhoneHome);
        out.writeString(mPhoneMobile);
        out.writeString(mPhoneWork);
        out.writeString(mEmail);
        out.writeString(mStreet);
        out.writeString(mZipcode);
        out.writeString(mCity);
        out.writeString(mCountry);
        out.writeLong(mBirthday == null ? 0 : mBirthday.getTime());
        out.writeString(mCustomerGroup);
        out.writeString(mNotes);
        out.writeInt(mNewsletter ? 1 : 0);
        out.writeString(mCustomFields);
        out.writeLong(mLastModified.getTime());
        out.writeInt(mConsentImage == null ? 0 : mConsentImage.length);
        out.writeByteArray(mConsentImage == null ? new byte[0] : mConsentImage);
        out.writeInt(mImage == null ? 0 : mImage.length);
        out.writeByteArray(mImage == null ? new byte[0] : mImage);
    }

    // this is used to regenerate your object. All Parcelables must have a CREATOR that implements these two methods
    public static final Parcelable.Creator<Customer> CREATOR = new Parcelable.Creator<Customer>() {
        public Customer createFromParcel(Parcel in) {
            return new Customer(in);
        }

        public Customer[] newArray(int size) {
            return new Customer[size];
        }
    };

    // example constructor that takes a Parcel and gives you an object populated with it's values
    private Customer(Parcel in) {
        mId = in.readLong();
        mTitle = in.readString();
        mFirstName = in.readString();
        mLastName = in.readString();
        mPhoneHome = in.readString();
        mPhoneMobile = in.readString();
        mPhoneWork = in.readString();
        mEmail = in.readString();
        mStreet = in.readString();
        mZipcode = in.readString();
        mCity = in.readString();
        mCountry = in.readString();
        long birthday = in.readLong(); mBirthday = birthday == 0 ? null : new Date(birthday);
        mCustomerGroup = in.readString();
        mNotes = in.readString();
        mNewsletter = in.readInt() == 1;
        mCustomFields = in.readString();
        mLastModified = new Date(in.readLong());
        mConsentImage = new byte[in.readInt()];
        in.readByteArray(mConsentImage);
        mImage = new byte[in.readInt()];
        in.readByteArray(mImage);
    }
}
