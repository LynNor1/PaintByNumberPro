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
        theFrame.setBounds(x_corner, y_corner,
            width, height);
    }

    public static void UpperLeftFrame (JFrame theFrame)
    {
        // Start with existing packed frame size
        java.awt.Dimension myFrameSize = theFrame.getSize ();
        int width = myFrameSize.width;
        int height = myFrameSize.height;
        // Make sure window sits in upper left
        int x_corner = MARGIN;
        int y_corner = MARGIN;
        theFrame.setBounds(x_corner, y_corner,
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
        theFrame.setBounds(x_corner, y_corner,
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
        theFrame.setBounds(x_corner, y_corner,
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
        theFrame.setBounds(x_corner, y_corner,
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
        theDialog.setBounds(x_corner, y_corner,
            width, height);
    }

    private static Dimension GetScreenSize ()
    {
        Toolkit theKit = PaintByNumberPro.GetOpeningFrame().getToolkit();
        Dimension screenSize = theKit.getScreenSize();
        return screenSize;
    }

}

