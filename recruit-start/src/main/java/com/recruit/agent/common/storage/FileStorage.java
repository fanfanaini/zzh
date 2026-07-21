package com.recruit.agent.common.storage;

public interface FileStorage {

    StoredFile store(FileUpload upload);

    StoredFileContent open(String objectName);

    void delete(String objectName);
}
