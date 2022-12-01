package alvin.docker.infra.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import alvin.docker.infra.model.Feedback;

@Repository
public interface FeedbackRepository extends CrudRepository<Feedback, Long>, JpaSpecificationExecutor<Feedback> {
    List<Feedback> findAll();
}
