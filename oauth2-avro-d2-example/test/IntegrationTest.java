/**
 * Copyright 2014 Thomas Feng
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;
import static play.test.Helpers.running;
import static play.test.Helpers.testServer;

import java.net.URL;
import java.util.function.Consumer;

import me.tfeng.play.plugins.AvroPlugin;

import org.apache.avro.AvroRemoteException;
import org.apache.avro.ipc.AsyncHttpTransceiver;
import org.apache.avro.ipc.HttpTransceiver;
import org.apache.avro.ipc.specific.SpecificRequestor;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import play.libs.Json;
import play.libs.ws.WS;
import play.libs.ws.WSResponse;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableMap;
import com.ning.http.client.AsyncHttpClient;

import controllers.protocols.Example;
import controllers.protocols.ExampleClient;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath*:spring/*.xml"})
public class IntegrationTest {

  private static class TransceiverWithAuthorization extends AsyncHttpTransceiver {

    private final String authorizationToken;

    public TransceiverWithAuthorization(URL url, String authorizationToken) {
      super(url);
      this.authorizationToken = authorizationToken;
    }

    @Override
    protected Consumer<AsyncHttpClient.BoundRequestBuilder> getRequestPreparer(URL url,
        byte[] body) {
      Consumer<AsyncHttpClient.BoundRequestBuilder> superConsumer =
          super.getRequestPreparer(url, body);
      return builder -> {
        superConsumer.accept(builder);
        builder.setHeader("Authorization", "Bearer " + authorizationToken);
      };
    }
  }

  private static final int TIMEOUT = 10000;

  @Value("${avro-d2-plugin.server-port}")
  private int port;

  @Value("${security.trusted.client.id}")
  private String trustedClientId;

  @Value("${security.trusted.client.secret}")
  private String trustedClientSecret;

  @Value("${security.untrusted.client.id}")
  private String untrustedClientId;

  @Value("${security.untrusted.client.secret}")
  private String untrustedClientSecret;

  @Value("${security.user.password}")
  private String userPassword;

  @Test
  public void testD2Request() {
    running(testServer(port), () -> {
      try {
        WSResponse response;

        response = authenticateClient(trustedClientId, trustedClientSecret);
        String clientAccessToken = response.asJson().findPath("accessToken").textValue();

        response = authenticateUser(clientAccessToken, "test", userPassword);
        String userAccessToken = response.asJson().findPath("accessToken").textValue();

        response = WS.url("http://localhost:" + port + "/proxy")
            .setQueryParameter("message", "Test Message through Client")
            .setHeader("Authorization", "Bearer " + userAccessToken).get().get(TIMEOUT);
        assertThat(response.getBody()).isEqualTo("Test Message through Client");
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }

  @Test
  public void testD2RequestMissingAuthorization() {
    running(testServer(port), () -> {
      try {
        WSResponse response = WS.url("http://localhost:" + port + "/proxy")
            .setQueryParameter("message", "Test Message through Client").get().get(TIMEOUT);
        assertThat(response.getStatus()).isEqualTo(401);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }

  @Test
  public void testD2RequestWrongAuthorization() {
    running(testServer(port), () -> {
      try {
        WSResponse response;

        response = authenticateClient(trustedClientId, trustedClientSecret);
        String clientAccessToken = response.asJson().findPath("accessToken").textValue();

        response = WS.url("http://localhost:" + port + "/proxy")
            .setQueryParameter("message", "Test Message through Client")
            .setHeader("Authorization", "Bearer " + clientAccessToken).get().get(TIMEOUT);
        assertThat(response.getStatus()).isEqualTo(401);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }

  @Test
  public void testDirectRequest() {
    running(testServer(port), () -> {
      try {
        WSResponse response;

        response = authenticateClient(trustedClientId, trustedClientSecret);
        String clientAccessToken = response.asJson().findPath("accessToken").textValue();

        response = authenticateUser(clientAccessToken, "test", userPassword);
        String userAccessToken = response.asJson().findPath("accessToken").textValue();

        TransceiverWithAuthorization transceiver =
            new TransceiverWithAuthorization(new URL("http://localhost:" + port + "/example"),
                userAccessToken);
        Example example = AvroPlugin.getInstance().client(Example.class, transceiver);
        assertThat(example.echo("Test Message").toString()).isEqualTo("Test Message");

        ExampleClient exampleClient = AvroPlugin.getInstance().client(ExampleClient.class, transceiver);
        assertThat(exampleClient.echo("Test Message").get(TIMEOUT).toString()).isEqualTo("Test Message");
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }

  @Test
  public void testDirectRequestMissingAuthorization() {
    running(testServer(port), () -> {
      try {
        HttpTransceiver transceiver =
            new HttpTransceiver(new URL("http://localhost:" + port + "/example"));
        Example example = SpecificRequestor.getClient(Example.class, transceiver);
        example.echo("Test Message");
        fail("AvroRemoteException is expected");
      } catch (AvroRemoteException e) {
        assertThat(e.getCause().getMessage())
            .isEqualTo("Server returned HTTP response code: 401 for URL: http://localhost:" + port
                + "/example");
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }

  @Test
  public void testDirectRequestWrongAuthorization() {
    running(testServer(port), () -> {
      try {
        WSResponse response = authenticateClient(trustedClientId, trustedClientSecret);
        String clientAccessToken = response.asJson().findPath("accessToken").textValue();

        TransceiverWithAuthorization transceiver =
            new TransceiverWithAuthorization(new URL("http://localhost:" + port + "/example"),
                clientAccessToken);
        Example example = AvroPlugin.getInstance().client(Example.class, transceiver);
        example.echo("Test Message");
        fail("AvroRemoteException is expected");
      } catch (AvroRemoteException e) {
        assertThat(e.getCause().getMessage())
            .isEqualTo("Server returned HTTP response code: 401 for URL: http://localhost:" + port
                + "/example");
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }

  @Test
  public void testUserAccessDeniedWithUntrustedClient() {
    running(testServer(port), () -> {
      WSResponse response;

      response = authenticateClient(untrustedClientId, untrustedClientSecret);
      String clientAccessToken = response.asJson().findPath("accessToken").textValue();

      response = authenticateUser(clientAccessToken, "test", userPassword);
      assertThat(response.getStatus()).isEqualTo(401);
    });
  }

  private WSResponse authenticateClient(String clientId, String clientSecret) {
    ObjectNode request = Json.newObject();
    request.putAll(ImmutableMap.of("clientId", request.textNode(clientId),
        "clientSecret", request.textNode(clientSecret)));
    return WS.url("http://localhost:" + port + "/client/authenticate").post(request).get(TIMEOUT);
  }

  private WSResponse authenticateUser(String clientAccessToken, String username, String password) {
    ObjectNode request = Json.newObject();
    request.putAll(ImmutableMap.of("username", request.textNode(username),
        "password", request.textNode(password)));
    return WS.url("http://localhost:" + port + "/user/authenticate")
        .setHeader("Authorization", "Bearer " + clientAccessToken).post(request).get(TIMEOUT);
  }
}
