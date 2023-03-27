package bot.model;

/**
 * Factory class to get the complex callback queries
 * */
public class CallbackQueryDataFactory {

    /**
     * Assembles a callback data string based on the given instruction name and arguments.
     *
     * @param instructionName the name of the instruction to include in the callback data string.
     * @param arguments       the arguments to include in the callback data string, separated by colons.
     * @return the assembled callback data string.
     * Example callback data string: "remove_friend:confirm:123"
     */
    public static String assembleCallbackData(String instructionName, String... arguments) {
        StringBuilder sb = new StringBuilder(instructionName);
        for (String argument : arguments) {
            sb.append(":").append(argument);
        }
        return sb.toString();
    }

    /**
     * Contains utility methods related to callback data from answering on a friend request.
     */
    public static class FriendRequestAnswer {
        public static final String CALLBACK_REGEX = "(accept|decline)_friend_request:\\d*:\\d*";

        /**
         * Returns callback data for accepting a friend request.
         *
         * @param from the ID of the user who sent the friend request
         * @param to the ID of the user who received the friend request
         * @return a String containing the generated callback data
         * Example: "accept_friend_request:1337:1234"
         */
        public static String acceptRequest(long from, long to) {
            return assembleCallbackData("accept_friend_request", String.valueOf(from), String.valueOf(to));
        }

        /**
         * Returns callback data for declining a friend request.
         *
         * @param from the ID of the user who sent the friend request
         * @param to the ID of the user who received the friend request
         * @return a String containing the generated callback data
         * Example: "decline_friend_request:1:2"
         */
        public static String declineRequest(long from, long to) {
            return assembleCallbackData("decline_friend_request", String.valueOf(from), String.valueOf(to));
        }

        /**
         * Tests whether the provided callback data matches the answer on friend request format.
         *
         * @param data the callback data to test
         * @return true if the provided data matches the format, false otherwise
         */
        public static boolean test(String data) {
            return data.matches(CALLBACK_REGEX);
        }
    }

    /**
     * Contains utility methods related to callback data from removing a friend.
     */
    public static class RemoveFriend {
        public static final String INSTRUCTION_NAME = "remove_friend";
        public static final String CALLBACK_REGEX = "remove_friend:((\\d*|confirm:\\d*)|(abort)|(index:\\d*))";

        /**
         * Returns a callback data string for confirming the removal of a friend.
         *
         * @param userId the ID of the friend to remove.
         * @return the callback data string for confirming the removal of the friend.
         * Example callback data string: "remove_friend:confirm:123"
         */
        public static String getConfirmCallback(String userId) {
            return assembleCallbackData(INSTRUCTION_NAME, "confirm", userId);
        }

        /**
         * Returns a callback data string for aborting the removal of a friend.
         *
         * @return the callback data string for aborting the removal of a friend.
         * Example callback data string: "remove_friend:abort"
         */
        public static String getAbortCallback() {
            return assembleCallbackData(INSTRUCTION_NAME, "abort");
        }

        /**
         * Returns a callback data string for setting a new start index for pagination of the list of friends.
         *
         * @param newStartIndex the new index from which the list of friends will start next time.
         * @return the callback data string for setting a new start index.
         * Example callback data string: "remove_friend:index:20"
         */
        public static String getNewIndexCallback(int newStartIndex) {
            return assembleCallbackData(INSTRUCTION_NAME, "index" , String.valueOf(newStartIndex));
        }

        /**
         * Returns a callback data string for selecting a friend to remove.
         *
         * @param userId the ID of the friend to select for removal.
         * @return the callback data string for selecting a friend to remove.
         * Example callback data string: "remove_friend:123"
         */
        public static String getSelectUserCallback(long userId) {
            return assembleCallbackData(INSTRUCTION_NAME, String.valueOf(userId));
        }

        /**
         * Tests whether the given callback data string matches the regex pattern for remove friend requests.
         *
         * @param data the callback data string to test.
         * @return true if the given callback data string matches the regex pattern for removing a friend, false otherwise.
         */
        public static boolean test(String data) {
            return data.matches(CALLBACK_REGEX);
        }
    }
}
