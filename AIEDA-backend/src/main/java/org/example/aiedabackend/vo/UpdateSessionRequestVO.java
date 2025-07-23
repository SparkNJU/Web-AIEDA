package org.example.aiedabackend.vo;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UpdateSessionRequestVO {
    private Integer uid;
    private Integer sid;
    private String title;
}
