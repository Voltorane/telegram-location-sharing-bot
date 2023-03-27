package utils;

public interface Constants {
    // reply messages
    String greetMessage = """
                             Welcome to GeoPal, your personal location-sharing assistant on Telegram! With GeoPal,
                             you can easily share your current location with your friends and family on Telegram,
                             and keep tabs on their whereabouts too. Here's what you can do with GeoPal:
                             
                             - Register with us using the /start command, and let's get started on sharing your location!
                             - Add friends with the /add_friend command and share your location with them whenever you want.
                             - Keep track of all your friends using the /friend_list command.
                             - Need some space? No problem! Use the /remove_friend command to stop sharing your location with someone.
                             - Share your current location with your friends using the /share_location command,
                              and let them know where you're at in just one click.
                              GeoPal makes it easy to stay connected with your loved ones, no matter where you are.
                              Try it out today and see how much easier it can make your life!
                              """;

    String START_DESCRIPTION = "Start using GeoPal to share location with your friends!";
    String SHARE_LOCATION_DESCRIPTION = "Share location with your friends!";
    String ERROR_MESSAGE_ENDING = "If error persists, please contact administrator";

    interface FriendRequestConstants {
        String ADD_FRIEND = "Add Friend\uD83D\uDC64";
        String SEND_WITHOUT_COMMENT = "Send without comments✅";
        String ABORT_SENDING = "Abort friend request❌";
        // needed when user provided comment for a friend request
        String SEND = "Send✅";
        String ACCEPT = "Accept✅";
        String DECLINE = "Decline❌";

        // simple callbacks, don't need factory class (although can be done in the next releases :))
        String CONFIRM_CALLBACK_QUERY = "confirm";
        String ABORT_CALLBACK_QUERY = "abort";
    }

    interface RemoveFriendConstants {
        String SEND = "Accept✅";
        String ABORT = "Abort❌";
        String PREVIOUS = "« Previous";
        String NEXT = "Next »";
    }

    String SHARE_LOCATION_BUTTON = "Share Location\uD83D\uDCCD";
    String ABORT_BUTTON = "❌";
}
