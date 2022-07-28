package org.example;

import com.mongodb.ConnectionString;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.bson.Document;

import static com.mongodb.client.model.Filters.eq;

public class MongoDBConfiguration {
    private MongoClient mongoClient;
    private PropertiesConfiguration config;

    public MongoDBConfiguration(PropertiesConfiguration config) {
        this.config = config;
    }

    public MongoClient getMongoClient() {
        String uri = config.getString("mongodb.uri");
        String user = config.getString("mongodb.user");
        String password = config.getString("mongodb.password");
        String host = config.getString("mongodb.hostname");
        String port = config.getString("mongodb.port");
        String options = config.getString("mongodb.options");

        String fullUri = String.format(uri, user, password, host, port, options);

//        MongoClient mongoClient = MongoClients.create(fullUri);
//        MongoDatabase database = mongoClient.getDatabase("smartshark_2_2");
//        MongoCollection<Document> collection = database.getCollection("project");
//
//        Document doc = collection.find(eq("name", "ant-ivy")).first();
//        System.out.println(doc.toJson());

        return MongoClients.create(fullUri);
    }
}
