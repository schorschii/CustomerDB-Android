package de.georgsieber.customerdb;

import android.content.Context;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import de.georgsieber.customerdb.model.Customer;

class CustomerAdapter extends BaseAdapter {

    private Context mContext;
    private List<Customer> mCustomers;
    private LayoutInflater mInflater;
    private boolean mShowCheckbox = false;

    CustomerAdapter(Context context, List<Customer> customers) {
        mContext = context;
        mCustomers = customers;
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }
    CustomerAdapter(Context context, List<Customer> customers, checkedChangedListener listener) {
        mContext = context;
        mCustomers = customers;
        mListener = listener;
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return mCustomers.size();
    }

    @Override
    public Object getItem(int position) {
        return mCustomers.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Customer c = mCustomers.get(position);

        if(convertView == null) {
            convertView = mInflater.inflate(R.layout.item_list_customer, null);
        }
        TextView tv1 = (convertView.findViewById(R.id.textViewCustomerListItem1));
        TextView tv2 = (convertView.findViewById(R.id.textViewCustomerListItem2));

        tv1.setText(c.getFirstLine());
        if(c.getSecondLine().trim().equals("")) {
            tv2.setText(mContext.getString(R.string.no_details));
        } else {
            tv2.setText(c.getSecondLine());
        }

        CheckBox currentListItemCheckBox = convertView.findViewById(R.id.checkBoxCustomerListItem);
        if(mShowCheckbox) {
            currentListItemCheckBox.setTag(position);
            currentListItemCheckBox.setChecked(mSparseBooleanArray.get(position));
            currentListItemCheckBox.setOnCheckedChangeListener(mCheckedChangeListener);
            currentListItemCheckBox.setVisibility(View.VISIBLE);
        } else {
            currentListItemCheckBox.setVisibility(View.GONE);
        }
        return convertView;
    }

    boolean getShowCheckbox() {
        return mShowCheckbox;
    }
    void setShowCheckbox(boolean visible) {
        mShowCheckbox = visible;
        notifyDataSetChanged();
    }

    private SparseBooleanArray mSparseBooleanArray = new SparseBooleanArray();
    private CompoundButton.OnCheckedChangeListener mCheckedChangeListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            mSparseBooleanArray.put((Integer) buttonView.getTag(), isChecked);
            if(mListener != null) mListener.checkedChanged(getCheckedItems());
        }
    };

    void setAllChecked(boolean checked) {
        for(int i=0; i<mCustomers.size(); i++) {
            mSparseBooleanArray.put(i, checked);
        }
        if(mListener != null) mListener.checkedChanged(getCheckedItems());
    }

    ArrayList<Customer> getCheckedItems() {
        ArrayList<Customer> mTempArray = new ArrayList<Customer>();
        for(int i=0;i<mCustomers.size();i++) {
            if(mSparseBooleanArray.get(i)) {
                mTempArray.add(mCustomers.get(i));
            }
        }
        return mTempArray;
    }

    private checkedChangedListener mListener = null;
    public interface checkedChangedListener {
        void checkedChanged(ArrayList<Customer> checked);
    }
    void setCheckedChangedListener(checkedChangedListener listener) {
        this.mListener = listener;
    }

}
