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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import org.oscm.app.business.ProvisioningResults;
import org.oscm.app.domain.CustomAttribute;
import org.oscm.app.domain.InstanceParameter;
import org.oscm.app.domain.ServiceInstance;
import org.oscm.app.v2_0.data.InstanceDescription;
import org.oscm.app.v2_0.data.Setting;
import org.oscm.provisioning.data.*;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AsynchronousProvisioningProxyTest {

  @InjectMocks @Spy private AsynchronousProvisioningProxy app = new AsynchronousProvisioningProxy();
  @Mock private EntityManager entityManager;
  @Mock private ProvisioningResults provisioningResults;

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
    ServiceInstance instance = app.createPersistentServiceInstance(request, description);

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
  public void testSaveAttributes() {
    // given
    List<ServiceAttribute> attributes = Arrays.asList(serviceAttribute("KEY", "VALUE"));
    User user = new User();
    Query query = mock(Query.class);
    when(entityManager.createNamedQuery(anyString())).thenReturn(query);
    when(provisioningResults.newOkBaseResult()).thenCallRealMethod();
    when(provisioningResults.getBaseResult(any(), anyInt(), anyString())).thenCallRealMethod();

    // when
    BaseResult result = app.saveAttributes("orgId", attributes, user);

    // then
    assertEquals(0, result.getRc());
    assertEquals("Ok", result.getDesc());
    verify(entityManager, times(attributes.size())).persist(any(CustomAttribute.class));
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
