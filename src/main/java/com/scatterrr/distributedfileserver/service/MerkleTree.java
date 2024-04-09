package com.scatterrr.distributedfileserver.service;

import lombok.NoArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.stream.Collectors;

import static com.scatterrr.distributedfileserver.config.Config.ROOT;

@Service
@NoArgsConstructor
public class MerkleTree {

    public String createMerkleTree(ArrayList<String> chunkLists) {
        String filename = chunkLists.get(0).replaceAll("-\\d+\\.txt", "");;
        ArrayList<String> txnLists = new ArrayList<>(chunkLists.stream()
                .map(chunkPath -> {
                            try {
                                return Files.readString(Paths.get(ROOT + "chunks/" +
                                        filename + "/" + chunkPath));
                            } catch (IOException e) {
                                e.printStackTrace();
                                return null;
                            }
                        }
                )
                .collect(Collectors.toList()));
        return merkleTree(txnLists).get(0);
    }

    public boolean isValid(ArrayList<String> original_list,
                           ArrayList<String> tampered_list) {
        String original_merkleRoot = createMerkleTree(original_list);
        String tampered_merkleRoot = createMerkleTree(tampered_list);
        return original_merkleRoot.equals(tampered_merkleRoot);
    }

    private ArrayList<String> merkleTree(ArrayList<String> hashList) {
        //Return the Merkle Root
        if (hashList.size() == 1) {
            return hashList;
        }
        ArrayList<String> parentHashList = new ArrayList<>();
        //Hash the leaf transaction pair to get parent transaction
        for (int i = 0; i < hashList.size(); i += 2) {
            if (i + 1 == hashList.size()){
                break;
            }
            String hashedString = getSHA(hashList.get(i).concat(hashList.get(i + 1)));
            parentHashList.add(hashedString);
        }
        // If odd number of transactions , add the last transaction again
        if (hashList.size() % 2 == 1) {
            String lastHash = hashList.get(hashList.size() - 1);
            String hashedString = getSHA(lastHash.concat(lastHash));
            parentHashList.add(hashedString);
        }
        return merkleTree(parentHashList);
    }

    public static String getSHA(String input) {
        try {

            // Static getInstance method is called with hashing SHA
            MessageDigest md = MessageDigest.getInstance("SHA-256");

            // digest() method called
            // to calculate message digest of an input
            // and return array of byte
            byte[] messageDigest = md.digest(input.getBytes());

            // Convert byte array into signum representation
            BigInteger no = new BigInteger(1, messageDigest);

            // Convert message digest into hex value
            String hashtext = no.toString(16);

            while (hashtext.length() < 32) {
                hashtext = "0" + hashtext;
            }

            return hashtext;
        }

        // For specifying wrong message digest algorithms
        catch (NoSuchAlgorithmException e) {
            System.out.println("Exception thrown"
                    + " for incorrect algorithm: " + e);

            return null;
        }
    }

}