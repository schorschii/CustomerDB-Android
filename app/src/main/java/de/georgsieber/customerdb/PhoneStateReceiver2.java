package de.georgsieber.customerdb;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;

import de.georgsieber.customerdb.model.Customer;

public class PhoneStateReceiver2 extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, Intent intent) {
        // local database init
        CustomerDatabase localDBConn = new CustomerDatabase(context);

        if(intent.getExtras() != null) {
            String incomingNumber = intent.getExtras().getString("number");
            final Customer callingCustomer = localDBConn.getCustomerByNumber(incomingNumber);
            if(callingCustomer != null) {
                Log.d("cutomerdbphonedbg", "SHOW_INFO__FOUND");
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        showCallInfo(context, callingCustomer);
                    }
                }, 500);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        showCallInfo(context, callingCustomer);
                    }
                }, 3500);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        showCallInfo(context, callingCustomer);
                    }
                }, 7000);
                saveLastCallInfo(context, incomingNumber, callingCustomer.getFullName(false));
            } else {
                Log.d("cutomerdbphonedbg", "SHOW_INFO__NOT_FOUND");
                saveLastCallInfo(context, incomingNumber, null);
            }
        }
    }

    private void showCallInfo(Context context, Customer callingCustomer) {
        Toast.makeText(context,
                context.getResources().getString(R.string.app_name) + ":\n" + callingCustomer.getFullName(false) + " " + context.getResources().getString(R.string.calling),
                Toast.LENGTH_LONG)
                .show();

    }

    protected void saveLastCallInfo(Context c, String number, String customer) {
        @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");

        String callInfo = sdf.format(new Date()) +" "+
                number +" ("+ (customer == null ? c.getResources().getString(R.string.no_customer_found) : customer) +")";

        SharedPreferences settings = c.getSharedPreferences(MainActivity.PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("last-call-received", callInfo);
        editor.apply();
    }
}
