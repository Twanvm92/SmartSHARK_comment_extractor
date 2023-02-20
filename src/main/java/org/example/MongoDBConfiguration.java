package org.example;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.apache.commons.configuration.PropertiesConfiguration;

public class MongoDBConfiguration {
  private final PropertiesConfiguration config;

  public MongoDBConfiguration(PropertiesConfiguration config) {
    this.config = config;
  }

  public MongoClient getMongoClient() {
    String uri = config.getString("mongodb.uri");
    String user = config.getString("mongodb.user");
    String password = config.getString("mongodb.password");
    String host = config.getString("mongodb.hostname");
    String port = config.getString("mongodb.port");
    String options = config.getString("mongodb.options");

    String fullUri = String.format(uri, user, password, host, port, options);

    return MongoClients.create(fullUri);
  }
}
