package com.seckill.controller.goods;

import com.seckill.dto.SeckillGoodsDTO;
import com.seckill.result.Result;
import com.seckill.service.AdminGoodsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/goods/admin")
public class AdminGoodsController {

    @Autowired
    private AdminGoodsService adminGoodsService;
   // ===================== 秒杀商品接口 =====================
    @GetMapping("/seckill/list")
    public Result<List<Map<String, Object>>> listAllSeckillGoods() {
        return adminGoodsService.listAllSeckillGoods();
    }

    @PostMapping("/seckill/add")
    public Result addSeckillGoods(@RequestBody SeckillGoodsDTO dto) {
        return adminGoodsService.addSeckillGoods(dto);
    }

    @PutMapping("/seckill/update/{id}")
    public Result updateSeckillGoods(@PathVariable Long id, @RequestBody SeckillGoodsDTO dto) {
        return adminGoodsService.updateSeckillGoods(id, dto);
    }

    @DeleteMapping("/seckill/delete/{id}")
    public Result deleteSeckillGoods(@PathVariable Long id) {
        return adminGoodsService.deleteSeckillGoods(id);
    }

    @GetMapping("/seckill/{id}")
    public Result<Map<String, Object>> getSeckillGoods(@PathVariable Long id) {
        return adminGoodsService.getSeckillGoods(id);
    }
}