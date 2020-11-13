/**
 * *****************************************************************************
 *
 * <p>Copyright FUJITSU LIMITED 2019
 *
 * <p>Creation Date: 09.04.2019
 *
 * <p>*****************************************************************************
 */
package org.oscm.app.v2_0.service;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.oscm.app.business.APPlatformControllerFactory;
import org.oscm.app.business.exceptions.ServiceInstanceNotFoundException;
import org.oscm.app.dao.ServiceInstanceDAO;
import org.oscm.app.domain.PlatformConfigurationKey;
import org.oscm.app.domain.ServiceInstance;
import org.oscm.app.v2_0.data.PasswordAuthentication;
import org.oscm.app.v2_0.data.ProvisioningSettings;
import org.oscm.app.v2_0.data.Setting;
import org.oscm.app.v2_0.data.User;
import org.oscm.app.v2_0.exceptions.APPlatformException;
import org.oscm.app.v2_0.exceptions.ConfigurationException;
import org.oscm.app.v2_0.exceptions.ObjectNotFoundException;
import org.oscm.app.v2_0.exceptions.SuspendException;
import org.oscm.app.v2_0.intf.APPlatformController;
import org.oscm.vo.VOUserDetails;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"javax.management.*", "javax.script.*", "jdk.internal.reflect.*", "javax.net.ssl.*"})
@PrepareForTest({APPlatformServiceBean.class, APPlatformControllerFactory.class, MessageDigest.class, KeyStore.class})
public class APPlatformServiceBeanTest {

  @InjectMocks
  private APPlatformServiceBean applatformService;

  @Mock
  private APPConfigurationServiceBean appConfigurationService;
  @Mock
  private APPAuthenticationServiceBean authService;
  @Mock
  private ServiceInstanceDAO instanceDAO;
  @Mock
  private APPConcurrencyServiceBean concurrencyService;
  @Mock
  private APPCommunicationServiceBean mailService;
  @Mock
  private PasswordAuthentication passwordAuthentication;
  @Mock
  private APPlatformController platformController;


  @Before
  public void setup() {
    applatformService = PowerMockito.spy(new APPlatformServiceBean());
    PowerMockito.mockStatic(APPlatformControllerFactory.class);
    PowerMockito.mockStatic(MessageDigest.class);
    PowerMockito.mockStatic(KeyStore.class);

    MockitoAnnotations.initMocks(this);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void updateUserCredentials_isExecutedSuccessfully_ifAllControllersConfigured()
      throws Exception {

    // given
    List<String> controllers =
        Arrays.asList("PROXY", "ess.aws", "ess.openstack", "ess.azure", "ess.vmware");
    when(appConfigurationService.getUserConfiguredControllers(anyString())).thenReturn(controllers);
    doReturn(Optional.of("test")).when(applatformService).decryptPassword(anyString());

    // when
    applatformService.updateUserCredentials(1000, "test", "dsads1232ewqewe:321ed==esdsdczsfnb3");

    // then
    verify(appConfigurationService, times(1)).storeAppConfigurationSettings(any(HashMap.class));
    verify(appConfigurationService, times(4))
        .storeControllerConfigurationSettings(anyString(), any(HashMap.class));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void updateUserCredentials_hasNoInteractions_ifNoControllerIsConfigured()
      throws Exception {

    // given
    List<String> controllers = new ArrayList<>();
    when(appConfigurationService.getUserConfiguredControllers(anyString())).thenReturn(controllers);

    // when
    applatformService.updateUserCredentials(1000, "test", "test");

    // then
    verify(appConfigurationService, never()).storeAppConfigurationSettings(any(HashMap.class));
    verify(appConfigurationService, never())
        .storeControllerConfigurationSettings(anyString(), any(HashMap.class));
  }

  @Test
  public void isSsoMode_returnTrue_ifAuthModeIsOIDC() throws Exception {

    // given
    when(appConfigurationService.getProxyConfigurationSetting(
        PlatformConfigurationKey.BSS_AUTH_MODE))
        .thenReturn("OIDC");

    // when
    boolean ssoMode = applatformService.isSsoMode();

    // then
    assertTrue(ssoMode);
  }

  @Test
  public void isSsoMode_returnFalse_ifAuthModeIsInternal() throws Exception {

    // given
    when(appConfigurationService.getProxyConfigurationSetting(
        PlatformConfigurationKey.BSS_AUTH_MODE))
        .thenReturn("INTERNAL");

    // when
    boolean ssoMode = applatformService.isSsoMode();

    // then
    assertFalse(ssoMode);
  }

  @Test
  public void testAuthenticate() throws Exception {
    // given
    PasswordAuthentication authentication = new PasswordAuthentication("username", "password");
    VOUserDetails userDetails = new VOUserDetails();
    userDetails.setKey(1000);
    userDetails.setUserId("userId");
    userDetails.setLocale("en");
    userDetails.setEMail("email");
    userDetails.setFirstName("firstName");
    userDetails.setLastName("lastName");
    when(authService.getAuthenticatedTMForController(
        anyString(), any(PasswordAuthentication.class)))
        .thenReturn(userDetails);

    // when
    User user = applatformService.authenticate("controllerId", authentication);

    // then
    assertEquals(userDetails.getKey(), user.getUserKey());
    assertEquals(userDetails.getUserId(), user.getUserId());
    assertEquals(userDetails.getLocale(), user.getLocale());
    assertEquals(userDetails.getEMail(), user.getEmail());
    assertEquals(userDetails.getFirstName(), user.getFirstName());
    assertEquals(userDetails.getLastName(), user.getLastName());
  }

  @Test
  public void testListServiceInstance() throws Exception {
    // given
    PasswordAuthentication authentication = new PasswordAuthentication("username", "password");
    List<ServiceInstance> instances =
        Arrays.asList(newServiceInstance("id_1", "orgId"), newServiceInstance("id_2", "orgId"));
    when(instanceDAO.getInstancesForController(anyString())).thenReturn(instances);

    // when
    Collection<String> instancesIds = applatformService.listServiceInstances("cid", authentication);
    instances.forEach(instance -> assertTrue(instancesIds.contains(instance.getInstanceId())));
  }

  @Test
  public void testListServiceInstanceWithOrg() throws Exception {
    // given
    String orgId = "orgId";
    PasswordAuthentication authentication = new PasswordAuthentication("username", "password");
    List<ServiceInstance> instances =
        Arrays.asList(
            newServiceInstance("id_1", orgId),
            newServiceInstance("id_2", orgId),
            newServiceInstance("id_3", "anotherOrg"));
    when(instanceDAO.getInstancesForController(anyString())).thenReturn(instances);

    // when
    Collection<String> instancesIds =
        applatformService.listServiceInstances("cid", orgId, authentication);
    instances.stream()
        .filter(instance -> orgId.equals(instance.getOrganizationId()))
        .forEach(instance -> assertTrue(instancesIds.contains(instance.getInstanceId())));
  }

  @Test
  public void testLockServiceInstance() throws Exception {
    // given
    String ctrlId = "ctrlId";
    String instanceId = "instanceId";
    PasswordAuthentication authentication = new PasswordAuthentication("user", "pwd");

    // when
    applatformService.lockServiceInstance(ctrlId, instanceId, authentication);

    // then
    verify(authService, times(1)).authenticateTMForInstance(ctrlId, instanceId, authentication);
    verify(concurrencyService, times(1)).lockServiceInstance(ctrlId, instanceId);
  }

  @Test
  public void testUnlockServiceInstance() throws Exception {
    // given
    String ctrlId = "ctrlId";
    String instanceId = "instanceId";
    PasswordAuthentication authentication = new PasswordAuthentication("user", "pwd");

    // when
    applatformService.unlockServiceInstance(ctrlId, instanceId, authentication);

    // then
    verify(authService, times(1)).authenticateTMForInstance(ctrlId, instanceId, authentication);
    verify(concurrencyService, times(1)).unlockServiceInstance(ctrlId, instanceId);
  }

  @Test(expected = SuspendException.class)
  public void testSendMail_throwsException() throws Exception {
    // given
    doThrow(new APPlatformException("msg"))
        .when(mailService)
        .sendMail(anyList(), anyString(), anyString());

    // when
    applatformService.sendMail(new ArrayList<>(), "subject", "text");
  }

  @Test
  public void testGetEventServiceUrl() throws Exception {
    // given
    String url = "http://url";
    when(appConfigurationService.getProxyConfigurationSetting(
        PlatformConfigurationKey.APP_BASE_URL))
        .thenReturn(url);

    // when
    String eventUrl = applatformService.getEventServiceUrl();

    // then
    assertEquals(url + "/notify", eventUrl);
  }

  @Test
  public void testStoreServiceInstanceDetails() throws Exception {
    // given
    String controllerId = "ctrlId";
    String instanceId = "instanceId";
    ProvisioningSettings settings =
        new ProvisioningSettings(new HashMap<>(), new HashMap<>(), "en");
    PasswordAuthentication authentication = new PasswordAuthentication("user", "pwd");
    ServiceInstance instance = mock(ServiceInstance.class);
    when(instanceDAO.getInstanceById(anyString())).thenReturn(instance);

    // when
    applatformService.storeServiceInstanceDetails(
        controllerId, instanceId, settings, authentication);

    // then
    verify(instance, times(1)).setInstanceParameters(settings.getParameters());
  }

  @Test(expected = ObjectNotFoundException.class)
  public void testStoreServiceInstanceDetails_instanceNotFound() throws Exception {
    // given
    String controllerId = "ctrlId";
    String instanceId = "instanceId";
    ProvisioningSettings settings =
        new ProvisioningSettings(new HashMap<>(), new HashMap<>(), "en");
    PasswordAuthentication authentication = new PasswordAuthentication("user", "pwd");
    ServiceInstance instance = mock(ServiceInstance.class);
    when(instanceDAO.getInstanceById(anyString()))
        .thenThrow(new ServiceInstanceNotFoundException("msg"));

    // when
    applatformService.storeServiceInstanceDetails(
        controllerId, instanceId, settings, authentication);

    // then
    verify(instance, times(0)).setInstanceParameters(settings.getParameters());
  }

  @Test
  public void testGetServiceInstanceDetails() throws Exception {
    // given
    String controllerId = "ctrlId";
    String instanceId = "instanceId";
    String subscriptionId = "subId";
    String orgId = "ogId";
    ServiceInstance instance = new ServiceInstance();
    instance.setInstanceId(instanceId);
    instance.setControllerId(controllerId);
    instance.setSubscriptionId(subscriptionId);
    instance.setOrganizationId(orgId);
    when(instanceDAO.getInstanceById(anyString())).thenReturn(instance);

    // when
    applatformService.getServiceInstanceDetails(controllerId, instanceId, subscriptionId, orgId);

    // then
    verify(appConfigurationService, times(1)).getProvisioningSettings(any(), any(), anyBoolean());
  }

  @Test(expected = ObjectNotFoundException.class)
  public void testGetServiceInstanceDetails_instanceNotMatched() throws Exception {
    // given
    String controllerId = "ctrlId";
    String instanceId = "instanceId";
    String subscriptionId = "subId";
    String orgId = "ogId";
    ServiceInstance instance = new ServiceInstance();
    instance.setInstanceId(instanceId);
    instance.setControllerId(controllerId);
    instance.setSubscriptionId(subscriptionId);
    instance.setOrganizationId(orgId);
    when(instanceDAO.getInstanceById(anyString())).thenReturn(instance);

    // when
    applatformService.getServiceInstanceDetails("otherctrl", instanceId, subscriptionId, orgId);

    // then
    verify(appConfigurationService, times(0)).getProvisioningSettings(any(), any(), anyBoolean());
  }

  @Test
  public void testGetServiceInstanceDetailsWithAuth() throws Exception {
    // given
    String controllerId = "ctrlId";
    String instanceId = "instanceId";
    ServiceInstance instance = new ServiceInstance();
    instance.setInstanceId(instanceId);
    instance.setControllerId(controllerId);
    when(instanceDAO.getInstanceById(anyString())).thenReturn(instance);

    // when
    applatformService.getServiceInstanceDetails(
        controllerId, instanceId, new PasswordAuthentication("user", "pwd"));

    // then
    verify(appConfigurationService, times(1)).getProvisioningSettings(any(), any());
  }

  @Test(expected = ObjectNotFoundException.class)
  public void testGetServiceInstanceDetailsWithAuth_serviceNotFound() throws Exception {
    // given
    String controllerId = "ctrlId";
    String instanceId = "instanceId";
    ServiceInstance instance = new ServiceInstance();
    instance.setInstanceId(instanceId);
    instance.setControllerId(controllerId);
    when(instanceDAO.getInstanceById(anyString()))
        .thenThrow(new ServiceInstanceNotFoundException("msg"));

    // when
    applatformService.getServiceInstanceDetails(
        controllerId, instanceId, new PasswordAuthentication("user", "pwd"));

    // then
    verify(appConfigurationService, times(0)).getProvisioningSettings(any(), any());
  }

  @Test
  public void testCheckToken_truststoreIsNull() throws Exception {
    // given
    when(appConfigurationService.getProxyConfigurationSetting(
        PlatformConfigurationKey.APP_TRUSTSTORE))
        .thenReturn(null);

    // when
    boolean check = applatformService.checkToken("token", "signature");

    // then
    assertFalse(check);
  }

  @Test
  public void testCheckToken_CertNull() throws Exception {

    byte[] digest = new byte[12];
    MessageDigest messageDigest = mock(MessageDigest.class);
    FileInputStream inputStream = mock(FileInputStream.class);
    KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());

    when(appConfigurationService.getProxyConfigurationSetting(PlatformConfigurationKey.APP_TRUSTSTORE))
        .thenReturn("locale");
    when(appConfigurationService.getProxyConfigurationSetting(PlatformConfigurationKey.APP_TRUSTSTORE_PASSWORD))
        .thenReturn("password");
    when(appConfigurationService.getProxyConfigurationSetting(PlatformConfigurationKey.APP_TRUSTSTORE_BSS_ALIAS))
        .thenReturn("alias");
    when(MessageDigest.getInstance("SHA-256")).thenReturn(messageDigest);
    when(messageDigest.digest()).thenReturn(digest);
    PowerMockito.whenNew(FileInputStream.class).withAnyArguments().thenReturn(inputStream);
    when(KeyStore.getInstance(any())).thenReturn(keystore);

    boolean check = applatformService.checkToken("token", "signature");

    assertFalse(check);
  }

  @Test
  public void testCheckToken_KeyNull() throws Exception {

    byte[] digest = new byte[12];
    MessageDigest messageDigest = mock(MessageDigest.class);
    FileInputStream inputStream = mock(FileInputStream.class);
    KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
    Certificate certificate = mock(Certificate.class);

    when(appConfigurationService.getProxyConfigurationSetting(PlatformConfigurationKey.APP_TRUSTSTORE))
        .thenReturn("locale");
    when(appConfigurationService.getProxyConfigurationSetting(PlatformConfigurationKey.APP_TRUSTSTORE_PASSWORD))
        .thenReturn("password");
    when(appConfigurationService.getProxyConfigurationSetting(PlatformConfigurationKey.APP_TRUSTSTORE_BSS_ALIAS))
        .thenReturn("alias");
    when(MessageDigest.getInstance("SHA-256")).thenReturn(messageDigest);
    when(messageDigest.digest()).thenReturn(digest);
    PowerMockito.whenNew(FileInputStream.class).withAnyArguments().thenReturn(inputStream);
    when(KeyStore.getInstance(any())).thenReturn(keystore);
    when(keystore.getCertificate(anyString())).thenReturn(certificate);

    boolean check = applatformService.checkToken("token", "signature");

    assertFalse(check);
  }

  @Test
  public void testCheckToken_NullOrEmptyTransformation() throws Exception {

    byte[] digest = new byte[12];
    MessageDigest messageDigest = mock(MessageDigest.class);
    FileInputStream inputStream = mock(FileInputStream.class);
    KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
    Certificate certificate = mock(Certificate.class);
    PublicKey key = mock(PublicKey.class);

    when(appConfigurationService.getProxyConfigurationSetting(PlatformConfigurationKey.APP_TRUSTSTORE))
        .thenReturn("locale");
    when(appConfigurationService.getProxyConfigurationSetting(PlatformConfigurationKey.APP_TRUSTSTORE_PASSWORD))
        .thenReturn("password");
    when(appConfigurationService.getProxyConfigurationSetting(PlatformConfigurationKey.APP_TRUSTSTORE_BSS_ALIAS))
        .thenReturn("alias");
    when(MessageDigest.getInstance("SHA-256")).thenReturn(messageDigest);
    when(messageDigest.digest()).thenReturn(digest);
    PowerMockito.whenNew(FileInputStream.class).withAnyArguments().thenReturn(inputStream);
    when(KeyStore.getInstance(any())).thenReturn(keystore);
    when(keystore.getCertificate(anyString())).thenReturn(certificate);
    when(certificate.getPublicKey()).thenReturn(key);

    boolean check = applatformService.checkToken("token", "signature");

    assertFalse(check);
  }

  @Test
  public void testDecryptPassword_FailedDecryption() throws Exception {

    FileInputStream inputStream = mock(FileInputStream.class);
    KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
    PublicKey key = mock(PublicKey.class);

    when(appConfigurationService.getProxyConfigurationSetting(PlatformConfigurationKey.APP_TRUSTSTORE))
        .thenReturn("locale");
    when(appConfigurationService.getProxyConfigurationSetting(PlatformConfigurationKey.APP_TRUSTSTORE_PASSWORD))
        .thenReturn("password");
    when(appConfigurationService.getProxyConfigurationSetting(PlatformConfigurationKey.APP_TRUSTSTORE_BSS_ALIAS))
        .thenReturn("alias");
    PowerMockito.whenNew(FileInputStream.class).withAnyArguments().thenReturn(inputStream);
    when(KeyStore.getInstance(any())).thenReturn(keystore);
    when(keystore.getKey(anyString(), any())).thenReturn(key);

    Optional<String> result = applatformService.decryptPassword("password");

    assertEquals(Optional.empty(), result);
  }

  @Test
  public void testExist() {

    boolean result = applatformService.exists(anyString(), anyString());

    assertFalse(result);
  }

  @Test
  public void testGetBSSWebServiceUrl() throws ConfigurationException {

    applatformService.getBSSWebServiceUrl();

    verify(appConfigurationService, times(1)).getProxyConfigurationSetting(PlatformConfigurationKey.BSS_WEBSERVICE_URL);
  }

  @Test
  public void testGetControllerSettings() throws APPlatformException {

    applatformService.getControllerSettings("ControllerId", passwordAuthentication);

    verify(appConfigurationService, times(1)).getControllerConfigurationSettings("ControllerId");
  }

  @Test
  public void testStoreControllerSettings() throws Exception {
    HashMap<String, Setting> controllerSettings = new HashMap<>();
    when(APPlatformControllerFactory.getInstance(anyString())).thenReturn(platformController);

    applatformService.storeControllerSettings("ControllerId", controllerSettings, passwordAuthentication);

    verify(applatformService, times(1)).requestControllerSettings("ControllerId");
  }

  private static ServiceInstance newServiceInstance(String instanceId, String orgId) {
    ServiceInstance serviceInstance = new ServiceInstance();
    serviceInstance.setInstanceId(instanceId);
    serviceInstance.setOrganizationId(orgId);
    return serviceInstance;
  }
}
