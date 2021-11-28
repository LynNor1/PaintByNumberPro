/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package paintbynumberpro;

import java.awt.*;

/**
 *
 * @author Lynne
 */
public class PuzzleSolverThread extends Thread {

    // ----------------------------
    // Code for solving the puzzle!
    // ----------------------------

    private PBNPuzzle puzzleToSolve = null;
    private boolean do_stop = false;
	private boolean auto_stop_at_each_new_guess = true;
	private boolean auto_stop = false; // switch to true when triggered by a new guess

    private PBNFrame frameToRedraw = null;

    private boolean debug = false;
    private int[] start_debug_row = { 22, 19, 18};
    private int[] start_debug_col = { 0, 0, 27};
    private PuzzleSquare.SquareStatus start_debug_status[] = {
        PuzzleSquare.SquareStatus.FILLED, 
        PuzzleSquare.SquareStatus.FILLED,
        PuzzleSquare.SquareStatus.FILLED };

    PuzzleSolverThread () {}

    public void SetPuzzleToSolve (PBNPuzzle thePuzzle)
    {
        puzzleToSolve = thePuzzle;
    }
    public void SetDebugging (boolean state)
    { debug = state; }
    synchronized public void SetStop ()
    {
        do_stop = true;
        PuzzleSolver.SetDoStop(true);
    }
	
	public void SetAutoStopAtEachNewGuess (boolean state)
	{ auto_stop_at_each_new_guess = state; }
	
	public boolean GetAutoStop ()
	{ return auto_stop; }

    public void RedrawFrame ()
    {
        if (frameToRedraw != null) frameToRedraw.repaint();
    }

    private boolean IterateOnceUntilErrorOrNoChange (int guess_level)
    {
        if (puzzleToSolve == null) return false;

        boolean no_change = false;
        boolean success = true;

        while (success && !no_change && !do_stop)
        {
            int first_num_knowns = puzzleToSolve.CountKnownSquares();
			
			// ----------------------------
			// BEGINNING OF NEW SOLVER CODE
			// ----------------------------
			
			BetterPuzzleSolver solver = new BetterPuzzleSolver ();	
				
            // Edge processing
            boolean keep_edge_processing = true;
            while (keep_edge_processing && !do_stop)
            {
//				System.out.println ("ProcessEdges...");
                int prev_num_knowns = puzzleToSolve.CountKnownSquares();
                success = PuzzleSolver.ProcessEdges (puzzleToSolve, guess_level, false);
                int num_knowns = puzzleToSolve.CountKnownSquares();
                RedrawFrame();				
                if (success && (prev_num_knowns != num_knowns))
				{
//					System.out.println ("CheckPuzzleSoFar...");
                    success = PuzzleSolver.CheckPuzzleSoFar (puzzleToSolve, true, false, false);
				}

                keep_edge_processing = success && (prev_num_knowns != num_knowns);
            }

			// Process from possible solutions
			boolean keep_processing = true;
			while (keep_processing && !do_stop)
			{
//				System.out.println ("Process with smarter solver...");
                int prev_num_knowns = puzzleToSolve.CountKnownSquares();
                success = solver.ProcessPuzzle(puzzleToSolve, guess_level, debug);
                int num_knowns = puzzleToSolve.CountKnownSquares();
                RedrawFrame();
                if (success && (prev_num_knowns != num_knowns))
				{
//					System.out.println ("CheckPuzzleSoFar...");
                    success = PuzzleSolver.CheckPuzzleSoFar (puzzleToSolve, true, false, false);
				}

                keep_processing = success && (prev_num_knowns != num_knowns);
 			}		

            int last_num_knowns = puzzleToSolve.CountKnownSquares();

            no_change = (last_num_knowns == first_num_knowns);
        }

        return success;
    }

    @Override public void run()
    {
        assert (puzzleToSolve != null);

        // Tell the draw handler to turn on the Stop button in the control frame
        PaintByNumberPro.GetDrawHandler().SetStopButton(true);
        PuzzleSolver.SetDoStop(false);

        // Set puzzle NOT solved
        puzzleToSolve.SetSolved(false);

        // Do a sanity check on the clues first
        if (!puzzleToSolve.SanityCheckTheClues()) 
        {
            PaintByNumberPro.GetDrawHandler().SetMode (PBNHandler.Mode.NORMAL);
            return;
        }

        // Get the PBNFrame if we're watching
        frameToRedraw = null;
        PBNHandler theHandler = PaintByNumberPro.GetDrawHandler();
        assert (theHandler != null);
        frameToRedraw = theHandler.GetTheFrame();

        // Initialize the guessing parameters
        PuzzleSolver.InitializeGuessVariables(puzzleToSolve);

        // Get the largest guess level with at least one square
        int guess_level = puzzleToSolve.GetMaxGuessLevelWithKnownSquares ();
        puzzleToSolve.SetGuessLevel(guess_level);

        // Fill in the obvious stuff
        if (puzzleToSolve.CountKnownSquares() == 0)
        {
            PuzzleSolver.FillInEasyToComputeSquares (puzzleToSolve, true);
            RedrawFrame();
        }

        // Precompute the total number of squares
        int total_squares = puzzleToSolve.GetRows() * puzzleToSolve.GetCols();

        // Iterate once      
        boolean success = IterateOnceUntilErrorOrNoChange (guess_level);
        if (!success)
        {
            PaintByNumberPro.HandleErrorMessage("Puzzle Solver",
                    "Error initializing puzzle.  There must be something wrong with the clues", frameToRedraw);
            PaintByNumberPro.GetDrawHandler().SetMode (PBNHandler.Mode.NORMAL);
            return;
        }

        // Alright here we go!
        boolean abort_processing = false;
		auto_stop = false;
        while (!abort_processing && puzzleToSolve.CountKnownSquares() < total_squares && !do_stop)
        {
			if (auto_stop_at_each_new_guess)
			{
				PaintByNumberPro.HandleMessage ("Puzzle Solver",
						"Auto stop!");				
				abort_processing = true;
				auto_stop = true;
			}
			else
			{
			
            // Set up for a new guess (we'll mark the start of the guess ourselves)
            puzzleToSolve.StartNewGuessLevel();
            guess_level = puzzleToSolve.GetGuessLevel();

            // Pick our guess
            Point guess_rect = PuzzleSolver.GenerateNewGuess (puzzleToSolve);
            if (guess_rect == null)
            {
                PaintByNumberPro.HandleErrorMessage("Puzzle Solver",
                        "Hmmm... Unable to generate a new guess", frameToRedraw);
                PaintByNumberPro.GetDrawHandler().SetMode (PBNHandler.Mode.NORMAL);
                return;
            }

            // Fill in the square at our guess
            int row = guess_rect.y;
            int col = guess_rect.x;
            PuzzleSquare ps = puzzleToSolve.GetPuzzleSquareAt(row, col);
            assert (ps.IsUnknown());
            puzzleToSolve.SetPuzzleRowCol(row, col, PuzzleSquare.SquareStatus.FILLED, guess_level);
            puzzleToSolve.SetPuzzleRowColSpecialMarked (row, col, true);
             
            RedrawFrame();

            // see if we've created any errors
            boolean good_so_far = PuzzleSolver.CheckPuzzleSoFar (puzzleToSolve, true, false, false);

            // If no errors, then Iterate
            if (good_so_far) good_so_far = IterateOnceUntilErrorOrNoChange (guess_level);

            // If there are errors, then we need to undo the last guesses one by one until
            // we reach a guess level where there are no errors currently in the puzzle
            while (!good_so_far && !abort_processing && !do_stop)
            {
                puzzleToSolve.UndoLastGuess (true);
                RedrawFrame();

                // Reset guess level to current guess level - 1 (because
                // the UndoLastGuess leaves it at the current guess level)
                int level = puzzleToSolve.GetGuessLevel();
                level--;
                if (level < 0)
                {
                    PaintByNumberPro.HandleErrorMessage ("Puzzle Solver",
                            "Guesses have been undone to the very beginning.\n" +
                            "There must be something wrong with the clues\n" +
                            PuzzleSolver.GetLastMessage() + "\n" +
                            PuzzleSolver.ReportCurrentProcess(), frameToRedraw);
                    abort_processing = true;
                }
                puzzleToSolve.SetGuessLevel(level);

                // see if we've created any errors simply by undoing the last guess
                good_so_far = PuzzleSolver.CheckPuzzleSoFar (puzzleToSolve, true, false, false);

                // If no errors, then Iterate
                if (good_so_far)
                {
                    good_so_far = IterateOnceUntilErrorOrNoChange(level);
                }
            }
			
			} // auto_stop_at_each_new_guess

            // Otherwise, we can just make a new guess
        }

        // Tell the draw handler to turn on the Stop button in the control frame
        PaintByNumberPro.GetDrawHandler().SetStopButton(false);
        PuzzleSolver.SetDoStop(false);

        // Switch modes back to normal
        PaintByNumberPro.GetDrawHandler().SetMode (PBNHandler.Mode.NORMAL);

        // Well if we made it this far, the puzzle should be solved!
        if (!do_stop && !auto_stop)
        {
            if (PuzzleSolver.IsPuzzleCorrect(puzzleToSolve))
            {
                puzzleToSolve.commitGuesses();
                puzzleToSolve.SetSolved(true);
                PaintByNumberPro.HandleMessage ("Puzzle Solver",
                        "Puzzle has been solved");
            } else
            {
                PaintByNumberPro.HandleErrorMessage ("Puzzle Solver",
                        "Puzzle does not appear to be solved correctly",
                        frameToRedraw);
            }
        }
    }

}
