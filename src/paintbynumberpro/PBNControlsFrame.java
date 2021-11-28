/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * PBNFrame.java
 *
 * Created on Jan 1, 2012, 7:03:18 PM
 */

package paintbynumberpro;

import java.awt.event.*;

/**
 *
 * @author user
 */
public class PBNControlsFrame extends javax.swing.JFrame implements WindowListener {

    private PBNHandler myDrawHandler;
    private final int DEFAULT_AUTO_SAVE_INTERVAL = 10;

    /** Creates new form PBNFrame */
    public PBNControlsFrame(PBNHandler theHandler) {
        myDrawHandler = theHandler;
        initComponents();
        InitializeBasicsFromDrawHandler ();
        InitializeFromPuzzle();

        // Set window listener
        this.addWindowListener (this);
    }
    
    private void SetDrawControlsEnabled (boolean state)
    {
        BoxSizeUp_JButton.setEnabled (state);
        BoxSizeDown_JButton.setEnabled (state);
        FontSizeUp_JButton.setEnabled (state);
        FontSizeDown_JButton.setEnabled (state);
        ClueWidthUp_JButton.setEnabled (state);
        ClueWidthDown_JButton.setEnabled (state);
        ClueHeightUp_JButton.setEnabled (state);
        ClueHeightDown_JButton.setEnabled (state);
        Refresh_JButton.setEnabled (state);
    }

    private void InitializeBasicsFromDrawHandler ()
    {
        if (myDrawHandler == null) return;
        UpdateBoxSizeLabel ();
        UpdateFontSizeLabel ();
        UpdateClueWidthLabel ();
        UpdateClueHeightLabel ();
        SetDrawControlsEnabled (true);
    }

    public boolean GetAutoMarkStart ()
    { return AutoMarkStart_JCheckBox.isSelected(); }

    public boolean GetAssumeGuessWrong ()
    { return AssumeGuessWrong_JCheckBox.isSelected(); }
	
	public boolean GetDebugSelected()
	{ return Debug_JCheckBox.isSelected(); }

    public boolean GetAutoSaveOnOff ()
    { return AutoSave_JCheckBox.isSelected(); }

    public int GetAutoSaveInterval ()
    {
        String label = (String)AutoSave_JComboBox.getSelectedItem();
        int min = DEFAULT_AUTO_SAVE_INTERVAL;
        if (label != null)
        {
            try
            { min = Integer.parseInt (label); }
            catch (NumberFormatException nfe) {}
        }
        return min;
    }

    public void UpdateLastSaved (int secs_ago)
    {
        int min = secs_ago/60;
        int sec = secs_ago - min*60;
        String time = Integer.toString(min) + ":";
        if (sec < 10) time += "0";
        time += Integer.toString(sec);
        LastSave_JLabel.setText (time);
    }

    public void UpdateGuessLevel (int level)
    {
        String item = (String)myDrawHandler.GetComboBoxModel().getElementAt (level);
        GuessLevel_JComboBox.setSelectedItem (item);
        GuessLevel_JLabel.setForeground (myDrawHandler.GetGuessColor(level));
    }

    private void UpdateBoxSizeLabel ()
    {
        int box_size = myDrawHandler.GetBoxSize();
        BoxSize_JLabel.setText (Integer.toString (box_size));
    }

    private void UpdateFontSizeLabel ()
    {
        int font_size = myDrawHandler.GetFontSize();
        FontSize_JLabel.setText (Integer.toString (font_size));
    }

    private void UpdateClueWidthLabel ()
    {
        int clue_width = myDrawHandler.GetClueWidth();
        ClueWidth_JLabel.setText (Integer.toString (clue_width));
    }

    private void UpdateClueHeightLabel ()
    {
        int clue_height = myDrawHandler.GetClueHeight();
        ClueHeight_JLabel.setText (Integer.toString (clue_height));
    }

    public void ModeChanged ()
    {
        InitializeFromPuzzle ();
    }

    private void InitializeFromPuzzle ()
    {
        PBNPuzzle thePuzzle = myDrawHandler.GetThePuzzle();
        boolean puzzle_is_valid = thePuzzle != null;
        boolean mode_is_normal = (myDrawHandler.GetTheMode() == PBNHandler.Mode.NORMAL);

        SetPuzzleItemsState (puzzle_is_valid && mode_is_normal);

        if (thePuzzle == null)
            PuzzleSolvedStatus_JLabel.setText ("Puzzle is NOT SET yet");
        else
        {
            if (thePuzzle.IsSolved())
                PuzzleSolvedStatus_JLabel.setText ("Puzzle is SOLVED");
            else
                PuzzleSolvedStatus_JLabel.setText ("Puzzle is NOT solved");
        }
        GuessLevel_JComboBox.setEnabled(puzzle_is_valid && mode_is_normal);
        StartNewGuess_JButton.setEnabled(puzzle_is_valid && mode_is_normal);
        AutoMarkStart_JCheckBox.setEnabled(puzzle_is_valid && mode_is_normal);
        AssumeGuessWrong_JCheckBox.setEnabled(puzzle_is_valid && mode_is_normal);
        UndoLastGuess_JButton.setEnabled(puzzle_is_valid && mode_is_normal);
        CommitGuesses_JButton.setEnabled(puzzle_is_valid && mode_is_normal);
        ClearPuzzle_JButton.setEnabled(puzzle_is_valid && mode_is_normal);
        RemoveMarks_JButton.setEnabled(puzzle_is_valid && mode_is_normal);
        Undo_JButton.setEnabled(puzzle_is_valid && mode_is_normal);
        GiveMeAClue_JButton.setEnabled(puzzle_is_valid && mode_is_normal);
        if (puzzle_is_valid && mode_is_normal) UpdateGuessLevel (thePuzzle.GetGuessLevel());
        if (AutoMarkStart_JCheckBox.isSelected() && puzzle_is_valid && mode_is_normal)
            thePuzzle.SetAutoMarkStartFromControls(AutoMarkStart_JCheckBox.isSelected());
        Refresh_JButton.setEnabled(puzzle_is_valid && mode_is_normal);
        
    }

    public void SetUndoItemState (boolean state)
    {
        if (state)
            Undo_JButton.setEnabled(state &&
                    myDrawHandler.GetTheMode() != PBNHandler.Mode.AUTO_SOLVE);
        else
            Undo_JButton.setEnabled(false);
    }

    public void SetPuzzleItemsState (boolean puzzle_is_active)
    {
        if (puzzle_is_active)
        {
            PBNPuzzle myPuzzle = myDrawHandler.GetThePuzzle();
            if (myPuzzle != null && myPuzzle.GetFile() != null)
                Save_JButton.setEnabled(puzzle_is_active);
            else
                Save_JButton.setEnabled(false);
        } else Save_JButton.setEnabled(puzzle_is_active);
        SaveAs_JButton.setEnabled(puzzle_is_active);
        Print_JButton.setEnabled(puzzle_is_active);
        FixPuzzleClues_JButton.setEnabled(puzzle_is_active);
        Redraw_JButton.setEnabled(puzzle_is_active);
        CheckSolution_JButton.setEnabled(puzzle_is_active);
        CheckForErrors_JButton.setEnabled(puzzle_is_active);
        SolvePuzzle_JButton.setEnabled(puzzle_is_active);
        Debug_JCheckBox.setEnabled(puzzle_is_active);
		autoStop_JCheckBox.setEnabled(puzzle_is_active);
        AutoSave_JCheckBox.setEnabled(puzzle_is_active);
        AutoSave_JComboBox.setEnabled(puzzle_is_active);
        GiveMeAClue_JButton.setEnabled(puzzle_is_active);
        Refresh_JButton.setEnabled(puzzle_is_active);
    }

    public void SetStopButton (boolean enabled)
    {
        Stop_JButton.setEnabled(enabled);
    }

    // Window Listener methods
    public void windowClosing (WindowEvent we)
    {
        if (myDrawHandler.GetTheMode() != PBNHandler.Mode.AUTO_SOLVE)
            PaintByNumberPro.CloseThePuzzle();
        else
            PaintByNumberPro.HandleMessage("Auto-Solver Still Running", "You must stop the auto-solver before quitting!");
    }

    public void windowDeactivated (WindowEvent we) { }
    public void windowActivated (WindowEvent we) { }
    public void windowDeiconified (WindowEvent we) { }
    public void windowIconified (WindowEvent we) { }
    public void windowClosed (WindowEvent we) { }
    public void windowOpened (WindowEvent we) { }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel6 = new javax.swing.JPanel();
        jPanel1 = new javax.swing.JPanel();
        Save_JButton = new javax.swing.JButton();
        SaveAs_JButton = new javax.swing.JButton();
        AutoSave_JCheckBox = new javax.swing.JCheckBox();
        jLabel1 = new javax.swing.JLabel();
        LastSave_JLabel = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        AutoSave_JComboBox = new javax.swing.JComboBox();
        Print_JButton = new javax.swing.JButton();
        FixPuzzleClues_JButton = new javax.swing.JButton();
        Redraw_JButton = new javax.swing.JButton();
        jLabel2 = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        CheckForErrors_JButton = new javax.swing.JButton();
        GiveMeAClue_JButton = new javax.swing.JButton();
        jPanel3 = new javax.swing.JPanel();
        PuzzleSolvedStatus_JLabel = new javax.swing.JLabel();
        SolvePuzzle_JButton = new javax.swing.JButton();
        Debug_JCheckBox = new javax.swing.JCheckBox();
        CheckSolution_JButton = new javax.swing.JButton();
        Stop_JButton = new javax.swing.JButton();
        autoStop_JCheckBox = new javax.swing.JCheckBox();
        jPanel4 = new javax.swing.JPanel();
        AutoMarkStart_JCheckBox = new javax.swing.JCheckBox();
        GuessLevel_JLabel = new javax.swing.JLabel();
        StartNewGuess_JButton = new javax.swing.JButton();
        UndoLastGuess_JButton = new javax.swing.JButton();
        AssumeGuessWrong_JCheckBox = new javax.swing.JCheckBox();
        CommitGuesses_JButton = new javax.swing.JButton();
        ClearPuzzle_JButton = new javax.swing.JButton();
        GuessLevel_JComboBox = new javax.swing.JComboBox();
        EditColors_JButton = new javax.swing.JButton();
        RemoveMarks_JButton = new javax.swing.JButton();
        Undo_JButton = new javax.swing.JButton();
        jPanel5 = new javax.swing.JPanel();
        jLabel6 = new javax.swing.JLabel();
        BoxSizeUp_JButton = new javax.swing.JButton();
        BoxSizeDown_JButton = new javax.swing.JButton();
        jLabel7 = new javax.swing.JLabel();
        FontSizeUp_JButton = new javax.swing.JButton();
        FontSizeDown_JButton = new javax.swing.JButton();
        jLabel8 = new javax.swing.JLabel();
        ClueWidthUp_JButton = new javax.swing.JButton();
        ClueWidthDown_JButton = new javax.swing.JButton();
        jLabel9 = new javax.swing.JLabel();
        ClueHeightUp_JButton = new javax.swing.JButton();
        ClueHeightDown_JButton = new javax.swing.JButton();
        BoxSize_JLabel = new javax.swing.JLabel();
        FontSize_JLabel = new javax.swing.JLabel();
        ClueWidth_JLabel = new javax.swing.JLabel();
        ClueHeight_JLabel = new javax.swing.JLabel();
        SaveSettings_JButton = new javax.swing.JButton();
        Refresh_JButton = new javax.swing.JButton();

        org.jdesktop.layout.GroupLayout jPanel6Layout = new org.jdesktop.layout.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 100, Short.MAX_VALUE)
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 100, Short.MAX_VALUE)
        );

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setResizable(false);

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Puzzle"));

        Save_JButton.setText("Save");
        Save_JButton.setEnabled(false);
        Save_JButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                Save_JButtonActionPerformed(evt);
            }
        });

        SaveAs_JButton.setText("Save As...");
        SaveAs_JButton.setEnabled(false);
        SaveAs_JButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                SaveAs_JButtonActionPerformed(evt);
            }
        });

        AutoSave_JCheckBox.setText("Auto-save");
        AutoSave_JCheckBox.setEnabled(false);
        AutoSave_JCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                AutoSave_JCheckBoxActionPerformed(evt);
            }
        });

        jLabel1.setText("Last saved");

        LastSave_JLabel.setText("<>");

        jLabel3.setText("mins ago");

        AutoSave_JComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "10", "5", "2" }));
        AutoSave_JComboBox.setEnabled(false);
        AutoSave_JComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                AutoSave_JComboBoxActionPerformed(evt);
            }
        });

        Print_JButton.setText("Print");
        Print_JButton.setEnabled(false);
        Print_JButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                Print_JButtonActionPerformed(evt);
            }
        });

        FixPuzzleClues_JButton.setText("Fix Puzzle Clues");
        FixPuzzleClues_JButton.setEnabled(false);
        FixPuzzleClues_JButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                FixPuzzleClues_JButtonActionPerformed(evt);
            }
        });

        Redraw_JButton.setText("Redraw");
        Redraw_JButton.setEnabled(false);
        Redraw_JButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                Redraw_JButtonActionPerformed(evt);
            }
        });

        jLabel2.setText("mins");

        org.jdesktop.layout.GroupLayout jPanel1Layout = new org.jdesktop.layout.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel1Layout.createSequentialGroup()
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING, false)
                    .add(org.jdesktop.layout.GroupLayout.LEADING, Print_JButton, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(Save_JButton, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(SaveAs_JButton, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel1Layout.createSequentialGroup()
                        .add(Redraw_JButton)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(FixPuzzleClues_JButton))
                    .add(jPanel1Layout.createSequentialGroup()
                        .add(AutoSave_JCheckBox)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(AutoSave_JComboBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jLabel2))
                    .add(jPanel1Layout.createSequentialGroup()
                        .add(jLabel1)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(LastSave_JLabel)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jLabel3)))
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel1Layout.createSequentialGroup()
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(Save_JButton)
                    .add(jLabel1)
                    .add(LastSave_JLabel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 16, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel3))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(SaveAs_JButton)
                    .add(AutoSave_JCheckBox)
                    .add(AutoSave_JComboBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel2))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 9, Short.MAX_VALUE)
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(Print_JButton)
                    .add(Redraw_JButton)
                    .add(FixPuzzleClues_JButton)))
        );

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder("Hints & Clues"));

        CheckForErrors_JButton.setText("Check for Errors");
        CheckForErrors_JButton.setEnabled(false);
        CheckForErrors_JButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                CheckForErrors_JButtonActionPerformed(evt);
            }
        });

        GiveMeAClue_JButton.setText("Give Me a Clue!");
        GiveMeAClue_JButton.setEnabled(false);
        GiveMeAClue_JButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                GiveMeAClue_JButtonActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout jPanel2Layout = new org.jdesktop.layout.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel2Layout.createSequentialGroup()
                .add(CheckForErrors_JButton)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(GiveMeAClue_JButton)
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                .add(CheckForErrors_JButton)
                .add(GiveMeAClue_JButton))
        );

        jPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder("Solve Puzzle"));

        PuzzleSolvedStatus_JLabel.setText("Puzzle has NOT been solved");

        SolvePuzzle_JButton.setText("Auto Finish");
        SolvePuzzle_JButton.setToolTipText("Run the automatic solver");
        SolvePuzzle_JButton.setEnabled(false);
        SolvePuzzle_JButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                SolvePuzzle_JButtonActionPerformed(evt);
            }
        });

        Debug_JCheckBox.setText("Debug");
        Debug_JCheckBox.setToolTipText("Puzzle is checked after each row or col is processed - slow!");
        Debug_JCheckBox.setEnabled(false);
        Debug_JCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                Debug_JCheckBoxActionPerformed(evt);
            }
        });

        CheckSolution_JButton.setText("Check My Solution");
        CheckSolution_JButton.setEnabled(false);
        CheckSolution_JButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                CheckSolution_JButtonActionPerformed(evt);
            }
        });

        Stop_JButton.setText("Stop");
        Stop_JButton.setEnabled(false);
        Stop_JButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                Stop_JButtonActionPerformed(evt);
            }
        });

        autoStop_JCheckBox.setSelected(true);
        autoStop_JCheckBox.setText("Auto Stop");
        autoStop_JCheckBox.setToolTipText("Have auto-solver stop when it needs to make a guess");
        autoStop_JCheckBox.setEnabled(false);

        org.jdesktop.layout.GroupLayout jPanel3Layout = new org.jdesktop.layout.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel3Layout.createSequentialGroup()
                .add(20, 20, 20)
                .add(jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel3Layout.createSequentialGroup()
                        .add(PuzzleSolvedStatus_JLabel)
                        .add(0, 0, 0)
                        .add(CheckSolution_JButton))
                    .add(jPanel3Layout.createSequentialGroup()
                        .add(SolvePuzzle_JButton)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(Debug_JCheckBox)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(autoStop_JCheckBox)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(Stop_JButton)))
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel3Layout.createSequentialGroup()
                .add(jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(PuzzleSolvedStatus_JLabel)
                    .add(CheckSolution_JButton))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(SolvePuzzle_JButton)
                    .add(Debug_JCheckBox)
                    .add(Stop_JButton)
                    .add(autoStop_JCheckBox)))
        );

        jPanel4.setBorder(javax.swing.BorderFactory.createTitledBorder("Puzzle Controls"));

        AutoMarkStart_JCheckBox.setSelected(true);
        AutoMarkStart_JCheckBox.setText("Auto-mark start of new guess");
        AutoMarkStart_JCheckBox.setToolTipText("Add special mark to first square selected at new guess level");
        AutoMarkStart_JCheckBox.setEnabled(false);
        AutoMarkStart_JCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                AutoMarkStart_JCheckBoxActionPerformed(evt);
            }
        });

        GuessLevel_JLabel.setText("Guess Level");

        StartNewGuess_JButton.setText("Start New Guess");
        StartNewGuess_JButton.setEnabled(false);
        StartNewGuess_JButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                StartNewGuess_JButtonActionPerformed(evt);
            }
        });

        UndoLastGuess_JButton.setText("Undo Last Guess");
        UndoLastGuess_JButton.setEnabled(false);
        UndoLastGuess_JButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                UndoLastGuess_JButtonActionPerformed(evt);
            }
        });

        AssumeGuessWrong_JCheckBox.setSelected(true);
        AssumeGuessWrong_JCheckBox.setText("Assume guess was wrong");
        AssumeGuessWrong_JCheckBox.setToolTipText("After guess undone, inverts original assumption.  Next square selected is your new guess.");
        AssumeGuessWrong_JCheckBox.setEnabled(false);
        AssumeGuessWrong_JCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                AssumeGuessWrong_JCheckBoxActionPerformed(evt);
            }
        });

        CommitGuesses_JButton.setText("Commit Guesses");
        CommitGuesses_JButton.setEnabled(false);
        CommitGuesses_JButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                CommitGuesses_JButtonActionPerformed(evt);
            }
        });

        ClearPuzzle_JButton.setText("Clear Puzzle!");
        ClearPuzzle_JButton.setEnabled(false);
        ClearPuzzle_JButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ClearPuzzle_JButtonActionPerformed(evt);
            }
        });

        GuessLevel_JComboBox.setModel(myDrawHandler.GetComboBoxModel());
        GuessLevel_JComboBox.setEnabled(false);
        GuessLevel_JComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                GuessLevel_JComboBoxActionPerformed(evt);
            }
        });

        EditColors_JButton.setText("Edit Colors");
        EditColors_JButton.setEnabled(false);

        RemoveMarks_JButton.setText("Remove Marks");
        RemoveMarks_JButton.setEnabled(false);
        RemoveMarks_JButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                RemoveMarks_JButtonActionPerformed(evt);
            }
        });

        Undo_JButton.setText("UNDO");
        Undo_JButton.setEnabled(false);
        Undo_JButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                Undo_JButtonActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout jPanel4Layout = new org.jdesktop.layout.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel4Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel4Layout.createSequentialGroup()
                        .add(StartNewGuess_JButton)
                        .add(jPanel4Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(jPanel4Layout.createSequentialGroup()
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 173, Short.MAX_VALUE)
                                .add(Undo_JButton))
                            .add(jPanel4Layout.createSequentialGroup()
                                .add(18, 18, 18)
                                .add(AutoMarkStart_JCheckBox)
                                .addContainerGap())))
                    .add(jPanel4Layout.createSequentialGroup()
                        .add(jPanel4Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(jPanel4Layout.createSequentialGroup()
                                .add(UndoLastGuess_JButton)
                                .add(18, 18, 18)
                                .add(AssumeGuessWrong_JCheckBox))
                            .add(jPanel4Layout.createSequentialGroup()
                                .add(CommitGuesses_JButton)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(ClearPuzzle_JButton))
                            .add(RemoveMarks_JButton)
                            .add(jPanel4Layout.createSequentialGroup()
                                .add(GuessLevel_JLabel)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                                .add(GuessLevel_JComboBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 131, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .add(18, 18, 18)
                                .add(EditColors_JButton)))
                        .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel4Layout.createSequentialGroup()
                .add(jPanel4Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(GuessLevel_JLabel)
                    .add(GuessLevel_JComboBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(EditColors_JButton))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel4Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(StartNewGuess_JButton)
                    .add(AutoMarkStart_JCheckBox))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel4Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(UndoLastGuess_JButton)
                    .add(AssumeGuessWrong_JCheckBox))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel4Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(CommitGuesses_JButton)
                    .add(ClearPuzzle_JButton))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel4Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(RemoveMarks_JButton)
                    .add(Undo_JButton))
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel5.setBorder(javax.swing.BorderFactory.createTitledBorder("Drawing Controls"));

        jLabel6.setText("Box Size");

        BoxSizeUp_JButton.setText("+");
        BoxSizeUp_JButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                BoxSizeUp_JButtonActionPerformed(evt);
            }
        });

        BoxSizeDown_JButton.setText("-");
        BoxSizeDown_JButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                BoxSizeDown_JButtonActionPerformed(evt);
            }
        });

        jLabel7.setText("Font Size");

        FontSizeUp_JButton.setText("+");
        FontSizeUp_JButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                FontSizeUp_JButtonActionPerformed(evt);
            }
        });

        FontSizeDown_JButton.setText("-");
        FontSizeDown_JButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                FontSizeDown_JButtonActionPerformed(evt);
            }
        });

        jLabel8.setText("Clue Width Spacing");

        ClueWidthUp_JButton.setText("+");
        ClueWidthUp_JButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ClueWidthUp_JButtonActionPerformed(evt);
            }
        });

        ClueWidthDown_JButton.setText("-");
        ClueWidthDown_JButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ClueWidthDown_JButtonActionPerformed(evt);
            }
        });

        jLabel9.setText("Clue Height Spacing");

        ClueHeightUp_JButton.setText("+");
        ClueHeightUp_JButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ClueHeightUp_JButtonActionPerformed(evt);
            }
        });

        ClueHeightDown_JButton.setText("-");
        ClueHeightDown_JButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ClueHeightDown_JButtonActionPerformed(evt);
            }
        });

        BoxSize_JLabel.setText("<>");

        FontSize_JLabel.setText("<>");

        ClueWidth_JLabel.setText("<>");

        ClueHeight_JLabel.setText("<>");

        SaveSettings_JButton.setText("Save Settings");
        SaveSettings_JButton.setEnabled(false);
        SaveSettings_JButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                SaveSettings_JButtonActionPerformed(evt);
            }
        });

        Refresh_JButton.setText("Refresh");
        Refresh_JButton.setToolTipText("Refresh the puzzle drawing");
        Refresh_JButton.setEnabled(false);
        Refresh_JButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                Refresh_JButtonActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout jPanel5Layout = new org.jdesktop.layout.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel5Layout.createSequentialGroup()
                        .add(jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(jPanel5Layout.createSequentialGroup()
                                .add(jLabel6)
                                .add(8, 8, 8)
                                .add(BoxSize_JLabel))
                            .add(jPanel5Layout.createSequentialGroup()
                                .add(jLabel7)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                                .add(FontSize_JLabel))
                            .add(jPanel5Layout.createSequentialGroup()
                                .add(jLabel8)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                                .add(ClueWidth_JLabel))
                            .add(jPanel5Layout.createSequentialGroup()
                                .add(jLabel9)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(ClueHeight_JLabel)))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 38, Short.MAX_VALUE)
                        .add(jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(jPanel5Layout.createSequentialGroup()
                                .add(BoxSizeUp_JButton)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(BoxSizeDown_JButton))
                            .add(jPanel5Layout.createSequentialGroup()
                                .add(FontSizeUp_JButton)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(FontSizeDown_JButton))
                            .add(jPanel5Layout.createSequentialGroup()
                                .add(ClueWidthUp_JButton)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(ClueWidthDown_JButton))
                            .add(jPanel5Layout.createSequentialGroup()
                                .add(ClueHeightUp_JButton)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(ClueHeightDown_JButton))))
                    .add(jPanel5Layout.createSequentialGroup()
                        .add(SaveSettings_JButton)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(Refresh_JButton)
                        .add(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel5Layout.createSequentialGroup()
                .add(jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(BoxSizeUp_JButton)
                    .add(BoxSizeDown_JButton)
                    .add(jLabel6)
                    .add(BoxSize_JLabel))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel7)
                    .add(FontSizeUp_JButton)
                    .add(FontSizeDown_JButton)
                    .add(FontSize_JLabel))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(ClueWidthUp_JButton)
                    .add(ClueWidthDown_JButton)
                    .add(jLabel8)
                    .add(ClueWidth_JLabel))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(ClueHeightUp_JButton)
                    .add(ClueHeightDown_JButton)
                    .add(jLabel9)
                    .add(ClueHeight_JLabel))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(SaveSettings_JButton)
                    .add(Refresh_JButton)))
        );

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING, false)
                    .add(jPanel4, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(jPanel1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jPanel2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jPanel5, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jPanel3, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap(18, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .add(14, 14, 14)
                .add(jPanel2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .add(18, 18, 18)
                .add(jPanel3, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(jPanel4, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 16, Short.MAX_VALUE)
                .add(jPanel5, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void Save_JButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_Save_JButtonActionPerformed
        if (myDrawHandler != null) myDrawHandler.SavePuzzle ();
    }//GEN-LAST:event_Save_JButtonActionPerformed

    private void SaveAs_JButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_SaveAs_JButtonActionPerformed
        if (myDrawHandler != null) myDrawHandler.SavePuzzleAs ();
    }//GEN-LAST:event_SaveAs_JButtonActionPerformed

    private void CheckForErrors_JButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_CheckForErrors_JButtonActionPerformed
        if (myDrawHandler != null) myDrawHandler.CheckPuzzle();
    }//GEN-LAST:event_CheckForErrors_JButtonActionPerformed

    private void Undo_JButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_Undo_JButtonActionPerformed
        if (myDrawHandler != null) 
		{
			myDrawHandler.Undo();
			myDrawHandler.Redraw();
		}
    }//GEN-LAST:event_Undo_JButtonActionPerformed

    private void RemoveMarks_JButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_RemoveMarks_JButtonActionPerformed
        if (myDrawHandler != null) 
		{
			myDrawHandler.RemoveMarks();
			myDrawHandler.Redraw();
		}
    }//GEN-LAST:event_RemoveMarks_JButtonActionPerformed

    private void CommitGuesses_JButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_CommitGuesses_JButtonActionPerformed
        if (myDrawHandler != null) 
		{
			myDrawHandler.CommitGuesses();
			myDrawHandler.Redraw();
		}
    }//GEN-LAST:event_CommitGuesses_JButtonActionPerformed

    private void UndoLastGuess_JButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_UndoLastGuess_JButtonActionPerformed
        if (myDrawHandler != null) 
		{
			myDrawHandler.UndoLastGuess();
			myDrawHandler.Redraw();
		}
    }//GEN-LAST:event_UndoLastGuess_JButtonActionPerformed

    private void ClearPuzzle_JButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ClearPuzzle_JButtonActionPerformed
        if (myDrawHandler != null) myDrawHandler.ClearPuzzle();
    }//GEN-LAST:event_ClearPuzzle_JButtonActionPerformed

    private void Print_JButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_Print_JButtonActionPerformed
        if (myDrawHandler != null) myDrawHandler.Print();
    }//GEN-LAST:event_Print_JButtonActionPerformed

    private void Redraw_JButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_Redraw_JButtonActionPerformed
        if (myDrawHandler != null) myDrawHandler.Redraw();
    }//GEN-LAST:event_Redraw_JButtonActionPerformed

    private void SolvePuzzle_JButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_SolvePuzzle_JButtonActionPerformed
        if (myDrawHandler != null) 
			myDrawHandler.SolvePuzzle(Debug_JCheckBox.isSelected(), autoStop_JCheckBox.isSelected());
    }//GEN-LAST:event_SolvePuzzle_JButtonActionPerformed

    private void BoxSizeUp_JButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_BoxSizeUp_JButtonActionPerformed
        if (myDrawHandler != null) { myDrawHandler.IncrBoxSize(); UpdateBoxSizeLabel(); }
    }//GEN-LAST:event_BoxSizeUp_JButtonActionPerformed

    private void BoxSizeDown_JButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_BoxSizeDown_JButtonActionPerformed
        if (myDrawHandler != null) { myDrawHandler.DecrBoxSize(); UpdateBoxSizeLabel(); }
    }//GEN-LAST:event_BoxSizeDown_JButtonActionPerformed

    private void FontSizeUp_JButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_FontSizeUp_JButtonActionPerformed
        if (myDrawHandler != null) { myDrawHandler.IncrFontSize(); UpdateFontSizeLabel(); }
    }//GEN-LAST:event_FontSizeUp_JButtonActionPerformed

    private void FontSizeDown_JButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_FontSizeDown_JButtonActionPerformed
        if (myDrawHandler != null) { myDrawHandler.DecrFontSize(); UpdateFontSizeLabel(); }
    }//GEN-LAST:event_FontSizeDown_JButtonActionPerformed

    private void ClueHeightUp_JButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ClueHeightUp_JButtonActionPerformed
        if (myDrawHandler != null) { myDrawHandler.IncrClueHeight(); UpdateClueHeightLabel(); }
    }//GEN-LAST:event_ClueHeightUp_JButtonActionPerformed

    private void ClueHeightDown_JButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ClueHeightDown_JButtonActionPerformed
        if (myDrawHandler != null) { myDrawHandler.DecrClueHeight(); UpdateClueHeightLabel(); }
    }//GEN-LAST:event_ClueHeightDown_JButtonActionPerformed

    private void ClueWidthUp_JButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ClueWidthUp_JButtonActionPerformed
        if (myDrawHandler != null) { myDrawHandler.IncrClueWidth(); UpdateClueWidthLabel(); }
    }//GEN-LAST:event_ClueWidthUp_JButtonActionPerformed

    private void ClueWidthDown_JButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ClueWidthDown_JButtonActionPerformed
        if (myDrawHandler != null) { myDrawHandler.DecrClueWidth(); UpdateClueWidthLabel(); }
    }//GEN-LAST:event_ClueWidthDown_JButtonActionPerformed

    private void FixPuzzleClues_JButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_FixPuzzleClues_JButtonActionPerformed
        if (myDrawHandler != null) myDrawHandler.EditClues();
    }//GEN-LAST:event_FixPuzzleClues_JButtonActionPerformed

    private void GiveMeAClue_JButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_GiveMeAClue_JButtonActionPerformed
        if (myDrawHandler != null) myDrawHandler.GiveMeAClue();
    }//GEN-LAST:event_GiveMeAClue_JButtonActionPerformed

    private void StartNewGuess_JButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_StartNewGuess_JButtonActionPerformed
        if (myDrawHandler != null) myDrawHandler.StartNewGuess();
    }//GEN-LAST:event_StartNewGuess_JButtonActionPerformed

    private void GuessLevel_JComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_GuessLevel_JComboBoxActionPerformed
        int item = GuessLevel_JComboBox.getSelectedIndex();
        if (myDrawHandler != null) myDrawHandler.HandleSetGuessLevelFromControls(item);
    }//GEN-LAST:event_GuessLevel_JComboBoxActionPerformed

    private void SaveSettings_JButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_SaveSettings_JButtonActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_SaveSettings_JButtonActionPerformed

    private void AutoMarkStart_JCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_AutoMarkStart_JCheckBoxActionPerformed
        if (AssumeGuessWrong_JCheckBox.isSelected())
			AutoMarkStart_JCheckBox.setSelected(true);
		PBNPuzzle myPuzzle = myDrawHandler.GetThePuzzle();
		if (myPuzzle != null) myPuzzle.SetAutoMarkStartFromControls(AutoMarkStart_JCheckBox.isSelected());
    }//GEN-LAST:event_AutoMarkStart_JCheckBoxActionPerformed

    private void AutoSave_JCheckBoxActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_AutoSave_JCheckBoxActionPerformed
    {//GEN-HEADEREND:event_AutoSave_JCheckBoxActionPerformed
        if (myDrawHandler != null) myDrawHandler.SetAutoSave (AutoSave_JCheckBox.isSelected());
    }//GEN-LAST:event_AutoSave_JCheckBoxActionPerformed

    private void AutoSave_JComboBoxActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_AutoSave_JComboBoxActionPerformed
    {//GEN-HEADEREND:event_AutoSave_JComboBoxActionPerformed
        if (myDrawHandler != null) myDrawHandler.SetAutoSaveInterval(GetAutoSaveInterval());
    }//GEN-LAST:event_AutoSave_JComboBoxActionPerformed

    private void CheckSolution_JButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_CheckSolution_JButtonActionPerformed
    {//GEN-HEADEREND:event_CheckSolution_JButtonActionPerformed
        if (myDrawHandler != null) myDrawHandler.CheckMySolution ();
    }//GEN-LAST:event_CheckSolution_JButtonActionPerformed

    private void Debug_JCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_Debug_JCheckBoxActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_Debug_JCheckBoxActionPerformed

    private void Stop_JButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_Stop_JButtonActionPerformed
        if (myDrawHandler != null) myDrawHandler.TellSolverToStop();
    }//GEN-LAST:event_Stop_JButtonActionPerformed

    private void Refresh_JButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_Refresh_JButtonActionPerformed
        if (myDrawHandler != null) myDrawHandler.Redraw ();
    }//GEN-LAST:event_Refresh_JButtonActionPerformed

    private void AssumeGuessWrong_JCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_AssumeGuessWrong_JCheckBoxActionPerformed
		// This option doesn't work unless the Auto Mark Start option is also selected
		if (AssumeGuessWrong_JCheckBox.isSelected()) AutoMarkStart_JCheckBox.setSelected(true);
    }//GEN-LAST:event_AssumeGuessWrong_JCheckBoxActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox AssumeGuessWrong_JCheckBox;
    private javax.swing.JCheckBox AutoMarkStart_JCheckBox;
    private javax.swing.JCheckBox AutoSave_JCheckBox;
    private javax.swing.JComboBox AutoSave_JComboBox;
    private javax.swing.JButton BoxSizeDown_JButton;
    private javax.swing.JButton BoxSizeUp_JButton;
    private javax.swing.JLabel BoxSize_JLabel;
    private javax.swing.JButton CheckForErrors_JButton;
    private javax.swing.JButton CheckSolution_JButton;
    private javax.swing.JButton ClearPuzzle_JButton;
    private javax.swing.JButton ClueHeightDown_JButton;
    private javax.swing.JButton ClueHeightUp_JButton;
    private javax.swing.JLabel ClueHeight_JLabel;
    private javax.swing.JButton ClueWidthDown_JButton;
    private javax.swing.JButton ClueWidthUp_JButton;
    private javax.swing.JLabel ClueWidth_JLabel;
    private javax.swing.JButton CommitGuesses_JButton;
    private javax.swing.JCheckBox Debug_JCheckBox;
    private javax.swing.JButton EditColors_JButton;
    private javax.swing.JButton FixPuzzleClues_JButton;
    private javax.swing.JButton FontSizeDown_JButton;
    private javax.swing.JButton FontSizeUp_JButton;
    private javax.swing.JLabel FontSize_JLabel;
    private javax.swing.JButton GiveMeAClue_JButton;
    private javax.swing.JComboBox GuessLevel_JComboBox;
    private javax.swing.JLabel GuessLevel_JLabel;
    private javax.swing.JLabel LastSave_JLabel;
    private javax.swing.JButton Print_JButton;
    private javax.swing.JLabel PuzzleSolvedStatus_JLabel;
    private javax.swing.JButton Redraw_JButton;
    private javax.swing.JButton Refresh_JButton;
    private javax.swing.JButton RemoveMarks_JButton;
    private javax.swing.JButton SaveAs_JButton;
    private javax.swing.JButton SaveSettings_JButton;
    private javax.swing.JButton Save_JButton;
    private javax.swing.JButton SolvePuzzle_JButton;
    private javax.swing.JButton StartNewGuess_JButton;
    private javax.swing.JButton Stop_JButton;
    private javax.swing.JButton UndoLastGuess_JButton;
    private javax.swing.JButton Undo_JButton;
    private javax.swing.JCheckBox autoStop_JCheckBox;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    // End of variables declaration//GEN-END:variables

}
