package in.onenotify.auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TokenService {
  private final Algorithm algorithm;

  public TokenService(@Value("${app.jwt-secret}") String secret) {
    if (secret.length() < 48)
      throw new IllegalStateException("JWT secret must be at least 48 characters");
    algorithm = Algorithm.HMAC256(secret);
  }

  public String issue(UUID user, UUID session) {
    return JWT.create()
        .withIssuer("onenotify")
        .withSubject(user.toString())
        .withClaim("sid", session.toString())
        .withIssuedAt(Instant.now())
        .withExpiresAt(Instant.now().plusSeconds(900))
        .sign(algorithm);
  }

  public record Identity(UUID user, UUID session) {}

  public Identity verify(String token) {
    var verified = JWT.require(algorithm).withIssuer("onenotify").build().verify(token);
    return new Identity(
        UUID.fromString(verified.getSubject()),
        UUID.fromString(verified.getClaim("sid").asString()));
  }
}
