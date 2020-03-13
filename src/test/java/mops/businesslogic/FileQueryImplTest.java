package mops.businesslogic;

import mops.persistence.file.FileInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class FileQueryImplTest {

    private FileInfo fileInfo;

    @BeforeEach
    void setUp() {
        fileInfo = FileInfo.builder()
                .id(1L)
                .name("cv")
                .directory(1L)
                .type("pdf")
                .size(80L)
                .owner("iTitus")
                .build();
    }

    @Test
    public void findOwnerTest() {
        FileQuery fileQuery = FileQuery.builder()
                .owners(List.of("iTitus"))
                .build();

        assertThat(fileQuery.checkMatch(fileInfo)).isTrue();
    }
}