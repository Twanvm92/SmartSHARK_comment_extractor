package org.example;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;

public class HunkDao {
    private final String SMARTSHARK_DATABASE;
    private MongoDatabase db;
    private MongoClient mongoClient;

    public HunkDao(String SMARTSHARK_DATABASE, MongoClient mongoClient) {
        this.SMARTSHARK_DATABASE = SMARTSHARK_DATABASE;
        this.mongoClient = mongoClient;
        this.db = this.mongoClient.getDatabase(SMARTSHARK_DATABASE);
    }
}
