/**
 * *****************************************************************************
 *
 * <p>Copyright FUJITSU LIMITED 2020
 *
 * <p>Creation Date: Sep 14, 2020
 *
 * <p>*****************************************************************************
 */
package org.oscm.app.approval.controller;

import org.oscm.app.v2_0.APPlatformServiceFactory;
import org.oscm.app.v2_0.data.ControllerSettings;
import org.oscm.app.v2_0.exceptions.APPlatformException;
import org.oscm.app.v2_0.i18n.Messages;
import org.oscm.app.v2_0.intf.ControllerAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;

import javax.ejb.Singleton;

@Singleton(mappedName = "bss/controlleraccess/ess.approval")
public class ApprovalControllerAccess implements ControllerAccess {

  private static final long serialVersionUID = 2872054079271208066L;
  private static final Logger LOGGER = LoggerFactory.getLogger(ApprovalControllerAccess.class);
  
  private ControllerSettings settings;

  @Override
  public String getControllerId() {
    return ApprovalController.ID;
  }

  @Override
  public String getMessage(String locale, String key, Object... args) {
    return Messages.get(locale, key, args);
  }

  @Override
  public List<String> getControllerParameterKeys() {
    LinkedList<String> result = new LinkedList<>();
    return result;
  }
  
  public ControllerSettings getSettings() {
      if (settings == null) {
          try {
              APPlatformServiceFactory.getInstance()
                      .requestControllerSettings(getControllerId());
              LOGGER.debug(
                      "Settings were NULL. Requested from APP and got {}",
                      settings);
          } catch (APPlatformException e) {
              LOGGER.error(
                      "Error while ControllerAcces was requesting controller setting from APP",
                      e);
          }
      }
      return settings;
  }
  
  public void storeSettings(ControllerSettings settings) {
      this.settings = settings;
  }
}
