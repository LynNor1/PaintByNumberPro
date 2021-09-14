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
public class Counter extends JComponent
{
    private int counter = 0;
    private String counterStr = "0";
    private PBNHandler myDrawHandler = null;
    
    /** Creates a new instance of Magnifier */
    public Counter (PBNHandler theHandler)
    {
        myDrawHandler = theHandler;
        Dimension d = new Dimension (20, 15);
        setSize (d);
        setMaximumSize(d);
        setMinimumSize (d);
        setPreferredSize (d);
        setOpaque(true);
    }

    public synchronized void SetCounter (int num)
    {
        counter = num;
        counterStr = Integer.toString (counter);
    }

    public void SetPuzzleDrawHandler (PBNHandler theHandler)
    { myDrawHandler = theHandler; }

    @Override public synchronized void paintComponent (Graphics g)
    {

        g.setColor (Color.white);
        Dimension d = this.getSize();
        g.fillRect (0, 0, d.width, d.height);

        g.setColor (Color.black);
        g.drawRect (0, 0, d.width-1, d.height-1);

        int x = 2;
        int y = d.height - 2;

        if (myDrawHandler != null) g.setColor (myDrawHandler.GetSideMarkerColor());
        g.drawString (counterStr, x, y);
    }
    
    public final void MoveTo (int lowerleft_x, int lowerleft_y)
    {
        Dimension d = this.getSize();
        setLocation (lowerleft_x, lowerleft_y - d.height);
    }
    
}
