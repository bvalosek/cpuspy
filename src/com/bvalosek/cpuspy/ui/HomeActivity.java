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
import android.widget.ListView;
import android.widget.BaseAdapter;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ScrollView;

/** main activity class */
public class HomeActivity extends Activity
{
   /** talk to the app */
   private CpuSpyApp mApp = null;

   /** view of the list of states */
   ListView mUiStateList = null;

   /** scroll window where everything is put */
   ScrollView mUiMainScroll = null;

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

      // fetch current states
      mApp.updateTimeInStates ();

      // find views
      //mUiStateList = (ListView)findViewById (R.id.ui_states_list);
      mUiMainScroll = (ScrollView)findViewById (R.id.ui_main_scroll);
      mUiStatesView = (LinearLayout)findViewById (R.id.ui_states_view);

      // FILL IT UP
      for (CpuState state : mApp.getStates () ) {
         generateStateRow (state, mUiStatesView);
      }

      // attach list adapter
      //mUiStateList.setAdapter( new StatesListAdapter( (Context)mApp) );
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

      // modify the row
      freqText.setText (Integer.toString (state.freq));
      perText.setText (Float.toString (per) );
      durText.setText (Integer.toString(state.duration));

      // add it to parent and return
      parent.addView(theRow);
   	return theRow;
   }

   /** adapter to connect list of states to the listview */
   private class StatesListAdapter extends BaseAdapter {

      // keep track of our layout inflater
      private LayoutInflater mInflater;

      /** ctor when given a context to create our ladder infaltor in */
      public StatesListAdapter (Context c) {
      	//m ake the inflator
      	mInflater = LayoutInflater.from(c);
      }

      /** number of elements */
      public int getCount() {
      	return mApp.getStates().size();
      }

      /** get from a position */
      public Object getItem(int pos) {
      	return mApp.getStates().get(pos);
      }

      /** get ID for a position */
      public long getItemId(int position) {
      	return position;
      }

      /** actually redner the view */
      public View getView(int pos, View convertView, ViewGroup parent) {
         LinearLayout theRow = null;

         // create the view of we need to for this row
         if (convertView == null) {
         	theRow = (LinearLayout)mInflater.inflate(
         	      R.layout.state_row, parent, false);
         } else {
            theRow = (LinearLayout)convertView;         
         }

         // the convo piece
         CpuState state = mApp.getStates().get(pos);

         // what percetnage we've got
         float per = (float)state.duration * 100 / mApp.getTotalStateTime (); 

         // map UI elements to objects
         TextView freqText = (TextView)theRow.findViewById(R.id.ui_freq_text);
         TextView durText = (TextView)theRow.findViewById(R.id.ui_duration_text);
         TextView perText = (TextView)theRow.findViewById(R.id.ui_percentage_text);

         // modify the row
         freqText.setText (Integer.toString (state.freq));
         perText.setText (Float.toString (per) );
         durText.setText (Integer.toString(state.duration));

         // return the finished view
         return theRow; 
      }

   }
}
