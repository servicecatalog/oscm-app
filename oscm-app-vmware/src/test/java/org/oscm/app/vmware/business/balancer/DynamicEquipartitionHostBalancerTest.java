/**
 * *****************************************************************************
 *
 * <p>Copyright FUJITSU LIMITED 2020
 *
 * <p>Creation Date: 20.11.2020
 *
 * <p>*****************************************************************************
 */
package org.oscm.app.vmware.business.balancer;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.oscm.app.v2_0.exceptions.APPlatformException;
import org.oscm.app.vmware.business.VMPropertyHandler;
import org.oscm.app.vmware.business.VMwareDatacenterInventory;
import org.oscm.app.vmware.business.model.VMwareHost;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"javax.management.*", "javax.script.*", "jdk.internal.reflect.*"})
@PrepareForTest({DynamicEquipartitionHostBalancer.class, XMLHelper.class})
public class DynamicEquipartitionHostBalancerTest {

  @InjectMocks
  private DynamicEquipartitionHostBalancer dehBalancer;
  private VMPropertyHandler properties;
  private VMwareDatacenterInventory inventory;
  private Node node;
  private NodeList nodeList;
  private Document document;
  private List<VMwareHost> hosts;

  @Before
  public void setUp() {
    dehBalancer = PowerMockito.spy(new DynamicEquipartitionHostBalancer());

    properties = mock(VMPropertyHandler.class);
    inventory = mock(VMwareDatacenterInventory.class);
    node = mock(Node.class);
    nodeList = mock(NodeList.class);
    document = mock(Document.class);
    PowerMockito.mockStatic(XMLHelper.class);

    hosts = new LinkedList<>();
    hosts.add(new VMwareHost(inventory));

    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void testNext() throws APPlatformException {
    //given
    when(inventory.getHosts()).thenReturn(hosts);
    //when
    VMwareHost result = dehBalancer.next(properties);
    //then
    assertEquals(hosts.get(0), result);
  }

  @Test(expected = APPlatformException.class)
  public void testNextThrowsException() throws APPlatformException {
    //when
    dehBalancer.next(properties);
  }

  @Test
  public void testSetConfiguration() {
    //given
    when(node.getOwnerDocument()).thenReturn(document);
    when(document.getElementsByTagName(anyString())).thenReturn(nodeList);
    when(XMLHelper.getAttributeValue(any(), anyString(), anyString())).thenReturn("");
    //when
    dehBalancer.setConfiguration(node);
    //then
    verify(node, times(1)).getOwnerDocument();
  }
}
