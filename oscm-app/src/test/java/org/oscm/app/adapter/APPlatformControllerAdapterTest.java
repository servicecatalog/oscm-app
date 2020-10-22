/**
 * *****************************************************************************
 *
 * <p>Copyright FUJITSU LIMITED 2018
 *
 * <p>Creation Date: 29.10.15 10:08
 *
 * <p>****************************************************************************
 */
package org.oscm.app.adapter;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.oscm.app.v2_0.data.ControllerSettings;
import org.oscm.app.v2_0.data.OperationParameter;
import org.oscm.app.v2_0.data.ProvisioningSettings;
import org.oscm.app.v2_0.data.ServiceUser;
import org.oscm.app.v2_0.intf.APPlatformController;

import java.util.ArrayList;
import java.util.Properties;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class APPlatformControllerAdapterTest {

  private APPlatformControllerAdapter adapter;
  @Mock private APPlatformController delegate;

  @Before
  public void setUp() {
    adapter = new APPlatformControllerAdapter(delegate);
  }

  @Test
  public void constructorTest() {
    assertNotNull(adapter.getDelegate());
  }

  @Test
  public void testCreateInstance() throws Exception {
    // given
    ProvisioningSettings provisioningSettings = mock(ProvisioningSettings.class);
    // when
    adapter.createInstance(provisioningSettings);
    // then
    verify(delegate, times(1)).createInstance(provisioningSettings);
  }

  @Test
  public void testModifyInstance() throws Exception {
    // given
    ProvisioningSettings provisioningSettings = mock(ProvisioningSettings.class);
    String instanceId = "instance_id";
    // when
    adapter.modifyInstance(instanceId, provisioningSettings, provisioningSettings);
    // then
    verify(delegate, times(1))
        .modifyInstance(instanceId, provisioningSettings, provisioningSettings);
  }

  @Test
  public void testDeleteInstance() throws Exception {
    // given
    ProvisioningSettings provisioningSettings = mock(ProvisioningSettings.class);
    String instanceId = "instance_id";
    // when
    adapter.deleteInstance(instanceId, provisioningSettings);
    // then
    verify(delegate, times(1)).deleteInstance(instanceId, provisioningSettings);
  }

  @Test
  public void testGetInstanceStatus() throws Exception {
    // given
    ProvisioningSettings provisioningSettings = mock(ProvisioningSettings.class);
    String instanceId = "instance_id";
    // when
    adapter.getInstanceStatus(instanceId, provisioningSettings);
    // then
    verify(delegate, times(1)).getInstanceStatus(instanceId, provisioningSettings);
  }

  @Test
  public void testNotifyInstance() throws Exception {
    // given
    ProvisioningSettings provisioningSettings = mock(ProvisioningSettings.class);
    String instanceId = "instance_id";
    Properties properties = new Properties();
    // when
    adapter.notifyInstance(instanceId, provisioningSettings, properties);
    // then
    verify(delegate, times(1)).notifyInstance(instanceId, provisioningSettings, properties);
  }

  @Test
  public void testActivateInstance() throws Exception {
    // given
    ProvisioningSettings provisioningSettings = mock(ProvisioningSettings.class);
    String instanceId = "instance_id";
    // when
    adapter.activateInstance(instanceId, provisioningSettings);
    // then
    verify(delegate, times(1)).activateInstance(instanceId, provisioningSettings);
  }

  @Test
  public void testDeactivateInstance() throws Exception {
    // given
    ProvisioningSettings provisioningSettings = mock(ProvisioningSettings.class);
    String instanceId = "instance_id";
    // when
    adapter.deactivateInstance(instanceId, provisioningSettings);
    // then
    verify(delegate, times(1)).deactivateInstance(instanceId, provisioningSettings);
  }

  @Test
  public void testCreateUsers() throws Exception {
    // given
    ProvisioningSettings provisioningSettings = mock(ProvisioningSettings.class);
    String instanceId = "instance_id";
    ArrayList<ServiceUser> serviceUsers = new ArrayList<>();
    // when
    adapter.createUsers(instanceId, provisioningSettings, serviceUsers);
    // then
    verify(delegate, times(1)).createUsers(instanceId, provisioningSettings, serviceUsers);
  }

  @Test
  public void testDeleteUsers() throws Exception {
    // given
    ProvisioningSettings provisioningSettings = mock(ProvisioningSettings.class);
    String instanceId = "instance_id";
    ArrayList<ServiceUser> serviceUsers = new ArrayList<>();
    // when
    adapter.deleteUsers(instanceId, provisioningSettings, serviceUsers);
    // then
    verify(delegate, times(1)).deleteUsers(instanceId, provisioningSettings, serviceUsers);
  }

  @Test
  public void testUpdateUsers() throws Exception {
    // given
    ProvisioningSettings provisioningSettings = mock(ProvisioningSettings.class);
    String instanceId = "instance_id";
    ArrayList<ServiceUser> serviceUsers = new ArrayList<>();
    // when
    adapter.updateUsers(instanceId, provisioningSettings, serviceUsers);
    // then
    verify(delegate, times(1)).updateUsers(instanceId, provisioningSettings, serviceUsers);
  }

  @Test
  public void testGetControllerStatus() throws Exception {
    // given
    ControllerSettings settings = mock(ControllerSettings.class);
    // when
    adapter.getControllerStatus(settings);
    // then
    verify(delegate, times(1)).getControllerStatus(settings);
  }

  @Test
  public void testGetOperationParameters() throws Exception {
    // given
    ProvisioningSettings provisioningSettings = mock(ProvisioningSettings.class);
    String instanceId = "instance_id";
    String operationId = "operation_id";
    String userId = "user_id";
    // when
    adapter.getOperationParameters(userId, instanceId, operationId, provisioningSettings);
    // then
    verify(delegate, times(1))
        .getOperationParameters(userId, instanceId, operationId, provisioningSettings);
  }

  @Test
  public void testExecuteServiceOperation() throws Exception {
    // given
    ProvisioningSettings provisioningSettings = mock(ProvisioningSettings.class);
    String instanceId = "instance_id";
    String operationId = "operation_id";
    String userId = "user_id";
    String transactionId = "transaction_id";
    ArrayList<OperationParameter> parameters = new ArrayList<>();
    // when
    adapter.executeServiceOperation(
        userId, instanceId, operationId, transactionId, parameters, provisioningSettings);
    // then
    verify(delegate, times(1))
        .executeServiceOperation(
            userId, instanceId, operationId, transactionId, parameters, provisioningSettings);
  }

  @Test
  public void testSetControllerSettings() {
    // given
    ControllerSettings settings = mock(ControllerSettings.class);
    // when
    adapter.setControllerSettings(settings);
    // then
    verify(delegate, times(1)).setControllerSettings(settings);
  }

  @Test
  public void testGetServersNumber() throws Exception {
    // given
    String instanceId = "instance_id";
    String subscriptionId = "subscription_id";
    String orgId = "org_id";
    // when
    adapter.getServersNumber(instanceId, subscriptionId, orgId);
    // then
    verify(delegate, times(1)).getServersNumber(instanceId, subscriptionId, orgId);
  }

  @Test
  public void testGatherUsageData() throws Exception {
    // given
    ProvisioningSettings provisioningSettings = mock(ProvisioningSettings.class);
    String instanceId = "instance_id";
    String startTime = "start_time";
    String controllerId = "controller_id";
    String endTime = "end_time";
    // when
    adapter.gatherUsageData(controllerId, instanceId, startTime, endTime, provisioningSettings);
    // then
    verify(delegate, times(1))
        .gatherUsageData(controllerId, instanceId, startTime, endTime, provisioningSettings);
  }

  @Test
  public void testPing() throws Exception {
    // given
    String controllerId = "controller_id";
    // when
    adapter.ping(controllerId);
    // then
    verify(delegate, times(1)).ping(controllerId);
  }

  @Test
  public void testCanPing() throws Exception {
    // when
    adapter.canPing();
    // then
    verify(delegate, times(1)).canPing();
  }
}
