package org.oscm.app.vmware.business.statemachine;

import com.vmware.vim25.*;
import org.apache.commons.pool2.KeyedObjectPool;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.oscm.app.v2_0.APPlatformServiceFactory;
import org.oscm.app.v2_0.data.InstanceStatus;
import org.oscm.app.v2_0.data.ProvisioningSettings;
import org.oscm.app.v2_0.data.Setting;
import org.oscm.app.v2_0.intf.APPlatformService;
import org.oscm.app.vmware.business.VM;
import org.oscm.app.vmware.business.VMPropertyHandler;
import org.oscm.app.vmware.remote.vmware.ServiceConnection;
import org.oscm.app.vmware.remote.vmware.VMClientPool;
import org.oscm.app.vmware.remote.vmware.VMwareClient;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"javax.management.*", "javax.script.*", "jdk.internal.reflect.*"})
@PrepareForTest({Actions.class, VMClientPool.class, APPlatformServiceFactory.class})
public class ActionsTest {

  private Actions actions;
  private InstanceStatus instanceStatus;
  private VMPropertyHandler propertyHandler;
  private VM vm;
  private VMwareClient vmClient;
  private VMClientPool clientPool;
  private APPlatformService platformService;
  private KeyedObjectPool<String, VMwareClient> objectPool;
  private TaskInfo taskInfo;
  private ServiceConnection serviceConnection;
  private VimPortType portType;
  private ManagedObjectReference objectReference;
  private ServiceContent serviceContent;
  private LocalizedMethodFault localizedMethodFault;
  private MethodFault methodFault;

  static ProvisioningSettings ps;
  static HashMap<String, Setting> parameters;
  static HashMap<String, Setting> configSettings;
  static HashMap<String, Setting> attributes;
  static HashMap<String, Setting> customAttributes;

  @BeforeClass
  public static void setup() {
    parameters = new HashMap<>();
    configSettings = new HashMap<>();
    attributes = new HashMap<>();
    customAttributes = new HashMap<>();

    parameters.put("SM_STATE", new Setting("state1", "CREATE_VM"));
    parameters.put("SM_STATE_HISTORY", new Setting("state2", "State History"));
    parameters.put("SM_STATE_MACHINE", new Setting("state3", "create_vm.xml"));
    configSettings.put("key1", new Setting("name1", "value1"));
    configSettings.put("key2", new Setting("name2", "value2"));
    attributes.put("attr1", new Setting("key1", "value1"));
    attributes.put("attr2", new Setting("key2", "value2"));
    customAttributes.put("cuAttr1", new Setting("key1", "value1"));
    customAttributes.put("cuAttr2", new Setting("key2", "value2"));

    ps = new ProvisioningSettings(parameters, attributes, customAttributes, configSettings, "en");
  }

  @Before
  public void setUp() {

    PowerMockito.mockStatic(VMClientPool.class);
    PowerMockito.mockStatic(APPlatformServiceFactory.class);

    instanceStatus = mock(InstanceStatus.class);
    propertyHandler = mock(VMPropertyHandler.class);
    vm = mock(VM.class);
    vmClient = mock(VMwareClient.class);
    clientPool = mock(VMClientPool.class);
    platformService = mock(APPlatformService.class);
    objectPool = mock(KeyedObjectPool.class);
    taskInfo = mock(TaskInfo.class);
    serviceConnection = mock(ServiceConnection.class);
    portType = mock(VimPortType.class);
    objectReference = mock(ManagedObjectReference.class);
    serviceContent = mock(ServiceContent.class);
    localizedMethodFault = mock(LocalizedMethodFault.class);
    methodFault = mock(MethodFault.class);

    when(APPlatformServiceFactory.getInstance()).thenReturn(platformService);
    MockitoAnnotations.initMocks(this);

    actions = PowerMockito.spy(new Actions());
  }

  @Test
  public void testConfigureVMReturnConfiguring() throws Exception {

    PowerMockito.whenNew(VMPropertyHandler.class).withAnyArguments().thenReturn(propertyHandler);
    PowerMockito.when(VMClientPool.getInstance()).thenReturn(clientPool);
    when(clientPool.getPool()).thenReturn(objectPool);
    when(objectPool.borrowObject(anyString())).thenReturn(vmClient);
    PowerMockito.whenNew(VM.class).withAnyArguments().thenReturn(vm);
    when(vm.reconfigureVirtualMachine(propertyHandler)).thenReturn(taskInfo);

    String result = actions.configureVM("Instance ID", ps, instanceStatus);

    assertEquals("configuring", result);
  }

  @Test
  public void testConfigureVMReturnFailed() {

    PowerMockito.when(VMClientPool.getInstance()).thenReturn(clientPool);
    when(clientPool.getPool()).thenReturn(objectPool);

    String result = actions.configureVM("Instance ID", ps, instanceStatus);

    assertEquals("failed", result);
  }

  @Test
  public void testShutdownVMReturnStopped() throws Exception {

    PowerMockito.whenNew(VMPropertyHandler.class).withAnyArguments().thenReturn(propertyHandler);
    PowerMockito.when(VMClientPool.getInstance()).thenReturn(clientPool);
    when(clientPool.getPool()).thenReturn(objectPool);
    when(objectPool.borrowObject(anyString())).thenReturn(vmClient);
    PowerMockito.whenNew(VM.class).withAnyArguments().thenReturn(vm);

    String result = actions.shutdownVM("Instance ID", ps, instanceStatus);

    assertEquals("stopped", result);
  }

  @Test
  public void testShutdownVMReturnFailed() {

    PowerMockito.when(VMClientPool.getInstance()).thenReturn(clientPool);
    when(clientPool.getPool()).thenReturn(objectPool);

    String result = actions.shutdownVM("Instance ID", ps, instanceStatus);

    assertEquals("failed", result);
  }

  @Test
  public void testPowerOffVMReturnStopping() throws Exception {

    PowerMockito.whenNew(VMPropertyHandler.class).withAnyArguments().thenReturn(propertyHandler);
    PowerMockito.when(VMClientPool.getInstance()).thenReturn(clientPool);
    when(clientPool.getPool()).thenReturn(objectPool);
    when(objectPool.borrowObject(anyString())).thenReturn(vmClient);
    PowerMockito.whenNew(VM.class).withAnyArguments().thenReturn(vm);
    when(vm.stop(true)).thenReturn(taskInfo);

    String result = actions.powerOffVM("Instance ID", ps, instanceStatus);

    assertEquals("stopping", result);
  }

  @Test
  public void testPowerOffVMReturnFailed() {

    PowerMockito.when(VMClientPool.getInstance()).thenReturn(clientPool);
    when(clientPool.getPool()).thenReturn(objectPool);

    String result = actions.powerOffVM("Instance ID", ps, instanceStatus);

    assertEquals("failed", result);
  }

  @Test
  public void testStartVMReturnStarting() throws Exception {

    PowerMockito.whenNew(VMPropertyHandler.class).withAnyArguments().thenReturn(propertyHandler);
    PowerMockito.when(VMClientPool.getInstance()).thenReturn(clientPool);
    when(clientPool.getPool()).thenReturn(objectPool);
    when(objectPool.borrowObject(anyString())).thenReturn(vmClient);
    PowerMockito.whenNew(VM.class).withAnyArguments().thenReturn(vm);
    when(vm.start()).thenReturn(taskInfo);

    String result = actions.startVM("Instance ID", ps, instanceStatus);

    assertEquals("starting", result);
  }

  @Test
  public void testStartVMReturnFailed() {

    PowerMockito.when(VMClientPool.getInstance()).thenReturn(clientPool);
    when(clientPool.getPool()).thenReturn(objectPool);

    String result = actions.startVM("Instance ID", ps, instanceStatus);

    assertEquals("failed", result);
  }

  @Test
  public void testCheckVMRunningReturnNotRunning() throws Exception {

    PowerMockito.whenNew(VMPropertyHandler.class).withAnyArguments().thenReturn(propertyHandler);
    PowerMockito.when(VMClientPool.getInstance()).thenReturn(clientPool);
    when(clientPool.getPool()).thenReturn(objectPool);
    when(objectPool.borrowObject(anyString())).thenReturn(vmClient);
    PowerMockito.whenNew(VM.class).withAnyArguments().thenReturn(vm);

    String result = actions.checkVMRunning("Instance ID", ps, instanceStatus);

    assertEquals("not running", result);
  }

  @Test
  public void testCheckVMRunningReturnFailed() {

    PowerMockito.when(VMClientPool.getInstance()).thenReturn(clientPool);
    when(clientPool.getPool()).thenReturn(objectPool);

    String result = actions.checkVMRunning("Instance ID", ps, instanceStatus);

    assertEquals("failed", result);
  }

  @Test
  public void testCheckVMStoppedReturnRunning() throws Exception {

    PowerMockito.whenNew(VMPropertyHandler.class).withAnyArguments().thenReturn(propertyHandler);
    PowerMockito.when(VMClientPool.getInstance()).thenReturn(clientPool);
    when(clientPool.getPool()).thenReturn(objectPool);
    when(objectPool.borrowObject(anyString())).thenReturn(vmClient);
    PowerMockito.whenNew(VM.class).withAnyArguments().thenReturn(vm);

    String result = actions.checkVMStopped("Instance ID", ps, instanceStatus);

    assertEquals("running", result);
  }

  @Test
  public void testCheckVMStoppedReturnFailed() {

    PowerMockito.when(VMClientPool.getInstance()).thenReturn(clientPool);
    when(clientPool.getPool()).thenReturn(objectPool);

    String result = actions.checkVMStopped("Instance ID", ps, instanceStatus);

    assertEquals("failed", result);
  }

  @Test
  public void testFinalizeProvisioningReturnSuccess() throws Exception {

    PowerMockito.whenNew(VMPropertyHandler.class).withAnyArguments().thenReturn(propertyHandler);
    PowerMockito.when(VMClientPool.getInstance()).thenReturn(clientPool);
    when(clientPool.getPool()).thenReturn(objectPool);
    when(objectPool.borrowObject(anyString())).thenReturn(vmClient);
    PowerMockito.whenNew(VM.class).withAnyArguments().thenReturn(vm);

    String result = actions.finalizeProvisioning("Instance ID", ps, instanceStatus);

    assertEquals("success", result);
  }

  @Test
  public void testFinalizeProvisioningReturnFailed() {

    PowerMockito.when(VMClientPool.getInstance()).thenReturn(clientPool);
    when(clientPool.getPool()).thenReturn(objectPool);

    String result = actions.finalizeProvisioning("Instance ID", ps, instanceStatus);

    assertEquals("failed", result);
  }

  @Test
  public void testFinish() {

    String result = actions.finish("Instance ID", ps, instanceStatus);

    assertEquals("success", result);
  }

  @Test
  public void testThrowSuspendException() {

    String result = actions.throwSuspendException("Instance ID", ps, instanceStatus);

    assertEquals("failed", result);
  }

  @Test
  public void testInspectTaskResultReturnSuccess() throws Exception {

    PowerMockito.whenNew(VMPropertyHandler.class).withAnyArguments().thenReturn(propertyHandler);
    PowerMockito.when(VMClientPool.getInstance()).thenReturn(clientPool);
    when(clientPool.getPool()).thenReturn(objectPool);
    when(objectPool.borrowObject(anyString())).thenReturn(vmClient);

    String result = actions.inspectTaskResult("Instance ID", ps, instanceStatus);

    assertEquals("success", result);
  }

  @Test
  public void testInspectTaskResultReturnCatchError() {

    PowerMockito.when(VMClientPool.getInstance()).thenReturn(clientPool);
    when(clientPool.getPool()).thenReturn(objectPool);

    String result = actions.inspectTaskResult("Instance ID", ps, instanceStatus);

    assertEquals("error", result);
  }

  @Test
  public void testInspectTaskResultReturnError() throws Exception {
    List<TaskInfo> taskList = new ArrayList<>();
    TaskInfo task = new TaskInfo();
    task.setKey("Task key");
    task.setState(TaskInfoState.ERROR);
    task.setError(localizedMethodFault);
    taskList.add(task);
    List<LocalizableMessage> messages = new ArrayList<>();
    LocalizableMessage message = new LocalizableMessage();
    message.setMessage("Message");
    messages.add(message);

    PowerMockito.whenNew(VMPropertyHandler.class).withAnyArguments().thenReturn(propertyHandler);
    PowerMockito.when(VMClientPool.getInstance()).thenReturn(clientPool);
    when(clientPool.getPool()).thenReturn(objectPool);
    when(objectPool.borrowObject(anyString())).thenReturn(vmClient);
    when(propertyHandler.getServiceSetting(anyString())).thenReturn("Task key");
    when(vmClient.getConnection()).thenReturn(serviceConnection);
    when(serviceConnection.getService()).thenReturn(portType);
    when(serviceConnection.getServiceContent()).thenReturn(serviceContent);
    when(serviceContent.getTaskManager()).thenReturn(objectReference);
    when(portType.createCollectorForTasks(any(), any())).thenReturn(objectReference);
    when(portType.readPreviousTasks(any(), anyInt())).thenReturn(taskList);
    when(localizedMethodFault.getFault()).thenReturn(methodFault);
    when(methodFault.getFaultMessage()).thenReturn(messages);
    when(methodFault.getFaultCause()).thenReturn(localizedMethodFault);

    when(taskInfo.getState()).thenReturn(TaskInfoState.ERROR);

    String result = actions.inspectTaskResult("Instance ID", ps, instanceStatus);

    assertEquals("error", result);
  }
}
