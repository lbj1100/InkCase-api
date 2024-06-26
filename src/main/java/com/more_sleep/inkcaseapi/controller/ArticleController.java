package com.more_sleep.inkcaseapi.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.more_sleep.inkcaseapi.common.R;
import com.more_sleep.inkcaseapi.entity.Article;
import com.more_sleep.inkcaseapi.entity.vo.DataVo;
import com.more_sleep.inkcaseapi.service.IArticleService;
import lombok.AllArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 *
 */


@RestController
@RequestMapping("/article")
@AllArgsConstructor
public class ArticleController {
    private final IArticleService articleService;


    // http://localhost:8888/articles?pageNumber=1&pageSize=5&name=a.createDate&sort=desc
    @GetMapping("/page")
    public R<Page<Article>> page(Integer pageNumber, Integer pageSize, Long categoryId) {

        System.out.println("lbj=="+categoryId);

        // 1.分页构造器
        Page<Article> pageInfo = new Page<>(pageNumber, pageSize);

        // 2.条件构造器
         LambdaQueryWrapper<Article> queryWrapper = new LambdaQueryWrapper<>();
         queryWrapper.orderByDesc(Article::getCreateDate);

        if (categoryId != null){
            queryWrapper.eq(Article::getCategoryId, categoryId);
        }


        // 3.执行
        articleService.page(pageInfo, queryWrapper);
        return R.success(articleService.pageList(pageInfo, queryWrapper));
    }

    @GetMapping("/{id}")
    public R<Article> get(@PathVariable Long id) {
        return R.success(articleService.getByIdWithBody(id));
    }

    @GetMapping
    // @Cacheable的作用是将方法的返回值缓存起来，以便下次使用相同的参数时，可以直接从缓存中获取，而不需要再次执行该方法
    // value：缓存的名称，必须指定至少一个
    // key：缓存的key，默认为空，表示使用方法的参数类型及参数值作为key，支持SpEL
    // unless：条件表达式，当结果为true时，不缓存，默认为空，支持SpEL
    @Cacheable(value = "article",
            key = " #pageNumber+'_'+#pageSize+'_'+#year+'_'+#month+'_'+#categoryId ",
            unless = "!(#year != null && #month != null && #year < T(java.time.LocalDate).now().getYear() || (#year == T(java.time.LocalDate).now().getYear() && #month < T(java.time.LocalDate).now().getMonthValue()))"
    )
    public R<List<Article>> list(Integer pageNumber, Integer pageSize,
                                 Integer year, Integer month, Long categoryId) {
        return R.success(articleService.listWithAll(pageNumber, pageSize, year, month, categoryId));
    }

    @GetMapping("/hot")
    @Cacheable(value = "hot", key = "'hot'")
    public R<List<Article>> getHot() {
        return R.success(articleService.getHot(6));
    }

    @GetMapping("/new")
    public R<List<Article>> getNew() {
        return R.success(articleService.getNew(6));
    }

    @GetMapping("/listArchives")
    public R<List<DataVo>> getWithTime() {
        return R.success(articleService.getWithTime());
    }

    @GetMapping("/category/{id}")
    public R<List<Article>> getByCategoryId(@PathVariable Long id) {
        System.out.println("lbj=="+id);
        return R.success(articleService.getByCategoryId(id));
    }

    @DeleteMapping("/{id}")
    @CacheEvict(value = {"article"}, key = "'view_' + #id")
    public R<String> delete(@PathVariable Long id) {
        articleService.removeByIdWithAll(id);
        return R.success("删除成功");
    }

    @PutMapping
    @CacheEvict(value = {"article"}, key = "'view_' + #article.id")
    public R<String> update(@RequestBody Article article) {
        articleService.updateByIdWithAll(article);
        return R.success("修改成功");
    }

    @GetMapping("/view/{id}")
// 缓存文章的浏览量大于me.view.count
// 如果文章浏览量大于count，那么清除hot缓存
    @CacheEvict(value = "hot", allEntries = true, condition = "#result.data.viewCounts > @environment.getProperty('${me.view.count}', T(java.lang.Integer) )")
    public R<Article> getArticleAndAddViews(@PathVariable("id") Long id) {
        Article article = articleService.getArticleAndAddViews(id);

        return R.success(article);
    }

    @PostMapping("/publish")
    // @CacheEvict的作用是清除缓存
    // value：缓存的名称，必须指定至少一个
    // allEntries：是否清空所有缓存，默认为false
    // 如果我想清空指定的缓存，可以使用key属性
    // key：缓存的key，默认为空，表示使用方法的参数类型及参数值作为key，支持SpEL
    // condition：条件表达式，当结果为true时，清除缓存，默认为空，支持SpEL
    // 如果有多个key，可以使用key属性的数组形式
    @CacheEvict(value = {"article", "category"})
    public R<Map> publicArticle(@RequestBody Article article) {
        // 如果id为空，说明是新增
        Long articleId = null;
        if (article.getId() == null) {
            System.out.println("lbj==publishArticle");
            articleId = articleService.publishArticle(article);
        } else {
            System.out.println("lbj==up" + article);
            articleService.updateByIdWithAll(article);
            articleId = article.getId();
        }


        return R.success(Map.of("articleId", articleId));
    }
}
