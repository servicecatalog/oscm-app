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
import org.oscm.app.v2_0.exceptions.ConfigurationException;
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

import java.net.URLEncoder;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"javax.management.*", "javax.script.*", "jdk.internal.reflect.*"})
@PrepareForTest({CreateActions.class, VMClientPool.class, APPlatformServiceFactory.class,
    Pattern.class, Messages.class, URLEncoder.class, MessageFormat.class})
public class CreateActionsTest {

  private CreateActions createActions;
  private InstanceStatus instanceStatus;
  private VMPropertyHandler propertyHandler;
  private VM vm;
  private VMwareClient vmClient;
  private VMClientPool clientPool;
  private APPlatformService platformService;
  private KeyedObjectPool<String, VMwareClient> objectPool;
  private Pattern pattern;
  private Matcher matcher;
  private StringBuffer stringBuffer;

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
    PowerMockito.mockStatic(Pattern.class);
    PowerMockito.mockStatic(Messages.class);
    PowerMockito.mockStatic(URLEncoder.class);
    PowerMockito.mockStatic(MessageFormat.class);

    instanceStatus = mock(InstanceStatus.class);
    propertyHandler = mock(VMPropertyHandler.class);
    vm = mock(VM.class);
    vmClient = mock(VMwareClient.class);
    clientPool = mock(VMClientPool.class);
    platformService = mock(APPlatformService.class);
    objectPool = mock(KeyedObjectPool.class);
    pattern = PowerMockito.mock(Pattern.class);
    matcher = PowerMockito.mock(Matcher.class);
    stringBuffer = PowerMockito.mock(StringBuffer.class);

    when(APPlatformServiceFactory.getInstance()).thenReturn(platformService);
    MockitoAnnotations.initMocks(this);

    createActions = PowerMockito.spy(new CreateActions());
  }

  @Test
  public void testImportVMReturnSkipped() {
    // when
    String result = createActions.importVM("Instance ID", ps, instanceStatus);
    // then
    assertEquals("skipped", result);
  }

  @Test
  public void testImportVMReturnImported() throws Exception {
    // given
    PowerMockito.whenNew(VMPropertyHandler.class).withAnyArguments().thenReturn(propertyHandler);
    when(propertyHandler.isServiceSettingTrue(anyString())).thenReturn(true);
    PowerMockito.when(VMClientPool.getInstance()).thenReturn(clientPool);
    when(clientPool.getPool()).thenReturn(objectPool);
    when(objectPool.borrowObject(anyString())).thenReturn(vmClient);
    PowerMockito.whenNew(VM.class).withAnyArguments().thenReturn(vm);
    // when
    String result = createActions.importVM("Instance ID", ps, instanceStatus);
    // then
    assertEquals("imported", result);
  }

  @Test
  public void testImportVMReturnFailed() throws Exception {
    // given
    PowerMockito.whenNew(VMPropertyHandler.class).withAnyArguments().thenReturn(propertyHandler);
    when(propertyHandler.isServiceSettingTrue(anyString())).thenReturn(true);
    PowerMockito.when(VMClientPool.getInstance()).thenReturn(clientPool);
    when(clientPool.getPool()).thenReturn(objectPool);
    when(objectPool.borrowObject(anyString())).thenThrow(new IllegalStateException("Test"));
    PowerMockito.when(MessageFormat.format(anyString(), any())).thenReturn("Script executing not completed");
    // when
    String result = createActions.importVM("Instance ID", ps, instanceStatus);
    // then
    assertEquals("failed", result);
  }

  @Test
  public void testValidateInstanceNameReturnSuccess() throws Exception {
    // given
    PowerMockito.whenNew(VMPropertyHandler.class).withAnyArguments().thenReturn(propertyHandler);
    when(propertyHandler.getServiceSetting(anyString())).thenReturn("regex");
    when(propertyHandler.getInstanceName()).thenReturn("Instance");
    PowerMockito.when(Pattern.compile("regex")).thenReturn(pattern);
    when(pattern.matcher(anyString())).thenReturn(matcher);
    when(matcher.matches()).thenReturn(true);
    // when
    String result = createActions.validateInstanceName("Instance ID", ps, instanceStatus);
    // then
    assertEquals("success", result);
  }

  @Test(expected = APPlatformException.class)
  public void testValidateInstanceNameReturnException() throws Exception {
    // given
    PowerMockito.whenNew(VMPropertyHandler.class).withAnyArguments().thenReturn(propertyHandler);
    when(propertyHandler.getServiceSetting(anyString())).thenReturn("regex");
    when(propertyHandler.getInstanceName()).thenReturn("Instance");
    PowerMockito.when(Pattern.compile("regex")).thenReturn(pattern);
    when(pattern.matcher(anyString())).thenReturn(matcher);
    // when
    createActions.validateInstanceName("Instance ID", ps, instanceStatus);
  }

  @Test
  public void testReserveIPAddressReturnFailed() throws Exception {
    // when
    String result = createActions.reserveIPAddress("Instance ID", ps, instanceStatus);
    // then
    assertEquals("failed", result);
  }

  @Test
  public void testReserveIPAddressReturnSuccess() throws Exception {
    // given
    PowerMockito.whenNew(VMPropertyHandler.class).withAnyArguments().thenReturn(propertyHandler);
    // when
    String result = createActions.reserveIPAddress("Instance ID", ps, instanceStatus);
    // then
    assertEquals("success", result);
  }

  @Test
  public void testCreateVMReturnCreating() throws Exception {
    // given
    PowerMockito.whenNew(VMPropertyHandler.class).withAnyArguments().thenReturn(propertyHandler);
    PowerMockito.when(VMClientPool.getInstance()).thenReturn(clientPool);
    when(clientPool.getPool()).thenReturn(objectPool);
    when(objectPool.borrowObject(anyString())).thenReturn(vmClient);
    PowerMockito.whenNew(VM.class).withAnyArguments().thenReturn(vm);
    // when
    String result = createActions.createVM("Instance ID", ps, instanceStatus);
    // then
    assertEquals("creating", result);
  }

  @Test
  public void testCreateVMReturnFailed() {
    // given
    PowerMockito.when(VMClientPool.getInstance()).thenReturn(clientPool);
    when(clientPool.getPool()).thenReturn(objectPool);
    PowerMockito.when(Messages.get(anyString(), anyString(), any())).thenReturn("Create not completed");
    // when
    String result = createActions.createVM("Instance ID", ps, instanceStatus);
    // then
    assertEquals("failed", result);
  }

  @Test
  public void testExecuteScriptReturnSuccess() throws Exception {
    // given
    PowerMockito.whenNew(VMPropertyHandler.class).withAnyArguments().thenReturn(propertyHandler);
    PowerMockito.when(VMClientPool.getInstance()).thenReturn(clientPool);
    when(clientPool.getPool()).thenReturn(objectPool);
    when(objectPool.borrowObject(anyString())).thenReturn(vmClient);
    when(propertyHandler.getServiceSetting(anyString())).thenReturn("test");
    PowerMockito.whenNew(VM.class).withAnyArguments().thenReturn(vm);
    // when
    String result = createActions.executeScript("Instance ID", ps, instanceStatus);
    // then
    assertEquals("success", result);
    verify(vm, times(1)).runScript(propertyHandler);
  }

  @Test
  public void testExecuteScriptReturnFailed() throws Exception {
    // given
    PowerMockito.when(VMClientPool.getInstance()).thenReturn(clientPool);
    when(clientPool.getPool()).thenReturn(objectPool);
    PowerMockito.when(Messages.get(anyString(), anyString(), any())).thenReturn("Script executing not completed");
    when(objectPool.borrowObject(anyString())).thenThrow(new IllegalStateException("Test"));
    // when
    String result = createActions.executeScript("Instance ID", ps, instanceStatus);
    // then
    assertEquals("failed", result);
  }

  @Test
  public void testUpdateLinuxPwdReturnSuccess() throws Exception {
    // given
    PowerMockito.whenNew(VMPropertyHandler.class).withAnyArguments().thenReturn(propertyHandler);
    PowerMockito.when(VMClientPool.getInstance()).thenReturn(clientPool);
    when(clientPool.getPool()).thenReturn(objectPool);
    when(objectPool.borrowObject(anyString())).thenReturn(vmClient);
    when(propertyHandler.getServiceSetting(anyString())).thenReturn("test");
    PowerMockito.whenNew(VM.class).withAnyArguments().thenReturn(vm);
    when(vm.isScriptExecuting()).thenReturn(false);
    // when
    String result = createActions.updateLinuxPwd("Instance ID", ps, instanceStatus);
    // then
    assertEquals("success", result);
    verify(vm, times(1)).updateLinuxVMPassword(propertyHandler);
  }

  @Test
  public void testUpdateLinuxPwdReturnFailed() throws Exception {
    // given
    PowerMockito.when(VMClientPool.getInstance()).thenReturn(clientPool);
    when(clientPool.getPool()).thenReturn(objectPool);
    PowerMockito.when(Messages.get(anyString(), anyString(), any())).thenReturn("Password updating not completed");
    when(objectPool.borrowObject(anyString())).thenThrow(new IllegalStateException("Test"));
    // when
    String result = createActions.updateLinuxPwd("Instance ID", ps, instanceStatus);
    // then
    assertEquals("failed", result);
  }

  @Test
  public void testSuspendAfterCreationReturnSuccess() throws Exception {
    // given
    PowerMockito.whenNew(VMPropertyHandler.class).withAnyArguments().thenReturn(propertyHandler);
    // when
    String result = createActions.suspendAfterCreation("Instance ID", ps, instanceStatus);
    // then
    assertEquals("success", result);
    verify(instanceStatus, never()).setRunWithTimer(false);
  }

  @Test
  public void testSuspendAfterCreationReturnSuccessAndSendEmail() throws Exception {
    // given
    PowerMockito.whenNew(VMPropertyHandler.class).withAnyArguments().thenReturn(propertyHandler);
    when(propertyHandler.getServiceSetting(anyString())).thenReturn("test");
    PowerMockito.whenNew(StringBuffer.class).withAnyArguments().thenReturn(stringBuffer);
    when(stringBuffer.append(anyString())).thenReturn(stringBuffer);
    when(propertyHandler.getSettings()).thenReturn(ps);
    PowerMockito.when(Messages.get(anyString(), anyString(), any())).thenReturn("Subject");
    // when
    String result = createActions.suspendAfterCreation("Instance ID", ps, instanceStatus);
    // then
    assertEquals("success", result);
    verify(instanceStatus, times(1)).setRunWithTimer(false);
  }

  @Test
  public void testSuspendAfterCreationReturnFailed() throws Exception {
    // given
    PowerMockito.whenNew(VMPropertyHandler.class).withAnyArguments().thenReturn(propertyHandler);
    when(propertyHandler.getServiceSetting(anyString())).thenReturn("test");
    when(platformService.getEventServiceUrl()).thenThrow(new ConfigurationException("Test exception"));
    PowerMockito.when(Messages.get(anyString(), anyString(), any())).thenReturn("Email not sent");
    // when
    String result = createActions.suspendAfterCreation("Instance ID", ps, instanceStatus);
    // then
    assertEquals("failed", result);
  }
}
