package com.dataplatform.data.service.ticket;

import com.dataplatform.data.mapper.KnowledgeArticleMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 知识库服务 - 数据库持久化版本
 */
@Service
public class KnowledgeService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeService.class);

    private final KnowledgeArticleMapper articleMapper;

    public KnowledgeService(KnowledgeArticleMapper articleMapper) {
        this.articleMapper = articleMapper;
    }

    public KnowledgeArticle createArticle(KnowledgeArticle article) {
        articleMapper.insert(article);
        return article;
    }

    public KnowledgeArticle getArticle(Long id) {
        articleMapper.incrementViewCount(id);
        return articleMapper.selectById(id);
    }

    public List<KnowledgeArticle> searchArticles(String keyword, String category) {
        return articleMapper.search(keyword, category);
    }

    public List<KnowledgeArticle> getPopularArticles(int topN) {
        return articleMapper.selectPopular(topN);
    }

    public void markHelpful(Long id) {
        articleMapper.incrementHelpfulCount(id);
    }

    public KnowledgeArticle updateArticle(Long id, KnowledgeArticle update) {
        update.setId(id);
        articleMapper.update(update);
        return articleMapper.selectById(id);
    }

    public boolean deleteArticle(Long id) {
        KnowledgeArticle existing = articleMapper.selectById(id);
        if (existing == null) return false;
        articleMapper.delete(id);
        return true;
    }
}
