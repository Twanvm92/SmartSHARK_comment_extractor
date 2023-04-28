package org.example;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.apache.commons.configuration.PropertiesConfiguration;

/** This class represents the configuration of a MongoDB database connection. */
public class MongoDBConfiguration {
  private final PropertiesConfiguration config;

  /**
   * Constructs a new instance of MongoDBConfiguration with the specified config.
   *
   * @param config The configuration properties used to set up the connection.
   */
  public MongoDBConfiguration(PropertiesConfiguration config) {
    this.config = config;
  }

  /**
   * Returns a new instance of {@link com.mongodb.client.MongoClient} based on the configuration
   * properties.
   *
   * @return A new instance of {@link com.mongodb.client.MongoClient} based on the configuration
   *     properties.
   */
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
