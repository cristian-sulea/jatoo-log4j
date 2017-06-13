/*
 * Copyright (C) Cristian Sulea ( http://cristian.sulea.net )
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jatoo.log4j;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * {@link Log4jUtils} test class.
 * 
 * @author <a href="http://cristian.sulea.net" rel="author">Cristian Sulea</a>
 * @version 1.0, June 24, 2014
 */
public class Log4jUtilsTest {

  private static final File WORKING_DIRECTORY = new File("target");
  private static final File APP_DIRECTORY = new File("target");

  private static final File LOGS1_DIRECTORY = new File("logs");
  private static final File LOGS2_DIRECTORY = new File(WORKING_DIRECTORY, "logs");

  @BeforeClass
  public static void beforeClass() {}

  @AfterClass
  public static void afterClass() {}

  @Before
  public void before() {
    Assert.assertNull(System.getProperty(Log4jUtils.SYSTEM_PROPERTY_LOGS_FOLDER));
  }

  @After
  public void after() throws Exception {

    Log4jUtils.destroy();

    Assert.assertNull(System.getProperty(Log4jUtils.SYSTEM_PROPERTY_LOGS_FOLDER));

    FileUtils.deleteDirectory(LOGS1_DIRECTORY);
    Assert.assertFalse(LOGS1_DIRECTORY.exists());

    FileUtils.deleteDirectory(LOGS2_DIRECTORY);
    Assert.assertFalse(LOGS2_DIRECTORY.exists());
  }

  @Test
  public void testInit() {

    Log4jUtils.init();

    Assert.assertNotNull(System.getProperty(Log4jUtils.SYSTEM_PROPERTY_LOGS_FOLDER));
  }

  @Test
  public void testInitWithWorkingDirectory() {

    Log4jUtils.init(WORKING_DIRECTORY);

    Assert.assertEquals(new File(WORKING_DIRECTORY, "logs").getAbsolutePath(), System.getProperty("logs.folder"));
    Assert.assertTrue(new File(WORKING_DIRECTORY, "logs").exists());
  }

  @Test
  public void testInitWithWorkingDirectoryAndAppDirectory() {

    Log4jUtils.init(WORKING_DIRECTORY, APP_DIRECTORY);

    Assert.assertEquals(new File(WORKING_DIRECTORY, "logs").getAbsolutePath(), System.getProperty("logs.folder"));
    Assert.assertTrue(new File(WORKING_DIRECTORY, "logs").exists());
  }

  @Test
  public void testOneLogger() {

    Log4jUtils.init(WORKING_DIRECTORY);

    Log logger = LogFactory.getLog(Log4jUtilsTest.class);

    logger.debug("debug");
    logger.info("info");
    logger.warn("warn");
    logger.error("error");
    logger.fatal("fatal");
  }

  @Test
  public void testManyLoggers() {

    Log4jUtils.init(WORKING_DIRECTORY);

    //
    // logger 1

    Log logger1 = LogFactory.getLog("logger1");

    logger1.debug("debug");
    logger1.info("info");
    logger1.warn("warn");
    logger1.error("error");
    logger1.fatal("fatal");

    //
    // logger 2

    Log logger2 = LogFactory.getLog("logger2");

    logger2.debug("debug");
    logger2.info("info");
    logger2.warn("warn");
    logger2.error("error");
    logger2.fatal("fatal");
  }

}
