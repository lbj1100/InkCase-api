package com.more_sleep.inkcaseapi.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.more_sleep.inkcaseapi.entity.Article;
import com.more_sleep.inkcaseapi.entity.ArticleBody;
import com.more_sleep.inkcaseapi.entity.Comment;
import com.more_sleep.inkcaseapi.entity.User;
import com.more_sleep.inkcaseapi.entity.vo.DataVo;
import com.more_sleep.inkcaseapi.mapper.IArticleBodyMapper;
import com.more_sleep.inkcaseapi.mapper.IArticleMapper;
import com.more_sleep.inkcaseapi.mapper.ICategoryMapper;
import com.more_sleep.inkcaseapi.mapper.IUserMapper;
import com.more_sleep.inkcaseapi.service.IArticleService;
import com.more_sleep.inkcaseapi.service.ICommentService;
import com.more_sleep.inkcaseapi.service.IUserService;
import lombok.AllArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 *
 */
@Service
@AllArgsConstructor
public class ArticleServiceImpl extends ServiceImpl<IArticleMapper, Article> implements IArticleService {
    private final IArticleMapper articleMapper;
    private final IArticleBodyMapper articleBodyMapper;
    private final ICategoryMapper categoryMapper;
    private final IUserMapper userMapper;
    private final IUserService userService;
    private final ICommentService commentService;
    @Override
    public Page<Article> pageList(Page<Article> pageInfo, LambdaQueryWrapper<Article> queryWrapper) {

        // 把ArticleBody和Category装进去
        articleMapper.selectPage(pageInfo, queryWrapper);
        List<Article> list = pageInfo.getRecords().stream().peek(article -> {
            // 数据库中的body字段是ArticleBody的id
            // 应该如何处理？ 1. 通过id查询ArticleBody 2. 通过ArticleBodyMapper查询
            article.setAuthor(userMapper.selectById(article.getAuthorId()));
            article.setBody(articleBodyMapper.selectById(article.getBodyId()));
            article.setCategory(categoryMapper.selectById(article.getCategoryId()));
        }).toList();

        pageInfo.setRecords(list);
        return pageInfo;
    }

    @Override
    public Article getByIdWithBody(Long id) {
        Article article = articleMapper.selectById(id);
        article.setAuthor(userMapper.selectById(article.getAuthorId()));
        article.setBody(articleBodyMapper.selectById(article.getBodyId()));
        article.setCategory(categoryMapper.selectById(article.getCategoryId()));
        return article;
    }

    @Override
    public List<Article> listWithAll(Integer pageNumber, Integer pageSize, Integer year, Integer month, Long categoryId) {
        Page<Article> pageInfo = new Page<>(pageNumber, pageSize);
        System.out.println(pageNumber + " " +pageSize);
        LambdaQueryWrapper<Article> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderBy(true, false, Article::getCreateDate);
//        queryWrapper.eq(year != null, Article::getCreateDate, year);
//        queryWrapper.eq(month != null, Article::getCreateDate, month);
        queryWrapper.apply(year != null, "YEAR(create_date) = {0}", year);
        queryWrapper.apply(month != null, "MONTH(create_date) = {0}", month);
        queryWrapper.eq(categoryId != null, Article::getCategoryId, categoryId);
        articleMapper.selectPage(pageInfo, queryWrapper);

        List<Article> list = pageInfo.getRecords();
        return list.stream().peek(article -> {
            article.setAuthor(userMapper.selectById(article.getAuthorId()));
            article.setBody(articleBodyMapper.selectById(article.getBodyId()));
            article.setCategory(categoryMapper.selectById(article.getCategoryId()));
        }).toList();
    }

    @Override
    public List<Article> getHot(Integer limit) {
        LambdaQueryWrapper<Article> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderBy(true, false, Article::getViewCounts);

        Page<Article> pageInfo = new Page<>();
        pageInfo.setSize(limit);
        pageInfo.setCurrent(0);
        return page(pageInfo,queryWrapper).getRecords();
    }

    @Override
    public List<Article> getNew(Integer limit) {
        LambdaQueryWrapper<Article> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderBy(true, false, Article::getCreateDate);

        Page<Article> pageInfo = new Page<>();
        pageInfo.setSize(limit);
        pageInfo.setCurrent(0);
        return page(pageInfo,queryWrapper).getRecords();
    }

    @Override
    public List<DataVo> getWithTime() {
        return articleMapper.getWithTime();
    }

    @Override
    @Transactional
    public void updateByIdWithAll(Article article) {
        // 根据articleId查询body的Id
        Article oldArticle = getById(article.getId());
        article.setBodyId(oldArticle.getBodyId());
        ArticleBody newBody = article.getBody();
        newBody.setId(oldArticle.getBodyId());
        System.out.println("lbj==" + article);
        articleMapper.updateById(article);
        articleBodyMapper.updateById(newBody);
        categoryMapper.updateById(article.getCategory());
    }

    @Override
    public List<Article> getByCategoryId(Long id) {
        LambdaQueryWrapper<Article> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Article::getCategoryId, id);
        return list(queryWrapper).stream().peek(article -> {
            article.setAuthor(userMapper.selectById(article.getAuthorId()));
            article.setBody(articleBodyMapper.selectById(article.getBodyId()));
            article.setCategory(categoryMapper.selectById(article.getCategoryId()));
        }).toList();
    }

    @Override
    @Transactional
    public Article getArticleAndAddViews(Long id) {
        Article article = articleMapper.selectById(id);
        System.out.println("id:  "+id);
        System.out.println("article:  "+article);
        if (article == null) {
            throw new IllegalArgumentException("Article with id " + id + " not found");
        }
        article.setViewCounts(article.getViewCounts() + 1);
        articleMapper.updateById(article);
        article.setAuthor(userMapper.selectById(article.getAuthorId()));
        article.setBody(articleBodyMapper.selectById(article.getBodyId()));
        article.setCategory(categoryMapper.selectById(article.getCategoryId()));
        return article;
    }

    @Override
    public Long publishArticle(Article article) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        User user = userService.getByName(username);
        article.setCategoryId(
                article.getCategory().getId()
        );
        // 如何拿到body的id
        articleBodyMapper.insert(article.getBody());
        System.out.println("bodyID: "+article.getBody().getId());
        article.setBodyId(article.getBody().getId());
        if (null != user) {
            article.setAuthor(user);
            article.setAuthorId(user.getId());
        }

        articleMapper.insert(article);
        return article.getId();
    }

    @Override
    @Transactional
    public void removeByIdWithAll(Long id) {
        Article article = getById(id);
        LambdaQueryWrapper<Comment> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Comment::getArticleId, id);
        queryWrapper.eq(Comment::getLevel, 2);
        commentService.remove(queryWrapper);
        queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Comment::getArticleId, id);
        queryWrapper.eq(Comment::getLevel, 1);
        commentService.remove(queryWrapper);
        queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Comment::getLevel, 0);
        queryWrapper.eq(Comment::getArticleId, id);
        commentService.remove(queryWrapper);
        articleMapper.deleteById(id);
        articleBodyMapper.deleteById(article.getBodyId());
    }
}
