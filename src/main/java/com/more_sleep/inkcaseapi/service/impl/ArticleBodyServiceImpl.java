package com.more_sleep.inkcaseapi.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.more_sleep.inkcaseapi.entity.ArticleBody;
import com.more_sleep.inkcaseapi.mapper.IArticleBodyMapper;
import com.more_sleep.inkcaseapi.service.IArticleBodyService;
import org.springframework.stereotype.Service;

/**
 *
 */
@Service
public class ArticleBodyServiceImpl extends ServiceImpl<IArticleBodyMapper, ArticleBody> implements IArticleBodyService{
}
