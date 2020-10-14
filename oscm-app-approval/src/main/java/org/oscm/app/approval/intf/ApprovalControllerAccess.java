/**
 * *****************************************************************************
 *
 * <p>Copyright FUJITSU LIMITED 2020
 *
 * <p>Creation Date: 13.10.2020
 *
 * <p>*****************************************************************************
 */
package org.oscm.app.approval.intf;

import javax.ejb.Remote;

import org.oscm.app.v2_0.data.ControllerSettings;

/** @author goebel */
@Remote
public interface ApprovalControllerAccess  {
  public static final String ID = "ess.approval";
  
  public ControllerSettings getSettings();
}
