package bot;

import bot.model.*;
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

        // create folder for logs
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
                    userStorage.addUser(ctx.user(), ctx.chatId());
                })
                .build();
    }

    /**
     * Sends help message to the user (same as with /start)
     * */
    @SuppressWarnings("unused")
    public Ability help() {
        return Ability
                .builder()
                .name("help")
                .info("send help message")
                .input(0)
                .privacy(PUBLIC)
                .locality(USER)
                .action(ctx -> responseHandler.replyToStart(ctx.chatId()))
                .build();
    }

    /**
     * Returns text that will be sent by /share_location
     *
     * @param userName user name that will be attached to the location message
     * @param location location that will be sent to users (country, city)
     * @return text that will be sent by /share_location
     * */
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
    private void sendLocationToFriends(User user, long chatId, Location location) throws IOException, UserNotRegisteredException {
        if (location == null) {
            throw new RuntimeException("Location was not provided!");
        }
        LocationFinder.Location parsedLocation
                = LocationFinder.getLocation(location.getLatitude(), location.getLongitude());
        String locationText = getLocationText(user.getUserName(), parsedLocation);
        GeoUser locationSender = userStorage.getOrRegister(user, chatId);

        try {
            List<Long> friendChatIds = getFriendChatIdsToShareLocationWith(locationSender);
            if (friendChatIds.isEmpty()) {
                responseHandler.sendHasNoFriends(locationSender.getChatId());
                return;
            }
            responseHandler.sendLocationToFriends(locationSender,
                    friendChatIds, locationText);
            responseHandler.sendLocationSharingResult(locationSender, true);
        } catch (TelegramApiException e) {
            responseHandler.sendLocationSharingResult(locationSender, false);
        } catch (IllegalArgumentException e) {
            logger.error("Location sharing failed: " + e.getMessage());
            responseHandler.sendErrorMessage(locationSender.getChatId(), "Location sharing failed! Please try later!"
            );
        }
    }

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
                .action(ctx -> {
                    if (userStorage.getOrRegister(ctx.user(), ctx.chatId()).getFriends().isEmpty()) {
                            responseHandler.sendHasNoFriends(ctx.chatId());
                    } else {
                        responseHandler.askForLocation(ctx.chatId());
                    }
                })
                .reply((bot, upd) -> {
                            Location location = upd.getMessage().getLocation();
                            try {
                                sendLocationToFriends(upd.getMessage().getFrom(), upd.getMessage().getChatId(),
                                        location);
                            } catch (Exception e) {
                                responseHandler.sendErrorMessage(getChatId(upd), e.getMessage());
                            }
                        }, Flag.LOCATION)
                .build();
    }

    /**
     * Performs action if user was shared by add_friend ability
     *
     * @param upd update with shared user in message
     * @return OngoingFriendRequest wrapper or {@code null} if receiver is already a friend
     * @throws IllegalArgumentException if user was not shared for some reason
     * @throws UserNotRegisteredException if user that was shared is not registered
     */
    private OngoingFriendRequest friendRequestUserShared(Update upd) {
        UserShared userShared = upd.getMessage().getUserShared();
        if (userShared == null) {
            throw new IllegalArgumentException("User was not shared!");
        }
        GeoUser sender = userStorage.getOrRegister(upd.getMessage().getFrom(), upd.getMessage().getChatId());
        GeoUser receiver = userStorage.getUser(userShared.getUserId());
        if (receiver == null) {
            throw new UserNotRegisteredException("User is not registered by @" + getBotUsername() +
                    "! Please advise him to register and try again!");
        }
        if (sender.getFriends().contains(receiver)) {
            return null;
        }
        OngoingFriendRequest request = new OngoingFriendRequest(sender, receiver);
        ongoingFriendRequests.put(sender.getUserId(), request);
        return request;
    }

    /**
     * Function that returns predicate for an {@link Update} that checks if callbackQuery data of that update is
     * a valid response for friend request
     *
     * @return predicate for an {@link Update} that checks if callbackQuery data of that update is a
     * valid response for friend request
     */
    @NotNull
    private Predicate<Update> friendRequestResponseCallback() {
        return upd -> CallbackQueryDataFactory.FriendRequestAnswer.test(upd.getCallbackQuery().getData());
    }

    /**
     * Performs action if friend request was answered by the receiver
     *
     * @param upd update with answer query in {@code upd.callbackQuery.data}
     */
    private void friendRequestAnswered(Update upd) {
        String[] arguments = upd.getCallbackQuery().getData().split(":");
        if (arguments.length != 3) {
            responseHandler.sendErrorMessage(upd.getCallbackQuery().getMessage().getChatId(), "Invalid amount of arguments in callback query!"
            );
            logger.error("Invalid amount of arguments in callback query for chat {} !",
                    upd.getCallbackQuery().getMessage().getChatId());
        }

        GeoUser sender = userStorage.getUser(Long.parseLong(arguments[1]));
        if (sender == null) {
            responseHandler.sendErrorMessage(upd.getCallbackQuery().getMessage().getChatId(), "You are not registered by the bot!" +
                    " Please do so with the /start command");
            return;
        }

        GeoUser receiver = userStorage.getUser(Long.parseLong(arguments[2]));
        if (receiver == null) {
            responseHandler.sendErrorMessage(upd.getCallbackQuery().getMessage().getChatId(), "User is not registered by the bot!" +
                            " Please advise them to register to use this functionality"
            );
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
                responseHandler.sendErrorMessage(receiver.getChatId(), "Invalid callback instruction for friend request!"
                );
                logger.error("Invalid callback instruction for friend request!");
            }
        }
        responseHandler.removeInlineKeyboard(receiver.getChatId(), request.inlineMessageId());
    }

    /**
     * Reply that is triggered when friend request receiver responds on it
     * */
    @SuppressWarnings("unused")
    public Reply friendRequestAnswered() {
        return Reply.of((bot, upd) -> friendRequestAnswered(upd), friendRequestResponseCallback());
    }

    /**
     * Function that returns predicate for an {@link Update} that checks if user from which the update is from
     * has ongoing friend requests. This way, we can guarantee that some reserved texts won't be misinterpreted
     * in the friend request
     *
     * @return predicate for an {@link Update} that checks if user from which the update is from
     * has ongoing friend requests
     */
    @NotNull
    private Predicate<Update> hasOngoingFriendRequests() {
        return upd -> {
            User user = upd.getMessage().getFrom();
            Long chatId = upd.getMessage() == null
                    ? upd.getCallbackQuery().getMessage().getChatId()
                    : upd.getMessage().getChatId();
            GeoUser geoUser = userStorage.getOrRegister(user, chatId);
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
     * Function that returns predicate for an {@link Update} that checks if message text of that update is
     * equal to the {@code msg}
     *
     * @param msg text that will be compared to the message text of an update
     * @return predicate for an {@link Update} that checks if message text of that update is
     * equal to the {@code msg}
     */
    @NotNull
    private Predicate<Update> hasMessageWith(String msg) {
        return upd -> upd.getMessage().getText().equalsIgnoreCase(msg);
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
     * {@link ReplyFlow} that represents the flow of sending and getting response of the
     * friend request
     */
    @SuppressWarnings("unused")
    public ReplyFlow friendRequestFlow() {
        // some messages in friend request flow, user can click the abort button, that ends the friend request
        Reply abortFriendRequest = Reply.of(
                (bot, upd) -> {
                    Message messageReceived = upd.getCallbackQuery().getMessage();
                    User user = upd.getCallbackQuery().getFrom();
                    GeoUser sender = userStorage.getOrRegister(user, messageReceived.getChatId());
                    logger.info("{} aborted request", messageReceived.getChatId());

                    responseHandler.abortedSendingFriendRequest(sender);
                    // removing ongoing friend request indicates finish of the request
                    ongoingFriendRequests.remove(sender.getUserId());
                    // deleting message as it's no longer needed
                    responseHandler.deleteMessage(messageReceived.getChatId(), messageReceived.getMessageId());
                }, hasCallbackWith(Constants.FriendRequestConstants.ABORT_CALLBACK_QUERY));

        // used for both cases, when user has no comments and when he sends the friend request after the preview
        Reply confirmedSendingFriendRequest = Reply.of(
                (bot, upd) -> {
                    Message messageReceived = upd.getCallbackQuery().getMessage();
                    User user = upd.getCallbackQuery().getFrom();
                    GeoUser sender = userStorage.getOrRegister(user, messageReceived.getChatId());
                    OngoingFriendRequest wrapper = ongoingFriendRequests.get(sender.getUserId());
                    logger.info("{} confirmed sending friend request!", user.getId());

                    // sending an actual friend request to the recipient
                    responseHandler.sendFriendRequest(wrapper.getSender(), wrapper.getReceiver(), wrapper.getComment());
                    // removing ongoing friend request indicates finish of the request
                    ongoingFriendRequests.remove(sender.getUserId());
                    responseHandler.deleteMessage(sender.getChatId(), upd.getCallbackQuery().getMessage().getMessageId());
                }, hasCallbackWith(Constants.FriendRequestConstants.CONFIRM_CALLBACK_QUERY));

        // user has written text as reply bot
        ReplyFlow hasCommentsFlow = ReplyFlow.builder(db)
                .onlyIf(upd -> !upd.getMessage().getText().isEmpty() && !upd.getMessage().isCommand())
                .action((bot, upd) -> {
                    Message messageReceived = upd.getMessage();
                    User user = messageReceived.getFrom();
                    GeoUser sender = userStorage.getOrRegister(user, messageReceived.getChatId());
                    logger.info("{} has comments: {}", messageReceived.getChatId(), messageReceived.getText());

                    OngoingFriendRequest wrapper = ongoingFriendRequests.get(sender.getUserId());
                    // comment will be accessible when confirming the friend request sending
                    wrapper.setComment(upd.getMessage().getText());
                    // request preview has options as to send the friend request or abort it
                    responseHandler.sendFriendRequestPreview(wrapper.getSender(), wrapper.getReceiver(), wrapper.getComment());
                    // remove keyboard from the message that asked for further comments
                    responseHandler.deleteMessage(messageReceived.getChatId(), wrapper.getSenderInlineMessageId());
                })
                .next(confirmedSendingFriendRequest)
                .next(abortFriendRequest)
                .build();

        ReplyFlow userSharedFlow = ReplyFlow.builder(db)
                .onlyIf(userSharedRegistered())
                .action((bot, upd) -> {
                    Message messageReceived = upd.getMessage();
                    OngoingFriendRequest wrapper;
                    try {
                        wrapper = friendRequestUserShared(upd);
                    } catch (UserNotRegisteredException | IllegalArgumentException e) {
                        // recipient is not registered or user wasn't shared
                        responseHandler.sendErrorMessage(messageReceived.getChatId(), e.getMessage());
                        return;
                    }
                    if (wrapper == null) {
                        silent.send("User is already your friend!", messageReceived.getChatId());
                        return;
                    }
                    Integer inlineMessageId = responseHandler.askForCommentForFriendRequest(messageReceived.getChatId());
                    if (inlineMessageId == null) {
                        logger.error("Failed setting inline message id to friend request for {}", messageReceived.getChatId());
                        return;
                    }
                    // storing inlineMessageId in ongoing request to remove inline keyboard, after request is done
                    wrapper.setSenderInlineMessageId(inlineMessageId);
                })
                .next(hasCommentsFlow)
                .next(confirmedSendingFriendRequest)
                .next(abortFriendRequest)
                .build();

        Reply userSharedNotRegistered = Reply.of(
                (bot, upd) -> responseHandler.sendErrorMessage(upd.getMessage().getChatId(), "User is not registered by @" + getBotUsername() +
                        "! Please advise him to register and try again! "), upd -> !userSharedRegistered().test(upd));

        return ReplyFlow.builder(db)
                .onlyIf(hasMessageWith("/add_friend"))
                .action((bot, upd) -> responseHandler.askToShareFriendToAdd(upd.getMessage().getChatId()))
                .next(userSharedFlow)
                .next(userSharedNotRegistered)
                .build();
    }

    /**
     * Returns string representation of a single friend that will be used in the friend list.
     *
     * @param friend user, whose string representation will be returned
     * @return string representation of a single friend that will be used in the friend list
     * in form "user.firstName user.lastName - user.userName"
     * */
    private String getFriendForList(GeoUser friend) {
        String firstName = friend.getUser().getFirstName();
        String lastName = friend.getUser().getLastName() == null ? "" : friend.getUser().getLastName();
        return String.format("%s %s - @%s", firstName, lastName,
                friend.getUser().getUserName());
    }

    /**
     * Returns friend list representation user's friend list.
     *
     * @param friends friends that will be listed
     * @return string representation of user's friend list
     * */
    private String getFriendListRepresentation(List<GeoUser> friends) {
        StringBuilder sb = new StringBuilder();
        int i = 1;
        for (GeoUser friend : friends) {
            // entry in form "i) friendRepresentation"
            sb.append(i).append(") ").append(getFriendForList(friend)).append("\n");
            i++;
        }
        return sb.toString();
    }

    /**
     * Ability that sends friend list to user
     * */
    @SuppressWarnings("unused")
    public Ability friendList() {
        return Ability
                .builder()
                .name("friend_list")
                .info("share list of friends to user")
                .input(0)
                .privacy(PUBLIC)
                .locality(USER)
                .action(ctx -> {
                    GeoUser user = userStorage.getOrRegister(ctx.user(), ctx.chatId());
                    responseHandler.sendFriendList(ctx.chatId(),
                            getFriendListRepresentation(user.getFriends().stream().toList()));
                })
                .build();
    }

    /**
     * Function that returns predicate for an {@link Update} that checks if callbackQuery data of that update is
     * valid callback for removeFriend instruction flow
     *
     * @return predicate for an {@link Update} that checks if callbackQuery data of that update is
     * valid callback for removeFriend instruction flow
     */
    @NotNull
    private Predicate<Update> isRemoveFriendCallback() {
        return upd -> CallbackQueryDataFactory.RemoveFriend.test(upd.getCallbackQuery().getData());
    }

    /**
     * @return text representation of a friend for button in a list (usually remove friend list)
     * */
    private String getFriendButtonText(GeoUser friend) {
        return String.format("@%s", friend.getUser().getUserName());
    }

    /**
     * @param user user whose friends will be translated into buttons
     * @return  a list of button entries for {@link org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup}
     * in form [friendName, remove_friend:friendId], where key should represent button text and value - button callback
     * */
    private List<Map.Entry<String, String>> getRemoveFriendButtons(GeoUser user) {
        List<Map.Entry<String, String>> buttons = new ArrayList<>();
        int i  = 1;
        for (GeoUser friend : user.getFriends()) {
            buttons.add(new AbstractMap.SimpleImmutableEntry<>(String.format("%d) %s", i, getFriendButtonText(friend)),
                    CallbackQueryDataFactory.RemoveFriend.getSelectUserCallback(friend.getUserId())));
            i++;
        }
        return buttons;
    }

    /**
     * Ability to remove friend from user's friend list
     * */
    @SuppressWarnings("unused")
    public Ability removeFriend() {
        return Ability
                .builder()
                .name("remove_friend")
                .info("remove friend from user friend list")
                .input(0)
                .privacy(PUBLIC)
                .locality(USER)
                .action(ctx -> {
                    GeoUser user = userStorage.getOrRegister(ctx.user(), ctx.chatId());
                    responseHandler.sendFriendListToRemove(ctx.chatId(), null, getRemoveFriendButtons(user), 0, true);
                })
                .reply((bot, upd) -> {
                    CallbackQuery callbackQuery = upd.getCallbackQuery();
                    Message messageReceived = callbackQuery.getMessage();
                    GeoUser user = userStorage.getOrRegister(callbackQuery.getFrom(),
                            messageReceived.getChatId());
                    List<Map.Entry<String, String>> buttons = getRemoveFriendButtons(user);
                    // parse incoming arguments
                    String[] arguments = callbackQuery.getData().split(":");
                    switch (arguments[1]) {
                        // next/previous were pressed, new start index is provided
                        case "index" -> responseHandler.sendFriendListToRemove(messageReceived.getChatId(),
                                messageReceived.getMessageId(), buttons, Integer.parseInt(arguments[2]), false);
                        case "abort" -> {
                            responseHandler.deleteMessage(messageReceived.getChatId(), messageReceived.getMessageId());
                            responseHandler.sendActionAbortedMessage(messageReceived);
                        }
                        case "confirm" -> {
                            GeoUser friend = userStorage.getUser(Long.parseLong(arguments[2]));
                            if (friend == null) {
                                logger.error("User is not registered, could not delete him!");
                                return;
                            }
                            user.removeFriend(friend);
                            friend.removeFriend(user);
                            responseHandler.sendSuccessfullyDeleted(messageReceived.getChatId(), friend.getUser().getUserName());
                            responseHandler.sendDeletedFromFriends(friend.getChatId(), user.getUser().getUserName());
                            responseHandler.deleteMessage(messageReceived.getChatId(), messageReceived.getMessageId());
                        }
                        default -> {
                            // provided the friend he wants to remove
                            GeoUser friendToRemove = userStorage.getUser(Long.parseLong(arguments[1]));
                            if (friendToRemove == null) {
                                logger.error("User is not registered, could not delete him!");
                                return;
                            }
                            if (!user.getFriends().contains(friendToRemove)) {
                                silent.send("User is no longer your friend! Could not remove him!", user.getChatId());
                                return;
                            }
                            responseHandler.askToConfirmFriendRemove(upd.getCallbackQuery().getMessage().getChatId(),
                                    friendToRemove.getUser().getUserName(), arguments[1]);
                        }
                    }

                }, isRemoveFriendCallback())
                .build();
    }
}
