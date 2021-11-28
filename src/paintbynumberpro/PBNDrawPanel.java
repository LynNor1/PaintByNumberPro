package paintbynumberpro;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.*;
import javax.swing.*;

public class PBNDrawPanel extends JLayeredPane implements MouseListener,
													Scrollable,
													KeyListener,
                                                    MouseMotionListener,
                                                    AdjustmentListener
{
		
	private PBNPuzzle myPuzzle;
    private PBNHandler myDrawHandler = null;
    private PBNHandler.Mode myMode = null;
    private Counter myCounterComponent;
    private ColCluesComponent myColCluesComponent;
    private RowCluesComponent myRowCluesComponent;

    private Point mouseDownPuzzleRectSquare;
    private Point lastSquareSelected;
    private int mouseDownSquareStatus;
    private boolean trackingInPuzzleRect;
    private boolean doingPopupMenu = false;
    private boolean shiftDown = false;
    private boolean draggingFilledSquares = false;
	private boolean is_dragging = false;
    private int num_draggingFilledSquares = 0;
	
	PBNDrawPanel(PBNHandler theHandler)
	{
        myDrawHandler = theHandler;
        assert (myDrawHandler != null);
		myPuzzle = myDrawHandler.GetThePuzzle();
        assert (myPuzzle != null);
        myMode = myDrawHandler.GetTheMode();

		SetDefaultPreferredSizeIfNeeded ();
		setBackground(PaintByNumberPro.GetSettings().GetBackgroundColor());
        myCounterComponent = new Counter(myDrawHandler);
        myCounterComponent.setVisible(false);
        myCounterComponent.SetPuzzleDrawHandler(myDrawHandler);
        add(myCounterComponent, 3);
        
        myColCluesComponent = new ColCluesComponent (myDrawHandler);
        myColCluesComponent.setVisible(false);
        myColCluesComponent.SetPuzzleDrawHandler(myDrawHandler);
        add(myColCluesComponent, 2);
        
        myRowCluesComponent = new RowCluesComponent (myDrawHandler);
        myRowCluesComponent.setVisible(false);
        myRowCluesComponent.SetPuzzleDrawHandler(myDrawHandler);
        add(myRowCluesComponent, 2);

        // set and request keyboard focus
        this.setFocusable(true);
        this.addKeyListener (this);
        this.requestFocus();
	}

    public Counter GetCounterComponent ()
    { return myCounterComponent; }
    
    public ColCluesComponent GetColCluesComponent()
    { return myColCluesComponent; }
    public RowCluesComponent GetRowCluesComponent ()
    { return myRowCluesComponent; }
	
	private void SetDefaultPreferredSizeIfNeeded ()
	{
        boolean is_set = false;
        if (myDrawHandler != null && myPuzzle != null)
        {
            Rectangle r = myDrawHandler.GetPanelRect();
            if (r != null)
            {
                setPreferredSize (new Dimension (r.width, r.height));
                setSize (new Dimension (r.width, r.height));
                is_set = true;
            }
        }
        if (!is_set)
        {
            setPreferredSize (PaintByNumberPro.GetViewportDimensions());
            setSize (PaintByNumberPro.GetViewportDimensions());
        }
	}
	
	@Override public void paintComponent(Graphics g)
	{
		super.paintComponent (g);
		if (myPuzzle != null && myDrawHandler != null)
        {
			myDrawHandler.DrawPuzzle (g, null, false);
            RedrawCluesComponents ();
        }
	}
    
    public void RedrawCluesComponents ()
    {
        if (myColCluesComponent != null && myColCluesComponent.isVisible())
		{
			myColCluesComponent.ReinitializeDimensions();
            myColCluesComponent.repaint();
		}
        if (myRowCluesComponent != null && myRowCluesComponent.isVisible())
		{
			myRowCluesComponent.ReinitializeDimensions();
            myRowCluesComponent.repaint();    
		}
    }

    public void ModeChanged ()
    {
        myMode = myDrawHandler.GetTheMode();
    }

	// ----------------------------------
	// Implement the Scrollable interface
	// ----------------------------------
	public Dimension getPreferredScrollableViewportSize()
	{
		if (myPuzzle == null)
		{
			return (PaintByNumberPro.GetViewportDimensions());
		} else
		{
			return (new Dimension (myDrawHandler.GetPanelRect().width,
				myDrawHandler.GetPanelRect().height));
		}
	}
	public int getScrollableBlockIncrement (Rectangle theRect,
		int orientation, int direction)
	{
		if (myPuzzle == null) return 100;
		else return ((myDrawHandler.GetBoxSize()-1)*5);
	}
	public boolean getScrollableTracksViewportHeight ()
	{
		return false;
	}
	public boolean getScrollableTracksViewportWidth()
	{
		return false;
	}
	public int getScrollableUnitIncrement (Rectangle theRect,
		int orientation, int direction)
	{
		if (myPuzzle == null) return 10;
		else return (myDrawHandler.GetBoxSize()-1);
	}

    private void CleanUpRowCol (int row, int col)
    {
        // Restore column of mouseDownPuzzleRectSquare
		int target_col = col;
        for (int irow=0; irow<myPuzzle.GetRows(); irow++)
            myPuzzle.RestoreSquareStatusFromBackup(target_col, irow);
        // Restore row of mouseDownPuzzleRectSquare
		int target_row = row;
        for (int icol=0; icol<myPuzzle.GetCols(); icol++)
            myPuzzle.RestoreSquareStatusFromBackup(icol, target_row);
    }

	// -------------------------------------
	// Implement the MouseListener interface
    // and MouseMotionListener interface
	// -------------------------------------

    public void mouseMoved (MouseEvent ev)
    {
//        mouseMovePuzzleRectSquare = myDrawHandler.GetSelectedSquare (ev.getPoint());
//        repaint();
    }

    public void mouseDragged (MouseEvent ev)
    {
        if (myMode == PBNHandler.Mode.AUTO_SOLVE) return;

        boolean cleanupRowCol = false;
        if (trackingInPuzzleRect)
        {
			// we are dragging
			is_dragging = true;
			
            Point pt = ev.getPoint();
            Point myPuzzleRectSquare = myDrawHandler.GetSelectedSquare(pt);
            if (myPuzzleRectSquare != null && !myPuzzleRectSquare.equals(lastSquareSelected))
            {
                // Handle dragging along a column
                if (mouseDownPuzzleRectSquare.x == myPuzzleRectSquare.x &&
                    mouseDownPuzzleRectSquare.y != myPuzzleRectSquare.y)
                {
                    if (myPuzzleRectSquare.y > mouseDownPuzzleRectSquare.y)
                    {
                        int x = mouseDownPuzzleRectSquare.x;
                        for (int y = mouseDownPuzzleRectSquare.y+1; y <= myPuzzleRectSquare.y; y++)
                        {
                            if (myDrawHandler.CanModifySquare(y, x))
                                myPuzzle.SetSquareStatusTemp (x, y, mouseDownSquareStatus);
                        }
                        if (myPuzzleRectSquare.y < (myPuzzle.GetRows()-1))
                        {
                            for (int y=myPuzzleRectSquare.y+1; y<myPuzzle.GetRows(); y++)
                                myPuzzle.RestoreSquareStatusFromBackup(x, y);
                        }
                    } else
                    {
                        int x = mouseDownPuzzleRectSquare.x;
                        if (myPuzzleRectSquare.y > 0)
                        {
                            for (int y=0; y<myPuzzleRectSquare.y; y++)
                                myPuzzle.RestoreSquareStatusFromBackup(x, y);
                        }
                        for (int y = myPuzzleRectSquare.y; y <= mouseDownPuzzleRectSquare.y-1; y++)
                            if (myDrawHandler.CanModifySquare(y, x))
                                myPuzzle.SetSquareStatusTemp (x, y, mouseDownSquareStatus);
                    }
                    // Save last square that's selected during this click-n-drag
                    lastSquareSelected.x = myPuzzleRectSquare.x;
                    lastSquareSelected.y = myPuzzleRectSquare.y;
					myDrawHandler.SetCurrentSelection (lastSquareSelected);
//					myDrawHandler.HighlightClues (null, mouseDownPuzzleRectSquare.y, mouseDownPuzzleRectSquare.x, false);					
					myDrawHandler.HighlightClues (null, lastSquareSelected.y, lastSquareSelected.x, true);										
                    // Count number of adjacent filled squares in this column
                    if (draggingFilledSquares)
                        num_draggingFilledSquares = myPuzzle.CountAdjacentFilledSquaresInCol (lastSquareSelected.y, lastSquareSelected.x);

                // Handle dragging along a row
                } else if (mouseDownPuzzleRectSquare.y == myPuzzleRectSquare.y &&
                           mouseDownPuzzleRectSquare.x != myPuzzleRectSquare.x)
                {
                    if (myPuzzleRectSquare.x > mouseDownPuzzleRectSquare.x)
                    {
                        int y = mouseDownPuzzleRectSquare.y;
                        for (int x = mouseDownPuzzleRectSquare.x+1; x <= myPuzzleRectSquare.x; x++)
                            if (myDrawHandler.CanModifySquare(y, x))
                                myPuzzle.SetSquareStatusTemp (x, y, mouseDownSquareStatus);
                        if (myPuzzleRectSquare.x < (myPuzzle.GetCols()-1))
                        {
                            for (int x = myPuzzleRectSquare.x+1; x < myPuzzle.GetCols(); x++)
                                myPuzzle.RestoreSquareStatusFromBackup(x, y);
                        }
                    } else
                    {
                        int y = mouseDownPuzzleRectSquare.y;
                        if (myPuzzleRectSquare.x > 0)
                        {
                            for (int x = 0; x < myPuzzleRectSquare.x; x++)
                                myPuzzle.RestoreSquareStatusFromBackup(x, y);
                        }
                        for (int x = myPuzzleRectSquare.x; x <= mouseDownPuzzleRectSquare.x-1; x++)
                            if (myDrawHandler.CanModifySquare(y, x))
                                myPuzzle.SetSquareStatusTemp (x, y, mouseDownSquareStatus);
                    }
                    // Save last square that's selected during this click-n-drag
                    lastSquareSelected.x = myPuzzleRectSquare.x;
                    lastSquareSelected.y = myPuzzleRectSquare.y;
					myDrawHandler.SetCurrentSelection (lastSquareSelected);
//					myDrawHandler.HighlightClues (null, mouseDownPuzzleRectSquare.y, mouseDownPuzzleRectSquare.x, false);										
					myDrawHandler.HighlightClues (null, lastSquareSelected.y, lastSquareSelected.x, true);					
                    if (draggingFilledSquares)
                        num_draggingFilledSquares = myPuzzle.CountAdjacentFilledSquaresInRow (lastSquareSelected.y, lastSquareSelected.x);
                } else
                {
                    cleanupRowCol = true;
                    lastSquareSelected.x = -1;
                    lastSquareSelected.y = -1;
					myDrawHandler.SetCurrentSelection(mouseDownPuzzleRectSquare);
					num_draggingFilledSquares = myPuzzle.CountAdjacentFilledSquaresInRow (mouseDownPuzzleRectSquare.y, mouseDownPuzzleRectSquare.x);
					myDrawHandler.HighlightClues (null, mouseDownPuzzleRectSquare.y, mouseDownPuzzleRectSquare.x, true);										
                }
            }
            if (!cleanupRowCol) 
				myDrawHandler.DrawSideMarkersAt(lastSquareSelected.y, lastSquareSelected.x);
			else
				myDrawHandler.DrawSideMarkersAt(mouseDownPuzzleRectSquare.y, mouseDownPuzzleRectSquare.x);
            if (draggingFilledSquares)
			{
				if (cleanupRowCol)
					myDrawHandler.DrawCounterAt (mouseDownPuzzleRectSquare.y, mouseDownPuzzleRectSquare.x, num_draggingFilledSquares);
				else
					myDrawHandler.DrawCounterAt (lastSquareSelected.y, lastSquareSelected.x, num_draggingFilledSquares);
			}					
            if (cleanupRowCol)
			{
				for (int y=0; y<myPuzzle.GetRows(); y++)
					myPuzzle.RestoreSquareStatusFromBackup(mouseDownPuzzleRectSquare.x, y);			
				for (int x=0; x<myPuzzle.GetCols(); x++)
					myPuzzle.RestoreSquareStatusFromBackup(x, mouseDownPuzzleRectSquare.y);						
				myDrawHandler.SetCurrentSelection (mouseDownPuzzleRectSquare);
				myDrawHandler.HighlightClues (null, mouseDownPuzzleRectSquare.y, mouseDownPuzzleRectSquare.x, true);														
			}
        }
    }

    // Handle mouse up/down in same cell (puzzle rect or clues)
	public void mouseClicked (MouseEvent ev) {}
	public void mouseEntered (MouseEvent ev) {}

    // Handle mouse-down event
    // If puzzle square selected, then initiate click-n-drag tracking
	public void mousePressed (MouseEvent ev)
    {
        if (myMode == PBNHandler.Mode.AUTO_SOLVE) return;
		
		// reset mouse dragging
		is_dragging = false;

        // Add another way to detect right-click
        int modifiers = ev.getModifiersEx();
        boolean metaKeyDown = false;
        metaKeyDown = ((modifiers&MouseEvent.META_DOWN_MASK) == MouseEvent.META_DOWN_MASK);
        boolean myRightClick = false;
        int which_button = ev.getButton();
        myRightClick = ((which_button == MouseEvent.BUTTON1 && metaKeyDown) ||
                (which_button == MouseEvent.BUTTON3));
            
        // Only track if NOT the popup trigger event and NOT shift key pressed
        trackingInPuzzleRect = false;
        mouseDownSquareStatus = PuzzleSquare.UNKNOWN;
        mouseDownPuzzleRectSquare = null;
        doingPopupMenu = false;
        lastSquareSelected = null;
        int mods = ev.getModifiers();
        shiftDown = ((mods & MouseEvent.SHIFT_MASK) == MouseEvent.SHIFT_MASK);
        if (!ev.isPopupTrigger() && !myRightClick)
        {
            if (!shiftDown)
            {
                Point pt = ev.getPoint();
                mouseDownPuzzleRectSquare = myDrawHandler.GetSelectedSquare(pt);
                if (mouseDownPuzzleRectSquare != null &&
                    myDrawHandler.CanModifySquare(mouseDownPuzzleRectSquare.y, mouseDownPuzzleRectSquare.x))
                {
                    trackingInPuzzleRect = true;
                    PuzzleSquare ps = myPuzzle.GetPuzzleSquareAt(mouseDownPuzzleRectSquare.y,
                            mouseDownPuzzleRectSquare.x);
                    PuzzleSquare cloned_ps = new PuzzleSquare(ps);
                    cloned_ps.SetMarkedStatus (false);
                    cloned_ps.SetSpecialMarked(false);
                    draggingFilledSquares = cloned_ps.IsFilled();
                    if (draggingFilledSquares) num_draggingFilledSquares = 0;
                    mouseDownSquareStatus = cloned_ps.GetStatusAsInt();
                    lastSquareSelected = new Point (mouseDownPuzzleRectSquare);
                }
            }
        // popup trigger is TRUE
        } else
        {
            Point pt = ev.getPoint();
            Point selectedSquare = myDrawHandler.GetSelectedSquare(pt);
            if (selectedSquare != null)
            {
                doingPopupMenu = true;
                myDrawHandler.GetTheFrame().ShowPopupMenu(ev, selectedSquare);
            } else if (myDrawHandler.IsClickInClue (pt))
            {
                doingPopupMenu = true;
                myDrawHandler.GetTheFrame().ShowCluePopupMenu (ev);
            } else
            {
                doingPopupMenu = true;
                myDrawHandler.GetTheFrame ().ShowSolvePuzzlePopupMenu(ev);
            }
        }
    }

    // Handle mouse-up event
    // If we started click-n-drag tracking in mousePressed, then we continue
    // it here
	public void mouseReleased (MouseEvent ev) 
    {
        if (myMode == PBNHandler.Mode.AUTO_SOLVE) return;

        if (trackingInPuzzleRect)
        {
            Point pt = ev.getPoint();
            Point myPuzzleRectSquare = myDrawHandler.GetSelectedSquare(pt);
            if (myPuzzleRectSquare == null)
            {
                if (lastSquareSelected != null &&
                    lastSquareSelected.x >= 0 &&
                    lastSquareSelected.y >- 0)
                    myPuzzleRectSquare = new Point (lastSquareSelected.x, lastSquareSelected.y);
            }
            if (myPuzzleRectSquare != null)
            {
                // If mouse-up in same square as mouse-down, then call mouseClicked()
                if (myPuzzleRectSquare.x == mouseDownPuzzleRectSquare.x &&
                    myPuzzleRectSquare.y == mouseDownPuzzleRectSquare.y)
                    if (!is_dragging) myDrawHandler.MouseClickXY(ev.getPoint());
                // Finalize changes
                else if(myPuzzleRectSquare.x == mouseDownPuzzleRectSquare.x)
                {
                    int x = myPuzzleRectSquare.x;
                    int starty, endy;
                    if (myPuzzleRectSquare.y > mouseDownPuzzleRectSquare.y)
                    {
                        starty = mouseDownPuzzleRectSquare.y+1;
                        endy = myPuzzleRectSquare.y;
                    } else
                    {
                        starty = myPuzzleRectSquare.y;
                        endy = mouseDownPuzzleRectSquare.y-1;
                    }
                    for (int y=starty; y<=endy; y++)
                        myPuzzle.FinalizeSquareStatusToBackup(x, y, y==starty);
                } else
                {
                    int y = myPuzzleRectSquare.y;
                    int startx, endx;
                    if (myPuzzleRectSquare.x > mouseDownPuzzleRectSquare.x)
                    {
                        startx = mouseDownPuzzleRectSquare.x+1;
                        endx = myPuzzleRectSquare.x;
                    } else
                    {
                        startx = myPuzzleRectSquare.x;
                        endx = mouseDownPuzzleRectSquare.x-1;
                    }
                    for (int x=startx; x<=endx; x++)
                        myPuzzle.FinalizeSquareStatusToBackup(x, y, x==startx);
                }
                myPuzzle.SetCurrentSelection(myPuzzleRectSquare.x, myPuzzleRectSquare.y);
            }
        } else if (!doingPopupMenu)
        {
            // Look for SHIFT key if a clue is currently selected
            int mods = ev.getModifiers();
            shiftDown = ((mods & MouseEvent.SHIFT_MASK) == MouseEvent.SHIFT_MASK);
            if (shiftDown)
            {
                myDrawHandler.HandleShiftClick(ev.getPoint());
            }  else
                myDrawHandler.MouseClickXY(ev.getPoint());
        }
        trackingInPuzzleRect = false;
        mouseDownPuzzleRectSquare = null;
        mouseDownSquareStatus = PuzzleSquare.StatusToInt(new PuzzleSquare(PuzzleSquare.SquareStatus.UNKNOWN));
        myDrawHandler.HideDrawCounter();
    }

	public void mouseExited (MouseEvent ev) {}

	// -----------------------------------
	// Implement the KeyListener interface
	//------------------------------------
	public void keyPressed (KeyEvent ev) { }
	public void keyTyped (KeyEvent ev) {}
	public void keyReleased (KeyEvent ev)
	{
        if (myMode == PBNHandler.Mode.AUTO_SOLVE) return;

        int keycode = ev.getKeyCode();
        int mods = ev.getModifiers();
        shiftDown = ((mods & KeyEvent.SHIFT_MASK) == KeyEvent.SHIFT_MASK);

		if (keycode == KeyEvent.VK_ENTER ||
			keycode == KeyEvent.VK_KP_DOWN ||
			keycode == KeyEvent.VK_KP_UP ||
			keycode == KeyEvent.VK_KP_LEFT ||
			keycode == KeyEvent.VK_KP_RIGHT ||
			keycode == KeyEvent.VK_DOWN ||
			keycode == KeyEvent.VK_UP ||
			keycode == KeyEvent.VK_LEFT ||
			keycode == KeyEvent.VK_RIGHT)
		{
			if (myDrawHandler.GetSound()) Toolkit.getDefaultToolkit().beep();
		}
		if (keycode == KeyEvent.VK_ENTER)
        {
            if (shiftDown)
                myDrawHandler.HandleShiftEnter();
            else
                myPuzzle.CycleCurSelection();
        }  else if (keycode == KeyEvent.VK_DOWN ||
			keycode == KeyEvent.VK_KP_DOWN)
		{
			myPuzzle.CurSelectionDown();
		} else if (keycode == KeyEvent.VK_UP ||
			keycode == KeyEvent.VK_KP_UP)
		{
			myPuzzle.CurSelectionUp();
		} else if (keycode == KeyEvent.VK_LEFT ||
			keycode == KeyEvent.VK_KP_LEFT)
		{
			myPuzzle.CurSelectionLeft();
		} else if (keycode == KeyEvent.VK_RIGHT ||
			keycode == KeyEvent.VK_KP_RIGHT)
		{
			myPuzzle.CurSelectionRight();
		}
		myDrawHandler.scrollSelection();
	}
    
	// ------------------------------------------
	// Implement the AdjustmentListener interface
	//-------------------------------------------
    
    public void adjustmentValueChanged (AdjustmentEvent ae)
    {
        repaint();
    }
}
