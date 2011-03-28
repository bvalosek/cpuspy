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
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ProgressBar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import java.util.List;
import java.util.ArrayList;

/** main activity class */
public class HomeActivity extends Activity
{
   /** talk to the app */
   private CpuSpyApp mApp = null;

   /** where we dump the states views */
   private LinearLayout mUiStatesView = null;

   /** additional states text */
   private TextView mUiAdditionalStates = null;

   /** additional states haeder */
   private TextView mUiHeaderAdditionalStates = null;

   /** warning for no states */
   private TextView mUiStatesWarning = null;

   /** kernel string */
   private TextView mUiKernelString = null; 

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
      mUiKernelString = (TextView)findViewById (R.id.ui_kernel_string);
      mUiAdditionalStates = (TextView)findViewById (R.id.ui_additional_states);
      mUiHeaderAdditionalStates = (TextView)findViewById (
         R.id.ui_header_additional_states);
      mUiStatesWarning = (TextView)findViewById (R.id.ui_states_warning);

      // update title
      setTitle(getResources().getText(R.string.app_name) + " v" + 
         getResources().getText(R.string.version_name) );

      // draw all the info
      updateData ();
   }

   /** resume */
   @Override public void onResume () {
      super.onResume ();
      updateView ();
   }

   /** update the view */
   public void updateView () {
      /* fill up the layout with views for the states, recording which ones
       * are empty */
      mUiStatesView.removeAllViews();
      List<String> extraStates = new ArrayList<String> ();
      for (CpuState state : mApp.getStates () ) {
         if (state.duration > 0) {
            generateStateRow (state, mUiStatesView);
         } else {
            extraStates.add (state.freq/1000 + " MHz");
         }
      }

      // no states?!
      if ( mApp.getStates().size() == 0) {
      	mUiStatesWarning.setVisibility (View.VISIBLE);
      }

      // for all empty views, edit the additional line textview
      if (extraStates.size() > 0) {
         int n = 0;
         String str = ""; 
         // construct nice looking comma-seperated list
         for (String s : extraStates) {
            if (n++ > 0)
               str += ", ";
            str += s;
         }
         mUiAdditionalStates.setVisibility(View.VISIBLE);
         mUiHeaderAdditionalStates.setVisibility (View.VISIBLE);
         mUiAdditionalStates.setText (str);
      }


      // kernel line
      mUiKernelString.setText ( mApp.getKernelString () );
   }

   /** update the data */
   public void updateData () {
      // get the time spent in states
      mApp.updateTimeInStates ();
      
      // from /proc/version
      mApp.updateKernelString ();
   }

   /** spit out a view representing a cpustate so we can cram it into a ScrollView */
   private View generateStateRow (CpuState state, ViewGroup parent) {
      // inflate the XML into a view in the parent
      LayoutInflater inf = LayoutInflater.from ( (Context)mApp );
      LinearLayout theRow = (LinearLayout)inf.inflate(
         R.layout.state_row, parent, false);

      // what percetnage we've got
      float per = (float)state.duration * 100 / mApp.getTotalStateTime (); 

      // pretty strings
      String sFreq = state.freq / 1000 + " MHz";
      String sPer = (int)per + "%";
      int h = (int)Math.floor (state.duration / 6000);
      int m = (state.duration / 100) % 60;
      String sDur;
      if (m < 10)
      	sDur = h + ":0" + m;
      else
      	sDur = h + ":" + m;
   

      // map UI elements to objects
      TextView freqText = (TextView)theRow.findViewById(R.id.ui_freq_text);
      TextView durText = (TextView)theRow.findViewById(R.id.ui_duration_text);
      TextView perText = (TextView)theRow.findViewById(R.id.ui_percentage_text);
      ProgressBar bar = (ProgressBar)theRow.findViewById(R.id.ui_bar);

      // modify the row
      freqText.setText (sFreq);
      perText.setText (sPer);
      durText.setText (sDur);
      bar.setProgress ( (int)per);

      // add it to parent and return
      parent.addView(theRow);
   	return theRow;
   }


   /** called when we want to infalte the menu */
   @Override public boolean onCreateOptionsMenu (Menu menu) {
      // request inflater from activity and inflate into its menu
      MenuInflater inflater = getMenuInflater ();
      inflater.inflate (R.menu.home_menu, menu);

      // made it
      return true;
   }

   /** called to handle a menu event */
   @Override public boolean onOptionsItemSelected (MenuItem item) {
      // what it do mayne
      switch (item.getItemId () ) {
      /* pressed the load menu button */
      case R.id.menu_refresh:
         updateData ();
         updateView ();
         break;
      case R.id.menu_reset:
         mApp.resetStates();
         updateData ();
         updateView();
      	break;
      }

      // made it
      return true;
   }


}
