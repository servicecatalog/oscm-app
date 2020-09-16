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

import org.oscm.app.approval.data.Server;
import org.oscm.app.v2_0.APPlatformServiceFactory;
import org.oscm.app.v2_0.data.ProvisioningSettings;
import org.oscm.app.v2_0.exceptions.APPlatformException;
import org.oscm.app.v2_0.intf.APPlatformService;
import org.oscm.app.v2_0.intf.InstanceAccess;
import org.oscm.app.v2_0.intf.ServerInformation;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ApprovalInstanceAccess implements InstanceAccess {

  private APPlatformService platformService;

  @PostConstruct
  public void initialize() {
    platformService = APPlatformServiceFactory.getInstance();
  }

  @Override
  public List<? extends ServerInformation> getServerDetails(
      String instanceId, String subscriptionId, String organizationId) throws APPlatformException {
    ProvisioningSettings settings =
        platformService.getServiceInstanceDetails(
            ApprovalController.ID, instanceId, subscriptionId, organizationId);
    PropertyHandler ph = new PropertyHandler(settings);

    List<Server> servers = new ArrayList<>();
    Server s = new Server();
    s.setId(instanceId);
    s.setPrivateIP(Arrays.asList("127.0.0.1"));
    s.setPublicIP(Arrays.asList("127.0.0.1"));
    s.setStatus(ph.getState().name());
    servers.add(s);

    return servers;
  }

  @Override
  public String getAccessInfo(String instanceId, String subscriptionId, String organizationId)
      throws APPlatformException {
    ProvisioningSettings settings =
        platformService.getServiceInstanceDetails(
            ApprovalController.ID, instanceId, subscriptionId, organizationId);

    return settings.getServiceAccessInfo();
  }

  @Override
  public String getMessage(String locale, String key, Object... arguments) {
    return null;
  }
}
