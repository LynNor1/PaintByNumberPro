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
public class BlobUtilities {

    public static ArrayList<Blob> ExtractBlobsFromListForward (PuzzleSquare[] myList, int start)
    {
        if (myList == null) return null;
        if (start >= myList.length) return null;

        ArrayList<Blob> list = new ArrayList<Blob>();

        int start_blob = -1;
        int end_blob = -1;
        int index = start;
        boolean finished = false;

        while (!finished)
        {
            while (index < myList.length && !myList[index].IsFilled()) index++;
            if (index == myList.length) 
            {
                if (list.isEmpty()) return null;
                else return list;
            }

            start_blob = index;

            while (index < myList.length && myList[index].IsFilled()) index++;
            if (index == myList.length) finished = true;

            end_blob = index-1;

            Blob b = new Blob(start_blob, end_blob);
            list.add(b);

        }

        return list;
    }

    public static ArrayList<Blob> ExtractBlobsFromListBackward (PuzzleSquare[] myList, int start)
    {
        if (myList == null) return null;
        if (start < 0) return null;

        ArrayList<Blob> list = new ArrayList<Blob>();

        int start_blob = -1;
        int end_blob = -1;
        int index = start;
        boolean finished = false;

        while (!finished)
        {
            while (index >= 0 && !myList[index].IsFilled()) index--;
            if (index < 0)
            {
                if (list.isEmpty()) return null;
                else return list;
            }

            start_blob = index;

            while (index >= 0 && myList[index].IsFilled()) index--;
            if (index < 0) finished = true;

            end_blob = index+1;

            Blob b = new Blob(start_blob, end_blob);
            list.add(b);
        }

        return list;
    }

}
