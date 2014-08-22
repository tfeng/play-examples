import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Properties;

import me.tfeng.play.plugins.AvroD2Plugin;
import me.tfeng.play.plugins.SpringGlobalSettings;

import org.apache.commons.io.FileUtils;
import org.apache.zookeeper.server.ServerCnxnFactory;
import org.apache.zookeeper.server.ZooKeeperServer;
import org.apache.zookeeper.server.persistence.FileTxnSnapLog;
import org.apache.zookeeper.server.quorum.QuorumPeerConfig;

import play.Application;
import play.Logger;
import play.Logger.ALogger;

public class Global extends SpringGlobalSettings {

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
