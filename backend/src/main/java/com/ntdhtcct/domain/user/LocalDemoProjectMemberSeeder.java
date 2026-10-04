package com.ntdhtcct.domain.user;

import com.ntdhtcct.domain.project.ProjectRepository;
import com.ntdhtcct.entity.ProjectMember;
import com.ntdhtcct.repository.ProjectMemberRepository;
import com.ntdhtcct.repository.RoleRepository;
import com.ntdhtcct.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(4)
@ConditionalOnProperty(
        prefix = "spring.datasource",
        name = "driver-class-name",
        havingValue = "org.h2.Driver"
)
public class LocalDemoProjectMemberSeeder implements CommandLineRunner {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;

    public LocalDemoProjectMemberSeeder(
            ProjectRepository projectRepository,
            ProjectMemberRepository projectMemberRepository,
            RoleRepository roleRepository,
            UserRepository userRepository
    ) {
        this.projectRepository = projectRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
    }

    @Override
    public void run(String... args) {
        var demoUser = userRepository.findByEmailIgnoreCase("test@test.com");
        var adminRole = roleRepository.findByName("ADMIN");

        if (demoUser.isEmpty() || adminRole.isEmpty()) {
            return;
        }

        projectRepository.findAll().forEach(project -> {
            if (!projectMemberRepository.existsByProjectIdAndUserIdAndStatus(
                    project.getId(), demoUser.get().getId(), "ACTIVE")) {
                projectMemberRepository.save(
                        new ProjectMember(project, demoUser.get(), adminRole.get())
                );
            }
        });
    }
}