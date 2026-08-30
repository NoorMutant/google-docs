package com.ajaia.docs.config;

import com.ajaia.docs.domain.AppUser;
import com.ajaia.docs.domain.Document;
import com.ajaia.docs.domain.DocumentShare;
import com.ajaia.docs.domain.DocumentVersion;
import com.ajaia.docs.domain.ShareRole;
import com.ajaia.docs.repo.AppUserRepository;
import com.ajaia.docs.repo.DocumentRepository;
import com.ajaia.docs.repo.DocumentShareRepository;
import com.ajaia.docs.repo.DocumentVersionRepository;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * There is no signup screen, so the reviewer accounts are created here on the
 * first start. The password is the same for everyone and is shown on the
 * login page on purpose. This is a demo build, not a production one.
 */
@Configuration
public class DataSeeder {

    public static final String DEMO_PASSWORD = "demo123";

    @Bean
    public ApplicationRunner seedDemoData(AppUserRepository users,
                                          DocumentRepository documents,
                                          DocumentShareRepository shares,
                                          DocumentVersionRepository versions,
                                          PasswordEncoder encoder) {
        return args -> {
            if (users.count() > 0) {
                return;
            }

            String hash = encoder.encode(DEMO_PASSWORD);
            AppUser alice = users.save(new AppUser("alice@ajaia.test", "Alice Bennett", hash));
            AppUser bob = users.save(new AppUser("bob@ajaia.test", "Bob Carter", hash));
            AppUser carol = users.save(new AppUser("carol@ajaia.test", "Carol Diaz", hash));
            users.save(new AppUser("dan@ajaia.test", "Dan Everett", hash));

            Document plan = documents.save(new Document(
                    "Q3 product plan",
                    "<h1>Q3 product plan</h1>"
                            + "<p>This document is seeded so there is something to look at on first login.</p>"
                            + "<h2>Goals</h2>"
                            + "<ul><li>Ship the editor</li><li>Ship sharing</li><li>Keep the scope honest</li></ul>",
                    alice));

            Document notes = documents.save(new Document(
                    "Standup notes",
                    "<h2>Monday</h2><p>Editor toolbar wired up.</p>"
                            + "<h2>Tuesday</h2><p>Sharing roles added.</p>",
                    bob));

            // Alice shares her plan with Bob as an editor and Carol as a viewer,
            // so the two access levels can be seen straight away.
            shares.save(new DocumentShare(plan, bob, ShareRole.EDITOR));
            shares.save(new DocumentShare(plan, carol, ShareRole.VIEWER));
            shares.save(new DocumentShare(notes, alice, ShareRole.VIEWER));

            // Seeded documents start with a version so the history panel has
            // something in it on first open.
            versions.save(new DocumentVersion(plan, 1, alice));
            versions.save(new DocumentVersion(notes, 1, bob));
        };
    }
}
