package org.example.services;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.example.daos.HunkDAO;

import java.util.List;

public class HunkService {
    private HunkDAO hunkDAO;

    public HunkService(HunkDAO hunkDAO) {
        this.hunkDAO = hunkDAO;
    }

    public List<Document> getHunks(ObjectId lastSeenId, int limit) {
        return hunkDAO.getHunks(lastSeenId, limit);
    }

    public List<Document> getHunks(int limit) {
        return hunkDAO.getHunks(limit);
    }

    public long getHunksCount() {
        return hunkDAO.getHunksCount();
    }

    public long getHunksCount(ObjectId lastSeenId) {
        return hunkDAO.getHunksCount(lastSeenId);
    }
}
