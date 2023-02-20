package org.example.services;

import java.util.List;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.example.daos.HunkDAO;

public class HunkService {
  private final HunkDAO hunkDAO;

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
