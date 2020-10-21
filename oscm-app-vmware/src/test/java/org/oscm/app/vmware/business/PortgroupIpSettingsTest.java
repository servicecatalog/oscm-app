/**
 * *****************************************************************************
 *
 * <p>Copyright FUJITSU LIMITED 2020
 *
 * <p>Creation Date: 14 Oct 2020
 *
 * <p>*****************************************************************************
 */
package org.oscm.app.vmware.business;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.oscm.app.vmware.business.model.Cluster;
import org.oscm.app.vmware.business.model.DistributedVirtualSwitch;
import org.oscm.app.vmware.business.model.Portgroup;
import org.oscm.app.vmware.business.model.PortgroupIPPool;
import org.oscm.app.vmware.persistence.DataAccessService;

/** @author worf */
public class PortgroupIpSettingsTest {

  private Cluster cluster;
  private DistributedVirtualSwitch dvs;
  private Portgroup portgroup;
  private List<PortgroupIPPool> portgroupIPPool;
  @Mock DataAccessService das;
  @Mock VMPropertyHandler ph;
  @Mock PortgroupIpSettings pis;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    initializeVariables();
    initializeMocks();
    initializeCluster();
    initializeSwitch();
    initializePortgroup();
    pis = new PortgroupIpSettings(ph, 0);
    pis = Mockito.spy(pis);
  }

  private void initializeVariables() {
    this.cluster = new Cluster();
    this.dvs = new DistributedVirtualSwitch();
    this.portgroup = new Portgroup();

    PortgroupIPPool ip1 = new PortgroupIPPool();
    PortgroupIPPool ip = new PortgroupIPPool();
    ip.setTkey(0);
    ip1.setTkey(1);
    ip.setIn_use(true);
    ip1.setIn_use(true);
    portgroupIPPool = new ArrayList<PortgroupIPPool>();
    portgroupIPPool.add(ip);
    portgroupIPPool.add(ip1);
  }

  private void initializeCluster() {
    List<Cluster> clusters = new ArrayList<Cluster>();
    cluster.setName("testCluster");
    clusters.add(cluster);
    doReturn(clusters).when(ph).getClusters();
    doReturn("testCluster").when(ph).getTargetCluster();
  }

  private void initializeSwitch() {
    List<DistributedVirtualSwitch> dvss = new ArrayList<DistributedVirtualSwitch>();
    dvs.setName("testDvs");
    dvss.add(dvs);
    doReturn(dvss).when(ph).getDistributedVirtualSwitch(cluster);
    doReturn("testDvs").when(ph).getSwitchUUID(anyInt());
  }

  private void initializePortgroup() {
    List<Portgroup> pgs = new ArrayList<Portgroup>();
    portgroup.setName("testPortgroup");
    pgs.add(portgroup);
    doReturn(pgs).when(ph).getPortgroup(dvs);
    doReturn("testPortgroup").when(ph).getPortGroup(anyInt());
  }

  private void initializeMocks() {
    doReturn(das).when(ph).getDataAccessService();
    doReturn(cluster).when(pis).getCluster(anyString(), any());
    doReturn(dvs).when(pis).getDistributedVirtualSwitch(anyString(), any());
    doReturn(portgroup).when(pis).getPortgroup(anyString(), any());
    doReturn(portgroupIPPool).when(ph).getPortgroupIPPool(any());
  }

  @Test
  public void getCluster() {
    // given
    Cluster c1 = new Cluster();
    Cluster c2 = new Cluster();
    c1.setName("c1");
    c2.setName("c2");

    List<Cluster> clusters = new ArrayList<Cluster>();
    clusters.add(c1);
    clusters.add(c2);

    // when
    Cluster result = pis.getCluster("c1", clusters);

    // then
    assertEquals(c1, result);
  }

  @Test
  public void getDistributedVirtualSwitch() {
    // given
    DistributedVirtualSwitch d1 = new DistributedVirtualSwitch();
    DistributedVirtualSwitch d2 = new DistributedVirtualSwitch();
    d1.setName("d1");
    d2.setName("d2");

    List<DistributedVirtualSwitch> switches = new ArrayList<DistributedVirtualSwitch>();
    switches.add(d1);
    switches.add(d2);

    // when
    DistributedVirtualSwitch result = pis.getDistributedVirtualSwitch("d1", switches);

    // then
    assertEquals(d1, result);
  }

  @Test
  public void getPortgroup() {
    // given
    Portgroup pg1 = new Portgroup();
    Portgroup pg2 = new Portgroup();
    pg1.setName("pg1");
    pg2.setName("cpg");

    List<Portgroup> portgroup = new ArrayList<Portgroup>();
    portgroup.add(pg1);
    portgroup.add(pg2);

    // when
    Portgroup result = pis.getPortgroup("pg1", portgroup);

    // then
    assertEquals(pg1, result);
  }

  @Test
  public void getIpAdressFromIpPool() throws Exception {

    // given
    doNothing().when(das).reservePortgroupIPAddress(anyInt());
    PortgroupIPPool ip = new PortgroupIPPool();
    ip.setTkey(2);
    ip.setIp_adress("127.0.0.1");
    portgroupIPPool.add(ip);

    // when
    String result = pis.getIpAdressFromIpPool();

    // then
    assertEquals("127.0.0.1", result);
  }

  @Test
  public void returnIpAdressToIpPool() throws Exception {
    // given
    PortgroupIPPool ip = new PortgroupIPPool();
    ip.setTkey(2);
    ip.setIp_adress("127.0.0.1");
    portgroupIPPool.add(ip);
    ip.setIn_use(true);

    doNothing().when(das).unReservePortgroupIPAddress(anyInt());

    // when
    pis.returnIpAdressToIpPool("127.0.0.1");

    // then
    verify(das, times(1)).unReservePortgroupIPAddress(anyInt());
  }
}
