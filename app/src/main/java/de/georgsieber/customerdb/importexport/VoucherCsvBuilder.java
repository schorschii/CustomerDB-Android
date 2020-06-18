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
import de.georgsieber.customerdb.model.Voucher;

public class VoucherCsvBuilder {

    private List<Voucher> mVouchers = new ArrayList<>();

    public VoucherCsvBuilder(List<Voucher> _vouchers) {
        mVouchers = _vouchers;
    }
    public VoucherCsvBuilder(Voucher _voucher) {
        mVouchers.add(_voucher);
    }

    private String buildCsvContent() {
        StringWriter content = new StringWriter();

        CSVWriter csvWriter = new CSVWriter(content);

        List<String> headers = new ArrayList<>(Arrays.asList(
                "id", "voucher_no", "original_value", "current_value",
                "from_customer", "for_customer",
                "issued", "valid_until", "redeemed", "last_modified",
                "notes"
        ));
        csvWriter.writeNext(headers.toArray(new String[0]));

        for(Voucher currentVoucher : mVouchers) {
            List<String> values = new ArrayList<>(Arrays.asList(
                    Long.toString(currentVoucher.mId),
                    currentVoucher.mVoucherNo,
                    Double.toString(currentVoucher.mOriginalValue),
                    Double.toString(currentVoucher.mCurrentValue),
                    currentVoucher.mFromCustomer,
                    currentVoucher.mForCustomer,
                    currentVoucher.mIssued==null ? "" : CustomerDatabase.storageFormatWithTime.format(currentVoucher.mIssued),
                    currentVoucher.mValidUntil==null ? "" : CustomerDatabase.storageFormatWithTime.format(currentVoucher.mValidUntil),
                    currentVoucher.mRedeemed==null ? "" : CustomerDatabase.storageFormatWithTime.format(currentVoucher.mRedeemed),
                    currentVoucher.mLastModified==null ? "" : CustomerDatabase.storageFormatWithTime.format(currentVoucher.mLastModified),
                    currentVoucher.mNotes
            ));
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

    public static List<Voucher> readCsvFile(InputStreamReader isr) throws Exception {
        List<Voucher> newVouchers = new ArrayList<>();
        CSVReader reader = new CSVReader(isr);
        String[] headers = new String[]{};
        String[] line;
        int counter = 0;
        while((line = reader.readNext()) != null) {
            try {
                if(counter == 0) {
                    headers = line;
                } else {
                    Voucher newVoucher = new Voucher();
                    int counter2 = 0;
                    for(String field : line) {
                        //Log.e("CSV", headers[counter2] + " -> "+ field);
                        newVoucher.putVoucherAttribute(headers[counter2], field);
                        counter2 ++;
                    }
                    newVouchers.add(newVoucher);
                }
            } catch(Exception ignored) {}
            counter ++;
        }
        return newVouchers;
    }

}
