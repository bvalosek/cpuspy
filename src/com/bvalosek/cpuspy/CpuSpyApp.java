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

/** main application class */
public class CpuSpyApp extends Application {

   public static final String TIME_IN_STATE_PATH =
      "/sys/devices/system/cpu/cpu0/cpufreq/stats/time_in_state";

   /** simple struct for states/time */
   public class CpuState {
      public CpuState(int a, int b) { freq = a; duration =b; }
      public int freq = 0;
      public int duration = 0;
   }
   
   /** get the time-in-states info */
   public List<CpuState> getTimeInStates () {
      List<CpuState> states = new ArrayList<CpuState>();

      try {
         // create a buffered reader to read in the time-in-states log
         InputStream is = new FileInputStream (TIME_IN_STATE_PATH);
         InputStreamReader ir = new InputStreamReader (is);
         BufferedReader br = new BufferedReader (ir);

         // read in the lines
         String line;
         while ( (line = br.readLine ()) != null ) {
            // split open line and convert to Integers
            String[] nums = line.split (" ");
            states.add ( new CpuState  (
               Integer.parseInt (nums[0]), Integer.parseInt (nums[1]) ) );
         }

         is.close ();

      } catch (Exception e) {
         Log.e ("cpuspy", e.getMessage() );
         return null;
      }

      // made it
      return states;
   }

}
