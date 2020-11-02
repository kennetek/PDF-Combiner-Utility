/*
 * Copyright (C) 2020 Kenneth Hodson
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.kennetek.pdfcombiner; 

import java.awt.Dimension;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.Level;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Stream;
import javax.swing.Box;
import javax.swing.JFileChooser;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.filechooser.FileNameExtensionFilter;
import static org.kennetek.kswing.KUtils.*;
import org.kennetek.kswing.*;
import org.apache.pdfbox.multipdf.PDFMergerUtility;

/**
 * 11/2/2020
 * 
 * Simple utility for merging any number of PDFs into a single file. 
 * The motivation for this creation was, say, a hypothetical situation where you need to submit a file for an online 
 * course, and it is due, in, lets say, 120 seconds (of course I have NEVER been in this situation, I am a great 
 * student). There is not much time to spend opening a fully featured program to combine PDFs, never mind navigating to 
 * each folder and gathering filenames to use some sort of terminal utility. So here we have this, a small GUI that will
 * save your last used directories and not ask any questions. 
 * tldr; homework speedrunning strategy
 * 
 * @author Kenneth Hodson
 */
public class PDFCombiner extends KFrame {

    // Constants
    private static final Logger LOG = Logger.getLogger(PDFCombiner.class.getName());   // logger
    private static final String PATH = System.getProperty("user.dir") + "\\";       // root path
    private static final String MSG_NO_FOLDER = "< no output folder selected >";    // message when no output folder
    private static final Dimension BUTTON =  new Dimension(25, 25);                 // icon button size
    
    // Global Variable Data
    protected List<File> listFilePDF = new ArrayList();     // pdf inputs
    protected File fileFolderOut;                           // output folder
    String strDocFolder = null;                             // string path to input folder
    String strOutFolder = null;                             // string path to output folder
    
    // Swing Components
    protected JTextArea dispFolderName;                         // Label to display the output folder
    protected KTextField inputFileName;                         // Input field for the output file name
    protected KList<File> body;                                 // List that displays input file paths
    public JFileChooser fileChooserPDF = new JFileChooser();    // input file selector
    public JFileChooser fileChooserFolder = new JFileChooser(); // output folder selector
    
    // *********************************************************************************************************** //
    // Construct and Run
    // *********************************************************************************************************** //
    
    public PDFCombiner() {
        super("PDF Combiner", new Dimension(500,500));
        setBorder(10);
        
        setEnvironment(KC.TRANSPARENT, KC.Y_AXIS, KC.FILL, 5 * KC.GAP);
        addContent(new KTitle("Select as many PDF files as you need, select a destination folder, and merge into a "
                + "single PDF. Maintains their order. This program DOES NOT WARN WHEN OVERRWRITING FILES so that files "
                + "may be saved faster. Essentially, it assumes you know what you are doing."));
        addContent(new JSeparator());
        
        KPanel header = new KPanel(KC.X_AXIS);
        header.add(new KTitle("File Paths"), 1f);
        header.add(new KButton("plus.png", BUTTON, this::buttonAdd));
        addContent(header);
        
        addContent(new KScrollPane(body = new KList()), 1f);
        body.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        KPanel buttons = new KPanel(KC.X_AXIS, KC.TRANSPARENT, 10 * KC.GAP);
        buttons.add(new KButton("arrow_up.png",     BUTTON, () -> buttonMoveUp(body.getSelectedValue())));
        buttons.add(new KButton("arrow_down.png",   BUTTON, () -> buttonMoveDown(body.getSelectedValue())));
        buttons.add(new KButton("eye.png",          BUTTON, () -> buttonView(body.getSelectedValue())));
        buttons.add(new KButton("delete.png",       BUTTON, () -> buttonRemove(body.getSelectedValue())));
        buttons.add(Box.createHorizontalGlue(), 1f);
        addContent(buttons);
        
        KPanel footer = new KPanel(KC.X_AXIS, KC.TRANSPARENT, 10 * KC.GAP, KC.FILL);
        footer.add(dispFolderName = new KTitle(MSG_NO_FOLDER), 1f);
        footer.add(new KButton("Change", this::buttonOutput));
        addContent(footer);
        
        KPanel choose = new KPanel(KC.X_AXIS, KC.TRANSPARENT, 10 * KC.GAP, KC.FILL);
        KButton bCombine = new KButton("Combine", this::buttonCombine);
        getRootPane().setDefaultButton( bCombine );
        choose.add(inputFileName = new KTextField(), 1f);
        choose.add(bCombine);
        inputFileName.linkToButton(bCombine);
        addContent(choose);
        
        initFileChooser();
    }
    
    /** Separate init for file choosers for readability */
    private void initFileChooser() {
        fileChooserPDF.setPreferredSize(new Dimension(800, 600));
        fileChooserFolder.setPreferredSize(new Dimension(800, 600));
        
        String dataPath = PATH + "data\\";
        new File(dataPath).mkdirs(); // make sure the filepath exists
        
        try (Stream<Path> paths = Files.walk(Paths.get(PATH + "data\\"))) {
            paths
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".dat"))
                .forEach((p) -> {
                    try {
                        String data = new String(Files.readAllBytes(p));
                        switch (p.getFileName().toString()) {
                            case "doc.dat" : strDocFolder = data; break;
                            case "out.dat" : strOutFolder = data; break;
                        }
                    } catch (IOException ex) {
                        LOG.log(Level.SEVERE, p.toString());
                        LOG.log(Level.SEVERE, null, ex);
                    }
                });
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
        
        FileNameExtensionFilter filter = new FileNameExtensionFilter("PDF Files", "pdf");
        fileChooserPDF.setFileFilter(filter);
        fileChooserPDF.setAcceptAllFileFilterUsed(false);
        fileChooserPDF.setMultiSelectionEnabled(true);
        if (strDocFolder != null) fileChooserPDF.setCurrentDirectory(new File(strDocFolder.trim()));
        
        fileChooserFolder.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooserFolder.setAcceptAllFileFilterUsed(false);
        if (strOutFolder != null) {
            fileFolderOut = new File(strOutFolder.trim());
            fileChooserFolder.setCurrentDirectory(fileFolderOut);
            dispFolderName.setText(fileFolderOut.getAbsolutePath().trim());
            revalidate();
            repaint();
        }
    }
    
    /**
     * Creates and shows the GUI
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        run(PDFCombiner::new);
    }
    
    // *********************************************************************************************************** //
    // Utilities
    // *********************************************************************************************************** //
    
    /** Updates labels and the filename list */
    private void refresh() {
        body.setModel(listFilePDF);
        dispFolderName.setText(fileFolderOut == null ? MSG_NO_FOLDER : fileFolderOut.getAbsolutePath().trim());
        
        if (strDocFolder != null) {
            String filepath = PATH + "data\\doc.dat";
            deleteFile(filepath);
            appendFile(filepath, strDocFolder);
        }
        
        if (strOutFolder != null) {
            String filepath = PATH + "data\\out.dat";
            deleteFile(filepath);
            appendFile(filepath, strOutFolder);
        }
    }
    
    /** Resets labels and filename list */
    private void clear() {
        listFilePDF.clear();
        inputFileName.setText("");
        refresh();
    }
    
    /**
     * Combines a sequence of PDF files into a single file. 
     * @param des       {@code String} representation of the destination file path
     * @param source    ordered {@code Array} of {@code Files} to merge in sequence
     * @throws FileNotFoundException
     * @throws IOException 
     */
    public static void combinePDF(String des, File... source) throws FileNotFoundException, IOException {
        PDFMergerUtility ut = new PDFMergerUtility();
        for (File f : source) ut.addSource(f);
        ut.setDestinationFileName(des);
        ut.mergeDocuments();
    }
    
    // *********************************************************************************************************** //
    // Button Functions
    // *********************************************************************************************************** //
    
    /**
     * The plus button above the file list. Opens a file chooser to select PDF files to include. Also saves the 
     * folder which files were selected from last, then updates file list and saves the folder name to the file system.
     */
    private void buttonAdd() {
        int returnVal = fileChooserPDF.showOpenDialog(null);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            listFilePDF.addAll(Arrays.asList(fileChooserPDF.getSelectedFiles()));
            strDocFolder = fileChooserPDF.getCurrentDirectory().getAbsolutePath();
            refresh();
        }
    }
    
    /**
     * Button to choose the output folder. Saves directory name to file system. 
     */
    private void buttonOutput() {
        int returnVal = fileChooserFolder.showOpenDialog(null);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            fileFolderOut = fileChooserFolder.getSelectedFile();
            strOutFolder = fileFolderOut.getAbsolutePath();
            refresh();
        }
    }
    
    /**
     * Button that ultimately combines the PDFs. Checks various possible error causes
     */
    private void buttonCombine() {
        if (inputFileName.getText().isBlank()) {
            errorBox("Error", "Enter a filename.");
            return;
        } 
        if (strDocFolder == null) {
            errorBox("Error", "Choose an output folder.");
            return;
        } 
        if (listFilePDF.size() < 2) {
            errorBox("Error", "Choose multiple files to merge.");
            return; 
        }

        String output = strOutFolder.trim() + "\\" + inputFileName.getText();
        if (!output.endsWith(".pdf")) output += ".pdf";
        try {
            combinePDF(output, listFilePDF.toArray(File[]::new));
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, null, ex);
            errorBox("Error", "IO error, you likely used illegal characters.");
            return; 
        }

        clear();
        openFile(output);
    }
    
    /**
     * Changes order of file list. If possible, moves selected {@code File} up by one
     * @param file  {@code File} to move
     */
    private void buttonMoveUp(File file) {
        if (file == null) return; 
        int idx = 0; 
        for (int i = 0; i < listFilePDF.size(); i++) if (listFilePDF.get(i).equals(file)) idx = i;
        if (idx > 0) {
            listFilePDF.remove(idx);
            listFilePDF.add(idx - 1, file);
            refresh();
        }
    }
    
    /**
     * Changes order of file list. If possible, moves selected {@code File} down by one
     * @param file  {@code File} to move
     */
    private void buttonMoveDown(File file) {
        if (file == null) return;
        int idx = 0; 
        for (int i = 0; i < listFilePDF.size(); i++) if (listFilePDF.get(i).equals(file)) idx = i;
        if (idx < listFilePDF.size() - 1) {
            listFilePDF.remove(idx);
            listFilePDF.add(idx + 1, file);
            refresh();
        }
    }
    
    /**
     * Open currently selected {@code File} with default application associated with it's file type. Should open 
     * Adobe Acrobat or some other PDF viewer. 
     * @param file  {@code File} to view
     */
    private void buttonView(File file) {
        if (file == null) return;
        openFile(file);
        
    }
    
    /**
     * Remove a single {@code File} from the list  
     * @param file  {@code File} to remove
     */
    private void buttonRemove(File file) {
        if (file == null) return;
        listFilePDF.remove(file);
        refresh();
    }
    
}

