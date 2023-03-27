package bot.model;

import org.jetbrains.annotations.NotNull;
import org.telegram.telegrambots.meta.api.objects.User;

import java.util.HashMap;
import java.util.Map;

public class GeoUserStorage {
    // TelegramUser.id to GeoUser
    private final Map<Long, GeoUser> users = new HashMap<>();

    public Map<Long, GeoUser> getUsers() {
        return users;
    }

    /**
     * Adds a new user to the storage.
     * If a user with the given chatId already exists in the storage, the existing user is returned,
     * otherwise a new user is created, added to the storage and returned.
     *
     * @param user   the Telegram User object representing the user to add
     * @param chatId the chatId corresponding to the user
     * @return the GeoUser corresponding to the added or if already existed
     */
    @NotNull
    public GeoUser addUser(User user, Long chatId) {
        GeoUser geoUser;
        if (users.containsKey(user.getId())) {
            geoUser = users.get(user.getId());
        } else {
            geoUser = new GeoUser(user, chatId);
            users.put(user.getId(), geoUser);
        }
        return geoUser;
    }

    /**
     * Returns the GeoUser with the given userId if it exists in the storage, otherwise returns {@code null}.
     * Throws a UserNotRegisteredException if the user is not registered.
     *
     * @param userId the id of the user to retrieve
     * @return the GeoUser corresponding to the given userId, or {@code null} if it does not exist in the storage
     */
    public GeoUser getUser(long userId){
        if (!users.containsKey(userId)) {
            return null;
        }
        return users.get(userId);
    }

    /**
     * Returns the GeoUser with the given user object if it exists in the storage, otherwise adds the user
     * to the storage and returns the newly created GeoUser object.
     *
     * @param user   the Telegram User object representing the user to retrieve or add
     * @param chatId the chatId corresponding to the user
     * @return the GeoUser corresponding to the given user, either retrieved from the storage or newly created
     */
    public GeoUser getOrRegister(User user, Long chatId) {
        if (users.containsKey(user.getId())) {
            return users.get(user.getId());
        }
        return addUser(user, chatId);
    }
}
