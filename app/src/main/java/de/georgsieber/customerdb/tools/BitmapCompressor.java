package de.georgsieber.customerdb.tools;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.File;

public class BitmapCompressor {

    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of mImage
        int height = options.outHeight;
        int width = options.outWidth;
        int inSampleSize = 1;

        while ((width/inSampleSize) > reqWidth || (height/inSampleSize) > reqHeight) {
            inSampleSize += 1;
        }

        Log.i("BitmapCompressor", Integer.toString(inSampleSize));
        return inSampleSize;
    }

    public static Bitmap getSmallBitmap(File file) {

        long originalSize = file.length();

        Log.i("BitmapCompressor", "Original mImage size is: " + originalSize + " bytes.");

        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file.getAbsolutePath(), options);

        // Calculate inSampleSize based on a preset ratio
        options.inSampleSize = calculateInSampleSize(options, 850, 700);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;

        Bitmap compressedImage = BitmapFactory.decodeFile(file.getAbsolutePath(), options);

        Log.i("BitmapCompressor", "Compressed mImage size is " + compressedImage.getByteCount() + " bytes");

        return compressedImage;
    }
}
