/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package paintbynumberpro;

import javax.swing.*;
import java.awt.*;
import java.io.Serializable;

/**
 *
 * @author user
 */
public class ProgressBarComponent extends JComponent implements Serializable {

    PBNPuzzle myPuzzle = null;
    private static int MIN_WIDTH = 100;
    private static int MAX_WIDTH = 200;
    private static int PREF_HEIGHT = 20;

    ProgressBarComponent ()
    {
        super();
        this.setMinimumSize(new Dimension (MIN_WIDTH, PREF_HEIGHT));
        this.setMaximumSize(new Dimension (MAX_WIDTH, PREF_HEIGHT));
        this.setPreferredSize (new Dimension ((MIN_WIDTH+MAX_WIDTH)/2, PREF_HEIGHT));
    }

    public PBNPuzzle getPuzzle()
    { return myPuzzle; }

    public void setPuzzle (PBNPuzzle thePuzzle)
    { myPuzzle = thePuzzle; }

    public void paintComponent (Graphics g)
    {
        super.paintComponent(g);

        Graphics2D g2D;
        if (g instanceof Graphics2D) g2D = (Graphics2D)g;
        else			     return;

        // Draw bounding rectangle
        Rectangle theRect = this.getBounds();
		g2D.setColor(Color.black);
		g2D.drawRect(theRect.x, theRect.y, theRect.width, theRect.height);
        
        // we're done if there's no puzzle
        if (myPuzzle == null) return;

        // Precalculate width of component and pixels per puzzle square
        float full_width = (float)theRect.width;
        float total_squares = (float)(myPuzzle.GetCols() * myPuzzle.GetRows());
        float pix_per_sq = full_width / total_squares;

        // Get max # of guess levels
        int max_guess = myPuzzle.GetMaxGuessLevelWithKnownSquares();

        // Loop over each guess level to get # of squares and draw into progress bar
        int y = 0;
        int x = theRect.x;
        int x_end;
        int tot_sqs = 0;
        for (int guess=0; guess<max_guess; guess++)
        {
            int num_sqs = myPuzzle.CountKnownSquares(guess);
            tot_sqs += num_sqs;
            x_end = Math.round ((float)tot_sqs * pix_per_sq);
            if (x_end > x)
            {
                Color guesscolor = PaintByNumberPro.GetDrawHandler().GetGuessColor (guess);
                g2D.setColor (guesscolor);
                g2D.fillRect (x, theRect.y, x_end-x, theRect.height);
                x = x_end;
            }
        }
    }

}
