package com.seckill.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.seckill.entity.SeckillGoods;
import com.seckill.mapper.SeckillGoodsMapper;
import com.seckill.service.SeckillGoodsService;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class SeckillGoodsServiceImpl extends ServiceImpl<SeckillGoodsMapper, SeckillGoods> implements SeckillGoodsService {

    @Override
    public List<SeckillGoods> listAllSeckillGoods() {
        // MyBatis-Plus 自带查询所有方法，直接调用
        return this.list();
    }
    @Override
    public SeckillGoods getSeckillGoodsById(Long id) {
        // MyBatis-Plus自带查询方法，直接调用
        return this.getById(id);
    }
}