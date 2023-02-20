package org.example.daos;

import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import java.time.LocalDateTime;
import java.util.Map;
import org.bson.Document;
import org.bson.types.ObjectId;

public class ConfigDAO extends AbstractDao {
  public static final String CONFIGS_COLLECTION = "config";
  private final MongoCollection<Document> configsCollection;

  public ConfigDAO(String SMARTSHARK_DATABASE, String COMMENT_DATABASE, MongoClient mongoClient) {
    super(SMARTSHARK_DATABASE, COMMENT_DATABASE, mongoClient);
    configsCollection = commentDb.getCollection(CONFIGS_COLLECTION);
  }

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
