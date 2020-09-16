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

import org.oscm.app.v2_0.i18n.Messages;
import org.oscm.app.v2_0.intf.ControllerAccess;

import java.util.LinkedList;
import java.util.List;

public class ApprovalControllerAccess implements ControllerAccess {

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
}
