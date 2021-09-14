/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package paintbynumberpro;

import javax.swing.DefaultComboBoxModel;
/**
 *
 * @author Lynne
 */
public class MyComboBoxModel extends DefaultComboBoxModel {

    PBNPuzzle myPuzzle = null;
    private int prev_list_size;

    MyComboBoxModel (PBNPuzzle thePuzzle)
    { SetPuzzle (thePuzzle); }

    public void SetPuzzle (PBNPuzzle thePuzzle)
    { myPuzzle = thePuzzle; }

    //----------------------------------------------------------------
    // methods for combo box model (for the guess level-drop-down menu
    //----------------------------------------------------------------

      @Override public Object getElementAt(int index) {
          String str;
          if (index == 0) str = "I'm Sure";
          else str = "Level "  + index;
        return str;
      }

      @Override public int getSize() {
          int new_size = 1;
          if (myPuzzle != null)
              new_size = myPuzzle.GetMaxGuessLevel() + 1;
        if (new_size < prev_list_size)
            fireIntervalRemoved (this, prev_list_size-1, prev_list_size-1);
        else if (new_size > prev_list_size)
            fireIntervalAdded (this, new_size-1, new_size-1);
        prev_list_size = new_size;
        return new_size;
      }

}
