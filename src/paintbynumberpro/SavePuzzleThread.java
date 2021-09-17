/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package paintbynumberpro;

import java.util.*;
import java.io.*;
import javax.swing.JOptionPane;

/**
 *
 * @author Lynne
 */
public class SavePuzzleThread extends Thread {

    private boolean auto_save_on = false;
    private long auto_save_interval;         // in milliseconds
    private boolean save_now = false;
    private Date last_saved = Calendar.getInstance().getTime();
    private File to_file = null;
    boolean do_stop = false;

    private PBNHandler myDrawHandler = null;

    SavePuzzleThread (PBNHandler theDrawHandler)
    {
        myDrawHandler = theDrawHandler;
    }

    public void SetAutoSaveOnOff (boolean state, int interval_min)
    {
        SetAutoSaveOnOff (state);
        SetAutoSaveInterval (interval_min);
    }

    public void SetAutoSaveOnOff (boolean state)
    {
        auto_save_on = state;
    }

    public void SetAutoSaveInterval (int interval_min)
    {
        auto_save_interval = interval_min*60*1000;
    }

    public void SaveNow ()
    {
        System.out.println ("SavePuzzleThread().SaveNow() called");
        to_file = null;
        save_now = true;
    }

    public void SaveNow (File toFile)
    {
        System.out.println ("SavePuzzleThread().SaveNow() to file: " + toFile.getName());
        save_now = true;
        to_file = toFile;
    }

    public void SetStop ()
    {
        System.out.println ("SavePuzzleThread().SetStop() called");
        do_stop = true;
    }

    public void run ()
    {
        System.out.println ("SavePuzzleThread()s has been started.");
        while (!do_stop)
        {
//            System.out.println ("   SavePuzzleThread() running myDrawHandler mode: " + myDrawHandler.GetTheMode());
//            System.out.println ("      auto_save_on: " + auto_save_on + " save_now: " + save_now);
            // Only do something if the mode is NORMAL
			// 9/16/2021 - Saving is allowed for all modes NORMAL or AUTO-SOLVING
//            if (myDrawHandler.GetTheMode() == PBNHandler.Mode.NORMAL)
//            {
                // Let's see if it's time to auto-save
                if (auto_save_on)
                {
                    Date now = Calendar.getInstance().getTime();
                    long elapsed_time = now.getTime() - last_saved.getTime();
                    if (elapsed_time >= auto_save_interval) save_now = true;
                }

                // Save the puzzle now
                if (save_now && myDrawHandler != null)
                {
                    PBNPuzzle thePuzzle = myDrawHandler.GetThePuzzle();
                    if (thePuzzle != null)
                    {
                        File myFile = to_file;
                        if (myFile == null) myFile = thePuzzle.GetFile();
                        if (myFile != null)
                        {						
                            if (PuzzleStaticUtilities.WritePuzzleToFile(thePuzzle, myFile))
                            {									
                                myDrawHandler.PuzzleSavedSuccessfullyToFile (myFile);
                                if (!auto_save_on)
                                    JOptionPane.showMessageDialog (null,
                                            "Puzzle saved to file: " + myFile.getName(),
                                            "Puzzle Saved!",
                                            JOptionPane.INFORMATION_MESSAGE, null);
                            } else
                            {									
                                JOptionPane.showMessageDialog (null,
                                        "Error saving puzzle to file: " + myFile.getName(),
                                        "Puzzle NOT Saved",
                                        JOptionPane.INFORMATION_MESSAGE, null);
                            }
                            last_saved = Calendar.getInstance().getTime();
                        }
                        save_now = false;
                    }
                }

                // Update the GUI
                if (myDrawHandler != null)
                {
                    Date now = Calendar.getInstance().getTime();
                    long elapsed_time_sec = now.getTime() - last_saved.getTime();
                    elapsed_time_sec /= 1000;
                    myDrawHandler.UpdateLastSaved((int)elapsed_time_sec);
                    if (to_file != null)          
                        myDrawHandler.SetFile(to_file);
                }

                // Sleep for 1/10th of a second
                try { Thread.sleep(100); }
                catch (InterruptedException ie) {}
//            }
        }
    }

}
