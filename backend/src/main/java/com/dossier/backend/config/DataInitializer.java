package com.dossier.backend.config;

import com.dossier.backend.owner.Owner;
import com.dossier.backend.owner.OwnerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * On startup, checks the owners table and assigns default credentials to any owner without them.
 * Default username: owner_{id}, default password: 888888.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private static final String DEFAULT_PASSWORD = "888888";

    private final OwnerRepository ownerRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<Owner> owners = ownerRepository.findAll();
        for (Owner owner : owners) {
            if (owner.getUsername() == null) {
                String username = "owner" + owner.getId();
                // Avoid collisions with existing usernames
                if (ownerRepository.existsByUsername(username)) {
                    username = "owner" + owner.getId() + "_" + System.currentTimeMillis();
                }
                owner.setUsername(username);
                owner.setPasswordHash(passwordEncoder.encode(DEFAULT_PASSWORD));
                ownerRepository.save(owner);
                log.info("DataInitializer: set default credentials for owner id={}, username={}",
                    owner.getId(), username);
            }
        }
    }
}
