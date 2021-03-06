package mops.presentation;

import com.c4_soft.springaddons.test.security.context.support.WithIDToken;
import com.c4_soft.springaddons.test.security.context.support.WithMockKeycloackAuth;
import com.c4_soft.springaddons.test.security.web.servlet.request.keycloak.ServletKeycloakAuthUnitTestingSupport;
import mops.businesslogic.directory.DeleteService;
import mops.businesslogic.directory.DirectoryService;
import mops.businesslogic.directory.ZipService;
import mops.businesslogic.file.FileService;
import mops.businesslogic.permission.PermissionService;
import mops.businesslogic.search.SearchService;
import mops.businesslogic.security.SecurityService;
import mops.businesslogic.security.UserPermission;
import mops.exception.MopsException;
import mops.persistence.directory.Directory;
import mops.persistence.permission.DirectoryPermissions;
import mops.presentation.form.EditDirectoryForm;
import mops.presentation.form.FileQueryForm;
import mops.presentation.form.RolePermissionsForm;
import mops.util.KeycloakContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@KeycloakContext
@WebMvcTest(DirectoryController.class)
class DirectoryControllerTest extends ServletKeycloakAuthUnitTestingSupport {

    @MockBean
    DirectoryService directoryService;
    @MockBean
    FileService fileService;
    @MockBean
    SecurityService securityService;
    @MockBean
    PermissionService permissionService;
    @MockBean
    DeleteService deleteService;
    @MockBean
    SearchService searchService;
    @MockBean
    ZipService zipService;

    /**
     * Setups the a Mock MVC Builder.
     */
    @BeforeEach
    void setup() throws MopsException {
        Directory directory = mock(Directory.class);
        Directory root = mock(Directory.class);

        UserPermission userPermission = new UserPermission(true, true, true);
        DirectoryPermissions permissions = DirectoryPermissions.builder()
                .entry("admin", true, true, true)
                .entry("viewer", true, false, false)
                .build();

        given(root.getId()).willReturn(1L);
        given(directory.getId()).willReturn(2L);
        given(directoryService.getDirectory(eq(1L))).willReturn(root);
        given(directoryService.getSubFolders(any(), eq(1L))).willReturn(List.of());
        given(fileService.getFilesOfDirectory(any(), eq(1L))).willReturn(List.of());
        given(directoryService.createFolder(any(), eq(1L), any())).willReturn(directory);
        given(deleteService.deleteFolder(any(), eq(1L))).willReturn(root);
        given(directoryService.updatePermission(any(), eq(1L), any())).willReturn(permissions);
        given(searchService.searchFolder(any(), eq(1L), any())).willReturn(List.of());
        given(securityService.getPermissionsOfUser(any(), any())).willReturn(userPermission);
        given(permissionService.getPermissions(any())).willReturn(permissions);
    }

    /**
     * Tests if the correct view is returned for showing content of a folder.
     */
    @Test
    @WithMockKeycloackAuth(roles = "studentin", idToken = @WithIDToken(email = "user@mail.de"))
    void showContent() throws Exception {
        mockMvc().perform(get("/material1/dir/{dirId}", 1))
                .andExpect(status().isOk())
                .andExpect(view().name("overview"))
                .andDo(document("index/DirectoryController/{method-name}",
                        pathParameters(
                                parameterWithName("dirId").description("The directory id.")
                        )));
    }

    /**
     * Tests the route after uploading a file.
     */
    @Test
    @WithMockKeycloackAuth(roles = "studentin", idToken = @WithIDToken(email = "user@mail.de"))
    void uploadFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain",
                "teststring".getBytes(StandardCharsets.UTF_8));
        mockMvc().perform(fileUpload("/material1/dir/{dirId}/upload", 1)
                .file(file)
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/material1/dir/1"))
                .andDo(document("index/DirectoryController/{method-name}",
                        pathParameters(
                                parameterWithName("dirId").description("The directory id.")
                        )));
    }

    @Test
    @WithMockKeycloackAuth(roles = "studentin", idToken = @WithIDToken(email = "user@mail.de"))
    public void zipDirectory() throws Exception {
        byte[] expected = { 0x46, 0x55, 0x43, 0x4b, 0x20, 0x59, 0x4f, 0x55 };
        doAnswer(invocation -> {
            ByteArrayOutputStream bos = invocation.getArgument(2);
            bos.writeBytes(expected);
            return bos;
        }).when(zipService).zipDirectory(any(), eq(1L), any(OutputStream.class));
        MvcResult result = mockMvc().perform(get("/material1/dir/{dirId}/zip", 1)
                .with(csrf())
                .contentType(MediaType.APPLICATION_OCTET_STREAM))
                .andExpect(status().isOk())
                .andDo(document("index/DirectoryController/{method-name}",
                        pathParameters(
                                parameterWithName("dirId").description("The directory id.")
                        )))
                .andReturn();

        assertThat(result.getResponse().getContentType()).isEqualTo(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        assertThat(result.getResponse().getContentLength()).isEqualTo(expected.length);
        assertThat(result.getResponse().getContentAsByteArray()).isEqualTo(expected);
    }

    /**
     * Tests the route after creating a sub folder. It should be the new folder.
     */
    @Test
    @WithMockKeycloackAuth(roles = "studentin", idToken = @WithIDToken(email = "user@mail.de"))
    void createFolder() throws Exception {
        mockMvc().perform(post("/material1/dir/{dirId}/create", 1)
                .param("folderName", "Vorlesungen")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/material1/dir/2"))
                .andDo(document("index/DirectoryController/{method-name}",
                        pathParameters(
                                parameterWithName("dirId").description("The directory id.")
                        )));
    }

    /**
     * Tests the route after editing a folder.
     */
    @Test
    @WithMockKeycloackAuth(roles = "studentin", idToken = @WithIDToken(email = "user@mail.de"))
    void editFolder() throws Exception {
        EditDirectoryForm editDirectoryForm = new EditDirectoryForm();

        RolePermissionsForm permissionsForm1 = new RolePermissionsForm();
        permissionsForm1.setRole("admin");
        permissionsForm1.setRead(true);
        permissionsForm1.setWrite(true);
        permissionsForm1.setDelete(true);

        RolePermissionsForm permissionsForm2 = new RolePermissionsForm();
        permissionsForm2.setRole("viewer");
        permissionsForm2.setRead(true);
        permissionsForm2.setWrite(true);
        permissionsForm2.setDelete(false);

        editDirectoryForm.setRolePermissions(List.of(permissionsForm1, permissionsForm2));

        mockMvc().perform(post("/material1/dir/{dirId}/edit", 1)
                .flashAttr("editDirectoryForm", editDirectoryForm)
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/material1/dir/1"))
                .andDo(document("index/DirectoryController/{method-name}",
                        pathParameters(
                                parameterWithName("dirId").description("The directory id.")
                        )));
    }

    /**
     * Tests the route after renaming a folder.
     */
    @Test
    @WithMockKeycloackAuth(roles = "studentin", idToken = @WithIDToken(email = "user@mail.de"))
    void renameFolder() throws Exception {
        mockMvc().perform(post("/material1/dir/{dirId}/rename", 1)
                .param("originDirId", String.valueOf(1L))
                .param("newName", "New Name")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/material1/dir/1"))
                .andDo(document("index/DirectoryController/{method-name}",
                        pathParameters(
                                parameterWithName("dirId").description("The directory id.")
                        )));
    }

    /**
     * Tests the route after searching a folder.
     */
    @Test
    @WithMockKeycloackAuth(roles = "studentin", idToken = @WithIDToken(email = "user@mail.de"))
    void searchFolder() throws Exception {
        FileQueryForm fileQueryForm = new FileQueryForm();
        fileQueryForm.setNames(new String[] { "cv" });
        fileQueryForm.setOwners(new String[] { "Thabb" });
        fileQueryForm.setTypes(new String[] { "pdf" });
        fileQueryForm.setTags(new String[] { "awesome" });

        mockMvc().perform(post("/material1/dir/{dirId}/search", 1)
                .flashAttr("fileQueryForm", fileQueryForm)
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("overview"))
                .andDo(document("index/DirectoryController/{method-name}",
                        pathParameters(
                                parameterWithName("dirId").description("The directory id.")
                        )));
    }

    /**
     * Tests if a user can delete a directory.
     */
    @Test
    @WithMockKeycloackAuth(roles = "studentin", idToken = @WithIDToken(email = "user@mail.de"))
    void deleteDirectory() throws Exception {
        mockMvc().perform(post("/material1/dir/{dirId}/delete", 1)
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/material1/dir/1"))
                .andDo(document("index/DirectoryController/{method-name}",
                        pathParameters(
                                parameterWithName("dirId").description("The directory id.")
                        )));
    }

    /**
     * Test if route is secured.
     */
    @Test
    void notSignedIn() throws Exception {
        mockMvc().perform(get("/material1/dir/{dirId}", 1))
                .andExpect(status().is3xxRedirection());
    }
}
