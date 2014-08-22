import static org.fest.assertions.Assertions.assertThat;
import static play.test.Helpers.running;
import static play.test.Helpers.testServer;

import java.net.URL;

import org.apache.avro.ipc.HttpTransceiver;
import org.apache.avro.ipc.specific.SpecificRequestor;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import play.libs.ws.WS;
import play.libs.ws.WSResponse;
import controllers.protocols.Example;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath*:spring/*.xml"})
public class IntegrationTest {

  private static final int TIMEOUT = 10000;

  @Value("${avro-d2-plugin.server-port}")
  private int port;

  @Test
  public void testD2Request() {
    running(testServer(port), () -> {
      try {
        WSResponse response = WS.url("http://localhost:" + port + "/client")
            .setQueryParameter("message", "Test Message through Client").get().get(TIMEOUT);
        assertThat(response.getBody()).isEqualTo("Test Message through Client");
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }

  @Test
  public void testDirectRequest() {
    running(testServer(port), () -> {
      try {
        HttpTransceiver transceiver =
            new HttpTransceiver(new URL("http://localhost:" + port + "/example"));
        Example example = SpecificRequestor.getClient(Example.class, transceiver);
        assertThat(example.echo("Test Message")).isEqualTo("Test Message");
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }
}
