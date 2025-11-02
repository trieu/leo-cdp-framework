package test.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import leotech.system.util.RedisPubSubClient;
import redis.clients.jedis.JedisPool;
import rfx.core.util.Utils;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RedisPubSubClientTest {

    private static final String HELLO_REDIS_8_X = "hello-redis-8.x";
	private static final String CHANNEL = "test-channel";
    private static final String QUEUE = "test-queue";
    private static final JedisPool pool = new JedisPool("localhost", 6480);

    @BeforeAll
    static void setup() {
        System.out.println("🔧 Ensure Redis 8.x is running at localhost");
       
        try {
        	 boolean ok = pool.borrowObject().ping().equals("PONG");
        	 System.out.println("=> Is Redis 8.x running ? " + ok);
		} catch (Exception e) {
			// TODO: handle exception
		}
    }

    @AfterAll
    static void teardown() {
        RedisPubSubClient.shutdown();
        pool.close();
    }

    @Test
    @Order(1)
    void testPublishSubscribe() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> received = new AtomicReference<>();

        RedisPubSubClient.subscribeAsync(CHANNEL, new RedisPubSubClient.RedisPubSubCallback() {
            @Override
            public void process(String channel, String message) {
            	System.out.println("[Subscriber] process " + message + " at " +new Date());
                received.set(message);
                latch.countDown();
            }

            @Override
            public void log(String s) {
                System.out.println("[Subscriber] log " + s + " at " +new Date());
            }
        });

        // Give the subscriber a moment to connect
        Utils.sleep(2000);

        RedisPubSubClient.publish(CHANNEL, HELLO_REDIS_8_X, count -> {
            System.out.println("Published to " + CHANNEL + " subscribers: " + count + " at " +new Date());
        });

        boolean ok = latch.await(5, TimeUnit.SECONDS);
        assertTrue(ok, "Subscriber did not receive message in time");
        assertEquals(HELLO_REDIS_8_X, received.get());
    }

    @Test
    @Order(2)
    void testEnqueueDequeue() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> result = new AtomicReference<>();

        RedisPubSubClient.enqueue(QUEUE, "item-123", len -> System.out.println("Queue len: " + len));

        // Allow some time to enqueue
        Thread.sleep(200);

        RedisPubSubClient.dequeue(QUEUE, val -> {
            result.set(val);
            latch.countDown();
        });

        boolean ok = latch.await(2, TimeUnit.SECONDS);
        assertTrue(ok, "Queue operation did not complete");
        assertEquals("item-123", result.get());
    }
}