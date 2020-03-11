package mops.presentation;

import lombok.AllArgsConstructor;
import mops.businesslogic.*;
import mops.businesslogic.utils.AccountUtil;
import mops.persistence.directory.Directory;
import mops.persistence.file.FileInfo;
import mops.security.DeleteAccessPermission;
import mops.security.ReadAccessPermission;
import mops.security.exception.WriteAccessPermission;
import org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;

@Controller
@RequestMapping("/material1/dir")
@AllArgsConstructor
public class DirectoryController {

    /**
     * Manages all directory queries.
     */
    private final DirectoryService directoryService;

    /**
     * Manges all meta data file queries.
     */
    private final FileInfoService fileInfoService;

    /**
     * Manges all file queries.
     */
    private final FileService fileService;

    /**
     * @param token keycloak auth token
     * @param model spring view model
     * @param dirId id of the folder
     * @return route to folder
     */
    @GetMapping("/{dirId}")
    public String showFolderContent(KeycloakAuthenticationToken token,
                                    Model model,
                                    @PathVariable("dirId") long dirId) throws ReadAccessPermission {
        Account account = AccountUtil.getAccountFromToken(token);
        List<Directory> directories = directoryService.getSubFolders(account, dirId);
        List<FileInfo> files = fileService.getFilesOfDirectory(account, dirId);
        model.addAttribute("dirs", directories);
        model.addAttribute("files", files);
        return "directory";
    }

    /**
     * Uploads a file.
     *
     * @param token         keycloak auth token
     * @param model         spring view model
     * @param dirId         id of the directory id where it will be uploaded
     * @param multipartFile file object
     * @return route after completion
     */
    @PostMapping("/{dirId}/upload")
    public String uploadFile(KeycloakAuthenticationToken token,
                             Model model,
                             @PathVariable("dirId") long dirId,
                             @Param("file") MultipartFile multipartFile) {
        Account account = AccountUtil.getAccountFromToken(token);
        //TODO: exception handling and user error message
        fileService.uploadFile(account, dirId, multipartFile, Set.of());
        return String.format("redirect:/material1/dir/%d", dirId);
    }

    /**
     * Creates a new sub folder.
     *
     * @param token       keycloak auth token
     * @param model       spring view model
     * @param parentDirId id of the parent folder
     * @param folderName  name of the new sub folder
     * @return object of the folder
     */
    @PostMapping("/{parentDirId}/create")
    public String createSubFolder(KeycloakAuthenticationToken token,
                                  Model model,
                                  @PathVariable("parentDirId") long parentDirId,
                                  @RequestAttribute("folderName") String folderName) throws WriteAccessPermission {
        Account account = AccountUtil.getAccountFromToken(token);
        Directory directory = directoryService.createFolder(account, parentDirId, folderName);
        return String.format("redirect:/material1/dir/%d", directory.getId());
    }

    /**
     * Deletes a folder.
     *
     * @param token user credentials
     * @param model spring view model
     * @param dirId id of the folder to be deleted
     * @return the id of the parent folder
     */
    @DeleteMapping("/{dirId}")
    public String deleteFolder(KeycloakAuthenticationToken token,
                               Model model,
                               @PathVariable("dirId") long dirId) throws DeleteAccessPermission, ReadAccessPermission {
        Account account = AccountUtil.getAccountFromToken(token);
        Directory directory = directoryService.deleteFolder(account, dirId);
        return String.format("redirect:/material1/dir/%d", directory.getId());
    }

    /**
     * Searches a folder for files.
     *
     * @param token user credentials
     * @param model spring view model
     * @param dirId id of the folder to be searched
     * @param query wrapper object of the query parameter
     * @return route to files view
     */
    @PostMapping("/{dirId}/search")
    public String searchFolder(KeycloakAuthenticationToken token,
                               Model model,
                               @PathVariable("dirId") long dirId,
                               @ModelAttribute("searchQuery") FileQuery query) {
        Account account = AccountUtil.getAccountFromToken(token);
        List<FileInfo> files = directoryService.searchFolder(account, dirId, query);
        model.addAttribute("files", files);
        return "files";
    }
}

