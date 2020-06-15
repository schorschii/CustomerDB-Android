package de.georgsieber.customerdb.model;

import android.view.View;

public class CustomField {
    public int mId = -1;
    public String mTitle = "";
    public int mType = -1;
    public String mValue;
    public View mEditViewReference;

    public CustomField(String title, int type) {
        this.mTitle = title;
        this.mType = type;
    }

    public CustomField(int id, String title, int type) {
        this.mId = id;
        this.mTitle = title;
        this.mType = type;
    }

    public CustomField(String title, String value) {
        this.mTitle = title;
        this.mValue = value;
    }

    @Override
    public String toString() {
        return mTitle;
    }
}
