package de.georgsieber.customerdb.model;

import android.annotation.SuppressLint;
import android.os.Parcel;
import android.os.Parcelable;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

import de.georgsieber.customerdb.CustomerDatabase;
import de.georgsieber.customerdb.tools.DateControl;

public class Voucher implements Parcelable {
    public long mId = -1;
    public double mOriginalValue = 0;
    public double mCurrentValue = 0;
    public String mVoucherNo = "";
    public String mFromCustomer = "";
    public String mForCustomer = "";
    public String mNotes = "";
    public Date mIssued = new Date();
    public Date mValidUntil;
    public Date mRedeemed;
    public Date mLastModified;
    public int mRemoved = 0;

    public Voucher() {}
    public Voucher(
            long _id,
            double _currentValue,
            double _originalValue,
            String _voucherNo,
            String _fromCustomer,
            String _forCustomer,
            Date _issued,
            Date _validUntil,
            Date _redeemed,
            Date _lastModified,
            String _notes,
            int _removed
    ) {
        mId = _id;
        mOriginalValue = _originalValue;
        mCurrentValue = _currentValue;
        mVoucherNo = _voucherNo;
        mFromCustomer = _fromCustomer;
        mForCustomer = _forCustomer;
        mIssued = _issued;
        mValidUntil = _validUntil;
        mRedeemed = _redeemed;
        mNotes = _notes;
        mRemoved = _removed;
        mLastModified = _lastModified;
    }

    public static long generateID() {
        /* This function generates an unique mId for a new record.
         * Required in order to get unique ids over multiple devices,
         * which are not in sync with the central mysql server */
        @SuppressLint("SimpleDateFormat")
        DateFormat idFormat = new SimpleDateFormat("yyMMddkkmmss");
        String random;
        if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
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

    public void putVoucherAttribute(String key, String value) {
        switch(key) {
            case "v_id":
            case "id":
                mId = Long.parseLong(value); break;
            case "v_current_value":
            case "current_value":
                mCurrentValue = Float.parseFloat(value); break;
            case "v_original_value":
            case "original_value":
                mOriginalValue = Float.parseFloat(value); break;
            case "v_voucher_no":
            case "voucher_no":
                mVoucherNo = value; break;
            case "v_from_customer":
            case "from_customer":
                mFromCustomer = value; break;
            case "v_for_customer":
            case "for_customer":
                mForCustomer = value; break;
            case "v_notes":
            case "notes":
                mNotes = value; break;
            case "v_removed":
            case "removed":
                mRemoved = Integer.parseInt(value); break;
            case "v_issued":
            case "issued":
                try {
                    mIssued = CustomerDatabase.storageFormatWithTime.parse(value);
                } catch (ParseException e) { e.printStackTrace(); }
                break;
            case "v_redeemed":
            case "redeemed":
                try {
                    mRedeemed = CustomerDatabase.storageFormatWithTime.parse(value);
                } catch (ParseException e) { e.printStackTrace(); }
                break;
            case "v_valid_until":
            case "valid_until":
                try {
                    mValidUntil = CustomerDatabase.storageFormatWithTime.parse(value);
                } catch (ParseException e) { e.printStackTrace(); }
                break;
            case "v_last_modified":
            case "last_modified":
                try {
                    mLastModified = new Date();
                    mLastModified = CustomerDatabase.storageFormatWithTime.parse(value);
                } catch (ParseException e) { e.printStackTrace(); }
                break;
        }
    }

    public String getIdString() {
        return Long.toString(mId);
    }
    public String getCurrentValueString() {
        return String.format(Locale.getDefault(), "%.2f", mCurrentValue);
    }
    public String getOriginalValueString() {
        return String.format(Locale.getDefault(), "%.2f", mOriginalValue);
    }

    public String getIssuedString() {
        if(mIssued == null) return "";
        return DateControl.displayDateFormat.format(mIssued);
    }
    public String getValidUntilString() {
        if(mValidUntil == null) return "";
        return DateControl.displayDateFormat.format(mValidUntil);
    }
    public String getRedeemedString() {
        if(mRedeemed == null) return "";
        return DateControl.displayDateFormat.format(mRedeemed);
    }
    public String getLastModifiedString() {
        if(mLastModified == null) return "";
        return DateControl.displayDateFormat.format(mLastModified);
    }

    public void remove() {
        mCurrentValue = 0;
        mOriginalValue = 0;
        mVoucherNo = "";
        mFromCustomer = "";
        mForCustomer = "";
        mNotes = "";
        mLastModified = new Date();
        mRemoved = 1;
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
        out.writeDouble(mOriginalValue);
        out.writeDouble(mCurrentValue);
        out.writeString(mVoucherNo);
        out.writeString(mFromCustomer);
        out.writeString(mForCustomer);
        out.writeString(mNotes);
        out.writeLong(mIssued == null ? 0 : mIssued.getTime());
        out.writeLong(mValidUntil == null ? 0 : mValidUntil.getTime());
        out.writeLong(mRedeemed == null ? 0 : mRedeemed.getTime());
        out.writeLong(mLastModified.getTime());
    }

    // this is used to regenerate your object. All Parcelables must have a CREATOR that implements these two methods
    public static final Parcelable.Creator<Voucher> CREATOR = new Parcelable.Creator<Voucher>() {
        public Voucher createFromParcel(Parcel in) {
            return new Voucher(in);
        }

        public Voucher[] newArray(int size) {
            return new Voucher[size];
        }
    };

    // example constructor that takes a Parcel and gives you an object populated with it's values
    private Voucher(Parcel in) {
        mId = in.readLong();
        mOriginalValue = in.readDouble();
        mCurrentValue = in.readDouble();
        mVoucherNo = in.readString();
        mFromCustomer = in.readString();
        mForCustomer = in.readString();
        mNotes = in.readString();
        long issued = in.readLong(); mIssued = issued == 0 ? null : new Date(issued);
        long validUntil = in.readLong(); mValidUntil = validUntil == 0 ? null : new Date(validUntil);
        long redeemed = in.readLong(); mRedeemed = redeemed == 0 ? null : new Date(redeemed);
        mLastModified = new Date(in.readLong());
    }
}
