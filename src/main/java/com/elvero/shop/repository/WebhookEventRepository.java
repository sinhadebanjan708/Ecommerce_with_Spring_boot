
package com.elvero.shop.repository;

import com.elvero.shop.entity.WebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WebhookEventRepository
        extends JpaRepository<WebhookEvent, Long> {

    boolean existsByGatewayAndEventId(
            String gateway,
            String eventId
    );
}
