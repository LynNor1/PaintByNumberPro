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
public class NewPuzzleSolver {


	
	// -------------------------------------------
	// Private classes we need to solve the puzzle
	// -------------------------------------------

	// Contiguous squares of UNKNOWN or FILLED
	private class Range 
	{
		private int start;
		private int end;
		private ArrayList<Integer> blob_indices;
		
		Range (int s, int e)
		{
			start = s;
			end = e;
			blob_indices = new ArrayList();
		}
		public boolean OverlapsWithNominalClueRange (Clue myClue)
		{			
			if (start > myClue.nominal_end_range) return false;
			if (end   < myClue.nominal_start_range) return false;
			
			return true;
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
		private LinkedList<Integer> clue_indices;
		Blob (int s, int e, boolean anch_start, boolean anch_end,
				boolean fixed_start, boolean fixed_end)
		{
			start = s;
			end = e;
			is_anchored_start = anch_start;
			is_anchored_end = anch_end;
			is_fixed_start = fixed_start;
			is_fixed_end = fixed_end;
		}
		public boolean IsAnchored()
		{ return is_anchored_start || is_anchored_end; }
		public boolean IsFixed()
		{ return is_fixed_start || is_fixed_end; }
		public boolean IsFullyFixed()
		{ return is_fixed_start && is_fixed_end; }
		public int GetLength()
		{ return end-start+1; }
	}
	// Contiguous squares of UNKNOWN surrounded by EMPTYs
	private class Mystery
	{
		private int start;
		private int end;
		Mystery (int s, int e)
		{
			start = s;
			end = e;
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
			PuzzleSquare squares[] = NewPuzzleSolver.CopyRowFromPuzzle(myPuzzle, row);
			
			// Process this row
			PuzzleSquare new_squares[] = ProcessLine (clues, squares, guess_level);
			
			// See if something has changed
			boolean something_changed = ComparePuzzleSquares (new_squares, squares);
			
			// Update the puzzle with any new changes
			if (something_changed) NewPuzzleSolver.CopyRowToPuzzle (myPuzzle, new_squares, row);
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
			PuzzleSquare squares[] = NewPuzzleSolver.CopyColFromPuzzle(myPuzzle, col);
			
			// Process this column
			PuzzleSquare new_squares[] = ProcessLine (clues, squares, guess_level);
			
			// See if something has changed
			boolean something_changed = ComparePuzzleSquares (new_squares, squares);			
			
			// Update the puzzle with any new changes
			if (something_changed) NewPuzzleSolver.CopyColToPuzzle (myPuzzle, squares, col);
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
		
		// ---- Fill in squares based on nominal Clue ranges ----
		new_squares = UpdateSquaresFromNominalClues (myClues, squares, guess_level);
		squares = new_squares;
		
		// Get all "Ranges"
		// -- A Range is a contiguous set of UNKNOWN or FILLED squares
		ArrayList<Range> existing_ranges = GetAllRanges (squares);
		
		// If there are no ranges, then all squares are filled in
		if (existing_ranges.isEmpty()) return squares;
		
		// ---- Based on nominal ranges (i.e. available ranges), have each Clue
		// ---- set up a list of adjusted ranges that the Clue could actual reside within
		UpdateClueRangesFromExistingRanges (myClues, existing_ranges);
				
		// Get all "Blobs"
		// -- A Blob is a contiguous set of FILLED squares
		ArrayList<Blob> blobs = GetAllBlobs (squares);		
		
		// ---- Further break up clue ranges based on Blobs ----
		// ---- For example, a clue of 5 cannot exist anywhere within the range: ...FFF..F
		// ---- because we cannot actually fill in the two .. between the F's
		// ---- (Each clue will end up with its own set of Ranges)
		UpdateClueRangesConsideringBlobs (myClues, blobs);	
		
		// ---- Now fill in squares for the updated clue Ranges, if there is only
		// ---- 1 range per clue and the range is sufficiently small
		new_squares = UpdatePuzzleFromClueRangesAndBlobs (myClues, blobs, squares, guess_level);
		int count_differences = CountPuzzleSquareDifferences (squares, new_squares);
		squares = new_squares;
		
		// ---- Calculate new Blobs if we've changed anything ----
		if (count_differences > 0) blobs = GetAllBlobs (squares);
		
		// ---- Now associate Blobs with Ranges within each Clue ----
		AssociateBlobsWithRangesForEachClue (myClues, blobs);
		
		// ---- Now associate Blobs with Clues ----
		AssociateBlobsWithClues (myClues, blobs);		
		
		// ---- Now clean up Blob associations with Clues (e.g. if Blob0 is associated
		// ---- with only Clue0 and Blob1 is associated with Clue0 and Clue1, then 
		// ---- Blob1 can only be associated with Clue1 because Clue0 is now "taken" by
		// ---- Blob0.  So we need to remove all the Clue1 Ranges that contain Blob0)
		CleanUpBlobAssociationsWithClues (myClues, blobs);
		
		// ---- Now if we have one-to-one relationships between Blobs and Clues,
		// ---- we can eliminate Ranges from each Clue that do NOT contain the 
		// ---- one Blob
		EliminateRangesForSingleBlobs (myClues);
		
		// ---- Now update puzzle again for clue ranges
		// ---- Now fill in squares for the updated clue Ranges, if there is only
		// ---- 1 range per clue and the range is sufficiently small
		new_squares = UpdatePuzzleFromClueRangesAndBlobs (myClues, blobs, squares, guess_level);
		count_differences = CountPuzzleSquareDifferences (squares, new_squares);
		squares = new_squares;
				
		// ---- Update everything Blob related again ----
		if (count_differences > 0)
		{
			blobs = GetAllBlobs (squares);		
			AssociateBlobsWithRangesForEachClue (myClues, blobs);
			AssociateBlobsWithClues (myClues, blobs);		
			CleanUpBlobAssociationsWithClues (myClues, blobs);
			EliminateRangesForSingleBlobs (myClues);	
		}
		
		// ---- Now process Blobs that are associated with multiple clues
		// ---- (you can assume that they're at least the smallest clue_val of the
		// ---- bunch)
		new_squares = UpdatePuzzleFromBlobsWithMultipleClues (myClues, blobs, squares, guess_level);
		count_differences = CountPuzzleSquareDifferences (squares, new_squares);
		squares = new_squares;			
				
		// ---- Update everything Blob related again ----
		if (count_differences > 0)
		{
			blobs = GetAllBlobs (squares);		
			AssociateBlobsWithRangesForEachClue (myClues, blobs);
			AssociateBlobsWithClues (myClues, blobs);		
			CleanUpBlobAssociationsWithClues (myClues, blobs);
			EliminateRangesForSingleBlobs (myClues);	
		}		
		
		// ---- Now definitively link Clues with Blobs if possible and 
		
		// Get all "Mysteries"
		// -- An Mystery is a contiguous set of UNKNOWN squares surrounded by Xs
		ArrayList<Mystery> mysteries = GetAllMysteries (squares);

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
	
	// returns number of squares whose values actually changed
	private PuzzleSquare[] UpdateSquaresFromNominalClues (Clue[] myClues, PuzzleSquare[] old_squares, int guess_level)
	{
		PuzzleSquare[] squares = new PuzzleSquare[old_squares.length];
		for (int i=0; i<old_squares.length; i++) squares[i] = new PuzzleSquare(old_squares[i]);
		
		int num_changed = 0;
		
		for (int clue_idx=0; clue_idx<myClues.length; clue_idx++)
		{
			int range_len = myClues[clue_idx].GetNominalRangeLength();
			assert (range_len >= myClues[clue_idx].value);
			if (range_len < myClues[clue_idx].value*2)
			{
				int uncertainty = range_len - myClues[clue_idx].value;
				int temp_clue = myClues[clue_idx].value - uncertainty;
				int start_square = myClues[clue_idx].nominal_start_range;
				for (int i=0; i<temp_clue; i++)
				{
					int cell = i+start_square+uncertainty;
					assert (squares[cell].GetStatus() != PuzzleSquare.SquareStatus.EMPTY);
					if (squares[cell].GetStatus() != PuzzleSquare.SquareStatus.FILLED)
					{
						num_changed++;
						squares[cell].SetStatus(PuzzleSquare.SquareStatus.FILLED, guess_level);
					}
				}
			}
		}
		return squares;
	}
	
	private void UpdateClueRangesFromExistingRanges (Clue[] myClues, ArrayList<Range> ranges)
	{
		int num_clues = myClues.length;
		int num_ranges = ranges.size();
		
		for (int clue_idx = 0; clue_idx<num_clues; clue_idx++)
		{
			Clue myClue = myClues[clue_idx];
			int clue_val = myClue.value;
			myClue.actualRanges = new ArrayList<Range>();
			
			for (int range_idx=0; range_idx<num_ranges; range_idx++)
			{
				Range r = ranges.get(range_idx);
				if (r.OverlapsWithNominalClueRange (myClue))
				{
					// get intersection with range
					int start = Max (r.start, myClue.nominal_start_range);
					int end = Min (r.end, myClue.nominal_end_range);
					int temp_len = end-start+1;

					// add this new Range to the Clue
					if (temp_len >= clue_val)
						myClue.actualRanges.add(new Range (start, end));
				}
			}
			
			assert (!myClue.actualRanges.isEmpty());
		}
	}
	
	// Returns true if the current Clue has only one Range and that one Range
	// isn't big enough to be shared with the Clue to the left
	private boolean ClueHasOneUnshareableRangeWithLeft (Clue[] myClues, int clue_idx)
	{
		Clue clue = myClues[clue_idx];
		int clue_val = clue.value;
		int clue_val_left = 0;
		
		int num_ranges = clue.actualRanges.size();
		if (num_ranges > 1) return false;	// Clue has multiple Ranges
		
		int range_len = clue.actualRanges.get(0).GetLength();
		if (range_len == clue_val) return true;	// Clue's single Range is just big enough for the one Clue
		
		if (clue_idx > 0)
		{
			Clue clue_left = myClues[clue_idx-1];
			if (clue_left.accounted_for) return true; // Clue to the left is accounted for
			clue_val_left = clue_left.value;
			
			int min_len_needed = clue_val_left + clue_val + 1;
			return (range_len < min_len_needed); // Clue's single Range isn't big enough to share with
												 // clue to the left
		} else
			return true;	// Range does not need to be shared to the left
	}
	
	// Returns true if the current Clue has only one Range and that one Range
	// isn't big enough to be shared with the Clue to the right
	private boolean ClueHasOneUnshareableRangeWithRight (Clue[] myClues, int clue_idx)
	{
		Clue clue = myClues[clue_idx];
		int clue_val = clue.value;
		int clue_val_right = 0;
		
		int num_ranges = clue.actualRanges.size();
		if (num_ranges > 1) return false;	// Clue has multiple Ranges
		
		int range_len = clue.actualRanges.get(0).GetLength();
		if (range_len == clue_val) return true;	// Clue's single Range is just big enough for the one Clue
		
		if (clue_idx < myClues.length-1)
		{
			Clue clue_right = myClues[clue_idx+1];
			if (clue_right.accounted_for) return true; // Clue to the right is accounted for
			clue_val_right = clue_right.value;
			
			int min_len_needed = clue_val_right + clue_val + 1;
			return (range_len < min_len_needed); // Clue's single Range isn't big enough to share with
												 // clue to the right
		} else
			return true;	// Range does not need to be shared to the right
	}
	
	private void UpdateClueRangesConsideringBlobs (Clue[] myClues, ArrayList<Blob> blobs)
	{
		int num_clues = myClues.length;
		if (blobs.isEmpty()) return;
		
		// Process each Clue separately
		for (int clue_idx=0; clue_idx<num_clues; clue_idx++) {
			
			Clue myClue = myClues[clue_idx];
			int clue_val = myClue.value;
			
			boolean ranges_changed = true;
			
			while (ranges_changed) {
				
				ranges_changed = false;
			
				// Create an empty Range list so we can copy in unmodified Ranges and 
				// add in the ones that got modified when we removed Blobs that were too big
				ArrayList<Range> newRanges = new ArrayList();

				// ---- Process to eliminate Blobs that are too big for the given Clue ----
				for (Range range : myClue.actualRanges) {
					
					ArrayList<Blob> blobsInRange = range.GetBlobsInRange (blobs);

					// If there are no Blobs in this range then we can keep it
					if (blobsInRange.isEmpty())
					{
						newRanges.add(range);
						continue;
					}

					// Check if any Blobs are larger than the clue_val and break up
					// Range to eliminate those big blobs
					boolean is_processed = false;
					for (Blob b : blobsInRange)
					{
						if (b.GetLength() > clue_val)
						{
							ArrayList<Range> newRangeList = EliminateBlobFromRange (range, b);
							if (!newRangeList.isEmpty())
							{							
								// Add the new Ranges to the list
								for (Range r: newRangeList)
								{
									if (r.GetLength() >= clue_val)
										newRanges.add(r);
								}
							}
							is_processed = true;
							ranges_changed = true;
						}
					}
					if (!is_processed)
						newRanges.add(range);
				}
				
				// update the Ranges for the clue
				myClue.actualRanges = newRanges;
			}			
				
			ranges_changed = true;
			while (ranges_changed) {
				ranges_changed = false;
				
				// Create an empty Range list so we can copy in unmodified Ranges and 
				// add in the ones that got modified when we removed Blobs that were too big
				ArrayList<Range> newRanges = new ArrayList();				
				
				// Now process the new LinkedList of Ranges for Blobs which
				// cannot be joined
				for (Range range : myClue.actualRanges)
				{
					ArrayList<Blob> blobsInRange = range.GetBlobsInRange (blobs);

					// If there are no Blobs in this range then we can keep it
					if (blobsInRange.isEmpty() || blobsInRange.size() == 1) {
						newRanges.add(range);
						continue;
					}

					// Now look separate Ranges with Blobs that cannot be combined
					int num_blobs_in_range = blobsInRange.size();
					boolean is_processed = false;
					for (int ib=0; ib<(num_blobs_in_range-1); ib++)
					{
						Blob b1 = blobsInRange.get(ib);
						Blob b2 = blobsInRange.get(ib+1);
						assert (b1.end < b2.start);
						int possible_len = b2.end - b1.start + 1;
						if (possible_len > clue_val)
						{
							int uncertainty =  clue_val - b1.GetLength();
							Range r1 = new Range (range.start, b1.end+uncertainty);
							Range r2 = new Range (b1.end+uncertainty+1, range.end);
							if (r1.GetLength() >= clue_val) newRanges.add(r1);
							if (r2.GetLength() >= clue_val) newRanges.add(r2);
							ranges_changed = true;
							is_processed = true;
							break;
						}
					}
					if (!is_processed) newRanges.add(range);
				}
				
				// Update clue's actualRanges with the new LinkedList
				myClue.actualRanges = newRanges;				
			}
		}
	}
	
	// ---- Now fill in squares for the updated clue Ranges, if there is only
	// ---- 1 range per clue and the range is sufficiently small
	// ---- RETURN number of squares actually modified
	private PuzzleSquare[] UpdatePuzzleFromClueRangesAndBlobs (Clue[] myClues, ArrayList<Blob> blobs,
			PuzzleSquare[] old_squares, int guess_level)
	{
		PuzzleSquare[] squares = new PuzzleSquare[old_squares.length];
		for (int i=0; i<old_squares.length; i++) squares[i] = new PuzzleSquare(old_squares[i]);
		
		int num_changed = 0;
		
		for (int clue_idx=0; clue_idx<myClues.length; clue_idx++)
		{
			Clue myClue = myClues[clue_idx];
			int clue_val = myClue.value;
			
			if (myClue.actualRanges.size() == 1)
			{
				Range r = myClue.actualRanges.get(0);
				Blob b = null;
				if (r.blob_indices != null && r.blob_indices.size() == 1) 
				{
					int bidx = r.blob_indices.get(0);
					b = blobs.get(bidx);
				}
				
				// process the single if it is fixed at the start and all other
				// clues to its left are tied to a specific single range that is
				// small enough to accomodate only itself.
				if (b != null)
				{
					int dist_left = b.start - r.start;
					int dist_right = r.end - b.end;
					if (dist_left < clue_val || dist_right < clue_val)
					{
						// If Blob starts close enough to the left side and
						// the current Range cannot be shared with the Clue to 
						// the left, then extend the Blob from the left edge
						if (dist_left < clue_val && ClueHasOneUnshareableRangeWithLeft (myClues, clue_idx))
						{
							int count_unknowns = 0;
							int last_cell = 0;
							boolean start_filling = false;
							for (int ii=0; ii<clue_val; ii++)
							{
								int cell = ii+r.start;
								last_cell = cell;
								
								if (!start_filling && squares[cell].GetStatus() == PuzzleSquare.SquareStatus.UNKNOWN)
									count_unknowns++;
								
								if (squares[cell].GetStatus() == PuzzleSquare.SquareStatus.FILLED)
									start_filling = true;
								else if (start_filling)
								{
									if (squares[cell].GetStatus() != PuzzleSquare.SquareStatus.FILLED)
									{
										num_changed++;
										squares[cell].SetStatus(PuzzleSquare.SquareStatus.FILLED, guess_level);
									}
								}
							}
							if (count_unknowns == 0 && last_cell < squares.length-1)
							{
								if (squares[last_cell+1].GetStatus() != PuzzleSquare.SquareStatus.EMPTY)
								{
									num_changed++;
									squares[last_cell+1].SetStatus(PuzzleSquare.SquareStatus.EMPTY, guess_level);
								}								
							}
							
							// if this Range cannot be shared to the Right as well, then
							// let's close off the rest of the Range
							if (ClueHasOneUnshareableRangeWithRight (myClues, clue_idx))
							{
								if (count_unknowns > 0)
								{
									for (int ii=0; ii<count_unknowns; ii++)
									{
										last_cell++;
										if (last_cell < squares.length)
											assert (squares[last_cell].GetStatus() == PuzzleSquare.SquareStatus.UNKNOWN);
									}
								}
								// Add EMPTYs to the rest of the range to the right
								if (last_cell < r.end-1)
								{
									while (last_cell <= r.end-1)
									{
										last_cell++;
										if (squares[last_cell].GetStatus() != PuzzleSquare.SquareStatus.EMPTY)
										{
											num_changed++;
											squares[last_cell].SetStatus(PuzzleSquare.SquareStatus.EMPTY, guess_level);
										}
									}
								}
							}
						}
						
						// If Blob starts close enough to the right side and
						// the current Range cannot be shared with the Clue to
						// the right, then extend the Blob from the right edge
						if (dist_right < clue_val && ClueHasOneUnshareableRangeWithRight (myClues, clue_idx))
						{
							int count_unknowns = 0;
							int last_cell = 0;
							boolean start_filling = false;
							for (int ii=0; ii<clue_val; ii++)
							{
								int cell = r.end - ii;
								last_cell = cell;
								
								if (!start_filling && squares[cell].GetStatus() == PuzzleSquare.SquareStatus.UNKNOWN)
									count_unknowns++;
								
								if (squares[cell].GetStatus() == PuzzleSquare.SquareStatus.FILLED)
									start_filling = true;
								else if (start_filling)
								{
									if (squares[cell].GetStatus() != PuzzleSquare.SquareStatus.FILLED)
									{
										num_changed++;
										squares[cell].SetStatus(PuzzleSquare.SquareStatus.FILLED, guess_level);
									}
								}
							}
							if (count_unknowns == 0 && last_cell > 0)
							{
								if (squares[last_cell-1].GetStatus() != PuzzleSquare.SquareStatus.EMPTY)
								{
									num_changed++;
									squares[last_cell-1].SetStatus(PuzzleSquare.SquareStatus.EMPTY, guess_level);
								}								
							}							
							
							// if this Range cannot be shared to the Left as well, then
							// let's close off the rest of the Range
							if (ClueHasOneUnshareableRangeWithLeft (myClues, clue_idx))
							{
								if (count_unknowns > 0)
								{
									for (int ii=0; ii<count_unknowns; ii++)
									{
										last_cell--;
										if (last_cell >= 0)
											assert (squares[last_cell].GetStatus() == PuzzleSquare.SquareStatus.UNKNOWN);
									}
								}
								// Add EMPTYs to the rest of the range to the left
								if (last_cell > r.start)
								{
									while (last_cell > r.start)
									{
										last_cell--;
										if (squares[last_cell].GetStatus() != PuzzleSquare.SquareStatus.EMPTY)
										{
											num_changed++;
											squares[last_cell].SetStatus(PuzzleSquare.SquareStatus.EMPTY, guess_level);
										}
									}
								}
							}					
						}
					}
				}
				
				// process a Range that is sufficiently small
				int range_len = r.GetLength();
				if (range_len < clue_val*2)
				{
					int uncertainty = range_len - clue_val;
					int temp_val = clue_val - uncertainty;
					for (int i=0; i<temp_val; i++)
					{
						int cell = i+uncertainty+r.start;
						if (squares[cell].GetStatus() != PuzzleSquare.SquareStatus.FILLED)
						{
							num_changed++;
							squares[cell].SetStatus (PuzzleSquare.SquareStatus.FILLED, guess_level);
						}
					}
					
					// If no uncertainty, put X's on both ends of the Range
					if (uncertainty == 0)
					{
						if (r.start > 0)
						{
							int cell = r.start-1;
							if (squares[cell].GetStatus() != PuzzleSquare.SquareStatus.EMPTY)
							{
								num_changed++;
								squares[cell].SetStatus (PuzzleSquare.SquareStatus.EMPTY, guess_level);
							}
						}
						if (r.end < squares.length-1)
						{
							int cell = r.end+1;
							if (squares[cell].GetStatus() != PuzzleSquare.SquareStatus.EMPTY)
							{
								num_changed++;
								squares[cell].SetStatus (PuzzleSquare.SquareStatus.EMPTY, guess_level);
							}
						}
					}
				}				
			}
		}
		return squares;
	}

	private PuzzleSquare[] UpdatePuzzleFromBlobsWithMultipleClues (Clue[] myClues, 
			ArrayList<Blob> blobs, PuzzleSquare[] old_squares, int guess_level)
	{
		PuzzleSquare[] squares = new PuzzleSquare[old_squares.length];
		for (int i=0; i<old_squares.length; i++) squares[i] = new PuzzleSquare(old_squares[i]);
		
		int num_changed = 0;
		
		for (Blob b: blobs)
		{
			LinkedList<Integer> clue_list = b.clue_indices;
			if (clue_list.size() > 1)
			{
				boolean unique_clue_val = false;
				int min_clue_val = 0;
				for (int i=0; i<clue_list.size(); i++)
				{
					int clue_idx = clue_list.get(i);
					int clue_val = myClues[clue_idx].value;
					if (min_clue_val == 0)
					{
						min_clue_val = clue_val;
						unique_clue_val = true;
					} else
					{
						if (clue_val != min_clue_val) unique_clue_val = false;
						if (clue_val < min_clue_val) min_clue_val = clue_val;
					}
				}
				
				// now we can process the Range which contains this Blob
				Range r = ExtendRangeFromBlob (squares, b);
				
				// process blob for minimum clue_val if close enough to one anchored edge
				int uncertainty = min_clue_val - b.GetLength();
				if (uncertainty == 0 && unique_clue_val)
				{
					if (b.start > 0)
					{
						if (squares[b.start-1].GetStatus() != PuzzleSquare.SquareStatus.EMPTY)
						{
							num_changed++;
							squares[b.start-1].SetStatus(PuzzleSquare.SquareStatus.EMPTY, guess_level);		
						}
					}
					if (b.end < squares.length-1)
					{
						if (squares[b.end+1].GetStatus() != PuzzleSquare.SquareStatus.EMPTY)
						{
							num_changed++;
							squares[b.end+1].SetStatus(PuzzleSquare.SquareStatus.EMPTY, guess_level);
						}
					}
				} else if (uncertainty < min_clue_val)
				{
					if ((b.start - r.start) <= uncertainty && IsRangeAnchoredAtStart(squares, r))
					{
						int temp_clue = min_clue_val - uncertainty;
						if (temp_clue > b.GetLength())
						{
							boolean squares_filled_now = false;
							for (int idx=0; idx<min_clue_val; idx++)
							{
								int cell = r.start+idx;
								if (squares[cell].GetStatus() == PuzzleSquare.SquareStatus.FILLED)
									squares_filled_now = true;
								if (squares[cell].GetStatus() == PuzzleSquare.SquareStatus.UNKNOWN && squares_filled_now)
								{
									num_changed++;
									squares[cell].SetStatus(PuzzleSquare.SquareStatus.FILLED);
								}
							}
						}
					}
					if ((r.end - b.end) <= uncertainty && IsRangeAnchoredAtEnd (squares, r))
					{
						int temp_clue = min_clue_val - uncertainty;
						if (temp_clue > b.GetLength())
						{
							boolean squares_filled_now = false;
							for (int idx=0; idx<min_clue_val; idx++)
							{
								int cell = r.end-idx;
								if (squares[cell].GetStatus() == PuzzleSquare.SquareStatus.FILLED)
									squares_filled_now = true;
								if (squares[cell].GetStatus() == PuzzleSquare.SquareStatus.UNKNOWN && squares_filled_now)
								{
									num_changed++;
									squares[cell].SetStatus(PuzzleSquare.SquareStatus.FILLED);
								}
							}
						}
					}
				}
			}
		}
		return squares;
	}
	
	private static int Min (int i1, int i2)
	{ return (i1 < i2 ? i1 : i2); }
	private static int Max (int i1, int i2)
	{ return (i1 > i2 ? i1 : i2); }	
	
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
	
	// Figure out which Blobs are in which Ranges for each Clue (each Clue can have a
	// different set of Ranges, especially if a wide Range had been broken up into pieces)
	private void AssociateBlobsWithRangesForEachClue (Clue[] clues, ArrayList<Blob> blobs)
	{
		for (int i=0; i<clues.length; i++)
		{
			Clue clue = clues[i];
			
			ArrayList<Range> ranges = clue.actualRanges;
			
			for (Range r : ranges)
				r.GetBlobsInRange(blobs);
		}
	}
	
	// For each Blob, we now have a list of which Clues it may be associated with
	private void AssociateBlobsWithClues (Clue[] clues, ArrayList<Blob> blobs)
	{
		for (Blob b : blobs)
		{
			int my_b_index = blobs.indexOf(b);
			
			b.clue_indices = new LinkedList();
			
			boolean not_added_to_list;
			
			// For each Clue, see if any of its Ranges contains the Blob b
			for (int idx=0; idx<clues.length; idx++)
			{
				not_added_to_list = true;
				Clue clue = clues[idx];
				for (Range r: clue.actualRanges)
				{
					for (Integer bi : r.blob_indices)
					{
						if (bi.intValue() == my_b_index && not_added_to_list)
						{
							b.clue_indices.add(idx);
							not_added_to_list = false;
						}
					}
				}
			}
		}
	}
	
	private void CleanUpBlobAssociationsWithClues (Clue[] clues, ArrayList<Blob> blobs)
	{
		boolean[] clue_taken = new boolean[clues.length];
		for (int i=0; i<clue_taken.length; i++) clue_taken[i] = false;
		
		// from left to right
		for (int ib=0; ib<blobs.size(); ib++)
		{
			Blob b = blobs.get(ib);
			
			if (b.clue_indices.size() == 1)
			{
				int clue_idx = b.clue_indices.get(0);
				clue_taken[clue_idx] = true;
				
				// remove this index from all other Blobs to the right
				if (ib < (blobs.size()-1))
				{
					for (int ibb=ib+1; ibb<blobs.size(); ibb++)
					{
						Blob blob_to_right = blobs.get(ibb);
						for (int LE_clue_idx=0; LE_clue_idx<=clue_idx; LE_clue_idx++)
						{
							int indx_of_val = GetIndexInLinkedListOfValue (blob_to_right.clue_indices, LE_clue_idx);
							if (indx_of_val >= 0) blob_to_right.clue_indices.remove(indx_of_val);
						}
					}
				}
			}
		}
		
		// From right to left
		for (int ib=blobs.size()-1; ib>=0; ib--)
		{
			Blob b = blobs.get(ib);
			
			if (b.clue_indices.size() == 1)
			{
				int clue_idx = b.clue_indices.get(0);
				clue_taken[clue_idx] = true;
				
				// remove this index from all other Blobs to the left
				if (ib > 0)
				{
					for (int ibb=ib-1; ibb>=0; ibb--)
					{
						Blob blob_to_left = blobs.get(ibb);
						for (int GE_clue_idx=clue_idx; GE_clue_idx<clues.length; GE_clue_idx++)
						{
							int indx_of_val = GetIndexInLinkedListOfValue (blob_to_left.clue_indices, GE_clue_idx);
							if (indx_of_val >= 0) blob_to_left.clue_indices.remove(indx_of_val);
						}
					}
				}
			}
		}
		
		// Initialize all SingleBlob associations with all Clues
		for (Clue c: clues)
			c.SetSingleBlob(null);
		
		// Now check one more time to see which Blobs have unique associations with which Clues
		for (int ib=0; ib<blobs.size(); ib++)
		{
			Blob b = blobs.get(ib);
			if (b.clue_indices.size() == 1)
			{
				int clue_idx = b.clue_indices.get(0);
				clue_taken[clue_idx] = true;
				
				clues[clue_idx].SetSingleBlob (b);
			}
		}
	}
	
	private void EliminateRangesForSingleBlobs (Clue[] clues)
	{
		for (int ic=0; ic<clues.length; ic++)
		{
			Clue c = clues[ic];
			if (c.single_blob != null)
			{
				ArrayList<Range> newRanges = new ArrayList();
				for (Range r: c.actualRanges)
				{
					if (BlobInRange (c.single_blob, r))
						newRanges.add(r);
				}
				c.actualRanges = newRanges;
			}
		}
	}
	
	private static int GetIndexInLinkedListOfValue (LinkedList<Integer> list, int look_for_val)
	{
		for (Integer val : list)
			if (val.intValue() == look_for_val) return list.indexOf(val);
		return -1;
	}
	
	private static boolean BlobInRange (Blob b, Range r)
	{ return (b.start >= r.start && b.end <= r.end); }
	
	private ArrayList<Blob> GetBlobsInRange (Range r, ArrayList<Blob> blobs)
	{
		ArrayList<Blob> newBlobs = new ArrayList();
		for (Blob b : blobs)
		{
			if (BlobInRange (b, r))
				newBlobs.add(b);
		}
		return newBlobs;
	}
	
	private ArrayList<Range> EliminateBlobFromRange (Range r, Blob b)
	{
		ArrayList<Range> ranges = new ArrayList();
		
		// We need to add 1 extra space around the blob before removing
		
		// See if we have a Range to the right of the Blob
		int new_start = r.start;
		int new_end = b.start - 2;
		if (new_end >= new_start)
			ranges.add(new Range (new_start, new_end));
		
		// See if we have a range to the left of the Blob
		new_start = b.end+2;
		new_end =r.end;
		if (new_end >= new_start)
			ranges.add(new Range (new_start, new_end));
		
		return ranges;

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
					}
					blob_list.add(new Blob (start, end, is_anchored_start, is_anchored_end,
						is_fixed_start, is_fixed_end));				
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
			blob_list.add(new Blob (start, end, is_anchored_start, is_anchored_end,
				is_fixed_start, is_fixed_end));
		}
		
		return blob_list;
	}
	
	private ArrayList<Mystery> GetAllMysteries (PuzzleSquare[] squares)
	{
		ArrayList<Mystery> empty_list = new ArrayList();
		
		int start = 0;
		int end = squares.length-1;
		boolean mystery_open = false;
		for (int i=0; i<squares.length; i++)
		{
			// square is UNKNOWN
			if (squares[i].GetStatus() == PuzzleSquare.SquareStatus.UNKNOWN)
			{
				if (!mystery_open)
				{
					// check if previous square was EMPTY
					if (i==0 || (i>0 && squares[i-1].GetStatus() == PuzzleSquare.SquareStatus.EMPTY))
					{
						if (!mystery_open) start = i;
						mystery_open = true;
						end = i;
					}
				} else end = i;
				
			} else if (squares[i].GetStatus() == PuzzleSquare.SquareStatus.FILLED)
			{
				// NOT a Mystery if not surrounded by EMPTYs only
				mystery_open = false;
				
			} else // square is EMPTY *and* a Mystery is already started
			{
				if (mystery_open)
				{
					empty_list.add(new Mystery (start, end));
					mystery_open = false;
				}
			}
		}
		if (mystery_open)
			empty_list.add(new Mystery (start, end));
		
		return empty_list;
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