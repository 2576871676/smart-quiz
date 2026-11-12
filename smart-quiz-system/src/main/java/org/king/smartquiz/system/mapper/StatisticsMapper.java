package org.king.smartquiz.system.mapper;

import org.apache.ibatis.annotations.Param;
import org.king.smartquiz.system.vo.AnswerCountVO;
import org.king.smartquiz.system.vo.DimensionStatVO;
import org.king.smartquiz.system.vo.TrendPointVO;
import org.king.smartquiz.system.vo.WeakPointVO;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 学习统计聚合 Mapper（自定义 SQL，见 resources/mapper/StatisticsMapper.xml）。
 * 聚合在数据库侧完成，answer_record 与 question 联表取题型/难度/科目/知识点维度。
 */
public interface StatisticsMapper {

    /** 答题计数（总数/对/错/未判分，单行条件聚合） */
    AnswerCountVO countAnswers(@Param("userId") Long userId);

    /** 按题型正确率 */
    List<DimensionStatVO> accuracyByType(@Param("userId") Long userId);

    /** 按难度正确率 */
    List<DimensionStatVO> accuracyByDifficulty(@Param("userId") Long userId);

    /** 按科目正确率 */
    List<DimensionStatVO> accuracyBySubject(@Param("userId") Long userId);

    /** 最近答题趋势（按天） */
    List<TrendPointVO> trend(@Param("userId") Long userId, @Param("since") LocalDateTime since);

    /** 薄弱知识点 TOP（按答错次数降序） */
    List<WeakPointVO> weakPoints(@Param("userId") Long userId, @Param("limit") int limit);

    /** 最近 N 天答题数 */
    Integer countRecent(@Param("userId") Long userId, @Param("since") LocalDateTime since);
}
