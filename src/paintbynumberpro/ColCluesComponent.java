/*
 * Magnifier.java
 *
 * Created on November 5, 2007, 10:00 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package paintbynumberpro;

import java.awt.*;
import javax.swing.*;
/**
 *
 * @author Lynne
 */
public class ColCluesComponent extends JComponent
{
    private PBNHandler myDrawHandler = null;
    private int[] clues = null;
    private int num_clues = 0;
    private int col = -1;
//    private boolean isVisible = false;
//    private Dimension myDimensions = null;
//    private int ul_x = 0;
//    private int ul_y = 0;
    
    private static Color backgroundColor = new Color (189, 223, 255);
    
    /** Creates a new instance of Magnifier */
    public ColCluesComponent (PBNHandler theHandler)
    {
        myDrawHandler = theHandler;
        InitializeDimensionsForNumClues (1);
        setOpaque(true);
    }
    
    private void InitializeDimensionsForNumClues (int num_clues)
    {
        Dimension myDimensions = new Dimension (myDrawHandler.GetBoxSize(), num_clues*myDrawHandler.GetClueHeight());
        setSize (myDimensions);
        setMaximumSize(myDimensions);
        setMinimumSize (myDimensions);
        setPreferredSize (myDimensions);
    }

    public void SetForCol (int c)
    {
        col = c;
        clues = null;
        num_clues = myDrawHandler.GetThePuzzle().GetCol_NClues(col);
        if (num_clues <= 0) return;
        InitializeDimensionsForNumClues (num_clues);
        clues = myDrawHandler.GetThePuzzle().GetCol_Clues(col);
    }
    
//    public void setVisible (boolean isVis)
//    { isVisible = isVis; }
//    public boolean isVisible ()
//    { return isVisible; }

    public void SetPuzzleDrawHandler (PBNHandler theHandler)
    { myDrawHandler = theHandler; }

    @Override
    public synchronized void paintComponent (Graphics g)
    {
//        if (!isVisible) 
//        {
//            myDrawHandler.GetMessageWindow().AddMessage ("ColCluesComponent->paintComponent() drawing NOTHING");
//            return;
//        }
        
//        myDrawHandler.GetMessageWindow().AddMessage ("ColCluesComponent->paintComponent() drawing at ul x y " + ul_x + " " + ul_y);
            
		Graphics2D g2D = (Graphics2D)g;
        Font f = myDrawHandler.GetCurrentFont();
		g2D.setFont(f);
		g2D.setStroke(new BasicStroke(1));
        
        g.setColor (backgroundColor);
        Dimension d = this.getSize();
//        Dimension d = myDimensions;
        int ul_x = 0;
        int ul_y = 0;
        g.fillRect (ul_x, ul_y, d.width, d.height);

        g.setColor (Color.black);
        g.drawRect (ul_x, ul_y, d.width-1, d.height-1);
        
        if (clues == null || clues.length < 1) return;
        
        int clue_height = myDrawHandler.GetThePuzzle().GetClue_Height();
        int box_size = myDrawHandler.GetBoxSize();

        int x = ul_x + 2;
        int y = ul_y + clue_height - 2;
        int bx = 2;
        int by = 2;
        
        for (int i=0; i<num_clues; i++)
        {
            int clue;
            String strClue;
            Integer iClue;
            int status;
            Selection cs = myDrawHandler.GetThePuzzle().GetCurrentSelection ();
            if (cs.isColClueSelected(col, i))
                g.setColor(myDrawHandler.GetSelectColor());
            else
                g.setColor(backgroundColor);
            g.fillRect(bx+1, by, box_size-4, clue_height-3);
            clue = clues[i];
            status = myDrawHandler.GetThePuzzle().GetCol_Clue_Status(col, i);
            iClue = new Integer(clue);
            Color c= myDrawHandler.GetColorForClueStatus(status);
            g.setColor(c);
            strClue = iClue.toString();
            g.drawString(strClue, x, y);
            g.setColor(Color.black);      
            y += clue_height;
            by += clue_height;
        }    
    }
    
    public final void MoveToTopOfVisRect (int lowerleft_x, int lowerleft_y, int min_y)
    {
        Dimension d = this.getSize();
        int y_bottom = min_y + d.height;
        // if bottom of component is < col_clue_rect.y+col_clue_rect.height, then
        // make the component disappear
        Rectangle col_clue_rect = myDrawHandler.GetColClueRect();
        if (y_bottom < (col_clue_rect.y+col_clue_rect.height))
        {
            PBNHandler.GetMessageWindow().AddMessage ("ColCluesComponent->MoveToLowerLeftLocationAtEdge() set INVISIBLE");
            setVisible (false);
        } else
        {
            setLocation (lowerleft_x, min_y);
            PBNHandler.GetMessageWindow().AddMessage("ColCluesComponent->MoveToLowerLeftLocationAtEdge() set VISIBLE");                
            setVisible(true);
        }
    }
    
//    private void setLocation (int x, int y)
//    {
//        ul_x = x;
//        ul_y = y;
//    }    
}
