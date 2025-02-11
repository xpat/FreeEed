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
package org.freeeed.main;

import java.io.*;
import java.text.DecimalFormat;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.swing.JOptionPane;

import org.freeeed.services.Project;
import org.freeeed.services.Settings;
import org.freeeed.ui.StagingProgressUI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Package the input directories into zip archives. Zip is selected because it allows comments, which contain path,
 * custodian, and later- forensics information.
 */
public class PackageArchive {

    private static final Logger logger = LoggerFactory.getLogger(PackageArchive.class);
    private double gigsPerArchive;
    // these are needed for the internal working of the code, not for outside	
    private int packageFileCount = 0;
    private DecimalFormat packageFileNameFormat = new DecimalFormat("input00000");
    private String packageFileNameSuffix = ".zip";
    static final int BUFFER = 4096;
    static byte data[] = new byte[BUFFER];
    private int filesCount;
    private ZipOutputStream zipOutputStream;
    private FileOutputStream fileOutputStream;
    private String zipFileName;
    private String rootDir;
    private boolean fileSizeReached;
    private StagingProgressUI stagingUI;
    private boolean interrupted = false;
    
    public PackageArchive(StagingProgressUI stagingUI) {
        this.stagingUI = stagingUI;
        init();
    }
    
    private void init() {
        gigsPerArchive = Project.getCurrentProject().getGigsPerArchive();
    }
    
    public void packageArchive(String dir) throws Exception {
        rootDir = dir;
        // separate directories will go into separate zip files
        resetZipStreams();
        packageArchiveRecursively(new File(dir));
        if (filesCount > 0) {
            logger.info("Wrote {} files", filesCount);
        }
        zipOutputStream.close();
        fileOutputStream.close();
        writeInventory();
    }

    /**
     * TODO: this is taken from an (old) article on compression:
     * http://java.sun.com/developer/technicalArticles/Programming/compression/ can it be improved?
     *
     * @param file
     * @param zipOutputStream
     * @throws IOException
     */
    private void packageArchiveRecursively(File file) throws Exception {
        if (file.isFile()) {
            if (stagingUI != null) {
                stagingUI.updateProcessingFile(file.getAbsolutePath());
            }
            double newSizeGigs = (1.
                    * (file.length() + new File(zipFileName).length()))
                    / ParameterProcessing.ONE_GIG;            
            if (newSizeGigs > gigsPerArchive
                    && filesCount > 0) {
                fileSizeReached = true;
                resetZipStreams();
            }
            ++filesCount;
            FileInputStream fileInputStream = new FileInputStream(file);
            BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream, BUFFER);
            
            File rootFile = new File(rootDir);
            String parent = rootFile.getParent();
            String relativePath = file.getPath();
            if (parent != null) {
                relativePath = file.getPath().substring(new File(rootDir).getParent().length() + 1);
            }
            
            ZipEntry zipEntry = new ZipEntry(relativePath);
            zipOutputStream.putNextEntry(zipEntry);
            // TODO - add zip file comment: custodian, path, other info
            int count;
            while ((count = bufferedInputStream.read(data, 0,
                    BUFFER)) != -1) {
                zipOutputStream.write(data, 0, count);
            }
            bufferedInputStream.close();
            fileInputStream.close();
            
            if (stagingUI != null) {
                stagingUI.updateProgress(file.length());
            }
            
        } else if (file.isDirectory()) {
            // add all files in a directory
            if (file.canRead() && file.listFiles() != null) {
                for (File f : file.listFiles()) {
                    if (interrupted) {
                        break;
                    }
                    
                    packageArchiveRecursively(f);
                }
            } else {
                JOptionPane.showMessageDialog(null, "You don't have read access to this file:\n"
                        + file.getPath() + "\n"
                        + "No files will be staged. Please fix the permissions first");
                throw new Exception("No read access to file " + file.getPath());
            }
        }
    }
    
    private void resetZipStreams() throws Exception {
        ++packageFileCount;
        if (zipOutputStream != null) {
            zipOutputStream.close();
        }
        if (fileOutputStream != null) {
            fileOutputStream.close();
        }
        String stagingDir = Project.getCurrentProject().getStagingDir();
        new File(stagingDir).mkdirs();
        zipFileName = stagingDir
                + System.getProperty("file.separator")
                + packageFileNameFormat.format(packageFileCount)
                + Project.getCurrentProject().getFormattedCustodian()
                + packageFileNameSuffix;
        fileOutputStream = new FileOutputStream(zipFileName);
        zipOutputStream = new ZipOutputStream(new BufferedOutputStream(fileOutputStream));
        if (filesCount > 0 && fileSizeReached) {
            logger.info("Wrote {} files ", filesCount);
        }
        logger.info("Writing output to staging: {}", zipFileName);
        filesCount = 0;
        fileSizeReached = false;
    }

    /**
     * Write the list of zip files that has been created - it will be used by Hadoop
     */
    public static void writeInventory() throws IOException {
        Project project = Project.getCurrentProject();
        String stagingDir = project.getStagingDir();
        File[] zipFiles = new File(stagingDir).listFiles();
        File inventory = new File(project.getInventoryFileName());
        BufferedWriter out = new BufferedWriter(new FileWriter(inventory, false));
        for (File file : zipFiles) {
            if (file.getName().endsWith(".zip")) {
                out.write(stagingDir + System.getProperty("file.separator")
                        + file.getName() + System.getProperty("line.separator"));
            }
        }
        out.close();
    }
    
    public void setInterrupted(boolean interrupted) {
        this.interrupted = interrupted;
    }
}
