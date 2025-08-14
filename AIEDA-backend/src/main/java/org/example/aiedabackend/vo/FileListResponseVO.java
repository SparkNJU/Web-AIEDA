package org.example.aiedabackend.vo;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class FileListResponseVO {
    private List<FileVO> files;
    private Integer totalCount;
}
