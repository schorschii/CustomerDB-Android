package de.georgsieber.customerdb.tools;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import java.io.File;
import java.util.Date;

import de.georgsieber.customerdb.R;

public class StorageControl {

    private static String DIR_TEMP = "tmp";
    private static String DIR_EXPORT = "export";

    public static File getStorageLogo(Context c) {
        File exportDir = c.getExternalFilesDir(null);
        return new File(exportDir, "logo.png");
    }
    public static File getStorageExportCsv(Context c) {
        return getFile(DIR_EXPORT, "export.csv", c);
    }
    public static File getStorageExportVcf(Context c) {
        return getFile(DIR_EXPORT, "export.vcf", c);
    }
    public static File getStorageImageTemp(Context c) {
        return getFile(DIR_TEMP, "image.tmp.jpg", c);
    }
    public static File getStorageAppTemp(Context c) {
        return getFile(DIR_TEMP, "plugin.apk", c);
    }
    public static File getStorageFileTemp(Context c, String filename) {
        return getFile(DIR_TEMP, filename, c);
    }

    private static File getFile(String dir, String filename, Context c) {
        File exportDir = new File(c.getExternalFilesDir(null), dir);
        exportDir.mkdirs();
        return new File(exportDir, filename);
    }

    public static void scanFile(File f, Context c) {
        Uri uri = Uri.fromFile(f);
        Intent scanFileIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri);
        c.sendBroadcast(scanFileIntent);
    }

    public static String getNewPictureFilename(Context c) {
        return (c.getString(R.string.picture) + " " + DateControl.displayDateFormat.format(new Date())).replaceAll("[^A-Za-z0-9 ]", "_") + ".jpg";
    }

    public static String getNewDrawingFilename(Context c) {
        return (c.getString(R.string.drawing) + " " + DateControl.displayDateFormat.format(new Date())).replaceAll("[^A-Za-z0-9 ]", "_") + ".jpg";
    }

    public static void deleteTempFiles(Context c) {
        try {
            deleteFilesInDirectory(new File(c.getExternalFilesDir(null), DIR_TEMP), c);
        } catch(Exception ignored) {}
    }

    public static boolean deleteFilesInDirectory(File fileOrDirectory, Context c) {
        if(!fileOrDirectory.isDirectory()) return false;
        for(File child : fileOrDirectory.listFiles()) {
            if(!child.isDirectory()) {
                if(!child.delete()) return false;
                scanFile(child, c); // otherwise file still appears via MTP...
            }
        }
        return true;
    }

}
