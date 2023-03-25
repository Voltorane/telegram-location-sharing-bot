package utils;

public interface Constants {
    // reply messages
    String greetMessage = "Hello! I'm location bot!";

    // ability descriptions
    String START_DESCRIPTION = "Start using GeoPal to share location with your friends!";
    String SHARE_LOCATION_DESCRIPTION = "Share location with your friends!";
    String SEND_FRIEND_INVITE_DESCRIPTION = "Send friend invitation to user!" +
            " If he accepts it - you'll officially become friends";
    String ERROR_MESSAGE_ENDING = "If error persists, please contact administrator";

    String ADD_FRIEND_BUTTON = "Add Friend\uD83D\uDC64";
    String SHARE_LOCATION_BUTTON = "Share Location\uD83D\uDCCD";
    String ABORT_BUTTON = "❌";
    String ACCEPT_FRIEND_REQUEST_INLINE_BUTTON = "Accept✅";
    String DECLINE_FRIEND_REQUEST_INLINE_BUTTON = "Decline❌";
}
