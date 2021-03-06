package mops.presentation;

import com.c4_soft.springaddons.test.security.context.support.WithIDToken;
import com.c4_soft.springaddons.test.security.context.support.WithMockKeycloackAuth;
import com.c4_soft.springaddons.test.security.web.servlet.request.keycloak.ServletKeycloakAuthUnitTestingSupport;
import mops.businesslogic.directory.DirectoryService;
import mops.businesslogic.group.GroupRootDirWrapper;
import mops.exception.MopsException;
import mops.persistence.directory.Directory;
import mops.presentation.form.FileQueryForm;
import mops.util.KeycloakContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.mockito.BDDMockito.given;
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
class GroupControllerTest extends ServletKeycloakAuthUnitTestingSupport {

    static final long GROUP_ID = 1L;
    static final long DIR_ID = 2L;

    @MockBean
    DirectoryService directoryService;

    /**
     * Setup service/repo mocks.
     */
    @BeforeEach
    void setup() throws MopsException {
        given(directoryService.getOrCreateRootFolder(GROUP_ID))
                .willReturn(new GroupRootDirWrapper(
                        Directory.builder()
                                .id(DIR_ID)
                                .name("root")
                                .permissions(0L)
                                .groupOwner(GROUP_ID)
                                .build()
                ));
    }

    /**
     * Tests the API for getting the group url.
     */
    @Test
    @WithMockKeycloackAuth(roles = "api_user", idToken = @WithIDToken(email = "user@mail.de"))
    void getRootDirectoryUrl() throws Exception {
        mockMvc().perform(get("/material1/group/{groupId}/url", GROUP_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.group_id").value(GROUP_ID))
                .andExpect(jsonPath("$.root_dir_id").value(DIR_ID))
                .andExpect(jsonPath("$.root_dir_url").value("/material1/dir/" + DIR_ID))
                .andDo(document("index/GroupController/{method-name}",
                        pathParameters(
                                parameterWithName("groupId").description("The group id.")
                        ),
                        responseFields(
                                fieldWithPath(".group_id").description("The id of the group."),
                                fieldWithPath(".root_dir_id").description("The id of the group's root directory."),
                                fieldWithPath(".root_dir_url").description("The url of the group's root directory.")
                        )));
    }

    @Test
    @WithMockKeycloackAuth(roles = "studentin", idToken = @WithIDToken(email = "user@mail.de"))
    void getGroupUrlForbidden() throws Exception {
        mockMvc()
                .perform(get("/material1/group/{groupId}/url", GROUP_ID))
                .andExpect(status().isForbidden());
    }

    /**
     * Tests if the redirect to the group's root directory works.
     */
    @Test
    @WithMockKeycloackAuth(roles = "studentin", idToken = @WithIDToken(email = "user@mail.de"))
    void getRootDirectory() throws Exception {
        mockMvc().perform(get("/material1/group/{groupId}", GROUP_ID))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlTemplate("/material1/dir/{dirId}", DIR_ID))
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
        FileQueryForm fileQueryForm = new FileQueryForm();
        fileQueryForm.setNames(new String[] { "cv" });
        fileQueryForm.setOwners(new String[] { "Thabb" });
        fileQueryForm.setTypes(new String[] { "pdf" });
        fileQueryForm.setTags(new String[] { "awesome" });

        mockMvc().perform(post("/material1/group/{groupId}/search", GROUP_ID)
                .flashAttr("fileQueryForm", fileQueryForm)
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlTemplate("/material1/dir/{dirId}/search", DIR_ID))
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
        mockMvc().perform(get("/material1/group/{groupId}", GROUP_ID))
                .andExpect(status().is3xxRedirection());
    }
}
