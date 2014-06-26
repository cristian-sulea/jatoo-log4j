/*
 * Copyright (C) 2014 Cristian Sulea ( http://cristian.sulea.net )
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
 * @version 4.3, June 26, 2014
 */
public class Log4jUtils {

  public static final String SYSTEM_PROPERTY_LOGS_DIRECTORY = "logs.directory";

  private static boolean isInitialized = false;

  public static void init() {
    init(new File(System.getProperty("user.dir")));
  }

  public static void init(File workingDirectory) {
    init(workingDirectory, new File(System.getProperty("user.dir")));
  }

  public static void init(File workingDirectory, File appDirectory) {

    if (!isInitialized) {
      synchronized (Log4jUtils.class) {
        if (!isInitialized) {

          //
          // ensure logs directory
          // and add him to the system properties

          File logsDirectory = new File(workingDirectory, "logs");
          logsDirectory.mkdirs();

          System.setProperty(SYSTEM_PROPERTY_LOGS_DIRECTORY, logsDirectory.getAbsolutePath());

          Properties properties = new Properties();

          //
          // configure from jar

          updatePropertiesFromURL(properties, Log4jUtils.class.getClassLoader().getResource("META-INF/log4j/log4j.properties"), true);

          //
          // configure from config folder

          updatePropertiesFromFile(properties, appDirectory.getAbsolutePath() + "/config/log4j/log4j.properties", false);

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

          String[] loggers = new File(appDirectory, "config/log4j/loggers/").list(new FilenameFilter() {
            public boolean accept(File dir, String name) {
              return name.endsWith(".properties");
            }
          });

          if (loggers != null && loggers.length > 0) {
            for (String logger : loggers) {
              updatePropertiesFromFile(properties, appDirectory.getAbsolutePath() + "/config/log4j/loggers/" + logger, false);
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

  public static boolean isInitialized() {
    synchronized (Log4jUtils.class) {
      return isInitialized;
    }
  }

  public static void destroy() {

    if (isInitialized) {
      synchronized (Log4jUtils.class) {
        if (isInitialized) {

          System.clearProperty(SYSTEM_PROPERTY_LOGS_DIRECTORY);

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
  private static void updatePropertiesFromURL(Properties properties, URL url, boolean logError) {

    LogLog.debug("Reading configuration file [" + url + "].");

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
        LogLog.error("Could not read configuration file [" + url + "].", e);
        LogLog.error("Ignoring configuration file [" + url + "].");
      }
    }

    finally {
      if (stream != null) {
        try {
          stream.close();
        } catch (InterruptedIOException ignore) {
          Thread.currentThread().interrupt();
        } catch (IOException | RuntimeException ignore) {}
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
  private static void updatePropertiesFromFile(Properties properties, String file, boolean logError) {

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
