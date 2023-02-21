package org.example.daos;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.bson.Document;
import org.bson.conversions.Bson;

public class ProjectDao extends AbstractDao {

  public static final String PROJECTS_COLLECTION = "project";
  private final MongoCollection<Document> projectsCollection;

  public ProjectDao(String SMARTSHARK_DATABASE, String COMMENT_DATABASE, MongoClient mongoClient) {
    super(SMARTSHARK_DATABASE, COMMENT_DATABASE, mongoClient);
    projectsCollection = db.getCollection(PROJECTS_COLLECTION);
  }

  public void outputProjectsWithHunks() {

    List<? extends Bson> pipeline;
    try {
      pipeline =
          Arrays.asList(
              new Document()
                  .append(
                      // new stage
                      // join project with vcs_system
                      "$lookup",
                      new Document()
                          .append("from", "vcs_system")
                          .append("localField", "_id")
                          .append("foreignField", "project_id")
                          .append("as", "vcs_system")),
              // new stage
              new Document().append("$unwind", new Document().append("path", "$vcs_system")),
              new Document()
                  .append(
                      // new stage
                      // join vcs_system with branch
                      "$lookup",
                      new Document()
                          .append("from", "branch")
                          .append("let", new Document().append("vcs_id", "$vcs_system._id"))
                          .append(
                              "pipeline",
                              Collections.singletonList(
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
              // new stage
              new Document().append("$unwind", new Document().append("path", "$branch")),
              // new stage
              // join commit with branch and vcs_system
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
                              Collections.singletonList(
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
                                                                                      List.of())))),
                                                              new Document()
                                                                  .append(
                                                                      "$gte",
                                                                      Arrays.asList(
                                                                          "$committer_date",
                                                                          new SimpleDateFormat(
                                                                                  "yyyy-MM-dd HH:mm:ss.SSSZ")
                                                                              //
                                                                              //
                                                                              //
                                                                              //
                                                                              // TODO change date!
                                                                              .parse(
                                                                                  "2020-01-01 01:00:00.000+0100"))),
                                                              new Document()
                                                                  .append(
                                                                      "$eq",
                                                                      Arrays.asList(
                                                                          "$vcs_system_id",
                                                                          "$$vcs_id"))))))))
                          .append("as", "commit")),
              new Document().append("$unwind", new Document().append("path", "$commit")),
              // new stage
              // join commit with file_action
              new Document()
                  .append(
                      "$lookup",
                      new Document()
                          .append("from", "file_action")
                          .append("let", new Document().append("commit_id_let", "$commit._id"))
                          .append(
                              "pipeline",
                              Collections.singletonList(
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
                                                                          "$lines_added", 0.0)),
                                                              new Document()
                                                                  .append(
                                                                      "$eq",
                                                                      Arrays.asList(
                                                                          "$commit_id",
                                                                          "$$commit_id_let"))))))))
                          .append("as", "file_action")),
              // new stage
              new Document().append("$unwind", new Document().append("path", "$file_action")),
              // new stage
              // join file_action with hunk
              new Document()
                  .append(
                      "$lookup",
                      new Document()
                          .append("from", "hunk")
                          .append(
                              "let",
                              new Document().append("file_action_id_let", "$file_action._id"))
                          .append(
                              "pipeline",
                              Collections.singletonList(
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
              // new stage
              new Document().append("$unwind", new Document().append("path", "$hunk")),
              // new stage
              // join file_action with file
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
                              Collections.singletonList(
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
                                                              "$_id", "$$file_action_file_id"))))))
                          .append("as", "file")),
              new Document().append("$unwind", new Document().append("path", "$file")),
              // new stage
              new Document()
                  .append(
                      "$project",
                      new Document()
                          .append("_id", 1.0)
                          .append("name", 1.0)
                          .append("vcs_system._id", 1.0)
                          .append("vcs_system.url", 1.0)
                          .append("branch._id", 1.0)
                          .append("branch.name", 1.0)
                          .append("commit.committer_date", 1.0)
                          .append("commit._id", 1.0)
                          .append("commit.revision_hash", 1.0)
                          .append("file_action._id", 1.0)
                          .append("file._id", 1.0)
                          .append("file.path", 1.0)
                          .append("hunk.content", 1.0)
                          .append("hunk._id", 1.0)
                          .append("hunk.old_start", 1.0)
                          .append("hunk.new_start", 1.0)),
              // new stage
              new Document()
                  .append(
                      "$out",
                      new Document()
                          .append("db", COMMENT_DATABASE)
                          .append("coll", HunkDAO.HUNKS_COLLECTION_INTERMED)));
    } catch (ParseException e) {
      throw new RuntimeException(e);
    }

    projectsCollection.aggregate(pipeline).toCollection();
  }
}
