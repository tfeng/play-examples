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

package beans;

import java.io.ByteArrayOutputStream;

import kafka.javaapi.producer.Producer;
import kafka.producer.KeyedMessage;
import me.tfeng.play.plugins.KafkaPlugin;

import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Component;

import play.Logger;
import play.Logger.ALogger;
import controllers.protocols.Message;
import controllers.protocols.UserMessage;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
@Component
public class MessageImpl implements Message {

  private static final ALogger LOG = Logger.of(MessageImpl.class);

  private final EncoderFactory encoderFactory = EncoderFactory.get();

  private final SpecificDatumWriter<UserMessage> eventWriter =
      new SpecificDatumWriter<>(UserMessage.SCHEMA$);

  private Producer<byte[], byte[]> producer;

  @Override
  public void send(UserMessage message) {
    LOG.info("Sending message to Kafka: " + message);

    try {
      if (producer == null) {
        producer = KafkaPlugin.getInstance().createProducer();
      }

      byte[] key = message.getSubject().getBytes("UTF-8");

      ByteArrayOutputStream stream = new ByteArrayOutputStream();
      BinaryEncoder binaryEncoder = encoderFactory.binaryEncoder(stream, null);
      eventWriter.write(message, binaryEncoder);
      binaryEncoder.flush();
      IOUtils.closeQuietly(stream);
      byte[] value = stream.toByteArray();

      producer.send(new KeyedMessage<byte[], byte[]>("kafka-example", key, value));
    } catch (Exception e) {
      throw new RuntimeException("Unable to send Kafka event for message: " + message, e);
    }
  }
}
