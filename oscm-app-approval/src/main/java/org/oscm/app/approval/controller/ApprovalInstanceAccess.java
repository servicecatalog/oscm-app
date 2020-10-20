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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import javax.annotation.PostConstruct;

import org.oscm.app.approval.data.Server;
import org.oscm.app.v2_0.APPlatformServiceFactory;
import org.oscm.app.v2_0.data.PasswordAuthentication;
import org.oscm.app.v2_0.data.ProvisioningSettings;
import org.oscm.app.v2_0.data.Setting;
import org.oscm.app.v2_0.exceptions.APPlatformException;
import org.oscm.app.v2_0.intf.APPlatformService;
import org.oscm.app.v2_0.intf.InstanceAccess;
import org.oscm.app.v2_0.intf.ServerInformation;

public class ApprovalInstanceAccess implements InstanceAccess {

  private static final long serialVersionUID = -7079269432576472393L;
  protected APPlatformService platformService;

  APPlatformService getPlatformService() {
    if (platformService == null) {
      platformService = APPlatformServiceFactory.getInstance();
    }
    return platformService;
  }

  @PostConstruct
  public void initialize() {
    platformService = APPlatformServiceFactory.getInstance();
  }

  public Collection<String> getInstancesForOrganization(String supplierId)
      throws APPlatformException {
    return getPlatformService().listServiceInstances(ApprovalController.ID, supplierId, null);
  }

  public ClientData getCustomerSettings(String clientOrganizationId) throws APPlatformException {

    ClientData data = new ClientData(clientOrganizationId);

    Collection<String> instances =
        getPlatformService().listServiceInstances(ApprovalController.ID, null);

    for (String instance : instances) {
      ProvisioningSettings ps =
          getPlatformService().getServiceInstanceDetails(ApprovalController.ID, instance, null);
      if (getApprovers(ps, clientOrganizationId).isPresent()) {
        data.set(ps);
        break;
      }
    }
    return data;
  }

  public BasicSettings getBasicSettings() throws APPlatformException {
    final String wsdUrl = getPlatformService().getBSSWebServiceWSDLUrl();

    BasicSettings basicSettings = new BasicSettings(wsdUrl);

    Collection<String> instances =
        getPlatformService().listServiceInstances(ApprovalController.ID, null);

    for (String instance : instances) {
      ProvisioningSettings ps =
          getPlatformService().getServiceInstanceDetails(ApprovalController.ID, instance, null);
      if (anyApprover(ps).isPresent()) {
        return new BasicSettings(ps, wsdUrl);
      }
    }
    return basicSettings;
  }

  private Optional<String> getApprovers(ProvisioningSettings ps, String customerOrgId) {
    HashMap<String, Setting> map = new HashMap<String, Setting>();
    map.putAll(ps.getParameters());
    map.putAll(ps.getCustomAttributes());

    return map.keySet()
        .stream()
        .filter(k -> k.startsWith("APPROVER_ORG_ID_" + customerOrgId))
        .findAny();
  }

  private Optional<String> anyApprover(ProvisioningSettings ps) {
    HashMap<String, Setting> map = new HashMap<String, Setting>();
    map.putAll(ps.getParameters());
    map.putAll(ps.getCustomAttributes());

    return map.keySet().stream().filter(k -> k.startsWith("APPROVER_ORG_ID_")).findAny();
  }

  @Override
  public List<? extends ServerInformation> getServerDetails(
      String instanceId, String subscriptionId, String organizationId) throws APPlatformException {
    ProvisioningSettings settings =
        getPlatformService()
            .getServiceInstanceDetails(
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
        getPlatformService()
            .getServiceInstanceDetails(
                ApprovalController.ID, instanceId, subscriptionId, organizationId);

    return settings.getServiceAccessInfo();
  }

  @Override
  public String getMessage(String locale, String key, Object... arguments) {
    return null;
  }

  static boolean isPresent(Setting... settings) {
    boolean is = true;
    for (Setting s : settings) {
      is &= (s != null && s.getKey() != null && s.getValue() != null && s.getValue().length() > 0);
    }
    return is;
  }

  /** Client data for approval trigger callback. */
  public class ClientData implements Serializable {

    private static final long serialVersionUID = -2960629135717720907L;

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

    public boolean isSet() {
      return isPresent(
          getOrgAdminUserId(), getOrgAdminUserId(), getOrgAdminUserPwd(), getApproverOrgId());
    }

    public void set(ProvisioningSettings ps) {
      setApproverOrgId(getSetting(ps, PARAM_APPROVER_ORG_ID));
      setOrgAdminUserId(getSetting(ps, PARAM_USER_ID));
      setOrgAdminUserKey(getSetting(ps, PARAM_USER_KEY));
      setOrgAdminUserPwd(getSetting(ps, PARAM_USER_PWD));
    }

    private Setting getConfigSetting(ProvisioningSettings ps, String key) {
      return ps.getConfigSettings().get(key);
    }

    private Setting getSetting(ProvisioningSettings ps, String key) {
      Setting val = ps.getCustomAttributes().get(key);
      if (val == null) {
        val = ps.getAttributes().get(key);
        if (val == null) val = ps.getParameters().get(key);
      }
      return val;
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

    void setApproverOrgId(Setting approverOrgId) {
      this.approverOrgId = approverOrgId;
    }

    public Setting getApproverOrgId() {
      return this.approverOrgId;
    }

    Setting orgAdminUserId;
    Setting orgAdminUserKey;
    Setting orgAdminUserPwd;
    Setting approverOrgId;
  }

  /** Client data for approval trigger callback. */
  public class BasicSettings implements Serializable {
    private static final long serialVersionUID = 9206647239461503333L;
    boolean isSet = false;

    BasicSettings(String wsdlUrl) {
      this.wsdlUrl = wsdlUrl;
    }

    BasicSettings(ProvisioningSettings ps, String wsdlUrl) {
      this.wsdlUrl = wsdlUrl;
      approvalUrl = ps.getConfigSettings().get("APPROVAL_URL");
      ownerCredentials = ps.getAuthentication();
      isSet = ownerCredentials != null && isPresent(approvalUrl);
    }

    public PasswordAuthentication getOwnerCredentials() {
      return ownerCredentials;
    }

    public String getWsdlUrl() {
      return wsdlUrl;
    }

    public Setting getApprovalURL() {
      return approvalUrl;
    }

    public boolean isSet() {
      return isSet;
    }

    private String wsdlUrl;
    Setting approvalUrl;
    PasswordAuthentication ownerCredentials;
  }
}
