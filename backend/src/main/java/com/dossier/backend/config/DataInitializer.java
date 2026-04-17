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
 * 启动时检查 owners 表，为没有登录凭证的 owner 设置默认账号
 * 默认用户名：owner_{id}，默认密码：888888
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
                // 避免与已有用户名冲突
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
