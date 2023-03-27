package utils;

public interface Constants {
    // reply messages
    String greetMessage = "Hello! I'm location bot!";

    // ability descriptions
    String START_DESCRIPTION = "Start using GeoPal to share location with your friends!";
    String SHARE_LOCATION_DESCRIPTION = "Share location with your friends!";
    String ERROR_MESSAGE_ENDING = "If error persists, please contact administrator";

    interface FriendRequestConstants {
        String SEND_FRIEND_INVITE_DESCRIPTION = "Send friend invitation to user!" +
                " If he accepts it - you'll officially become friends";
        String ADD_FRIEND = "Add Friend\uD83D\uDC64";
        String SEND_WITHOUT_COMMENT = "Send without comments✅";
        String ABORT_SENDING = "Abort friend request❌";
        // needed when user provided comment for a friend request
        String SEND = "Send✅";
        String ACCEPT = "Accept✅";
        String DECLINE = "Decline❌";

        String CONFIRM_CALLBACK_QUERY = "confirm";
        String ABORT_CALLBACK_QUERY = "abort";
    }

    String SHARE_LOCATION_BUTTON = "Share Location\uD83D\uDCCD";
    String ABORT_BUTTON = "❌";
}
