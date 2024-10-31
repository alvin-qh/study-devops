package alvin.docker.infra.model;

import java.io.Serializable;

/**
 * 该接口表示一个具备 id 的实体类型
 */
public interface Identity extends Serializable {
    Long getId();
}
