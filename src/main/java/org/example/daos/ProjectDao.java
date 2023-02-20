package org.example.daos;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Variable;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.mongodb.client.model.Projections.*;


public class ProjectDao extends AbstractDao {

    public final static String PROJECTS_COLLECTION = "project";
    private MongoCollection<Document> projectsCollection;
    private MongoCollection<Document> projectsCollectionIntermed;

    public ProjectDao(String SMARTSHARK_DATABASE, String COMMENT_DATABASE, MongoClient mongoClient) {
        super(SMARTSHARK_DATABASE, COMMENT_DATABASE, mongoClient);
        projectsCollection = db.getCollection(PROJECTS_COLLECTION);
        projectsCollectionIntermed = commentDb.getCollection(PROJECTS_COLLECTION);
    }

    public void outputProjectsWithHunks() {

        List<Bson> pipeline = new ArrayList<>();
// ################# vcs
        Bson vcsLookup = Aggregates.lookup("vcs_system", "_id", "project_id", "vcs_system");
        pipeline.add(vcsLookup);
        Bson vcsUnwind = Aggregates.unwind("$vcs_system");
        pipeline.add(vcsUnwind);

// ################# branch
        List<Variable<String>> branchLet = new ArrayList<>(List.of(new Variable<>("vcs_id", "$vcs_system._id")));
        Bson branchLookupEqId = new Document("$eq", Arrays.asList("$vcs_system_id", "$$vcs_id"));
        Bson branchLookupEqHead = new Document("$eq", Arrays.asList("$is_origin_head", true));
        Bson branchLookupAnd = Filters.expr(Filters.and(branchLookupEqId, branchLookupEqHead));
        Bson branchLookupMatch = Aggregates.match(branchLookupAnd);

        List<Bson> branchLookupPipeline = Arrays.asList(branchLookupMatch);
        Bson branchLookup = Aggregates.lookup("branch",
                branchLet,
                branchLookupPipeline,
                "branch");

        pipeline.add(branchLookup);
        Bson branchUnwind = Aggregates.unwind("$branch");
        pipeline.add(branchUnwind);

// ################# commit
        List<Variable<Document>> commitLet = new ArrayList<>(List.of(new Variable<>("branchName",
                new Document("$concat", List.of("refs/remotes/","$branch.name")))));
        Bson commitLookupInName = new Document("$in",
                Arrays.asList("$$branchName", new Document("$ifNull",
                        Arrays.asList("$branches", new ArrayList<>()))));
//        TODO set date here to filter by command line arg
//        TODO also set end date? for interval?
        Bson commitLookupGteDate = new Document("$gte",
                List.of("$committer_date", LocalDateTime.of(2019, 1, 1, 1, 1)));
        Bson commitLookupAnd = Filters.expr(Filters.and(commitLookupInName, commitLookupGteDate));
        Bson commitLookupMatch = Aggregates.match(commitLookupAnd);

        List<Bson> commitLookupPipeline = Arrays.asList(commitLookupMatch);
        Bson commitLookup = Aggregates.lookup("commit",
                commitLet,
                commitLookupPipeline,
                "commit");

        pipeline.add(commitLookup);
        Bson commitUnwind = Aggregates.unwind("$commit");
        pipeline.add(commitUnwind);

// ################# file_action
        List<Variable<String>> fileActionLet = new ArrayList<>(List.of(new Variable<>("commit_id_let", "$commit._id")));
        Bson fileActionLookupEqId = new Document("$eq", Arrays.asList("$commit_id", "$$commit_id_let"));
        Bson fileActionLookupGtLines = new Document("$gt",
                List.of("$lines_added", 0));
        Bson fileActionLookupAnd = Filters.expr(Filters.and(fileActionLookupEqId, fileActionLookupGtLines));
        Bson fileActionLookupMatch = Aggregates.match(fileActionLookupAnd);

        List<Bson> fileActionLookupPipeline = Arrays.asList(fileActionLookupMatch);
        Bson fileActionLookup = Aggregates.lookup("file_action",
                fileActionLet,
                fileActionLookupPipeline,
                "file_action");

        pipeline.add(fileActionLookup);
        Bson fileActionUnwind = Aggregates.unwind("$file_action");
        pipeline.add(fileActionUnwind);

// ################# hunk
        List<Variable<String>> hunkLet = new ArrayList<>(List.of(new Variable<>("file_action_id_let", "$file_action._id")));
        Bson hunkLookupEqId = new Document("$eq", Arrays.asList("$file_action_id", "$$file_action_id_let"));
        Bson hunkLookupAnd = Filters.expr(Filters.and(hunkLookupEqId));
        Bson hunkLookupMatch = Aggregates.match(hunkLookupAnd);

        List<Bson> hunkLookupPipeline = Arrays.asList(hunkLookupMatch);
        Bson hunkLookup = Aggregates.lookup("hunk",
                hunkLet,
                hunkLookupPipeline,
                "hunk");

        pipeline.add(hunkLookup);
        Bson hunkUnwind = Aggregates.unwind("$hunk");
        pipeline.add(hunkUnwind);

        Bson finalProject = Aggregates.project(fields(
                exclude("_id"),
                include("name"),
                include("commit.committer_date"),
                include("hunk.content"),
                include("hunk._id")));
        pipeline.add(finalProject);

        Bson out = Aggregates.out(COMMENT_DATABASE, HunkDAO.HUNKS_COLLECTION_INTERMED);
        pipeline.add(out);

        projectsCollection.aggregate(pipeline).toCollection();
    }
}
