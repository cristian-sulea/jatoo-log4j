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
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.Properties;

import org.apache.log4j.LogManager;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.helpers.LogLog;

/**
 * A collection of utility methods to ease the work with Apache Log4j™ logging
 * library.
 * 
 * @author <a href="http://cristian.sulea.net" rel="author">Cristian Sulea</a>
 * @version 4.4, September 14, 2015
 */
public final class Log4jUtils {

  /** The logs folder parameter used in configuration files. */
  public static final String SYSTEM_PROPERTY_LOGS_FOLDER = "logs.folder";

  /** Be sure that the init/destroy methods are executed only once. */
  private static boolean isInitialized = false;

  /**
   * Utility classes should not have a public or default constructor.
   */
  private Log4jUtils() {}

  /**
   * Initializes Log4J with the <code>logs</code> folder created in the working
   * directory (where the JVM is started).
   */
  public static void init() {
    init(new File(System.getProperty("user.dir")));
  }

  /**
   * Initializes Log4J with the <code>logs</code> folder created in the
   * specified folder.
   * 
   * @param logsFolder
   *          the parent folder of the <code>logs</code> folder
   */
  public static void init(final File logsFolder) {
    init(logsFolder, new File(System.getProperty("user.dir")));
  }

  /**
   * Initializes Log4J with the <code>logs</code> folder created in the
   * specified folder, trying to search for new properties or loggers in the
   * second specified folder.
   * 
   * @param logsFolder
   *          the parent folder of the <code>logs</code> folder
   * @param configFolder
   *          the parent folder of the <code>config</code> folder
   */
  public static void init(final File logsFolder, final File configFolder) {

    if (!isInitialized) {
      synchronized (Log4jUtils.class) {
        if (!isInitialized) {

          //
          // ensure logs folder
          // and add him to the system properties

          File logsFolderFile = new File(logsFolder, "logs");
          logsFolderFile.mkdirs();

          System.setProperty(SYSTEM_PROPERTY_LOGS_FOLDER, logsFolderFile.getAbsolutePath());

          Properties properties = new Properties();

          //
          // configure from jar

          updatePropertiesFromURL(properties, Log4jUtils.class.getClassLoader().getResource("META-INF/log4j/log4j.properties"), true);

          //
          // configure from config folder

          updatePropertiesFromFile(properties, configFolder.getAbsolutePath() + "/config/log4j/log4j.properties", false);

          //
          // load loggers from jar

          try {

            Enumeration<URL> loggersEnumeration = Log4jUtils.class.getClassLoader().getResources("META-INF/log4j/loggers.properties");

            while (loggersEnumeration.hasMoreElements()) {
              updatePropertiesFromURL(properties, loggersEnumeration.nextElement(), true);
            }
          }

          catch (Exception e) {
            LogLog.error("Could not load loggers from jar(s) [META-INF/log4j/loggers.properties].", e);
          }

          //
          // load loggers from config folder

          String[] loggers = new File(configFolder, "config/log4j/loggers/").list(new FilenameFilter() {
            public boolean accept(final File dir, final String name) {
              return name.endsWith(".properties");
            }
          });

          if (loggers != null && loggers.length > 0) {
            for (String logger : loggers) {
              updatePropertiesFromFile(properties, configFolder.getAbsolutePath() + "/config/log4j/loggers/" + logger, false);
            }
          }

          //
          // configure

          PropertyConfigurator.configure(properties);

          //
          // done

          isInitialized = true;
        }
      }
    }
  }

  /**
   * @return <code>true</code> if the <code>Log4J</code> is initialized;
   *         <code>false</code> otherwise
   */
  public static boolean isInitialized() {
    synchronized (Log4jUtils.class) {
      return isInitialized;
    }
  }

  /**
   * Release all the resources.
   */
  public static void destroy() {

    if (isInitialized) {
      synchronized (Log4jUtils.class) {
        if (isInitialized) {

          System.clearProperty(SYSTEM_PROPERTY_LOGS_FOLDER);

          LogManager.shutdown();

          isInitialized = false;
        }
      }
    }
  }

  /**
   * Update the {@link Properties} with values from the specified {@link URL}.
   * 
   * @param properties
   *          the {@link Properties} object to be updated
   * @param url
   *          the configuration file with the new values
   * @param logError
   *          <code>true</code> if an error must be logged when something goes
   *          wrong, like file missing for example
   */
  private static void updatePropertiesFromURL(final Properties properties, final URL url, final boolean logError) {

    LogLog.debug("Reading configuration file... [" + url + "]");

    //
    // read properties from provided URL

    Properties p = new Properties();

    InputStream stream = null;
    URLConnection connection = null;

    try {

      connection = url.openConnection();
      connection.setUseCaches(false);

      stream = connection.getInputStream();

      p.load(stream);
    }

    catch (Exception e) {

      if (e instanceof InterruptedIOException || e instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }

      if (logError) {
        LogLog.error("Could not read configuration file... [" + url + "]", e);
        LogLog.error("Ignoring configuration file... [" + url + "]");
      }
    }

    finally {
      if (stream != null) {

        try {
          stream.close();
        }

        catch (InterruptedIOException e) {
          Thread.currentThread().interrupt();
          if (logError) {
            LogLog.error("Hmm... In this case (InterruptedIOException) call also #interrupt() on the current thread! [" + url + "]", e);
          }
        }

        catch (IOException e) {
          if (logError) {
            LogLog.error("The stream.close() call failed. [" + url + "]", e);
          }
        }

        catch (RuntimeException e) {
          if (logError) {
            LogLog.error("This is strange... How can i have something else (except an io exception) on a stream.close() call? [" + url + "]", e);
          }
        }
      }
    }

    //
    // update properties

    for (String key : p.stringPropertyNames()) {
      properties.setProperty(key, p.getProperty(key));
    }
  }

  /**
   * Update the {@link Properties} with values from the specified file.
   * 
   * @param properties
   *          the {@link Properties} object to be updated
   * @param file
   *          the configuration file with the new values
   * @param logError
   *          <code>true</code> if an error must be logged when something goes
   *          wrong, like file missing for example
   */
  private static void updatePropertiesFromFile(final Properties properties, final String file, final boolean logError) {

    URL url;

    try {
      url = new File(file).toURI().toURL();
    }

    catch (MalformedURLException e) {

      LogLog.error("Could not convert file to URL [" + file + "].", e);
      LogLog.error("Ignoring configuration file [" + file + "].");

      return;
    }

    updatePropertiesFromURL(properties, url, logError);
  }

}
