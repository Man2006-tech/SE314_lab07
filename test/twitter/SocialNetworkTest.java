/* Copyright (c) 2007-2016 MIT 6.005 course staff, all rights reserved.
 * Redistribution of original or derived work requires permission of course staff.
 */
package twitter;

import static org.junit.Assert.*;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

/**
 * Tests for the SocialNetwork class.
 */
public class SocialNetworkTest {

    /*
     * Testing strategy for guessFollowsGraph():
     *
     * - Number of tweets: empty, one, multiple.
     * - Mentions: none, one, multiple.
     * - Authors: one author across multiple tweets.
     * - Repeated mentions: same user mentioned more than once.
     * - Username case: uppercase and lowercase usernames.
     *
     * The postcondition is underdetermined because additional evidence
     * may be used and authors with no follows may be omitted as keys.
     * Therefore, tests check required behavior instead of exact graph
     * contents when the specification does not require them.
     *
     * Testing strategy for influencers():
     *
     * - Graph size: empty, one user, multiple users.
     * - Follower counts: zero, one, multiple.
     * - Influence: clear leader, different counts, tied counts.
     * - Users appearing as keys and as values.
     * - Tied users may appear in either order.
     */

    private static final Instant TIME =
            Instant.parse("2026-01-01T12:00:00Z");

    // Creates a tweet for use in tests.
    private Tweet tweet(long id, String author, String text) {
        return new Tweet(id, author, text, TIME);
    }

    // Creates a set of usernames.
    private Set<String> users(String... names) {
        return new HashSet<>(Arrays.asList(names));
    }

    // Checks whether a set contains a username, ignoring case.
    private boolean containsIgnoreCase(Set<String> names, String target) {
        for (String name : names) {
            if (name.equalsIgnoreCase(target)) {
                return true;
            }
        }
        return false;
    }

    // Checks whether a graph contains a key, ignoring case.
    private boolean containsKeyIgnoreCase(
            Map<String, Set<String>> graph, String target) {
        for (String name : graph.keySet()) {
            if (name.equalsIgnoreCase(target)) {
                return true;
            }
        }
        return false;
    }

    // Gets the follow set for a username, ignoring case.
    private Set<String> getFollowsIgnoreCase(
            Map<String, Set<String>> graph, String target) {
        for (Map.Entry<String, Set<String>> entry : graph.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(target)) {
                return entry.getValue();
            }
        }
        return Collections.emptySet();
    }

    // Creates a graph from username and follow-set pairs.
    private Map<String, Set<String>> graph(Object... entries) {
        Map<String, Set<String>> result = new HashMap<>();

        for (int i = 0; i < entries.length; i += 2) {
            String user = (String) entries[i];

            @SuppressWarnings("unchecked")
            Set<String> follows = (Set<String>) entries[i + 1];

            result.put(user, follows);
        }

        return result;
    }

    // Test 1: An empty tweet list produces an empty graph.
    @Test
    public void testGuessFollowsGraphEmptyTweets() {
        Map<String, Set<String>> result =
                SocialNetwork.guessFollowsGraph(
                        Collections.<Tweet>emptyList());

        assertTrue(result.isEmpty());
    }

    // Test 2: Tweets without mentions create no follow relationships.
    @Test
    public void testGuessFollowsGraphTweetsWithoutMentions() {
        List<Tweet> tweets = Arrays.asList(
                tweet(1, "alice", "Hello everyone"),
                tweet(2, "bob", "Nice day"));

        Map<String, Set<String>> result =
                SocialNetwork.guessFollowsGraph(tweets);

        // Empty author entries are allowed, so check the edges instead.
        for (Set<String> follows : result.values()) {
            assertTrue(follows.isEmpty());
        }
    }

    // Test 3: A user follows the person they mention.
    @Test
    public void testGuessFollowsGraphSingleMention() {
        List<Tweet> tweets =
                Arrays.asList(tweet(1, "ernie", "Hello @Bert"));

        Map<String, Set<String>> result =
                SocialNetwork.guessFollowsGraph(tweets);

        assertTrue(containsKeyIgnoreCase(result, "ernie"));
        assertTrue(containsIgnoreCase(
                getFollowsIgnoreCase(result, "ernie"), "bert"));
    }

    // Test 4: Multiple mentions create multiple follow relationships.
    @Test
    public void testGuessFollowsGraphMultipleMentions() {
        List<Tweet> tweets = Arrays.asList(
                tweet(1, "ernie", "Hello @Bert and @CookieMonster"));

        Map<String, Set<String>> result =
                SocialNetwork.guessFollowsGraph(tweets);

        Set<String> follows = getFollowsIgnoreCase(result, "ernie");

        assertTrue(containsIgnoreCase(follows, "bert"));
        assertTrue(containsIgnoreCase(follows, "cookiemonster"));
    }

    // Test 5: Mentions across multiple tweets from one author are combined.
    @Test
    public void testGuessFollowsGraphMultipleTweetsFromOneUser() {
        List<Tweet> tweets = Arrays.asList(
                tweet(1, "ernie", "Hello @Bert"),
                tweet(2, "ERNIE", "Hello again @Elmo"),
                tweet(3, "ernie", "Another message to @Bert"));

        Map<String, Set<String>> result =
                SocialNetwork.guessFollowsGraph(tweets);

        Set<String> follows = getFollowsIgnoreCase(result, "ernie");

        assertTrue(containsIgnoreCase(follows, "bert"));
        assertTrue(containsIgnoreCase(follows, "elmo"));

        // Bert should occur only once, even though he was mentioned twice.
        assertEquals(2, follows.size());
    }

    // Test 6: An empty graph produces an empty influencer list.
    @Test
    public void testInfluencersEmptyGraph() {
        List<String> result = SocialNetwork.influencers(
                Collections.<String, Set<String>>emptyMap());

        assertTrue(result.isEmpty());
    }

    // Test 7: A user with no followers has a follower count of zero.
    @Test
    public void testInfluencersSingleUserWithoutFollowers() {
        Map<String, Set<String>> followsGraph = new HashMap<>();
        followsGraph.put("alice", new HashSet<String>());

        List<String> result = SocialNetwork.influencers(followsGraph);

        // The specification requires all distinct usernames in the graph.
        assertEquals(Collections.singletonList("alice"), result);
    }

    // Test 8: The user followed by the most people ranks first.
    @Test
    public void testInfluencersSingleInfluencer() {
        Map<String, Set<String>> followsGraph = graph(
                "alice", users("bob"),
                "david", users("bob"));

        List<String> result = SocialNetwork.influencers(followsGraph);

        assertEquals("bob", result.get(0));
    }

    // Test 9: Users are ordered by descending follower count.
    @Test
    public void testInfluencersMultipleInfluencers() {
        Map<String, Set<String>> followsGraph = graph(
                "alice", users("bob", "charlie"),
                "david", users("bob"),
                "erin", users("bob", "charlie"));

        List<String> result = SocialNetwork.influencers(followsGraph);

        assertEquals("bob", result.get(0));
        assertEquals("charlie", result.get(1));
        assertEquals(5, result.size());
    }

    // Test 10: Users with tied follower counts may appear in either order.
    @Test
    public void testInfluencersTiedInfluence() {
        Map<String, Set<String>> followsGraph = graph(
                "alice", users("bob"),
                "charlie", users("david"));

        List<String> result = SocialNetwork.influencers(followsGraph);

        assertEquals(4, result.size());
        assertTrue(result.containsAll(Arrays.asList(
                "alice", "bob", "charlie", "david")));

        // Bob and David each have one follower.
        // No particular ordering is required for tied counts.
        assertEquals(1, Collections.frequency(result, "bob"));
        assertEquals(1, Collections.frequency(result, "david"));
    }
}
