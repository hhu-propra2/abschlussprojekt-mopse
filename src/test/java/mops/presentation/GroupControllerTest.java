package mops.presentation;

import com.c4_soft.springaddons.test.security.context.support.WithIDToken;
import com.c4_soft.springaddons.test.security.context.support.WithMockKeycloackAuth;
import com.c4_soft.springaddons.test.security.web.servlet.request.keycloak.ServletKeycloakAuthUnitTestingSupport;
import mops.businesslogic.*;
import mops.exception.MopsException;
import mops.persistence.DirectoryPermissionsRepository;
import mops.persistence.DirectoryRepository;
import mops.persistence.FileInfoRepository;
import mops.persistence.FileRepository;
import mops.utils.KeycloakContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@KeycloakContext
@WebMvcTest(GroupController.class)
public class GroupControllerTest extends ServletKeycloakAuthUnitTestingSupport {

    @MockBean
    DirectoryRepository directoryRepository;
    @MockBean
    DirectoryPermissionsRepository directoryPermissionsRepository;
    @MockBean
    FileInfoRepository fileInfoRepository;
    @MockBean
    FileRepository fileRepository;
    @MockBean
    GroupService groupService;
    @MockBean
    FileService fileService;
    @MockBean
    DirectoryService directoryService;

    /**
     * Setup service/repo mocks.
     */
    @BeforeEach
    void setup() throws MopsException {
        given(fileService.getAllFilesOfGroup(any(), eq(1L))).willReturn(List.of());
        given(fileService.searchFilesInGroup(any(), eq(1L), any())).willReturn(List.of());
        given(groupService.getGroupUrl(any(), eq(1L))).willReturn(new GroupDirUrlWrapper(1L));
    }

    /**
     * Tests the API for getting the group url.
     */
    @Test
    @WithMockKeycloackAuth(roles = "studentin", idToken = @WithIDToken(email = "user@mail.de"))
    void getGroupUrl() throws Exception {
        mockMvc().perform(get("/material1/group/{groupId}/url", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("/material1/dir/1"))
                .andExpect(jsonPath("$.group_id").value(1L))
                .andDo(document("index/GroupController/{method-name}",
                        pathParameters(
                                parameterWithName("groupId").description("The group id.")
                        ),
                        responseFields(
                                fieldWithPath(".group_id").description("The id of the group."),
                                fieldWithPath(".url").description("The url of the group.")
                        )));
    }

    /**
     * Tests if all files of the a group are returned.
     */
    @Test
    @WithMockKeycloackAuth(roles = "studentin", idToken = @WithIDToken(email = "user@mail.de"))
    void getAllFilesOfDirectory() throws Exception {
        mockMvc().perform(get("/material1/group/{groupId}", 1))
                .andExpect(status().isOk())
                .andExpect(view().name("files"))
                .andDo(document("index/GroupController/{method-name}",
                        pathParameters(
                                parameterWithName("groupId").description("The group id.")
                        )));
    }

    /**
     * Tests if the correct view is called upon searching in a group.
     */
    @Test
    @WithMockKeycloackAuth(roles = "studentin", idToken = @WithIDToken(email = "user@mail.de"))
    void searchFile() throws Exception {
        FileQuery fileQuery = mock(FileQuery.class);

        mockMvc().perform(post("/material1/group/{groupId}/search", 1)
                .requestAttr("searchQuery", fileQuery)
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("files"))
                .andDo(document("index/GroupController/{method-name}",
                        pathParameters(
                                parameterWithName("groupId").description("The group id.")
                        )));
    }

    /**
     * Test if route is secured.
     */
    @Test
    void notSignedIn() throws Exception {
        mockMvc().perform(get("/material1/group/1"))
                .andExpect(status().is3xxRedirection());
    }
}
