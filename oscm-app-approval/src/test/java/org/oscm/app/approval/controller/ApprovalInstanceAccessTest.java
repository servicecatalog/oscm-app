/**
 * *****************************************************************************
 *
 * <p>Copyright FUJITSU LIMITED 2020
 *
 * <p>Creation Date: 28.09.2020
 *
 * <p>*****************************************************************************
 */
package org.oscm.app.approval.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.oscm.app.v2_0.data.PasswordAuthentication;
import org.oscm.app.v2_0.data.ProvisioningSettings;
import org.oscm.app.v2_0.data.Setting;
import org.oscm.app.v2_0.exceptions.APPlatformException;
import org.oscm.app.v2_0.intf.APPlatformService;

/** @author goebel */
public class ApprovalInstanceAccessTest {
  ApprovalInstanceAccess access;
  HashMap<String, Setting> configSettings = new HashMap<String, Setting>();
  HashMap<String, Setting> attributes = new HashMap<String, Setting>();
  HashMap<String, Setting> params = new HashMap<String, Setting>();
  HashMap<String, Setting> customAttributes = new HashMap<String, Setting>();
  PasswordAuthentication auth = new PasswordAuthentication("user", "pwd");

  @Before
  public void setup() throws Exception {
    APPlatformService s = mock(APPlatformService.class);
    access = spy(new ApprovalInstanceAccess());
    doReturn(s).when(access).getPlatformService();
    access.platformService = s;
    ProvisioningSettings ps = mockProvisioningSettings();
    doReturn("http://oscm-core/{service}?wsdl").when(s).getBSSWebServiceWSDLUrl();
    Collection<String> result = Arrays.asList(new String[] {"instance_12345678"});
    doReturn(result).when(s).listServiceInstances(any(), any());
    doReturn(ps).when(s).getServiceInstanceDetails(anyString(), anyString(), any());
  }

  @Test
  public void getBasicSettings_Set() throws Exception {
    // given
    givenAnyApprover();
    givenBasicSettings();

    // when
    ApprovalInstanceAccess.BasicSettings settings = access.getBasicSettings();

    // then
    assertNotNull(settings);
    assertTrue(settings.isSet());
    assertNotNull(settings.getWsdlUrl());
  }

  @Test
  public void getCustomerSettings() throws APPlatformException {
    // given
    givenCustomerSettings("3fe2a1");

    // when
    ApprovalInstanceAccess.ClientData data = access.getCustomerSettings("3fe2a1");

    // then
    assertNotNull(data);
    assertEquals("11001", data.getOrgAdminUserKey().getValue());
    assertEquals("orgAdmin", data.getOrgAdminUserId().getValue());
    assertEquals("_crypt:secret", data.getOrgAdminUserPwd().getValue());
  }

  @Test
  public void getBasicSettings_noApprover() throws Exception {
    // given
    givenBasicSettings();

    // when
    ApprovalInstanceAccess.BasicSettings settings = access.getBasicSettings();

    // then
    assertNotNull(settings);
    assertFalse(settings.isSet());
  }

  Map<String, Setting> givenCustomerSettings(String id) {
    customAttributes.put("APPROVER_ORG_ID_" + id, new Setting("APPROVER_ORG_ID_" + id, "2e3f1a"));
    customAttributes.put("USERID_" + id, new Setting("USERID_" + id, "orgAdmin"));
    customAttributes.put("USERKEY_" + id, new Setting("USERKEY_" + id, "11001"));
    customAttributes.put("USERPWD_" + id, new Setting("USERPWD_" + id, "_crypt:secret", true));

    return customAttributes;
  }

  Map<String, Setting> givenAnyApprover() {
    customAttributes.put("APPROVER_ORG_ID_bla", new Setting("APPROVER_ORG_ID_bla", "2e3f1a"));
    return customAttributes;
  }

  void givenBasicSettings() throws Exception {
    configSettings.put("APPROVAL_URL", new Setting("APPROVAL_URL", "http://oscm-app/approval"));
    givenPasswordAutentication("admin", "adminpw");
  }

  private void givenPasswordAutentication(String user, String password) {
    auth = new PasswordAuthentication(user, password);
  }

  private ProvisioningSettings mockProvisioningSettings() throws Exception {
    ProvisioningSettings ps = mock(ProvisioningSettings.class);
    doReturn(attributes).when(ps).getAttributes();
    doReturn(configSettings).when(ps).getConfigSettings();
    doReturn(customAttributes).when(ps).getCustomAttributes();
    doReturn(params).when(ps).getParameters();
    doReturn(auth).when(ps).getAuthentication();
    return ps;
  }
}
