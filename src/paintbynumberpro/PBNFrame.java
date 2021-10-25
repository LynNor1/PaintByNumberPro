package paintbynumberpro;

import java.awt.*;

import javax.swing.*;

import java.awt.event.*;

// PBNFrame Software copyright 2007, Lynne Norikane

public class PBNFrame extends JFrame implements WindowListener {

    private PBNHandler myDrawHandler = null;
    private PBNHandler.Mode myMode = null;
	
	private PBNPuzzle myPuzzle = null;
	private PBNDrawPanel myPanel = null;
    private JPopupMenu popupMenu, popupClueMenu, solvePuzzleMenu;
	public JScrollPane myScrollPane;
    private Point popupSelectedSquare = null;
    private Point popupLockAtPoint = null;
	
	// Constructor
	public PBNFrame (String title, PBNHandler theHandler)
	{
        myDrawHandler = theHandler;
        assert (myDrawHandler != null);
        myMode = myDrawHandler.GetTheMode();
        myPuzzle = myDrawHandler.GetThePuzzle();
        assert (myPuzzle != null);
		
		// Set window name and operation on close
		setTitle (title);
		setDefaultCloseOperation (DO_NOTHING_ON_CLOSE);
		
        // Create popup menu for puzzle
        popupMenu = new JPopupMenu();
        JMenuItem markPopupItem = new JMenuItem("Mark");
        JMenuItem unmarkPopupItem = new JMenuItem ("Unmark");
        JMenuItem removeMarksPopupItem = new JMenuItem ("Remove all marks");
        JMenuItem newGuessPopupItem = new JMenuItem ("Start new guess");
        JMenuItem undoLastGuessPopupItem = new JMenuItem ("Undo last guess");
        JMenuItem commitGuessesPopupItem = new JMenuItem ("Commit guesses");
        JMenuItem clearPuzzlePopupItem = new JMenuItem ("Clear puzzle");
        popupMenu.add(markPopupItem);
        popupMenu.add(unmarkPopupItem);
        popupMenu.add(removeMarksPopupItem);
        popupMenu.addSeparator();
        popupMenu.add(newGuessPopupItem);
        popupMenu.add(undoLastGuessPopupItem);
        popupMenu.add(commitGuessesPopupItem);
        popupMenu.addSeparator();
        popupMenu.add(clearPuzzlePopupItem);

        // Create popup menu for clues
        popupClueMenu = new JPopupMenu();
        JMenuItem autoFillPopupItem = new JMenuItem ("Auto fill");
        JMenuItem checkForwardPopupItem = new JMenuItem ("Check line");
//        JMenuItem blobPopupItem = new JMenuItem ("Process blobs");
        JMenuItem processEdgePopupItem = new JMenuItem ("Process edges");
		JMenuItem processInnerLinePopupItem = new JMenuItem ("Process inner line");
//        JMenuItem processBumpersPopupItem = new JMenuItem ("Process bumpers");
//        JMenuItem cleanUpUnknownsPopupItem = new JMenuItem ("Clean up unknowns");
        popupClueMenu.add (autoFillPopupItem);
        popupClueMenu.add (checkForwardPopupItem);
        popupClueMenu.addSeparator();
        popupClueMenu.add(processEdgePopupItem);
		popupClueMenu.add(processInnerLinePopupItem);
//        popupClueMenu.add(blobPopupItem);
//        popupClueMenu.add(processBumpersPopupItem);
//        popupClueMenu.add(cleanUpUnknownsPopupItem);
        autoFillPopupItem.setEnabled(true);
//        blobPopupItem.setEnabled(true);
        processEdgePopupItem.setEnabled(true);
		processInnerLinePopupItem.setEnabled(true);
//        processBumpersPopupItem.setEnabled(true);
//        cleanUpUnknownsPopupItem.setEnabled(true);

        // Create popup menu for solving puzzle
        solvePuzzleMenu = new JPopupMenu();
        JMenuItem fillInObviousPopupItem = new JMenuItem ("Fill in obvious squares");
        JMenuItem checkPuzzlePopupItem = new JMenuItem ("Check puzzle");
        JMenuItem startNewGuessPopupItem = new JMenuItem ("Start new guess");
        JMenuItem getNextGuessPopupItem = new JMenuItem ("Get & set next guess");
        JMenuItem undoLastGuessSolverPopupItem = new JMenuItem ("Undo last guess");
        JMenuItem processEdgesPopupItem = new JMenuItem ("Process all edges");
		JMenuItem processInnerPuzzleRowsPopupItem = new JMenuItem ("Process inner puzzle rows");
		JMenuItem processInnerPuzzleColsPopupItem = new JMenuItem ("Process inner puzzle cols");		
        solvePuzzleMenu.add (fillInObviousPopupItem);
        solvePuzzleMenu.add (checkPuzzlePopupItem);
        solvePuzzleMenu.addSeparator();
        solvePuzzleMenu.add (startNewGuessPopupItem);
        solvePuzzleMenu.add (getNextGuessPopupItem);
        solvePuzzleMenu.add (undoLastGuessSolverPopupItem);
        solvePuzzleMenu.addSeparator();
        solvePuzzleMenu.add(processEdgesPopupItem);
		solvePuzzleMenu.add(processInnerPuzzleRowsPopupItem);
		solvePuzzleMenu.add(processInnerPuzzleColsPopupItem);
        processEdgesPopupItem.setEnabled(true);
		
		// Create a JPanel of a given size
		myPanel = new PBNDrawPanel(myDrawHandler);
		myPanel.addMouseListener(myPanel);
        myPanel.addMouseMotionListener(myPanel);
        myPanel.requestFocusInWindow();
		this.addKeyListener(myPanel);

		// Put the JPanel in a Scroll Pane
		myScrollPane = new JScrollPane (myPanel,
			JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
			JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        myScrollPane.getHorizontalScrollBar().addAdjustmentListener(myPanel);
        myScrollPane.getVerticalScrollBar().addAdjustmentListener(myPanel);
		
		// Add contentBox to contentPane
		Container contentPane = getContentPane();
		contentPane.setLayout(new BorderLayout());
		contentPane.add(myScrollPane, BorderLayout.CENTER);
		contentPane.setBackground (Color.white);

        // Set window listener
        this.addWindowListener (this);

        fillInObviousPopupItem.addActionListener (new ActionListener ()
        {
			public void actionPerformed (ActionEvent ae)
			{
				if (myDrawHandler != null) myDrawHandler.FillInTheObvious();
			}
        });

        checkPuzzlePopupItem.addActionListener (new ActionListener ()
        {
			public void actionPerformed (ActionEvent ae)
			{
				if (myDrawHandler != null) myDrawHandler.CheckPuzzle();
			}
        });

        startNewGuessPopupItem.addActionListener (new ActionListener ()
        {
			public void actionPerformed (ActionEvent ae)
			{
				if (myDrawHandler != null) myDrawHandler.StartNewGuess();
			}
        });

        getNextGuessPopupItem.addActionListener (new ActionListener ()
        {
			public void actionPerformed (ActionEvent ae)
			{
				if (myDrawHandler != null) myDrawHandler.GetAndSetNextGuess();
			}
        });

        undoLastGuessSolverPopupItem.addActionListener (new ActionListener ()
        {
			public void actionPerformed (ActionEvent ae)
			{
				if (myDrawHandler != null) 
				{
					myDrawHandler.UndoLastGuess();
					myDrawHandler.Redraw();
				}
			}
        });

        processEdgesPopupItem.addActionListener (new ActionListener ()
        {
			public void actionPerformed (ActionEvent ae)
			{
				if (myDrawHandler != null) myDrawHandler.ProcessEdgesFromPopup();
			}
        });
		
		processInnerPuzzleRowsPopupItem.addActionListener(new ActionListener ()
		{
			public void actionPerformed (ActionEvent ae)
			{
				if (myDrawHandler != null) myDrawHandler.ProcessInnerPuzzleRowsFromPopup();
			}
		});
				
		processInnerPuzzleColsPopupItem.addActionListener(new ActionListener ()
		{
			public void actionPerformed (ActionEvent ae)
			{
				if (myDrawHandler != null) myDrawHandler.ProcessInnerPuzzleColsFromPopup();
			}
		});		

		/*
        processAllBlobsPopupItem.addActionListener (new ActionListener ()
        {
			public void actionPerformed (ActionEvent ae)
			{
//				if (myDrawHandler != null) myDrawHandler.ProcessSingleCluesFromPopup();
                if (myDrawHandler != null) myDrawHandler.ProcessAllBlobsFromPopup();
			}
        });
		*/

		/*
        processAllBumpersPopupItem.addActionListener (new ActionListener ()
        {
			public void actionPerformed (ActionEvent ae)
			{
				if (myDrawHandler != null) myDrawHandler.ProcessCluesInOneSpotFromPopup();
			}
        });
		*/

		/*
        cleanUpAllUnknownsPopupItem.addActionListener (new ActionListener ()
        {
			public void actionPerformed (ActionEvent ae)
			{
				if (myDrawHandler != null) myDrawHandler.CleanUpUnknownsFromPopup();
			}
        });
		*/
		
		// Create a listener for remove marks
		removeMarksPopupItem.addActionListener(new ActionListener()
		{
			public void actionPerformed (ActionEvent ae)
			{
				if (myDrawHandler != null) 
				{
					myDrawHandler.RemoveMarks();
					myDrawHandler.Redraw();
				}
			}
		}
		);

		// Create a listener for commit last guess
		commitGuessesPopupItem.addActionListener (new ActionListener()
		{
			public void actionPerformed (ActionEvent ae)
			{
				if (myDrawHandler != null) 
				{
					myDrawHandler.CommitGuesses();
					myDrawHandler.Redraw();
				}
			}
		}
		);

		newGuessPopupItem.addActionListener (new ActionListener()
		{
			public void actionPerformed (ActionEvent ae)
			{
                if (myDrawHandler != null) myDrawHandler.StartNewGuess();
			}
		}
		);

		undoLastGuessPopupItem.addActionListener(new ActionListener ()
		{
			public void actionPerformed (ActionEvent ae)
			{
                if (myDrawHandler != null) 
				{
					myDrawHandler.UndoLastGuess();
					myDrawHandler.Redraw();
				}
			}
		}
		);


		clearPuzzlePopupItem.addActionListener(new ActionListener ()
		{
			public void actionPerformed (ActionEvent ae)
			{
                if (myDrawHandler != null) myDrawHandler.ClearPuzzle();
			}
		}
		);

        // Create a listener for lock
        autoFillPopupItem.addActionListener (new ActionListener ()
        {
			public void actionPerformed (ActionEvent ae)
			{
				if (myPuzzle != null && myDrawHandler != null)
				{
                    myDrawHandler.AutoFillFromPopup(popupLockAtPoint);
				}
			}
        });

		/*
        // Create a listener for lock
        blobPopupItem.addActionListener (new ActionListener ()
        {
			public void actionPerformed (ActionEvent ae)
			{
				if (myPuzzle != null && myDrawHandler != null)
				{
                    myDrawHandler.ProcessBlobsFromPopupItem (popupLockAtPoint);
				}
			}
        });

        processBumpersPopupItem.addActionListener (new ActionListener ()
        {
			public void actionPerformed (ActionEvent ae)
			{
				if (myPuzzle != null && myDrawHandler != null)
				{
                    myDrawHandler.ProcessBumpersFromPopup(popupLockAtPoint);
				}
			}
        });
		*/

        // Create a listener for lock
        processEdgePopupItem.addActionListener (new ActionListener ()
        {
			public void actionPerformed (ActionEvent ae)
			{
				if (myPuzzle != null && myDrawHandler != null)
				{
                    myDrawHandler.ProcessEdgeFromPopup(popupLockAtPoint);
				}
			}
        });
		
        // Create a listener for lock
        processInnerLinePopupItem.addActionListener (new ActionListener ()
        {
			public void actionPerformed (ActionEvent ae)
			{
				if (myPuzzle != null && myDrawHandler != null)
				{
                    myDrawHandler.ProcessInnerLineFromPopup(popupLockAtPoint);
				}
			}
        });		

		/*
        cleanUpUnknownsPopupItem.addActionListener (new ActionListener ()
        {
			public void actionPerformed (ActionEvent ae)
			{
				if (myPuzzle != null && myDrawHandler != null)
				{
                    myDrawHandler.CleanUpUnknownsFromPopup(popupLockAtPoint);
				}
			}
        });
		*/

		// Create a listener for mark
		markPopupItem.addActionListener (new ActionListener()
		{
			public void actionPerformed (ActionEvent ae)
			{
				if (myPuzzle != null)
				{
					myPuzzle.MarkRowColFromPopup(popupSelectedSquare.y, popupSelectedSquare.x);
				}
			}
		}
		);

		// Create a listener for mark
		unmarkPopupItem.addActionListener (new ActionListener()
		{
			public void actionPerformed (ActionEvent ae)
			{
				if (myPuzzle != null)
				{
					myPuzzle.UnmarkRowColFromPopup(popupSelectedSquare.y, popupSelectedSquare.x);
				}
			}
		}
		);

        // Create a listener for check forward
        checkForwardPopupItem.addActionListener (new ActionListener()
        {
			public void actionPerformed (ActionEvent ae)
			{
				if (myPuzzle != null && myDrawHandler != null)
				{
					myDrawHandler.HandleCheckPuzzleFromPopup(popupLockAtPoint, true);
				}
			}
        });

	}

    public void ModeChanged ()
    {
        myMode = myDrawHandler.GetTheMode();
        myPanel.ModeChanged();
    }
	
	public PBNDrawPanel getPanel ()
	{
		return myPanel;
	}

    public void ShowPopupMenu(MouseEvent e, Point selectedSquare)
    {
        popupMenu.show (e.getComponent(),
                e.getX(), e.getY());
        popupSelectedSquare = selectedSquare;
    }

    public void ShowCluePopupMenu (MouseEvent e)
    {
        popupClueMenu.show (e.getComponent(), e.getX(), e.getY());
        popupLockAtPoint = e.getPoint();
    }

    public void ShowSolvePuzzlePopupMenu (MouseEvent e)
    {
        solvePuzzleMenu.show (e.getComponent(), e.getX(), e.getY());
    }
    
    public Rectangle GetVisibleRect ()
    {   
        return myPanel.getVisibleRect();
    }

    // Window Listener methods
    public void windowClosing (WindowEvent we)
    {
        if (myMode != PBNHandler.Mode.AUTO_SOLVE)
            PaintByNumberPro.CloseThePuzzle();
        else
            PaintByNumberPro.HandleMessage("Auto-Solver Still Running", "You must stop the auto-solver before quitting!");
    }
    
    public void windowDeactivated (WindowEvent we) { }
    public void windowActivated (WindowEvent we) { }
    public void windowDeiconified (WindowEvent we) { }
    public void windowIconified (WindowEvent we) { }
    public void windowClosed (WindowEvent we) { }
    public void windowOpened (WindowEvent we) { }
}
