package bot.model;

import java.util.Objects;

/**
 * Class is a wrapper for an ongoing friend request that is defined by the starting of
 * the add_friend command sequence. Request is considered ongoing, between the {@code add_friend}
 * command start and an abort/completion of the command flow
 */
public class OngoingFriendRequest {
    private final GeoUser sender;
    private final GeoUser receiver;

    // message by the sender of request with (SEND WITHOUT COMMENTS/ABORT) options
    private int senderInlineMessageId;
    private String comment = "";

    public OngoingFriendRequest(GeoUser sender, GeoUser receiver) {
        this.sender = sender;
        this.receiver = receiver;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OngoingFriendRequest that = (OngoingFriendRequest) o;
        return sender.equals(that.sender) && receiver.equals(that.receiver);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sender, receiver);
    }

    public GeoUser getSender() {
        return sender;
    }

    public GeoUser getReceiver() {
        return receiver;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public int getSenderInlineMessageId() {
        return senderInlineMessageId;
    }

    public void setSenderInlineMessageId(int senderInlineMessageId) {
        this.senderInlineMessageId = senderInlineMessageId;
    }
}
