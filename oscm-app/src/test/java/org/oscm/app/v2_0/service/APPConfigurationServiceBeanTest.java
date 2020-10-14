/**
 * *****************************************************************************
 *
 * <p>Copyright FUJITSU LIMITED 2020
 *
 * <p>Creation Date: 09.10.2020
 *
 * <p>*****************************************************************************
 */
package org.oscm.app.v2_0.service;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import org.oscm.app.domain.*;
import org.oscm.app.v2_0.data.*;
import org.oscm.app.v2_0.exceptions.ConfigurationException;
import org.oscm.encrypter.AESEncrypter;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class APPConfigurationServiceBeanTest {

  @Spy @InjectMocks APPConfigurationServiceBean serviceBean = new APPConfigurationServiceBean();

  @Mock private EntityManager entityManager;

  @BeforeClass
  public static void setUp() {
    AESEncrypter.generateKey();
  }

  @Test
  public void testGetProxyConfigurationSetting() throws Exception {
    // given
    Query query = mock(Query.class);
    when(entityManager.createNamedQuery(anyString())).thenReturn(query);

    ConfigurationSetting setting = new ConfigurationSetting();
    setting.setSettingKey("KEY");
    setting.setSettingValue("VALUE");
    when(query.getSingleResult()).thenReturn(setting);

    // when
    PlatformConfigurationKey userId = PlatformConfigurationKey.BSS_USER_ID;
    String expectedSetting = serviceBean.getProxyConfigurationSetting(userId);

    // then
    assertEquals(expectedSetting, setting.getSettingValue());
  }

  @Test(expected = ConfigurationException.class)
  public void testGetProxyConfigurationSetting_NoResult() throws Exception {
    // given
    Query query = mock(Query.class);
    when(entityManager.createNamedQuery(anyString())).thenReturn(query);
    when(query.getSingleResult()).thenThrow(new NoResultException());

    // when
    PlatformConfigurationKey userId = PlatformConfigurationKey.BSS_USER_ID;
    serviceBean.getProxyConfigurationSetting(userId);
  }

  @Test
  public void testGetControllerOrganizations() {
    // given
    Query query = mock(Query.class);
    when(entityManager.createNamedQuery(anyString())).thenReturn(query);

    ConfigurationSetting setting = new ConfigurationSetting();
    setting.setControllerId("CONTROLLER_ID");
    setting.setSettingValue("VALUE");
    ArrayList<ConfigurationSetting> settings = new ArrayList<>();
    settings.add(setting);
    when(query.getResultList()).thenReturn(settings);

    // when
    HashMap<String, String> organizations = serviceBean.getControllerOrganizations();

    // then
    assertEquals(settings.size(), organizations.size());
  }

  @Test
  public void testStoreControllerOrganizations() {
    // given
    Query query = mock(Query.class);
    when(entityManager.createNamedQuery(anyString())).thenReturn(query);

    ConfigurationSetting setting = new ConfigurationSetting();
    setting.setControllerId("CONTROLLER_ID");
    setting.setSettingValue("VALUE");
    ArrayList<ConfigurationSetting> settings = new ArrayList<>();
    settings.add(setting);
    when(query.getResultList()).thenReturn(settings);

    // when
    HashMap<String, String> organizations = new HashMap<>();
    organizations.put("CONTROLLER_ID", "orgId");
    serviceBean.storeControllerOrganizations(organizations);

    // then
    verify(entityManager, times(1)).persist(any(ConfigurationSetting.class));
  }

  @Test
  public void testStoreControllerOrganizations_newOrg() {
    // given
    Query query = mock(Query.class);
    when(entityManager.createNamedQuery(anyString())).thenReturn(query);

    ConfigurationSetting setting = new ConfigurationSetting();
    setting.setControllerId("CONTROLLER_ID");
    setting.setSettingValue("VALUE");
    ArrayList<ConfigurationSetting> settings = new ArrayList<>();
    settings.add(setting);
    when(query.getResultList()).thenReturn(settings);

    // when
    HashMap<String, String> organizations = new HashMap<>();
    organizations.put("CONTROLLER_ID", "orgId");
    organizations.put("NEW_CONTROLLER_ID", "newOrgId");
    serviceBean.storeControllerOrganizations(organizations);

    // then
    verify(entityManager, times(2)).persist(any(ConfigurationSetting.class));
  }

  @Test
  public void testGetProxySettings() throws Exception {
    // given
    Query query = mock(Query.class);
    when(entityManager.createNamedQuery(anyString())).thenReturn(query);
    List<ConfigurationSetting> mandatorySettings = mandatoryProxySettings();
    when(query.getResultList()).thenReturn(mandatorySettings);

    // when
    HashMap<String, Setting> proxySettings = serviceBean.getProxyConfigurationSettings();

    // then
    assertEquals(mandatorySettings.size(), proxySettings.size());
    mandatorySettings.forEach(
        setting -> assertTrue(proxySettings.containsKey(setting.getSettingKey())));
  }

  @Test(expected = ConfigurationException.class)
  public void testGetProxySettings_missingMandatorySetting() throws Exception {
    // given
    Query query = mock(Query.class);
    when(entityManager.createNamedQuery(anyString())).thenReturn(query);
    ConfigurationSetting configurationSetting = newConfigurationSetting("APP_BASE_URL", "value");
    List<ConfigurationSetting> settings = new ArrayList<>();
    settings.add(configurationSetting);
    when(query.getResultList()).thenReturn(settings);

    // when
    serviceBean.getProxyConfigurationSettings();
  }

  @Test
  public void testGetControllerConfigurationSettings() throws Exception {
    // given
    Query query = mock(Query.class);
    when(entityManager.createNamedQuery(anyString())).thenReturn(query);
    List<ConfigurationSetting> mandatorySettings = new ArrayList<>();
    mandatorySettings.add(newConfigurationSetting("BSS_ORGANIZATION_ID", "orgId"));
    when(query.getResultList()).thenReturn(mandatorySettings);

    // when
    HashMap<String, Setting> controllerSettings =
        serviceBean.getControllerConfigurationSettings("CONTROLLER_ID");

    // then
    assertEquals(mandatorySettings.size(), controllerSettings.size());
    mandatorySettings.forEach(
        setting -> assertTrue(controllerSettings.containsKey(setting.getSettingKey())));
  }

  @Test(expected = ConfigurationException.class)
  public void testGetControllerConfigurationSettings_missingMandatorySetting() throws Exception {
    // given
    Query query = mock(Query.class);
    when(entityManager.createNamedQuery(anyString())).thenReturn(query);
    List<ConfigurationSetting> mandatorySettings = new ArrayList<>();
    when(query.getResultList()).thenReturn(mandatorySettings);

    // when
    serviceBean.getControllerConfigurationSettings("CONTROLLER_ID");
  }

  @Test
  public void testGetCustomAttributes() throws Exception {
    // given
    TypedQuery query = mock(TypedQuery.class);
    when(entityManager.createNamedQuery(anyString(), any())).thenReturn(query);
    List<CustomAttribute> customAttributes = new ArrayList<>();
    customAttributes.add(newAttribute("KEY", "value", false, "controller_id"));
    when(query.getResultList()).thenReturn(customAttributes);

    // when
    HashMap<String, Setting> attributes = serviceBean.getCustomAttributes("orgId");

    // then
    assertEquals(customAttributes.size(), attributes.size());
    customAttributes.forEach(
        setting -> assertTrue(attributes.containsKey(setting.getAttributeKey())));
  }

  @Test
  public void testStoreControllerConfigurationSettings() throws Exception {
    // given
    Query query = mock(Query.class);
    when(entityManager.createNamedQuery(anyString())).thenReturn(query);

    ArrayList<ConfigurationSetting> settings = new ArrayList<>();
    settings.add(newConfigurationSetting("KEY", "VALUE"));
    when(query.getResultList()).thenReturn(settings);

    // when
    HashMap<String, Setting> settingsToStore = new HashMap<>();
    settingsToStore.put("KEY", new Setting("KEY", "NEW_VALUE"));
    serviceBean.storeControllerConfigurationSettings("CONTROLLER_ID", settingsToStore);

    // then
    verify(entityManager, times(1)).persist(any(Setting.class));
  }

  @Test
  public void testStoreControllerConfigurationSettings_newSetting() throws Exception {
    // given
    Query query = mock(Query.class);
    when(entityManager.createNamedQuery(anyString())).thenReturn(query);

    ArrayList<ConfigurationSetting> settings = new ArrayList<>();
    settings.add(newConfigurationSetting("KEY", "VALUE"));
    when(query.getResultList()).thenReturn(settings);

    // when
    HashMap<String, Setting> settingsToStore = new HashMap<>();
    settingsToStore.put("KEY", new Setting("KEY", "NEW_VALUE"));
    settingsToStore.put("NEW_KEY", new Setting("NEW_KEY", "NEW_VALUE"));
    serviceBean.storeControllerConfigurationSettings("CONTROLLER_ID", settingsToStore);

    // then
    verify(entityManager, times(2)).persist(any(Setting.class));
  }

  @Test
  public void testStoreAppConfigurationSettings() throws Exception {
    // given
    Query query = mock(Query.class);
    when(entityManager.createNamedQuery(anyString())).thenReturn(query);

    ArrayList<ConfigurationSetting> settings = new ArrayList<>();
    settings.add(newConfigurationSetting("KEY", "VALUE"));
    when(query.getResultList()).thenReturn(settings);

    // when
    HashMap<String, String> settingsToStore = new HashMap<>();
    settingsToStore.put("KEY", "VALUE");
    serviceBean.storeAppConfigurationSettings(settingsToStore);

    // then
    verify(entityManager, times(1)).persist(any(ConfigurationSetting.class));
  }

  @Test
  public void testGetUserConfiguredControllers() {
    // given
    Query query = mock(Query.class);
    when(entityManager.createNamedQuery(anyString())).thenReturn(query);

    List<String> controllers = Arrays.asList("CONTROLLER_1", "CONTROLLER_1");
    when(query.getResultList()).thenReturn(controllers);

    // when
    List<String> usersControllers = serviceBean.getUserConfiguredControllers("userId");

    // then
    assertEquals(controllers, usersControllers);
  }

  @Test
  public void testGetProvisioningSettings() throws Exception {
    // given
    doReturn(new HashMap<>()).when(serviceBean).getControllerConfigurationSettings(anyString());
    doReturn(new HashMap<>()).when(serviceBean).getCustomAttributes(anyString());
    doReturn(new PasswordAuthentication("username", "pwd"))
        .when(serviceBean)
        .getAuthenticationForBESTechnologyManager(anyString(), any(ServiceInstance.class));

    ServiceInstance instance = new ServiceInstance();
    instance.setOrganizationId("orgId");
    instance.setOrganizationName("orgName");
    instance.setSubscriptionId("subId");
    instance.setReferenceId("refId");
    instance.setBesLoginURL("loginUrl");
    instance.setServiceAccessInfo("info");
    ServiceUser serviceUser = new ServiceUser();

    // when
    ProvisioningSettings settings =
        serviceBean.getProvisioningSettings(instance, serviceUser, false);

    // then
    assertEquals(instance.getOrganizationId(), settings.getOrganizationId());
    assertEquals(instance.getOrganizationName(), settings.getOrganizationName());
    assertEquals(instance.getSubscriptionId(), settings.getSubscriptionId());
    assertEquals(instance.getOrganizationId(), settings.getOrganizationId());
    assertEquals(instance.getReferenceId(), settings.getReferenceId());
    assertEquals(instance.getBesLoginURL(), settings.getBesLoginURL());
    assertEquals(instance.getServiceAccessInfo(), settings.getServiceAccessInfo());
    assertEquals(serviceUser, settings.getRequestingUser());
  }

  @Test
  public void testGetAuthenticationForBESTechnologyManager() throws Exception {
    // given
    Map<String, Setting> controllerSettings = new HashMap<>();
    String keyName = ControllerConfigurationKey.BSS_USER_KEY.name();
    String pwdName = ControllerConfigurationKey.BSS_USER_PWD.name();
    controllerSettings.put(keyName, new Setting(keyName, "userKey"));
    controllerSettings.put(pwdName, new Setting(pwdName, "pwd"));
    doReturn(controllerSettings).when(serviceBean).getControllerConfigurationSettings(anyString());

    // when
    PasswordAuthentication authentication =
        serviceBean.getAuthenticationForBESTechnologyManager(
            "CONTROLLER_ID", new ServiceInstance());

    // then
    assertEquals(controllerSettings.get(keyName).getValue(), authentication.getUserName());
    assertEquals(controllerSettings.get(pwdName).getValue(), authentication.getPassword());
  }

  @Test
  public void testGetAuthenticationForBESTechnologyManager_emptyControllerSettings()
      throws Exception {
    // given
    doReturn(new HashMap<>()).when(serviceBean).getControllerConfigurationSettings(anyString());
    InstanceParameter userParameter = new InstanceParameter();
    userParameter.setParameterKey(InstanceParameter.BSS_USER);
    userParameter.setParameterValue("userId");
    InstanceParameter pwdParameter = new InstanceParameter();
    pwdParameter.setParameterKey(InstanceParameter.BSS_USER_PWD);
    pwdParameter.setParameterValue("pwd");
    ServiceInstance serviceInstance = new ServiceInstance();
    serviceInstance.setInstanceParameters(Arrays.asList(userParameter, pwdParameter));

    // when
    PasswordAuthentication authentication =
        serviceBean.getAuthenticationForBESTechnologyManager("CONTROLLER_ID", serviceInstance);

    // then
    assertEquals(userParameter.getParameterValue(), authentication.getUserName());
    assertEquals(pwdParameter.getParameterValue(), authentication.getPassword());
  }

  @Test(expected = ConfigurationException.class)
  public void testGetAuthenticationForBESTechnologyManager_emptyControllerSettingsAndInstance()
      throws Exception {
    // given
    doReturn(new HashMap<>()).when(serviceBean).getControllerConfigurationSettings(anyString());

    // when
    serviceBean.getAuthenticationForBESTechnologyManager("CONTROLLER_ID", new ServiceInstance());
  }

  @Test
  public void testGetAuthenticationForAPPAdmin() throws Exception {
    // given
    Map<String, Setting> proxySettings = new HashMap<>();
    String keyName = PlatformConfigurationKey.BSS_USER_KEY.name();
    String pwdName = PlatformConfigurationKey.BSS_USER_PWD.name();
    proxySettings.put(keyName, new Setting(keyName, "userKey"));
    proxySettings.put(pwdName, new Setting(pwdName, "pwd"));

    // when
    PasswordAuthentication authentication = serviceBean.getAuthenticationForAPPAdmin(proxySettings);

    // then
    assertEquals(proxySettings.get(keyName).getValue(), authentication.getUserName());
    assertEquals(proxySettings.get(pwdName).getValue(), authentication.getPassword());
  }

  @Test(expected = ConfigurationException.class)
  public void testGetAuthenticationForAPPAdmin_emptySettings() throws Exception {
    // given
    Map<String, Setting> proxySettings = new HashMap<>();

    // when
    serviceBean.getAuthenticationForAPPAdmin(proxySettings);
  }

  private static List<ConfigurationSetting> mandatoryProxySettings() throws ConfigurationException {
    ArrayList<ConfigurationSetting> settings = new ArrayList<>();
    settings.add(newConfigurationSetting("APP_BASE_URL", "value"));
    settings.add(newConfigurationSetting("APP_TIMER_INTERVAL", "value"));
    settings.add(newConfigurationSetting("APP_TIMER_REFRESH_SUBSCRIPTIONS", "value"));
    settings.add(newConfigurationSetting("BSS_WEBSERVICE_URL", "value"));
    settings.add(newConfigurationSetting("BSS_WEBSERVICE_WSDL_URL", "value"));
    settings.add(newConfigurationSetting("BSS_USER_KEY", "value"));
    settings.add(newConfigurationSetting("BSS_USER_PWD", "value"));
    settings.add(newConfigurationSetting("APP_ADMIN_MAIL_ADDRESS", "value"));
    settings.add(newConfigurationSetting("BSS_AUTH_MODE", "value"));
    settings.add(newConfigurationSetting("APP_TRUSTSTORE", "value"));
    settings.add(newConfigurationSetting("APP_TRUSTSTORE_BSS_ALIAS", "value"));
    settings.add(newConfigurationSetting("APP_TRUSTSTORE_PASSWORD", "value"));
    settings.add(newConfigurationSetting("APP_KEY_PATH", "value"));
    return settings;
  }

  private static ConfigurationSetting newConfigurationSetting(String key, String value)
      throws ConfigurationException {
    ConfigurationSetting setting = new ConfigurationSetting();
    setting.setSettingKey(key);
    setting.setSettingValue(value);
    setting.setDecryptedValue(value);
    return setting;
  }

  private static CustomAttribute newAttribute(
      String key, String value, boolean isEncrypted, String controllerId) {
    CustomAttribute attribute = new CustomAttribute();
    attribute.setAttributeKey(key);
    attribute.setAttributeValue(value);
    attribute.setEncrypted(isEncrypted);
    attribute.setControllerId(controllerId);
    return attribute;
  }
}
