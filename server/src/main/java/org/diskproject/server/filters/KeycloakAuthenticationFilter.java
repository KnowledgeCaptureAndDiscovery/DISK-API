package org.diskproject.server.filters;

import java.io.IOException;
import java.security.interfaces.RSAPublicKey;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.InvalidParameterException;
import java.util.Collections;
import java.util.List;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.auth0.jwk.JwkException;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.UrlJwkProvider;

@PreMatching
public class KeycloakAuthenticationFilter implements ContainerRequestFilter {
    @Context
    HttpServletRequest request;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        // Only filter POSTS
        if (requestContext.getMethod().equals("GET") ||
            requestContext.getMethod().equals("FETCH") ||
            requestContext.getMethod().equals("OPTIONS")) {
                return;
        }

        // Special case for getData.
        UriInfo uriInfo = requestContext.getUriInfo();
        String path = uriInfo.getAbsolutePath().getPath();
        if (path.endsWith("getData"))
            return;

        // All other request must have an authorization token.
        String token = requestContext.getHeaderString("Authorization");
        if (token != null && !token.equals("")) {
            final JwtValidator validator = new JwtValidator();
            try {
                // Remove the Bearer part
                if (token.startsWith("Bearer")) {
                    token = token.substring(7);
                }
                DecodedJWT jwtToken = validator.validate(token);
                String email = jwtToken.getClaim("email").asString();
                requestContext.setProperty("username", email);
                return;
            } catch (InvalidParameterException e) {
                e.printStackTrace();
            }
        }

        requestContext.abortWith(
                Response.status(Response.Status.UNAUTHORIZED)
                        .entity("Access denied").build());
    }

    public class JwtValidator {
        private final List<String> allowedIsses = Collections
                .singletonList("https://auth.mint.isi.edu/realms/production");

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
