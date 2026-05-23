package com.ruoyi.mall.domain;

import java.util.Date;

/**
 * 跑腿评价表 mall_runner_rating
 */
public class MallRunnerRating
{
    private Long   ratingId;
    private Long   orderId;
    private Long   runnerMemberId;
    private Long   raterMemberId;
    private Integer score;
    private String comment;
    private Date   createTime;

    /** 非DB：评价人昵称 */
    private String raterNickName;
    /** 非DB：评价人头像 */
    private String raterAvatarUrl;

    public Long    getRatingId()       { return ratingId; }
    public void    setRatingId(Long v) { this.ratingId = v; }

    public Long    getOrderId()       { return orderId; }
    public void    setOrderId(Long v) { this.orderId = v; }

    public Long    getRunnerMemberId()       { return runnerMemberId; }
    public void    setRunnerMemberId(Long v) { this.runnerMemberId = v; }

    public Long    getRaterMemberId()       { return raterMemberId; }
    public void    setRaterMemberId(Long v) { this.raterMemberId = v; }

    public Integer getScore()          { return score; }
    public void    setScore(Integer v) { this.score = v; }

    public String  getComment()        { return comment; }
    public void    setComment(String v){ this.comment = v; }

    public Date    getCreateTime()       { return createTime; }
    public void    setCreateTime(Date v) { this.createTime = v; }

    public String  getRaterNickName()        { return raterNickName; }
    public void    setRaterNickName(String v){ this.raterNickName = v; }

    public String  getRaterAvatarUrl()        { return raterAvatarUrl; }
    public void    setRaterAvatarUrl(String v){ this.raterAvatarUrl = v; }
}
