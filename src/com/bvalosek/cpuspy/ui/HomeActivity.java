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
import com.bvalosek.cpuspy.CpuSpyApp.CpuState;

// imports
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import java.util.List;

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
      List<CpuState> states = mApp.getTimeInStates ();

      for (CpuState state : states) {
         Log.d ("cpuspy", "freq=" + state.freq + " dur=" + state.duration );
      }
   }
}
