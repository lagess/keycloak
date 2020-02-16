package org.keycloak.models.sessions.infinispan.entities;

import org.infinispan.commons.marshall.SerializeWith;
import org.jboss.logging.Logger;



@SerializeWith(UserSessionEntity.ExternalizerImpl.class)
public class ArtifactEntity extends SessionEntity {

    public static final Logger logger = Logger.getLogger(ArtifactEntity.class);


    @Override
    public boolean equals(Object obj) {
        return false;
    }

    @Override
    public int hashCode() {
        return 0;
    }
}