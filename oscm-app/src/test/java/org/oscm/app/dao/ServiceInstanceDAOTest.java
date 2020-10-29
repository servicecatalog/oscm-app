/**
 * ***************************************************************************** Copyright FUJITSU
 * LIMITED 2018 *****************************************************************************
 */
package org.oscm.app.dao;

import org.junit.Before;
import org.junit.Test;
import org.oscm.app.business.exceptions.ServiceInstanceNotFoundException;
import org.oscm.app.domain.InstanceParameter;
import org.oscm.app.domain.ServiceInstance;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

public class ServiceInstanceDAOTest {

  private static ServiceInstanceDAO siDAO;
  private EntityManager em;
  private Query query;

  @Before
  public void setup() {
    siDAO = spy(new ServiceInstanceDAO());
    em = mock(EntityManager.class);
    siDAO.em = em;
    query = mock(Query.class);
    doReturn(query).when(em).createNamedQuery(anyString());
  }

  @Test(expected = ServiceInstanceNotFoundException.class)
  public void getInstance_null() throws Exception {
    siDAO.getInstance(null, null, null);
  }

  @Test(expected = ServiceInstanceNotFoundException.class)
  public void getInstanceById_null() throws Exception {
    siDAO.getInstanceById(null);
  }

  @Test(expected = ServiceInstanceNotFoundException.class)
  public void getInstanceBySubscriptionAndOrganization_null() throws Exception {
    siDAO.getInstanceBySubscriptionAndOrganization(null, null);
  }

  @Test
  public void getInstance_instanceId_null() throws Exception {
    // when
    siDAO.getInstance(null, "subId", "orgId");

    // then
    verify(siDAO, times(1)).getInstanceBySubscriptionAndOrganization("subId", "orgId");
  }

  @Test
  public void getInstance_instanceId_notNull() throws Exception {
    // when
    siDAO.getInstance("instanceId", "subId", "orgId");

    // then
    verify(siDAO, times(1)).getInstanceById("instanceId");
  }

  @Test
  public void prepareDeletionInstance() throws Exception {
    // given
    ServiceInstance instance = new ServiceInstance();
    instance.setSubscriptionId("subscriptionId");
    doReturn(instance).when(siDAO.em).find(eq(ServiceInstance.class), anyLong());
    // when
    siDAO.markAsDeleted(instance);
    // given
    assertEquals(Boolean.TRUE, instance.getSubscriptionId().contains("#"));
    assertEquals(Boolean.FALSE, instance.isLocked());
  }

  @Test
  public void testDeleteInstance() throws Exception {
    // given
    ServiceInstance serviceInstance = new ServiceInstance();
    doReturn(serviceInstance).when(siDAO).find(serviceInstance);

    // when
    siDAO.deleteInstance(serviceInstance);

    // then
    verify(em, times(1)).remove(serviceInstance);
    verify(em, times(1)).flush();
  }

  @Test
  public void testGetInstanceById() throws Exception {
    // given
    String controllerId = "ess.cid";
    String instanceId = "instanceId";
    ServiceInstance serviceInstance = new ServiceInstance();
    serviceInstance.setControllerId(controllerId);
    serviceInstance.setInstanceId(instanceId);

    when(query.getSingleResult()).thenReturn(serviceInstance);

    // when
    ServiceInstance instance = siDAO.getInstanceById(controllerId, instanceId);

    // then
    assertEquals(serviceInstance, instance);
  }

  @Test
  public void testGetLockedInstanceForController() {
    // given
    String controllerId = "ess.cid";
    ServiceInstance serviceInstance = new ServiceInstance();
    serviceInstance.setControllerId(controllerId);

    when(query.getSingleResult()).thenReturn(serviceInstance);

    // when
    ServiceInstance instance = siDAO.getLockedInstanceForController(controllerId);

    // then
    assertEquals(serviceInstance, instance);
  }

  @Test
  public void testRestoreInstance() throws Exception {
    // given
    String subId = "#subId";
    ServiceInstance serviceInstance = new ServiceInstance();
    serviceInstance.setSubscriptionId(subId);

    doReturn(serviceInstance).when(siDAO).find(serviceInstance);

    // when
    siDAO.restoreInstance(serviceInstance);

    // then
    assertFalse(serviceInstance.getSubscriptionId().contains("#"));
    verify(em, times(1)).flush();
  }

  @Test
  public void testResumeInstance() throws Exception {
    // given
    ServiceInstance serviceInstance = new ServiceInstance();
    doReturn(serviceInstance).when(siDAO).find(serviceInstance);

    // when
    ServiceInstance instance = siDAO.resumeInstance(serviceInstance);

    // then
    assertTrue(instance.getRunWithTimer());
    verify(em, times(1)).flush();
  }

  @Test
  public void testAbortPendingInstance() throws Exception {
    // given
    ServiceInstance serviceInstance = new ServiceInstance();
    doReturn(serviceInstance).when(siDAO).find(serviceInstance);

    // when
    ServiceInstance instance = siDAO.abortPendingInstance(serviceInstance);

    // then
    assertTrue(instance.getRunWithTimer());
    verify(em, times(1)).flush();
  }

  @Test
  public void testSuspendInstance() throws Exception {
    // given
    ServiceInstance serviceInstance = new ServiceInstance();
    doReturn(serviceInstance).when(siDAO).find(serviceInstance);

    // when
    siDAO.suspendInstance(serviceInstance);

    // then
    assertFalse(serviceInstance.getRunWithTimer());
    assertFalse(serviceInstance.isLocked());
    verify(em, times(1)).flush();
  }

  @Test
  public void testUnlockInstance() throws Exception {
    // given
    ServiceInstance serviceInstance = new ServiceInstance();
    doReturn(serviceInstance).when(siDAO).find(serviceInstance);

    // when
    siDAO.unlockInstance(serviceInstance);

    // then
    assertFalse(serviceInstance.isLocked());
    verify(em, times(1)).flush();
  }

  @Test
  public void testGetInstancesForController() {
    // given
    ServiceInstance serviceInstance = new ServiceInstance();
    List<ServiceInstance> instances = Collections.singletonList(serviceInstance);
    when(query.getResultList()).thenReturn(instances);

    // when
    List<ServiceInstance> instancesList = siDAO.getInstancesForController("cid");

    // then
    assertEquals(instances, instancesList);
  }

  @Test
  public void testGetInstanceParameters() {
    // given
    ServiceInstance serviceInstance = new ServiceInstance();
    List<InstanceParameter> instanceParameters = Collections.singletonList(new InstanceParameter());
    when(query.getResultList()).thenReturn(instanceParameters);

    // when
    List<InstanceParameter> parameters = siDAO.getInstanceParameters(serviceInstance);

    // then
    assertEquals(instanceParameters, parameters);
  }

  @Test
  public void testGetInstancesInWaitingState() {
    // given
    ServiceInstance serviceInstance = new ServiceInstance();
    List<ServiceInstance> instances = Collections.singletonList(serviceInstance);
    when(query.getResultList()).thenReturn(instances);

    // when
    List<ServiceInstance> instancesList = siDAO.getInstancesInWaitingState();

    // then
    assertEquals(instances, instancesList);
  }

  @Test
  public void testExists() {
    // give
    ServiceInstance serviceInstance = new ServiceInstance();
    when(query.getSingleResult()).thenReturn(serviceInstance);

    // when
    boolean exists = siDAO.exists("instanceId");

    // then
    assertTrue(exists);
  }

  @Test
  public void testExistsWithControllerId() {
    // give
    ServiceInstance serviceInstance = new ServiceInstance();
    when(query.getSingleResult()).thenReturn(serviceInstance);

    // when
    boolean exists = siDAO.exists("cid", "instanceId");

    // then
    assertTrue(exists);
  }

  @Test
  public void testGetInstances() {
    // given
    ServiceInstance serviceInstance = new ServiceInstance();
    List<ServiceInstance> instances = Collections.singletonList(serviceInstance);
    when(query.getResultList()).thenReturn(instances);

    // when
    List<ServiceInstance> instancesList = siDAO.getInstances();

    // then
    assertEquals(instances, instancesList);
  }

  @Test
  public void testGetInstancesSuspendedByApp() {
    // given
    ServiceInstance serviceInstance = new ServiceInstance();
    List<ServiceInstance> instances = Collections.singletonList(serviceInstance);
    when(query.getResultList()).thenReturn(instances);

    // when
    List<ServiceInstance> instancesList = siDAO.getInstancesSuspendedbyApp();

    // then
    assertEquals(instances, instancesList);
  }

  @Test
  public void testUpdateParam() {
    // given
    ServiceInstance serviceInstance = new ServiceInstance();
    InstanceParameter parameter = new InstanceParameter();
    parameter.setParameterKey("KEY");
    serviceInstance.setInstanceParameters(Collections.singletonList(parameter));
    when(query.getSingleResult()).thenReturn(serviceInstance);

    // when
    siDAO.updateParam(serviceInstance, "VALUE", "KEY");

    // then
    verify(em, times(1)).flush();
  }
}
