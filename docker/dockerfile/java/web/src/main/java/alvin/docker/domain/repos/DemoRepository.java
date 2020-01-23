package alvin.docker.domain.repos;

import alvin.docker.domain.models.Demo;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DemoRepository extends CrudRepository<Demo, Long>, JpaSpecificationExecutor<Demo> {
}
