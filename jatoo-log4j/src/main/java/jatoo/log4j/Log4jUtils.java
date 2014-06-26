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
import java.net.URL;
import java.util.Enumeration;

import org.apache.log4j.LogManager;
import org.apache.log4j.PropertyConfigurator;

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

          //
          // configure from jar

          try {
            PropertyConfigurator.configure(Log4jUtils.class.getClassLoader().getResourceAsStream("META-INF/log4j/log4j.properties"));
          } catch (Exception e) {
            e.printStackTrace(System.err);
          }

          //
          // configure from config folder

          if (new File(appDirectory, "config/log4j/log4j.properties").exists()) {
            PropertyConfigurator.configure(appDirectory.getAbsolutePath() + "/config/log4j/log4j.properties");
          }

          //
          // load loggers from jar

          try {

            Enumeration<URL> loggersEnumeration = Log4jUtils.class.getClassLoader().getResources("META-INF/log4j/loggers.properties");

            while (loggersEnumeration.hasMoreElements()) {
              PropertyConfigurator.configure(loggersEnumeration.nextElement());
            }
          }

          catch (Exception e) {
            e.printStackTrace(System.err);
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
              PropertyConfigurator.configure(appDirectory.getAbsolutePath() + "/config/log4j/loggers/" + logger);
            }
          }

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

}
