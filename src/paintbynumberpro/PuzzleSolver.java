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
    private static boolean debug_detail = false;
    private static int target_row[] = null;
    private static int target_col[] = null;
    private static PuzzleSquare.SquareStatus target_status[] = null;
    private static boolean boolean_AND = false;
    private static PuzzleSquare[] prevList = null;
    private static PuzzleSquare[] newList = null;
    private static boolean do_stop = false;
    
    // vars for reporting current col/row being operated on
    private static int current_col = -1;
    private static int current_row = -1;
    private static int current_guess_level = -1;
    private static String current_process = "";
    
    // private new boolean type
    private enum BType { TRUE, FALSE, STRONG_FALSE };

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
        /*
        if (myPuzzle == null || myRow == null) return;
        for (int i=0; i<myPuzzle.GetCols(); i++)
            myPuzzle.SetPuzzleRowCol(row, i, myRow[i]);
            */
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

	public static boolean CheckPuzzleSoFar (PBNPuzzle myPuzzle, boolean for_solver)
	{ return CheckPuzzleSoFar (myPuzzle, for_solver, null); }
	
    public static boolean CheckPuzzleSoFar (PBNPuzzle myPuzzle, boolean for_solver, PuzzleSolverThread theThread)
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
			if (!bps.Better_CanSolutionFit (myPuzzle, true, r))
            {
                if (!for_solver && last_msg != null)
                    PaintByNumberPro.HandleErrorMessage (title, last_msg);
//				else
//					System.out.println ("Error in row " + r);
                return false;
            }
        }

        // Check cols next
        for (int c=0; c<myPuzzle.GetCols(); c++)
			if (!bps.Better_CanSolutionFit (myPuzzle, false, c))
            {
                if (!for_solver && last_msg != null)
                    PaintByNumberPro.HandleErrorMessage (title, last_msg);
//				else
//					System.out.println ("Error in col " + c);
                return false;
            }

        if (!for_solver)
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

    public static boolean CanSolutionFitInCol (PBNPuzzle myPuzzle, int col, boolean verbose)
    {
        if (myPuzzle.GetCol_NClues(col) == 0)
        {
            // Make sure we don't have any filled in squares
            for (int r=0; r<myPuzzle.GetRows(); r++)
                if (myPuzzle.GetPuzzleSquareAt (r, col).IsFilled())
                {
                    last_msg = "There can be no filled in squares in col " + col;
                    if (verbose)
                        PaintByNumberPro.HandleErrorMessage (title, last_msg);
                    return false;
                }
            return true;
        }
        else
        {
            // Make sure we have enough unknown and filled squares for all of our clues
            int total_clues_squares = 0;
            for (int i=0; i<myPuzzle.GetCol_NClues(col); i++)
                total_clues_squares += myPuzzle.GetCol_Clues (col, i);
            int total_unknown_and_filled = 0;
            for (int i=0; i<myPuzzle.GetRows(); i++)
                if (!myPuzzle.GetPuzzleSquareAt (i, col).IsEmpty())
                    total_unknown_and_filled++;
            if (total_unknown_and_filled < total_clues_squares)
            {
                last_msg = "There aren't enough unknown and filled squares in col " + col +
                            " to handle all of the clues";
                if (verbose)
                    PaintByNumberPro.HandleErrorMessage (title, last_msg);
                return false;
            }

            // Find first filled or unknown row (i.e. first square where the next clue can start)
            int cur_row = 0;
            PuzzleSquare ps = myPuzzle.GetPuzzleSquareAt(cur_row, col);
            cur_row++;
            while (ps.IsEmpty() && cur_row < myPuzzle.GetRows())
            {
                ps = myPuzzle.GetPuzzleSquareAt(cur_row, col);
                cur_row++;
            }
            assert (!ps.IsEmpty());
            cur_row--;
            
            // Now find the last possible square where we can start clue 0 from
            total_clues_squares += (myPuzzle.GetCol_NClues(col)-1);
            int slop = myPuzzle.GetRows() - total_clues_squares;
            if (cur_row > slop)
            {
                last_msg = myPuzzle.GetCol_NClues(col) + " clues cannot possibly fit in col " + col;
                if (verbose)
                    PaintByNumberPro.HandleErrorMessage (title, last_msg);
                return false;
            }

            // Copy the row into my own PuzzleSquare array so it can be manipulated
            PuzzleSquare myCol[] = CopyColFromPuzzle (myPuzzle, col);

            // Alright, see if we can fit the clues in from clue 0
            // starting at column cur_col-1 (which is either unknown or filled)
            boolean its_possible = false;
            boolean found_filled = false;
            for (int rr=cur_row; rr<=slop && !found_filled && !its_possible; rr++)
            {
                if (myCol[rr].IsFilled()) found_filled = true;
				BetterPuzzleSolver bps = new BetterPuzzleSolver(null);
				its_possible = bps.CanSolutionFitStartingFromClue(myPuzzle, myCol, false, col, rr, 0, 1);				
//                if (CanSolutionFitInColStartingFromClue(myPuzzle, myCol, col, rr, 0, verbose) == BType.TRUE) its_possible = true;
            }
            if (its_possible)
            {
                last_msg = "Col " + col + " looks good so far!";
                if (verbose)
                    PaintByNumberPro.HandleMessage (title, last_msg);
            } else
            {
//                System.out.println ("Error in CanSolutionFitInColFromRowClue...");
//                System.out.println (last_msg);
//                PuzzleStaticUtilities.DumpArray (myCol, "Internal col " + col + ": ");
                if (verbose)
                    PaintByNumberPro.HandleErrorMessage (title, last_msg);
            }
            return its_possible;
        }
    }

    public static boolean CanSolutionFitInRow (PBNPuzzle myPuzzle, int row, boolean verbose)
    {
        if (myPuzzle.GetRow_NClues(row) == 0)
        {
            // Make sure we don't have any filled in squares
            for (int c=0; c<myPuzzle.GetCols(); c++)
                if (myPuzzle.GetPuzzleSquareAt (row, c).IsFilled())
                {
                    last_msg = "There can be no filled in squares in row " + row;
                    if (verbose)
                        PaintByNumberPro.HandleErrorMessage (title, last_msg);
                    return false;
                }
            return true;
        }
        else 
        {
            // Make sure we have enough unknown and filled squares for all of our clues
            int total_clues_squares = 0;
            for (int i=0; i<myPuzzle.GetRow_NClues(row); i++)
                total_clues_squares += myPuzzle.GetRow_Clues (row, i);
            int total_unknown_and_filled = 0;
            for (int i=0; i<myPuzzle.GetCols(); i++)
                if (!myPuzzle.GetPuzzleSquareAt (row, i).IsEmpty())
                    total_unknown_and_filled++;
            if (total_unknown_and_filled < total_clues_squares)
            {
                last_msg = "There aren't enough unknown and filled squares in row " + row +
                            " to handle all of the clues";
                if (verbose)
                    PaintByNumberPro.HandleErrorMessage (title, last_msg);
                return false;
            }

            // Find first filled or unknown column (i.e. first square where the next clue can start)
            int cur_col = 0;
            PuzzleSquare ps = myPuzzle.GetPuzzleSquareAt(row, cur_col);
            cur_col++;
            while (ps.IsEmpty() && cur_col < myPuzzle.GetCols())
            {
                ps = myPuzzle.GetPuzzleSquareAt(row, cur_col);
                cur_col++;
            }
            assert (!ps.IsEmpty());
            cur_col--;

            // Now find the last possible square where we can start clue 0 from
            total_clues_squares += (myPuzzle.GetRow_NClues(row)-1);
            int slop = myPuzzle.GetCols() - total_clues_squares;
            if (cur_col > slop)
            {
                last_msg = myPuzzle.GetRow_NClues(row) + " clues cannot possibly fit in row " + row;
                if (verbose)
                    PaintByNumberPro.HandleErrorMessage (title, last_msg);
                return false;
            }

            // Copy the row into my own PuzzleSquare array so it can be manipulated
            PuzzleSquare myRow[] = CopyRowFromPuzzle (myPuzzle, row);

            // Alright, see if we can fit the clues in from clue 0
            // starting at column cur_col-1 (which is either unknown or filled)
            boolean its_possible = false;
            boolean found_filled = false;
            for (int cc=cur_col; cc<=slop && !found_filled && !its_possible; cc++)
            {
                if (myRow[cc].IsFilled()) found_filled = true;
				if (!myRow[cc].IsEmpty())	// Move on until we get to a new UNKNOWN or FILLED square
				{
					BetterPuzzleSolver bps = new BetterPuzzleSolver(null);
					its_possible = bps.CanSolutionFitStartingFromClue(myPuzzle, myRow, true, row, cc, 0, 1);
//					if (CanSolutionFitInRowStartingFromClue(myPuzzle, myRow, row, cc, 0, verbose, 1) == BType.TRUE) its_possible = true;

				}
			}
            if (its_possible)
            {
                last_msg = "Row " + row + " looks good so far!";
                if (verbose)
                    PaintByNumberPro.HandleErrorMessage (title, last_msg);
            } else
            {
//                System.out.println ("Error in CanSolutionFitInRowFromColClue...");
//                System.out.println (last_msg);
//                PuzzleStaticUtilities.DumpArray (myRow, "Internal row " + row + ": ");
                if (verbose)
                    PaintByNumberPro.HandleErrorMessage (title, last_msg);
            }
            return its_possible;
        }
    }

    // This method is used recursively!
    // Do NOT change the actual puzzle, but operate on myRow instead
    private static BType CanSolutionFitInRowStartingFromClue (PBNPuzzle myPuzzle, PuzzleSquare[] myRow,
            int row, int cur_col, int cur_clue, boolean verbose, int recursion_level)
    {
        assert (!myRow[cur_col].IsEmpty());

        boolean started_filled = myPuzzle.GetPuzzleSquareAt (row, cur_col).IsFilled();
        
        // first let's see if there are *any* stretches of continuous unknown and filled spaces
        // that will accommodate our clue from the current column
        int clue_len = myPuzzle.GetRow_Clues(row, cur_clue); 
        if (clue_len > 1)
        {
            int cur_stretch = 0;        

            int[] theClues = myPuzzle.GetRow_Clues(row);
            int nclues = myPuzzle.GetRow_NClues(row);    
            int num_needed[] = GetMinSpacesNeededPastThisClue (theClues, nclues, cur_clue);
            int clue_sum = num_needed[0];
            int separators = num_needed[1];
            int num_spaces_needed = clue_sum+separators;
            if (num_spaces_needed > 0)
            {
                // When we get to the very first stretch of unknowns and fills that we can put our clue
                // into, then we'll just check to make sure we have enough available spaces to the right
                // of the clue for the rest of the clues
                boolean found_a_spot = false;
                for (int i=cur_col; i<myPuzzle.GetCols() && !found_a_spot; i++)
                {
                    if (myRow[i].IsEmpty())
                        cur_stretch = 0;
                    else 
                    {
                        cur_stretch++;
                        if (cur_stretch == clue_len)
                        {
                   
                            // now check to see if we have enough unknown or filled spaces available
                            // to the right of where we are right now (we don't care if the order
                            // is right or not, we just want the overall count)
                            int count_avail = 0;
                            int count_separators = 0;
                            int cur_stretch_empty = 0;
                            int cols = myPuzzle.GetCols();
                            if (num_spaces_needed > 0 && i < (cols-num_spaces_needed))
                            {
                                for (int j=i+1; j<cols; j++)
                                {
                                    if (!myRow[j].IsEmpty()) // unknowns and filled
                                    {
                                        count_avail++;
                                        if (cur_stretch_empty > 0)  // just ended a stretch of X's
                                        {
                                            count_separators++;
                                            cur_stretch_empty = 0;
                                        }
                                        if (myRow[j].IsUnknown())   // a U may be a separator if surrounded by Unknowns and/or Filled
                                        {
                                            int count = 0;
                                            if (j > 0 && !myRow[j-1].IsEmpty()) count++;
                                            if (j < (cols-1) && !myRow[j+1].IsEmpty()) count++;
                                            if (count == 2) count_separators++;
                                        }
                                    } else                  // X
                                    {
                                        cur_stretch_empty++;
                                    }
                                }
                            }

                            if (count_avail < clue_sum || count_separators < separators)
                            {
                                last_msg = "In row " + row + ", clue " + cur_clue + " could fit ending at column " + i + ",\n" +
                                        " but rest of clues will not fit to the right";                           
                                return BType.STRONG_FALSE;
                            } else found_a_spot = true;;
                        }       
                    }
                }
                if (!found_a_spot)
                {
                    last_msg = "In row " + row + ", clue " + cur_clue + " does not fit anywhere starting from col " + cur_col;
                    return BType.STRONG_FALSE;
                }
            }
        } 

        // okay, so let's start the current clue in the current column and see what happens
        int col = cur_col;
        int num_filled = 0;
        int num_already_filled = 0;
        boolean success = true;
        for (int i=0; i<myPuzzle.GetRow_Clues(row, cur_clue) && success; i++)
        {
            // let's set the current column to filled if we can (if we can't then we need to move on)
            if (!myRow[col].IsEmpty())
            {
                if (myRow[col].IsFilled()) num_already_filled++;
                col++;
                num_filled++;
                if (col == myPuzzle.GetCols() && num_filled < myPuzzle.GetRow_Clues(row,cur_clue))
                    success = false;
            } else
                success = false;
        }
        
        // if we succeeded...
        if (success)
        {
            // If we're at the end of the row AND the current clue is the last one, then we're all done
            if (col == myPuzzle.GetCols())
            {
                if (cur_clue == (myPuzzle.GetRow_NClues(row) - 1)) 
                {
                    last_msg = "In row " + row + ", clue " + cur_clue + " fits at the end of the row";
                    return BType.TRUE;
                }
                else 
                {
                    last_msg = "ACK In row " + row + ", we got to end of row before finding all of the clues";
                    if (verbose)
                        PaintByNumberPro.HandleMessage (title, last_msg);
                     return BType.FALSE;
                }

            // We should be able to fill the next square in with an X
            } else
            {
                // If the next square is already filled, then the clue didn't fit starting at the
                // current location
                if (myRow[col].IsFilled())
                {
                    // If we started out with a filled square, then this is a true failure
                    if (started_filled)
                    {
                        last_msg = "ACK In row " + row + ", clue " + cur_clue + " does NOT fit starting at column " +
                                    cur_col;
                        if (verbose)
                            PaintByNumberPro.HandleMessage (title, last_msg);
                        return BType.FALSE;
                    }

                    // But if we started out with an UNKNOWN square, then we can advance to the
                    // next UNKNOWN or FILLED square and try again
                    else
                    {
                        while (true)
                        {
                            int new_cur_col = AdvanceToNextUnknownOrFilledSpaceForward (myRow, cur_col+1);
                            // If there are no more non-empty squares, then we're done for!
                            if (new_cur_col == myPuzzle.GetCols())
                            {
                                last_msg = "ACK In row " + row + ", clue " + cur_clue + " did NOT fit starting at column " +
                                            cur_col + "\nbut cannot find a new location to start fitting in the clue";
                                if (verbose)
                                    PaintByNumberPro.HandleMessage (title, last_msg);
                                return BType.FALSE;
                            }
                            started_filled = myPuzzle.GetPuzzleSquareAt (row, new_cur_col).IsFilled();
                        
                            // If we can fit the rest of the clues in the rest of the line, then we're done!
                            BType can_do = CanSolutionFitInRowStartingFromClue (myPuzzle, myRow, row, new_cur_col, cur_clue, verbose, recursion_level+1);
                            if (can_do == BType.TRUE)
                            {
                                last_msg = "In row " + row + " we can put clue " + cur_clue + " starting at col " +
                                        new_cur_col;
                                return can_do;
                            } else if (can_do == BType.STRONG_FALSE)
                            {
                                last_msg = "In row " + row + " there is no way we can put clue " + cur_clue + " starting at col " +
                                        new_cur_col;                                
                                return can_do;
                            // Otherwise, we'll try again by moving to the next non-empty square as a starting point
                            } else if (started_filled) 
                            {
                                last_msg = "ACK In row " + row + " clue " + cur_clue + " does NOT fit starting at column " +
                                        new_cur_col + "\nbecause first square was FILLED";
                                if (verbose)
                                    PaintByNumberPro.HandleMessage (title, last_msg);
                                return BType.FALSE;
                            }
                            else cur_col++;
                        }
                    }

                // Fill in the next square as Empty and advance to the next clue and starting
                // at the next UNKNOWN or FILLED square past the one we just filled in
                } else
                {
                    // Figuratively fill in the next square with X and skip ahead
                    col++;
                    // If we've gotten to the end of the row and we haven't found all of the
                    // clues, then we're done for
                    if (col == myPuzzle.GetCols() && cur_clue < (myPuzzle.GetRow_NClues(row)-1)) return BType.FALSE;
                    // Otherwise, move on to the next clue...
                    int new_cur_clue = cur_clue+1;
                    // If we've gotten to the last clue then we may be good
                    if (new_cur_clue == myPuzzle.GetRow_NClues(row)) 
                    {
                        // Make sure rest of the row doesn't contain any fills
                        for (int c=col; c<myPuzzle.GetCols(); c++)
                            if (myPuzzle.GetPuzzleSquareAt (row, c).IsFilled())
                            {
                                last_msg = "In row " + row + ", we found filled squares past the end of the last clue!";
                                if (verbose)
                                    PaintByNumberPro.HandleMessage (title, last_msg);
                                return BType.FALSE;
                            }
                        last_msg = "In row " + row + ", we found places for all the clues!";
                        return BType.TRUE;
                    }
                    while (true)
                    {
                        // Move on to the next UNKNOWN or FILLED square
                        int new_cur_col = AdvanceToNextUnknownOrFilledSpaceForward (myRow, col);
                        // If there are no more non-empty spquares, then we're done for!
                        if (new_cur_col == myPuzzle.GetCols())
                        {
                            last_msg = "ACK In row " + row + ", can't find a place to put clue " +
                                        new_cur_clue;
                            if (verbose)
                                PaintByNumberPro.HandleMessage (title, last_msg);
                            return BType.FALSE;
                        }
                        started_filled = myPuzzle.GetPuzzleSquareAt (row, new_cur_col).IsFilled();

                        BType can_do = CanSolutionFitInRowStartingFromClue (myPuzzle, myRow, row, new_cur_col, new_cur_clue, verbose, recursion_level+1);
                        if (can_do == BType.TRUE)
                        {
                            last_msg = "In row " + row + " we can put clue " + new_cur_clue + " starting at col " +
                                    new_cur_col;
                            return can_do;
                        } else if (can_do == BType.STRONG_FALSE)
                        {
                            last_msg = "In row " + row + " there is no way we can put clue " + new_cur_clue + " starting at col " +
                                    new_cur_col;                            
                            return can_do;
                        } else if (started_filled) 
                        {
                            last_msg = "ACK In row " + row + " clue " + new_cur_clue + " does NOT fit starting at column " +
                                    new_cur_col + "\nbecause first square was FILLED";
                            if (verbose)
                                PaintByNumberPro.HandleMessage (title, last_msg);
                            return BType.FALSE;
                        }
                        else col++;
                    }
                }
            }

        } else

        // else if we failed...
        {

            // If we're at the end of the row, then we're done for
            if (col == myPuzzle.GetCols()) 
            {
                last_msg = "ACK In row " + row + ", unable to find a place for clue " +
                            cur_clue;
                if (verbose)
                    PaintByNumberPro.HandleMessage (title, last_msg);
                return BType.FALSE;
            }

            // If there were some filled-in pixels and we were unable to put our
            // clue in the spot, then this is a true failure
            else if (num_already_filled > 0)
            {
                last_msg = "ACK In row " + row + ", clue " + cur_clue + " does not fit starting in column " +
                        cur_col;
                if (verbose)
                    PaintByNumberPro.HandleMessage (title, last_msg);
                return BType.FALSE;
            // If we're NOT at the end of the row, then we need to see if we can stick the
            // current clue into the NEXT non-filled space
            }  else
            {
                // If we started out with a filled square, then this is a true failure
                if (started_filled)
                {
                    last_msg = "ACK In row " + row + ", clue " + cur_clue + " does not fit at column " +
                                cur_col;
                    if (verbose)
                        PaintByNumberPro.HandleMessage (title, last_msg);
                    return BType.FALSE;
                }

                // But if we started out with an UNKNOWN square, then we can advance to the
                // next UNKNOWN or FILLED square and try again
                else
                {
                    while (true)
                    {
                        int new_cur_col = AdvanceToNextUnknownOrFilledSpaceForward (myRow, cur_col+1);
                        // If there are no more non-empty squares, then we're done for!
                        if (new_cur_col == myPuzzle.GetCols())
                        {
                            last_msg = "ACK In row " + row + ", unable to find a place to put clue " +
                                        cur_clue;
                            if (verbose)
                                PaintByNumberPro.HandleMessage (title, last_msg);
                            return BType.FALSE;
                        }
                        started_filled = myPuzzle.GetPuzzleSquareAt(row, new_cur_col).IsFilled();

                        BType can_do = CanSolutionFitInRowStartingFromClue (myPuzzle, myRow, row, new_cur_col, cur_clue, verbose, recursion_level+1);
                        if (can_do == BType.TRUE)
                        {
                            last_msg = "In row " + row + " we can put clue " + cur_clue + " starting at col " +
                                    new_cur_col;
                            return can_do;
                        } else if (can_do == BType.STRONG_FALSE)
                        {
                            last_msg = "In row " + row + " there is no way we can put clue " + cur_clue + " starting at col " +
                                    new_cur_col;
                            return can_do;
                        }
                        else if (started_filled) 
                        {
                            last_msg = "ACK In row " + row + " clue " + cur_clue + " does NOT fit starting at column " +
                                    new_cur_col + "\nbecause first square was FILLED";
                            if (verbose)
                                PaintByNumberPro.HandleMessage (title, last_msg);
                            return BType.FALSE;
                        }
                        else cur_col++;
                    }
                }
            }
        }
    }

    // This method is used recursively!
    // Do NOT change the actual puzzle, but operate on myRow instead
    private static BType CanSolutionFitInColStartingFromClue (PBNPuzzle myPuzzle, PuzzleSquare[] myCol,
            int col, int cur_row, int cur_clue, boolean verbose)
    {
        assert (!myCol[cur_row].IsEmpty());

        boolean started_filled = myPuzzle.GetPuzzleSquareAt (cur_row, col).IsFilled();
        
        // first let's see if there are *any* stretches of continuous unknown and filled spaces
        // that will accommodate our clue from the current column
        int clue_len = myPuzzle.GetCol_Clues(col, cur_clue); 
        if (clue_len > 1)
        {
            int cur_stretch = 0;        

            int[] theClues = myPuzzle.GetCol_Clues(col);
            int nclues = myPuzzle.GetCol_NClues(col);    
            int num_needed[] = GetMinSpacesNeededPastThisClue (theClues, nclues, cur_clue);
            int clue_sum = num_needed[0];
            int separators = num_needed[1];
            int num_spaces_needed = clue_sum+separators;
            if (num_spaces_needed > 0)
            {
                // When we get to the very first stretch of unknowns and fills that we can put our clue
                // into, then we'll just check to make sure we have enough available spaces to the right
                // of the clue for the rest of the clues
                boolean found_a_spot = false;
                for (int i=cur_row; i<myPuzzle.GetRows() && !found_a_spot; i++)
                {
                    if (myCol[i].IsEmpty())
                        cur_stretch = 0;
                    else 
                    {
                        cur_stretch++;
                        if (cur_stretch == clue_len)
                        {
                   
                            // now check to see if we have enough unknown or filled spaces available
                            // to the right of where we are right now (we don't care if the order
                            // is right or not, we just want the overall count)
                            int count_avail = 0;
                            int count_separators = 0;
                            int cur_stretch_empty = 0;
                            int rows = myPuzzle.GetRows();
                            if (num_spaces_needed > 0 && i < (rows-num_spaces_needed))
                            {
                                for (int j=i+1; j<rows; j++)
                                {
                                    if (!myCol[j].IsEmpty()) // unknowns and filled
                                    {
                                        count_avail++;
                                        if (cur_stretch_empty > 0)  // just ended a stretch of X's
                                        {
                                            count_separators++;
                                            cur_stretch_empty = 0;
                                        }
                                        if (myCol[j].IsUnknown())   // a U may be a separator if surrounded by Unknowns and/or Filled
                                        {
                                            int count = 0;
                                            if (j > 0 && !myCol[j-1].IsEmpty()) count++;
                                            if (j < (rows-1) && !myCol[j+1].IsEmpty()) count++;
                                            if (count == 2) count_separators++;
                                        }
                                    } else                  // X
                                    {
                                        cur_stretch_empty++;
                                    }
                                }
                            }

                            if (count_avail < clue_sum || count_separators < separators)
                            {
                                last_msg = "In column " + col + ", clue " + cur_clue + " could fit ending at row " + i + ",\n" +
                                        " but rest of clues will not fit to the bottom";                           
                                return BType.STRONG_FALSE;
                            } else found_a_spot = true;;
                        }       
                    }
                }
                if (!found_a_spot)
                {
                    last_msg = "In column " + col + ", clue " + cur_clue + " does not fit anywhere starting from row " + cur_row;
                    return BType.STRONG_FALSE;
                }
            }
        } 

        // okay, so let's start the current clue in the current row and see what happens
        int row = cur_row;
        int num_filled = 0;
        int num_already_filled = 0;
        boolean success = true;
        int clue_val = myPuzzle.GetCol_Clues (col, cur_clue);
        for (int i=0; i<clue_val && success; i++)
        {
            // let's set the current row to filled if we can (if we can't then we need to move on)
            if (!myCol[row].IsEmpty())
            {
                if (myCol[row].IsFilled()) num_already_filled++;
                row++;
                num_filled++;
                if (row == myPuzzle.GetRows() && num_filled < clue_val)
                    success = false;
            } else
                success = false;
        }

        // if we succeeded...
        if (success)
        {
            // If we're at the end of the column AND the current clue is the last one, then we're all done
            if (row == myPuzzle.GetRows())
            {
                if (cur_clue == (myPuzzle.GetCol_NClues(col)-1))
                {
                    last_msg = "In col " + col + ", clue " + cur_clue + " fits at the bottom of the column";
                    return BType.TRUE;
                }
                else
                {
                    last_msg = "ACK In col " + col + ", we got to bottom of the column before finding all of the clues";
                    if (verbose)
                        PaintByNumberPro.HandleMessage (title, last_msg);
                     return BType.FALSE;
                }

            // We should be able to fill the next square in with an X
            } else
            {
                // If the next square is already filled, then the clue didn't fit starting at the
                // current location
                if (myCol[row].IsFilled())
                {
                    // If we started out with a filled square, then this is a true failure
                    if (started_filled)
                    {
                        last_msg = "ACK In col " + col + ", clue " + cur_clue + " does NOT fit starting at row " +
                                    cur_row;
                        if (verbose)
                            PaintByNumberPro.HandleMessage (title, last_msg);
                        return BType.FALSE;
                    }

                    // But if we started out with an UNKNOWN square, then we can advance to the
                    // next UNKNOWN or FILLED square and try again
                    else
                    {
                        while (true)
                        {
                            int new_cur_row = AdvanceToNextUnknownOrFilledSpaceForward (myCol, cur_row+1);
                            // If there are no more non-empty squares, then we're done for!
                            if (new_cur_row == myPuzzle.GetRows())
                            {
                                last_msg = "ACK In col " + col + ", clue " + cur_clue + " did NOT fit starting at row " +
                                            cur_row + "\nbut cannot find a new location to start fitting in the clue";
                                if (verbose)
                                    PaintByNumberPro.HandleMessage (title, last_msg);
                                return BType.FALSE;
                            }
                            started_filled = myPuzzle.GetPuzzleSquareAt (new_cur_row, col).IsFilled();

                            BType can_do = CanSolutionFitInColStartingFromClue (myPuzzle, myCol, col, new_cur_row, cur_clue, verbose);
                            if (can_do == BType.TRUE)
                            {
                                last_msg = "In col " + col + " we can fit clue " + cur_clue + " starting in row " +
                                    new_cur_row;
                                return can_do;
                            } else if (can_do == BType.STRONG_FALSE)
                            {
                                last_msg = "In col " + col + " there is no way we can fit clue " + cur_clue + " starting in row " +
                                    new_cur_row;
                                return can_do;     
                            } else if (started_filled)
                            {
                                last_msg = "ACK In col " + col + " clue " + cur_clue + " does NOT fit starting at column " +
                                        new_cur_row + "\nbecause first square was FILLED";
                                if (verbose)
                                    PaintByNumberPro.HandleMessage (title, last_msg);
                                return BType.FALSE;
                            }
                            else cur_row++;
                        }
                    }

                // Fill in the next square as Empty and advance to the next clue and starting
                // at the next UNKNOWN or FILLED square past the one we just filled in
                } else
                {
                    // Figuratively fill in the next square with X and skip ahead
                    row++;
                    // If we've gotten to the end of the column and we haven't found all of the
                    // clues, then we're done for
                    if (row == myPuzzle.GetRows() && cur_clue < (myPuzzle.GetCol_NClues(col)-1)) return BType.FALSE;
                    // Otherwise, move on to the next clue...
                    int new_cur_clue = cur_clue+1;
                    // If we've gotten to the last clue then we're good!
                    if (new_cur_clue == myPuzzle.GetCol_NClues(col))
                    {
                        // Make sure rest of the col doesn't contain any fills
                        for (int r=row; r<myPuzzle.GetRows(); r++)
                            if (myPuzzle.GetPuzzleSquareAt (r, col).IsFilled())
                            {
                                last_msg = "In col " + col + ", we found filled squares past the end of the last clue!";
                                if (verbose)
                                    PaintByNumberPro.HandleMessage (title, last_msg);
                                return BType.FALSE;
                            }
                        last_msg = "In col " + col + ", we found places for all the clues!";
                        return BType.TRUE;
                    }
                    while (true)
                    {
                        // Move on to the next UNKNOWN or FILLED square
                        int new_cur_row = AdvanceToNextUnknownOrFilledSpaceForward (myCol, row);
                        // If there are no more non-empty spquares, then we're done for!
                        if (new_cur_row == myPuzzle.GetRows())
                        {
                            last_msg = "ACK In col " + col + ", can't find a place to put clue " +
                                        new_cur_clue;
                            if (verbose)
                                PaintByNumberPro.HandleMessage (title, last_msg);
                            return BType.FALSE;
                        }
                        started_filled = myPuzzle.GetPuzzleSquareAt(new_cur_row, col).IsFilled();

                        BType can_do = CanSolutionFitInColStartingFromClue (myPuzzle, myCol, col, new_cur_row, new_cur_clue, verbose);
                        if (can_do == BType.TRUE)
                        {
                            last_msg = "In col " + col + " we can fit clue " + new_cur_clue + " starting in row " +
                                    new_cur_row;
                            return can_do;
                        }
                        else if (can_do == BType.STRONG_FALSE)
                        {
                            last_msg = "In col " + col + " thyere is no way we can fit clue " + new_cur_clue + " starting in row " +
                                    new_cur_row;
                            return can_do;
                        }
                        else if (started_filled)
                        {
                            last_msg = "ACK In col " + col + " clue " + new_cur_clue + " does NOT fit starting at column " +
                                    new_cur_row + "\nbecause first square was FILLED";
                            if (verbose)
                                PaintByNumberPro.HandleMessage (title, last_msg);
                            return BType.FALSE;
                        } else
                            row++;
                    }
                }
            }

        } else

        // else if we failed...
        {

            // If we're at the end of the column, then we're done for
            if (row == myPuzzle.GetRows())
            {
                last_msg = "ACK In col " + col + ", unable to find a place for clue " +
                            cur_clue;
                if (verbose)
                    PaintByNumberPro.HandleMessage (title, last_msg);
                return BType.FALSE;
            }

            // If the spot we were trying to squeeze the current clue into already
            // had some filled spots, then this was a true failure
            else if (num_already_filled > 0)
            {
                last_msg = "ACK In col " + col + ", unable to put clue " + cur_clue + " into space starting at row " +
                            cur_row;
                if (verbose)
                    PaintByNumberPro.HandleMessage (title, last_msg);
                return BType.FALSE;

            // If we're NOT at the end of the row, then we need to see if we can stick the
            // current clue into the NEXT non-filled space
            } else
            {
                // If we started out with a filled square, then this is a true failure
                if (started_filled)
                {
                    last_msg = "ACK In col " + col + ", clue " + cur_clue + " does not fit at row " +
                                cur_row;
                    if (verbose)
                        PaintByNumberPro.HandleMessage (title, last_msg);
                    return BType.FALSE;
                }

                // But if we started out with an UNKNOWN square, then we can advance to the
                // next UNKNOWN or FILLED square and try again
                else
                {
                    while (true)
                    {
                        int new_cur_row = AdvanceToNextUnknownOrFilledSpaceForward (myCol, cur_row+1);
                        // If there are no more non-empty squares, then we're done for!
                        if (new_cur_row == myPuzzle.GetRows())
                        {
                            last_msg = "ACK In col " + col + ", unable to find a place to put clue " +
                                        cur_clue;
                            if (verbose)
                                PaintByNumberPro.HandleMessage (title, last_msg);
                            return BType.FALSE;
                        }
                        started_filled = myPuzzle.GetPuzzleSquareAt (new_cur_row, col).IsFilled();

                        BType can_do = CanSolutionFitInColStartingFromClue (myPuzzle, myCol, col, new_cur_row, cur_clue, verbose);
                        if (can_do == BType.TRUE)
                        {
                            last_msg = "In col " + col + " we can fit clue " + cur_clue + " starting in row " +
                                    new_cur_row;
                            return can_do;
                        }
                        else if (can_do == BType.STRONG_FALSE)
                        {
                            last_msg = "In col " + col + " there is no we can fit clue " + cur_clue + " starting in row " +
                                    new_cur_row;
                            return can_do;
                        }
                        else if (started_filled) 
                        {
                            last_msg = "ACK In col " + col + " clue " + cur_clue + " does NOT fit starting at column " +
                                    new_cur_row + "\nbecause first square was FILLED";
                            if (verbose)
                                PaintByNumberPro.HandleMessage (title, last_msg);
                            return BType.FALSE;
                        }
                        else cur_row++;
                    }
                }
            }
        }
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
            if (debug_detail) prevList = CopyRowFromPuzzle (myPuzzle, r);
            CleanUpUnknownsInRow (myPuzzle, r, guess_level);
            if (TargetMet (myPuzzle))
            {
                newList = CopyRowFromPuzzle (myPuzzle, r);
                DumpPrevAndNewLists ("CleanUpUnknowns row " + r);
                PaintByNumberPro.HandleMessageForSolver ("Debugging Target Met",
                        "At CleanUpUnknowns row " + r);
            }
        }
        for (int c=0; c<myPuzzle.GetCols() && !do_stop; c++)
        {
            if (debug_detail) prevList = CopyColFromPuzzle (myPuzzle, c);
            CleanUpUnknownsInCol (myPuzzle, c, guess_level);
            if (TargetMet (myPuzzle))
            {
                newList = CopyRowFromPuzzle (myPuzzle, c);
                DumpPrevAndNewLists ("CleanUpUnknowns col " + c);
                PaintByNumberPro.HandleMessageForSolver ("Debugging Target Met",
                        "At CleanUpUnknowns col " + c);
            }
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
            if (debug_detail) prevList = CopyRowFromPuzzle (myPuzzle, r);
            success = ProcessRowForwardFromEdge (myPuzzle, r, guess_level, verbose);
            if (TargetMet (myPuzzle))
            {
                newList = CopyRowFromPuzzle (myPuzzle, r);
                DumpPrevAndNewLists("ProcessLeftColumn row " + r);
                PaintByNumberPro.HandleMessageForSolver ("Debugging Target Met",
                        "At ProcessLeftColumn row " + r);
            }
        }
        return success;
    }

    public static boolean ProcessRightColumn (PBNPuzzle myPuzzle, int guess_level, boolean verbose)
    {
        if (myPuzzle == null) return false;

        boolean success = true;
        for (int r=0; r<myPuzzle.GetRows() && success && !do_stop; r++)
        {
            if (debug_detail) prevList = CopyRowFromPuzzle (myPuzzle, r);
            success = ProcessRowBackwardFromEdge (myPuzzle, r, guess_level, verbose);
            if (TargetMet (myPuzzle))
            {
                newList = CopyRowFromPuzzle (myPuzzle, r);
                DumpPrevAndNewLists("ProcessRightColumn row " + r);
                PaintByNumberPro.HandleMessageForSolver ("Debugging Target Met",
                        "At ProcessRightColumn row " + r);
            }
        }
        return success;
    }

    public static boolean ProcessTopRow (PBNPuzzle myPuzzle, int guess_level, boolean verbose)
    {
        if (myPuzzle == null) return false;

        boolean success = true;
        for (int c=0; c<myPuzzle.GetCols() && success && !do_stop; c++)
        {
            if (debug_detail) prevList = CopyColFromPuzzle (myPuzzle, c);
            success = ProcessColumnForwardFromEdge (myPuzzle, c, guess_level, verbose);
            if (TargetMet (myPuzzle))
            {
                newList = CopyColFromPuzzle (myPuzzle, c);
                DumpPrevAndNewLists ("ProcessTopRow col " + c);
                PaintByNumberPro.HandleMessageForSolver ("Debugging Target Met",
                        "At ProcessTopRow col " + c);
            }
        }
        return success;
    }

    public static boolean ProcessBottomRow (PBNPuzzle myPuzzle, int guess_level, boolean verbose)
    {
        if (myPuzzle == null) return false;

        boolean success = true;
        for (int c=0; c<myPuzzle.GetCols() && success && !do_stop; c++)
        {
            if (debug_detail) prevList = CopyColFromPuzzle (myPuzzle, c);
            success = ProcessColumnBackwardFromEdge (myPuzzle, c, guess_level, verbose);
            if (TargetMet (myPuzzle))
            {
                newList = CopyColFromPuzzle (myPuzzle, c);
                DumpPrevAndNewLists ("ProcessBottomRow col " + c);
                PaintByNumberPro.HandleMessageForSolver ("Debugging Target Met",
                        "At ProcessBottomRow col " + c);
            }
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

    public static boolean ProcessBlobsInColFromPopup (PBNPuzzle myPuzzle, int c)
    {
        boolean is_good = ProcessBlobsInCol (myPuzzle, c, myPuzzle.GetGuessLevel());
        if (is_good)
        {
            PaintByNumberPro.HandleMessage ("Processing Blobs", "Completed for col " + c + " without errors");
        } else
        {
            PaintByNumberPro.HandleErrorMessage (title, last_msg);
        }
        return is_good;
    }

    public static boolean ProcessBlobsInRowFromPopup (PBNPuzzle myPuzzle, int r)
    {
        boolean is_good = ProcessBlobsInRow (myPuzzle, r, myPuzzle.GetGuessLevel());
        if (is_good)
        {
            PaintByNumberPro.HandleMessage ("Processing Blobs", "Completed for row " + r + " without errors");
        } else
        {
            PaintByNumberPro.HandleErrorMessage (title, last_msg);
        }
        return is_good;
    }

    public static void CleanUpUnknownsInColFromPopup (PBNPuzzle myPuzzle, int c)
    {
        if (myPuzzle == null) return;
        CleanUpUnknownsInCol (myPuzzle, c, myPuzzle.GetGuessLevel());
    }

    public static void CleanUpUnknownsInRowFromPopup (PBNPuzzle myPuzzle, int r)
    {
        if (myPuzzle == null) return;
        CleanUpUnknownsInRow (myPuzzle, r, myPuzzle.GetGuessLevel());
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

    public static boolean ProcessSingleClueInRowFromPopup (PBNPuzzle myPuzzle, int row)
    {
        if (myPuzzle == null) return false;
        int guess_level = myPuzzle.GetGuessLevel();
        boolean is_good = ProcessSingleClueInRow (myPuzzle, row, guess_level, false);
        if (is_good)
        {
            if (myPuzzle.GetRow_NClues(row) == 1)
                PaintByNumberPro.HandleMessage("Single Clue Processing", "Single clue in row " + row + " completed");
            else
                PaintByNumberPro.HandleMessage("Single Clue Processing", "Too many clues in row " + row + " for this process");
        }
        else
        {
            PaintByNumberPro.HandleErrorMessage(title, last_msg);
        }
        return is_good;
    }

    public static boolean ProcessSingleClueInColFromPopup (PBNPuzzle myPuzzle, int col)
    {
        if (myPuzzle == null) return false;
        int guess_level = myPuzzle.GetGuessLevel();
        boolean is_good = ProcessSingleClueInCol (myPuzzle, col, guess_level, false);
        if (is_good)
        {
            if (myPuzzle.GetCol_NClues(col) == 1)
                PaintByNumberPro.HandleMessage("Single Clue Processing", "Single clue in col " + col + " completed");
            else
                PaintByNumberPro.HandleMessage("Single Clue Processing", "Too many clues in col " + col + " for this process");
        }
        else
        {
            PaintByNumberPro.HandleErrorMessage(title, last_msg);
        }
        return is_good;
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

    private static boolean FillInOverlapForward (PuzzleSquare[] myList, int start, int end, int clue, int guess_level)
    {
        if (myList == null) return false;
        if (end < start) return false;

        int len = end - start + 1;
        int slop = len - clue;
        if (slop < 0) return false;
/*
        if (slop > 0)
        {
            boolean start_filling = false;
            for (int i=start; i<(start+slop); i++)
            {
                if (myList[i].IsFilled()) start_filling = true;
                if (start_filling && myList[i].IsUnknown())
                    myList[i].SetStatus(PuzzleSquare.SquareStatus.FILLED, guess_level);
            }
            start_filling = false;
            for (int i=end; i>(end-slop); i--)
            {
                if (myList[i].IsFilled()) start_filling = true;
                if (start_filling && myList[i].IsUnknown())
                    myList[i].SetStatus(PuzzleSquare.SquareStatus.FILLED, guess_level);
            }
        }
 * 
 */
        for (int i=start+slop; i<=(end-slop); i++)
            if (myList[i].IsUnknown ()) myList[i].SetStatus (PuzzleSquare.SquareStatus.FILLED, guess_level);
        return true;
    }

    private static boolean FillInOverlapBackward (PuzzleSquare[] myList, int start, int end, int clue, int guess_level)
    {
        if (myList == null) return false;
        if (start < end) return false;

        int len = start - end + 1;
        int slop = len - clue;
        if (slop < 0) return false;

        /*
        if (slop > 0)
        {
            boolean start_filling = false;
            for (int i=start; i>(start+slop); i--)
            {
                if (myList[i].IsFilled()) start_filling = true;
                if (start_filling && myList[i].IsUnknown())
                    myList[i].SetStatus(PuzzleSquare.SquareStatus.FILLED, guess_level);
            }
            start_filling = false;
            for (int i=end; i<(end+slop); i++)
            {
                if (myList[i].IsFilled()) start_filling = true;
                if (start_filling && myList[i].IsUnknown())
                    myList[i].SetStatus(PuzzleSquare.SquareStatus.FILLED, guess_level);
            }
        }
         * 
         */
        for (int i=start-slop; i>=(end+slop); i--)
            if (myList[i].IsUnknown ()) myList[i].SetStatus (PuzzleSquare.SquareStatus.FILLED, guess_level);
        return true;
    }

    public static void ProcessBumpers (PBNPuzzle myPuzzle, int guess_level)
    {
        if (myPuzzle == null) return;
        for (int r=0; r<myPuzzle.GetRows() && !do_stop; r++)
        {
            if (debug_detail) prevList = CopyRowFromPuzzle (myPuzzle, r);
            ProcessBumpersInRow (myPuzzle, r, guess_level);
            if (TargetMet (myPuzzle))
            {
                newList = CopyRowFromPuzzle (myPuzzle, r);
                DumpPrevAndNewLists ("ProcessBumpersInRow row " + r);
                PaintByNumberPro.HandleMessageForSolver ("Debugging Target Met",
                        "At ProcessBumpers row " + r);
            }
        }
        for (int c=0; c<myPuzzle.GetCols() && !do_stop; c++)
        {
            if (debug_detail) prevList = CopyColFromPuzzle (myPuzzle, c);
            ProcessBumpersInCol (myPuzzle, c, guess_level);
            if (TargetMet (myPuzzle))
            {
                newList = CopyColFromPuzzle (myPuzzle, c);
                DumpPrevAndNewLists ("ProcessBumpersInCol col " + c);
                PaintByNumberPro.HandleMessageForSolver ("Debugging Target Met",
                        "At ProcessBumpers col " + c);
            }
        }
    }

    public static void ProcessBumpersInCol (PBNPuzzle myPuzzle, int col)
    {
        if (myPuzzle == null) return;
        ProcessBumpersInCol (myPuzzle, col, myPuzzle.GetGuessLevel());
    }

    public static void ProcessBumpersInRow (PBNPuzzle myPuzzle, int row)
    {
        if (myPuzzle == null) return;
        ProcessBumpersInRow (myPuzzle, row, myPuzzle.GetGuessLevel());
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
        return GenerateGuessFromEdgeWithMostClueSquares (myPuzzle);
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

    public static boolean ProcessBlobs (PBNPuzzle myPuzzle, int guess_level)
    {
        if (myPuzzle == null) return false;
        boolean success = true;
        for (int r=0; r<myPuzzle.GetRows() && !do_stop; r++)
        {
            if (debug_detail) prevList = CopyRowFromPuzzle (myPuzzle, r);
            success = ProcessBlobsInRow (myPuzzle, r, guess_level);
            if (TargetMet (myPuzzle))
            {
                newList = CopyRowFromPuzzle (myPuzzle, r);
                DumpPrevAndNewLists ("ProcessBlobsInRow row " + r);
                PaintByNumberPro.HandleMessageForSolver ("Debugging Target Met",
                        "At ProcessBlobs row " + r);
            }
            if (!success) return false;
        }
        for (int c=0; c<myPuzzle.GetCols() && !do_stop; c++)
        {
            if (debug_detail) prevList = CopyColFromPuzzle (myPuzzle, c);
            success = ProcessBlobsInCol (myPuzzle, c, guess_level);
            if (TargetMet (myPuzzle))
            {
                newList = CopyColFromPuzzle (myPuzzle, c);
                DumpPrevAndNewLists ("ProcessBlobsInCol col " + c);
                PaintByNumberPro.HandleMessageForSolver ("Debugging Target Met",
                        "At ProcessBlobs col " + c);
            }
            if (!success) return false;
        }
        return true;
    }

    private static boolean ProcessBlobsInCol (PBNPuzzle myPuzzle, int col, int guess_level)
    {
        if (myPuzzle == null) return false;
        
        SetCurrentProcess (col, -1, guess_level, "ProcessBlobsInCol");         

        PuzzleSquare[] myList = PuzzleSolver.CopyColFromPuzzle(myPuzzle, col);
        int myClues[] = new int[myPuzzle.GetCol_NClues(col)];
        for (int i=0; i<myPuzzle.GetCol_NClues(col); i++)
            myClues[i] = myPuzzle.GetCol_Clues (col, i);
        boolean success = ProcessBlobs (myList, myClues, guess_level);
        if (success) PuzzleSolver.CopyColToPuzzle (myPuzzle, myList, col);
        return success;
    }

    private static boolean ProcessBlobsInRow (PBNPuzzle myPuzzle, int row, int guess_level)
    {
        if (myPuzzle == null) return false;
        
        SetCurrentProcess (-1, row, guess_level, "ProcessBlobsInRow");          

        PuzzleSquare[] myList = PuzzleSolver.CopyRowFromPuzzle(myPuzzle, row);
        int myClues[] = new int[myPuzzle.GetRow_NClues(row)];
        for (int i=0; i<myPuzzle.GetRow_NClues(row); i++)
            myClues[i] = myPuzzle.GetRow_Clues (row, i);        
        boolean success = ProcessBlobs (myList, myClues, guess_level);
        if (success) PuzzleSolver.CopyRowToPuzzle (myPuzzle, myList, row);
        return success;
    }

    private static boolean ProcessBlobs (PuzzleSquare[] myList, int[] clues, int guess_level)
    {
        if (myList == null || clues == null) return false;

        ArrayList<Blob> myBlobList = BlobUtilities.ExtractBlobsFromListForward(myList, 0);
        if (myBlobList == null || myBlobList.isEmpty()) return true;
        
        // this really can be done once per column/row and passed to this routine        
        boolean[] unique_and_biggest = ClueIsUniqueAndBiggest(clues);
        boolean clues_are_the_same = AllCluesTheSame(clues);
        int max_clue_val = GetBiggestClueVal(clues);

        // create a new bumper list with IDs so I can keep track of which is which
        ArrayList<Blob> myBlobIDList = new ArrayList<Blob>();
        for (int b=0; b<myBlobList.size(); b++)
        {
            Blob blob = (Blob)myBlobList.get(b);
            blob.SetID (b);
            myBlobIDList.add(blob);
        }

        // loop over each blob and make a list of each clue that the blob could
        // be part of and make sure that the clue would fit in the location
        // of the blob
        ArrayList<Integer>[] cluesForBlob = new ArrayList[myBlobIDList.size()];
        boolean unique_and_biggest_clue_matched = false;
        int unique_and_biggest_clue_idx = -1;
        int unique_and_biggest_blob_idx = -1;
        for (Blob blob : myBlobIDList)
        {
            int blob_len = blob.GetLength();
            int blob_id = blob.GetID();
            cluesForBlob[blob_id] = new ArrayList<Integer>();
            for (int cl=0; cl<clues.length; cl++)
            {
                if (unique_and_biggest[cl] &&
                        blob_len == clues[cl] &&
                        blob.GetStartIndex() >= MinDistanceFromLeft (clues, cl) &&
                        (myList.length - blob.GetEndIndex() - 1) >= MinDistanceFromRight (clues, cl))
                {
                    Integer myInt = new Integer (cl);
                    cluesForBlob[blob_id].clear();
                    cluesForBlob[blob_id].add(myInt);
                    unique_and_biggest_clue_matched = true;
                    unique_and_biggest_clue_idx = cl;
                    unique_and_biggest_blob_idx = blob_id;
                    break;
                }                
                else if (blob_len <= clues[cl] &&
                        blob.GetStartIndex() >= MinDistanceFromLeft (clues, cl) &&
                        (myList.length - blob.GetEndIndex() - 1) >= MinDistanceFromRight (clues, cl))
                {
                    Integer myInt = new Integer (cl);
                    cluesForBlob[blob_id].add(myInt);
                }
            }
            if (cluesForBlob[blob_id].isEmpty()) return false;
        }  
        
        // now remove the unique_and_biggest_clue from all other blog if it has been matched up
        if (unique_and_biggest_clue_matched)
        {
            Integer removeInt = null;            
            for (Blob blob : myBlobIDList)
            {
                int blob_id = blob.GetID();
                if (blob_id != unique_and_biggest_blob_idx)
                {
                    removeInt = null;
                    for (Integer myClueInt: cluesForBlob[blob_id])
                    {
                        int myClueIntIdx = myClueInt.intValue();
                        if (myClueIntIdx == unique_and_biggest_clue_idx)
                        {
                            removeInt = myClueInt;
                        }
                    }
                    if (removeInt != null)
                        cluesForBlob[blob_id].remove(removeInt);
                }
            }
        }

        // now cull the clue list in the forward direction based on the clue num
        // and if the blob is sufficiently far away from both the left and right
        // edges based on the other clues
        int prev_clue = -1;
        for (Blob blob : myBlobIDList)
        {
            int blob_id = blob.GetID();

            if (!cluesForBlob[blob_id].isEmpty())
            {
                boolean all_done = false;
                int cur_clue = -1;
                while (!all_done)
                {
                    Integer first_clue = (Integer)cluesForBlob[blob_id].get(0);
                    cur_clue = first_clue.intValue();

                    // remove it from the list cur_clue is smaller than the
                    // last clue OR if the blob is not far enough from the
                    // left or right edge based on the other clues
                    if (cur_clue < prev_clue)
                    {
                        cluesForBlob[blob_id].remove(first_clue);
                    } else
                    {
                        prev_clue = cur_clue;
                        all_done = true;
                    }
                    if (cluesForBlob[blob_id].isEmpty()) all_done = true;
                }
            }
        }

        // now process the list going backwards so remove any bumpers that are
        // out of sequence
        prev_clue = clues.length;
        int num_blobs = cluesForBlob.length;
        for (int bl=num_blobs-1; bl>=0; bl--)
        {
            Blob blob = (Blob)myBlobIDList.get(bl);
            int blob_id = blob.GetID();

            if (!cluesForBlob[blob_id].isEmpty())
            {
                boolean all_done = false;
                int cur_clue = clues.length;
                while (!all_done)
                {
                    int last = cluesForBlob[blob_id].size() - 1;
                    Integer last_clue = (Integer)cluesForBlob[blob_id].get(last);
                    cur_clue = last_clue.intValue();

                    // remove it from the list cur_clue is smaller than the
                    // last clue OR if the blob is not far enough from the
                    // left or right edge based on the other clues
                    if (cur_clue > prev_clue)
                    {
                        cluesForBlob[blob_id].remove(last_clue);
                    } else
                    {
                        prev_clue = cur_clue;
                        all_done = true;
                    }
                    if (cluesForBlob[blob_id].isEmpty()) all_done = true;
                }
            }
        }
        
        // Now let's uniquely assign clues to blobs, if blobs are separated
        // by only EMPTYs
        //
        // Forward processing
        int last_idx = 0;
        boolean keep_processing = true;
        for (Blob blob: myBlobIDList)
        {
            if (keep_processing)
            {
                int blob_id = blob.GetID();
                int blob_len = blob.GetLength();
                int clue_idx = cluesForBlob[blob_id].get(0).intValue();
                if (blob_len == clues[clue_idx] &&
                        AllEmptyBetweenInclusive (myList, last_idx, blob.GetStartIndex()-1))
                {
                    // this clue is uniquely assigned to this blob
                    // so remove this clue from all other blob lists
                    Integer removeInt = null;            
                    for (Blob blob2 : myBlobIDList)
                    {
                        int blob2_id = blob2.GetID();
                        if (blob2_id != blob_id)
                        {
                            removeInt = null;
                            for (Integer myClueInt: cluesForBlob[blob2_id])
                            {
                                int myClueIntIdx = myClueInt.intValue();
                                if (myClueIntIdx == clue_idx)
                                    removeInt = myClueInt;
                            }
                            if (removeInt != null)
                                cluesForBlob[blob2_id].remove(removeInt);
                        }
                    }
                    cluesForBlob[blob_id].clear();
                    cluesForBlob[blob_id].add(new Integer(clue_idx));
                    last_idx = blob.GetEndIndex()+1;
                } else keep_processing = false;
            }
        }
        
        // Backward processing
        last_idx = myList.length-1;
        keep_processing = true;
        for (int ib=myBlobIDList.size()-1; ib>=0; ib--)
        {
            Blob blob = myBlobIDList.get(ib);
            if (keep_processing)
            {
                int blob_id = blob.GetID();
                int blob_len = blob.GetLength();              
                int clue_list_len = cluesForBlob[blob_id].size();
                int clue_idx = cluesForBlob[blob_id].get(clue_list_len-1).intValue();
                if (blob_len == clues[clue_idx] &&
                        AllEmptyBetweenInclusive (myList, blob.GetEndIndex()+1, last_idx))
                {
                    // this clue is uniquely assigned to this blob
                    // so remove this clue from all other blob lists
                    Integer removeInt = null;            
                    for (Blob blob2 : myBlobIDList)
                    {
                        int blob2_id = blob2.GetID();
                        if (blob2_id != blob_id)
                        {
                            removeInt = null;
                            for (Integer myClueInt: cluesForBlob[blob2_id])
                            {
                                int myClueIntIdx = myClueInt.intValue();
                                if (myClueIntIdx == clue_idx)
                                    removeInt = myClueInt;
                            }
                            if (removeInt != null)
                                cluesForBlob[blob2_id].remove(removeInt);
                        }
                    }                    
                    cluesForBlob[blob_id].clear();
                    cluesForBlob[blob_id].add(new Integer(clue_idx));
                    last_idx = blob.GetStartIndex()-1;
                } else keep_processing = false;
            }
        }  

        // Now process all of the blobs for which there is a unique
        // clue associated with it AND that clue doesn't occur in the
        // lists of any other blobs
        //
        // Also process blobs whose lengths match that of the clue when all
        // clue values are the same
        //
        // All process blobs whose lengths are the same as the maximum
        // clue value
        for (Blob blob: myBlobIDList)
        {
            int blob_id = blob.GetID();
            
            if (clues_are_the_same && blob.GetLength() == clues[0] ||
                blob.GetLength() == max_clue_val)
            {
                int left_blob = blob.GetStartIndex() - 1;
                int right_blob = blob.GetEndIndex() + 1;
                if (left_blob >= 0)
                {
                    if (myList[left_blob].IsUnknown())
                        myList[left_blob].SetStatus (PuzzleSquare.SquareStatus.EMPTY, guess_level);
                    else if (myList[left_blob].IsFilled())
                        return false;
                }
                if (right_blob < myList.length)
                {
                    if (myList[right_blob].IsUnknown())
                        myList[right_blob].SetStatus (PuzzleSquare.SquareStatus.EMPTY, guess_level);
                    else if (myList[right_blob].IsFilled())
                        return false;
                }
            }

            if (cluesForBlob[blob_id].size() == 1)
            {
                Integer myClueInt = (Integer)cluesForBlob[blob_id].get(0);
                int clue = myClueInt.intValue();

                // Check how many times this clue appears in all blob lists
                // (if more than once, then don't process)
                int num_times_clue_in_lists = 0;
                for (int i=0; i<myBlobIDList.size(); i++)
                {
                    for (Integer myInt: cluesForBlob[i])
                    {
                        if (myInt.intValue() == clue) num_times_clue_in_lists++;
                    }
                }             

                // Only process if num_times_clue_in_lists == 1
                if (num_times_clue_in_lists == 1)
                {

                    // calculate the slop for the blob and clue
                    int slop = clues[clue] - blob.GetLength();

                    // put EMPTY squares on both sides of the blob if slop is 0
                    if (slop == 0)
                    {
                        int left_blob = blob.GetStartIndex() - 1;
                        int right_blob = blob.GetEndIndex() + 1;
                        if (left_blob >= 0)
                        {
                            if (myList[left_blob].IsUnknown())
                                myList[left_blob].SetStatus (PuzzleSquare.SquareStatus.EMPTY, guess_level);
                            else if (myList[left_blob].IsFilled())
                                return false;
                        }
                        if (right_blob < myList.length)
                        {
                            if (myList[right_blob].IsUnknown())
                                myList[right_blob].SetStatus (PuzzleSquare.SquareStatus.EMPTY, guess_level);
                            else if (myList[right_blob].IsFilled())
                                return false;
                        }
                    } else
                    {

                        // if there are any empty squares in the slop region to the
                        // left of the blob, then process forward from the last empty
                        // square
                        int slop_start = blob.GetStartIndex() - slop - 1;
                        int slop_end = blob.GetStartIndex() - 1;
                        // process forward from left edge
                        if (slop_start < 0)
                            ProcessForwardFromClueAt (myList, clues, clue, 0, guess_level);
                        else
                        {
                            int last_empty = -1;
                            boolean some_filled = false;
                            for (int i=slop_start; i<=slop_end; i++)
                            {
                                if (myList[i].IsEmpty()) last_empty = i;
                                if (myList[i].IsFilled()) some_filled = true;
                            }
                            if (!some_filled && last_empty >= 0)
                                ProcessForwardFromClueAt (myList, clues, clue, last_empty+1, guess_level);
                            
                            // If prior blob is assigned uniquely to prior clue
                            // and is the proper length, then I can fill in
                            // empties from the end of the prior blob to the
                            // beginning of the slop region
                            if (blob_id > 0 && clue > 0)
                            {
                                Blob priorBlob = myBlobIDList.get(blob_id-1);
                                if (cluesForBlob[blob_id-1].size() == 1 &&
                                    cluesForBlob[blob_id-1].get(0).intValue() == (clue-1) &&
                                    priorBlob.GetLength() == clues[clue-1])
                                {
                                    if (priorBlob.GetEndIndex()+1 <= slop_start)
                                    {
                                        for (int i=priorBlob.GetEndIndex()+1; i<=slop_start; i++)
                                        {
                                            if (myList[i].IsUnknown()) myList[i].SetStatus (PuzzleSquare.SquareStatus.EMPTY, guess_level);
                                            else if (myList[i].IsFilled()) return false;
                                        }
                                    }
                                }
                            }
                        }                                  

                        // if there any empty squares in the slop region to the right
                        // of the blob, then process backward from the first empty square
                        slop_start = blob.GetEndIndex() + 1;
                        slop_end = blob.GetEndIndex() + slop + 1;
                        // process backward from right edge
                        if (slop_end >= myList.length)
                            ProcessBackwardFromClueAt (myList, clues, clue, myList.length-1, guess_level);
                        else
                        {
                            int last_empty = myList.length;
                            boolean some_filled = false;
                            for (int i=slop_end; i>=slop_start; i--)
                            {
                                if (myList[i].IsEmpty()) last_empty = i;
                                if (myList[i].IsFilled()) some_filled = true;
                            }
                            if (!some_filled && last_empty < myList.length)
                                ProcessBackwardFromClueAt (myList, clues, clue, last_empty-1, guess_level);
                            
                            // If next blob is assigned uniquely to next clue
                            // and is the proper length, then I can fill in
                            // empties from the start of the next blob to the
                            // end of the slop region
                            if (blob_id < (myBlobIDList.size()-1) && clue < (clues.length-1))
                            {
                                Blob nextBlob = myBlobIDList.get(blob_id+1);
                                if (cluesForBlob[blob_id+1].size() == 1 &&
                                    cluesForBlob[blob_id+1].get(0).intValue() == (clue+1) &&
                                    nextBlob.GetLength() == clues[clue+1])
                                {
                                    if (nextBlob.GetStartIndex()-1 >= slop_end)
                                    {
                                        for (int i=nextBlob.GetStartIndex()-1; i>=slop_end; i--)
                                        {
                                            if (myList[i].IsUnknown()) myList[i].SetStatus (PuzzleSquare.SquareStatus.EMPTY, guess_level);
                                            else if (myList[i].IsFilled()) return false;
                                        }
                                    }
                                }
                            }                            
                        }
                    }

                    // if this is the first clue, then empty out any squares
                    // to the left of the slop region on the left of the blob
                    if (clue == 0)
                    {
                        int left_edge = blob.GetStartIndex() - slop - 1;
                        if (left_edge >= 0)
                        {
                            for (int i=0; i<=left_edge; i++)
                            {
                                if (myList[i].IsUnknown())
                                    myList[i].SetStatus (PuzzleSquare.SquareStatus.EMPTY, guess_level);
                                else if (myList[i].IsFilled())
                                    return false;
                            }
                        }
                    }

                    // if this is the last clue, then empty out any squares
                    // to the right of the slop region on the right of the blob
                    if (clue == (clues.length-1))
                    {
                        int right_edge = blob.GetEndIndex() + slop + 1;
                        if (right_edge < myList.length)
                        {
                            for (int i=right_edge; i<myList.length; i++)
                            {
                                if (myList[i].IsUnknown())
                                    myList[i].SetStatus (PuzzleSquare.SquareStatus.EMPTY, guess_level);
                                else if (myList[i].IsFilled())
                                    return false;
                            }
                        }
                    }
                
                    // If there is only one blob that can be associated with
                    // the current Blob's clue, then continue processing
//                    if (blobsForClues[clue].size() == 1)
                    { 

                        // now let's look at the clues to the left and the right
                        // if they're all the same value and we find any blobs
                        // that have the same length, then we can safely assume
                        // that those blobs are empty to their left/right
                        if (clue > 0 && clue < (clues.length-1))
                        {

                            // let's look at clues to the left (but only
                            // if there are blobs to the left as well)
                            int blobID = blob.GetID();
                            if (blobID > 0)
                            {
                                int clue_val = clues[0];
                                boolean clue_val_unique = true;
                                if (clue > 1)
                                {
                                    for (int cl=1; cl<clue; cl++)
                                        if (clues[cl] != clue_val) clue_val_unique = false;
                                }
                                if (clue_val_unique)
                                {
                                    for (int bl=0; bl<blobID; bl++)
                                    {
                                        Blob testBlob = myBlobIDList.get(bl);
                                        if (testBlob.GetLength() == clue_val)
                                        {
                                            int left_index = testBlob.GetStartIndex() - 1;
                                            int right_index = testBlob.GetEndIndex() + 1;
                                            if (left_index >= 0)
                                            {
                                                if (myList[left_index].IsUnknown())
                                                    myList[left_index].SetStatus(PuzzleSquare.SquareStatus.EMPTY, guess_level);
                                                else if (myList[left_index].IsFilled()) 
                                                    return false;
                                            }
                                            if (right_index < myList.length)
                                            {
                                                if (myList[right_index].IsUnknown())
                                                    myList[right_index].SetStatus(PuzzleSquare.SquareStatus.EMPTY, guess_level);
                                                else if (myList[right_index].IsFilled()) 
                                                    return false;
                                            }
                                        }
                                    }
                                }                            
                            }

                            // now let's look at clues to the right (but only
                            // if there are blobs to the right as well)
                            if (blobID < (myBlobIDList.size()-1))
                            {
                                int clue_val = clues[clue+1];
                                boolean clue_val_unique = true;
                                if (clue < (clues.length-2))
                                {
                                    for (int cl=clue+2; cl<clues.length; cl++)
                                        if (clues[cl] != clue_val) clue_val_unique = false;
                                }
                                if (clue_val_unique)
                                {
                                    for (int bl=blobID+1; bl<myBlobIDList.size(); bl++)
                                    {
                                        Blob testBlob = myBlobIDList.get(bl);
                                        if (testBlob.GetLength() == clue_val)
                                        {
                                            int left_index = testBlob.GetStartIndex() - 1;
                                            int right_index = testBlob.GetEndIndex() + 1;
                                            if (left_index >= 0)
                                            {
                                                if (myList[left_index].IsUnknown())
                                                    myList[left_index].SetStatus(PuzzleSquare.SquareStatus.EMPTY, guess_level);
                                                else if (myList[left_index].IsFilled()) 
                                                    return false;
                                            }
                                            if (right_index < myList.length)
                                            {
                                                if (myList[right_index].IsUnknown())
                                                    myList[right_index].SetStatus(PuzzleSquare.SquareStatus.EMPTY, guess_level);
                                                else if (myList[right_index].IsFilled()) 
                                                    return false;
                                            }
                                        }
                                    }                                                                  
                                }   
                            }
                        }
                    }
                }
            }
        }
        return true;
    }

    private static int MinDistanceFromLeft (int[] clues, int clue)
    {
        if (clues == null) return 0;
        if (clue > 0)
        {
            int min_distance = 0;
            for (int i=0; i<clue; i++)
            {
                min_distance += clues[i];
                min_distance += 1;
            }
            return min_distance;
        } else return 0;
    }

    private static int MinDistanceFromRight (int[] clues, int clue)
    {
        if (clues == null) return 0;
        if (clue < (clues.length-1))
        {
            int min_distance = 0;
            for (int i=clue+1; i<clues.length; i++)
            {
                min_distance += clues[i];
                min_distance += 1;
            }
            return min_distance;
        } else return 0;
    }

    public static void SetTarget (
            int[] target_r, int[] target_c, PuzzleSquare.SquareStatus[] target_st, boolean do_AND)
    {
        debug_detail = true;
        target_row = target_r;
        target_col = target_c;
        target_status = target_st;
        boolean_AND = do_AND;
    }

    private static boolean TargetMet (PBNPuzzle myPuzzle)
    {
        if (!debug_detail) return false;
        return CheckATarget (myPuzzle, target_row, target_col, target_status, boolean_AND);
    }

    public static boolean CheckATarget (PBNPuzzle myPuzzle, int[] rows, int[] cols,
            PuzzleSquare.SquareStatus[] status, boolean do_AND)
    {
        if (rows == null || cols == null || status == null) return false;
        assert (rows.length == cols.length && cols.length == status.length);
        boolean target_met;
        if (do_AND)
        {
            target_met = true;
            for (int i=0; i<rows.length; i++)
                if (!(myPuzzle.GetPuzzleSquareAt(rows[i], cols[i]).GetStatus() ==
                    status[i])) target_met = false;
        } else
        {
            target_met = false;
            for (int i=0; i<rows.length; i++)
                if ((myPuzzle.GetPuzzleSquareAt(rows[i], cols[i]).GetStatus() ==
                    status[i])) target_met = true;
        }
        return target_met;
    }
    
    public static String DumpList (PuzzleSquare[] theList)
    {
        String s = "";
        for (int i=0; i<theList.length; i++)
            s += theList[i].toString() + " ";
        return s;
    }
    
    public static String DumpListFromStart (PuzzleSquare[] theList, int start_idx)
    {
        String s = "";
        if (start_idx > 0)
            for (int i=0; i<start_idx; i++) 
                s += ". ";
        for (int i=0; i<theList.length; i++)
            s += theList[i].toString(true) + " ";
        return s;
    }

    private static void DumpPrevAndNewLists (String title)
    {
        System.out.println (title + ":");
        System.out.println (DumpList (prevList));
        System.out.println (DumpList (newList));
    }
    
    private static String DumpClues (int[] theClues)
    {
        String s= "[";
        for (int i=0; i<theClues.length; i++)
            s += theClues[i] + " ";
        s += "]";
        return s;
    }
    
    private static String DumpCluesFromStart (int[] theClues, int start_idx)
    {
        String s= "[";
        if (start_idx > 0)
            for (int i=0; i<start_idx; i++)
                s += "x ";
        for (int i=0; i<theClues.length; i++)
            s += theClues[i] + " ";
        s += "]";
        return s;
    }    

    public static void SetDebuggingOff ()
    { debug_detail = false; }

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

    private static boolean[] ClueIsUniqueAndBiggest (int[] clues)
    {
        boolean[] unique_and_biggest = new boolean[clues.length];
        for (int i=0; i<clues.length; i++)
        {
            int clue_val = clues[i];
            int clue_count = 0;
            boolean biggest = true;
            for (int cl=0; cl<clues.length; cl++)
            {
                if (clues[cl] > clue_val) biggest = false;
                if (clues[cl] == clue_val) clue_count++;
            }
            unique_and_biggest[i] = (clue_count == 1) && biggest;
        }
        return unique_and_biggest;
    }
    
    public static boolean AllCluesTheSame (int[] clues)
    {
        int clue_val = clues[0];
        for (int i=0; i<clues.length; i++)
        {
            if (clues[i] != clue_val) return false;
        }
        return true;  
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
    
    private static boolean AllEmptyBetweenInclusive (PuzzleSquare[] myList, int start, int end)
    {
        assert (end >= start);
        if (start >= 0 && start < myList.length &&
            end >= 0 && end < myList.length)
        {
            for (int i=start; i<=end; i++)
                if (!myList[i].IsEmpty()) return false;
        }
        return true;
    }
    
    private static int GetBiggestClueVal (int[] clues)
    {
        int max_clue_val = clues[0];
        for (int i=0; i<clues.length; i++)
            if (clues[i] > max_clue_val)
                max_clue_val = clues[i];
        return max_clue_val;
    }
    
    // if you have clues [ 3 2 12 4 3 ] and you call this method for
    // clue #2 (value = 12), then the min number of spaces you need past
    // this clue is _XXXX_XXX = 9 spaces
    // Or this_clue == 4 (value 3), then the min number of spaces you need
    // past this clue is 0
    // Or if you have just one clue [4] then the min number of spaces you 
    // need past this clue is 0
    //
    // Returns two values: int[0] = summation of all clue values past the current clue
    //                     int[1] = # of separators needed between current clue and
    //                              remaining clues
    private static int[] GetMinSpacesNeededPastThisClue (int[] clues, int nclues, int this_clue)
    {
        int[] vals = new int[2];
        vals[0] = 0;
        vals[1] = 0;
        if (clues == null || this_clue == (nclues-1)) return vals;
        assert (this_clue < nclues);
        for (int i=this_clue+1; i<nclues; i++)
            if (clues[i] > 0)
            {
                vals[0] += clues[i];
                vals[1] ++;
            }
        return vals;
    }
}
