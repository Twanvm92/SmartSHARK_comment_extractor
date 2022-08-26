package org.example.daos;

import com.mongodb.MongoBulkWriteException;
import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.*;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HunkDAO extends AbstractDao{

    public final static String HUNKS_COLLECTION = "hunk";
    private MongoCollection<Document> hunksCollection;
    private MongoCollection<Document> hunksCollectionIntermed;

    public HunkDAO(String SMARTSHARK_DATABASE, String COMMENT_DATABASE, MongoClient mongoClient) {
        super(SMARTSHARK_DATABASE, COMMENT_DATABASE, mongoClient);
        hunksCollection = db.getCollection(HUNKS_COLLECTION);
        hunksCollectionIntermed = commentDb.getCollection(HUNKS_COLLECTION);
    }

    public BulkWriteResult addHunks(List<Document> documents) {

        List<WriteModel<Document>> bulkWriteOps = new ArrayList<>();
        for (Document document:
                documents) {
            Bson filter = Filters.eq("old_id", document.getObjectId("hunk._id"));
            Bson update = Updates.setOnInsert(new Document(Map.ofEntries(
                    Map.entry("added_lines", document.get("hunk.added_lines")),
                    Map.entry("committer_date", document.getDate("commit.committer_date")),
                    Map.entry("old_id", document.getObjectId("hunk._id"))
            )));
            UpdateOptions options = new UpdateOptions().upsert(true);
            bulkWriteOps.add(new UpdateOneModel<>(filter, update, options));
        }

        BulkWriteResult bulkWriteResult = null;

        try {
            bulkWriteResult = hunksCollectionIntermed.bulkWrite(bulkWriteOps);
        } catch (MongoBulkWriteException e) {
            System.out.println("Could not add hunks: " + e.getMessage());
            System.exit(1);
        }

        return bulkWriteResult;
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
