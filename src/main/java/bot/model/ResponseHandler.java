package bot.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.abilitybots.api.db.DBContext;
import org.telegram.abilitybots.api.sender.MessageSender;
import org.telegram.abilitybots.api.sender.SilentSender;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.MessageEntity;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import utils.Constants;
import utils.Constants.*;

import java.util.List;
import java.util.Map;

/**
 * Class responsible for all communication from bot to user
 */
public class ResponseHandler {
    private static final Logger logger = LoggerFactory.getLogger(ResponseHandler.class);
    private final MessageSender sender;
    private final SilentSender silent;
    private DBContext db;
    private static final int MAX_BUTTONS_PER_LIST = 5;

    public ResponseHandler(MessageSender sender,
                           SilentSender silent, DBContext db) {
        this.sender = sender;
        this.silent = silent;
        this.db = db;
    }

    /**
     * Sends greet message to user
     *
     * @param chatId chat of the user to greet
     * */
    public void replyToStart(long chatId) {
        silent.send(Constants.greetMessage, chatId);
    }

    /**
     * Deletes message from user's chat
     *
     * @param chatId chat to delete message from
     * @param messageId message to delete
     * */
    public void deleteMessage(Long chatId, int messageId) {
        try {
            sender.execute(DeleteMessage.builder()
                    .messageId(messageId)
                    .chatId(chatId)
                    .build());
        } catch (TelegramApiException e) {
            logger.error("Deleting message failed! {}", e.getMessage());
        }
    }

    /**
     * Sends notification message that previous action was aborted
     *
     * @param incomingAbortMessage message that raised an abort
     * */
    public void sendActionAbortedMessage(Message incomingAbortMessage) {
        try {
            deleteMessage(incomingAbortMessage.getChatId(), incomingAbortMessage.getMessageId());
            sender.execute(SendMessage.builder()
                    .chatId(incomingAbortMessage.getChatId())
                    .text("Action aborted!")
                    .replyMarkup(KeyboardFactory.removeKeyboard())
                    .build());
        } catch (TelegramApiException e) {
            logger.error("Action abortion notification sending failed!");
        }
    }

    /**
     * Sends error message to user
     *
     * @param chatId chat to send error message to
     * @param text text of the error message (additional error ending will be added)
     * */
    public void sendErrorMessage(long chatId, String text) {
        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text(text + Constants.ERROR_MESSAGE_ENDING)
                .replyMarkup(KeyboardFactory.removeKeyboard())
                .build();
        try {
            sender.execute(message);
        } catch (TelegramApiException e) {
            logger.error("Error sending failed by {}!", chatId);
        }
    }

    /**
     * Executes message editing, that removes inline keyboard from the message
     *
     * @param chatId    id of chat where keyboard will be removed
     * @param messageId id of message where keyboard will be removed
     */
    public void removeInlineKeyboard(long chatId, int messageId) {
        try {
            EditMessageReplyMarkup replyMarkup = KeyboardFactory.removeInlineKeyboard(chatId, messageId);
            sender.execute(replyMarkup);
        } catch (TelegramApiException e) {
            logger.error("Could not remove inline reply keyboard markup from user {}! {}", chatId, e.getMessage());
        }
    }

    /**
     * Sends a dummy message to the user with empty keyboard and instantly deletes it
     * */
    public void removeReplyKeyboardMarkup(long chatId) {
        SendMessage m = SendMessage.builder()
                .chatId(chatId)
                .text(".")
                .replyMarkup(KeyboardFactory.removeKeyboard())
                .build();
        Message message;
        try {
            message = sender.execute(m);
            DeleteMessage deleteMessage = DeleteMessage.builder().chatId(chatId).messageId(message.getMessageId()).build();
            sender.execute(deleteMessage);
        } catch (TelegramApiException e) {
            logger.error("Removing keyboard failed! {}", e.getMessage());
        }
    }

    /**
     * Sends add friend request message to user
     * See {@code KeyboardFactory.addFriendKeyboard()} for implementation of {@link ReplyKeyboardMarkup}
     * for this message
     *
     * @param chatId id of chat to send message to
     */
    public void askToShareFriendToAdd(long chatId) {
        ReplyKeyboardMarkup keyboardMarkup = KeyboardFactory.addFriendKeyboard();
        SendMessage message = SendMessage.builder()
                .text("Please share friend you want to add!")
                .replyMarkup(keyboardMarkup)
                .chatId(chatId)
                .build();
        try {
            // not removing previous reply markup, as we are sending a new one
            logger.info("Sending add friend request to chat {}!", chatId);
            sender.execute(message);
        } catch (TelegramApiException e) {
            logger.error("Sending Add friend Request to chat {} failed! {}", chatId, e.getMessage());
            sendErrorMessage(chatId, "Add friend request failed! Please try again!");
        }
    }

    /**
     * Sends a message that asks the user if he wants to give any additional comments to the friend request
     *
     * @param chatId chat the message will be sent to
     * @return id of the message that is asking to provide further comment for the friend request
     * */
    public Integer askForCommentForFriendRequest(long chatId) {
        InlineKeyboardMarkup keyboardMarkup =
                KeyboardFactory.friendRequestCommentInlineKeyboard(FriendRequestConstants.CONFIRM_CALLBACK_QUERY,
                FriendRequestConstants.ABORT_CALLBACK_QUERY);
        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text("If you would like to add any comment, that will be attached to the friend request, you can do it now :)" +
                        " Just send it to me and I will address it to the receiver!")
                .replyMarkup(keyboardMarkup)
                .build();

        // message with a choice to provide the comment for the friend request
        try {
            // remove previous reply keyboard, as not sending a new one (only inline)
            removeReplyKeyboardMarkup(chatId);
            return sender.execute(message).getMessageId();
        } catch (TelegramApiException e) {
            logger.error("Asking for comments failed for {}", chatId);
            return null;
        }
    }

    /**
     * Returns String that should be sent to the receiver of the friend request
     *
     * @param userName user name of the sender
     * @param comment text that will be attached with the friend request
     * @return text that should be sent to the receiver of the friend request
     * */
    private String getFriendRequestMessage(String userName, String comment) {
        return String.format("You got new friend request from @%s\n\n%s", userName, comment);
    }

    public void sendFriendRequestPreview(GeoUser requestSender, GeoUser receiver, String comment) {
        String text = String.format("""
                        Please confirm the sending of friend request!
                        @%s will receive a following message from you:

                        %s""", receiver.getUser().getUserName(), comment);
        InlineKeyboardMarkup keyboardMarkup =
                KeyboardFactory.friendRequestConfirmInlineKeyboard(FriendRequestConstants.CONFIRM_CALLBACK_QUERY,
                        FriendRequestConstants.ABORT_CALLBACK_QUERY);
        SendMessage message = SendMessage.builder()
                .chatId(requestSender.getChatId())
                .replyMarkup(keyboardMarkup)
                .text(text)
                .build();
        try {
            // remove previous reply keyboard
            removeReplyKeyboardMarkup(requestSender.getChatId());
            sender.execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public void abortedSendingFriendRequest(GeoUser requestSender) {
        SendMessage message = SendMessage.builder()
                .chatId(requestSender.getChatId())
                .text("Friend request is aborted!")
                .build();
        try {
            sender.execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sends message to {@code requestSender} and {@code receiver} informing about the friend request
     *
     * @param requestSender sender of the request
     * @param receiver      receiver of the request
     * @param requestText   text that will be visible to the receiver
     */
    public void sendFriendRequest(GeoUser requestSender, GeoUser receiver, String requestText) {
        String acceptFriendRequestCallback =
                CallbackQueryDataFactory.FriendRequestAnswer.acceptRequest(requestSender.getUserId(), receiver.getUserId());
        String declineFriendRequestCallback =
                CallbackQueryDataFactory.FriendRequestAnswer.declineRequest(requestSender.getUserId(), receiver.getUserId());
        InlineKeyboardMarkup keyboardMarkup = KeyboardFactory.friendRequestInlineKeyboard(acceptFriendRequestCallback,
                declineFriendRequestCallback);

        // add mention of the sender in the request message
        MessageEntity entity = MessageEntity
                .builder()
                .type("mention")
                .offset(32)
                .length(requestSender.getUser().getUserName().length())
                .user(requestSender.getUser())
                .build();

        SendMessage friendRequestMessage = SendMessage.builder()
                .entities(List.of(entity))
                .chatId(receiver.getChatId())
                .text(getFriendRequestMessage(requestSender.getUser().getUserName(), requestText))
                .replyMarkup(keyboardMarkup)
                .build();
        try {
            Message m = sender.execute(friendRequestMessage);
            requestSender.sendFriendRequest(receiver,
                    new GeoUser.FriendRequest(requestSender, receiver, requestText, m.getMessageId()));
            SendMessage messageToSender = SendMessage.builder()
                    .text("You have sent request to: @" + receiver.getUser().getUserName())
                    .chatId(requestSender.getChatId())
                    .build();
            sender.execute(messageToSender);
        } catch (TelegramApiException e) {
            sendErrorMessage(requestSender.getChatId(), e.getMessage());
        }
    }

    /**
     * Sends messages informing that {@code receiver} accepted friend request from {@code sender}
     *
     * @param sender   sender of the initial friend request
     * @param receiver receiver of the initial friend request
     */
    public void sendFriendRequestAccepted(GeoUser sender, GeoUser receiver) {
        silent.send(String.format("@%s has accepted your friend request!",
                receiver.getUser().getUserName()), sender.getChatId());
        silent.send(String.format("You have accepted @%s friend request!",
                sender.getUser().getUserName()), receiver.getChatId());
    }

    /**
     * Sends messages informing that {@code receiver} declined friend request from {@code sender}
     *
     * @param sender   sender of the initial friend request
     * @param receiver receiver of the initial friend request
     */
    public void sendFriendRequestDeclined(GeoUser sender, GeoUser receiver) {
        silent.send(String.format("@%s has declined your friend request!",
                receiver.getUser().getUserName()), sender.getChatId());
        silent.send(String.format("You have declined @%s friend request!",
                sender.getUser().getUserName()), receiver.getChatId());
    }

    /**
     * Sends message with location request
     * See {@code KeyboardFactory.shareLocationKeyboard()} for implementation of {@link ReplyKeyboardMarkup}
     *
     * @param chatId id of chat to send message to
     */
    public void askForLocation(long chatId) {
        ReplyKeyboardMarkup keyboardMarkup = KeyboardFactory.shareLocationKeyboard();
        SendMessage message = SendMessage
                .builder()
                .chatId(chatId)
                .text("Please share your location!")
                .replyMarkup(keyboardMarkup)
                .build();
        try {
            logger.info("Sending location request to chat {}!", chatId);
            sender.execute(message);
        } catch (TelegramApiException e) {
            logger.error("Sending location request to chat {} failed! {}", chatId, e.getMessage());
            sendErrorMessage(chatId, "Send location request failed! Please try again!");
        }
    }

    /**
     * Sends result notification to the location sharer {@code user}
     * */
    public void sendLocationSharingResult(GeoUser user, boolean success) {
        SendMessage message = SendMessage.builder()
                .chatId(user.getChatId())
                //text will be changed
                .text(" ")
                .replyMarkup(KeyboardFactory.removeKeyboard())
                .build();
        if (success) {
            message.setText("Successfully shared location with your geo pals!");
        } else {
            message.setText("Location sharing failed! Please try again later!");
        }
        try {
            sender.execute(message);
        } catch (TelegramApiException e) {
            logger.error("Sending location sharing result failed!");
        }
    }

    /**
     * Sends {@code locationText} to all chats in {@code chatIds} from {@code user}
     *
     * @param user sender of the location (usually will be in the text message sent to friends
     * @param chatIds list of ids of chats the message will be sent to
     * @param locationText text that contains the location of the {@code user}
     * @throws TelegramApiException if message execution failed
     * @throws IllegalArgumentException if message did not contain text
     * */
    public void sendLocationToFriends(GeoUser user, List<Long> chatIds, String locationText) throws TelegramApiException {
        SendMessage locationMessage = SendMessage
                .builder()
                // chat id must be changed in the sendMessageToChats
                .chatId(0L)
                .text(locationText)
                .build();
        sendMessageToChats(user, chatIds, locationMessage);
    }

    /**
     * Sends {@code text} to all the users friends
     *
     * @param user user to get friends from
     * @param chatIds list of ids of chats the message will be sent to
     * @param message message that will be sent to all {@code chatIds}. Should already contain text.
     * @throws TelegramApiException if message execution failed
     * @throws IllegalArgumentException if message did not contain text
     */
    public void sendMessageToChats(GeoUser user, List<Long> chatIds, SendMessage message)
            throws TelegramApiException, IllegalArgumentException {
        if (message.getText().isEmpty()) {
            throw new IllegalArgumentException("Message text cannot be empty!");
        }
        for (long chatId : chatIds) {
            try {
                message.setChatId(chatId);
                sender.execute(message);
            } catch (TelegramApiException e) {
                logger.error("Text sending failed by user: {}", user.getChatId());
                throw new TelegramApiException("Text sending failed by: " + user.getChatId());
            }
        }
    }

    /**
     * Sends a text representation of the friendList to user
     *
     * @param chatId user's chat that will receive the message
     * @param friendList text representation of his friend
     * */
    public void sendFriendList(Long chatId, String friendList) {
        if (friendList.isEmpty()) {
            sendHasNoFriends(chatId);
        } else {
            silent.send("Here are your friends:\n" + friendList, chatId);
        }
    }

//    public void abortFriendRemoving(long chatId) {
//        silent.send("Friend removing aborted!", chatId);
//    }

    /**
     * If {@code isFirstMessage} is {@code true} - sends a message to the user with an inline keyboard with friend
     * that can be removed
     * Otherwise - method is called for an already existing message with {@code messageId} and the keyboard is edited
     * in that particular message.
     *
     * @param chatId id of user's chat
     * @param buttons list of buttons in format [buttonText, buttonCallback]
     * @param startIndex index from which buttons will be sent inclusive
     * @param isFirstMessage if true - sends a message to the user with an inline keyboard with friend
     * that can be removed. Otherwise method is called for an already existing message with {@code messageId} and the keyboard is edited
     * in that particular message
     * */
    public void sendFriendListToRemove(long chatId, Integer messageId, List<Map.Entry<String, String>> buttons,
                                       int startIndex, boolean isFirstMessage) {
        if (buttons.size() <= startIndex) {
            logger.error("Button list is smaller than start index");
            return;
        } else if (startIndex < 0) {
            logger.error("Start index cannot be negative!");
            return;
        }
        int lastIndex = startIndex + MAX_BUTTONS_PER_LIST - 1;
        // list that will be sent to user
        List<Map.Entry<String, String>> resultList = buttons.subList(startIndex, Math.min(buttons.size(), lastIndex + 1));
        // if last button index is less that the generated one -> need no next button (this is the last page)
        String nextBtnCallback = buttons.size() - 1 <= lastIndex
                ? null
                : CallbackQueryDataFactory.RemoveFriend.getNewIndexCallback(Math.min(buttons.size() - 1, lastIndex + 1));
        String prevBtnCallback = startIndex + resultList.size() <= MAX_BUTTONS_PER_LIST
                ? null
                : CallbackQueryDataFactory.RemoveFriend.getNewIndexCallback(Math.max(0, startIndex - MAX_BUTTONS_PER_LIST));
        InlineKeyboardMarkup keyboardMarkup =
                KeyboardFactory.removeFriendInlineKeyboard(resultList, nextBtnCallback, prevBtnCallback);
        if (isFirstMessage) {
            // need to send a message and attach the list to it
            SendMessage message = SendMessage.builder()
                    .chatId(chatId)
                    .text("Please select friend you want to remove:")
                    .replyMarkup(keyboardMarkup)
                    .build();
            try {
                sender.execute(message);
            } catch (TelegramApiException e) {
                logger.error("Sending remove friend list failed: {}", e.getMessage());
            }
        } else {
            assert messageId != null;
            // only editing the message if it's already there
            EditMessageReplyMarkup editMessageReplyMarkup = EditMessageReplyMarkup
                    .builder()
                    .chatId(chatId)
                    .messageId(messageId)
                    .replyMarkup(keyboardMarkup)
                    .build();
            try {
                sender.execute(editMessageReplyMarkup);
            } catch (TelegramApiException e) {
                logger.error("Sending remove friend list failed: {}", e.getMessage());
            }
        }
    }

    /**
     Sends a confirmation message to a Telegram chat asking if the user wants to remove a friend.

     @param chatId the ID of the Telegram chat where the message should be sent.
     @param userName the username of the friend to be removed.
     @param friendId the ID of the friend to be removed.
     */
    public void askToConfirmFriendRemove(long chatId, String userName , String friendId) {
        InlineKeyboardMarkup keyboardMarkup =
                KeyboardFactory.removeFriendConfirmInlineKeyboard(CallbackQueryDataFactory.RemoveFriend.getConfirmCallback(friendId));
        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text("Are you sure you want to remove friend:\n@" + userName)
                .replyMarkup(keyboardMarkup)
                .build();

        try {
            sender.execute(message);
        } catch (TelegramApiException e) {
            logger.error("Friend remove confirmation failed");
        }
    }

    /**
     * Sends a message to a Telegram chat confirming that a friend has been successfully deleted.
     *
     * @param chatId    the ID of the Telegram chat where the message should be sent.
     * @param userName  the username of the friend who was deleted.
     */
    public void sendSuccessfullyDeleted(long chatId, String userName) {
        silent.send("Successfully removed @" + userName + " from friends!", chatId);
    }

    /**
     * Sends a message to a Telegram chat informing a user that they have been removed from a friend's list.
     *
     * @param chatId    the ID of the Telegram chat where the message should be sent.
     * @param userName  the username of the friend who removed the user.
     */
    public void sendDeletedFromFriends(long chatId, String userName) {
        silent.send("@" + userName + " has removed you from friends. You are no longer sharing location with them! " +
                "If you want to add them back to friends - send new /add_friend command!", chatId);
    }

    /**
     * Sends a message to a Telegram chat informing the user that they don't have any friends yet.
     *
     * @param chatId    the ID of the Telegram chat where the message should be sent.
     */
    public void sendHasNoFriends(long chatId) {
        silent.send("You don't have any friends yet :( You can add them via /add_friend command", chatId);
    }
}
