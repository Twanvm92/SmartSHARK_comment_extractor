package org.example.daos;

import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

import com.mongodb.MongoBulkWriteException;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates;
import java.util.List;
import java.util.Map;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.example.models.CommentDTO;

public class CommentDao extends AbstractDao {

  public static final String COMMENTS_COLLECTION = "comments";
  public static final String DEDUPLICATED_COMMENTS_COLLECTION = "deduplicated_nonfiltered_comments";
  private final MongoCollection<CommentDTO> commentsCollection;
  private final MongoCollection<Document> commentsCollectionsWithoutPojo;

  public CommentDao(
      String smartsharkDatabaseName, String commentDatabaseName, MongoClient mongoClient) {
    super(smartsharkDatabaseName, commentDatabaseName, mongoClient);
    CodecRegistry pojoCodecRegistry =
        fromRegistries(
            MongoClientSettings.getDefaultCodecRegistry(),
            fromProviders(PojoCodecProvider.builder().automatic(true).build()));
    commentsCollection =
        commentDb
            .getCollection(COMMENTS_COLLECTION, CommentDTO.class)
            .withCodecRegistry(pojoCodecRegistry);
    commentsCollectionsWithoutPojo = commentDb.getCollection(COMMENTS_COLLECTION);
  }

  public void addComments(List<CommentDTO> commentDTOs) {
    try {
      commentsCollection.insertMany(commentDTOs);
    } catch (MongoBulkWriteException e) {
      System.out.println("Could not add comments: " + e);
      System.exit(1);
    }
  }

  public void getAndAddDeduplicatedComments() {
    commentsCollectionsWithoutPojo
        .aggregate(
            List.of(
                Aggregates.match(new Document("filtered", false)),
                Aggregates.group(
                    new Document(
                        Map.ofEntries(
                            Map.entry("project_name", "$project_name"),
                            Map.entry("content", "$content"))),
                    Accumulators.sum("count", 1),
                    Accumulators.push("comment_ids", "$_id")),
                Aggregates.out(DEDUPLICATED_COMMENTS_COLLECTION)))
        .allowDiskUse(true)
        .toCollection();
  }
}
