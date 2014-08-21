import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;
import static play.test.Helpers.running;
import static play.test.Helpers.testServer;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.apache.avro.AvroRemoteException;
import org.apache.avro.Protocol;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.ipc.HttpTransceiver;
import org.apache.avro.ipc.Ipc;
import org.apache.avro.ipc.generic.GenericRequestor;
import org.apache.avro.ipc.specific.SpecificRequestor;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.google.common.collect.ImmutableList;

import controllers.protocols.Example;
import controllers.protocols.KTooLargeError;
import controllers.protocols.Point;
import controllers.protocols.Points;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath*:spring/*.xml"})
public class IntegrationTest {

  @Test
  public void testExampleBinaryRequest() {
    running(testServer(3333), () -> {
      try {
        HttpTransceiver transceiver = new HttpTransceiver(new URL("http://localhost:3333/example"));
        Example example = SpecificRequestor.getClient(Example.class, transceiver);
        assertThat(example.echo("Test Message")).isEqualTo("Test Message");
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }

  @Test
  public void testExampleJsonRequest() {
    running(testServer(3333), () -> {
      try {
        Object response =
            sendJsonRequest("http://localhost:3333/example", Example.PROTOCOL, "echo",
                "{\"message\": \"Test Message\"}");
        assertThat(response).isEqualTo("Test Message");
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }

  @Test
  public void testPointsBinaryRequest() {
    running(testServer(3333), () -> {
      try {
        HttpTransceiver transceiver = new HttpTransceiver(new URL("http://localhost:3333/points"));
        Points points = SpecificRequestor.getClient(Points.class, transceiver);
        Point center = Point.newBuilder().setX(0.0).setY(0.0).build();

        // []
        try {
          points.getNearestPoints(center, 1);
          fail("KTooLargeError is expected");
        } catch (KTooLargeError e) {
          assertThat(e.getK()).isEqualTo(1);
        }

        // [one]
        Point one = Point.newBuilder().setX(1.0).setY(1.0).build();
        points.addPoint(one);
        assertThat(points.getNearestPoints(center, 1)).isEqualTo(ImmutableList.of(one));
        try {
          points.getNearestPoints(center, 2);
          fail("KTooLargeError is expected");
        } catch (KTooLargeError e) {
          assertThat(e.getK()).isEqualTo(2);
        }

        // [one, five]
        Point five = Point.newBuilder().setX(5.0).setY(5.0).build();
        points.addPoint(five);
        assertThat(points.getNearestPoints(center, 1)).isEqualTo(ImmutableList.of(one));
        assertThat(points.getNearestPoints(center, 2)).isEqualTo(ImmutableList.of(one, five));
        try {
          points.getNearestPoints(center, 3);
          fail("KTooLargeError is expected");
        } catch (KTooLargeError e) {
          assertThat(e.getK()).isEqualTo(3);
        }

        // [one, five, five]
        points.addPoint(five);
        assertThat(points.getNearestPoints(center, 1)).isEqualTo(ImmutableList.of(one));
        assertThat(points.getNearestPoints(center, 2)).isEqualTo(ImmutableList.of(one, five));
        assertThat(points.getNearestPoints(center, 3)).isEqualTo(ImmutableList.of(one, five, five));
        try {
          points.getNearestPoints(center, 4);
          fail("KTooLargeError is expected");
        } catch (KTooLargeError e) {
          assertThat(e.getK()).isEqualTo(4);
        }

        // []
        points.clear();
        try {
          points.getNearestPoints(center, 1);
          fail("KTooLargeError is expected");
        } catch (KTooLargeError e) {
          assertThat(e.getK()).isEqualTo(1);
        }
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }

  @Test
  public void testPointsJsonRequest() {
    running(testServer(3333), () -> {
      try {
        String url = "http://localhost:3333/points";
        GenericData.Record record = new GenericData.Record(Points.PROTOCOL.getMessages()
            .get("getNearestPoints").getErrors().getTypes().get(1));
        Object response;

        // []
        try {
          response = sendJsonRequest(url, Points.PROTOCOL, "getNearestPoints",
              "{\"from\": {\"x\": 0.0, \"y\": 0.0}, \"k\": 1}");
          fail("AvroRemoteException is expected");
        } catch (AvroRemoteException e) {
          record.put("k", 1);
          assertThat(e.getValue()).isEqualTo(record);
        }

        // [one]
        Point one = Point.newBuilder().setX(1.0).setY(1.0).build();
        sendJsonRequest(url, Points.PROTOCOL, "addPoint", "{\"point\": {\"x\": 1.0, \"y\": 1.0}}");
        response = sendJsonRequest(url, Points.PROTOCOL, "getNearestPoints",
            "{\"from\": {\"x\": 0.0, \"y\": 0.0}, \"k\": 1}");
        assertThat(response.toString()).isEqualTo(ImmutableList.of(one).toString());
        try {
          response = sendJsonRequest(url, Points.PROTOCOL, "getNearestPoints",
              "{\"from\": {\"x\": 0.0, \"y\": 0.0}, \"k\": 2}");
          fail("AvroRemoteException is expected");
        } catch (AvroRemoteException e) {
          record.put("k", 2);
          assertThat(e.getValue()).isEqualTo(record);
        }

        // [one, five]
        Point five = Point.newBuilder().setX(5.0).setY(5.0).build();
        sendJsonRequest(url, Points.PROTOCOL, "addPoint", "{\"point\": {\"x\": 5.0, \"y\": 5.0}}");
        response = sendJsonRequest(url, Points.PROTOCOL, "getNearestPoints",
            "{\"from\": {\"x\": 0.0, \"y\": 0.0}, \"k\": 1}");
        assertThat(response.toString()).isEqualTo(ImmutableList.of(one).toString());
        response = sendJsonRequest(url, Points.PROTOCOL, "getNearestPoints",
            "{\"from\": {\"x\": 0.0, \"y\": 0.0}, \"k\": 2}");
        assertThat(response.toString()).isEqualTo(ImmutableList.of(one, five).toString());
        try {
          response = sendJsonRequest(url, Points.PROTOCOL, "getNearestPoints",
              "{\"from\": {\"x\": 0.0, \"y\": 0.0}, \"k\": 3}");
          fail("AvroRemoteException is expected");
        } catch (AvroRemoteException e) {
          record.put("k", 3);
          assertThat(e.getValue()).isEqualTo(record);
        }

        // [one, five, five]
        sendJsonRequest(url, Points.PROTOCOL, "addPoint", "{\"point\": {\"x\": 5.0, \"y\": 5.0}}");
        response = sendJsonRequest(url, Points.PROTOCOL, "getNearestPoints",
            "{\"from\": {\"x\": 0.0, \"y\": 0.0}, \"k\": 1}");
        assertThat(response.toString()).isEqualTo(ImmutableList.of(one).toString());
        response = sendJsonRequest(url, Points.PROTOCOL, "getNearestPoints",
            "{\"from\": {\"x\": 0.0, \"y\": 0.0}, \"k\": 2}");
        assertThat(response.toString()).isEqualTo(ImmutableList.of(one, five).toString());
        response = sendJsonRequest(url, Points.PROTOCOL, "getNearestPoints",
            "{\"from\": {\"x\": 0.0, \"y\": 0.0}, \"k\": 3}");
        assertThat(response.toString()).isEqualTo(ImmutableList.of(one, five, five).toString());
        try {
          response = sendJsonRequest(url, Points.PROTOCOL, "getNearestPoints",
              "{\"from\": {\"x\": 0.0, \"y\": 0.0}, \"k\": 4}");
          fail("AvroRemoteException is expected");
        } catch (AvroRemoteException e) {
          record.put("k", 4);
          assertThat(e.getValue()).isEqualTo(record);
        }

        // []
        sendJsonRequest(url, Points.PROTOCOL, "clear", "");
        try {
          response = sendJsonRequest(url, Points.PROTOCOL, "getNearestPoints",
              "{\"from\": {\"x\": 0.0, \"y\": 0.0}, \"k\": 1}");
          fail("AvroRemoteException is expected");
        } catch (AvroRemoteException e) {
          record.put("k", 1);
          assertThat(e.getValue()).isEqualTo(record);
        }
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }

  private Object sendJsonRequest(String url, Protocol protocol, String message, String data)
      throws URISyntaxException, IOException {
    URI uri = new URL(url).toURI();
    Schema schema = protocol.getMessages().get(message).getRequest();
    GenericRequestor client = new GenericRequestor(protocol, Ipc.createTransceiver(uri));
    GenericDatumReader<Object> reader = new GenericDatumReader<Object>(schema);
    Object request = reader.read(null, DecoderFactory.get().jsonDecoder(schema, data));
    return client.request(message, request);
  }
}
