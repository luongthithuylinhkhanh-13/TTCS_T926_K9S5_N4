package com.ntdhtcct.domain.user;

import com.ntdhtcct.domain.project.ProjectRepository;
import com.ntdhtcct.domain.project.Project;
import com.ntdhtcct.entity.ProjectMember;
import com.ntdhtcct.repository.ProjectMemberRepository;
import com.ntdhtcct.repository.RoleRepository;
import com.ntdhtcct.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;

import java.util.Optional;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
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
        var demoUser = userRepository.findByEmailIgnoreCase("test@test.com");
        var projectManagers = java.util.List.of(
                "lanc5676@gmail.com",
                "linh.work@gmail.com",
                "kien.work@gmail.com",
                "bach.work@gmail.com",
                "chinh.work@gmail.com",
                "dchinh@chinh.com",
                "lan.work@gmail.com"
        );
        var adminRole = roleRepository.findByName("PROJECT_MANAGER");

        if (adminRole.isEmpty()) {
            return;
        }

        projectRepository.findAll().forEach(project -> {
            demoUser.ifPresent(user -> upsertAdminMember(project, user, adminRole.get()));
            projectManagers.stream()
                    .map(userRepository::findByEmailIgnoreCase)
                    .flatMap(Optional::stream)
                    .forEach(user -> upsertAdminMember(project, user, adminRole.get()));
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