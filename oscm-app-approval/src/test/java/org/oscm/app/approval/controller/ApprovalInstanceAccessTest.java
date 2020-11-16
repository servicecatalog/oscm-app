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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.oscm.app.approval.data.State;
import org.oscm.app.v2_0.data.PasswordAuthentication;
import org.oscm.app.v2_0.data.ProvisioningSettings;
import org.oscm.app.v2_0.data.Setting;
import org.oscm.app.v2_0.exceptions.APPlatformException;
import org.oscm.app.v2_0.intf.APPlatformService;
import org.oscm.app.v2_0.intf.ServerInformation;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"javax.management.*", "javax.script.*", "jdk.internal.reflect.*"})
@PrepareForTest({ApprovalInstanceAccess.class})
public class ApprovalInstanceAccessTest {
  ApprovalInstanceAccess access;
  HashMap<String, Setting> configSettings = new HashMap<String, Setting>();
  HashMap<String, Setting> attributes = new HashMap<String, Setting>();
  HashMap<String, Setting> params = new HashMap<String, Setting>();
  HashMap<String, Setting> customAttributes = new HashMap<String, Setting>();
  PasswordAuthentication auth = new PasswordAuthentication("user", "pwd");
  private ProvisioningSettings settings;
  private APPlatformService platformService;
  private PropertyHandler propertyHandler;

  @Before
  public void setup() throws Exception {
    platformService = mock(APPlatformService.class);
    access = spy(new ApprovalInstanceAccess());
    settings = mock(ProvisioningSettings.class);
    propertyHandler = mock(PropertyHandler.class);
    doReturn(platformService).when(access).getPlatformService();
    access.platformService = platformService;
    ProvisioningSettings ps = mockProvisioningSettings();
    doReturn("http://oscm-core/{service}?wsdl").when(platformService).getBSSWebServiceWSDLUrl();
    Collection<String> result = Arrays.asList(new String[]{"instance_12345678"});
    doReturn(result).when(platformService).listServiceInstances(any(), any());
    doReturn(ps).when(platformService).getServiceInstanceDetails(anyString(), anyString(), any());
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
  public void getBasicSettings_Params() throws Exception {
    // given
    givenAnyApprover();
    givenBasicSettingsWithParams();

    // when
    ApprovalInstanceAccess.BasicSettings settings = access.getBasicSettings();

    // then
    assertNotNull(settings);
    assertTrue(settings.isSet());
    assertNotNull(settings.getParams());
    assertEquals("Test", settings.getParams().get("APPROVAL_MSG_SUBECT"));
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

  @Test
  public void getServerDetails() throws Exception {
    // given
    when(access.getPlatformService()).thenReturn(platformService);
    when(platformService.getServiceInstanceDetails(anyString(), anyString(), anyString(), anyString())).thenReturn(settings);
    PowerMockito.whenNew(PropertyHandler.class).withAnyArguments().thenReturn(propertyHandler);
    when(propertyHandler.getState()).thenReturn(State.CREATING);

    // when
    List<? extends ServerInformation> result =
        access.getServerDetails("instanceId", "subscriptionId", "organizationId");

    // then
    assertEquals(1, result.size());
  }

  @Test
  public void getAccessInfo() throws Exception {
    // given
    when(access.getPlatformService()).thenReturn(platformService);
    when(platformService.getServiceInstanceDetails(anyString(), anyString(), anyString(), anyString())).thenReturn(settings);

    // when
    access.getAccessInfo("instanceId", "subscriptionId", "organizationId");

    // then
    verify(settings, times(1)).getServiceAccessInfo();
  }

  @Test
  public void getPlatformService() {

    // when
    APPlatformService result = access.getPlatformService();

    // then
    assertEquals(platformService, result);
  }

  @Test
  public void getPlatformService_AssignNewInstance() {
    platformService = null;

    // when
    assertNotNull(access.getPlatformService());
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

  void givenBasicSettingsWithParams() throws Exception {
    givenBasicSettings();
    params.put("APPROVAL_MSG_SUBECT", new Setting("APPROVAL_MSG_SUBECT", "Test"));
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
