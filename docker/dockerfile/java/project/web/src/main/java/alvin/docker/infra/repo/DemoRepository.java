package alvin.docker.infra.repo;

import alvin.docker.infra.model.Demo;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DemoRepository extends CrudRepository<Demo, Long>, JpaSpecificationExecutor<Demo> {
}
