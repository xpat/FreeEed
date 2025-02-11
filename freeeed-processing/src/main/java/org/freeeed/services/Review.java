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
package org.freeeed.services;

import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;

import org.freeeed.main.ParameterProcessing;

/**
 *
 * @author mark
 */
public class Review {

    public static boolean deliverFiles() throws IOException {
        Project project = Project.getCurrentProject();
        File outputFolder = new File(project.getResultsDir());
        File[] files = outputFolder.listFiles();
        if (files == null || files.length == 0) {
            return false;
        }
        // TODO find a way to see that MR job is running and results are not ready yet

        // if I have a "part...." file there, rename it to output.csv
        for (File file : files) {
            String fileName = file.getName();
            if (fileName.startsWith("part-r")) {
                String newFileName = file.getParent() + File.separator
                        + fileName.replace("part-r", "load") 
                        + ParameterProcessing.METADATA_FILE_EXT;
                Files.move(file, new File(newFileName));
            }
            if (file.getName().equals("_SUCCESS")) {
                file.delete();
            }

        }
        if (Stats.getInstance().getStatsFile().exists()) {
            Files.move(Stats.getInstance().getStatsFile(), new File(outputFolder.getPath() + "/report.txt"));
        }
        return true;
    }
}
