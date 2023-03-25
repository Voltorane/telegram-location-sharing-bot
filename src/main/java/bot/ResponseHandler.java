package bot;

import bot.storage.GeoUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.abilitybots.api.db.DBContext;
import org.telegram.abilitybots.api.sender.MessageSender;
import org.telegram.abilitybots.api.sender.SilentSender;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.MessageEntity;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import services.LocationFinder;
import utils.Constants;

import java.util.ArrayList;
import java.util.List;

import static org.telegram.abilitybots.api.util.AbilityUtils.getChatId;

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

    public void sendErrorMessage(String message, long chatId) {
        silent.send(message + Constants.ERROR_MESSAGE_ENDING, chatId);
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
            logger.info("Sending add friend request to chat {}!", chatId);
            sender.execute(message);
        } catch (TelegramApiException e) {
            logger.error("Sending Add friend Request to chat {} failed! {}", chatId, e.getMessage());
            // TODO MAKE SEND ERROR TO USER AND ABORT THE REQUEST
            sendErrorMessage("Add friend request failed! Please try again!", chatId);
        }
    }

    /**
     * Returns String that should be sent to the receiver of the friend request
     *
     * @param userName user name of the sender
     * @param text text that will be attached with the friend request
     * @return text that should be sent to the receiver of the friend request
     * */
    private String getFriendRequestMessage(String userName, String text) {
        return String.format("You got new friend request from @%s\n\n%s", userName, text);
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
        if (success) {
            silent.send("Successfully shared location with your geo pals!",
                    user.getChatId());
        } else {
            sendErrorMessage("Location sharing failed!", user.getChatId());
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
                // TODO REMOVE COMMENTS
//                MessageEntity entity = MessageEntity.builder()
//                        .type("mention")
//                        .offset(0)
//                        .length(user.getUser().getUserName().length())
//                        .user(user.getUser())
//                        .build();
                // resetting message chat id to chat ids from the list
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
}
