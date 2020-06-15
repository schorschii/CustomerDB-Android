package de.georgsieber.customerdb;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import java.util.List;

import de.georgsieber.customerdb.model.Customer;

public class CustomerAdapterBirthday extends BaseAdapter {
    private Context mContext;
    private List<Customer> mCustomers;

    CustomerAdapterBirthday(Context context, List<Customer> customers) {
        mContext = context;
        mCustomers = customers;
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
        if(convertView == null) {
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.item_list_customer, null);
        }

        TextView tv1 = (convertView.findViewById(R.id.textViewCustomerListItem1));
        TextView tv2 = (convertView.findViewById(R.id.textViewCustomerListItem2));
        CheckBox cb = (convertView.findViewById(R.id.checkBoxCustomerListItem));

        tv1.setText(mCustomers.get(position).getFirstLine());
        tv2.setText(mCustomers.get(position).getBirthdayString(mContext.getResources().getString(R.string.birthdaytodaynote)));
        cb.setVisibility(View.GONE);

        return convertView;
    }
}
