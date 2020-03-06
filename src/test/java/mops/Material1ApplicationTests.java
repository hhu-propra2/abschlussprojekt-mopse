package mops;

import mops.businesslogic.FileService;
import mops.businesslogic.GroupService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest
class Material1ApplicationTests {

    /**
     * Necessary mock until GroupService is implemented.
     */
    @MockBean
    private GroupService groupService;

    /**
     * Necessary mock until FileService is implemented.
     */
    @MockBean
    private FileService fileService;

    @Test
    void contextLoads() {
    }
}
