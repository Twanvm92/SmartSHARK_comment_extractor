package org.example.daos;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import java.util.ArrayList;
import java.util.List;
import org.bson.Document;
import org.bson.types.ObjectId;

public class HunkDAO extends AbstractDao {

  public static final String HUNKS_COLLECTION_INTERMED = "hunk";
  private final MongoCollection<Document> hunksCollectionIntermed;

  public HunkDAO(String SMARTSHARK_DATABASE, String COMMENT_DATABASE, MongoClient mongoClient) {
    super(SMARTSHARK_DATABASE, COMMENT_DATABASE, mongoClient);
    hunksCollectionIntermed = commentDb.getCollection(HUNKS_COLLECTION_INTERMED);
  }

  public List<Document> getHunks(ObjectId lastSeenId, int limit) {
    return hunksCollectionIntermed
        .find(Filters.gt("_id", lastSeenId))
        .sort(new Document("_id", 1))
        .limit(limit)
        .into(new ArrayList<>());
  }

  public List<Document> getHunks(int limit) {
    return hunksCollectionIntermed
        .find()
        .sort(new Document("_id", 1))
        .limit(limit)
        .into(new ArrayList<>());
  }

  public long getHunksCount() {
    return hunksCollectionIntermed.countDocuments();
  }

  public long getHunksCount(ObjectId lastSeenId) {
    return hunksCollectionIntermed.countDocuments(Filters.gt("_id", lastSeenId));
  }
}
