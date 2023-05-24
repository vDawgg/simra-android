package de.tuberlin.mcc.simra.app.util;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import androidx.documentfile.provider.DocumentFile;
import de.tuberlin.mcc.simra.app.BuildConfig;
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

    public static boolean zipToDb(Uri toLocation, Context context) {
        final int BUFFER = 2048;
        byte[] buffer = new byte[BUFFER];
        try {
            DocumentFile parent = DocumentFile.fromTreeUri(context, toLocation);
            try {
                parent.findFile("SimRa.zip").delete();
            } catch (NullPointerException ignored) {

            }
            DocumentFile zipFile = parent.createFile("application/zip", "SimRa.zip");
            Uri zipUri = null;
            if (zipFile != null) {
                zipUri = zipFile.getUri();
            }
            FileOutputStream dest = (FileOutputStream) context.getContentResolver().openOutputStream(zipUri);
            ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(dest));

            //Get the content of the MetaData table and create incident and DataLog files for the
            // respective entries
            MetaDataEntry[] metaDataEntries = MetaData.getMetadataEntriesSortedByKey(context);

            return zip(metaDataEntries, out, context);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Zips all rides in the list of metadata files and the shared prefs
     * @param metaDataEntries array of metadata entries for writes that should be written to zip
     * @param out the zip output stream
     * @param context android context
     */
    public static boolean zip(MetaDataEntry[] metaDataEntries, ZipOutputStream out, Context context) {
        int BUFFER = 2048;
        byte[] buffer = new byte[BUFFER];
        try {
            for (MetaDataEntry me : metaDataEntries) {
                //Create and zip the DataLog file
                ZipEntry dataLogFile = new ZipEntry(me.rideId + "_accGps.csv");
                out.putNextEntry(dataLogFile);

                buffer = (Files.getFileInfoLine() + DataLog.DATA_LOG_HEADER + System.lineSeparator()).getBytes(StandardCharsets.UTF_8);
                out.write(buffer, 0, buffer.length);
                for (DataLogEntry de : DataLog.loadDataLogEntriesOfRide(me.rideId, context)) {
                    buffer = (de.stringifyDataLogEntry() + System.lineSeparator()).getBytes(StandardCharsets.UTF_8);
                    out.write(buffer, 0, buffer.length);
                }
                dataLogFile.setTime(me.lastModified);
                out.closeEntry();

                //Create and zip the IncidentLog file
                ZipEntry incidentLogFile = new ZipEntry("accEvents" + me.rideId + ".csv");
                out.putNextEntry(incidentLogFile);

                IncidentLog incidentLog = IncidentLog.loadIncidentLog(me.rideId, context);
                buffer = (Files.getFileInfoLine(incidentLog.nn_version) + IncidentLog.INCIDENT_LOG_HEADER + System.lineSeparator()).getBytes(StandardCharsets.UTF_8);
                out.write(buffer, 0, buffer.length);
                for (IncidentLogEntry ie : incidentLog.getIncidents().values()) {
                    buffer = (ie.stringifyDataLogEntry() + System.lineSeparator()).getBytes(StandardCharsets.UTF_8);
                    out.write(buffer, 0, buffer.length);
                }
                incidentLogFile.setTime(me.lastModified);
                out.closeEntry();
            }
            //Create and zip the MetaData file
            ZipEntry metaDataFile = new ZipEntry("metaData.csv");
            out.putNextEntry(metaDataFile);

            buffer = (Files.getFileInfoLine() + MetaData.METADATA_HEADER + System.lineSeparator()).getBytes(StandardCharsets.UTF_8);
            out.write(buffer, 0, buffer.length);
            for (MetaDataEntry e : metaDataEntries) {
                buffer = (e.stringifyMetaDataEntry() + System.lineSeparator()).getBytes(StandardCharsets.UTF_8);
                out.write(buffer, 0, buffer.length);
            }
            metaDataFile.setTime(metaDataEntries[metaDataEntries.length - 1].lastModified);
            out.closeEntry();

            //Zip the shared prefs files
            File sharedPrefsDirectory = Directories.getSharedPrefsDirectory(context);
            File[] sharedPrefs = sharedPrefsDirectory.listFiles();
            if (sharedPrefs != null) {
                for (File f : Directories.getSharedPrefsDirectory(context).listFiles()) {
                    FileInputStream fi = new FileInputStream(f);
                    BufferedInputStream origin = new BufferedInputStream(fi, BUFFER);
                    ZipEntry zipEntry = new ZipEntry(f.getName());
                    out.putNextEntry(zipEntry);

                    int count;
                    while ((count = fi.read(buffer)) >= 0) {
                        out.write(buffer, 0, count);
                    }
                    zipEntry.setTime(f.lastModified());
                    out.closeEntry();
                }
            }
            out.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void zipDb(MetaDataEntry[] metaDataEntries, Context context) {
        int BUFFER = 2048;
        byte[] buffer = new byte[BUFFER];
        try {
            FileOutputStream dest = new FileOutputStream(Directories.getBaseFolderPath(context) + "zip.zip");
            ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(dest));

            zip(metaDataEntries, out, context);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

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
                if (entry.getName().endsWith("_accGps.csv")) {
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

                    //1st line = nn_version
                    nn_version = Integer.parseInt(stringList.remove(0).split("#", -1)[2]);
                    //2nd line = header
                    stringList.remove(0);
                    for (String s : stringList) {
                        IncidentLogEntry incidentLogEntry = IncidentLogEntry.parseEntryFromLine(s, rideId);
                        Log.d("DEBUG", incidentLogEntry.stringifyDataLogEntry());
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
        BufferedReader br = new BufferedReader(new InputStreamReader(zis));
        List<String> stringList = new ArrayList<>();
        String line;
        while ((line = br.readLine()) != null) {
            stringList.add(line);
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
