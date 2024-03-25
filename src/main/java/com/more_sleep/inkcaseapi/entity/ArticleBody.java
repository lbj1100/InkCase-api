package com.more_sleep.inkcaseapi.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * 文章内容
 * @Author: lbj
 * @Date: 2024/3/25
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("me_article_body")
public class ArticleBody {

    private static final long serialVersionUID = -7611409995977927628L;

    private String content; // 内容

    private String contentHtml;
}