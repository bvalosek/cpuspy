//-----------------------------------------------------------------------------
//
// (C) Brandon Valosek, 2011 <bvalosek@gmail.com>
//
//-----------------------------------------------------------------------------

package com.bvalosek.cpuspy;

// imports
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import android.app.Application;
import android.content.SharedPreferences;
import android.util.Log;
import android.util.SparseArray;

/** main application class */
public class CpuSpyApp extends Application {

    private static final String KERNEL_VERSION_PATH = "/proc/version";

    private static final String TAG = "CpuSpyApp";

    private static final String PREF_NAME = "CpuSpyPreferences";
    private static final String PREF_OFFSETS = "offsets";

    /** the long-living object used to monitor the system frequency states */
    private CpuStateMonitor _monitor = new CpuStateMonitor();

    private String _kernelVersion = "";

    /**
     * On application start, load the saved offsets and stash the
     * current kernel version string
     */
    @Override public void onCreate(){
    	super.onCreate();
        loadSettings();
        updateKernelVersion();
    }
    
    @Override public void onTerminate() {
    	super.onTerminate();
    	saveSettings();
    }
    
    /** @return the kernel version string */
    public String getKernelVersion() {
        return _kernelVersion;
    }

    /** @return the internal CpuStateMonitor object */
    public CpuStateMonitor getCpuStateMonitor() {
        return _monitor;
    }

    public void loadSettings()
    {
    	loadOffsets();
    	
        SharedPreferences settings = getSharedPreferences(
                PREF_NAME, MODE_PRIVATE);
        String prefs = settings.getString (PREF_OFFSETS, "");

        if (prefs == null || prefs.length() < 1) {
            return;
        }
        
        Logger.setEnabled(settings.getBoolean("log", true));	
    }
    
    /**
     * Load the saved string of offsets from preferences and put it into
     * the state monitor
     */
    public void loadOffsets() {
        SharedPreferences settings = getSharedPreferences(
                PREF_NAME, MODE_PRIVATE);
        String prefs = settings.getString (PREF_OFFSETS, "");

        if (prefs == null || prefs.length() < 1) {
            return;
        }      
        
        // split the string by peroids and then the info by commas and load
        final SparseArray<Long> offsets = new SparseArray<Long>();
        final String[] sOffsets = prefs.split(",");
        for (String offset : sOffsets) {
            final String[] parts = offset.split(" ");
            offsets.put (Integer.parseInt(parts[0]),
                         Long.valueOf(parts[1]));
        }

        _monitor.setOffsets(offsets);
    }

    public void saveSettings()
    {
    	saveOffsets();
    	
        SharedPreferences settings = getSharedPreferences(
                PREF_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();

        editor.putBoolean("log", Logger.isEnabled());
        editor.commit();
    }
    
    /**
     * Save the state-time offsets as a string
     * e.g. "100 24, 200 251, 500 124 etc
     */
    public void saveOffsets() {
        SharedPreferences settings = getSharedPreferences(
                PREF_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();

        // build the string by iterating over the freq->duration map
        String str = "";

        final SparseArray<Long> offsets = _monitor.getOffsets();
        int count = offsets.size();
        for(int i = 0; i < count; i++) {
        	   str += offsets.keyAt(i) + " " + offsets.valueAt(i) + ",";
        	}

        editor.putString(PREF_OFFSETS, str);
        editor.commit();
    }

    /** Try to read the kernel version string from the proc fileystem */
    public String updateKernelVersion() {
        try {
            InputStream is = new FileInputStream(KERNEL_VERSION_PATH);
            InputStreamReader ir = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(ir);

            String line;
            while ((line = br.readLine())!= null ) {
                _kernelVersion = line;
            }

            is.close();
        } catch (IOException e) {
            Log.e(TAG, "Problem reading kernel version file");
            return "";
        }

        // made it
        return _kernelVersion;
    }
}
