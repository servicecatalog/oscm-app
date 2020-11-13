/**
 * *****************************************************************************
 *
 * <p>Copyright FUJITSU LIMITED 2020
 *
 * <p>Creation Date: 16.10.2020
 *
 * <p>*****************************************************************************
 */
package org.oscm.app.v2_0.service;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.oscm.app.business.APPlatformControllerFactory;
import org.oscm.app.business.ProvisioningResults;
import org.oscm.app.business.exceptions.BadResultException;
import org.oscm.app.dao.ServiceInstanceDAO;
import org.oscm.app.domain.CustomAttribute;
import org.oscm.app.domain.InstanceParameter;
import org.oscm.app.domain.ProvisioningStatus;
import org.oscm.app.domain.ServiceInstance;
import org.oscm.app.v2_0.data.InstanceDescription;
import org.oscm.app.v2_0.data.ProvisioningSettings;
import org.oscm.app.v2_0.data.Setting;
import org.oscm.app.v2_0.intf.APPlatformController;
import org.oscm.provisioning.data.*;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.management.*")
@PrepareForTest(APPlatformControllerFactory.class)
public class AsynchronousProvisioningProxyTest {

  private AsynchronousProvisioningProxy proxy;
  @Mock private APPlatformController controller;
  @Mock private EntityManager entityManager;
  @Mock private ProvisioningResults provisioningResults;
  @Mock private Logger logger;
  @Mock private ServiceInstanceDAO instanceDAO;
  @Mock private APPConfigurationServiceBean configService;
  @Mock private APPTimerServiceBean timerService;

  @Before
  public void setup() throws Exception {
    PowerMockito.mockStatic(APPlatformControllerFactory.class);
    when(APPlatformControllerFactory.getInstance(anyString())).thenReturn(controller);

    proxy = spy(new AsynchronousProvisioningProxy());
    proxy.em = entityManager;
    proxy.provResult = provisioningResults;
    proxy.logger = logger;
    proxy.instanceDAO = instanceDAO;
    proxy.configService = configService;
    proxy.timerService = timerService;
  }

  @Test
  public void testAsyncCreateInstance() throws Exception {
    // given
    InstanceRequest instanceRequest = new InstanceRequest();
    User user = new User();

    InstanceDescription description = new InstanceDescription();
    doReturn(description).when(proxy).getInstanceDescription(instanceRequest, user);

    ServiceInstance instance = new ServiceInstance();
    doReturn(instance).when(proxy).createPersistentServiceInstance(instanceRequest, description);

    BaseResult baseResult = new BaseResult();
    when(provisioningResults.getSuccesfulResult(any(), anyString())).thenReturn(baseResult);

    // when
    BaseResult result = proxy.asyncCreateInstance(instanceRequest, user);

    // then
    assertEquals(baseResult, result);
    verify(timerService, times(1)).initTimers();
    verify(proxy, times(1)).getInstanceDescription(instanceRequest, user);
    verify(proxy, times(1)).createPersistentServiceInstance(instanceRequest, description);
  }

  @Test
  public void testCreatePersistentServiceInstance() throws Exception {
    // given
    InstanceRequest request = new InstanceRequest();
    request.setOrganizationId("orgId");
    request.setOrganizationName("orgName");
    request.setParameterValue(
        Collections.singletonList(
            serviceParameter(InstanceParameter.CONTROLLER_ID, "controllerId")));

    InstanceDescription description = new InstanceDescription();
    description.setInstanceId("instanceId");
    description.setBaseUrl("baseUrl");
    HashMap<String, Setting> settingsMap = new HashMap<>();
    settingsMap.put("KEY1", new Setting("KEY1", "VALUE1"));
    settingsMap.put("KEY2", new Setting("KEY2", "VALUE1"));
    description.setChangedParameters(settingsMap);
    description.setChangedAttributes(settingsMap);

    // when
    ServiceInstance instance = proxy.createPersistentServiceInstance(request, description);

    // then
    assertEquals(request.getOrganizationId(), instance.getOrganizationId());
    assertEquals(request.getOrganizationName(), instance.getOrganizationName());
    assertEquals(description.getInstanceId(), instance.getInstanceId());
    assertEquals(description.getBaseUrl(), instance.getServiceBaseURL());
    instance
        .getInstanceParameters()
        .forEach(parameter -> assertTrue(settingsMap.containsKey(parameter.getParameterKey())));
    instance
        .getInstanceAttributes()
        .forEach(attribute -> assertTrue(settingsMap.containsKey(attribute.getAttributeKey())));
    verify(entityManager, times(1)).persist(any(ServiceInstance.class));
  }

  @Test
  public void testGetInstanceDescription() throws Exception {
    // given
    InstanceRequest instanceRequest = new InstanceRequest();
    instanceRequest.setParameterValue(
        Collections.singletonList(
            serviceParameter(InstanceParameter.CONTROLLER_ID, "controllerId")));

    InstanceDescription description = new InstanceDescription();
    description.setInstanceId("instanceId");
    when(controller.createInstance(any(ProvisioningSettings.class))).thenReturn(description);

    // when
    InstanceDescription instanceDescription =
        proxy.getInstanceDescription(instanceRequest, new User());

    // then
    assertEquals(description, instanceDescription);
  }

  @Test(expected = BadResultException.class)
  public void testGetInstanceDescription_InstanceIdIsEmpty() throws Exception {
    // given
    InstanceRequest instanceRequest = new InstanceRequest();
    instanceRequest.setParameterValue(
        Collections.singletonList(
            serviceParameter(InstanceParameter.CONTROLLER_ID, "controllerId")));

    InstanceDescription description = new InstanceDescription();
    when(controller.createInstance(any(ProvisioningSettings.class))).thenReturn(description);

    // when
    proxy.getInstanceDescription(instanceRequest, new User());
  }

  @Test(expected = BadResultException.class)
  public void testGetInstanceDescription_InstanceIsNotUnique() throws Exception {
    // given
    InstanceRequest instanceRequest = new InstanceRequest();
    instanceRequest.setParameterValue(
        Collections.singletonList(
            serviceParameter(InstanceParameter.CONTROLLER_ID, "controllerId")));

    InstanceDescription description = new InstanceDescription();
    description.setInstanceId("instanceId");
    when(controller.createInstance(any(ProvisioningSettings.class))).thenReturn(description);
    when(instanceDAO.exists(anyString())).thenReturn(true);

    // when
    proxy.getInstanceDescription(instanceRequest, new User());
  }

  @Test
  public void testSaveAttributes() {
    // given
    List<ServiceAttribute> attributes = Arrays.asList(serviceAttribute("KEY", "VALUE"));
    User user = new User();
    Query query = mock(Query.class);
    when(entityManager.createNamedQuery(anyString())).thenReturn(query);
    when(provisioningResults.newOkBaseResult()).thenCallRealMethod();
    when(provisioningResults.getBaseResult(any(), anyInt(), anyString())).thenCallRealMethod();

    // when
    BaseResult result = proxy.saveAttributes("orgId", attributes, user);

    // then
    assertEquals(0, result.getRc());
    assertEquals("Ok", result.getDesc());
    verify(entityManager, times(attributes.size())).persist(any(CustomAttribute.class));
  }

  @Test
  public void testCreateUsers() throws Exception {
    // given
    ServiceInstance instance = new ServiceInstance();
    instance.setInstanceId("instanceId");
    instance.setControllerId("controllerId");
    instance.setProvisioningStatus(ProvisioningStatus.COMPLETED);
    when(instanceDAO.getInstanceById(anyString())).thenReturn(instance);

    UserResult result = new UserResult();
    ArrayList<User> users = new ArrayList<>();
    result.setUsers(users);

    when(provisioningResults.getOKResult(any())).thenReturn(result);

    // when
    UserResult userResult = proxy.createUsers(instance.getInstanceId(), users, new User());

    // then
    assertEquals(result, userResult);
    verify(timerService, times(1)).initTimers();
    verify(entityManager, times(1)).persist(any());
  }

  @Test
  public void testUpdateUsers() throws Exception {
    // given
    ServiceInstance instance = new ServiceInstance();
    instance.setInstanceId("instanceId");
    instance.setControllerId("controllerId");
    instance.setProvisioningStatus(ProvisioningStatus.COMPLETED);
    when(instanceDAO.getInstanceById(anyString())).thenReturn(instance);

    BaseResult result = new BaseResult();
    when(provisioningResults.newOkBaseResult()).thenReturn(result);

    ArrayList<User> users = new ArrayList<>();

    // when
    BaseResult baseResult = proxy.updateUsers(instance.getInstanceId(), users, new User());

    // then
    assertEquals(result, baseResult);
    verify(timerService, times(1)).initTimers();
    verify(entityManager, times(1)).persist(any());
  }

  @Test
  public void testDeleteUsers() throws Exception {
    // given
    ServiceInstance instance = new ServiceInstance();
    instance.setInstanceId("instanceId");
    instance.setControllerId("controllerId");
    instance.setProvisioningStatus(ProvisioningStatus.COMPLETED);
    when(instanceDAO.getInstanceById(anyString())).thenReturn(instance);

    BaseResult result = new BaseResult();
    when(provisioningResults.newOkBaseResult()).thenReturn(result);

    ArrayList<User> users = new ArrayList<>();

    // when
    BaseResult baseResult = proxy.deleteUsers(instance.getInstanceId(), users, new User());

    // then
    assertEquals(result, baseResult);
    verify(timerService, times(1)).initTimers();
    verify(entityManager, times(1)).persist(any());
  }

  private static ServiceParameter serviceParameter(String key, String value) {
    ServiceParameter parameter = new ServiceParameter();
    parameter.setParameterId(key);
    parameter.setValue(value);
    return parameter;
  }

  private static ServiceAttribute serviceAttribute(String key, String value) {
    ServiceAttribute attribute = new ServiceAttribute();
    attribute.setAttributeId(key);
    attribute.setValue(value);
    return attribute;
  }
}
