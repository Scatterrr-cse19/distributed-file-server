package com.scatterrr.distributedfileserver;

import com.scatterrr.distributedfileserver.service.FileManager;
import com.scatterrr.distributedfileserver.service.MerkleTree;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import static com.scatterrr.distributedfileserver.config.Config.ROOT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@SpringBootTest
@RunWith(SpringRunner.class)
@TestPropertySource(locations = "classpath:application-test.properties")
public class ValidationTest {

    private static final ArrayList<byte[]> chunks = new ArrayList<>();

    private static final String fileName = "data";
    private String originalRoot;

    @Autowired
    private FileManager fileManager;

    @Autowired
    private MerkleTree merkleTree;

    @Before
    public void setUp() throws Exception{
        byte[] content = Files.readAllBytes(Paths.get(ROOT + fileName + ".txt"));
        MultipartFile inputFile = new MockMultipartFile(fileName + ".txt",
                fileName + ".txt", "text/plain", content);
        chunks.addAll(fileManager.chunkFile(inputFile));

        String merkleRoot = merkleTree.createMerkleTree(chunks);

        FileOutputStream os = new FileOutputStream(ROOT + "merkleRoot.txt");
        os.write(merkleRoot.getBytes());
        originalRoot = new String(
                Files.readAllBytes(Paths.get(ROOT + "merkleRoot.txt"))
        );
    }

    @Test
    public void validChunksTest() throws Exception{
        String merkleRoot = merkleTree.createMerkleTree(chunks);
        assertEquals(originalRoot, merkleRoot);
    }

    @Test
    public void tamperedChunksTest() throws Exception{
        ArrayList<byte[]> tamperedChunks = new ArrayList<>(chunks);
        byte[] tamperedChunk = tamperedChunks.get(0);
        tamperedChunk[tamperedChunk.length - 1] = (byte) (tamperedChunk[tamperedChunk.length - 1] + 1);

        String merkleRoot = merkleTree.createMerkleTree(tamperedChunks);
        assertNotEquals(originalRoot, merkleRoot);
    }

    @Test
    public void missingChunksTest() throws Exception {
        ArrayList<byte[]> tamperedChunks = new ArrayList<>(chunks);
        tamperedChunks.remove(0);

        String merkleRoot = merkleTree.createMerkleTree(tamperedChunks);
        assertNotEquals(originalRoot, merkleRoot);
    }

    @Test
    public void extraChunksTest() throws Exception {
        ArrayList<byte[]> tamperedChunks = new ArrayList<>(chunks);
        tamperedChunks.add(new byte[0]);

        String merkleRoot = merkleTree.createMerkleTree(tamperedChunks);
        assertNotEquals(originalRoot, merkleRoot);
    }

    @After
    public void tearDown() throws Exception{
        Files.delete(Path.of(ROOT + "merkleRoot.txt"));
    }

}
