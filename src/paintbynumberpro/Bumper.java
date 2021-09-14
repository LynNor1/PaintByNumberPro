/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package paintbynumberpro;

/**
 *
 * @author user
 */

// What is a bumper?  It's a description of the start and end of a continuous
// sequence of unknown and filled spaces with a description of the start
// and end indices of the bumper and the number of filled spaces, if any

public class Bumper {

    private int start_index = -1;
    private int end_index = -1;
    private int num_filled = 0;
    private int id = 0;

    Bumper (int start, int end, int filled)
    {
        start_index = start;
        end_index = end;
        num_filled = filled;
    }

    public void SetID (int num)
    { id = num; }

    public void SetStartIndex (int index)
    { start_index = index; }

    public void SetEndIndex (int index)
    { end_index = index; }

    public void SetNumFilled (int num)
    { num_filled = num; }

    public int GetStartIndex () { return start_index; }
    public int GetEndIndex () { return end_index; }
    public int GetLength ()
    {
        if (end_index >= start_index)
            return end_index - start_index + 1;
        else
            return start_index - end_index + 1;
    }
    public int GetNumFilled () { return num_filled; }
    public int GetID () { return id; }

}
