package de.georgsieber.customerdb.print;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
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

import de.georgsieber.customerdb.R;
import de.georgsieber.customerdb.tools.DateControl;
import de.georgsieber.customerdb.model.Voucher;

public class VoucherPrintDocumentAdapter extends PrintDocumentAdapter {
    private Voucher mCurrentVoucher;
    private Context mCurrentContext;
    private SharedPreferences mSettings;
    private PrintedPdfDocument mPdfDocument;
    private String mCurrency;

    private final int mFontSize;
    private final float mLineHeight;

    public VoucherPrintDocumentAdapter(Context context, Voucher voucher, String currency, SharedPreferences settings) {
        mCurrentVoucher = voucher;
        mCurrentContext = context;
        mCurrency = currency;
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
                .Builder("printvoucher.tmp.pdf")
                .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                .build();
        callback.onLayoutFinished(info, true);
    }

    @Override
    public void onWrite(PageRange[] pages, ParcelFileDescriptor destination, CancellationSignal cancellationSignal, WriteResultCallback callback) {
        drawVoucher();

        // Write PDF document to file
        try {
            mPdfDocument.writeTo(new FileOutputStream( destination.getFileDescriptor() ));
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

    private PdfDocument.Page mPage;
    private Canvas mCanvas;
    private Float mY;

    private void incrementLine(float factor) {
        if(mY != null) mY += mLineHeight * factor;
        if(mY == null
                || (mPage != null && mY + mLineHeight > mPage.getCanvas().getHeight())) {
            int prevPageNumber = mPage==null ? 0 : mPage.getInfo().getPageNumber();
            if(mPage != null) mPdfDocument.finishPage(mPage);
            mPage = mPdfDocument.startPage(prevPageNumber + 1);
            mCanvas = mPage.getCanvas();
            mY = mLineHeight * 2;
        }
    }

    private void drawVoucher() {
        incrementLine(1);

        int x0 = 15;
        int charsPerLine = Math.round((mPage.getInfo().getPageWidth() - 10) / (mFontSize/4));

        Paint p = new Paint();
        Paint p_gray = new Paint();
        p.setColor(Color.BLACK);
        p_gray.setColor(Color.GRAY);
        p.setTextSize(mFontSize);
        mCanvas.drawText(mCurrentVoucher.getCurrentValueString()+mCurrency, x0, mY, p);

        p.setTextSize(mFontSize /2);
        p_gray.setTextSize(mFontSize /2);

        incrementLine(2);
        if(!mCurrentVoucher.mVoucherNo.equals("")) {
            mCanvas.drawText(mCurrentContext.getResources().getString(R.string.voucher_number)+": "+mCurrentVoucher.mVoucherNo, x0, mY, p_gray);
            incrementLine(1.25f);
        }
        mCanvas.drawText(mCurrentContext.getResources().getString(R.string.id)+": "+mCurrentVoucher.getIdString(), x0, mY, p_gray);

        if(mCurrentVoucher.mCurrentValue != mCurrentVoucher.mOriginalValue) {
            incrementLine(1.25f);
            mCanvas.drawText(mCurrentContext.getResources().getString(R.string.original_value), x0, mY, p_gray);
            incrementLine(1);
            mCanvas.drawText(mCurrentVoucher.getOriginalValueString()+mCurrency, x0, mY, p);
        }

        if(mCurrentVoucher.mIssued != null) {
            incrementLine(1.25f);
            mCanvas.drawText(mCurrentContext.getResources().getString(R.string.issued), x0, mY, p_gray);
            incrementLine(1);
            mCanvas.drawText(DateControl.displayDateFormat.format(mCurrentVoucher.mIssued), x0, mY, p);
        }

        if(mCurrentVoucher.mValidUntil != null) {
            incrementLine(1.25f);
            mCanvas.drawText(mCurrentContext.getResources().getString(R.string.valid_until), x0, mY, p_gray);
            incrementLine(1);
            mCanvas.drawText(DateControl.displayDateFormat.format(mCurrentVoucher.mValidUntil), x0, mY, p);
        }

        if(mCurrentVoucher.mRedeemed != null) {
            incrementLine(1.25f);
            mCanvas.drawText(mCurrentContext.getResources().getString(R.string.redeemed), x0, mY, p_gray);
            incrementLine(1);
            mCanvas.drawText(DateControl.displayDateFormat.format(mCurrentVoucher.mRedeemed), x0, mY, p);
        }

        if(!mCurrentVoucher.mFromCustomer.equals("")) {
            incrementLine(1.25f);
            mCanvas.drawText(mCurrentContext.getResources().getString(R.string.from_customer), x0, mY, p_gray);
            incrementLine(1);
            mCanvas.drawText(mCurrentVoucher.mFromCustomer, x0, mY, p);
        }

        if(!mCurrentVoucher.mForCustomer.equals("")) {
            incrementLine(1.25f);
            mCanvas.drawText(mCurrentContext.getResources().getString(R.string.for_customer), x0, mY, p_gray);
            incrementLine(1);
            mCanvas.drawText(mCurrentVoucher.mForCustomer, x0, mY, p);
        }

        if(!mCurrentVoucher.mNotes.equals("")) {
            incrementLine(1.25f);
            mCanvas.drawText(mCurrentContext.getResources().getString(R.string.notes), x0, mY, p_gray);
            for(String s : PrintTools.wordWrap(mCurrentVoucher.mNotes, charsPerLine).split("\n")) {
                incrementLine(1);
                mCanvas.drawText(s, x0, mY, p);
            }
        }

        mPdfDocument.finishPage(mPage);
        mY = null; mPage = null;
    }
}
