package com.example.tasktracker.util;

import com.example.tasktracker.repository.OrganizationRepository;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
@RequiredArgsConstructor
public class GenerateInviteCode {
    private final OrganizationRepository organizationRepository;

    private static final String ALPHABET = "abcdfghijlmnoprtuvwxyz23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    public String getCode() {
        final int MAX_TRIES = 6;

        for (int i = 0; i < MAX_TRIES; i++) {
            String code = generateCode();

            if (!codeExist(code)) {
                return code;
            }
        }

        throw new IllegalStateException(
                "Failed to generate unique invite code"
        );
    }

    public String generateCode(){
        return generateString(4) + "-" + generateString(4);
    }

    public String generateString(int length){
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++){
            sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }

    public boolean codeExist(String code){
        return organizationRepository.existsByInviteCode(code);
    }
}
