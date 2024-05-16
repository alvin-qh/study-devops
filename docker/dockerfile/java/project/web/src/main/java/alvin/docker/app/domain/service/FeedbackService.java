package alvin.docker.app.domain.service;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import alvin.docker.core.http.exception.ClientException;
import alvin.docker.infra.model.Feedback;
import alvin.docker.infra.repository.FeedbackRepository;
import lombok.RequiredArgsConstructor;

/**
 * {@link Feedback} 实体相关的服务类型
 */
@Component
@RequiredArgsConstructor
public class FeedbackService {
    // 注入 Feedback 实体持久化对象
    private final FeedbackRepository feedbackRepository;

    /**
     * 创建实体对象
     *
     * @param feedback {@link Feedback} 实体对象
     */
    @Transactional
    public void create(Feedback feedback) {
        // 查询相同 title 的记录是否存在
        if (feedbackRepository.existsByTitle(feedback.getTitle())) {
            throw new ClientException("duplicate_title");
        }

        // 存储实体对象
        feedbackRepository.save(feedback);
    }

    /**
     * 查询所有的实体对象集合
     *
     * @return 实体对象集合
     */
    @Transactional(readOnly = true)
    public List<Feedback> list() {
        return feedbackRepository.findAll();
    }

    /**
     * 删除实体对象
     *
     * @param id 实体对象 id
     */
    @Transactional
    public void delete(Long id) {
        feedbackRepository.findById(id)
                .ifPresent(entity -> feedbackRepository.delete(entity));
    }
}
