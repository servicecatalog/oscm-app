/*******************************************************************************
 *
 *  Copyright FUJITSU LIMITED 2020
 *
 *  Creation Date: 2020-11-20
 *
 *******************************************************************************/
package org.oscm.app.vmware.business.statemachine;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.oscm.app.v2_0.APPlatformServiceFactory;
import org.oscm.app.v2_0.data.PasswordAuthentication;
import org.oscm.app.v2_0.data.ProvisioningSettings;
import org.oscm.app.v2_0.data.Setting;
import org.oscm.app.v2_0.exceptions.SuspendException;
import org.oscm.app.v2_0.intf.APPlatformService;
import org.oscm.app.vmware.business.Controller;
import org.oscm.app.vmware.business.VMPropertyHandler;
import org.oscm.app.vmware.business.statemachine.api.StateMachineException;
import org.oscm.app.vmware.business.statemachine.api.StateMachineProperties;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"javax.management.*", "javax.script.*", "jdk.internal.reflect.*"})
@PrepareForTest({StateMachine.class, Thread.class, States.class, JAXBContext.class, APPlatformServiceFactory.class})
public class StateMachineTest {

  private StateMachine stateMachine;
  private Thread thread;
  private ClassLoader loader;
  private State mockState;
  private States states;
  private JAXBContext jaxbContext;
  private Unmarshaller unmarshaller;
  private VMPropertyHandler propertyHandler;
  private PasswordAuthentication passwordAuthentication;
  private APPlatformService apPlatformService;
  private InputStream inputStream;

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
  public void setUp() throws Exception {
    PowerMockito.mockStatic(Thread.class);
    PowerMockito.mockStatic(JAXBContext.class);
    PowerMockito.mockStatic(APPlatformServiceFactory.class);
    thread = mock(Thread.class);
    loader = mock(ClassLoader.class);
    states = mock(States.class);
    mockState = mock(State.class);
    jaxbContext = mock(JAXBContext.class);
    unmarshaller = mock(Unmarshaller.class);
    propertyHandler = mock(VMPropertyHandler.class);
    passwordAuthentication = mock(PasswordAuthentication.class);
    apPlatformService = mock(APPlatformService.class);
    inputStream = mock(InputStream.class);

    when(Thread.currentThread()).thenReturn(thread);
    when(thread.getContextClassLoader()).thenReturn(loader);
    when(loader.getResourceAsStream(anyString())).thenReturn(inputStream);
    when(JAXBContext.newInstance(States.class)).thenReturn(jaxbContext);
    when(jaxbContext.createUnmarshaller()).thenReturn(unmarshaller);
    when(unmarshaller.unmarshal(inputStream)).thenReturn(states);
    stateMachine = PowerMockito.spy(new StateMachine(ps));
  }

  @Test
  public void testInitializeProvisioningSettings() {

    StateMachine.initializeProvisioningSettings(ps, "CREATE_VM");

    assertEquals("", ps.getParameters().get(StateMachineProperties.SM_STATE_HISTORY).getValue());
    assertEquals("CREATE_VM", ps.getParameters().get(StateMachineProperties.SM_STATE_MACHINE).getValue());
    assertEquals("BEGIN", ps.getParameters().get(StateMachineProperties.SM_STATE).getValue());
  }

  @Test
  public void testExecuteAction() throws Exception {

    List<State> listStates = prepareListStates();
    when(states.getStates()).thenReturn(listStates);
    when(states.invokeAction(any(), anyString(), any(), any())).thenReturn("CREATE_VM");
    PowerMockito.when(stateMachine, "getNextState", listStates.get(0), "CREATE_VM").thenReturn("CREATE_VM");
    PowerMockito.whenNew(VMPropertyHandler.class).withAnyArguments().thenReturn(propertyHandler);
    when(propertyHandler.getServiceSetting(anyString())).thenReturn("suspended");

    stateMachine.executeAction(ps, "Instance ID", null);

    PowerMockito.verifyPrivate(stateMachine).invoke("setReferenceForTimeout", propertyHandler);
    verify(propertyHandler, times(1)).getServiceSetting(VMPropertyHandler.GUEST_READY_TIMEOUT_REF);
  }

  @Test
  public void testExecuteActionNotSameState() throws Exception {

    List<State> listStates = prepareListStates();
    when(states.getStates()).thenReturn(listStates);
    when(states.invokeAction(any(), anyString(), any(), any())).thenReturn("CREATE_VM");
    PowerMockito.when(stateMachine, "getNextState", listStates.get(0), "CREATE_VM").thenReturn("STOP_VM");
    PowerMockito.whenNew(VMPropertyHandler.class).withAnyArguments().thenReturn(propertyHandler);

    stateMachine.executeAction(ps, "Instance ID", null);

    PowerMockito.verifyPrivate(stateMachine).invoke("setReferenceForTimeout", propertyHandler);
    verify(propertyHandler, never()).getServiceSetting(VMPropertyHandler.GUEST_READY_TIMEOUT_REF);
  }

  @Test(expected = SuspendException.class)
  public void testExecuteActionThrowException() throws Exception {

    List<State> listStates = prepareListStates();
    when(states.getStates()).thenReturn(listStates);
    when(states.invokeAction(any(), anyString(), any(), any())).thenReturn("CREATE_VM");
    PowerMockito.when(stateMachine, "getNextState", listStates.get(0), "CREATE_VM").thenReturn("CREATE_VM");
    PowerMockito.whenNew(VMPropertyHandler.class).withAnyArguments().thenReturn(propertyHandler);
    when(propertyHandler.getServiceSetting(anyString())).thenReturn("500");
    when(mockState.getTimeout()).thenReturn("500");
    PowerMockito.doNothing().when(stateMachine, "storeSettings", "Instance ID", propertyHandler);

    stateMachine.executeAction(ps, "Instance ID", null);
  }

  @Test
  public void testStoreSettings() throws Exception {

    when(propertyHandler.getTechnologyProviderCredentials()).thenReturn(passwordAuthentication);
    when(APPlatformServiceFactory.getInstance()).thenReturn(apPlatformService);

    Whitebox.invokeMethod(stateMachine, "storeSettings", "Instance ID", propertyHandler);

    verify(propertyHandler, times(1)).getProvisioningSettings();
    verify(apPlatformService, times(1))
        .storeServiceInstanceDetails(Controller.ID, "Instance ID", propertyHandler.getProvisioningSettings(), passwordAuthentication);
  }

  @Test
  public void testGetReadyTimeout() throws Exception {

    when(mockState.getTimeout()).thenReturn("$555");
    when(propertyHandler.getGuestReadyTimeout(anyString())).thenReturn("5");

    String result = Whitebox.invokeMethod(stateMachine, "getReadyTimeout", mockState, propertyHandler);

    assertEquals("5", result);
  }

  @Test
  public void testExceededTimeoutReturnFalse() throws Exception {

    assertFalse(Whitebox.invokeMethod(stateMachine, "exceededTimeout", propertyHandler, ""));
  }

  @Test
  public void testExceededTimeoutCatchException() throws Exception {

    assertFalse(Whitebox.invokeMethod(stateMachine, "exceededTimeout", propertyHandler, ""));
  }

  @Test(expected = StateMachineException.class)
  public void testGetStateThrowException() throws Exception {

    Whitebox.invokeMethod(stateMachine, "getState", "");
  }

  @Test(expected = StateMachineException.class)
  public void testGetNextStateThrowException() throws Exception {

    Whitebox.invokeMethod(stateMachine, "getNextState", mockState, "");
  }

  @Test
  public void testAppendStateToHistoryReturnState() throws Exception {

    String result =
        Whitebox.invokeMethod(stateMachine, "appendStateToHistory", "State", "");

    assertEquals("State", result);
  }

  @Test
  public void testAppendStateToHistoryReturnStateHistory() throws Exception {

    String result =
        Whitebox.invokeMethod(stateMachine, "appendStateToHistory", "State", "HistoryState");

    assertEquals("HistoryState", result);
  }

  @Test
  public void testLoadPreviousStateFromHistory() throws Exception {

    String result = stateMachine.loadPreviousStateFromHistory(ps);

    assertEquals("State History", result);
  }

  private List<State> prepareListStates() {
    State state = new State();
    Event event = new Event();
    event.setId("CREATE_VM");
    event.setState("Creating");
    state.setId("CREATE_VM");
    state.setTimeout("100");
    List<Event> listEvents = new ArrayList<>();
    listEvents.add(event);
    state.setEvents(listEvents);
    List<State> listStates = new ArrayList<>();
    listStates.add(state);
    state = new State();
    state.setId("STOP_VM");
    state.setTimeout("500");
    listStates.add(state);
    return listStates;
  }
}
