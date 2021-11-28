/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package paintbynumberpro;

import java.util.*;
import java.awt.*;

/**
 *
 * @author user
 */
public class PuzzleSolver extends Thread {

    private static final String title = "Check Puzzle";
    private static final String UNKNOWN_ERROR = "Unknown error";
    private static String last_msg = null;

    private static boolean guess_vars_initialized = false;
    private static Random random = null;

    private static int cur_guess_col = 0;
    private static int cur_guess_row = 0;
    private static boolean incr_column_first = true;
    private static int incr_col_dir = 1;
    private static int incr_row_dir = 1;

    // vars for my spiral guess generator
    private static int spiral_start_row, spiral_start_col;
    private static boolean spiral_start_incr_column_first;
    private static int spiral_start_incr_dir;

    // vars for controlling detailed debugging and auto-solving
    private static boolean do_stop = false;
    
    // vars for reporting current col/row being operated on
    private static int current_col = -1;
    private static int current_row = -1;
    private static int current_guess_level = -1;
    private static String current_process = "";

    // ----------------------------------------------------------
    // Utility functions to transfer rows/cols to/from the puzzle
    // ----------------------------------------------------------

    private static PuzzleSquare[] CopyRowFromPuzzle (PBNPuzzle myPuzzle, int row)
    {
        PuzzleSquare[] newRow = new PuzzleSquare[myPuzzle.GetCols()];
        for (int i=0; i<myPuzzle.GetCols(); i++)
            newRow[i] = new PuzzleSquare(myPuzzle.GetPuzzleSquareAt (row, i));
        return newRow;
    }

    private static PuzzleSquare[] CopyColFromPuzzle (PBNPuzzle myPuzzle, int col)
    {
        PuzzleSquare[] newCol = new PuzzleSquare[myPuzzle.GetRows()];
        for (int i=0; i<myPuzzle.GetRows(); i++)
            newCol[i] = new PuzzleSquare(myPuzzle.GetPuzzleSquareAt (i, col));
        return newCol;
    }
    
    public static int[] GetCluesForRowFromPuzzle (PBNPuzzle myPuzzle, int row)
    {
        int nclues = myPuzzle.GetRow_NClues(row);      
        if (nclues == 0) return null;
        int myClues[] = new int[nclues];       
        for (int cl=0; cl<nclues; cl++) myClues[cl] = myPuzzle.GetRow_Clues (row, cl);    
        return myClues;
    }
    
    public static int[] GetCluesForColFromPuzzle (PBNPuzzle myPuzzle, int col)
    {
        int nclues = myPuzzle.GetCol_NClues(col);      
        if (nclues == 0) return null;
        int myClues[] = new int[nclues];       
        for (int cl=0; cl<nclues; cl++) myClues[cl] = myPuzzle.GetCol_Clues (col, cl);    
        return myClues;
    }

    private static void CopyRowToPuzzle (PBNPuzzle myPuzzle, PuzzleSquare[] myRow, int row)
    {
        CopyRowToPuzzleStartingFrom (myPuzzle, myRow, row, 0);
    }
    
    private static void CopyRowToPuzzleStartingFrom (PBNPuzzle myPuzzle, PuzzleSquare[] myRow, int row, int start_col)
    {
        if (myPuzzle == null || myRow == null) return;
        int num_squares = myRow.length;
        for (int i=0; i<num_squares; i++)
            myPuzzle.SetPuzzleRowCol(row, i+start_col, myRow[i]);
    }    

    private static void CopyColToPuzzle (PBNPuzzle myPuzzle, PuzzleSquare[] myCol, int col)
    {
        if (myPuzzle == null || myCol == null) return;
        for (int i=0; i<myPuzzle.GetRows(); i++)
            myPuzzle.SetPuzzleRowCol(i, col, myCol[i]);
    }

    private static boolean ProcessOverlaps (PuzzleSquare[] myRowCol, int start_index, int end_index,
            int[] clues, int guess_level)
    {
        if (myRowCol == null || clues == null) return false;

        // total up the clues plus minimum number of empty spaces
        int tot = 0;
        if (clues.length > 0)
        {
            for (int cl=0; cl<clues.length; cl++) tot += clues[cl];
            tot += (clues.length-1);
        } else return true;

        // get the number of slop spaces
        if (start_index > end_index) return false;
        int length = end_index - start_index + 1;
        int slop = length - tot;

        // if any of the clues are > the slop, then we can process the overlap
        boolean can_process = false;
        for (int cl=0; cl<clues.length; cl++) if (clues[cl] > slop) can_process = true;

        // if slop is too big, then we're done
        if (!can_process) return true;

        // now continue with processing, only inserting the squares for which the clues
        // were > the slop
        int[] forwardSquares = new int[myRowCol.length];
        int[] backwardSquares = new int[myRowCol.length];
        for (int i=0; i<myRowCol.length; i++)
        {
            forwardSquares[i] = -1;
            backwardSquares[i] = -1;
        }

        int index = start_index;
        for (int cl=0; cl<clues.length; cl++)
        {
            for (int sp=0; sp<clues[cl]; sp++)
            {
                if (clues[cl] > slop) forwardSquares[index++] = cl;
                else index++;
                if (index > end_index && (sp < (clues[cl]-1) || cl < (clues.length-1)))
                {
                    PaintByNumberPro.HandleErrorMessage (title,
                            "While processing overlaps, found error in the clues (forwards)!");
                    return false;
                }
            }
            index++;
            if (index > end_index) break; // this might happen at end of row/col
        }

        index = end_index;
        for (int cl=clues.length-1; cl>=0; cl--)
        {
            for (int sp=0; sp<clues[cl]; sp++)
            {
                if (clues[cl] > slop) backwardSquares[index--] = cl;
                else index--;
                if (index < start_index && (sp < (clues[cl]-1) || cl > 0))
                {
                    PaintByNumberPro.HandleErrorMessage (title,
                            "While processing overlaps, found error in the clues (backwards)!");
                    return false;
                }
            }
            index--;
            if (index < start_index) break; // this might happen at start of row/col
        }

        for (int i=start_index; i<=end_index; i++)
        {
            if (forwardSquares[i] >= 0 && backwardSquares[i]>= 0 &&
                forwardSquares[i] == backwardSquares[i])
            {
                if (myRowCol[i].IsUnknown())
                    myRowCol[i].SetStatus (PuzzleSquare.SquareStatus.FILLED, guess_level);
            }
            if (slop == 0 && forwardSquares[i] < 0 && backwardSquares[i] < 0)
            {
                if (myRowCol[i].IsUnknown())
                    myRowCol[i].SetStatus (PuzzleSquare.SquareStatus.EMPTY, guess_level);
            }
        }

        return true;
    }

    private static boolean ProcessOverlaps (PuzzleSquare[] myRowCol, int[] clues)
    {
        return ProcessOverlaps (myRowCol, 0, myRowCol.length-1, clues, 0);
    }

    // ----------------------------------------------
    // Code to check puzzle solution or status so far
    // ----------------------------------------------

	public static boolean CheckPuzzleSoFar (PBNPuzzle myPuzzle, boolean for_solver, 
			boolean do_debug, boolean suppress_success_dialog)
	{ return CheckPuzzleSoFar (myPuzzle, for_solver, null, do_debug, suppress_success_dialog); }
	
    public static boolean CheckPuzzleSoFar (PBNPuzzle myPuzzle, boolean for_solver, 
			PuzzleSolverThread theThread, boolean do_debug, boolean suppress_success_dialog)
    {
        // Note: If the auto-solver is running (i.e. for_solver is TRUE), then
        // we don't want to see the error messages
        // If the auto-solver is NOT running, then the operator must have chosen
        // to check the puzzle and they WILL want to see the error message
        if (myPuzzle == null) return false;
        last_msg = UNKNOWN_ERROR;
		
		BetterPuzzleSolver bps = new BetterPuzzleSolver(theThread);

        // Check rows first
        for (int r=0; r<myPuzzle.GetRows(); r++)
        {
			if (!bps.CanSolutionFit (myPuzzle, true, r))
            {
                if (!for_solver && last_msg != null)
                    PaintByNumberPro.HandleErrorMessage (title, "Error in row " + r);
				else if (do_debug)
					System.out.println ("Error in row " + r);
                return false;
            }
        }

        // Check cols next
        for (int c=0; c<myPuzzle.GetCols(); c++)
			if (!bps.CanSolutionFit (myPuzzle, false, c))
            {
                if (!for_solver && last_msg != null)
                    PaintByNumberPro.HandleErrorMessage (title, "Error in col " + c);
				else if (do_debug)
					System.out.println ("Error in col " + c);
                return false;
            }

        if (!for_solver && !suppress_success_dialog)
            PaintByNumberPro.HandleMessage ("Check Puzzle",
                "Your puzzle looks good so far!");

        return true;
     }

    public static boolean IsPuzzleCorrect (PBNPuzzle myPuzzle)
    {
        if (myPuzzle == null) return false;
        last_msg = UNKNOWN_ERROR;

        // Check rows first
        for (int r=0; r<myPuzzle.GetRows(); r++)
            if (!IsRowCorrect (myPuzzle, r, false))
            {
                if (last_msg != null)
                    PaintByNumberPro.HandleErrorMessage (title, last_msg);
                return false;
            }

        // Check cols next
        for (int c=0; c<myPuzzle.GetCols(); c++)
            if (!IsColCorrect (myPuzzle, c, false))
            {
                if (last_msg != null)
                    PaintByNumberPro.HandleErrorMessage (title, last_msg);
                return false;
            }

        PaintByNumberPro.HandleMessage ("Check Puzzle",
                "Hooray!  Your solution is correct!");

        return true;
    }
    
    public static boolean IsRowCorrect (PBNPuzzle myPuzzle, int row, boolean verbose)
    {
        // Treat unknown squares as empty

        // make sure status of all squares is !unknown
        for (int i=0; i<myPuzzle.GetCols(); i++)
            if (myPuzzle.GetPuzzleSquareAt(row, i).IsUnknown()) 
            {
                last_msg = "Not all squares in row " + row + " have been filled in";
                if (verbose) PaintByNumberPro.HandleErrorMessage (title,
                        last_msg);
                return false;
            }
        
        // if we've gotten this far, then all squares have been filled in
        
        // now check to see if we have a correct solution
        int cur_col = 0;    // next column to check
        int cur_clue = 0;   // next clue to check
        if (myPuzzle.GetRow_NClues(row) > 0)
        {
            // handle case of clue = 0
            if (myPuzzle.GetRow_NClues(row) == 1 && myPuzzle.GetRow_Clues (row, 0) == 0)
            {
                for (int i=0; i<myPuzzle.GetCols(); i++)
                {
                    PuzzleSquare ps = myPuzzle.GetPuzzleSquareAt (row, i);
                    if (ps.IsFilled())
                    {
                        last_msg = "In row " + row + ", found a filled square where none are expected";
                        if (verbose) PaintByNumberPro.HandleErrorMessage (title, last_msg);
                        return false;
                    }
                }
                return true;
            // handle all other cases
            } else
            {
                PuzzleSquare ps = myPuzzle.GetPuzzleSquareAt(row, cur_col);
                cur_col++;
                while (cur_col <= myPuzzle.GetCols() && cur_clue < myPuzzle.GetRow_NClues(row))
                {
                   // Process xs
                   if (!ps.IsFilled())
                   {
                       while (!ps.IsFilled() && cur_col < myPuzzle.GetCols())
                       {
                           ps = myPuzzle.GetPuzzleSquareAt(row, cur_col);
                           cur_col++;
                       }
                   } else if (ps.IsFilled())
                   {
                       int num_filleds = 1;
                       while (ps.IsFilled() && cur_col < myPuzzle.GetCols())
                       {
                           ps = myPuzzle.GetPuzzleSquareAt(row, cur_col);
                           cur_col++;
                           if (ps.IsFilled()) num_filleds++;
                       }
                       if (num_filleds == myPuzzle.GetRow_Clues (row, cur_clue)) cur_clue ++;
                       else
                       {
                           last_msg = "In row " + row + ", found " + num_filleds + " boxes filled in instead of " +
                                   myPuzzle.GetRow_Clues (row, cur_clue) + " as expected for clue " +
                                   cur_clue;
                           if (verbose) PaintByNumberPro.HandleErrorMessage(title, last_msg);
                           return false;
                       }
                   }
                }
            }
        }

        // Make sure we got to the last clue
        if (cur_clue < myPuzzle.GetRow_NClues(row))
        {
            last_msg = "Did not find clue " + cur_clue + " in row " + row;
            if (verbose) PaintByNumberPro.HandleErrorMessage (title, last_msg);
            return false;
        }

        // Check to make sure the end of the row is filled with Xs
        if (cur_col < myPuzzle.GetCols())
        {
            for (int col = cur_col; col < myPuzzle.GetCols(); col++)
                if (myPuzzle.GetPuzzleSquareAt(row, col).IsFilled())
                {
                    last_msg = "Expected all squares to be empty from col " + cur_col + " to end of row " + row;
                    if (verbose) PaintByNumberPro.HandleErrorMessage (title, last_msg);
                    return false;
                }
        }

        return true;
    }

    public static boolean IsColCorrect (PBNPuzzle myPuzzle, int col, boolean verbose)
    {
        // Treat all unknown squares as empty

        // make sure status of all squares is !unknown
        for (int i=0; i<myPuzzle.GetRows(); i++)
            if (myPuzzle.GetPuzzleSquareAt(i, col).IsUnknown())
            {
                last_msg = "Not all squares in col " + col + " have been filled in";
                if (verbose) PaintByNumberPro.HandleErrorMessage (title,
                        last_msg);
                return false;
            }

        // if we've gotten this far, then all squares have been filled in

        // now check to see if we have a correct solution
        int cur_row = 0;    // next row to check
        int cur_clue = 0;   // next clue to check
        if (myPuzzle.GetCol_NClues(col) > 0)
        {
            // handle case of clue = 0
            if (myPuzzle.GetCol_NClues(col) == 1 && myPuzzle.GetCol_Clues (col, 0) == 0)
            {
                for (int i=0; i<myPuzzle.GetRows(); i++)
                {
                    PuzzleSquare ps = myPuzzle.GetPuzzleSquareAt (i, col);
                    if (ps.IsFilled())
                    {
                        last_msg = "In col " + col + ", found a filled square where none are expected";
                        if (verbose) PaintByNumberPro.HandleErrorMessage (title, last_msg);
                        return false;
                    }
                }
                return true;
            // handle all other cases
            } else
            {
                PuzzleSquare ps = myPuzzle.GetPuzzleSquareAt(cur_row, col);
                cur_row++;
                while (cur_row <= myPuzzle.GetRows() && cur_clue < myPuzzle.GetCol_NClues(col))
                {
                   // Process xs
                   if (!ps.IsFilled())
                   {
                       while (!ps.IsFilled() && cur_row < myPuzzle.GetRows())
                       {
                           ps = myPuzzle.GetPuzzleSquareAt(cur_row, col);
                           cur_row++;
                       }
                   } else if (ps.IsFilled())
                   {
                       int num_filleds = 1;
                       while (ps.IsFilled() && cur_row < myPuzzle.GetRows())
                       {
                           ps = myPuzzle.GetPuzzleSquareAt(cur_row, col);
                           cur_row++;
                           if (ps.IsFilled()) num_filleds++;
                       }
                       if (num_filleds == myPuzzle.GetCol_Clues (col, cur_clue)) cur_clue ++;
                       else
                       {
                           last_msg = "In col " + col + ", found " + num_filleds + " boxes filled in instead of " +
                                   myPuzzle.GetCol_Clues (col, cur_clue) + " as expected for clue " +
                                   cur_clue;
                           if (verbose) PaintByNumberPro.HandleErrorMessage(title, last_msg);
                           return false;
                       }
                   }
                }
            }
        }

        // Make sure we got to the last clue
        if (cur_clue < myPuzzle.GetCol_NClues(col))
        {
            last_msg = "Did not find clue " + cur_clue + " in col " + col;
            if (verbose) PaintByNumberPro.HandleErrorMessage (title, last_msg);
            return false;
        }

        // Check to make sure the end of the row is filled with Xs
        if (cur_row < myPuzzle.GetRows())
        {
            for (int row = cur_row; row < myPuzzle.GetRows(); row++)
                if (myPuzzle.GetPuzzleSquareAt(row, col).IsFilled())
                {
                    last_msg = "Expected all squares to be empty from row " + cur_row + " to end of col " + col;
                    if (verbose) PaintByNumberPro.HandleErrorMessage (title, last_msg);
                    return false;
                }
        }

        return true;
    }

    private static int AdvanceToNextUnknownOrFilledSpaceForward (PuzzleSquare[] myRowOrCol, int start_col)
    {
       int len = myRowOrCol.length;

       assert (start_col >= 0 && start_col < len);
       for (int i=start_col; i<len; i++) if (!myRowOrCol[i].IsEmpty()) return i;
       return len;
    }

    private static int AdvanceToNextUnknownOrFilledSpaceBackward (PuzzleSquare[] myRowOrCol, int start_col)
    {
       int len = myRowOrCol.length;

       assert (start_col >= 0 && start_col < len);
       for (int i=start_col; i>=0; i--) if (!myRowOrCol[i].IsEmpty()) return i;
       return len;
    }

    // ----------------------------------------------
    // Code to fill in the *really* obvious squares
    // ----------------------------------------------

    public static void FillInEasyToComputeSquares (PBNPuzzle myPuzzle, boolean verbose)
    {
        if (myPuzzle == null) return;

        // Handle rows first
        for (int r=0; r<myPuzzle.GetRows(); r++)
            if (!FillInOverlapsInRow (myPuzzle, r, verbose)) return;

        // Handle cols next
        for (int c=0; c<myPuzzle.GetCols(); c++)
            if (!FillInOverlapsInCol (myPuzzle, c, verbose)) return;

        boolean changing = true;

        while (changing)
        {
            int prev_known = myPuzzle.CountKnownSquares();

            // Process left column
            if (!ProcessLeftColumn (myPuzzle, 0, verbose)) return;

            // Process right column
            if (!ProcessRightColumn (myPuzzle, 0, verbose)) return;

            // Process top row
            if (!ProcessTopRow (myPuzzle, 0, verbose)) return;

            // Process bottom row
            if (!ProcessBottomRow (myPuzzle, 0, verbose)) return;

            // Process single clues
            if (!ProcessAllSingleClues (myPuzzle, 0, verbose)) return;

            // Process clues that can only fit in one spot
            ProcessBumpers (myPuzzle, 0);

            // Clean up unique unknowns if all clues account for
            CleanUpUnknowns (myPuzzle, 0);
            
            int cur_known = myPuzzle.CountKnownSquares();
            
            changing = (prev_known != cur_known);
        }

    }

    public static boolean ProcessEdges (PBNPuzzle myPuzzle, int guess_level, boolean verbose)
    {
        // Process left column
        if (!ProcessLeftColumn (myPuzzle, guess_level, verbose)) return false;

        // Process right column
        if (!ProcessRightColumn (myPuzzle, guess_level, verbose)) return false;

        // Process top row
        if (!ProcessTopRow (myPuzzle, guess_level, verbose)) return false;

        // Process bottom row
        if (!ProcessBottomRow (myPuzzle, guess_level, verbose)) return false;

        return true;
    }
    
    private static void CleanUpUnknownsInRow (PBNPuzzle myPuzzle, int row, int guess_level)
    {
        if (myPuzzle == null) return;
        
        SetCurrentProcess (-1, row, guess_level, "CleanUpUnknownsInRow");  
        
        int nclues = myPuzzle.GetRow_NClues(row);
        
        boolean ready_for_clean = true;
        int cur_clue = 0;
        int num_filled = 0;
        boolean filling = false;
        for (int c=0; c<myPuzzle.GetCols() && ready_for_clean; c++)
        {
            if (myPuzzle.GetPuzzleSquareAt(row, c).IsFilled())
            {
                num_filled++;
                filling = true;
            }
            else
            {
                if (filling)
                {
                    if (cur_clue >= nclues) ready_for_clean = false;
                    else if (num_filled != myPuzzle.GetRow_Clues(row, cur_clue)) ready_for_clean = false;
                    num_filled = 0;
                    cur_clue++;
                    filling = false;
                }
            }
        }
        if (filling)
        {
            if (cur_clue >= nclues) ready_for_clean = false;
            else if (num_filled != myPuzzle.GetRow_Clues(row, cur_clue)) ready_for_clean = false;
        }
        
        if (ready_for_clean && cur_clue == nclues)
        {
            for (int c=0; c<myPuzzle.GetCols(); c++)
                if (myPuzzle.GetPuzzleSquareAt(row, c).IsUnknown())
                    myPuzzle.SetPuzzleRowCol(row, c, PuzzleSquare.SquareStatus.EMPTY, guess_level);
        }
    }

    private static void CleanUpUnknownsInCol (PBNPuzzle myPuzzle, int col, int guess_level)
    {
        if (myPuzzle == null) return;
        
        SetCurrentProcess (col, -1, guess_level, "CleanUpUnknownsInCol"); 
        
        int nclues = myPuzzle.GetCol_NClues(col);

        boolean ready_for_clean = true;
        int cur_clue = 0;
        int num_filled = 0;
        boolean filling = false;
        for (int r=0; r<myPuzzle.GetRows() && ready_for_clean; r++)
        {
            if (myPuzzle.GetPuzzleSquareAt(r, col).IsFilled())
            {
                num_filled++;
                filling = true;
            }
            else
            {
                if (filling)
                {
                    if (cur_clue >= nclues) ready_for_clean = false;
                    else if (num_filled != myPuzzle.GetCol_Clues(col, cur_clue)) ready_for_clean = false;
                    num_filled = 0;
                    cur_clue++;
                    filling = false;
                }
            }
        }
        if (filling)
        {
            if (cur_clue >= nclues) ready_for_clean = false;
            else if (num_filled != myPuzzle.GetCol_Clues(col, cur_clue)) ready_for_clean = false;
        }

        if (ready_for_clean && cur_clue == nclues)
        {
            for (int r=0; r<myPuzzle.GetRows(); r++)
                if (myPuzzle.GetPuzzleSquareAt(r, col).IsUnknown())
                    myPuzzle.SetPuzzleRowCol(r, col, PuzzleSquare.SquareStatus.EMPTY, guess_level);
        }
    }

    public static void CleanUpUnknowns (PBNPuzzle myPuzzle, int guess_level)
    {
        if (myPuzzle == null) return;
        for (int r=0; r<myPuzzle.GetRows() && !do_stop; r++)
        {
            CleanUpUnknownsInRow (myPuzzle, r, guess_level);
         }
        for (int c=0; c<myPuzzle.GetCols() && !do_stop; c++)
        {
            CleanUpUnknownsInCol (myPuzzle, c, guess_level);
        }
    }

    public static boolean FillInOverlapsInRow (PBNPuzzle myPuzzle, int row, boolean verbose)
    {
        if (myPuzzle == null) return false;

        if (myPuzzle.GetRow_NClues(row) == 0)
        {
            for (int c=0; c<myPuzzle.GetCols(); c++)
                myPuzzle.SetPuzzleRowCol (row, c, PuzzleSquare.SquareStatus.EMPTY);
                return true;
        } else
        {
            int nclues = myPuzzle.GetRow_NClues(row);
            int clues[] = new int[nclues];
            for (int cl=0; cl<nclues; cl++)
                clues[cl] = myPuzzle.GetRow_Clues(row, cl);

            PuzzleSquare myRow[] = CopyRowFromPuzzle (myPuzzle, row);
            if (!ProcessOverlaps (myRow, clues)) 
            {
                last_msg = "Error processing overlaps in row " + row;
                if (verbose)
                    PaintByNumberPro.HandleErrorMessage(title, last_msg);
                return false;
            }
            CopyRowToPuzzle (myPuzzle, myRow, row);
        }
        return true;
    }

    public static boolean FillInOverlapsInColFromPopup (PBNPuzzle myPuzzle, int col)
    {
        boolean is_good = FillInOverlapsInCol (myPuzzle, col, false);
        if (is_good)
        {
            PaintByNumberPro.HandleMessage ("Processing Overlaps", "Completed for col " + col + " without errors");
        } else
        {
            PaintByNumberPro.HandleErrorMessage (title, last_msg);
        }
        return is_good;
    }

    public static boolean FillInOverlapsInRowFromPopup (PBNPuzzle myPuzzle, int row)
    {
        boolean is_good = FillInOverlapsInRow (myPuzzle, row, false);
        if (is_good)
        {
            PaintByNumberPro.HandleMessage ("Processing Overlaps", "Completed for row " + row + " without errors");
        } else
        {
            PaintByNumberPro.HandleErrorMessage (title, last_msg);
        }
        return is_good;
    }

    public static boolean FillInOverlapsInCol (PBNPuzzle myPuzzle, int col, boolean verbose)
    {
        if (myPuzzle == null) return false;

        if (myPuzzle.GetCol_NClues(col) == 0)
        {
            for (int r=0; r<myPuzzle.GetRows(); r++)
                myPuzzle.SetPuzzleRowCol (r, col, PuzzleSquare.SquareStatus.EMPTY);
                return true;
        } else
        {
            int nclues = myPuzzle.GetCol_NClues(col);
            int clues[] = new int[nclues];
            for (int cl=0; cl<nclues; cl++)
                clues[cl] = myPuzzle.GetCol_Clues(col, cl);

            PuzzleSquare myCol[] = CopyColFromPuzzle (myPuzzle, col);
            if (!ProcessOverlaps (myCol, clues))
            {
                last_msg = "Processing overlaps in col " + col;
                if (verbose)
                    PaintByNumberPro.HandleErrorMessage(title,
                        "Error processing overlaps in col " + col);
                return false;
            }
            CopyColToPuzzle (myPuzzle, myCol, col);
        }
        return true;
    }

    public static boolean ProcessLeftColumn (PBNPuzzle myPuzzle, int guess_level, boolean verbose)
    {
        if (myPuzzle == null) return false;

        boolean success = true;
        for (int r=0; r<myPuzzle.GetRows() && success && !do_stop; r++)
        {
            success = ProcessRowForwardFromEdge (myPuzzle, r, guess_level, verbose);
        }
        return success;
    }

    public static boolean ProcessRightColumn (PBNPuzzle myPuzzle, int guess_level, boolean verbose)
    {
        if (myPuzzle == null) return false;

        boolean success = true;
        for (int r=0; r<myPuzzle.GetRows() && success && !do_stop; r++)
        {
            success = ProcessRowBackwardFromEdge (myPuzzle, r, guess_level, verbose);
        }
        return success;
    }

    public static boolean ProcessTopRow (PBNPuzzle myPuzzle, int guess_level, boolean verbose)
    {
        if (myPuzzle == null) return false;

        boolean success = true;
        for (int c=0; c<myPuzzle.GetCols() && success && !do_stop; c++)
        {
           success = ProcessColumnForwardFromEdge (myPuzzle, c, guess_level, verbose);
        }
        return success;
    }

    public static boolean ProcessBottomRow (PBNPuzzle myPuzzle, int guess_level, boolean verbose)
    {
        if (myPuzzle == null) return false;

        boolean success = true;
        for (int c=0; c<myPuzzle.GetCols() && success && !do_stop; c++)
        {
            success = ProcessColumnBackwardFromEdge (myPuzzle, c, guess_level, verbose);
        }
        return success;
    }

    // Return false only if there's an error
    public static boolean ProcessForwardFromClueAt (PuzzleSquare[] myList, int[] clues, int from_clue, int from_index,
            int guess_level)
    {
        if (myList == null || clues == null) return false;
        if (from_clue < 0 || from_clue >= clues.length) return false;
        if (from_index < 0 || from_index >= myList.length) return false;

        if (myList[from_index].IsEmpty()) return false;

        int num_unknowns = 0;
        int num_filled = 0;
        boolean start_filling = false;
        int index = from_index;
        boolean at_empty = false;
        for (int i=0; i<clues[from_clue] && index < myList.length && !at_empty; i++)
        {
            if (myList[index].IsFilled())
            {
                start_filling = true;
                num_filled++;
            }
            if (myList[index].IsUnknown())
            {
                if (start_filling)
                {
                    myList[index].SetStatus(PuzzleSquare.SquareStatus.FILLED, guess_level);
                    num_filled++;
                }
                else num_unknowns++;
            }
            if (myList[index].IsEmpty()) at_empty = true;
            index++;
        }

        boolean next_is_empty = true;
        if (index < myList.length) next_is_empty = myList[index].IsEmpty();

        boolean next_is_filled = false;
        if (index < myList.length) next_is_filled = myList[index].IsFilled();

        // this is to handle clue=3, _ _ _ F F  to X X _ F F
        if(next_is_filled && num_filled < clues[from_clue])
        {
            // find start of filled sequence and end
            int start_seq = index;
            int end_seq = index;
            int temp = start_seq;
            while (temp >= 0 && myList[temp].IsFilled())
            {
                start_seq = temp;
                temp--;
            }
            temp = end_seq;
            while (temp < myList.length && myList[temp].IsFilled())
            {
                end_seq = temp;
                temp++;
            }
            // determine amount of slop, if any between sequence length and clue value
            int seq_len = end_seq - start_seq + 1;
            int slop = clues[from_clue] - seq_len;
            if (slop < 0) return false;
            // fill in with empties starting at from_index to slop
            boolean keep_going = true;
            int slop_cnt = 0;
            for (int indx=start_seq-1; indx >= from_index && keep_going; indx--)
            {
                if (!myList[indx].IsUnknown())
                    keep_going = false;
                else if (slop_cnt >= slop)
                    myList[indx].SetStatus (PuzzleSquare.SquareStatus.EMPTY, guess_level);
                slop_cnt++;
            }

        }

        // this is to handle clue=3, F _ _ _ to F F F X
        else if(start_filling && num_filled == clues[from_clue] && !next_is_empty)
        {
            if (myList[index].IsUnknown())
                myList[index].SetStatus (PuzzleSquare.SquareStatus.EMPTY, guess_level);
            else if (myList[index].IsFilled()) return false;
        }

        // this is to handle clue=3, _ F _ X to F F F X and then moving to next clue
        else if(start_filling &&
            (num_unknowns+num_filled) == clues[from_clue] &&
            next_is_empty)
        {
            index = from_index;
            for (int i=0; i<clues[from_clue]; i++)
            {
                if (myList[index].IsUnknown())
                    myList[index].SetStatus (PuzzleSquare.SquareStatus.FILLED, guess_level);
                index++;
            }

            // move to next clue
            int next_clue = from_clue+1;
            if (next_clue == clues.length) return true;

            // move to next possible place for a clue
            int next_nonempty_index = AdvanceToNextUnknownOrFilledSpaceForward (myList, index);
            if (next_nonempty_index == myList.length) return false;
            return (ProcessForwardFromClueAt (myList, clues, next_clue, next_nonempty_index, guess_level));

        // this is to handle clue=3, _ _ X to X X X and then moving on to next possible location for same clue
        } else if (at_empty)
        {
            if (start_filling) return false;

            index = from_index;
            while (myList[index].IsUnknown())
                myList[index++].SetStatus (PuzzleSquare.SquareStatus.EMPTY, guess_level);

            int next_nonempty_index = AdvanceToNextUnknownOrFilledSpaceForward (myList, index);
            if (next_nonempty_index == myList.length) return false;

            return (ProcessForwardFromClueAt (myList, clues, from_clue, next_nonempty_index, guess_level));
        }

        return true;
    }

    // Return false only if there's an error
    public static boolean ProcessBackwardFromClueAt (PuzzleSquare[] myList, int[] clues, int from_clue, int from_index,
            int guess_level)
    {
        if (myList == null || clues == null) return false;
        if (from_clue < 0 || from_clue >= clues.length) return false;
        if (from_index < 0 || from_index >= myList.length) return false;

        if (myList[from_index].IsEmpty()) return false;

        int num_unknowns = 0;
        int num_filled = 0;
        boolean start_filling = false;
        int index = from_index;
        boolean at_empty = false;
        for (int i=0; i<clues[from_clue] && index >= 0 && !at_empty; i++)
        {
            if (myList[index].IsFilled())
            {
                start_filling = true;
                num_filled++;
            }
            if (myList[index].IsUnknown())
            {
                if (start_filling)
                {
                    myList[index].SetStatus(PuzzleSquare.SquareStatus.FILLED, guess_level);
                    num_filled++;
                }
                else num_unknowns++;
            }
            if (myList[index].IsEmpty()) at_empty = true;
            index--;
        }

        boolean next_is_empty = true;
        if (index >= 0) next_is_empty = myList[index].IsEmpty();

        boolean next_is_filled = false;
        if (index >= 0) next_is_filled = myList[index].IsFilled();
        
        // this is to handle clue=3, _ _ _ F F  to X X _ F F
        if(next_is_filled && num_filled < clues[from_clue])
        {
            // find start of filled sequence and end
            int start_seq = index;
            int end_seq = index;
            int temp = start_seq;
            while (temp >= 0 && myList[temp].IsFilled())
            {
                start_seq = temp;
                temp--;
            }
            temp = end_seq;
            while (temp < myList.length && myList[temp].IsFilled())
            {
                end_seq = temp;
                temp++;
            }
            // determine amount of slop, if any between sequence length and clue value
            int seq_len = end_seq - start_seq + 1;
            int slop = clues[from_clue] - seq_len;
            if (slop < 0) return false;
            // fill in with empties starting at from_index to slop
            boolean keep_going = true;
            int slop_cnt = 0;
            for (int indx=end_seq+1; indx <= from_index && keep_going; indx++)
            {
                if (!myList[indx].IsUnknown())
                    keep_going = false;
                else if (slop_cnt >= slop)
                    myList[indx].SetStatus (PuzzleSquare.SquareStatus.EMPTY, guess_level);
                slop_cnt++;
            }

        }

        else if(start_filling && num_filled == clues[from_clue] && !next_is_empty)
        {
            if (myList[index].IsUnknown())
                myList[index].SetStatus (PuzzleSquare.SquareStatus.EMPTY, guess_level);
            else if (myList[index].IsFilled()) return false;
        }
        else if(start_filling &&
            (num_unknowns+num_filled) == clues[from_clue] &&
            next_is_empty)
        {
            index = from_index;
            for (int i=0; i<clues[from_clue]; i++)
            {
                if (myList[index].IsUnknown())
                    myList[index].SetStatus (PuzzleSquare.SquareStatus.FILLED, guess_level);
                index--;
            }

            int next_clue = from_clue-1;
            if (next_clue < 0) return true;

            int next_nonempty_index = index;
            boolean found_next_space = false;
            while (!found_next_space)
            {
                if (!myList[next_nonempty_index].IsEmpty()) found_next_space = true;
                else
                {
                    next_nonempty_index--;
                    if (next_nonempty_index < 0) return false;
                }
            }
            if (found_next_space)
                return (ProcessBackwardFromClueAt (myList, clues, next_clue, next_nonempty_index, guess_level));
            else return false;
        } else if (at_empty)
        {
            if (start_filling) return false;

            index = from_index;
            while (myList[index].IsUnknown())
                myList[index--].SetStatus (PuzzleSquare.SquareStatus.EMPTY, guess_level);

            int next_nonempty_index = index;
            boolean found_next_space = false;
            while (!found_next_space)
            {
                if (!myList[next_nonempty_index].IsEmpty()) found_next_space = true;
                else
                {
                    next_nonempty_index--;
                    if (next_nonempty_index < 0) return false;
                }
            }
            if (found_next_space)
                return (ProcessBackwardFromClueAt (myList, clues, from_clue, next_nonempty_index, guess_level));
            else return false;
        }

        return true;
    }
    
    public static boolean ProcessRowEdgesFromPopup (PBNPuzzle myPuzzle, int r)
    {
        boolean is_good = ProcessRowEdges (myPuzzle, r, myPuzzle.GetGuessLevel(), false);
        if (is_good)
        {
            PaintByNumberPro.HandleMessage ("Processing Edges", "Completed for row " + r + " without errors");
        } else
        {
            PaintByNumberPro.HandleErrorMessage (title, last_msg);
        }
        return is_good;
    }

    public static boolean ProcessColEdgesFromPopup (PBNPuzzle myPuzzle, int c)
    {
        boolean is_good = ProcessColEdges (myPuzzle, c, myPuzzle.GetGuessLevel(), false);
        if (is_good)
        {
            PaintByNumberPro.HandleMessage ("Processing Edges", "Completed for col " + c + " without errors");
        } else
        {
            PaintByNumberPro.HandleErrorMessage (title, last_msg);
        }
        return is_good;
    }

    public static boolean ProcessColEdges (PBNPuzzle myPuzzle, int c, int guess_level, boolean verbose)
    {
        if (myPuzzle == null) return false;
        if (!ProcessColumnForwardFromEdge (myPuzzle, c, guess_level, verbose)) return false;
        if (!ProcessColumnBackwardFromEdge (myPuzzle, c, guess_level, verbose)) return false;
        return true;
    }

    public static boolean ProcessRowEdges (PBNPuzzle myPuzzle, int r, int guess_level, boolean verbose)
    {
        if (myPuzzle == null) return false;
        if (!ProcessRowForwardFromEdge (myPuzzle, r, guess_level, verbose)) return false;
        if (!ProcessRowBackwardFromEdge (myPuzzle, r, guess_level, verbose)) return false;
        return true;
    }

    public static boolean ProcessForwardForSingleClue (PuzzleSquare[] myList, int clue_val, int guess_level, boolean verbose)
    {
        int[] clue_list = new int[1];
        clue_list[0] = clue_val;
        int cur_index = AdvanceToNextUnknownOrFilledSpaceForward (myList, 0);
        if (ProcessForwardFromClueAt (myList, clue_list, 0, cur_index, guess_level)) return true;
        else return false;
    }

    public static boolean ProcessBackwardForSingleClue (PuzzleSquare[] myList, int clue_val, int guess_level, boolean verbose)
    {
        int[] clue_list = new int[1];
        clue_list[0] = clue_val;
        int cur_index = AdvanceToNextUnknownOrFilledSpaceBackward (myList, myList.length-1);
        if (ProcessBackwardFromClueAt (myList, clue_list, 0, cur_index, guess_level)) return true;
        else return false;
    }

    public static boolean ProcessRowBackwardFromEdge (PBNPuzzle myPuzzle, int r, int guess_level, boolean verbose)
    {
        if (myPuzzle == null) return false;
        
        SetCurrentProcess (-1, r, guess_level, "ProcessRowBackwardFromEdge");        

        int nclues = myPuzzle.GetRow_NClues(r);
        if (nclues > 0)
        {
            int num_unknowns = 0;
            for (int c=0; c<myPuzzle.GetCols(); c++)
                if (myPuzzle.GetPuzzleSquareAt (r, c).IsUnknown()) num_unknowns++;

            if (num_unknowns > 0 && num_unknowns < myPuzzle.GetCols())
            {
                int myClues[] = new int[nclues];
                for (int cl=0; cl<nclues; cl++) myClues[cl] = myPuzzle.GetRow_Clues (r, cl);

                PuzzleSquare myList[] = PuzzleSolver.CopyRowFromPuzzle (myPuzzle, r);
                int cur_col = AdvanceToNextUnknownOrFilledSpaceBackward (myList, myPuzzle.GetCols()-1);
                if (ProcessBackwardFromClueAt (myList, myClues, nclues-1, cur_col, guess_level))
                    PuzzleSolver.CopyRowToPuzzle (myPuzzle, myList, r);
                else
                {
                    last_msg = "There is an error in row " + r + " starting from right edge";
                    if (verbose)
                        PaintByNumberPro.HandleErrorMessage(title, last_msg);
                    return false;
                }
            }
        }
        return true;

    }

    public static boolean ProcessRowForwardFromEdge (PBNPuzzle myPuzzle, int r, int guess_level, boolean verbose)
    {
        if (myPuzzle == null) return false;
        
        SetCurrentProcess (-1, r, guess_level, "ProcessRowForwardFromEdge");
                       
        PuzzleSquare myList[] = PuzzleSolver.CopyRowFromPuzzle (myPuzzle, r);        
        int myClues[] = PuzzleSolver.GetCluesForRowFromPuzzle (myPuzzle, r);

        /* another unnecessary failed attempt
        ActivePuzzleSquares aps = GetActivePuzzleSquares (myList, myClues);
        if (aps == null) return true;        
        myClues = aps.GetClues();
        myList = aps.GetPuzzleSquares();
        */
        
        int nclues = 0;
        if (myClues!= null) nclues = myClues.length;        
        int myCols = myList.length;

        if (nclues > 0)
        {
            int num_unknowns = 0;
            for (int c=0; c<myCols; c++)
                if (myPuzzle.GetPuzzleSquareAt (r, c).IsUnknown()) num_unknowns++;

            if (num_unknowns > 0 && num_unknowns < myCols)
            {

                int cur_col = AdvanceToNextUnknownOrFilledSpaceForward (myList, 0);
                if (ProcessForwardFromClueAt (myList, myClues, 0, cur_col, guess_level))
//                    PuzzleSolver.CopyRowToPuzzleStartingFrom (myPuzzle, myList, r, aps.GetStartSquare());
                    PuzzleSolver.CopyRowToPuzzle (myPuzzle, myList, r);
                else
                {
//                    last_msg = "There is an error in row " + r + " starting from left edge (which starts at col " + aps.GetStartSquare() + ")";
                    last_msg = "There is an error in row " + r + " starting from left edge";
                    if (verbose)
                        PaintByNumberPro.HandleErrorMessage(title, last_msg);
                    return false;
                }
            }
        }
        return true;
    }

    public static boolean ProcessColumnForwardFromEdge (PBNPuzzle myPuzzle, int c, int guess_level, boolean verbose)
    {
        if (myPuzzle == null) return false;
        
        SetCurrentProcess (c, -1, guess_level, "ProcessColumnForwardFromEdge");  

        int nclues = myPuzzle.GetCol_NClues(c);
        if (nclues > 0)
        {
            int num_unknowns = 0;
            for (int r=0; r<myPuzzle.GetRows(); r++)
                if (myPuzzle.GetPuzzleSquareAt (r, c).IsUnknown()) num_unknowns++;

            if (num_unknowns > 0 && num_unknowns < myPuzzle.GetRows())
            {
                int myClues[] = new int[nclues];
                for (int cl=0; cl<nclues; cl++) myClues[cl] = myPuzzle.GetCol_Clues (c, cl);

                PuzzleSquare myList[] = PuzzleSolver.CopyColFromPuzzle (myPuzzle, c);
                int cur_row = AdvanceToNextUnknownOrFilledSpaceForward (myList, 0);
                if (ProcessForwardFromClueAt (myList, myClues, 0, cur_row, guess_level))
                    PuzzleSolver.CopyColToPuzzle (myPuzzle, myList, c);
                else
                {
                    last_msg = "There is an error in col " + c + " starting from top edge";
                    if (verbose)
                        PaintByNumberPro.HandleErrorMessage(title, last_msg);
                    return false;
                }
            }
        }
        return true;
    }

    public static boolean ProcessColumnBackwardFromEdge (PBNPuzzle myPuzzle, int c, int guess_level, boolean verbose)
    {
        if (myPuzzle == null) return false;
        
        SetCurrentProcess (c, -1, guess_level, "ProcessColumnBackwardFromEdge");          

        int nclues = myPuzzle.GetCol_NClues(c);
        if (nclues > 0)
        {
            int num_unknowns = 0;
            for (int r=0; r<myPuzzle.GetRows(); r++)
                if (myPuzzle.GetPuzzleSquareAt (r, c).IsUnknown()) num_unknowns++;

            if (num_unknowns > 0 && num_unknowns < myPuzzle.GetRows())
            {
                int myClues[] = new int[nclues];
                for (int cl=0; cl<nclues; cl++) myClues[cl] = myPuzzle.GetCol_Clues (c, cl);

                PuzzleSquare myList[] = PuzzleSolver.CopyColFromPuzzle (myPuzzle, c);
                int cur_row = AdvanceToNextUnknownOrFilledSpaceBackward (myList, myPuzzle.GetRows()-1);
                if (ProcessBackwardFromClueAt (myList, myClues, nclues-1, cur_row, guess_level))
                    PuzzleSolver.CopyColToPuzzle (myPuzzle, myList, c);
                else
                {
                    last_msg = "There is an error in col " + c + " starting from bottom edge";
                    if (verbose)
                        PaintByNumberPro.HandleErrorMessage(title, last_msg);
                    return false;
                }
            }
        }
        return true;
    }

    public static boolean ProcessAllSingleClues (PBNPuzzle myPuzzle, int guess_level, boolean verbose)
    {
        if (myPuzzle == null) return false;
        for (int r=0; r<myPuzzle.GetRows(); r++)
            if (!ProcessSingleClueInRow (myPuzzle, r, guess_level, verbose)) return false;
        for (int c=0; c<myPuzzle.GetCols(); c++)
            if (!ProcessSingleClueInCol (myPuzzle, c, guess_level, verbose)) return false;
        return true;
    }

    public static boolean ProcessSingleClueInRow (PBNPuzzle myPuzzle, int row, int guess_val, boolean verbose)
    {
        if (myPuzzle == null) return false;
        if (myPuzzle.GetRow_NClues(row) == 1)
        {
            PuzzleSquare[] myList = PuzzleSolver.CopyRowFromPuzzle (myPuzzle, row);
            if (ProcessSingleClues (myList, myPuzzle.GetRow_Clues(row, 0), guess_val, verbose))
                PuzzleSolver.CopyRowToPuzzle (myPuzzle, myList, row);
            else 
            {
                last_msg = "Error processing the single clue in row " + row;
                if (verbose)
                    PaintByNumberPro.HandleErrorMessage(title, last_msg);
                return false;
            }
        }
        return true;
    }

    public static boolean ProcessSingleClueInCol (PBNPuzzle myPuzzle, int col, int guess_val, boolean verbose)
    {
        if (myPuzzle == null) return false;
        if (myPuzzle.GetCol_NClues(col) == 1)
        {
            PuzzleSquare[] myList = PuzzleSolver.CopyColFromPuzzle (myPuzzle, col);
            if (ProcessSingleClues (myList, myPuzzle.GetCol_Clues(col, 0), guess_val, verbose))
                PuzzleSolver.CopyColToPuzzle (myPuzzle, myList, col);
            else 
            {
                last_msg = "Error processing the single clue in col " + col;
                if (verbose)
                    PaintByNumberPro.HandleErrorMessage(title, last_msg);
                return false;
            }
        }
        return true;
    }

    public static boolean ProcessSingleClues (PuzzleSquare[] myList, int clue_val, int guess_level, boolean verbose)
    {
        if (myList == null) return false;
        
        // Handle easiest case first with single clue of value 0
        if (clue_val == 0)
        {
            for (int i=0; i<myList.length; i++)
            {
                if (myList[i].IsFilled()) return false;
                else myList[i].SetStatus (PuzzleSquare.SquareStatus.EMPTY, guess_level);
            }
            return true;
        }

        // See how many unknowns and find first/last filled squares, if any
        int num_unknowns = 0;
        int first_index = -1;
        int last_index = -1;
        for (int i=0; i<myList.length; i++)
        {
            if (myList[i].IsUnknown()) num_unknowns++;
            if (myList[i].IsFilled())
            {
                if (first_index < 0) first_index = i;
                last_index = i;
            }
        }

        // don't bother processing if all unknowns
        if (num_unknowns == myList.length) return true;

        // go ahead and process forward and backward (this should close up
        // any gaps that are too small for the clue and should extend any
        // filled squares together, if possible)
        if (!ProcessForwardForSingleClue (myList, clue_val, guess_level, verbose)) return false;
        if (!ProcessBackwardForSingleClue (myList, clue_val, guess_level, verbose)) return false;

        // now figure out slop between both ends of filled squares
        // and the clue value
        if (first_index >= 0 && last_index >= 0)
        {

            // first find the new ends of the filled region
            first_index = -1;
            last_index = -1;
            for (int i=0; i<myList.length; i++)
            {
                if (myList[i].IsFilled())
                {
                    if (first_index < 0) first_index = i;
                    last_index = i;
                }
            }

            // now calculate the slop
            int filled_len = last_index-first_index+1;
            int slop = clue_val - filled_len;
            if (slop < 0) return false;

            // fill in all unknowns with empties beyond the slop region
            int end_index = first_index-slop;
            if (end_index >= 0)
                for (int i=0; i<end_index; i++)
                    if (myList[i].IsUnknown()) myList[i].SetStatus (PuzzleSquare.SquareStatus.EMPTY, guess_level);

            int start_index = last_index+slop+1;
            if (last_index < myList.length)
                for (int i=start_index; i<myList.length; i++)
                    if (myList[i].IsUnknown()) myList[i].SetStatus (PuzzleSquare.SquareStatus.EMPTY, guess_level);
        }

        return true;
    }

    public static void ProcessBumpers (PBNPuzzle myPuzzle, int guess_level)
    {
        if (myPuzzle == null) return;
        for (int r=0; r<myPuzzle.GetRows() && !do_stop; r++)
        {
            ProcessBumpersInRow (myPuzzle, r, guess_level);
        }
        for (int c=0; c<myPuzzle.GetCols() && !do_stop; c++)
        {
            ProcessBumpersInCol (myPuzzle, c, guess_level);
        }
    }

    private static void ProcessBumpersInCol (PBNPuzzle myPuzzle, int col, int guess_level)
    {
        if (myPuzzle == null) return;
        
        SetCurrentProcess (col, -1, guess_level, "ProcessBumpersInCol");         

        PuzzleSquare[] myList = PuzzleSolver.CopyColFromPuzzle(myPuzzle, col);
        int myClues[] = new int[myPuzzle.GetCol_NClues(col)];
        for (int i=0; i<myPuzzle.GetCol_NClues(col); i++)
            myClues[i] = myPuzzle.GetCol_Clues (col, i);
        ProcessBumpers (myList, myClues, guess_level);
        PuzzleSolver.CopyColToPuzzle (myPuzzle, myList, col);
    }

    private static void ProcessBumpersInRow (PBNPuzzle myPuzzle, int row, int guess_level)
    {
        if (myPuzzle == null) return;
        
        SetCurrentProcess (-1, row, guess_level, "ProcessBumpersInRow");          

        PuzzleSquare[] myList = PuzzleSolver.CopyRowFromPuzzle(myPuzzle, row);
        int myClues[] = new int[myPuzzle.GetRow_NClues(row)];
        for (int i=0; i<myPuzzle.GetRow_NClues(row); i++)
            myClues[i] = myPuzzle.GetRow_Clues (row, i);
        ProcessBumpers (myList, myClues, guess_level);
        PuzzleSolver.CopyRowToPuzzle (myPuzzle, myList, row);
    }

    private static void ProcessBumpers (PuzzleSquare[] myList, int[] clues, int guess_level)
    {
        if (myList == null || clues == null) return;

        ArrayList<Bumper> myBumperList = BumperUtilities.ExtractBumpersFromListForward(myList, 0);
        if (myBumperList == null || myBumperList.isEmpty()) return;

        // create a new bumper list with IDs so I can keep track of which is which
        ArrayList<Bumper> myBumpIDList = new ArrayList<Bumper>();
        for (int b=0; b<myBumperList.size(); b++)
        {
            Bumper bump = (Bumper)myBumperList.get(b);
            bump.SetID (b);
            myBumpIDList.add(bump);
        }

        // loop over each clue and make a list of each bumper the clue could fit in
        ArrayList[] cl_list = new ArrayList[clues.length];
        for (int cl=0; cl<clues.length; cl++)
        {
            int clue_val = clues[cl];
            cl_list[cl] = new ArrayList<Bumper>();
            for (int b=0; b<myBumpIDList.size(); b++)
            {
                Bumper bump = (Bumper)myBumpIDList.get(b);
                if (bump.GetLength() >= clue_val)
                    cl_list[cl].add(bump);
            }
        }

        // now process the list so that the bumper IDs always increase OR at least
        // remain the same if *all* of the clues with the same bumper ID can fit in
        // that particular bumper
        int last_id = -1;
        for (int cl=0; cl<clues.length; cl++)
        {
            // get the first bump in the list
            if (!cl_list[cl].isEmpty())
            {
                boolean all_done = false;
                int id = -1;
                while (!all_done)
                {
                    Bumper first_bump = (Bumper)cl_list[cl].get(0);
                    id = first_bump.GetID();

                    // remove it from the list if its id is smaller than the
                    // current one
                    if (id < last_id)
                        cl_list[cl].remove(first_bump);
                    else if (id == last_id)
                    {
                        // add up all clues with this ID, including minimum 1
                        // space between
                        int min_bump_len = 0;
                        int num_clues = 0;
                        for (int cl2=0; cl2<=cl; cl2++)
                        {
                            if (!cl_list[cl2].isEmpty())
                            {
                                Bumper thebump = (Bumper)cl_list[cl2].get(0);
                                if (thebump.GetID() == last_id)
                                {
                                    min_bump_len += clues[cl2];
                                    num_clues++;
                                }
                            }
                        }
                        min_bump_len += (num_clues-1);
                        // if we cannot fit these collective clues in this
                        // bumper, then remove it from the list
                        if (first_bump.GetLength() < min_bump_len)
                        {
                            cl_list[cl].remove(first_bump);
                            if (!cl_list[cl].isEmpty())
                            {
                                Bumper thebump = (Bumper)cl_list[cl].get(0);
                                last_id = thebump.GetID();
                            }
                        }

                        all_done = true;
                    } else
                    {
                        last_id = id;
                        all_done = true;
                    }
                    if (cl_list[cl].isEmpty()) all_done = true;
                }
            }
        }

        // now process the list going backwards so remove any bumpers that are
        // out of sequence
        last_id = myBumpIDList.size();
        for (int cl=(clues.length-1); cl>=0; cl--)
        {
            // remove all of the clues in the list that is > the last_id
            if (!cl_list[cl].isEmpty())
            {
                boolean all_done = false;
                int id = clues.length;
                while (!cl_list[cl].isEmpty() && !all_done)
                {
                    // start from the bottom
                    int size = cl_list[cl].size()-1;
                    Bumper last_bump = (Bumper)cl_list[cl].get(size);
                    id = last_bump.GetID();

                    // remove from the list if its id is larger than the current one
                    if (id > last_id)
                        cl_list[cl].remove(last_bump);
                    else if (id == last_id)
                    {
                        // add up all clues with this ID, including minimum 1
                        // space between
                        int min_bump_len = 0;
                        int num_clues = 0;
                        for (int cl2=cl; cl2<clues.length; cl2++)
                        {
                            if (!cl_list[cl2].isEmpty())
                            {
                                int last_one = cl_list[cl2].size()-1;
                                Bumper thebump = (Bumper)cl_list[cl2].get(last_one);
                                if (thebump.GetID() == last_id)
                                {
                                    min_bump_len += clues[cl2];
                                    num_clues++;
                                }
                            }
                        }
                        min_bump_len += (num_clues-1);
                        // if we cannot fit these collective clues in this
                        // bumper, then remove it from the list
                        if (last_bump.GetLength() < min_bump_len)
                        {
                            cl_list[cl].remove(last_bump);
                            if (!cl_list[cl].isEmpty())
                            {
                                int last_one = cl_list[cl].size()-1;
                                Bumper thebump = (Bumper)cl_list[cl].get(last_one);
                                last_id = thebump.GetID();
                            }
                        }
                        all_done = true;
                    }
                    else
                    {
                        last_id = id;
                        all_done = true;
                    }
                    if (cl_list[cl].isEmpty()) all_done = true;
                }
            }
        }

        // Figure out which clues uniquely must fall within each bump
        ArrayList<Integer> cluesInBump[] = new ArrayList[myBumpIDList.size()];
        for (Bumper bump : myBumpIDList)
        {
            int bump_id = bump.GetID();
            cluesInBump[bump_id] = new ArrayList<Integer>();
            for (int cl=0; cl<clues.length; cl++)
            {
                if (cl_list[cl].size() == 1)
                {
                    Bumper bum = (Bumper)cl_list[cl].get(0);
                    if (bum.GetID() == bump_id)
                    {
                        Integer myInt = new Integer(cl);
                        cluesInBump[bump_id].add(myInt);
                    }
                }
            }
        }

        // Now loop over each bumper and process all clues that
        // uniquely fit in it
        for (Bumper bump : myBumpIDList)
        {
            int bump_id = bump.GetID();
            if (cluesInBump[bump_id].size() > 0)
            {
                int clue_cnt = 0;
                int tempClues[] = new int[cluesInBump[bump_id].size()];
                for (Integer myInt : cluesInBump[bump_id])
                {
                    int cl = myInt.intValue();
                    tempClues[clue_cnt++] = clues[cl];
                }
                ProcessOverlaps (myList, bump.GetStartIndex(), bump.GetEndIndex(), tempClues, guess_level);
            }
        }

        /*
        // now only process clues if there's only one bumper left in its list
        // AND it can it can be overlap processed
        for (int cl=0; cl<clues.length; cl++)
        {
            if (cl_list[cl].size() == 1)
            {
                Bumper bump = (Bumper)cl_list[cl].get(0);

                if (bump.GetLength() < (clues[cl]*2))
                    FillInOverlapForward (myList, bump.GetStartIndex(), bump.GetEndIndex(), clues[cl], guess_level);
            }
        }
         * 
         */
    }

    public static void InitializeGuessVariables (PBNPuzzle myPuzzle)
    {
        if (myPuzzle == null) return;

        Date now = Calendar.getInstance().getTime();
        long now_time = now.getTime();
        random = new Random(now_time);
        
        int ncols = myPuzzle.GetCols();
        int nrows = myPuzzle.GetRows();

        // For GenerateNewGuessFromEdgeWithMostClueSquares
        int tot_clue_spaces_first_col = 0;
        if (myPuzzle.GetCol_NClues(0) > 0)
            for (int cl=0; cl<myPuzzle.GetCol_NClues(0); cl++)
                tot_clue_spaces_first_col += myPuzzle.GetCol_Clues(0, cl);

        int tot_clue_spaces_last_col = 0;
        if (myPuzzle.GetCol_NClues(ncols-1) > 0)
            for (int cl=0; cl<myPuzzle.GetCol_NClues(ncols-1); cl++)
                tot_clue_spaces_last_col += myPuzzle.GetCol_Clues(ncols-1, cl);

        int tot_clue_spaces_first_row = 0;
        if (myPuzzle.GetRow_NClues(0) > 0)
            for (int cl=0; cl<myPuzzle.GetRow_NClues(0); cl++)
                tot_clue_spaces_first_row += myPuzzle.GetRow_Clues(0, cl);

        int tot_clue_spaces_last_row = 0;
        if (myPuzzle.GetRow_NClues(nrows-1) > 0)
            for (int cl=0; cl<myPuzzle.GetRow_NClues(nrows-1); cl++)
                tot_clue_spaces_last_row += myPuzzle.GetRow_Clues(nrows-1, cl);

        // figure out where to start and what direction to go in
        int tot_max = tot_clue_spaces_first_col;

        cur_guess_col = 0;
        cur_guess_row = 0;
        incr_column_first = false;
        incr_col_dir = 1;
        incr_row_dir = 1;
        spiral_start_incr_dir = -1;
        spiral_start_row = nrows-1;
        spiral_start_col = 0;

        if (tot_clue_spaces_last_col > tot_max)
        {
            tot_max = tot_clue_spaces_last_col;
            cur_guess_col = ncols-1;
            cur_guess_row = 0;
            incr_column_first = false;
            incr_col_dir = -1;
            incr_row_dir = 1;
            spiral_start_incr_dir = 1;
            spiral_start_row = 0;
            spiral_start_col = ncols-1;
        }

        if (tot_clue_spaces_first_row > tot_max)
        {
            tot_max = tot_clue_spaces_first_row;
            cur_guess_col = 0;
            cur_guess_row = 0;
            incr_column_first = true;
            incr_col_dir = 1;
            incr_row_dir = 1;
            spiral_start_incr_dir = 1;
            spiral_start_row = 0;
            spiral_start_col = 0;
        }

        if (tot_clue_spaces_last_row > tot_max)
        {
            cur_guess_col = 0;
            cur_guess_row = nrows-1;
            incr_column_first = true;
            incr_col_dir = 1;
            incr_row_dir = -1;
            spiral_start_incr_dir = -1;
            spiral_start_row = nrows-1;
            spiral_start_col = ncols-1;
        }
        spiral_start_incr_column_first = incr_column_first;

        guess_vars_initialized = true;
    }

    public static Point GenerateNewGuess (PBNPuzzle myPuzzle)
    {
        if (!guess_vars_initialized) InitializeGuessVariables (myPuzzle);
		return GenerateGuessFromRowOrColWithFewestUnknowns (myPuzzle);
//        return GenerateGuessFromEdgeWithMostClueSquares (myPuzzle);
    }
	
	private static Point GenerateGuessFromRowOrColWithFewestUnknowns (PBNPuzzle myPuzzle)
	{
		int least_unknowns = myPuzzle.GetCols();
		if (myPuzzle.GetRows() > least_unknowns) least_unknowns = myPuzzle.GetRows();
		
		boolean is_row = true;
		int num = -1;
		for (int i=0; i<myPuzzle.GetCols(); i++)
		{
			int unknowns = myPuzzle.CountUnknownSquaresInCol(i);
			if (unknowns > 0 && unknowns < least_unknowns)
			{
				is_row = false;
				num = i;
				least_unknowns = unknowns;
			}
		}		
		for (int i=0; i<myPuzzle.GetRows(); i++)
		{
			int unknowns = myPuzzle.CountUnknownSquaresInRow(i);
			if (unknowns > 0 && unknowns < least_unknowns)
			{
				is_row = true;
				num = i;
				least_unknowns = unknowns;
			}
		}
		
		// now grab the first unknown square in our designated row or column
		if (is_row)
		{
			int col = 0;
			while (col < myPuzzle.GetCols() && !myPuzzle.GetPuzzleSquareAt(num, col).IsUnknown()) col++;
			assert (col < myPuzzle.GetCols());
			return (new Point (col, num));
		} else
		{
			int row = 0;
			while (row < myPuzzle.GetRows() && !myPuzzle.GetPuzzleSquareAt(row, num).IsUnknown()) row++;
			assert (row < myPuzzle.GetRows());
			return (new Point (num, row));
		}
	}

    private static Point GenerateSpiralGuess (PBNPuzzle myPuzzle)
    {
        int ncols = myPuzzle.GetCols();
        int nrows = myPuzzle.GetRows();
        int max_squares = ncols*nrows;

        boolean is_good = false;
        int row = spiral_start_row;
        int col = spiral_start_col;
        int spiral_col_max = ncols - 1;
        int spiral_col_min = 0;
        int spiral_row_max = nrows - 1;
        int spiral_row_min = 0;
        int spiral_incr_dir = spiral_start_incr_dir;
        boolean incr_col_first = spiral_start_incr_column_first;
        int num_turns = 0;
        int count = 0;
        // Clockwise!
        while (!is_good && count < max_squares)
        {
            if (myPuzzle.GetPuzzleSquareAt(row, col).IsUnknown()) is_good = true;
            else
            {
                // go to next square in spiral
                count++;
                if (incr_col_first)
                {
                    col += spiral_incr_dir;
                    if (col > spiral_col_max)
                    {
                        // need to make turn in upper right corner
                        num_turns++;
                        col = spiral_col_max;
                        row++;
                        incr_col_first = false;
                        spiral_incr_dir = 1;
                    } else if (col < spiral_col_min)
                    {
                        // need to make turn in lower left corner
                        num_turns++;
                        col = spiral_col_min;
                        row--;
                        incr_col_first = false;
                        spiral_incr_dir = -1;
                    }
                }
                else
                {
                    row += spiral_incr_dir;
                    if (row > spiral_row_max)
                    {
                        // need to make turn in lower right corner
                        num_turns++;
                        row = spiral_row_max;
                        col--;
                        incr_col_first = true;
                        spiral_incr_dir = -1;

                    } else if (row < spiral_row_min)
                    {
                        // need to make turn in upper left corner
                        num_turns++;
                        row = spiral_row_min;
                        col++;
                        incr_col_first = true;
                        spiral_incr_dir = 1;
                    }

                }

                if (num_turns == 4)
                {
                    // time to inset the spiral by 1
                    if ((spiral_row_max - spiral_row_min) > 1)
                    {
                        spiral_row_min++;
                        spiral_row_max--;
                    }
                    if ((spiral_col_max - spiral_col_min) > 1)
                    {
                        spiral_col_min++;
                        spiral_col_max--;
                    }
                    // adjust row col
                    if (row < spiral_row_min) row = spiral_row_min;
                    if (row > spiral_row_max) row = spiral_row_max;
                    if (col < spiral_col_min) col = spiral_col_min;
                    if (col > spiral_col_max) col = spiral_col_max;
                    num_turns = 0;
                }
            }
        }

        if (is_good)
            return new Point (col, row);
        else return null;
    }

    private static Point GenerateGuessFromEdgeWithMostClueSquares (PBNPuzzle myPuzzle)
    {
        int ncols = myPuzzle.GetCols();
        int nrows = myPuzzle.GetRows();
        int max_squares = ncols*nrows;

        boolean is_good = false;
        int row = cur_guess_row;
        int col = cur_guess_col;
        int count = 0;
        while (!is_good && count < max_squares)
        {
            if (myPuzzle.GetPuzzleSquareAt(row, col).IsUnknown()) is_good = true;
            else
            {
                count++;
                if (incr_column_first)
                    col += incr_col_dir;
                else
                    row += incr_row_dir;
                if (col == ncols)
                {
                    col = 0;
                    row += incr_row_dir;
                    if (row < 0) row = nrows-1;
                    if (row == nrows) row = 0;
                } else if (col < 0)
                {
                    col = ncols-1;
                    row += incr_row_dir;
                    if (row < 0) row = nrows-1;
                    if (row == nrows) row = 0;
                }
                if (row == nrows)
                {
                    row = 0;
                    col += incr_col_dir;
                    if (col < 0) col = ncols-1;
                    if (col == ncols) col = 0;
                } else if (row < 0)
                {
                    row = nrows-1;
                    col += incr_col_dir;
                    if (col < 0) col = ncols-1;
                    if (col == ncols) col = 0;
                }
            }
        }

        if (is_good)
            return new Point (col, row);
        else return null;
    }

    private static Point GenerateNewGuessFromUpperLeft (PBNPuzzle myPuzzle)
    {
        for (int r=0; r<myPuzzle.GetRows(); r++)
            for (int c=0; c<myPuzzle.GetCols(); c++)
                if (myPuzzle.GetPuzzleSquareAt(r, c).IsUnknown())
                    return new Point (c, r);
        return null;
    }

    private static Point GenerateRandomGuess (PBNPuzzle myPuzzle)
    {
        // generate a random row and col
        int row = random.nextInt();
        row = row%myPuzzle.GetRows();
        if (row < 0) row += myPuzzle.GetRows();
        int col = random.nextInt();
        col = col%myPuzzle.GetCols();
        if (col < 0) col += myPuzzle.GetCols();

        boolean is_good = false;
        while (!is_good)
        {
            if (myPuzzle.GetPuzzleSquareAt(row, col).IsUnknown()) is_good = true;
            else
            {
                col++;
                if (col == myPuzzle.GetCols())
                {
                    col = 0;
                    row++;
                    if (row == myPuzzle.GetRows()) row = 0;
                }
            }
        }
        return (new Point (col, row));
    }

    public static void SetDoStop (boolean stop)
    { do_stop = stop; }

    public static String GetLastMessage ()
    { return last_msg; }

    public static void InitializeLastMessage ()
    { last_msg =  UNKNOWN_ERROR; }

    public static boolean PuzzlesAreDifferent (PBNPuzzle startPuzzle,
            PBNPuzzle endPuzzle)
    {
        if (startPuzzle == null || endPuzzle == null)
        {
            PaintByNumberPro.HandleErrorMessage("Compare Puzzles Error",
                    "One of the puzzles is NULL");
            return false;
        }

        int rows, cols;
        rows = startPuzzle.GetRows();
        cols = startPuzzle.GetCols();
        if (rows != endPuzzle.GetRows() || cols != endPuzzle.GetCols())
        {
            PaintByNumberPro.HandleErrorMessage ("Compare Puzzles Error",
                    "Puzzle dimensions do not match!");
            return false;
        }

        for (int i=0; i<rows; i++)
        {
            for (int j=0; j<cols; j++)
            {
                PuzzleSquare startPS = startPuzzle.GetPuzzleSquareAt(i, j);
                PuzzleSquare endPS = endPuzzle.GetPuzzleSquareAt(i, j);
                if (startPS.IsUnknown() && !endPS.IsUnknown())
                {
                    PaintByNumberPro.HandleMessage ("Give Me a Clue",
                                "Something can be done in row " + i +
                                " col " + j);
                    startPuzzle.SetCurrentSelection(j, i);
                    return true;
                }
            }
        }

        return false;
    }
    
    public static void SetCurrentProcess (int c, int r, int gl, String msg)
    {
        current_col = c;
        current_row = r;
        current_guess_level = gl;
        current_process = msg;
    }
    
    public static String ReportCurrentProcess ()
    {
        return ("Current col row guess_level process: " + current_col + " " +
                current_row + " " + current_guess_level + " " + current_process);
    }
	
}
