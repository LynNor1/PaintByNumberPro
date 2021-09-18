package paintbynumberpro;

public class Selection extends Object {

	private int selectedRow;
	private int selectedCol;
	private int selClue_Row;
	private int selClue_Col;
	private int selClue_Num;
	private int status;
	
	Selection ()
	{
		selectedRow = -1;
		selectedCol = -1;
		selClue_Row = -1;
		selClue_Col = -1;
		selClue_Num = -1;
		status = 0;
	}
	
	Selection (int row, int col)
	{
		this();
		selectedRow = row;
		selectedCol = col;
	}
	
	Selection (int Clue_Row, int Clue_Col, int Clue_Num)
	{
		this();
		selClue_Row = Clue_Row;
		selClue_Col = Clue_Col;
		selClue_Num = Clue_Num;
	}
	
	Selection (Selection aSelection)
	{
		selectedRow = aSelection.selectedRow;
		selectedCol = aSelection.selectedCol;
		selClue_Row = aSelection.selClue_Row;
		selClue_Col = aSelection.selClue_Col;
		selClue_Num = aSelection.selClue_Num;
		status = aSelection.status;
	}
	
	public boolean isEqual (Selection thisSelection)
	{
		if (selectedRow != thisSelection.selectedRow) return false;
		if (selectedCol != thisSelection.selectedCol) return false;
		if (selClue_Row != thisSelection.selClue_Row) return false;
		if (selClue_Col != thisSelection.selClue_Col) return false;
		if (selClue_Num != thisSelection.selClue_Num) return false;
		// ignore status as this function is to see if a new
		// item has been selected
		return true;
	}
	
	public boolean isExactlyEqual (Selection thisSelection)
	{
		if (selectedRow != thisSelection.selectedRow) return false;
		if (selectedCol != thisSelection.selectedCol) return false;
		if (selClue_Row != thisSelection.selClue_Row) return false;
		if (selClue_Col != thisSelection.selClue_Col) return false;
		if (selClue_Num != thisSelection.selClue_Num) return false;
		if (status      != thisSelection.status     ) return false;
		return true;
	}
	
	public boolean isBoxSelected (int row, int col)
	{
		return (selectedRow == row && selectedCol == col);
	}
	
	public boolean isColClueSelected (int col, int clue)
	{
		return (clue == selClue_Num && col == selClue_Col);
	}
	
	public boolean isRowClueSelected (int row, int clue)
	{
		return (clue == selClue_Num && row == selClue_Row);
	}
	
	public boolean isColClueSelected (int col)
	{
		return (col == selClue_Col);
	}
	
	public boolean isRowClueSelected (int row)
	{
		return (row == selClue_Row);
	}

    public boolean isAClueSelected ()
    {
        return (selectedRow == -1 || selectedCol == -1);
    }

    public boolean isAPuzzleSquareSelected ()
    {
        return (selectedRow >= 0 && selectedCol >= 0);
    }
	
	public void setRowColSelected (int row, int col, int status)
	{
		selectedRow = row;
		selectedCol = col;
		selClue_Row = -1;
		selClue_Col = -1;
		selClue_Num = -1;
		this.status = status;
	}
	
	public void setColClueSelected (int col, int clue, int status)
	{
		selectedRow = -1;
		selectedCol = -1;
		selClue_Row = -1;
		selClue_Col = col;
		selClue_Num = clue;
		this.status = status;
	}

	public void setRowClueSelected (int row, int clue, int status)
	{
		selectedRow = -1;
		selectedCol = -1;
		selClue_Row = row;
		selClue_Col = -1;
		selClue_Num = clue;
		this.status = status;
	}
	
	public int getRowSelected ()
	{
		return selectedRow;
	}
	
	public int getColSelected ()
	{
		return selectedCol;
	}
	
	public int getClueColSelected ()
	{
		return selClue_Col;
	}
	
	public int getClueRowSelected ()
	{
		return selClue_Row;
	}
	
	public int getClueNumSelected ()
	{
		return selClue_Num;
	}
	
	public int getStatus()
	{
		return status;
	}
	
	public void setNothingSelected ()
	{
		selectedRow = -1;
		selectedCol = -1;
		selClue_Row = -1;
		selClue_Col = -1;
		selClue_Num = -1;
		status = 0;
	}
	
	public boolean somethingSelected()
	{
		boolean selected = 
			selectedRow != -1 || selectedCol != -1 ||
			selClue_Row != -1 || selClue_Col != -1 ||
			selClue_Num != -1;
		return selected;
	}
	
	public void setStatus(int status)
	{
		this.status = status;
	}

    public void copySelection (Selection s)
    {
        if (s == null) return;
        selClue_Col = s.selClue_Col;
        selClue_Row = s.selClue_Row;
        selClue_Num = s.selClue_Num;
        selectedCol = s.selectedCol;
        selectedRow = s.selectedRow;
        status = s.status;
    }
	
	public String toString()
	{
		return ("Selection " + selectedRow + " " + selectedCol + " " +
			selClue_Row + " " + selClue_Col + " " + selClue_Num + " " + status);
	}

    public void UpdateStatusFromPuzzle(PBNPuzzle thePuzzle)
    {
        if (selectedRow != -1 && selectedCol != 1)
            status = thePuzzle.GetSquareStatusAsInt (selectedCol, selectedRow);
        else if (selClue_Row != -1)
            status = thePuzzle.GetRowClueStatus (selClue_Row, selClue_Num);
        else if (selClue_Col != -1)
            status = thePuzzle.GetColClueStatus (selClue_Col, selClue_Num);
    }
}
