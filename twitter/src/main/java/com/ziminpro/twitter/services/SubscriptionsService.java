package com.ziminpro.twitter.services;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.ziminpro.twitter.dao.SubscriptionRepository;
import com.ziminpro.twitter.dtos.Constants;
import com.ziminpro.twitter.dtos.HttpResponseExtractor;
import com.ziminpro.twitter.dtos.Roles;
import com.ziminpro.twitter.dtos.Subscription;
import com.ziminpro.twitter.dtos.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;

@Service
public class SubscriptionsService {

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private UMSConnector umsConnector;

    @Value("${ums.paths.user}")
    private String uriUser;

    public Mono<ResponseEntity<Map<String, Object>>> getSubscriptionsForSubscriberById(UUID subscriberId, String token) {
        return umsConnector.retrieveUmsData(uriUser + "/" + subscriberId.toString(), token).flatMap(res -> {
            Map<String, Object> response = new HashMap<>();

            Subscription subscriptions = new Subscription();
            User user = HttpResponseExtractor.extractDataFromHttpClientResponse(res, User.class);

            if (user.hasRole(Roles.SUBSCRIBER)) {
                subscriptions = subscriptionRepository.getSubscription(subscriberId);
            }
            if (subscriptions.getSubscriber() == null) {
                response.put(Constants.CODE, "404");
                response.put(Constants.MESSAGE,
                        "Subscriptions for user with ID " + subscriberId.toString() + " is not found");
                response.put(Constants.DATA, subscriptions);
            } else {
                response.put(Constants.CODE, "201");
                response.put(Constants.MESSAGE, "Subscriptions have been retrieved");
                response.put(Constants.DATA, subscriptions);
            }
            return Mono.just(ResponseEntity.ok().header(HttpHeaders.CONTENT_TYPE, Constants.APPLICATION_JSON)
                    .header(Constants.ACCEPT, Constants.APPLICATION_JSON).body(response));
        });
    }

    public Mono<ResponseEntity<Map<String, Object>>> createSubscription(Subscription subscription, String token) {
        return umsConnector.retrieveUmsData(uriUser + "/" + subscription.getSubscriber().toString(), token).flatMap(res -> {
            Map<String, Object> response = new HashMap<>();

            boolean isCreated = false;
            User user = HttpResponseExtractor.extractDataFromHttpClientResponse(res, User.class);

            if (user.hasRole(Roles.SUBSCRIBER)) {
                isCreated = subscriptionRepository.createSubscription(subscription);
            }
            if (!isCreated) {
                response.put(Constants.CODE, "500");
                response.put(Constants.MESSAGE, "Subscriptions has not been created");
                response.put(Constants.DATA, false);
            } else {
                response.put(Constants.CODE, "200");
                response.put(Constants.MESSAGE, "Subscription has been created");
                response.put(Constants.DATA, true);
            }
            return Mono.just(ResponseEntity.ok().header(HttpHeaders.CONTENT_TYPE, Constants.APPLICATION_JSON)
                    .header(Constants.ACCEPT, Constants.APPLICATION_JSON).body(response));
        });
    }

    public Mono<ResponseEntity<Map<String, Object>>> updateSubscriptionForSubscriberById(Subscription subscription, String token) {
        return umsConnector.retrieveUmsData(uriUser + "/" + subscription.getSubscriber().toString(), token).flatMap(res -> {
            Map<String, Object> response = new HashMap<>();

            boolean isUpdated = false;
            User user = HttpResponseExtractor.extractDataFromHttpClientResponse(res, User.class);

            if (user.hasRole(Roles.SUBSCRIBER)) {
                isUpdated = subscriptionRepository.updateSubscription(subscription);
            }
            if (!isUpdated) {
                response.put(Constants.CODE, "500");
                response.put(Constants.MESSAGE, "Subscription has not been updated");
                response.put(Constants.DATA, false);
            } else {
                response.put(Constants.CODE, "201");
                response.put(Constants.MESSAGE, "Subscription has been updated");
                response.put(Constants.DATA, true);
            }
            return Mono.just(ResponseEntity.ok().header(HttpHeaders.CONTENT_TYPE, Constants.APPLICATION_JSON)
                    .header(Constants.ACCEPT, Constants.APPLICATION_JSON).body(response));
        });
    }

    public Mono<ResponseEntity<Map<String, Object>>> deleteSubscriptionsForSubscriberById(UUID subscriberId, String token) {
        return umsConnector.retrieveUmsData(uriUser + "/" + subscriberId.toString(), token).flatMap(res -> {
            Map<String, Object> response = new HashMap<>();

            boolean isDeleted = false;
            User user = HttpResponseExtractor.extractDataFromHttpClientResponse(res, User.class);

            if (user.hasRole(Roles.SUBSCRIBER)) {
                isDeleted = subscriptionRepository.deleteSubscriptions(subscriberId);
            }
            if (!isDeleted) {
                response.put(Constants.CODE, "500");
                response.put(Constants.MESSAGE, "Subscriptions has not been deleted");
                response.put(Constants.DATA, false);
            } else {
                response.put(Constants.CODE, "201");
                response.put(Constants.MESSAGE, "Subscriptions has been deleted");
                response.put(Constants.DATA, true);
            }
            return Mono.just(ResponseEntity.ok().header(HttpHeaders.CONTENT_TYPE, Constants.APPLICATION_JSON)
                    .header(Constants.ACCEPT, Constants.APPLICATION_JSON).body(response));
        });
    }
}
