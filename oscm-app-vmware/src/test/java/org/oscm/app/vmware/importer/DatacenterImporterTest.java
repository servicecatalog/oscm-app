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
import org.oscm.app.vmware.parser.DatacenterParser;
import org.oscm.app.vmware.parser.model.Datacenter;
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
@PrepareForTest({DatacenterImporter.class, XMLHelper.class})
public class DatacenterImporterTest {

  private DataAccessService das;
  private DatacenterImporter datacenterImporter;
  private Datacenter datacenter;
  private DataSource dataSource;
  private Connection connection;
  private PreparedStatement preparedStatement;
  private ResultSet resultSet;
  private DatacenterParser datacenterParser;

  @Before
  public void setUpStreams() throws Exception {
    das = mock(DataAccessService.class);
    datacenterImporter = PowerMockito.spy(new DatacenterImporter(das));
    dataSource = mock(DataSource.class);
    connection = mock(Connection.class);
    preparedStatement = mock(PreparedStatement.class);
    resultSet = mock(ResultSet.class);
    datacenterParser = mock(DatacenterParser.class);

    when(das.getDatasource()).thenReturn(dataSource);
    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
    when(preparedStatement.executeQuery()).thenReturn(resultSet);
  }


  @Test
  public void testSaveExecute() throws Exception {
    // given
    datacenter = new Datacenter();
    datacenter.vCenter = "vCenter";
    datacenter.datacenter = "datacenter";
    datacenter.datacenterID = "datacenterID";
    when(resultSet.next()).thenReturn(true);
    PowerMockito.mockStatic(XMLHelper.class);
    // when
    Whitebox.invokeMethod(datacenterImporter, "save", datacenter);
    // then
    verify(preparedStatement, times(1)).setString(1, "datacenter");
    verify(preparedStatement, times(1)).setString(2, "datacenterID");
    verify(preparedStatement, times(1)).execute();
  }

  @Test
  public void testGetVCenterKey() throws Exception {
    // given
    when(resultSet.next()).thenReturn(true);
    // when
    int result = Whitebox.invokeMethod(datacenterImporter, "getVCenterKey", "");
    // then
    assertEquals(0, result);
  }

  @Test(expected = Exception.class)
  public void testGetVCenterKeyThrowException() throws Exception {
    // when
    Whitebox.invokeMethod(datacenterImporter, "getVCenterKey", "");
  }

  @Test
  public void testLoad() throws Exception {
    // given
    datacenter = new Datacenter();
    datacenter.vCenter = "vCenter";
    datacenter.datacenter = "datacenter";
    datacenter.datacenterID = "datacenterID";
    PowerMockito.whenNew(DatacenterParser.class).withAnyArguments().thenReturn(datacenterParser);
    when(datacenterParser.readNextObject()).thenReturn(datacenter, null);
    when(resultSet.next()).thenReturn(true);
    PowerMockito.mockStatic(XMLHelper.class);
    // when
    datacenterImporter.load(any());
    // then
    PowerMockito.verifyPrivate(datacenterImporter, times(1)).invoke("save", datacenter);
    verify(preparedStatement, times(1)).setString(1, "datacenter");
    verify(preparedStatement, times(1)).setString(2, "datacenterID");
  }
}
