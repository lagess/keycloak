package org.keycloak.models.sessions.infinispan.entities;

import org.infinispan.commons.marshall.SerializeWith;
import org.jboss.logging.Logger;



@SerializeWith(UserSessionEntity.ExternalizerImpl.class)
public class ArtifactResponseEntity extends SessionEntity {

    public static final Logger logger = Logger.getLogger(ArtifactResponseEntity.class);

    private String artifactResponse;

    public String getArtifactResponse() {
        return artifactResponse;
    }

    public void setArtifactResponse(String artifactResponse) {
        this.artifactResponse = artifactResponse;
    }

    @Override
    public boolean equals(Object obj) {
        return false;
    }

    @Override
    public int hashCode() {
        return 0;
    }
}