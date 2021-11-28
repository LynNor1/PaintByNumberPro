package paintbynumberpro;

import java.awt.*;
import java.awt.print.*;
import java.io.File;
import javax.swing.*;
import java.util.Calendar;

public class PBNHandler implements Printable {

	private PBNFrame myPuzzleFrame = null;
    private PBNControlsFrame myControlsFrame = null;
    private PBNPuzzle myPuzzle = null;
    private SavePuzzleThread mySaveThread = null;
    private MyComboBoxModel myComboBoxModel = null;
    private File lastPuzzleFile = null;
    private PuzzleSolverThread solverThread = null;
    private static MessageWindow myMessageWindow = null;

    // Fixed drawing parameters
	private final int MIN_BOX_SIZE = 7;
	private final int MIN_FONT_SIZE = 5;
	private final int MIN_CLUE_HEIGHT = 9;
	private final int MIN_CLUE_WIDTH = 9;
	private final int MARGIN = 20;
    private final int SIDE_MARKER_WIDTH = 2;
    private final int HOVER_CLUE_MARGIN = 0;
	private final Color markColor = Color.blue;
    private final Color specialMarkColor = new Color(153, 9, 255);
    private final Color sideMarkerColor = new Color(153, 9, 255);
	private final Color selectColor = Color.yellow;
	private final Color puzzleSelectColor = Color.cyan;
    private final Color regularPuzzleColor = Color.gray;
	private final Color backgroundColor = Color.white;
	private final Color[] clueColors = new Color[] { Color.black,
		Color.red, Color.green };

    // Modifyable (sp?) drawing settings
	private int BOX_SIZE = 0;
	private int CLUE_HEIGHT = 0;
	private int CLUE_WIDTH = 0;
    private int font_size = 0;
	private Color[] guessColors = null;
    private boolean sound = false;
    private String font_name = "";
    private int font_style = 0;
    
    // Calculated drawing parameters
    private Rectangle puzzle_rect = null;
    private Rectangle row_clue_rect = null;
    private Rectangle col_clue_rect = null;
    private Rectangle panel_rect = null;

    // Font
    private Font myFont = null;
	
    // Mode the program is running in
    public enum Mode { NORMAL, DRAWING, AUTO_SOLVE };
    public Mode myMode = Mode.NORMAL;

    PBNHandler (PBNPuzzle thePuzzle, Mode theMode)
    {
        myPuzzle = thePuzzle;
        assert (myPuzzle != null);
        myMode = theMode;

        // Let the puzzle know that I'm its draw handler
        myPuzzle.SetDrawHandler (this);

        // Initialize the box sizes, clue widths, etc. from defaults
        // and then from the puzzle
        InitializeSettings();
        UpdateSettingsFromPuzzle();

        // This must be done after we have the settings
        SetupDrawingRectangles();

        // Create a new combo box model for the guess levels
        // This must be done before creating the controls frame
        myComboBoxModel = new MyComboBoxModel (myPuzzle);
        
        // Create a new SavePuzzleThread to run continuously
        mySaveThread = new SavePuzzleThread(this);
        mySaveThread.start();

        // Create a new ControlsFrame
        myControlsFrame = new PBNControlsFrame(this);
        WindowUtilities.UpperRightFrame(myControlsFrame);
        myControlsFrame.setVisible(true);
        myControlsFrame.toFront();
        if (mySaveThread != null)
        {
            mySaveThread.SetAutoSaveOnOff(myControlsFrame.GetAutoSaveOnOff(),
                    myControlsFrame.GetAutoSaveInterval());
        }

        // Create the Puzzle frame
        File thePuzzleFile = myPuzzle.GetFile();
        if (myPuzzleFrame != null)
        {
            myPuzzleFrame.dispose();
            myPuzzleFrame = null;
        }
        if (thePuzzleFile != null)
            myPuzzleFrame = new PBNFrame (myPuzzle.GetFile().getName(), this);
        else
            myPuzzleFrame = new PBNFrame ("Puzzle Not Saved", this);
        WindowUtilities.UpperLeftFrame(myPuzzleFrame);

        // This needs to be done after the puzzle frame is created and after
        // the panel rects have been initialized
        SetPreferredPanelSizes();

        // this needs to be done after the puzzle frame is created, but perhaps
        // before its repainted
        InitializeFont ();

        // Okay, bring the puzzle frame forward now!
        myPuzzleFrame.setVisible(true);
        myPuzzleFrame.pack();
        myPuzzleFrame.repaint();
        myPuzzleFrame.toFront();

        // create a message window
        myMessageWindow = new MessageWindow();
//        myMessageWindow.setVisible(true);
    }

    public Mode GetTheMode ()
    { return myMode; }

    public void SetLastPuzzleFileUsed (File theFile)
    { lastPuzzleFile = theFile; }
    public File GetLastPuzzleFileUsed ()
    { return lastPuzzleFile; }
    
    public void SetFile (File theFile)
    { myPuzzle.SetFile(theFile); }

    public void ClosePuzzle ()
    {
        // only respond if the puzzle is not solving!
        if (myMode != PBNHandler.Mode.AUTO_SOLVE)
        {
            // Stop the auto-save thread
            mySaveThread.SetStop();

            // Dispose of the controls frame
            myControlsFrame.dispose();

            // Dispose of the puzzle frame
            myPuzzleFrame.dispose();
        }
    }
    public void CloseAllPuzzleFrames ()
    {
        if (myPuzzleFrame != null) myPuzzleFrame.dispose();
    }

    public MyComboBoxModel GetComboBoxModel ()
    { return myComboBoxModel; }

    public void SetAutoSave (boolean onoff)
    {
        if (mySaveThread != null)
            mySaveThread.SetAutoSaveOnOff(onoff, font_size);
    }

    public void SetAutoSaveInterval (int interval_min)
    {
        if (mySaveThread != null)
            mySaveThread.SetAutoSaveInterval (interval_min);
    }

    private void InitializeSettings ()
    {
        BOX_SIZE = PaintByNumberPro.GetSettings().GetIntegerSettingFor (Settings.SettingKeyword.BOX_SIZE);
        CLUE_HEIGHT = PaintByNumberPro.GetSettings().GetIntegerSettingFor(Settings.SettingKeyword.CLUE_HEIGHT);
        CLUE_WIDTH = PaintByNumberPro.GetSettings().GetIntegerSettingFor(Settings.SettingKeyword.CLUE_WIDTH);
        guessColors = new Color[Settings.NUM_GUESS_COLORS];
        for (int i=0; i<guessColors.length; i++)
            guessColors[i] = PaintByNumberPro.GetSettings().GetGuessColorSettingFor (i);
    }

    private void UpdateSettingsFromPuzzle ()
    {
        int size = myPuzzle.GetBOX_SIZE();
        if (size > 0) BOX_SIZE = size;
        size = myPuzzle.GetClue_Height();
        if (size > 0) CLUE_HEIGHT = size;
        size = myPuzzle.GetClue_Width();
        if (size > 0) CLUE_WIDTH = size;
        size = myPuzzle.GetFont_Size();
        if (size > 0) font_size = size;
    }

    private void InitializeFont ()
    {
        if (myFont != null) return;
        JFrame theFrame = GetTheFrame();
        if (theFrame == null) theFrame = PaintByNumberPro.GetOpeningFrame();
        if (theFrame != null) myFont = theFrame.getFont();

        if (myFont != null)
        {
            int cur_font_size = myFont.getSize();
            font_name = myFont.getName();
            font_style = myFont.getStyle();
            if (font_size > 0 && font_size != cur_font_size)
            {
                Font newFont = new Font (font_name,
                        font_style, font_size);
                myFont = newFont;
            } else
                font_size = myFont.getSize();
        }
    }
	
	// Set up the size of the panel within the scroll pane and
	// create the 3 rectangles delineating the 3 major clickable areas
	private void SetupDrawingRectangles ()
	{
        if (myPuzzle == null)
        {
            puzzle_rect = null;
            row_clue_rect = null;
            col_clue_rect = null;
            panel_rect = null;
            return;
        }
		
		// Compute overall width and height of JPanel
        // Note: 3/7/12 Made row_clues_width a little wider because sometimes that
        //       clue on the right edge is written right into the left edge of
        //       the puzzle
		int boxsize1 = BOX_SIZE - 1;
		int puzzle_width = myPuzzle.GetCols() * boxsize1 + 1;
		int puzzle_height = myPuzzle.GetRows() * boxsize1 + 1;
		int row_clues_width = myPuzzle.GetMax_Row_Clues() * CLUE_WIDTH + BOX_SIZE/2;
		int col_clues_height = myPuzzle.GetMax_Col_Clues() * CLUE_HEIGHT;
		int panel_width = MARGIN * 2 + puzzle_width + row_clues_width + SIDE_MARKER_WIDTH;
		int panel_height = MARGIN * 2 + puzzle_height + col_clues_height + SIDE_MARKER_WIDTH;
		
		// create rectangles delineating the 3 major clickable areas
		puzzle_rect = new Rectangle (MARGIN + row_clues_width,
			MARGIN + col_clues_height, puzzle_width, puzzle_height);
		row_clue_rect = new Rectangle (MARGIN, MARGIN + col_clues_height,
			row_clues_width, puzzle_height);
		col_clue_rect = new Rectangle (MARGIN + row_clues_width, MARGIN,
			puzzle_width, col_clues_height);
		panel_rect = new Rectangle (0, 0, panel_width, panel_height);
	}

    private void SetPreferredPanelSizes ()
    {
        if (panel_rect == null) return;
        Dimension d = new Dimension (panel_rect.width, panel_rect.height);
        if (myPuzzleFrame != null) 
		{
			PBNDrawPanel myPanel = myPuzzleFrame.getPanel();
			myPanel.setPreferredSize (d);
			myPanel.setSize(d);
		}
    }

    private Selection GetCurrentSelection ()
    {
        assert (myPuzzle != null);

        Selection cs;
        cs = myPuzzle.GetCurrentSelection ();
        return cs;
    }
	public void SetCurrentSelection (Point square)
	{
		myPuzzle.SetCurrentSelection(square.x, square.y);
	}

    private Graphics GetCurrentGraphics ()
    {
        PBNFrame theFrame = GetTheFrame ();
        if (theFrame != null)
            return theFrame.getPanel().getGraphics();
        else
            return null;
    }

    public Font GetCurrentFont ()
    {
        if (myFont != null) return myFont;
        InitializeFont ();
        return myFont;
    }

    public void SetFrameTitle (String new_title)
    {
        PBNFrame theFrame = GetTheFrame ();
        if (theFrame != null) theFrame.setTitle (new_title);
    }
    
    public PBNControlsFrame GetControlsFrame ()
    { return this.myControlsFrame; }

    // --------------------------------------------------
    // Code for modifying / retrieving drawing parameters
    // --------------------------------------------------

    private void RedrawEverything ()
    {
        SetupDrawingRectangles();
        SetPreferredPanelSizes();
        if (myPuzzleFrame != null) 
		{
			myPuzzleFrame.repaint();
		}
    }

    public PBNPuzzle GetThePuzzle () { return myPuzzle; }
    public PBNFrame GetTheFrame () { return myPuzzleFrame; }
    public int GetBoxSize () { return BOX_SIZE; }
    public int GetClueHeight () { return CLUE_HEIGHT; }
    public int GetClueWidth () { return CLUE_WIDTH; }
    public int GetFontSize () { return font_size; }
    public boolean GetSound () { return sound; }
    public Rectangle GetPanelRect () { return panel_rect; }
    public Rectangle GetColClueRect () { return col_clue_rect; }
    public Rectangle GetRowClueRect () { return row_clue_rect; }
    public Font GetFont () { return myFont; }
    public int GetNumGuessColors ()
    {
        if (guessColors == null) return 0;
        else return guessColors.length;
    }
    public Color GetGuessColor (int guess_level)
    {
        Color c = regularPuzzleColor;
        if (guessColors == null || guess_level <= 0) return c;
        int i = (guess_level-1)%guessColors.length;
        return guessColors[i];
    }
    public Color GetSideMarkerColor ()
    { return sideMarkerColor; }
    
    public void IncrClueHeight ()
    { CLUE_HEIGHT++; RedrawEverything(); }
    public void IncrClueWidth ()
    { CLUE_WIDTH++; RedrawEverything(); }
    public void IncrBoxSize ()
    { BOX_SIZE+=2; RedrawEverything(); }
    
    public void DecrClueHeight ()
    {
        CLUE_HEIGHT--;
        if (CLUE_HEIGHT < MIN_CLUE_HEIGHT) CLUE_HEIGHT = MIN_CLUE_HEIGHT;
        RedrawEverything();
    }
    public void DecrClueWidth ()
    {
        CLUE_WIDTH--;
        if (CLUE_WIDTH < MIN_CLUE_WIDTH) CLUE_WIDTH = MIN_CLUE_WIDTH;
        RedrawEverything();
    }
    public void DecrBoxSize() 
    {
        BOX_SIZE-=2;
        if (BOX_SIZE < MIN_BOX_SIZE) BOX_SIZE = MIN_BOX_SIZE;
        RedrawEverything();
    }

	public void IncrFontSize()
	{
        if (myFont == null) InitializeFont();
        font_size++;
		Font newFont = new Font (font_name, font_style, font_size);
		myFont = newFont;
		RedrawEverything();
	}

	public void DecrFontSize()
	{
		if (font_size > MIN_FONT_SIZE)
		{
			font_size--;
			Font newFont = new Font (font_name, font_style, font_size);
			myFont = newFont;
			RedrawEverything();
		}
	}

    // -------------
    // Drawing code!
    // -------------

    public void DrawColCluesComponent (int row, int col)
    {
        PBNFrame myFrame = this.GetTheFrame();
        if (myFrame == null || myFrame.getPanel() == null) return;

        Rectangle visRect = myPuzzleFrame.GetVisibleRect();
        Graphics g = myPuzzleFrame.getPanel().getGraphics();
        int y = col_clue_rect.y + col_clue_rect.height;
        int x = col*(BOX_SIZE-1) + puzzle_rect.x + BOX_SIZE - 5;        
        ColCluesComponent cluesComponent = myFrame.getPanel().GetColCluesComponent();
        cluesComponent.SetForCol (col);
        cluesComponent.MoveToTopOfVisRect (x, y, visRect.y);
        if (cluesComponent.isVisible()) cluesComponent.paintComponent(g);
    }

    public void HideColCluesComponent ()
    {
        PBNFrame myFrame = this.GetTheFrame();
        if (myFrame == null || myFrame.getPanel() == null) return;

        ColCluesComponent cluesComponent = myFrame.getPanel().GetColCluesComponent();
        myMessageWindow.AddMessage("PBNHandler->HideColCluesComponent() set INVISIBLE");        
        cluesComponent.setVisible(false);
        cluesComponent.repaint();
    }

    public void DrawRowCluesComponent (int row, int col)
    {
        PBNFrame myFrame = this.GetTheFrame();
        if (myFrame == null || myFrame.getPanel() == null) return;
        
        // Create a little rectangle to draw the counter in
        Rectangle visRect = myPuzzleFrame.GetVisibleRect();
        int x = row_clue_rect.x+row_clue_rect.width;        
        int y = row*(BOX_SIZE-1) + puzzle_rect.y + BOX_SIZE - 5;
        RowCluesComponent cluesComponent = myFrame.getPanel().GetRowCluesComponent();
        cluesComponent.SetForRow (row);
        cluesComponent.MoveToUpperRightLocationAtEdge (x, y, visRect.x);
        if (cluesComponent.isVisible()) cluesComponent.repaint();
    }

    public void HideRowCluesComponent ()
    {
        PBNFrame myFrame = this.GetTheFrame();
        if (myFrame == null || myFrame.getPanel() == null) return;

        RowCluesComponent cluesComponent = myFrame.getPanel().GetRowCluesComponent();
        myMessageWindow.AddMessage("PBNHandler->HideRowCluesComponent() set INVISIBLE");
        cluesComponent.setVisible(false);
        cluesComponent.repaint();
    }

    public void DrawCounterAt (int row, int col, int num)
    {
        PBNFrame myFrame = this.GetTheFrame();
        if (myFrame == null || myFrame.getPanel() == null) return;

		// set LL location of box to draw
		int x = col*(BOX_SIZE-1) + puzzle_rect.x;
		int y = row*(BOX_SIZE-1) + puzzle_rect.y;

        // Create a little rectangle to draw the counter in
        x += 5;
        y -= 5;
		
        Counter counterComponent = myFrame.getPanel().GetCounterComponent();
        counterComponent.SetCounter(num);
        counterComponent.MoveTo(x, y);
        counterComponent.setVisible(true);
        counterComponent.repaint();
    }

    public void HideDrawCounter ()
    {
        PBNFrame myFrame = this.GetTheFrame();
        if (myFrame == null || myFrame.getPanel() == null) return;

        Counter counterComponent = myFrame.getPanel().GetCounterComponent();
        counterComponent.setVisible(false);
    }

    public void DrawSideMarkersAt (int row, int col)
    {
        Graphics g = this.GetCurrentGraphics();
        DrawSideMarkersAt (g, row, col);
    }

    private void DrawSideMarkersAt (Graphics g, int row, int col)
    {
		// recast Graphics to Graphics2D
		if (g == null) g = GetCurrentGraphics();
		Graphics2D g2D = (Graphics2D)g;

		// set UL y location box to draw
		int x = col*(BOX_SIZE-1) + puzzle_rect.x;
		int y = row*(BOX_SIZE-1) + puzzle_rect.y;

        // clear any existing side markers
        g2D.setColor (backgroundColor);
        g2D.fillRect (puzzle_rect.x, puzzle_rect.y+puzzle_rect.height,
                puzzle_rect.width, SIDE_MARKER_WIDTH+1);
        g2D.fillRect (puzzle_rect.x + puzzle_rect.width, puzzle_rect.y,
                SIDE_MARKER_WIDTH+1, puzzle_rect.height);

        g2D.setColor (sideMarkerColor);
        if (col >= 0 && col < myPuzzle.GetCols())
        {
            g2D.fillRect (x, puzzle_rect.y+puzzle_rect.height+1,
                    BOX_SIZE-1, SIDE_MARKER_WIDTH);
        }
        if (row >= 0 && row < myPuzzle.GetRows())
        {
            g2D.fillRect (puzzle_rect.x + puzzle_rect.width + 1, y,
                    SIDE_MARKER_WIDTH,
                    BOX_SIZE - 1);
        }

        g2D.setColor (Color.BLACK);
    }

    private void DrawSideMarkersForCurrentSelection (Graphics g)
    {
        Selection cs = GetCurrentSelection ();
        if (cs == null) return;

        // get the selection
        int col = cs.getColSelected();
        if (col < 0)
            col = cs.getClueColSelected();
        int row = cs.getRowSelected();
        if (row < 0)
            row = cs.getClueRowSelected();

        DrawSideMarkersAt (g, row, col);
    }
	
	public void HighlightClues (Graphics g, int row, int col, boolean highlight)
    {
 		// recast Graphics to Graphics2D
		if (g == null) g = GetCurrentGraphics();
		Graphics2D g2D = (Graphics2D)g; 

        Stroke bs1 = new BasicStroke(1);
		g2D.setStroke(bs1);

		// set UL y location box to draw
		int x = col*(BOX_SIZE-1) + puzzle_rect.x;
		int y = row*(BOX_SIZE-1) + puzzle_rect.y;

		// Draw boxes around associated col/row clues
		if (highlight)
			g2D.setColor (puzzleSelectColor);
		else
			g2D.setColor(backgroundColor);
		g2D.drawRect(row_clue_rect.x, y+2,
			row_clue_rect.width-2, BOX_SIZE-5);
		g2D.drawRect(x+2, col_clue_rect.y-1,
			BOX_SIZE-5, col_clue_rect.height-1);
		g2D.setColor(Color.black);
    }

    public void HighlightClues (Graphics g, int row, int col)
    {

		// recast Graphics to Graphics2D
		if (g == null) g = GetCurrentGraphics();
		Graphics2D g2D = (Graphics2D)g;

        Stroke bs1 = new BasicStroke(1);
		g2D.setStroke(bs1);

		// set UL y location box to draw
		int x = col*(BOX_SIZE-1) + puzzle_rect.x;
		int y = row*(BOX_SIZE-1) + puzzle_rect.y;

		// Draw boxes around associated col/row clues
		Selection cs = GetCurrentSelection ();
		if (cs.isBoxSelected(row, col))
			g2D.setColor (puzzleSelectColor);
		else
			g2D.setColor(backgroundColor);
		g2D.drawRect(row_clue_rect.x, y+2,
			row_clue_rect.width-2, BOX_SIZE-5);
		g2D.drawRect(x+2, col_clue_rect.y-1,
			BOX_SIZE-5, col_clue_rect.height-1);
		g2D.setColor(Color.black);
    }

    private void HighlightClues (Graphics g)
    {
        Selection cs = GetCurrentSelection ();
        if (cs == null) return;  

		// recast Graphics to Graphics2D
		if (g == null) g = GetCurrentGraphics ();
		Graphics2D g2D = (Graphics2D)g;

        int row = cs.getRowSelected();
        int col = cs.getColSelected();

        if (row < 0 || row >= myPuzzle.GetRows() || col < 0 || col >= myPuzzle.GetCols()) return;

        Stroke bs1 = new BasicStroke(1);
		g2D.setStroke(bs1);

		// set UL y location box to draw
		int x = col*(BOX_SIZE-1) + puzzle_rect.x;
		int y = row*(BOX_SIZE-1) + puzzle_rect.y;

		// Draw boxes around associated col/row clues
		g2D.setColor(puzzleSelectColor);
		g2D.drawRect(row_clue_rect.x, y+2,
			row_clue_rect.width-2, BOX_SIZE-5);
		g2D.drawRect(x+2, col_clue_rect.y-1,
			BOX_SIZE-5, col_clue_rect.height-1);
		g2D.setColor(Color.black);
    }
    
    public void RedrawCluesComponents ()
    {
        PBNDrawPanel panel = this.GetTheFrame().getPanel();
        panel.RedrawCluesComponents();
    }
	
	// Draw the contents of the puzzle panel
	public void DrawPuzzle (Graphics g, Rectangle theRect, boolean for_printing)
	{	
		// recast Graphics to Graphics2D
		if (g == null) g = GetCurrentGraphics ();
		Graphics2D g2D = (Graphics2D)g;
		g2D.setFont(GetCurrentFont ());
        
        myMessageWindow.AddMessage("PBNHandler->**DrawPuzzle()**");
		if (!for_printing)
		{
			Rectangle visRect = myPuzzleFrame.GetVisibleRect();
			g.setClip (visRect.x, visRect.y, visRect.width, visRect.height);    
		}
		
		Stroke bs1 = new BasicStroke(1);
		Stroke bs2 = new BasicStroke(2);
        g2D.setStroke (bs1);
		
		// if the area to redraw is not specified, do the whole puzzle
//		if (theRect == null) 
            theRect = panel_rect;
		
		g2D.setColor(backgroundColor);
		g2D.fillRect(theRect.x, theRect.y, theRect.width, theRect.height);
		g2D.setColor(Color.black);
		
		// draw the puzzle
		Rectangle iRect = theRect.intersection(puzzle_rect);
		if (iRect != null && iRect.width > 0)
		{
            myMessageWindow.AddMessage("PBNHandler->DrawPuzzle() drawing full puzzle");
			iRect.x -= puzzle_rect.x;
			iRect.y -= puzzle_rect.y;
							
			// draw the individual boxes within the rectangle
			int x = iRect.x / (BOX_SIZE-1) * (BOX_SIZE-1);
			int y = iRect.y / (BOX_SIZE-1) * (BOX_SIZE-1);
			int row = y / (BOX_SIZE-1);
			int col = x / (BOX_SIZE-1);
			while (y <= iRect.height)
			{
				x = iRect.x / (BOX_SIZE-1) * (BOX_SIZE-1);
				col = x / (BOX_SIZE-1);
				while (x <= iRect.width)
				{
					DrawPuzzleBox (g, row, col, false);
					x += (BOX_SIZE-1);
					col++;
				}
				y += (BOX_SIZE-1);
				row++;
			}			
			DrawPuzzleBoldLines(g);
		}
		
		// draw the column clues
        Rectangle floatRect = new Rectangle(col_clue_rect);
		iRect = theRect.intersection(col_clue_rect);
        if (iRect != null && iRect.height > 0)
		{
            myMessageWindow.AddMessage("PBNHandler->DrawPuzzle() full column clues");            
			// draw the vertical lines
			iRect.x -= floatRect.x;
			int x = iRect.x / (BOX_SIZE-1) * (BOX_SIZE-1);
			boolean bold_line;
			while (x <= floatRect.width)
			{
				bold_line = (x % ((BOX_SIZE-1)*5)) == 0;
				if (bold_line) g2D.setStroke(bs2);
				g2D.drawLine(x+floatRect.x, 
					floatRect.y, 
					x+floatRect.x, 
					floatRect.y+floatRect.height);
				if (bold_line) g2D.setStroke(bs1);
				x += BOX_SIZE - 1;
			}
			// draw the clues
			int col1 = iRect.x / (BOX_SIZE-1);
			int col2 = (iRect.x+iRect.width) / (BOX_SIZE-1);
			for (int col=col1; col<=col2; col++)
			{
				DrawColumnClues (g, col);
			}
		}
		
		// draw the row clues
        floatRect = new Rectangle(row_clue_rect);
		iRect = theRect.intersection(row_clue_rect);
		if (iRect != null && iRect.width > 0)
		{
            myMessageWindow.AddMessage("PBNHandler->DrawPuzzle() full row clues");                
			// draw the horizontal lines
			iRect.y -= floatRect.y;
			int y = iRect.y / (BOX_SIZE-1) * (BOX_SIZE-1);
			while (y <= floatRect.height)
			{
				boolean bold_line = (y % ((BOX_SIZE-1)*5)) == 0;
				if (bold_line) g2D.setStroke(bs2);
				g2D.drawLine(row_clue_rect.x, 
					y+floatRect.y, 
					floatRect.x + floatRect.width, 
					y+floatRect.y);
				if (bold_line) g2D.setStroke(bs1);
				y += BOX_SIZE - 1;
			}
			// draw the clues
			int row1 = iRect.y / (BOX_SIZE-1);
			int row2 = (iRect.y+iRect.height) / (BOX_SIZE-1);
			for (int row=row1; row<=row2; row++)
			{
				DrawRowClues (g, row);
			}
		}

        // Highlight the select row/column clues
        HighlightClues (g);
        
        // Draw the hoving clues for the selected row/column
        DrawFloatingClues (g);
		
		// Draw the Copyright
		int year = Calendar.getInstance().get(Calendar.YEAR);
        String msg = "PaintByNumberPro Software Copyright (c) " + year + ", Lynne N Newberry";
        if (myPuzzle.GetSource() != null) msg += "  Puzzle source: " + myPuzzle.GetSource();
		g2D.drawString(msg,
			panel_rect.x + 4, panel_rect.y+panel_rect.height - 4);
	}
	
	public void DrawColumnClues (Graphics g, int col)
	{
		if (col < 0 || col >= myPuzzle.GetCols()) return;
		int num_clues = myPuzzle.GetCol_NClues (col);
		for (int i=0; i<num_clues; i++)
			DrawColumnClue (g, col, i);
	}
	
	public void DrawColumnClue (Graphics g, int col, int clue_num)
	{
		Graphics2D g2D = (Graphics2D)g;
		g2D.setFont(GetCurrentFont ());
		g2D.setStroke(new BasicStroke(1));
		
		// return right away if invalid column
		if (col < 0 || col >= myPuzzle.GetCols()) return;
	
		// set x location of text to write and box to draw
		int x = col*(BOX_SIZE-1) + col_clue_rect.x + 2;
		int bx = col*(BOX_SIZE-1) + col_clue_rect.x + 1;
		
		// get number of clues for this row
		int n = myPuzzle.GetCol_NClues(col);
		if (clue_num < 0 || clue_num >= n) return; // NEW
		
		// get starting y position
		int y = (myPuzzle.GetMax_Col_Clues() - n + clue_num) * CLUE_HEIGHT + col_clue_rect.y +
			CLUE_HEIGHT - 2;
		int by = (myPuzzle.GetMax_Col_Clues() - n + clue_num) * CLUE_HEIGHT + col_clue_rect.y + 1;
		
		// draw the clue
		int clue;
		String strClue;
		Integer iClue;
		int status;
        Selection cs = GetCurrentSelection ();
		if (cs.isColClueSelected(col, clue_num))
			g2D.setColor(selectColor);
		else
			g2D.setColor(backgroundColor);
		g2D.fillRect(bx+1, by, BOX_SIZE-4, CLUE_HEIGHT-2);
		clue = myPuzzle.GetCol_Clues (col, clue_num);
        status = myPuzzle.GetCol_Clue_Status(col, clue_num);
		iClue = new Integer(clue);
		g2D.setColor(clueColors[status]);
		strClue = iClue.toString();
		g2D.drawString(strClue, x, y);
		g2D.setColor(Color.black);

	}
	
	public void DrawRowClues (Graphics g, int row)
	{
		if (row < 0 || row >= myPuzzle.GetRows()) return;
		int num_clues = myPuzzle.GetRow_NClues (row);
		for (int i=0; i<num_clues; i++)
			DrawRowClue (g, row, i);
	}
	
	public void DrawRowClue (Graphics g, int row, int clue_num)
	{
		Graphics2D g2D = (Graphics2D)g;
		g2D.setFont(GetCurrentFont());
		g2D.setStroke (new BasicStroke(1));
		
		// return right away if invalid column
		if (row < 0 || row >= myPuzzle.GetRows()) return;
	
		// set y location of text to write
		int y = row*(BOX_SIZE-1) + row_clue_rect.y + (BOX_SIZE-1) - 3;
		int by = row*(BOX_SIZE-1) + row_clue_rect.y + 1;
		
		// get number of clues for this row
		int n = myPuzzle.GetRow_NClues (row);
		if (clue_num < 0 || clue_num >= n) return;
		
		// get starting x position
		int x = (myPuzzle.GetMax_Row_Clues() - n + clue_num) * CLUE_WIDTH + row_clue_rect.x + 2;
		int bx = (myPuzzle.GetMax_Row_Clues() - n + clue_num) * CLUE_WIDTH + row_clue_rect.x + 1;
		
		// loop over the clues
		int clue;
		String strClue;
		Integer iClue;
		int status;
        Selection cs = GetCurrentSelection ();
		if (cs.isRowClueSelected(row, clue_num))
			g2D.setColor(selectColor);
		else
			g2D.setColor(backgroundColor);
		g2D.fillRect(bx, by+1, CLUE_WIDTH-2, BOX_SIZE-4);
        clue = myPuzzle.GetRow_Clues (row, clue_num);
        status = myPuzzle.GetRow_Clue_Status(row, clue_num);
		iClue = new Integer(clue);
		strClue = iClue.toString();
		g2D.setColor(clueColors[status]);
		g2D.drawString(strClue, x, y);
		g2D.setColor(Color.black);
	}
    
    public void DrawFloatingClues (Graphics g)
    {
        Selection cs = GetCurrentSelection ();
        if (cs == null) return;
        int col = cs.getColSelected();
        int row = cs.getRowSelected();
        if (row < 0 || row >= myPuzzle.GetRows() || col < 0 || col >= myPuzzle.GetCols()) 
        {
            myMessageWindow.AddMessage ("PBNHandler->DrawFloatingClues() no puzzle rect selected therefore both INVISIBLE");            
            HideColCluesComponent();
            HideRowCluesComponent();
        }        
        else
        {
            Rectangle visRect = myPuzzleFrame.GetVisibleRect();
            if (row_clue_rect.x < visRect.x)
            {
                myMessageWindow.AddMessage ("PBNHandler->DrawFloatingClues() possibly draw Row clues");                         
                DrawRowCluesComponent(row, col);
            } else
            {
                myMessageWindow.AddMessage ("PBNHandler->DrawFloatingClues() hide Row clues");                      
                HideRowCluesComponent();
            }
            if (col_clue_rect.y < visRect.y)
            {
                myMessageWindow.AddMessage ("PBNHandler->DrawFloatingClues() possibly draw Col clues");                    
                DrawColCluesComponent (row, col);
            } else
            {
                myMessageWindow.AddMessage ("PBNHandler->DrawFloatingClues() hide Col clues");                     
                HideColCluesComponent ();
            }
        }
    }

    // this function draws whatever is selected with or without the bold lines
    // (this includes puzzle squares and clues)
	public void DrawSelection (Selection theSelection, boolean draw_bold)
	{
		int row = theSelection.getRowSelected();
		int col = theSelection.getColSelected();
		int row_clue = theSelection.getClueRowSelected();
		int col_clue = theSelection.getClueColSelected();
		int clue_num = theSelection.getClueNumSelected();
		Graphics g = GetCurrentGraphics ();
		if (row >= 0 && row < myPuzzle.GetRows() &&
			col >= 0 && col < myPuzzle.GetCols()) {
			DrawPuzzleBox (g, row, col, draw_bold);
		}
		else if (row_clue >= 0 && row_clue < myPuzzle.GetRows() &&
			clue_num >= 0 && clue_num < myPuzzle.GetMax_Row_Clues())
		{
			DrawRowClues (g, row_clue);
		} else if (col_clue >= 0 && col_clue < myPuzzle.GetCols() &&
			clue_num >= 0 && clue_num < myPuzzle.GetMax_Col_Clues())
		{
            DrawColumnClues (g, col_clue);
		}
        DrawSideMarkersForCurrentSelection (g);
        myMessageWindow.AddMessage("PBNHandler->**DrawSelection()** draw floating clues");        
        DrawFloatingClues (g);
	}

    // This function is used to draw the puzzle square WITH bold lines on
    // any of its 4 edges, if necessary (but draw the bold lines only at the
    // square, not along the entire puzzle so as to avoid flashing when doing
    // click-n-drag)
    public void DrawPuzzleBox (Selection theSelection, boolean draw_bold,
            boolean draw_full_bold)
    {
		int row = theSelection.getRowSelected();
		int col = theSelection.getColSelected();
		Graphics g = GetCurrentGraphics ();
		if (row >= 0 && row < myPuzzle.GetRows() &&
			col >= 0 && col < myPuzzle.GetCols()) {
			DrawPuzzleBox (g, row, col, draw_bold, draw_full_bold);
		}
    }

    // This function is used to draw the puzzle square WITH bold lines on
    // any of its 4 edges, if necessary (but draw the bold lines only at the
    // square, not along the entire puzzle so as to avoid flashing when doing
    // click-n-drag)
    public void DrawPuzzleBox (int row, int col, boolean draw_bold,
            boolean draw_full_bold)
    {
		Graphics g = GetCurrentGraphics ();
		if (row >= 0 && row < myPuzzle.GetRows() &&
			col >= 0 && col < myPuzzle.GetCols()) {
			DrawPuzzleBox (g, row, col, draw_bold, draw_full_bold);
		}
    }

    // This version of DrawPuzzleBox will call the version of DrawPuzzleBox,
    // telling that version to draw bold lines along full width/height of
    // the puzzle_rect
    public void DrawPuzzleBox (Graphics g, int row, int col, boolean draw_bold)
    {
        DrawPuzzleBox (g, row, col, draw_bold, true);
    }

    // draw_bold means to draw the bold lines every 5 cells
    // draw_full_bold means to draw the lines the full width or height of the
    // puzzle_rect, otherwise it'll just draw the bold lines around the immediate
    // puzzle square
	private void DrawPuzzleBox (Graphics g, int row, int col,
			boolean draw_bold, boolean draw_full_bold)
	{
		// return right away if invalid column
		if (row < 0 || row >= myPuzzle.GetRows()) return;
		if (col < 0 || col >= myPuzzle.GetCols()) return;
		
		if (g == null) 
		{
			if (myPuzzleFrame != null) g = myPuzzleFrame.getPanel().getGraphics();
		}
		Graphics2D g2D = (Graphics2D)g;
		g2D.setFont(GetCurrentFont());
        Stroke bs1 = new BasicStroke(1);
        Stroke bs2 = new BasicStroke(2);
		g2D.setStroke(bs1);
		
		// draw bold vertical lines either across entire puzzle OR
        // along just the edges of the current square
		if (draw_bold)
        {
            if (draw_full_bold) DrawPuzzleBoldLines(g);
            else DrawPuzzleBoldLines(g, row, col);
        }
	
		// set UL y location box to draw
		int x = col*(BOX_SIZE-1) + puzzle_rect.x;
		int y = row*(BOX_SIZE-1) + puzzle_rect.y;
		
		// Draw boxes around associated col/row clues
        HighlightClues (g, row, col);

		// get box status
		boolean unknown = false;
		boolean filled = false;
		boolean marked = false;
        boolean special_marked = false;
		boolean guess = false;
		int guess_level = 0;
        PuzzleSquare ps = myPuzzle.GetPuzzleSquareAt(row, col);

        filled = ps.IsFilled();
        marked = ps.IsMarked();
        special_marked = ps.IsSpecialMarked();
        guess = ps.IsGuess();
        guess_level = ps.GetGuessLevel();
        unknown = ps.IsUnknown();
		
		// always draw black rectangle
		g2D.drawRect(x, y, BOX_SIZE-1, BOX_SIZE-1);
			
		// if status is positive, then box needs to be filled in
		if (filled && !unknown)
		{
			if (!guess)
            {
                g2D.setColor (regularPuzzleColor);
            }  else
			{
				g2D.setColor(GetGuessColor (guess_level));
			}
			g2D.fillRect(x+1, y+1, BOX_SIZE-2, BOX_SIZE-2);
            if (guess)
            {
                g2D.setColor (Color.white);
                String myGuessStr = Integer.toString(guess_level);
                g2D.drawString (myGuessStr, x+2, y+BOX_SIZE-3);
            }
			g2D.setColor(Color.black);
		} else
			
		// if status is negative, then box needs to be filled with
		// an X, outline is still black, fill in first with white
		if (!filled && !unknown)
		{
			g2D.setColor(backgroundColor);
			g2D.fillRect(x+1, y+1, BOX_SIZE-2, BOX_SIZE-2);
			if (!guess)
            {
                g2D.setColor (regularPuzzleColor);
            } else
			{
				g2D.setColor(GetGuessColor (guess_level));
			}
			g2D.drawLine(x+1, y+1, x+BOX_SIZE-2, y+BOX_SIZE-2);
			g2D.drawLine(x+1, y+BOX_SIZE-2, x+BOX_SIZE-2, y+1);
            if (guess)
            {
                g2D.setColor (Color.black);
                String myGuessStr = Integer.toString(guess_level);
                g2D.drawString (myGuessStr, x+2, y+BOX_SIZE-3);
            }
			g2D.setColor(Color.black);
		} else
			
		// if status is known, fill in box with white
		if (unknown)
		{
			g2D.setColor(backgroundColor);
			g2D.fillRect(x+1, y+1, BOX_SIZE-2, BOX_SIZE-2);
			g2D.setColor(Color.black);
		}
		
		// if box is marked, then draw in mark color
		if (marked || special_marked)
		{
            g2D.setStroke (bs2);
            if (marked) g2D.setColor(markColor);
            else g2D.setColor(specialMarkColor);
			g2D.drawRect(x+2, y+2, BOX_SIZE-4, BOX_SIZE-4);
			g2D.setColor(Color.black);
            g2D.setStroke (bs1);
		}
		
		// if box is selected, draw in selection color
        Selection cs = GetCurrentSelection ();
		if (cs.isBoxSelected(row, col))
		{
			g2D.setColor (puzzleSelectColor);
			g2D.drawRect(x+1, y+1, BOX_SIZE-3, BOX_SIZE-3);
			g2D.setColor (Color.black);
		}
	}

	public void DrawPuzzleBoldLines (Graphics g)
	{
		if (g == null) g = GetCurrentGraphics();
		Graphics2D g2D = (Graphics2D)g;
		g2D.setFont(GetCurrentFont ());
		Stroke bs2 = new BasicStroke(2);
		Stroke bs1 = new BasicStroke(1);
		g2D.setStroke(bs1);
		int x = 0;
		int y = 0;
		// draw vertical lines
		g2D.setStroke(bs2);
		while (x <= puzzle_rect.width)
		{
			g2D.drawLine(x+puzzle_rect.x, puzzle_rect.y,
				x+puzzle_rect.x, puzzle_rect.y+puzzle_rect.height-1);
			x += (BOX_SIZE-1)*5;
		}
		// draw horizontal lines
		while (y <= puzzle_rect.height)
		{
			g2D.drawLine(puzzle_rect.x, puzzle_rect.y+y,
				puzzle_rect.x+puzzle_rect.width-1, puzzle_rect.y+y);
			y += (BOX_SIZE-1)*5;
		}
		g2D.setStroke(bs1);
		// redraw all marked boxes
		for (int i=0; i<myPuzzle.GetRows(); i++)
			for (int j=0; j<myPuzzle.GetCols(); j++)
			{
                PuzzleSquare ps = myPuzzle.GetPuzzleSquareAt(i, j);
                if (ps.IsMarked() || ps.IsSpecialMarked())
					DrawPuzzleBox (null, i, j, false);
			}
	}

    // This version of DrawPuzzleBoldLines draws the bold lines around just
    // the one puzzle square (instead of on the entire puzzle rect) - this is
    // for speed when tracking click-n-drag
	public void DrawPuzzleBoldLines (Graphics g, int row, int col)
	{
        // return immediately if no bold lines need to be drawn
        boolean left_bold = (col%5 == 0);
        boolean right_bold = ((col+1)%5 == 0);
        boolean top_bold = (row%5 == 0);
        boolean bottom_bold = ((row+1)%5 == 0);
        if (!left_bold && !right_bold && !top_bold && !bottom_bold) return;

		if (g == null)
		{
			if (myPuzzleFrame != null) g = myPuzzleFrame.getPanel().getGraphics();
		}
		Graphics2D g2D = (Graphics2D)g;
		g2D.setFont(GetCurrentFont ());
		Stroke bs2 = new BasicStroke(2);
		Stroke bs1 = new BasicStroke(1);

		g2D.setStroke(bs2);
        int x, y;
        x = col*(BOX_SIZE-1) + puzzle_rect.x;
        y = row*(BOX_SIZE-1) + puzzle_rect.y;

        // Draw left bold
        if (left_bold) g2D.drawLine (x, y, x, y+BOX_SIZE-1);
        // Draw right bold
        if (right_bold)
        {
//            if (col == (GetCols()-1))
//            {
                g2D.drawLine(x + BOX_SIZE - 1, y, x + BOX_SIZE - 1, y + BOX_SIZE - 1);
//            } else
//                g2D.drawLine(x + BOX_SIZE, y, x + BOX_SIZE, y + BOX_SIZE - 1);
        }
        // Draw top bold
        if (top_bold) g2D.drawLine (x, y, x+BOX_SIZE-1, y);
        // Draw bottom bold
        if (bottom_bold)
        {
//            if (row == (GetRows()-1))
//            {
                g2D.drawLine(x, y + BOX_SIZE - 1, x + BOX_SIZE - 1, y + BOX_SIZE - 1);
//            } else
//                g2D.drawLine(x, y + BOX_SIZE, x + BOX_SIZE - 1, y + BOX_SIZE);
        }

        g2D.setStroke(bs1);
		// redraw marked boxes
        PuzzleSquare ps = myPuzzle.GetPuzzleSquareAt(row, col);
        if (ps.IsMarked() || ps.IsSpecialMarked())
            DrawPuzzleBox (null, row, col, false);
	}

    // ===============================
    // Code to make drawing printable!
    // ===============================

	public int print (Graphics g, PageFormat pageformat, int pageIndex)
	{
		Graphics2D g2D = (Graphics2D) g;

		if (pageIndex != 0) return NO_SUCH_PAGE;

		// Get size of available printing area
		int imageableWidth =
			(int)Math.floor(pageformat.getImageableWidth());
		int imageableHeight =
			(int)Math.floor(pageformat.getImageableHeight());
		int panelWidth = imageableWidth;
		int panelHeight = imageableHeight;

		// use scaling to fit the puzzle onto the page
		double scale1 = (double)panelWidth  / (double)panel_rect.width;
		double scale2 = (double)panelHeight / (double)panel_rect.height;
		double scale = scale1 < scale2 ? scale1 : scale2;
		if (scale > 1.0) scale = 1.0;

		// Place panel in center of printable page
        double X = pageformat.getImageableX();
        double Y = pageformat.getImageableY();
        double offsetX = (imageableWidth - panel_rect.width*scale)/2;
        double offsetY = (imageableHeight - panel_rect.height*scale)/2;
        g2D.translate (X + offsetX, Y + offsetY);

		// scale to fit the page
		g2D.scale(scale, scale);

		// Draw the puzzle
		DrawPuzzle (g2D, null, true);

		return PAGE_EXISTS;
	}

    // ---------------------------------------------------------
    // code to handle mouse events (e.g. what square is selected
    // ---------------------------------------------------------

    // return col, row of selected picture cell (as a Point) or
    // null if not a picture cell
    public Point GetSelectedSquare (Point pt)
    {
        if (puzzle_rect.contains(pt))
        {
			pt.x -= puzzle_rect.x;
			pt.y -= puzzle_rect.y;
			int col = pt.x / (BOX_SIZE-1);
			int row = pt.y / (BOX_SIZE-1);
			if (col < 0 || col >= myPuzzle.GetCols() ||
                row < 0 || row >= myPuzzle.GetRows()) return null;
            else return new Point(col, row);
        } else return null;
    }

    public void HandleShiftClick (Point pt)
    {
        if (IsClickInClue(pt))
            myPuzzle.GoToLastSquareSelection();
        else if (IsClickInPuzzle(pt))
            myPuzzle.GoToLastClueSelection();
    }

    public void HandleShiftEnter ()
    {
        if (myPuzzle.isAClueSelected())
            myPuzzle.GoToLastSquareSelection();
        else if (myPuzzle.isAPuzzleSquareSelected())
            myPuzzle.GoToLastClueSelection();
    }

    public boolean IsClickInClue (Point pt)
    {
        return (col_clue_rect.contains (pt) ||
                row_clue_rect.contains (pt));
    }

    public boolean IsClickInPuzzle (Point pt)
    {
        return (puzzle_rect.contains (pt));
    }

    private Point GetColClueSelectedAtPoint (Point pt)
    {
        Point col_clue = null;
		if (col_clue_rect.contains(pt))
		{
			pt.x -= col_clue_rect.x;
			pt.y -= col_clue_rect.y;
			int col = pt.x / (BOX_SIZE-1);
			int row = pt.y / CLUE_HEIGHT;
			if (col < 0 || col >= myPuzzle.GetCols()) return col_clue;
			int n_clues = myPuzzle.GetCol_NClues(col);
			int clue = row - (myPuzzle.GetMax_Col_Clues() - n_clues);
			if (clue < 0 || clue >= n_clues) return col_clue;
            col_clue = new Point (col, clue);
            // x is the col
            // y is the clue
        }
        return col_clue;
    }

    private int GetNearestRowSelectedAtPoint (Point pt)
    {
        pt.y -= row_clue_rect.y;
        int row = pt.y / (BOX_SIZE-1);
        if (row < 0) row = 0;
        if (row >= myPuzzle.GetRows()) row = myPuzzle.GetRows()-1;
        return row;
    }

    private int GetNearestColSelectedAtPoint (Point pt)
    {
        pt.x -= col_clue_rect.x;
        int col = pt.x / (BOX_SIZE-1);
        if (col < 0) col = 0;
        if (col >= myPuzzle.GetCols()) col = myPuzzle.GetCols()-1;
        return col;
    }

    private Point GetRowClueSelectedAtPoint (Point pt)
    {
        Point row_clue = null;
		if (row_clue_rect.contains(pt))
		{
			pt.x -= row_clue_rect.x;
			pt.y -= row_clue_rect.y;
			int col = pt.x / CLUE_WIDTH;
			int row = pt.y / (BOX_SIZE-1);
			if (row < 0 || row >= myPuzzle.GetRows()) return row_clue;
			int n_clues = myPuzzle.GetRow_NClues (row);;
			int clue = col - (myPuzzle.GetMax_Row_Clues() - n_clues);
			if (clue < 0 || clue >= n_clues) return row_clue;
            row_clue = new Point (row, clue);
            // x is the row
            // y is the clue
        }
        return row_clue;
    }

    public void HandleCheckPuzzleFromPopup (Point pt, boolean forward)
    {
		BetterPuzzleSolver bps = new BetterPuzzleSolver();
        boolean success = true;
        PuzzleSolver.InitializeLastMessage();
		if (col_clue_rect.contains(pt))
		{
            int col = this.GetNearestColSelectedAtPoint(pt);
            success = bps.CanSolutionFit(myPuzzle, false, col);
//            PuzzleStaticUtilities.DumpOneColumn(myPuzzle, col);
        } else if (row_clue_rect.contains (pt))
        {
            int row = this.GetNearestRowSelectedAtPoint(pt);
            success = bps.CanSolutionFit(myPuzzle, true, row);
//            PuzzleStaticUtilities.DumpOneRow (myPuzzle, row);
        }
        if (success)
        {
            PaintByNumberPro.HandleMessage("Check for Errors",
                "All went well!");
        } else
        {
            PaintByNumberPro.HandleErrorMessage("Error in Puzzle",
                PuzzleSolver.GetLastMessage());
        }
    }

    public void AutoFillFromPopup (Point pt)
    {
        if (col_clue_rect.contains(pt))
        {
            int col = this.GetNearestColSelectedAtPoint (pt);
            PuzzleSolver.FillInOverlapsInColFromPopup(myPuzzle, col);
        } else if (row_clue_rect.contains (pt))
        {
            int row = this.GetNearestRowSelectedAtPoint(pt);
            PuzzleSolver.FillInOverlapsInRowFromPopup(myPuzzle, row);
        }
        PBNFrame myFrame = this.GetTheFrame();
        myFrame.repaint();
    }

    public void ProcessEdgeFromPopup (Point pt)
    {
        boolean success = true;
        PuzzleSolver.InitializeLastMessage();
        int prev_num_knowns = myPuzzle.CountKnownSquares();
        if (col_clue_rect.contains(pt))
        {
            int col = this.GetNearestColSelectedAtPoint (pt);
            success = PuzzleSolver.ProcessColEdgesFromPopup (myPuzzle, col);
        } else if (row_clue_rect.contains (pt))
        {
            int row = this.GetNearestRowSelectedAtPoint(pt);
            success = PuzzleSolver.ProcessRowEdgesFromPopup (myPuzzle, row);
        }
        int num_knowns = myPuzzle.CountKnownSquares();
        PBNFrame myFrame = this.GetTheFrame();
        myFrame.repaint();
        if (success)
        {
            PaintByNumberPro.HandleMessage("Process Edges",
                "All went well! " + (num_knowns-prev_num_knowns) + " squares changed");
        } else
        {
            PaintByNumberPro.HandleErrorMessage("Process Edges Error",
                PuzzleSolver.GetLastMessage());
        }
    }

    public void ProcessInnerLineFromPopup (Point pt)
    {
        PuzzleSolver.InitializeLastMessage();
        int prev_num_knowns = myPuzzle.CountKnownSquares();
		BetterPuzzleSolver bps = new BetterPuzzleSolver();
		String row_or_col = "Column";
        PBNFrame myFrame = this.GetTheFrame();		
		PBNControlsFrame ctrlsFrame = this.GetControlsFrame();
		boolean do_debug = ctrlsFrame.GetDebugSelected();
        if (col_clue_rect.contains(pt))
        {
            int col = this.GetNearestColSelectedAtPoint (pt);
			int[] clues = PuzzleSolver.GetCluesForColFromPuzzle(myPuzzle, col);
			PuzzleSquare[] squares = BetterPuzzleSolver.CopyColFromPuzzle(myPuzzle, col);
			int count_unknowns_before = bps.CountUnknownSquares (squares);

			bps.ProcessLine (clues, squares, myPuzzle.GetGuessLevel(), false, col, do_debug);
			int count_unknowns_after = bps.CountUnknownSquares (squares);
			boolean something_changed = count_unknowns_before != count_unknowns_after;					
			if (something_changed) 
				bps.CopyColToPuzzle (myPuzzle, squares, col);	
        } else if (row_clue_rect.contains (pt))
        {
			row_or_col = "Row";
            int row = this.GetNearestRowSelectedAtPoint(pt);
			int[] clues = PuzzleSolver.GetCluesForRowFromPuzzle(myPuzzle, row);
			PuzzleSquare[] squares = BetterPuzzleSolver.CopyRowFromPuzzle(myPuzzle, row);
			int count_unknowns_before = bps.CountUnknownSquares (squares);

			bps.ProcessLine (clues, squares, myPuzzle.GetGuessLevel(), true, row, do_debug);
			int count_unknowns_after = bps.CountUnknownSquares (squares);
			boolean something_changed = count_unknowns_before != count_unknowns_after;					
			if (something_changed) 
				bps.CopyRowToPuzzle (myPuzzle, squares, row);	
        }
        int num_knowns = myPuzzle.CountKnownSquares();
        myFrame.repaint();
		PaintByNumberPro.HandleMessage("Process Inner " + row_or_col,
			"All went well! " + (num_knowns-prev_num_knowns) + " squares changed");
    }

    public void ProcessEdgesFromPopup ()
    {
        if (myPuzzle == null) return;
        PuzzleSolver.InitializeLastMessage();
        int prev_num_knowns = myPuzzle.CountKnownSquares();
        int first_num_knowns = prev_num_knowns;
        int num_knowns = 0;
        boolean keep_going = true;
        boolean success = true;
        while (keep_going)
        {
            success = PuzzleSolver.ProcessEdges(myPuzzle, myPuzzle.GetGuessLevel(), false);
            num_knowns = myPuzzle.CountKnownSquares();
            keep_going = success && (num_knowns != prev_num_knowns);
            prev_num_knowns = num_knowns;
        }
        PBNFrame myFrame = this.GetTheFrame();
        myFrame.repaint();
        if (success)
        {
            PaintByNumberPro.HandleMessage("Process Edges",
                "All went well! " + (num_knowns-first_num_knowns) + " squares changed");
        } else
        {
            PaintByNumberPro.HandleErrorMessage("Process Edges Error",
                PuzzleSolver.GetLastMessage());
        }
    }
	
	public void ProcessInnerPuzzleRowsFromPopup ()
	{
        if (myPuzzle == null) return;
        PuzzleSolver.InitializeLastMessage();
        int prev_num_knowns = myPuzzle.CountKnownSquares();
        int first_num_knowns = prev_num_knowns;
        int num_knowns = 0;
        boolean success = true;
        PBNFrame myFrame = this.GetTheFrame();		
		BetterPuzzleSolver bps = new BetterPuzzleSolver();
		for (int row=0; row<myPuzzle.GetRows(); row++)
		{
			int[] clues = PuzzleSolver.GetCluesForRowFromPuzzle(myPuzzle, row);
			PuzzleSquare[] squares = BetterPuzzleSolver.CopyRowFromPuzzle(myPuzzle, row);
			int count_unknowns_before = bps.CountUnknownSquares (squares);

			// See if something has changed			
			bps.ProcessLine (clues, squares, myPuzzle.GetGuessLevel(), true, row, false);
			int count_unknowns_after = bps.CountUnknownSquares (squares);
			boolean something_changed = count_unknowns_before != count_unknowns_after;					
			if (something_changed) 
			{
				BetterPuzzleSolver.CopyRowToPuzzle (myPuzzle, squares, row);
				myFrame.repaint();
			}				
		}
        num_knowns = myPuzzle.CountKnownSquares();		
        if (success)
        {
            PaintByNumberPro.HandleMessage("Process Inner Puzzle Rows",
                "All went well! " + (num_knowns-first_num_knowns) + " squares changed");
        } else
        {
            PaintByNumberPro.HandleErrorMessage("Process Inner Puzzle Rows Error",
                PuzzleSolver.GetLastMessage());
        }		
	}
	
	public void ProcessInnerPuzzleColsFromPopup ()
	{
        if (myPuzzle == null) return;
        PuzzleSolver.InitializeLastMessage();
        int prev_num_knowns = myPuzzle.CountKnownSquares();
        int first_num_knowns = prev_num_knowns;
        int num_knowns = 0;
        boolean success = true;
        PBNFrame myFrame = this.GetTheFrame();		
		BetterPuzzleSolver bps = new BetterPuzzleSolver();
		for (int col=0; col<myPuzzle.GetCols(); col++)
		{
			int[] clues = PuzzleSolver.GetCluesForColFromPuzzle(myPuzzle, col);
			PuzzleSquare[] squares = BetterPuzzleSolver.CopyColFromPuzzle(myPuzzle, col);
			int count_unknowns_before = bps.CountUnknownSquares (squares);

			bps.ProcessLine (clues, squares, myPuzzle.GetGuessLevel(), false, col, false);
			int count_unknowns_after = bps.CountUnknownSquares (squares);
			boolean something_changed = count_unknowns_before != count_unknowns_after;					
			if (something_changed) 
			{
				BetterPuzzleSolver.CopyColToPuzzle (myPuzzle, squares, col);
				myFrame.repaint();
			}								
		}	
        num_knowns = myPuzzle.CountKnownSquares();		
        if (success)
        {
            PaintByNumberPro.HandleMessage("Process Inner Puzzle Cols",
                "All went well! " + (num_knowns-first_num_knowns) + " squares changed");
        } else
        {
            PaintByNumberPro.HandleErrorMessage("Process Inner Puzzle Cols Error",
                PuzzleSolver.GetLastMessage());
        }		
	}	

    public boolean CanModifySquare (int row, int col)
    {
        PuzzleSquare ps = myPuzzle.GetPuzzleSquareAt(row, col);
        int cur_guess = myPuzzle.GetGuessLevel();
        boolean can_modify = ps.GetStatus() == PuzzleSquare.SquareStatus.UNKNOWN ||
                cur_guess == ps.GetGuessLevel();
        return (can_modify);
    }

	// handle mouse click events
	public void MouseClickXY (Point pt)
	{
        // Do not respond to mouse-clicks if we're looking at the solution
        if (myMode == Mode.AUTO_SOLVE) return;

		if (sound) Toolkit.getDefaultToolkit().beep();
        // check if in puzzle
		if (puzzle_rect.contains(pt))
		{
			pt.x -= puzzle_rect.x;
			pt.y -= puzzle_rect.y;
			int col = pt.x / (BOX_SIZE-1);
			int row = pt.y / (BOX_SIZE-1);
			if (col < 0 || col >= myPuzzle.GetCols() || row < 0 || row >= myPuzzle.GetRows()) return;
            // Only change square if it's got the same guess level as
            // the one we're working on OR it's an UNKNOWN square
            myPuzzle.HandleCyclePuzzleRect (row, col);
		} else

		// check if in column clues
		if (col_clue_rect.contains(pt))
		{
            Point col_clue = GetColClueSelectedAtPoint(pt);
            if (col_clue != null)
                myPuzzle.HandleCycleColClue (col_clue.x, col_clue.y);

		} else

		// check if in row clues
		if (row_clue_rect.contains(pt))
		{
            Point row_clue = GetRowClueSelectedAtPoint(pt);
            if (row_clue != null)
                myPuzzle.HandleCycleRowClue (row_clue.x, row_clue.y);
		}

		// no selection!
		else
		{
            myPuzzle.HandleNoSelection ();
		}
	}

    public void scrollSelection ()
    {        
        Selection curSelection = myPuzzle.GetCurrentSelection();
		int row = curSelection.getRowSelected();
		int col = curSelection.getColSelected();
		int row_clue = curSelection.getClueRowSelected();
		int col_clue = curSelection.getClueColSelected();
		int clue_num = curSelection.getClueNumSelected();

		Rectangle theRect = null;
		if (row >= 0 && row < myPuzzle.GetRows() &&
			col >= 0 && col < myPuzzle.GetCols()) {
			int x = puzzle_rect.x + col * (BOX_SIZE-1);
			int y = puzzle_rect.y + row * (BOX_SIZE-1);
			theRect = new Rectangle (x+1, y+1,
				BOX_SIZE-2, BOX_SIZE-2);
		}
		else if (row_clue >= 0 && row_clue < myPuzzle.GetRows() &&
			clue_num >= 0 && clue_num < myPuzzle.GetMax_Row_Clues())
		{
			int x = (myPuzzle.GetMax_Row_Clues() - myPuzzle.GetRow_NClues (row_clue) + clue_num) * CLUE_WIDTH + row_clue_rect.x + 1;
			int y = row_clue*(BOX_SIZE-1) + row_clue_rect.y + 1;
			theRect = new Rectangle (x, y+1,
				CLUE_WIDTH-2, BOX_SIZE-4);
		} else if (col_clue >= 0 && col_clue < myPuzzle.GetCols() &&
			clue_num >= 0 && clue_num < myPuzzle.GetMax_Col_Clues())
		{
			int x = col_clue*(BOX_SIZE-1) + col_clue_rect.x + 1;
			int y = (myPuzzle.GetMax_Col_Clues() - myPuzzle.GetCol_NClues (col_clue) + clue_num) * CLUE_HEIGHT + col_clue_rect.y + 1;
			theRect = new Rectangle (x+1, y, BOX_SIZE-4, CLUE_HEIGHT-2);
		}

		// if off the viewable part of scrollpane, move scroll bars
        PBNFrame theFrame = GetTheFrame();
		if (theRect != null && theFrame != null)
		{
			JViewport theVP = theFrame.myScrollPane.getViewport();
			Rectangle theVP_rect = theVP.getViewRect();
			if (!theRect.intersects(theVP_rect))
			{
				Point pt = new Point (theVP_rect.x, theVP_rect.y);
				if (theRect.x < theVP_rect.x) pt.x = theRect.x;
				if (theRect.y < theVP_rect.y) pt.y = theRect.y;
				int diff = (theRect.x+theRect.width)-
					(theVP_rect.x+theVP_rect.width);
				if (diff > 0) pt.x += diff;
				diff = (theRect.y+theRect.height)-
					(theVP_rect.y+theVP_rect.height);
				if (diff > 0) pt.y += diff;
				// use this method instead of theVP.scrollRectToVisible
				// as it appears to be more reliable
				theVP.setViewPosition (pt);
			}
		}
    }

    // ---------------------------
    // Handle updating controls
    // ---------------------------

    public void UpdateLastSaved (int secs_ago)
    {
        if (myControlsFrame != null)
            myControlsFrame.UpdateLastSaved (secs_ago);
    }

    public void PuzzleSavedSuccessfullyToFile (File theFile)
    {
        myPuzzle.SetFile (theFile);
        lastPuzzleFile = theFile;
    }

    public void SetGuessingControlItems (int guess_level)
    {
        if (myControlsFrame != null)
            myControlsFrame.UpdateGuessLevel (myPuzzle.GetGuessLevel());
    }

    public void SetUndoItemEnabled (boolean state)
    {
        if (myControlsFrame != null)
            myControlsFrame.SetUndoItemState(state);
    }

    public void SavePuzzle()
    {
        if (myPuzzle != null && myPuzzle.GetFile() != null && mySaveThread != null)
            mySaveThread.SaveNow();
    }

    public void SavePuzzleAs()
    {
        if (myPuzzle != null)
        {
            JFileChooser chooser;
            if (myPuzzle.GetFile() != null)
                chooser = new JFileChooser(myPuzzle.GetFile().getParent());
            else
                chooser = new JFileChooser();
            int option = chooser.showSaveDialog(null);
            if (option == JFileChooser.APPROVE_OPTION)
            {
                File sf = chooser.getSelectedFile();
                if (sf != null)
                {
                    // Update file name with .pbn if doesn't already end with it
                    String name = sf.getName();
                    if (!name.endsWith (PuzzleStaticUtilities.acceptable_suffixes[0]))
                    {
                        File new_sf = new File (sf.getParent(), sf.getName() + PuzzleStaticUtilities.acceptable_suffixes[0]);
                        sf = new_sf;
                    }
                    boolean try_write = false;
                    if (sf.exists())
                    {
                        if (JOptionPane.showConfirmDialog (null,
                                "Write over " + sf.getName() + "?",
                                "Confirmation",
                                JOptionPane.YES_NO_CANCEL_OPTION)
                            == JOptionPane.YES_OPTION)
                            try_write = true;
                    } else try_write = true;
                    if (try_write && mySaveThread != null)
                    {
                        PBNPreferences.INSTANCE.SetLastSavedFile(sf.getPath());
                        mySaveThread.SaveNow (sf);
                    }
                }
            }
        }
    }

    public void Undo ()
    {
        if (myPuzzle != null) myPuzzle.PopFromStack();
    }
    
    public void RemoveMarks ()
    {
		if (myPuzzle != null) 
		{
			myPuzzle.RemoveMarks();
			Redraw();
		}        
    }

    public void CommitGuesses ()
    {
		if (myPuzzle != null) myPuzzle.commitGuesses();
    }

    public void UndoLastGuess ()
    {
        boolean assume_wrong = myControlsFrame.GetAssumeGuessWrong();
        if (myPuzzle != null) myPuzzle.UndoLastGuess(assume_wrong);
    }

    public void ClearPuzzle ()
    {
        if (myPuzzle != null) myPuzzle.clearPuzzle();
    }

	// return true if okay so far
    public boolean CheckPuzzle (boolean suppress_success_dialog)
    {
        return (PuzzleSolver.CheckPuzzleSoFar(myPuzzle, false, false, suppress_success_dialog));
    }

    public void CheckMySolution ()
    {
        PuzzleSolver.IsPuzzleCorrect(myPuzzle);
    }

    public void FillInTheObvious ()
    {
        int num_knowns = myPuzzle.CountKnownSquares();
        int answer = JOptionPane.YES_OPTION;
        if (num_knowns > 0)
        {
            answer = JOptionPane.showConfirmDialog (null,
                "You have already worked on this puzzle.  Are you sure you want to do this?",
                "Fill In Obvious Squares", JOptionPane.YES_NO_OPTION);
        }
        if (answer == JOptionPane.YES_OPTION)
        {
            PuzzleSolver.FillInEasyToComputeSquares(myPuzzle, true);
            PBNFrame myFrame = this.GetTheFrame();
            myFrame.repaint();
        }
    }

    public void Print ()
    {
        if (myPuzzle == null) return;
        PrinterJob printJob = PrinterJob.getPrinterJob();
        PageFormat pageFormat = printJob.defaultPage();
        PageFormat newPageFormat = printJob.pageDialog(pageFormat);
        if (newPageFormat != pageFormat)
        {
            printJob.setPrintable(this, newPageFormat);
            if (printJob.printDialog())
            {
                try
                {
                    printJob.print();
                }
                catch (PrinterException pe)
                {
                    JOptionPane.showMessageDialog(null,
                        "Error printing the puzzle",
                        "Printer Error",
                        JOptionPane.ERROR_MESSAGE);
                }

            }
        }
    }

    public void Redraw ()
    {
        if (myPuzzleFrame != null) 
		{
			myPuzzleFrame.getPanel().repaint();
			myPuzzleFrame.getPanel().RedrawCluesComponents();
		}
    }


    public void Sound (boolean on_off)
    {
        sound = on_off;
    }

    // called by the SolverThread when it finishes
    public void SetMode (PBNHandler.Mode newMode)
    {
        myMode = newMode;
        myControlsFrame.ModeChanged();
        myPuzzleFrame.ModeChanged();
    }

    public void SolvePuzzle (boolean debugging, boolean auto_stop_at_new_guess)
    {
        if (myPuzzle == null) return;

        // new mode!
        myMode = Mode.AUTO_SOLVE;
        myControlsFrame.ModeChanged();
        myPuzzleFrame.ModeChanged();

        // Start up the solver thread!
        solverThread = new PuzzleSolverThread ();
//        solverThread.SetPuzzleToSolve (mySolvedPuzzle);
        solverThread.SetPuzzleToSolve (myPuzzle);
        solverThread.SetDebugging (debugging);
		solverThread.SetAutoStopAtEachNewGuess(auto_stop_at_new_guess);
        solverThread.start();
    }

    public void SetStopButton (boolean enabled)
    {
        myControlsFrame.SetStopButton (enabled);
    }


    public void TellSolverToStop ()
    {
        if (solverThread != null && solverThread.isAlive())
            solverThread.SetStop();
    }
    
    public void EditClues ()
    {
        if (myPuzzle != null)
        {
//            if (JOptionPane.showConfirmDialog (null,
//                    "Any edits you make will create a new blank puzzle.\n" +
//                    "Are you sure you want to continue?",
//                    "Confirmation",
//                    JOptionPane.YES_NO_CANCEL_OPTION)
//                == JOptionPane.YES_OPTION)
//            {
                PBNPuzzle newPuzzle = PuzzleStaticUtilities.ClonePuzzle (myPuzzle);
                new PBNGetCluesDialog (newPuzzle);
                if (newPuzzle.GetMax_Col_Clues() > 0 && newPuzzle.GetMax_Row_Clues() > 0)
                {
                    // Redo the puzzle clue statuses in case max_col_clues or
                    // max_row_clues changd after it was edited.
                    PuzzleStaticUtilities.FinishInitializingPuzzle(newPuzzle);
                    if (myPuzzle.CopyCluesFromPuzzle (newPuzzle)) Redraw();
                }
//            }
        }
    }

    public void StartNewGuess ()
    {
        if (myPuzzle != null) myPuzzle.StartNewGuessLevel ();
    }

    public void GetAndSetNextGuess ()
    {
        if (myPuzzle != null)
        {
            myPuzzle.GetAndSetNextGuess(myControlsFrame.GetAutoMarkStart());
            Redraw();
        }
    }

    public boolean GetAutoMarkStart ()
    {
        if (myControlsFrame != null) return myControlsFrame.GetAutoMarkStart();
        else return false;
    }

    public void HandleSetGuessLevelFromControls (int level)
    {
        myPuzzle.SetGuessLevel(level);
    }

    public void GiveMeAClue ()
    {
        if (myPuzzle != null)
        {
            // Clone puzzle
            PBNPuzzle clonePuzzle = new PBNPuzzle (myPuzzle);
            if (clonePuzzle != null)
            {
                PuzzleSolver.ProcessEdges (clonePuzzle, 0, false);
                if (PuzzleSolver.PuzzlesAreDifferent (myPuzzle, clonePuzzle)) return;
				BetterPuzzleSolver bps = new BetterPuzzleSolver();
				bps.ProcessPuzzle(clonePuzzle, 0, false);
                if (PuzzleSolver.PuzzlesAreDifferent (myPuzzle, clonePuzzle)) return;
                else
                    PaintByNumberPro.HandleMessage ("Give Me a Clue", "I don't have a clue right now!");
            } else
                PaintByNumberPro.HandleErrorMessage ("Error Cloning Puzzle", "Unable to clone puzzle!");
        }
    }
    
    public Color GetSelectColor()
    { return selectColor; }
    public Color GetBackgroundColor()
    { return backgroundColor; }
    public Color GetColorForClueStatus (int status)
    { return clueColors[status]; }
    
    public static MessageWindow GetMessageWindow ()
    { return myMessageWindow; }
    public static void HandleCloseMessageWindow ()
    {  myMessageWindow.setVisible(false); }

}
