package com.ziminpro.ums.dao;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.ziminpro.ums.dtos.Constants;
import com.ziminpro.ums.dtos.LastSession;
import com.ziminpro.ums.dtos.Roles;
import com.ziminpro.ums.dtos.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcUmsRepository implements UmsRepository {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final Logger log = LoggerFactory.getLogger(JdbcUmsRepository.class);

    @Override
    public Map<UUID, User> findAllUsers() {
        Map<UUID, User> users = new HashMap<>();

        List<Object> oUsers = jdbcTemplate.query(Constants.GET_ALL_USERS,
                (rs, rowNum) -> new User(DaoHelper.bytesArrayToUuid(rs.getBytes("users.id")), rs.getString("users.name"),
                        rs.getString("users.email"), rs.getString("users.password"), rs.getInt("users.created"),
                        Arrays.asList(new Roles(DaoHelper.bytesArrayToUuid(rs.getBytes("roles.id")),
                                rs.getString("roles.name"), rs.getString("roles.description"))),
                        new LastSession(rs.getInt("last_visit.in"), rs.getInt("last_visit.out"))));

        for (Object oUser : oUsers) {
            if (!users.containsKey(((User) oUser).getId())) {
                User user = new User();
                user.setId(((User) oUser).getId());
                user.setName(((User) oUser).getName());
                user.setEmail(((User) oUser).getEmail());
                user.setPassword(((User) oUser).getPassword());
                user.setCreated(((User) oUser).getCreated());
                user.setLastSession(((User) oUser).getLastSession());
                users.put(((User) oUser).getId(), user);
            }
            users.get(((User) oUser).getId()).addRole(((User) oUser).getRoles().get(0));
        }
        return users;
    }

    @Override
    public User findUserByID(UUID userId) {
        User user = new User();
        List<Object> users = jdbcTemplate.query(Constants.GET_USER_BY_ID_FULL,
                (rs, rowNum) -> new User(DaoHelper.bytesArrayToUuid(rs.getBytes("users.id")), rs.getString("users.name"),
                        rs.getString("users.email"), rs.getString("users.password"), rs.getInt("users.created"),
                        Arrays.asList(new Roles(DaoHelper.bytesArrayToUuid(rs.getBytes("roles.id")),
                                rs.getString("roles.name"), rs.getString("roles.description"))),
                        new LastSession(rs.getInt("last_visit.in"), rs.getInt("last_visit.out"))),
                userId.toString());
        for (Object oUser : users) {
            if (user.getId() == null) {
                user.setId(((User) oUser).getId());
                user.setName(((User) oUser).getName());
                user.setEmail(((User) oUser).getEmail());
                user.setPassword(((User) oUser).getPassword());
                user.setCreated(((User) oUser).getCreated());
                user.setLastSession(((User) oUser).getLastSession());
            }
            user.addRole(((User) oUser).getRoles().get(0));
        }
        return user;
    }

    @Override
    public UUID createUser(User user) {
        long timestamp = Instant.now().getEpochSecond();
        Map<String, Roles> roles = this.findAllRoles();
        UUID userId = UUID.randomUUID();

        try {
            jdbcTemplate.update(Constants.CREATE_USER, userId.toString(), user.getName(), user.getEmail(),
                    user.getPassword(), timestamp, null);
            for (Roles role : user.getRoles()) {
                jdbcTemplate.update(Constants.ASSIGN_ROLE, userId.toString(),
                        roles.get(role.getRole()).getRoleId().toString());
            }
        } catch (Exception e) {
            log.error("Ошибка при создании пользователя: ", e);
            return null;
        }

        return userId;
    }

    @Override
    public int deleteUser(UUID userId) {
        return jdbcTemplate.update(Constants.DELETE_USER, userId.toString());
    }

    @Override
    public Map<String, Roles> findAllRoles() {
        Map<String, Roles> roles = new HashMap<>();
        jdbcTemplate.query(Constants.GET_ALL_ROLES, rs -> {
            Roles role = new Roles(DaoHelper.bytesArrayToUuid(rs.getBytes("roles.id")), rs.getString("roles.name"),
                    rs.getString("roles.description"));
            roles.put(rs.getString("roles.name"), role);
        });
        return roles;
    }

    @Override
    public User findUserByEmail(String email) {
        User user = new User();
        try {
            List<Object> users = jdbcTemplate.query(Constants.GET_USER_BY_EMAIL,
                    (rs, rowNum) -> {
                        UUID roleId = null;
                        byte[] roleIdBytes = rs.getBytes("roles.id");
                        if (roleIdBytes != null) {
                            roleId = DaoHelper.bytesArrayToUuid(roleIdBytes);
                        }
                        return new User(DaoHelper.bytesArrayToUuid(rs.getBytes("users.id")), rs.getString("users.name"),
                                rs.getString("users.email"), rs.getString("users.password"), rs.getInt("users.created"),
                                roleId != null ? Arrays.asList(new Roles(roleId,
                                        rs.getString("roles.name"), rs.getString("roles.description")))
                                        : Arrays.asList(),
                                new LastSession(rs.getInt("last_visit.in"), rs.getInt("last_visit.out")));
                    },
                    email);
            for (Object oUser : users) {
                if (user.getId() == null) {
                    user.setId(((User) oUser).getId());
                    user.setName(((User) oUser).getName());
                    user.setEmail(((User) oUser).getEmail());
                    user.setPassword(((User) oUser).getPassword());
                    user.setCreated(((User) oUser).getCreated());
                    user.setLastSession(((User) oUser).getLastSession());
                }
                if (!((User) oUser).getRoles().isEmpty()) {
                    user.addRole(((User) oUser).getRoles().get(0));
                }
            }
        } catch (Exception e) {
            return null;
        }
        return user;
    }

    @Override
    public User findUserByGitHubId(String githubId) {
        User user = new User();
        try {
            List<Object> users = jdbcTemplate.query(Constants.GET_USER_BY_GITHUB_ID,
                    (rs, rowNum) -> {
                        UUID roleId = null;
                        byte[] roleIdBytes = rs.getBytes("roles.id");
                        if (roleIdBytes != null) {
                            roleId = DaoHelper.bytesArrayToUuid(roleIdBytes);
                        }
                        return new User(DaoHelper.bytesArrayToUuid(rs.getBytes("users.id")), rs.getString("users.name"),
                                rs.getString("users.email"), rs.getString("users.password"), rs.getInt("users.created"),
                                roleId != null ? Arrays.asList(new Roles(roleId,
                                        rs.getString("roles.name"), rs.getString("roles.description")))
                                        : Arrays.asList(),
                                new LastSession(rs.getInt("last_visit.in"), rs.getInt("last_visit.out")));
                    },
                    githubId);
            for (Object oUser : users) {
                if (user.getId() == null) {
                    user.setId(((User) oUser).getId());
                    user.setName(((User) oUser).getName());
                    user.setEmail(((User) oUser).getEmail());
                    user.setPassword(((User) oUser).getPassword());
                    user.setCreated(((User) oUser).getCreated());
                    user.setLastSession(((User) oUser).getLastSession());
                }
                if (!((User) oUser).getRoles().isEmpty()) {
                    user.addRole(((User) oUser).getRoles().get(0));
                }
            }
        } catch (Exception e) {
            return null;
        }
        return user;
    }

    @Override
    public UUID createUserWithGitHub(User user, String githubId) {
        long timestamp = Instant.now().getEpochSecond();
        Map<String, Roles> roles = this.findAllRoles();
        UUID userId = UUID.randomUUID();

        try {
            jdbcTemplate.update(Constants.CREATE_USER_WITH_GITHUB, userId.toString(), user.getName(), user.getEmail(),
                    "", githubId, timestamp, null);
            if (user.getRoles() != null) {
                for (Roles role : user.getRoles()) {
                    if (roles.containsKey(role.getRole())) {
                        jdbcTemplate.update(Constants.ASSIGN_ROLE, userId.toString(),
                                roles.get(role.getRole()).getRoleId().toString());
                    }
                }
            }
            Roles subscriberRole = roles.get("SUBSCRIBER");
            if (subscriberRole != null && (user.getRoles() == null || user.getRoles().isEmpty())) {
                jdbcTemplate.update(Constants.ASSIGN_ROLE, userId.toString(),
                        subscriberRole.getRoleId().toString());
            }
        } catch (Exception e) {
            return null;
        }

        return userId;
    }
}
