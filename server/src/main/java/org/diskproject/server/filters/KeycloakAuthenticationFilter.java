package org.diskproject.server.filters;

import java.io.IOException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.InvalidParameterException;
import java.security.interfaces.RSAPublicKey;
import java.util.Collections;
import java.util.List;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.gargoylesoftware.htmlunit.javascript.host.fetch.Request;
import com.auth0.jwk.JwkException;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.UrlJwkProvider;

@PreMatching
public class KeycloakAuthenticationFilter implements ContainerRequestFilter {
  @Context
  HttpServletRequest request;

  @Override

  public void filter(ContainerRequestContext requestContext) throws IOException {
    // header which contains jwt can be changed
    if (requestContext.getMethod().equals("GET") || requestContext.getMethod().equals("FETCH")) {
      System.out.println("Method: " + requestContext.getMethod());
    } else {
      String token = requestContext.getHeaderString("Authorization");
      if (token == null || "".equals(token)) {
        requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).entity("Access denied").build());
      }
      final JwtValidator validator = new JwtValidator();
      try {
        if (token.contains("Bearer")) {
          token = token.substring(7);
        }
        DecodedJWT jwtToken = validator.validate(token.replaceAll("Bearer ", ""));
        String email = jwtToken.getClaim("email").asString();
        requestContext.setProperty("username", email);
      } catch (InvalidParameterException e) {
        requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).entity("Access denied").build());
        e.printStackTrace();
      }
    }
  }

  public class JwtValidator {

    private final List<String> allowedIsses = Collections
        .singletonList("https://auth.mint.isi.edu/auth/realms/production");

    private String getKeycloakCertificateUrl(DecodedJWT token) {
      return token.getIssuer() + "/protocol/openid-connect/certs";
    }

    private RSAPublicKey loadPublicKey(DecodedJWT token) throws JwkException, MalformedURLException {

      final String url = getKeycloakCertificateUrl(token);
      JwkProvider provider = new UrlJwkProvider(new URL(url));

      return (RSAPublicKey) provider.get(token.getKeyId()).getPublicKey();
    }

    /**
     * Validate a JWT token
     * 
     * @param token
     * @return decoded token
     */
    public DecodedJWT validate(String token) {
      try {
        final DecodedJWT jwt = JWT.decode(token);

        if (!allowedIsses.contains(jwt.getIssuer())) {
          throw new InvalidParameterException(String.format("Unknown Issuer %s", jwt.getIssuer()));
        }

        RSAPublicKey publicKey = loadPublicKey(jwt);

        Algorithm algorithm = Algorithm.RSA256(publicKey, null);
        JWTVerifier verifier = JWT.require(algorithm)
            .withIssuer(jwt.getIssuer())
            .build();

        verifier.verify(token);
        return jwt;

      } catch (Exception e) {
        throw new InvalidParameterException("JWT validation failed: " + e.getMessage());
      }
    }
  }
}
