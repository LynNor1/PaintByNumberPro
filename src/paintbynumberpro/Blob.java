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
// sequence of filled spaces with a description of the start
// and end indices of the bumper

public class Blob {

    private int start_index = -1;
    private int end_index = -1;
    private int id = 0;

    Blob (int start, int end)
    {
        start_index = start;
        end_index = end;
    }

    public void SetID (int num)
    { id = num; }

    public void SetStartIndex (int index)
    { start_index = index; }

    public void SetEndIndex (int index)
    { end_index = index; }

    public int GetStartIndex () { return start_index; }
    public int GetEndIndex () { return end_index; }
    public int GetLength ()
    {
        if (end_index >= start_index)
            return end_index - start_index + 1;
        else
            return start_index - end_index + 1;
    }
    public int GetID () { return id; }

}
