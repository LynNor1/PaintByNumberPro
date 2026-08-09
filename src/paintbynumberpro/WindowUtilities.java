/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package paintbynumberpro;

import javax.swing.*;
import java.awt.*;

/**
 *
 * @author Lynne
 */
public class WindowUtilities {

    private static final int MARGIN = 20;

    public static void CenterFrame (JFrame theFrame)
    {
        theFrame.pack();

        java.awt.Dimension myFrameSize = theFrame.getSize ();
        Dimension screenSize = GetScreenSize();

        // Start with existing packed frame size
        int width = myFrameSize.width;
        int height = myFrameSize.height;
        // Make sure window sits in upper left
        int x_corner = (screenSize.width-width)/2;
        int y_corner = (screenSize.height-height)/2;
        Point clamped = ClampToScreen (x_corner, y_corner, width, height, screenSize);
        theFrame.setBounds(clamped.x, clamped.y,
            width, height);
    }

    public static void UpperLeftFrame (JFrame theFrame)
    {
        // Start with existing packed frame size
        Dimension screenSize = GetScreenSize();
        java.awt.Dimension myFrameSize = theFrame.getSize ();
        int width = myFrameSize.width;
        int height = myFrameSize.height;
        // Make sure window sits in upper left
        int x_corner = MARGIN;
        int y_corner = MARGIN;
        Point clamped = ClampToScreen (x_corner, y_corner, width, height, screenSize);
        theFrame.setBounds(clamped.x, clamped.y,
            width, height);
    }

    public static void UpperRightFrame (JFrame theFrame)
    {
        // Start with existing packed frame size
        Dimension screenSize = GetScreenSize();
        java.awt.Dimension myFrameSize = theFrame.getSize ();
        int width = myFrameSize.width;
        int height = myFrameSize.height;
        // Make sure window sits in upper left
        int x_corner = screenSize.width - width - MARGIN;
        int y_corner = MARGIN;
        Point clamped = ClampToScreen (x_corner, y_corner, width, height, screenSize);
        theFrame.setBounds(clamped.x, clamped.y,
            width, height);
    }

    public static void LowerLeftFrame (JFrame theFrame)
    {
        // Start with existing packed frame size
        Dimension screenSize = GetScreenSize();
        java.awt.Dimension myFrameSize = theFrame.getSize ();
        int width = myFrameSize.width;
        int height = myFrameSize.height;
        // Make sure window sits in upper left
        int x_corner = MARGIN;
        int y_corner = screenSize.height - height - MARGIN;
        Point clamped = ClampToScreen (x_corner, y_corner, width, height, screenSize);
        theFrame.setBounds(clamped.x, clamped.y,
            width, height);
    }

    public static void LowerRightFrame (JFrame theFrame)
    {
        // Start with existing packed frame size
        Dimension screenSize = GetScreenSize();
        java.awt.Dimension myFrameSize = theFrame.getSize ();
        int width = myFrameSize.width;
        int height = myFrameSize.height;
        // Make sure window sits in upper left
        int x_corner = screenSize.width - width - MARGIN;
        int y_corner = screenSize.height - height - MARGIN;
        Point clamped = ClampToScreen (x_corner, y_corner, width, height, screenSize);
        theFrame.setBounds(clamped.x, clamped.y,
            width, height);
    }

    public static void CenterDialog (JDialog theDialog)
    {
        theDialog.pack();

        Dimension myFrameSize = theDialog.getSize ();
        Dimension screenSize = GetScreenSize();

        // Start with existing packed frame size
        int width = myFrameSize.width;
        int height = myFrameSize.height;
        // Make sure window sits in upper left
        int x_corner = (screenSize.width-width)/2;
        int y_corner = (screenSize.height-height)/2;
        Point clamped = ClampToScreen (x_corner, y_corner, width, height, screenSize);
        theDialog.setBounds(clamped.x, clamped.y,
            width, height);
    }

    // Slide a window back onto the screen if the requested position would
    // push any part of it off the bottom/right (e.g. a tall Controls-style
    // window placed near a screen edge on a small display). Falls back to
    // the top-left corner if the window doesn't fit on screen at all.
    private static Point ClampToScreen (int x, int y, int width, int height, Dimension screenSize)
    {
        int x_clamped = Math.min (x, screenSize.width - width);
        int y_clamped = Math.min (y, screenSize.height - height);
        x_clamped = Math.max (x_clamped, 0);
        y_clamped = Math.max (y_clamped, 0);
        return new Point (x_clamped, y_clamped);
    }

    private static Dimension GetScreenSize ()
    {
        Toolkit theKit = PaintByNumberPro.GetOpeningFrame().getToolkit();
        Dimension screenSize = theKit.getScreenSize();
        return screenSize;
    }

}

