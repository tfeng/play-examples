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

import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.List;

import me.tfeng.play.http.PostRequestPreparer;
import me.tfeng.play.plugins.AvroPlugin;
import me.tfeng.play.spring.test.AbstractSpringTest;

import org.apache.avro.AvroRuntimeException;
import org.apache.avro.ipc.AsyncHttpTransceiver;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;

import play.libs.F.Promise;
import play.libs.Json;
import play.libs.ws.WS;
import play.libs.ws.WSResponse;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableMap;

import controllers.protocols.Example;
import controllers.protocols.ExampleClient;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
public class IntegrationTest extends AbstractSpringTest {

  private static class TransceiverWithAuthorization extends AsyncHttpTransceiver {

    private final String authorizationToken;

    public TransceiverWithAuthorization(URL url, String authorizationToken) {
      super(url);
      this.authorizationToken = authorizationToken;
    }

    @Override
    public Promise<List<ByteBuffer>> asyncTransceive(List<ByteBuffer> request,
        PostRequestPreparer postRequestPreparer) {
      return super.asyncTransceive(request, (builder, contentType, url) -> {
        if (postRequestPreparer != null) {
          postRequestPreparer.prepare(builder, contentType, url);
        }
        builder.setHeader("Authorization", "Bearer " + authorizationToken);
      });
    }

    @Override
    public List<ByteBuffer> transceive(List<ByteBuffer> request,
        PostRequestPreparer postRequestPreparer) throws IOException {
      return super.transceive(request, (builder, contentType, url) -> {
        if (postRequestPreparer != null) {
          postRequestPreparer.prepare(builder, contentType, url);
        }
        builder.setHeader("Authorization", "Bearer " + authorizationToken);
      });
    }
  }

  private static final int TIMEOUT = Integer.MAX_VALUE;

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
        Example example = AvroPlugin.client(Example.class, transceiver);
        assertThat(example.echo("Test Message")).isEqualTo("Test Message");

        ExampleClient exampleClient = AvroPlugin.client(ExampleClient.class, transceiver);
        assertThat(exampleClient.echo("Test Message").get(TIMEOUT)).isEqualTo("Test Message");
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }

  @Test
  public void testDirectRequestMissingAuthorization() {
    running(testServer(port), () -> {
      try {
        URL url = new URL("http://localhost:" + port + "/example");
        Example example = AvroPlugin.client(Example.class, url);
        example.echo("Test Message");
        fail("AvroRuntimeException is expected");
      } catch (AvroRuntimeException e) {
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
        Example example = AvroPlugin.client(Example.class, transceiver);
        example.echo("Test Message");
        fail("AvroRuntimeException is expected");
      } catch (AvroRuntimeException e) {
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
