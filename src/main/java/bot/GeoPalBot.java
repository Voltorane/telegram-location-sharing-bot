package bot;

import exceptions.ApiKeyException;
import exceptions.UserNotRegisteredException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jetbrains.annotations.NotNull;
import org.telegram.abilitybots.api.bot.AbilityBot;
import org.telegram.abilitybots.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import services.LocationFinder;
import bot.storage.GeoUser;
import bot.storage.GeoUserStorage;
import utils.ConfigLoader;
import utils.Constants;

import java.io.IOException;
import java.util.*;
import java.util.function.Predicate;

import static org.telegram.abilitybots.api.objects.Locality.USER;
import static org.telegram.abilitybots.api.objects.Privacy.PUBLIC;
import static org.telegram.abilitybots.api.util.AbilityUtils.getChatId;

public class GeoPalBot extends AbilityBot {
    public static final Logger logger = LoggerFactory.getLogger(GeoPalBot.class);
    private static long CREATOR_ID;

    private final GeoUserStorage userStorage;
    private final ResponseHandler responseHandler;
    //    private final FriendRequestingService friendRequestingService;
    public Map<Long, OngoingFriendRequest> ongoingFriendRequests = new HashMap<>();

    /**
     * Constructor for the LocationSharingBot
     *
     * @throws IOException     if configuration property parsing failed
     * @throws ApiKeyException if creator ID was not provided or incorrectly provided in the configuration properties
     */
    public GeoPalBot() throws IOException, NumberFormatException, ApiKeyException {
        super(ConfigLoader.getProperty(ConfigLoader.ConfigProperty.TELEGRAM_BOT_API_KEY),
                ConfigLoader.getProperty(ConfigLoader.ConfigProperty.TELEGRAM_BOT_USERNAME));
        String creatorIdSting = ConfigLoader.getProperty(ConfigLoader.ConfigProperty.TELEGRAM_BOT_CREATOR_ID);
        if (creatorIdSting.isEmpty()) {
            throw new ApiKeyException("Bot configuration is incorrect. Creator ID is empty!" +
                    "Please check config.properties!");
        }
        try {
            CREATOR_ID = Long.parseLong(creatorIdSting);
        } catch (NumberFormatException e) {
            throw new ApiKeyException("Bot configuration is incorrect, please check config.properties!");
        }
        responseHandler = new ResponseHandler(sender, silent, db);
        userStorage = new GeoUserStorage();
//        friendRequestingService = new FriendRequestingService();

        java.io.File logsDir = new java.io.File("logs");
        if (!logsDir.exists()) {
            if (logsDir.mkdir()) {
                logger.info("Created logs directory");
            } else {
                logger.error("Failed to create logs directory");
            }
        }
    }

    @Override
    public long creatorId() {
        return CREATOR_ID;
    }

    @NotNull
    private Predicate<Update> hasMessageWith(String msg) {
        return upd -> upd.getMessage().getText().equalsIgnoreCase(msg);
    }

    @NotNull
    private Predicate<Update> friendRequestCallback() {
        return upd -> upd.getCallbackQuery() != null
                && upd.getCallbackQuery().getData().matches("(accept|decline)_friend_request:\\d*:\\d*");
    }

    @NotNull
    private Predicate<Update> friendDeleteCallback() {
        return upd -> upd.getCallbackQuery() != null
                && upd.getCallbackQuery().getData().matches("(remove_friend:\\d*)" +
                "|(remove_friend:next:\\d*)" +
                "|(remove_friend:previous:\\d*)|(remove_friend:abort)");
    }

    /**
     * @return list of chat ids of friends that will receive shared location from the user
     */
    private List<Long> getFriendChatIdsToShareLocationWith(GeoUser user) {
        return user.getFriends().stream().map(GeoUser::getChatId).toList();
    }

    /**
     * Registers user with the bot, initializes his contact storages
     */
    @SuppressWarnings("unused")
    public Ability start() {
        return Ability
                .builder()
                .name("start")
                .info("start conversation with bot")
                .input(0)
                .privacy(PUBLIC)
                .locality(USER)
                .action(ctx -> {
                    responseHandler.replyToStart(ctx.chatId());
                    // TODO MOVE TO REGISTER ABILITY
//                    responseHandler.removeReplyKeyboardMarkup(ctx.chatId());
                    // TODO add distinction between correct registering of a user and one with exceptions
                    this.userStorage.addUser(ctx.user(), ctx.chatId());
                })
                .build();
    }

    private String getLocationText(String userName, LocationFinder.Location location) {
        return String.format("@%s is now in %s!", userName, location);
    }

    /**
     * Sends location to all friends of the {@code user}. Text with location is generated
     * by the {@code getLocationText()}.
     *
     * @param user     sender of the location text
     * @param location Telegram Bot API location, that will be sent to friends of the user
     */
    private void sendLocationToFriends(User user, Location location) throws IOException, UserNotRegisteredException {
        if (location == null) {
            throw new RuntimeException("Location was not provided!");
        }
        LocationFinder.Location parsedLocation
                = LocationFinder.getLocation(location.getLatitude(), location.getLongitude());
        String locationText = getLocationText(user.getUserName(), parsedLocation);
        GeoUser locationSender = userStorage.getOrRegister(user, user.getId());

        try {
            responseHandler.sendLocationToFriends(locationSender,
                    getFriendChatIdsToShareLocationWith(locationSender), locationText);
            responseHandler.sendLocationSharingResult(locationSender, true);
        } catch (TelegramApiException e) {
            responseHandler.sendLocationSharingResult(locationSender, false);
        } catch (IllegalArgumentException e) {
            logger.error("Location sharing failed: " + e.getMessage());
            responseHandler.sendErrorMessage("Location sharing failed! Please try later!",
                    locationSender.getChatId());
        }
    }

//    public void sendFriendRequest(GeoUser.FriendRequest friendRequest) {
////        GeoUser sender = friendRequest.sender();
////        GeoUser receiver = friendRequest.receiver();
////        String invitationText = friendRequest.text();
//        // todo add invitations to pending friend requests
//        // todo add answering to friend requests
//        MessageEntity entity = MessageEntity.builder()
//                .type("mention")
//                .offset(32)
//                .length(sender.getUser().getUserName().length())
//                .user(sender.getUser())
//                .build();
//        InlineKeyboardMarkup keyboardMarkup = InlineKeyboardMarkup.builder()
//                .keyboardRow(
//                        List.of(
//                                InlineKeyboardButton.builder()
//                                        .text("Accept✅")
//                                        // callback data in form (response:to:from)
//                                        .callbackData(String.format("accept_friend_request:%s:%s", sender.getUserId(),
//                                                receiver.getUserId()))
//                                        .build(),
//                                InlineKeyboardButton.builder()
//                                        .text("Decline❌")
//                                        .callbackData(String.format("decline_friend_request:%s:%s", sender.getUserId(),
//                                                receiver.getUserId()))
//                                        .build())
//                )
//                .build();
//        // add inline keyboard to answer
//        SendMessage message = SendMessage.builder()
//                .entities(List.of(entity))
//                .chatId(receiver.getChatId())
//                .text(getFriendRequestMessage(sender.getUser().getUserName(), invitationText))
//                .replyMarkup(keyboardMarkup)
//                .build();
//        try {
//            Message m = execute(message);
//            sender.sendFriendRequest(receiver, new GeoUser.FriendRequest(sender, receiver, invitationText, m.getMessageId()));
//        } catch (TelegramApiException e) {
//            sendErrorMessage(e.getMessage(), sender.getChatId());
//        }
//    }

    /**
     * Ability that represents the "/share_location" command from user
     */
    @SuppressWarnings("unused")
    public Ability askLocation() {
        return Ability
                .builder()
                .name("share_location")
                .info(Constants.SHARE_LOCATION_DESCRIPTION)
                .input(0)
                .privacy(PUBLIC)
                .locality(USER)
                .action(ctx -> responseHandler.askForLocation(ctx.chatId()))
                .reply((bot, upd) -> {
                            Location location = upd.getMessage().getLocation();
                            try {
                                this.sendLocationToFriends(upd.getMessage().getFrom(), location);
                            } catch (Exception e) {
                                responseHandler.sendErrorMessage(e.getMessage(), getChatId(upd));
                            }
                        }, Flag.LOCATION
                )
                .build();
    }

    public Ability sayHello() {
        return Ability
                .builder()
                .name("hello")
                .info("say hello")
                .privacy(PUBLIC)
                .locality(USER)
                .action(ctx -> {
                    silent.send("Hello World!", ctx.chatId());
                    logger.error("hello error");
                    logger.info("info");
                    logger.warn("warn");
                })
                .build();
    }

//    public Ability askForFriendToRemove() {
//        InlineKeyboardMarkup keyboardMarkup = InlineKeyboardMarkup.builder().build();
//
//        return Ability
//                .builder()
//                .name("remove_friend")
//                .input(0)
//                .privacy(PUBLIC)
//                .locality(USER)
//                .action(ctx -> {
//                    GeoUser user = userStorage.getUser(ctx.user().getId());
//                    SendMessage message = SendMessage.builder()
//                            .text("Please select friend you want to remove:")
//                            .replyMarkup(KeyboardFactory.removeFriendKeyboard(user.getFriends().stream().toList(), 0))
//                            .chatId(ctx.chatId())
//                            .build();
//                    try {
//                        sender.execute(message);
//                    } catch (TelegramApiException e) {
//                        e.printStackTrace();
//                    }
//                })
//                .flag(Flag.CALLBACK_QUERY)
//                .reply((bot, upd) -> {
//                    CallbackQuery query = upd.getCallbackQuery();
//                    RemoveFriendCommand command = new RemoveFriendCommand(query);
//                    command.executeCallbackCommand(this, upd);
//                }, friendDeleteCallback())
//                .build();
//    }

    /**
     * Performs action if user was shared by add_friend ability
     *
     * @param upd update with shared user in message
     * @throws IllegalArgumentException if user was not shared for some reason
     * @throws UserNotRegisteredException if user that was shared is not registered
     */
    private OngoingFriendRequest friendRequestUserShared(Update upd) {
        UserShared userShared = upd.getMessage().getUserShared();
        if (userShared == null) {
            throw new IllegalArgumentException("User was not shared!");
        }
        GeoUser sender = userStorage.getOrRegister(upd.getMessage().getFrom(), upd.getMessage().getFrom().getId());
        GeoUser receiver = userStorage.getUser(userShared.getUserId());
        if (receiver == null) {
            throw new UserNotRegisteredException("User is not registered by @" + this.getBotUsername() +
                    "! Please advise him to register and try again!");
        }
        OngoingFriendRequest request = new OngoingFriendRequest(sender, receiver);
        ongoingFriendRequests.put(sender.getUserId(), request);
        return request;
    }

    /**
     * Performs action if friend request was answered by the receiver
     *
     * @param upd update with answer query in {@code upd.callbackQuery.data}
     */
    private void friendRequestAnswered(Update upd) {
        String[] arguments = upd.getCallbackQuery().getData().split(":");
        if (arguments.length != 3) {
            responseHandler.sendErrorMessage("Invalid amount of arguments in callback query!",
                    upd.getCallbackQuery().getMessage().getChatId());
            logger.error("Invalid amount of arguments in callback query for chat {} !",
                    upd.getCallbackQuery().getMessage().getChatId());
        }

        GeoUser sender = userStorage.getUser(Long.parseLong(arguments[1]));
        if (sender == null) {
            responseHandler.sendErrorMessage("You are not registered by the bot!" +
                    " Please do so with the /start command", upd.getCallbackQuery().getMessage().getChatId());
            return;
        }

        GeoUser receiver = userStorage.getUser(Long.parseLong(arguments[2]));
        if (receiver == null) {
            responseHandler.sendErrorMessage("User is not registered by the bot!" +
                            " Please advise them to register to use this functionality",
                    upd.getCallbackQuery().getMessage().getChatId());
            return;
        }

        GeoUser.FriendRequest request = receiver.getIncomingFriendRequests().get(sender);
        switch (arguments[0]) {
            case "accept_friend_request" -> {
                receiver.acceptFriendRequest(sender);
                // send acceptance messages
                responseHandler.sendFriendRequestAccepted(sender, receiver);
            }
            case "decline_friend_request" -> {
                receiver.declineFriendRequest(sender);
                // send decline messages
                responseHandler.sendFriendRequestDeclined(sender, receiver);
            }
            default -> {
                responseHandler.sendErrorMessage("Invalid callback instruction for friend request!",
                        receiver.getChatId());
                logger.error("Invalid callback instruction for friend request!");
            }
        }
        responseHandler.removeInlineKeyboard(receiver.getChatId(), request.inlineMessageId());
    }

    public Reply friendRequestAnswered() {
        return Reply.of((bot, upd) -> friendRequestAnswered(upd), friendRequestCallback());
    }

    @NotNull
    private Predicate<Update> hasOngoingFriendRequests() {
        return upd -> {
            User user = upd.getMessage().getFrom();
            GeoUser geoUser = userStorage.getOrRegister(user, user.getId());
            return ongoingFriendRequests.containsKey(geoUser.getUserId());
        };
    }

    @SuppressWarnings("unused")
    public Reply actionAborted() {
        return Reply.of((bot, upd) -> responseHandler.sendActionAbortedMessage(upd.getMessage()),
                // check that bot received an abort command and has no ongoing actions (friend requesting)
                upd -> upd.getMessage().getText().equals("❌") && !hasOngoingFriendRequests().test(upd));
    }

    /**
     * Function that returns predicate for an {@link Update} that checks if update message is a command
     */
    @NotNull
    private Predicate<Update> isCommand() {
        return upd -> upd.getMessage().isCommand();
    }

    /**
     * Function that returns predicate for an {@link Update} that checks if callbackQuery data of that update is
     * equal to the {@code callbackData}
     *
     * @param callbackData data that will be compared to the callbackQuery data of an update
     * @return predicate for an {@link Update} that checks if callbackQuery data of that update is
     * equal to the {@code callbackData}
     */
    @NotNull
    private Predicate<Update> hasCallbackWith(String callbackData) {
        return upd -> upd.getCallbackQuery().getData().equals(callbackData);
    }

    /**
     * Function that returns predicate for an {@link Update} that checks if user shared by this update is registered by the bot
     * */
    @NotNull
    private Predicate<Update> userSharedRegistered() {
        return upd -> userStorage.getUsers().containsKey(upd.getMessage().getUserShared().getUserId());
    }

    /**
     * {@link ReplyFlow} that represents the flow of sending, receiving and getting response of the
     * friend request
     */
    @SuppressWarnings("unused")
    public ReplyFlow friendRequestFlow() {
        // some messages in friend request flow, user can click the abort button, that ends the friend request
        Reply abortFriendRequest = Reply.of(
                (bot, upd) -> {
                    Message messageReceived = upd.getCallbackQuery().getMessage();
                    User user = upd.getCallbackQuery().getFrom();
                    GeoUser sender = userStorage.getOrRegister(user, user.getId());
                    logger.info("{} aborted request", messageReceived.getChatId());

                    responseHandler.abortedSendingFriendRequest(sender);
                    // removing ongoing friend request indicates finish of the request
                    ongoingFriendRequests.remove(sender.getUserId());
                    // deleting message as it's no longer needed
                    responseHandler.deleteMessage(upd.getCallbackQuery().getMessage());
                }, hasCallbackWith(Constants.FriendRequestConstants.ABORT_CALLBACK_QUERY));

        // used for both cases, when user has no comments and when he sends the friend request after the preview
        Reply confirmedSendingFriendRequest = Reply.of(
                (bot, upd) -> {
//                    Message messageReceived =
                    User user = upd.getCallbackQuery().getFrom();
                    GeoUser sender = userStorage.getOrRegister(user, user.getId());
                    OngoingFriendRequest wrapper = ongoingFriendRequests.get(sender.getUserId());
                    logger.info("{} confirmed sending friend request!", user.getId());

                    // sending an actual friend request to the recipient
                    responseHandler.sendFriendRequest(wrapper.getSender(), wrapper.getReceiver(), wrapper.getComment());
                    // removing ongoing friend request indicates finish of the request
                    ongoingFriendRequests.remove(sender.getUserId());
                    responseHandler.removeInlineKeyboard(sender.getChatId(), upd.getCallbackQuery().getMessage().getMessageId());
                }, hasCallbackWith(Constants.FriendRequestConstants.CONFIRM_CALLBACK_QUERY));

//        Reply noComments = Reply.of(
//                (bot, upd) -> {
//                    long chatId = upd.getCallbackQuery().getMessage().getChatId();
//                    logger.info("{} has no comments", chatId);
//                    Message messageReceived = upd.getCallbackQuery().getMessage();
//                    User user = messageReceived.getFrom();
//                    GeoUser sender = userStorage.getOrRegister(user, user.getId());
//
//                    OngoingFriendRequest wrapper = ongoingFriendRequests.get(sender);
//                    responseHandler.sendFriendRequest(wrapper.getSender(), wrapper.getReceiver(), "");
//                    ongoingFriendRequests.remove(sender);
//                    responseHandler.removeInlineKeyboard(chatId, upd.getCallbackQuery().getMessage().getMessageId());
//                }, hasCallbackWith(Constants.FriendRequestConstants.CONFIRM_CALLBACK_QUERY));

        // user has written text as reply bot
        ReplyFlow hasCommentsFlow = ReplyFlow.builder(db)
                .onlyIf(upd -> !upd.getMessage().getText().isEmpty() && !upd.getMessage().isCommand())
                .action((bot, upd) -> {
                    Message messageReceived = upd.getMessage();
                    User user = messageReceived.getFrom();
                    GeoUser sender = userStorage.getOrRegister(user, user.getId());
                    logger.info("{} has comments: {}", messageReceived.getChatId(), messageReceived.getText());

                    OngoingFriendRequest wrapper = ongoingFriendRequests.get(sender.getUserId());
                    // comment will be accessible when confirming the friend request sending
                    wrapper.setComment(upd.getMessage().getText());
                    // request preview has options as to send the friend request or abort it
                    responseHandler.sendFriendRequestPreview(wrapper.getSender(), wrapper.getReceiver(), wrapper.getComment());
                    // remove keyboard from the message that asked for further comments
                    responseHandler.removeInlineKeyboard(messageReceived.getChatId(), wrapper.getSenderInlineMessageId());
                })
                .next(confirmedSendingFriendRequest)
                .next(abortFriendRequest)
                .build();
//        Reply abortedSendingFriendRequest = Reply.of(
//                (bot, upd) -> {
//                    logger.info("{} aborted sending friend request!", upd.getCallbackQuery().getFrom().getId());
//                    Message messageReceived = upd.getCallbackQuery().getMessage();
//                    User user = messageReceived.getFrom();
//                    GeoUser sender = userStorage.getOrRegister(user, user.getId());
//
//                    responseHandler.abortedSendingFriendRequest(sender);
//                    ongoingFriendRequests.remove(sender);
//                    // TODO think of removing the message completely and not only the keyboard
//                    responseHandler.removeInlineKeyboard(sender.getChatId(),
//                            messageReceived.getMessageId());
//                }, hasCallbackWith(Constants.FriendRequestConstants.ABORT_CALLBACK_QUERY));
        ReplyFlow userSharedFlow = ReplyFlow.builder(db)
                .onlyIf(userSharedRegistered())
                .action((bot, upd) -> {
                    Message messageReceived = upd.getMessage();
                    OngoingFriendRequest wrapper;
                    try {
                        wrapper = friendRequestUserShared(upd);
                    } catch (UserNotRegisteredException | IllegalArgumentException e) {
                        // recipient is not registered or user wasn't shared
                        responseHandler.sendErrorMessage(e.getMessage(), messageReceived.getChatId());
                        return;
                    }
                    int inlineMessageId = responseHandler.askForCommentForFriendRequest(messageReceived.getChatId());
                    // storing inlineMessageId in ongoing request to remove inline keyboard, after request is done
                    wrapper.setSenderInlineMessageId(inlineMessageId);
                })
                .next(hasCommentsFlow)
                .next(confirmedSendingFriendRequest)
                .next(abortFriendRequest)
                .build();

        Reply userSharedNotRegistered = Reply.of(
                (bot, upd) -> {
                    responseHandler.sendErrorMessage("User is not registered by @" + this.getBotUsername() +
                            "! Please advise him to register and try again! ", upd.getMessage().getChatId());
                }, upd -> !userSharedRegistered().test(upd));

        return ReplyFlow.builder(db)
                .onlyIf(hasMessageWith("/add_friend"))
                .action((bot, upd) -> responseHandler.askToShareFriendToAdd(upd.getMessage().getChatId()))
                .next(userSharedFlow)
                .next(userSharedNotRegistered)
                .build();
    }

//    @SuppressWarnings("unused")
//    public Ability askForFriendToAdd() {
//        return Ability
//                .builder()
//                .name("add_friend")
//                .input(0)
//                .privacy(PUBLIC)
//                .locality(USER)
//                .action(ctx -> responseHandler.askToShareFriendToAdd(ctx.chatId()))
//                // friend request send by sender (receiver chosen)
//                .reply((bot, upd) -> friendRequestUserShared(upd),
//                        update -> update.getMessage().getUserShared() != null
//                        && update.getMessage().getUserShared().getRequestId().equals("1"))
//                // friend request answered by the receiver
//                .reply((bot, upd) -> friendRequestAnswered(upd), friendRequestCallback())
//                .build();
//    }
}
