package org.example;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseProblemException;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.comments.CommentsCollection;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.bson.Document;

import java.util.List;
import java.util.Optional;

import static com.mongodb.client.model.Filters.eq;

public class Main {
    public static void main(String[] args) throws ConfigurationException {

        PropertiesConfiguration config = new PropertiesConfiguration("application.properties");

        MongoDBConfiguration mongoConfig = new MongoDBConfiguration(config);

        try (MongoClient mongoClient = MongoClients.create(fullUri)) {
            MongoDatabase database = mongoClient.getDatabase("smartshark_2_2");
            MongoCollection<Document> collection = database.getCollection("project");

            Document doc = collection.find(eq("name", "ant-ivy")).first();
            System.out.println(doc.toJson());
        }
//        StaticJavaParser.getConfiguration().setPreprocessUnicodeEscapes(true);
//        CompilationUnit cu = StaticJavaParser.parse("\u002f\u002a <- multi line comment start */\n");
        JavaParser parser = new JavaParser();
        ParseResult<CompilationUnit> pr = parser.parse("+ for (final RDFSyntax s : RDFSyntax.w3cSyntaxes()) { \n" +
                "+ // this is a line comment");


        Optional<CommentsCollection> comments = pr.getCommentsCollection();
        System.out.println(comments.orElseThrow().getComments().first().getContent());
//        if (comments.size() > 0) {
//            System.out.println(comments.get(0).getContent());
//        }
    }

}