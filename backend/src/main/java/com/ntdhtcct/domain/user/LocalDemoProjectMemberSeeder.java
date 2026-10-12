package com.ntdhtcct.domain.user;

import com.ntdhtcct.domain.project.ProjectRepository;
import com.ntdhtcct.domain.project.Project;
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
        var adminUser = userRepository.findByEmailIgnoreCase("lanc5676@gmail.com");
        var adminRole = roleRepository.findByName("PROJECT_MANAGER");

        if (adminRole.isEmpty()) {
            return;
        }

        projectRepository.findAll().forEach(project -> {
            demoUser.ifPresent(user -> upsertAdminMember(project, user, adminRole.get()));
            adminUser.ifPresent(user -> upsertAdminMember(project, user, adminRole.get()));
        });
    }

    private void upsertAdminMember(
            Project project,
            com.ntdhtcct.entity.User user,
            com.ntdhtcct.entity.Role adminRole
    ) {
        ProjectMember member = projectMemberRepository
                .findByProjectIdAndUserId(project.getId(), user.getId())
                .orElseGet(() -> new ProjectMember(project, user, adminRole));
        member.setRole(adminRole);
        member.setStatus("ACTIVE");
        projectMemberRepository.save(member);
    }
}