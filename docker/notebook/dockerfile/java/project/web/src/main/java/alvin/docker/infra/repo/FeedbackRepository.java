package alvin.docker.infra.repo;

import alvin.docker.infra.model.Feedback;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FeedbackRepository extends CrudRepository<Feedback, Long>, JpaSpecificationExecutor<Feedback> {
    List<Feedback> findAll();
}
