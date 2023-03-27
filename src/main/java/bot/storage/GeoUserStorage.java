package bot.storage;

import exceptions.UserNotRegisteredException;
import org.jetbrains.annotations.NotNull;
import org.telegram.telegrambots.meta.api.objects.User;

import java.util.HashMap;
import java.util.Map;

// TODO MOVE IT TO THE BOT CLASS
public class GeoUserStorage {
    private final Map<Long, GeoUser> users = new HashMap<>();

    public Map<Long, GeoUser> getUsers() {
        return users;
    }

    @NotNull
    public GeoUser addUser(User user, Long userId) {
        GeoUser geoUser = null;
        if (users.containsKey(userId)) {
            // todo throw a meningful exception that this user is already there
            geoUser = users.get(userId);
        } else {
            geoUser = new GeoUser(user, userId);
            users.put(user.getId(), geoUser);
        }
        return geoUser;
    }

    // TODO CHANGE THE FUNCTIONALITY OF GET USER
    public GeoUser getUser(long userId) throws UserNotRegisteredException{
        if (!users.containsKey(userId)) {
            return null;
//            throw new UserNotRegisteredException("User with id " + userId + " is not registered!");
        }
        return users.get(userId);
    }

    /**
     * Returns GeoUser from user list if presented or generates a new one and automatically adds to users
     * */
    // TODO REMOVE USERID FROM HERE USE JUST THE USER
    public GeoUser getOrRegister(User user, Long userId) {
        assert user.getId().equals(userId);
        if (users.containsKey(userId)) {
            return users.get(userId);
        }
        return addUser(user, userId);
    }
}
