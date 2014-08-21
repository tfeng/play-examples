import java.io.File;
import java.io.InputStream;
import java.util.Properties;

import me.tfeng.play.plugins.AvroD2Plugin;

import org.apache.zookeeper.server.ServerCnxnFactory;
import org.apache.zookeeper.server.ZooKeeperServer;
import org.apache.zookeeper.server.persistence.FileTxnSnapLog;
import org.apache.zookeeper.server.quorum.QuorumPeerConfig;

import play.Application;
import play.GlobalSettings;

public class Global extends GlobalSettings {

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
      FileTxnSnapLog txnLog = new FileTxnSnapLog(new File(quorumConfig.getDataLogDir()), new File(
          quorumConfig.getDataDir()));
      zkServer.setTxnLogFactory(txnLog);
      zkServer.setTickTime(quorumConfig.getTickTime());
      zkServer.setMinSessionTimeout(quorumConfig.getMinSessionTimeout());
      zkServer.setMaxSessionTimeout(quorumConfig.getMaxSessionTimeout());
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
