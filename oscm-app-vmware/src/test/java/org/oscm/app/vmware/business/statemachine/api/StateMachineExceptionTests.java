package org.oscm.app.vmware.business.statemachine.api;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class StateMachineExceptionTests {

  StateMachineException stateMachineException;

  @Test
  public void testStateMachineExceptionConstructor() {
    // when
    stateMachineException = new StateMachineException("Exception message", new Exception(), "Instance ID", "State", "Method name");
    // then
    assertEquals("Instance ID", stateMachineException.getInstanceId());
    assertEquals("State", stateMachineException.getClazz());
    assertEquals("Method name", stateMachineException.getMethod());
  }
}
