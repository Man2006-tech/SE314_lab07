
/* Copyright (c) 2007-2016 MIT 6.005 course staff, all rights reserved.
 * Redistribution of original or derived work requires permission of course staff.
 */
package twitter;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Comparator;

/**
 * SocialNetwork provides methods that operate on a social network.
 *
 * A social network is represented by a Map<String, Set<String>> where
 * map[A] is the set of people that person A follows on Twitter.
 */
public class SocialNetwork {

    /**
     * Guess who might follow whom, from evidence found in tweets.
     *
     * A user is considered to follow another user if they mention that
     * user in a tweet. Usernames are normalized to lowercase because
     * Twitter usernames are not case-sensitive.
     *
     * @param tweets a list of tweets providing evidence; not modified
     * @return a social network inferred from the given tweets
     */
    public static Map<String, Set<String>> guessFollowsGraph(List<Tweet> tweets) {
        // Each author maps to the set of users they appear to follow.
        Map<String, Set<String>> followsGraph = new HashMap<>();

        for (Tweet tweet : tweets) {
            // Normalize the author's username to handle case differences.
            String author = tweet.getAuthor().toLowerCase(Locale.ROOT);

            // Reuse Extract to identify mentions in this tweet.
            Set<String> mentionedUsers = Extract.getMentionedUsers(
                    Collections.singletonList(tweet));

            for (String mentionedUser : mentionedUsers) {
                String mentioned = mentionedUser.toLowerCase(Locale.ROOT);

                // The social network specification forbids self-following.
                if (!author.equals(mentioned)) {
                    // Create the author's set only when a valid follow is found.
                    // A set also prevents duplicate follow relationships.
                    followsGraph
                            .computeIfAbsent(author, key -> new HashSet<>())
                            .add(mentioned);
                }
            }
        }

        return followsGraph;
    }

    /**
     * Find users with the greatest influence, measured by follower count.
     *
     * @param followsGraph a social network
     * @return all distinct usernames in descending order of follower count
     */
    public static List<String> influencers(Map<String, Set<String>> followsGraph) {
        // Store the number of followers for each distinct username.
        Map<String, Integer> followerCounts = new HashMap<>();

        // Include every user who appears as a key or as a followed user.
        for (Map.Entry<String, Set<String>> entry : followsGraph.entrySet()) {
            String user = entry.getKey().toLowerCase(Locale.ROOT);
            followerCounts.putIfAbsent(user, 0);

            // Each user in this set follows the relationship from the key user.
            // Therefore, each mentioned username gains one follower.
            for (String followedUser : entry.getValue()) {
                String followed = followedUser.toLowerCase(Locale.ROOT);

                followerCounts.putIfAbsent(followed, 0);
                followerCounts.put(followed, followerCounts.get(followed) + 1);
            }
        }

        // Start with all distinct usernames in the network.
        List<String> result = new ArrayList<>(followerCounts.keySet());

        // Sort from highest follower count to lowest.
        result.sort(Comparator.comparingInt(
                (String user) -> followerCounts.get(user)).reversed());

        return result;

}
}
