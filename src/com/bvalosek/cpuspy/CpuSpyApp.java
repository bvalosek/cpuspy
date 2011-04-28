//-----------------------------------------------------------------------------
// cpuspy
//
// (c) Brandon Valosek, 2011 <bvalosek@gmail.com>
//
// CpuSpyApp.java
//
// Main application class
//
//-----------------------------------------------------------------------------

package com.bvalosek.cpuspy;

// imports
import android.app.Application;
import android.util.Log;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import android.content.SharedPreferences;
import android.os.SystemClock;
import java.util.Collections;
import java.io.DataOutputStream;

/** main application class */
public class CpuSpyApp extends Application {

   // various FS points we query
   public static final String TIME_IN_STATE_PATH =
      "/sys/devices/system/cpu/cpu0/cpufreq/stats/time_in_state";
   public static final String VERSION_PATH = "/proc/version";

   // used during settings save
   private static final String PREF_NAME = "cpuspy";
   private static final String PREF_OFFSETS = "cpuOffsets";

   /** offsets used if user reset */
   private Map<Integer, Integer> mOffsets = new HashMap<Integer, Integer>();

   /** array of the states / duration */
   private List<CpuState> mStates = new ArrayList<CpuState>();

   /** kernel build string */
   private String mKernelString = "";

   /** on startup */
   @Override public void onCreate () {
      // load offsets
      loadOffsets ();
   }

   /** access state list */
   public List<CpuState> getStates () {
      List<CpuState> ret = new ArrayList<CpuState>();

      /* if we have an offset for a state, subtract it, otherwise
       * just go with it */
      for (CpuState state : mStates) {
         int dur = state.duration;
         if (mOffsets.containsKey(state.freq) ) {
            dur = state.duration - mOffsets.get (state.freq);

            // if dur is negative, it means we've rebooted since
            // last offset save, so clear them out
            if (dur < 0)
               dur = state.duration;
         }

         ret.add( new CpuState (state.freq, dur) );
      }

      return ret;
   }

   /** access kerenl string*/
   public String getKernelString () {
      return mKernelString;
   }

   /** get the total state time */
   public int getTotalStateTime () {
      // looop through and add up
      int r = 0;
      for (CpuState state : mStates) {
         r += state.duration;
      }

      // account for offsets
      int offs = 0;
      for (Map.Entry<Integer,Integer> entry : mOffsets.entrySet() ) {
         offs += entry.getValue ();
      }

      return r - offs;
   }

   /** simple struct for states/time */
   public class CpuState implements Comparable<CpuState> {
      public CpuState(int a, int b) { freq = a; duration =b; }
      public int freq = 0;
      public int duration = 0;

      /** for sorting, compare the freqs */
      public int compareTo (CpuState state) {
         Integer a = new Integer (freq);
         Integer b = new Integer (state.freq);
         return a.compareTo(b);
      }
   }

   /** remove offsets */
   public void restoreStates () {
      updateTimeInStates ();
      mOffsets.clear ();
      saveOffsets ();
   }

   /** update offsets to "reset" state times */
   public List<CpuState> resetStates () {
      updateTimeInStates ();

      // loop through current states and set the offsets
      for (CpuState state : mStates) {
         mOffsets.put (state.freq, state.duration);
      }

      // return states with offsets
      saveOffsets ();
      return getStates ();
   }

   /** loads offset prefs */
   public void loadOffsets () {
      SharedPreferences settings = getSharedPreferences (PREF_NAME, 0);
      String prefs = settings.getString (PREF_OFFSETS, "");

      if (prefs == null || prefs.length() < 1) {
         return;
      }

      // each offset seperated by commas
      String[] offsets = prefs.split (",");
      for (String offset : offsets) {
         // drop in the offsets
         String[] parts = offset.split(" ");
         mOffsets.put (Integer.parseInt(parts[0]), Integer.parseInt(parts[1]) );
      }
   }

   /** saves the offset prefs */
   public void saveOffsets () {
      SharedPreferences settings = getSharedPreferences ("cpuspy", 0);
      SharedPreferences.Editor editor = settings.edit ();

      // add each offset
      String str = "";
      for (Map.Entry<Integer,Integer> entry : mOffsets.entrySet() ) {
         str += entry.getKey() + " " + entry.getValue() + ",";
      }

      // write the pref
      editor.putString ("cpuOffsets", str);
      editor.commit ();
   }

   /** get the kernel string */
   public String updateKernelString () {
      try {
         // create a buffered reader to read in the time-in-states log
         InputStream is = new FileInputStream (VERSION_PATH);
         InputStreamReader ir = new InputStreamReader (is);
         BufferedReader br = new BufferedReader (ir);

         // clear out the array and read in the new state lines
         String line;
         while ( (line = br.readLine ()) != null ) {
            mKernelString = line;
         }

         is.close ();

      } catch (Exception e) {
         Log.e ("cpuspy", e.getMessage() );
         return null;
      }

      // made it
      return mKernelString;
   }

   /** cat out the time_in_state as root as an alternative to reading the
    * file. THis is necesary for ROMs who have blocked user/group read access
    * to the file due to some bug */
   private List<CpuState> getStatesAsRoot () {
      // attempt to cat out the contents of the state file in su
      Process           process = null;
      DataOutputStream  out = null;
      InputStreamReader inReader = null;
      try {
         // run su and get an output stream
         process = Runtime.getRuntime().exec("su");
         out = new DataOutputStream( process.getOutputStream() );
         out.writeChars("cat " + TIME_IN_STATE_PATH + "\nexit\n");
         out.flush();

         // get the output
         inReader = new InputStreamReader (process.getInputStream() );
         BufferedReader br = new BufferedReader (inReader);
         readInStates (br);
      } catch (Exception e) {
         Log.d ("cpuspy", "Tried to read file via root cat but failed");
         Log.e ("cpuspy", e.getMessage() );
      } finally {
         // kill everythong
         try {
            out.close ();
            inReader.close ();
            process.destroy();
         } catch (Exception e) {
            Log.e ("cpuspy", e.getMessage() );
         }
      }

      return mStates;
   }

   /** read from a reader the state lines into mStates */
   private void readInStates (BufferedReader br) {
      try {
         // clear out the array and read in the new state lines
         mStates.clear ();
         String line;
         while ( (line = br.readLine ()) != null ) {
            // split open line and convert to Integers
            String[] nums = line.split (" ");
            mStates.add ( new CpuState  (
               Integer.parseInt (nums[0]), Integer.parseInt (nums[1]) ) );
         }
      } catch (Exception e) {
         Log.e ("cpuspy", e.getMessage() );
      }
   }

   /** get the time-in-states info */
   public List<CpuState> updateTimeInStates () {
      try {
         // create a buffered reader to read in the time-in-states log
         InputStream is = new FileInputStream (TIME_IN_STATE_PATH);
         InputStreamReader ir = new InputStreamReader (is);
         BufferedReader br = new BufferedReader (ir);

         // clear out the array and read in the new state lines
         readInStates (br);

         is.close ();
      } catch (Exception e) {
         Log.e ("cpuspy", e.getMessage() );
         Log.d ("cpuspy", "Could not read file normally, trying root");

         // didn't work, blindly try root
         if (getStatesAsRoot() == null) {
            return null;
         }
      }

      // add in sleep state
      int sleepTime = (int)(SystemClock.elapsedRealtime() - SystemClock.uptimeMillis ()) / 10;
      mStates.add ( new CpuState (0, sleepTime));

      // sort highest to lowest freq
      Collections.sort (mStates, Collections.reverseOrder());

      // made it
      return mStates;
   }

}
