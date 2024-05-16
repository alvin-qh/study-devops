package alvin.docker.app.endpoint.model;

import java.io.Serializable;

import javax.validation.constraints.NotBlank;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * {@link alvin.docker.infra.model.Feedback Feedback} 相关的表单类型
 */
@Data
@NoArgsConstructor(access = AccessLevel.PACKAGE)
@AllArgsConstructor
public class FeedbackForm implements Serializable {
    @NotBlank
    private String title;

    @NotBlank
    private String content;
}
