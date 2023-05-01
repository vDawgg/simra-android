package de.tuberlin.mcc.simra.app.util;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import androidx.documentfile.provider.DocumentFile;
import de.tuberlin.mcc.simra.app.BuildConfig;
import de.tuberlin.mcc.simra.app.R;
import de.tuberlin.mcc.simra.app.activities.AboutActivity;
import de.tuberlin.mcc.simra.app.activities.SettingsActivity;
import de.tuberlin.mcc.simra.app.entities.DataLog;
import de.tuberlin.mcc.simra.app.entities.DataLogEntry;
import de.tuberlin.mcc.simra.app.entities.IncidentLog;
import de.tuberlin.mcc.simra.app.entities.IncidentLogEntry;
import de.tuberlin.mcc.simra.app.entities.MetaData;
import de.tuberlin.mcc.simra.app.entities.MetaDataEntry;

import static de.tuberlin.mcc.simra.app.util.SharedPref.clearSharedPrefs;
import static de.tuberlin.mcc.simra.app.util.SharedPref.createEntry;

public class IOUtils {
    private static final String TAG = "IOUtils_LOG";

    public static boolean isDirectoryEmpty(String path) {
        File dir = new File(path);
        if (dir.isDirectory()) {
            return dir.listFiles().length == 0;
        }
        return false;
    }

    /*
     *
     * Zips a file at a location and places the resulting zip file at the toLocation
     * Example: zipFileAtPath("downloads/myfolder", "downloads/myFolder.zip");
     */
    public static void zipFolder(String inputFolderPath, String outZipPath) {
        try {
            FileOutputStream fos = new FileOutputStream(outZipPath);
            ZipOutputStream zos = new ZipOutputStream(fos);
            File srcFile = new File(inputFolderPath);
            File[] files = srcFile.listFiles();
            Log.d(TAG, "Zip directory: " + srcFile.getName());
            for (int i = 0; i < files.length; i++) {
                Log.d("", "Adding file: " + files[i].getName());
                byte[] buffer = new byte[1024];
                FileInputStream fis = new FileInputStream(files[i]);
                zos.putNextEntry(new ZipEntry(files[i].getName()));
                int length;
                while ((length = fis.read(buffer)) > 0) {
                    zos.write(buffer, 0, length);
                }
                zos.closeEntry();
                fis.close();
            }
            zos.close();
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }
    }

    public static boolean zipTo(String sourcePath, Uri toLocation, Context ctx) {

        final int BUFFER = 2048;

        File sourceFile = new File(sourcePath);
        try {
            BufferedInputStream origin = null;
            DocumentFile parent = DocumentFile.fromTreeUri(ctx, toLocation);
            try {
                parent.findFile("SimRa.zip").delete();
            } catch (NullPointerException ignored) {

            }
            DocumentFile zipFile = parent.createFile("application/zip", "SimRa.zip");
            Uri zipUri = null;
            if (zipFile != null) {
                zipUri = zipFile.getUri();
            }
            FileOutputStream dest = (FileOutputStream) ctx.getContentResolver().openOutputStream(zipUri);
            ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(dest));
            if (sourceFile.isDirectory()) {
                Log.d(TAG, "parent: " + sourceFile.getParent() + " length: " + sourceFile.getParent().length());
                zipSubFolder(out, sourceFile, sourceFile.getParent().length(),ctx);
            } else {
                byte[] data = new byte[BUFFER];
                FileInputStream fi = new FileInputStream(sourcePath);
                origin = new BufferedInputStream(fi, BUFFER);
                ZipEntry entry = new ZipEntry(getLastPathComponent(sourcePath));
                entry.setTime(sourceFile.lastModified()); // to keep modification time after unzipping
                out.putNextEntry(entry);

                int count;
                while ((count = origin.read(data, 0, BUFFER)) != -1) {
                    out.write(data, 0, count);
                }}

            out.close();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /*
     *
     * Zips a subfolder
     *
     */

    private static void zipSubFolder(ZipOutputStream out, File folder,
                              int basePathLength, Context ctx) throws IOException {
        if(!(folder.getAbsolutePath().equals(ctx.getFilesDir().getParent()) || folder.getAbsolutePath().contains("files") || folder.getAbsolutePath().contains("shared_prefs"))) {
            return;
        }
        final int BUFFER = 2048;

        File[] fileList = folder.listFiles();
        BufferedInputStream origin = null;
        for (File file : fileList) {
            Log.d(TAG, "file: " + file.getPath() + " is directory: " + file.isDirectory());
            if (file.isDirectory()) {
                zipSubFolder(out, file, basePathLength, ctx);
            } else {

                byte data[] = new byte[BUFFER];
                String unmodifiedFilePath = file.getPath();
                String relativePath = unmodifiedFilePath
                        .substring(basePathLength);
                if(!relativePath.contains(".zip")) {
                    FileInputStream fi = new FileInputStream(unmodifiedFilePath);

                    origin = new BufferedInputStream(fi, BUFFER);
                    ZipEntry entry = new ZipEntry(relativePath);

                    entry.setTime(file.lastModified()); // to keep modification time after unzipping
                    out.putNextEntry(entry);
                    int count;
                    while ((count = origin.read(data, 0, BUFFER)) != -1) {
                        out.write(data, 0, count);
                    }
                    origin.close();
                }
            }
        }
    }

    /*
     * gets the last path component
     *
     * Example: getLastPathComponent("downloads/example/fileToZip");
     * Result: "fileToZip"
     */
    public static String getLastPathComponent(String filePath) {
        String[] segments = filePath.split("/");
        if (segments.length == 0)
            return "";
        String lastPathComponent = segments[segments.length - 1];
        return lastPathComponent;
    }

    //TODO: Implement this using the db!
    public static void zip(List<File> files, File zipFile ) throws IOException {
        final int BUFFER_SIZE = 4096;

        BufferedInputStream origin;

        try (ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(zipFile)))) {
            byte[] data = new byte[BUFFER_SIZE];

            for (File file : files) {
                if (!file.isFile()) {
                    continue;
                }
                FileInputStream fileInputStream = new FileInputStream(file);

                origin = new BufferedInputStream(fileInputStream, BUFFER_SIZE);

                String filePath = file.getAbsolutePath();

                try {
                    ZipEntry entry = new ZipEntry(filePath.substring(filePath.lastIndexOf("/") + 1));

                    out.putNextEntry(entry);

                    int count;
                    while ((count = origin.read(data, 0, BUFFER_SIZE)) != -1) {
                        out.write(data, 0, count);
                    }
                } finally {
                    origin.close();
                }
            }
        }
    }

    /**
     * imports rides, settings and statistics from a previously via export created SimRa.zip file.
     * The settings and statistics (.xml files) are being parsed and written as shared preferences.
     * The rides, incidents, metaData and other files and folders are simply extracted as .csv files.
     * @param zipUri The URI of SimRa.zip chosen by the file picker
     * @param context Activity context needed to write to shared preferences
     * @return false, if IOException is thrown, true otherwise.
     */
    public static boolean importSimRaData(Uri zipUri, Context context) {
        InputStream is;
        ZipInputStream zis;
        try {
            is = context.getContentResolver().openInputStream(zipUri);
            zis = new ZipInputStream(new BufferedInputStream(is));
            ZipEntry entry;

            while ((entry = zis.getNextEntry()) != null) {
                String entryFilePath = entry.getName().replaceFirst("/" + context.getPackageName(), "");
                String[] entryFilePathArray = entryFilePath.split("/");
                StringBuilder tempPath = new StringBuilder(context.getFilesDir().getParentFile().getPath());
                for (int i = 1; i < entryFilePathArray.length; i++) {
                    tempPath.append("/").append(entryFilePathArray[i]);
                    // create parent directories, if not present yet
                    if (i < entryFilePathArray.length - 1) {
                        File folder = new File(tempPath.toString());
                        if (!folder.exists()) {
                            folder.mkdir();
                        }
                    // delete the file if it already exists. Otherwise, extract it or parse the xml.
                    } else {
                        File file = new File(tempPath.toString());
                        if (file.exists()) {
                            file.delete();
                        }
                        if (file.getName().endsWith(".xml")) {
                            loadSharePrefs(file, zis, context);
                        } else {
                            unzipContent(file, zis);
                        }
                    }
                }
                zis.closeEntry();
            }

            zis.close();
        }
        catch(IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    // TODO: Test this!
    /**
     * Imports the files contained in the zip folder to the db (for csv) or shared prefs (for xml)
     * @param zipUri
     * @param context
     * @return
     */
    public static boolean importSimRaDataDB(Uri zipUri, Context context) {
        InputStream is;
        ZipInputStream zis;
        try {
            is = context.getContentResolver().openInputStream(zipUri);
            zis = new ZipInputStream(new BufferedInputStream(is));
            ZipEntry entry;

            while ((entry = zis.getNextEntry()) != null) {
                //TODO: Find out what the suffixes for the different csv files are!
                if (entry.getName().endsWith("_accGps.csv")) {
                    //TODO: Check if this actually only reads the content of one file!
                    List<String> stringList = unzipToStringList(zis);
                    List<DataLogEntry> dataLogEntries = new ArrayList<>();

                    String entryWithoutSuffix = entry.getName().replace("_accGps.csv", "");
                    int rideId = Integer.parseInt(entryWithoutSuffix.substring(entryWithoutSuffix.length() - 1));

                    for (String s : stringList) {
                        if (s.contains("#") || s.contains("lat")) continue;
                        dataLogEntries.add(DataLogEntry.parseDataLogEntryFromLine(s, rideId));
                    }
                    DataLog.saveDataLogEntries(dataLogEntries, context);
                    continue;
                }
                if (entry.getName().contains("accEvents")) {
                    List<String> stringList = unzipToStringList(zis);
                    TreeMap<Integer, IncidentLogEntry> incidentLogEntries = new TreeMap<>();

                    int nn_version = 0;
                    String entryWithoutSuffix = entry.getName().replace(".csv", "");
                    int rideId = Integer.parseInt(entryWithoutSuffix.substring(entryWithoutSuffix.length() - 1));

                    //TODO: Fix this if there is still enough time as this is more of a shitshow than it needs to be
                    for (String s : stringList) {
                        if (s.split("#").length>2) {
                            nn_version = Integer.parseInt(s.split("#",-1)[2]);
                        }
                        if (s.contains("key")) continue;
                        IncidentLogEntry incidentLogEntry = IncidentLogEntry.parseEntryFromLine(s);
                        incidentLogEntries.put(incidentLogEntry.key, incidentLogEntry);
                    }
                    IncidentLog incidentLog = new IncidentLog(rideId, incidentLogEntries, nn_version);
                    IncidentLog.saveIncidentLog(incidentLog, context);
                    continue;
                }
                if (entry.getName().endsWith("metaData.csv")) {
                    List<String> stringList = unzipToStringList(zis);
                    List<MetaDataEntry> metaDataEntries = new ArrayList<>();

                    for (String s : stringList) {
                        if (s.contains("#") || s.contains("key")) continue;
                        metaDataEntries.add(MetaDataEntry.parseEntryFromLine(s));
                    }

                    MetaData.updateOrAddMetadataEntries(metaDataEntries, context);
                    continue;
                }
                if (entry.getName().endsWith(".xml")) {
                    File temp = new File(IOUtils.Directories.getSharedPrefsDirectory(context)+"temp.xml");
                    loadSharePrefs(temp, zis, context);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * Parses a shared preferences file (.xml) and overwrites/creates new entries in the respective
     * shared preferences file.
     * @param file
     * @param zis
     * @param context
     * @throws IOException
     */
    private static void loadSharePrefs(File file, ZipInputStream zis, Context context) throws IOException {
        File tempFile = new File(file.getAbsolutePath().replace(".xml","_temp.xml"));
        unzipContent(tempFile, zis);
        String sharedPrefName = file.getName().replace(".xml","");
        clearSharedPrefs(sharedPrefName, context);
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(tempFile))) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                if (!line.trim().isEmpty() && !line.startsWith("<?xml") && !line.startsWith("<map>") && !line.startsWith("</map>")) {
                    createEntry(sharedPrefName, line, context);
                }
            }
        }
        tempFile.delete();
    }

    /**
     * Unzips the content in zis to file.
     * @param file
     * @param zis
     * @throws IOException
     */
    private static void unzipContent(File file, ZipInputStream zis) throws IOException {
        FileOutputStream fOut;
        int count;
        byte[] buffer = new byte[1024];
        // Log.d(TAG, "file " + file.getPath() + " is being created");
        file.createNewFile();
        fOut = new FileOutputStream(file);
        while ((count = zis.read(buffer)) != -1) {
            fOut.write(buffer, 0, count);
        }
        fOut.close();
    }

    private static List<String> unzipToStringList(ZipInputStream zis) throws IOException {
        byte[] buffer = new byte[1024];
        List<String> stringList = new ArrayList<>();
        while (zis.read(buffer) != -1) {
            //TODO: Should probably skip the first few entries if they only contain the header and an empty line
            stringList.add(new String(buffer, StandardCharsets.UTF_8));
        }
        return stringList;
    }

    public static class Directories {

        /**
         * Returns the Base Folder (Private App File Directory)
         *
         * @param ctx The App Context
         * @return Path with trailing slash
         */
        public static String getBaseFolderPath(Context ctx) {
            // return getExternalBaseDirectoryPath();
            return ctx.getFilesDir() + "/";
        }

        public static File getSharedPrefsDirectory(Context context) {
            File[] dirs = context.getFilesDir().getParentFile().listFiles();
            for (int i = 0; i < dirs.length; i++) {
                if (dirs[i].getName().equals("shared_prefs")) {
                    return dirs[i];
                }
            }
            return null;
        }

        /**
         * Returns the External Folder Path (Shared File Directory)
         * Might be on SD Card
         *
         * @return Path with trailing slash
         */

        public static String getExternalBaseDirectoryPath() {
            String app_folder_path = Environment.getExternalStorageDirectory().toString() + "/simra/";
            File dir = new File(app_folder_path);
            if (!dir.exists() && !dir.mkdirs()) {

            }
            return app_folder_path;
        }
    }

    /**
     * Well known Files
     * Using this should be only temporary
     * Those we need access to from all over the app, because the access was never centralized...
     */
    public static class Files {
        public static String getFileInfoLine() {
            return BuildConfig.VERSION_CODE + "#1" + System.lineSeparator();
        }
        public static String getFileInfoLine(int modelVersion) {
            return BuildConfig.VERSION_CODE + "#1#" + modelVersion + System.lineSeparator();
        }
        public static String getMetaDataFilePath(Context context) {
            return IOUtils.Directories.getBaseFolderPath(context) + "metaData.csv";
        }

        public static File getMetaDataFile(Context context) {
            return new File(getMetaDataFilePath(context));
        }

        public static String getGPSLogFileName(int rideId, boolean isTempFile) {
            return (isTempFile ? "Temp" : "") + rideId + "_accGps.csv";
        }

        public static String getGPSLogFilePath(int rideId, boolean isTempFile, Context context) {
            return IOUtils.Directories.getBaseFolderPath(context) + getGPSLogFileName(rideId, isTempFile);
        }

        public static File getGPSLogFile(int rideId, boolean isTempFile, Context context) {
            return new File(getGPSLogFilePath(rideId, isTempFile, context));
        }

        public static File getRegionsFile(Context context) {
            return new File(IOUtils.Directories.getBaseFolderPath(context) + "simRa_regions.config");
        }

        public static File getDENewsFile(Context context) {
            return new File(IOUtils.Directories.getBaseFolderPath(context) + "simRa_news_de.config");
        }
        public static File getENNewsFile(Context context) {
            return new File(IOUtils.Directories.getBaseFolderPath(context) + "simRa_news_en.config");
        }

    }
}
