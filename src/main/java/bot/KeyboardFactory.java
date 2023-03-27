package bot;

import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButtonRequestUser;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import utils.Constants;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
                .text(Constants.FriendRequestConstants.ADD_FRIEND)
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
                                        .text(Constants.FriendRequestConstants.ACCEPT)
                                        // callback data in form (response:to:from)
                                        .callbackData(acceptCallback)
                                        .build(),
                                InlineKeyboardButton
                                        .builder()
                                        .text(Constants.FriendRequestConstants.DECLINE)
                                        .callbackData(declineCallback)
                                        .build()
                        )
                )
                .build();
    }

    public static InlineKeyboardMarkup friendRequestCommentInlineKeyboard(String sendCallback, String abortCallback) {
        return InlineKeyboardMarkup.builder()
                .keyboardRow(
                        List.of(
                                InlineKeyboardButton
                                        .builder()
                                        .text(Constants.FriendRequestConstants.SEND_WITHOUT_COMMENT)
                                        // callback data in form (response:to:from)
                                        .callbackData(sendCallback)
                                        .build(),
                                InlineKeyboardButton
                                        .builder()
                                        .text(Constants.FriendRequestConstants.ABORT_SENDING)
                                        .callbackData(abortCallback)
                                        .build()
                        )
                )
                .build();
    }

    // todo change abort callback to unified from constants
    public static InlineKeyboardMarkup friendRequestConfirmInlineKeyboard(String confirmCallback, String abortCallback) {
        return InlineKeyboardMarkup.builder()
                .keyboardRow(
                        List.of(
                                InlineKeyboardButton
                                        .builder()
                                        .text(Constants.FriendRequestConstants.SEND)
                                        // callback data in form (response:to:from)
                                        .callbackData(confirmCallback)
                                        .build(),
                                InlineKeyboardButton
                                        .builder()
                                        .text(Constants.FriendRequestConstants.ABORT_SENDING)
                                        .callbackData(abortCallback)
                                        .build()
                        )
                )
                .build();
    }

    /**
     * @param buttons button map with button text as key and callback data as value
     * @param nextBtnCallback if null, no "next" button will be created
     * @param previousBtnCallback if null, no "previous" button will be created  */
    public static InlineKeyboardMarkup removeFriendInlineKeyboard(List<Map.Entry<String, String>> buttons, String nextBtnCallback,
                                                                  String previousBtnCallback) {
        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        for (Map.Entry<String, String> button : buttons) {
            // setting keyboard rows with button text as key and callback data as value
            rows.add(
                    List.of(
                            InlineKeyboardButton.builder()
                                    .text(button.getKey())
                                    .callbackData(button.getValue())
                                    .build()
                    )
            );
        }

        List<InlineKeyboardButton> listInstructionRow = new ArrayList<>();
        if (previousBtnCallback != null) {
            listInstructionRow.add(
                    InlineKeyboardButton
                            .builder()
                            .text(Constants.RemoveFriendConstants.PREVIOUS)
                            .callbackData(previousBtnCallback)
                            .build()
            );
        }
        if (nextBtnCallback != null) {
            listInstructionRow.add(
                    InlineKeyboardButton
                            .builder()
                            .text(Constants.RemoveFriendConstants.NEXT)
                            .callbackData(nextBtnCallback)
                            .build()
            );
        }
        if (!listInstructionRow.isEmpty())
            rows.add(listInstructionRow);


        InlineKeyboardButton abortButton = InlineKeyboardButton
                .builder()
                .text(Constants.RemoveFriendConstants.ABORT)
                .callbackData(Constants.RemoveFriendConstants.ABORT_CALLBACK_QUERY)
                .build();
        // last button is the abort button
        rows.add(List.of(abortButton));
        keyboardMarkup.setKeyboard(rows);
        return keyboardMarkup;
    }

    public static InlineKeyboardMarkup removeFriendConfirmInlineKeyboard(String confirmCallback) {
        return InlineKeyboardMarkup.builder()
                .keyboardRow(
                        List.of(
                                InlineKeyboardButton
                                        .builder()
                                        .text(Constants.FriendRequestConstants.SEND)
                                        .callbackData(confirmCallback)
                                        .build(),
                                InlineKeyboardButton
                                        .builder()
                                        .text(Constants.RemoveFriendConstants.ABORT)
                                        .callbackData(Constants.RemoveFriendConstants.ABORT_CALLBACK_QUERY)
                                        .build()
                        )
                )
                .build();
    }
}
