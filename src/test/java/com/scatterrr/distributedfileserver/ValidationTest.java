package com.scatterrr.distributedfileserver;

import com.scatterrr.distributedfileserver.service.FileManager;
import com.scatterrr.distributedfileserver.service.MerkleTree;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
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
public class ValidationTest {

    private static final ArrayList<String> chunks = new ArrayList<>(){{
        add("data-1.txt");
        add("data-2.txt");
        add("data-3.txt");
        add("data-4.txt");
        add("data-5.txt");
        add("data-6.txt");
        add("data-7.txt");
        add("data-8.txt");
        add("data-9.txt");
        add("data-10.txt");
        add("data-11.txt");
    }};

    private String originalRoot;
    private static final String fileName = "data";

    @Autowired
    private FileManager fileManager;

    @Autowired
    private MerkleTree merkleTree;

    @Before
    public void setUp() throws Exception{
        byte[] content = Files.readAllBytes(Paths.get(ROOT + fileName + ".txt"));
        MultipartFile inputFile = new MockMultipartFile(fileName + ".txt",
                fileName + ".txt", "text/plain", content);
        ArrayList<String> chunks = fileManager.chunkFile(inputFile);

        File outputDir = new File(ROOT + "output/");
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }
        String merkleRoot = merkleTree.createMerkleTree(chunks);

        FileOutputStream os = new FileOutputStream(ROOT + "output/merkleRoot.txt");
        os.write(merkleRoot.getBytes());
        originalRoot = new String(
                Files.readAllBytes(Paths.get(ROOT + "output/merkleRoot.txt"))
        );
    }

    @Test
    public void validChunksTest() throws Exception{
        String merkleRoot = merkleTree.createMerkleTree(chunks);
        assertEquals(originalRoot, merkleRoot);
    }

    @Test
    public void tamperedChunksTest() throws Exception{
        ArrayList<String> tamperedChunks = new ArrayList<>(chunks);
        String tampredChunk = tamperedChunks.get(0);
        String originalcontent = new String(
                Files.readAllBytes(Paths.get(ROOT + "chunks/" + fileName + "/" + tampredChunk)));

        String tampredcontent = originalcontent + ">";

        FileOutputStream os = new FileOutputStream(ROOT + "chunks/" + fileName + "/" + tampredChunk);
        os.write(tampredcontent.getBytes());

        String merkleRoot = merkleTree.createMerkleTree(tamperedChunks);
        assertNotEquals(originalRoot, merkleRoot);

        os.write(originalcontent.getBytes());
    }

    @Test
    public void missingChunksTest() throws Exception{
        ArrayList<String> tamperedChunks = new ArrayList<>(chunks);
        tamperedChunks.remove(0);

        String merkleRoot = merkleTree.createMerkleTree(tamperedChunks);
        assertNotEquals(originalRoot, merkleRoot);
    }

    @Test
    public void tamperedOriginalRootHashTest() throws Exception{
        String tamperedRoot = originalRoot + ">";
        String merkleRoot = merkleTree.createMerkleTree(chunks);
        assertNotEquals(tamperedRoot, merkleRoot);
    }

    @Test
    public void extraChunksTest() throws Exception{
        String originalRoot = new String(
                Files.readAllBytes(Paths.get(ROOT + "output/merkleRoot.txt"))
        );

        ArrayList<String> tamperedChunks = new ArrayList<>(chunks);
        tamperedChunks.add("data-1.txt");

        String merkleRoot = merkleTree.createMerkleTree(tamperedChunks);
        assertNotEquals(originalRoot, merkleRoot);
    }

    @After
    public void tearDown() throws Exception{
        Path path = Path.of(ROOT + "chunks/");
        Files.walk(path)
                .filter(Files::isRegularFile)
                .map(Path::toFile)
                .forEach(File::deleteOnExit);
        Files.delete(Path.of(ROOT + "output/merkleRoot.txt"));
        new File(ROOT + "chunks/"+ fileName).deleteOnExit();
        new File(ROOT + "chunks/").deleteOnExit();
        new File(ROOT + "output/").deleteOnExit();
    }

}
