/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package paintbynumberpro;

import java.util.*;

/**
 *
 * @author user
 */
public class BetterPuzzleSolver {
	
	private PuzzleSolverThread mySolverThread;
	
	// -------------------------------------------
	// Private classes we need to solve the puzzle
	// -------------------------------------------
	
	// Exception which can be thrown when we're trying to recursively decide
	// if a solution exists within the squares
	private class CannotFitSolutionException extends Exception
	{
		CannotFitSolutionException (String msg)
		{ super(msg); }
	}
	private class WeAreGoodException extends Exception
	{
		WeAreGoodException (String msg)
		{ super(msg); }
	}

	// Contiguous squares of UNKNOWN or FILLED
	public class Range 
	{
		private int start;
		private int end;
		private ArrayList<Integer> blob_indices;
		private LinkedList<Integer> clue_indices;
		private ArrayList<Blob> blobs;	// only used by CanSolutionFitInRow|ColStartingFromClue
		
		Range (int s, int e)
		{
			start = s;
			end = e;
			blob_indices = new ArrayList();
			clue_indices = new LinkedList();
		}
		public ArrayList<Blob> GetBlobsInRange (ArrayList<Blob> blobList)
		{
			blob_indices = new ArrayList();
			ArrayList<Blob> newBlobs = new ArrayList();
			for (Blob b : blobList)
			{
				if (b.start >= start && b.end <= end)
				{
					newBlobs.add(b);
					blob_indices.add(blobList.indexOf(b));
//					last_message += "   Adding Blob index " + blobList.indexOf(b) + " to Range with start " +
//							start + " end " + end + "\n";
				}
			}
			return newBlobs;
		}
		public void FindBlobsInRange (PuzzleSquare[] squares)
		{
			blobs = new ArrayList();
			int blob_start = 0;
			int blob_end = 0;
			boolean blob_open = false;
			for (int i=start; i<=end; i++)
			{
				if (squares[i].IsFilled())
				{
					if (!blob_open)
					{
						blob_start = i;
						blob_open = true;
					}
					blob_end = i;
				} else
				{
					if (blob_open)
					{
						blobs.add(new Blob (blob_start, blob_end));
						blob_open = false;
					}
				}
			}
			if (blob_open)
				blobs.add(new Blob (blob_start, blob_end));		
		}
		public int GetLength() {
			return end-start+1;
		}
		public int MaxClueIndex ()
		{
			int max_clue_index = -1;
			for (Integer ii : clue_indices)
			{
				int clue_index = ii.intValue();
				if (clue_index > max_clue_index)
					max_clue_index = clue_index;
			}
			return max_clue_index;
		}
		public int MinClueValue (int[] clues)
		{
			int min_value = 0;
			for (Integer ii : clue_indices)
			{
				int clue_index = ii.intValue();
				int clue_val = clues[clue_index];
				if (min_value == 0 || clue_val < min_value)
					min_value = clue_val;
			}	
			return min_value;
		}
		public boolean UniqueClueValues (int[] clues)
		{
			int unique_val = 0;
			for (Integer ii : clue_indices)
			{
				int clue_index = ii.intValue();
				int clue_val = clues[clue_index];
				if (unique_val == 0)
					unique_val = clue_val;
				else if (unique_val != clue_val) return false;
			}	
			return true;			
		}
		public boolean ContainsClueIndex (int index)
		{
			for (Integer ii : clue_indices)
			{
				int clue_index = ii.intValue();
				if (clue_index == index) return true;
			}
			return false;
		}
		public void RemoveClueIndicesSmallerThan (int big_index)
		{
			LinkedList<Integer>new_list = new LinkedList();
			for (Integer ii : clue_indices)
			{
				int clue_index = ii.intValue();
				if (clue_index >= big_index)
					new_list.add(new Integer(clue_index));
			}
			clue_indices = new_list;
		}
		public void RemoveClueIndicesLargerThan (int small_index)
		{
			LinkedList<Integer>new_list = new LinkedList();
			for (Integer ii : clue_indices)
			{
				int clue_index = ii.intValue();
				if (clue_index <= small_index)
					new_list.add(new Integer(clue_index));
			}
			clue_indices = new_list;
		}
		
		public void RemoveClueIndex (int index)
		{
			LinkedList<Integer>new_list = new LinkedList();
			for (Integer ii : clue_indices)
			{
				int clue_index = ii.intValue();
				if (clue_index != index)
					new_list.add(new Integer(clue_index));
			}
			clue_indices = new_list;
		}		
		public boolean ContainsBlobs()
		{ return (blob_indices.size() > 0); }
		public boolean IsAMystery (PuzzleSquare[] squares)
		{
			for (int i=start; i<=end; i++)
				if (squares[i].GetStatus() != PuzzleSquare.SquareStatus.UNKNOWN)
					return false;
			return true;
		}
		public boolean IsABlob (PuzzleSquare[] squares)
		{
			for (int i=start; i<=end; i++)
				if (squares[i].GetStatus() != PuzzleSquare.SquareStatus.FILLED)
					return false;
			return true;
		}
		public boolean ContainsBlob (Blob b)
		{ return (b.start >= start && b.end <= end); }
	}
	
	// Contiguous squares of UNKNOWN between EMPTY ends
	public class AntiBlob extends Blob
	{
		AntiBlob (int s, int e)
		{ super (s, e); }
	}
	
	// Contiguous squares of FILLED
	public class Blob
	{
		public int start;
		public int end;
		private boolean is_anchored_start;	// EMPTY (X) at start edge of Blob
		private boolean is_anchored_end;	// EMPTY (X) at end edge of Blob
		private boolean is_fixed_start;		// no UNKNOWN spaces from start of row/col to start of Blob
		private boolean is_fixed_end;		// no UNKNOWN spaces from end of Blob to end of row/col
		private boolean is_fixed_to_blob_prev; // no UNKNOWN spaces from start of Blob to prev Blob (if any)
		private boolean is_fixed_to_blob_next;   // no UNKNOWN spaces from end of Blob to next Blob (if any);
		private LinkedList<Integer> clue_indices;
		private ArrayList<Clue2> clue_list;
		Blob (int s, int e)
		{
			this(s, e, false, false, false, false, false, false);
		}
		Blob (int s, int e, boolean anch_start, boolean anch_end,
				boolean fixed_start, boolean fixed_end,
				boolean fixed_to_prev_blob, boolean fixed_to_next_blob)
		{
			start = s;
			end = e;
			is_anchored_start = anch_start;
			is_anchored_end = anch_end;
			is_fixed_start = fixed_start;
			is_fixed_end = fixed_end;
			is_fixed_to_blob_prev = fixed_to_prev_blob;
			is_fixed_to_blob_next = fixed_to_next_blob;
		}
		public String toStringShort ()
		{
			String descrpt = "" + start + " " + end + " (" + (end-start+1) + ")";
			if (is_anchored_start) descrpt += " AS";
			if (is_anchored_end) descrpt += " AE";
			return descrpt;
		}
		public String toStringShortWithClueList ()
		{			
			String descrpt = "" + start + " " + end + " (" + (end-start+1) + ")";
			if (clue_list != null && !clue_list.isEmpty())
			{
				descrpt += " Clue idxs";
				for (Clue2 cl : clue_list)
					descrpt += " " + cl.index;
			}
			return descrpt;			
		}
		@Override
		public String toString ()
		{
			String descrpt = "" + start + " " + end + " (" + (end-start+1) + ")";
			if (is_anchored_start) descrpt += " AS";
			if (is_anchored_end) descrpt += " AE";
			if (is_fixed_start) descrpt += " FS";
			if (is_fixed_end) descrpt += " FE";
			if (is_fixed_to_blob_prev) descrpt += " F2Prev";
			if (is_fixed_to_blob_next) descrpt += " F2Next";
			if (clue_indices != null && clue_indices.size() > 0)
			{
				descrpt += " Clue indices:";
				for (int i=0; i<clue_indices.size(); i++)
					descrpt += " " + clue_indices.get(i);
			}
			return descrpt;
		}
		public boolean IsAnchored()
		{ return is_anchored_start || is_anchored_end; }
		public boolean IsFullyAnchored()
		{ return is_anchored_start && is_anchored_end; }
		public boolean IsFixed()
		{ return is_fixed_start || is_fixed_end; }
		public boolean IsFullyFixed()
		{ return is_fixed_start && is_fixed_end; }
		public int GetLength()
		{ return end-start+1; }
		public int GetMaxClueVal (PuzzleSquare[] squares)
		{
			int extent_start = start;
			int extent_end = end;
			
			// how far can I extend to the left before hitting another FILLED space
			int peek_idx = extent_start - 1;
			while (peek_idx >= 0)
			{
				if (squares[peek_idx].GetStatus() == PuzzleSquare.SquareStatus.EMPTY)
					break;
				if (squares[peek_idx].GetStatus() == PuzzleSquare.SquareStatus.FILLED)
				{
					extent_start++;	// We need one space between us and the Blob to the left
					break;
				}
				extent_start--;
				peek_idx--;
			}
			
			// how far can I extend to the left before hitting another FILLED space
			peek_idx = extent_end + 1;
			while (peek_idx < squares.length)
			{
				if (squares[peek_idx].GetStatus() == PuzzleSquare.SquareStatus.EMPTY)
					break;
				if (squares[peek_idx].GetStatus() == PuzzleSquare.SquareStatus.FILLED)
				{
					extent_end--;	// We need one space between us and the Blob to the right
					break;
				}
				extent_end++;
				peek_idx++;
			}			
			
			return (extent_end - extent_start + 1);
		}
		public void RemoveClueIndexFromClueList (int index)
		{
			ArrayList<Clue2> newList = new ArrayList();
			for (Clue2 cl : clue_list)
				if (cl.index != index) newList.add(cl);
			clue_list = newList;
		}
		public boolean HasIndexInClueList (int index)
		{
			for (Clue2 cl : clue_list)
				if (cl.index == index) return true;
			return false;
		}
	}
	
	public class Clues2
	{
		private Clue2[] clue_list;
		private int num_squares;
		private boolean is_row;
		private int col_row_num;
		Clues2 (int N, boolean isRow, int num)
		{
			num_squares = N;
			is_row = isRow;
			col_row_num = num;
		}
		public int GetNumClues()
		{
			if (clue_list == null) return 0;
			else return clue_list.length;
		}
	}
	
	public class Clues
	{
		private Clue[] clue_list;
		private int num_squares;
		private boolean is_row;
		private int col_row_num;
		
		Clues (int square_length, boolean isRow, int num)
		{
			num_squares = square_length;
			is_row = isRow;
			col_row_num = num;
		}
		public ArrayList<Integer> GetIndicesOfCluesWithValue (int clue_val)
		{
			ArrayList<Integer> list = new ArrayList();
			for (int i=0; i<clue_list.length; i++)
			{
				 if (clue_list[i].value == clue_val)
					 list.add(i);
			}
			return list;
		}
		public ArrayList<Integer> GetIndicesOfCluesWithGEValue (int clue_val)
		{
			ArrayList<Integer> list = new ArrayList();
			for (int i=0; i<clue_list.length; i++)
			{
				 if (clue_list[i].value >= clue_val)
					 list.add(i);
			}
			return list;
		}
		public int NumClues ()
		{ return clue_list.length; }
	}
	
	private class Clue2
	{
		private int index;
		private int value;
		private int start_blob = -2;
		private int end_blob = -2;
		private int start_extent = -2;
		private int end_extent = -2;
		private boolean has_blob = false;
		private Clue2 next = null;
		private Clue2 prev = null;
		Clue2 (int idx, int val)
		{
			index = idx;
			value = val;
		}
		public void SetBlob (int s, int e)
		{
			start_blob = s;
			end_blob = e;
			has_blob = true;
			
			// only update extents if uncertainty is 0
			int uncertainty = value - (end_blob-start_blob+1);
			if (uncertainty == 0)
			{
				start_extent = start_blob;
				end_extent   = end_blob;
			}
		}
		public void SetExtents (int s, int e)
		{
			start_extent = s;
			end_extent = e;
		}
		public void SetNext (Clue2 c)
		{ next = c; }
		public void SetPrev (Clue2 c)
		{ prev = c; }
		public boolean IsFixed()
		{ return ((has_blob && (end_blob-start_blob+1)==value) || value==0); }
	}

	// Class to keep track of a Clue and it's possible associations
	private class Clue
	{
		private int min_extent = 0;
		private int max_extent = -1;
		private boolean is_fixed = false; // we know where this clue is 100%
		private boolean is_anchored = false; // we know which Blob this clue is attached to
		private int index;
		private int value;
		private int start_filled = -1;
		private int end_filled = -1;
		
		Clue (int idx, int val)
		{
			index = idx;
			value = val;
		}
		public void SetFixedAtBlob (Blob b)
		{
			is_fixed = true;
			is_anchored = true;
			start_filled = b.start;
			end_filled = b.end;
			min_extent = start_filled;
			max_extent = end_filled;
		}
		public void SetFixed (int start, int end)
		{
			is_fixed = true;
			is_anchored = true;
			start_filled = start;
			end_filled = end;
			min_extent = start_filled;
			max_extent = end_filled;
		}
		public void SetAnchored (int start, int end)
		{
			is_fixed = false;
			is_anchored = true;
			start_filled = start;
			end_filled = end;
			int uncertainty = value - (end-start+1);
			assert (uncertainty >= 0);
			int new_min_extent = start_filled - uncertainty;
			int new_max_extent = end_filled + uncertainty;
			if (new_min_extent > min_extent) min_extent = new_min_extent;
			if (new_max_extent < max_extent) max_extent = new_max_extent;
		}
	}
	
	public BetterPuzzleSolver (PuzzleSolverThread myThread)
	{ mySolverThread = myThread; }
	public BetterPuzzleSolver ()
	{ this(null); }
	
	// ------------------------------------------------
	// Primary puzzle solving "loop":
	// Process all rows first, then process all columns
	// ------------------------------------------------
	public boolean ProcessPuzzle (PBNPuzzle myPuzzle, int guess_level, boolean do_debug)
	{
		int rows = myPuzzle.GetRows();
		int cols = myPuzzle.GetCols();
		
		boolean abort_after_cols = false;
		boolean abort_after_rows = false;
		
		// ----------------------------
		// Loop over all rows to bottom
		// ----------------------------
		for (int row=0; row<rows; row++)
		{
			
			// Gather up the clues
			int[] clues = PuzzleSolver.GetCluesForRowFromPuzzle(myPuzzle, row);
			
			// Gather up the status of the puzzle for this row
			PuzzleSquare squares[] = BetterPuzzleSolver.CopyRowFromPuzzle(myPuzzle, row);
			int count_unknowns_before = CountUnknownSquares (squares);
			
			// Copy the starting row for debugging purposes
			PuzzleSquare save_sqs[] = null;
			if (do_debug)
			{
				save_sqs = new PuzzleSquare[squares.length];
				for (int i=0; i<squares.length; i++)
					save_sqs[i] = new PuzzleSquare (squares[i]);
			}
			
//			System.out.println ("Processing row " + row);
			
			// Process this row
			ProcessLine (clues, squares, guess_level, true, row, false);
			int count_unknowns_after = CountUnknownSquares (squares);
			
			// See if something has changed
			boolean something_changed = count_unknowns_before != count_unknowns_after;
			
			// Update the puzzle with any new changes
			if (something_changed) 
			{
				BetterPuzzleSolver.CopyRowToPuzzle (myPuzzle, squares, row);
				if (mySolverThread != null) mySolverThread.RedrawFrame();
			}
						
			// This is temporarily added in for debugging
			if (do_debug && something_changed && !PuzzleSolver.CheckPuzzleSoFar (myPuzzle, true, do_debug))
			{
				System.out.println ("Puzzle check failed after row " + row + ":\n");
				System.out.println ("Clues:");
				System.out.println (DumpCluesOneLine (clues));
				System.out.println ("Old squares:");
				System.out.println (DumpSquares(save_sqs));
				System.out.println ("New squares:");
				System.out.println (DumpSquares(squares));
				noop();
				// Uncomment the next line for detailed debugging
//				ProcessLine_Better (clues, save_sqs, guess_level, true, row, true);				
				return false;
			}
			
		}
		if (abort_after_rows) return false;
		
		// -----------------------------------
		// Loop over all columns left to right
		// -----------------------------------
		for (int col=0; col<cols; col++)
		{
			
			// Gather up the clues
			int [] clues = PuzzleSolver.GetCluesForColFromPuzzle(myPuzzle, col);
			
			// Gather up the status of the puzzle for this row
			PuzzleSquare squares[] = BetterPuzzleSolver.CopyColFromPuzzle(myPuzzle, col);
			int count_unknowns_before = CountUnknownSquares (squares);		
			
			// Copy the starting row for debugging purposes
			PuzzleSquare save_sqs[] = null;
			if (do_debug)
			{
				save_sqs = new PuzzleSquare[squares.length];
				for (int i=0; i<squares.length; i++)
					save_sqs[i] = new PuzzleSquare (squares[i]);
			}
			
//			System.out.println ("Processing col " + col);
			
			// Process this column
			ProcessLine (clues, squares, guess_level, false, col, false);
			int count_unknowns_after = CountUnknownSquares (squares);			
			
			// See if something has changed
			boolean something_changed = count_unknowns_before != count_unknowns_after;			
			
			// Update the puzzle with any new changes
			if (something_changed) 
			{
				BetterPuzzleSolver.CopyColToPuzzle (myPuzzle, squares, col);
				if (mySolverThread != null) mySolverThread.RedrawFrame();				
			}
			
			// This is temporarily added in for debugging
			if (do_debug && something_changed && !PuzzleSolver.CheckPuzzleSoFar (myPuzzle, true, do_debug))
			{
				System.out.println ("Puzzle check failed after column " + col + ":\n");
				System.out.println ("Clues:");
				System.out.println (DumpCluesOneLine (clues));				
				System.out.println ("Old squares:");
				System.out.println (DumpSquares(save_sqs));
				System.out.println ("New squares:");
				System.out.println (DumpSquares(squares));				
				noop();
				// Uncomment the next line for detailed debugging
//				ProcessLine_Better (clues, save_sqs, guess_level, true, col, true);					
				return false;
			}
		}
		if (abort_after_cols) return false;
		
		
		return true;
	}
	
	public int CountUnknownSquares (PuzzleSquare[] squares)
	{
		int count = 0;
		for (int i=0; i<squares.length; i++)
			if (squares[i].IsUnknown()) count++;
		return count;
	}
	
	// ------------------------------------------------------------------------
	// Process one set of clues and PuzzleSquares (can be either column or row)
	// ------------------------------------------------------------------------
	
	public PuzzleSquare[] ProcessLine (int[] clues, PuzzleSquare[] squares, int guess_level,
			boolean is_row, int num, boolean debug)
	{
		// debug is going to be ignored for now because the calling code
		// will provide the high level before/after picture to stdout, which
		// should be enough.  Any more complicated debugging has to be done
		// by setting do_debug here to true for whatever row or column is
		// causing the problem.
		
		boolean do_debug = debug;	// This is for more detailed debugging
		boolean high_level_debug = false;	// This is for a before/after picture
		
		// -----------------------------------------------------
		// Pre-Check: Make sure there is something for us to do!
		// -----------------------------------------------------
		int count = 0;
		for (int i=0; i<squares.length; i++)
			if (squares[i].IsUnknown()) count++;
		if (count == 0) return squares;
		
		// --------------------------------------------------------------
		// If there are still some UNKNOWNs in our squares, then carry on
		// --------------------------------------------------------------
		
		// Number of squares
		int N = squares.length;
		
		// Create Clues
		int Nclues = clues.length;
		Clues2 myClues = new Clues2(squares.length, is_row, num);
		myClues.clue_list = new Clue2[Nclues+2];
		
		// add a Clue2 at the beginning that anchors the start of the line
		Clue2 clue2 = new Clue2 (-1, 0);
		clue2.SetBlob (-2, -2);	// we do this so that the X between this "clue"
								// and the next one is at -1
		myClues.clue_list[0] = clue2;								
								
		// add our real clues next
		for (int i=0; i<Nclues; i++)
			myClues.clue_list[i+1] = new Clue2 (i, clues[i]);
		
		// add a Clue2 at the end that anchors the end of the line
		clue2 = new Clue2 (clues.length, 0);
		clue2.SetBlob (N+1, N+1); // we do this so that the X between this "clue"
								  // and the next one is at N
		myClues.clue_list[Nclues+1] = clue2;
		
		// Update NClues to include these end clues
		int Nclues2 = Nclues+2;
		
		// Set up Prev and Next Clue2s
		for (int i=0; i<Nclues2-1; i++)
			myClues.clue_list[i].SetNext (myClues.clue_list[i+1]);
		for (int i=1; i<Nclues2; i++)
			myClues.clue_list[i].SetPrev (myClues.clue_list[i-1]);
		
		// ---------------------------------------------------------------------
		// Part 0: Pre-calculate the start/stop extents just based on clues and
		// not based on the actual squares
		// ---------------------------------------------------------------------
		int n = 0;
		// calculate extent starts
		for (int i=0; i<Nclues; i++)
		{
			myClues.clue_list[i+1].start_extent = n;
			n += 1+myClues.clue_list[i+1].value;
		}
		n = N-1;
		// calculate extent ends
		for (int i=Nclues-1; i>=0; i--)
		{
			myClues.clue_list[i+1].end_extent = n;
			n -= 1+myClues.clue_list[i+1].value;
		}
		
		if (do_debug)
		{
			System.out.println ("Initial setup:");
			System.out.println (DumpClues2 (myClues));
			System.out.println (DumpSquares (squares));
			noop();
		}		
		
		if (high_level_debug)
		{
			if (is_row) System.out.println ("Row " + num);
			else        System.out.println ("Col " + num);
			String clues_str = "";
			for (int i=0; i<clues.length; i++)
				clues_str += "" + clues[i] + " ";
			System.out.println (clues_str);
			System.out.println (DumpSquares(squares));
			noop();
		}
				
		// We'll use these vars multiple times
		ArrayList<Clue2>CluesWithBlobs = null;
		
		// -----------------------------------------------------------------
		// Part 1a: Attach fixed blobs (with X's on each end) to clues when
		// the clue value is unique
		// -----------------------------------------------------------------
		
		boolean changing = true;
		while (changing)
		{
			changing = false;
		
			// Generate list of CluesWithBlobs
			CluesWithBlobs = new ArrayList();
			for (int i=0; i<Nclues2; i++)
				if (myClues.clue_list[i].has_blob) CluesWithBlobs.add(myClues.clue_list[i]);
			assert (CluesWithBlobs.size() >= 2);

			// Process the spaces between CluesWithBlobs
			int num_CluesWithBlobs = CluesWithBlobs.size();

			// Process pair of consequtive CluesWithBlobs
			for (int icwb=0; icwb<(num_CluesWithBlobs-1); icwb++)
			{
				Clue2 cwb1 = CluesWithBlobs.get(icwb);
				Clue2 cwb2 = CluesWithBlobs.get(icwb+1);
				
				// Only process if the two CluesWithBlobs are NOT consequtive clues
				if (cwb1.next != cwb2)
				{

					int start_extent = cwb1.end_blob+2;
					int end_extent   = cwb2.start_blob-2;

					ArrayList<Blob> blobs = GetAllBlobs (squares, start_extent, end_extent);

					// (1a) For each Blob that is attached on both edges, see if there is only
					// one Clue that has that same size
					for (Blob b : blobs)
					{
						int b_size = b.GetLength();

						if (b.is_anchored_start && b.is_anchored_end)
						{
							count = 0;
							int match_index = -1;
							for (Clue2 cl=cwb1.next; cl!=cwb2; cl=cl.next)
								if (cl.value == b_size && !cl.has_blob) 
								{
									count++;
									match_index = cl.index;
								}

							if (count == 1)
							{
								Clue2 cl = myClues.clue_list[match_index+1];
								cl.SetBlob(b.start, b.end);
								changing = true;

								if (do_debug)
								{
									System.out.println ("Clue " + cl.index + " (" + cl.value + ") is fixed to blob from " + 
											b.start + " to " + b.end);
									noop();
								}
							}
						}			
					}
				}
			}
		}
		
		if (do_debug)
		{
			System.out.println ("After attaching XFFFX Blobs to Clues");
			System.out.println (DumpSquaresWithClues (squares, myClues));
			noop();
		}
		
		// ---------------------------------------------------------------------
		// (1b) For each Blob that is big enough to be attached to only one Clue
		// anchor it to the Clue.  Remember, at this point, the only Blobs that
		// are attached to a Clue are *fixed* Blobs (with X's on both ends).
		// 
		// In order for this to work, the Blob needs to be attached on both
		// ends.  If it isn't, then sometimes Blobs get matched to the wrong
		// clue altogether when they should have been connected to adjacent
		// Blobs for a different clue.
		// ---------------------------------------------------------------------
		changing = true;
		while (changing)
		{
			changing = false;
		
			// Update CluesWithBlobs list
			CluesWithBlobs = new ArrayList();
			for (int i=0; i<Nclues2; i++)
			{
				Clue2 thisClue = myClues.clue_list[i];
				if (thisClue.IsFixed()) CluesWithBlobs.add(thisClue);
			}
			assert (CluesWithBlobs.size() >= 2);

			// Process the spaces between CluesWithBlobs
			int num_CluesWithBlobs = CluesWithBlobs.size();

			// Process pair of consequtive CluesWithBlobs
			for (int icwb=0; icwb<(num_CluesWithBlobs-1); icwb++)
			{
				Clue2 cwb1 = CluesWithBlobs.get(icwb);
				Clue2 cwb2 = CluesWithBlobs.get(icwb+1);
				
				// Only process if the two CluesWithBlobs are NOT consequtive clues
				if (cwb1.next != cwb2)
				{
					
					int start_extent = cwb1.end_blob+2;
					int end_extent   = cwb2.start_blob-2;

					ArrayList<Blob> blobs = GetAllBlobs (squares, start_extent, end_extent);
				
					for (Blob b : blobs)
					{
						int b_size = b.GetLength();
						if (!b.IsFullyAnchored()) continue;

						count = 0;
						Clue2 save_cl = null;
						int match_index = -1;
						for (Clue2 cl=cwb1.next; cl!=cwb2; cl=cl.next)
						if (!cl.IsFixed() && cl.value >= b_size)
						{
							count++;
							match_index = cl.index;
							save_cl = cl;
						}

						if (count == 1)
						{
							Clue2 cl = save_cl;
							// If Blob length matches clue value, then add
							// X's on each end of the Blob
							if (cl.value == b_size)
							{
								int cell = b.start-1;
								if (cell >= 0)
								{
									assert (!squares[cell].IsFilled());
									if (squares[cell].IsUnknown())
										squares[cell].SetStatus(PuzzleSquare.SquareStatus.EMPTY, guess_level);
								}
								cell = b.end+1;
								if (cell < N)
								{
									assert (!squares[cell].IsFilled());
									if (squares[cell].IsUnknown())
										squares[cell].SetStatus(PuzzleSquare.SquareStatus.EMPTY, guess_level);
								}
								cl.SetBlob(b.start, b.end);					
								if (do_debug)
								{
									System.out.println ("Clue " + cl.index + " (" + cl.value + ") is fixed to blob from " + 
											b.start + " to " + b.end);								
									noop();
								}	
								changing = true;
							} else
							{
								cl.SetBlob(b.start, b.end);
								if (do_debug)
								{
									System.out.println ("Clue " + cl.index + " (" + cl.value + ") is attached to blob from " + 
											b.start + " to " + b.end);
									System.out.println ("   before update extent: " + cl.start_extent + " " + cl.end_extent);
									noop();
								}
								
								if (AttachAndProcessBlobToClue (squares, b, cl, guess_level))
									changing = true;
								
								if (do_debug)
								{
									System.out.println ("   after update extent: " + cl.start_extent + " " + cl.end_extent);
									System.out.println (DumpSquaresWithClues(squares, myClues));
									noop();
								}								
							}
						}
					}
				}
			}
		}	
		
		if (do_debug)
		{
			System.out.println ("After attaching Blobs to Clues");
			System.out.println (DumpSquaresWithClues (squares, myClues));
			noop();
		}		
		
		// ---------------------------------------------------------------------
		// (1c) If there is only one Clue between CluewWithBlobs and only
		// one Blob, then the Blob may be associated with the Clue if it cannot
		// be merged with the Blob to the right or left
		// ---------------------------------------------------------------------
		changing = true;
		while (changing)
		{
			changing = false;
		
			// Update CluesWithBlobs list
			CluesWithBlobs = new ArrayList();
			for (int i=0; i<Nclues2; i++)
				if (myClues.clue_list[i].has_blob) CluesWithBlobs.add(myClues.clue_list[i]);
			assert (CluesWithBlobs.size() >= 2);

			// Process the spaces between CluesWithBlobs
			int num_CluesWithBlobs = CluesWithBlobs.size();

			// Process pair of consequtive CluesWithBlobs
			for (int icwb=0; icwb<(num_CluesWithBlobs-1); icwb++)
			{
				Clue2 cwb1 = CluesWithBlobs.get(icwb);
				Clue2 cwb2 = CluesWithBlobs.get(icwb+1);
				
				int clues_between = 0;
				Clue2 cl = cwb1;
				while (cl.next != cwb2)
				{
					clues_between++;
					cl = cl.next;
				}
				cl = cwb1.next;	// This is the one clue between
				
				// Only process if one Clue between CluesWithBlobs
				if (clues_between == 1)
				{
					
					int start_extent = cwb1.end_blob+2;
					int end_extent   = cwb2.start_blob-2;

					ArrayList<Blob> blobs = GetAllBlobs (squares, start_extent, end_extent);
				
					if (blobs.size() == 1)
					{
						Blob b = blobs.get(0);

						boolean can_connect = BlobCanConnectToPrevBlob (squares, b, cl) ||
											  BlobCanConnectToNextBlob (squares, b, cl);
						
						if (can_connect)
						{
							if (do_debug)
							{
								System.out.println ("Blob " + b.start + " " + b.end + 
										" CAN connect to clue to left or right so it will NOT be atached to Clue " +
										cl.index + " (" + cl.value + ")");
								noop();
							}
						} else // cannot connect to Blobs to right or left
						{
							cl.SetBlob (b.start, b.end);

							if (do_debug)
							{
								System.out.println ("Clue " + cl.index + " (" + cl.value + ") must be attached to blob from " + 
										b.start + " to " + b.end);
								System.out.println ("   before update extent: " + cl.start_extent + " " + cl.end_extent);
								noop();
							}
							
							if (AttachAndProcessBlobToClue (squares, b, cl, guess_level))
								changing = true;						

							if (do_debug)
							{
								System.out.println ("   after update extent: " + cl.start_extent + " " + cl.end_extent);
								System.out.println (DumpSquaresWithClues(squares, myClues));
								noop();
							}
						}
					}
				}
			}
		}
		
		// -----------------------------------------------------------
		// Part (1d): Recalculate the min/max extents of clues between
		// Clues with Blobs
		// -----------------------------------------------------------
		
		// Update CluesWithBlobs list
		CluesWithBlobs = new ArrayList();
		for (int i=0; i<Nclues2; i++)
			if (myClues.clue_list[i].has_blob) CluesWithBlobs.add(myClues.clue_list[i]);
		assert (CluesWithBlobs.size() >= 2);
		
		boolean did_process = false;
		
		// Loop over sequential sets of CwBs
		int num_cwbs = CluesWithBlobs.size();
		for (int i=0; i<num_cwbs-1; i++)
		{
			Clue2 cwb1 = CluesWithBlobs.get(i);
			Clue2 cwb2 = CluesWithBlobs.get(i+1);
			
			// If no clues in between CwBs, then just move on
			if (cwb1.next == cwb2) continue;
			
			did_process = true;
			
			// get possible start of range between CwBs
			int start_range = cwb1.end_blob + 2;
			int end_range = cwb2.start_blob - 2;
			for (int is=start_range; is<N; is++)
				if (squares[is].IsEmpty()) start_range++;
				else break;
			for (int is=end_range; is>=0; is--)
				if (squares[is].IsEmpty()) end_range--;
				else break;			
			if (end_range < start_range) continue;
//			assert (end_range >= start_range);
			
			// now establish start_extents for Clues between CwBs
			int temp_extent= start_range;
			for (Clue2 cl=cwb1.next; cl!=cwb2; cl=cl.next)
			{
				if (cl.start_extent < temp_extent)
					cl.start_extent = temp_extent;
				temp_extent += 1 + cl.value;
			}
			
			// establish end extents for Clues between CwBs
			temp_extent = end_range;
			for (Clue2 cl=cwb2.prev; cl!=cwb1; cl=cl.prev)
			{
				if (temp_extent < cl.end_extent)
					cl.end_extent = temp_extent;
				temp_extent -= (1 + cl.value);
			}
		}
		if (did_process && do_debug)
		{
			System.out.println ("After recalculating start/end extents for Clues with no Blobs");
			System.out.println (DumpClues2 (myClues));			
			noop();
		}		
		
		// --------------------------------------------------------
		// Part (1e): Edge Processing between Clues with Blobs
		// --------------------------------------------------------
		
		// Loop over sequential sets of CwBs (CluesWithBlobs list did NOT
		// change during Part (1d))
		for (int i=0; i<num_cwbs-1; i++)
		{
			Clue2 cwb1 = CluesWithBlobs.get(i);
			Clue2 cwb2 = CluesWithBlobs.get(i+1);
			
			// establish range to search for Blobs and AntiBlobs
			int start_range = cwb1.end_blob  +2;
			int end_range   = cwb2.start_blob-2;
			
			// get all Blobs and AntiBlobs within the range
			ArrayList<Blob> blob_list = GetAllBlobsAndAntiBlobsInRange (squares, start_range, end_range);
			
			// If list is empty, there is nothing to do
			if (blob_list.isEmpty()) continue;
			
			if (do_debug)
			{
				System.out.println ("\nAll Blobs and AntiBlobs between Clue " +
					cwb1.index + " (" + cwb1.value + ") and " +
					cwb2.index + " (" + cwb2.value + ")");
				System.out.println (DumpBlobs(blob_list));
				noop();
			}
						
			// If no clues in between CwBs, then remove any X____X
			// attached anti-blobs (there still could Blobs that can be
			// connected to the clues to the right or left)
			if (cwb1.next == cwb2)
			{
				for (Blob b : blob_list)
				{
					if (b instanceof AntiBlob && b.is_anchored_start && b.is_anchored_end)
					{
						for (int is=b.start; is<=b.end; is++)
							if (squares[is].IsUnknown())
							{
								assert (!squares[is].IsFilled());
								squares[is].SetStatus (PuzzleSquare.SquareStatus.EMPTY, guess_level);
							}
						if (do_debug)
						{
							System.out.println ("Clearing AntiBlob " + b.start + " " + b.end 
								+ " between Clue " +
								cwb1.index + " (" + cwb1.value + ") and " +
								cwb2.index + " (" + cwb2.value + ")");
							noop();
						}
					}
				}
				
			// process space between CwBs when there are Clues in between
			} else {
				
				// Loop over each AntiBlob and Blob and ascertain which clues
				// it could contain based on size and whether it is anchored
				// or not.  Also consider if the Blob/AntiBlob could be
				// attached to the Blob on its left or right as appropriate.
				for (Blob b : blob_list)
				{
					// Initialize clue_list for the Blob
					b.clue_list = new ArrayList();
					
					// Get SquareStatus and location of the first non blob-type
					// to the left and right
					PuzzleSquare.SquareStatus ss = PuzzleSquare.SquareStatus.FILLED;
					if (b instanceof AntiBlob) ss = PuzzleSquare.SquareStatus.UNKNOWN;
					int left_loc = GetLocNextSquareStatusToLeftNotSS (squares, b.start, ss);
					int right_loc = GetLocNextSquareStatusToRightNotSS (squares, b.end, ss);
					PuzzleSquare.SquareStatus leftSS = PuzzleSquare.SquareStatus.EMPTY;
					PuzzleSquare.SquareStatus rightSS = PuzzleSquare.SquareStatus.EMPTY;
					if (left_loc >= 0) leftSS = squares[left_loc].GetStatus();
					if (right_loc < N) rightSS = squares[right_loc].GetStatus();
					
					// Consider each clue, including the anchoring Clues, just
					// in case the Blob or AntiBlob can be associated with those
					// Clues
//					for (Clue2 cl=cwb1.next; cl!=cwb2; cl=cl.next)
					for (Clue2 cl=cwb1; cl!=cwb2.next; cl=cl.next)
					{

						// Calculate overlap of Blob or AntiBlob with Clue
						int start_overlap = (b.start < cl.start_extent) ? cl.start_extent : b.start;
						int end_overlap   = (b.end   > cl.end_extent  ) ? cl.end_extent   : b.end;
						int overlap_len;
						// Consider NO overlap
						if (b.end < cl.start_extent || b.start > cl.end_extent)
							overlap_len = 0;
						else
							overlap_len = end_overlap - start_overlap + 1;
						
						// if there's no overlap, move on to next Clue
						if (overlap_len == 0) continue;
						
						// If this is a Blob (not an Anti-Blob), then it 
						// must be fully contained within the Clue's extent
						if (!(b instanceof AntiBlob) && 
							(b.start < cl.start_extent || b.end > cl.end_extent)) continue;
						
						// if Empty on both ends, then let's consider what clues
						// could fit within the AntiBlob or Blob
						if (leftSS == PuzzleSquare.SquareStatus.EMPTY &&
							rightSS == PuzzleSquare.SquareStatus.EMPTY)
						{
							// Consider an AntiBlob X______X
							if (b instanceof AntiBlob) {
								// Consider the overlap between the AntiBlob
								// and the clue's extent
								if (overlap_len >= cl.value)
								{
									b.clue_list.add(cl);
									if (do_debug)
									{
										System.out.println ("X__X AntiBlob at " + b.start + " " + b.end +
											" *could* contain Clue " + cl.index + " (" + cl.value + ")");
//										System.out.println (DumpSquares(squares));
										noop();
									}
								}
								
							// Consider the Blob XFFFFFX
							} else {
								// Blob length must match the clue value
								if (b.GetLength() == cl.value && overlap_len == cl.value)
								{
									b.clue_list.add(cl);
									if (do_debug)
									{
										System.out.println ("XFFX Blob at " + b.start + " " + b.end +
											" *could* match Clue " + cl.index + " (" + cl.value + ")");
//										System.out.println (DumpSquares(squares));
										noop();
									}									
								}
							}
							
						// if Empty on left end and not-Blob-type on right end...
						// (e.g. XBBBBB____... or X_______BBB...)
						} else if (leftSS == PuzzleSquare.SquareStatus.EMPTY)
						{
							// Consider the AntiBlob X____BB...
							if (b instanceof AntiBlob) {
								
								// Consider putting a Clue in the AntiBlob's
								// space separate from the adjacent Blob
								// (e.g. XFFF__BB...)
								
								// Recalculate overlap's end if it happens
								// to fall on that last square of the AntiBlob
								int temp_end_overlap = end_overlap;
								if (end_overlap == b.end) temp_end_overlap--;
								int temp_overlap_len = temp_end_overlap - start_overlap + 1;
								if (temp_overlap_len >= cl.value)
								{
									b.clue_list.add(cl);
									if (do_debug)
									{
										System.out.println ("X_____B... AntiBlob at " + b.start + " " + b.end +
											" *could* independently contain Clue " + cl.index + " (" + cl.value + ")");
//										System.out.println (DumpSquares(squares));
										noop();
									}	
									continue;
								}
								
								// Consider using the space in the AntiBlob and
								// merging it with the Blob(s) to the right to see
								// if that could also work.  Use a brute force
								// method
								
								int cell = 0;
								boolean found_a_soln = false;
								for (int istart=b.start; istart<=b.end && !found_a_soln; istart++)
								{
									found_a_soln = true;
									for (int ii=0; ii<cl.value && found_a_soln; ii++)
									{
										cell = istart+ii;
										if (cell < N && squares[cell].IsEmpty()) found_a_soln = false;
									}
									if (found_a_soln && cell< N-1 && squares[cell+1].IsFilled()) found_a_soln = false;
								}
								
								// This recalculated blob end must be within the
								// Clue's extent AND the Blob must be <= clue value
								if (found_a_soln)
								{
									b.clue_list.add(cl);
									if (do_debug)
									{
										System.out.println ("X_____B... AntiBlob at " + b.start + " " + b.end +
											" *could* combine with Blob to the right to contain Clue " + cl.index + " (" + cl.value + ")");
//										System.out.println (DumpSquares(squares));
										noop();
									}
								}
								
							// Consider the Blob XBBBBB__...  or clue value 5 with XB_B_B_...
							} else {
								
								// If Blob length is too big for current Clue, then
								// move on
								if (b.GetLength() > cl.value) continue;
								
								// Now let's edge process to see if we would have
								// enough room for this clue
								int end_cell = b.start + cl.value - 1;
								if (end_cell >= N) continue;
								
								// If any X's between b.start and end_cell, then it
								// won't fit
								boolean will_fit = true;
								for (int is=b.start; is<=end_cell; is++)
									if (squares[is].IsEmpty()) will_fit = false;
								if (!will_fit) continue;
								
								// If the end_cell is adjacent to another B, then it
								// also won't fit
								if (end_cell < N-1 && squares[end_cell+1].IsFilled())
									continue;
											
								// If the end_cell is outside the clue's extent, then
								// it also won't fit
								if (end_cell > cl.end_extent)
									continue;
								
								// If we're here, then it could work!
								b.clue_list.add(cl);
								if (do_debug)
								{
									System.out.println ("XFFFFF__... Blob at " + b.start + " " + b.end +
										" *could* contain Clue " + cl.index + " (" + cl.value + ")");
//									System.out.println (DumpSquares(squares));
									noop();									
								}								
							}
							
						// if Empty on right end and not-Blob-type on the left end...
						// (e.g. ___BBBBBBBX or BB________X)
						} else if (rightSS == PuzzleSquare.SquareStatus.EMPTY)
						{
							// Consider the AntiBlob ...BB____X
							if (b instanceof AntiBlob) {
								
								// Consider putting a Clue in the AntiBlob's
								// space separate from the adjacent Blob
								// (e.g. XFFF__BB...)
								
								// Recalculate overlap's end if it happens
								// to fall on that last square of the AntiBlob
								int temp_start_overlap = start_overlap;
								if (start_overlap == b.start) temp_start_overlap++;
								int temp_overlap_len = end_overlap - temp_start_overlap + 1;
								if (temp_overlap_len >= cl.value)
								{
									b.clue_list.add(cl);
									if (do_debug)
									{
										System.out.println ("...BB_____X AntiBlob at " + b.start + " " + b.end +
											" *could* independently contain Clue " + cl.index + " (" + cl.value + ")");
//										System.out.println (DumpSquares(squares));
										noop();
									}	
									continue;
								}
								
								// Consider using the space in the AntiBlob and
								// merging it with the Blob(s) to the left to see
								// if that could also work
								
								int cell = 0;
								boolean found_a_soln = false;
								for (int istart=b.end; istart>=b.start && !found_a_soln; istart--)
								{
									found_a_soln = true;
									for (int ii=0; ii<cl.value && found_a_soln; ii++)
									{
										cell = istart-ii;
										if (cell < 0 || squares[cell].IsEmpty()) found_a_soln = false;
									}
									if (found_a_soln && cell>0 && squares[cell-1].IsFilled()) found_a_soln = false;
								}
								
								// This recalculated blob end must be within the
								// Clue's extent AND the Blob must be <= clue value
								if (found_a_soln)
								{
									b.clue_list.add(cl);
									if (do_debug)
									{
										System.out.println ("...BB_____X AntiBlob at " + b.start + " " + b.end +
											" *could* combine with Blob to the left to contain Clue " + cl.index + " (" + cl.value + ")");
//										System.out.println (DumpSquares(squares));
										noop();
									}
								}
								
							// Consider the Blob ...___BBBBX  or clue value 5 with ...B_B_BX
							} else {
								
								// If Blob length is too big for current Clue, then
								// move on
								if (b.GetLength() > cl.value) continue;
								
								// Now let's edge process to see if we would have
								// enough room for this clue
								int end_cell = b.end - cl.value + 1;
								if (end_cell < 0) continue;
								
								// If any X's between b.start and end_cell, then it
								// won't fit
								boolean will_fit = true;
								for (int is=b.end; is>=end_cell; is--)
									if (squares[is].IsEmpty()) will_fit = false;
								if (!will_fit) continue;
								
								// If the end_cell is adjacent to another B, then it
								// also won't fit
								if (end_cell > 0 && squares[end_cell-1].IsFilled())
									continue;
											
								// If the end_cell is outside the clue's extent, then
								// it also won't fit
								if (end_cell < cl.start_extent)
									continue;
								
								// If we're here, then it could work!
								b.clue_list.add(cl);
								if (do_debug)
								{
									System.out.println ("...___BBX Blob at " + b.start + " " + b.end +
										" *could* contain Clue " + cl.index + " (" + cl.value + ")");
//									System.out.println (DumpSquares(squares));
									noop();									
								}								
							}
							
						// Blob surrounded ____BBBBBB____
						} else if (!(b instanceof AntiBlob))
						{
							// If Blob length is too big, then no go
							if (b.GetLength() > cl.value) continue;
							
							// Okay this is going to be rather brute forcy...
							// but just see if there is *anywhere* we can put our
							// current clue, within the clue extents and considering
							// the uncertainty.
							int uncertainty = cl.value - b.GetLength();
							int start_check_cell = b.start - uncertainty;
							if (start_check_cell < cl.start_extent)
								start_check_cell = cl.start_extent;
							boolean does_fit = false;
							// loop over all possible clue positions
							for (int istart=start_check_cell; istart<=b.start && !does_fit; istart++)
							{
								int iend = istart+cl.value-1;
								// only process if iend falls within the clue's extent
								if (iend <= cl.end_extent)
								{
									does_fit = true;
									int cell = istart;
									for (int ii=0; ii<cl.value && does_fit; ii++)
										if (squares[cell++].IsEmpty()) does_fit = false;
									// if it fits so far, make sure that we don't have a
									// filled square right before our test clue position or
									// a filled square right after
									if (istart > 0 && squares[istart-1].IsFilled()) does_fit = false;
									if (iend < N-1 && squares[iend+1].IsFilled()) does_fit = false;
								}
							}
							
							// If we're here and does_fit is true, then this Blob
							// can be associated with the Clue
							if (does_fit)
							{
								b.clue_list.add(cl);
								if (do_debug)
								{
									System.out.println ("...___BB__... Blob at " + b.start + " " + b.end +
										" *could* contain Clue " + cl.index + " (" + cl.value + ")");
//									System.out.println (DumpSquares(squares));
									noop();																		
								}
							}
							
						// AntiBlob surrounded BBBB_______BB
						} else
						{
							// If AntiBlob length and overlap is big enough, then we can
							// put our Clue in this AntiBlob
							int temp_start_overlap = start_overlap;
							int temp_end_overlap   = end_overlap;
							if (temp_start_overlap == b.start) temp_start_overlap++;
							if (temp_end_overlap   == b.end)   temp_end_overlap--;
							if (temp_end_overlap-temp_start_overlap+1 >= cl.value)
							{
								b.clue_list.add(cl);
								if (do_debug)
								{
									System.out.println ("...BB____BB... AntiBlob at " + b.start + " " + b.end +
										" *could* contain Clue " + cl.index + " (" + cl.value + ") by itself");
//									System.out.println (DumpSquares(squares));
									noop();																											
								}
								continue;
							}
							
							// Okay this is going to be rather brute forcy...
							// but just see if there is *anywhere* we can put our
							// current clue, within the clue extents and considering
							// the adjacent Blobs and our AntiBlob
							
							// start with clue value ENDING at the first square in our overlap area
							// (Remember this is the overlap between the AntiBlob and the clue's extent
							int start_cell = start_overlap-cl.value+1;
							if (start_cell < cl.start_extent) start_cell = cl.start_extent;
							// end with clue value STARTING at the last square in our overlap area
							int end_start_cell = end_overlap;
							
							boolean does_fit = false;
							for (int istart=start_cell; istart<=end_start_cell && !does_fit; istart++)
							{
								// If we have a FILLED square right before the start of our clue, then no-go
								if (istart > 0 && squares[istart-1].IsFilled()) continue;
								
								does_fit = true;
								int cell = istart;
								for (int ii=0; ii<cl.value && does_fit; ii++)
								{
									if (cell < 0 || cell >= N) does_fit = false;
									else if (squares[cell].IsEmpty()) does_fit = false;
									cell++;
								}
								if (does_fit && cell < N && squares[cell].IsFilled()) does_fit = false;
								if (does_fit && (istart < cl.start_extent || (cell-1) > cl.end_extent)) does_fit = false;
							}						
							
							// If we're here and does_fit is true, then this AntiBlob
							// can be associated with the Clue
							if (does_fit)
							{
								b.clue_list.add(cl);
								if (do_debug)
								{
									System.out.println ("...BB___BB... AntiBlob at " + b.start + " " + b.end +
										" *could* contain Clue " + cl.index + " (" + cl.value + ")");
//									System.out.println (DumpSquares(squares));
									noop();																		
								}
							}							
						}
					}
				}
				
				// Report our findings so far
				if (do_debug)
				{
					System.out.println (DumpBlobsWithClueList(blob_list));
					noop();
				}
				
				// ------------------------------------------------------------------
				// Now post-process the Blobs and Anti-Blobs based on which clues are
				// associated with which Blobs
				// ------------------------------------------------------------------
				
				// Pre-process our Blob list: if a true Blob has only one Clue and
				// that one Clue is not associated with any other Blobs,
				// remove that Clue from all other Blobs and Anti-Blobs
				for (Blob b: blob_list)
				{
					if (!(b instanceof AntiBlob) && b.clue_list.size() == 1)
					{
						Clue2 cl = b.clue_list.get(0);
						int cl_index = cl.index;
						
						// See if this Clue is associated with any other true Blobs
						int cb = 0;
						for (Blob bb: blob_list)
							if (bb != b && !(bb instanceof AntiBlob) && bb.HasIndexInClueList(cl_index) &&
									BlobsCanConnectForClueValue (squares, b, bb, cl.value))
								cb++;
						
						// Remove this Clue from all other Blobs and Anti-Blobs that
						// are NOT connected to this Blob except this one
						boolean clue_is_unique = false;
						if (cb == 0)
						{
							for (Blob bb: blob_list)
								if (bb != b) 
								{
									if (bb instanceof AntiBlob)
									{
										if (!BlobAndAntiBlobAreAdjacent (b, (AntiBlob)bb))
											bb.RemoveClueIndexFromClueList(cl_index);
									} else
										bb.RemoveClueIndexFromClueList(cl_index);
								}
							clue_is_unique = true;		
							
						// Remove from other Blob's clue lists if there is no
						// way the current blob could be connected to the other blob
						} else
						{
							for (Blob bb : blob_list)
							{
								if (bb != b && !(bb instanceof AntiBlob) && bb.HasIndexInClueList(cl_index))
								{
									boolean can_connect = true;
									int bstart = b.start;
									int bend   = b.end;
									if (bb.start > b.end) bend = bb.end;
									if (bb.end < b.start) bstart = bb.start;
									for (int ii=bstart; ii<=bend && can_connect; ii++)
										if (squares[ii].IsEmpty()) can_connect = false;
									int blen = bend-bstart+1;
									if (!can_connect || blen > myClues.clue_list[cl_index+1].value)
										bb.RemoveClueIndexFromClueList(cl_index);
								}
							}
							
							// Recount number of times this clue shows up with other real Blobs
							cb = 0;
							for (Blob bb: blob_list)
								if (bb != b && !(bb instanceof AntiBlob) && bb.HasIndexInClueList(cl_index) &&
										BlobsCanConnectForClueValue (squares, b, bb, cl.value))
									cb++;
							
							clue_is_unique = cb == 0;							
						}
						
						if (clue_is_unique)
						{
							// Also, remove all clue_indices higher than the one that
							// is anchored to the current Blob b from the clue_lists
							// to our left
							int our_b_index = blob_list.indexOf(b);
							int n_real_clues = myClues.GetNumClues()-2;
							if (our_b_index > 0 && cl_index < n_real_clues-1)
							{
								for (int ib=0; ib<our_b_index; ib++)
								{
									Blob bb = blob_list.get(ib);
									// Remove all clue indices > our cl_index
									for (int ii=cl_index+1; ii<n_real_clues; ii++)
										bb.RemoveClueIndexFromClueList(ii);
								}
							}
							// Also, remove all clue_indices lower than the one that
							// is anchored to the current Blob b from the clue_lists
							// to our right
							if (our_b_index < blob_list.size()-1 && cl_index > 0)
							{
								for (int ib=our_b_index+1; ib<blob_list.size(); ib++)
								{
									Blob bb = blob_list.get(ib);
									// Remove all clue indices > our cl_index
									for (int ii=0; ii<cl_index; ii++)
										bb.RemoveClueIndexFromClueList(ii);
								}
							}							
						}
					}
				}
								
				// Report our findings so far
				if (do_debug)
				{
					System.out.println ("After 1st pre-processing the Blobs with Clues list:");
					System.out.println (DumpBlobsWithClueList(blob_list));
					noop();
				}	
				
				// If a Blob is associated with only one Clue, process that Blob for
				// that Clue
				for (Blob b : blob_list)
				{
					// If a Blob (not an AntiBlob) 
					if (!(b instanceof AntiBlob) && b.clue_list.size() == 1)
					{
						Clue2 cl = b.clue_list.get(0);
						int clue_index = cl.index;
						cl.SetBlob(b.start, b.end);
						AttachAndProcessBlobToClue (squares, b, cl, guess_level);
						
						// If this clue is in multiple true Blobs, do NOT remove
						// from other clue lists
						int ib = 0;
						for (Blob bb : blob_list)
							if (bb != b && !(bb instanceof AntiBlob) && bb.HasIndexInClueList(clue_index) &&
									BlobsCanConnectForClueValue (squares, b, bb, cl.value))
								ib++;
						
						// Remove this clue_index from all other clue_lists
						if (ib == 0)
							for (Blob bb : blob_list)
								if (bb != b)
								{
									if (bb instanceof AntiBlob)
									{
										if (!BlobAndAntiBlobAreAdjacent (b, (AntiBlob)bb))
											bb.RemoveClueIndexFromClueList(clue_index);
									} else
										bb.RemoveClueIndexFromClueList(clue_index);
								}

						// Update extents for next Clue and prev Clue
						Clue2 nextClue = cl.next;
						Clue2 prevClue = cl.prev;
						if (nextClue != null && nextClue.start_extent < b.end+1)
							nextClue.start_extent = b.end+1;
						if (prevClue != null && prevClue.end_extent > b.start-1)
							prevClue.end_extent = b.start-1;
						
						if (do_debug)
						{
							System.out.println ("Blob at " + b.start + " " + b.end +
								" processed for Clue " + cl.index + " (" + cl.value + ")");
							noop();
						}
					}
				}
				
				if (do_debug)
				{
					System.out.println ("After processing all Blobs with only one Clue:");
					System.out.println (DumpBlobsWithClueList (blob_list));
					System.out.println (DumpSquaresWithClues (squares, myClues));
					noop();
				}
				
				// Now look if any AntiBlobs have no possible clues AND X them out
				// (we've already checked to see if they could contain *part* of
				// another clue)
				for (Blob b : blob_list)
				{
					if ((b instanceof AntiBlob) && b.clue_list.isEmpty())
					{
						// It is possible that this AntiBlob no longer exists! so
						// we need to only X it out if it is UNKNOWN
						for (int cell=b.start; cell<=b.end; cell++)
							if (squares[cell].IsUnknown())
								squares[cell].SetStatus(PuzzleSquare.SquareStatus.EMPTY, guess_level);
						
						if (do_debug)
						{
							System.out.println ("AntiBlob at " + b.start + " " + b.end +
								" has no Clues so possibly X'd out");
							noop();
						}						
					}
				}
				
				if (do_debug)
				{
					System.out.println ("After Xing out AntiBlobs with no Clues:");
					System.out.println (DumpBlobsWithClueList (blob_list));
					System.out.println (DumpSquaresWithClues (squares, myClues));
					noop();
				}				
				
				// look for any clues which are associated with only one Blob or AntiBlob
				int start_clue_index = cwb1.index+1;
				int end_clue_index   = cwb2.index-1;
				for (int clue_index=start_clue_index; clue_index<=end_clue_index; clue_index++)
				{
					// Count # of Blobs associated with this clue_index
					int b_count = 0;
					Blob saveBlob = null;
					for (Blob b : blob_list)
					{
						if (b.clue_list.size() > 0)
						{
							for (Clue2 cl : b.clue_list)
								if (cl.index == clue_index)
								{
									b_count++;
									saveBlob = b;
								}
						}
					}
					
					// If the count is 1, then let's process this clue and Blob
					if (b_count == 1)
					{
						if (!(saveBlob instanceof AntiBlob)) {
							
							Clue2 myClue = myClues.clue_list[clue_index+1];	// Remember the clue_list has fake Clues at the edges!
							myClue.SetBlob (saveBlob.start, saveBlob.end);
							saveBlob.clue_list = new ArrayList();	// Remove all clues associated with this Blob now
							// Remove this clue_index from all other clue_lists
							for (Blob bb : blob_list)
								if (bb != saveBlob)
									bb.RemoveClueIndexFromClueList(clue_index);			
							
							if (do_debug)
							{
								System.out.println ("Blob at " + saveBlob.start + " " + saveBlob.end +
									" was the only one with Clue " + clue_index + " (" + myClue.value + ")");
								noop();
							}													
							
						} else {
							
							// process size of AntiBlob in case edges have FILLED spaces
							int start_ab = saveBlob.start;
							int end_ab   = saveBlob.end;
							// Take out the following two lines - we can't assume that the FILLED
							// squares at one end or the other of the Anti-Blob is NOT going to be used
							// by the clue
//							if (start_ab > 0 && squares[start_ab-1].IsFilled()) start_ab++;
//							if (end_ab < N-1 && squares[end_ab+1].IsFilled()) end_ab--;
							Clue2 myClue = myClues.clue_list[clue_index+1];
							if (start_ab < myClue.start_extent) start_ab = myClue.start_extent;
							if (end_ab   > myClue.end_extent)   end_ab   = myClue.end_extent;
							
							// now see what we've got
							int extent_len = end_ab - start_ab + 1;
							int uncertainty = extent_len - myClue.value;
							
							// if uncertainty is < clue value, then we can process in the
							// middle of the AntiBlob
							if (uncertainty < myClue.value && uncertainty >= 0)
							{
								int temp_val = myClue.value - uncertainty;
								for (int ii=0; ii<temp_val; ii++)
								{
									int cell = start_ab+uncertainty+ii;
									if (squares[cell].IsUnknown())
									{
										assert (!squares[cell].IsEmpty());
										squares[cell].SetStatus(PuzzleSquare.SquareStatus.FILLED, guess_level);
									}
								}
								myClue.SetBlob (start_ab+uncertainty, start_ab+uncertainty+temp_val-1);
								if (uncertainty == 0)
									PutEmptyOnBothEnds (squares, start_ab, start_ab+myClue.value-1, guess_level);
								
								// Just because this clue fit within this AntiBlob, that
								// doesn't mean that all other clues do NOT fit.  We should only
								// eliminate the clues in our list that truly don't fit now that
								// we know that this clue does fit.
								if (saveBlob.clue_list.size() == 1 && saveBlob.is_anchored_end && saveBlob.is_anchored_start)
								{
									// Remove this clue_index from all other clue_lists
									for (Blob bb : blob_list)
										if (bb != saveBlob)
											bb.RemoveClueIndexFromClueList(clue_index);	
								}
								
								if (do_debug)
								{
									System.out.println ("AntiBlob at " + saveBlob.start + " " + saveBlob.end +
										" was the only one with Clue " + clue_index + " (" + myClue.value + ") and it fits!");
									noop();
								}																						
							}
						}
					}
				}
				
				if (do_debug)
				{
					System.out.println ("After Clues associated with only one Blob or AntiBlob:");
					System.out.println (DumpBlobsWithClueList (blob_list));
					System.out.println (DumpSquaresWithClues (squares, myClues));
					noop();
				}							
				
				// If a Blob (true Blob, not AntiBlob) associated with multiple Clues,
				// then process the Blob as if it is the smallest clue value, but don't
				// *assume* it is that clue value
				for (Blob b : blob_list)
				{
					// process true Blobs for minimum clue_value
					if (!(b instanceof AntiBlob) && b.clue_list.size() > 0)
					{
						boolean all_clue_values_same = true;
						int min_clue_value = 0;
						int num_clues = b.clue_list.size();
						for (Clue2 cl : b.clue_list)
							if (min_clue_value == 0) min_clue_value = cl.value;
							else {
								if (cl.value != min_clue_value)
									all_clue_values_same = false;
								if (cl.value < min_clue_value) 
									min_clue_value = cl.value;								
							}
						
						// process for smallest clue value
						if (all_clue_values_same)
						{
							// This Blob has a fixed Clue value, but we're not sure
							// which Clue it is associated with
							if (num_clues > 1) {
								AttachAndProcessBlobToClue (squares, b, min_clue_value, guess_level);
								if (do_debug)
								{
									System.out.println ("Blob at " + b.start + " " + b.end +
										" must have clue value " + min_clue_value + "");
									noop();								
								}								
							} else if (num_clues == 1) {
								Clue2 myClue = b.clue_list.get(0);
								myClue.SetBlob (b.start, b.end);
								AttachAndProcessBlobToClue (squares, b, myClue, guess_level);
								if (do_debug)
								{
									System.out.println ("Blob at " + b.start + " " + b.end +
										" is matched to Clue " + myClue.index + " (" + min_clue_value + ")");
									noop();								
								}									
							}
						} else
						{
							// Process Blob for minimum clue size
							int uncertainty = min_clue_value - b.GetLength();
							if (uncertainty < min_clue_value)
							{
								// find nearest X on the left side X__FFF (but abort if
								// we encounter an F first)
								int left_X = -1;
								boolean found_left_X = false;
								if (b.start > 0)
								{
									int ii;
									for (ii=b.start-1; ii>=0; ii--)
									{
										if (squares[ii].IsFilled()) break;
										else if (squares[ii].IsEmpty())
										{
											left_X = ii;
											found_left_X = true;
											break;
										}
									}
									if (ii<0) {
										found_left_X = true;
										left_X = -1;
									}
								}
								
								// process clue from left edge if not too far away
								if (found_left_X && (b.start-left_X-1) < uncertainty)
								{
									// process from left edge
									boolean is_filling = false;
									int cell = left_X+1;
									for (int ii=0; ii<min_clue_value; ii++)
									{
										if (squares[cell].IsFilled()) is_filling = true;
										else if (is_filling)
										{
											if (squares[cell].IsUnknown())
											{
												assert (!squares[cell].IsEmpty());
												squares[cell].SetStatus(PuzzleSquare.SquareStatus.FILLED, guess_level);
											}
										}
										cell++;
									}
									
									if (do_debug)
									{
										System.out.println ("Blob at " + b.start + " " + b.end +
											" is processed from left X for minimum clue value (" + min_clue_value + ")");
										noop();								
									}									
									
								}
								
								// find nearest X on the ride side FFF__X (but abort if
								// we encounter an F first).
								int right_X = N;
								boolean found_right_X = false;
								if (b.end < N-1)
								{
									int ii;
									for (ii=b.end+1; ii<N; ii++)
									{
										if (squares[ii].IsFilled()) break;
										else if (squares[ii].IsEmpty())
										{
											right_X = ii;
											found_right_X = true;
											break;
										}
									}
									if (ii==N) {
										found_right_X = true;
										right_X = N;
									}
								}
								
								// process clue from right edge if not too far away
								if (found_right_X && (right_X-b.end-1) < uncertainty)
								{
									// process from left edge
									boolean is_filling = false;
									int cell = right_X-1;
									for (int ii=0; ii<min_clue_value; ii++)
									{
										if (squares[cell].IsFilled()) is_filling = true;
										else if (is_filling)
										{
											if (squares[cell].IsUnknown())
											{
												assert (!squares[cell].IsEmpty());
												squares[cell].SetStatus(PuzzleSquare.SquareStatus.FILLED, guess_level);
											}
										}
										cell--;
									}
									
									if (do_debug)
									{
										System.out.println ("Blob at " + b.start + " " + b.end +
											" is processed from right X for minimum clue value (" + min_clue_value + ")");
										noop();								
									}																		
								}								
							}
						}
					}
				}

				if (do_debug)
				{
					System.out.println ("After processing Blobs with multiple Clues:");
					System.out.println (DumpBlobsWithClueList (blob_list));
					System.out.println (DumpSquaresWithClues (squares, myClues));
					noop();
				}								
			}
		}
													
		// X out any UNKNOWN squares that lay outside the extents of consequtive clues		
		
		if (do_debug)
		{
			System.out.println ("After edge processing between Clues with no Blobs");
			System.out.println (DumpSquaresWithClues (squares, myClues));
			noop();			
		}
		
		// X out any UNKNOWN squares that lay outside the extents of consequtive clues
		for (int i=0; i<myClues.GetNumClues()-1; i++)
		{
			Clue2 cl1 = myClues.clue_list[i];
			Clue2 cl2 = myClues.clue_list[i+1];
			int start = cl1.end_extent+1;
			int end   = cl2.start_extent-1;
			if (start <  0) start = 0;
			if (end   >= N) end = N-1;
			for (int isq=start; isq<=end; isq++)
				if (squares[isq].IsUnknown())
					squares[isq].SetStatus(PuzzleSquare.SquareStatus.EMPTY, guess_level);
		}

		if (high_level_debug)
		{
			System.out.println (DumpSquaresWithClues (squares, myClues));
			noop();
		}
		
		
		return squares;
	}
	
	private boolean BlobAndAntiBlobAreAdjacent (Blob b, AntiBlob ab)
	{	
		// because Blobs may have changed in their start/ends since the first
		// time they were created, we're going to check for overlap here
		// instead of direct adjacency
		if (ab.end+1 >= b.start  && ab.start <= b.start) return true;
		if (ab.start <= b.end+1  && ab.end   >= b.end  ) return true;
		return false;
	}
	
	private boolean BlobsCanConnectForClueValue (PuzzleSquare[] squares, Blob b1, Blob b2, int clue_value)
	{
		int start_combined = b1.start < b2.start ? b1.start : b2.start;
		int end_combined   = b1.end   > b2.end   ? b1.end   : b2.end;
		if (end_combined - start_combined + 1 > clue_value) return false;
		for (int i=start_combined; i<=end_combined; i++)
			if (squares[i].IsEmpty()) return false;
		return true;
	}
	
	private boolean AttachAndProcessBlobToClue (PuzzleSquare[] squares, Blob b, int clue_val, int guess_level)
	{
		// Make a fake Clue2 so we can process a Blob with only a clue value
		Clue2 fakeClue = new Clue2(0, clue_val);
		fakeClue.start_extent = 0;
		fakeClue.end_extent = squares.length-1;
		return (AttachAndProcessBlobToClue (squares, b, fakeClue, guess_level));
	}
	
	private boolean AttachAndProcessBlobToClue (PuzzleSquare[] squares, Blob b, Clue2 cl, int guess_level)
	{
		// return if anything changed or not
		boolean something_changed = false;
		
		// number of squares
		int N = squares.length;
		
		// save old clue extents to see if they've changed
		int old_start_extent = cl.start_extent;
		int old_end_extent   = cl.end_extent;
		
		// calculate uncertainty
		int uncertainty = cl.value - b.GetLength();
		if (uncertainty < 0) return something_changed;
//		assert (uncertainty >= 0);

		// we can update the start/stop extents, possibly, to
		// bring them in closer to the clue
		int possible_start_extent = b.start-uncertainty;
		if (cl.start_extent < possible_start_extent)
			cl.start_extent = possible_start_extent;
		int possible_end_extent = b.end+uncertainty;
		if (cl.end_extent > possible_end_extent)
			cl.end_extent = possible_end_extent;
		
		// ----------------------------------------------------------------
		// let's do the simplest processing first just based on the updated
		// extents and the clue value
		// ----------------------------------------------------------------
		int ext_uncertainty = (cl.end_extent - cl.start_extent + 1) - cl.value;
		if (ext_uncertainty < 0) return something_changed;
//		assert (ext_uncertainty >= 0);
		if (ext_uncertainty < cl.value && ext_uncertainty >= 0)
		{
			int temp_val = cl.value-ext_uncertainty;
			int cell = cl.start_extent + ext_uncertainty;
			for (int i=0; i<temp_val; i++)
			{
				if (squares[cell].IsUnknown())
				{
					assert (!squares[cell].IsEmpty());
					squares[cell].SetStatus(PuzzleSquare.SquareStatus.FILLED, guess_level);
					something_changed = true;
				}
				cell++;
			}
			// extend outwards to get this blob's full extent
			int bstart = cl.start_extent + ext_uncertainty;
			int bend = cell-1;
			if (bstart > 0)
			{
				while (bstart >= 0 && squares[bstart].IsFilled())
					bstart--;
				bstart++;
			}
			if (bend < N-1)
			{
				while (bend < N && squares[bend].IsFilled())
					bend++;
				bend--;
			}
			
			// now if necessary, merge this blob with the blob that was input to
			// this method, which we know is associated with this clue
			if (bend < b.start-1)
			{
				for (int i=bend+1; i<b.start; i++)
				{
					if (squares[i].IsUnknown())
					{
						assert (!squares[i].IsEmpty());
						squares[i].SetStatus(PuzzleSquare.SquareStatus.FILLED, guess_level);
						something_changed = true;
					}
				}
				// now bend is b.end
				bend = b.end;
			}
			if (bstart > b.end+1)
			{
				for (int i=b.end+1; i<bstart; i++)
				{
					if (squares[i].IsUnknown())
					{
						assert (!squares[i].IsEmpty());
						squares[i].SetStatus(PuzzleSquare.SquareStatus.FILLED, guess_level);	
					}
				}
				// now bstart is b.start
			}
			
			// Update clue extents based on this merged blob
			cl.SetBlob(bstart, bend);
			b.start = bstart;
			b.end = bend;
			
			// Recalculate new uncertainty
			uncertainty = cl.value - b.GetLength();
			
			// Handle uncertainty of 0
			if (uncertainty == 0)
			{
				PutEmptyOnBothEnds (squares, bstart, bend, guess_level);
				return something_changed;
			}
		}

		// refine the processing if the uncertainty > 0
		if (uncertainty > 0)
		{

			// Now do some extent editing if this Blob cannot be connected to
			// an adjacent blob (because the connected blob would be too big)
			int iNextLeft = GetLocNextSquareStatusToLeft (squares, b.start);
			int iNextRight = GetLocNextSquareStatusToRight (squares, b.end);
			PuzzleSquare.SquareStatus nextToLeft = PuzzleSquare.SquareStatus.EMPTY;
			if (iNextLeft >= 0) nextToLeft = squares[iNextLeft].GetStatus();
			PuzzleSquare.SquareStatus nextToRight = PuzzleSquare.SquareStatus.EMPTY;
			if (iNextRight < N) nextToRight = squares[iNextRight].GetStatus();

			// check space between start of blob and this next square to the left
			// (The following computation will work even when iNextLeft is -1 (left
			// edge) or iNextRight is N (right edge)
			int left_space = b.start - iNextLeft - 1;
			int right_space = iNextRight - b.end - 1;

			// Process left edge, if next left square is EMPTY or
			// we don't have enough room to connect to the Blob to the
			// left
			if ((nextToLeft == PuzzleSquare.SquareStatus.EMPTY && left_space <= uncertainty) ||
				(nextToLeft == PuzzleSquare.SquareStatus.FILLED && left_space >= uncertainty) ||
				(b.start-cl.start_extent < uncertainty))
			{
				boolean is_filling = false;

				int start_cell = iNextLeft+1; // Assume next square to left is EMPTY
				if (start_cell < cl.start_extent) start_cell = cl.start_extent;
				if (nextToLeft == PuzzleSquare.SquareStatus.FILLED)
				{
					start_cell = b.start-uncertainty;
					if (start_cell < cl.start_extent) start_cell = cl.start_extent;
					if (iNextLeft+1 == start_cell) start_cell++;
					assert (start_cell >= 0);
				}

				int new_b_start = 0;
				int new_b_end = 0;

				int cell = start_cell;
				for (int i=0; i<cl.value; i++)
				{
					if (cell < 0 || cell >= N) break;
					if (!is_filling && squares[cell].IsFilled()) 
					{
						is_filling = true;
						new_b_start = cell;
						new_b_end = cell;
					}
					else if (is_filling)
					{
						if (squares[cell].IsUnknown())
						{
							assert (!squares[cell].IsEmpty());
							squares[cell].SetStatus (PuzzleSquare.SquareStatus.FILLED, guess_level);
							something_changed = true;
						}
						new_b_end = cell;
					}
					cell++;
				}
				cl.SetBlob (new_b_start, new_b_end);
				if (new_b_end - new_b_start + 1 == cl.value)
				{
					if (PutEmptyOnBothEnds (squares, new_b_start, new_b_end, guess_level))
						something_changed = true;
				}
				// Update start/end extents
				int idx = start_cell;
				if (cl.start_extent < idx) cl.start_extent = idx;
				idx = cell+uncertainty-1;
				if (cl.end_extent > idx) cl.end_extent = idx;
			}

			// Process right edge, if next left square is EMPTY or
			// we do not have enough space to connect to the Blob
			// to the right
			if ((nextToRight == PuzzleSquare.SquareStatus.EMPTY && right_space <= uncertainty) ||
				(nextToRight == PuzzleSquare.SquareStatus.FILLED && right_space >= uncertainty) ||
				(cl.end_extent - b.end < uncertainty))
			{
				boolean is_filling = false;

				int start_cell = iNextRight-1; // Assume next square to right is EMPTY
				if (start_cell > cl.end_extent) start_cell = cl.end_extent;
				if (nextToRight == PuzzleSquare.SquareStatus.FILLED)
				{
					start_cell = b.end+uncertainty;
					if (start_cell > cl.end_extent) start_cell = cl.end_extent;
					if (iNextRight-1 == start_cell) start_cell--;
					assert (start_cell < N);
				}

				int new_b_start = 0;
				int new_b_end = 0;										

				int cell = start_cell;
				for (int i=0; i<cl.value; i++)
				{
					if (cell < 0 || cell >= N) break;
					if (!is_filling && squares[cell].IsFilled()) 
					{
						is_filling = true;
						new_b_end = cell;
						new_b_start = cell;
					}
					else if (is_filling)
					{
						if (squares[cell].IsUnknown())
						{
							assert (!squares[cell].IsEmpty());
							squares[cell].SetStatus (PuzzleSquare.SquareStatus.FILLED, guess_level);
							something_changed = true;
						}
						new_b_start = cell;
					}
					cell--;
				}
				cl.SetBlob (new_b_start, new_b_end);
				if (new_b_end - new_b_start + 1 == cl.value)
					if (PutEmptyOnBothEnds (squares, new_b_start, new_b_end, guess_level))
						something_changed = true;
				// Update start/end extents
				int idx = start_cell;
				if (cl.end_extent > idx) cl.end_extent = idx;		
				idx = cell-uncertainty+1;										
				if (cl.start_extent < idx) cl.start_extent = idx;
			}

			// Process left edge if we can connect to the Blob to the left
			if (nextToLeft == PuzzleSquare.SquareStatus.FILLED &&
					left_space < uncertainty)
			{
				// Get end and start of the Blob to the left of us
				int bleft_end = iNextLeft;
				int bleft_start = bleft_end;
				for (int i=iNextLeft; i>= 0; i--)
				{
					if (!squares[i].IsFilled()) break;
					bleft_start = i;
				}

				// If we connect to the Blob to the left and our
				// total Blob length is > clue_val, then we cannot
				// connect to the Blob to the left  OR
				// if the total Blob length is > clue_val of the clue
				// to the left, then we cannot connect
				int combined_blob_len = b.end - bleft_start + 1;
				boolean must_be_separated = combined_blob_len > cl.value;
				if (must_be_separated)
				{
					boolean is_filling = false;

					int start_cell = iNextLeft+2; // Separate from Blob to the left

					int new_b_start = 0;
					int new_b_end = 0;																					

					int cell = start_cell;
					for (int i=0; i<cl.value; i++)
					{
						if (!is_filling && squares[cell].IsFilled()) 
						{
							is_filling = true;
							new_b_start = cell;
							new_b_end = cell;
						}
						else if (is_filling)
						{
							if (squares[cell].IsUnknown())
							{
								assert (!squares[cell].IsEmpty());
								squares[cell].SetStatus (PuzzleSquare.SquareStatus.FILLED, guess_level);
								something_changed = true;
							}
							new_b_end = cell;
						}
						cell++;
					}
					cl.SetBlob (new_b_start, new_b_end);
					if (new_b_end - new_b_start + 1 == cl.value)
						if (PutEmptyOnBothEnds (squares, new_b_start, new_b_end, guess_level))
							something_changed = true;
					// Update start/end extents
					int idx = start_cell;
					if (cl.start_extent < idx) cl.start_extent = idx;
					idx = cell+uncertainty-1;
					if (cl.end_extent > idx) cl.end_extent = idx;											
				}

			}

			// Process left edge if we can connect to the Blob to the left
			if (nextToRight == PuzzleSquare.SquareStatus.FILLED &&
					right_space < uncertainty)
			{
				// Get end and start of the Blob to the left of us
				int bright_start = iNextRight;
				int bright_end = bright_start;
				for (int i=iNextRight; i < N; i++)
				{
					if (!squares[i].IsFilled()) break;
					bright_end = i;
				}

				// If we connect to the Blob to the left and our
				// total Blob length is > clue_val, then we cannot
				// connect to the Blob to the left   OR
				// if the total Blob length is > clue_val of the 
				// clue to the right, then we cannot connect
				int combined_blob_len = bright_end - b.start + 1;
				boolean must_be_separated = combined_blob_len > cl.value;
				if (must_be_separated)
				{
					boolean is_filling = false;

					int new_b_start = 0;
					int new_b_end = 0;																																

					int start_cell = iNextRight-2; // Separate from Blob to the right

					int cell = start_cell;
					for (int i=0; i<cl.value; i++)
					{
						if (!is_filling && squares[cell].IsFilled()) 
						{
							is_filling = true;
							new_b_start = cell;
							new_b_end = cell;
						}
						else if (is_filling)
						{
							if (squares[cell].IsUnknown())
							{
								assert (!squares[cell].IsEmpty());
								squares[cell].SetStatus (PuzzleSquare.SquareStatus.FILLED, guess_level);
								something_changed = true;
							}
							new_b_start = cell;
						}
						cell--;
					}
					cl.SetBlob(new_b_start, new_b_end);
					if (new_b_end - new_b_start + 1 == cl.value)
						if (PutEmptyOnBothEnds (squares, new_b_start, new_b_end, guess_level))
							something_changed = true;
					// Update start/end extents
					int idx = start_cell;
					if (cl.end_extent > idx) cl.end_extent = idx;
					idx = cell-uncertainty+1;
					if (cl.start_extent < idx) cl.start_extent = idx;											
				}										
			}	
			
		// uncertainty is 0
		} else if (uncertainty == 0) {
			int cell = b.start-1;
			if (cell >= 0 && squares[cell].IsUnknown())
			{
				assert (!squares[cell].IsFilled());
				squares[cell].SetStatus(PuzzleSquare.SquareStatus.EMPTY, guess_level);
			}
			cell = b.end+1;
			if (cell < squares.length && squares[cell].IsUnknown())
			{
				assert (!squares[cell].IsFilled());
				squares[cell].SetStatus(PuzzleSquare.SquareStatus.EMPTY, guess_level);	
			}
		}
		
		// Check if extents have changed
		if (cl.start_extent != old_start_extent || cl.end_extent != old_end_extent)
			something_changed = true;
		
		return something_changed;
	}
	
	private boolean BlobCanConnectToPrevBlob (PuzzleSquare[] squares, Blob b, Clue2 cl)
	{
		boolean can_connect = true;
		if (b.start == 0) return false;
		
		// see if there is even a Blob to the left
		int bprev_start = -1;
		int bprev_end = -1;
		boolean blob_started = false;
		for (int i=b.start-1; i>=0; i--)
		{
			if (!blob_started && squares[i].IsEmpty()) return false;
			else if (squares[i].IsFilled())
			{
				if (!blob_started)
				{
					blob_started = true;
					bprev_end = i;
					bprev_start = i;
				} else
					bprev_start = i;
			} else if (blob_started && (squares[i].IsUnknown() || squares[i].IsEmpty()))
			{
				// blob is finished
				break;
			}
		}
		
		// maximum combined blob length
		int max_blob_len = b.end - bprev_start + 1;
		
		// if max_blob_len is big enough for the current clue or the
		// previous clue, then it *can* be combined
		Clue2 cl_prev = cl.prev;
		return (max_blob_len <= cl.value ||
				max_blob_len <= cl_prev.value);
	}
	
	private boolean BlobCanConnectToNextBlob (PuzzleSquare[] squares, Blob b, Clue2 cl)
	{
		boolean can_connect = true;
		if (b.start == 0) return false;
		
		// see if there is even a Blob to the left
		int bnext_start = -1;
		int bnext_end = -1;
		boolean blob_started = false;
		for (int i=b.end+1; i<squares.length; i++)
		{
			if (!blob_started && squares[i].IsEmpty()) return false;
			else if (squares[i].IsFilled())
			{
				if (!blob_started)
				{
					blob_started = true;
					bnext_end = i;
					bnext_start = i;
				} else
					bnext_end = i;
			} else if (blob_started && (squares[i].IsUnknown() || squares[i].IsEmpty()))
			{
				// blob is finished
				break;
			}
		}
		if (!blob_started) return false;
		
		// maximum combined blob length
		int max_blob_len = bnext_end - b.start + 1;
		
		// if max_blob_len is big enough for the current clue or the
		// previous clue, then it *can* be combined
		Clue2 cl_next = cl.next;
		return (max_blob_len <= cl.value ||
				max_blob_len <= cl_next.value);
	}	
	
	// Return index of next square to the left that does not match the Square Status
	private int GetLocNextSquareStatusToLeftNotSS (PuzzleSquare[] squares, int isq, PuzzleSquare.SquareStatus ss)
	{
		if (isq > 0)
		{
			for (int i=isq-1; i>=0; i--)
				if (squares[i].GetStatus() != ss) return i;			
		}
		return -1;
	}
	// Return index of next square to the left that does not match the Square Status
	private int GetLocNextSquareStatusToRightNotSS (PuzzleSquare[] squares, int isq, PuzzleSquare.SquareStatus ss)
	{
		if (isq < squares.length-1)
		{
			for (int i=isq+1; i<squares.length; i++)
				if (squares[i].GetStatus() != ss) return i;
		}
		return squares.length;
	}
	
	
	// Return index of next square to the left that does NOT contain an UNKNOWN
	private int GetLocNextSquareStatusToLeft (PuzzleSquare[] squares, int isq)
	{
		return GetLocNextSquareStatusToLeftNotSS (squares, isq, PuzzleSquare.SquareStatus.UNKNOWN);
	}
	// Return next square status to the left, ignoring any UNKNOWN squares
	private int GetLocNextSquareStatusToRight (PuzzleSquare[] squares, int isq)
	{
		return GetLocNextSquareStatusToRightNotSS (squares, isq, PuzzleSquare.SquareStatus.UNKNOWN);
	}
	
	private boolean PutEmptyOnBothEnds (PuzzleSquare[] squares, int blob_start, int blob_end, int guess_level)
	{
		boolean something_changed = false;
		int idx = blob_start-1;
		if (idx >= 0 && squares[idx].IsUnknown())
		{
			assert (!squares[idx].IsFilled());
			squares[idx].SetStatus (PuzzleSquare.SquareStatus.EMPTY, guess_level);
			something_changed = true;
		}
		idx = blob_end+1;
		if (idx < squares.length && squares[idx].IsUnknown())
		{
			assert (!squares[idx].IsFilled());
			something_changed = true;
			squares[idx].SetStatus (PuzzleSquare.SquareStatus.EMPTY, guess_level);
		}
		return something_changed;
	}
	
	// -----------------------------------
	// Utility functions to process a line
	// -----------------------------------
	
	public static String DumpSquaresWithClues (PuzzleSquare[] sqs, Clues2 clues2)
	{
		if (sqs == null) return "";
		String msg1 = DumpSquares (sqs);
		
		String msg2 = "";
		for (int i=0; i<sqs.length; i++)
		{
			int clue_idx = GetClueIndexForSquare (i, clues2);
			if (clue_idx < 0) msg2 += " ";
			else msg2 += clue_idx % 10;	// just the 10's digit 
			if ((i+1) % 5 == 0) msg2 += " ";
		}
		msg2 += "\n";

		return msg1 + msg2;
	}
	
	private static int GetClueIndexForSquare (int i, Clues2 clues2)
	{
		for (Clue2 cl : clues2.clue_list)
			if (i >= cl.start_blob && i<= cl.end_blob) return cl.index;
		return -2;
	}
	
	public static String DumpSquaresAndGuessLevels (PuzzleSquare[] sqs, int start_col)
	{
		if (sqs == null) return "";
		String msg1 = DumpSquares (sqs);

		String msg2 = "";
		for (int i=0; i<sqs.length; i++)
		{
			int guess_level = sqs[i].GetGuessLevel();
			msg2 += ""+guess_level;
			if (guess_level > 9) msg2 += " ";	// put a space between double-digit guess levels
			if ((i+1) % 5 == 0) msg2 += " ";
		}
		msg2 += "\n";
		
		String msg3 = "";
		if (start_col >= 0 && start_col < sqs.length)
		{
			for (int i=0; i<sqs.length; i++)
			{
				if (i==start_col) msg2 += "*";
				else msg2 += " ";
				if ((i+1) % 5 == 0) msg2 += " ";
			}
			msg2 += "\n";			
		}
		
		return msg1 + msg2 + msg3;		
		
	}
	public static String DumpSquares (PuzzleSquare[] sqs)
	{
		if (sqs == null) return "";
		String msg = "";
		for (int i=0; i<sqs.length; i++)
		{
			if (sqs[i].IsEmpty()) msg += "x";
			else if (sqs[i].IsFilled()) msg += "F";
			else msg += ".";
			if ((i+1) % 5 == 0) msg += " ";
		}
		msg += "\n";
		return msg;
	}
	public static String DumpClues (int[] clues)
	{
		if (clues == null) return "";
		String msg = "Clues:\n";
		for (int i=0; i<clues.length; i++)
			if (clues[i] > 0) msg += "" + i + ") " + clues[i] + "\n";
		return msg;
	}
	public static String DumpCluesOneLine (int[] clues)
	{
		if (clues == null) return "";
		String msg = "";
		for (int i=0; i<clues.length; i++)
			if (clues[i] > 0) msg += " " + clues[i];
		msg += "\n";
		return msg;
	}
	public static String DumpClues2 (Clues2 myClues)
	{
		if (myClues == null) return "";
		String msg = "";
		if (myClues.is_row)
			msg += "Row: " + myClues.col_row_num;
		else
			msg += "Column: " + myClues.col_row_num;
		msg += "\n";
		for (int i=0; i<myClues.clue_list.length; i++)
		{
			Clue2 clue = myClues.clue_list[i];
			msg += "" + clue.index + ") " + clue.value + " ";
			msg += "Extent: " + clue.start_extent + " " + clue.end_extent +
					" (" + (clue.end_extent-clue.start_extent+1) + ")";
			if (clue.has_blob)
				msg += " Blob: " + clue.start_blob + " " + clue.end_blob +
					" (" + (clue.end_blob - clue.start_blob + 1) + ")";
			msg += "\n";
		}
		return msg;				
	}
	public static String DumpClues (Clues myClues)
	{
		if (myClues == null) return "";
		String msg = "";
		if (myClues.is_row)
			msg += "Row: " + myClues.col_row_num;
		else
			msg += "Column: " + myClues.col_row_num;
		msg += "\n";
		for (int i=0; i<myClues.clue_list.length; i++)
		{
			Clue clue = myClues.clue_list[i];
			msg += "" + i + ") " + clue.value + " ";
			if (clue.is_fixed) 
				msg += "Fixed: " + clue.start_filled + " " + clue.end_filled +
						" (" + (clue.end_filled-clue.start_filled+1) + ")\n";
			else if (clue.is_anchored)
				msg += "Anchored: " + clue.start_filled + " " + clue.end_filled +
						" (" + (clue.end_filled-clue.start_filled+1) + ")\n";
			else
				msg += "Extent: " + clue.min_extent + " " + clue.max_extent +
						" (" + (clue.max_extent-clue.min_extent+1) + ")\n";
		}
		return msg;
	}
	
	public static String DumpBlobsWithClueList (ArrayList<Blob> blobs)
	{
		if (blobs == null) return "";
		else if (blobs.size() == 0) return "Blobs (* = AntiBlob): none\n";
		String msg = "Blobs:\n";
		for (int i=0; i<blobs.size(); i++)
		{
			Blob b = blobs.get(i);
			if (b instanceof AntiBlob) msg += "* ";
			msg += "" + i + ") " + b.toStringShortWithClueList();
			msg += "\n";
		}
		return msg;		
	}
	public static String DumpBlobs (ArrayList<Blob> blobs)
	{
		if (blobs == null) return "";
		else if (blobs.size() == 0) return "Blobs (* = AntiBlob): none\n";
		String msg = "Blobs:\n";
		for (int i=0; i<blobs.size(); i++)
		{
			Blob b = blobs.get(i);
			if (b instanceof AntiBlob) msg += "* ";
			msg += "" + i + ") " + b.toStringShort();
			msg += "\n";
		}
		return msg;
	}
	
	// Blobs are sequences of FFFFF and AntiBlobs are sequences of UNKNOWNS or ______
	private ArrayList<Blob> GetAllBlobsAndAntiBlobsInRange (PuzzleSquare[] squares, int start_square, int end_square)
	{
		ArrayList<Blob> blob_list = new ArrayList();
		boolean blob_open = false;
		boolean antiblob_open = false;

		int start = -1;
		int end = -1;
		for (int i=start_square; i<=end_square; i++)
		{
			// square is FILLED
			if (squares[i].GetStatus() == PuzzleSquare.SquareStatus.FILLED)
			{
				// First check if we have an open antiblob
				if (antiblob_open)
				{
					AntiBlob ab = new AntiBlob (start, end);
					blob_list.add(ab);
					antiblob_open = false;
				}
				// Now work on the blob
				if (!blob_open) 
				{
					start = i;
					blob_open = true;
				}
				end = i;
			} else if (squares[i].GetStatus() == PuzzleSquare.SquareStatus.UNKNOWN)
			{
				// First check if we have an open blob
				if (blob_open)
				{
					Blob b = new Blob (start, end);
					blob_list.add(b);
					blob_open = false;
				}
				// Now work on the antiblob
				if (!antiblob_open) 
				{
					start = i;
					antiblob_open = true;
				}
				end = i;				
			} else
			{
				// end any blobs or antiblobs we may have open
				if (blob_open)
				{
					Blob b = new Blob (start, end);
					blob_list.add(b);
					blob_open = false;
				}
				if (antiblob_open)
				{
					AntiBlob ab = new AntiBlob (start, end);
					blob_list.add(ab);
					antiblob_open = false;
				}
			}
		}
		// end any blobs or antiblobs we may have open
		if (blob_open)
		{
			Blob b = new Blob (start, end);
			blob_list.add(b);
			blob_open = false;
		}
		if (antiblob_open)
		{
			AntiBlob ab = new AntiBlob (start, end);
			blob_list.add(ab);
			antiblob_open = false;
		}
		
		// now go back and post-process all of the Blobs and AntiBlobs to see if they're
		// attached or not (i.e. XFFFFX or X_____X)
		for (Blob b : blob_list)
		{
			int is = b.start-1;
			if (is < 0 || squares[is].IsEmpty()) b.is_anchored_start = true;
			is = b.end+1;
			if (is >= squares.length || squares[is].IsEmpty()) b.is_anchored_end = true;
		}
		
		return blob_list;
	}
	private ArrayList<Blob> GetAllBlobs (PuzzleSquare[] squares, int start_square, int end_square)
	{
		ArrayList<Blob> blob_list = new ArrayList();
		
		boolean blob_open = false;
		boolean is_anchored_start = false;
		boolean is_anchored_end = false;
		boolean is_fixed_start = false;
		boolean is_fixed_end = false;
		boolean is_fixed_to_prev_blob = false;
		boolean is_fixed_to_next_blob = false;
		int start = -1;
		int end = -1;
		for (int i=start_square; i<=end_square; i++)
		{
			// square is FILLED
			if (squares[i].GetStatus() == PuzzleSquare.SquareStatus.FILLED)
			{
				if (!blob_open) 
				{
					start = i;
					if (i==0) {
						is_anchored_start = true;
						is_fixed_start = true;
						is_fixed_to_prev_blob = true;
					} else if (squares[i-1].IsEmpty())
					{
						is_anchored_start = true;
						is_fixed_start = true;
						for (int j=0; j<i; j++)
							if (squares[j].IsUnknown())
							{
								is_fixed_start = false;
								break;
							}
					}
				}
				blob_open = true;
				end = i;
				
			} else // square is EMPTY or UNKNOWN
			{
				if (blob_open)
				{
					if (squares[i].IsEmpty())
					{
						is_anchored_end = true;
						is_fixed_end = true;
						for (int j=i; j<squares.length; j++)
							if (squares[j].IsUnknown())
							{
								is_fixed_end = false;
								break;
							}
						is_fixed_to_prev_blob = true;
						if (start > 0)
						{
							for (int j=start-1; j>=0; j--)
							{
								if (squares[j].IsUnknown())
								{
									is_fixed_to_prev_blob = false;
									break;
								} else if (squares[j].IsFilled())
									break;
							}
						}
						is_fixed_to_next_blob = true;
						if (end < squares.length-1)
						{
							for (int j=end+1; j<squares.length; j++)
							{
								if (squares[j].IsUnknown())
								{
									is_fixed_to_next_blob = false;
									break;
								} else if (squares[j].IsFilled())
									break;
							}
						}												
					}
					Blob b = new Blob (start, end, is_anchored_start, is_anchored_end,
						is_fixed_start, is_fixed_end, is_fixed_to_prev_blob, is_fixed_to_next_blob);
					blob_list.add(b);	
					blob_open = false;
					is_anchored_start = false;
					is_anchored_end = false;
					is_fixed_start = false;
					is_anchored_end = false;
					is_fixed_to_prev_blob = false;
					is_fixed_to_next_blob = false;
				}
			}
		}
		if (blob_open)
		{
			is_anchored_end = true;
			is_fixed_end = true;
			is_fixed_to_prev_blob = true;
			if (start > 0)
			{
				for (int j=start-1; j>=0; j--)
				{
					if (squares[j].IsUnknown())
					{
						is_fixed_to_prev_blob = false;
						break;
					} else if (squares[j].IsFilled())
						break;
				}
			}
			is_fixed_to_next_blob = true;
			assert (end == end_square);
			if (end < squares.length-1)
			{
				for (int j=end+1; j<squares.length; j++)
				{
					if (squares[j].IsUnknown())
					{
						is_fixed_to_next_blob = false;
						break;
					} else if (squares[j].IsFilled())
						break;
				}
			}
			Blob b = new Blob (start, end, is_anchored_start, is_anchored_end,
				is_fixed_start, is_fixed_end, is_fixed_to_prev_blob, is_fixed_to_next_blob);
			blob_list.add(b);

		}
		
		return blob_list;
	}
	
    // ----------------------------------------------------------
    // Utility functions to transfer rows/cols to/from the puzzle
    // ----------------------------------------------------------

    public static PuzzleSquare[] CopyRowFromPuzzle (PBNPuzzle myPuzzle, int row)
    {
        PuzzleSquare[] newRow = new PuzzleSquare[myPuzzle.GetCols()];
        for (int i=0; i<myPuzzle.GetCols(); i++)
            newRow[i] = new PuzzleSquare(myPuzzle.GetPuzzleSquareAt (row, i));
        return newRow;
    }

    public static PuzzleSquare[] CopyColFromPuzzle (PBNPuzzle myPuzzle, int col)
    {
        PuzzleSquare[] newCol = new PuzzleSquare[myPuzzle.GetRows()];
        for (int i=0; i<myPuzzle.GetRows(); i++)
            newCol[i] = new PuzzleSquare(myPuzzle.GetPuzzleSquareAt (i, col));
        return newCol;
    }
	
    public static void CopyColToPuzzle (PBNPuzzle myPuzzle, PuzzleSquare[] myCol, int col)
    {
        if (myPuzzle == null || myCol == null) return;
        for (int i=0; i<myPuzzle.GetRows(); i++)
            myPuzzle.SetPuzzleRowCol(i, col, myCol[i]);
    }
	
    public static void CopyRowToPuzzle (PBNPuzzle myPuzzle, PuzzleSquare[] myRow, int row)
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
	
	// -----------------------------------------------------------------------------------
	// Functions for old PuzzleSolver to replace CanSolutionFitInRow|ColStartingFromClue()
	// CanSolutionFitInRow|Col()
	// -----------------------------------------------------------------------------------
	
	// This is the most pitiful thing I could think of to get the solution returned by Better_
	public PuzzleSquare[] PossibleSolutionThatFits (PBNPuzzle myPuzzle, boolean is_row, int row_or_col)
	{
		int N = is_row ? myPuzzle.GetCols() : myPuzzle.GetRows();
		PuzzleSquare[] temp_sqs = new PuzzleSquare[N];
		for (int i=0; i<N; i++)
		{
			if (is_row) temp_sqs[i] = new PuzzleSquare(myPuzzle.GetBackupPuzzleSquareAt(row_or_col, i));
			else        temp_sqs[i] = new PuzzleSquare(myPuzzle.GetBackupPuzzleSquareAt(i, row_or_col));
			temp_sqs[i].SetNotAGuess();
		}
		
		int debug_row = -1;
		int debug_row2 = -1;
		boolean do_debug = (is_row && (row_or_col == debug_row || row_or_col == debug_row2));
		
		// set the initial guess level
		int guess_level = 1;
		
		// set the current clue being processed
		int clue_idx = 0;
		int clue_val = 0;
				
		// helper vars
		int Nclues;
		if (is_row) Nclues = myPuzzle.GetRow_NClues(row_or_col);
		else        Nclues = myPuzzle.GetCol_NClues(row_or_col);	
		
		// set start of current extent to process
		int start_col = 0;	
		
		// keep processing until we have no options left
		boolean keep_processing = true;
		while (keep_processing)
		{
			try {
		
				// -----------------------------------------------
				// Set-up for processing clue_idx within cur_Range
				// -----------------------------------------------
				
				// Get all Ranges starting from start_col to end
				ArrayList<Range> ranges = GetAllRangesInExtent (temp_sqs, start_col, temp_sqs.length-1);
				if (ranges.isEmpty()) return temp_sqs;	// All is well!

				// For each Range, find the Blobs (if any) within each Range
				for (Range r: ranges)
					r.FindBlobsInRange (temp_sqs);

				// Set current Range
				int cur_range_idx = 0;
				Range cur_Range = ranges.get(cur_range_idx);	

				// Update clue value
				if (is_row) clue_val = myPuzzle.GetRow_Clues(row_or_col, clue_idx);
				else        clue_val = myPuzzle.GetCol_Clues(row_or_col, clue_idx);

				if (do_debug) {
					System.out.println ("Processing clue index " + clue_idx + 
							" (" + clue_val + ") from square " + start_col);
					System.out.println (DumpSquaresAndGuessLevels (temp_sqs, start_col));
					noop();
				}			

				// -------------------------------------------------------------
				// While we're just eliminating Ranges that are too small and are
				// empty, we can loop over processing the current clue
				// -------------------------------------------------------------
				boolean keep_skipping_empty_small_ranges = true;
				while (keep_skipping_empty_small_ranges)
				{
					int range_len = cur_Range.GetLength();

					// current range is too small for current clue so let's EMPTY it out
					if (range_len < clue_val)
					{

						// if the current Range has a Blob, then we have a problem
						if (!cur_Range.blobs.isEmpty())
							throw new CannotFitSolutionException ("Range is too small AND it has a Blob in it");

						// otherwise let's just EMPTY out the current Range
						for (int idx=cur_Range.start; idx<=cur_Range.end; idx++)
							temp_sqs[idx].SetStatus(PuzzleSquare.SquareStatus.EMPTY, guess_level);	

						if (do_debug) {
							System.out.println ("Range too small for clue index " + clue_idx + " (" + clue_val + ") so EMPTY it");
							System.out.println (DumpSquaresAndGuessLevels (temp_sqs, start_col));	
							noop();
						}

						// and skip to the next Range
						cur_range_idx++;

						// if we've run out of Ranges, then we have a problem
						if (cur_range_idx == ranges.size())
							throw new CannotFitSolutionException ("Eliminated a small Range, but no more left!");

						// otherwise just get the next Range and try and process the
						// current clue again
						cur_Range = ranges.get(cur_range_idx);

					// cur_Range is big enough that it *could* hold the next clue
					} else 
						keep_skipping_empty_small_ranges = false;

				} // end skipping Ranges that are too small
			
				// --------------------------------------------------------------
				// Done skipping Ranges that were too small, now process the ones
				// that are big enough to hold the current clue.
				// --------------------------------------------------------------
				int range_len = cur_Range.GetLength();				
				assert (range_len >= clue_val);

				// if we're at the last clue, then we *could* be done, depending
				// on Blobs in the current Range and all other Ranges to the right
				if (clue_idx == Nclues-1)
				{
					// count # of filled squares and the maximum extent of filled
					// squares
					int count = 0;
					int first_filled = -1;
					int last_filled = -1;
					for (int i=cur_Range.start; i<N; i++)
						if (temp_sqs[i].IsFilled()) 
						{
							count++;
							if (first_filled == -1) first_filled = i;
							last_filled = i;
						}
					
					// if # of filled squares is 0, then fill in clue right here
					if (count == 0)
					{
						int cell = cur_Range.start;
						for (int i=0; i<clue_val; i++)
							if (!temp_sqs[cell+i].IsFilled())
								temp_sqs[cell+i].SetStatus(PuzzleSquare.SquareStatus.FILLED, guess_level);
						if (cell+clue_val < N)
							if (!temp_sqs[cell+clue_val].IsEmpty())
								temp_sqs[cell+clue_val].SetStatus(PuzzleSquare.SquareStatus.EMPTY, guess_level);
						throw new WeAreGoodException ("Last clue fits in current Range with no other existing blobs!");					
					}
					
					// if # of filled squares exceeds the clue_val or if the max
					// extent of the filled squares is too big, then we have a problem
					if (count > clue_val || (last_filled-first_filled+1) > clue_val)
						throw new CannotFitSolutionException ("Last Range(s)' blobs do not work for last clue");
					
					// we might be good from here if we find the Range with the Blob and we
					// can fill in the clue from there
					Range temp_Range = null;
					int count_ranges_with_blobs = 0;
					for (int i=ranges.indexOf(cur_Range); i<ranges.size(); i++)
						if (ranges.get(i).blobs.size() > 0) 
						{
							temp_Range = ranges.get(i);
							count_ranges_with_blobs++;
						}
					assert (temp_Range != null);
					
					// given that this is the last clue, there should only be
					// one Range that contains a blob we can try and assign to
					// the Clue
					if (count_ranges_with_blobs > 1)
						throw new CannotFitSolutionException ("Last clue cannot be assigned to multiple Ranges with Blobs");
					
					// let's fill in our clue between the first/last_filled slots
					// and then add the difference to whichever end works
					for (int cell=first_filled; cell<=last_filled; cell++)
					{
						if (!temp_sqs[cell].IsFilled())
							temp_sqs[cell].SetStatus(PuzzleSquare.SquareStatus.FILLED, guess_level);
					}
					int extras_needed = clue_val - (last_filled - first_filled + 1);
					if (extras_needed > 0)
					{
						// let's try to the right of the last_filled
						int cell = last_filled+1;
						while (cell < N && extras_needed > 0)
						{
							if (temp_sqs[cell].IsUnknown())
							{
								temp_sqs[cell].SetStatus(PuzzleSquare.SquareStatus.FILLED, guess_level);
								extras_needed--;
								cell++;
							} else if (temp_sqs[cell].IsEmpty())
								break;
						}
						// now go to the left of the first_filled
						cell = first_filled-1;
						while (cell >= 0 && extras_needed > 0)
							if (temp_sqs[cell].IsUnknown())
							{
								temp_sqs[cell].SetStatus(PuzzleSquare.SquareStatus.FILLED, guess_level);
								extras_needed--;
								cell--;
							} else if (temp_sqs[cell].IsEmpty())
								break;
						// if we've succeeded, we're good!
						if (extras_needed == 0)
							throw new WeAreGoodException ("Last Range(s) are good enough for last clue!");
						else
							throw new CannotFitSolutionException ("Unable to fit in our last clue!");							
					} else
						throw new WeAreGoodException ("Last clue fit in perfectly!");						
				}

				// let's see if there is room for the *next* clue if we
				// take into account the space we need for *this* clue within
				// the current Range
				if (cur_Range.blobs.size() > 0)
				{

					// let's see if the first Blob is close enough to the
					// start of the Range such that the Blob *must* be part of
					// the current clue
					Blob b = cur_Range.blobs.get(0);
					int uncertainty = b.start - cur_Range.start;

					// If Blob is bound to the current clue, but is larger
					// than the current clue value, then we have a problem!
					if (uncertainty < clue_val && b.GetLength() > clue_val)
						throw new CannotFitSolutionException ("Clue " + clue_idx + " is close enough to start of Blob, but Blob size is too big");

					// Blob can be attached to current clue
					if (uncertainty < clue_val)
					{
						// Fill in the squares with FILLED at this current guess_level
						int count = 0;
						int cell = cur_Range.start;
						for (int idx=0; idx<clue_val; idx++)
						{
							assert (!temp_sqs[cell].IsEmpty());
							if (!temp_sqs[cell].IsFilled())
							{
								temp_sqs[cell].SetStatus(PuzzleSquare.SquareStatus.FILLED, guess_level, clue_idx);
								count++;
							}
							cell++;
						}
						// Fill in one past with EMPTY at the current guess_level
						if (cell < N)
						{
							if (temp_sqs[cell].IsFilled())
								throw new CannotFitSolutionException ("End of clue " + clue_idx + " cannot be set to EMPTY");
							
							if (!temp_sqs[cell].IsEmpty())
							{
								temp_sqs[cell].SetStatus(PuzzleSquare.SquareStatus.EMPTY, guess_level, clue_idx);
								count++;
							}
						}											

						// increment guess_level for the next clue (unless the clue
						// was already set in the puzzle, i.e. we didn't have to fill
						// in any new squares)
						if (count > 0) guess_level++;

						if (do_debug) {
							System.out.println ("Blob (" + b.start + " " + b.end + ") is attached to clue index " + clue_idx +
									" (" + clue_val + ") guess_level is " + guess_level);
							System.out.println (DumpSquaresAndGuessLevels (temp_sqs, start_col));	
							noop();
						}

						// Move on to the next clue
						clue_idx++;

						// Move to next start position
						start_col = cell+1;
						
						// If we're on the last clue AND we have no more FILLED squares,
						// then we're done
						if (clue_idx >= Nclues)
						{
							if (CountFilledSquaresToRightFromIdx (temp_sqs, start_col) == 0)
								throw new WeAreGoodException ("There are no more clues and no more filled squares to the right");
							else
								throw new CannotFitSolutionException ("There are no more clues but there are filled squares to the right");
						}
											
						// If we're at the end of the squares, then that's a problem, too
						if (start_col >= N)
							throw new CannotFitSolutionException ("We have more clues to process, but we've run out of squares");						
							
						// If start of next valid range is beyond the end
						// of the current Range, then move on to the next Range
						if (start_col > cur_Range.end)
						{
							cur_range_idx++;

							// If we don't have any more Ranges, then we have a problem
							if (cur_range_idx == ranges.size())
								throw new CannotFitSolutionException ("We have more clues, but no more Ranges to process");

							cur_Range = ranges.get(cur_range_idx);
							start_col = cur_Range.start;
						}

					// no Blobs close enough to start of Range, so we assume that our current
					// clue might be within this Range and then process the
					// remaining clues in the remaining space
					} else
					{	
						// let's fill in our clue here
						int cell = cur_Range.start;
						for (int idx=0; idx<clue_val; idx++)
						{
							assert (!temp_sqs[cell].IsEmpty());
							if (!temp_sqs[cell].IsFilled())
								temp_sqs[cell].SetStatus(PuzzleSquare.SquareStatus.FILLED, guess_level, clue_idx);
							cell++;
						}
						// and fill in an X at the end of the clue
						if (cell < N)
						{
							if (temp_sqs[cell].IsFilled())
								throw new CannotFitSolutionException ("Cannot EMPTY end of clue");
							if (!temp_sqs[cell].IsEmpty())
								temp_sqs[cell].SetStatus(PuzzleSquare.SquareStatus.EMPTY, guess_level);
						}
						if (do_debug) {
							System.out.println ("Fitting clue index " + clue_idx + " (" + clue_val + ") into available space");				
							System.out.println (DumpSquaresAndGuessLevels (temp_sqs, start_col));
							noop();
						}

						// Move on to the next clue and guess_level
						clue_idx++;
						guess_level++;

						// Move to next start position
						start_col = cell+1;
						
						// If we're on the last clue AND we have no more FILLED squares,
						// then we're done
						if (clue_idx >= Nclues)
						{
							if (CountFilledSquaresToRightFromIdx (temp_sqs, start_col) == 0)
								throw new WeAreGoodException ("There are no more clues and no more filled squares to the right");
							else
								throw new CannotFitSolutionException ("There are no more clues but there are filled squares to the right");
						}
					
						// If we're at the end of the squares, then that's a problem, too
						if (start_col >= N)
							throw new CannotFitSolutionException ("We have more clues to process, but we've run out of squares");						
							
						// If start of next valid range is beyond the end
						// of the current Range, then move on to the next Range
						if (start_col > cur_Range.end)
						{
							cur_range_idx++;

							// If we don't have any more Ranges, then we have a problem
							if (cur_range_idx == ranges.size())
								throw new CannotFitSolutionException ("We have more clues, but no more Ranges to process");

							cur_Range = ranges.get(cur_range_idx);
							start_col = cur_Range.start;
						}						
					}

				// no Blobs in current Range, so we assume that our current
				// clue might be within this Range and then process the
				// remaining clues in the remaining space
				} else
				{
					// let's fill in our clue here
					int cell = cur_Range.start;
					for (int idx=0; idx<clue_val; idx++)
					{
						assert (!temp_sqs[cell].IsEmpty());
						if (!temp_sqs[cell].IsFilled())
							temp_sqs[cell].SetStatus(PuzzleSquare.SquareStatus.FILLED, guess_level, clue_idx);
						cell++;
					}
					if (cell < N)
					{
						if (temp_sqs[cell].IsFilled())
							throw new CannotFitSolutionException ("Tried to put our clue in any empty Range, but end of clue is FILLED!");
						if (!temp_sqs[cell].IsEmpty())
							temp_sqs[cell].SetStatus(PuzzleSquare.SquareStatus.EMPTY, guess_level);
					}
					if (do_debug) {
						System.out.println ("Fitting clue index " + clue_idx + " (" + clue_val + ") into available space");								
						System.out.println (DumpSquaresAndGuessLevels (temp_sqs, start_col));
						noop();
					}

					// Move on to the next clue and guess_level
					clue_idx++;
					guess_level++;

					// Move to next start position
					start_col = cell+1;

					// If we're on the last clue AND we have no more FILLED squares,
					// then we're done
					if (clue_idx >= Nclues)
					{
						if (CountFilledSquaresToRightFromIdx (temp_sqs, start_col) == 0)
							throw new WeAreGoodException ("There are no more clues and no more filled squares to the right");
						else
							throw new CannotFitSolutionException ("There are no more clues but there are filled squares to the right");
					}
					
					// If we're at the end of the squares, then that's a problem, too
					if (start_col >= N)
						throw new CannotFitSolutionException ("We have more clues to process, but we've run out of squares");

					// If start of next valid range is beyond the end
					// of the current Range, then move on to the next Range
					if (start_col > cur_Range.end)
					{
						cur_range_idx++;

						// If we don't have any more Ranges, then we have a problem
						if (cur_range_idx == ranges.size())
							throw new CannotFitSolutionException ("We have more clues, but no more Ranges to process");

						cur_Range = ranges.get(cur_range_idx);
						start_col = cur_Range.start;
					}	
				}
			} // try
		
			// ----------------------------
			// Solution works - we're done!
			// ----------------------------
			catch (WeAreGoodException cfse) {
				if (do_debug)
					System.out.println (cfse.getMessage());
				return temp_sqs;
			} // catch WeAreGoodException (i.e. we're done)
			
			// -------------------------
			// Solution does not fit yet
			// -------------------------
			catch (CannotFitSolutionException cfse) {
				if (do_debug)
				{
					System.out.println ("Guess level: " + guess_level + "\nSolution doesn't fit because:\n" + cfse.getMessage());
					noop();
				}

				// if we're at guess_level 1 and it didn't fit, then we're hosed
				int keep_cell = -1;
				int keep_clue_idx = -1;
				if (guess_level > 0) {
					// locate first *isolated* FILLED square at the current guess_level
					while (keep_cell < 0 && guess_level > 0) {
						for (int idx=0; idx<temp_sqs.length; idx++)
							if (temp_sqs[idx].IsFilled() && temp_sqs[idx].GetGuessLevel() == guess_level)
							{
								// do not break up Blobs that are connected
								int prior_idx = idx-1;
								if (prior_idx < 0 || !temp_sqs[prior_idx].IsFilled())
								{
									keep_cell = idx;
									keep_clue_idx = temp_sqs[idx].clue_index;
									break;
								}
							}
						if (keep_cell < 0) guess_level--;
					}
				}

				// If we couldn't find an appropriate cell to EMPTY, then just
				// start with the UNKNOWN squares at guess level 0
				if (keep_cell < 0)
				{
					// locate first UNKNOWN square
					for (int idx=0; idx<temp_sqs.length; idx++)
						if (temp_sqs[idx].IsUnknown() && temp_sqs[idx].GetGuessLevel() == 0)
						{
							keep_cell = idx;
							keep_clue_idx = 0;
							break;
						}	
					guess_level = 1;
				}

				if (keep_cell < 0) return null;	// There is no more guess procesing to be done

				// undo all of the squares that are at the current guess_level and above
				// (as long as guess level is > 0)
				for (int idx=keep_cell; idx<temp_sqs.length; idx++)
				{
					int gs = temp_sqs[idx].GetGuessLevel();
					if (gs >= guess_level && gs > 0)
						temp_sqs[idx].SetStatus(PuzzleSquare.SquareStatus.UNKNOWN, 0);
				}
				if (do_debug) {
					System.out.println ("Undoing guess level >= " + guess_level);
					System.out.println (DumpSquaresAndGuessLevels (temp_sqs, start_col));
					noop();
				}

				// reduce our guess_level
				guess_level--;
				assert (guess_level >= 0);
				assert (temp_sqs[keep_cell].IsUnknown());
				temp_sqs[keep_cell].SetStatus(PuzzleSquare.SquareStatus.EMPTY, guess_level);
				if (do_debug) {
					System.out.println ("Setting " + keep_cell + " to EMPTY at guess_level " + (guess_level < 0 ? 0 : guess_level));
					System.out.println (DumpSquaresAndGuessLevels (temp_sqs, start_col));
					noop();
				}

				// now go back to our previous guess_level
				guess_level++;

				// reset the current clue_index to that which was associated
				// with the blob we just removed
				clue_idx = keep_clue_idx;

				// also restart where we process our line from
				start_col = keep_cell+1;

				// if we're at the end of the line, we have a problem
				if (start_col >= N) return null;	
				
			} // catch CanFitSolutionException		
		}

		return null;		
	}
	
	public boolean CanSolutionFit (PBNPuzzle myPuzzle, boolean is_row, int row_or_col) 
	{
		PuzzleSquare[] possible_sqs = PossibleSolutionThatFits (myPuzzle, is_row, row_or_col);
		return (possible_sqs != null);
	}
	
	private ArrayList<Range> GetAllRangesInExtent (PuzzleSquare[] squares, int extent_start, int extent_end)
	{
		ArrayList<Range> range_list = new ArrayList();
		assert (extent_start >= 0 && extent_end < squares.length);
		
		int start = extent_start;
		int end   = extent_end;
		boolean range_open = false;
		for (int i=extent_start; i<=extent_end; i++)
		{
			// square is UNKNOWN or FILLED (so part of a Range)
			if (squares[i].IsUnknown() || squares[i].IsFilled())
			{
				if (!range_open) start = i;
				range_open = true;
				end = i;
				
			} else // square is EMPTY (no longer in a Range)
			{
				if (range_open) 
					range_list.add(new Range (start, end));
				range_open = false;
			}
		}
		if (range_open)
			range_list.add(new Range (start, end));			
		
		return range_list;
	}
	
	public boolean CanSolutionFitStartingFromClue (PBNPuzzle myPuzzle, 
			PuzzleSquare[] squares, boolean is_row, int row_or_col, int start_col, int start_clue, int recursion_level)
	{
		if (is_row && row_or_col == 17)
			System.out.println ("Recurse level " + recursion_level);
		
		// Get all Ranges starting from start_col to end
		ArrayList<Range> ranges = GetAllRangesInExtent (squares, start_col, squares.length-1);
		if (ranges.isEmpty()) return false;
		
		// For each Range, find the Blobs (if any) within each Range
		for (Range r: ranges)
			r.FindBlobsInRange (squares);
		
		// Set current Range
		int cur_range_idx = 0;
		Range cur_Range = ranges.get(cur_range_idx);
		
		// Keep track of the last Range in which we had some leeway to add a
		// clue in the UNKNOWN spaces towards the beginning of the Range
		Range last_leeway_Range = null;
		
		// helper vars
		int Nclues;
		if (is_row)
			Nclues = myPuzzle.GetRow_NClues(row_or_col);
		else
			Nclues = myPuzzle.GetCol_NClues(row_or_col);
		
		// Loop over all clues from start_clue onwards
		// The actual "looping" over clues happens recursively
//		for (int clue_idx = start_clue; clue_idx < Nclues; clue_idx++)
//		{
			int clue_idx = start_clue;
			int clue_val;
			if (is_row)
				clue_val = myPuzzle.GetRow_Clues(row_or_col, clue_idx);
			else
				clue_val = myPuzzle.GetCol_Clues(row_or_col, clue_idx);
			int cur_col = start_col;
			
			boolean it_fits = false;
			
			boolean keep_processing_current_clue = true;
			while (keep_processing_current_clue)
			{
				int range_len = cur_Range.GetLength();

				// current range is too small for current clue
				if (range_len < clue_val)
				{
					// if the current Range has a Blob, then we have a problem
					if (!cur_Range.blobs.isEmpty()) return false;

					// otherwise we can just skip this empty Range
					cur_range_idx++;
					
					// if we've run out of Ranges, then we have a problem
					if (cur_range_idx == ranges.size()) return false;
					
					// otherwise just get the next Range and try and process the
					// current clue again
					cur_Range = ranges.get(cur_range_idx);

				// current clue *could* fit in current range
				} else
				{
					// if we're at the last clue, then we're done!
					if (clue_idx == Nclues-1) return true;
					
					// let's see if there is room for the *next* clue if we
					// take into account the space we need for *this* clue within
					// the current Range
					if (cur_Range.blobs.size() > 0)
					{
						
						// let's see if the first Blob is close enough to the
						// start of the Range such that the Blob *must* be part of
						// the current clue
						Blob b = cur_Range.blobs.get(0);
						int uncertainty = b.start - cur_Range.start;
						
						// If Blob is bound to the current clue, but is larger
						// than the current clue value, then we have a problem!
						if (uncertainty < clue_val && b.GetLength() > clue_val) return false;
						
						// Blob is bound to current clue
						if (uncertainty < clue_val)
						{
							// So we can move on to the next clue
							int next_clue_idx = clue_idx + 1;
							assert (next_clue_idx < Nclues);
							
							// We can also move on to processing a smaller
							// extent to the right which doesn't include the
							// blob associated with the clue_idx
//							int next_cur_col = b.end+2;
							int next_cur_col = cur_Range.start+clue_val+1;
							
							// If start of next valid range is beyond the end
							// of the current Range, then move on to the next Range
							if (next_cur_col > cur_Range.end)
							{
								cur_range_idx++;
								
								// If we don't have any more Ranges, then we have a problem
								if (cur_range_idx == ranges.size()) return false;
								
								cur_Range = ranges.get(cur_range_idx);
								next_cur_col = cur_Range.start;
							}
							
							keep_processing_current_clue = false;
							it_fits = CanSolutionFitStartingFromClue (myPuzzle, squares, is_row, row_or_col, next_cur_col, next_clue_idx, recursion_level+1);
							if (it_fits) return true;

							
						// no Blobs close enough to start of Range, so we assume that our current
						// clue might be within this Range and then process the
						// remaining clues in the remaining space
						} else
						{							
							// THIS APPROACH IS WAY TOO SLOW DUE TO MUCH RECURSION REQUIRED
							// LYNNE!!!!
							int next_cur_col = start_col + clue_val + 2;
							
							// If start of next valid range is beyond the end
							// of the current Range, then move on to the next Range
							if (next_cur_col > cur_Range.end)
							{
								cur_range_idx++;
								
								// If we don't have any more Ranges, then we have a problem
								if (cur_range_idx == ranges.size()) return false;
								
								cur_Range = ranges.get(cur_range_idx);
								next_cur_col = cur_Range.start;
							} else
								last_leeway_Range = cur_Range;	// There was enough empty space at
																// beginning of this Range for the curent clue
							
							// Move to next clue
							int next_clue_idx = clue_idx + 1;
							assert (next_clue_idx < Nclues);
							
							keep_processing_current_clue = false;							
							it_fits = CanSolutionFitStartingFromClue (myPuzzle, squares, is_row, row_or_col, next_cur_col, next_clue_idx, recursion_level+1);							
							if (it_fits) return true;
						}
						
					// no Blobs in current Range, so we assume that our current
					// clue might be within this Range and then process the
					// remaining clues in the remaining space
					} else
					{
						last_leeway_Range = cur_Range;	// no Blobs in this Range
						
						int next_cur_col = start_col + clue_val + 2;

						// If start of next valid range is beyond the end
						// of the current Range, then move on to the next Range
						if (next_cur_col > cur_Range.end)
						{
							cur_range_idx++;

							// If we don't have any more Ranges, then we have a problem
							if (cur_range_idx == ranges.size()) return false;

							cur_Range = ranges.get(cur_range_idx);
							next_cur_col = cur_Range.start;
						}

						// Move to next clue
						int next_clue_idx = clue_idx + 1;
						assert (next_clue_idx < Nclues);

						keep_processing_current_clue = false;
						it_fits = CanSolutionFitStartingFromClue (myPuzzle, squares, is_row, row_or_col, next_cur_col, next_clue_idx, recursion_level+1);													
						if (it_fits) return true;
					}
				}
			}
				
			// If it doesn't fit yet, let's see if we have some leeway so
			// we can make our search range starter a little smaller
			while (!it_fits)
			{					
				if (last_leeway_Range != null)
				{
					cur_col = last_leeway_Range.start + 1;
					if (cur_col == squares.length) return false;
					if (cur_col > last_leeway_Range.end)
					{
						cur_range_idx = ranges.indexOf(last_leeway_Range) + 1;
						if (cur_range_idx == ranges.size()) return false;
						cur_Range = ranges.get(cur_range_idx);
						cur_col = cur_Range.start;

						last_leeway_Range = null;
						if (cur_Range.blobs.isEmpty()) last_leeway_Range = cur_Range;
						else
						{
							Blob b = cur_Range.blobs.get(0);
							if (b.start - cur_Range.start > clue_val + 1)
								last_leeway_Range = cur_Range;
						}
					}

					it_fits = CanSolutionFitStartingFromClue (myPuzzle, squares, is_row, row_or_col, cur_col, clue_idx, recursion_level+1);													
					if (it_fits) return true;	
				} else return false;			
			}
//		}
		return false;
	}
	
	private static int CountFilledSquaresToRightFromIdx (PuzzleSquare[] sqs, int start_col)
	{
		if (start_col >= sqs.length) return 0;
		int count = 0;
		for (int i=start_col; i<sqs.length; i++)
			if (sqs[i].IsFilled()) count++;
		return count;
	}
	
	// No-operation!  Just gives me something I can stop the debugger at
	private static void noop ()
	{ }

}