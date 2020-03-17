package mops.presentation;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mops.businesslogic.Account;
import mops.businesslogic.DirectoryService;
import mops.businesslogic.FileQueryForm;
import mops.businesslogic.FileService;
import mops.businesslogic.query.FileQuery;
import mops.businesslogic.utils.AccountUtil;
import mops.exception.MopsException;
import mops.persistence.directory.Directory;
import mops.persistence.file.FileInfo;
import org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

@Controller
@RequestMapping("/material1/dir")
@AllArgsConstructor
@Slf4j
public class DirectoryController {

    /**
     * Manages all directory queries.
     */
    private final DirectoryService directoryService;

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
    @SuppressWarnings({ "PMD.DataflowAnomalyAnalysis", "PMD.EmptyCatchBlock" })
    public String showFolderContent(KeycloakAuthenticationToken token,
                                    Model model,
                                    @PathVariable("dirId") long dirId) {
        log.info("Folder content for folder with id {} requested.", dirId);
        Account account = AccountUtil.getAccountFromToken(token);
        List<Directory> directories = null;
        List<FileInfo> files = null;
        try {
            directories = directoryService.getSubFolders(account, dirId);
            files = fileService.getFilesOfDirectory(account, dirId);
        } catch (MopsException e) {
            // TODO: Add exception handling, remove PMD warning suppression
            log.error("Failed to retrieve the folder content for directory with id {}", dirId);
        }
        model.addAttribute("dirs", directories);
        model.addAttribute("files", files);
        model.addAttribute("fileQueryForm", new FileQueryForm());
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
    @SuppressWarnings({ "PMD.DataflowAnomalyAnalysis", "PMD.EmptyCatchBlock" })
    public String uploadFile(KeycloakAuthenticationToken token,
                             Model model,
                             @PathVariable("dirId") long dirId,
                             @RequestAttribute("file") MultipartFile multipartFile) {
        log.info("Upload of a file in directory with id {} requested.", dirId);
        Account account = AccountUtil.getAccountFromToken(token);
        try {
            fileService.saveFile(account, dirId, multipartFile, Set.of());
        } catch (MopsException e) {
            // TODO: Add exception handling, remove PMD warning suppression
            log.error("Failed to upload file in directory with id {}", dirId);
        }
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
    @SuppressFBWarnings("NP_NULL_ON_SOME_PATH_EXCEPTION")
    @SuppressWarnings({ "PMD.DataflowAnomalyAnalysis", "PMD.EmptyCatchBlock", "" })
    public String createSubFolder(KeycloakAuthenticationToken token,
                                  Model model,
                                  @PathVariable("parentDirId") long parentDirId,
                                  @RequestAttribute("folderName") String folderName) {
        log.info("Sub folder creation requested in parent folder with id {}", parentDirId);
        Account account = AccountUtil.getAccountFromToken(token);
        Directory directory = null;
        try {
            directory = directoryService.createFolder(account, parentDirId, folderName);
        } catch (MopsException e) {
            // TODO: Add exception handling, remove PMD warning suppression and findbugs warning
            // TODO: this can be done by replacing Directory directory = null; with Directory directory;
            log.error("Failed to create folder in parent directory with id {}", parentDirId);
        }
        //there is no other way
        return String.format("redirect:/material1/dir/%d", directory.getId()); //NOPMD
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
    @SuppressFBWarnings("NP_NULL_ON_SOME_PATH_EXCEPTION")
    @SuppressWarnings({ "PMD.DataflowAnomalyAnalysis", "PMD.EmptyCatchBlock" })
    public String deleteFolder(KeycloakAuthenticationToken token,
                               Model model,
                               @PathVariable("dirId") long dirId) {
        log.info("Deletion of folder with id {} requested", dirId);
        Account account = AccountUtil.getAccountFromToken(token);
        Directory directory = null;
        try {
            directory = directoryService.deleteFolder(account, dirId);
        } catch (MopsException e) {
            // TODO: Add exception handling, remove PMD warning suppression and findbugs warning
            // TODO: this can be done by replacing Directory directory = null; with Directory directory;
            log.error("Failed to delete folder with id {}", dirId);
        }
        //there is no other way
        return String.format("redirect:/material1/dir/%d", directory.getId()); //NOPMD
    }

    /**
     * Searches a folder for files.
     *
     * @param token     user credentials
     * @param model     spring view model
     * @param dirId     id of the folder to be searched
     * @param queryForm wrapper object of the query form parameter
     * @return route to files view
     */
    @PostMapping("/{dirId}/search")
    @SuppressWarnings({ "PMD.DataflowAnomalyAnalysis", "PMD.EmptyCatchBlock", "PMD.LawOfDemeter" })
    public String searchFolder(KeycloakAuthenticationToken token,
                               Model model,
                               @PathVariable("dirId") long dirId,
                               @RequestAttribute("fileQueryForm") FileQueryForm queryForm) {
        log.info("Search in for file in the folder with the id {}.", dirId);
        Account account = AccountUtil.getAccountFromToken(token);
        List<FileInfo> files = null;

        // convert FileQueryForm to FileQuery
        Iterable<String> nameList = Arrays.asList(queryForm.getFileNames());
        Iterable<String> ownerList = Arrays.asList(queryForm.getOwners());
        Iterable<String> typeList = Arrays.asList(queryForm.getTypes());
        Iterable<String> tagList = Arrays.asList(queryForm.getTags());
        FileQuery query = FileQuery.builder()
                .names(nameList)
                .owners(ownerList)
                .types(typeList)
                .tags(tagList)
                .build();

        try {
            files = directoryService.searchFolder(account, dirId, query);
        } catch (MopsException e) {
            // TODO: Add exception handling, remove PMD warning suppression
            log.error("Failed to search in folder with id {}", dirId);
        }
        model.addAttribute("files", files);
        return "files";
    }
}
