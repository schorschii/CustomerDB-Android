package de.georgsieber.customerdb;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.ArrayList;

import de.georgsieber.customerdb.model.Customer;
import de.georgsieber.customerdb.tools.ColorControl;


public class BirthdayActivity extends AppCompatActivity {

    private final static int VIEW_REQUEST = 1;
    private BirthdayActivity me;

    private CustomerDatabase mDb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // init settings
        SharedPreferences settings = getSharedPreferences(MainActivity.PREFS_NAME, 0);

        // init db
        mDb = new CustomerDatabase(this);

        // init activity view
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_birthday);
        me = this;

        // init toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if(getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // init colors
        ColorControl.updateActionBarColor(this, settings);

        // show birthdays from intent extra
        try {
            ArrayList<Customer> birthdays = MainActivity.getSoonBirthdayCustomers(mDb.getCustomers(null, false, false));
            bindToListView(birthdays);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // ListView init
        final ListView listView = findViewById(R.id.mainBirthdayList);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Customer customerObject = (Customer) listView.getItemAtPosition(position);
                Intent myIntent = new Intent(me, CustomerDetailsActivity.class);
                myIntent.putExtra("customer-id", customerObject.mId);
                startActivityForResult(myIntent, VIEW_REQUEST);
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode) {
            case(VIEW_REQUEST): {
                if (resultCode == Activity.RESULT_OK) {
                    // end birthday activity if changes were made and pass data to MainActivity
                    if(data.getStringExtra("action").equals("update")) {
                        Intent output = new Intent();
                        output.putExtra("action", "update");
                        setResult(RESULT_OK, output);
                        finish();
                    }
                }
                break;
            }
        }
    }

    private void bindToListView(ArrayList<Customer> customers) {
        ((ListView)findViewById(R.id.mainBirthdayList)).setAdapter(new CustomerAdapterBirthday(this, customers));
    }
}
