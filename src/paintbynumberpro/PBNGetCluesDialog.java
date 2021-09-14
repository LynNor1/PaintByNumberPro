package paintbynumberpro;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import java.util.StringTokenizer;

/**
 *
 * @author user
 */
public class PBNGetCluesDialog extends JDialog implements ActionListener {
    
    private PBNPuzzle myPuzzle;

    private JRadioButton rowsRB, colsRB;
    private JLabel prevL, currL, nextL;
    private JTextField currTF, prevTF, nextTF;

    private JButton prevB, nextB;
    private JButton cancelB, okB;

    private int last_col, last_row; // Used to keep track of which col or row
                                    // we were editing last
    private final int EDIT_ROW_CLUES = 0, EDIT_COL_CLUES = 1;
    private int edit_what;

    PBNGetCluesDialog (PBNPuzzle thePuzzle)
    {
        myPuzzle = thePuzzle;
        if (myPuzzle == null) return;
        
        // Initialize the max_col_clues and max_row_clues
        if (!GetMaxClues ()) return;

        setTitle ("Enter Puzzle Clues");
        setModalityType (Dialog.ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

        rowsRB = new JRadioButton ("Row Clues");
        colsRB = new JRadioButton ("Col Clues");
        ButtonGroup bg = new ButtonGroup();
        bg.add(rowsRB);
        bg.add(colsRB);
        rowsRB.addActionListener (this);
        colsRB.addActionListener (this);
        rowsRB.setSelected(true);
        edit_what = EDIT_ROW_CLUES;

        prevL = new JLabel ("");
        currL = new JLabel ("");
        nextL = new JLabel ("");
        prevTF = new JTextField ("");
        prevTF.setEditable(false);
        prevTF.setEnabled (false);
        currTF = new JTextField ("");
        nextTF = new JTextField ("");
        nextTF.setEditable(false);
        nextTF.setEnabled(false);
        currTF.addActionListener(this);

        prevB = new JButton ("<-");
        nextB = new JButton ("->");
        cancelB = new JButton ("Cancel");
        okB = new JButton ("Done");
        prevB.addActionListener (this);
        nextB.addActionListener (this);
        cancelB.addActionListener (this);
        okB.addActionListener (this);

        JPanel panel = new JPanel();
        GroupLayout gl = new GroupLayout(panel);
        panel.setLayout(gl);

        gl.setAutoCreateGaps(true);
        gl.setAutoCreateContainerGaps(true);

        gl.setHorizontalGroup(
           gl.createParallelGroup(GroupLayout.Alignment.TRAILING)
                .addGroup (gl.createParallelGroup (GroupLayout.Alignment.LEADING)
                    .addGroup (gl.createSequentialGroup ()
                        .addComponent (rowsRB)
                        .addComponent (colsRB)
                    )
                    .addGroup (gl.createSequentialGroup ()
                        .addComponent (prevL)
                        .addComponent (prevTF)
                    )
                    .addGroup (gl.createSequentialGroup ()
                        .addComponent (currL)
                        .addComponent (currTF)
                    )
                    .addGroup (gl.createSequentialGroup ()
                        .addComponent (nextL)
                        .addComponent (nextTF)
                    )
                )
                .addGroup (gl.createSequentialGroup()
                    .addComponent (prevB)
                    .addComponent (nextB)
                    .addComponent (cancelB)
                    .addComponent (okB)
                )
        );
        gl.setVerticalGroup(
           gl.createSequentialGroup()
                .addGroup (gl.createParallelGroup (GroupLayout.Alignment.CENTER)
                    .addComponent (rowsRB)
                    .addComponent (colsRB)
                )
                .addGroup (gl.createParallelGroup(GroupLayout.Alignment.CENTER)
                    .addComponent (prevL)
                    .addComponent (prevTF)
                )
                .addGroup (gl.createParallelGroup(GroupLayout.Alignment.CENTER)
                    .addComponent (currL)
                    .addComponent (currTF)
                )
                .addGroup (gl.createParallelGroup(GroupLayout.Alignment.CENTER)
                    .addComponent (nextL)
                    .addComponent (nextTF)
                )
                .addGroup (gl.createParallelGroup(GroupLayout.Alignment.CENTER)
                    .addComponent (prevB)
                    .addComponent (nextB)
                    .addComponent (cancelB)
                    .addComponent (okB)
                )
        );

        getContentPane().add(panel);

        RepackWindowSize(true);

        Dimension dim = prevTF.getPreferredSize();
        int w = dim.width;
        if (w < 500) w = 500;
        dim.width = w;
        prevTF.setMaximumSize(dim);
        dim = currTF.getPreferredSize();
        dim.width = w;
        currTF.setMaximumSize(dim);
        dim = nextTF.getPreferredSize();
        dim.width = w;
        nextTF.setMaximumSize(dim);

        // Initialize Dialog
        last_row = 0;
        last_col = 0;
        SetCurrentRowOrCol();

        setVisible(true);

    }

    private void RepackWindowSize (boolean move_window)
    {
        pack();
		Dimension scrnSize = this.getToolkit().getScreenSize();
		Dimension wndSize = this.getPreferredSize ();
        if (move_window)
        {
            this.setBounds((scrnSize.width-wndSize.width)/2,
                (scrnSize.height-wndSize.height)/2,
                (scrnSize.width+wndSize.width)/2,
                (scrnSize.height + wndSize.height)/2);
        }
		this.setSize(wndSize);
    }

    private boolean GetMaxClues()
    {
        int max_c_clues = 0;
        int max_r_clues = 0;
        int cols = myPuzzle.GetCols();
        int rows = myPuzzle.GetRows();
        if (cols <= 0 || rows <= 0) return false;
        if (myPuzzle.GetCol_NClues() == null || myPuzzle.GetRow_NClues() == null) return false;

        max_c_clues = myPuzzle.GetCol_NClues(0);
        for (int i=0; i<cols; i++)
        {
            if (myPuzzle.GetCol_NClues(i) > max_c_clues)
                max_c_clues = myPuzzle.GetCol_NClues(i);
        }

        max_r_clues = myPuzzle.GetRow_NClues(0);
        for (int i=0; i<rows; i++)
        {
            if (myPuzzle.GetRow_NClues(i) > max_r_clues)
                max_r_clues = myPuzzle.GetRow_NClues(i);
        }

        myPuzzle.SetMax_Col_Clues(max_c_clues);
        myPuzzle.SetMax_Row_Clues(max_r_clues);

        return true;
    }

    private void SetCurrentRowOrCol()
    {
        if (edit_what == EDIT_ROW_CLUES)
        {
            int row = last_row - 1;
            SetRowClues (row, prevL, prevTF);
            row++;
            SetRowClues (row, currL, currTF);
            row++;
            SetRowClues (row, nextL, nextTF);
        } else
        {
            int col = last_col - 1;
            SetColClues (col, prevL, prevTF);
            col++;
            SetColClues (col, currL, currTF);
            col++;
            SetColClues (col, nextL, nextTF);
        }
    }

    private void SetRowClues (int row, JLabel label, JTextField tf)
    {
        if (row < 0 || row >= myPuzzle.GetRows())
        {
            label.setText ("Row -");
            tf.setText ("");
        } else
        {
            if ((row%5==0))
                label.setForeground (Color.GREEN);
            else
                label.setForeground (Color.BLACK);
            label.setText ("Row " + row);
            String clues = "";
            if (myPuzzle.GetRow_NClues (row) > 0)
            {
                for (int i=0; i<myPuzzle.GetRow_NClues (row); i++)
                {
                    clues += " ";
                    clues += Integer.toString (myPuzzle.GetRow_Clues (row, i));
                }
            }
            tf.setText (clues);
        }
    }

    private void SetColClues (int col, JLabel label, JTextField tf)
    {
        if (col < 0 || col >= myPuzzle.GetCols())
        {
            label.setText ("Col -");
            tf.setText ("");
        } else
        {
            if ((col%5==0))
                label.setForeground (Color.GREEN);
            else
                label.setForeground (Color.BLACK);
            label.setText ("Col " + col);
            String clues = "";
            if (myPuzzle.GetCol_NClues(0) > 0)
            {
                for (int i=0; i<myPuzzle.GetCol_NClues(col); i++)
                {
                    clues += " ";
                    clues += Integer.toString (myPuzzle.GetCol_Clues (col, i));
                }
            }
            tf.setText (clues);
        }
    }

    private boolean ProcessClue ()
    {
        String str = currTF.getText();
        if (str != null) str.trim();
        if (str == null || str.length() == 0)
        {
            if (edit_what == EDIT_ROW_CLUES)
                myPuzzle.SetRow_NClues (last_row, 0);
            else
                myPuzzle.SetCol_NClues (last_col, 0);
            return true;
        }

        StringTokenizer st = new StringTokenizer(str);
        int n = 0;
        while (st.hasMoreTokens()) {
            String clue = st.nextToken();
            int iclue;
            try
            {
                iclue = Integer.parseInt(clue);
            }
            catch (NumberFormatException nfe)
            {
                JOptionPane.showMessageDialog(this,
                    "Invalid text: " + clue,
                    "Clue Error",
                    JOptionPane.WARNING_MESSAGE);
                return false;
            }
            if (iclue < 0)
            {
                JOptionPane.showMessageDialog(this,
                    "Clue must be >= 0!",
                    "Clue Error",
                    JOptionPane.WARNING_MESSAGE);
                return false;
            }
            if (edit_what == EDIT_ROW_CLUES)
                myPuzzle.SetRow_Clues (last_row, n, iclue);
            else
                myPuzzle.SetCol_Clues(last_col, n, iclue);
            n++;
        }
        if (edit_what == EDIT_ROW_CLUES)
            myPuzzle.SetRow_NClues (last_row, n);
        else
            myPuzzle.SetCol_NClues (last_col, n);
        return true;
    }

    public void actionPerformed (ActionEvent e)
    {
        if (e.getSource() == cancelB)
        {
            boolean do_cancel = true;
            if (GetMaxClues())
            {
                if (myPuzzle.GetMax_Col_Clues() > 0 ||
                    myPuzzle.GetMax_Row_Clues() > 0)
                {
                    if (JOptionPane.showConfirmDialog (null,
                                    "Throw away changes?",
                                    "Confirmation",
                                    JOptionPane.YES_NO_CANCEL_OPTION)
                            != JOptionPane.YES_OPTION) do_cancel = false;
                }
            }
            if (do_cancel)
            {
                // Set max col and row clues to 0 so calling method knows that
                // the dialog was canceled
                myPuzzle.SetMax_Col_Clues(0);
                myPuzzle.SetMax_Row_Clues(0);
                this.dispose();
            }
        } else if (e.getSource() == okB)
        {
            // Process that last clue
            if (!ProcessClue()) return;
            // Make sure we have at least *some* clues!
            if (GetMaxClues())
            {
                if (myPuzzle.GetMax_Col_Clues() == 0)
                    JOptionPane.showMessageDialog(this,
                        "No column clues entered!",
                        "Column Clue Error",
                        JOptionPane.WARNING_MESSAGE);
                else if (myPuzzle.GetMax_Row_Clues() == 0)
                    JOptionPane.showMessageDialog(this,
                        "No row clues entered!",
                        "Row Clue Error",
                        JOptionPane.WARNING_MESSAGE);
                else this.dispose();
            } else
            {
                JOptionPane.showMessageDialog(this,
                    "Error computing maximum row and column clues",
                    "Error",
                    JOptionPane.WARNING_MESSAGE);
            }

        } else if (e.getSource() == prevB)
        {
            GoToPrevClue();
        } else if (e.getSource() == nextB)
        {
            GoToNextClue();
        } else if (e.getSource() == rowsRB && edit_what != EDIT_ROW_CLUES)
        {
            edit_what = EDIT_ROW_CLUES;
            SetCurrentRowOrCol();
        } else if (e.getSource() == colsRB && edit_what != EDIT_COL_CLUES)
        {
            edit_what = EDIT_COL_CLUES;
            SetCurrentRowOrCol();
        } else if (e.getSource() == currTF)
        {
            GoToNextClue();
        }
    }

    private void GoToNextClue()
    {
        if (ProcessClue())
        {
            if (edit_what == EDIT_ROW_CLUES)
            {
                last_row++;
                if (last_row >= myPuzzle.GetRows())
                    last_row = myPuzzle.GetRows()-1;

            } else
            {
                last_col++;
                if (last_col >= myPuzzle.GetCols())
                    last_col = myPuzzle.GetCols()-1;
            }
            SetCurrentRowOrCol();
        }
    }

    private void GoToPrevClue()
    {
        if (ProcessClue())
        {
            if (edit_what == EDIT_ROW_CLUES)
            {
                last_row--;
                if (last_row < 0) last_row = 0;

            } else
            {
                last_col--;
                if (last_col < 0) last_col = 0;
            }
            SetCurrentRowOrCol();
        }
    }

}
