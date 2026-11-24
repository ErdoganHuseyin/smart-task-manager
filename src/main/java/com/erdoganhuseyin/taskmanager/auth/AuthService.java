package com.erdoganhuseyin.taskmanager.auth;

import com.erdoganhuseyin.taskmanager.auth.dto.LoginRequest;
import com.erdoganhuseyin.taskmanager.auth.dto.RegisterRequest;
import com.erdoganhuseyin.taskmanager.auth.dto.TokenResponse;
import com.erdoganhuseyin.taskmanager.auth.dto.UserResponse;
import com.erdoganhuseyin.taskmanager.security.JwtProperties;
import com.erdoganhuseyin.taskmanager.user.User;
import com.erdoganhuseyin.taskmanager.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final JwtProperties jwtProperties;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       JwtEncoder jwtEncoder, JwtProperties jwtProperties) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
        this.jwtProperties = jwtProperties;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String email = normalize(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyUsedException(email);
        }
        User user = new User(email, passwordEncoder.encode(request.password()));
        return UserResponse.from(userRepository.save(user));
    }

    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(normalize(request.email()))
                .orElseThrow(InvalidCredentialsException::new);
        // matches() hashes the given password with the stored salt and compares the results.
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        return new TokenResponse(createToken(user), "Bearer", jwtProperties.expiration().toSeconds());
    }

    private String createToken(User user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("smart-task-manager")
                .issuedAt(now)
                .expiresAt(now.plus(jwtProperties.expiration()))
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    // "Ali@Mail.com" and "ali@mail.com " should be the same account.
    private static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
