package alvin.docker.app.api.model;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class FeedbackForm {
    @NotBlank
    private String title;

    @NotBlank
    private String content;

    protected FeedbackForm() {
    }

    public FeedbackForm(String title, String content) {
        this.title = title;
        this.content = content;
    }
}
