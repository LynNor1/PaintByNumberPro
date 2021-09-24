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

	private static String last_message = "";
	
	// -------------------------------------------
	// Private classes we need to solve the puzzle
	// -------------------------------------------

	// Contiguous squares of UNKNOWN or FILLED
	public class Range 
	{
		private int start;
		private int end;
		private ArrayList<Integer> blob_indices;
		private LinkedList<Integer> clue_indices;
		
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
	
	// Contiguous squares of FILLED
	public class Blob
	{
		private int start;
		private int end;
		private boolean is_anchored_start;	// EMPTY (X) at start edge of Blob
		private boolean is_anchored_end;	// EMPTY (X) at end edge of Blob
		private boolean is_fixed_start;		// no UNKNOWN spaces from start of row/col to start of Blob
		private boolean is_fixed_end;		// no UNKNOWN spaces from end of Blob to end of row/col
		private boolean is_fixed_to_blob_prev; // no UNKNOWN spaces from start of Blob to prev Blob (if any)
		private boolean is_fixed_to_blob_next;   // no UNKNOWN spaces from end of Blob to next Blob (if any);
		private LinkedList<Integer> clue_indices;
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
	
	// ------------------------------------------------
	// Primary puzzle solving "loop":
	// Process all rows first, then process all columns
	// ------------------------------------------------
	public boolean ProcessPuzzle (PBNPuzzle myPuzzle, int guess_level)
	{
		int rows = myPuzzle.GetRows();
		int cols = myPuzzle.GetCols();
		
		// ----------------------------
		// Loop over all rows to bottom
		// ----------------------------
		for (int row=0; row<rows; row++)
		{
			
			// Gather up the clues
			int[] clues = PuzzleSolver.GetCluesForRowFromPuzzle(myPuzzle, row);
			
			// Gather up the status of the puzzle for this row
			PuzzleSquare squares[] = BetterPuzzleSolver.CopyRowFromPuzzle(myPuzzle, row);
			
			// Process this row
			PuzzleSquare new_squares[] = ProcessLine (clues, squares, guess_level, true, row);
			
			// See if something has changed
			boolean something_changed = ComparePuzzleSquares (new_squares, squares);
			
			// Update the puzzle with any new changes
			if (something_changed) BetterPuzzleSolver.CopyRowToPuzzle (myPuzzle, new_squares, row);
		}
		
		// -----------------------------------
		// Loop over all columns left to right
		// -----------------------------------
		for (int col=0; col<cols; col++)
		{
			
			// Gather up the clues
			int [] clues = PuzzleSolver.GetCluesForColFromPuzzle(myPuzzle, col);
			
			// Gather up the status of the puzzle for this row
			PuzzleSquare squares[] = BetterPuzzleSolver.CopyColFromPuzzle(myPuzzle, col);
			
			// Process this column
			PuzzleSquare new_squares[] = ProcessLine (clues, squares, guess_level, false, col);
			
			// See if something has changed
			boolean something_changed = ComparePuzzleSquares (new_squares, squares);			
			
			// Update the puzzle with any new changes
			if (something_changed) BetterPuzzleSolver.CopyColToPuzzle (myPuzzle, squares, col);
		}
		
		return true;
	}
	
	// ------------------------------------------------------------------------
	// Process one set of clues and PuzzleSquares (can be either column or row)
	// ------------------------------------------------------------------------
	
	private PuzzleSquare[] ProcessLine (int[] clues, PuzzleSquare[] squares, int guess_level,
			boolean is_row, int num)
	{	
		PuzzleSquare[] new_squares;
		PuzzleSquare[] orig_squares = new PuzzleSquare[squares.length];
		for (int i=0; i<orig_squares.length; i++) orig_squares[i] = new PuzzleSquare(squares[i]);
		
		try
		{
		
			// ---- Calculate initial ranges for all clues (assumes nothing about status of puzzle) ----
			last_message = "Calling InitializeClues() for " + (is_row ? " row " : " col ") + num + "\n";
			Clues myClues = InitializeClues (clues, squares.length, is_row, num);

			// Get all "Ranges"
			// -- A Range is a contiguous set of UNKNOWN or FILLED squares
			last_message = "Calling GetAllRanges() for " + (is_row ? " row " : " col ") + num + "\n";			
			ArrayList<Range> ranges = GetAllRanges (squares);

			// If there are no ranges, then all squares are filled in
			last_message = "No UNKNOWN squares in " + (is_row ? " row " : " col ") + num + "\n";
			if (ranges.isEmpty()) return squares;

			// Get all "Blobs"
			// -- A Blob is a contiguous set of FILLED squares
			last_message = "Calling GetAllBlobs() for " + (is_row ? " row " : " col ") + num + "\n";			
			ArrayList<Blob> blobs = GetAllBlobs (squares);
			if (!blobs.isEmpty())
			{

				// Update Ranges with Blobs that they contain
				last_message = "Finding Blobs in Ranges for " + (is_row ? " row " : " col ") + num + "\n";			
				for (Range r: ranges) r.GetBlobsInRange(blobs);

				// Associate Clues with Ranges (without consideration of clue order or 
				// anything else) (Ranges end up with list of clue_indices)
				boolean is_different = true;
				while (is_different)
				{
					last_message = "Associating Clues with Ranges for " + (is_row ? " row " : " col ") + num + "\n";
					new_squares = AssociateCluesWithRanges (clues, blobs, squares, ranges, guess_level);
					is_different = this.ComparePuzzleSquares(squares, new_squares);
					squares = new_squares;
					if (is_different)
					{
						ranges = GetAllRanges (squares);
						blobs = GetAllBlobs (squares);
						for (Range r: ranges) r.GetBlobsInRange(blobs);					
					}
				}
				
				// Clean up Clue associations with Ranges
				last_message = "Clean Up Clue Associations for " + (is_row ? " row " : " col ") + num + "\n";
				CleanUpClueAssociationsWithRangesAndLinkedBlobs (ranges, myClues, blobs, squares);

				// Process all Ranges that have only one possible Clue
				last_message = "Process Ranges with 1 or 0 Possible Clues for " + (is_row ? " row " : " col ") + num + "\n";				
				new_squares = ProcessRangesWithOneOrZeroPossibleClues (ranges, myClues, squares, guess_level);
				squares = new_squares;

				// Process all Ranges that have multiple clues by processing for the smallest
				// clue value
				new_squares = ProcessRangesForMinClueValue (ranges, clues, blobs, squares, guess_level);
				squares = new_squares;	
			}

			// Fancy new processing of Clues based on the state of the squares and 
			// whether or not Clues have been anchored already or fixed
			new_squares = ProcessSquaresFromClues (myClues, squares, guess_level);
			squares = new_squares;
		}
		catch (PuzzleSolverException pse)
		{
			System.out.println (pse.getMessage());
			squares = orig_squares;
		}

		return squares;
	}
	
	// Return true if something has changed, false if the same
	private boolean ComparePuzzleSquares (PuzzleSquare[] sqs1, PuzzleSquare[] sqs2)
	{
		assert (sqs1 != null && sqs2 != null && sqs1.length == sqs2.length);
		
		for (int i=0; i<sqs1.length; i++)
			if (sqs1[i].GetStatus() != sqs2[i].GetStatus()) return true;
		
		return false;
	}
	
	// Return true if something has changed, false if the same
	private int CountPuzzleSquareDifferences (PuzzleSquare[] sqs1, PuzzleSquare[] sqs2)
	{
		assert (sqs1 != null && sqs2 != null && sqs1.length == sqs2.length);
		int differences = 0;
		
		for (int i=0; i<sqs1.length; i++)
			if (sqs1[i].GetStatus() != sqs2[i].GetStatus())
				differences++;
		
		return differences;
	}	
	
	// -----------------------------------
	// Utility functions to process a line
	// -----------------------------------
	
	private Clues InitializeClues (int[] clues, int num_squares, boolean is_row, int num_row_col)
			throws PuzzleSolverException
	{
		Clues myClues = new Clues(num_squares, is_row, num_row_col);
		myClues.clue_list = new Clue[clues.length];
		
		for (int clue_idx=0; clue_idx<clues.length; clue_idx++)
		{
			int nom_start = 0;
			if (clue_idx > 0)
			{
				for (int earlier_clue_idx=0; earlier_clue_idx<clue_idx; earlier_clue_idx++)
					nom_start += clues[earlier_clue_idx] + 1;
			}
			int nom_end = num_squares-1;
			if (clue_idx < (clues.length-1))
			{
				for (int later_clue_idx=clue_idx+1; later_clue_idx<clues.length; later_clue_idx++)
					nom_end -= clues[later_clue_idx] + 1;				
			}
			myClues.clue_list[clue_idx] = new Clue (clue_idx, clues[clue_idx]);
			myClues.clue_list[clue_idx].min_extent = nom_start;
			myClues.clue_list[clue_idx].max_extent = nom_end;
			
			if (nom_end < nom_start)
				throw new PuzzleSolverException ("Error in Clues initializer for clue " + clue_idx,
					myClues, null);
			
//			last_message += "  Clue initialized for index " + clue_idx + 
//					" value " + clues[clue_idx] + " extent (" + nom_start + " " + nom_end + ")\n";
		}
		return myClues;
	}
	
	private PuzzleSquare[] ProcessSquaresFromClues (Clues myClues, PuzzleSquare[] old_squares, int guess_level)
			throws PuzzleSolverException
	{
		PuzzleSquare[] squares = new PuzzleSquare[old_squares.length];
		for (int i=0; i<old_squares.length; i++)
			squares[i] = new PuzzleSquare(old_squares[i]);
		
		// We should already have nominal extents set up from InitializeClues
		
		// We could also have some clues that have fixed locations (search
		// for "clue_is_located" in other methods)
		
		// Let's further update the clue_is_located info by sweeping the squares
		// from left to right and right to left, in order to pick up any fixed
		// clues that weren't already picked up.  This let's us also possibly
		// anchor clues
		// I WANT TO WORK THIS IN EVENTUALLY!!! LYNNE!!!!!
		/*
		int which_clue = 0;
		boolean keep_processing = true;
		int isquare = 0;
		boolean tracking_blob = false;	// Set it to true while we're tracking a Blob
		int blob_start = 0, blob_end = 0;	// When we're done with Blob, assign it to
									// which_clue
		boolean checking_uncertainty = false;
		int count_uncertainty = 0;
		while (keep_processing)
		{
			// If puzzle square is UNKNOWN and we're not checking_uncertainty
			// then we're done
			if (squares[isquare].IsUnknown())
			{
				// We're already counting UNKNOWNs
				if (checking_uncertainty)
				{
					count_uncertainty++;
					if (count_uncertainty >= myClues.clue_list[which_clue].value)
						keep_processing = false;
				} else
				{
					// See if it's worth tracking the UNKNOWNs (clue value must
					// be > 1)
					if (myClues.clue_list[which_clue].value == 1)
						keep_processing = false;
					else
					{
						checking_uncertainty = true;
						count_uncertainty++;
					}
				}
			} else if (squares[isquare].IsEmpty())
			{
				// if we were tracking a Blob, then assign it to the Clue's
				// fixed location
				if (tracking_blob)
				{
					int blob_len = blob_end - blob_start + 1;
					assert (blob_len == myClues.clue_list[which_clue].value);
					myClues.clue_list[which_clue].SetFixed(blob_start, blob_end);
					which_clue++;
					keep_processing =  which_clue < myClues.clue_list.length;
				}
				tracking_blob = false;
			} else
			{
				// Handle a FILLED square
				if (!tracking_blob)
				{
					blob_start = isquare;
					tracking_blob = true;
				}
				blob_end = isquare;
			}
			isquare++;
			if (isquare == squares.length) keep_processing = false;
		}
		if (isquare == squares.length && tracking_blob)
		{
			int blob_len = blob_end - blob_start + 1;
			assert (blob_len == myClues.clue_list[which_clue].value);
			myClues.clue_list[which_clue].SetFixed(blob_start, blob_end);			
		}
		*/
		
		// Let's get the Blobs.  If any are fully anchored and match a single
		// clue value, then we can assign that blob to that clue.  We can
		// also look at how many clue values are >= blob length.  If there is
		// only one clue, then we can anchor (or fix) that blob to that clue
		ArrayList<Blob> blobs = GetAllBlobs (squares);
		for (Blob b: blobs)
		{
			// let's look at fixed Blobs first
			if (b.IsFullyAnchored())
			{
				int blob_len = b.GetLength();
				ArrayList<Integer> clue_indices = myClues.GetIndicesOfCluesWithValue (blob_len);
				if (clue_indices.size() == 1)
				{
					int clue_index = clue_indices.get(0);
					Clue myClue = myClues.clue_list[clue_index];
					myClue.SetFixed(b.start, b.end);
				}
			}
			
			// let's look at large blobs
			int blob_len = b.GetLength();
			ArrayList<Integer> clue_indices = myClues.GetIndicesOfCluesWithGEValue (blob_len);
			if (clue_indices.size() == 1)
			{
				int clue_index = clue_indices.get(0);
				Clue myClue = myClues.clue_list[clue_index];
				if (myClue.value == blob_len)
				{
					myClue.SetFixed (b.start, b.end);
					if (b.start > 0 && !squares[b.start-1].IsEmpty())
					{
						assert (!squares[b.start-1].IsFilled());
						squares[b.start-1].SetStatus (PuzzleSquare.SquareStatus.EMPTY, guess_level);
					}
					if (b.end < squares.length-1 && !squares[b.end+1].IsEmpty())
					{
						assert (!squares[b.end+1].IsFilled());						
						squares[b.end+1].SetStatus(PuzzleSquare.SquareStatus.EMPTY, guess_level);
					}
				} else
				{
					myClue.SetAnchored (b.start, b.end);
				}
			}
		}
		
		// Process each clues' extents based on the actual data, removing spaces
		// to the left and right that may have been claimed by clues to left and
		// right, always leaving one space to separate from the blob belonging to
		// that clue
		
		// Find closest clue to the left that is either fixed or anchored
		for (int my_index=0; my_index<myClues.NumClues(); my_index++)
		{
			Clue myClue = myClues.clue_list[my_index];
			
			// We can skip this process if the clue is already fixed or anchored
			if (myClue.is_fixed || myClue.is_anchored) continue;
			
			if (myClue.max_extent < myClue.min_extent)
				throw new PuzzleSolverException ("Min max extent error for clue " + my_index,
						myClues, squares);
						
			// Process left side
			int closest_clue = -1;
			if (my_index > 0)
			{
				for (int j=0; j<my_index; j++)
					if (myClues.clue_list[j].is_fixed || myClues.clue_list[j].is_anchored)
						closest_clue = j;
			}
			if (closest_clue >= 0)
			{
				int new_min_extent = myClues.clue_list[closest_clue].end_filled;
				if (myClues.clue_list[closest_clue].is_fixed) new_min_extent++;
				if (new_min_extent > myClue.min_extent) myClue.min_extent = new_min_extent;
			}
			if (myClue.max_extent < myClue.min_extent)
				throw new PuzzleSolverException ("Min max extent error for clue " + my_index + 
						"after processing left side for fixed clues",
						myClues, squares);
		
			// Process right side
			closest_clue = -1;
			if (my_index < myClues.NumClues()-1)
			{
				for (int j=myClues.NumClues()-1; j>my_index; j--)
					if (myClues.clue_list[j].is_fixed || myClues.clue_list[j].is_anchored)
						closest_clue = j;
			}
			if (closest_clue >= 0)
			{
				int new_max_extent = myClues.clue_list[closest_clue].start_filled;
				if (myClues.clue_list[closest_clue].is_fixed) new_max_extent--;
				if (new_max_extent < myClue.max_extent) myClue.max_extent = new_max_extent;
			}
			if (myClue.max_extent < myClue.min_extent)
				throw new PuzzleSolverException ("Min max extent error for clue " + my_index + 
						" after processing right side for fixed clues",
						myClues, squares);
		
			// Now if the ends of the new extent contain EMPTYs, then remove those
			// from the extent
			
			if (myClue.max_extent > myClue.min_extent)
			{
				boolean process_edge = true;
				int start = myClue.min_extent;
				while (process_edge && start < squares.length)
				{
					if (squares[start].IsEmpty()) 
						start++;
					else process_edge = false;
				}
				myClue.min_extent = start;
				if (myClue.max_extent < myClue.min_extent)
					throw new PuzzleSolverException ("Min max extent error for clue " + my_index + 
							" after removing Xs from left edge of extent",
							myClues, squares);
				process_edge = true;
				start = myClue.max_extent;
				while (process_edge && start >= 0)
				{
					if (squares[start].IsEmpty()) 
						start--;
					else process_edge = false;
				}
				myClue.max_extent = start;
				if (myClue.max_extent < myClue.min_extent)
					throw new PuzzleSolverException ("Min max extent error for clue " + my_index + 
							" after removing Xs from right edge of extent",
							myClues, squares);
			}
			
			// Now copy this clue extent into a new PuzzleSquare[] in order to
			// search for Ranges
			int extent_len = myClue.max_extent - myClue.min_extent + 1;
			if (extent_len <= 0)
			{
				throw new PuzzleSolverException ("Min max extent error for clue " + my_index + 
						" after all extent processing",
						myClues, squares);
			}
			PuzzleSquare[] extentSquares = new PuzzleSquare[extent_len];
			for (int j=0; j<extent_len; j++)
				extentSquares[j] = new PuzzleSquare (squares[j+myClue.min_extent]);
			
			// Find Ranges within the extent
			ArrayList<Range> ranges = GetAllRanges(extentSquares);
			
			// From the list, remove all ranges that are too small to contain
			// the clue value
			int clue_val = myClue.value;
			ArrayList<Range> new_ranges = new ArrayList();
			for (Range r : ranges)
				if (r.GetLength() >= clue_val)
					new_ranges.add(r);
			
			// If the remaining list contains only one Range, then see if it is
			// small enough to process
			if (new_ranges.size() == 1)
			{
				Range r = new_ranges.get(0);
				if (r.GetLength() < 2*clue_val)
				{
					int uncertainty = r.GetLength() - clue_val;
					int temp_val = clue_val - uncertainty;
					for (int j=0; j<temp_val; j++)
					{
						int cell = j+uncertainty+myClue.min_extent+r.start;
						if (!squares[cell].IsFilled())
						{
							assert (!squares[cell].IsEmpty());
							squares[cell].SetStatus(PuzzleSquare.SquareStatus.FILLED, guess_level);
						}
					}
					
					// update the Clue now that it's either fixed or anchored
					int start_filled = uncertainty+myClue.min_extent+r.start;
					int end_filled   = start_filled + temp_val - 1;
					if (uncertainty == 0)
						myClue.SetFixed (start_filled, end_filled);
					else
					{
						myClue.SetAnchored (start_filled, end_filled);
						assert (clue_val > (end_filled-start_filled+1));
					}
				}
			}	
		}
		
		// Now let's set all UNKNOWN squares to EMPTY if they're between *fixed*
		// clues (edges are treated like fixed clues
		int num_clues = myClues.NumClues();
		Clue myClue = myClues.clue_list[0];
		if (myClue.is_fixed && myClue.min_extent > 0)
		{
			int edge = myClue.min_extent - 1;
			for (int i=0; i<edge; i++)
			{
				if (!squares[i].IsEmpty())
				{
					assert (!squares[i].IsFilled());
					squares[i].SetStatus(PuzzleSquare.SquareStatus.EMPTY, guess_level);
				}
			}
		}
		
		for (int i=0; i<num_clues-1; i++)
		{
			myClue = myClues.clue_list[i];
			Clue myClueNext = myClues.clue_list[i+1];
			if (myClue.is_fixed && myClueNext.is_fixed)
			{
				int edge_start = myClue.max_extent + 1;
				int edge_end   = myClue.min_extent - 1;
				if (edge_end-edge_start >= 0)
				{
					for (int j=edge_start; j<=edge_end; j++)
					if (!squares[j].IsEmpty())
					{
						assert (!squares[j].IsFilled());
						squares[j].SetStatus(PuzzleSquare.SquareStatus.EMPTY, guess_level);	
					}
				}
			}
		}
		
		myClue = myClues.clue_list[num_clues-1];
		if (myClue.is_fixed && myClue.max_extent < squares.length-1)
		{
			int edge = myClue.max_extent+1;
			for (int i=edge; i<squares.length; i++)
			{
				if (!squares[i].IsEmpty())
				{
					assert (!squares[i].IsFilled());
					squares[i].SetStatus(PuzzleSquare.SquareStatus.EMPTY, guess_level);
				}
			}				
		}
		
		// Lastly, let's process the spaces and clues in-between fixed and
		// anchored clues (if there's one or more such clues)
		// WANT TO WORK THIS IN EVENTUALLY!!!  LYNNE!!!!
		/*
		Clue last_Anchored = null;
		for (int i=0; i<myClues.NumClues(); i++)
		{
			myClue = myClues.clue_list[i];
			if (myClue.is_anchored)
			{
				last_Anchored = myClue;
			}
		}
		*/
		
		return squares;
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
			msg += "" + i + ") " + clues[i] + "\n";
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
	public static String DumpBlobs (ArrayList<Blob> blobs)
	{
		if (blobs == null) return "";
		else if (blobs.size() == 0) return "Blobs: none\n";
		String msg = "Blobs:\n";
		for (int i=0; i<blobs.size(); i++)
		{
			Blob b = blobs.get(i);
			msg += "" + i + ") " + b.toString();
			msg += "\n";
		}
		return msg;
	}
	public static String DumpRanges (ArrayList<Range> ranges)
	{
		if (ranges == null) return "";
		else if (ranges.size() == 0) return "Ranges: none\n";
		String msg = "Ranges:\n";
		for (int i=0; i<ranges.size(); i++)
		{
			Range r = ranges.get(i);
			msg += "" + i + ") " + r.start + " " + r.end + " (" +
					r.GetLength() + ")";
			if (r.clue_indices != null && r.clue_indices.size() > 0)
			{
				msg += " Clue indices:";
				for (int j=0; j<r.clue_indices.size(); j++)
					msg += " " + r.clue_indices.get(j);
			}
			if (r.blob_indices != null && r.blob_indices.size() > 0)
			{
				msg += " Blob indices:";
				for (int j=0; j<r.blob_indices.size(); j++)
					msg += " " + r.blob_indices.get(j);
			}
			msg += "\n";
		}
		return msg;		
	}
	
	private PuzzleSquare[] ProcessRangesForMinClueValue (ArrayList<Range> ranges, int[] clues, 
			ArrayList<Blob> blobs, PuzzleSquare[] old_squares, int guess_level)
	{
		PuzzleSquare[] squares = new PuzzleSquare[old_squares.length];
		for (int i=0; i<old_squares.length; i++)
			squares[i] = new PuzzleSquare(old_squares[i]);
		
		int num_changed = 0;
		
		for (Range r: ranges)
		{
			int min_clue_val = r.MinClueValue(clues);
			boolean is_unique = r.UniqueClueValues(clues);
			if (min_clue_val > 0)
			{
				// look at all the Blobs in our Range
				for (Blob b: r.GetBlobsInRange(blobs))
				{
					int uncertainty = min_clue_val - b.GetLength();
					// Isolate the Blob if it is the perfect size
					if (uncertainty == 0 && is_unique)
					{
						int cell = b.start-1;
						if (cell >= 0 && !squares[cell].IsEmpty())
						{
							num_changed++;
							assert (!squares[cell].IsFilled());
							squares[cell].SetStatus(PuzzleSquare.SquareStatus.EMPTY, guess_level);
						}
						cell = b.end+1;
						if (cell < squares.length && !squares[cell].IsEmpty())
						{
							num_changed++;
							assert (!squares[cell].IsFilled());
							squares[cell].SetStatus(PuzzleSquare.SquareStatus.EMPTY, guess_level);							
						}
					} else if (uncertainty < min_clue_val)
					{
						// See how far away Blob is from nearest EMPTY
						int buffer_to_left = 0;
						int buffer_to_right = 0;
						if (b.start > 0) 
							buffer_to_left = DistanceToNearestLeftEmpty (squares, b.start-1);
						if (b.end < squares.length-1)
							buffer_to_right = DistanceToNearestRightEmpty (squares, b.end+1);
						
						// Process Blob from the left
						if (buffer_to_left < uncertainty)
						{
							int start = b.start-buffer_to_left;
							boolean start_filling = false;
							for (int i=0; i<min_clue_val; i++)
							{
								int cell = start+i;
								if (squares[cell].IsFilled())
									start_filling = true;
								if (start_filling && !squares[cell].IsFilled())
								{
									num_changed++;
									assert (!squares[cell].IsEmpty());
									squares[cell].SetStatus(PuzzleSquare.SquareStatus.FILLED, guess_level);
								}
							}
						}
						
						// Process Blob from the right
						if (buffer_to_right < uncertainty)
						{
							int start = b.end+buffer_to_right;
							boolean start_filling = false;
							for (int i=0; i<min_clue_val; i++)
							{
								int cell = start-i;
								if (squares[cell].IsFilled())
									start_filling = true;
								if (start_filling && !squares[cell].IsFilled())
								{
									num_changed++;
									assert (!squares[cell].IsEmpty());
									squares[cell].SetStatus(PuzzleSquare.SquareStatus.FILLED, guess_level);
								}
							}
						}						
					}
				}
			}
		}
		
		return squares;
	}
	
	private int DistanceToNearestLeftEmpty (PuzzleSquare[] squares, int start)
	{
		int count = 0;
		for (int cell=start; cell>=0; cell--)
		{
			if (!squares[cell].IsEmpty()) count++;
			else return count;
		}
		return count;
	}
	
	private int DistanceToNearestRightEmpty (PuzzleSquare[] squares, int start)
	{
		int count = 0;
		for (int cell=start; cell<squares.length; cell++)
		{
			if (!squares[cell].IsEmpty()) count++;
			else return count;
		}
		return count;
	}
	
	// The logic assumed in this method is apparently incorrect.  If I just process the
	// Ranges with no Clues associated, it doesn't work.  If I just process the Ranges with
	// 1 Clue associated, it also doesn't work.  So don't call this one!!
	private PuzzleSquare[] ProcessRangesWithOneOrZeroPossibleClues (ArrayList<Range> ranges, 
			Clues myClues, PuzzleSquare[] old_squares, int guess_level)
	{
		PuzzleSquare[] squares = new PuzzleSquare[old_squares.length];
		for (int i=0; i<old_squares.length; i++)
			squares[i] = new PuzzleSquare(old_squares[i]);
		
		int[] clues = new int[myClues.NumClues()];
		for (int i=0; i<clues.length; i++) clues[i] = myClues.clue_list[i].value;
		
		int num_changed = 0;
		
		for (Range r: ranges)
		{
			if (r.clue_indices.size() == 0)
			{
				for (int cell=r.start; cell<=r.end; cell++)
				{
					if (!squares[cell].IsEmpty())
					{
						num_changed++;
						if (squares[cell].IsFilled())
						{
							System.out.println (DumpClues(myClues));
							System.out.println (DumpRanges(ranges));
						}
						assert (!squares[cell].IsFilled());
						squares[cell].SetStatus(PuzzleSquare.SquareStatus.EMPTY, guess_level);
					}
				}
			}
			else 
			if (r.clue_indices.size() == 1 && r.ContainsBlobs())
			{
				int clue_idx = r.clue_indices.get(0);
				int clue_val = clues[clue_idx];
				
				int range_len = r.GetLength();
				
				// Fill in the obvious cells
				int uncertainty = range_len - clue_val;
				if (uncertainty < clue_val)
				{
					int temp_val = clue_val - uncertainty;
					for (int i=0; i<temp_val; i++)
					{
						int cell = uncertainty+i+r.start;
						if (!squares[cell].IsFilled())
						{
							num_changed++;
							assert (!squares[cell].IsEmpty());
							squares[cell].SetStatus(PuzzleSquare.SquareStatus.FILLED, guess_level);
						}
					}
				}	
				
				// Fill in any cells between FILLED cells in range
				int first_cell_filled = -1;
				int last_cell_filled = -1;
				for (int cell=r.start; cell<=r.end; cell++)
				{
					if (squares[cell].IsFilled())
					{
						if (first_cell_filled == -1) first_cell_filled = cell;
						last_cell_filled = cell;
					}								
				}
				int blob_length = 0;
				if (first_cell_filled > 0 && last_cell_filled > 0)
				{
					for (int cell=first_cell_filled; cell<=last_cell_filled; cell++)
					{
						if (!squares[cell].IsFilled())
						{
							num_changed++;
							assert (!squares[cell].IsEmpty());
							squares[cell].SetStatus(PuzzleSquare.SquareStatus.FILLED, guess_level);
						}
					}
					blob_length = last_cell_filled - first_cell_filled + 1;
				}
				
				// Put Xs in any cells falling beyond possible boundary of new Blob
				if (blob_length > 0)
				{
					uncertainty = clue_val - blob_length;
					if (uncertainty > 0)
					{
						int cell = last_cell_filled+uncertainty+1;
						while (cell <= r.end)
						{
							if (!squares[cell].IsEmpty())
							{
								num_changed++;
								assert (!squares[cell].IsFilled());
								squares[cell].SetStatus(PuzzleSquare.SquareStatus.EMPTY, guess_level);
							}
							cell++;
						}
						cell = first_cell_filled-uncertainty-1;
						while (cell >= r.start)
						{
							if (!squares[cell].IsEmpty())
							{
								num_changed++;
								assert (!squares[cell].IsFilled());
								squares[cell].SetStatus(PuzzleSquare.SquareStatus.EMPTY, guess_level);
							}
							cell--;
						}						
					}
				}
			}
		}
		
		return squares;
	}
		
	private void CleanUpClueAssociationsWithRangesAndLinkedBlobs (ArrayList<Range> ranges, Clues myClues,
			ArrayList<Blob> blobs, PuzzleSquare[] squares) throws PuzzleSolverException
	{
		// Make a list of clues to be compatible with code written earlier in this method
		int[] clues = new int[myClues.NumClues()];
		for (int i=0; i<myClues.NumClues(); i++) clues[i] = myClues.clue_list[i].value;
			
		// for each Range with only one clue_index, eliminate that clue from
		// all other Range lists
		for (Range r: ranges)
		{
			if (r.clue_indices.size() == 1 && r.ContainsBlobs())
			{
				int clue_index = r.clue_indices.get(0);
				int r_index = ranges.indexOf(r);
				EliminateClueFromAllRangesExceptOne (ranges, clue_index, r_index);
				
				// also eliminate any clue indices that are greater than this one for
				// Ranges to the left
				EliminateCluesLargerThanClueIndexToTheLeft (ranges, clue_index, r_index);
			}
		}
		
		// from left to right, eliminate any clues that are > than the maximum
		// clue value for a Range to the right
		for (int ir=0; ir<ranges.size()-1; ir++)
		{
			Range r = ranges.get(ir);
			
			// get smallest maximum clue index for all ranges to the right
			int smallest_max_clue_index = SmallestMaxClueIndexForRanges (ranges, ir+1, ranges.size()-1);
			if (smallest_max_clue_index < 0) continue;
			
			// eliminate any clue indices that are larger than this smallest max
			// for the current Range
			LinkedList<Integer> new_cluelist = new LinkedList();
			for (Integer ii : r.clue_indices)
			{
				if (ii.intValue() <= smallest_max_clue_index)
					new_cluelist.add(new Integer(ii.intValue()));
			}
			r.clue_indices = new_cluelist;
		}
		
		// Now look for any clues that are unique to one Range
		for (int ic=0; ic<clues.length; ic++)
		{
			int count_range = 0;
			int ir = -1;
			for (Range r: ranges)
			{
				if (r.ContainsClueIndex(ic)) 
				{
					count_range++;
					ir = ranges.indexOf(r);
				}
			}
			if (count_range == 1 && ir < ranges.size()-1)
			{
				// eliminate all smaller clue indices for ranges to the right
				for (int i=ir+1; i<ranges.size(); i++)
				{
					Range r_right = ranges.get(i);
					r_right.RemoveClueIndicesSmallerThan(ic);
				}
				// eliminate all larger clue indices for ranges to the left
				for (int i=0; i<ir; i++)
				{
					Range r_left = ranges.get(i);
					r_left.RemoveClueIndicesLargerThan(ic);
				}
			}
		}
		
		boolean[] clue_is_located = new boolean[clues.length];
		int[]     blob_idx_for_clue = new int[clues.length];
		int[]     range_idx_for_clue = new int[clues.length];
		for (int i=0; i<clues.length; i++) 
		{
			clue_is_located[i] = false;
			blob_idx_for_clue[i] = -1;
			range_idx_for_clue[i] = -1;
		}
		
		// Now let's see if we have any chains of linked fixed Blobs that can be
		// uniquely matched to sequences of Clues
		ArrayList<ArrayList<Blob>> linked_lists = GetChainsOfLinkedFixedBlobs (blobs);
		if (linked_lists.size() > 0)
		{
			// For each linked list, see if there is a unique set of clues associated
			// with the sequence of linked Blobs
			for (ArrayList<Blob> linked_list: linked_lists)
			{
				int[] blob_clue_vals = new int[linked_list.size()];
				int idx = 0;
				for (Blob b : linked_list)
				{
					blob_clue_vals[idx++] = b.GetLength();
				}
				
				// See how many times we can match this sequence in our clue list
				int num_matched_sequences = GetNumberOfMatchingSequences (clues, blob_clue_vals);
				if (num_matched_sequences == 1)
				{
					// Find starting clue index for matching sequence
					int start_sequence = GetStartOfFirstMatchingSequences (clues, blob_clue_vals);
					
					// Mark each of these clues as located
					for (int i=0; i<blob_clue_vals.length; i++)
					{
						clue_is_located[i+start_sequence] = true;
						blob_idx_for_clue[i+start_sequence] = blobs.indexOf(linked_list.get(i));
						Clue clue = myClues.clue_list[i+start_sequence];
						clue.SetFixedAtBlob (linked_list.get(i));	
					}
				}
			}
			
			// Now for each uniquely located clue, we need to find the Range that
			// contains the Blob and say that that Range uniquely contains the clue
			// and eliminate that clue from all other Ranges
			for (int i=0; i<clues.length; i++)
			{
				if (clue_is_located[i])
				{
					Blob b = blobs.get(blob_idx_for_clue[i]);
					
					// Find Range that contains the Blob
					int ir = -1;
					for (Range r: ranges)
					{
						if (r.ContainsBlob(b))
						{
							// Reset clue_list so it only has the one clue
							r.clue_indices = new LinkedList();
							r.clue_indices.add(new Integer(i));
							ir = ranges.indexOf(r);
						}
					}
					assert (ir >= 0);
					
					// Eliminate clue i from all other Ranges
					for (Range r: ranges)
					{
						int idx = ranges.indexOf(r);
						if (idx != ir)
							r.RemoveClueIndex(i);
					}
				}
			}
		}

		// Now let's see if we have any chains of linked fixed Blobs that can be
		// matched to sequences of Clues from left edge
		ArrayList<Range> range_list = GetChainOfLinkedIncompleteRangesWithBlobsFromLeft (ranges, squares);
		if (range_list.size() > 1)
		{
			// Starting with first "available clue" from left
			int first_avail_clue = -1;
			for (int i=0; i<clue_is_located.length; i++)
				if (!clue_is_located[i])
				{
					first_avail_clue = i;
					break;
				}
			
			// Make sure we have at least 3 sequential available clues to look at
			boolean three_unlocated_clues = true;	// we know the 1st one is okay
			for (int i=1; i<3; i++)
			{
				int clue_idx = first_avail_clue+i;
				if (clue_idx >= clue_is_located.length) three_unlocated_clues = false;
				if (clue_is_located[clue_idx]) three_unlocated_clues = false;
			}
			
			// Only continue if we have three unlocated clues in a row
			if (three_unlocated_clues)
			{
				// See if 1st Range can accommodate the 1st 3 clues - if it can,
				// then there's nothing further to do
				boolean first_range_is_good;
				
				if (range_list.get(0).GetLength() >=
						clues[first_avail_clue] + 1 + clues[first_avail_clue+1] +
						1 + clues[first_avail_clue+2])				
					first_range_is_good = false;
				
				// See if 1st Range can accomodate the 1st two clues
				else
					first_range_is_good = range_list.get(0).GetLength() >=
						clues[first_avail_clue] + 1 + clues[first_avail_clue+1];

				// See if 3rd clue can be accomodated by the 2nd Range
				boolean second_range_is_good = range_list.get(1).GetLength() >=
						clues[first_avail_clue+2];

				// If 3rd clue cannot be accomodated by 2nd Range, then the 1st clue
				// must be in the 1st Range and the 2nd clue in the 2nd Range
				if (first_range_is_good && !second_range_is_good)
				{
					for (int ir=0; ir<2; ir++)
					{
						Range r = range_list.get(ir);
						int irdx = ranges.indexOf(r);
						int clue_idx = first_avail_clue+ir;
						r.clue_indices = new LinkedList();
						r.clue_indices.add(new Integer(clue_idx));
						EliminateClueFromAllRangesExceptOne (ranges, clue_idx, irdx);
					}
				}
			}
		}	
		
		// Now let's see if we have any chains of linked fixed Blobs that can be
		// matched to sequences of Clues
		range_list = GetChainOfLinkedIncompleteRangesWithBlobsFromRight (ranges, squares);
		if (range_list.size() > 1)
		{
			// Starting with first "available clue" from left
			int first_avail_clue = -1;
			for (int i=clue_is_located.length-1; i>=0; i--)
				if (!clue_is_located[i])
				{
					first_avail_clue = i;
					break;
				}
			
			// Make sure we have at least 3 sequential available clues to look at
			boolean three_unlocated_clues = true;	// we know the 1st one is okay
			for (int i=1; i<3; i++)
			{
				int clue_idx = first_avail_clue-i;
				if (clue_idx < 0) three_unlocated_clues = false;
				else if (clue_is_located[clue_idx]) three_unlocated_clues = false;
			}
			
			// Only continue if we have three unlocated clues in a row
			if (three_unlocated_clues)
			{
				// See if 1st Range can accommodate the 1st 3 clues - if it can,
				// then there's nothing further to do
				boolean first_range_is_good;
				
				if (range_list.get(0).GetLength() >=
						clues[first_avail_clue] + 1 + clues[first_avail_clue-1] +
						1 + clues[first_avail_clue-2])				
					first_range_is_good = false;
				
				// See if 1st Range can accomodate the 1st two clues
				else
					first_range_is_good = range_list.get(0).GetLength() >=
						clues[first_avail_clue] + 1 + clues[first_avail_clue-1];

				// See if 3rd clue can be accomodated by the 2nd Range
				boolean second_range_is_good = range_list.get(1).GetLength() >=
						clues[first_avail_clue-2];

				// If 3rd clue cannot be accomodated by 2nd Range, then the 1st clue
				// must be in the 1st Range and the 2nd clue in the 2nd Range
				if (first_range_is_good && !second_range_is_good)
				{
					for (int ir=0; ir<2; ir++)
					{
						Range r = range_list.get(ir);
						int irdx = ranges.indexOf(r);
						int clue_idx = first_avail_clue-ir;
						r.clue_indices = new LinkedList();
						r.clue_indices.add(new Integer(clue_idx));
						EliminateClueFromAllRangesExceptOne (ranges, clue_idx, irdx);
					}
				}
			}
		}			
	}
	
	private int GetNumberOfMatchingSequences (int[] clues, int[] blob_clues)
	{
		int num_matches = 0;
		int clue_num_diff = clues.length - blob_clues.length;
		
		for (int i=0; i<=clue_num_diff; i++)
		{
			boolean sequence_matches = true;
			for (int j=0; j<blob_clues.length; j++)
				if (clues[j+i] != blob_clues[j])
				{
					sequence_matches = false;
					break;
				}
			if (sequence_matches)
				num_matches++;
		}
		return num_matches;
	}
	
	private int GetStartOfFirstMatchingSequences (int[] clues, int[] blob_clues)
	{
		int clue_num_diff = clues.length - blob_clues.length;
		
		for (int i=0; i<=clue_num_diff; i++)
		{
			boolean sequence_matches = true;
			for (int j=0; j<blob_clues.length; j++)
				if (clues[j+i] != blob_clues[j])
				{
					sequence_matches = false;
					break;
				}
			if (sequence_matches)
				return i;
		}
		return -1;
	}
	
	private ArrayList<Range> GetChainOfLinkedIncompleteRangesWithBlobsFromLeft (ArrayList<Range> ranges,
			PuzzleSquare[] squares)
	{
		ArrayList<Range> linked_list = new ArrayList();
		
		int istart = 0;
		// let's skip past all of the Ranges that are Blobs (i.e. completely filled)
		for (int i=0; i<ranges.size(); i++)
		{
			Range r = ranges.get(i);
			if (!r.IsABlob(squares)) break;
			istart++;
		}
		
		boolean start_linked_list = false;
		for (int i=istart; i<ranges.size(); i++)
		{
			Range r = ranges.get(i);
			
			// If we encounter a mystery right away, then we're done
			if (!start_linked_list && r.IsAMystery(squares)) break;
			
			// We can START a linked list if we find a Range that has a Blob
			// in it, but it is not itself a complete Blob (all FILLEDs)
			if (!start_linked_list && r.ContainsBlobs() && !r.IsABlob(squares))
			{
				// This could be the start of a list
				start_linked_list = true;
				linked_list = new ArrayList();
				linked_list.add(r);
				
			// We can continue to add to our list a Range with a Blob but is
			// not itself a complete Blob (all FILLEDs)
			} else if (start_linked_list && r.ContainsBlobs() && !r.IsABlob(squares))
			{
				linked_list.add(r);
			} 
			
			// If we are making a list and encounter our first Range that
			// either a Mystery or a Blob or we're at the end, then we
			// end our list
			if (start_linked_list && (r.IsAMystery(squares) || r.IsABlob(squares) || i==ranges.size()-1))
			{
				// This is end of our list.
				start_linked_list = false;
				// Don't return a linked list of length 1
				if (linked_list.size() == 1) linked_list = new ArrayList();
				return linked_list;
			}
		}
		
		return linked_list;
	}
	
	private ArrayList<Range> GetChainOfLinkedIncompleteRangesWithBlobsFromRight (ArrayList<Range> ranges,
			PuzzleSquare[] squares)
	{
		ArrayList<Range> linked_list = new ArrayList();
		
		int istart = ranges.size()-1;
		// let's skip past all of the Ranges that are Blobs (i.e. completely filled)
		for (int i=ranges.size()-1; i>=0; i--)
		{
			Range r = ranges.get(i);
			if (!r.IsABlob(squares)) break;
			istart--;
		}
		
		boolean start_linked_list = false;
		for (int i=istart; i>=0; i--)
		{
			Range r = ranges.get(i);
			
			// If we encounter a mystery right away, then we're done
			if (!start_linked_list && r.IsAMystery(squares)) break;
			
			// We can START a linked list if we find a Range that has a Blob
			// in it, but it is not itself a complete Blob (all FILLEDs)
			if (!start_linked_list && r.ContainsBlobs() && !r.IsABlob(squares))
			{
				// This could be the start of a list
				start_linked_list = true;
				linked_list = new ArrayList();
				linked_list.add(r);
				
			// We can continue to add to our list a Range with a Blob but is
			// not itself a complete Blob (all FILLEDs)
			} else if (start_linked_list && r.ContainsBlobs() && !r.IsABlob(squares))
			{
				linked_list.add(r);
			} 
			
			// If we are making a list and encounter our first Range that
			// either a Mystery or a Blob or we're at the end, then we
			// end our list
			if (start_linked_list && (r.IsAMystery(squares) || r.IsABlob(squares) || i==ranges.size()-1))
			{
				// This is end of our list.
				start_linked_list = false;
				// Don't return a linked list of length 1
				if (linked_list.size() == 1) linked_list = new ArrayList();
				return linked_list;
			}
		}
		
		return linked_list;
	}
	
	private ArrayList<ArrayList<Blob>> GetChainsOfLinkedFixedBlobs (ArrayList<Blob> blobs)
	{
		ArrayList<ArrayList<Blob>> list_of_lists = new ArrayList();
		ArrayList<Blob> linked_list = new ArrayList();
		
		boolean start_linked_list = false;
		for (int i=0; i<blobs.size(); i++)
		{
			Blob b = blobs.get(i);
			if (!start_linked_list && b.is_anchored_start)
			{
				// This could be the start of a list
				start_linked_list = true;
				linked_list = new ArrayList();
				linked_list.add(b);
			} else if (start_linked_list && b.is_fixed_to_blob_prev)
			{
				linked_list.add(b);
			} 
			if (start_linked_list && (!b.is_fixed_to_blob_next || i==blobs.size()-1))
			{
				// This is end of our list.  If list is > 1 length, then
				// we'll keep it
				start_linked_list = false;
				if (linked_list.size() > 1)
					list_of_lists.add(linked_list);
			}
		}
		
		return list_of_lists;
	}
	
	private int SmallestMaxClueIndexForRanges (ArrayList<Range> ranges, int start_range, int end_range)
	{
		int smallest_max_clue_index = -1;
		for (int ir=start_range; ir<=end_range; ir++)
		{
			Range r = ranges.get(ir);
			int max_clue_index = r.MaxClueIndex();
			if (smallest_max_clue_index < 0 || max_clue_index < smallest_max_clue_index)
				smallest_max_clue_index = max_clue_index;
		}
		return smallest_max_clue_index;
	}
	
	private void EliminateCluesLargerThanClueIndexToTheLeft (ArrayList<Range> ranges,
		int clue_index, int range_index)
	{
		LinkedList<Integer> newList;
		
		if (range_index > 0)
		{
			for (int ir=0; ir<range_index; ir++)
			{
				newList = RemoveClueIndicesLargerThanIndexFromList (ranges.get(ir).clue_indices, clue_index);
				ranges.get(ir).clue_indices = newList;
			}
		}
	}
	
	private void EliminateClueFromAllRangesExceptOne (ArrayList<Range> ranges,
		int clue_to_remove, int range_to_ignore)
	{
		for (int ir=0; ir<ranges.size(); ir++)
		{
			if (ir != range_to_ignore)
				RemoveClueIndexFromList (ranges.get(ir).clue_indices, clue_to_remove);
		}
	}
	
	private LinkedList<Integer> RemoveClueIndicesLargerThanIndexFromList (LinkedList<Integer> index_list, int clue_index)
	{
		LinkedList<Integer> newList = new LinkedList();
		
		for (Integer ii: index_list)
			if (ii.intValue() <= clue_index)
				newList.add(ii);
		
		return newList;
	}
	
	private void RemoveClueIndexFromList (LinkedList<Integer> index_list, int val_to_remove)
	{
		for (Integer ii : index_list)
		{
			if (ii.intValue() == val_to_remove)
			{
				index_list.remove(ii);
				break;	// Assuming this index occurs only once in the list!!
			}
		}
	}

	private PuzzleSquare[] AssociateCluesWithRanges (int[] clues, ArrayList<Blob> blobs, 
			PuzzleSquare[] old_squares, ArrayList<Range> ranges, int guess_level)
	{
		PuzzleSquare[] squares = new PuzzleSquare[old_squares.length];
		for (int i=0; i<squares.length; i++) squares[i] = new PuzzleSquare(old_squares[i]);
		
		int num_changed = 0;
		
		for (Range r: ranges)
		{
			last_message += "   Range " + ranges.indexOf(r) + " clues:";
			r.clue_indices = new LinkedList();
			for (int ic=0; ic<clues.length; ic++)
			{
				int clue_val = clues[ic];
				
				boolean index_consumed = false;					

				// let's check if the clue could fit anywhere within Range
				// (this is sort of a brute force approach)
				if (!index_consumed)
				{
					// Copy our Range
					PuzzleSquare[] range_squares = new PuzzleSquare[r.GetLength()];

					for (int i=r.start; i<=r.end-clue_val+1; i++)
					{
						// reinitialize the range_squares
						for (int j=0; j<r.GetLength(); j++) 
							range_squares[j] = new PuzzleSquare(squares[r.start+j]);

						// now fill in clue_val # of squares starting at i-r.start
						for (int j=0; j<clue_val; j++)
						{
							int cell = j+i-r.start;
							assert (cell <= r.end);
							if (!range_squares[cell].IsFilled())
							{
								num_changed++;	// Since we're operating on a *copy* of the puzzle,
												// this shouldn't matter
								// No assert necessary here
								range_squares[cell].SetStatus (PuzzleSquare.SquareStatus.FILLED, guess_level);
							}
						}

						// now get all Blobs in our range_squares
						ArrayList<Blob> tempBlobs =  GetAllBlobs (range_squares);

						// if any of our Blobs are exactly the clue_val length, then we know this could work
						for (Blob bb : tempBlobs)
						{
							if (bb.GetLength() == clue_val && !index_consumed)
							{
								r.clue_indices.add(ic);									
								index_consumed = true;
								last_message += " " + ic;
								break;
							}
						}
						
						if (index_consumed)
							break;
					}
				}
			}
			last_message += "\n";
		}
		
		return squares;
	}
	
	private boolean IsRangeAnchoredAtStart (PuzzleSquare[] squares, Range r)
	{
		if (r.start == 0) return true;
		else return squares[r.start-1].GetStatus() == PuzzleSquare.SquareStatus.EMPTY;
	}
	private boolean IsRangeAnchoredAtEnd (PuzzleSquare[] squares, Range r)
	{
		if (r.end == squares.length-1) return true;
		else return squares[r.end+1].GetStatus() == PuzzleSquare.SquareStatus.EMPTY;		
	}
	private Range ExtendRangeFromBlob (PuzzleSquare[] squares, Blob b)
	{
		// find start of Range
		int start = b.start;
		if (start > 0)
		{
			for (int i=b.start-1; i>=0; i--)
			{
				if (squares[i].GetStatus() != PuzzleSquare.SquareStatus.EMPTY)
					start--;
			}
		}
		// find end of Range
		int end = b.end;
		if (end < squares.length-1)
		{
			for (int i=b.end+1; i<squares.length; i++)
			{
				if (squares[i].GetStatus() != PuzzleSquare.SquareStatus.EMPTY)
					end++;
			}
		}
		
		// Return extended Range
		Range r = new Range (start, end);
		return r;
	}
	
	private ArrayList<Range> GetAllRanges (PuzzleSquare[] squares)
	{
		ArrayList<Range> range_list = new ArrayList();
		
		int start = 0;
		int end = squares.length-1;
		boolean range_open = false;
		for (int i=0; i<squares.length; i++)
		{
			// square is UNKNOWN or FILLED (so part of a Range)
			if (squares[i].IsUnknown() ||  squares[i].IsFilled())
			{
				if (!range_open) start = i;
				range_open = true;
				end = i;
				
			} else // square is EMPTY (no longer in a Range)
			{
				if (range_open) 
				{
					range_list.add(new Range (start, end));
					last_message += "  new range (" + start + " " + end + ")\n";
				}
				range_open = false;
			}
		}
		if (range_open)
		{
			range_list.add(new Range (start, end));
			last_message += "  new range (" + start + " " + end + ")\n";	
		}
		
		return range_list;
	}

	private ArrayList<Blob> GetAllBlobs (PuzzleSquare[] squares)
	{
		ArrayList<Blob> blob_list = new ArrayList();
		
		int start = 0;
		int end = squares.length-1;
		boolean blob_open = false;
		boolean is_anchored_start = false;
		boolean is_anchored_end = false;
		boolean is_fixed_start = false;
		boolean is_fixed_end = false;
		boolean is_fixed_to_prev_blob = false;
		boolean is_fixed_to_next_blob = false;
		for (int i=0; i<squares.length; i++)
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
					last_message += "   new Blob: " + b.toString();
					last_message += "\n";
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
			assert (end == squares.length-1);
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
			last_message += "   new Blob: " + b.toString();
			last_message += "\n";

		}
		
		return blob_list;
	}
	
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
	
    private static void CopyColToPuzzle (PBNPuzzle myPuzzle, PuzzleSquare[] myCol, int col)
    {
        if (myPuzzle == null || myCol == null) return;
        for (int i=0; i<myPuzzle.GetRows(); i++)
            myPuzzle.SetPuzzleRowCol(i, col, myCol[i]);
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
}