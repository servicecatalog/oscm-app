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
import org.oscm.app.vmware.parser.model.Portgroup;
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
@PrepareForTest({PortgroupImporter.class, XMLHelper.class})
public class PortgroupImporterTest {

  private DataAccessService das;
  private PortgroupImporter portgroupImporter;
  private DataSource dataSource;
  private Connection connection;
  private PreparedStatement preparedStatement;
  private ResultSet resultSet;
  private Portgroup portgroup;

  @Before
  public void setUpStreams() throws Exception {
    das = mock(DataAccessService.class);
    portgroupImporter = PowerMockito.spy(new PortgroupImporter(das));
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
    portgroup = new Portgroup();
    portgroup.vCenter = "vCenter";
    portgroup.datacenter = "datacenter";
    portgroup.cluster = "cluster";
    portgroup.distributedVirtualSwitch = "distributedVirtualSwitch";
    portgroup.uuid = "uuid";
    portgroup.name = "name";
    when(resultSet.next()).thenReturn(true);
    PowerMockito.mockStatic(XMLHelper.class);
    // when
    Whitebox.invokeMethod(portgroupImporter, "save", portgroup);
    // then
    verify(preparedStatement, times(1)).setString(1, "uuid");
    verify(preparedStatement, times(1)).setString(2, "name");
    verify(preparedStatement, times(1)).execute();
  }

  @Test
  public void testGetDVSKey() throws Exception {
    // given
    when(resultSet.next()).thenReturn(true);
    // when
    int result = Whitebox.invokeMethod(portgroupImporter, "getDVSKey", "", "", "", "");
    // then
    assertEquals(0, result);
  }

  @Test(expected = Exception.class)
  public void testGetDVSKeyThrowException() throws Exception {
    // when
    Whitebox.invokeMethod(portgroupImporter, "getDVSKey", "", "");
  }

  @Test
  public void testLoad() throws Exception {
    // given
    final File initialFile = new File("resources/csv/portgroup.csv");
    final InputStream csvFile =
        new DataInputStream(new FileInputStream(initialFile));
    when(resultSet.next()).thenReturn(true);
    PowerMockito.mockStatic(XMLHelper.class);
    // when
    portgroupImporter.load(csvFile);
    // then
    PowerMockito.verifyPrivate(portgroupImporter, times(1)).invoke("save", any());
    verify(preparedStatement, times(1)).setString(1, "DSwitch 1");
    verify(preparedStatement, times(1)).setString(2, "esscluster");
  }
}
