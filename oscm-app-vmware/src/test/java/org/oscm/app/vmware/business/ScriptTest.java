/*******************************************************************************
 *
 *  Copyright FUJITSU LIMITED 2018
 *
 *  Creation Date: 2016-05-24
 *
 *******************************************************************************/

package org.oscm.app.vmware.business;

import com.vmware.vim25.*;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.oscm.app.vmware.business.Script.OS;
import org.oscm.app.vmware.remote.bes.ServiceParamRetrieval;
import org.oscm.app.vmware.remote.vmware.ManagedObjectAccessor;
import org.oscm.app.vmware.remote.vmware.ServiceConnection;
import org.oscm.app.vmware.remote.vmware.VMwareClient;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSessionContext;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.io.OutputStream;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"javax.management.*", "javax.script.*", "jdk.internal.reflect.*"})
@PrepareForTest({Script.class, SSLContext.class, HttpsURLConnection.class, IOUtils.class})
public class ScriptTest {

  Script script = Script.getInstance();

  @Mock
  private VMPropertyHandler vph;
  OS os;
  @Mock
  private ServiceParamRetrieval spr;
  @Mock
  private VMwareClient vmw;
  @Mock
  ServiceConnection sCon;
  @Mock
  ServiceContent sCont;
  @Mock
  ManagedObjectReference guestOpManager;
  @Mock
  ManagedObjectReference fileManagerRef;
  @Mock
  ManagedObjectReference processManagerRef;
  @Mock
  ManagedObjectAccessor moa;
  @Mock
  GuestProcessInfo procInf;
  @Mock
  ScriptExecutionObjectReferences objectRef;

  private SSLContext sslContext;
  private SSLSessionContext sessionContext;
  private StringWriter stringWriter;
  private URL url;
  private HttpURLConnection urlConnection;
  private ManagedObjectReference objectReference;
  private NamePasswordAuthentication auth;
  private VimPortType vimPort;
  private OutputStream outputStream;
  String WINDOWS_GUEST_FILE_PATH = "WINDOWS_GUEST_FILE_PATH";
  String LINUX_GUEST_FILE_PATH = "LINUX_GUEST_FILE_PATH";
  String guestUserId = "guestUserId";
  String guestPassword = "guestPassword";
  List<String> pwList;

  @Before
  public void setUp() throws Exception {
    vimPort = PowerMockito.mock(VimPortType.class);
    objectReference = PowerMockito.mock(ManagedObjectReference.class);
    auth = PowerMockito.mock(NamePasswordAuthentication.class);

    mockScript();
    os = OS.LINUX;
    MockitoAnnotations.initMocks(this);
    setUpMocks();

    sslContext = PowerMockito.mock(SSLContext.class);
    sessionContext = PowerMockito.mock(SSLSessionContext.class);
    stringWriter = PowerMockito.mock(StringWriter.class);
    url = PowerMockito.mock(URL.class);
    urlConnection = PowerMockito.mock(HttpURLConnection.class);
    outputStream = PowerMockito.mock(OutputStream.class);
    PowerMockito.mockStatic(SSLContext.class);
    PowerMockito.mockStatic(HttpsURLConnection.class);
    PowerMockito.mockStatic(IOUtils.class);

    PowerMockito.when(SSLContext.getInstance(anyString())).thenReturn(sslContext);
    PowerMockito.when(sslContext.getServerSessionContext()).thenReturn(sessionContext);
    PowerMockito.doNothing().when(HttpsURLConnection.class, "setDefaultSSLSocketFactory", any());
  }

  private void setUpMocks() throws Exception {

    pwList = new ArrayList<String>();
    pwList.add("newPw");
    List<GuestProcessInfo> procInfo = new ArrayList<GuestProcessInfo>();
    procInfo.add(procInf);

    doReturn(spr).when(script).createServiceParameterRetrieval(any(VMPropertyHandler.class));
    doReturn(objectRef).when(script).getScriptExecutionObjectReferences(any());
    doReturn(guestOpManager).when(objectRef).getGuestOpManger();
    doReturn(fileManagerRef).when(objectRef).getFileManagerRef();
    doReturn(processManagerRef).when(objectRef).getProcessManagerRef();
    doReturn(vimPort).when(objectRef).getVimPort();
    doReturn(moa).when(objectRef).getMoa();
    doReturn(new URL("https://github.com/servicecatalog/")).when(script).getVSphereURL();
    doReturn(procInfo).when(script).getProcInfo(any(), any(), any(), any(), any());
    when(procInf.getEndTime()).thenReturn(getDate());
  }

  private XMLGregorianCalendar getDate()
      throws DatatypeConfigurationException {
    XMLGregorianCalendar date = DatatypeFactory.newInstance().newXMLGregorianCalendar();
    date.setSecond(5);
    return date;
  }

  private void mockScript() {
    script = PowerMockito.spy(script);
    setSpy(script);
  }

  private void setSpy(Script mock) {
    try {
      Field script = Script.class.getDeclaredField("script");
      script.setAccessible(true);
      script.set(script, mock);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @After
  public void resetSingleton() throws Exception {
    Field script = Script.class.getDeclaredField("script");
    script.setAccessible(true);
    script.set(null, null);
  }

  @Test
  public void executeUpdateScript() throws Exception {
    // given
    String updateScript = VMScript.updateLinuxVMRootPassword("testPassword");
    script.initScript(vph, os, updateScript);
    doNothing().when(script).uploadScriptFileToVM(any(), any(), any(), any(), any(), any());
    doReturn(pwList).when(script).addOsIndependetServiceParameters(any());
    // when
    script.execute(vmw, objectReference);
    // then
    verify(script).execute(vmw, objectReference);
    verify(script, times(1)).setScriptExecuting(eq(true));
    verify(script, times(1)).setScriptExecuting(eq(false));
  }

  @Test
  public void executeUpdateScript_withSetPwException() throws Exception {
    // given
    String updateScript = VMScript
        .updateLinuxVMRootPassword("testPassword");
    script.initScript(vph, os, updateScript);
    doNothing().when(script).uploadScriptFileToVM(any(), any(), any(), any(), any(), any());
    doReturn(pwList).when(script).addOsIndependetServiceParameters(any());

    // when
    when(script.getProcInfo(any(), any(), any(), any(), any()))
        .thenThrow(new GuestOperationsFaultFaultMsg(any(), any()));
    script.execute(vmw, objectReference);
    // then
    verify(script).execute(vmw, objectReference);
    verify(script, times(1)).setScriptExecuting(eq(true));
    verify(script, times(1)).setScriptExecuting(eq(false));
  }

  @Test
  public void executeUpdateScript_withException() throws Exception {
    // given
    String updateScript = VMScript
        .updateLinuxVMRootPassword("testPassword");
    script.initScript(vph, os, updateScript);
    // when
    try {
      when(script.getVSphereURL())
          .thenThrow(new Exception(any(), any()));
      script.execute(vmw, objectReference);
    } catch (Exception e) {
      // then
      verify(script).execute(vmw, objectReference);
      verify(script, times(1)).setScriptExecuting(eq(true));
      verify(script, times(1)).setScriptExecuting(eq(false));
    }
  }

  @Test
  public void testHidePasswordsLinux() throws Exception {
    // given
    String script = "#!/bin/bash" + OS.LINUX.getLineEnding()
        + "LINUX_ROOT_PWD='sunny'" + OS.LINUX.getLineEnding()
        + "DOMAIN_NAME='sunnyside.up.com'" + OS.LINUX.getLineEnding()
        + "INSTANCENAME='sunny'" + OS.LINUX.getLineEnding()
        + "REQUESTING_USER='Julia@DarkSide.com'"
        + OS.LINUX.getLineEnding()
        + "SCRIPT_URL='http://localhost:28880/test_script.sh'"
        + OS.LINUX.getLineEnding() + "SCRIPT_USERID='root'"
        + OS.LINUX.getLineEnding() + "SCRIPT_PWD='sunnyscript'"
        + OS.LINUX.getLineEnding() + "NIC1_DNS_SERVER=''"
        + OS.LINUX.getLineEnding() + "NIC1_DNS_SUFFIX=''"
        + OS.LINUX.getLineEnding() + "NIC1_GATEWAY=''"
        + OS.LINUX.getLineEnding() + "NIC1_IP_ADDRESS=''"
        + OS.LINUX.getLineEnding() + OS.LINUX.getLineEnding()
        + "shutdown -h now";

    List<String> passwords = new ArrayList<String>();
    passwords.add("sunny");
    passwords.add("sunnyscript");
    // when
    String changedScript = Script
        .hidePasswords(script, passwords, OS.LINUX);
    // then
    assertTrue(changedScript.contains("LINUX_ROOT_PWD='"
        + Script.HIDDEN_PWD));
    assertTrue(changedScript.contains("SCRIPT_PWD='" + Script.HIDDEN_PWD));
    System.out.println(changedScript);
  }

  @Test
  public void testHidePasswordsLinux_EmptyPwd() throws Exception {
    // given
    String script = "#!/bin/bash" + OS.LINUX.getLineEnding()
        + "LINUX_ROOT_PWD=''" + OS.LINUX.getLineEnding()
        + "DOMAIN_NAME='sunnyside.up.com'" + OS.LINUX.getLineEnding()
        + "INSTANCENAME='sunny'" + OS.LINUX.getLineEnding()
        + "REQUESTING_USER='Julia@DarkSide.com'"
        + OS.LINUX.getLineEnding()
        + "SCRIPT_URL='http://localhost:28880/test_script.sh'"
        + OS.LINUX.getLineEnding() + "SCRIPT_USERID='root'"
        + OS.LINUX.getLineEnding() + "SCRIPT_PWD='sunnyscript'"
        + OS.LINUX.getLineEnding() + "NIC1_DNS_SERVER=''"
        + OS.LINUX.getLineEnding() + "NIC1_DNS_SUFFIX=''"
        + OS.LINUX.getLineEnding() + "NIC1_GATEWAY=''"
        + OS.LINUX.getLineEnding() + "NIC1_IP_ADDRESS=''"
        + OS.LINUX.getLineEnding() + OS.LINUX.getLineEnding()
        + "shutdown -h now";

    List<String> passwords = new ArrayList<String>();
    passwords.add("");
    passwords.add("sunnyscript");
    // when
    String changedScript = Script
        .hidePasswords(script, passwords, OS.LINUX);
    // then
    System.out.println(changedScript);
    assertTrue(changedScript.contains("LINUX_ROOT_PWD='"
        + Script.HIDDEN_PWD));
    assertTrue(changedScript.contains("SCRIPT_PWD='" + Script.HIDDEN_PWD));
  }

  @Test
  public void testHidePasswordsLinuxScript() throws Exception {
    // given
    String script = "#!/bin/bash" + OS.LINUX.getLineEnding()
        + "LINUX_ROOT_PWD=''" + OS.LINUX.getLineEnding()
        + "DOMAIN_NAME='sunnyside.up.com'" + OS.LINUX.getLineEnding()
        + "INSTANCENAME='sunny'" + OS.LINUX.getLineEnding()
        + "REQUESTING_USER='Julia@DarkSide.com'"
        + OS.LINUX.getLineEnding()
        + "SCRIPT_URL='http://localhost:28880/test_script.sh'"
        + OS.LINUX.getLineEnding() + "SCRIPT_USERID='root'"
        + OS.LINUX.getLineEnding() + "SCRIPT_PWD='sunnyscript'"
        + OS.LINUX.getLineEnding() + "NIC1_DNS_SERVER=''"
        + OS.LINUX.getLineEnding() + "NIC1_DNS_SUFFIX=''"
        + OS.LINUX.getLineEnding() + "NIC1_GATEWAY=''"
        + OS.LINUX.getLineEnding() + "NIC1_IP_ADDRESS=''"
        + OS.LINUX.getLineEnding() + OS.LINUX.getLineEnding()
        + "echo -e testPassword \n"
        + "'testPassword | passwd root\n";

    List<String> passwords = new ArrayList<String>();
    passwords.add("");
    passwords.add("sunnyscript");
    // when
    String changedScript = Script.hideScriptPasswords(script, "testPassword");
    // then
    System.out.println(changedScript);
    assertTrue(changedScript.contains("echo -e '*****' \n" +
        "''*****' | passwd root"));
  }

  @Test
  public void testHidePasswordsWindows() throws Exception {
    // given
    String script = "set WINDOWS_LOCAL_ADMIN_PWD=sunny"
        + OS.WINDOWS.getLineEnding()
        + "set WINDOWS_DOMAIN_ADMIN_PWD=admin123"
        + OS.WINDOWS.getLineEnding()
        + "set DOMAIN_NAME=sunnyside.up.com"
        + OS.WINDOWS.getLineEnding() + "set INSTANCENAME=sunny"
        + OS.WINDOWS.getLineEnding()
        + "set REQUESTING_USER=Julia@DarkSide.com"
        + OS.WINDOWS.getLineEnding()
        + "set SCRIPT_URL=http://localhost:28880/test_script.sh"
        + OS.WINDOWS.getLineEnding()
        + "set SCRIPT_USERID=Administrator"
        + OS.WINDOWS.getLineEnding() + "set SCRIPT_PWD=sunnyscript"
        + OS.WINDOWS.getLineEnding() + "set NIC1_DNS_SERVER="
        + OS.WINDOWS.getLineEnding() + "set NIC1_DNS_SUFFIX="
        + OS.WINDOWS.getLineEnding() + "set NIC1_GATEWAY="
        + OS.WINDOWS.getLineEnding() + "set NIC1_IP_ADDRESS="
        + OS.WINDOWS.getLineEnding();

    List<String> passwords = new ArrayList<String>();
    passwords.add("sunny");
    passwords.add("sunnyscript");
    passwords.add("admin123");
    // when
    String changedScript = Script.hidePasswords(script, passwords,
        OS.WINDOWS);
    // then
    assertTrue(changedScript.contains("WINDOWS_LOCAL_ADMIN_PWD="
        + Script.HIDDEN_PWD));
    assertTrue(changedScript.contains("WINDOWS_DOMAIN_ADMIN_PWD="
        + Script.HIDDEN_PWD));
    assertTrue(changedScript.contains("SCRIPT_PWD=" + Script.HIDDEN_PWD));
    System.out.println(changedScript);
  }

  @Test
  public void testHidePasswordsWindows_EmptyPwd() throws Exception {
    // given
    String script = "set WINDOWS_LOCAL_ADMIN_PWD="
        + OS.WINDOWS.getLineEnding()
        + "set WINDOWS_DOMAIN_ADMIN_PWD=admin123"
        + OS.WINDOWS.getLineEnding()
        + "set DOMAIN_NAME=sunnyside.up.com"
        + OS.WINDOWS.getLineEnding() + "set INSTANCENAME=sunny"
        + OS.WINDOWS.getLineEnding()
        + "set REQUESTING_USER=Julia@DarkSide.com"
        + OS.WINDOWS.getLineEnding()
        + "set SCRIPT_URL=http://localhost:28880/test_script.sh"
        + OS.WINDOWS.getLineEnding()
        + "set SCRIPT_USERID=Administrator"
        + OS.WINDOWS.getLineEnding() + "set SCRIPT_PWD=sunnyscript"
        + OS.WINDOWS.getLineEnding() + "set NIC1_DNS_SERVER="
        + OS.WINDOWS.getLineEnding() + "set NIC1_DNS_SUFFIX="
        + OS.WINDOWS.getLineEnding() + "set NIC1_GATEWAY="
        + OS.WINDOWS.getLineEnding() + "set NIC1_IP_ADDRESS="
        + OS.WINDOWS.getLineEnding();

    List<String> passwords = new ArrayList<String>();
    passwords.add("");
    passwords.add("sunnyscript");
    passwords.add("admin123");
    // when
    String changedScript = Script.hidePasswords(script, passwords,
        OS.WINDOWS);
    // then
    assertTrue(changedScript.contains("WINDOWS_LOCAL_ADMIN_PWD="
        + Script.HIDDEN_PWD));
    assertTrue(changedScript.contains("WINDOWS_DOMAIN_ADMIN_PWD="
        + Script.HIDDEN_PWD));
    assertTrue(changedScript.contains("SCRIPT_PWD=" + Script.HIDDEN_PWD));
    System.out.println(changedScript);
  }

  @Test
  public void getGuestProgramSpecLinux() {
    //given
    String tempFilePath = "test/testPath/Linux";
    //when
    GuestProgramSpec spec = script.getGuestProgramSpec(tempFilePath);
    //then
    assertEquals(spec.getArguments(), " > " + tempFilePath + " 2>&1");
  }

  @Test
  public void getGuestProgramSpecWindows() throws Exception {
    //given
    String tempFilePath = "test\\testPath\\Windows";
    Whitebox.setInternalState(script, "os", OS.WINDOWS);
    //when
    GuestProgramSpec spec = script.getGuestProgramSpec(tempFilePath);
    //then
    assertEquals(spec.getArguments(), " > " + tempFilePath);
  }

  @Test
  public void getScriptExecutionObjectReferences() throws Exception {
    //when
    ScriptExecutionObjectReferences objectRef = script.getScriptExecutionObjectReferences(
        vmw);
    //then
    assertEquals(objectRef.getGuestOpManger(), guestOpManager);
    assertEquals(objectRef.getFileManagerRef(), fileManagerRef);
    assertEquals(objectRef.getMoa(), moa);
    assertEquals(objectRef.getProcessManagerRef(), processManagerRef);
  }

  @Test
  public void getPasswordAuthentication() {
    //given
    NamePasswordAuthentication autentication = new NamePasswordAuthentication();
    autentication.setPassword(guestPassword);
    autentication.setUsername(guestUserId);
    doReturn(autentication).when(script).getPasswordAuthentication();
    //when
    NamePasswordAuthentication auth = script.getPasswordAuthentication();
    //then
    assertEquals(guestPassword, auth.getPassword());
    assertEquals(guestUserId, auth.getUsername());
  }

  @Test
  public void testDownloadFile() throws Exception {
    //given
    PowerMockito.whenNew(StringWriter.class).withNoArguments().thenReturn(stringWriter);
    PowerMockito.whenNew(URL.class).withAnyArguments().thenReturn(url);
    when(url.openConnection()).thenReturn(urlConnection);
    PowerMockito.doNothing().when(IOUtils.class, "copy", any(), any(), anyString());
    when(urlConnection.getResponseCode()).thenReturn(200);
    //when
    String result = script.downloadFile("url");
    //then
    verify(urlConnection, times(1)).disconnect();
    verify(urlConnection, times(1)).getResponseCode();
    assertEquals("stringWriter", result);
  }

  @Test(expected = Exception.class)
  public void testDownloadFileThrowExcepion() throws Exception {
    //given
    PowerMockito.whenNew(StringWriter.class).withNoArguments().thenReturn(stringWriter);
    PowerMockito.whenNew(URL.class).withAnyArguments().thenReturn(url);
    when(url.openConnection()).thenReturn(urlConnection);
    PowerMockito.doNothing().when(IOUtils.class, "copy", any(), any(), anyString());
    //when
    script.downloadFile("url");
  }

  @Test
  public void testUploadScriptFileToVMInWindows() throws Exception {
    //given
    Whitebox.setInternalState(script, "os", OS.WINDOWS);
    when(vimPort.initiateFileTransferToGuest(any(), any(), any(), anyString(), any(), anyInt(), eq(true))).thenReturn("https://company.com.my/file");
    PowerMockito.whenNew(URL.class).withAnyArguments().thenReturn(url);
    when(url.openConnection()).thenReturn(urlConnection);
    when(urlConnection.getOutputStream()).thenReturn(outputStream);
    when(urlConnection.getResponseCode()).thenReturn(200);
    //when
    Whitebox.invokeMethod(script, "uploadScriptFileToVM", vimPort, objectReference, objectReference, auth, "script", "hostname");
    //then
    verify(urlConnection, times(1)).disconnect();
    verify(urlConnection, times(1)).getResponseCode();
  }

  @Test
  public void testUploadScriptFileToVMInLinux() throws Exception {
    //given
    when(vimPort.initiateFileTransferToGuest(any(), any(), any(), anyString(), any(), anyInt(), eq(true))).thenReturn("https://company.com.my/file");
    PowerMockito.whenNew(URL.class).withAnyArguments().thenReturn(url);
    when(url.openConnection()).thenReturn(urlConnection);
    when(urlConnection.getOutputStream()).thenReturn(outputStream);
    when(urlConnection.getResponseCode()).thenReturn(200);
    //when
    Whitebox.invokeMethod(script, "uploadScriptFileToVM", vimPort, objectReference, objectReference, auth, "script", "hostname");
    //then
    verify(urlConnection, times(1)).disconnect();
    verify(urlConnection, times(1)).getResponseCode();
  }

  @Test(expected = Exception.class)
  public void testUploadScriptFileToVMThrowException() throws Exception {
    //given
    Whitebox.setInternalState(script, "os", OS.WINDOWS);
    when(vimPort.initiateFileTransferToGuest(any(), any(), any(), anyString(), any(), anyInt(), eq(true))).thenReturn("https://company.com.my/file");
    PowerMockito.whenNew(URL.class).withAnyArguments().thenReturn(url);
    when(url.openConnection()).thenReturn(urlConnection);
    when(urlConnection.getOutputStream()).thenReturn(outputStream);
    //when
    Whitebox.invokeMethod(script, "uploadScriptFileToVM", vimPort, objectReference, objectReference, auth, "script", "hostname");
  }

  @Test
  public void testAddServiceParametersForWindowsVms() throws Exception {
    //given
    String updateScript = VMScript.updateLinuxVMRootPassword("testPassword");
    script.initScript(vph, os, updateScript);
    StringBuffer sb = new StringBuffer();
    when(spr.getServiceSetting(anyString())).thenReturn("Setting1", "Setting1");
    //when
    List<String> result = Whitebox.invokeMethod(script, "addServiceParametersForWindowsVms", sb);
    //then
    assertEquals("Setting1", result.get(0));
    PowerMockito.verifyPrivate(script, times(6)).invoke("buildParameterCommand", anyString());
  }

  @Test
  public void testAddOsIndependentServiceParameters() throws Exception {
    //given
    String updateScript = VMScript.updateLinuxVMRootPassword("testPassword");
    script.initScript(vph, os, updateScript);
    StringBuffer sb = new StringBuffer();
    when(spr.getServiceSetting(anyString())).thenReturn("5");
    //when
    List<String> result = script.addOsIndependetServiceParameters(sb);
    //then
    assertEquals("5", result.get(0));
    PowerMockito.verifyPrivate(script, times(34)).invoke("buildParameterCommand", anyString());
  }
}
