/**
 * ***************************************************************************** Copyright FUJITSU
 * LIMITED 2018 *****************************************************************************
 */
package org.oscm.app.dao;

import org.junit.Before;
import org.junit.Test;
import org.oscm.app.domain.Operation;
import org.oscm.app.domain.ServiceInstance;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.Properties;

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

public class OperationDAOTest {

  private static OperationDAO opDAO = spy(new OperationDAO());
  private static EntityManager em = mock(EntityManager.class);

  private Query query = mock(Query.class);

  @Before
  public void setup() {
    opDAO.em = em;
    doNothing().when(em).persist(anyObject());
    doReturn(query).when(em).createNamedQuery(anyString());
  }

  @Test
  public void addOperationForQueue() {
    // given
    ServiceInstance instance = new ServiceInstance();
    Properties opParameters = new Properties();
    String transactionId = "tId";

    // when
    Operation operation = opDAO.addOperationForQueue(instance, opParameters, transactionId);

    // then
    assertEquals(instance, operation.getServiceInstance());
    assertEquals(opParameters, operation.getParametersAsProperties());
    assertEquals(transactionId, operation.getTransactionId());
    assertTrue(operation.isForQueue());
    verify(em).persist(operation);
  }

  @Test
  public void addOperation() {
    // given
    ServiceInstance instance = new ServiceInstance();
    Properties opParameters = new Properties();
    String transactionId = "tId";

    // when
    Operation operation = opDAO.addOperation(instance, opParameters, transactionId);

    // then
    assertEquals(instance, operation.getServiceInstance());
    assertEquals(opParameters, operation.getParametersAsProperties());
    assertEquals(transactionId, operation.getTransactionId());
    assertFalse(operation.isForQueue());
    verify(em).persist(operation);
  }

  @Test
  public void testGetOperationFromQueue() {
    // given
    String instanceId = "instanceId";
    Operation operation = new Operation();
    when(query.getSingleResult()).thenReturn(operation);

    // when
    Operation operationFromQueue = opDAO.getOperationFromQueue(instanceId);

    // then
    assertEquals(operation, operationFromQueue);
  }

  @Test
  public void testGetOperationByInstanceId() {
    // given
    String instanceId = "instanceId";
    Operation operation = new Operation();
    when(query.getSingleResult()).thenReturn(operation);

    // when
    Operation operationByInstanceId = opDAO.getOperationByInstanceId(instanceId);

    // then
    assertEquals(operation, operationByInstanceId);
  }

  @Test
  public void tesRemoveOperation() {
    // when
    opDAO.removeOperation(1000);

    // then
    verify(query, times(1)).executeUpdate();
  }
}
