/**
 * *****************************************************************************
 *
 * <p>Copyright FUJITSU LIMITED 2020
 *
 * <p>Creation Date: 28.09.2020
 *
 * <p>*****************************************************************************
 */
package org.oscm.app.approval.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.oscm.app.v2_0.data.Setting;
import org.oscm.app.v2_0.exceptions.APPlatformException;
import org.oscm.app.v2_0.intf.APPlatformService;

/** @author goebel */
public class ApprovalInstanceAccessTest {
  ApprovalInstanceAccess access;
  Map<String, Setting> ps = new HashMap<String, Setting>();

  @Before
  public void setup() throws APPlatformException {
    APPlatformService s = mock(APPlatformService.class);
    access = spy(new ApprovalInstanceAccess());

    ApprovalInstanceAccess.ClientData data = access.new ClientData("3fe2a1");
    access.platformService = s;

    Collection<String> result = Arrays.asList(new String[] {"instance_12345678"});
    doReturn(result).when(access.platformService).listServiceInstances(any(), any(), any());

    Answer<Void> answer =
        new Answer<Void>() {
          @SuppressWarnings("unchecked")
          @Override
          public Void answer(InvocationOnMock invocation) throws Throwable {
            Object[] arguments = invocation.getArguments();
            if (arguments != null && arguments.length > 1 && arguments[1] != null) {
              ((Predicate<Map<String, Setting>>) arguments[1]).test(ps);
            }
            return null;
          }
        };

    doAnswer(answer).when(access.platformService).listServiceInstances(any(), any(), any());
  }

  @Test
  public void getCustomerSettings() throws APPlatformException {
    // given
    givenCustomerSettings("3fe2a1");

    // when
    ApprovalInstanceAccess.ClientData data = access.getCustomerSettings("3fe2a1");

    // then
    assertNotNull(data);
    assertEquals("11001", data.getOrgAdminUserKey().getValue());
    assertEquals("orgAdmin", data.getOrgAdminUserId().getValue());
    assertEquals("_crypt:secret", data.getOrgAdminUserPwd().getValue());
  }

  Map<String, Setting> givenCustomerSettings(String id) {
    ps.put("APPROVER_ORG_ID_" + id, new Setting("APPROVER_ORG_ID_" + id, "2e3f1a"));
    ps.put("USERID_" + id, new Setting("USERID_" + id, "orgAdmin"));
    ps.put("USERKEY_" + id, new Setting("USERKEY_" + id, "11001"));
    ps.put("USERPWD_" + id, new Setting("USERPWD_" + id, "_crypt:secret", true));
    return ps;
  }
}
