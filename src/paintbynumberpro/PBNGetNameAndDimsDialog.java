package paintbynumberpro;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

import javax.swing.*;
import java.awt.event.*;
import java.awt.*;

/**
 *
 * @author user
 */
public class PBNGetNameAndDimsDialog extends JDialog implements ActionListener
{
    private PBNPuzzle myPuzzle;
    private JTextField sourceTF;
    private JTextField rowsTF;
    private JTextField colsTF;
    private JButton okB;
    private JButton cancelB;

    PBNGetNameAndDimsDialog (PBNPuzzle thePuzzle)
    {
        if (thePuzzle == null) return;
        myPuzzle = thePuzzle;

        setTitle ("Enter Puzzle Name and Dimensions");
        setModalityType (Dialog.ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

        JLabel sourceL = new JLabel ("Name of puzzle:");
        JLabel colsL = new JLabel ("Cols:");
        JLabel rowsL = new JLabel ("Rows:");
        sourceTF = new JTextField ("My Puzzle");
        rowsTF = new JTextField ("10");
        colsTF = new JTextField ("10");
        okB = new JButton ("OK");
        cancelB = new JButton ("Cancel");
        okB.addActionListener (this);
        cancelB.addActionListener (this);

        JPanel panel = new JPanel();
        GroupLayout gl = new GroupLayout(panel);
        panel.setLayout(gl);

        gl.setAutoCreateGaps(true);
        gl.setAutoCreateContainerGaps(true);

        gl.setHorizontalGroup(
           gl.createParallelGroup(GroupLayout.Alignment.TRAILING)
                .addGroup (gl.createParallelGroup (GroupLayout.Alignment.LEADING)
                    .addComponent (sourceL)
                    .addComponent (sourceTF)
                )
                .addGroup (gl.createSequentialGroup()
                    .addComponent (colsL)
                    .addComponent (colsTF)
                    .addComponent (rowsL)
                    .addComponent (rowsTF)
                )
                .addGroup (gl.createSequentialGroup()
                    .addComponent (cancelB)
                    .addComponent (okB)
                )
        );
        gl.setVerticalGroup(
           gl.createSequentialGroup()
                .addComponent(sourceL)
                .addComponent(sourceTF)
                .addGroup (gl.createParallelGroup(GroupLayout.Alignment.CENTER)
                    .addComponent (colsL)
                    .addComponent (colsTF)
                    .addComponent (rowsL)
                    .addComponent (rowsTF)
                )
                .addGroup (gl.createParallelGroup(GroupLayout.Alignment.CENTER)
                    .addComponent (cancelB)
                    .addComponent (okB)
                )
        );
        
        getContentPane().add(panel);

        RepackWindowSize(true);

        Dimension dim = sourceTF.getPreferredSize();
        dim.width = 500;
        sourceTF.setMaximumSize(dim);
        dim = colsTF.getPreferredSize();
        dim.width = 200;
        colsTF.setMaximumSize(dim);
        dim = rowsTF.getPreferredSize();
        dim.width = 200;
        rowsTF.setMaximumSize(dim);

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

    public void actionPerformed (ActionEvent e)
    {
        if (e.getSource() == okB) HandleOKButton ();
        else if (e.getSource() == cancelB) this.dispose();
    }

    private void HandleOKButton ()
    {
        int c = 0;
        int r = 0;
        // Try to read the cols and rows from the JTextFields
        try
        {
            c = Integer.parseInt (colsTF.getText());
            r = Integer.parseInt (rowsTF.getText());
        }
        catch (NumberFormatException  nfe)
        {
            JOptionPane.showMessageDialog(this,
                "Invalid text for either cols or rows",
                "Error",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (c <= 0 || r <= 0)
        {
            JOptionPane.showMessageDialog(this,
                "Cols and rows must be > 0",
                "Error",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        if ((c%5) != 0 || (r%5) != 0)
        {
            JOptionPane.showMessageDialog(this,
                "Cols and rows must be factors of 5!",
                "Error",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        // If we've gotten this far, then we have valid rows and cols
        myPuzzle.SetPuzzleNameAndDims (sourceTF.getText(), c, r);

        // Dismiss this dialog
        this.dispose();
    }
}
