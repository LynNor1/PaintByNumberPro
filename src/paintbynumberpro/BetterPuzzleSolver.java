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
	
	// -------------------------------------------
	// Private classes we need to solve the puzzle
	// -------------------------------------------

	// Contiguous squares of UNKNOWN or FILLED
	private class Range 
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
				if (b.start >= start && b.start <= end && b.end >= start && b.end <= end)
				{
					newBlobs.add(b);
					blob_indices.add(blobList.indexOf(b));
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
	
	// Contiguous squares of UNKNOWN with either EMPTY (X) to each edge or
	// a buffer of one UNKNOWN square next to a FILLED square
	private class AntiBlob 
	{
		private int start;
		private int end;
		AntiBlob (int s, int e)
		{
			start = s;
			end = e;
		}
		public int GetLength ()
		{ return end-start+1; }
	}
	
	// Contiguous squares of FILLED
	private class Blob
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

	// Class to keep track of a Clue and it's possible associations
	private class Clue
	{
		private int nominal_start_range;
		private int nominal_end_range;
		private int index;
		private int value;
		private ArrayList<Range> actualRanges;
		private Blob single_blob;
		private boolean accounted_for = false;
		
		Clue (int idx, int val, int nom_start, int nom_end)
		{
			index = idx;
			value = val;
			nominal_start_range = nom_start;
			nominal_end_range = nom_end;
			single_blob = null;
			accounted_for = false;
		}
		
		public int GetNominalRangeLength ()
		{ return (nominal_end_range - nominal_start_range + 1); }
		
		public void SetSingleBlob (Blob b)
		{
			single_blob = b; 
			accounted_for = (b != null);
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
//			System.out.println ("New processing row " + row);
			
			// Gather up the clues
			int[] clues = PuzzleSolver.GetCluesForRowFromPuzzle(myPuzzle, row);
			
			// Gather up the status of the puzzle for this row
			PuzzleSquare squares[] = BetterPuzzleSolver.CopyRowFromPuzzle(myPuzzle, row);
			
			// Process this row
			PuzzleSquare new_squares[] = ProcessLine (clues, squares, guess_level);
			
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
//			System.out.println ("New processing col " + col);
			
			// Gather up the clues
			int [] clues = PuzzleSolver.GetCluesForColFromPuzzle(myPuzzle, col);
			
			// Gather up the status of the puzzle for this row
			PuzzleSquare squares[] = BetterPuzzleSolver.CopyColFromPuzzle(myPuzzle, col);
			
			// Process this column
			PuzzleSquare new_squares[] = ProcessLine (clues, squares, guess_level);
			
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
	
	private PuzzleSquare[] ProcessLine (int[] clues, PuzzleSquare[] squares, int guess_level)
	{	
		PuzzleSquare[] new_squares;
		
		// ---- Calculate initial ranges for all clues (assumes nothing about status of puzzle) ----
		Clue[] myClues = InitializeClues (clues, squares.length);
		
		// Get all "Ranges"
		// -- A Range is a contiguous set of UNKNOWN or FILLED squares
		ArrayList<Range> existing_ranges = GetAllRanges (squares);
		
		// If there are no ranges, then all squares are filled in
		if (existing_ranges.isEmpty()) return squares;
				
		// Get all "Blobs"
		// -- A Blob is a contiguous set of FILLED squares
		ArrayList<Blob> blobs = GetAllBlobs (squares);
		if (blobs.isEmpty()) return squares;
		
		// Update Ranges with Blobs that they contain
		for (Range r: existing_ranges) r.GetBlobsInRange(blobs);
		
		// Associate Clues with Ranges (without consideration of clue order or 
		// anything else) (Ranges end up with list of clue_indices)
		AssociateCluesWithRanges (clues, blobs, squares, existing_ranges);
		
		// Clean up Clue associations with Ranges
		CleanUpClueAssociationsWithRangesAndLinkedBlobs (existing_ranges, clues, blobs, squares);
		
		// Process all Ranges that have only one possible Clue
		new_squares = ProcessRangesWithOneOrZeroPossibleClues (existing_ranges, clues, squares, guess_level);
		squares = new_squares;
		
		// Process all Ranges that have multiple clues by processing for the smallest
		// clue value
		new_squares = ProcessRangesForMinClueValue (existing_ranges, clues, blobs, squares, guess_level);
		squares = new_squares;

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
	
	private Clue[] InitializeClues (int[] clues, int num_squares)
	{
		Clue[] myClues = new Clue[clues.length];
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
			myClues[clue_idx] = new Clue (clue_idx, clues[clue_idx], nom_start, nom_end);
		}
		return myClues;
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
							squares[cell].SetStatus(PuzzleSquare.SquareStatus.EMPTY, guess_level);
						}
						cell = b.end+1;
						if (cell < squares.length && !squares[cell].IsEmpty())
						{
							num_changed++;
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
	
	private PuzzleSquare[] ProcessRangesWithOneOrZeroPossibleClues (ArrayList<Range> ranges, 
			int[] clues, PuzzleSquare[] old_squares, int guess_level)
	{
		PuzzleSquare[] squares = new PuzzleSquare[old_squares.length];
		for (int i=0; i<old_squares.length; i++)
			squares[i] = new PuzzleSquare(old_squares[i]);
		
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
						squares[cell].SetStatus(PuzzleSquare.SquareStatus.EMPTY, guess_level);
					}
				}
			}
			else if (r.clue_indices.size() == 1)
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
	
	
	private void CleanUpClueAssociationsWithRangesAndLinkedBlobs (ArrayList<Range> ranges, int[] clues,
			ArrayList<Blob> blobs, PuzzleSquare[] squares)
	{
		// for each Range with only one clue_index, eliminate that clue from
		// all other Range lists
		for (Range r: ranges)
		{
			if (r.clue_indices.size() == 1)
			{
				int clue_index = r.clue_indices.get(0);
				int r_index = ranges.indexOf(r);
				EliminateClueFromAllRangesExceptOne (ranges, clue_index, r_index);
				
				// also elimiante any clues that are greater than this one for
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
		// matched to sequences of Clues
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
					blob_clue_vals[idx++] = b.GetLength();
				
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
		// matched to sequences of Clues
		// SOMETHING IS AMISS WITH THE LOGIC HERE!!
		/*
		ArrayList<Range> range_list = GetChainOfLinkedIncompleteRangesWithBlobsFromLeft (ranges, squares);
		if (range_list.size()> 0)
		{
			// Starting with first "available clue" from left
			int first_avail_clue = -1;
			for (int i=0; i<clue_is_located.length; i++)
				if (!clue_is_located[i])
				{
					first_avail_clue = i;
					break;
				}

			// Get sequence length for linked ranges
			int seq_len = range_list.size();

			// See if we can accomodate the 1st sequence of available clues
			// with our range list
			boolean first_list_good = true;
			for (int i=0; i<seq_len; i++)
			{
				int clue_idx = first_avail_clue+i;
				if (clue_idx >= clues.length)
				{
					first_list_good = false;
					break;
				}
				if (clue_is_located[clue_idx])
				{
					first_list_good = false;
					break;
				}

				int clue_val = clues[clue_idx];
				Range r = range_list.get(i);
				if (r.GetLength() < clue_val)
				{
					first_list_good = false;
					break;
				}
			}

			// Now let's check on the next list
			boolean second_list_good = true;
			for (int i=0; i<seq_len; i++)
			{
				int clue_idx = first_avail_clue+i+1;
				if (clue_idx >= clues.length)
				{
					second_list_good = false;
					break;
				}
				if (clue_is_located[clue_idx])
				{
					second_list_good = false;
					break;
				}

				int clue_val = clues[clue_idx];
				Range r = range_list.get(i);
				if (r.GetLength() < clue_val)
				{
					second_list_good = false;
					break;
				}
			}		

			// Now if first list is good and 2nd is NOT, then we can
			// assign unique clues to ranges
			if (first_list_good && !second_list_good)
			{
				for (int ir=0; ir<seq_len-1; ir++)
				{
					Range r = range_list.get(ir);
					int irdx = ranges.indexOf(r);
					int clue_idx = first_avail_clue + ir;
					r.clue_indices = new LinkedList();
					r.clue_indices.add(new Integer(clue_idx));

					EliminateClueFromAllRangesExceptOne (ranges, clue_idx, irdx);
				}
			}	
		}
		*/
		
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
	
	private LinkedList<Integer> RemoveClueIndicesLargerThanIndexFromList (LinkedList<Integer> list, int clue_index)
	{
		LinkedList<Integer> newList = new LinkedList();
		
		for (Integer ii: list)
			if (ii.intValue() <= clue_index)
				newList.add(ii);
		
		return newList;
	}
	
	private void RemoveClueIndexFromList (LinkedList<Integer> list, int val_to_remove)
	{
		for (Integer ii : list)
		{
			if (ii.intValue() == val_to_remove)
			{
				list.remove(ii);
				break;
			}
		}
	}

	private PuzzleSquare[] AssociateCluesWithRanges (int[] clues, ArrayList<Blob> blobs, 
			PuzzleSquare[] old_squares, ArrayList<Range> existing_ranges)
	{
		PuzzleSquare[] squares = new PuzzleSquare[old_squares.length];
		for (int i=0; i<squares.length; i++) squares[i] = new PuzzleSquare(old_squares[i]);
		
		int num_changed = 0;
		
		for (Range r: existing_ranges)
		{
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
							if (!range_squares[cell].IsFilled())
							{
								num_changed++;
								range_squares[cell].SetStatus (PuzzleSquare.SquareStatus.FILLED);
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
								break;
							}
						}
						
						if (index_consumed)
							break;
					}
				}
			}
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
			// square is UNKNOWN or FILLED
			if (squares[i].GetStatus() == PuzzleSquare.SquareStatus.UNKNOWN || 
					squares[i].GetStatus() == PuzzleSquare.SquareStatus.FILLED)
			{
				if (!range_open) start = i;
				range_open = true;
				end = i;
				
			} else // square is EMPTY
			{
				if (range_open) range_list.add(new Range (start, end));
				range_open = false;
			}
		}
		if (range_open)
			range_list.add(new Range (start, end));
		
		return range_list;
	}
	
	// An AntiBlob is a range of UNKNOWN squares within a Range r with enough
	// padding from Blobs to house an independent clue
	private ArrayList<AntiBlob> GetAllAntiBlobsInRange (Range r, PuzzleSquare[] squares)
	{
		ArrayList<AntiBlob> anti_blobs = new ArrayList();
		
		int start = r.start;
		int end;
		boolean anti_blob_started = false;
		
		for (int i=r.start; i<=r.end; i++)
		{
			PuzzleSquare.SquareStatus ss = squares[i].GetStatus();
			
			// process possible end of AntiBlob
			if (ss == PuzzleSquare.SquareStatus.EMPTY) {	// This should never happen
				if (anti_blob_started)
				{
					end = i-1; // back off by one
					anti_blobs.add(new AntiBlob (start, end));
					anti_blob_started = false;
				}
				
			} else if (ss == PuzzleSquare.SquareStatus.FILLED) {
				if (anti_blob_started)
				{
					end = i-2; // back off by two
					if (end>=start)
					{
						anti_blobs.add(new AntiBlob (start, end));
						anti_blob_started = false;						
					}
				}
				
			} else {	// process possible start of AntiBlob
				if (!anti_blob_started)
				{
					// check to make sure previous square is EMPTY or
					int peek = i-1;
					if (peek < 0 || squares[peek].IsEmpty())
					{
						start = i;
						anti_blob_started = true;
					}
					// check to make sure there is at least one UNKNOWN space
					// between here and last FILLED space
					else if (squares[peek].IsUnknown())
					{
						start = i;
						anti_blob_started = true;
					}
				}
			}
		}
		
		if (anti_blob_started)	// Ranges are surrounded by EMPTYs
		{
			end = r.end;
			anti_blobs.add(new AntiBlob (start, end));	
		}
		
		return anti_blobs;
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
					} else if (squares[i-1].GetStatus() == PuzzleSquare.SquareStatus.EMPTY)
					{
						is_anchored_start = true;
						is_fixed_start = true;
						for (int j=0; j<i; j++)
							if (squares[j].GetStatus() == PuzzleSquare.SquareStatus.UNKNOWN)
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
					if (squares[i].GetStatus() == PuzzleSquare.SquareStatus.EMPTY)
					{
						is_anchored_end =true;
						is_fixed_end = true;
						for (int j=i; j<squares.length; j++)
							if (squares[j].GetStatus() == PuzzleSquare.SquareStatus.UNKNOWN)
							{
								is_fixed_end = false;
								break;
							}
						is_fixed_to_prev_blob = true;
						if (start > 0)
						{
							for (int j=start-1; j>=0; j++)
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
					blob_list.add(new Blob (start, end, is_anchored_start, is_anchored_end,
						is_fixed_start, is_fixed_end, is_fixed_to_prev_blob, is_fixed_to_next_blob));				
					blob_open = false;
					is_anchored_start = false;
					is_anchored_end = false;
					is_fixed_start = false;
					is_anchored_end = false;
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
				for (int j=start-1; j>=0; j++)
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
			blob_list.add(new Blob (start, end, is_anchored_start, is_anchored_end,
				is_fixed_start, is_fixed_end, is_fixed_to_prev_blob, is_fixed_to_next_blob));
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