package com.f1predict.auth.api;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Published contract of the auth domain for other domains in this app.
 *
 * The league domain needs usernames to render a member list. Before the services
 * were consolidated that would have meant another HTTP hop to auth-service (and
 * an endpoint that did not exist); it is now a direct call. Implemented by
 * AuthDirectoryService.
 */
public interface AuthDirectory {

    /**
     * Usernames for the given user ids. Ids with no matching user are omitted
     * rather than returned as null, so callers must tolerate a shorter list.
     */
    List<UserSummary> getUsers(Collection<UUID> userIds);
}
