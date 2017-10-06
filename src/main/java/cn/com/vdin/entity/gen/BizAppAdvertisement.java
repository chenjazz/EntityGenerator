package cn.com.vdin.entity.gen;

import javax.persistence.*;
import java.time.*;
import java.util.*;
import java.math.*;
import lombok.*;

@Getter
@Setter
@Entity
@Table(name = "biz_app_advertisements")
public class BizAppAdvertisement {
    @Id
    private String id;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private String appId;

    private LocalDateTime closedAt;

    private String deptId;

    private LocalDateTime openedAt;

    private Integer osType;

    private String skins;

    private Integer state;

}
