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
import android.widget.Toast;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

import android.content.Context;
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
   private static final String PREF_BOOT_TIME = "boot_time";

   /** offsets used if user reset */
   private Map<Integer, Integer> mOffsets = new HashMap<Integer, Integer>();

   /** array of the states / duration */
   private List<CpuState> mStates = new ArrayList<CpuState>();

   /** kernel build string */
   private String mKernelString = "";

   void toast(final String text) {
      Log.d("cpuspy", text);
      Context context= getApplicationContext();
      Toast.makeText(context, text, Toast.LENGTH_LONG).show();
   }

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
      toast("Offsets cleared.");
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
      toast("Offsets saved.");
      return getStates ();
   }

   /** @return Absolute time at last boot, in milliseconds since the
       beginning of the epoch.  If the system time is modified, for
       example using "setCurrentTimeMillis", the result will change in
       the same way.

       CAVEAT: Depending on timing and on the implementation of
       System.currentTimeMillis() and SystemClock.elapsedRealtime(),
       the return value of "boot_time_millis" should be expected to
       slightly vary. In other words, subsequent calls to this function will
       probably not all yield the same result.  This should be taken
       into account when comparing values.

       FIXME: Is there a more direct and stable method of determining
       the absolute time at the last boot?  For example, is there a
       boot log with a time stamp?

       Note that milliseconds are not an appropriate unit here, since
       accuracy of boot time is going to be on the order of seconds.
       Seconds would be more appropriate. However, just reducing the
       accuracy to seconds will still not eliminate the variation.

       Note that the return type of "currentTimeMillis" and
       "elapsedRealTime" is "long", so their values can be expected to
       never wrap.
   */
   static long boot_time_millis() {
      return (System.currentTimeMillis() - SystemClock.elapsedRealtime());
   }

   /** Loads offset prefs.

       If the system was rebooted after last writing offsets, the
       offsets are cleared.
   */
   public void loadOffsets () {
      SharedPreferences settings = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
      String prefs = settings.getString (PREF_OFFSETS, "");
      if (prefs == null || prefs.length() < 1) {
         toast("No saved offsets found.");
         return;
      }

      final String boot_time_s = settings.getString (PREF_BOOT_TIME, "");
      if (null == boot_time_s || boot_time_s.length() < 1) {
         toast("Saved offsets apply to an unknown boot time, clearing them.");
         restoreStates();
         return;
      }
      final long boot_time_from_file= Long.parseLong(boot_time_s);
      final long min_boot_cycle_time= 30*1000; // msec
      // Differences less than the assumed boot cycle time
      // are assumed to be insignificant variation and ignored.
      if (boot_time_from_file + min_boot_cycle_time < boot_time_millis()) {
         toast("Saved offsets apply to a previous boot, clearing them.");
         restoreStates();
         return;
      }

      toast("Applying saved offsets...");
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
      SharedPreferences settings = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
      SharedPreferences.Editor editor = settings.edit ();

      // add each offset
      String str = "";
      for (Map.Entry<Integer,Integer> entry : mOffsets.entrySet() ) {
         str += entry.getKey() + " " + entry.getValue() + ",";
      }

      // write the pref
      /* We store the boot time instead of the current time because
         the boot time is less likely to slip past the next boot time
         by time adjustments than the current time is.

         The next boot time may be just a few seconds ahead. Because
         shutdown takes just a few seconds, and it may also take only
         a few seconds until elapsedRealTime begins to advance during
         a reboot, time adjustments in the range of seconds can foil a
         comparison of boot time with current time. In contrast, boot
         times are usually at least minutes apart. */
      editor.putString(PREF_BOOT_TIME, "" + boot_time_millis());
      editor.putString(PREF_OFFSETS, str);
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
            //Log.e ("cpuspy", e.getMessage() );
         }
      }

      // hacky
      if (mStates.size() == 0)
         return null;

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
