/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package paintbynumberpro;

import java.awt.*;
import javax.swing.*;

/**
 *
 * @author Lynne
 */
public class PaintByNumberPro {

    // Primary objects this program needs
    // (1) A puzzle
    // (2) A main window (Open Puzzle, Enter Puzzle, Create Puzzle)
    // (3) A set of program settings
    private static OpeningJFrame openingFrame = null;   // (2)
//    private static PBNPuzzle myPuzzle = null;           // (1) (DrawHandler should have link to the puzzle)
    private static Settings mySettings = null;          // (3)

    // After a puzzle is open, then this program needs
    // (1) An interactive puzzle window
    // (2) A primary puzzle control window (Save, Print, Fix Clues, hints, solve)
    // (3) A puzzle drawing controls window (Choose font, font size, etc.)
    // (4) A puzzle solving control window (Start a new guess, commit guesses, etc.)
    // (5) A puzzle solution viewing window
    // (6) A handler for linking the puzzle to the PBNFrames
//    private static PBNFrame myPBNFrame = null;          // (4) (DrawHandler should keep these itself)
//    private static PBNFrame myPBNSolutionFrame = null;  // (5) (DrawHandler should keep these itself)
    private static PBNHandler myHandler = null; // (6)

    // Current version
//    private static String versionStr = "0.1alpha (12/30/11)";
    private static String versionStr = "1.0 Dec 2012";

    // Some quantities used by PBNDrawPanel
    private static int viewport_width;
	private static int viewport_height;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        // Create the primary window
        // Do this on the Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater (new Runnable()
        {
            public void run() { StartProgram(); }
        });

    }

    private static void StartProgram ()
    {
        // Get the preferences
        mySettings = new Settings();
        mySettings.ReadSettings();

        // Open the main JFrame
        openingFrame = new OpeningJFrame (versionStr);
        WindowUtilities.CenterFrame (openingFrame);
        openingFrame.setVisible(true);
        openingFrame.toFront();

        SetViewportDimensions ();
    }

    private static void SetViewportDimensions ()
    {
        assert (openingFrame != null);
		Toolkit theKit = openingFrame.getToolkit();
		Dimension wndSize = theKit.getScreenSize();
		viewport_width = wndSize.width*7/8/2;
		viewport_height = wndSize.height*7/8/2;
    }

    public static Dimension GetViewportDimensions ()
    {
        if (viewport_width == 0 || viewport_height == 0)
            SetViewportDimensions();
        return new Dimension (viewport_width, viewport_height);
    }

    public static OpeningJFrame GetOpeningFrame ()
    { return openingFrame; }

    public static void CloseApplication (boolean do_ask)
    {
        int answer = JOptionPane.YES_OPTION;
        if (do_ask)
        {
            answer = JOptionPane.showConfirmDialog (null,
                "Are you sure you want to quit?",
                "Application Quitting", JOptionPane.YES_NO_CANCEL_OPTION);
        }
        if (answer == JOptionPane.YES_OPTION) 
        {
            mySettings.SaveSettings();
            System.exit(0);
        }
    }

    public static PBNFrame GetPBNFrame ()
    {
        if (myHandler != null) return myHandler.GetTheFrame ();
        else return null;
    }

    public static PBNHandler GetDrawHandler ()
    { return myHandler; }

    public static void OpenAPuzzle ()
    {
        PuzzleStaticUtilities.OpenAPuzzle();
        // OpenAPuzzle() calls SetNewPuzzle when finished (if successful)
    }

    public static void EnterANewPuzzle ()
    {
        PuzzleStaticUtilities.CreateNewEmptyPuzzle();
        // CreateNewEmptyPuzzle() calls SetNewPuzzle when finished (if successful)
    }

    public static void CloseThePuzzle ()
    {
        if (myHandler == null) return;
        PBNPuzzle myPuzzle = myHandler.GetThePuzzle();
        if (myPuzzle != null)
        {
            int answer = JOptionPane.showConfirmDialog (null,
                    "Do you want to save the puzzle first?",
                    "Application Quitting", JOptionPane.YES_NO_CANCEL_OPTION);
            if (answer == JOptionPane.YES_OPTION) 
                PuzzleStaticUtilities.WritePuzzleToFile (myPuzzle, myPuzzle.GetFile(), false);
            if (answer == JOptionPane.CANCEL_OPTION) return;

            // Tell the handler to close itself
            myHandler.ClosePuzzle();
            myHandler = null;

            // Bring the mainFrame back up
            assert (openingFrame != null);
            openingFrame.setVisible(true);
        }
    }

    public static void SetNewPuzzle (PBNPuzzle thePuzzle)
    {
        // Create a new Handler with this puzzle!
        myHandler = new PBNHandler (thePuzzle, PBNHandler.Mode.NORMAL);

        // Save the puzzle's directory in the settings
//        File theDir = myPuzzle.GetFile();
//        mySettings.SetSettingFor (Settings.SettingKeyword.PUZZLE_DIR, theDir.getParent());

        // Close the mainFrame
        openingFrame.setVisible(false);
    }

    public static Settings GetSettings ()
    { return mySettings; }

    public static void HandleErrorMessage(String title, String msg)
    {
        PBNFrame myFrame = myHandler.GetTheFrame();
        HandleErrorMessage (title, msg, myFrame);
    }

    public static void HandleErrorMessage (String title, String msg,
            javax.swing.JFrame myFrame)
    {
        JOptionPane.showMessageDialog (myFrame,
            msg, title, JOptionPane.ERROR_MESSAGE);
    }

    public static void HandleMessageForSolver (String title, String msg)
    {
        int answer = JOptionPane.YES_OPTION;
        answer = JOptionPane.showConfirmDialog (null,
            msg + "\nKeep going?",
            title, JOptionPane.YES_NO_OPTION);
        if (answer == JOptionPane.NO_OPTION && myHandler != null)
            myHandler.TellSolverToStop();

    }

    public static void HandleMessage(String title, String msg)
    {
        PBNFrame myFrame = myHandler.GetTheFrame();
        JOptionPane.showMessageDialog (myFrame,
            msg, title, JOptionPane.INFORMATION_MESSAGE);
    }

}
