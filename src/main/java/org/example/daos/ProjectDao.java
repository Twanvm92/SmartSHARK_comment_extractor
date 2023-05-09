package org.example.daos;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;
import org.bson.BsonRegularExpression;
import org.bson.Document;
import org.bson.conversions.Bson;

/**
 * This class provides a DAO (Data Access Object) implementation for the project collection in the
 * MongoDB database.
 */
public class ProjectDao extends AbstractDao {

  public static final String PROJECTS_COLLECTION = "project";
  private final MongoCollection<Document> projectsCollection;

  /**
   * Constructs a new instance of the ProjectDao class with the specified MongoDB database names and
   * client.
   *
   * @param SMARTSHARK_DATABASE The name of the MongoDB database for SmartSHARK.
   * @param COMMENT_DATABASE The name of the MongoDB database for comments.
   * @param mongoClient The client to connect to the MongoDB database.
   */
  public ProjectDao(String SMARTSHARK_DATABASE, String COMMENT_DATABASE, MongoClient mongoClient) {
    super(SMARTSHARK_DATABASE, COMMENT_DATABASE, mongoClient);
    projectsCollection = db.getCollection(PROJECTS_COLLECTION);
  }

  //  TODO integrate the date filter into the CLI with argparser.
  /**
   * This method retrieves all projects with their respective hunks from the database using an
   * aggregation pipeline with multiple stages of $lookup, $unwind, and $match. The resulting
   * documents are filtered by those with at least one hunk containing more than 0 lines added, and
   * the committer_date of the commit associated with the hunk is greater than or equal to January
   * 1, 2020 at 1:00 AM CET. The hunks are collected from each project's branch that is considered
   * the 'main' branch.
   */
  public void outputProjectsWithHunks() throws ParseException {

    List<? extends Bson> pipeline =
        Arrays.asList(
            new Document()
                .append(
                    "$lookup",
                    new Document()
                        .append("from", "vcs_system")
                        .append("localField", "_id")
                        .append("foreignField", "project_id")
                        .append("as", "vcs_system")),
            new Document().append("$unwind", new Document().append("path", "$vcs_system")),
            new Document()
                .append(
                    "$lookup",
                    new Document()
                        .append("from", "branch")
                        .append("let", new Document().append("vcs_id", "$vcs_system._id"))
                        .append(
                            "pipeline",
                            Arrays.asList(
                                new Document()
                                    .append(
                                        "$match",
                                        new Document()
                                            .append(
                                                "$expr",
                                                new Document()
                                                    .append(
                                                        "$and",
                                                        Arrays.asList(
                                                            new Document()
                                                                .append(
                                                                    "$eq",
                                                                    Arrays.asList(
                                                                        "$vcs_system_id",
                                                                        "$$vcs_id")),
                                                            new Document()
                                                                .append(
                                                                    "$eq",
                                                                    Arrays.asList(
                                                                        "$is_origin_head",
                                                                        true))))))))
                        .append("as", "branch")),
            new Document().append("$unwind", new Document().append("path", "$branch")),
            new Document()
                .append(
                    "$lookup",
                    new Document()
                        .append("from", "commit")
                        .append(
                            "let",
                            new Document()
                                .append(
                                    "branchName",
                                    new Document()
                                        .append(
                                            "$concat",
                                            Arrays.asList("refs/remotes/", "$branch.name")))
                                .append("vcs_id", "$vcs_system._id"))
                        .append(
                            "pipeline",
                            Arrays.asList(
                                new Document()
                                    .append(
                                        "$match",
                                        new Document()
                                            .append(
                                                "$expr",
                                                new Document()
                                                    .append(
                                                        "$and",
                                                        Arrays.asList(
                                                            new Document()
                                                                .append(
                                                                    "$in",
                                                                    Arrays.asList(
                                                                        "$$branchName",
                                                                        new Document()
                                                                            .append(
                                                                                "$ifNull",
                                                                                Arrays.asList(
                                                                                    "$branches",
                                                                                    Arrays
                                                                                        .asList())))),
                                                            new Document()
                                                                .append(
                                                                    "$gte",
                                                                    Arrays.asList(
                                                                        "$committer_date",
                                                                        //
                                                                        //
                                                                        //                    TODO
                                                                        // make date passable
                                                                        // command line argument
                                                                        new SimpleDateFormat(
                                                                                "yyyy-MM-dd HH:mm:ss.SSSZ")
                                                                            .parse(
                                                                                "2016-01-01 01:00:00.000+0100"))),
                                                            new Document()
                                                                .append(
                                                                    "$eq",
                                                                    Arrays.asList(
                                                                        "$vcs_system_id",
                                                                        "$$vcs_id"))))))))
                        .append("as", "commit")),
            new Document().append("$unwind", new Document().append("path", "$commit")),
            new Document()
                .append(
                    "$lookup",
                    new Document()
                        .append("from", "file_action")
                        .append("let", new Document().append("commit_id_let", "$commit._id"))
                        .append(
                            "pipeline",
                            Arrays.asList(
                                new Document()
                                    .append(
                                        "$match",
                                        new Document()
                                            .append(
                                                "$expr",
                                                new Document()
                                                    .append(
                                                        "$and",
                                                        Arrays.asList(
                                                            new Document()
                                                                .append(
                                                                    "$gt",
                                                                    Arrays.asList(
                                                                        "$lines_added", 0)),
                                                            new Document()
                                                                .append(
                                                                    "$eq",
                                                                    Arrays.asList(
                                                                        "$commit_id",
                                                                        "$$commit_id_let"))))))))
                        .append("as", "file_action")),
            new Document().append("$unwind", new Document().append("path", "$file_action")),
            new Document()
                .append(
                    "$lookup",
                    new Document()
                        .append("from", "hunk")
                        .append(
                            "let", new Document().append("file_action_id_let", "$file_action._id"))
                        .append(
                            "pipeline",
                            Arrays.asList(
                                new Document()
                                    .append(
                                        "$match",
                                        new Document()
                                            .append(
                                                "$expr",
                                                new Document()
                                                    .append(
                                                        "$eq",
                                                        Arrays.asList(
                                                            "$file_action_id",
                                                            "$$file_action_id_let"))))))
                        .append("as", "hunk")),
            new Document().append("$unwind", new Document().append("path", "$hunk")),
            new Document()
                .append(
                    "$lookup",
                    new Document()
                        .append("from", "file")
                        .append(
                            "let",
                            new Document().append("file_action_file_id", "$file_action.file_id"))
                        .append(
                            "pipeline",
                            Arrays.asList(
                                new Document()
                                    .append(
                                        "$match",
                                        new Document()
                                            .append(
                                                "$expr",
                                                new Document()
                                                    .append(
                                                        "$and",
                                                        Arrays.asList(
                                                            new Document()
                                                                .append(
                                                                    "$eq",
                                                                    Arrays.asList(
                                                                        "$_id",
                                                                        "$$file_action_file_id")),
                                                            new Document()
                                                                .append(
                                                                    "$regexMatch",
                                                                    new Document()
                                                                        .append("input", "$path")
                                                                        .append(
                                                                            "regex",
                                                                            new BsonRegularExpression(
                                                                                "\\.java$"))
                                                                        .append(
                                                                            "options", "i"))))))))
                        .append("as", "file")),
            new Document().append("$unwind", new Document().append("path", "$file")),
            new Document()
                .append(
                    "$project",
                    new Document()
                        .append("_id", 0)
                        .append("name", 1)
                        .append("vcs_system._id", 1)
                        .append("vcs_system.url", 1)
                        .append("branch._id", 1)
                        .append("branch.name", 1)
                        .append("commit.committer_date", 1)
                        .append("commit._id", 1)
                        .append("commit.revision_hash", 1)
                        .append("file_action._id", 1)
                        .append("file_action.file_id", 1)
                        .append("file._id", 1)
                        .append("file.path", 1)
                        .append("hunk.content", 1)
                        .append("hunk._id", 1)
                        .append("hunk.old_start", 1)
                        .append("hunk.new_start", 1)),
            new Document()
                .append("$out", new Document().append("db", "twan_satd").append("coll", "hunk")));

    projectsCollection.aggregate(pipeline).toCollection();
  }
}
