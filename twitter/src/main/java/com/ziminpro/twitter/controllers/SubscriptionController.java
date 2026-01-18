package com.ziminpro.twitter.controllers;

import java.util.Map;
import java.util.UUID;

import com.ziminpro.twitter.dtos.Constants;
import com.ziminpro.twitter.dtos.Subscription;
import com.ziminpro.twitter.services.SubscriptionsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import reactor.core.publisher.Mono;

@RestController
public class SubscriptionController {

    @Autowired
    private SubscriptionsService subscriptionsService;

    @RequestMapping(method = RequestMethod.GET, path = Constants.URI_SUBSCRIPTION + "/{subscriber-id}")
    public Mono<ResponseEntity<Map<String, Object>>> getSubscriptionBySubscriberId(
            @PathVariable(value = "subscriber-id", required = true) UUID subscriberId,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String token) {
        return subscriptionsService.getSubscriptionsForSubscriberById(subscriberId, token);
    }

    @RequestMapping(method = RequestMethod.PUT, path = Constants.URI_SUBSCRIPTIONS, consumes = Constants.APPLICATION_JSON)
    public Mono<ResponseEntity<Map<String, Object>>> updateSubscription(
            @RequestBody Subscription subscription,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String token) {
        return subscriptionsService.updateSubscriptionForSubscriberById(subscription, token);
    }

    @RequestMapping(method = RequestMethod.POST, path = Constants.URI_SUBSCRIPTIONS, consumes = Constants.APPLICATION_JSON)
    public Mono<ResponseEntity<Map<String, Object>>> createSubscription(
            @RequestBody Subscription subscription,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String token) {
        return subscriptionsService.createSubscription(subscription, token);
    }

    @RequestMapping(method = RequestMethod.DELETE, path = Constants.URI_SUBSCRIPTION + "/{subscriber-id}")
    public Mono<ResponseEntity<Map<String, Object>>> deleteSubscriptions(
            @PathVariable(value = "subscriber-id", required = true) UUID subscriberId,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String token) {
        return subscriptionsService.deleteSubscriptionsForSubscriberById(subscriberId, token);
    }
}
