schema-evolution-example
=========

An example to demonstrate how schema evolution is handled between a client and a service in [Avro IPC](http://avro.apache.org/docs/current/spec.html#Protocol+Declaration).

---

#### Design

[Avro specification](https://avro.apache.org/docs/current/spec.html) defines how schemas may evolve in a backward-compatible way. If the change from an old version of the schema to a new version is backward compatible, one may use the new version of the schema to decode the data encoded with the old version.

This also applies to Avro IPC (inter-process communication), where the request and response between a client and a server are encoded in Avro records. As long as the server can decode the request from the client and can encode the responde in a format that the client can understand, then proper communication may happen between them, even if they are using different versions of the schema.

##### ZooKeeper

The design of schema evolution in IPC leverages the infrastructure underlying the [Play Avro D2 plugin](https://github.com/tfeng/play-plugins/tree/master/avro-d2-plugin), an example of which can be found [here](https://github.com/tfeng/play-examples/tree/master/avro-d2-example). To recap, Avro D2 utilizes [ZooKeeper](http://zookeeper.apache.org/) as a service registry, which allows clients to dynamically discover (hence the acronym "D2") addresses of a server that can service a request. In the scenario of schema evolution, ZooKeeper is also utilized as the schema repository, where different versions of a schema may be stored and be referenced in a concise way (by using the full name of the schema as well as the MD5 of the schema itself).

##### Request flow

Overall, the idea can be captured in this flow of operations:

1. When the server starts, it registers itself in the server registry in ZooKeeper for potential clients to discover it. Technically, it stores the URI to its service endpoint(s) in ```/protocols/<protocol>/servers/<id>```, where ```<protocol>``` is the full name of the protocol that one of its endpoint supports, and ```<id>``` is a system-generated identifier (which has no importance). It also stores tne schema of the current version of the protocol in ```/protocols/<protocol>/versions/<md5>```, where ```<protocol>``` is the same full name of the protocol, and ```<md5>``` is the MD5 checksum of the current version of that protocol.
2. Before a client makes a request to the server, it stores its own version of the protocol in ```/protocols/<protocol>/versions/<md5>``` in ZooKeeper. This version need not be the same as the version that the server is using.
3. When the client sends the request, the MD5 of the client protocol is encoded in the [handshake request](http://avro.apache.org/docs/current/spec.html#handshake) of the message.
4. When the server receives the request, it reads the MD5 of the client protocol and compares it against the MD5 of its own protocol. If the two do not match, the server loads the protocol from ZooKeeper and cache it locally thereafter. The server would then decode the request with the client protocol and try to upgrade it to its version. Assuming the server protocol iss more recent and it is backward compatible with the client protocol, this upgrade will be successful.
5. The server processes the request and produces a response.
6. If the client protocol is different, the server also downgrades the response before it is sent back to the client.
7. In the response sent back to the client, there is a handshake response at the beginning of the message. It informs the client of the server protocol, which the client may or may not use in the future.

##### Benefit

The benefit of this approach tois that the client and server may continue to use their own versions of the protocol indefinitely, assuming all new version of the protocol is backward compatible with the previous version. There is no need to synchronize the release of the client and the server in order to ensure they communicate using the same version.

#### Manual testing

Run with ```activator run```.

When the application is started, it first creates a ZooKeeper server using a temporary directory as data storage. A server supporting protocol ```controllers.protocols.EmployeeRegistry``` is registered at ```/protocols/controllers.protocols.EmployeeRegistry/servers```. The server's version of the protocol is also stored at ```/protocols/controllers.protocols.EmployeeRegistry/versions/3AC1B30392B877DE6A89B64C5518D175```.

##### Direct requests to the server

The server has an HTTP endpoint that support requests in the JSON format, so one may issue the following command to directly access its service.

Count the current employees.
```bash
$ curl -X POST -H "Content-Type: avro/json" http://localhost:9000/current/countEmployees
0
```

Add 3 employees.
```
$ curl -X POST -H "Content-Type: avro/json" -d '{"employee": {"firstName": "Thomas", "lastName": "Feng", "gender: "MALE", "dateOfBirth": {"year": 2000, "month": 1, "day": 1}}}' http://localhost:9000/current/addEmployee
1
$ curl -X POST -H "Content-Type: avro/json" -d '{"employee": {"firstName": "Jackson", "lastName": "Wang", "gender": "MALE", "dateOfBirth": {"year": 2001, "month": 5, "day": 15}}}' http://localhost:9000/current/addEmployee
2
$ curl -X POST -H "Content-Type: avro/json" -d '{"employee": {"firstName": "Christine", "lastName": "Lee", "gender": "FEMALE", "dateOfBirth": {"year": 2000, "month": 8, "day": 20}}}' http://localhost:9000/current/addEmployee
3
```

Count the current employees.
```bash
$ curl -X POST -H "Content-Type: avro/json" http://localhost:9000/current/countEmployees
3
```

Make an employee manager.
```bash
$ curl -X POST -H "Content-Type: avro/json" -d '{"managerId": 1, "employeeId": 2}' http://localhost:9000/current/makeManager
null
$ curl -X POST -H "Content-Type: avro/json" -d '{"managerId": 1, "employeeId": 3}' http://localhost:9000/current/makeManager
null
```

Get all the employees under a manager.
```bash
$ curl -X POST -H "Content-Type: avro/json" -d '{"managerId": 1}' http://localhost:9000/current/getEmployees
[{"id":2,"firstName":"Jackson","lastName":"Wang","gender":"MALE","dateOfBirth":{"year":2001,"month":5,"day":15}},{"id":3,"firstName":"Christine","lastName":"Lee","gender":"FEMALE","dateOfBirth":{"year":2000,"month":8,"day":20}}]
```

Get the manager of an employee.
```bash
$ curl -X POST -H "Content-Type: avro/json" -d '{"employeeId": 2}' http://localhost:9000/current/getManager
{"id":1,"firstName":"Thomas","lastName":"Feng","gender":"MALE","dateOfBirth":{"year":2000,"month":1,"day":1}}
```

##### Sending requests to the server through a legacy client

The server also has a different HTTP endpoint under ```/legacy``` that exposes a legacy client using an old version of the protocol. When the user accesses this endpoint, the request is first sent to the customized controller, which then invokes the client to send requests to the service.

Because the protocol that the client has is older than the service's, a schema evolution scenario is simulated. The client does not understand new features of the protocol that are added only in the new version, and the service must convert the requests and responses back and forth between the two versions of the protocol in order for the client to understand.

The old version does not have a method to count employees, so counting the current employees would fail.
```bash
$ curl -i "http://localhost:9000/legacy/countEmployees"
HTTP/1.1 400 Bad Request
Content-Length: 0
```

Add 3 employees. The requests must be sent in a customized way to the Play controller, as defined in the ```routes``` file.
```bash
$ curl "http://localhost:9000/legacy/addEmployee?firstName=Thomas&lastName=Feng"
1
$ curl "http://localhost:9000/legacy/addEmployee?firstName=Jackson&lastName=Wang"
2
$ curl "http://localhost:9000/legacy/addEmployee?firstName=Christine&lastName=Lee"
3
```

Make an employee manager.
```bash
$ curl "http://localhost:9000/legacy/makeManager?managerId=1&employeeId=2"
null
$ curl "http://localhost:9000/legacy/makeManager?managerId=1&employeeId=3"
null
```

Get all the employees under a manager. Because the old protocol does not define the extra fields ```gender``` and ```dateOfBirth```, they are not being returned by the service to the client, and hence the controller using that client does not return them to the user.
```bash
$ curl "http://localhost:9000/legacy/getEmployees?managerId=1"
[{"id": 2, "firstName": "Jackson", "lastName": "Wang"}, {"id": 3, "firstName": "Christine", "lastName": "Lee"}]
```

Get the manager of an employee.
```bash
$ curl "http://localhost:9000/legacy/getManager?employeeId=2"
{"id": 1, "firstName": "Thomas", "lastName": "Feng"}
```