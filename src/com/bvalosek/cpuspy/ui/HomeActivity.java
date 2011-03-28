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
import android.widget.ListView;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ScrollView;
import android.widget.ProgressBar;

/** main activity class */
public class HomeActivity extends Activity
{
   /** talk to the app */
   private CpuSpyApp mApp = null;

   /** view of the list of states */
   ListView mUiStateList = null;

   /** where we dump the states views */
   LinearLayout mUiStatesView = null;

   /** Called when the activity is first created. */
   @Override public void onCreate(Bundle savedInstanceState)
   {
      super.onCreate(savedInstanceState);

      // inflate the view
      setContentView(R.layout.home_layout);

      // get app context
      mApp = (CpuSpyApp)getApplicationContext ();

      // find views
      mUiStatesView = (LinearLayout)findViewById (R.id.ui_states_view);

      // draw all the info
      updateView ();
   }

   /** update the view */
   public void updateView () {

      // FILL IT UP
      mApp.updateTimeInStates ();
      for (CpuState state : mApp.getStates () ) {
         generateStateRow (state, mUiStatesView);
      }

   }

   /** spit out a view representing a cpustate so we can cram it into a ScrollView */
   private View generateStateRow (CpuState state, ViewGroup parent) {
      // inflate the XML into a view in the parent
      LayoutInflater inf = LayoutInflater.from ( (Context)mApp );
      LinearLayout theRow = (LinearLayout)inf.inflate(
         R.layout.state_row, parent, false);

      // what percetnage we've got
      float per = (float)state.duration * 100 / mApp.getTotalStateTime (); 

      // map UI elements to objects
      TextView freqText = (TextView)theRow.findViewById(R.id.ui_freq_text);
      TextView durText = (TextView)theRow.findViewById(R.id.ui_duration_text);
      TextView perText = (TextView)theRow.findViewById(R.id.ui_percentage_text);
      ProgressBar bar = (ProgressBar)theRow.findViewById(R.id.ui_bar);

      // modify the row
      freqText.setText (Integer.toString (state.freq));
      perText.setText (Float.toString (per) );
      durText.setText (Integer.toString(state.duration));
      bar.setProgress ( (int)per);

      // add it to parent and return
      parent.addView(theRow);
   	return theRow;
   }

}
