/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package paintbynumberpro;

/**
 *
 * @author user
 */
public class PuzzleSquare {

    // Stuff for converting square status to int and back
	public static final int UNKNOWN = 0;		// set puzzle[][] to 0 if status
										// of box is unknown
	private static final int KNOWN = 1;			// puzzle[][] to +/-1 if status is
										// known
	private static final int FILLED_SIGN = 1;	// sign of puzzle[][] indicates
	private static final int EMPTY_SIGN = -1;	// whether box is filled or empty
	private static final int MARKED = 500;		// puzzle[][] +/- MARKED indicates
										// box is marked
										// Note: puzzle[][] > +/- 1
										// indicates guess level starting
										// with 1 (i.e. puzzle[][] = +/-2
										// for guess level 1)
    private static final int SPECIAL_MARKED = 1000;     // puzzle[][] +/- SPECIAL_MARKED indicates
                                                        // this was first puzzle square when a new
                                                        // guess was started

    // Enum for status of square
    public enum SquareStatus { UNKNOWN, FILLED, EMPTY };

    // Internal state of the square
    private boolean is_special_marked = false;	// see comments for MARKED and SPECIAL_MARKED above
    private boolean is_marked = false;
    private int guess_level = 0;
    private SquareStatus status = SquareStatus.UNKNOWN;
	
	// Only used by BetterPuzzleSolver.Better_CanSolutionFit()
	public int clue_index = 0;

    PuzzleSquare (int fromStatus)
    {
        SetStatusFromInt (fromStatus);
    }

    PuzzleSquare (SquareStatus startingStatus)
    {
        status = startingStatus;
    }

    PuzzleSquare (PuzzleSquare cloneFrom)
    {
        if (cloneFrom != null) CloneStatusFromSquare (cloneFrom);
    }

    public static int StatusToInt (SquareStatus ss, boolean is_special_marked, boolean is_marked,
            int guess_level)
    {
        int ival = UNKNOWN; // 0
        if (ss == SquareStatus.FILLED || ss == SquareStatus.EMPTY)
            ival = KNOWN + guess_level;
        if (is_special_marked) ival += SPECIAL_MARKED;
        if (is_marked) ival += MARKED;
        if (ss == SquareStatus.EMPTY) ival *= EMPTY_SIGN;
        return ival;
    }

    public static int StatusToInt (PuzzleSquare ps)
    {
        int ival = UNKNOWN;     // 0

        if (ps.IsFilled() || ps.IsEmpty()) ival = KNOWN + ps.GetGuessLevel();

        if (ps.IsSpecialMarked()) ival += SPECIAL_MARKED;
        if (ps.IsMarked()) ival += MARKED;

        if (ps.IsEmpty()) ival *= EMPTY_SIGN;

        return ival;
    }

    public static PuzzleSquare StatusFromInt (int ival)
    {
        PuzzleSquare ps = new PuzzleSquare(SquareStatus.UNKNOWN);
        ps.SetStatusFromInt (ival);
        return ps;
    }

    public int GetStatusAsInt ()
    { return PuzzleSquare.StatusToInt (this); }

    public void CloneStatusFromSquare (PuzzleSquare ps)
    {
        is_special_marked = ps.IsSpecialMarked();
        is_marked = ps.IsMarked();
        guess_level = ps.GetGuessLevel();
        status = ps.GetStatus();
    }

    public void SetStatusFromInt (int ival)
    {
        // get sign and remove it
        int sgn = FILLED_SIGN;
        if (ival < 0) sgn = EMPTY_SIGN;

        int tmp = ival*sgn;

        // check if special-marked
        if (tmp >= SPECIAL_MARKED)
        {
            SetSpecialMarked(true);
            tmp -= SPECIAL_MARKED;
        } else
			SetSpecialMarked(false);

        // check if marked
        if (tmp >= MARKED)
        {
            SetMarkedStatus(true);
            tmp -= MARKED;
        } else
			SetMarkedStatus(false);

        // extract the guess level
        if (tmp != UNKNOWN)
        {
            SetGuessLevel (tmp - KNOWN);
            if (sgn == FILLED_SIGN) SetStatus(SquareStatus.FILLED);
            else SetStatus(SquareStatus.EMPTY);
        }

        // set status
        else SetStatus (SquareStatus.UNKNOWN);
    }

    public boolean IsSpecialMarked () { return is_special_marked; }
    public boolean IsMarked () { return is_marked; }
    public boolean IsGuess () { return guess_level > 0; }
    public boolean IsUnknown () { return status == SquareStatus.UNKNOWN; }
    public boolean IsFilled () { return status == SquareStatus.FILLED; }
    public boolean IsEmpty () { return status == SquareStatus.EMPTY; }

    public SquareStatus GetStatus () { return status; }
    public int GetGuessLevel () { return guess_level; }
    public String GetStatusName ()
    {
        if (status == SquareStatus.UNKNOWN) return "UNKNOWN";
        else if (status == SquareStatus.EMPTY) return "EMPTY";
        else return "FILLED";
    }

    public void SetSpecialMarked (boolean stat) { is_special_marked = stat; }
    public void SetMarkedStatus (boolean stat) { is_marked = stat; }
    public void SetGuessLevel (int level) { guess_level = level; }
    public void SetNotAGuess () { guess_level = 0; }
    public void SetStatus (SquareStatus stat) { status = stat; }
    public void SetStatus (SquareStatus stat, int level)
    {
        status = stat;
        guess_level = level;
    }
	public void SetStatus (SquareStatus stat, int level, int clue_idx)
	{
		status = stat;
		guess_level = level;
		clue_index = clue_idx;
	}
    
    public void Reset ()
    {
        is_special_marked = false;
        is_marked = false;
        guess_level = 0;
        status = SquareStatus.UNKNOWN;
    }

    public void ToggleSpecialMarked () { is_special_marked = !is_special_marked; }
    public void ToggleMarked () { is_marked = !is_marked; }
    public void CycleStatus ()
    {
        SquareStatus new_stat;
        if (status == SquareStatus.UNKNOWN)
            new_stat = SquareStatus.FILLED;
        else if (status == SquareStatus.FILLED)
            new_stat = SquareStatus.EMPTY;
        else
            new_stat = SquareStatus.UNKNOWN;
        status = new_stat;
    }

    public void IncrementGuessLevel () { guess_level++; }
    public void DecrementGuessLevel ()
    {
        guess_level--;
        if (guess_level < 0) guess_level = 0;
    }

    public static int StripSpecialMarkedMarkedGuess (PuzzleSquare ps)
    {
        return StatusToInt (ps.GetStatus(), false, false, 0);
    }

    public static int StripMarked (PuzzleSquare ps)
    {
        return StatusToInt (ps.GetStatus(), ps.IsSpecialMarked(), false, ps.GetGuessLevel());
    }
    
    public String toString (boolean simple)
    {
        String str;
        if (status == SquareStatus.EMPTY) str = "X";
        else if (status == SquareStatus.FILLED) str = "O";
        else str = "_";
        if (!simple)
        {
            if (guess_level > 0)
                str += "(" + guess_level + ")";
            if (is_marked)
                str += "M";
            else if (is_special_marked)
                str += "S";
        }
        return str; 
    }

    @Override public String toString ()
    {
        return toString (false);
    }

}
