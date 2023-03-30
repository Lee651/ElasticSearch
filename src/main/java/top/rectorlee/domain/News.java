package top.rectorlee.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Lee
 * @description 新闻实体
 * @date 2023-03-22  22:04:27
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class News {
    private Integer id;

    private String title;

    private String url;

    private String content;
}
