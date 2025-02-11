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

import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.WritableComparable;
import org.apache.hadoop.io.WritableComparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author mark
 */
public class KeyComparator extends WritableComparator {
    private static final Logger logger = LoggerFactory.getLogger(KeyComparator.class);
    protected KeyComparator() {
        super(Text.class, true);
    }
    // TODO most likely, it will work with straight compare without the split
    @Override
    public int compare(WritableComparable t1, WritableComparable t2) {           
        String[] t1Split = t1.toString().split("\t");
        String[] t2Split = t2.toString().split("\t");
        int comp = t1Split[0].compareTo(t2Split[0]);
        if (comp != 0) {
            return comp;
        }
        logger.debug("t1={}, t2={}", t1.toString(), t2.toString());
        return t1Split[1].compareTo(t2Split[1]);
    }
}