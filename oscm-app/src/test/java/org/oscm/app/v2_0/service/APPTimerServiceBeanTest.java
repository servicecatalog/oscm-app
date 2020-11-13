/**
 * *****************************************************************************
 *
 * <p>Copyright FUJITSU LIMITED 2018
 *
 * <p>Creation Date: 28.11.2014
 *
 * <p>*****************************************************************************
 */
package org.oscm.app.v2_0.service;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.oscm.app.business.APPlatformControllerFactory;
import org.oscm.app.business.ProductProvisioningServiceFactoryBean;
import org.oscm.app.business.exceptions.BESNotificationException;
import org.oscm.app.dao.BesDAO;
import org.oscm.app.dao.OperationDAO;
import org.oscm.app.dao.ServiceInstanceDAO;
import org.oscm.app.domain.*;
import org.oscm.app.v2_0.data.InstanceStatus;
import org.oscm.app.v2_0.data.ProvisioningSettings;
import org.oscm.app.v2_0.data.Setting;
import org.oscm.app.v2_0.exceptions.*;
import org.oscm.app.v2_0.intf.APPlatformController;
import org.oscm.encrypter.AESEncrypter;
import org.oscm.operation.data.OperationResult;
import org.oscm.provisioning.data.InstanceInfo;
import org.oscm.provisioning.data.InstanceRequest;
import org.oscm.provisioning.data.InstanceResult;
import org.oscm.provisioning.intf.ProvisioningService;
import org.oscm.types.enumtypes.OperationStatus;
import org.oscm.vo.VOUserDetails;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;

import javax.ejb.Timer;
import javax.ejb.TimerService;
import javax.persistence.EntityManager;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"javax.management.*", "javax.script.*", "jdk.internal.reflect.*"})
@PrepareForTest({APPTimerServiceBean.class, APPlatformControllerFactory.class, ProvisioningStatus.class})
public class APPTimerServiceBeanTest {
  private static final String CONTROLLER_ID = "ess.aws";

  @InjectMocks
  private APPTimerServiceBean timerService;
  private Timer timer;

  private TimerService ts;
  private EntityManager em;
  private ServiceInstanceDAO instanceDAO;
  private APPConfigurationServiceBean configService;
  private ProductProvisioningServiceFactoryBean provFactoryBean;
  private BesDAO besDAOMock;
  private OperationDAO operationDAO;
  private APPCommunicationServiceBean mailService;
  private Logger logger;
  private APPTimerServiceBean timerBean;
  private ProvisioningService provisioningService;
  private ServiceInstance serviceInstance;
  private APPlatformController controller;
  private InstanceStatus instanceStatus;
  private ProvisioningStatus provisioningStatus;
  private OperationServiceBean operationServiceBean;
  private OperationResult operationResult;
  private Operation operation;
  private Setting setting;
  private InstanceParameter instanceParameter;
  private InstanceResult instanceResult;
  private InstanceInfo instanceInfo;
  private Properties properties;
  private ProvisioningSettings provisioningSettings;

  @BeforeClass
  public static void setUp() {
    AESEncrypter.generateKey();
  }

  @Before
  public void setup() throws Exception {
    timerService = PowerMockito.spy(new APPTimerServiceBean());
    timer = mock(Timer.class);
    em = mock(EntityManager.class);
    logger = mock(Logger.class);
    timerService.em = em;
    timerService.logger = logger;
    doNothing().when(em).persist(any(ServiceInstance.class));
    ts = mock(TimerService.class);
    mailService = Mockito.mock(APPCommunicationServiceBean.class);
    besDAOMock = mock(BesDAO.class);
    operationDAO = mock(OperationDAO.class);
    provFactoryBean = mock(ProductProvisioningServiceFactoryBean.class);
    configService = mock(APPConfigurationServiceBean.class);
    instanceDAO = mock(ServiceInstanceDAO.class);
    timerBean = mock(APPTimerServiceBean.class);
    provisioningService = mock(ProvisioningService.class);
    serviceInstance = mock(ServiceInstance.class);
    controller = mock(APPlatformController.class);
    instanceStatus = mock(InstanceStatus.class);
    provisioningStatus = mock(ProvisioningStatus.class);
    operationServiceBean = mock(OperationServiceBean.class);
    operationResult = mock(OperationResult.class);
    operation = mock(Operation.class);
    setting = mock(Setting.class);
    instanceParameter = mock(InstanceParameter.class);
    instanceResult = mock(InstanceResult.class);
    instanceInfo = mock(InstanceInfo.class);
    properties = mock(Properties.class);
    provisioningSettings = mock(ProvisioningSettings.class);
    PowerMockito.mockStatic(APPlatformControllerFactory.class);

    MockitoAnnotations.initMocks(this);

    timerService.instanceDAO = instanceDAO;
    timerService.configService = configService;
    timerService.mailService = mailService;
    timerService.besDAO = besDAOMock;
    timerService.operationDAO = operationDAO;
    timerService.timerService = ts;
    timerService.appTimerServiceBean = timerBean;
    timerService.provServFact = provFactoryBean;
    Collection<Timer> timers = new ArrayList<>();

    doReturn(getResult()).when(instanceDAO).getInstancesInWaitingState();
    doReturn(timers).when(timerService.timerService).getTimers();
    when(provFactoryBean.getInstance(any(ServiceInstance.class))).thenReturn(provisioningService);
  }

  private List<ServiceInstance> getResult() {
    List<ServiceInstance> result = new ArrayList<>();
    ServiceInstance serviceInstance = new ServiceInstance();
    serviceInstance.setInstanceId("aws-4df6b429-910b-48aa-88f5-d20a08f4d67f");
    serviceInstance.setControllerId("ess.aws");
    result.add(serviceInstance);
    return result;
  }

  @Test
  public void handleTimer_APPSuspended() {
    // given
    doReturn(Boolean.TRUE).when(configService).isAPPSuspend();

    // when
    timerService.handleTimer(timer);

    // then
    verify(timerBean, times(1)).cancelTimers();
  }

  @Test
  public void handleTimer_doHandleSystems_APPSuspended() throws Exception {
    // given
    doReturn(Boolean.TRUE).when(configService).isAPPSuspend();

    // when
    timerService.doHandleSystems(getResult(), ProvisioningStatus.getWaitingForCreation());

    // then
    verify(provFactoryBean, times(0)).getInstance(any(ServiceInstance.class));
    verify(timerService, times(0)).doHandleControllerProvisioning(any(ServiceInstance.class));
  }

  @Test
  public void handleBESNotificationException() {
    // given
    ServiceInstance serviceInstance = getResult().get(0);
    doReturn(Boolean.TRUE).when(besDAOMock).isCausedByConnectionException(any(Throwable.class));
    doNothing().when(timerService).suspendApp(any(ServiceInstance.class), anyString());

    // when
    timerService.handleBESNotificationException(
        serviceInstance,
        ProvisioningStatus.WAITING_FOR_SYSTEM_CREATION,
        null,
        new BESNotificationException("connection refused", new Throwable()));

    // then
    verify(timerService, times(1)).suspendApp(any(ServiceInstance.class), anyString());
  }

  @Test
  public void suspendApp() throws Exception {
    // given
    ServiceInstance serviceInstance = getResult().get(0);
    doNothing().when(timerService).sendMailToAppAdmin(anyString());
    doNothing().when(configService).setAPPSuspend(anyString());
    doReturn("APP_BASE_URL")
        .when(configService)
        .getProxyConfigurationSetting(eq(PlatformConfigurationKey.APP_BASE_URL));

    // when
    timerService.suspendApp(serviceInstance, "mail_bes_notification_error_app_admin");

    // then
    verify(timerService, times(1)).sendMailToAppAdmin(anyString());
  }

  @Test
  public void sendMailToAppAdmin() throws Exception {
    // given
    doNothing().when(timerService).sendActionMailToAppAdmin(anyString(), anyString());
    doReturn("APP_BASE_URL")
        .when(configService)
        .getProxyConfigurationSetting(eq(PlatformConfigurationKey.APP_BASE_URL));

    // when
    timerService.sendMailToAppAdmin("mail_bes_notification_error_app_admin");
    // then
    verify(timerService, times(1)).sendActionMailToAppAdmin(anyString(), anyString());
  }

  @Test
  public void sendActionMailToAppAdmin_Exception() throws Exception {
    // given
    VOUserDetails adminuser = new VOUserDetails();
    adminuser.setKey(1000L);
    Mockito.doThrow(new ConfigurationException("")).when(configService).getAPPAdministrator();

    // when
    timerService.sendActionMailToAppAdmin("mail_bes_notification_error_app_admin", "");
    // then
    verify(timerService, times(0))
        .getMailBodyForInfo(
            anyString(), anyString(), any(ServiceInstance.class), any(Throwable.class));
  }

  @Test
  public void sendActionMailToAppAdmin_linkNull() throws Exception {
    // given
    VOUserDetails adminuser = new VOUserDetails();
    adminuser.setKey(1000L);
    doReturn(adminuser).when(configService).getAPPAdministrator();

    doReturn("").when(timerService).getMailSubject(anyString(), anyString(), anyString());
    doReturn("")
        .when(timerService)
        .getMailBodyForInfo(
            anyString(), anyString(), any(ServiceInstance.class), any(Throwable.class));
    doNothing().when(mailService).sendMail(anyListOf(String.class), anyString(), anyString());

    // when
    timerService.sendActionMailToAppAdmin("mail_bes_notification_error_app_admin", null);
    // then
    verify(timerService, times(1))
        .getMailBodyForInfo(
            anyString(), anyString(), any(ServiceInstance.class), any(Throwable.class));
    verify(timerService, times(0))
        .getMailBodyForAction(
            anyString(),
            anyString(),
            any(ServiceInstance.class),
            any(Throwable.class),
            anyString(),
            anyBoolean());
  }

  @Test
  public void sendActionMailToAppAdmin_linkNotNull() throws Exception {
    // given
    VOUserDetails adminuser = new VOUserDetails();
    adminuser.setKey(1000L);
    doReturn(adminuser).when(configService).getAPPAdministrator();

    doReturn("").when(timerService).getMailSubject(anyString(), anyString(), anyString());
    doReturn("")
        .when(timerService)
        .getMailBodyForAction(
            anyString(),
            anyString(),
            any(ServiceInstance.class),
            any(Throwable.class),
            anyString(),
            anyBoolean());
    doNothing().when(mailService).sendMail(anyListOf(String.class), anyString(), anyString());

    // when
    timerService.sendActionMailToAppAdmin("mail_bes_notification_error_app_admin", "");
    // then
    verify(timerService, times(1))
        .getMailBodyForAction(
            anyString(),
            anyString(),
            any(ServiceInstance.class),
            any(Throwable.class),
            anyString(),
            anyBoolean());
    verify(timerService, times(0))
        .getMailBodyForInfo(
            anyString(), anyString(), any(ServiceInstance.class), any(Throwable.class));
  }

  @Test
  public void restart_BESNotAvailable() {
    // given
    doReturn(Boolean.FALSE).when(besDAOMock).isBESAvalible();

    // when
    boolean result = timerService.restart(false);

    assertEquals(Boolean.FALSE, result);
  }

  @Test
  public void restart() throws Exception {
    // given
    List<ServiceInstance> instances = prepareInstance("instance ID");
    doReturn(Boolean.TRUE).when(besDAOMock).isBESAvalible();
    doReturn(instances).when(instanceDAO).getInstancesSuspendedbyApp();

    doNothing()
        .when(timerService)
        .sendActionMail(
            anyBoolean(),
            any(ServiceInstance.class),
            anyString(),
            any(Throwable.class),
            anyString(),
            anyBoolean());

    doReturn("APP_BASE_URL")
        .when(configService)
        .getProxyConfigurationSetting(eq(PlatformConfigurationKey.APP_BASE_URL));
    // when
    boolean result = timerService.restart(false);

    assertEquals(Boolean.TRUE, result);
    verify(timerService, times(1))
        .sendActionMail(
            anyBoolean(),
            any(ServiceInstance.class),
            anyString(),
            any(Throwable.class),
            anyString(),
            anyBoolean());
  }

  @Test
  public void restart_NoMail() {
    // given
    List<ServiceInstance> instances = prepareInstance(null);
    doReturn(Boolean.TRUE).when(besDAOMock).isBESAvalible();
    doReturn(instances).when(instanceDAO).getInstancesSuspendedbyApp();

    // when
    boolean result = timerService.restart(false);

    assertEquals(Boolean.TRUE, result);
    assertEquals(Boolean.FALSE, instances.get(0).isSuspendedByApp());
    verify(timerService, times(0))
        .sendActionMail(
            anyBoolean(),
            any(ServiceInstance.class),
            anyString(),
            any(Throwable.class),
            anyString(),
            anyBoolean());
  }

  private List<ServiceInstance> prepareInstance(String instanceId) {
    List<ServiceInstance> instances = new ArrayList<>();
    ServiceInstance instance = new ServiceInstance();
    instance.setInstanceId(instanceId);
    instance.setControllerId(CONTROLLER_ID);
    instances.add(instance);
    return instances;
  }

  @Test
  public void testDoHandleInstanceProvisioning_provisioningCompleted() {
    // given
    ServiceInstance instance = new ServiceInstance();
    instance.setProvisioningStatus(ProvisioningStatus.WAITING_FOR_SYSTEM_CREATION);
    instance.setInstanceProvisioning(true);
    instance.setControllerReady(true);
    instance.setSubscriptionId("SubId");
    InstanceParameter instanceParameter = new InstanceParameter();
    instanceParameter.setParameterKey(InstanceParameter.PUBLIC_IP);
    instanceParameter.setParameterValue("0.0.0.0");
    instance.setInstanceParameters(Collections.singletonList(instanceParameter));
    List<ServiceInstance> instances = Collections.singletonList(instance);

    InstanceResult instanceResult = new InstanceResult();
    InstanceInfo instanceInfo = new InstanceInfo();
    instanceInfo.setAccessInfo("accessInfo");
    instanceInfo.setLoginPath("loginPath");
    instanceInfo.setBaseUrl("http://base.url:80");
    instanceResult.setInstance(instanceInfo);
    when(provisioningService.createInstance(any(InstanceRequest.class), any()))
        .thenReturn(instanceResult);

    // when
    timerService.doHandleSystems(instances, ProvisioningStatus.getWaitingForCreation());

    // then
    assertEquals(ProvisioningStatus.COMPLETED, instance.getProvisioningStatus());
    verify(em, times(2)).persist(instance);
  }

  @Test
  public void testDoHandleInstanceProvisioning_provisioningAborted() throws Exception {
    // given
    ServiceInstance instance = new ServiceInstance();
    instance.setProvisioningStatus(ProvisioningStatus.WAITING_FOR_SYSTEM_CREATION);
    instance.setInstanceProvisioning(true);
    instance.setControllerReady(true);
    instance.setSubscriptionId("#SubId");

    List<ServiceInstance> instances = Collections.singletonList(instance);

    InstanceResult instanceResult = new InstanceResult();
    instanceResult.setRc(1);
    InstanceInfo instanceInfo = new InstanceInfo();
    instanceInfo.setAccessInfo("accessInfo");
    instanceInfo.setLoginPath("loginPath");
    instanceResult.setInstance(instanceInfo);
    when(provisioningService.createInstance(any(InstanceRequest.class), any()))
        .thenReturn(instanceResult);

    // when
    timerService.doHandleSystems(instances, ProvisioningStatus.getWaitingForCreation());

    // then
    assertEquals(ProvisioningStatus.COMPLETED, instance.getProvisioningStatus());
    verify(em, times(2)).persist(instance);
    verify(timerService, times(1))
        .notifyOnProvisioningAbortion(
            any(ServiceInstance.class), any(InstanceResult.class), any(APPlatformException.class));
  }

  @Test
  public void testDoHandleInstanceProvisioning_pingFailed() {
    // given
    ServiceInstance instance = new ServiceInstance();
    instance.setProvisioningStatus(ProvisioningStatus.WAITING_FOR_SYSTEM_CREATION);
    instance.setInstanceProvisioning(true);
    List<ServiceInstance> instances = Collections.singletonList(instance);

    when(provisioningService.sendPing(anyString())).thenThrow(new RuntimeException());

    // when
    timerService.doHandleSystems(instances, ProvisioningStatus.getWaitingForCreation());

    // then
    verify(em, times(0)).persist(instance);
    assertEquals(ProvisioningStatus.WAITING_FOR_SYSTEM_CREATION, instance.getProvisioningStatus());
  }

  @Test
  public void testInitTimers() {
    // when
    timerService.initTimers();

    // then
    verify(timerBean, times(1)).initTimers_internal();
    verify(logger, times(1)).info("Timer initialization finished");
  }

  @Test
  public void testInitTimers_internal() throws Exception {
    // given
    when(configService.getProxyConfigurationSetting(PlatformConfigurationKey.APP_TIMER_INTERVAL))
        .thenReturn("60");

    // when
    timerService.initTimers_internal();

    // then
    verify(ts, times(1)).createTimer(anyLong(), anyLong(), anyString());
  }

  @Test
  public void testInitTimers_internal_ConfigurationException() throws Exception {
    // given
    when(configService.getProxyConfigurationSetting(PlatformConfigurationKey.APP_TIMER_INTERVAL))
        .thenThrow(new ConfigurationException("msg"));

    // when
    timerService.initTimers_internal();

    // then
    verify(ts, times(1)).createTimer(anyLong(), anyLong(), anyString());
  }

  @Test
  public void testCancelTimers() {
    // given
    Timer timer = mock(Timer.class);
    String timerInfo = "d432dac0-5f81-11e4-9803-0800200c9a66";
    when(timer.getInfo()).thenReturn(timerInfo);
    when(ts.getTimers()).thenReturn(Collections.singletonList(timer));

    // when
    timerService.cancelTimers();

    // then
    verify(timer, times(1)).cancel();
  }

  @Test
  public void testHandleTimer() {
    // given
    Timer timer = mock(Timer.class);

    // when
    timerService.handleTimer(timer);

    // then
    verify(timerService, times(7))
        .doHandleSystems(anyListOf(ServiceInstance.class), any(EnumSet.class));
  }

  @Test
  public void testHandleException_instanceWaitingForCreation() {
    // given
    ServiceInstance instance = new ServiceInstance();
    instance.setProvisioningStatus(ProvisioningStatus.WAITING_FOR_SYSTEM_CREATION);
    instance.setSubscriptionId("SubId");
    APPlatformException exception = new APPlatformException("MSG");

    // when
    timerService.handleException(
        instance, ProvisioningStatus.WAITING_FOR_SYSTEM_CREATION, exception);

    // then
    verify(em, times(1)).remove(any(ServiceInstance.class));
  }

  @Test
  public void testHandleException_instanceWaitingForModification() throws Exception {
    // given
    ServiceInstance instance = mock(ServiceInstance.class);
    when(instance.getProvisioningStatus())
        .thenReturn(ProvisioningStatus.WAITING_FOR_SYSTEM_MODIFICATION);
    when(instance.getSubscriptionId()).thenReturn("SubId");
    doNothing().when(instance).rollbackServiceInstance(any());
    doNothing()
        .when(timerService)
        .sendActionMail(
            anyBoolean(),
            any(ServiceInstance.class),
            anyString(),
            any(),
            anyString(),
            anyBoolean());
    APPlatformException exception = new APPlatformException("MSG");

    // when
    timerService.handleException(
        instance, ProvisioningStatus.WAITING_FOR_SYSTEM_MODIFICATION, exception);

    // then
    verify(em, times(1)).persist(any(ServiceInstance.class));
  }

  @Test
  public void testHandleException_instanceWaitingForActivation() {
    // given
    ServiceInstance instance = new ServiceInstance();
    instance.setProvisioningStatus(ProvisioningStatus.WAITING_FOR_SYSTEM_ACTIVATION);
    instance.setSubscriptionId("SubId");
    APPlatformException exception = new APPlatformException("MSG");
    doNothing()
        .when(timerService)
        .sendInfoMail(anyBoolean(), any(ServiceInstance.class), anyString(), any());

    // when
    timerService.handleException(
        instance, ProvisioningStatus.WAITING_FOR_SYSTEM_ACTIVATION, exception);

    // then
    verify(em, times(1)).persist(any(ServiceInstance.class));
  }

  @Test
  public void testHandleException_instanceWaitingForOperation() {
    // given
    ServiceInstance instance = new ServiceInstance();
    instance.setProvisioningStatus(ProvisioningStatus.WAITING_FOR_SYSTEM_OPERATION);
    instance.setSubscriptionId("SubId");
    APPlatformException exception = new APPlatformException("MSG");
    doNothing()
        .when(timerService)
        .sendInfoMail(anyBoolean(), any(ServiceInstance.class), anyString(), any());
    when(operationDAO.getOperationByInstanceId(anyString())).thenReturn(new Operation());

    // when
    timerService.handleException(
        instance, ProvisioningStatus.WAITING_FOR_SYSTEM_OPERATION, exception);

    // then
    verify(em, times(1)).persist(any(ServiceInstance.class));
    verify(em, times(1)).remove(any(Operation.class));
  }

  @Test
  public void testHandleException_instanceWaitingForUserAction() {
    // given
    ServiceInstance instance = new ServiceInstance();
    instance.setProvisioningStatus(ProvisioningStatus.WAITING_FOR_USER_CREATION);
    instance.setSubscriptionId("SubId");
    APPlatformException exception = new APPlatformException("MSG");
    doNothing()
        .when(timerService)
        .sendInfoMail(anyBoolean(), any(ServiceInstance.class), anyString(), any());

    // when
    timerService.handleException(instance, ProvisioningStatus.WAITING_FOR_USER_CREATION, exception);

    // then
    verify(em, times(1)).persist(any(ServiceInstance.class));
  }

  @Test
  public void testHandleSuspendException() throws Exception {
    // given
    ServiceInstance instance = new ServiceInstance();
    instance.setInstanceId("instanceId");
    instance.setControllerId("controllerId");
    SuspendException exception = new SuspendException("msg");

    VOUserDetails administrator = new VOUserDetails();
    administrator.setLocale("en");
    administrator.setEMail("email@email.com");
    when(configService.getAPPAdministrator()).thenReturn(administrator);

    // when
    timerService.handleSuspendException(
        instance, ProvisioningStatus.WAITING_FOR_SYSTEM_CREATION, exception);

    // then
    verify(timerService, times(1))
        .sendActionMail(
            anyBoolean(),
            any(ServiceInstance.class),
            anyString(),
            any(Exception.class),
            anyString(),
            anyBoolean());
  }

  @Test
  public void testSendActionMail() throws Exception {
    // given
    ServiceInstance instance = new ServiceInstance();
    String msgKey = "msgKey";
    String actionLink = "actionLink";

    VOUserDetails administrator = new VOUserDetails();
    administrator.setLocale("en");
    administrator.setEMail("email@email.com");

    when(configService.getAPPAdministrator()).thenReturn(administrator);

    // when
    timerService.sendActionMail(false, instance, msgKey, null, actionLink, true);

    // then
    verify(mailService, times(1)).sendMail(anyList(), anyString(), anyString());
  }

  @Test
  public void testSendInfoMail() throws Exception {
    // given
    ServiceInstance instance = new ServiceInstance();
    String msgKey = "msgKey";

    VOUserDetails techManager = new VOUserDetails();
    techManager.setLocale("en");
    techManager.setEMail("email@email.com");

    when(besDAOMock.getBESTechnologyManagers(any(ServiceInstance.class)))
        .thenReturn(Collections.singletonList(techManager));

    // when
    timerService.sendInfoMail(true, instance, msgKey, null);

    // then
    verify(mailService, times(1)).sendMail(anyList(), anyString(), anyString());
  }

  @Test
  public void testDoHandleControllerProvisioning_instanceReady_initTimers() throws Exception {

    when(serviceInstance.getProvisioningStatus()).thenReturn(provisioningStatus);
    when(APPlatformControllerFactory.getInstance(anyString())).thenReturn(controller);
    when(controller.getInstanceStatus(anyString(), any())).thenReturn(instanceStatus);
    when(instanceStatus.getChangedParameters()).thenReturn(null);
    when(instanceStatus.isReady()).thenReturn(false);
    when(provisioningStatus.isWaitingForCreation()).thenReturn(true);
    when(serviceInstance.getProvisioningStatus().isCompleted()).thenReturn(true);
    when(operationServiceBean.executeServiceOperationFromQueue(anyString())).thenReturn(operationResult);
    when(instanceDAO.getInstanceById(anyString())).thenReturn(serviceInstance);
    when(serviceInstance.getProvisioningStatus().isWaitingForOperation()).thenReturn(true);

    timerService.doHandleControllerProvisioning(serviceInstance);

    verify(timerService, times(1)).initTimers();
  }

  @Test
  public void testDoHandleControllerProvisioning_instanceReady_waitingForOperation() throws Exception {

    when(serviceInstance.getProvisioningStatus()).thenReturn(provisioningStatus);
    when(APPlatformControllerFactory.getInstance(anyString())).thenReturn(controller);
    when(controller.getInstanceStatus(anyString(), any())).thenReturn(instanceStatus);
    when(instanceStatus.getChangedParameters()).thenReturn(null);
    when(instanceStatus.isReady()).thenReturn(false);
    when(provisioningStatus.isWaitingForCreation()).thenReturn(false);
    when(provisioningStatus.isWaitingForOperation()).thenReturn(true);
    when(operationDAO.getOperationByInstanceId(anyString())).thenReturn(operation);

    timerService.doHandleControllerProvisioning(serviceInstance);

    verify(besDAOMock, times(1)).notifyAsyncOperationStatus(eq(serviceInstance), anyString(), eq(OperationStatus.RUNNING), anyList());
  }

  @Test
  public void testDoHandleControllerProvisioning_instanceNotReady_waitingForDeletion() throws Exception {

    when(serviceInstance.getProvisioningStatus()).thenReturn(provisioningStatus);
    when(APPlatformControllerFactory.getInstance(anyString())).thenReturn(controller);
    when(controller.getInstanceStatus(anyString(), any())).thenReturn(instanceStatus);
    when(instanceStatus.getChangedParameters()).thenReturn(null);
    when(instanceStatus.isReady()).thenReturn(true);
    when(provisioningStatus.isWaitingForDeletion()).thenReturn(true);

    timerService.doHandleControllerProvisioning(serviceInstance);

    verify(em, times(1)).remove(serviceInstance);
  }

  @Test
  public void testDoHandleControllerProvisioning_provisioningRequest_mapContainsPublicIP() throws Exception {

    HashMap<String, Setting> parameters = new HashMap<>();
    parameters.put("APP_PUBLIC_IP", setting);
    when(serviceInstance.getProvisioningStatus()).thenReturn(provisioningStatus);
    when(APPlatformControllerFactory.getInstance(anyString())).thenReturn(controller);
    when(controller.getInstanceStatus(anyString(), any())).thenReturn(instanceStatus);
    when(instanceStatus.getChangedParameters()).thenReturn(parameters);
    when(instanceStatus.isReady()).thenReturn(true);
    when(instanceStatus.isInstanceProvisioningRequested()).thenReturn(true);

    timerService.doHandleControllerProvisioning(serviceInstance);

    verify(instanceStatus, times(1)).setIsReady(false);
    verify(serviceInstance, times(1)).setServiceLoginPath(anyString());
    verify(em, times(2)).persist(serviceInstance);
  }

  @Test
  public void testDoHandleControllerProvisioning_provisioningRequest_mapWithoutPublicIP() throws Exception {

    when(serviceInstance.getProvisioningStatus()).thenReturn(provisioningStatus);
    when(APPlatformControllerFactory.getInstance(anyString())).thenReturn(controller);
    when(controller.getInstanceStatus(anyString(), any())).thenReturn(instanceStatus);
    when(instanceStatus.getChangedParameters()).thenReturn(null);
    when(instanceStatus.isReady()).thenReturn(true);
    when(instanceStatus.isInstanceProvisioningRequested()).thenReturn(true);
    when(serviceInstance.getParameterForKey(anyString())).thenReturn(instanceParameter);

    timerService.doHandleControllerProvisioning(serviceInstance);

    verify(serviceInstance, times(1)).getParameterForKey(InstanceParameter.PUBLIC_IP);
    verify(instanceStatus, times(1)).setIsReady(false);
    verify(serviceInstance, times(1)).setServiceLoginPath(anyString());
    verify(em, times(2)).persist(serviceInstance);
  }

  @Test
  public void testDoHandleControllerProvisioning_notProvisioningRequest() throws Exception {

    when(serviceInstance.getProvisioningStatus()).thenReturn(provisioningStatus);
    when(APPlatformControllerFactory.getInstance(anyString())).thenReturn(controller);
    when(controller.getInstanceStatus(anyString(), any())).thenReturn(instanceStatus);
    when(instanceStatus.getChangedParameters()).thenReturn(null);
    when(instanceStatus.isReady()).thenReturn(true);
    when(instanceStatus.isInstanceProvisioningRequested()).thenReturn(false);
    PowerMockito.whenNew(InstanceResult.class).withNoArguments().thenReturn(instanceResult);
    PowerMockito.whenNew(InstanceInfo.class).withNoArguments().thenReturn(instanceInfo);

    timerService.doHandleControllerProvisioning(serviceInstance);

    verify(serviceInstance, times(1)).setProvisioningStatus(ProvisioningStatus.COMPLETED);
    verify(serviceInstance, times(1)).setServiceLoginPath(anyString());
    verify(em, times(1)).persist(serviceInstance);
  }

  @Test(expected = NullPointerException.class)
  public void testDoHandleControllerProvisioning_exception() {

    timerService.doHandleControllerProvisioning(serviceInstance);

  }

  @Test
  public void testDoHandleControllerProvisioning_catchControllerException() throws ControllerLookupException {

    String errorMsg = "App controller not found";
    when(serviceInstance.getProvisioningStatus()).thenReturn(provisioningStatus);
    when(APPlatformControllerFactory.getInstance(anyString())).thenThrow(new ControllerLookupException(errorMsg));

    timerService.doHandleControllerProvisioning(serviceInstance);

    verify(logger, times(1)).error(errorMsg);
  }

  @Test
  public void testDoHandleControllerProvisioning_catchBESNotificationException() throws APPlatformException, BESNotificationException {

    String errorMsg = "Instance not alive";
    when(serviceInstance.getProvisioningStatus()).thenReturn(provisioningStatus);
    when(APPlatformControllerFactory.getInstance(anyString())).thenReturn(controller);
    when(controller.getInstanceStatus(anyString(), any())).thenThrow(new InstanceNotAliveException(errorMsg));
    doNothing().when(timerService).handleInstanceNotAliveException(any(), any(), any());
    when(operationDAO.getOperationByInstanceId(anyString())).thenReturn(operation);

    timerService.doHandleControllerProvisioning(serviceInstance);

    verify(besDAOMock, times(1)).notifyInstanceStatusOfAsyncOperation(any());
  }

  @Test
  public void testDoHandleControllerProvisioning_catchSuspendException() throws APPlatformException, BESNotificationException {

    String errorMsg = "Instance suspended";
    when(serviceInstance.getProvisioningStatus()).thenReturn(provisioningStatus);
    when(APPlatformControllerFactory.getInstance(anyString())).thenReturn(controller);
    when(controller.getInstanceStatus(anyString(), any())).thenThrow(new SuspendException(errorMsg));
    doNothing().when(timerService).handleSuspendException(any(), any(), any());
    when(operationDAO.getOperationByInstanceId(anyString())).thenReturn(operation);

    timerService.doHandleControllerProvisioning(serviceInstance);

    verify(besDAOMock, times(1)).notifyAsyncOperationStatus(any(), anyString(), any(), anyList());
  }

  @Test
  public void testHandleInstanceNotAliveException() throws Exception {

    HashMap<String, Setting> parameters = new HashMap<>();
    parameters.put("APP_PUBLIC_IP", setting);
    String errorMsg = "Instance not alive";
    PowerMockito.doNothing().when(timerService, "updateParameterMapSafe", serviceInstance, parameters);
    PowerMockito.doNothing().when(timerService).sendInfoMail(eq(true), eq(serviceInstance), anyString(), any());

    timerService.handleInstanceNotAliveException(serviceInstance, parameters, new InstanceNotAliveException(errorMsg));

    verify(timerService, times(1)).sendInfoMail(eq(true), eq(serviceInstance), anyString(), any());
  }

  @Test
  public void testRaiseEvent() throws Exception {

    when(instanceDAO.getInstanceById(anyString(), anyString())).thenReturn(serviceInstance);
    when(configService.getProvisioningSettings(any(), any(), anyBoolean())).thenReturn(provisioningSettings);
    when(APPlatformControllerFactory.getInstance(anyString())).thenReturn(controller);
    when(controller.notifyInstance(anyString(), any(), any())).thenReturn(instanceStatus);
    when(instanceStatus.getRunWithTimer()).thenReturn(true);
    when(properties.getProperty(anyString(), anyString())).thenReturn("yes");

    timerService.raiseEvent("ControllerId", "InstanceId", properties);

    verify(timerService, times(1)).initTimers();
    verify(instanceStatus, times(1)).setRunWithTimer(true);
  }
}
