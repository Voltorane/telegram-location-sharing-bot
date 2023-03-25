package bot;

import bot.storage.GeoUser;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButtonRequestUser;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import utils.Constants;

import java.util.ArrayList;
import java.util.List;

/**
 * Class that is responsible for creating all keyboards i.e. Inline or not
 * */
public class KeyboardFactory {
    // abort button that is used for all reply keyboard markups
    private static final KeyboardButton abortButton = new KeyboardButton(Constants.ABORT_BUTTON);

    /**
     * @return ReplyKeyboard with "Add friend" button with request user flag and an abort button
     * */
    public static ReplyKeyboardMarkup addFriendKeyboard() {
        KeyboardButton addFriendButton = KeyboardButton
                .builder()
                .text(Constants.ADD_FRIEND_BUTTON)
                .requestUser(KeyboardButtonRequestUser.builder()
                        .requestId("1")
                        .build())
                .build();
        return ReplyKeyboardMarkup
                .builder()
                .resizeKeyboard(true)
                .oneTimeKeyboard(true)
                .keyboardRow(
                        new KeyboardRow(
                                List.of(addFriendButton, abortButton)
                        )
                )
                .build();
    }

    /**
     * @return ReplyKeyboard with "Share location" button with request location flag and an abort button
     * */
    public static ReplyKeyboardMarkup shareLocationKeyboard() {
        KeyboardButton locationButton = KeyboardButton.builder()
                .text(Constants.SHARE_LOCATION_BUTTON)
                .requestLocation(true)
                .build();
        return ReplyKeyboardMarkup
                .builder()
                .resizeKeyboard(true)
                .oneTimeKeyboard(true)
                .keyboardRow(
                        new KeyboardRow(
                                List.of(locationButton, abortButton)
                        )
                ).build();
    }

    /**
     * @return ReplyKeyboardRemove with removeKeyboard flag enabled to remove the keyboard from the user
     * */
    public static ReplyKeyboardRemove removeKeyboard() {
        return ReplyKeyboardRemove
                .builder()
                .removeKeyboard(true)
                .build();
    }

    /**
     * @return empty ReplyKeyboardMarkup to clear the keyboard
     * */
    public static ReplyKeyboardMarkup clearKeyboard() {
        return ReplyKeyboardMarkup
                .builder()
                .keyboard(List.of(new KeyboardRow(new ArrayList<>())))
                .build();
    }

    /**
     * @return empty keyboard that can replace the existing inline keyboard -> clearing it on the message
     * */
    public static InlineKeyboardMarkup removeInlineKeyboard() {
        return InlineKeyboardMarkup
                .builder()
                .keyboard(new ArrayList<>())
                .build();
    }

    /**
     * Remove inline keyboard from the provided message
     *
     * @param chatId id of chat to remove keyboard in
     * @param inlineMessageId id of the message that has {@link InlineKeyboardMarkup} that needs to be removed
     * @return edit message reply markup, that removes inline keyboard from the message
     * */
    public static EditMessageReplyMarkup removeInlineKeyboard(long chatId, int inlineMessageId) {
        return EditMessageReplyMarkup
                .builder()
                .chatId(chatId)
                .messageId(inlineMessageId)
                .replyMarkup(removeInlineKeyboard())
                .build();
    }

    /**
     * Returns Inline Keyboard Markup for an incoming friend request notification with accept and decline options
     *
     * @param acceptCallback callback data to be sent with accept button
     * @param declineCallback callback data to be sent with decline button
     * @return Inline Keyboard Markup for an incoming friend request notification
     * */
    public static InlineKeyboardMarkup friendRequestInlineKeyboard(String acceptCallback, String declineCallback) {
        return InlineKeyboardMarkup.builder()
                .keyboardRow(
                        List.of(
                                InlineKeyboardButton
                                        .builder()
                                        .text(Constants.ACCEPT_FRIEND_REQUEST_INLINE_BUTTON)
                                        // callback data in form (response:to:from)
                                        .callbackData(acceptCallback)
                                        .build(),
                                InlineKeyboardButton
                                        .builder()
                                        .text(Constants.DECLINE_FRIEND_REQUEST_INLINE_BUTTON)
                                        .callbackData(declineCallback)
                                        .build()
                        )
                )
                .build();
    }
}
