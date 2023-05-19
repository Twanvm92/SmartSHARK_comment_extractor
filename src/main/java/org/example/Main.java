package org.example;

import com.mongodb.client.MongoClient;
import java.text.ParseException;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.bson.types.ObjectId;
import org.example.daos.CommentDao;
import org.example.daos.ConfigDAO;
import org.example.daos.HunkDAO;
import org.example.daos.ProjectDao;
import org.example.services.CommentService;
import org.example.services.HunkService;
import org.example.services.ProjectService;

public class Main {
  public static void main(String[] args) throws ConfigurationException, ParseException {
    /**
     * The main method for the project. Initializes the necessary components for the program to run
     * and calls the projectService extract and persist comments per project. Uses an
     * applications.properties file as configuration file to instantiate mongodb connections.
     *
     * @param args The command line arguments passed to the program. Currently not used.
     * @throws ConfigurationException If an error occurs while reading the configuration file.
     */
    PropertiesConfiguration config = new PropertiesConfiguration("application.properties");

    MongoDBConfiguration mongoConfig = new MongoDBConfiguration(config);

    MongoClient mongoClient = mongoConfig.getMongoClient();

    ProjectDao projectDao =
        new ProjectDao(
            config.getString("mongodb.database"),
            config.getString("mongodb.database.comments"),
            mongoClient);
    CommentDao commentDao =
        new CommentDao(
            config.getString("mongodb.database"),
            config.getString("mongodb.database.comments"),
            mongoClient);
    HunkDAO hunkDAO =
        new HunkDAO(
            config.getString("mongodb.database"),
            config.getString("mongodb.database.comments"),
            mongoClient);
    ConfigDAO configDAO =
        new ConfigDAO(
            config.getString("mongodb.database"),
            config.getString("mongodb.database.comments"),
            mongoClient);

    HunkService hunkService = new HunkService(hunkDAO);
    CommentService commentService = new CommentService(commentDao);
    ProjectService projectService =
        new ProjectService(projectDao, hunkService, configDAO, commentService);

    //    TODO need to configure this in command line if users want to start processing
    //    hunks from a certain hunk ordered by the creation time of the hunk in the intermediate
    //    database.
    ObjectId lastSeenId = new ObjectId("63095e9d2c57ac341b55dc61");
    //    TODO setup command line interface with an argparser for usability.
    //        TODO need pass limit as one command line argument
    //        TODO also need to pass getInitialHunks for first hunk retrieval and saving
    //        TODO commandline argument outputOriginalHunks and objectId both optional

    //    for first time and general usage: set outputOriginalHunks to true
    projectService.addCommentsByProject(100_000, false);
  }
}
