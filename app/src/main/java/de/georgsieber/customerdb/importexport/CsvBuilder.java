package de.georgsieber.customerdb.importexport;

import android.util.Log;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.georgsieber.customerdb.CustomerDatabase;
import de.georgsieber.customerdb.model.CustomField;
import de.georgsieber.customerdb.model.Customer;

public class CsvBuilder {

    private List<Customer> mCustomers = new ArrayList<>();
    private List<CustomField> mAllCustomFields;

    public CsvBuilder(List<Customer> _customers, List<CustomField> _allCustomFields) {
        mCustomers = _customers;
        mAllCustomFields = _allCustomFields;
    }
    public CsvBuilder(Customer _customer, List<CustomField> _allCustomFields) {
        mCustomers.add(_customer);
        mAllCustomFields = _allCustomFields;
    }

    private String buildCsvContent() {
        StringWriter content = new StringWriter();

        CSVWriter csvWriter = new CSVWriter(content);

        List<String> headers = new ArrayList<>(Arrays.asList(
                "id", "title", "first_name", "last_name",
                "phone_home", "phone_mobile", "phone_work", "email",
                "street", "zipcode", "city", "country", "customer_group",
                "newsletter", "birthday", "last_modified", "notes"
        ));
        for(CustomField cf : mAllCustomFields) {
            //Log.e("CSV", "add cf: "+cf.mTitle);
            headers.add(cf.mTitle);
        }
        csvWriter.writeNext(headers.toArray(new String[0]));

        for(Customer currentCustomer : mCustomers) {
            List<String> values = new ArrayList<>(Arrays.asList(
                    Long.toString(currentCustomer.mId),
                    currentCustomer.mTitle,
                    currentCustomer.mFirstName,
                    currentCustomer.mLastName,
                    currentCustomer.mPhoneHome,
                    currentCustomer.mPhoneMobile,
                    currentCustomer.mPhoneWork,
                    currentCustomer.mEmail,
                    currentCustomer.mStreet,
                    currentCustomer.mZipcode,
                    currentCustomer.mCity,
                    currentCustomer.mCountry,
                    currentCustomer.mCustomerGroup,
                    currentCustomer.mNewsletter ? "1" : "0",
                    currentCustomer.mBirthday==null ? "" : CustomerDatabase.storageFormatWithTime.format(currentCustomer.mBirthday),
                    currentCustomer.mLastModified==null ? "" : CustomerDatabase.storageFormatWithTime.format(currentCustomer.mLastModified),
                    currentCustomer.mNotes
            ));
            for(CustomField cf : mAllCustomFields) {
                values.add(currentCustomer.getCustomField(cf.mTitle));
            }
            csvWriter.writeNext(values.toArray(new String[0]));
        }

        return content.toString();
    }

    public boolean saveCsvFile(File f) {
        try {
            FileOutputStream stream = new FileOutputStream(f);
            stream.write(buildCsvContent().getBytes());
            stream.close();
            return true;
        } catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
        return false;
    }

    public static List<Customer> readCsvFile(InputStreamReader isr) throws Exception {
        List<Customer> newCustomers = new ArrayList<>();
        CSVReader reader = new CSVReader(isr);
        String[] headers = new String[]{};
        String[] line;
        int counter = 0;
        while((line = reader.readNext()) != null) {
            try {
                if(counter == 0) {
                    headers = line;
                } else {
                    Customer newCustomer = new Customer();
                    int counter2 = 0;
                    for(String field : line) {
                        //Log.e("CSV", headers[counter2] + " -> "+ field);
                        newCustomer.putCustomerAttribute(headers[counter2], field);
                        counter2 ++;
                    }
                    newCustomers.add(newCustomer);
                }
            } catch(Exception ignored) {}
            counter ++;
        }
        return newCustomers;
    }

}
