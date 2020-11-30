package org.oscm.app.vmware.business;

import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.ServiceContent;
import com.vmware.vim25.VimPortType;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.oscm.app.vmware.business.ScriptExecutionObjectReferences;
import org.oscm.app.vmware.remote.vmware.ManagedObjectAccessor;
import org.oscm.app.vmware.remote.vmware.ServiceConnection;
import org.oscm.app.vmware.remote.vmware.VMwareClient;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"javax.management.*", "javax.script.*", "jdk.internal.reflect.*"})
@PrepareForTest({ScriptExecutionObjectReferences.class})
public class ScriptExecutionObjectReferencesTest {

  private ScriptExecutionObjectReferences seor;
  private VimPortType vimPort;
  private ManagedObjectAccessor moa;
  private ManagedObjectReference objectReference;
  private VMwareClient vmw;
  private ServiceConnection serviceConnection;
  private ServiceContent serviceContent;

  @Before
  public void before() {
    vmw = mock(VMwareClient.class);
    vimPort = mock(VimPortType.class);
    moa = mock(ManagedObjectAccessor.class);
    objectReference = mock(ManagedObjectReference.class);
    serviceConnection = mock(ServiceConnection.class);
    serviceContent = mock(ServiceContent.class);
  }

  @Test
  public void testConstructor() throws Exception {
    // given
    when(vmw.getConnection()).thenReturn(serviceConnection);
    when(serviceConnection.getService()).thenReturn(vimPort);
    PowerMockito.whenNew(ManagedObjectAccessor.class).withAnyArguments().thenReturn(moa);
    when(serviceConnection.getServiceContent()).thenReturn(serviceContent);
    when(serviceContent.getGuestOperationsManager()).thenReturn(objectReference);
    when(moa.getDynamicProperty(objectReference, "fileManager")).thenReturn(objectReference);
    when(moa.getDynamicProperty(objectReference, "processManager")).thenReturn(objectReference);
    //when
    seor = PowerMockito.spy(Whitebox.invokeConstructor(ScriptExecutionObjectReferences.class, vmw));
    //then
    assertEquals(vimPort, seor.getVimPort());
    assertEquals(moa, seor.getMoa());
    assertEquals(objectReference, seor.getGuestOpManger());
    assertEquals(objectReference, seor.getFileManagerRef());
    assertEquals(objectReference, seor.getProcessManagerRef());
  }
}
