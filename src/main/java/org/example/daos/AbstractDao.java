package org.example.daos;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;

/**
 * This is an abstract class representing a data access object. It provides access to a MongoDB
 * instance with two databases: one is the database for the SMARTSHARK project and the other is the
 * database for the comments.
 */
public abstract class AbstractDao {

  protected final String COMMENT_DATABASE;
  protected MongoDatabase db;
  protected MongoDatabase commentDb;
  protected MongoClient mongoClient;

  /**
   * Constructs an abstract data access object with a MongoDB client, a SMARTSHARK database, and a
   * comment database.
   *
   * @param SMARTSHARK_DATABASE The name of the SMARTSHARK database.
   * @param COMMENT_DATABASE The name of the comment database.
   * @param mongoClient The MongoDB client used to connect to the databases.
   */
  public AbstractDao(String SMARTSHARK_DATABASE, String COMMENT_DATABASE, MongoClient mongoClient) {
    this.COMMENT_DATABASE = COMMENT_DATABASE;
    this.mongoClient = mongoClient;
    db = this.mongoClient.getDatabase(SMARTSHARK_DATABASE);
    commentDb = this.mongoClient.getDatabase(this.COMMENT_DATABASE);
  }
}
