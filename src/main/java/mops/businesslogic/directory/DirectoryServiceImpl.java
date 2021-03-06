package mops.businesslogic.directory;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mops.businesslogic.exception.DatabaseException;
import mops.businesslogic.exception.EmptyNameException;
import mops.businesslogic.exception.StorageLimitationException;
import mops.businesslogic.exception.WriteAccessPermissionException;
import mops.businesslogic.group.GroupRootDirWrapper;
import mops.businesslogic.group.GroupService;
import mops.businesslogic.permission.PermissionService;
import mops.businesslogic.security.Account;
import mops.businesslogic.security.SecurityService;
import mops.businesslogic.security.UserPermission;
import mops.exception.MopsException;
import mops.persistence.DirectoryRepository;
import mops.persistence.directory.Directory;
import mops.persistence.directory.DirectoryBuilder;
import mops.persistence.group.Group;
import mops.persistence.permission.DirectoryPermissions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.relational.core.conversion.DbActionExecutionException;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Handles meta data for directories.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DirectoryServiceImpl implements DirectoryService {

    /**
     * This connects to database related to directory information.
     */
    private final DirectoryRepository directoryRepository;
    /**
     * Handle permission checks for roles.
     */
    private final SecurityService securityService;
    /**
     * Handle permissions storage and retrieval.
     */
    private final PermissionService permissionService;
    /**
     * Connects to our group database.
     */
    private final GroupService groupService;
    /**
     * Represents the role of an admin.
     */
    @Value("${material1.mops.configuration.role.admin}")
    private String adminRole = "admin";
    /**
     * The max amount of folders per group.
     */
    @SuppressWarnings("checkstyle:MagicNumber")
    @Value("${material1.mops.configuration.quota.max-folders-in-group}")
    private long maxFoldersPerGroup = 200L;

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("PMD.LawOfDemeter")
    public List<Directory> getSubFolders(Account account, long parentDirID) throws MopsException {
        Directory directory = getDirectory(parentDirID);
        securityService.checkReadPermission(account, directory);
        try {
            List<Directory> directories = new ArrayList<>(directoryRepository.getAllSubFoldersOfParent(parentDirID));
            directories.sort(Directory.NAME_COMPARATOR);
            if (directory.isRoot()) {
                // If the current dir is the root folder,
                // there could be directories in it without
                // reading permission
                directories = removeNoReadPermissionDirectories(account, directories);
            }
            return directories;
        } catch (DataAccessException | IllegalArgumentException | DbActionExecutionException e) {
            log.error("Subfolders of parent folder with id '{}' could not be loaded:", parentDirID, e);
            throw new DatabaseException("Unterordner konnten nicht geladen werden.", e);
        }
    }

    /**
     * Removes all directories without reading permissions.
     *
     * @param account     the account
     * @param directories all directories that should be checked
     * @return filtered list
     * @throws MopsException on error
     */
    @SuppressWarnings("PMD.LawOfDemeter")
    private List<Directory> removeNoReadPermissionDirectories(Account account,
                                                              List<Directory> directories) throws MopsException {
        List<Directory> readableFolders = new ArrayList<>();
        for (Directory dir : directories) {
            boolean readPerm = securityService.getPermissionsOfUser(account, dir).isRead();
            if (readPerm) {
                readableFolders.add(dir);
            }
        }
        return readableFolders;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings({ "PMD.LawOfDemeter", "PMD.DataflowAnomalyAnalysis" })
    public List<Directory> getDirectoryPath(long dirId) throws MopsException {
        List<Directory> result = new LinkedList<>();
        Directory dir = getDirectory(dirId);
        while (!dir.isRoot()) {
            result.add(dir);
            dir = getDirectory(dir.getParentId());
        }
        // add root
        result.add(dir);
        //reversing list
        Collections.reverse(result);
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings({ "PMD.LawOfDemeter", "PMD.OnlyOneReturn", "PMD.DataflowAnomalyAnalysis" })
    public GroupRootDirWrapper getOrCreateRootFolder(long groupId) throws MopsException {
        Optional<GroupRootDirWrapper> optRootDir;
        try {
            optRootDir = directoryRepository
                    .getRootFolder(groupId)
                    .map(GroupRootDirWrapper::new);
        } catch (DataAccessException | IllegalArgumentException | DbActionExecutionException e) {
            log.error("Error while searching for root directory of group with id '{}':", groupId, e);
            throw new DatabaseException("Das Wurzelverzeichnis konnte nicht gefunden werden.", e);
        }

        if (optRootDir.isPresent()) {
            return optRootDir.get();
        }

        Group group = groupService.getGroup(groupId); // implicit check of existence

        DirectoryPermissions rootPermissions = groupService.getDefaultPermissions(groupId);
        rootPermissions = permissionService.savePermissions(rootPermissions);
        Directory directory = Directory.builder()
                .name(group.getName())
                .groupOwner(groupId)
                .permissions(rootPermissions)
                .build(); // no demeter violation here
        return new GroupRootDirWrapper(saveDirectory(directory));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings({ "PMD.LawOfDemeter", "PMD.AvoidReassigningParameters" })
    public Directory createFolder(Account account, long parentDirId, String dirName) throws MopsException {
        if (dirName.isEmpty()) {
            log.error("The user '{}' tried to create a sub folder with an empty name.", account.getName());
            throw new DatabaseException("Name leer.");
        }

        dirName = dirName.replaceAll("[^a-zA-Z0-9._-]", "_");

        Directory parentDir = getDirectory(parentDirId);
        long groupFolderCount = getDirCountInGroup(parentDir.getGroupOwner());
        if (groupFolderCount >= maxFoldersPerGroup) {
            log.error("The user '{}' tried to create another sub folder in the group with the id {}, "
                            + "but they already reached their max allowed folder count.",
                    account.getName(),
                    parentDirId);
            String error = "Deine Gruppe hat die maximale Anzahl an Ordnern erreicht. "
                    + "Du kannst keine weiteren mehr erstellen.";
            throw new StorageLimitationException(error);
        }
        securityService.checkWritePermission(account, parentDir);

        DirectoryBuilder builder = Directory.builder() //this is no violation of demeter's law
                .fromParent(parentDir)
                .name(dirName);

        if (parentDir.isRoot()) {
            DirectoryPermissions parentPermissions = permissionService.getPermissions(parentDir);
            DirectoryPermissions permissions = DirectoryPermissions.builder()
                    .from(parentPermissions)
                    .id((Long) null)
                    .build();
            DirectoryPermissions savedPermissions = permissionService.savePermissions(permissions);
            builder.permissions(savedPermissions);
        }

        Directory directory = builder.build();
        return saveDirectory(directory);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("PMD.LawOfDemeter")
    public DirectoryPermissions updatePermission(Account account,
                                                 long dirId,
                                                 DirectoryPermissions permissions) throws MopsException {
        Directory directory = getDirectory(dirId);
        securityService.checkIfRole(account, directory.getGroupOwner(), adminRole);

        Set<String> roles = groupService.getRoles(directory.getGroupOwner());
        if (!permissions.getRoles().equals(roles)) {
            log.error("The user '{}' tried to change the permissions of a directory to an invalid one. "
                            + "Role Permissions are missing or superfluous.",
                    account.getName());
            throw new DatabaseException("Neue Berechtigungen ungültig.");
        }

        if (!permissions.isAllowedToRead(adminRole)
                || !permissions.isAllowedToWrite(adminRole)
                || !permissions.isAllowedToDelete(adminRole)) {
            log.error("The user '{}' tried to change the permissions of the admin role.", account.getName());
            throw new DatabaseException("Neue Berechtigungen ungültig.");
        }

        DirectoryPermissions updatedPermissions = DirectoryPermissions.builder()
                .from(permissions)
                .id(directory.getPermissionsId())
                .build(); // no demeter violation here
        return permissionService.savePermissions(updatedPermissions);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("PMD.LawOfDemeter")
    public Directory getDirectory(long dirId) throws MopsException {
        try {
            return directoryRepository.findById(dirId).orElseThrow();
        } catch (DataAccessException | IllegalArgumentException | DbActionExecutionException
                | NoSuchElementException e) {
            log.error("The directory with the id '{}' was requested, but was not found in the database:", dirId, e);
            String error = String.format("Der Ordner mit der ID '%d' konnte nicht gefunden werden.", dirId);
            throw new DatabaseException(error, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Directory saveDirectory(Directory directory) throws MopsException {
        try {
            return directoryRepository.save(directory);
        } catch (DataAccessException | IllegalArgumentException | DbActionExecutionException e) {
            log.error("The directory with the id '{}' could not be saved to the database:", directory, e);
            String error = String.format("Der Ordner '%s' konnte nicht gespeichert werden.", directory.getName());
            if (e.getCause() instanceof DuplicateKeyException) {
                error = String.format("Der Ordner '%s' existiert bereits.", directory.getName());
            }
            throw new DatabaseException(error, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteDirectory(Directory directory) throws MopsException {
        try {
            directoryRepository.delete(directory);
        } catch (DataAccessException | IllegalArgumentException | DbActionExecutionException e) {
            log.error("The directory '{}' could not be deleted from the database:", directory, e);
            String error = String.format("Der Ordner '%s' konnte nicht gelöscht werden.", directory);
            throw new DatabaseException(error, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getDirCountInGroup(long groupId) throws MopsException {
        try {
            return directoryRepository.getDirCountInGroup(groupId);
        } catch (DataAccessException | IllegalArgumentException | DbActionExecutionException e) {
            log.error("Failed to get total directory count in group with id '{}':", groupId, e);
            throw new DatabaseException("Gesamtordneranzahl konnte nicht geladen werden!", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getTotalDirCount() throws MopsException {
        try {
            return directoryRepository.count();
        } catch (DataAccessException | IllegalArgumentException | DbActionExecutionException e) {
            log.error("Failed to get total directory count:", e);
            throw new DatabaseException("Gesamtordneranzahl konnte nicht geladen werden!", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings({ "PMD.AvoidReassigningParameters", "PMD.LawOfDemeter" })
    public Directory renameDirectory(Account account, long dirId, String newName) throws MopsException {
        if (newName.isEmpty()) {
            log.error("User {} tried to rename a directory without a name.",
                    account.getName()
            );
            throw new EmptyNameException("Der Dateiname darf nicht leer sein.");
        }

        Directory directory = getDirectory(dirId);

        if (directory.isRoot()) {
            log.error("User {} tried to rename the root folder of group '{}'.",
                    account.getName(), directory.getGroupOwner());
            throw new WriteAccessPermissionException("Keine Berechtigung um das Wurzelverzeichnis umzubenennen.");
        }

        UserPermission permissionsOfUser = securityService.getPermissionsOfUser(account, directory);

        if (!permissionsOfUser.isDelete() || !permissionsOfUser.isWrite()) {
            log.error("User {} tried to rename a directory without write and delete permission.",
                    account.getName()
            );
            throw new WriteAccessPermissionException("Keine Schreibberechtigung und Löschberechtigung");
        }

        newName = newName.replaceAll("[^a-zA-Z0-9.\\-]", "_");
        directory.setName(newName);
        return directoryRepository.save(directory);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Directory> getAllRootDirectories() throws MopsException {
        try {
            return directoryRepository.getAllRootDirectories();
        } catch (DataAccessException | IllegalArgumentException | DbActionExecutionException e) {
            log.error("Failed to get all root directories:", e);
            throw new DatabaseException("Die Wurzelverzeichnisse konnten nicht geladen werden!", e);
        }
    }
}
