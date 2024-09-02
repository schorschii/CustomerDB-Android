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

    private final int mFontSize;
    private final int mLineHeight;

    public CustomerPrintDocumentAdapter(Context context, Customer customer, CustomerDatabase database, SharedPreferences settings) {
        mCurrentCustomer = customer;
        mCurrentContext = context;
        mDb = database;
        mSettings = settings;

        mFontSize = mSettings.getInt("print-font-size", 44);
        mLineHeight = Math.round(mFontSize/1.55f);
    }

    @Override
    public void onLayout(PrintAttributes oldAttributes, PrintAttributes newAttributes, CancellationSignal cancellationSignal, LayoutResultCallback callback, Bundle extras) {
        if(cancellationSignal.isCanceled()) {
            callback.onLayoutCancelled();
            return;
        }

        mPdfDocument = new PrintedPdfDocument(mCurrentContext, newAttributes);

        if(cancellationSignal.isCanceled()) {
            callback.onLayoutCancelled();
            return;
        }

        PrintDocumentInfo info = new PrintDocumentInfo
                .Builder("printcustomer.tmp.pdf")
                .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                .build();
        callback.onLayoutFinished(info, true);
    }

    @Override
    public void onWrite(PageRange[] pages, ParcelFileDescriptor destination, CancellationSignal cancellationSignal, WriteResultCallback callback) {
        drawCustomer();

        // Write PDF document to file
        try {
            mPdfDocument.writeTo(new FileOutputStream( destination.getFileDescriptor() ));
        } catch(IOException e) {
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

    private PdfDocument.Page mPage;
    private Canvas mCanvas;
    private Integer mY;

    private void incrementLine() {
        if(mY != null) mY += mLineHeight;
        if(mY == null
                || (mPage != null && mY + mLineHeight > mPage.getCanvas().getHeight())) {
            int prevPageNumber = mPage==null ? 0 : mPage.getInfo().getPageNumber();
            if(mPage != null) mPdfDocument.finishPage(mPage);
            mPage = mPdfDocument.startPage(prevPageNumber + 1);
            mCanvas = mPage.getCanvas();
            mY = mLineHeight * 2;
        }
    }

    private void drawCustomer() {
        incrementLine();

        int x0 = 15;
        int x1 = Math.round(mPage.getInfo().getPageWidth()/2.8f);
        int charsPerLine = Math.round((mPage.getInfo().getPageWidth() - x1 - 10) / (mFontSize/4));

        if(mSettings.getBoolean("show-customer-picture", true)) {
            if(mCurrentCustomer.getImage().length != 0) {
                Bitmap bitmap = BitmapFactory.decodeByteArray(mCurrentCustomer.getImage(), 0, mCurrentCustomer.getImage().length);
                int width = (int) (mFontSize * 1.5);
                Rect dstRect = new Rect(mCanvas.getWidth() - width - x0, x0, mCanvas.getWidth() - x0, x0 + width);
                mCanvas.drawBitmap(bitmap, null, dstRect, null);
            }
        }

        Paint p = new Paint();
        Paint p_gray = new Paint();
        p.setColor(Color.BLACK);
        p_gray.setColor(Color.GRAY);
        p.setTextSize(mFontSize);
        mCanvas.drawText(mCurrentCustomer.getFullName(false), x0, mY, p);
        incrementLine();

        p.setTextSize(mFontSize /2);
        p_gray.setTextSize(mFontSize /2);

        if(mSettings.getBoolean("show-phone-field", true)) {
            incrementLine();
            mCanvas.drawText(mCurrentContext.getResources().getString(R.string.phonehome), x0, mY, p_gray);
            mCanvas.drawText(mCurrentCustomer.mPhoneHome, x1, mY, p);

            incrementLine();
            mCanvas.drawText(mCurrentContext.getResources().getString(R.string.phonemobile), x0, mY, p_gray);
            mCanvas.drawText(mCurrentCustomer.mPhoneMobile, x1, mY, p);

            incrementLine();
            mCanvas.drawText(mCurrentContext.getResources().getString(R.string.phonework), x0, mY, p_gray);
            mCanvas.drawText(mCurrentCustomer.mPhoneWork, x1, mY, p);
        }

        if(mSettings.getBoolean("show-email-field", true)) {
            incrementLine();
            mCanvas.drawText(mCurrentContext.getResources().getString(R.string.email), x0, mY, p_gray);
            mCanvas.drawText(mCurrentCustomer.mEmail, x1, mY, p);
        }

        if(mSettings.getBoolean("show-address-field", true)) {
            incrementLine();
            mCanvas.drawText(mCurrentContext.getResources().getString(R.string.address), x0, mY, p_gray);
            for(String s : mCurrentCustomer.getAddress().split("\n")) {
                mCanvas.drawText(s, x1, mY, p);
                incrementLine();
            }
        }

        if(mSettings.getBoolean("show-group-field", true)) {
            incrementLine();
            mCanvas.drawText(mCurrentContext.getResources().getString(R.string.group), x0, mY, p_gray);
            mCanvas.drawText(mCurrentCustomer.mCustomerGroup, x1, mY, p);
        }

        if(mSettings.getBoolean("show-notes-field", true)) {
            incrementLine(); incrementLine();
            mCanvas.drawText(mCurrentContext.getResources().getString(R.string.notes), x0, mY, p_gray);
            for(String s : PrintTools.wordWrap(mCurrentCustomer.mNotes, charsPerLine).split("\n")) {
                mCanvas.drawText(s, x1, mY, p);
                incrementLine();
            }
        }

        incrementLine();
        List<CustomField> customFields = mDb.getCustomFields();
        for(CustomField cf : customFields) {
            mCanvas.drawText(cf.mTitle, x0, mY, p_gray);
            String value = mCurrentCustomer.getCustomField(cf.mTitle);
            if(value != null) {
                for(String s : PrintTools.wordWrap(value, charsPerLine).split("\n")) {
                    mCanvas.drawText(s, x1, mY, p);
                    incrementLine();
                }
            }
            incrementLine();
        }

        if(mSettings.getBoolean("show-birthday-field", true)) {
            if(mCurrentCustomer.mBirthday != null) {
                incrementLine();
                mCanvas.drawText(mCurrentContext.getResources().getString(R.string.birthday), x0, mY, p_gray);
                mCanvas.drawText(mCurrentCustomer.getBirthdayString(), x1, mY, p);
            }
        }

        incrementLine();
        mCanvas.drawText(mCurrentContext.getResources().getString(R.string.lastchanged), x0, mY, p_gray);
        mCanvas.drawText(DateControl.displayDateFormat.format(mCurrentCustomer.mLastModified), x1, mY, p);

        if(mSettings.getBoolean("show-files", true)) {
            incrementLine(); incrementLine();
            mCanvas.drawText(mCurrentContext.getResources().getString(R.string.files), x0, mY, p_gray);
            for(CustomerFile file : mCurrentCustomer.getFiles()) {
                mCanvas.drawText(file.mName, x1, mY, p);
                incrementLine();
            }
        }

        mPdfDocument.finishPage(mPage);
        mY = null; mPage = null;
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
