/**
 * *****************************************************************************
 *
 * <p>Copyright FUJITSU LIMITED 2020
 *
 * <p>Creation Date: 16.11.2020
 *
 * <p>*****************************************************************************
 */
package org.oscm.app.approval.controller;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.oscm.app.approval.data.State;
import org.oscm.app.v2_0.data.*;
import org.oscm.app.v2_0.exceptions.APPlatformException;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"javax.management.*", "javax.script.*", "jdk.internal.reflect.*"})
@PrepareForTest({ApprovalController.class})
public class ApprovalControllerTest {

  private ApprovalController approvalController;
  private ApprovalControllerAccess controllerAccess;
  private ProvisioningSettings settings;
  private PropertyHandler propertyHandler;
  private InstanceStatus instanceStatus;
  private ApprovalInstanceAccess approvalInstanceAccess;
  private Dispatcher dispatcher;
  private ProvisioningSettings provisioningSettings;
  private Properties properties;
  private ControllerSettings controllerSettings;

  @Before
  public void setup() {
    approvalController = PowerMockito.spy(new ApprovalController());
    controllerAccess = mock(ApprovalControllerAccess.class);
    settings = mock(ProvisioningSettings.class);
    propertyHandler = mock(PropertyHandler.class);
    approvalInstanceAccess = mock(ApprovalInstanceAccess.class);
    dispatcher = mock(Dispatcher.class);
    instanceStatus = mock(InstanceStatus.class);
    provisioningSettings = mock(ProvisioningSettings.class);
    properties = mock(Properties.class);
    controllerSettings = mock(ControllerSettings.class);
  }

  @Test
  public void testCreateInstance() throws Exception {

    HashMap<String, Setting> parameters = new HashMap<>();
    Setting setting = new Setting("custom", "dark");
    parameters.put("set", setting);

    PowerMockito.whenNew(PropertyHandler.class).withAnyArguments().thenReturn(propertyHandler);
    PowerMockito.doNothing().when(approvalController, "checkIfAlreadyExisting", anyString());
    when(settings.getParameters()).thenReturn(parameters);

    InstanceDescription result = approvalController.createInstance(settings);

    assertEquals(parameters, result.getChangedParameters());
  }

  @Test(expected = APPlatformException.class)
  public void testCheckIfAlreadyExisting() throws Exception {

    Collection<String> data = new ArrayList<String>() {{
      add("Already subscribed");
    }};
    PowerMockito.whenNew(ApprovalInstanceAccess.class).withNoArguments().thenReturn(approvalInstanceAccess);
    when(approvalInstanceAccess.getInstancesForOrganization(anyString())).thenReturn(data);

    Whitebox.invokeMethod(approvalController, "checkIfAlreadyExisting", "organizationId");
  }

  @Test
  public void testModifyInstance() throws Exception {

    HashMap<String, Setting> parameters = new HashMap<>();
    Setting setting = new Setting("custom", "dark");
    parameters.put("set", setting);

    HashMap<String, Setting> attributes = new HashMap<>();
    setting = new Setting("color", "grey");
    attributes.put("att", setting);
    propertyHandler = new PropertyHandler(settings);

    PowerMockito.whenNew(PropertyHandler.class).withAnyArguments().thenReturn(propertyHandler);
    when(settings.getParameters()).thenReturn(parameters);
    when(settings.getAttributes()).thenReturn(attributes);

    InstanceStatus result = approvalController.modifyInstance("instanceId", settings, settings);

    assertEquals(parameters, result.getChangedParameters());
    assertEquals(attributes, result.getChangedAttributes());
    assertEquals(State.MODIFICATION_REQUESTED, propertyHandler.getState());
  }

  @Test
  public void testDeleteInstance() throws Exception {

    HashMap<String, Setting> parameters = new HashMap<>();
    Setting setting = new Setting("custom", "dark");
    parameters.put("set", setting);

    HashMap<String, Setting> attributes = new HashMap<>();
    setting = new Setting("color", "grey");
    attributes.put("att", setting);
    propertyHandler = new PropertyHandler(settings);

    PowerMockito.whenNew(PropertyHandler.class).withAnyArguments().thenReturn(propertyHandler);
    when(settings.getParameters()).thenReturn(parameters);
    when(settings.getAttributes()).thenReturn(attributes);

    InstanceStatus result = approvalController.deleteInstance("instanceId", settings);

    assertEquals(parameters, result.getChangedParameters());
    assertEquals(attributes, result.getChangedAttributes());
    assertEquals(State.DELETION_REQUESTED, propertyHandler.getState());
  }

  @Test
  public void testGetInstanceStatus() throws Exception {

    PowerMockito.whenNew(PropertyHandler.class).withAnyArguments().thenReturn(propertyHandler);
    PowerMockito.whenNew(Dispatcher.class).withAnyArguments().thenReturn(dispatcher);
    when(dispatcher.dispatch()).thenReturn(instanceStatus);

    InstanceStatus result = approvalController.getInstanceStatus("instanceId", settings);

    assertEquals(instanceStatus, result);
  }

  @Test
  public void testNotifyInstance() throws Exception {

    assertNull(approvalController.notifyInstance("instanceId", provisioningSettings, properties));
  }

  @Test
  public void testActivateInstance() throws Exception {

    assertNull(approvalController.activateInstance("instanceId", provisioningSettings));
  }

  @Test
  public void testDeactivateInstance() throws Exception {

    assertNull(approvalController.deactivateInstance("instanceId", provisioningSettings));
  }

  @Test
  public void testCreateUsers() throws Exception {

    List<ServiceUser> users = new ArrayList<>();

    assertNull(approvalController.createUsers("instanceId", provisioningSettings, users));
  }

  @Test
  public void testDeleteUsers() throws Exception {

    List<ServiceUser> users = new ArrayList<>();

    assertNull(approvalController.deleteUsers("instanceId", provisioningSettings, users));
  }

  @Test
  public void testUpdateUsers() throws Exception {

    List<ServiceUser> users = new ArrayList<>();

    assertNull(approvalController.updateUsers("instanceId", provisioningSettings, users));
  }

  @Test
  public void testGetControllerStatus() throws Exception {

    assertNull(approvalController.getControllerStatus(controllerSettings));
  }

  @Test
  public void testGetOperationParameters() throws Exception {

    assertNull(approvalController.getOperationParameters(anyString(), anyString(), anyString(), any()));
  }

  @Test
  public void testExecuteServiceOperation() throws Exception {

    assertNull(approvalController.executeServiceOperation(anyString(), anyString(), anyString(), anyString(), anyList(), any()));
  }

  @Test
  public void testSetControllerSettings() {

    approvalController.setControllerSettings(controllerSettings);

    verify(controllerAccess, never()).storeSettings(settings);
  }
}
