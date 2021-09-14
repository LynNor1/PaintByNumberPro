/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package paintbynumberpro;

import java.util.ArrayList;

/**
 *
 * @author user
 */
public class BumperUtilities {

    public static ArrayList<Bumper> ExtractBumpersFromListForward (PuzzleSquare[] myList, int start)
    {
        if (myList == null) return null;
        if (start >= myList.length) return null;

        ArrayList<Bumper> list = new ArrayList<Bumper>();

        int start_bumper = -1;
        int end_bumper = -1;
        int index = start;
        boolean finished = false;

        while (!finished)
        {
            while (index < myList.length && myList[index].IsEmpty()) index++;
            if (index == myList.length) 
            {
                if (list.isEmpty()) return null;
                else return list;
            }

            start_bumper = index;

            int num_filled = 0;
            while (index < myList.length && !myList[index].IsEmpty())
            {
                if (myList[index].IsFilled()) num_filled++;
                index++;
            }
            if (index == myList.length) finished = true;

            end_bumper = index-1;

            Bumper b = new Bumper(start_bumper, end_bumper, num_filled);
            list.add(b);

        }

        return list;
    }

    public static ArrayList<Bumper> ExtractBumpersFromListBackward (PuzzleSquare[] myList, int start)
    {
        if (myList == null) return null;
        if (start < 0) return null;

        ArrayList<Bumper> list = new ArrayList<Bumper>();

        int start_bumper = -1;
        int end_bumper = -1;
        int index = start;
        boolean finished = false;

        while (!finished)
        {
            while (index >= 0 && myList[index].IsEmpty()) index--;
            if (index < 0)
            {
                if (list.isEmpty()) return null;
                else return list;
            }

            start_bumper = index;

            int num_filled = 0;
            while (index >= 0 && !myList[index].IsEmpty())
            {
                if (myList[index].IsFilled()) num_filled++;
                index--;
            }
            if (index < 0) finished = true;

            end_bumper = index+1;

            Bumper b = new Bumper(start_bumper, end_bumper, num_filled);
            list.add(b);
        }

        return list;
    }

}
