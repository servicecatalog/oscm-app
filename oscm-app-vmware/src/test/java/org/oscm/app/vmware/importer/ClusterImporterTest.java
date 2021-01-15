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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.oscm.app.vmware.business.balancer.XMLHelper;
import org.oscm.app.vmware.parser.model.Cluster;
import org.oscm.app.vmware.persistence.DataAccessService;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import org.xml.sax.InputSource;

import javax.sql.DataSource;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.*;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"javax.management.*", "javax.script.*", "jdk.internal.reflect.*"})
@PrepareForTest({ClusterImporter.class, XMLHelper.class})
public class ClusterImporterTest {

  final String testXML = ""
      + "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>"
      + "<ess:essvcenter xmlns:ess=\"http://oscm.org/xsd\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://oscm.org/xsd ../../oscm-app-vmware\\resources\\XSD\\Loadbalancer_schema.xsd\">"
      + "<balancer class=\"org.oscm.app.vmware.business.balancer.EquipartitionHostBalancer\" cpuWeight=\"0.5\" memoryWeight=\"1\" vmWeight=\"1\"/>"
      + "<host enabled=\"true\" name=\"estvmwdev1.intern.est.fujitsu.com\">"
      + "<balancer class=\"org.oscm.app.vmware.business.balancer.EquipartitionStorageBalancer\" storage=\"VMdev0,VMdev1\"/>"
      + "</host>"
      + "<host enabled=\"true\" name=\"estvmwdev2.intern.est.fujitsu.com\">"
      + "<balancer class=\"org.oscm.app.vmware.business.balancer.EquipartitionStorageBalancer\" storage=\"VMdev0,VMdev1\"/>"
      + "</host>"
      + "<storage enabled=\"true\" limit=\"85%\" name=\"VMdev0\"/>"
      + "<storage enabled=\"true\" limit=\"85%\" name=\"VMdev1\"/>"
      + "</ess:essvcenter>";

  final String testWrongXML = ""
      + "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>"
      + "<ess:essvcenter xmlns:ess=\"http://oscm.org/xsd\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://oscm.org/xsd ../../oscm-app-vmware\\resources\\XSD\\Loadbalancer_schema.xsd\">"
      + "<balancer class=\"org.oscm.app.vmware.business.balancer.EquipartitionHostBalancer\" cpuWeight=\"0.5\" memoryWeight=\"1\" vmWeight=\"1\"/>"
      + "<host enabled=\"true\" name=\"estvmwdev1.intern.est.fujitsu.com\">"
      + "<balancer class=\"org.oscm.app.vmware.business.balancer.EquipartitionStorageBalancer\" storage=\"VMdev0,VMdev1\"/>"
      + "</host>"
      + "<host enabled=\"true\" name=\"estvmwdev2.intern.est.fujitsu.com\">"
      + "<balancer class=\"org.oscm.app.vmware.business.balancer.EquipartitionStorageBalancer\" storage=\"VMdev0,VMdev1\"/>"
      + "</host>"
      + "<storage enabled=\"true\" limit=\"85%\" name=\"VMdev0\"/>"
      + "<storage enabled=\"true\" limit=\"85%\"/>" + "</ess:essvcenter>";

  private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
  private final PrintStream originalErr = System.err;
  private DataAccessService das;
  private ClusterImporter clusterImporter;
  private Cluster cluster;
  private DataSource dataSource;
  private Connection connection;
  private PreparedStatement preparedStatement;
  private ResultSet resultSet;

  @Before
  public void setUpStreams() throws Exception {
    das = mock(DataAccessService.class);
    clusterImporter = PowerMockito.spy(new ClusterImporter(das));
    dataSource = mock(DataSource.class);
    connection = mock(Connection.class);
    preparedStatement = mock(PreparedStatement.class);
    resultSet = mock(ResultSet.class);

    when(das.getDatasource()).thenReturn(dataSource);
    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
    when(preparedStatement.executeQuery()).thenReturn(resultSet);
    System.setErr(new PrintStream(errContent));
    Locale.setDefault(Locale.ENGLISH);
  }

  @After
  public void restoreStreams() {
    System.setErr(originalErr);
  }

  private File getSource(String filename) throws Exception {
    ClassLoader loader = Thread.currentThread().getContextClassLoader();
    URL url = loader.getResource("META-INF/" + filename);
    File file = new File(url.toURI());
    return file;
  }

  @Test
  public void testValidSchema() throws Exception {
    // given
    File file = getSource("Loadbalancer_schema.xsd");
    String constant = XMLConstants.W3C_XML_SCHEMA_NS_URI;
    SchemaFactory xsdFactory = SchemaFactory.newInstance(constant);
    Schema schema = xsdFactory.newSchema(file);
    DocumentBuilderFactory dfactory = DocumentBuilderFactory.newInstance();
    dfactory.setNamespaceAware(true);
    // when
    dfactory.setValidating(false);
    dfactory.setIgnoringElementContentWhitespace(true);
    dfactory.setSchema(schema);
    DocumentBuilder builder = dfactory.newDocumentBuilder();
    builder.parse(new InputSource(new StringReader(testXML)));
  }

  @Test
  public void testInvalidSchema() throws Exception {
    // given
    File file = getSource("Loadbalancer_schema.xsd");
    String constant = XMLConstants.W3C_XML_SCHEMA_NS_URI;
    SchemaFactory xsdFactory = SchemaFactory.newInstance(constant);
    Schema schema = xsdFactory.newSchema(file);
    DocumentBuilderFactory dfactory = DocumentBuilderFactory.newInstance();
    dfactory.setNamespaceAware(true);
    // when
    dfactory.setValidating(false);
    dfactory.setIgnoringElementContentWhitespace(true);
    dfactory.setSchema(schema);
    DocumentBuilder builder = dfactory.newDocumentBuilder();
    builder.parse(new InputSource(new StringReader(testWrongXML)));
    // then
    assertTrue(errContent.toString()
        .contains("Attribute 'name' must appear on element 'storage'"));
  }

  @Test
  public void testSaveExecute() throws Exception {
    // given
    cluster = new Cluster();
    cluster.vCenter = "vCenter";
    cluster.datacenter = "datacenter";
    cluster.clusterName = "clusterName";
    cluster.loadBalancer = "loadBalancer";
    when(resultSet.next()).thenReturn(true);
    PowerMockito.mockStatic(XMLHelper.class);
    // when
    Whitebox.invokeMethod(clusterImporter, "save", cluster);
    // then
    verify(preparedStatement, times(1)).setString(1, "clusterName");
    verify(preparedStatement, times(1)).setString(2, "loadBalancer");
    verify(preparedStatement, times(1)).execute();
  }

  @Test(expected = Exception.class)
  public void testSaveExecuteThrowException() throws Exception {
    // given
    cluster = new Cluster();
    when(resultSet.next()).thenReturn(true);
    // when
    Whitebox.invokeMethod(clusterImporter, "save", cluster);
  }

  @Test
  public void testGetDatacenterKey() throws Exception {
    // given
    when(resultSet.next()).thenReturn(true);
    // when
    int result = Whitebox.invokeMethod(clusterImporter, "getDatacenterKey", "", "");
    // then
    assertEquals(0, result);
  }

  @Test(expected = Exception.class)
  public void testGetDatacenterKeyThrowException() throws Exception {
    // when
    Whitebox.invokeMethod(clusterImporter, "getDatacenterKey", "", "");
  }

  @Test
  public void testLoad() throws Exception {
    // given
    final File initialFile = new File("resources/csv/cluster.csv");
    final InputStream csvFile =
        new DataInputStream(new FileInputStream(initialFile));
    when(resultSet.next()).thenReturn(true);
    // when
    clusterImporter.load(csvFile);
    // then
    PowerMockito.verifyPrivate(clusterImporter, times(1)).invoke("save", any());
    verify(preparedStatement, times(1)).setString(1, "EST");
    verify(preparedStatement, times(1)).setString(2, "estvcsadev.intern.est.fujitsu.com");
  }
}
