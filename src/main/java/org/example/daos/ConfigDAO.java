package org.example.daos;

import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import java.time.LocalDateTime;
import java.util.Map;
import org.bson.Document;
import org.bson.types.ObjectId;

/**
 * A DAO class for MongoDB database connection and operations related to configurations. Extends the
 * AbstractDao class.
 */
public class ConfigDAO extends AbstractDao {
  public static final String CONFIGS_COLLECTION = "config";
  private final MongoCollection<Document> configsCollection;

  /**
   * Constructs a ConfigDAO object.
   *
   * @param SMARTSHARK_DATABASE the name of the SmartSHARK database.
   * @param COMMENT_DATABASE the name of the comment database.
   * @param mongoClient the MongoClient object that represents the MongoDB client.
   */
  public ConfigDAO(String SMARTSHARK_DATABASE, String COMMENT_DATABASE, MongoClient mongoClient) {
    super(SMARTSHARK_DATABASE, COMMENT_DATABASE, mongoClient);
    configsCollection = commentDb.getCollection(CONFIGS_COLLECTION);
  }

  /**
   * Adds the last created hunk's Mongodb Object ID to the configurations collection.
   *
   * @param id the ID to be added.
   */
  public void addLastId(ObjectId id) {
    LocalDateTime now = LocalDateTime.now();
    try {
      configsCollection.insertOne(
          new Document(Map.ofEntries(Map.entry("date_time", now), Map.entry("last_id", id))));
    } catch (MongoWriteException e) {
      System.out.println(e.getMessage());
    }
  }
}
