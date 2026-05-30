package com.seckill.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.seckill.entity.SeckillGoods;
import java.util.List;

public interface SeckillGoodsService extends IService<SeckillGoods> {
    // 查询所有秒杀商品（用户端）
    List<SeckillGoods> listAllSeckillGoods();
    // 新增：根据ID查询秒杀商品
    SeckillGoods getSeckillGoodsById(Long id);
}