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
import org.oscm.app.vmware.parser.model.PortgroupIPPool;
import org.oscm.app.vmware.persistence.DataAccessService;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import javax.sql.DataSource;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"javax.management.*", "javax.script.*", "jdk.internal.reflect.*"})
@PrepareForTest({PortgroupIPPoolImporter.class, XMLHelper.class})
public class PortgroupIPPoolImporterTest {

  private DataAccessService das;
  private PortgroupIPPoolImporter portgroupIPPoolImporter;
  private DataSource dataSource;
  private Connection connection;
  private PreparedStatement preparedStatement;
  private ResultSet resultSet;
  private PortgroupIPPool portgroupIPPool;

  @Before
  public void setUpStreams() throws Exception {
    das = mock(DataAccessService.class);
    portgroupIPPoolImporter = PowerMockito.spy(new PortgroupIPPoolImporter(das));
    dataSource = mock(DataSource.class);
    connection = mock(Connection.class);
    preparedStatement = mock(PreparedStatement.class);
    resultSet = mock(ResultSet.class);

    when(das.getDatasource()).thenReturn(dataSource);
    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
    when(preparedStatement.executeQuery()).thenReturn(resultSet);
  }


  @Test
  public void testSaveExecute() throws Exception {
    // given
    portgroupIPPool = new PortgroupIPPool();
    portgroupIPPool.vCenter = "vCenter";
    portgroupIPPool.datacenter = "datacenter";
    portgroupIPPool.cluster = "cluster";
    portgroupIPPool.distributedVirtualSwitch = "distributedVirtualSwitch";
    portgroupIPPool.portgroup = "portgroup";
    when(resultSet.next()).thenReturn(true);
    PowerMockito.mockStatic(XMLHelper.class);
    // when
    Whitebox.invokeMethod(portgroupIPPoolImporter, "save", portgroupIPPool);
    // then
    verify(preparedStatement, times(1)).setString(1, "portgroup");
    verify(preparedStatement, times(1)).setString(2, "distributedVirtualSwitch");
    verify(preparedStatement, times(1)).execute();
  }

  @Test
  public void testGetPortgroupTKey() throws Exception {
    // given
    when(resultSet.next()).thenReturn(true);
    // when
    int result = Whitebox.invokeMethod(portgroupIPPoolImporter, "getPortgroupTKey", "", "", "", "", "");
    // then
    assertEquals(0, result);
  }

  @Test(expected = Exception.class)
  public void testGetPortgroupTKeyThrowException() throws Exception {
    // when
    Whitebox.invokeMethod(portgroupIPPoolImporter, "getPortgroupTKey", "", "", "", "", "");
  }

  @Test
  public void testLoad() throws Exception {
    // given
    final File initialFile = new File("resources/csv/portgroupippool.csv");
    final InputStream csvFile =
        new DataInputStream(new FileInputStream(initialFile));
    when(resultSet.next()).thenReturn(true);
    // when
    portgroupIPPoolImporter.load(csvFile);
    // then
    PowerMockito.verifyPrivate(portgroupIPPoolImporter, times(5)).invoke("save", any());
    verify(preparedStatement, times(5)).setString(1, "DBPortGroup");
    verify(preparedStatement, times(5)).setString(2, "DSwitch 1");
  }
}
