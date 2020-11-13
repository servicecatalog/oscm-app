/*******************************************************************************
 *
 *  <p>Copyright FUJITSU LIMITED 2018
 *
 *  <p>Creation Date: 08.08.2012
 *
 *<p>******************************************************************************/

package org.oscm.app.v2_0.service;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.oscm.app.domain.PlatformConfigurationKey;
import org.oscm.app.v2_0.exceptions.APPlatformException;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMessage.RecipientType;
import javax.naming.Context;
import javax.naming.InitialContext;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static org.oscm.app.v2_0.service.APPCommunicationServiceBean.DEFAULT_MAIL_RESOURCE;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"javax.management.*", "javax.script.*", "jdk.internal.reflect.*"})
@PrepareForTest({APPCommunicationServiceBean.class, Transport.class})
public class APPCommunicationServiceBeanTest {

  private APPCommunicationServiceBean commService;
  private Session mailMock;
  private APPConfigurationServiceBean configurationService;
  private MimeMessage mimeMessage;
  private Context initialContext;

  @Before
  public void setup() throws Exception {

    Properties properties = new Properties();
    properties.put("mail.smtp.from", "test@ess.intern");
    mailMock = Session.getInstance(properties);

    commService = PowerMockito.spy(new APPCommunicationServiceBean());
    configurationService = mock(APPConfigurationServiceBean.class);
    mimeMessage = mock(MimeMessage.class);
    initialContext = mock(InitialContext.class);
    PowerMockito.mockStatic(Transport.class);
    commService.configService = configurationService;
    PowerMockito.whenNew(InitialContext.class).withNoArguments().thenReturn((InitialContext) initialContext);
    when(initialContext.lookup(anyString())).thenReturn(mailMock);
  }

  @Test
  public void testSendMail() throws Exception {
    List<String> mailAddresses = new ArrayList<>();
    mailAddresses.add("test@noreply.de");
    PowerMockito.when(commService.composeMessage(mailAddresses, "subject", "text")).thenReturn(mimeMessage);
    doNothing().when(commService).transportMail(Matchers.any(MimeMessage.class));

    commService.sendMail(mailAddresses, "subject", "text");

    verify(commService, times(1)).transportMail(any(MimeMessage.class));
  }

  @Test(expected = APPlatformException.class)
  public void testSendMailInvalidFromAddress() throws Exception {
    mailMock.getProperties().put("mail.smtp.from", "");

    commService.sendMail(Collections.singletonList("test@noreply.de"),
        "subject", "text");
  }

  @Test(expected = APPlatformException.class)
  public void testSendMailEmptyRecipientAddress() throws Exception {
    commService.sendMail(Collections.singletonList(""), "subject", "text");
  }

  @Test(expected = APPlatformException.class)
  public void testSendMailNullRecipientAddress() throws Exception {
    commService.sendMail(Collections.singletonList((String) null),
        "subject", "text");
  }

  @Test(expected = APPlatformException.class)
  public void testSendMailNullRecipientList() throws Exception {
    commService.sendMail(null, "subject", "text");
  }

  @Test(expected = APPlatformException.class)
  public void testSendMailEmptyRecipientList() throws Exception {
    commService.sendMail(new ArrayList<String>(), "subject", "text");
  }

  @Test(expected = APPlatformException.class)
  public void testSendMailTransportException() throws Exception {
    doThrow(new MessagingException("Transport problem")).when(commService)
        .transportMail(Matchers.any(MimeMessage.class));
    commService.sendMail(Collections.singletonList("test@noreply.de"),
        "subject", "text");
  }

  @Test(expected = APPlatformException.class)
  public void testSendMailInvalidCustomMailResource() throws Exception {
    PowerMockito.whenNew(InitialContext.class).withNoArguments().thenReturn(null);
    when(
        configurationService
            .getProxyConfigurationSetting(PlatformConfigurationKey.APP_MAIL_RESOURCE))
        .thenReturn("mail/notexisting");
    commService.sendMail(Collections.singletonList("test@noreply.de"),
        "subject", "text");
  }

  @Test
  public void testComposeMessage() throws Exception {

    MimeMessage message = commService
        .composeMessage(Collections.singletonList("test@noreply.de"),
            "subject", "text");

    Assert.assertNotNull(message);
    assertEquals("subject", message.getSubject());
    assertEquals("text", message.getContent());
    assertEquals("test@ess.intern", message.getFrom()[0].toString());
    Assert.assertNotNull(message.getAllRecipients());
    assertEquals(1, message.getAllRecipients().length);
    assertEquals("test@noreply.de",
        message.getAllRecipients()[0].toString());
  }

  @Test
  public void testComposeMessageCustomMailResource() throws Exception {
    when(
        configurationService
            .getProxyConfigurationSetting(PlatformConfigurationKey.APP_MAIL_RESOURCE))
        .thenReturn(DEFAULT_MAIL_RESOURCE);

    MimeMessage message = commService
        .composeMessage(Collections.singletonList("test@noreply.de"),
            "subject", "text");

    Assert.assertNotNull(message);
    assertEquals("subject", message.getSubject());
    assertEquals("text", message.getContent());
  }

  @Test
  public void testComposeMessageEmptyCustomMailResource() throws Exception {
    when(
        configurationService
            .getProxyConfigurationSetting(PlatformConfigurationKey.APP_MAIL_RESOURCE))
        .thenReturn("");

    MimeMessage message = commService
        .composeMessage(Collections.singletonList("test@noreply.de"),
            "subject", "text");

    Assert.assertNotNull(message);
    assertEquals("subject", message.getSubject());
    assertEquals("text", message.getContent());
  }

  @Test(expected = APPlatformException.class)
  public void testComposeMessage_mailInitFails() throws Exception {
    MimeMessage mockMessage = mock(MimeMessage.class);
    doThrow(new MessagingException("Mail initialization fails")).when(
        mockMessage).setFrom(any(Address.class));
    doReturn(mockMessage).when(commService).getMimeMessage(
        any(Session.class));

    commService
        .composeMessage(Collections.singletonList("test@noreply.de"),
            "subject", "text");
  }

  @Test(expected = APPlatformException.class)
  public void testComposeMessage_addRecipientsFails() throws Exception {
    MimeMessage mockMessage = mock(MimeMessage.class);
    doThrow(new MessagingException("Adding recipient addresses fails."))
        .when(mockMessage).addRecipients(any(RecipientType.class),
        any(Address[].class));
    doReturn(mockMessage).when(commService).getMimeMessage(
        any(Session.class));

    commService
        .composeMessage(Collections.singletonList("test@noreply.de"),
            "subject", "text");
  }

  @Test
  public void removeDuplicates() {
    final String RECIPIENT1 = "abc";
    final String RECIPIENT2 = "xyz";
    List<String> recipients = Arrays.asList(RECIPIENT1, RECIPIENT2,
        RECIPIENT1, RECIPIENT2);

    recipients = commService.removeDuplicates(recipients);

    assertEquals(2, recipients.size());
    assertTrue(recipients.contains(RECIPIENT1));
    assertTrue(recipients.contains(RECIPIENT2));
  }

  @Test
  public void testTransportMail() throws MessagingException {

    commService.transportMail(mimeMessage);

    PowerMockito.verifyStatic(Transport.class, times(1));
  }
}
