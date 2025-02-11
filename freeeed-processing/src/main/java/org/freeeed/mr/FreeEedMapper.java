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
package org.freeeed.mr;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.MapWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.freeeed.data.index.LuceneIndex;
import org.freeeed.data.index.SolrIndex;
import org.freeeed.ec2.S3Agent;
import org.freeeed.mail.EmailProperties;
import org.freeeed.main.ParameterProcessing;
import org.freeeed.main.PstProcessor;
import org.freeeed.main.ZipFileProcessor;
import org.freeeed.services.Project;
import org.freeeed.services.Settings;
import org.freeeed.services.Stats;
import org.freeeed.util.OsUtil;
import org.freeeed.util.TrueZipUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.Files;

/**
 * Maps input key/value pairs to a set of intermediate key/value pairs.
 *
 * @author mark
 */
public class FreeEedMapper extends Mapper<LongWritable, Text, Text, MapWritable> {

    private final static Logger logger = LoggerFactory.getLogger(FreeEedMapper.class);
    private LuceneIndex luceneIndex;

    /**
     * Called once for each key/value pair in the input split.
     *
     * @param key Key of input.
     * @param value Value of input.
     * @param context Holds result key/value after process, as well as other parameters.
     * @throws IOException if any IO errors occurs.
     * @throws InterruptedException if thread is interrupted.
     */
    @Override
    public void map(LongWritable key, Text value, Mapper.Context context)
            throws IOException, InterruptedException {
        // package (zip) file to be processed
        Project project = Project.getCurrentProject();
        project.resetCurrentMapCount();
        String[] inputs = value.toString().split(";");
        String zipFile = inputs[0];
        // no empty or incorrect lines!
        if (zipFile.trim().isEmpty()) {
            return;
        }
        logger.info("Processing: {}", zipFile);
        if (inputs.length >= 3) {
            project.setMapItemStart(Integer.parseInt(inputs[1]));
            project.setMapItemEnd(Integer.parseInt(inputs[2]));
            logger.info("From {} to {}", project.getMapItemStart(), project.getMapItemEnd());
        }
        int filesInZip = new TrueZipUtil().countFiles(zipFile);
        Stats.getInstance().setCurrentItemTotal(filesInZip);
        Stats.getInstance().setZipFileName(zipFile);
        
        project.setupCurrentCustodianFromFilename(zipFile);
        logger.info("Will use current custodian: {}", project.getCurrentCustodian());
        // if we are in Hadoop, copy to local tmp         
        if (project.isEnvHadoop()) {
            String extension = org.freeeed.services.Util.getExtension(zipFile);
            String tmpDir = ParameterProcessing.TMP_DIR_HADOOP;
            File tempZip = File.createTempFile("freeeed", ("." + extension), new File(tmpDir));
            tempZip.delete();
            if (project.isFsHdfs() || project.isFsLocal()) {
                String cmd = "hadoop fs -copyToLocal " + zipFile + " " + tempZip.getPath();
                OsUtil.runCommand(cmd);
            } else if (project.isFsS3()) {
                S3Agent s3agent = new S3Agent();
                s3agent.getStagedFileFromS3(zipFile, tempZip.getPath());
            }
            
            zipFile = tempZip.getPath();
        }
        
        if (PstProcessor.isPST(zipFile)) {
            try {
                new PstProcessor(zipFile, context, luceneIndex).process();
            } catch (Exception e) {
                logger.error("Problem with PST processing...", e);
            }
        } else {
            logger.info("Will create Zip File processor for: {}", zipFile);
            // process archive file
            ZipFileProcessor processor = new ZipFileProcessor(zipFile, context, luceneIndex);
            processor.process(false, null);
        }
    }

    // TODO move indexing to reducer
    @Override
    protected void setup(Mapper.Context context) {
        
        String settingsStr = context.getConfiguration().get(ParameterProcessing.SETTINGS_STR);
        Settings settings = Settings.loadFromString(settingsStr);
        Settings.setSettings(settings);
        // System.out.println("Mapper setup settings = " + Settings.getSettings());

        String projectStr = context.getConfiguration().get(ParameterProcessing.PROJECT);
        Project project = Project.loadFromString(projectStr);
        
        if (project.isEnvHadoop()) {
            // we need the system check only if we are not in local mode
            OsUtil.systemCheck();
            List <String> status = OsUtil.getSystemSummary();
            for (String stat: status) {
                logger.info(stat);
            }
            Configuration conf = context.getConfiguration();
            String taskId = conf.get("mapred.task.id");
            if (taskId != null) {
                Settings.getSettings().setProperty("mapred.task.id", taskId);
            }
        
            String metadataFileContents = context.getConfiguration().get(EmailProperties.PROPERTIES_FILE);
            try {
                new File(EmailProperties.PROPERTIES_FILE).getParentFile().mkdirs();
                Files.write(metadataFileContents.getBytes(), new File(EmailProperties.PROPERTIES_FILE));
            } catch (IOException e) {
                logger.error("Problem writing the email properties file to disk", e);
            }
        }
                
        if (project.isLuceneIndexEnabled()) {
            luceneIndex = new LuceneIndex(settings.getLuceneIndexDir(),
                    project.getProjectCode(), "" + context.getTaskAttemptID());
            luceneIndex.init();
        }
    }
    
    @Override
    @SuppressWarnings("unchecked")
    protected void cleanup(Mapper.Context context) {
        Stats stats = Stats.getInstance();
        
        SolrIndex.getInstance().flushBatchData();
        
        System.out.println("In zip file " + stats.getZipFileName()
                + " processed " + stats.getItemCount() + " items");
        
        if (luceneIndex != null) {
            
            try {
                luceneIndex.destroy();
                String zipFileName = luceneIndex.createIndexZipFile();
                
                String hdfsZipFileName = "/"
                        + Settings.getSettings().getLuceneIndexDir() + File.separator
                        + Project.getCurrentProject().getProjectCode() + File.separator
                        + context.getTaskAttemptID() + ".zip";
                
                String removeOldZip = "hadoop fs -rm " + hdfsZipFileName;
                OsUtil.runCommand(removeOldZip);
                
                String cmd = "hadoop fs -copyFromLocal " + zipFileName + " " + hdfsZipFileName;
                OsUtil.runCommand(cmd);
            } catch (IOException e) {
                logger.error("Error generating lucene index data", e);                
            }
        }
    }   
}
