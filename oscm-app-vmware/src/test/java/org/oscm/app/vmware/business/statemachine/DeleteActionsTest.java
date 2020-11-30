package org.oscm.app.vmware.business.statemachine;

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
import org.oscm.app.v2_0.exceptions.APPlatformException;
import org.oscm.app.v2_0.intf.APPlatformService;
import org.oscm.app.vmware.business.VM;
import org.oscm.app.vmware.business.VMPropertyHandler;
import org.oscm.app.vmware.i18n.Messages;
import org.oscm.app.vmware.remote.vmware.VMClientPool;
import org.oscm.app.vmware.remote.vmware.VMwareClient;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"javax.management.*", "javax.script.*", "jdk.internal.reflect.*"})
@PrepareForTest({DeleteActions.class, VMClientPool.class, APPlatformServiceFactory.class, Messages.class})
public class DeleteActionsTest {

  private DeleteActions deleteActions;
  private InstanceStatus instanceStatus;
  private VMPropertyHandler propertyHandler;
  private VM vm;
  private VMwareClient vmClient;
  private VMClientPool clientPool;
  private APPlatformService platformService;
  private KeyedObjectPool<String, VMwareClient> objectPool;

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
    vm = mock(VM.class);
    vmClient = mock(VMwareClient.class);
    clientPool = mock(VMClientPool.class);
    platformService = mock(APPlatformService.class);
    objectPool = mock(KeyedObjectPool.class);

    when(APPlatformServiceFactory.getInstance()).thenReturn(platformService);
    MockitoAnnotations.initMocks(this);

    deleteActions = PowerMockito.spy(new DeleteActions());
  }

  @Test
  public void testCheckVMExistsReturnExists() throws Exception {
    // given
    PowerMockito.whenNew(VMPropertyHandler.class).withAnyArguments().thenReturn(propertyHandler);
    when(propertyHandler.getInstanceName()).thenReturn("InstanceName");
    when(propertyHandler.getServiceSetting(anyString())).thenReturn("VCenter");
    PowerMockito.when(VMClientPool.getInstance()).thenReturn(clientPool);
    when(clientPool.getPool()).thenReturn(objectPool);
    when(objectPool.borrowObject(anyString())).thenReturn(vmClient);
    PowerMockito.whenNew(VM.class).withAnyArguments().thenReturn(vm);
    // when
    String result = deleteActions.checkVMExists("Instance ID", ps, instanceStatus);
    // then
    assertEquals("exists", result);
  }

  @Test
  public void testCheckVMExistsReturnNotExists() throws Exception {
    // given
    PowerMockito.whenNew(VMPropertyHandler.class).withAnyArguments().thenReturn(propertyHandler);
    when(propertyHandler.getInstanceName()).thenReturn("InstanceName");
    when(propertyHandler.getServiceSetting(anyString())).thenReturn("VCenter");
    PowerMockito.when(VMClientPool.getInstance()).thenReturn(clientPool);
    when(clientPool.getPool()).thenReturn(objectPool);
    when(objectPool.borrowObject(anyString())).thenThrow(new IllegalStateException("Test"));
    // when
    String result = deleteActions.checkVMExists("Instance ID", ps, instanceStatus);
    // then
    assertEquals("not exists", result);
  }

  @Test
  public void testReleaseIPAddressReturnSuccess() throws Exception {
    // given
    PowerMockito.whenNew(VMPropertyHandler.class).withAnyArguments().thenReturn(propertyHandler);
    // when
    String result = deleteActions.releaseIPaddress("Instance ID", ps, instanceStatus);
    // then
    assertEquals("success", result);
  }

  @Test
  public void testReleaseIPAddressReturnFailed() throws Exception {
    // given
    PowerMockito.whenNew(VMPropertyHandler.class).withAnyArguments().thenReturn(propertyHandler);
    doThrow(new Exception("Test")).when(propertyHandler).releaseManuallyDefinedIPAddresses();
    PowerMockito.when(Messages.get(anyString(), anyString(), any())).thenReturn("Failed");
    // when
    String result = deleteActions.releaseIPaddress("Instance ID", ps, instanceStatus);
    // then
    assertEquals("failed", result);
  }

  @Test
  public void testDeleteVMReturnDeleting() throws Exception {
    // given
    PowerMockito.whenNew(VMPropertyHandler.class).withAnyArguments().thenReturn(propertyHandler);
    when(propertyHandler.getServiceSetting(anyString())).thenReturn("VCenter");
    PowerMockito.when(VMClientPool.getInstance()).thenReturn(clientPool);
    when(clientPool.getPool()).thenReturn(objectPool);
    when(objectPool.borrowObject(anyString())).thenReturn(vmClient);
    PowerMockito.whenNew(VM.class).withAnyArguments().thenReturn(vm);
    // when
    String result = deleteActions.deleteVM("Instance ID", ps, instanceStatus);
    // then
    assertEquals("deleting", result);
  }

  @Test
  public void testDeleteVMReturnStopping() throws Exception {
    // given
    PowerMockito.whenNew(VMPropertyHandler.class).withAnyArguments().thenReturn(propertyHandler);
    when(propertyHandler.getServiceSetting(anyString())).thenReturn("VCenter");
    PowerMockito.when(VMClientPool.getInstance()).thenReturn(clientPool);
    when(clientPool.getPool()).thenReturn(objectPool);
    when(objectPool.borrowObject(anyString())).thenReturn(vmClient);
    PowerMockito.whenNew(VM.class).withAnyArguments().thenReturn(vm);
    when(vm.isRunning()).thenReturn(true);
    // when
    String result = deleteActions.deleteVM("Instance ID", ps, instanceStatus);
    // then
    assertEquals("stoping", result);
  }

  @Test
  public void testDeleteVMReturnFailed() throws Exception {
    // given
    when(propertyHandler.getServiceSetting(anyString())).thenReturn("VCenter");
    PowerMockito.when(VMClientPool.getInstance()).thenReturn(clientPool);
    when(clientPool.getPool()).thenReturn(objectPool);
    when(objectPool.borrowObject(anyString())).thenReturn(vmClient);
    PowerMockito.when(Messages.get(anyString(), anyString(), any())).thenReturn("Script executing not completed");
    // when
    String result = deleteActions.deleteVM("Instance ID", ps, instanceStatus);
    // then
    assertEquals("failed", result);
  }

  @Test
  public void testNotifyAdministratorReturnSuccess() throws Exception {
    // given
    PowerMockito.whenNew(VMPropertyHandler.class).withAnyArguments().thenReturn(propertyHandler);
    // when
    String result = deleteActions.notifyAdministrator("Instance ID", ps, instanceStatus);
    // then
    assertEquals("success", result);
  }

  @Test
  public void testNotifyAdministratorReturnSuccessAndSendEmail() throws Exception {
    // given
    PowerMockito.whenNew(VMPropertyHandler.class).withAnyArguments().thenReturn(propertyHandler);
    when(propertyHandler.getServiceSetting(anyString())).thenReturn("test");
    when(propertyHandler.getSettings()).thenReturn(ps);
    PowerMockito.when(Messages.get(anyString(), anyString(), any())).thenReturn("Subject");
    // when
    String result = deleteActions.notifyAdministrator("Instance ID", ps, instanceStatus);
    // then
    assertEquals("success", result);
  }

  @Test
  public void testNotifyAdministratorReturnFailed() throws Exception {
    // given
    PowerMockito.whenNew(VMPropertyHandler.class).withAnyArguments().thenReturn(propertyHandler);
    when(propertyHandler.getServiceSetting(anyString())).thenReturn("test");
    when(propertyHandler.getSettings()).thenReturn(ps);
    when(propertyHandler.getInstanceName()).thenThrow(new APPlatformException("Test"));
    PowerMockito.when(Messages.get(anyString(), anyString(), any())).thenReturn("Email not sent");
    // when
    String result = deleteActions.notifyAdministrator("Instance ID", ps, instanceStatus);
    // then
    assertEquals("failed", result);
  }
}
