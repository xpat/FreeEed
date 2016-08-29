/*
 *
 * Copyright SHMsoft, Inc. 
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*
 * HistoryUI.java
 *
 * Created on Jul 22, 2011, 11:58:10 AM
 */
package org.freeeed.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.RandomAccessFile;
import javax.swing.Timer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author mark
 */
public class HistoryUI extends javax.swing.JFrame implements ActionListener {

    private Timer timer = null;
    private static final int REFRESH_INTERVAL = 5000;
    private static final int HISTORY_BUFFER_SIZE = 10000;
    private static final byte[] HISTORY_BUFFER = new byte[HISTORY_BUFFER_SIZE];
    private static final String LOG_FILE = "logs/freeeed.log";
    
    private static final Logger logger = LoggerFactory.getLogger(HistoryUI.class);

    /**
     * Creates new form HistoryUI
     */
    public HistoryUI() {
        initComponents();
        timer = new Timer(REFRESH_INTERVAL, this);
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The
     * content of this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        closeButton = new javax.swing.JButton();
        historyScrollPane = new javax.swing.JScrollPane();
        historyTextArea = new javax.swing.JTextArea();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Processing history");

        closeButton.setText("Close");
        closeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                closeButtonActionPerformed(evt);
            }
        });

        historyTextArea.setColumns(20);
        historyTextArea.setLineWrap(true);
        historyTextArea.setRows(5);
        historyScrollPane.setViewportView(historyTextArea);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(closeButton)
                .addContainerGap())
            .addComponent(historyScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 822, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addComponent(historyScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 370, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(closeButton)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

	private void closeButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_closeButtonActionPerformed
            closeHistory();
	}//GEN-LAST:event_closeButtonActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton closeButton;
    private javax.swing.JScrollPane historyScrollPane;
    private javax.swing.JTextArea historyTextArea;
    // End of variables declaration//GEN-END:variables

    @Override
    public void setVisible(boolean b) {
        if (b) {
            try {
                timer.start();
                String history = tail();
                historyTextArea.setText(history);
                setLocationRelativeTo(getParent());
            } catch (Exception e) {
                logger.error("Could not display the log file in history");
            }
        }
        super.setVisible(b);
    }

    private void closeHistory() {
        timer.stop();
        setVisible(false);
        dispose();
    }

    private void refreshHistory() {
        try {
            historyTextArea.setText(tail());
        } catch (Exception e) {
            logger.error("Could not refresh the log file in history");
        }
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        refreshHistory();
    }

    @Override
    public void dispose() {
        timer.stop();
        super.dispose();
    }

    /**
     * Read the tail of the log file into the fixed buffer prepared for this purpose.
     *
     * @return String content of the tail of the file.
     * @throws IOException if anything goes wrong on file read.
     */
    private String tail() throws IOException {
        RandomAccessFile raf = null;
        try {
            raf = new RandomAccessFile(LOG_FILE, "r");
            long length = raf.length();
            long start = length - HISTORY_BUFFER_SIZE;
            if (start < 0) {
                start = 0;
            }
            raf.seek(start);
            int read = raf.read(HISTORY_BUFFER, 0, HISTORY_BUFFER_SIZE);
            if (read >= 0) {
                return new String(HISTORY_BUFFER, 0, read);
            } else {
                return new String(HISTORY_BUFFER);
            }
        } finally {
            if (raf != null) {
                raf.close();
            }
        }
    }
}
