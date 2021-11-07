/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package paintbynumberpro;

import java.io.*;
import javax.swing.*;
import java.util.*;

/**
 *
 * @author Lynne
 */
public class PuzzleStaticUtilities {

    public static String[] acceptable_suffixes = { ".pbn" };

    public static void OpenAPuzzle ()
    {
        // Get a default puzzle directory, if one exists
        /*
        String puzzleDirStr = PaintByNumberPro.GetSettings().GetSettingFor (Settings.SettingKeyword.PUZZLE_DIR);
        File puzzleDir = null;
        if (puzzleDirStr != null)
        {
            puzzleDir = new File(puzzleDirStr);
            if (!puzzleDir.exists()) puzzleDir = null;
        }
         *
         */

        // Get the last file used, if any
//        File puzzleDir = null;
//        PBNHandler theHandler = PaintByNumberPro.GetDrawHandler();
//        if (theHandler != null) puzzleDir = theHandler.GetLastPuzzleFileUsed();

        // Set up the file filter
        MyFileFilter filter = new MyFileFilter ();
        ArrayList fileSuffixes = new ArrayList();
        for (String suffix : acceptable_suffixes) fileSuffixes.add(suffix);
        filter.addAcceptableSuffixes (fileSuffixes);

        // Get an appropriate file to open
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter (filter);
        String lastFile = PBNPreferences.INSTANCE.GetLastSavedFile();
        if (lastFile != null) chooser.setSelectedFile(new File(lastFile));
        int option = chooser.showOpenDialog(null);
        if (option == JFileChooser.APPROVE_OPTION)
        {
            // Create a new puzzle and let the main program know about it!
            File myFile = chooser.getSelectedFile();
            PBNPuzzle myPuzzle = CreatePuzzleFromFile (myFile);

            if (myPuzzle != null)
            {
                PaintByNumberPro.SetNewPuzzle (myPuzzle);
                PBNPreferences.INSTANCE.SetLastSavedFile(myFile.getPath());
            }
        }
    }

	// Create and initialize a brand new puzzle by reading a file
	public static PBNPuzzle CreatePuzzleFromFile(File myFile)
	{
		boolean debug = false;

		if (!myFile.isFile() || !myFile.exists() ||
				!myFile.canRead())
		{
			JOptionPane.showMessageDialog (null,
				"Unable to read file: " + myFile.getName(),
				"File Error",
				JOptionPane.INFORMATION_MESSAGE, null);
			return null;
		}


        BufferedReader myStream;
		try
		{
            myStream = new BufferedReader (new FileReader(myFile));
		}
		catch (FileNotFoundException e)
		{
			JOptionPane.showMessageDialog (null,
				"File not found: " + myFile.getName(),
				"File Error",
				JOptionPane.INFORMATION_MESSAGE, null);
			return null;
		}

        PBNPuzzle myPuzzle = new PBNPuzzle ();

        boolean  gotRows = false, gotCols = false;
        int rows = 0;
        int cols = 0;
        String source = null;
        String line;
        try
        {
            while ((!gotRows || !gotCols) && (line = myStream.readLine()) != null)
            {
                StringTokenizer st = new StringTokenizer (line);

                // Only process non-empty lines
                if (st.countTokens() > 0)
                {
                    String firstToken = st.nextToken();

                    // Look for Source
                    if (firstToken.matches ("Source") && st.countTokens() > 0)
                    {
                        source = "";
                        int ntokens = st.countTokens();
                        for (int i=0; i<ntokens; i++)
                        {
                            String token = st.nextToken();
                            source += token + " ";
                        }
                        source = source.trim();
                        myPuzzle.SetSource (source);
                        if (debug) System.out.println ("Source is " + source);
                    }

                    // Look for Rows
                    else if(firstToken.matches("Rows") && st.countTokens() > 0)
                    {
                        rows = Integer.parseInt (st.nextToken());
                        gotRows = true;
                    }

                    // Look for Cols
                    else if(firstToken.matches("Cols") && st.countTokens() > 0)
                    {
                        cols = Integer.parseInt (st.nextToken());
                        gotCols = true;
                    }

                    // Unrecognized token
                    else
                    {
                        JOptionPane.showMessageDialog (null,
                            "Unrecognized line '" + line + "' while looking for Rows and Cols",
                            "File Error",
                            JOptionPane.INFORMATION_MESSAGE, null);
                        return null;
                    }
                }
            }

            // Check cols and rows
            if (!gotCols || !gotRows) {
                JOptionPane.showMessageDialog (null,
                    "Missing either Rows or Cols in file: " + myFile.getName(),
                    "File Error",
                    JOptionPane.INFORMATION_MESSAGE, null);
                return null;
            }
			if (debug) System.out.println ("Rows Cols " + rows + " " + cols);
			if (rows <= 0 || cols <= 0)
            {
                JOptionPane.showMessageDialog (null,
                    "Invalid Rows or Cols in file: " + myFile.getName(),
                    "File Error",
                    JOptionPane.INFORMATION_MESSAGE, null);
                return null;
            }
            myPuzzle.SetRows(rows);
            myPuzzle.SetCols(cols);

            // Get the Row_clues
			int row;
			int num_clues;
			int max_row_clues = 0;
            for (int i=0; i<rows && (line = myStream.readLine()) != null; i++)
            {
                StringTokenizer st = new StringTokenizer (line);
                if (st.countTokens() > 0)
                {
                    String firstToken = st.nextToken();
                    if (firstToken.matches ("Row_clues") && st.countTokens() > 2)
                    {
                        row = Integer.parseInt (st.nextToken());
                        if (row < 0 || row >= rows)
                        {
                            JOptionPane.showMessageDialog (null,
                                "Invalid row clue row number in line: '" + line + "'",
                                "File Error",
                                JOptionPane.INFORMATION_MESSAGE, null);
                            return null;
                        }
                        num_clues = Integer.parseInt (st.nextToken());
                        myPuzzle.SetRow_NClues (row, num_clues);
                        if (num_clues > max_row_clues)
                            max_row_clues = num_clues;
                        if (num_clues <= 0)
                        {
                            JOptionPane.showMessageDialog (null,
                                "Invalid # of row clues in line: '" + line + "'",
                                "File Error",
                                JOptionPane.INFORMATION_MESSAGE, null);
                            return null;
                        }
                        if (st.countTokens() < num_clues)
                        {
                            JOptionPane.showMessageDialog (null,
                                "Not enough row clues in line: '" + line + "'",
                                "File Error",
                                JOptionPane.INFORMATION_MESSAGE, null);
                            return null;
                        }
                        for (int k=0; k<num_clues; k++)
                        {
                            myPuzzle.SetRow_Clues (row, k, Integer.parseInt (st.nextToken()));
                            if (myPuzzle.GetRow_Clues(row, k) < 0)
                            {
                                JOptionPane.showMessageDialog (null,
                                    "Invalid row clue value in line: '" + line + "'",
                                    "File Error",
                                    JOptionPane.INFORMATION_MESSAGE, null);
                                return null;
                            }
                        }
                    } else
                    {
                        JOptionPane.showMessageDialog (null,
                            "Invalid row clue in line: '" + line + "'",
                            "File Error",
                            JOptionPane.INFORMATION_MESSAGE, null);
                        return null;
                    }
                    if (debug) System.out.println ("Row " + row + " # clues " + myPuzzle.GetRow_NClues(row));
                } else i--;
            }
            myPuzzle.SetMax_Row_Clues (max_row_clues);

			// Get the col clues
			int col;
			int max_col_clues = 0;
			for (int i=0; i<cols && (line = myStream.readLine()) != null; i++)
			{
                StringTokenizer st = new StringTokenizer (line);
                if (st.countTokens() > 0)
                {
                    String firstToken = st.nextToken();
                    if (firstToken.matches ("Col_clues") && st.countTokens() > 2)
                    {
                        col = Integer.parseInt (st.nextToken());
                        if (col < 0 || col >= cols)
                        {
                            JOptionPane.showMessageDialog (null,
                                "Invalid col clue col number in line: '" + line + "'",
                                "File Error",
                                JOptionPane.INFORMATION_MESSAGE, null);
                            return null;
                        }
                        num_clues = Integer.parseInt (st.nextToken());
                        myPuzzle.SetCol_NClues (col, num_clues);
                        if (num_clues > max_col_clues)
                            max_col_clues = num_clues;
                        if (num_clues <= 0)
                        {
                             JOptionPane.showMessageDialog (null,
                                "Invalid # of col clues in line: '" + line + "'",
                                "File Error",
                                JOptionPane.INFORMATION_MESSAGE, null);
                            return null;
                        }
                        if (st.countTokens() < num_clues)
                        {
                            JOptionPane.showMessageDialog (null,
                                "Not enough col clues in line: '" + line + "'",
                                "File Error",
                                JOptionPane.INFORMATION_MESSAGE, null);
                            return null;
                        }
                        for (int k=0; k<num_clues; k++)
                        {
                            myPuzzle.SetCol_Clues (col, k, Integer.parseInt (st.nextToken()));
                            if (myPuzzle.GetCol_Clues(col, k) < 0)
                            {
                                 JOptionPane.showMessageDialog (null,
                                    "Invalid col clue value in line: '" + line + "'",
                                    "File Error",
                                    JOptionPane.INFORMATION_MESSAGE, null);
                                return null;
                            }
                        }
                    } else
                    {
                        JOptionPane.showMessageDialog (null,
                            "Invalid col clue in line: '" + line + "'",
                            "File Error",
                            JOptionPane.INFORMATION_MESSAGE, null);
                        return null;
                    }
                    if (debug) System.out.println ("Col " + col + " " + myPuzzle.GetCol_NClues (col));
                } else i--;
			}
            myPuzzle.SetMax_Col_Clues(max_col_clues);

			// Create & initialize the col and row clue statuses
            myPuzzle.InitializeClueStatusArrays();

			// Create & initialize the puzzle
            myPuzzle.InitializePuzzleArrays();

			// If the puzzle has been worked on and saved, then the
			// status of row clues should be next (followed by the
			// status of col clues and then the state of the puzzle)
			for (int i=0; i<rows && (line = myStream.readLine()) != null; i++)
			{
                StringTokenizer st = new StringTokenizer (line);
                if (st.countTokens() > 0)
                {
                    String firstToken = st.nextToken();
                    if (firstToken.matches ("Row_clues_status") &&
                        st.countTokens() == (myPuzzle.GetRow_NClues(i)+2))
                    {
                        row = Integer.parseInt (st.nextToken());
                        if (row < 0 || row >= rows)
                        {
                            JOptionPane.showMessageDialog (null,
                                "Invalid Row_clues_status row number in line: '" + line + "'",
                                "File Error",
                                JOptionPane.INFORMATION_MESSAGE, null);
                            return null;
                        }
                        num_clues = Integer.parseInt (st.nextToken());
                        if (num_clues != myPuzzle.GetRow_NClues(row))
                        {
                            JOptionPane.showMessageDialog (null,
                                "Invalid Row_clues_status # clues in line: '" + line + "'",
                                "File Error",
                                JOptionPane.INFORMATION_MESSAGE, null);
                            return null;
                        }
                        if (st.countTokens() < num_clues)
                        {
                            JOptionPane.showMessageDialog (null,
                                "Not enough row clue status' in line: '" + line + "'",
                                "File Error",
                                JOptionPane.INFORMATION_MESSAGE, null);
                            return null;
                        }
                        for (int j=0; j<num_clues; j++)
                            myPuzzle.SetRow_Clue_Status (row, j, Integer.parseInt (st.nextToken()));
                        if (debug) System.out.println ("Clue status row " + row);
                    } else
                    {
                        JOptionPane.showMessageDialog (null,
                            "Unrecognized Row_clues_status in line '" + line,
                            "File Error",
                            JOptionPane.INFORMATION_MESSAGE, null);
                        return null;
                    }
                } else i--;
            }

            for (int i=0; i<cols && (line = myStream.readLine()) != null; i++)
			{
                StringTokenizer st = new StringTokenizer (line);
                if (st.countTokens() > 0)
                {
                    String firstToken = st.nextToken();
                    if (firstToken.matches ("Col_clues_status") &&
                        st.countTokens() == (myPuzzle.GetCol_NClues(i)+2))
                    {
                        col = Integer.parseInt (st.nextToken());
                        if (col < 0 || col >= cols)
                        {
                            JOptionPane.showMessageDialog (null,
                                "Invalid Col_clues_status col number in line: '" + line + "'",
                                "File Error",
                                JOptionPane.INFORMATION_MESSAGE, null);
                            return null;
                        }
                        num_clues = Integer.parseInt (st.nextToken());
                        if (num_clues != myPuzzle.GetCol_NClues (col))
                        {
                            JOptionPane.showMessageDialog (null,
                                "Invalid Col_clues_status # clues in line: '" + line + "'",
                                "File Error",
                                JOptionPane.INFORMATION_MESSAGE, null);
                            return null;
                        }
                        if (st.countTokens() < num_clues)
                        {
                            JOptionPane.showMessageDialog (null,
                                "Not enough col clue status' in line: '" + line + "'",
                                "File Error",
                                JOptionPane.INFORMATION_MESSAGE, null);
                            return null;
                        }
                        for (int j=0; j<num_clues; j++)
                            myPuzzle.SetCol_Clue_Status (col, j, Integer.parseInt (st.nextToken()));
                        if (debug) System.out.println ("Clue status col " + col);
                    }
                } else i--;
            }

            // Puzzle status
            for (int i=0; i<rows && (line = myStream.readLine()) != null; i++)
            {
                StringTokenizer st = new StringTokenizer (line);
                if (st.countTokens() > 0)
                {
                    String firstToken = st.nextToken();
                    if (firstToken.matches ("Puzzle") &&
                        st.countTokens() == (cols+1))
                    {
                        row = Integer.parseInt (st.nextToken());
                        if (row < 0 || row >= rows)
                        {
                            JOptionPane.showMessageDialog (null,
                                "Invalid Puzzle row in line: '" + line + "'",
                                "File Error",
                                JOptionPane.INFORMATION_MESSAGE, null);
                            return null;
                        }
                        for (int j=0; j<cols; j++)
                        {
                            myPuzzle.GetPuzzleSquareAt(row, j).SetStatusFromInt(Integer.parseInt (st.nextToken()));
                            myPuzzle.GetBackupPuzzleSquareAt(row, j).CloneStatusFromSquare (myPuzzle.GetPuzzleSquareAt(row, j));
                        }
                    } else
                    {
                        JOptionPane.showMessageDialog (null,
                            "Puzzle status row invalid: '" + line + "'",
                            "File Error",
                            JOptionPane.INFORMATION_MESSAGE, null);
                        return null;
                    }
                } else i--;
            }

            // Get the rest of the stuff, if anything
            while ((line = myStream.readLine()) != null)
            {
                StringTokenizer st = new StringTokenizer (line);
                if (st.countTokens() > 0)
                {
                    String firstToken = st.nextToken();
                    if (firstToken.matches ("Guess_level"))
                        myPuzzle.SetGuessLevel (Integer.parseInt (st.nextToken()));
                    else if (firstToken.matches ("Box_size"))
                        myPuzzle.SetBOX_SIZE (Integer.parseInt (st.nextToken()));
                    else if (firstToken.matches ("Font_size"))
                        myPuzzle.SetFont_Size (Integer.parseInt (st.nextToken()));
                    else if (firstToken.matches ("Clue_height"))
                        myPuzzle.SetClue_Height (Integer.parseInt (st.nextToken()));
                    else if (firstToken.matches ("Clue_width"))
                        myPuzzle.SetClue_Width (Integer.parseInt (st.nextToken()));
                    else
                    {
                        JOptionPane.showMessageDialog (null,
                                "Unrecognized token in line: '" + line + "'",
                                "File Error",
                                JOptionPane.INFORMATION_MESSAGE, null);
                        return null;
                    }
                }
            }
        }
		catch (IOException e)
		{
			JOptionPane.showMessageDialog (null,
					"Error reading file: " + myFile.getName(),
					"File Error",
					JOptionPane.INFORMATION_MESSAGE, null);
			return null;
		}
        catch (NumberFormatException nfe)
        {
			JOptionPane.showMessageDialog (null,
					"Error decoding number in: " + myFile.getName(),
					"File Error",
					JOptionPane.INFORMATION_MESSAGE, null);
			return null;
        }

		// Create current selection and last square selection
        myPuzzle.InitializeSelections();        
        myPuzzle.SetFile (myFile);

		// All is well!
		return myPuzzle;
	}

	public static boolean WritePuzzleToFile (PBNPuzzle thePuzzle, File myFile, boolean simple)
	{
        if (myFile == null) return false;
		if (!myFile.exists())
		{
			try
			{
				if (!myFile.createNewFile())
				{
					JOptionPane.showMessageDialog (null,
							"Unable to create file: " + myFile.getName(),
							"File Error",
							JOptionPane.INFORMATION_MESSAGE, null);
					return false;					
				}
			}
			catch (IOException e)
			{
				JOptionPane.showMessageDialog (null,
						"Unable to create file: " + myFile.getName(),
						"File Error",
						JOptionPane.INFORMATION_MESSAGE, null);
				return false;
			}
		}
		if (!myFile.canWrite())
		{
			JOptionPane.showMessageDialog (null,
					"Cannot write to file: " + myFile.getName(),
					"File Error",
					JOptionPane.INFORMATION_MESSAGE, null);
			return false;
		}
		BufferedWriter myWriter;
		try
		{
			myWriter = new BufferedWriter(
			new FileWriter(myFile.getPath()));
		}
		catch (IOException e)
		{
			JOptionPane.showMessageDialog (null,
					"Unable to create output data stream: " + myFile.getName(),
					"File Error",
					JOptionPane.INFORMATION_MESSAGE, null);
			return false;
		}
		try
		{
			// Clone the puzzle in case it's being updated as we run this function
			PBNPuzzle myPuzzle = new PBNPuzzle(thePuzzle);
			
			// Continue on with the writing
            if (myPuzzle.GetSource() != null) myWriter.write ("Source\t" + myPuzzle.GetSource() + "\n");
			myWriter.write("Rows\t" + myPuzzle.GetRows());
			myWriter.write("\nCols\t" + myPuzzle.GetCols());
			for (int i=0; i<myPuzzle.GetRows(); i++)
			{
				myWriter.write("\nRow_clues\t"+i+"\t"+myPuzzle.GetRow_NClues (i));
				for (int n=0; n<myPuzzle.GetRow_NClues (i); n++)
					myWriter.write("\t"+myPuzzle.GetRow_Clues(i, n));
			}
			for (int i=0; i<myPuzzle.GetCols(); i++)
			{
				myWriter.write("\nCol_clues\t"+i+"\t"+myPuzzle.GetCol_NClues(i));
				for (int n=0; n<myPuzzle.GetCol_NClues(i); n++)
					myWriter.write("\t"+myPuzzle.GetCol_Clues(i, n));
			}
			
			// Stop here if we're just doing a simple write
			if (simple)
			{
				myWriter.flush();
				return true;
			}
			
			// Continue on otherwise
			for (int i=0; i<myPuzzle.GetRows(); i++)
			{
				myWriter.write("\nRow_clues_status\t"+i+"\t"+myPuzzle.GetRow_NClues(i));
				for (int n=0; n<myPuzzle.GetRow_NClues(i); n++)
					myWriter.write("\t"+myPuzzle.GetRow_Clue_Status(i,n));
			}
			for (int i=0; i<myPuzzle.GetCols(); i++)
			{
				myWriter.write("\nCol_clues_status\t"+i+"\t"+myPuzzle.GetCol_NClues(i));
				for (int n=0; n<myPuzzle.GetCol_NClues(i); n++)
					myWriter.write("\t"+myPuzzle.GetCol_Clue_Status(i,n));
			}
			for (int i=0; i<myPuzzle.GetRows(); i++)
			{
				myWriter.write("\nPuzzle\t"+i);
				for (int n=0; n<myPuzzle.GetCols(); n++)
                {
                    PuzzleSquare ps = myPuzzle.GetPuzzleSquareAt(i, n);
					myWriter.write("\t"+PuzzleSquare.StatusToInt(ps));
                }
			}
            PBNHandler myDrawHandler = PaintByNumberPro.GetDrawHandler();
			myWriter.write("\nGuess_level\t"+myPuzzle.GetGuessLevel());
			myWriter.write("\nBox_size\t"+myDrawHandler.GetBoxSize());
			myWriter.write("\nFont_size\t"+myDrawHandler.GetFontSize());
			myWriter.write("\nClue_height\t"+myDrawHandler.GetClueHeight());
			myWriter.write("\nClue_width\t"+myDrawHandler.GetClueWidth());
			myWriter.write("\n");
			myWriter.flush();
            myDrawHandler.SetFrameTitle (myFile.getName());
		}
		catch (IOException e)
		{
			JOptionPane.showMessageDialog (null,
					"Error writing file: " + myFile.getName(),
					"File Error",
					JOptionPane.INFORMATION_MESSAGE, null);
			return false;
		}
		return true;
	}

    public static PBNPuzzle CreateNewEmptyPuzzle ()
    {
        PBNPuzzle newPuzzle = new PBNPuzzle ();
        // this method does handles *all* of the puzzle initilization
        if (!InitializeEmptyPuzzle(newPuzzle)) return null;
        // if the puzzle is valid
        if (newPuzzle.IsReady())
        {

            PaintByNumberPro.SetNewPuzzle (newPuzzle);
            JFileChooser chooser = new JFileChooser();
            String lastFile = PBNPreferences.INSTANCE.GetLastSavedFile();
            if (lastFile != null) chooser.setSelectedFile(new File (lastFile));
            int option = chooser.showSaveDialog(null);
            if (option == JFileChooser.APPROVE_OPTION)
            {
                File theFile = chooser.getSelectedFile();
                PuzzleStaticUtilities.WritePuzzleToFile(newPuzzle, theFile, false);      
                newPuzzle.SetFile (theFile);
            }
            // have the PBNControlsFrame refresh with the new file name
            
            PaintByNumberPro.GetDrawHandler().GetControlsFrame().SetPuzzleItemsState(true);
        } else newPuzzle = null;
        return newPuzzle;
    }

    private static boolean InitializeEmptyPuzzle (PBNPuzzle myPuzzle)
    {
        // Get the puzzle cols and rows (the dialog created here will
        // call SetPuzzleDimensions() if the operator selects the OK button
        // and valid dimensions were entered).
        new PBNGetNameAndDimsDialog (myPuzzle);

        // Only continue if we have valid dimensions
        if (myPuzzle.GetCols() <= 0 || myPuzzle.GetRows() <= 0) return false;

        // Get the puzzle clues (also sets the max_col|row_clues
        // If the operator canceled, then the dialog set max_col_clues or
        // max_row_clues to 0 which we have to check for!
        new PBNGetCluesDialog (myPuzzle);

        // Only continue if we have valid dimensions
        if (myPuzzle.GetMax_Col_Clues() <= 0 || myPuzzle.GetMax_Row_Clues() <= 0) return false;


        // If okay so far, then finish setting up the puzzle arrays
        return (FinishInitializingPuzzle (myPuzzle));
    }

    public static boolean FinishInitializingPuzzle (PBNPuzzle myPuzzle)
    {
        // Create & initialize the col and row clue statuses
        myPuzzle.InitializeClueStatusArrays();

        // Create & initialize the puzzle
        myPuzzle.InitializePuzzleArrays();

        // Initialize the current selections
        myPuzzle.InitializeSelections();

        // Sanity check the clues in the puzzle (ignoring the results)
        myPuzzle.SanityCheckTheClues();

        // Set valid!
        myPuzzle.SetReady(true);

        return true;
    }

    // Makes a clone of the input Puzzle
    public static PBNPuzzle ClonePuzzle (PBNPuzzle clonePuzzle)
    {

        if (clonePuzzle == null) return null;
        if (clonePuzzle.GetRows() <= 0 || clonePuzzle.GetCols() <= 0) return null;

        PBNPuzzle newPuzzle = new PBNPuzzle ();
    
        newPuzzle.SetFile (null);

        newPuzzle.SetPuzzleNameAndDims (clonePuzzle.GetSource(),
                clonePuzzle.GetCols(), clonePuzzle.GetRows());

        for (int i=0; i<clonePuzzle.GetCols(); i++)
        {
            newPuzzle.SetCol_NClues(i,clonePuzzle.GetCol_NClues(i));
            if (clonePuzzle.GetCol_NClues(i) > 0)
            {
                for (int j=0; j<clonePuzzle.GetCol_NClues(i); j++)
                    newPuzzle.SetCol_Clues (i, j, clonePuzzle.GetCol_Clues (i,j));
            }
        }

        for (int i=0; i<clonePuzzle.GetRows(); i++)
        {
            newPuzzle.SetRow_NClues(i, clonePuzzle.GetRow_NClues (i));
            if (clonePuzzle.GetRow_NClues (i) > 0)
            {
                for (int j=0; j<clonePuzzle.GetRow_NClues (i); j++)
                    newPuzzle.SetRow_Clues (i, j, clonePuzzle.GetRow_Clues (i,j));
            }
        }

        newPuzzle.SetMax_Col_Clues(clonePuzzle.GetMax_Col_Clues());
        newPuzzle.SetMax_Row_Clues(clonePuzzle.GetMax_Row_Clues());

        FinishInitializingPuzzle(newPuzzle);

        return newPuzzle;
    }

    private static class MyFileFilter extends javax.swing.filechooser.FileFilter
    {
        private ArrayList mySuffixes = null;
        private ArrayList myRejectedSuffixes = null;

        private MyFileFilter ()
        {  super(); }

        public void addAcceptableSuffixes (ArrayList theSuffixes)
        { mySuffixes = theSuffixes; }
        public void addSuffixesToReject (ArrayList theSuffixes)
        { myRejectedSuffixes = theSuffixes; }

        public boolean accept (File f)
        {
            if (f.isDirectory ()) return true;
            if (myRejectedSuffixes != null && myRejectedSuffixes.size() > 0)
            {
                for (Object obj:myRejectedSuffixes)
                {
                    String suffix = (String)obj;
                    if (f.getName().endsWith (suffix)) return false;
                }
            }
            if (mySuffixes != null && mySuffixes.size() > 0)
            {
                for (Object obj:mySuffixes)
                {
                    String suffix = (String)obj;
                    if (f.getName().endsWith (suffix)) return true;
                }
            }
            return false;
        }

        public String getDescription ()
        {
            String description = "";
            for (Object obj:mySuffixes)
            {
                String suffix = (String)obj;
                description += suffix;
                description += " ";
            }
            description = description.trim();
            return description;
        }
    }

    public static void DumpOneColumn (PBNPuzzle thePuzzle, int col)
    {
        if (thePuzzle == null) return;
        String str = "Col " + col + " clues: ";
        if (thePuzzle.GetCol_NClues(col) > 0)
        {
            for (int i=0; i<thePuzzle.GetCol_NClues(col); i++)
                str += thePuzzle.GetCol_Clues(col, i) + " ";
        } else str += "none";
        System.out.println (str);
        str = "Col " + col + ": ";
        for (int i=0; i<thePuzzle.GetRows(); i++)
            str += thePuzzle.GetPuzzleSquareAt(i, col).toString() + " ";
        System.out.println (str);
    }

    public static void DumpOneRow (PBNPuzzle thePuzzle, int row)
    {
        if (thePuzzle == null) return;
        String str = "Row " + row + " clues: ";
        if (thePuzzle.GetRow_NClues(row) > 0)
        {
            for (int i=0; i<thePuzzle.GetRow_NClues(row); i++)
                str += thePuzzle.GetRow_Clues(row, i) + " ";
        } else str += "none";
        System.out.println (str);
        str = "Row " + row + ": ";
        for (int i=0; i<thePuzzle.GetCols(); i++)
            str += thePuzzle.GetPuzzleSquareAt(row, i).toString() + " ";
        System.out.println (str);
    }

    public static void DumpOneColumn (PBNPuzzle thePuzzle, int col, String msg)
    {
        System.out.println (msg + ":");
        DumpOneColumn (thePuzzle, col);
    }

    public static void DumpOneRow (PBNPuzzle thePuzzle, int row, String msg)
    {
        System.out.println (msg + ":");
        DumpOneRow (thePuzzle, row);
    }

    public static void DumpArray (PuzzleSquare[] psList, String prepend)
    {
        String str = prepend + " ";
        for (int i=0; i<psList.length; i++)
            str += psList[i].toString() + " ";
        System.out.println (str);
    }
}
