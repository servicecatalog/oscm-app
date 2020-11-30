package org.oscm.app.vmware.business.statemachine;

import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.TaskInfo;
import com.vmware.vim25.VimPortType;
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
import org.oscm.app.vmware.business.VMPropertyHandler;
import org.oscm.app.vmware.i18n.Messages;
import org.oscm.app.vmware.remote.vmware.VMClientPool;
import org.oscm.app.vmware.remote.vmware.VMwareClient;
import org.oscm.app.vmware.remote.vmware.VMwareClientFactory;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"javax.management.*", "javax.script.*", "jdk.internal.reflect.*"})
@PrepareForTest({SnapshotActions.class, VMClientPool.class, APPlatformServiceFactory.class, Messages.class})
public class SnapshotActionsTest {

  private SnapshotActions snapshotActions;
  private InstanceStatus instanceStatus;
  private VMPropertyHandler propertyHandler;
  private VMwareClientFactory vMwareClientFactory;
  private VMwareClient vMwareClient;
  private VMClientPool clientPool;
  private APPlatformService platformService;
  private KeyedObjectPool<String, VMwareClient> objectPool;
  private ManagedObjectReference objectReference;
  private VimPortType portType;
  private TaskInfo taskInfo;

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
    PowerMockito.mockStatic(Messages.class);

    instanceStatus = mock(InstanceStatus.class);
    propertyHandler = mock(VMPropertyHandler.class);
    vMwareClientFactory = mock(VMwareClientFactory.class);
    vMwareClient = mock(VMwareClient.class);
    clientPool = mock(VMClientPool.class);
    platformService = mock(APPlatformService.class);
    objectPool = mock(KeyedObjectPool.class);
    objectReference = mock(ManagedObjectReference.class);
    portType = mock(VimPortType.class);
    taskInfo = mock(TaskInfo.class);

    when(APPlatformServiceFactory.getInstance()).thenReturn(platformService);
    MockitoAnnotations.initMocks(this);

    snapshotActions = PowerMockito.spy(new SnapshotActions());
  }

  @Test
  public void testDeleteSnapshotReturnRun() throws Exception {
    // given
    PowerMockito.whenNew(VMPropertyHandler.class).withAnyArguments().thenReturn(propertyHandler);
    PowerMockito.whenNew(VMwareClientFactory.class).withAnyArguments().thenReturn(vMwareClientFactory);
    when(vMwareClientFactory.getInstance(propertyHandler)).thenReturn(vMwareClient);
    when(vMwareClient.findSnapshot(anyString(), anyString())).thenReturn(objectReference);
    when(vMwareClient.getService()).thenReturn(portType);
    // when
    String result = snapshotActions.deleteSnapshots("Instance ID", ps, instanceStatus);
    // then
    assertEquals("run", result);
  }

  @Test
  public void testDeleteSnapshotReturnSuccess() throws Exception {
    // given
    PowerMockito.whenNew(VMPropertyHandler.class).withAnyArguments().thenReturn(propertyHandler);
    PowerMockito.whenNew(VMwareClientFactory.class).withAnyArguments().thenReturn(vMwareClientFactory);
    when(vMwareClientFactory.getInstance(propertyHandler)).thenReturn(vMwareClient);
    // when
    String result = snapshotActions.deleteSnapshots("Instance ID", ps, instanceStatus);
    // then
    assertEquals("success", result);
  }

  @Test
  public void testDeleteSnapshotReturnFailed() throws Exception {
    // given
    PowerMockito.whenNew(VMPropertyHandler.class).withAnyArguments().thenReturn(propertyHandler);
    PowerMockito.whenNew(VMwareClientFactory.class).withAnyArguments().thenReturn(vMwareClientFactory);
    when(vMwareClientFactory.getInstance(propertyHandler)).thenReturn(vMwareClient);
    when(vMwareClient.findSnapshot(anyString(), anyString())).thenThrow(new Exception("Test"));
    // when
    String result = snapshotActions.deleteSnapshots("Instance ID", ps, instanceStatus);
    // then
    assertEquals("failed", result);
  }

  @Test
  public void testCreateSnapshotReturnRun() throws Exception {
    // given
    PowerMockito.whenNew(VMPropertyHandler.class).withAnyArguments().thenReturn(propertyHandler);
    PowerMockito.when(VMClientPool.getInstance()).thenReturn(clientPool);
    when(clientPool.getPool()).thenReturn(objectPool);
    when(objectPool.borrowObject(anyString())).thenReturn(vMwareClient);
    when(vMwareClient.getVirtualMachine(anyString())).thenReturn(objectReference);
    when(vMwareClient.getService()).thenReturn(portType);
    // when
    String result = snapshotActions.createSnapshot("Instance ID", ps, instanceStatus);
    // then
    assertEquals("run", result);
  }

  @Test
  public void testCreateSnapshotReturnFailed() throws Exception {
    // given
    PowerMockito.whenNew(VMPropertyHandler.class).withAnyArguments().thenReturn(propertyHandler);
    PowerMockito.when(VMClientPool.getInstance()).thenReturn(clientPool);
    when(clientPool.getPool()).thenReturn(objectPool);
    when(objectPool.borrowObject(anyString())).thenThrow(new IllegalStateException("Test"));
    // when
    String result = snapshotActions.createSnapshot("Instance ID", ps, instanceStatus);
    // then
    assertEquals("failed", result);
  }

  @Test
  public void testSuccessfulTaskReturnSuccess() {
    // given
    when(taskInfo.getName()).thenReturn("CreateSnapshot_Task");
    when(taskInfo.getResult()).thenReturn(objectReference);
    // when
    String result = snapshotActions.successfulTask(taskInfo, propertyHandler);
    // then
    assertEquals("success", result);
  }

  @Test
  public void testFinishReturnSuccess() throws Exception {
    // given
    PowerMockito.whenNew(VMPropertyHandler.class).withAnyArguments().thenReturn(propertyHandler);
    // when
    String result = snapshotActions.finish("Instance ID", ps, instanceStatus);
    // then
    assertEquals("success", result);
  }
}
