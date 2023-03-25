package bot.storage;

import exceptions.UserNotRegisteredException;
import org.telegram.telegrambots.meta.api.objects.User;

import java.util.HashMap;
import java.util.Map;

public class GeoUserStorage {
    private final Map<Long, GeoUser> users = new HashMap<>();

    public Map<Long, GeoUser> getUsers() {
        return users;
    }

    public void addUser(User user, Long chatId) {
        if (users.containsKey(chatId)) {
            // todo throw a meningful exception that this user is already there
        } else {
            users.put(user.getId(), new GeoUser(user, chatId));
        }
    }

    public GeoUser getUser(long userId) throws UserNotRegisteredException{
        if (!users.containsKey(userId)) {
            throw new UserNotRegisteredException("User with id " + userId + " is not registered!");
        }
        return users.get(userId);
    }
}
