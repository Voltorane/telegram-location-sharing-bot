package exceptions;

public class InvalidCallbackQueryException extends RuntimeException{
    public InvalidCallbackQueryException(String message) {
        super(message);
    }
}
