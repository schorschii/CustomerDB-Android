package de.georgsieber.customerdb;

import java.util.Comparator;
import java.util.List;

import de.georgsieber.customerdb.model.CustomField;
import de.georgsieber.customerdb.model.Customer;

public class CustomerComparator implements Comparator<Customer> {

    private FIELD mSortField;
    private String mSortCustomField;
    private boolean mAscending;

    enum FIELD {
        TITLE,
        FIRST_NAME,
        LAST_NAME
    }

    CustomerComparator(String customFieldTitleForSort, boolean ascending) {
        this.mSortCustomField = customFieldTitleForSort;
        this.mAscending = ascending;
    }

    CustomerComparator(FIELD field, boolean ascending) {
        this.mSortField = field;
        this.mAscending = ascending;
    }

    @Override
    public int compare(Customer c1, Customer c2) {
        String fieldValue_1 = "";
        String fieldValue_2 = "";

        if(mSortCustomField != null) {
            List<CustomField> cf_1 = c1.getCustomFields();
            List<CustomField> cf_2 = c2.getCustomFields();
            for(CustomField cf : cf_1) {
                if(cf.mTitle.equals(mSortCustomField))
                    fieldValue_1 = cf.mValue;
            }
            for(CustomField cf : cf_2) {
                if(cf.mTitle.equals(mSortCustomField))
                    fieldValue_2 = cf.mValue;
            }
        }
        else if(mSortField != null) {
            if(mSortField == FIELD.TITLE) {
                fieldValue_1 = c1.mTitle;
                fieldValue_2 = c2.mTitle;
            }
            else if(mSortField == FIELD.FIRST_NAME) {
                fieldValue_1 = c1.mFirstName;
                fieldValue_2 = c2.mFirstName;
            }
            else if(mSortField == FIELD.LAST_NAME) {
                fieldValue_1 = c1.mLastName;
                fieldValue_2 = c2.mLastName;
            }
        }

        if(mAscending) return fieldValue_1.compareTo(fieldValue_2);
        else return fieldValue_2.compareTo(fieldValue_1);
    }
}
