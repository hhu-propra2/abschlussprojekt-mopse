package mops.businesslogic;

import mops.persistence.Directory;
import mops.persistence.FileInfo;

import java.util.List;

public interface DirectoryService {
    /**
     * Uploads a file.
     *
     * @param account  user credentials
     * @param dirId    the id of the folder where the file will be uploaded
     * @param fileInfo the file object
     */
    void uploadFile(Account account, int dirId, FileInfo fileInfo);

    /**
     * Returns all folders of the parent folder.
     *
     * @param account user credentials
     * @param dirId   id of the folder
     * @return list of folders
     */
    List<Directory> getSubFolders(Account account, int dirId);

    /**
     * Creates a new folder inside a folder.
     *
     * @param account user credentials
     * @param dirId   id of the parent folder
     * @param dirName name of the new folder
     * @return id of the new folder
     */
    int createFolder(Account account, int dirId, String dirName);

    /**
     * Deletes a folder.
     *
     * @param account user credential
     * @param dirId   id of the folder to be deleted
     * @return the parent id of the deleted folder
     */
    int deleteFolder(Account account, int dirId);
}
