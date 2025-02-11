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
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.freeeed.main;


import org.freeeed.services.Settings;
import org.junit.*;

/**
 *
 * @author mark
 */
public class VersionUpdateTest {

    public VersionUpdateTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of isNewVersionAvailable method, of class VersionUpdate.
     * @throws java.io.IOException
     */
    @Test
    public void testIsNewVersionAvailable() throws Exception {
        System.out.println("isNewVersionAvailable");
        Settings.load();
        VersionUpdate instance = new VersionUpdate();
        String updateInfo = instance.getUpdateInfo();
        System.out.println(updateInfo);
        boolean result = instance.isNewVersionAvailable();
        System.out.println(result);
    }
}
