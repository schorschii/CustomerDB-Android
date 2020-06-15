package de.georgsieber.customerdb.tools;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import java.io.File;

public class StorageControl {
    public static File getStorageLogo(Context c) {
        File exportDir = c.getExternalFilesDir(null);
        return new File(exportDir, "logo.png");
    }
    public static File getStorageExportCsv(Context c) {
        return getFile("export", "export.csv", c);
    }
    public static File getStorageExportVcf(Context c) {
        return getFile("export", "export.vcf", c);
    }
    public static File getStorageImageTemp(Context c) {
        return getFile("tmp", "image.tmp.jpg", c);
    }
    public static File getStorageAppTemp(Context c) {
        return getFile("tmp", "plugin.apk", c);
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

}
