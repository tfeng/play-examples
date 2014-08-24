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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Properties;

import me.tfeng.play.plugins.AvroD2Plugin;
import me.tfeng.play.security.oauth2.OAuth2GlobalSettings;

import org.apache.commons.io.FileUtils;
import org.apache.zookeeper.server.ServerCnxnFactory;
import org.apache.zookeeper.server.ZooKeeperServer;
import org.apache.zookeeper.server.persistence.FileTxnSnapLog;
import org.apache.zookeeper.server.quorum.QuorumPeerConfig;

import play.Application;
import play.Logger;
import play.Logger.ALogger;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
public class Global extends OAuth2GlobalSettings {

  private static final ALogger LOG = Logger.of(Global.class);

  private ServerCnxnFactory cnxnFactory;

  private ZooKeeperServer zkServer;

  @Override
  public void beforeStart(Application app) {
    try {
      Properties properties = new Properties();
      InputStream propertiesStream = getClass().getClassLoader().getResourceAsStream("zoo.cfg");
      properties.load(propertiesStream);

      QuorumPeerConfig quorumConfig = new QuorumPeerConfig();
      quorumConfig.parseProperties(properties);

      zkServer = new ZooKeeperServer();
      zkServer.setTickTime(quorumConfig.getTickTime());
      zkServer.setMinSessionTimeout(quorumConfig.getMinSessionTimeout());
      zkServer.setMaxSessionTimeout(quorumConfig.getMaxSessionTimeout());

      final File zkDirectory = Files.createTempDirectory("zookeeper").toFile();
      LOG.info("Using ZooKeeper directory: " + zkDirectory);
      Runtime.getRuntime().addShutdownHook(new Thread() {
        @Override
        public void run() {
          try {
            FileUtils.deleteDirectory(zkDirectory);
          } catch (IOException e) {
            LOG.warn("Unable to delete ZooKeeper directory: " + zkDirectory, e);
          }
        }
      });

      FileTxnSnapLog txnLog = new FileTxnSnapLog(zkDirectory, zkDirectory);
      zkServer.setTxnLogFactory(txnLog);

      cnxnFactory = ServerCnxnFactory.createFactory();
      cnxnFactory.configure(quorumConfig.getClientPortAddress(), quorumConfig.getMaxClientCnxns());
      cnxnFactory.startup(zkServer);
    } catch (Exception e) {
      throw new RuntimeException("Unable to start ZooKeeper server", e);
    }
  }

  @Override
  public void onStop(Application app) {
    AvroD2Plugin.getInstance().stopServers();
    cnxnFactory.shutdown();
    zkServer.shutdown();
  }
}
