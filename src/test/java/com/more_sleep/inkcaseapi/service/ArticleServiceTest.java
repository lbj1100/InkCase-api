package com.more_sleep.inkcaseapi.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.more_sleep.inkcaseapi.entity.Article;
import com.more_sleep.inkcaseapi.mapper.IArticleMapper;
import lombok.AllArgsConstructor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 *
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest
public class ArticleServiceTest {

    @Autowired
    private IArticleService articleService;

    @Test
    public void List() {
        articleService.list().forEach(System.out::println);
    }

    @Test
    public void PageList() {
        Page<Article> pageInfo = new Page<>(0, 3);

        // 2.条件构造器
        LambdaQueryWrapper<Article> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByDesc(Article::getCreateDate);
        articleService.pageList(pageInfo, queryWrapper);
        pageInfo.getRecords().forEach(System.out::println);
    }
}
