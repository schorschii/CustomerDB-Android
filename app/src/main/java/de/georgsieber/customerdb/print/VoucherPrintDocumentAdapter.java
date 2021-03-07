package de.georgsieber.customerdb.print;

import android.content.Context;
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
    private PrintedPdfDocument mPdfDocument;
    private String mCurrency;

    public VoucherPrintDocumentAdapter(Context context, Voucher voucher, String currency) {
        mCurrentVoucher = voucher;
        mCurrentContext = context;
        mCurrency = currency;
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
                .Builder("printvoucher.tmp.pdf")
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

        int fontSize = page.getInfo().getPageWidth()/15;
        int lineHeight = Math.round(fontSize/1.55f);
        int x0 = 15;
        int x1 = Math.round(page.getInfo().getPageWidth()/2.8f);
        int y = lineHeight*2;
        int charsPerLine = Math.round((page.getInfo().getPageWidth() - 10) / (fontSize/4));

        Paint p = new Paint();
        Paint p_gray = new Paint();
        p.setColor(Color.BLACK);
        p_gray.setColor(Color.GRAY);
        p.setTextSize(fontSize);
        c.drawText(mCurrentVoucher.getCurrentValueString()+mCurrency, x0, y, p);

        p.setTextSize(fontSize/2);
        p_gray.setTextSize(fontSize/2);

        y += lineHeight*2;
        if(!mCurrentVoucher.mVoucherNo.equals("")) {
            c.drawText(mCurrentContext.getResources().getString(R.string.voucher_number)+": "+mCurrentVoucher.mVoucherNo, x0, y, p_gray);
            y += lineHeight*1.25f;
        }
        c.drawText(mCurrentContext.getResources().getString(R.string.id)+": "+mCurrentVoucher.getIdString(), x0, y, p_gray);

        if(mCurrentVoucher.mCurrentValue != mCurrentVoucher.mOriginalValue) {
            y += lineHeight*1.25f;
            c.drawText(mCurrentContext.getResources().getString(R.string.original_value), x0, y, p_gray);
            y += lineHeight;
            c.drawText(mCurrentVoucher.getOriginalValueString()+mCurrency, x0, y, p);
        }

        if(mCurrentVoucher.mIssued != null) {
            y += lineHeight*1.25f;
            c.drawText(mCurrentContext.getResources().getString(R.string.issued), x0, y, p_gray);
            y += lineHeight;
            c.drawText(DateControl.displayDateFormat.format(mCurrentVoucher.mIssued), x0, y, p);
        }

        if(mCurrentVoucher.mValidUntil != null) {
            y += lineHeight*1.25f;
            c.drawText(mCurrentContext.getResources().getString(R.string.valid_until), x0, y, p_gray);
            y += lineHeight;
            c.drawText(DateControl.displayDateFormat.format(mCurrentVoucher.mValidUntil), x0, y, p);
        }

        if(mCurrentVoucher.mRedeemed != null) {
            y += lineHeight*1.25f;
            c.drawText(mCurrentContext.getResources().getString(R.string.redeemed), x0, y, p_gray);
            y += lineHeight;
            c.drawText(DateControl.displayDateFormat.format(mCurrentVoucher.mRedeemed), x0, y, p);
        }

        if(!mCurrentVoucher.mFromCustomer.equals("")) {
            y += lineHeight*1.25f;
            c.drawText(mCurrentContext.getResources().getString(R.string.from_customer), x0, y, p_gray);
            y += lineHeight;
            c.drawText(mCurrentVoucher.mFromCustomer, x0, y, p);
        }

        if(!mCurrentVoucher.mForCustomer.equals("")) {
            y += lineHeight*1.25f;
            c.drawText(mCurrentContext.getResources().getString(R.string.for_customer), x0, y, p_gray);
            y += lineHeight;
            c.drawText(mCurrentVoucher.mForCustomer, x0, y, p);
        }

        if(!mCurrentVoucher.mNotes.equals("")) {
            y += lineHeight*1.25f;
            c.drawText(mCurrentContext.getResources().getString(R.string.notes), x0, y, p_gray);
            for(String s : PrintTools.wordWrap(mCurrentVoucher.mNotes, charsPerLine).split("\n")) {
                y += lineHeight;
                c.drawText(s, x0, y, p);
            }
        }
    }
}
