package test.util;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import leotech.system.util.RedisClient;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.exceptions.JedisException;
import rfx.core.nosql.jedis.RedisClientFactory;
import rfx.core.nosql.jedis.RedisCommand;
import rfx.core.util.Utils;

public class TestQueue {

    static JedisPool jedisPool = RedisClientFactory.buildRedisPool("pubSubQueue");

    public static void main(String[] args) {
        System.out.println(ts() + "Starting Redis queue test...");

        // Run a basic connection test
        new RedisCommand<Void>(jedisPool) {
            @Override
            protected Void build(Jedis jedis) throws JedisException {
                String pong = jedis.ping();
                System.out.println(ts() + "Redis PING -> " + pong);
                return null;
            }
        }.executeAsync();

        // Use a latch to wait for async callbacks before exiting
        CountDownLatch latch = new CountDownLatch(3);

        new RedisCommand<Void>(jedisPool) {
            @Override
            protected Void build(Jedis jedis) throws JedisException {
                String segmentId = "segment1";

                RedisClient.enqueue(segmentId, "job1", rs -> {
                    System.out.println(ts() + "Enqueued job1: " + rs);
                    latch.countDown();
                });

                RedisClient.enqueue(segmentId, "job2", rs -> {
                    System.out.println(ts() + "Enqueued job2: " + rs);
                    latch.countDown();
                });

                RedisClient.enqueue(segmentId, "job3", rs -> {
                    System.out.println(ts() + "Enqueued job3: " + rs);
                    latch.countDown();
                });

                RedisClient.dequeue(segmentId, rs1 -> {
                    if (rs1 != null) {
                        System.out.println(ts() + "Dequeued: " + rs1);
                        RedisClient.dequeue(segmentId, rs2 -> {
                            if (rs2 != null) {
                                System.out.println(ts() + "Dequeued next: " + rs2);
                            } else {
                                System.out.println(ts() + "No more jobs in queue.");
                            }
                        });
                    } else {
                        System.out.println(ts() + "Queue is empty.");
                    }
                });

                return null;
            }
        }.executeAsync();

        try {
            boolean completed = latch.await(10, TimeUnit.SECONDS);
            if (!completed) {
                System.err.println(ts() + "Timeout waiting for async operations to finish.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Clean shutdown
        Utils.exitSystemAfterTimeout(100);
    }

    private static String ts() {
        return "[" + Thread.currentThread().getName() + " @ " + System.currentTimeMillis() + "] ";
    }
}
