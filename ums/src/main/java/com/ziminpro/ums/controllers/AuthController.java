package com.ziminpro.ums.controllers;

import com.ziminpro.ums.dao.UmsRepository;
import com.ziminpro.ums.dtos.*;
import com.ziminpro.ums.security.JwtUtil;
import com.ziminpro.ums.services.GitHubOAuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/auth")
public class
AuthController {

    @Autowired
    private UmsRepository umsRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private GitHubOAuthService gitHubOAuthService;

    @PostMapping("/login")
    public Mono<ResponseEntity<Map<String, Object>>> login(@RequestBody LoginRequest request) {
        Map<String, Object> response = new HashMap<>();

        User user = umsRepository.findUserByEmail(request.getEmail());
        if (user == null || user.getId() == null) {
            response.put(Constants.CODE, "401");
            response.put(Constants.MESSAGE, "Неверный email или пароль");
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response));
        }

        boolean passwordMatch = user.getPassword().equals(request.getPassword()) ||
                passwordEncoder.matches(request.getPassword(), user.getPassword());

        if (!passwordMatch) {
            response.put(Constants.CODE, "401");
            response.put(Constants.MESSAGE, "Неверный email или пароль");
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response));
        }

        List<String> roles = user.getRoles().stream()
                .map(Roles::getRole)
                .collect(Collectors.toList());

        String token = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getName(), roles);

        response.put(Constants.CODE, "200");
        response.put(Constants.MESSAGE, "Успешный вход");
        response.put("token", token);
        response.put(Constants.DATA, new AuthResponse(token, user.getId().toString(), user.getName(), user.getEmail()));

        return Mono.just(ResponseEntity.ok()
                .header(Constants.CONTENT_TYPE, Constants.APPLICATION_JSON)
                .body(response));
    }

    @PostMapping("/register")
    public Mono<ResponseEntity<Map<String, Object>>> register(@RequestBody RegisterRequest request) {
        Map<String, Object> response = new HashMap<>();

        User existingUser = umsRepository.findUserByEmail(request.getEmail());
        if (existingUser != null && existingUser.getId() != null) {
            response.put(Constants.CODE, "400");
            response.put(Constants.MESSAGE, "Пользователь с таким email уже существует");
            return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response));
        }

        User newUser = new User();
        newUser.setName(request.getName());
        newUser.setEmail(request.getEmail());
        newUser.setPassword(passwordEncoder.encode(request.getPassword()));

        if (request.getRoles() != null && !request.getRoles().isEmpty()) {
            newUser.setRoles(request.getRoles());
        } else {
            newUser.setRoles(Arrays.asList(new Roles(null, "SUBSCRIBER", null)));
        }

        UUID userId = umsRepository.createUser(newUser);
        if (userId == null) {
            response.put(Constants.CODE, "500");
            response.put(Constants.MESSAGE, "Ошибка при создании пользователя");
            return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response));
        }

        User createdUser = umsRepository.findUserByID(userId);
        List<String> roles = createdUser.getRoles().stream()
                .map(Roles::getRole)
                .collect(Collectors.toList());

        String token = jwtUtil.generateToken(userId, request.getEmail(), request.getName(), roles);

        response.put(Constants.CODE, "201");
        response.put(Constants.MESSAGE, "Пользователь успешно зарегистрирован");
        response.put("token", token);
        response.put(Constants.DATA, new AuthResponse(token, userId.toString(), request.getName(), request.getEmail()));

        return Mono.just(ResponseEntity.status(HttpStatus.CREATED)
                .header(Constants.CONTENT_TYPE, Constants.APPLICATION_JSON)
                .body(response));
    }

    @PostMapping("/github/callback")
    public Mono<ResponseEntity<Map<String, Object>>> githubCallback(@RequestBody GitHubCallbackRequest request) {
        return gitHubOAuthService.exchangeCodeForToken(request.getCode())
                .flatMap(tokenResponse -> {
                    if (tokenResponse.getError() != null) {
                        Map<String, Object> errorResponse = new HashMap<>();
                        errorResponse.put(Constants.CODE, "400");
                        errorResponse.put(Constants.MESSAGE, tokenResponse.getErrorDescription());
                        return Mono.just(ResponseEntity.badRequest().body(errorResponse));
                    }
                    return gitHubOAuthService.getGitHubUser(tokenResponse.getAccessToken())
                            .flatMap(githubUser -> {
                                String email = githubUser.getEmail();
                                Mono<String> emailMono = email != null ? Mono.just(email)
                                        : gitHubOAuthService.getGitHubUserEmail(tokenResponse.getAccessToken());

                                return emailMono.map(userEmail -> {
                                    Map<String, Object> response = new HashMap<>();
                                    String githubId = String.valueOf(githubUser.getId());

                                    User existingUser = umsRepository.findUserByGitHubId(githubId);

                                    User user;
                                    if (existingUser != null && existingUser.getId() != null) {
                                        user = existingUser;
                                    } else {
                                        User existingByEmail = umsRepository.findUserByEmail(userEmail);
                                        if (existingByEmail != null && existingByEmail.getId() != null) {
                                            user = existingByEmail;
                                        } else {
                                            User newUser = new User();
                                            newUser.setName(githubUser.getName() != null ? githubUser.getName()
                                                    : githubUser.getLogin());
                                            newUser.setEmail(userEmail);
                                            newUser.setRoles(Arrays.asList(new Roles(null, "SUBSCRIBER", null)));

                                            UUID userId = umsRepository.createUserWithGitHub(newUser, githubId);
                                            if (userId == null) {
                                                response.put(Constants.CODE, "500");
                                                response.put(Constants.MESSAGE, "Ошибка при создании пользователя");
                                                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                                        .<Map<String, Object>>body(response);
                                            }
                                            user = umsRepository.findUserByID(userId);
                                        }
                                    }

                                    List<String> roles = user.getRoles().stream()
                                            .map(Roles::getRole)
                                            .collect(Collectors.toList());

                                    String token = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getName(),
                                            roles);

                                    response.put(Constants.CODE, "200");
                                    response.put(Constants.MESSAGE, "Успешная авторизация через GitHub");
                                    response.put("token", token);
                                    response.put(Constants.DATA, new AuthResponse(token, user.getId().toString(),
                                            user.getName(), user.getEmail()));

                                    return ResponseEntity.ok().<Map<String, Object>>body(response);
                                });
                            });
                })
                .onErrorResume(e -> {
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put(Constants.CODE, "500");
                    errorResponse.put(Constants.MESSAGE, "Ошибка при авторизации через GitHub: " + e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse));
                });
    }

    @GetMapping("/me")
    public Mono<ResponseEntity<Map<String, Object>>> getCurrentUser(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        Map<String, Object> response = new HashMap<>();

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.put(Constants.CODE, "401");
            response.put(Constants.MESSAGE, "Требуется авторизация");
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response));
        }

        String token = authHeader.substring(7);

        if (!jwtUtil.validateToken(token)) {
            response.put(Constants.CODE, "401");
            response.put(Constants.MESSAGE, "Недействительный токен");
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response));
        }

        UUID userId = jwtUtil.extractUserId(token);
        User user = umsRepository.findUserByID(userId);

        if (user == null || user.getId() == null) {
            response.put(Constants.CODE, "404");
            response.put(Constants.MESSAGE, "Пользователь не найден");
            return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).body(response));
        }

        response.put(Constants.CODE, "200");
        response.put(Constants.MESSAGE, "Пользователь найден");
        response.put(Constants.DATA, user);

        return Mono.just(ResponseEntity.ok()
                .header(Constants.CONTENT_TYPE, Constants.APPLICATION_JSON)
                .body(response));
    }
}
