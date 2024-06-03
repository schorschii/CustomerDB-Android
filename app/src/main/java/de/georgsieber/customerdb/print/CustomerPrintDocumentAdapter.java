package de.georgsieber.customerdb.print;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.pdf.PdfDocument;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.print.PageRange;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintDocumentInfo;
import android.print.pdf.PrintedPdfDocument;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import de.georgsieber.customerdb.CustomerDatabase;
import de.georgsieber.customerdb.R;
import de.georgsieber.customerdb.model.CustomerFile;
import de.georgsieber.customerdb.tools.DateControl;
import de.georgsieber.customerdb.model.CustomField;
import de.georgsieber.customerdb.model.Customer;

public class CustomerPrintDocumentAdapter extends PrintDocumentAdapter {
    private final Customer mCurrentCustomer;
    private final Context mCurrentContext;
    private SharedPreferences mSettings;
    private CustomerDatabase mDb;

    private PrintedPdfDocument mPdfDocument;

    public CustomerPrintDocumentAdapter(Context context, Customer customer, CustomerDatabase database, SharedPreferences settings) {
        mCurrentCustomer = customer;
        mCurrentContext = context;
        mDb = database;
        mSettings = settings;
    }

    @Override
    public void onLayout(PrintAttributes oldAttributes, PrintAttributes newAttributes, CancellationSignal cancellationSignal, LayoutResultCallback callback, Bundle extras) {
        if(cancellationSignal.isCanceled() ) {
            callback.onLayoutCancelled();
            return;
        }

        mPdfDocument = new PrintedPdfDocument(mCurrentContext, newAttributes);

        if(cancellationSignal.isCanceled() ) {
            callback.onLayoutCancelled();
            return;
        }

        PrintDocumentInfo info = new PrintDocumentInfo
                .Builder("printcustomer.tmp.pdf")
                .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                .setPageCount(1)
                .build();
        callback.onLayoutFinished(info, true);
    }

    @Override
    public void onWrite(PageRange[] pages, ParcelFileDescriptor destination, CancellationSignal cancellationSignal, WriteResultCallback callback) {
        PdfDocument.Page page = mPdfDocument.startPage(1);
        // Draw page content for printing
        drawPage(page);
        // Rendering is complete, so page can be finalized.
        mPdfDocument.finishPage(page);

        // Write PDF document to file
        try {
            mPdfDocument.writeTo(new FileOutputStream(
                    destination.getFileDescriptor()));
        } catch (IOException e) {
            callback.onWriteFailed(e.toString());
            return;
        } finally {
            mPdfDocument.close();
            mPdfDocument = null;
        }

        PageRange[] pr = new PageRange[1];
        pr[0] = PageRange.ALL_PAGES;
        callback.onWriteFinished(pr);
    }

    private void drawPage(PdfDocument.Page page) {
        Canvas c = page.getCanvas();

        float fontSize = (float) page.getInfo().getPageWidth()/14;
        int lineHeight = Math.round(fontSize/1.55f);
        int x0 = 15;
        int x1 = Math.round(page.getInfo().getPageWidth()/2.8f);
        int y = lineHeight*2;
        int charsPerLine = Math.round((page.getInfo().getPageWidth() - x1 - 10) / (fontSize/4));

        if(mSettings.getBoolean("show-customer-picture", true)) {
            if (mCurrentCustomer.getImage().length != 0) {
                Bitmap bitmap = BitmapFactory.decodeByteArray(mCurrentCustomer.getImage(), 0, mCurrentCustomer.getImage().length);
                int width = (int) (fontSize * 1.5);
                Rect dstRect = new Rect(c.getWidth() - width - x0, x0, c.getWidth() - x0, x0 + width);
                c.drawBitmap(bitmap, null, dstRect, null);
            }
        }

        Paint p = new Paint();
        Paint p_gray = new Paint();
        p.setColor(Color.BLACK);
        p_gray.setColor(Color.GRAY);
        p.setTextSize(fontSize);
        c.drawText(mCurrentCustomer.getFullName(false), x0, y, p);
        y += lineHeight;

        p.setTextSize(fontSize /2);
        p_gray.setTextSize(fontSize /2);

        if(mSettings.getBoolean("show-phone-field", true)) {
            y += lineHeight;
            c.drawText(mCurrentContext.getResources().getString(R.string.phonehome), x0, y, p_gray);
            c.drawText(mCurrentCustomer.mPhoneHome, x1, y, p);

            y += lineHeight;
            c.drawText(mCurrentContext.getResources().getString(R.string.phonemobile), x0, y, p_gray);
            c.drawText(mCurrentCustomer.mPhoneMobile, x1, y, p);

            y += lineHeight;
            c.drawText(mCurrentContext.getResources().getString(R.string.phonework), x0, y, p_gray);
            c.drawText(mCurrentCustomer.mPhoneWork, x1, y, p);
        }

        if(mSettings.getBoolean("show-email-field", true)) {
            y += lineHeight;
            c.drawText(mCurrentContext.getResources().getString(R.string.email), x0, y, p_gray);
            c.drawText(mCurrentCustomer.mEmail, x1, y, p);
        }

        if(mSettings.getBoolean("show-address-field", true)) {
            y += lineHeight;
            c.drawText(mCurrentContext.getResources().getString(R.string.address), x0, y, p_gray);
            for(String s : mCurrentCustomer.getAddress().split("\n")) {
                c.drawText(s, x1, y, p);
                y += lineHeight;
            }
        }

        if(mSettings.getBoolean("show-group-field", true)) {
            y += lineHeight;
            c.drawText(mCurrentContext.getResources().getString(R.string.group), x0, y, p_gray);
            c.drawText(mCurrentCustomer.mCustomerGroup, x1, y, p);
        }

        if(mSettings.getBoolean("show-notes-field", true)) {
            y += lineHeight; y += lineHeight;
            c.drawText(mCurrentContext.getResources().getString(R.string.notes), x0, y, p_gray);
            for(String s : PrintTools.wordWrap(mCurrentCustomer.mNotes, charsPerLine).split("\n")) {
                c.drawText(s, x1, y, p);
                y += lineHeight;
            }
        }

        y += lineHeight;
        List<CustomField> customFields = mDb.getCustomFields();
        for(CustomField cf : customFields) {
            c.drawText(cf.mTitle, x0, y, p_gray);
            String value = mCurrentCustomer.getCustomField(cf.mTitle);
            if(value != null) {
                for(String s : PrintTools.wordWrap(value, charsPerLine).split("\n")) {
                    c.drawText(s, x1, y, p);
                    y += lineHeight;
                }
            }
            y += lineHeight;
        }

        if(mSettings.getBoolean("show-birthday-field", true)) {
            if(mCurrentCustomer.mBirthday != null) {
                y += lineHeight;
                c.drawText(mCurrentContext.getResources().getString(R.string.birthday), x0, y, p_gray);
                c.drawText(mCurrentCustomer.getBirthdayString(), x1, y, p);
            }
        }

        y += lineHeight;
        c.drawText(mCurrentContext.getResources().getString(R.string.lastchanged), x0, y, p_gray);
        c.drawText(DateControl.displayDateFormat.format(mCurrentCustomer.mLastModified), x1, y, p);

        if(mSettings.getBoolean("show-files", true)) {
            y += lineHeight; y += lineHeight;
            c.drawText(mCurrentContext.getResources().getString(R.string.files), x0, y, p_gray);
            for(CustomerFile file : mCurrentCustomer.getFiles()) {
                c.drawText(file.mName, x1, y, p);
                y += lineHeight;
            }
        }
    }

    @SuppressWarnings("SameParameterValue")
    private static Bitmap resize(Bitmap image, int maxWidth, int maxHeight) {
        if(maxHeight > 0 && maxWidth > 0) {
            int width = image.getWidth();
            int height = image.getHeight();
            float ratioBitmap = (float) width / (float) height;
            float ratioMax = (float) maxWidth / (float) maxHeight;

            int finalWidth = maxWidth;
            int finalHeight = maxHeight;
            if (ratioMax > ratioBitmap) {
                finalWidth = (int) ((float)maxHeight * ratioBitmap);
            } else {
                finalHeight = (int) ((float)maxWidth / ratioBitmap);
            }
            image = Bitmap.createScaledBitmap(image, finalWidth, finalHeight, true);
            return image;
        } else {
            return image;
        }
    }
}
