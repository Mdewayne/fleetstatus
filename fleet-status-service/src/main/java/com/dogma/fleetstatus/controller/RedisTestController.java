package com.dogma.fleetstatus.controller;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/redis")
public class RedisTestController {
    
    private final RedisTemplate<String, Object> redisTemplate;

    public RedisTestController(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @PostMapping("/{key}/{value}")
    public String setValue(@PathVariable String key, 
                          @PathVariable String value) {
        redisTemplate.opsForValue().set(key, value);
        return "OK";
    }

    @GetMapping("/{key}")
    public String getValue(@PathVariable String key) {
        Object value = redisTemplate.opsForValue().get(key);
        return value != null ? value.toString() : "Key not found";
    }
    
    @DeleteMapping("/{key}")
    public String deleteKey(@PathVariable String key) {
        Boolean deleted = redisTemplate.delete(key);
        return deleted ? "Deleted: " + key : "Key not found: " + key;
    }
}
