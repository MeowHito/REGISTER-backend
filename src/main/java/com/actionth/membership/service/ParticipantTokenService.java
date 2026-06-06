package com.actionth.membership.service;

import java.time.Duration;

public interface ParticipantTokenService {
    
    String createToken(String eventId, String participantUuid, Duration ttl);

    String resolveParticipantId(String eventId, String token);
}
