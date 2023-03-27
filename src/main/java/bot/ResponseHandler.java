package bot;

import bot.storage.GeoUser;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Class responsible for all communication from bot to user
 */
public class ResponseHandler {
    private static final Logger logger = LoggerFactory.getLogger(ResponseHandler.class);
    private final MessageSender sender;
    private final SilentSender silent;
    private DBContext db;

    public ResponseHandler(MessageSender sender,
                           SilentSender silent, DBContext db) {
        this.sender = sender;
        this.silent = silent;
        this.db = db;
    }

    public void replyToStart(long chatId) {
        silent.send(Constants.greetMessage, chatId);
    }

    // TODO REMOVE BEFORE SENDING DO PRODUCTION
    public void send(SendMessage message) {
        try {
            sender.execute(message);
        } catch (TelegramApiException e) {
            logger.error("Message sending failed: " + e.getMessage());
        }
    }

    public void deleteMessage(Message message) {
        try {
            sender.execute(DeleteMessage.builder()
                    .messageId(message.getMessageId())
                    .chatId(message.getChatId())
                    .build());
        } catch (TelegramApiException e) {
            logger.error("Deleting message failed! {}", e.getMessage());
        }
    }

    public void sendActionAbortedMessage(Message incomingAbortMessage) {
        try {
            deleteMessage(incomingAbortMessage);
            sender.execute(SendMessage.builder()
                    .chatId(incomingAbortMessage.getChatId())
                    .text("Action aborted!")
                    .replyMarkup(KeyboardFactory.removeKeyboard())
                    .build());
//            silent.send("Action aborted!", incomingAbortMessage.getChatId());
        } catch (TelegramApiException e) {
            logger.error("Action abortion notification sending failed!");
        }
    }

    public void sendErrorMessage(String text, long chatId) {
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
        Message message = null;
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
            // TODO MAKE SEND ERROR TO USER AND ABORT THE REQUEST
            sendErrorMessage("Add friend request failed! Please try again!", chatId);
        }
    }

    /**
     * @return id of the message that is asking to provide further comment for the friend request
     * */
    public int askForCommentForFriendRequest(long chatId) {
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
        Message inlineMessage;
        try {
            // remove previous reply keyboard, as not sending a new one (only inline)
            removeReplyKeyboardMarkup(chatId);
            inlineMessage = sender.execute(message);
        } catch (TelegramApiException e) {
            logger.error("Asking for comments failed for {}", chatId);
            // TODO ABORT THE FUNCTION (REMOVE THE KEYBOARD ETC.)
            // TODO CHANGE THIS TO THROWING AN ERROR
            return -1;
        }
        return inlineMessage.getMessageId();
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
        String text = String.format("Please confirm the sending of friend request!\n" +
                        "@%s will receive a following message from you:\n\n%s",
                receiver.getUser().getUserName(), comment);
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
        // TODO move callback query generation to separate place
        String acceptFriendRequestCallback =
                String.format("accept_friend_request:%s:%s", requestSender.getUserId(), receiver.getUserId());
        String declineFriendRequestCallback =
                String.format("decline_friend_request:%s:%s", requestSender.getUserId(), receiver.getUserId());
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
            sendErrorMessage(e.getMessage(), requestSender.getChatId());
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
            // TODO MAKE SEND ERROR TO USER AND ABORT THE REQUEST
            sendErrorMessage("Send location request failed! Please try again!", chatId);
        }
    }

    /**
     * Sends result notification to the location sharer {@code user}
     * */
    public void sendLocationSharingResult(GeoUser user, boolean success) {
        SendMessage message = SendMessage.builder()
                .chatId(user.getChatId())
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
//                SendMessage message = SendMessage.builder()
////                        .entities(List.of(entity))
//                        .chatId(chatId)
//                        .text(text)
//                        .build();
                sender.execute(message);
            } catch (TelegramApiException e) {
                logger.error("Text sending failed by user: {}", user.getChatId());
                throw new TelegramApiException("Text sending failed by: " + user.getChatId());
            }
        }
    }

    public void sendFriendList(Long chatId, String friendList) {
        String text = friendList.isEmpty()
                ?"You don't have any friends yet :( You can add them via /add_friend command"
                : "Here are your friends:\n" + friendList;
        silent.send(text, chatId);
    }

    public void abortFriendRemoving(long chatId) {
        silent.send("Friend removing aborted!", chatId);
    }

    public void sendFriendListToRemove(long chatId, List<Map.Entry<String, String>> buttons) {
        if (buttons.isEmpty()) {
            silent.send("You don't have any friends yet :( You can add them via /add_friend command", chatId);
            return;
        }
        int maxAmount = 5;
        // list that will be sent to user
        List<Map.Entry<String, String>> resultList = buttons.subList(0, Math.min(buttons.size(), maxAmount));
        String nextBtnCallback = resultList.size() < maxAmount ? null : "remove_friend:index:5";
        // don't need "previousButton" in the first list
        InlineKeyboardMarkup keyboardMarkup = KeyboardFactory.removeFriendInlineKeyboard(resultList, nextBtnCallback, null);

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
    }

    public void sendFriendListToRemove(long chatId, int messageId, List<Map.Entry<String, String>> buttons, int startIndex) {
        if (buttons.size() <= startIndex) {
            logger.error("Button list is smaller than start index");
            return;
        }
        int maxAmount = startIndex + 5;
        // list that will be sent to user
        List<Map.Entry<String, String>> resultList = buttons.subList(startIndex, Math.min(buttons.size(), maxAmount));
        String nextBtnCallback = buttons.size() - 1 <= maxAmount
                ? null
                : "remove_friend:index:" + Math.min(buttons.size() - 1, maxAmount);
        String prevBtnCallback = startIndex + resultList.size() <= 5
                ? null
                : "remove_friend:index:" + Math.max(0, startIndex - 5);
        // don't need "previousButton" in the first list
        InlineKeyboardMarkup keyboardMarkup =
                KeyboardFactory.removeFriendInlineKeyboard(resultList, nextBtnCallback, prevBtnCallback);

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

    public void askToConfirmFriendRemove(long chatId, String userName , String friendId) {
        InlineKeyboardMarkup keyboardMarkup = KeyboardFactory.removeFriendConfirmInlineKeyboard("remove_friend:confirm:"+friendId);
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

    public void sendSuccessfullyDeleted(long chatId, String userName) {
        silent.send("Successfully removed @" + userName + " from friends!", chatId);
    }

    /**
     * @param userName userName of a person who removed this one from friends
     */
    public void sendDeletedFromFriends(long chatId, String userName) {
        silent.send("@" + userName + " has removed you from friends. You are no longer sharing location with them! " +
                "If you want to add them back to friends - send new /add_friend command!", chatId);
    }
}
