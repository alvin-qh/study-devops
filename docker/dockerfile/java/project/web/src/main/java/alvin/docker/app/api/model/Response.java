package alvin.docker.app.api.model;

import lombok.Getter;

@Getter
public class Response<T> {
    private String status;
    private T payload;

    private Response(String status, T payload) {
        this.status = status;
        this.payload = payload;
    }

    public static <T> Response<T> success(T payload) {
        return new Response<>("success", payload);
    }

    public static <T> Response<T> error(T payload) {
        return new Response<>("error", payload);
    }
}
