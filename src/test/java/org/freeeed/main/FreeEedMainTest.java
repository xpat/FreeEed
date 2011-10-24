package org.freeeed.main;

import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import org.freeeed.main.PlatformUtil.PLATFORM;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

public class FreeEedMainTest {

    public FreeEedMainTest () {
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

    @Test
    public void testMain() {
        System.out.println("main");
        String[] args = new String[2];
        args[0] = "-param_file";
        String platform = PlatformUtil.getPlatform().toString().toLowerCase();
        args[1] = "sample_freeeed_" + platform + ".project";
        // delete output, so that the test should run
        try {
            if (new File(ParameterProcessing.OUTPUT_DIR + File.separator + "output").exists()) {
                Files.deleteRecursively(new File(ParameterProcessing.OUTPUT_DIR + File.separator + "output"));
            }
        } catch (IOException e) {
            e.printStackTrace(System.out);
        }
        FreeEedMain.main(args);
        // TODO - do more tests
        assertTrue (!(PlatformUtil.getPlatform() == PLATFORM.LINUX)
                || new File("freeeed_output/output/_SUCCESS").exists());
    }
}
