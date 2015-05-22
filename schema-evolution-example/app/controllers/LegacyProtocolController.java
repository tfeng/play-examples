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

package controllers;

import java.io.IOException;

import org.apache.avro.Protocol;
import org.apache.avro.Schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import me.tfeng.play.avro.AvroHelper;
import me.tfeng.play.avro.d2.AvroD2Client;
import play.Logger;
import play.Logger.ALogger;
import play.libs.F.Promise;
import play.mvc.Result;
import play.mvc.Results;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
public class LegacyProtocolController {

  private static final JsonNodeFactory JSON_NODE_FACTORY = new JsonNodeFactory(false);
  private static final AvroD2Client LEGACY_CLIENT;
  private static final Protocol LEGACY_PROTOCOL;
  private static final ALogger LOG = Logger.of(LegacyProtocolController.class);

  static {
    try {
      LEGACY_PROTOCOL = Protocol.parse(LegacyProtocolController.class.getResourceAsStream(
          "/legacy/employee_registry.avpr"));
      LEGACY_CLIENT = new AvroD2Client(LEGACY_PROTOCOL);
      LEGACY_CLIENT.setGeneric(true);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static Promise<Result> addEmployee(String firstName, String lastName)
      throws Exception {
    Schema requestSchema = LEGACY_PROTOCOL.getMessages().get("addEmployee").getRequest();

    Schema employeeSchema = requestSchema.getField("employee").schema();
    ObjectNode employeeValue = new ObjectNode(JSON_NODE_FACTORY);
    employeeValue.put("firstName", firstName);
    employeeValue.put("lastName", lastName);

    JsonNode record = AvroHelper.convertFromSimpleRecord(employeeSchema, employeeValue);
    Object[] args =
        new Object[] {AvroHelper.createGenericRequestFromRecord(employeeSchema, record)};
    return invoke("addEmployee", args);
  }

  public static Promise<Result> countEmployees() throws Exception {
    Object[] args = new Object[0];
    return invoke("countEmployees", args);
  }

  public static Promise<Result> getEmployees(long managerId) throws Exception {
    Object[] args = new Object[] {managerId};
    return invoke("getEmployees", args);
  }

  public static Promise<Result> getManager(long employeeId) throws Exception {
    Object[] args = new Object[] {employeeId};
    return invoke("getManager", args);
  }

  public static Promise<Result> makeManager(long managerId, long employeeId) throws Exception {
    Object[] args = new Object[] {managerId, employeeId};
    return invoke("makeManager", args);
  }

  public static Promise<Result> removeEmployee(long employeeId) throws Exception {
    Object[] args = new Object[] {employeeId};
    return invoke("removeEmployee", args);
  }

  private static Promise<Result> invoke(String method, Object[] args) throws Exception {
    return LEGACY_CLIENT.request(method, args)
        .<Result>map(result -> Results.ok(String.valueOf(result)))
        .recover(e -> {
          try {
            LOG.warn("Exception thrown while processing request; returning bad request", e);
            return Results.badRequest(e.getLocalizedMessage());
          } catch (Exception e2) {
            throw e;
          }
        });
  }
}

