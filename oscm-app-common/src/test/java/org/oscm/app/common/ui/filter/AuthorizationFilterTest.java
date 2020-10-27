/**
 * *****************************************************************************
 *
 * <p>COPYRIGHT (C) FUJITSU LIMITED - ALL RIGHTS RESERVED.
 *
 * <p>Creation Date: 26.05.2014
 *
 * <p>*****************************************************************************
 */
package org.oscm.app.common.ui.filter;

import org.apache.commons.codec.binary.Base64;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.oscm.app.v2_0.APPlatformServiceFactory;
import org.oscm.app.v2_0.data.PasswordAuthentication;
import org.oscm.app.v2_0.data.User;
import org.oscm.app.v2_0.intf.APPlatformService;
import org.oscm.app.v2_0.intf.ControllerAccess;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

/** Unit test of authorization filter */
@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.management.*")
@PrepareForTest(APPlatformServiceFactory.class)
public class AuthorizationFilterTest {

  @Mock private APPlatformService platformService;
  @Mock private FilterChain chain;
  @Mock private FilterConfig config;
  @Mock private HttpServletRequest req;
  @Mock private HttpServletResponse resp;
  @Mock private HttpSession session;
  @Mock private ControllerAccess controllerAccess;

  private AuthorizationFilter filter;
  private StringWriter responseOut = new StringWriter();
  private boolean exception = false;

  @Before
  public void setupClass() throws Exception {
    PowerMockito.mockStatic(APPlatformServiceFactory.class);
    when(APPlatformServiceFactory.getInstance()).thenReturn(platformService);

    when(resp.getWriter()).thenReturn(new PrintWriter(responseOut));
    when(req.getSession()).thenReturn(session);
    when(req.getLocale()).thenReturn(new Locale("en"));
    when(config.getInitParameter("exclude-url-pattern"))
        .thenReturn(
            "(.*/a4j/.*|.*/img/.*|.*/css/.*|.*/fonts/.*|.*/scripts/.*|.*/faq/.*|.*/org.richfaces.resources|.*/javax.faces.resource/.*|^/public/.*)");

    controllerAccess = mock(ControllerAccess.class);
    when(controllerAccess.getControllerId()).thenReturn("ess.common");

    filter = new AuthorizationFilter();
    filter.setControllerAccess(controllerAccess);
    filter.init(config);
  }

  @Test
  public void testAuthenticateLoggedIn() throws Exception {
    when(session.getAttribute(Matchers.eq("loggedInUserId"))).thenReturn("user1");

    // And o!
    filter.doFilter(req, resp, chain);

    // Check whether request has been forwarded
    verify(chain).doFilter(Matchers.eq(req), Matchers.eq(resp));
  }

  @Test
  public void testAuthenticateWrongInput() throws Exception {
    ServletRequest reqWrong = mock(ServletRequest.class);
    filter.doFilter(reqWrong, resp, chain);
    assertEquals("401", responseOut.toString());
    responseOut = new StringWriter();

    ServletResponse respWrong = mock(ServletResponse.class);
    when(respWrong.getWriter()).thenReturn(new PrintWriter(responseOut));
    filter.doFilter(req, respWrong, chain);
    assertEquals("401", responseOut.toString());
  }

  @Test
  public void testAuthenticateLogin() throws Exception {
    when(platformService.authenticate(anyString(), any(PasswordAuthentication.class)))
        .thenReturn(new User());
    exception = false;
    when(session.getAttribute(Matchers.eq("loggedInUserId"))).thenReturn(null);

    String credentials = "user1:password1";
    String credentialsEncoded = Base64.encodeBase64String(credentials.getBytes());

    when(req.getHeader(Matchers.eq("Authorization"))).thenReturn("Basic " + credentialsEncoded);

    // And go!
    filter.doFilter(req, resp, chain);

    // Check whether request has been forwarded and user is logged in
    verify(session).setAttribute(Matchers.eq("loggedInUserId"), Matchers.eq("user1"));
    verify(session).setAttribute(Matchers.eq("loggedInUserPassword"), Matchers.eq("password1"));
    verify(chain).doFilter(Matchers.eq(req), Matchers.eq(resp));

    // And destroy
    filter.destroy();
  }

  @Test
  public void testAuthenticateLoginMissingHeader() throws Exception {
    when(session.getAttribute(Matchers.eq("loggedInUserId"))).thenReturn(null);

    when(req.getHeader(Matchers.eq("Authorization"))).thenReturn(null);

    // And go!
    filter.doFilter(req, resp, chain);

    // Check whether user will be asked for login
    verify(resp).setStatus(Matchers.eq(401));
    verify(resp).setHeader(Matchers.eq("WWW-Authenticate"), Matchers.startsWith("Basic "));
  }

  @Test
  public void testAuthenticateLogin_JA() throws Exception {
    when(session.getAttribute(Matchers.eq("loggedInUserId"))).thenReturn(null);

    when(req.getHeader(Matchers.eq("Authorization"))).thenReturn(null);
    when(req.getLocale()).thenReturn(new Locale("ja"));

    // when
    filter.doFilter(req, resp, chain);

    // then
    verify(resp).setStatus(Matchers.eq(401));
    verify(resp)
        .setHeader(
            Matchers.eq("WWW-Authenticate"),
            Matchers.startsWith("Basic realm=\"Please log in as technology manager\""));
  }

  @Test
  public void testAuthenticateEmptyAuthentication() throws Exception {
    when(session.getAttribute(Matchers.eq("loggedInUserId"))).thenReturn(null);

    when(req.getHeader(Matchers.eq("Authorization"))).thenReturn("");

    // And go!
    filter.doFilter(req, resp, chain);

    // Check whether request has been forwarded
    verify(resp).setStatus(Matchers.eq(401));
    verify(resp).setHeader(Matchers.eq("WWW-Authenticate"), Matchers.startsWith("Basic "));
  }

  @Test
  public void testAuthenticateWrongAuthentication() throws Exception {
    when(session.getAttribute(Matchers.eq("loggedInUserId"))).thenReturn(null);

    String credentials = "user1:password1";
    String credentialsEncoded = Base64.encodeBase64String(credentials.getBytes());

    when(req.getHeader(Matchers.eq("Authorization")))
        .thenReturn("UnknownSSO " + credentialsEncoded);

    // And go!
    filter.doFilter(req, resp, chain);

    // Check whether request has been forwarded
    verify(resp).setStatus(Matchers.eq(401));
    verify(resp).setHeader(Matchers.eq("WWW-Authenticate"), Matchers.startsWith("Basic "));
  }

  @Test
  public void testAuthenticateWrongCredentials() throws Exception {
    when(session.getAttribute(Matchers.eq("loggedInUserId"))).thenReturn(null);

    String credentials = "user1_password1";
    String credentialsEncoded = Base64.encodeBase64String(credentials.getBytes());

    when(req.getHeader(Matchers.eq("Authorization"))).thenReturn("Basic " + credentialsEncoded);

    // And go!
    filter.doFilter(req, resp, chain);

    // Check whether request has been forwarded
    verify(resp).setStatus(Matchers.eq(401));
    verify(resp).setHeader(Matchers.eq("WWW-Authenticate"), Matchers.startsWith("Basic "));
  }

  @Test
  public void testAuthenticateWithException() throws Exception {
    exception = true;
    when(session.getAttribute(Matchers.eq("loggedInUserId"))).thenReturn(null);

    String credentials = "user1:password1";
    String credentialsEncoded = Base64.encodeBase64String(credentials.getBytes());

    when(req.getHeader(Matchers.eq("Authorization"))).thenReturn("Basic " + credentialsEncoded);

    // And go!
    filter.doFilter(req, resp, chain);

    // Check whether request has been forwarded
    verify(resp).setStatus(Matchers.eq(401));
    verify(resp).setHeader(Matchers.eq("WWW-Authenticate"), Matchers.startsWith("Basic "));
  }

  @Test
  public void testCustomTabAuth() throws Exception {
    when(platformService.checkToken(anyString(), anyString())).thenReturn(true);
    exception = false;
    when(req.getServletPath()).thenReturn("/serverInformation.jsf");

    final String instId = "stack-ad8c51f1-d44b-489c-a2f6-40e8e68e0d86";
    String encodedInstId =
        new String(Base64.encodeBase64(instId.getBytes()), StandardCharsets.UTF_8);
    doReturn(encodedInstId).when(req).getParameter(Matchers.eq("instId"));

    final String orgId = "org1";
    String encodedOrgId = new String(Base64.encodeBase64(orgId.getBytes()), StandardCharsets.UTF_8);
    doReturn(encodedOrgId).when(req).getParameter(Matchers.eq("orgId"));

    final String subId = "sub1";
    String encodedSubId = new String(Base64.encodeBase64(subId.getBytes()), StandardCharsets.UTF_8);
    doReturn(encodedSubId).when(req).getParameter(Matchers.eq("subId"));

    final String timestamp = Long.toString(System.currentTimeMillis());
    doReturn(timestamp).when(req).getParameter(Matchers.eq("timestamp"));

    final String signature = "123eadfgh2awdsf234asdfgs";
    String encodedSignature =
        new String(Base64.encodeBase64(signature.getBytes()), StandardCharsets.UTF_8);
    doReturn(encodedSignature).when(req).getParameter(Matchers.eq("signature"));

    // And go!
    filter.doFilter(req, resp, chain);

    // Check whether request has been forwarded and user is logged in
    verify(chain).doFilter(Matchers.eq(req), Matchers.eq(resp));
    verify(resp, never()).setStatus(Matchers.eq(401));
    verify(resp, never())
        .setHeader(Matchers.eq("WWW-Authenticate"), Matchers.startsWith("Basic "));

    // And destroy
    filter.destroy();
  }

  @Test
  public void testCustomTabAuth_expired() throws Exception {
    exception = false;
    when(req.getServletPath()).thenReturn("/serverInformation.jsf");

    final String instId = "stack-ad8c51f1-d44b-489c-a2f6-40e8e68e0d86";
    String encodedInstId =
        new String(Base64.encodeBase64(instId.getBytes()), StandardCharsets.UTF_8);
    doReturn(encodedInstId).when(req).getParameter(Matchers.eq("instId"));

    final String orgId = "org1";
    String encodedOrgId = new String(Base64.encodeBase64(orgId.getBytes()), StandardCharsets.UTF_8);
    doReturn(encodedOrgId).when(req).getParameter(Matchers.eq("orgId"));

    final String subId = "sub1";
    String encodedSubId = new String(Base64.encodeBase64(subId.getBytes()), StandardCharsets.UTF_8);
    doReturn(encodedSubId).when(req).getParameter(Matchers.eq("subId"));

    final String timestamp = Long.toString(System.currentTimeMillis() - 1000000);
    doReturn(timestamp).when(req).getParameter(Matchers.eq("timestamp"));

    final String signature = "123eadfgh2awdsf234asdfgs";
    String encodedSignature =
        new String(Base64.encodeBase64(signature.getBytes()), StandardCharsets.UTF_8);
    doReturn(encodedSignature).when(req).getParameter(Matchers.eq("signature"));

    // And go!
    filter.doFilter(req, resp, chain);

    // Check whether request has been forwarded and user is logged in
    verify(chain, never()).doFilter(Matchers.eq(req), Matchers.eq(resp));
    verify(resp).setStatus(Matchers.eq(401));

    // And destroy
    filter.destroy();
  }
}
