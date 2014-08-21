import static org.fest.assertions.Assertions.assertThat;
import static play.test.Helpers.running;
import static play.test.Helpers.testServer;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import play.libs.ws.WS;
import play.libs.ws.WSResponse;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath*:spring/*.xml"})
public class IntegrationTest {

  private static final int TIMEOUT = 10000;

  @Test
  public void testCustomName() {
    running(testServer(3333), () -> {
      WSResponse response = WS.url("http://localhost:3333").setQueryParameter("name", "Amy").get().get(TIMEOUT);
      assertThat(response.getBody()).isEqualTo("Hello, Amy!");
    });
  }

  @Test
  public void testDefaultName() {
    running(testServer(3333), () -> {
      WSResponse response = WS.url("http://localhost:3333").get().get(TIMEOUT);
      assertThat(response.getBody()).isEqualTo("Hello, Thomas!");
    });
  }
}
