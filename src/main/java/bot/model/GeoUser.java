package bot.model;

import org.telegram.telegrambots.meta.api.objects.User;

import java.io.Serializable;
import java.util.*;

public class GeoUser implements Serializable {
    private final long userId;
    private final long chatId;
    private final User user;

    public static record FriendRequest(GeoUser sender, GeoUser receiver, String text, Integer inlineMessageId)
            implements Serializable{
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            FriendRequest that = (FriendRequest) o;
            return sender.equals(that.sender) && receiver.equals(that.receiver) && text.equals(that.text);
        }

        @Override
        public int hashCode() {
            return Objects.hash(sender, receiver, text);
        }
    }

    private final HashMap<GeoUser, FriendRequest> incomingFriendRequests = new HashMap<>();
    private final HashMap<GeoUser, FriendRequest> outgoingFriendRequests = new HashMap<>();
    private final Set<GeoUser> friends = new HashSet<>();

    public GeoUser(User user, long chatId) {
        this.chatId = chatId;
        this.user = user;
        this.userId = user.getId();
    }

    public void sendFriendRequest(GeoUser receiver, FriendRequest request) {
        this.outgoingFriendRequests.put(receiver, request);
        receiver.incomingFriendRequests.put(this, request);
    }

    public void acceptFriendRequest(GeoUser friend) {
        if (incomingFriendRequests.containsKey(friend)) {
            incomingFriendRequests.remove(friend);
            friends.add(friend);
            friend.friendRequestAccepted(this);
        }
    }

    public void declineFriendRequest(GeoUser friend) {
        incomingFriendRequests.remove(friend);
        friend.friendRequestDeclined(this);
    }

    public void friendRequestAccepted(GeoUser friend) {
        if (outgoingFriendRequests.containsKey(friend)) {
            outgoingFriendRequests.remove(friend);
            friends.add(friend);
        }
    }

    public void friendRequestDeclined(GeoUser friend) {
        outgoingFriendRequests.remove(friend);
    }

    public void removeFriend(GeoUser friend) {
        friends.remove(friend);
    }

    public long getUserId() {
        return userId;
    }

    public long getChatId() {
        return chatId;
    }

    public User getUser() {
        return user;
    }

    public Map<GeoUser, FriendRequest> getIncomingFriendRequests() {
        return incomingFriendRequests;
    }

    public Map<GeoUser, FriendRequest> getOutgoingFriendRequests() {
        return outgoingFriendRequests;
    }

    public Set<GeoUser> getFriends() {
        return friends;
    }
}
