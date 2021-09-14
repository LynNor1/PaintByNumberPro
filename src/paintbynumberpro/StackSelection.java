package paintbynumberpro;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author user
 */
public class StackSelection {

    private Selection selection;
    private int priorStatus;        // Integer form of a square's status
    private int ID;
    static private int lastID = 0;

    StackSelection()
    {
        selection = null;
        priorStatus = PuzzleSquare.StatusToInt(new PuzzleSquare(PuzzleSquare.SquareStatus.UNKNOWN));
        ID = incrID();
    }

    StackSelection (Selection theSelection, int status, boolean newID)
    {
        selection = new Selection(theSelection);
        priorStatus = status;
        if (newID) ID = incrID();
        else ID = lastID;
    }

    private int incrID()
    {
        lastID++;
        return lastID;
    }

    public Selection getSelection ()
    { return selection; }

    public int getPriorStatus ()
    { return priorStatus; }

    public int getID ()
    { return ID; }
    
    public boolean isExactlyEqual (StackSelection compareStackSelection)
    {
        Selection compareSelection = compareStackSelection.getSelection();
        if (compareSelection.isExactlyEqual(selection))
        {
            if (priorStatus == compareStackSelection.getPriorStatus() &&
                ID == compareStackSelection.getID()) return true;
        }
        return false;
    }

}
