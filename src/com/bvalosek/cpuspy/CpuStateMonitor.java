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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import android.os.SystemClock;
import android.util.SparseArray;

/**
 * CpuStateMonitor is a class responsible for querying the system and getting
 * the time-in-state information, as well as allowing the user to set/reset
 * offsets to "restart" the state timers
 */
public class CpuStateMonitor {

    public static final String TIME_IN_STATE_PATH =
        "/sys/devices/system/cpu/cpu0/cpufreq/stats/time_in_state";

    //private static final String TAG = "CpuStateMonitor";

    private List<CpuState>      _states = new ArrayList<CpuState>();
    private SparseArray<Long>  _offsets = new SparseArray<Long>();

    /** exception class */
    public class CpuStateMonitorException extends Exception {

		private static final long serialVersionUID = 2724261744757837755L;

		public CpuStateMonitorException(String s) {
            super(s);
        }
    }

    /**
     * simple struct for states/time
     */
    public class CpuState implements Comparable<CpuState> {
        /** init with freq and duration */
        public CpuState(int a, long b) { freq = a; duration = b; }

        public int freq = 0;
        public long duration = 0;

        /** for sorting, compare the freqs */
        public int compareTo(CpuState state) {
            Integer a = Integer.valueOf(freq);
            Integer b = Integer.valueOf(state.freq);
            return a.compareTo(b);
        }
    }

    /** @return List of CpuState with the offsets applied */
    public List<CpuState> getStates() {
        List<CpuState> states = new ArrayList<CpuState>();

        /* check for an existing offset, and if it's not too big, subtract it
         * from the duration, otherwise just add it to the return List */
        for (CpuState state : _states) {
            long duration = state.duration;
            
            final Long value = _offsets.get(state.freq);
            
            if (value != null) {
                long offset = value.longValue();
                if (offset <= duration) {
                    duration -= offset;
                } else {
                    /* offset > duration implies our offsets are now invalid,
                     * so clear and recall this function */
                    _offsets.clear();
                    return getStates();
                }
            }

            states.add(new CpuState(state.freq, duration));
        }

        return states;
    }

    /**
     * @return Sum of all state durations including deep sleep, accounting
     * for offsets
     */
    public long getTotalStateTime() {
        long sum = 0;
        long offset = 0;

        for (CpuState state : _states) {
            sum += state.duration;
        }

        for(int i = 0; i < _offsets.size(); i++) {
        	   offset += _offsets.valueAt(i).longValue();
        	}

        return sum - offset;
    }

    /**
     * @return Map of freq->duration of all the offsets
     */
    public SparseArray<Long> getOffsets() {
        return _offsets;
    }

    /** Sets the offset map (freq->duration offset) */
    public void setOffsets(SparseArray<Long> offsets) {
        _offsets = offsets;
    }

    /**
     * Updates the current time in states and then sets the offset map to the
     * current duration, effectively "zeroing out" the timers
     */
    public void setOffsets() throws CpuStateMonitorException {
        _offsets.clear();
        updateStates();

        for (CpuState state : _states) {
            _offsets.put(state.freq, Long.valueOf(state.duration));
        }
    }

    /** removes state offsets */
    public void removeOffsets() {
        _offsets.clear();
    }

    /**
     * @return a list of all the CPU frequency states, which contains
     * both a frequency and a duration (time spent in that state
     */
    public List<CpuState> updateStates()
        throws CpuStateMonitorException {
        /* attempt to create a buffered reader to the time in state
         * file and read in the states to the class */
        try {
            InputStream is = new FileInputStream(TIME_IN_STATE_PATH);
            InputStreamReader ir = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(ir);
            _states.clear();
            readInStates(br);
            is.close();
        } catch (IOException e) {
            throw new CpuStateMonitorException(
                    "Problem opening time-in-states file");
        }

        /* deep sleep time determined by difference between elapsed
         * (total) boot time and the system uptime (awake) */
        long sleepTime = (SystemClock.elapsedRealtime()
                - SystemClock.uptimeMillis()) / 10;
        _states.add(new CpuState(0, sleepTime));

        Collections.sort(_states, Collections.reverseOrder());

        return _states;
    }

    /** read from a provided BufferedReader the state lines into the
     * States member field
     */
    private void readInStates(BufferedReader br)
        throws CpuStateMonitorException {
        try {
            String line;
            while ((line = br.readLine()) != null) {
                // split open line and convert to Integers
                final String[] nums = line.split(" ");
                _states.add(new CpuState(
                        Integer.parseInt(nums[0]),
                        Long.parseLong(nums[1])));
            }
        } catch (IOException e) {
            throw new CpuStateMonitorException(
                    "Problem processing time-in-states file");
        }
    }
}
