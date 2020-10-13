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

import org.oscm.app.approval.controller.ApprovalControllerAccess;
import org.oscm.app.v2_0.intf.APPlatformController;

/** @author goebel */
@Remote
public interface ApprovalController extends APPlatformController {
  public static final String ID = "ess.approval";
  
  public ApprovalControllerAccess getControllerAccess();
}
