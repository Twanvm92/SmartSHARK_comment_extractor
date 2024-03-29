package org.example.daos;

import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

import com.mongodb.MongoBulkWriteException;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import java.util.Arrays;
import java.util.List;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bson.conversions.Bson;
import org.example.models.CommentDTO;

/**
 * The CommentDao class extends the AbstractDao class and represents a data access object for
 * comments stored in a MongoDB database. It provides methods for adding comments to the database
 * and retrieving deduplicated comments using aggregation pipelines. The class utilizes the
 * CommentDTO class for creating and mapping comments to the MongoDB database.
 */
public class CommentDao extends AbstractDao {

  public static final String COMMENTS_COLLECTION = "comments_only_java";
  public static final String DEDUPLICATED_COMMENTS_COLLECTION = "deduplicated_nonfiltered_comments";
  private final MongoCollection<CommentDTO> commentsCollection;
  private final MongoCollection<Document> commentsCollectionsWithoutPojo;

  /**
   * Constructs a new CommentDao instance.
   *
   * @param smartsharkDatabaseName The name of the SmartSHARK database.
   * @param commentDatabaseName The name of the comment database.
   * @param mongoClient The MongoClient instance used to connect to the MongoDB server.
   */
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

  /**
   * Adds a list of CommentDTO objects to the database.
   *
   * @param commentDTOs The list of CommentDTO objects to add to the database.
   */
  public void addComments(List<CommentDTO> commentDTOs) {
    try {
      commentsCollection.insertMany(commentDTOs);
    } catch (MongoBulkWriteException e) {
      System.out.println("Could not add comments: " + e);
      System.exit(1);
    }
  }

  /**
   * Retrieves and adds deduplicated comments to the deduplicated comments collection using an
   * aggregation pipeline.
   */
  public void getAndAddDeduplicatedComments() {
    // aggregation pipeline
    List<? extends Bson> pipeline =
        Arrays.asList(
            // new stage
            new Document().append("$match", new Document().append("filtered", false)),
            new Document()
                .append(
                    // new stage
                    "$group",
                    new Document()
                        .append(
                            "_id",
                            new Document()
                                .append("project_name", "$project_name")
                                .append("content", "$content"))
                        .append("count", new Document().append("$sum", 1.0))
                        .append("comments", new Document().append("$push", "$$ROOT"))),
            new Document()
                .append(
                    // new stage
                    "$project",
                    new Document()
                        .append("_id", 0.0)
                        .append("group_key", "$_id")
                        .append("comments", 1.0)
                        .append("count", 1.0)),
            // new stage
            new Document().append("$out", DEDUPLICATED_COMMENTS_COLLECTION));
    commentsCollectionsWithoutPojo.aggregate(pipeline).allowDiskUse(true).toCollection();
  }
}
