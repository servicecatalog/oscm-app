/**
 * *****************************************************************************
 *
 * <p>Copyright FUJITSU LIMITED 2020
 *
 * <p>Creation Date: 11 Dec 2020
 *
 * <p>*****************************************************************************
 */

package org.oscm.app.vmware.importer;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.oscm.app.vmware.business.balancer.XMLHelper;
import org.oscm.app.vmware.parser.VLANParser;
import org.oscm.app.vmware.parser.model.VLAN;
import org.oscm.app.vmware.persistence.DataAccessService;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"javax.management.*", "javax.script.*", "jdk.internal.reflect.*"})
@PrepareForTest({VLANImporter.class, XMLHelper.class})
public class VLANImporterTest {

  private DataAccessService das;
  private VLANImporter vlanImporter;
  private VLAN vlan;
  private DataSource dataSource;
  private Connection connection;
  private PreparedStatement preparedStatement;
  private ResultSet resultSet;
  private VLANParser vlanParser;

  @Before
  public void setUpStreams() throws Exception {
    das = mock(DataAccessService.class);
    vlanImporter = PowerMockito.spy(new VLANImporter(das));
    dataSource = mock(DataSource.class);
    connection = mock(Connection.class);
    preparedStatement = mock(PreparedStatement.class);
    resultSet = mock(ResultSet.class);
    vlanParser = mock(VLANParser.class);
    vlan = new VLAN();
    vlan.vCenter = "vCenter";
    vlan.datacenter = "datacenter";
    vlan.cluster = "cluster";
    vlan.name = "name";
    vlan.gateway = "gateway";
    vlan.subnetMask = "subnetMask";
    vlan.dnsServer = "dnsServer";
    vlan.dnsSuffix = "dnsSuffix";
    vlan.enabled = "enabled";

    when(das.getDatasource()).thenReturn(dataSource);
    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
    when(preparedStatement.executeQuery()).thenReturn(resultSet);
  }

  @Test
  public void testSaveExecute() throws Exception {
    // given
    when(resultSet.next()).thenReturn(true);
    PowerMockito.mockStatic(XMLHelper.class);
    // when
    Whitebox.invokeMethod(vlanImporter, "save", vlan);
    // then
    verify(preparedStatement, times(1)).setString(1, "cluster");
    verify(preparedStatement, times(1)).setString(2, "datacenter");
    verify(preparedStatement, times(1)).execute();
  }

  @Test
  public void testGetClusterKey() throws Exception {
    // given
    when(resultSet.next()).thenReturn(true);
    // when
    int result = Whitebox.invokeMethod(vlanImporter, "getClusterKey", "", "", "");
    // then
    assertEquals(0, result);
  }

  @Test(expected = Exception.class)
  public void testGetClusterKeyThrowException() throws Exception {
    // when
    Whitebox.invokeMethod(vlanImporter, "getClusterKey", "", "", "");
  }

  @Test
  public void testLoad() throws Exception {
    // given
    PowerMockito.whenNew(VLANParser.class).withAnyArguments().thenReturn(vlanParser);
    when(vlanParser.readNextObject()).thenReturn(vlan, null);
    when(resultSet.next()).thenReturn(true);
    PowerMockito.mockStatic(XMLHelper.class);
    // when
    vlanImporter.load(any());
    // then
    PowerMockito.verifyPrivate(vlanImporter, times(1)).invoke("save", vlan);
    verify(preparedStatement, times(1)).setString(1, "cluster");
    verify(preparedStatement, times(1)).setString(2, "datacenter");
  }
}
