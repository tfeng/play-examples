avro-d2-example
=========

An example to simulate a scalable web service using [Avro IPC](http://avro.apache.org/docs/current/spec.html#Protocol+Declaration) as the communication protocol and using [ZooKeeper](http://zookeeper.apache.org/) to track the active machines (which could be more than one, in general).

The design is based on [LinkedIn](https://www.linkedin.com)'s [rest.li](http://rest.li/).

---

Run with ```activator run```.

#### Design

The computation power (and network bandwidth, and memory size, and storage capacity, ...) of a single machine is not without limit. The amount of online requests, however, can grow without bound. For a web service to be scalable, it must be able to run on more machines to gain more power. Therefore, while designing a server, one should think of it as a set of machines to begin with, rather than a single one.

For a set of machines to serve the traffic in a fair manner, there must be some sort of load balancing between them. A simple solution is by adopting a load balancer, which routes traffic in a somewhat fair fashion to the participating machines. There are software load balancers, and there are hardware ones (which are generally faster but not as inexpensive).

Another approach to load balancing is _client-side load balancing_. When applied, the clients (those who make requests to the servers) should route their requests directly to the machines, rather than relaying on an intermediate load balancer. The _cons_ of such an approach are:

1. clients need to know about the actual machines,
2. clients have additional logic, and
3. if clients behave maliciously (e.g. keep sending traffic to the same machine), some machines may be impacted.

The _pros_ are:

* a hop is avoided by not introducing an extra layer in the network,
* a single-point of failure is also avoided, and
* by knowing about the machines, clients may send traffic more intelligently (e.g., selecting machines based on geo location).

If a client is trusted and can utilize a library that implements the client-side load balancing logic, then No. 2 and No. 3 in the cons are addressed. For No. 1, however, we need a mechanism.

D2 in this document stands for _dynamic discovery_. It enables a client to dynamically discover machines that provide a service. The [Play Avro D2 plugin](https://github.com/tfeng/play-plugins/tree/master/plugins/avro-d2) implements such a mechanism on top of Avro IPC.
