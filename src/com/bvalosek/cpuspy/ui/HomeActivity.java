//-----------------------------------------------------------------------------
// cpuspy
//
// (c) Brandon Valosek, 2011 <bvalosek@gmail.com>
//
// HomeActivity.java
//
// Main UI
//-----------------------------------------------------------------------------

package com.bvalosek.cpuspy.ui;

// my stuff
import com.bvalosek.cpuspy.*;

// imports
import android.app.Activity;
import android.os.Bundle;
import java.util.Map;
import android.util.Log;

/** main activity class */
public class HomeActivity extends Activity
{
   /** talk to the app */
   private CpuSpyApp mApp = null;

   /** Called when the activity is first created. */
   @Override public void onCreate(Bundle savedInstanceState)
   {
      super.onCreate(savedInstanceState);

      // inflate the view
      setContentView(R.layout.main);

      // get app context
      mApp = (CpuSpyApp)getApplicationContext ();

      // get proc
      Map<Integer,Integer> states = mApp.getTimeInStates ();

      for (Map.Entry<Integer,Integer> entry : states.entrySet() ) {
         int freq = entry.getKey();
         int time = entry.getValue ();

         Log.d ("cpuspy", "freq=" + freq + " time=" + time);
      }

   }
}
