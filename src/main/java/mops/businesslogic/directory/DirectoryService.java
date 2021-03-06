package mops.businesslogic.directory;

import mops.businesslogic.group.GroupRootDirWrapper;
import mops.businesslogic.security.Account;
import mops.exception.MopsException;
import mops.persistence.directory.Directory;
import mops.persistence.permission.DirectoryPermissions;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Handles meta data for directories.
 */
@Service
public interface DirectoryService {

    /**
     * Returns all folders of the parent folder.
     *
     * @param account     user credentials
     * @param parentDirID id of the parent folder
     * @return list of folders
     */
    List<Directory> getSubFolders(Account account, long parentDirID) throws MopsException;

    /**
     * Builds directory path as list.
     *
     * @param dirId highest dir of path
     * @return directory path ordered list
     */
    List<Directory> getDirectoryPath(long dirId) throws MopsException;

    /**
     * Creates the group root directory.
     *
     * @param groupId the group id
     * @return the directory created
     */
    GroupRootDirWrapper getOrCreateRootFolder(long groupId) throws MopsException;

    /**
     * Creates a new folder inside a folder.
     *
     * @param account     user credentials
     * @param parentDirId id of the parent folder
     * @param dirName     name of the new folder
     * @return id of the new folder
     */
    Directory createFolder(Account account, long parentDirId, String dirName) throws MopsException;

    /**
     * Replaces the permissions for a directory and all its parents and children (which use the same permissions object)
     * with the given ones.
     *
     * @param account     user credentials
     * @param dirId       directory id for which the permission should be changed
     * @param permissions new permissions
     * @return the updated directory permissions
     */
    DirectoryPermissions updatePermission(Account account,
                                          long dirId,
                                          DirectoryPermissions permissions) throws MopsException;

    /**
     * Internal use only: possible security flaw!.
     * Check permission before fetching!
     *
     * @param dirId the id of the parent folder
     * @return directory object of the requested folder
     * @throws MopsException on error
     */
    Directory getDirectory(long dirId) throws MopsException;

    /**
     * Internal use only: possible security flaw!.
     * Check permission before saving!
     *
     * @param directory directory to be saved
     * @return directory object of the requested folder
     * @throws MopsException on error
     */
    Directory saveDirectory(Directory directory) throws MopsException;

    /**
     * Internal use only: possible security flaw!.
     * Check permission before deleting!
     *
     * @param directory directory to be deleted
     * @throws MopsException on error
     */
    void deleteDirectory(Directory directory) throws MopsException;

    /**
     * Get the total number of directories in a group.
     *
     * @param groupId group
     * @return directory count
     */
    long getDirCountInGroup(long groupId) throws MopsException;

    /**
     * Get the total number of directories in all groups.
     *
     * @return directory count
     */
    long getTotalDirCount() throws MopsException;

    /**
     * renames a directory.
     *
     * @param account user credentials
     * @param dirId   directory ID
     * @param newName new name
     * @return the directory
     * @throws MopsException on error
     */
    Directory renameDirectory(Account account, long dirId, String newName) throws MopsException;

    /**
     * Fetches list of all root directories.
     *
     * @return all root directories.
     * @throws MopsException on error.
     */
    List<Directory> getAllRootDirectories() throws MopsException;

}
