package com.ntdhtcct.domain.user;

import com.ntdhtcct.domain.project.ProjectRepository;
import com.ntdhtcct.entity.ProjectMember;
import com.ntdhtcct.repository.ProjectMemberRepository;
import com.ntdhtcct.repository.RoleRepository;
import com.ntdhtcct.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

@Component
@Order(4)
public class LocalDemoProjectMemberSeeder implements CommandLineRunner {

    private final Environment environment;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;

    public LocalDemoProjectMemberSeeder(
            Environment environment,
            ProjectRepository projectRepository,
            ProjectMemberRepository projectMemberRepository,
            RoleRepository roleRepository,
            UserRepository userRepository
    ) {
        this.environment = environment;
        this.projectRepository = projectRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
    }

    @Override
    public void run(String... args) {
        boolean isH2 = "org.h2.Driver".equals(
                environment.getProperty("spring.datasource.driver-class-name")
        );
        boolean isLocalDemoProfile = environment.acceptsProfiles(
                Profiles.of("local", "demo")
        );
        if (!isH2 && !isLocalDemoProfile) {
            return;
        }

        var demoUser = userRepository.findByEmailIgnoreCase("test@test.com");
        var adminRole = roleRepository.findByName("ADMIN");

        if (demoUser.isEmpty() || adminRole.isEmpty()) {
            return;
        }

        projectRepository.findAll().forEach(project -> {
            ProjectMember member = projectMemberRepository
                    .findByProjectIdAndUserId(project.getId(), demoUser.get().getId())
                    .orElseGet(() -> new ProjectMember(
                            project,
                            demoUser.get(),
                            adminRole.get()
                    ));
            member.setRole(adminRole.get());
            member.setStatus("ACTIVE");
            projectMemberRepository.save(member);
        });
    }
}