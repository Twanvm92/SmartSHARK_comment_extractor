package org.example;

import com.mongodb.client.MongoClient;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.example.daos.CommentDao;
import org.example.daos.ConfigDAO;
import org.example.daos.HunkDAO;
import org.example.daos.ProjectDao;
import org.example.services.CommentService;
import org.example.services.HunkService;
import org.example.services.ProjectService;


public class Main {
    public static void main(String[] args) throws ConfigurationException {

        PropertiesConfiguration config = new PropertiesConfiguration("application.properties");

        MongoDBConfiguration mongoConfig = new MongoDBConfiguration(config);

        MongoClient mongoClient = mongoConfig.getMongoClient();

        ProjectDao projectDao = new ProjectDao(
                config.getString("mongodb.database"),
                config.getString("mongodb.database.comments"),
                mongoClient);
        CommentDao commentDao = new CommentDao(
                config.getString("mongodb.database"),
                config.getString("mongodb.database.comments"),
                mongoClient);
        HunkDAO hunkDAO = new HunkDAO(
                config.getString("mongodb.database"),
                config.getString("mongodb.database.comments"),
                mongoClient);
        ConfigDAO configDAO = new ConfigDAO(
                config.getString("mongodb.database"),
                config.getString("mongodb.database.comments"),
                mongoClient
        );

        HunkService hunkService = new HunkService(hunkDAO);
        CommentService commentService = new CommentService(commentDao);
        ProjectService projectService = new ProjectService(projectDao, hunkService, configDAO, commentService);

//        TODO need pass limit as one command line argument
//        TODO also need to pass getInitialHunks for first hunk retrieval and saving
        projectService.addCommentsByProject(1_000_000);

    }

}