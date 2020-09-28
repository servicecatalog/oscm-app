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
import org.oscm.app.v2_0.data.Setting;
import org.oscm.app.v2_0.exceptions.APPlatformException;
import org.oscm.app.v2_0.intf.APPlatformService;
import org.oscm.app.v2_0.intf.InstanceAccess;
import org.oscm.app.v2_0.intf.ServerInformation;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public class ApprovalInstanceAccess implements InstanceAccess {

  private static final long serialVersionUID = -7079269432576472393L;
  protected APPlatformService platformService;

  @PostConstruct
  public void initialize() {
    platformService = APPlatformServiceFactory.getInstance();
  }

  public ClientData getCustomerSettings(String clientOrganizationId) throws APPlatformException {
    ClientData[] data = new ClientData[1];
    data[0] = new ClientData(clientOrganizationId);

    Predicate<Map<String, Setting>> filter =
        new Predicate<Map<String, Setting>>() {

          @Override
          public boolean test(Map<String, Setting> t) {
            boolean include = t.containsKey(data[0].PARAM_APPROVER_ORG_ID);
            if (include) {
              data[0].set(t);
            }
            return include;
          }
        };

    Collection<String> instances = platformService.listServiceInstances(ApprovalController.ID, filter, null);
    
    if (data[0].isSet()) return data[0];

    return null;
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

  /**
   * Client data for approval trigger callback.
   */
  public class ClientData {
    String clientOrganizationId = "";

    String PARAM_USER_KEY;
    String PARAM_USER_ID;
    String PARAM_USER_PWD;
    String PARAM_APPROVER_ORG_ID;

    public ClientData(String id) {
      clientOrganizationId = id;
      PARAM_USER_KEY = String.format("USERKEY_%s", clientOrganizationId);
      PARAM_USER_ID = String.format("USERID_%s", clientOrganizationId);
      PARAM_USER_PWD = String.format("USERPWD_%s", clientOrganizationId);
      PARAM_APPROVER_ORG_ID = String.format("APPROVER_ORG_ID_%s", clientOrganizationId);
    }

    public boolean exists(Map<String, Setting> map) {
      boolean exists = true;
      exists &= null != map.get(PARAM_APPROVER_ORG_ID);
      exists &= null != map.get(PARAM_USER_ID);
      exists &= null != map.get(PARAM_USER_KEY);
      exists &= null != map.get(PARAM_USER_PWD);
      return exists;
    }

    public boolean isSet() {
      boolean isSet = true;
      isSet &= null != getOrgAdminUserId();
      isSet &= null != getOrgAdminUserKey();
      isSet &= null != getOrgAdminUserPwd();
      return isSet;
    }

    public void set(Map<String, Setting> map) {
      setOrgAdminUserId(map.get(PARAM_USER_ID));
      setOrgAdminUserKey(map.get(PARAM_USER_KEY));
      setOrgAdminUserPwd(map.get(PARAM_USER_PWD));
    }

    public String getClientOrganizationId() {
      return clientOrganizationId;
    }

    public Setting getOrgAdminUserId() {
      return orgAdminUserId;
    }

    void setOrgAdminUserId(Setting orgAdminUserId) {
      this.orgAdminUserId = orgAdminUserId;
    }

    public Setting getOrgAdminUserKey() {
      return orgAdminUserKey;
    }

    void setOrgAdminUserKey(Setting orgAdminUserKey) {
      this.orgAdminUserKey = orgAdminUserKey;
    }

    public Setting getOrgAdminUserPwd() {
      return orgAdminUserPwd;
    }

    void setOrgAdminUserPwd(Setting orgAdminUserPwd) {
      this.orgAdminUserPwd = orgAdminUserPwd;
    }

    Setting orgAdminUserId;
    Setting orgAdminUserKey;
    Setting orgAdminUserPwd;
  }
}
