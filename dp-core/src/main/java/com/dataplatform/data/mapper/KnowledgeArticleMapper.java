package com.dataplatform.data.mapper;

import com.dataplatform.data.service.ticket.KnowledgeArticle;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface KnowledgeArticleMapper {

    void insert(KnowledgeArticle article);

    KnowledgeArticle selectById(Long id);

    List<KnowledgeArticle> search(@Param("keyword") String keyword,
                                   @Param("category") String category);

    List<KnowledgeArticle> selectPopular(@Param("topN") int topN);

    void update(KnowledgeArticle article);

    void delete(Long id);

    void incrementViewCount(Long id);

    void incrementHelpfulCount(Long id);
}
