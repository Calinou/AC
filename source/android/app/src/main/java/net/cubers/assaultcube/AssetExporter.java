package net.cubers.assaultcube;

import android.app.Activity;
import android.content.res.AssetManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

public class AssetExporter {

    private static String appversioncodefile = "androidappversioncode.txt"; // keep this synchronized with C++ world in serverbrowser.cpp

    public boolean isAssetExportRequired(Activity activity) {
        // Exporting assets is a heavy operation and so we skip this operation if
        // the previously exported assets are of the same version as the currently running app.
        // Essentially we export assets once each time the app has been updated to a new version.
        boolean versionsMatch = getVersionCodeOfCurrentApp().equals(getVersionCodeOfLastExportedAssets(activity));
        return !versionsMatch;
    }

    public boolean copyAssets(Activity activity) {
        AssetManager assetManager = activity.getAssets();
        ArrayList<String> files = new ArrayList<>();
        recursiveList(assetManager, "", files);
        boolean allWentWell = true;
        for (String filename : files) {
            InputStream in = null;
            OutputStream out = null;
            try {
                // The File:API is of no use - only *existing* paths can be checked if they're directories
                boolean isAssetContainer = false;
                try {
                    in = assetManager.open(filename);
                } catch(FileNotFoundException e) { // asset directories throw this exception and there appears to be no easier way to handle this issue
                    isAssetContainer = true;
                }
                if (!isAssetContainer){
                    File outFile = new File(activity.getExternalFilesDir(null), filename);
                    File parent = outFile.getParentFile();
                    if(parent != null) parent.mkdirs();
                    out = new FileOutputStream(outFile);
                    copyFile(in, out);
                }
            } catch(IOException e) {
                Log.e("assets", "Failed to copy asset file: " + filename, e);
                allWentWell = false;
            }
            finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                        // NOOP
                    }
                }
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException e) {
                        // NOOP
                    }
                }
            }
        }
        if (allWentWell) {
            trySetVersionCodeOfLastExportedAssets(activity, getVersionCodeOfCurrentApp());
        }
        return allWentWell;
    }

    private void recursiveList(AssetManager assetManager, String path, ArrayList<String> output)
    {
        try {
            String[] children = assetManager.list(path);
            for(String child : children)
            {
                String childpath = (path.equals("") ? "" : path + "/") + child;
                output.add(childpath);
                recursiveList(assetManager, childpath, output);
            }
        } catch (IOException e) {
            Log.e("assets", "Failed to get asset file list.", e);
        }
    }

    private void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while((read = in.read(buffer)) != -1){
            out.write(buffer, 0, read);
        }
    }

    private Integer getVersionCodeOfCurrentApp()
    {
        return BuildConfig.VERSION_CODE;
    }

    private Integer getVersionCodeOfLastExportedAssets(Activity activity)
    {
        File f = new File(activity.getExternalFilesDir(null), appversioncodefile);
        if(!f.exists()) return null;

        try {
            FileInputStream stream = new FileInputStream(f);
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            String line = reader.readLine();
            reader.close();
            return Integer.parseInt(line);
        } catch (IOException e) {
            return null;
        }
    }

    private void trySetVersionCodeOfLastExportedAssets(Activity activity, Integer version) {
        File f = new File(activity.getExternalFilesDir(null), appversioncodefile);
        try {
            FileOutputStream stream = new FileOutputStream(f);
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(stream));
            writer.write(version.toString());
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
