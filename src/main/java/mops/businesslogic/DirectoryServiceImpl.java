package mops.businesslogic;

import lombok.AllArgsConstructor;
import lombok.NonNull;
import mops.persistence.DirectoryPermissionsRepository;
import mops.persistence.DirectoryRepository;
import mops.persistence.FileInfoRepository;
import mops.persistence.directory.Directory;
import mops.persistence.file.FileInfo;
import mops.persistence.file.FileTag;
import mops.persistence.permission.DirectoryPermissionEntry;
import mops.persistence.permission.DirectoryPermissions;
import mops.security.PermissionService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

@Service
@AllArgsConstructor
public class DirectoryServiceImpl implements DirectoryService {

    /**
     * This connects to database related to directory information.
     */
    private final DirectoryRepository directoryRepository;

    /**
     * This connects to database related to File information.
     */
    private final FileInfoRepository fileInfoRepository;

    /**
     * API for GruppenFindung which handles permissions.
     */
    private final PermissionService permissionService;

    /**
     * This connects to database to handle directory permissions.
     */
    private final DirectoryPermissionsRepository directoryPermissionsRepo;

    /**
     * Uploads a file.
     *
     * @param account       user credentials
     * @param dirId         the id of the folder where the file will be uploaded
     * @param multipartFile the file object
     */
    @Override
    public FileInfo uploadFile(Account account, long dirId, MultipartFile multipartFile, Set<FileTag> fileTags) {

        FileInfo fileInfo = new FileInfo(multipartFile.getName(), dirId, multipartFile.getContentType(),
                multipartFile.getSize(), account.getName(), fileTags);

        fileInfoRepository.save(fileInfo);


        return fileInfo;
    }

    /**
     * Returns all folders of the parent folder.
     *
     * @param account     user credentials
     * @param parentDirID id of the parent folder
     * @return list of folders
     */
    @Override
    public List<Directory> getSubFolders(Account account, long parentDirID) {
        Directory directory = fetchDirectory(parentDirID);
        permissionService.fetchRoleForUserInGroup(account, directory);
        return directoryRepository.getAllSubFoldersOfParent(parentDirID);
    }

    /**
     * Creates the group root directory.
     *
     * @param account user credentials
     * @param groupId the group id
     * @return the directory created
     */
    @Override
    public Directory createRootFolder(Account account, Long groupId) {
        Directory directory = new Directory();
        directory.setName(groupId.toString());
        directory.setGroupOwner(groupId);
        Set<DirectoryPermissionEntry> permissions = permissionService.fetchRoleForUserInGroup(account, directory);
        DirectoryPermissions permission = new DirectoryPermissions(permissions);
        DirectoryPermissions rootPermissions = directoryPermissionsRepo.save(permission);
        directory.setPermission(rootPermissions);
        return directoryRepository.save(directory);
    }

    /**
     * Creates a new folder inside a folder.
     *
     * @param account     user credentials
     * @param parentDirId id of the parent folder
     * @param dirName     name of the new folder
     * @return id of the new folder
     */
    @Override
    public Directory createFolder(Account account, Long parentDirId, String dirName) {
        Directory rootDirectory = fetchDirectory(parentDirId);
        checkWritePermission(account, rootDirectory);
        Directory directory = rootDirectory.createSubDirectory(dirName); //NOPMD// this is no violation of demeter's law
        return directoryRepository.save(directory);
    }

    /**
     * Deletes a folder.
     *
     * @param account user credential
     * @param dirId   id of the folder to be deleted
     * @return the parent id of the deleted folder
     */
    @Override
    public long deleteFolder(Account account, long dirId) {
        return 0;
    }

    /**
     * Searches a folder for files.
     *
     * @param account user credentials
     * @param dirId   id of the folder to be searched
     * @param query   wrapper object of the query parameter
     * @return list of files
     */
    @Override
    public List<FileInfo> searchFolder(Account account, long dirId, FileQuery query) {
        return null;
    }


    /**
     * @param parentDirID the id of the parent folder
     * @return a directory object of the request folder
     */
    private Directory fetchDirectory(long parentDirID) {
        Optional<Directory> optionalDirectory = directoryRepository.findById(parentDirID);
        return optionalDirectory.orElseThrow(() -> { //NOPMD
            String errorMessage = String.format("There is no directory with the id: %d in the database.", parentDirID);
            return new NoSuchElementException(errorMessage);
        });
    }
}