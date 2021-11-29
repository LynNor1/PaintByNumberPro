package paintbynumberpro;

import java.io.*;
import java.awt.*;
import java.util.Stack;
import javax.swing.JOptionPane;

// Definitions:
//  Empty square: X
//  Filled square: F
//  Unknown square: _
//  Cluster: continuous sequence of Filled squares
//  Hole: continuous sequence of Unknown squares surrounded by Empty squares e.g. X____X
//  Bumper: continuous sequence of Unknown and/or Filled squares surrounded by
//          Empty squares e.g. X__FFF__FX
//
//  Bumpers can be Holes, but a Hole with a Filled square *cannot* be a Bumper

public class PBNPuzzle {

    private PBNHandler myDrawHandler = null;
    
    private static final int DEFAULT_BOX_SIZE = 19;
    private static final int DEFAULT_FONT_SIZE = 11;
    private static final int DEFAULT_CLUE_WIDTH = 16;
    private static final int DEFAULT_CLUE_HEIGHT = 13;

	private File myFile = null;
	private boolean isReady = false;
    private String source = null;
    private boolean isSolved = false;

    // The items that are shown as public, are used by the PBNGetCluesDialog
    // to set the clues from the operator or are used by the PBNPuzzle initializer
    // that clones an existing puzzle
	private int rows = 0, cols = 0;
	private int row_nclues[] = null, col_nclues[] = null;
	private int row_clues[][] = null, col_clues[][] = null;
	private int row_clue_status[][] = null, col_clue_status[][] = null;
	private int max_row_clues = 0, max_col_clues = 0;

    // Use puzzle to keep track of the current status of the puzzle
    // Use puzzle_backup to remember last known static state of the puzzle
    // For example, when tracking click-n-drag, you can restore a square
    // to its original status if the mouse moves away from a square by reloading
    // the status from the puzzle_backup
	private PuzzleSquare puzzle[][] = null;
    private PuzzleSquare puzzle_backup[][] = null;

	private Selection curSelection;
    private Selection lastSquareSelection;
    private Selection lastClueSelection;
	
	private int guess_level = 0;	// 0==known, 1+ = guess_level
    private boolean mark_first_selection = false;
	
	private static final int MAXCLUES = 50;
	
	public static final int UNACCOUNTEDFOR = 0;
	public static final int ACCOUNTEDFOR = 1;
	public static final int POSSIBLYACCOUNTEDFOR = 2;
	
	// undo stack
    // Philosophically - I want the undo stack to contain every change in
    // status for a particular selected item.  For example when a cell is changed
    // from unknown status to selected status.  Each "change" also is assigned
    // an ID.  If more than one change is to be undone at once, then they must
    // all have the same ID.
	private Stack<StackSelection> undoStack = new Stack();
	private int MAX_STACK_SIZE = 100;

    // stuff for puzzle drawing (from file)
    private int BOX_SIZE = DEFAULT_BOX_SIZE;
    private int clue_width = DEFAULT_CLUE_WIDTH, clue_height = DEFAULT_CLUE_HEIGHT;
    private int font_size = DEFAULT_FONT_SIZE;

    PBNPuzzle ()
    {
        isReady = false;
    }

    // This is a cloning constructor
    PBNPuzzle (PBNPuzzle aPuzzle)
    {
        this ();
        if (aPuzzle == null) return;

        myDrawHandler = aPuzzle.myDrawHandler;
        if (aPuzzle.myFile != null)
            myFile = new File(aPuzzle.myFile.getPath());
        else
            myFile = null;
        isReady = aPuzzle.isReady;
        if (aPuzzle.source != null)
            source = new String (aPuzzle.source);
        else
            source = null;
        isSolved = aPuzzle.isSolved;

        rows = aPuzzle.rows;
        cols = aPuzzle.cols;
        max_row_clues = aPuzzle.max_row_clues;
        max_col_clues = aPuzzle.max_col_clues;
        if (aPuzzle.row_nclues != null)
        {
            row_nclues = new int[rows];
            for (int i=0; i<rows; i++)
                row_nclues[i] = aPuzzle.row_nclues[i];
        } else row_nclues = null;
        if (aPuzzle.col_nclues != null)
        {
            col_nclues = new int[cols];
            for (int i=0; i<cols; i++)
                col_nclues[i] = aPuzzle.col_nclues[i];
        } else col_nclues = null;
        if (aPuzzle.row_clues != null)
        {
            row_clues = new int[rows][MAXCLUES];
            for (int r=0; r<rows; r++)
                for (int cl=0; cl<row_nclues[r]; cl++)
                    row_clues[r][cl] = aPuzzle.row_clues[r][cl];
        } else row_clues = null;
        if (aPuzzle.col_clues != null)
        {
            col_clues = new int[cols][MAXCLUES];
            for (int c=0; c<cols; c++)
                for (int cl=0; cl<col_nclues[c]; cl++)
                    col_clues[c][cl] = aPuzzle.col_clues[c][cl];
        } else col_clues = null;
        if (aPuzzle.row_clue_status != null)
        {
            row_clue_status = new int[rows][MAXCLUES];
            for (int r=0; r<rows; r++)
                for (int cl=0; cl<row_nclues[r]; cl++)
                     row_clue_status[r][cl] = aPuzzle.row_clue_status[r][cl];
        } else row_clue_status = null;
        if (aPuzzle.col_clue_status != null)
        {
            col_clue_status = new int[cols][MAXCLUES];
            for (int c=0; c<cols; c++)
                for (int cl=0; cl<col_nclues[c]; cl++)
                    col_clue_status[c][cl] = aPuzzle.col_clue_status[c][cl];
        } else col_clue_status = null;

        if (aPuzzle.puzzle != null)
        {
            puzzle = new PuzzleSquare[rows][cols];
            for (int r=0; r<rows; r++)
                for (int c=0; c<cols; c++)
                    puzzle[r][c] = new PuzzleSquare(aPuzzle.puzzle[r][c]);
        } else puzzle = null;
        if (aPuzzle.puzzle_backup != null)
        {
            puzzle_backup = new PuzzleSquare[rows][cols];
            for (int r=0; r<rows; r++)
                for (int c=0; c<cols; c++)
                    puzzle_backup[r][c] = new PuzzleSquare(aPuzzle.puzzle_backup[r][c]);
        } else puzzle_backup = null;

        if (aPuzzle.curSelection != null)
            curSelection = new Selection(aPuzzle.curSelection);
        else curSelection = null;
        if (aPuzzle.lastSquareSelection != null)
            lastSquareSelection = new Selection(aPuzzle.lastSquareSelection);
        else lastSquareSelection = null;
        if (aPuzzle.lastClueSelection != null)
            lastClueSelection = new Selection(aPuzzle.lastClueSelection);
        else lastClueSelection = null;

        guess_level = aPuzzle.guess_level;
        mark_first_selection = aPuzzle.mark_first_selection;

        if (aPuzzle.undoStack != null)
        {
            undoStack = new Stack ();
            int stackSize = aPuzzle.undoStack.size();
            if (stackSize > 0)
            {
                // Push objects onto new stack
                // Assume 0th object is the first one that was pushed
                // onto the original stack and should therefore be the
                // one that gets pushed first on the clone stack
                for (int idx=0; idx>stackSize; idx++)
                    undoStack.add (aPuzzle.undoStack.get(idx));
            }
        } else
            undoStack = null;
        MAX_STACK_SIZE = aPuzzle.MAX_STACK_SIZE;

        if (aPuzzle.BOX_SIZE > 0) BOX_SIZE = aPuzzle.BOX_SIZE;
        if (aPuzzle.clue_width > 0) clue_width = aPuzzle.clue_width;
        if (aPuzzle.clue_height > 0) clue_height = aPuzzle.clue_height;
        if (aPuzzle.font_size > 0) font_size = aPuzzle.font_size;

    }

    public void SetDrawHandler (PBNHandler theHandler)
    { myDrawHandler = theHandler; }

    public void SetPuzzleNameAndDims (String name, int cs, int rs)
    {
        if (cs <= 0 || rs <= 0) return;

        cols = cs;
        rows = rs;
        source = name;

        // Set up the arrays for holding the clues
        row_nclues = new int[rows];
        col_nclues = new int[cols];
        row_clues = new int[rows][MAXCLUES];
        col_clues = new int[cols][MAXCLUES];
        for (int i=0; i<cols; i++) col_nclues[i] = 0;
        for (int i=0; i<rows; i++) row_nclues[i] = 0;

    }

    // Sanity check for clues
    public boolean SanityCheckTheClues ()
    {
        int n_filled_cols = 0;
        int n_filled_rows = 0;

        for (int i=0; i<cols; i++)
        {
            int n_col = 0;
            if (col_nclues[i] > 0)
            {
                for (int j=0; j<col_nclues[i]; j++)
                    n_col += col_clues[i][j];
                if ((n_col+(col_nclues[i]-1) > rows))
                {
                    JOptionPane.showMessageDialog (null,
                        "Clues in col " + i + " are invalid", "Puzzle Sanity Check",
                        JOptionPane.INFORMATION_MESSAGE, null);
                    return false;
                }
            }
            n_filled_cols += n_col;
        }

        for (int i=0; i<rows; i++)
        {
            int n_row = 0;
            if (row_nclues[i] > 0)
            {
                for (int j=0; j<row_nclues[i]; j++)
                    n_row += row_clues[i][j];
                if ((n_row+(row_nclues[i]-1) > cols))
                {
                    JOptionPane.showMessageDialog (null,
                        "Clues in row " + i + " are invalid", "Puzzle Sanity Check",
                        JOptionPane.INFORMATION_MESSAGE, null);
                    return false;
                }
            }
            n_filled_rows += n_row;
        }

        if (n_filled_cols != n_filled_rows)
        {
            JOptionPane.showMessageDialog (null,
                "Total # filled squares in row direction (" + n_filled_rows + ")\n" +
                "does not match filled squares in col direction (" + n_filled_cols + ")",
                "Puzzle Sanity Check",
                JOptionPane.INFORMATION_MESSAGE, null);
            return false;
        }

        return true;
    }
	
	public boolean IsReady ()
	{ return isReady; }
    public void SetReady (boolean state)
    { isReady = state; }

    public boolean IsSolved ()
    { return isSolved; }
    public void SetSolved (boolean state)
    { isSolved = state; }

    public int GetMax_Stack_Size()
    { return MAX_STACK_SIZE; }
    public void SetMax_Stack_Size (int stack_size)
    { MAX_STACK_SIZE = stack_size; }

    // ------------------------------------------
    // Drawing Code (goes back to the DrawHandler
    // ------------------------------------------

    private void DrawSelection (Selection s, boolean stat)
    {
        if (myDrawHandler != null)
          myDrawHandler.DrawSelection (s, stat);
    }

    private void DrawPuzzleBox (Graphics g, int row, int col, boolean draw_bold)
    {
        if (myDrawHandler != null)
            myDrawHandler.DrawPuzzleBox (g, row, col, draw_bold);
    }

    private void DrawPuzzleBox (int row, int col, boolean draw_bold,
            boolean draw_full_bold)
    {
        if (myDrawHandler != null)
            myDrawHandler.DrawPuzzleBox (row, col, draw_bold, draw_full_bold);
    }

    private void DrawPuzzleBox (Selection sel, boolean draw_bold, boolean draw_bold_full)
    {
        if (myDrawHandler != null)
            myDrawHandler.DrawPuzzleBox (sel, draw_bold, draw_bold_full);
    }

    private void DrawPuzzleBoldLines (Graphics g)
    {
        if (myDrawHandler != null)
            myDrawHandler.DrawPuzzleBoldLines(g);
    }

    private void DrawPuzzle (Graphics g, Rectangle rect)
    {
        if (myDrawHandler != null)
        {
            myDrawHandler.DrawPuzzle (g, rect, false);
            myDrawHandler.RedrawCluesComponents();
        }
    }

    // -------------------------
    // Code to manipulate puzzle
    // -------------------------

    public void ToggleMarkRowColFromPopup (int row, int col)
    {
        int priorStatus = puzzle[row][col].GetStatusAsInt();
        ToggleMarkRowCol (row, col);
        Selection oldSelection = new Selection (curSelection);
        curSelection.setRowColSelected(row, col, puzzle[row][col].GetStatusAsInt());
        lastSquareSelection.copySelection (curSelection);
        if (!oldSelection.isEqual(curSelection)) DrawSelection (oldSelection, false);
        DrawSelection (curSelection, true);
        AddToStack (curSelection, priorStatus, true);
    }
	
	public void ToggleMarkRowCol (int row, int col)
	{
        puzzle[row][col].ToggleMarked();
        puzzle_backup[row][col].CloneStatusFromSquare(puzzle[row][col]);
	}

    public void ToggleMarkRowCol ()
    {
        if (curSelection == null) return;
        int row = curSelection.getRowSelected();
        int col = curSelection.getColSelected();
        if (row < 0 || row >= rows || col < 0 || col >= cols) return;
        ToggleMarkRowColFromPopup (row, col);
    }

    public void HandleLockColClue (int col, int clue)
    {
        System.out.println ("Implement HandleLockColClue()");
//        HandleNewColClueSelection (col, clue);
    }

    public void HandleUnlockColClue (int col, int clue)
    {
        if (IsClueLocked (col_clue_status[col][clue]))
        {
            Selection oldSelection = new Selection (curSelection);
            int priorStatus = col_clue_status[col][clue];
            curSelection.setColClueSelected(col, clue, col_clue_status[col][clue]);
            lastClueSelection.copySelection (curSelection);
            boolean sameSelection = oldSelection.isEqual(curSelection);
            if (sameSelection)
            {
                UnlockColClue(col, clue);
                curSelection.setColClueSelected(col, clue, col_clue_status[col][clue]);
                lastClueSelection.copySelection (curSelection);
                AddToStack(curSelection, priorStatus, true);
            }
            if (!sameSelection) DrawSelection (oldSelection, false);
            DrawSelection (curSelection, true);
        }
    }

    private void HandleNewColClueSelection (int col, int clue)
    {
        Selection oldSelection = new Selection (curSelection);
        int priorStatus = col_clue_status[col][clue];
        curSelection.setColClueSelected(col, clue, col_clue_status[col][clue]);
        lastClueSelection.copySelection (curSelection);
        boolean sameSelection = oldSelection.isEqual(curSelection);
        if (sameSelection)
        {
            CycleColClue(col, clue);
            curSelection.setColClueSelected(col, clue, col_clue_status[col][clue]);
            lastClueSelection.copySelection (curSelection);
            AddToStack(curSelection, priorStatus, true);
        }
        if (!sameSelection) DrawSelection (oldSelection, false);
        DrawSelection (curSelection, true);
    }

    public void HandleLockRowClue (int row, int clue)
    {
        System.out.println ("Implement HandleLockRowClue()");
//        HandleNewRowClueSelection (row, clue);
    }

    public void HandleUnlockRowClue (int row, int clue)
    {
        if (IsClueLocked (row_clue_status[row][clue]))
        {
            Selection oldSelection = new Selection (curSelection);
            int priorStatus = row_clue_status[row][clue];
            curSelection.setRowClueSelected(row, clue, row_clue_status[row][clue]);
            lastClueSelection.copySelection (curSelection);
            boolean sameSelection = oldSelection.isEqual(curSelection);
            if (sameSelection)
            {
                UnlockRowClue(row, clue);
                curSelection.setRowClueSelected(row, clue, row_clue_status[row][clue]);
                lastClueSelection.copySelection (curSelection);
                AddToStack(curSelection, priorStatus, true);
            }
            if (!sameSelection) DrawSelection (oldSelection, false);
            DrawSelection (curSelection, true);
        }
    }

    private void HandleNewRowClueSelection (int row, int clue)
    {
        Selection oldSelection = new Selection (curSelection);
        int priorStatus = row_clue_status[row][clue];
        curSelection.setRowClueSelected(row, clue, row_clue_status[row][clue]);
        lastClueSelection.copySelection (curSelection);
        boolean sameSelection = oldSelection.isEqual(curSelection);
        if (sameSelection)
        {
            CycleRowClue(row, clue);
            curSelection.setRowClueSelected(row, clue, row_clue_status[row][clue]);
            lastClueSelection.copySelection (curSelection);
            AddToStack (curSelection, priorStatus, true);
        }
        if (!sameSelection) DrawSelection (oldSelection, false);
        DrawSelection (curSelection, true);
    }

    private boolean IsClueLocked (int status)
    {
        System.out.println ("Implement IsClueLocked()");
        return false;
    }

    private void UnlockColClue (int col, int clue)
    {
        System.out.println ("Implement UnlockColClue()");
    }

    private void UnlockRowClue (int row, int clue)
    {
        System.out.println ("Implement UnlockRowClue()");
    }

    public void MarkRowColFromPopup (int row, int col)
    {

        int priorStatus = puzzle[row][col].GetStatusAsInt();
        MarkRowCol(row, col);
        if (puzzle[row][col].GetStatusAsInt() != priorStatus)
        {
            Selection oldSelection = new Selection (curSelection);
            curSelection.setRowColSelected(row, col, puzzle[row][col].GetStatusAsInt());
            lastSquareSelection.copySelection (curSelection);
            if (!oldSelection.isEqual(curSelection)) DrawSelection (oldSelection, false);
            DrawSelection (curSelection, true);
            AddToStack (curSelection, priorStatus, true);
        }
    }

    public void MarkRowCol (int row, int col)
    {
		if (row < 0 || row >= rows || col < 0 || col >= cols) return;
		puzzle[row][col].SetMarkedStatus(true);
        puzzle_backup[row][col].CloneStatusFromSquare(puzzle[row][col]);
    }

    public void MarkRowCol ()
    {
        if (curSelection == null) return;
        int row = curSelection.getRowSelected();
        int col = curSelection.getColSelected();
        if (row < 0 || row >= rows || col < 0 || col >= cols) return;
        MarkRowColFromPopup (row, col);
    }

    public void UnmarkRowColFromPopup (int row, int col)
    {
        int priorStatus = puzzle[row][col].GetStatusAsInt();
        UnmarkRowCol(row, col);
        if (puzzle[row][col].GetStatusAsInt() != priorStatus)
        {
            Selection oldSelection = new Selection (curSelection);
            curSelection.setRowColSelected(row, col, puzzle[row][col].GetStatusAsInt());
            lastSquareSelection.copySelection (curSelection);
            if (!oldSelection.isEqual(curSelection)) DrawSelection (oldSelection, false);
            DrawSelection (curSelection, true);
            AddToStack (curSelection, priorStatus, true);
        }
    }

    public void UnmarkRowCol (int row, int col)
    {
		if (row < 0 || row >= rows || col < 0 || col >= cols) return;
        puzzle[row][col].SetMarkedStatus(false);
        puzzle[row][col].SetSpecialMarked (false);
        puzzle_backup[row][col].CloneStatusFromSquare(puzzle[row][col]);
    }

    public void UnmarkRowCol ()
    {
        if (curSelection == null) return;
        int row = curSelection.getRowSelected();
        int col = curSelection.getColSelected();
        if (row < 0 || row >= rows || col < 0 || col >= cols) return;
        UnmarkRowColFromPopup (row, col);
    }

    public void InitializeClueStatusArrays ()
    {
        row_clue_status = new int[rows][max_row_clues];
        col_clue_status = new int[cols][max_col_clues];
        for (int i=0; i<rows; i++)
            for (int j=0; j<max_row_clues; j++)
                row_clue_status[i][j] = UNACCOUNTEDFOR;
        for (int i=0; i<cols; i++)
            for (int j=0; j<max_col_clues; j++)
                col_clue_status[i][j] = UNACCOUNTEDFOR;
   }
   public void InitializePuzzleArrays ()
   {
        puzzle = new PuzzleSquare [rows][cols];
        puzzle_backup = new PuzzleSquare [rows][cols];
        for (int i=0; i<rows; i++)
            for (int j=0; j<cols; j++)
            {
                puzzle[i][j] = new PuzzleSquare(PuzzleSquare.SquareStatus.UNKNOWN);
                puzzle_backup[i][j] = new PuzzleSquare(PuzzleSquare.SquareStatus.UNKNOWN);
            }
    }

    public void InitializeSelections ()
    {
		// Create current selection and last square selection
		curSelection = new Selection();
        lastSquareSelection = new Selection();
        lastClueSelection = new Selection ();
    }

    public void StartNewGuessLevel ()
    {
        int max_level = this.GetMaxGuessLevel();
        int cnt_squares = this.GetSquaresAtGuessLevel(max_level);
        if (cnt_squares > 0) guess_level = max_level + 1;
        else guess_level = max_level;
        myDrawHandler.SetGuessingControlItems(guess_level);
    }

    public void GetAndSetNextGuess (boolean mark_first)
    {
		int max_level = this.GetMaxGuessLevel();
		int cnt_squares = this.GetSquaresAtGuessLevel(max_level);
		if (cnt_squares > 0) 
		{
			guess_level = max_level+1;
			myDrawHandler.SetGuessingControlItems (guess_level);
		} else guess_level = max_level;
        Point next_guess = PuzzleSolver.GenerateNewGuess(this);
        int col = next_guess.x;
        int row = next_guess.y;
        if (!puzzle[row][col].IsUnknown())
        {
            PaintByNumberPro.HandleErrorMessage ("Generate Guess Error",
                    "Square row col " + row + " " + col + " already FILLED or EMPTY");
            return;
        }
        puzzle[row][col].SetStatus (PuzzleSquare.SquareStatus.FILLED, guess_level);
        if (mark_first) puzzle[row][col].SetSpecialMarked(true);
        puzzle_backup[row][col].CloneStatusFromSquare(puzzle[row][col]);
    }

    public void SetAutoMarkStartFromControls (boolean is_selected)
    {
		mark_first_selection = is_selected;
/*        mark_first_selection = false;
        if (is_selected)
        {
            int numsquares = GetSquaresAtGuessLevel(guess_level);
            if (numsquares == 0) mark_first_selection = true;
        }
*/
    }

    public void SetGuessLevel (int level)
    {
        guess_level = level;
        if (myDrawHandler != null)
        {
            myDrawHandler.SetGuessingControlItems(guess_level);
            SetAutoMarkStartFromControls (myDrawHandler.GetAutoMarkStart());
        }
    }
    public void SetSource (String src)
    { source = src; }
    public void SetCols (int cs)
    {
        cols = cs;
        col_nclues = new int[cols];
        col_clues = new int[cols][MAXCLUES];
        if (rows > 0) UpdateStackSize();
    }
    public void SetRows (int rs)
    {
        rows = rs;
        row_nclues = new int[rows];
        row_clues = new int[rows][MAXCLUES];
        if (cols > 0) UpdateStackSize();
    }
    public void SetRow_NClues (int row, int num_clues)
    { row_nclues[row] = num_clues; }
    public void SetCol_NClues (int col, int num_clues)
    { col_nclues[col] = num_clues; }
    public void SetRow_Clues (int row, int clue_num, int clue)
    { row_clues[row][clue_num] = clue; }
    public void SetCol_Clues (int col, int clue_num, int clue)
    { col_clues[col][clue_num] = clue; }
    public void SetMax_Row_Clues (int max)
    { max_row_clues = max; }
    public void SetMax_Col_Clues (int max)
    { max_col_clues = max; }
    public void SetRow_Clue_Status (int row, int clue_num, int status)
    { row_clue_status[row][clue_num] = status; }
    public void SetCol_Clue_Status (int col, int clue_num, int status)
    { col_clue_status[col][clue_num] = status; }
    public void SetCurrentSelection (Selection sel)
    { curSelection = sel; }
    public void SetLastSquareSelection (Selection sel)
    { lastSquareSelection = sel; }
    public void SetLastClueSelection (Selection sel)
    { lastClueSelection = sel; }
    public void SetPuzzleRowCol (int row, int col, PuzzleSquare ps)
    {
        puzzle[row][col].CloneStatusFromSquare(ps);
        puzzle_backup[row][col].CloneStatusFromSquare(puzzle[row][col]);
    }
    public void SetPuzzleRowCol (int row, int col, PuzzleSquare.SquareStatus status)
    {
        puzzle[row][col].SetStatus (status);
        puzzle_backup[row][col].CloneStatusFromSquare(puzzle[row][col]);
    }
    public void SetPuzzleRowCol (int row, int col, int guess_level)
    {
        puzzle[row][col].SetGuessLevel(guess_level);
        puzzle_backup[row][col].CloneStatusFromSquare(puzzle[row][col]);
    }
    public void SetPuzzleRowCol (int row, int col, PuzzleSquare.SquareStatus status, int guess_level)
    {
        puzzle[row][col].SetGuessLevel(guess_level);
        puzzle[row][col].SetStatus (status);
        puzzle_backup[row][col].CloneStatusFromSquare(puzzle[row][col]);
    }
    public void SetPuzzleRowColSpecialMarked (int row, int col, boolean special_marked)
    {
        puzzle[row][col].SetSpecialMarked(special_marked);
        puzzle_backup[row][col].CloneStatusFromSquare(puzzle[row][col]);
    }

    public int GetGuessLevel ()
    { return guess_level; }
    public int GetCols ()
    { return cols; }
    
    public int GetRows ()
    { return rows; }

    public int GetMax_Row_Clues ()
    { return max_row_clues; }

    public int GetMax_Col_Clues ()
    { return max_col_clues; }

    public int GetCol_NClues (int col)
    { return col_nclues[col]; }

    public int[] GetCol_NClues ()
    { return col_nclues; }

    public int GetRow_NClues (int row)
    { return row_nclues[row]; }

    public int[] GetRow_NClues ()
    { return row_nclues; }

    public int GetCol_Clues (int col, int clue_num)
    { return col_clues[col][clue_num]; }
    public int[] GetCol_Clues (int col)
    { return col_clues[col]; }

    public int GetRow_Clues (int row, int clue_num)
    { return row_clues[row][clue_num]; }
    public int[] GetRow_Clues (int row)
    { return row_clues[row]; }
	
	public int GetTotalColClues ()
	{
		int total = 0;
		for (int i=0; i<cols; i++)
		{
			int num_clues = GetCol_NClues(i);
			for (int j=0; j<num_clues; j++)
				total += this.GetCol_Clues (i, j);
		}
		return total;
	}
	public int GetTotalRowClues ()
	{
		int total = 0;
		for (int i=0; i<rows; i++)
		{
			int num_clues = GetRow_NClues(i);
			for (int j=0; j<num_clues; j++)
				total += this.GetRow_Clues (i, j);
		}
		return total;
	}
	

    public int GetCol_Clue_Status (int col, int clue_num)
    {
        int[][] col_clue_stat = GetCol_ClueStat ();
        return col_clue_stat[col][clue_num];
    }

    public int GetRow_Clue_Status (int row, int clue_num)
    {
        int[][] row_clue_stat = GetRow_ClueStat ();
        return row_clue_stat[row][clue_num];
    }

    public PuzzleSquare GetPuzzleSquareAt (int row, int col)
    { return puzzle[row][col]; }
    public PuzzleSquare GetBackupPuzzleSquareAt (int row, int col)
    { return puzzle_backup[row][col]; }
    private int[][] GetCol_ClueStat ()
    { return col_clue_status; }
    private int[][] GetRow_ClueStat ()
    { return row_clue_status; }
    private PuzzleSquare[][] GetPuzzle ()
    { return puzzle; }
    public Selection GetCurrentSelection ()
    { return curSelection; }
    public String GetSource ()
    { return source; }

    // return status of puzzle square
    public int GetSquareStatusAsInt (int col, int row)
    {
        if (col < 0 || col >= cols || row < 0 || row >= rows) return PuzzleSquare.UNKNOWN;
        else return PuzzleSquare.StatusToInt(puzzle[row][col]);
    }
    public int GetSquareStatus (Point sq)
    {
        int col = sq.x;
        int row = sq.y;
        return GetSquareStatusAsInt (col, row);
    }
    // return status of column clue
    public int GetColClueStatus (int col, int n)
    {
        if (col < 0 || col >= cols) return UNACCOUNTEDFOR;
        if (n < 0 || n >= col_nclues[col]) return UNACCOUNTEDFOR;
        return col_clue_status[col][n];
    }
    // return status of row clue
    public int GetRowClueStatus (int row, int n)
    {
        if (row < 0 || row >= rows) return UNACCOUNTEDFOR;
        if (n < 0 || n >= row_nclues[row]) return UNACCOUNTEDFOR;
        return row_clue_status[row][n];
    }

    // Set status of a puzzle square temporarily
    // (this is for click-n-drag so we're going to draw the square)
    public void SetSquareStatusTemp (int col, int row, int status)
    {
		if (col < 0 || col >= cols || row < 0 || row >= rows) return;
        puzzle[row][col].SetStatusFromInt(status);
        DrawPuzzleBox (row, col, true, false);
    }

    // Restore puzzle square status from puzzle_backup
    // (this is for click-n-drag so we're going to draw the square)
    public void RestoreSquareStatusFromBackup (int col, int row)
    {
		if (col < 0 || col >= cols || row < 0 || row >= rows) return;
        puzzle[row][col].CloneStatusFromSquare(puzzle_backup[row][col]);
        DrawPuzzleBox (row, col, true, false);
    }

    // Finalize puzzle square status (i.e. save status into the backup puzzle)
    // and add to the Undo Stack
    public void FinalizeSquareStatusToBackup (int col, int row, boolean newID)
    {
		if (col < 0 || col >= cols || row < 0 || row >= rows) return;
        Selection tempSelection = new Selection();
        int priorStatus = puzzle_backup[row][col].GetStatusAsInt();
        boolean marked = puzzle_backup[row][col].IsMarked();
        boolean special_marked = puzzle_backup[row][col].IsSpecialMarked();
        if (marked) puzzle[row][col].SetMarkedStatus(true);
        if (special_marked) puzzle[row][col].SetSpecialMarked (true);
        puzzle_backup[row][col].CloneStatusFromSquare(puzzle[row][col]);
        tempSelection.setRowColSelected(row, col, puzzle_backup[row][col].GetStatusAsInt());
        DrawPuzzleBox (tempSelection, true, false);
        AddToStack (tempSelection, priorStatus, newID);
    }

    public boolean isAClueSelected ()
    {
        return curSelection.isAClueSelected();
    }

    public boolean isAPuzzleSquareSelected ()
    {
        return curSelection.isAPuzzleSquareSelected();
    }

    public void GoToLastSquareSelection ()
    {
        Selection oldSelection = new Selection (curSelection);
        curSelection.copySelection (lastSquareSelection);
        if (!curSelection.isEqual (oldSelection))
            DrawSelection (oldSelection, false);
        DrawSelection (curSelection, true);
    }

    public void GoToLastClueSelection ()
    {
        Selection oldSelection = new Selection (curSelection);
        curSelection.copySelection (lastClueSelection);
        if (!curSelection.isEqual (oldSelection))
            DrawSelection (oldSelection, false);
        DrawSelection (curSelection, true);
    }

    // Set puzzle square to the current square
    public void SetCurrentSelection (int col, int row)
    {
        Selection oldSelection = new Selection(curSelection);
        curSelection.setRowColSelected (row, col, PuzzleSquare.StatusToInt(puzzle[row][col]));
        lastSquareSelection.copySelection(curSelection);
        boolean sameSelection = oldSelection.isEqual (curSelection);
        if (!sameSelection) DrawSelection (oldSelection, false);
        DrawSelection (curSelection, true);
    }

    // Set column clue as the current selection
    public void SetCurrentColClueSelection (int col, int clue)
    {
        Selection oldSelection = new Selection(curSelection);
        curSelection.setColClueSelected(col, clue, col_clue_status[col][clue]);
        lastClueSelection.copySelection(curSelection);
        boolean sameSelection = oldSelection.isEqual (curSelection);
        if (!sameSelection) DrawSelection (oldSelection, false);
        DrawSelection (curSelection, true);
    }

    // Set row clue as the current selection
    public void SetCurrentRowClueSelection (int row, int clue)
    {
        Selection oldSelection = new Selection(curSelection);
        curSelection.setRowClueSelected(row, clue, row_clue_status[row][clue]);
        lastClueSelection.copySelection(curSelection);
        boolean sameSelection = oldSelection.isEqual (curSelection);
        if (!sameSelection) DrawSelection (oldSelection, false);
        DrawSelection (curSelection, true);
    }
	
	public void CycleCurSelection ()
	{
		if (curSelection == null) return;
		if (curSelection.somethingSelected())
		{
            int prior_status = curSelection.getStatus();
			int row = curSelection.getRowSelected();
			int col = curSelection.getColSelected();
			int row_clue = curSelection.getClueRowSelected();
			int col_clue = curSelection.getClueColSelected();
			int clue_num = curSelection.getClueNumSelected();
			
			// Cycle puzzle
			if (row >= 0 && row < rows && col >= 0 && col < cols)
				CyclePuzzle (row, col);			
			// Cycle column clue
			else if (col_clue >= 0 && col_clue < cols)
				CycleColClue (col_clue, clue_num);
            // Cycle row clue
			else if (row_clue >= 0 && row_clue < rows)
				CycleRowClue (row_clue, clue_num);
            curSelection.UpdateStatusFromPuzzle (this);
            if (row >= 0 && row < rows && col >= 0 && col < cols)
                lastSquareSelection.copySelection(curSelection);
			DrawSelection(curSelection, false);
            AddToStack (curSelection, prior_status, true);
		}
	}

    public void HandleCycleColClue (int col, int clue)
    {
        Selection oldSelection = new Selection (curSelection);
        int priorStatus = col_clue_status[col][clue];
        curSelection.setColClueSelected(col, clue, col_clue_status[col][clue]);
        lastClueSelection.copySelection (curSelection);
        boolean sameSelection = oldSelection.isEqual(curSelection);
        if (sameSelection)
        {
            CycleColClue(col, clue);
            curSelection.setColClueSelected(col, clue, col_clue_status[col][clue]);
            lastClueSelection.copySelection (curSelection);
            AddToStack(curSelection, priorStatus, true);
        }
        if (!sameSelection) DrawSelection (oldSelection, false);
        DrawSelection (curSelection, true);
    }
    
	// cycle between states for a given column clue
	private void CycleColClue (int col, int clue_num)
	{
		int status = col_clue_status[col][clue_num];
		int new_status = 0;
		switch (status)
		{
		case (UNACCOUNTEDFOR):
			new_status = ACCOUNTEDFOR;
			break;
		case (ACCOUNTEDFOR):
			new_status = POSSIBLYACCOUNTEDFOR;
			break;
		default:
			new_status = UNACCOUNTEDFOR;
			break;
		}
		col_clue_status[col][clue_num] = new_status;
	}

    public void HandleCycleRowClue (int row, int clue)
    {
        Selection oldSelection = new Selection (curSelection);
        int priorStatus = row_clue_status[row][clue];
        curSelection.setRowClueSelected(row, clue, row_clue_status[row][clue]);
        lastClueSelection.copySelection (curSelection);
        boolean sameSelection = oldSelection.isEqual(curSelection);
        if (sameSelection)
        {
            CycleRowClue(row, clue);
            curSelection.setRowClueSelected(row, clue, row_clue_status[row][clue]);
            lastClueSelection.copySelection (curSelection);
            AddToStack (curSelection, priorStatus, true);
        }
        if (!sameSelection) DrawSelection (oldSelection, false);
        DrawSelection (curSelection, true);
    }
	
	// cycle between states for a given column clue
	private void CycleRowClue (int row, int clue_num)
	{
		int status = row_clue_status[row][clue_num];
		int new_status = 0;
		switch (status)
		{
		case (UNACCOUNTEDFOR):
			new_status = ACCOUNTEDFOR;
			break;
		case (ACCOUNTEDFOR):
			new_status = POSSIBLYACCOUNTEDFOR;
			break;
		default:
			new_status = UNACCOUNTEDFOR;
			break;
		}
		row_clue_status[row][clue_num] = new_status;
	}
    	
	// handle mouse click events
	public void HandleCyclePuzzleRect (int row, int col)
	{
        if (col < 0 || col >= cols || row < 0 || row >= rows) return;
        int priorStatus = puzzle[row][col].GetStatusAsInt();
//		boolean was_marked = puzzle[row][col].IsSpecialMarked();
        Selection oldSelection = new Selection (curSelection);
        curSelection.setRowColSelected(row, col, puzzle[row][col].GetStatusAsInt());
        lastSquareSelection.copySelection (curSelection);
        boolean sameSelection = oldSelection.isEqual(curSelection);
        if (sameSelection) 
        {
            CyclePuzzle (row, col);
//			boolean is_marked = puzzle[row][col].IsSpecialMarked();
            curSelection.setRowColSelected(row, col, puzzle[row][col].GetStatusAsInt());
            lastSquareSelection.copySelection (curSelection);
            AddToStack(curSelection, priorStatus, true);
        }
        if (!sameSelection) DrawSelection (oldSelection, false);
        DrawSelection (curSelection, true);
	}

	// cycle between UNKNOWN, filled, empty, MARKED, etc.
	private void CyclePuzzle (int row, int col)
	{
		if (row < 0 || row >= rows || col < 0 || col >= cols) return;
        // only cycle if UNKNOWN or (KNOWN and guess level matches)
        if (myDrawHandler.CanModifySquare(row, col))
        {
            // set new status AND apply current guess level
            puzzle[row][col].CycleStatus();
			if (puzzle[row][col].IsUnknown())
			{
				boolean is_marked = puzzle[row][col].IsSpecialMarked();
				if (is_marked) puzzle[row][col].SetSpecialMarked(false);
			} else
			{
				puzzle[row][col].SetGuessLevel (guess_level);
				int cnt = this.GetSquaresAtGuessLevel(guess_level);
				if (mark_first_selection && cnt == 1)
					puzzle[row][col].SetSpecialMarked (true);
			}
            puzzle_backup[row][col].CloneStatusFromSquare(puzzle[row][col]);
        }
	}

    public void HandleNoSelection ()
    {
        Selection oldSelection = new Selection (curSelection);
        curSelection.setNothingSelected();
        if (!oldSelection.isEqual(curSelection))
            DrawSelection (oldSelection, false);
        DrawSelection (curSelection, true);
    }
	
	public void RemoveMarks ()
	{
		int status;
        boolean first = true;
		for (int i=0; i<rows; i++)
		{
			for (int j=0; j<cols; j++)
			{
				boolean is_marked = puzzle[i][j].IsMarked() || puzzle[i][j].IsSpecialMarked();
				if (is_marked)
				{
					int prior_status = puzzle[i][j].GetStatusAsInt();
					puzzle[i][j].SetMarkedStatus(false);
					puzzle[i][j].SetSpecialMarked(false);
					status = puzzle[i][j].GetStatusAsInt();
					puzzle_backup[i][j].CloneStatusFromSquare(puzzle[i][j]);
					Selection sel = new Selection();
					sel.setRowColSelected(i, j, status);
					AddToStack (sel, prior_status, first);
					first = false;
				}
			}
		}
	}


	public void commitGuesses()
	{
		boolean is_guess;
        int status;
        int count = 0;
		for (int i=0; i<rows; i++)
		{
			for (int j=0; j<cols; j++)
			{
				status = puzzle[i][j].GetStatusAsInt();
                int prior_status = status;
                is_guess = puzzle[i][j].IsGuess();
				if (is_guess)
				{
					puzzle[i][j].SetNotAGuess();
                    puzzle[i][j].SetSpecialMarked(false);
                    puzzle_backup[i][j].CloneStatusFromSquare(puzzle[i][j]);
					DrawPuzzleBox (null, i, j, false);
                    if (status != prior_status)
                    {
                        Selection sel = new Selection();
                        sel.setRowColSelected(i, j, status);
                        AddToStack (sel, prior_status, count==0);
                        count++;
                    }
				}
			}
		}
		DrawPuzzleBoldLines(null);
		guess_level = 0;
        myDrawHandler.SetGuessingControlItems(guess_level);
	}
	
	public void UndoLastGuess(boolean assume_guess_wrong)
	{
        int level = GetMaxGuessLevel();
		if (level < 0) return;

        // look for the special-marked square at this guess level
        // and hang on to its value
        PuzzleSquare first_guess_ps = null;
        int first_guess_row = -1;
        int first_guess_col = -1;
        for (int i=0; i<rows; i++)
            for (int j=0; j<cols; j++)
                if (puzzle[i][j].IsSpecialMarked() &&
                    puzzle[i][j].GetGuessLevel() == level)
                {
                    first_guess_ps = new PuzzleSquare(puzzle[i][j]);
                    first_guess_row = i;
                    first_guess_col = j;
                }

        // undo the last guess as usual
		int count = 0;
        boolean marked;
		for (int i=0; i<rows; i++)
		{
			for (int j=0; j<cols; j++)
			{
                int gl = puzzle[i][j].GetGuessLevel();
                int prior_status = puzzle[i][j].GetStatusAsInt();
                if (gl == level && !puzzle[i][j].IsUnknown())
                {
                    marked = puzzle[i][j].IsMarked();

                    puzzle[i][j].Reset();
                    puzzle[i][j].SetMarkedStatus(marked);
                    puzzle_backup[i][j].CloneStatusFromSquare(puzzle[i][j]);
                    DrawPuzzleBox (null, i, j, false);
                    Selection sel = new Selection ();
                    sel.setRowColSelected(i, j, puzzle[i][j].GetStatusAsInt());
                    AddToStack (sel, prior_status, count == 0);
                    count++;
                }
			}
		}

        // the first guess must have been wrong, so set the first
        // pixel to the opposite state and set the guess level to
        // one less than the level just undone
        if (first_guess_ps != null)
        {
            if (assume_guess_wrong)
            {
                PuzzleSquare.SquareStatus ss = first_guess_ps.GetStatus();
                if (ss != PuzzleSquare.SquareStatus.UNKNOWN)
                {
                    if (ss == PuzzleSquare.SquareStatus.EMPTY)
                        puzzle[first_guess_row][first_guess_col].SetStatus(PuzzleSquare.SquareStatus.FILLED);
                    else
                        puzzle[first_guess_row][first_guess_col].SetStatus(PuzzleSquare.SquareStatus.EMPTY);
                    puzzle[first_guess_row][first_guess_col].SetGuessLevel (level -1);
                } else
                {
                    puzzle[first_guess_row][first_guess_col].SetGuessLevel (0);
                }
            } else
				puzzle[first_guess_row][first_guess_col].SetGuessLevel (0);
			
            puzzle[first_guess_row][first_guess_col].SetSpecialMarked(false);
            puzzle_backup[first_guess_row][first_guess_col].CloneStatusFromSquare(
                    puzzle[first_guess_row][first_guess_col]);
            DrawPuzzleBox (null, first_guess_row, first_guess_col, false);
        }

        // draw puzzle as usual
		DrawPuzzleBoldLines(null);

        // Reset guess level to one lower IF assume_guess_wrong was false
        // otherwise keep guess level at the level that was just undone
        // so user can make a new guess.  Also reset the mark_first_selection
        // boolean if it's on in the Controls GUI
        if (!assume_guess_wrong) guess_level = level - 1;
        else guess_level = level;
		myDrawHandler.SetGuessingControlItems(guess_level);
//        mark_first_selection = false;
//        if (myDrawHandler.GetAutoMarkStart() && guess_level > 0
//            && GetSquaresAtGuessLevel(guess_level) == 0) mark_first_selection = true;
		return;
	}
	
	// return number of boxes undone (if guess level is incorrect,
	// returns positive number so calling program thinks that the call
	// was successful)
	public void clearPuzzle()
	{
        int prior_status;
        int count = 0;
		for (int i=0; i<rows; i++)
		{
			for (int j=0; j<cols; j++)
            {
                prior_status = puzzle[i][j].GetStatusAsInt();
				puzzle[i][j].Reset();
                puzzle_backup[i][j].Reset();
                if (prior_status != PuzzleSquare.UNKNOWN)
                {
                    Selection sel = new Selection();
                    sel.setRowColSelected(i, j, PuzzleSquare.UNKNOWN);
                    AddToStack (sel, prior_status, count==0);
                    count++;
                }
            }
			for (int j=0; j<row_nclues[i]; j++)
            {
                prior_status = row_clue_status[i][j];
				row_clue_status[i][j] = UNACCOUNTEDFOR;
                if (prior_status != UNACCOUNTEDFOR)
                {
                    Selection sel = new Selection();
                    sel.setRowClueSelected (i, j, UNACCOUNTEDFOR);
                    AddToStack (sel, prior_status, count==0);
                    count++;
                }
            }
		}
		for (int i=0; i<cols; i++)
			for (int j=0; j<col_nclues[i]; j++)
            {
                prior_status = col_clue_status[i][j];
				col_clue_status[i][j] = UNACCOUNTEDFOR;
                if (prior_status != UNACCOUNTEDFOR)
                {
                    Selection sel = new Selection();
                    sel.setColClueSelected (i, j, UNACCOUNTEDFOR);
                    AddToStack (sel, prior_status, count==0);
                    count++;
                }
            }
//		DrawPuzzle(null, null);
		myDrawHandler.Redraw();
		guess_level = 0;
        myDrawHandler.SetGuessingControlItems(guess_level);
		return;
	}
	
	public void CurSelectionDown ()
	{
		if (curSelection == null) return;
		if (curSelection.somethingSelected())
		{
			Selection oldSelection = new Selection(curSelection);
			int row = curSelection.getRowSelected();
			int col = curSelection.getColSelected();
			int clue_col = curSelection.getClueColSelected();
			int clue_row = curSelection.getClueRowSelected();
			int clue_num = curSelection.getClueNumSelected();
			if (row >= 0 && row < rows && col >= 0 && col < cols)
			{
				if (row < (rows-1))
				{
					row++;
					curSelection.setRowColSelected(row, col, puzzle[row][col].GetStatusAsInt());
                    lastSquareSelection.copySelection (curSelection);
				}
			} else if (clue_col >= 0 && clue_col < cols)
			{
				clue_num++;
				if (clue_num == col_nclues[clue_col])
                {
					curSelection.setRowColSelected (0, clue_col, puzzle[0][clue_col].GetStatusAsInt());
                    lastSquareSelection.copySelection (curSelection);
                } else
                {
					curSelection.setColClueSelected (clue_col, clue_num, col_clue_status[clue_col][clue_num]);
                    lastClueSelection.copySelection (curSelection);
                }
                } else if (clue_row >= 0 && clue_row < rows)
			{
				if (clue_row < (rows-1))
				{
					int n = clue_num + (max_row_clues - row_nclues[clue_row]);
					clue_row++;
					clue_num = n - (max_row_clues - row_nclues[clue_row]);
					if (clue_num < 0) clue_num = 0;
					curSelection.setRowClueSelected(clue_row, clue_num, row_clue_status[clue_row][clue_num]);
                    lastClueSelection.copySelection (curSelection);
                }
			}
			if (!curSelection.isEqual(oldSelection))
			{
				DrawSelection(oldSelection, false);
				DrawSelection(curSelection, true);
			}
		}
	}
	
	public void CurSelectionUp ()
	{
		boolean draw_bold = false;
		if (curSelection == null) return;
		if (curSelection.somethingSelected())
		{
			Selection oldSelection = new Selection(curSelection);
			int row = curSelection.getRowSelected();
			int col = curSelection.getColSelected();
			int clue_col = curSelection.getClueColSelected();
			int clue_row = curSelection.getClueRowSelected();
			int clue_num = curSelection.getClueNumSelected();
			if (row >= 0 && row < rows && col >= 0 && col < cols)
			{
				if (row > 0)
				{
					row--;
					curSelection.setRowColSelected(row, col, puzzle[row][col].GetStatusAsInt());
                    lastSquareSelection.copySelection(curSelection);
				} else
				{
					int nclues = col_nclues[col];
					clue_num = nclues-1;
					curSelection.setColClueSelected(col, clue_num, col_clue_status[col][clue_num]);
                    lastClueSelection.copySelection (curSelection);
                    draw_bold = true;
				}
			} else if (clue_col >= 0 && clue_col < cols)
			{
				if (clue_num > 0)
				{
					clue_num--;
					curSelection.setColClueSelected (clue_col, clue_num, col_clue_status[clue_col][clue_num]);
                    lastClueSelection.copySelection (curSelection);
                }
			} else if (clue_row >= 0 && clue_row < rows)
			{
				if (clue_row > 0)
				{
					int n = clue_num + (max_row_clues - row_nclues[clue_row]);
					clue_row--;
					clue_num = n - (max_row_clues - row_nclues[clue_row]);
					if (clue_num < 0) clue_num = 0;
					curSelection.setRowClueSelected(clue_row, clue_num, row_clue_status[clue_row][clue_num]);
                    lastClueSelection.copySelection (curSelection);
                }
			}
			if (!curSelection.isEqual(oldSelection))
			{
				DrawSelection(oldSelection, false);
				DrawSelection(curSelection, true);
				if (draw_bold) DrawPuzzleBoldLines(null);
			}
		}
	}
	
	public void CurSelectionRight ()
	{
		if (curSelection == null) return;
		if (curSelection.somethingSelected())
		{
			Selection oldSelection = new Selection(curSelection);
			int row = curSelection.getRowSelected();
			int col = curSelection.getColSelected();
			int clue_col = curSelection.getClueColSelected();
			int clue_row = curSelection.getClueRowSelected();
			int clue_num = curSelection.getClueNumSelected();
			if (row >= 0 && row < rows && col >= 0 && col < cols)
			{
				if (col < (cols-1))
				{
					col++;
					curSelection.setRowColSelected(row, col, puzzle[row][col].GetStatusAsInt());
                    lastSquareSelection.copySelection (curSelection);
				}
			} else if (clue_col >= 0 && clue_col < cols)
			{
				if (clue_col < (cols-1))
				{
					int n = clue_num + (max_col_clues - col_nclues[clue_col]);
					clue_col++;
					clue_num = n - (max_col_clues - col_nclues[clue_col]);
					if (clue_num < 0) clue_num = 0;
					curSelection.setColClueSelected (clue_col, clue_num, col_clue_status[clue_col][clue_num]);
                    lastClueSelection.copySelection (curSelection);
                }
			} else if (clue_row >= 0 && clue_row < rows)
			{
				if (clue_num < (row_nclues[clue_row]-1))
				{
					clue_num++;
					curSelection.setRowClueSelected (clue_row, clue_num, row_clue_status[clue_row][clue_num]);
                    lastClueSelection.copySelection (curSelection);
                } else
                {
					curSelection.setRowColSelected (clue_row, 0, puzzle[clue_row][0].GetStatusAsInt());
                    lastSquareSelection.copySelection (curSelection);
                }
			}
			if (!curSelection.isEqual(oldSelection))
			{
				DrawSelection(oldSelection, false);
				DrawSelection(curSelection, true);
			}
		}
	}
	
	public void CurSelectionLeft ()
	{
		boolean draw_bold = false;
		if (curSelection == null) return;
		if (curSelection.somethingSelected())
		{
			Selection oldSelection = new Selection(curSelection);
			int row = curSelection.getRowSelected();
			int col = curSelection.getColSelected();
			int clue_col = curSelection.getClueColSelected();
			int clue_row = curSelection.getClueRowSelected();
			int clue_num = curSelection.getClueNumSelected();
			if (row >= 0 && row < rows && col >= 0 && col < cols)
			{
				if (col > 0)
				{
					col--;
					curSelection.setRowColSelected(row, col, puzzle[row][col].GetStatusAsInt());
                    lastSquareSelection.copySelection (curSelection);
				} else {
					curSelection.setRowClueSelected(row, row_nclues[row]-1, row_clue_status[row][row_nclues[row]-1]);
                    lastClueSelection.copySelection (curSelection);
                    draw_bold = true;
				}
			} else if (clue_col >= 0 && clue_col < cols)
			{
				if (clue_col > 0)
				{
					int n = clue_num + (max_col_clues - col_nclues[clue_col]);
					clue_col--;
					clue_num = n - (max_col_clues - col_nclues[clue_col]);
					if (clue_num < 0) clue_num = 0;
					curSelection.setColClueSelected (clue_col, clue_num, col_clue_status[clue_col][clue_num]);
                    lastClueSelection.copySelection (curSelection);
                }
			} else if (clue_row >= 0 && clue_row < rows)
			{
				if (clue_num > 0)
				{
					clue_num--;
					curSelection.setRowClueSelected (clue_row, clue_num, row_clue_status[clue_row][clue_num]);
                    lastClueSelection.copySelection (curSelection);
                }
			}
			if (!curSelection.isEqual(oldSelection))
			{
				DrawSelection(oldSelection, false);
				DrawSelection(curSelection, true);
				if (draw_bold) DrawPuzzleBoldLines(null);
			}
		}
	}

    private void UpdateStatusFromSelection (Selection theSelection)
    {
        int row = theSelection.getRowSelected();
        int col = theSelection.getColSelected();
        int rowclue = theSelection.getClueRowSelected();
        int colclue = theSelection.getClueColSelected();
        int n = theSelection.getClueNumSelected();
        int status = theSelection.getStatus();
        if (row >= 0 && row < rows && col >= 0 && col < cols)
        {
            puzzle[row][col].SetStatusFromInt(status);
            puzzle_backup[row][col].SetStatusFromInt(status);
        } else if (rowclue >= 0)
        {
            row_clue_status[rowclue][n] = status;
        } else if (colclue >= 0)
        {
            col_clue_status[colclue][n] = status;
        }
    }

    public void AddToStack (Selection theSelection, int priorStatus, boolean newID)
    {
        if (theSelection == null) return;
        if (theSelection.getStatus() == priorStatus)return;

		StackSelection newStackSelection = new StackSelection(theSelection, priorStatus, newID);
		boolean doAdd = true;
		if (undoStack.size() > 0)
		{
			StackSelection peekStackSelection = (StackSelection)undoStack.peek();
			if (peekStackSelection.isExactlyEqual(newStackSelection)) doAdd = false;
		}
		if (doAdd)
		{
			if (undoStack.size() >= MAX_STACK_SIZE)
			{
				// remove last element of the stack (element 0)
				undoStack.remove(0);
				undoStack.push(newStackSelection);
			} else
			{
				undoStack.add(newStackSelection);
			}
            myDrawHandler.SetUndoItemEnabled (true);
		}
    }
	
	public void PopFromStack()
	{
        int temp_guess_level;
        int new_guess_level = 0;
		if (undoStack.size() > 0)
		{
			Selection oldSelection = new Selection(curSelection);
            StackSelection curStackSelection = (StackSelection)undoStack.pop();
            int id = curStackSelection.getID();
			curSelection = curStackSelection.getSelection();
            curSelection.setStatus (curStackSelection.getPriorStatus());
            temp_guess_level = this.GetGuessLevelFromSelection(curSelection);
            if (temp_guess_level > new_guess_level) new_guess_level = temp_guess_level;
            UpdateStatusFromSelection (curSelection);
			if (!oldSelection.isEqual(curSelection)) DrawSelection(oldSelection, false);
			DrawSelection(curSelection, true);

            // Look for any more stack selections with the same ID
            boolean looking = true && !undoStack.isEmpty();
            while (looking)
            {
                StackSelection peekStackSelection = (StackSelection)undoStack.peek();
                if (id == peekStackSelection.getID())
                {
                    oldSelection = new Selection(curSelection);
                    curStackSelection = (StackSelection)undoStack.pop();
                    curSelection = curStackSelection.getSelection();
                    curSelection.setStatus (curStackSelection.getPriorStatus());
                    temp_guess_level = this.GetGuessLevelFromSelection(curSelection);
                    if (temp_guess_level > new_guess_level) new_guess_level = temp_guess_level;
                    UpdateStatusFromSelection (curSelection);
                    if (!oldSelection.isEqual(curSelection))
                        DrawSelection(oldSelection, false);
                    DrawSelection(curSelection, true);
                    looking = !undoStack.isEmpty();
                } else looking = false;
            }

            // Update guess level
            if (new_guess_level > guess_level)
            {
                guess_level = new_guess_level;
                myDrawHandler.SetGuessingControlItems(guess_level);
            }
		} else
            myDrawHandler.SetUndoItemEnabled(false);
	}

    private int GetGuessLevelFromSelection (Selection sel)
    {
        PuzzleSquare ps = new PuzzleSquare (sel.getStatus());
        int row = sel.getRowSelected();
        int col = sel.getColSelected();
        if (row < 0 || row >= rows || col < 0 || col >= cols) return 0;
        return (ps.GetGuessLevel());
    }

    public boolean checkPuzzleCol (int col, boolean top_to_bottom, boolean rigorous)
    {
        boolean noErrors = true;

        int stripped_status[] = new int[rows];
        int groups_status[] = new int[rows];
        int groups_num[] = new int[rows];
        int groups_row[] = new int[rows];
        int ngroups = 0;

        // Strip the LOCKED and MARKED and guess level for the entire row
        for (int i=0; i<rows; i++)
            stripped_status[i] = PuzzleSquare.StripSpecialMarkedMarkedGuess (puzzle[i][col]);

        // Accumulate a list of groupings from top to bottom or vice-versa
        int start_row, end_row, row_incr;
        if (top_to_bottom)
        {
            start_row = 0;
            end_row = rows;
            row_incr = 1;
        } else
        {
            start_row = rows-1;
            end_row = -1;
            row_incr = -1;
        }
        ngroups = 1;
        groups_status[ngroups-1] = stripped_status[start_row];
        groups_num[ngroups-1] = 0;
        groups_row[ngroups-1] = start_row;
        int prior_status = stripped_status[start_row];
        for (int i=start_row; i!=end_row; i+=row_incr)
        {
            // See if we have a new group
            if (stripped_status[i] != prior_status)
            {
                ngroups++;
                groups_status[ngroups-1] = stripped_status[i];
                groups_num[ngroups-1] = 1;
                groups_row[ngroups-1] = i;
                prior_status = stripped_status[i];
            } else
            {
                groups_num[ngroups-1]++;
            }
        }

        // Check for no-clue situation
        if (col_nclues[col] == 0)
        {
            for (int i=0; i<ngroups; i++)
            {
                if (groups_status[ngroups] != PuzzleSquare.UNKNOWN)
                {
                    if (top_to_bottom)
                        SetCurrentSelection (col, 0);
                    else
                        SetCurrentSelection (col, rows-1);
                    JOptionPane.showMessageDialog (null,
                        "There should be no marked squares in col " + col,
                        "Check Puzzle Column Error",
                        JOptionPane.INFORMATION_MESSAGE, null);
                    return false;
                }
            }
        }

        else
        {

            int start_clue;
            int end_clue;
            int clue_incr;
            if (top_to_bottom)
            {
                start_clue = 0;
                end_clue = col_nclues[col];
                clue_incr = 1;
            } else
            {
                start_clue = col_nclues[col]-1;
                end_clue = -1;
                clue_incr = -1;
            }
            int group = 0;
            int nknown = 0;
            for (int i=start_clue; i!=end_clue; i+=clue_incr)
            {
                // Search for this clue using groups of KNOWN squares
                boolean found = false;
                while (group < ngroups && !found)
                {
                    if (groups_status[group] != PuzzleSquare.UNKNOWN)
                    {
                        // Handle rigorous checking
                        if (rigorous)
                        {
                            if (groups_num[group] != col_clues[col][i])
                            {
                                SetCurrentSelection (col, groups_row[group]);
                                JOptionPane.showMessageDialog (null,
                                    "Wrong number of marked squares for clue " + (i+1) +
                                    " in col " + (col+1),
                                    "Check Puzzle Column Error",
                                    JOptionPane.INFORMATION_MESSAGE, null);
                                return false;
                            } else found = true;
                        // Handle non-rigorous checking
                        } else
                        {
                            // Check size of MARKED squares
                            nknown = groups_num[group];
                            if (nknown > col_clues[col][i])
                            {
                                SetCurrentSelection (col, groups_row[group]);
                                JOptionPane.showMessageDialog (null,
                                    "Too many marked squares for clue " + (i+1) +
                                    " in col " + (col+1),
                                    "Check Puzzle Column Error",
                                    JOptionPane.INFORMATION_MESSAGE, null);
                                return false;
                            }
                            // Check size of MARKED squares including adjacent UNKNOWN squares
                            if (group > 0 && groups_status[group-1] == PuzzleSquare.UNKNOWN)
                                nknown += groups_num[group-1];
                            if (group < (ngroups-1) && groups_status[group+1] == PuzzleSquare.UNKNOWN)
                                nknown += groups_num[group+1];
                            if (nknown < col_clues[col][i])
                            {
                                SetCurrentSelection (col, groups_row[group]);
                                JOptionPane.showMessageDialog (null,
                                    "Clue " + (i+1) + " in col " + (col+1) + " cannot be satisfied",
                                    "Check Puzzle Column Error",
                                    JOptionPane.INFORMATION_MESSAGE, null);
                                return false;
                            }
                            found = true;
                        }
                    }
                    group++;
                }
                if (!found)
                {
                    SetCurrentColClueSelection (col, i);
                    JOptionPane.showMessageDialog (null,
                        "Clue " + (i+1) + " in col " + (col+1) + " could not be found",
                        "Check Puzzle Column Error",
                        JOptionPane.INFORMATION_MESSAGE, null);
                    return false;
                }
            }
        }

        return noErrors;
    }

    public boolean checkPuzzleRow (int row, boolean left_to_right, boolean rigorous)
    {
        boolean noErrors = true;

        int stripped_status[] = new int[cols];
        int groups_status[] = new int[cols];
        int groups_num[] = new int[cols];
        int groups_col[] = new int[cols];
        int ngroups = 0;

        // Strip the LOCKED and MARKED and guess level for the entire row
        for (int i=0; i<cols; i++)
            stripped_status[i] = PuzzleSquare.StripSpecialMarkedMarkedGuess (puzzle[row][i]);

        // Accumulate a list of groupings from left to right
        int start_col, end_col, col_incr;
        if (left_to_right)
        {
            start_col = 0;
            end_col = cols;
            col_incr = 1;
        } else
        {
            start_col = cols-1;
            end_col = -1;
            col_incr = -1;
        }
        ngroups = 1;
        groups_status[ngroups-1] = stripped_status[start_col];
        groups_num[ngroups-1] = 0;
        groups_col[ngroups-1] = start_col;
        int prior_status = stripped_status[start_col];
        for (int i=start_col; i!=end_col; i+=col_incr)
        {
            // See if we have a new group
            if (stripped_status[i] != prior_status)
            {
                ngroups++;
                groups_status[ngroups-1] = stripped_status[i];
                groups_num[ngroups-1] = 1;
                groups_col[ngroups-1] = i;
                prior_status = stripped_status[i];
            } else
            {
                groups_num[ngroups-1]++;
            }
        }
        
        // Check for no-clue situation
        if (row_nclues[row] == 0)
        {
            for (int i=0; i<ngroups; i++)
            {
                if (groups_status[ngroups] != PuzzleSquare.UNKNOWN)
                {
                    if (left_to_right)
                        SetCurrentSelection (0, row);
                    else
                        SetCurrentSelection (cols-1, row);
                    JOptionPane.showMessageDialog (null,
                        "There should be no marked squares in row " + row,
                        "Check Puzzle Row Error",
                        JOptionPane.INFORMATION_MESSAGE, null);
                    return false;
                }
            }
        }

        else
        {

            int start_clue;
            int end_clue;
            int clue_incr;
            if (left_to_right)
            {
                start_clue = 0;
                end_clue = row_nclues[row];
                clue_incr = 1;
            } else
            {
                start_clue = row_nclues[row]-1;
                end_clue = -1;
                clue_incr = -1;
            }
            int group = 0;
            int nknown = 0;
            for (int i=start_clue; i!=end_clue; i+=clue_incr)
            {
                // Search for this clue using groups of KNOWN squares
                boolean found = false;
                while (group < ngroups && !found)
                {
                    if (groups_status[group] != PuzzleSquare.UNKNOWN)
                    {
                        // Handle rigorous checking
                        if (rigorous)
                        {
                            if (groups_num[group] != row_clues[row][i])
                            {
                                SetCurrentSelection (groups_col[group], row);
                                JOptionPane.showMessageDialog (null,
                                    "Wrong number of marked squares for clue " + (i+1) +
                                    " in row " + (row+1),
                                    "Check Puzzle Row Error",
                                    JOptionPane.INFORMATION_MESSAGE, null);
                                return false;
                            } else found = true;
                        // Handle non-rigorous checking
                        } else
                        {
                            // Check size of MARKED squares
                            nknown = groups_num[group];
                            if (nknown > row_clues[row][i])
                            {
                                SetCurrentSelection (groups_col[group], row);
                                JOptionPane.showMessageDialog (null,
                                    "Too many marked squares for clue " + (i+1) +
                                    " in row " + (row+1),
                                    "Check Puzzle Row Error",
                                    JOptionPane.INFORMATION_MESSAGE, null);
                                return false;
                            }
                            // Check size of MARKED squares including adjacent UNKNOWN squares
                            if (group > 0 && groups_status[group-1] == PuzzleSquare.UNKNOWN)
                                nknown += groups_num[group-1];
                            if (group < (ngroups-1) && groups_status[group+1] == PuzzleSquare.UNKNOWN)
                                nknown += groups_num[group+1];
                            if (nknown < row_clues[row][i])
                            {
                                SetCurrentSelection (groups_col[group], row);
                                JOptionPane.showMessageDialog (null,
                                    "Clue " + (i+1) + " in row " + (row+1) + " cannot be satisfied",
                                    "Check Puzzle Row Error",
                                    JOptionPane.INFORMATION_MESSAGE, null);
                                return false;
                            }
                            found = true;
                        }
                    }
                    group++;
                }
                if (!found)
                {
                    SetCurrentRowClueSelection (row, i);
                    JOptionPane.showMessageDialog (null,
                        "Clue " + (i+1) + " in row " + (row+1) + " could not be found",
                        "Check Puzzle Row Error",
                        JOptionPane.INFORMATION_MESSAGE, null);
                    return false;
                }
            }
        }

        return noErrors;
    }

    public boolean checkPuzzle ()
    {
        // Stop right away if any errors are found
        boolean noErrors = true;

        // Check all rows first
        for (int i=0; i<rows && noErrors; i++)
            noErrors = checkPuzzleRow (i, true, true);

        // Check all cols next
        for (int i=0; i<cols && noErrors; i++)
            noErrors = checkPuzzleCol (i, true, true);

        if (noErrors)
        {
            JOptionPane.showMessageDialog (null,
                "Your puzzle is perfect!",
                "Check Puzzle",
                JOptionPane.PLAIN_MESSAGE, null);
        }
        
        return noErrors;
    }

    public File GetFile ()
    { return myFile; }
    public void SetFile (File newFile)
    { myFile = newFile;  }

    private void UpdateStackSize ()
    {
        if (rows*cols > MAX_STACK_SIZE) MAX_STACK_SIZE = (rows*cols);
    }

    public int GetMaxGuessLevelWithKnownSquares ()
    {
        int maxguess = 0;
        for (int i=0; i<rows; i++)
            for (int j=0; j<cols; j++)
            {
                int level = puzzle[i][j].GetGuessLevel();
                if (level > maxguess && !puzzle[i][j].IsUnknown())
                    maxguess = level;
            }
        return maxguess;
    }
    
    public int GetMaxGuessLevel ()
    {
        int maxguess = GetMaxGuessLevelWithKnownSquares();
        return (maxguess < guess_level ? guess_level : maxguess);
    }
    
    private int GetSquaresAtGuessLevel (int guess_level)
    {
        int num = 0;
        for (int i=0; i<rows; i++)
            for (int j=0; j<cols; j++)
            {
                int level = puzzle[i][j].GetGuessLevel();
                if (level == guess_level && !puzzle[i][j].IsUnknown()) num++;
            }
        return num;
    }

    public int CountAdjacentFilledSquaresInCol (int row, int col)
    {
        int num = 0;
        boolean at_end = false;
        for (int r=row; r>=0 && !at_end; r--)
            if (puzzle[r][col].IsFilled()) num++;
            else at_end = true;
        if (row < (rows-1))
        {
            at_end = false;
            for (int r=(row+1); r<rows && !at_end; r++)
                if (puzzle[r][col].IsFilled()) num++;
                else at_end = true;
        }
        return num;
    }

    public int CountAdjacentFilledSquaresInRow (int row, int col)
    {
        int num = 0;
        boolean at_end = false;
        for (int c=col; c>=0 && !at_end; c--)
            if (puzzle[row][c].IsFilled()) num++;
            else at_end = true;
        if (col < (cols-1))
        {
            at_end = false;
            for (int c=(col+1); c<cols && !at_end; c++)
                if (puzzle[row][c].IsFilled()) num++;
                else at_end = true;
        }
        return num;
    }
	
	public int CountUnknownSquaresInRow (int row)
	{
		int total = 0;
		for (int c=0; c<cols; c++)
			if (puzzle[row][c].IsUnknown()) total++;
		return total;
	}
	
	public int CountUnknownSquaresInCol (int col)
	{
		int total = 0;
		for (int r=0; r<rows; r++)
			if (puzzle[r][col].IsUnknown()) total++;
		return total;
	}

    public int CountKnownSquares ()
    {
        int num = 0;
        for (int r=0; r<rows; r++)
            for (int c=0; c<cols; c++)
                if (!puzzle[r][c].IsUnknown()) num++;
       return num;
    }

    public int CountKnownSquares (int level)
    {
        int num = 0;
        for (int r=0; r<rows; r++)
            for (int c=0; c<cols; c++)
                if (!puzzle[r][c].IsUnknown() && puzzle[r][c].GetGuessLevel() == level) num++;
       return num;
    }

    public void SetBOX_SIZE(int size)
    { BOX_SIZE = size; }
    public int GetBOX_SIZE()
    { return BOX_SIZE; }
    public void SetClue_Height (int height)
    { clue_height = height; }
    public int GetClue_Height ()
    { return clue_height; }
    public void SetClue_Width (int width)
    { clue_width = width; }
    public int GetClue_Width ()
    { return clue_width; }
    public void SetFont_Size (int size)
    { font_size = size; }
    public int GetFont_Size()
    { return font_size; }
    
    public boolean CopyCluesFromPuzzle (PBNPuzzle fromPuzzle)
    {
        if (rows != fromPuzzle.GetRows() ||
            cols != fromPuzzle.GetCols())
        {
            PaintByNumberPro.HandleErrorMessage ("Copy Clues from Puzzle Error",
                    "Puzzle dimensions do not match!");
            return false;
        }
        
        // copy the # clues for each col and row
        max_row_clues = fromPuzzle.GetMax_Row_Clues();
        max_col_clues = fromPuzzle.GetMax_Col_Clues();
        for (int i=0; i<rows; i++) row_nclues[i] = fromPuzzle.GetRow_NClues(i);
        for (int i=0; i<cols; i++) col_nclues[i] = fromPuzzle.GetCol_NClues(i);
        for (int i=0; i<rows; i++)
        {
            if (row_nclues[i] > 0)
            {
                for (int j=0; j<row_nclues[i]; j++)
                {
                    row_clues[i][j] = fromPuzzle.GetRow_Clues(i, j);
                    row_clue_status[i][j] = fromPuzzle.GetRow_Clue_Status(i, j);
                }
            }
        }
        for (int i=0; i<cols; i++)
        {
            if (col_nclues[i] > 0)
            {
                for (int j=0; j<col_nclues[i]; j++)
                {
                    col_clues[i][j] = fromPuzzle.GetCol_Clues(i, j);
                    col_clue_status[i][j] = fromPuzzle.GetCol_Clue_Status(i, j);
                }
            }
        }
        return true;
    }
}
