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
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import me.tfeng.play.plugins.AvroPlugin;

import org.apache.avro.AvroRemoteException;
import org.apache.avro.Protocol;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.ipc.Ipc;
import org.apache.avro.ipc.generic.GenericRequestor;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import play.libs.F.Promise;

import com.google.common.collect.ImmutableList;

import controllers.protocols.Example;
import controllers.protocols.ExampleClient;
import controllers.protocols.KTooLargeError;
import controllers.protocols.Point;
import controllers.protocols.Points;
import controllers.protocols.PointsClient;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath*:spring/*.xml"})
public class IntegrationTest {

  private static final int TIMEOUT = 10000;

  @Test
  public void testExampleBinaryRequest() {
    running(testServer(3333), () -> {
      try {
        Example example =
            AvroPlugin.client(Example.class, new URL("http://localhost:3333/example"));
        assertThat(example.echo("Test Message").toString()).isEqualTo("Test Message");
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }

  @Test
  public void testExampleBinaryRequestAsync() {
    running(testServer(3333), () -> {
      try {
        ExampleClient example =
            AvroPlugin.client(ExampleClient.class, new URL("http://localhost:3333/example"));
        Promise<CharSequence> promise = example.echo("Test Message");
        assertThat(promise.get(TIMEOUT).toString()).isEqualTo("Test Message");
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
        assertThat(response.toString()).isEqualTo("Test Message");
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }

  @Test
  public void testPointsBinaryRequest() {
    running(testServer(3333), () -> {
      try {
        Points points = AvroPlugin.client(Points.class, new URL("http://localhost:3333/points"));
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
  public void testPointsBinaryRequestAsync() {
    running(testServer(3333), () -> {
      try {
        PointsClient points =
            AvroPlugin.client(PointsClient.class, new URL("http://localhost:3333/points"));
        Point center = Point.newBuilder().setX(0.0).setY(0.0).build();

        // []
        points.getNearestPoints(center, 1)
            .map(response -> fail("KTooLargeError is expected"))
            .recover(error -> {
              assertThat(error).isInstanceOf(KTooLargeError.class);
              assertThat(((KTooLargeError) error).getK()).isEqualTo(1);
              return null;
            })
            .get(TIMEOUT);

        // [one]
        Point one = Point.newBuilder().setX(1.0).setY(1.0).build();
        points.addPoint(one).get(TIMEOUT);
        points.getNearestPoints(center, 1)
            .map(response -> {
              return assertThat(response).isEqualTo(ImmutableList.of(one));
            })
            .get(TIMEOUT);
        points.getNearestPoints(center, 2)
            .map(response -> fail("KTooLargeError is expected"))
            .recover(error -> {
              assertThat(error).isInstanceOf(KTooLargeError.class);
              assertThat(((KTooLargeError) error).getK()).isEqualTo(2);
              return null;
            })
            .get(TIMEOUT);

        // [one, five]
        Point five = Point.newBuilder().setX(5.0).setY(5.0).build();
        points.addPoint(five).get(TIMEOUT);
        points.getNearestPoints(center, 1)
            .map(response -> {
              return assertThat(response).isEqualTo(ImmutableList.of(one));
            })
            .get(TIMEOUT);
        points.getNearestPoints(center, 2)
            .map(response -> {
              return assertThat(response).isEqualTo(ImmutableList.of(one, five));
            })
            .get(TIMEOUT);
        points.getNearestPoints(center, 3)
            .map(response -> fail("KTooLargeError is expected"))
            .recover(error -> {
              assertThat(error).isInstanceOf(KTooLargeError.class);
              assertThat(((KTooLargeError) error).getK()).isEqualTo(3);
              return null;
            })
            .get(TIMEOUT);

        // [one, five, five]
        points.addPoint(five).get(TIMEOUT);
        points.getNearestPoints(center, 1)
            .map(response -> {
              return assertThat(response).isEqualTo(ImmutableList.of(one));
            })
            .get(TIMEOUT);
        points.getNearestPoints(center, 2)
            .map(response -> {
              return assertThat(response).isEqualTo(ImmutableList.of(one, five));
            })
            .get(TIMEOUT);
        points.getNearestPoints(center, 3)
            .map(response -> {
              return assertThat(response).isEqualTo(ImmutableList.of(one, five, five));
            })
            .get(TIMEOUT);
        points.getNearestPoints(center, 4)
            .map(response -> fail("KTooLargeError is expected"))
            .recover(error -> {
              assertThat(error).isInstanceOf(KTooLargeError.class);
              assertThat(((KTooLargeError) error).getK()).isEqualTo(4);
              return null;
            })
            .get(TIMEOUT);

        // []
        points.clear().get(TIMEOUT);
        points.getNearestPoints(center, 1)
            .map(response -> fail("KTooLargeError is expected"))
            .recover(error -> {
              assertThat(error).isInstanceOf(KTooLargeError.class);
              assertThat(((KTooLargeError) error).getK()).isEqualTo(1);
              return null;
            })
            .get(TIMEOUT);
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
