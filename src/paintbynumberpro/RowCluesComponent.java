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
public class RowCluesComponent extends JComponent
{
    private PBNHandler myDrawHandler = null;
    private int[] clues = null;
    private int num_clues = 0;
    private int row = -1;

    private static Color backgroundColor = new Color (189, 223, 255);    

    /** Creates a new instance of Magnifier */
    public RowCluesComponent (PBNHandler theHandler)
    {
        myDrawHandler = theHandler;
        InitializeDimensionsForNumClues (1);
        setOpaque(true);
    }
	
	public void ReinitializeDimensions ()
	{
		InitializeDimensionsForNumClues (num_clues);
	}
        
    private void InitializeDimensionsForNumClues (int num_clues)
    {
        Dimension d = new Dimension (num_clues*myDrawHandler.GetClueWidth()+4, myDrawHandler.GetBoxSize());
        setSize (d);
        setMaximumSize(d);
        setMinimumSize (d);
        setPreferredSize (d);
    }

    public void SetForRow (int r)
    {
        row = r;
        clues = null;
        num_clues = myDrawHandler.GetThePuzzle().GetRow_NClues(row);
        if (num_clues <= 0) return;
        InitializeDimensionsForNumClues (num_clues);
        clues = myDrawHandler.GetThePuzzle().GetRow_Clues(row);
    }

    public void SetPuzzleDrawHandler (PBNHandler theHandler)
    { myDrawHandler = theHandler; }

    @Override public synchronized void paintComponent (Graphics g)
    {
		Graphics2D g2D = (Graphics2D)g;
        Font f = myDrawHandler.GetCurrentFont();
		g2D.setFont(f);
		g2D.setStroke(new BasicStroke(1));
        
        g.setColor (backgroundColor);
        Dimension d = this.getSize();
        g.fillRect (0, 0, d.width, d.height);

        g.setColor (Color.black);
        g.drawRect (0, 0, d.width-1, d.height-1);
        
        if (clues == null || clues.length < 1) return;
        
        int clue_width = myDrawHandler.GetClueWidth();
        int box_size = myDrawHandler.GetBoxSize();   

        int x = 2;
        int y = box_size - 2;
        int bx = 2;
        int by = 2;
        
        for (int i=0; i<num_clues; i++)
        {
            int clue;
            String strClue;
            Integer iClue;
            int status;
            Selection cs = myDrawHandler.GetThePuzzle().GetCurrentSelection ();
            if (cs.isRowClueSelected(row, i))
                g.setColor(myDrawHandler.GetSelectColor());
            else
                g.setColor(backgroundColor);
            g.fillRect(bx+1, by, clue_width-4, box_size-3);
            clue = clues[i];
            status = myDrawHandler.GetThePuzzle().GetRow_Clue_Status(row, i);
            iClue = new Integer(clue);
            Color c = myDrawHandler.GetColorForClueStatus(status);
            g.setColor(c);
            strClue = iClue.toString();
            g.drawString(strClue, x, y);
            g.setColor(Color.black);
            x += clue_width;
            bx += clue_width;
        }
    }
    
    public final void MoveToUpperRightLocationAtEdge (int upperright_x, int upperright_y, int edge_x)
    {
        Dimension d = this.getSize();
        int x = upperright_x - d.width;
        setVisible(false);
        if (x > edge_x) 
        {
            PBNHandler.GetMessageWindow().AddMessage("RowCluesComponent->MoveToUpperRightLocationAtEdge() set INVISIBLE");
//            setVisible(false);
        } else
        {
            if (x < edge_x) x = edge_x;
            setLocation (x, upperright_y);
            PBNHandler.GetMessageWindow().AddMessage("RowCluesComponent->MoveToUpperRightLocationAtEdge() set VISIBLE");
            setVisible(true);
        }
    }
    
}
