package mops.businesslogic.directory;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mops.businesslogic.exception.*;
import mops.businesslogic.file.FileInfoService;
import mops.businesslogic.file.query.FileQuery;
import mops.businesslogic.security.Account;
import mops.businesslogic.security.PermissionService;
import mops.businesslogic.security.RoleService;
import mops.businesslogic.security.UserPermission;
import mops.exception.MopsException;
import mops.persistence.DirectoryPermissionsRepository;
import mops.persistence.DirectoryRepository;
import mops.persistence.directory.Directory;
import mops.persistence.file.FileInfo;
import mops.persistence.permission.DirectoryPermissions;
import mops.persistence.permission.DirectoryPermissionsBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Handles meta data for directories.
 */
@Service
@AllArgsConstructor
@Slf4j
public class DirectoryServiceImpl implements DirectoryService {

    /**
     * Represents the role of an admin.
     */
    @Value("${material1.mops.configuration.admin}")
    public static final String ADMIN = "admin";
    /**
     * The max amount of folders per group.
     */
    @Value("${material1.mops.configuration.max-groups}")
    public static final long MAX_FOLDER_PER_GROUP = 200L;

    /**
     * This connects to database related to directory information.
     */
    private final DirectoryRepository directoryRepository;
    /**
     * Handles meta data of files.
     */
    private final FileInfoService fileInfoService;
    /**
     * Handle permission checks for roles.
     */
    private final RoleService roleService;
    /**
     * Handle permission checks for roles.
     */
    private final PermissionService permissionService;
    /**
     * This connects to database to handle directory permissions.
     */
    private final DirectoryPermissionsRepository directoryPermissionsRepo;

    /**
     * {@inheritDoc}
     */
    @Override
    //this is normal behaviour
    @SuppressWarnings({ "PMD.DataflowAnomalyAnalysis", "PMD.CyclomaticComplexity", "PMD.PrematureDeclaration" })
    public UserPermission getPermissionsOfUser(Account account, long dirId) throws MopsException {
        Directory directory = fetchDirectory(dirId);
        boolean write = true;
        boolean read = true;
        boolean delete = true;

        try {
            roleService.checkWritePermission(account, directory);
        } catch (WriteAccessPermissionException e) {
            write = false;
        } catch (MopsException e) {
            throw new MopsException("Keine Berechtigungsprüfung auf Schreiben möglich", e);
        }

        try {
            roleService.checkReadPermission(account, directory);
        } catch (ReadAccessPermissionException e) {
            read = false;
        } catch (MopsException e) {
            throw new MopsException("Keine Berechtigungsprüfung auf Lesen möglich", e);
        }

        try {
            roleService.checkDeletePermission(account, directory);
        } catch (DeleteAccessPermissionException e) {
            delete = false;
        } catch (MopsException e) {
            throw new MopsException("Keine Berechtigungsprüfung auf Löschen möglich", e);
        }

        return new UserPermission(read, write, delete);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Directory> getSubFolders(Account account, long parentDirID) throws MopsException {
        Directory directory = fetchDirectory(parentDirID);
        roleService.checkReadPermission(account, directory);
        return directoryRepository.getAllSubFoldersOfParent(parentDirID);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings({ "PMD.LawOfDemeter", "PMD.OnlyOneReturn" })
    public Directory getOrCreateRootFolder(long groupId) throws MopsException {
        Optional<Directory> optionalDirectory = directoryRepository.getRootFolder(groupId);
        if (optionalDirectory.isPresent()) {
            return optionalDirectory.get();
        }

        Set<String> roleNames = permissionService.fetchRolesInGroup(groupId);
        if (roleNames.isEmpty()) { // TODO: check for actual existence of group
            log.error("A root directory for group '{}' could not be created, as the group does not exist.", groupId);
            String error = "Es konnte kein Wurzelverzeichnis für die Gruppe erstellt werden, da sie nicht existiert.";
            throw new MopsException(error);
        }
        DirectoryPermissions rootPermissions = createDefaultPermissions(roleNames);
        rootPermissions = directoryPermissionsRepo.save(rootPermissions);
        Directory directory = Directory.builder()
                .name("")
                .groupOwner(groupId)
                .permissions(rootPermissions)
                .build(); // no demeter violation here
        return directoryRepository.save(directory);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("PMD.LawOfDemeter")
    public Directory createFolder(Account account, long parentDirId, String dirName) throws MopsException {
        Directory rootDirectory = fetchDirectory(parentDirId);
        long groupFolderCount = getDirCountInGroup(rootDirectory.getGroupOwner());
        if (groupFolderCount >= MAX_FOLDER_PER_GROUP) {
            log.error("The user '{}' tried to create another sub folder for the group with the id {}, "
                            + "but they already reached their max allowed folder count.",
                    account.getName(),
                    parentDirId);
            String error = "Your group has max allowed amount of folders. You can't create any more.";
            throw new StorageLimitationException(error);
        }
        roleService.checkWritePermission(account, rootDirectory);

        Directory directory = Directory.builder() //this is no violation of demeter's law
                .fromParent(rootDirectory)
                .name(dirName)
                .build();
        return directoryRepository.save(directory);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("PMD.LawOfDemeter") //these are not violations of demeter's law
    public Directory deleteFolder(Account account, long dirId) throws MopsException {
        Directory directory = fetchDirectory(dirId);
        roleService.checkDeletePermission(account, directory);

        List<FileInfo> files = fileInfoService.fetchAllFilesInDirectory(dirId);
        List<Directory> subFolders = getSubFolders(account, dirId);

        if (!files.isEmpty() || !subFolders.isEmpty()) {
            log.error("The user '{}' tried to delete the folder with id {}, but the folder was not empty.",
                    account.getName(),
                    dirId);
            String errorMessage = String.format("The directory %s is not empty.", directory.getName());
            throw new DeleteAccessPermissionException(errorMessage);
        }

        Directory parentDirectory = fetchDirectory(directory.getParentId());

        directoryRepository.delete(directory);

        return parentDirectory;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("PMD.LawOfDemeter")
    public List<FileInfo> searchFolder(Account account, long dirId, FileQuery query) throws MopsException {
        Directory directory = fetchDirectory(dirId);
        roleService.checkReadPermission(account, directory);
        List<FileInfo> fileInfos = fileInfoService.fetchAllFilesInDirectory(dirId);

        return fileInfos.stream() //this is a stream not violation of demeter's law
                .filter(query::checkMatch)
                .collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("PMD.LawOfDemeter")
    public DirectoryPermissions updatePermission(Account account,
                                                 long dirId,
                                                 DirectoryPermissions permissions) throws MopsException {
        Directory directory = fetchDirectory(dirId);
        checkIfAdmin(account, directory);
        DirectoryPermissions updatedPermissions = DirectoryPermissions.builder()
                .from(permissions)
                .id(directory.getPermissionsId())
                .build(); // no demeter violation here
        return directoryPermissionsRepo.save(updatedPermissions);
    }

    /**
     * Gets a directory.
     *
     * @param parentDirID the id of the parent folder
     * @return a directory object of the request folder
     */
    @SuppressWarnings("PMD.LawOfDemeter")
    private Directory fetchDirectory(long parentDirID) throws DatabaseException {
        Optional<Directory> optionalDirectory = directoryRepository.findById(parentDirID);
        // this is not a violation of demeter's law
        return optionalDirectory.orElseThrow(getException(parentDirID)); //this is not a violation of demeter's law
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("PMD.LawOfDemeter")
    public Directory getDirectory(long dirId) throws MopsException {
        try {
            return fetchDirectory(dirId);
        } catch (NoSuchElementException e) {
            throw new MopsException("Fehler beim Abrufen des Verzeichnisses", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    public long getDirCountInGroup(long groupId) throws MopsException {
        try {
            return directoryRepository.getDirCountInGroup(groupId);
        } catch (Exception e) {
            log.error("Failed to get total directory count in group with id {}.", groupId);
            throw new DatabaseException("Gesamtordneranzahl konnte nicht geladen werden!", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    public long getTotalDirCount() throws MopsException {
        try {
            return directoryRepository.count();
        } catch (Exception e) {
            log.error("Failed to get total directory count.");
            throw new DatabaseException("Gesamtordneranzahl konnte nicht geladen werden!", e);
        }
    }

    @SuppressWarnings({ "PMD.LawOfDemeter", "PMD.UnusedPrivateMethod" })
    private DirectoryPermissions fetchPermissions(Directory directory) throws DatabaseException {
        Optional<DirectoryPermissions> permissions = directoryPermissionsRepo.findById(directory.getPermissionsId());
        return permissions.orElseThrow(() -> { // this is not a violation of demeter's law
            log.error("The permission for directory with the id {} could not be fetched.",
                    directory.getId());
            String errorMessage = "Permission couldn't be fetched.";
            return new DatabaseException(errorMessage);
        });
    }

    private void checkIfAdmin(Account account, Directory directory) throws MopsException {
        roleService.checkIfRole(account, directory.getGroupOwner(), ADMIN);
    }

    /**
     * Creates the default permissions.
     *
     * @param roleNames all role names existing in the group
     * @return default directory permissions
     */
    //TODO: this is a template and can only implement when GruppenFindung defined their roles.
    @SuppressWarnings({ "PMD.LawOfDemeter" }) //Streams
    private DirectoryPermissions createDefaultPermissions(Set<String> roleNames) {
        DirectoryPermissionsBuilder builder = DirectoryPermissions.builder();
        builder.entry(ADMIN, true, true, true);
        roleNames
                .stream()
                .filter(role -> !role.equals(ADMIN))
                .forEach(role -> builder.entry(role, true, false, false));
        return builder.build();
    }

    /**
     * Gets a database exception.
     *
     * @param dirId directory id
     * @return a supplier to throw a exception
     */
    private Supplier<DatabaseException> getException(long dirId) {
        return () -> { //this is not a violation of the demeter's law
            log.error("The directory with the id {} was requested, but was not found in the database.",
                    dirId);
            String errorMessage = String.format("There is no directory with the id %s in the database.", dirId);
            return new DatabaseException(errorMessage);
        };
    }
}