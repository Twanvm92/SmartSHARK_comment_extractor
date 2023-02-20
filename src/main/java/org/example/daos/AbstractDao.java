package org.example.daos;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;

public abstract class AbstractDao {

  protected final String COMMENT_DATABASE;
  protected MongoDatabase db;
  protected MongoDatabase commentDb;
  protected MongoClient mongoClient;

  public AbstractDao(String SMARTSHARK_DATABASE, String COMMENT_DATABASE, MongoClient mongoClient) {
    this.COMMENT_DATABASE = COMMENT_DATABASE;
    this.mongoClient = mongoClient;
    db = this.mongoClient.getDatabase(SMARTSHARK_DATABASE);
    commentDb = this.mongoClient.getDatabase(this.COMMENT_DATABASE);
  }
}
