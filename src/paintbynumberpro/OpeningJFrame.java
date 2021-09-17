/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * OpeningJFrame.java
 *
 * Created on Dec 30, 2011, 2:45:39 PM
 */

package paintbynumberpro;

/**
 *
 * @author Lynne
 */
public class OpeningJFrame extends javax.swing.JFrame {

    /** Creates new form OpeningJFrame */
    public OpeningJFrame (String versionStr) {
        initComponents();
        String existingTitle = getTitle();
        setTitle (existingTitle + " " + versionStr);
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        Open_JButton = new javax.swing.JButton();
        EnterPuzzle_JButton = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();
        Exit_JButton = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("Paint By Number Pro");
        setResizable(false);

        Open_JButton.setText("Open Puzzle...");
        Open_JButton.setToolTipText("Open an existing puzzle file");
        Open_JButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                Open_JButtonActionPerformed(evt);
            }
        });

        EnterPuzzle_JButton.setText("Enter New Puzzle...");
        EnterPuzzle_JButton.setToolTipText("Create a new puzzle by copying clues from an existing puzzle (e.g. from a book)");
        EnterPuzzle_JButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                EnterPuzzle_JButtonActionPerformed(evt);
            }
        });

        jButton2.setText("Create New Puzzle...");
        jButton2.setToolTipText("Create a brand new puzzle by drawing a picture");
        jButton2.setEnabled(false);

        Exit_JButton.setText("Exit");
        Exit_JButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                Exit_JButtonActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(layout.createSequentialGroup()
                        .addContainerGap()
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING, false)
                            .add(org.jdesktop.layout.GroupLayout.LEADING, jButton2, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .add(org.jdesktop.layout.GroupLayout.LEADING, EnterPuzzle_JButton, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .add(org.jdesktop.layout.GroupLayout.LEADING, Open_JButton, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 224, Short.MAX_VALUE)))
                    .add(layout.createSequentialGroup()
                        .add(93, 93, 93)
                        .add(Exit_JButton)))
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(Open_JButton)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(EnterPuzzle_JButton)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jButton2)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(Exit_JButton)
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void Exit_JButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_Exit_JButtonActionPerformed
    {//GEN-HEADEREND:event_Exit_JButtonActionPerformed
        PaintByNumberPro.CloseApplication(true);
    }//GEN-LAST:event_Exit_JButtonActionPerformed

    private void Open_JButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_Open_JButtonActionPerformed
    {//GEN-HEADEREND:event_Open_JButtonActionPerformed
        PaintByNumberPro.OpenAPuzzle();
    }//GEN-LAST:event_Open_JButtonActionPerformed

    private void EnterPuzzle_JButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_EnterPuzzle_JButtonActionPerformed
        PaintByNumberPro.EnterANewPuzzle();
    }//GEN-LAST:event_EnterPuzzle_JButtonActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton EnterPuzzle_JButton;
    private javax.swing.JButton Exit_JButton;
    private javax.swing.JButton Open_JButton;
    private javax.swing.JButton jButton2;
    // End of variables declaration//GEN-END:variables

}