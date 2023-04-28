package org.example.daos;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import java.util.ArrayList;
import java.util.List;
import org.bson.Document;
import org.bson.types.ObjectId;

/** Provides data access methods for the intermediate Hunk collection in the MongoDB database. */
public class HunkDAO extends AbstractDao {

  public static final String HUNKS_COLLECTION_INTERMED = "hunk";
  private final MongoCollection<Document> hunksCollectionIntermed;

  /**
   * Constructs a new HunkDAO with the given database names and MongoClient.
   *
   * @param SMARTSHARK_DATABASE the name of the SmartSHARK database
   * @param COMMENT_DATABASE the name of the comment database
   * @param mongoClient the MongoClient
   */
  public HunkDAO(String SMARTSHARK_DATABASE, String COMMENT_DATABASE, MongoClient mongoClient) {
    super(SMARTSHARK_DATABASE, COMMENT_DATABASE, mongoClient);
    hunksCollectionIntermed = commentDb.getCollection(HUNKS_COLLECTION_INTERMED);
  }

  /**
   * Returns a list of hunks from the intermediate Hunk collection with IDs greater than the
   * specified ID, sorted by ID in ascending order, limited to the specified number of documents.
   *
   * @param lastSeenId the ID of the last Hunk seen by the client
   * @param limit the maximum number of documents to return
   * @return a list of hunks from the intermediate Hunk collection
   */
  public List<Document> getHunks(ObjectId lastSeenId, int limit) {
    return hunksCollectionIntermed
        .find(Filters.gt("_id", lastSeenId))
        .sort(new Document("_id", 1))
        .limit(limit)
        .into(new ArrayList<>());
  }

  /**
   * Returns a list of hunks from the intermediate Hunk collection, sorted by ID in ascending order,
   * limited to the specified number of documents.
   *
   * @param limit the maximum number of documents to return
   * @return a list of hunks from the intermediate Hunk collection
   */
  public List<Document> getHunks(int limit) {
    return hunksCollectionIntermed
        .find()
        .sort(new Document("_id", 1))
        .limit(limit)
        .into(new ArrayList<>());
  }

  /**
   * Returns the number of documents in the intermediate Hunk collection.
   *
   * @return the number of documents in the intermediate Hunk collection
   */
  public long getHunksCount() {
    return hunksCollectionIntermed.countDocuments();
  }

  /**
   * Returns the number of documents in the intermediate Hunk collection with IDs greater than the
   * specified ID.
   *
   * @param lastSeenId the ID of the last Hunk seen by the client
   * @return the number of documents in the intermediate Hunk collection with IDs greater than the
   *     specified ID
   */
  public long getHunksCount(ObjectId lastSeenId) {
    return hunksCollectionIntermed.countDocuments(Filters.gt("_id", lastSeenId));
  }
}
