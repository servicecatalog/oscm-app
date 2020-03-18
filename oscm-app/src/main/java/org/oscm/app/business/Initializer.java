/*******************************************************************************
 *
 *  Copyright FUJITSU LIMITED 2018
 *
 *  Creation Date: 2014-05-16
 *
 *******************************************************************************/
package org.oscm.app.business;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.*;
import java.io.File;
import java.io.InputStream;
import java.util.Collection;

import static org.oscm.app.common.Constants.APPLICATION_SERVER_HOME_CONSTANT;

@Singleton
@Startup
public class Initializer {

  private static final Logger logger = LoggerFactory.getLogger(Initializer.class);

  // Default name of log4j template
  private String LOG4J_TEMPLATE = "log4j2.properties.template";

  // Timer based log file monitoring
  private long TIMER_DELAY_VALUE = 60000;

  @Resource private TimerService timerService;
  private File logFile;
  private long logFileLastModified = 0;
  private boolean logFileWarning = false;

  @PostConstruct
  private void postConstruct() {
    try {
      // Get default config folder of GF instance
      String instanceRoot = System.getProperty(APPLICATION_SERVER_HOME_CONSTANT);
      String controllerId = "app.core";
      if (instanceRoot != null) {
        File root = new File(instanceRoot);
        if (root.isDirectory()) {
          // Determine log file
          String filePath = "/conf/log4j2." + controllerId + ".properties";
          logFile = new File(root, filePath);

          // If the target file does not exist we will provide it once
          // from the template
          if (!logFile.exists()) {
            publishTemplateFile();
          }

          // Read configuration
          handleOnChange(logFile);

          // And init timer based service
          logger.debug("Enable timer service for monitoring modification of " + logFile.getPath());
          initTimer();

        } else {
          logger.error("Failed to initialize log file: invalid instanceRoot " + instanceRoot);
          logFile = null;
        }

      } else {
        logger.error("Failed to initialize log file: missing instanceRoot");
      }

    } catch (Exception e) {
      logger.error("Failed to initialize log file", e);
      logFile = null;
    }
  }

  /** Initialize timer service. */
  private void initTimer() {
    Collection<?> timers = timerService.getTimers();
    if (timers.isEmpty()) {
      timerService.createTimer(0, TIMER_DELAY_VALUE, null);
    }
  }

  /** Copy template file to default destination */
  private void publishTemplateFile() {
    try (InputStream is = getClass().getClassLoader().getResourceAsStream(LOG4J_TEMPLATE)) {
      if (is == null) {
        logger.warn("Template file not found: " + LOG4J_TEMPLATE);
      }
      else if (logFile.getParentFile().exists()) {
        FileUtils.writeByteArrayToFile(logFile, IOUtils.toByteArray(is));
      }
    } catch (Exception e) {
      logger.error(
          "Failed to publish template file from "
              + LOG4J_TEMPLATE
              + " to "
              + logFile.getAbsolutePath(),
          e);
    }
  }

  /** Handles the timer event. */
  @Timeout
  public void handleTimer(@SuppressWarnings("unused") Timer timer) {
    if (logFile != null) {
      handleOnChange(logFile);
    }
  }

  /** On change event */
  void handleOnChange(File logFile) {
    try {
      long lastModified = logFile.lastModified();
      if (lastModified > logFileLastModified) {
        logFileLastModified = lastModified;
        logger.debug("Reload log4j configuration from " + logFile.getAbsolutePath());
        final LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        ctx.setConfigLocation(logFile.toURI());
        logFileWarning = false;
      }
    } catch (Exception e) {
      if (!logFileWarning) {
        logFileWarning = true;
        logger.error(logFile.getAbsolutePath(), e);
      }
    }
  }
}
