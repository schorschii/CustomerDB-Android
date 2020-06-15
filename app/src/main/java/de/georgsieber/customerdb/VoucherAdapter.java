package de.georgsieber.customerdb;

import android.annotation.SuppressLint;
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

import de.georgsieber.customerdb.model.Voucher;

public class VoucherAdapter extends BaseAdapter {

    private Context mContext;
    private List<Voucher> mVouchers;
    private String mCurrency;
    private LayoutInflater mInflater;
    private boolean mShowCheckbox = false;

    VoucherAdapter(Context context, List<Voucher> vouchers, String currency) {
        mContext = context;
        mVouchers = vouchers;
        mCurrency = currency;
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }
    VoucherAdapter(Context context, List<Voucher> vouchers, String currency, checkedChangedListener listener) {
        mContext = context;
        mVouchers = vouchers;
        mCurrency = currency;
        mListener = listener;
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return mVouchers.size();
    }

    @Override
    public Object getItem(int position) {
        return mVouchers.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @SuppressLint("SetTextI18n")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Voucher v = mVouchers.get(position);

        if(convertView == null) {
            convertView = mInflater.inflate(R.layout.item_list_customer, null);
        }
        TextView tv1 = (convertView.findViewById(R.id.textViewCustomerListItem1));
        TextView tv2 = (convertView.findViewById(R.id.textViewCustomerListItem2));

        if(v.mCurrentValue != v.mOriginalValue)
            tv1.setText(v.getCurrentValueString()+" "+mCurrency + "  ("+v.getOriginalValueString()+" "+mCurrency+")");
        else
            tv1.setText(v.getCurrentValueString()+" "+mCurrency);

        tv2.setText(v.mVoucherNo.equals("") ? v.getIdString() : v.mVoucherNo);

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
        for(int i = 0; i< mVouchers.size(); i++) {
            mSparseBooleanArray.put(i, checked);
        }
        if(mListener != null) mListener.checkedChanged(getCheckedItems());
    }

    ArrayList<Voucher> getCheckedItems() {
        ArrayList<Voucher> mTempArray = new ArrayList<>();
        for(int i = 0; i< mVouchers.size(); i++) {
            if(mSparseBooleanArray.get(i)) {
                mTempArray.add(mVouchers.get(i));
            }
        }
        return mTempArray;
    }

    private checkedChangedListener mListener = null;
    public interface checkedChangedListener {
        void checkedChanged(ArrayList<Voucher> checked);
    }
    void setCheckedChangedListener(checkedChangedListener listener) {
        this.mListener = listener;
    }

}
